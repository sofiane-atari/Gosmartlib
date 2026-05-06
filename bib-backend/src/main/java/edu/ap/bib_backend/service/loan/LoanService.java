package edu.ap.bib_backend.service.loan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.ap.bib_backend.jpa.entity.loan.Loan;
import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import edu.ap.bib_backend.service.smartschool.SmartschoolNotificationService;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.jpa.entity.AuthSession;
import edu.ap.bib_backend.jpa.repository.AuthSessionRepository;

import edu.ap.bib_backend.service.user.CurrentUserService;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanHistoryDTO;
import edu.ap.bib_backend.event.BookReturnedGamificationEvent;
import edu.ap.bib_backend.jpa.entity.AuthSession;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;

import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.enums.LoanStatus;
import edu.ap.bib_backend.jpa.repository.AuthSessionRepository;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.loan.LoanRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.service.book.BookService;
import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import edu.ap.bib_backend.service.smartschool.SmartschoolNotificationService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final BookService bookService;
    private final BookCopyRepository bookCopyRepository;
    private final ExternalUserDisplayService externalUserDisplayService;
    private final SmartschoolNotificationService smartschoolNotificationService;
    private final AuthSessionRepository authSessionRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${app.frontend-base-url:http://localhost:4200}")
    String frontendBaseUrl;

    public LoanService(
            LoanRepository loanRepository,
            LoanMapper loanMapper,
            BookService bookService,
            BookCopyRepository bookCopyRepository,
            ExternalUserDisplayService externalUserDisplayService,
            AuthSessionRepository authSessionRepository,
            SmartschoolNotificationService smartschoolNotificationService,
            @Value("${app.frontend-base-url:http://localhost:4200}") String frontendBaseUrl,
            CurrentUserService currentUserService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.bookService = bookService;
        this.bookCopyRepository = bookCopyRepository;
        this.externalUserDisplayService = externalUserDisplayService;
        this.authSessionRepository = authSessionRepository;
        this.smartschoolNotificationService = smartschoolNotificationService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.currentUserService = currentUserService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public LoanDTO create(LoanDTO loanDTO, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        LocalDate parsedReturnDate;

        try {
            parsedReturnDate = LocalDate.parse(loanDTO.returnDate());
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Retourdatum heeft een ongeldig formaat.");
        }

        if (!parsedReturnDate.isAfter(today)) {
            throw new IllegalStateException("Retourdatum moet minimaal één dag in de toekomst liggen.");
        }

        Loan loan = loanMapper.toEntity(loanDTO);
        loan.setCampus(campus);
        loan.setReturnDate(parsedReturnDate);

        // saving to ensure we have an ID for the loan before associating book copies
        loan = loanRepository.save(loan);

        for (var id : loanDTO.bookIds()) {

            Book book = bookService.getBookById(id, true);
            BookCopy copy = book.getBookCopies().getFirst();
            loan.getCopies().add(copy);
        }

        loan.setLoanDate(today);

        for (Long bookId : loanDTO.bookIds()) {
            Book book = bookService.getBookByIdForCurrentCampus(bookId, authentication);

            BookCopy copy = bookCopyRepository
                    .findFirstByBookIdAndCampusIdAndAvailability(book.getId(), campus.getId(), Availability.AVAILABLE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Geen beschikbaar exemplaar gevonden voor boek: " + book.getTitle()
                    ));

            copy.setAvailability(Availability.BORROWED);
            loan.getCopies().add(copy);
        }

        Loan saved = loanRepository.save(loan);
        sendSmartschoolLoanNotification(saved);

        return loanMapper.toDto(saved);
    }

    public PageDTO<LoanDTO> findActiveLoans(int page, int pageSize, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);

        List<Loan> loans = loanRepository.findByCampusIdAndReturnedDateIsNull(campus.getId());

        return toPagedLoanDtoResponse(loans, page, pageSize);
    }

    public PageDTO<LoanDTO> findAll(int page, int pageSize, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);

        List<Loan> loans = loanRepository.findByCampusId(campus.getId());

        return toPagedLoanDtoResponse(loans, page, pageSize);
    }

    @Transactional
    public LoanDTO returnLoan(Long loanId, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalStateException("Lening niet gevonden."));

        if (loan.getCampus() == null || !loan.getCampus().getId().equals(campus.getId())) {
            throw new IllegalStateException("Lening niet gevonden in deze campus.");
        }

        if (loan.getReturnedDate() != null) {
            throw new IllegalStateException("Deze lening is al teruggebracht.");
        }

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        loan.setReturnedDate(today);

        Loan savedLoan = loanRepository.save(loan);

        publishBookReturnedEvent(savedLoan, today);

        return loanMapper.toDto(savedLoan);
    }

    private void publishBookReturnedEvent(Loan loan, LocalDate returnedDate) {
        if (loan.getUser() == null || loan.getUser().getId() == null) {
            return;
        }

        boolean returnedOnTime = !returnedDate.isAfter(loan.getReturnDate());

        applicationEventPublisher.publishEvent(
                new BookReturnedGamificationEvent(
                        loan.getUser().getId(),
                        loan.getId(),
                        returnedOnTime,
                        loan.getReturnDate(),
                        returnedDate,
                        LocalDateTime.now()
                )
        );
    }

    private void sendSmartschoolLoanNotification(Loan loan) {
        try {
            if (loan.getUser() == null) {
                return;
            }

            String userId = loan.getUser().getId();

            AuthSession authSession = authSessionRepository.findById(userId)
                    .orElse(null);

            if (authSession == null) {
                return;
            }

            String accessToken = authSession.getSmartschoolAccessToken();

            if (accessToken == null || accessToken.isBlank()) {
                return;
            }

            String smartschoolBaseUrl = loan.getCopies().stream()
                    .findFirst()
                    .map(BookCopy::getCampus)
                    .map(Campus::getSchool)
                    .map(school -> school.getDomainName())
                    .orElse(null);

            if (smartschoolBaseUrl == null || smartschoolBaseUrl.isBlank()) {
                return;
            }

            String bookTitles = loan.getCopies().stream()
                    .map(copy -> copy.getBook().getTitle())
                    .collect(Collectors.joining(", "));

            long daysUntilReturn = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(ZoneId.of("Europe/Brussels")),
                    loan.getReturnDate());

            String title = "Boek uitgeleend";

            String returnLabel = daysUntilReturn == 1 ? "dag" : "dagen";

            String bookPart = loan.getCopies().size() == 1
                    ? "het volgende boek"
                    : "de volgende boeken";

            String mustPart = loan.getCopies().size() == 1
                    ? "Dit moet"
                    : "Deze moeten";

            String description = "U heeft " + bookPart + " uitgeleend: "
                    + bookTitles
                    + ". " + mustPart + " binnen "
                    + daysUntilReturn
                    + " " + returnLabel
                    + " terug binnen zijn, ten laatste op "
                    + loan.getReturnDate()
                    + ".";

            String notificationUrl = normalizeFrontendBaseUrl(frontendBaseUrl)
                    + "/history";

            smartschoolNotificationService.sendNotification(
                    smartschoolBaseUrl,
                    accessToken,
                    title,
                    description,
                    notificationUrl);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String normalizeFrontendBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:4200";
        }

        String normalized = baseUrl.trim();

        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private int getLoanSortPriority(Loan loan, LocalDate today) {
        if (loan.getReturnedDate() == null && loan.getReturnDate().isBefore(today)) {
            return 1; // Te laat
        }

        if (loan.getReturnedDate() == null) {
            return 2; // Uitgeleend
        }

        return 3; // Teruggebracht
    }

    private PageDTO<LoanDTO> toPagedLoanDtoResponse(List<Loan> loans, int page, int pageSize) {
        Map<String, Set<String>> userIdsBySchoolCode = loans.stream()
                .filter(loan -> loan.getUser() != null)
                .filter(loan -> loan.getUser().getSchool() != null)
                .filter(loan -> loan.getUser().getSchool().getDomainName() != null)
                .filter(loan -> loan.getUser().getOneRosterSourcedId() != null)
                .collect(Collectors.groupingBy(
                        loan -> loan.getUser().getSchool().getDomainName(),
                        Collectors.mapping(
                                loan -> loan.getUser().getOneRosterSourcedId(),
                                Collectors.toSet())));

        Map<String, String> displayNamesByUserId = new java.util.HashMap<>();

        userIdsBySchoolCode.forEach((schoolCode, userIds) -> {
            Map<String, String> names = externalUserDisplayService.getDisplayNames(schoolCode, userIds);
            displayNamesByUserId.putAll(names);
        });

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));

        List<LoanDTO> sortedDtos = loans.stream()
                .sorted(
                        Comparator
                                .comparingInt((Loan loan) -> getLoanSortPriority(loan, today))
                                .thenComparing(Loan::getReturnDate))
                .map(loan -> {
                    LoanDTO dto = loanMapper.toDto(loan);

                    String lookupId = loan.getUser().getOneRosterSourcedId();

                    return LoanDTO.builder()
                            .id(dto.id())
                            .userId(dto.userId())
                            .userDisplayName(displayNamesByUserId.getOrDefault(lookupId, "Onbekende gebruiker"))
                            .bookIds(dto.bookIds())
                            .books(dto.books())
                            .loanDate(dto.loanDate())
                            .returnDate(dto.returnDate())
                            .returnedDate(dto.returnedDate())
                            .status(dto.status())
                            .build();
                })
                .toList();

        int safePage = Math.max(page, 0);
        int safePageSize = Math.max(pageSize, 1);

        int totalItems = sortedDtos.size();
        int totalPages = (int) Math.ceil((double) totalItems / safePageSize);

        int fromIndex = safePage * safePageSize;
        int toIndex = Math.min(fromIndex + safePageSize, totalItems);

        List<LoanDTO> pageData = fromIndex >= totalItems
                ? List.of()
                : sortedDtos.subList(fromIndex, toIndex);

        PaginatedMeta meta = new PaginatedMeta(
                safePage,
                safePageSize,
                totalItems,
                totalPages);

        return new PageDTO<>(pageData, meta);
    }

    public LoanHistoryDTO findHistoryForCurrentUser(Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        List<LoanDTO> loans = loanRepository
                .findByUserIdAndCampusIdOrderByLoanDateDesc(currentUserId, campus.getId())
                .stream()
                .map(loanMapper::toDto)
                .toList();

        List<LoanDTO> activeLoans = loans.stream()
                .filter(loan -> loan.status() == LoanStatus.ACTIVE || loan.status() == LoanStatus.OVERDUE)
                .toList();

        List<LoanDTO> returnedLoans = loans.stream()
                .filter(loan -> loan.status() == LoanStatus.RETURNED)
                .toList();

        int totalLoanedBookCount = loans.stream()
                .mapToInt(loan -> loan.books() != null ? loan.books().size() : loan.bookIds().size())
                .sum();

        return LoanHistoryDTO.builder()
                .activeLoanCount(activeLoans.size())
                .returnedLoanCount(returnedLoans.size())
                .totalLoanedBookCount(totalLoanedBookCount)
                .activeLoans(activeLoans)
                .returnedLoans(returnedLoans)
                .build();
    }
}