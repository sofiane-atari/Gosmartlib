package edu.ap.bib_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanHistoryDTO;
import edu.ap.bib_backend.dto.loan.LoanStatsDTO;
import edu.ap.bib_backend.event.BookReturnedGamificationEvent;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.LoanStatus;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.book.BookService;
import edu.ap.bib_backend.service.message.HtmlMessageBuilder;
import edu.ap.bib_backend.service.message.component.BookRecommendationComponent;
import edu.ap.bib_backend.service.message.component.LoanBookListComponent;
import edu.ap.bib_backend.service.message.component.LoanGeneralInfoComponent;

import edu.ap.bib_backend.service.user.CurrentUserService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Service
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final BookService bookService;
    private final ExternalUserDisplayService externalUserDisplayService;
    private final BookCopyRepository bookCopyRepository;
    private final UserRepository userRepository;
    private final SmartschoolMessageService smartschoolMessageService;
    private final SmartschoolTokenRefreshService smartschoolTokenRefreshService;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final String frontendUrl;

    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper, BookService bookService,
            ExternalUserDisplayService externalUserDisplayService, BookCopyRepository bookCopyRepository,
            UserRepository userRepository, SmartschoolMessageService smartschoolMessageService,
            SmartschoolTokenRefreshService smartschoolTokenRefreshService,
            CurrentUserService currentUserService,
            ApplicationEventPublisher applicationEventPublisher,
            @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL:http://localhost:4200}") String frontendUrl) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.bookService = bookService;
        this.externalUserDisplayService = externalUserDisplayService;
        this.bookCopyRepository = bookCopyRepository;
        this.userRepository = userRepository;
        this.smartschoolMessageService = smartschoolMessageService;
        this.smartschoolTokenRefreshService = smartschoolTokenRefreshService;
        this.currentUserService = currentUserService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public List<LoanDTO> create(LoanDTO loanDTO, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);

        if (loanDTO.bookIds() == null || loanDTO.bookIds().isEmpty()) {
            throw new IllegalStateException("Een uitlening moet minstens één boek bevatten.");
        }

        User user = userRepository.findById(loanDTO.userId())
                .orElseThrow(() -> new IllegalStateException("Gebruiker niet gevonden."));

        LocalDate parsedReturnDate = resolveReturnDate(loanDTO);
        List<Long> requestedBookIds = expandRequestedBookIds(loanDTO);

        List<Loan> createdLoanEntities = new java.util.ArrayList<>();
        List<LoanDTO> createdLoans = new java.util.ArrayList<>();

        for (Long bookId : requestedBookIds) {
            Book book = bookService.getBookById(bookId, true);

            log.debug(
                    "Reserving one copy for book '{}' (id={}, isbn={}, author={})",
                    book.getTitle(),
                    bookId,
                    book.getIsbn(),
                    book.getAuthor());

            BookCopy availableCopy = bookCopyRepository
                    .findAvailableCopiesByBookIdAndCampusId(bookId, campus.getId())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Geen beschikbaar exemplaar voor boek: " + book.getTitle()));

            Loan singleBookLoan = new Loan();
            singleBookLoan.setUser(user);
            singleBookLoan.setCampus(campus);
            singleBookLoan.setLoanDate(LocalDate.now(ZoneId.of("Europe/Brussels")));
            singleBookLoan.setReturnDate(parsedReturnDate);
            singleBookLoan.getCopies().add(availableCopy);

            Loan savedLoan = loanRepository.save(singleBookLoan);

            createdLoanEntities.add(savedLoan);
            createdLoans.add(loanMapper.toDto(savedLoan));
        }

        sendLoanConfirmationMessage(
                loanDTO.userId(),
                loanDTO.bookIds(),
                createdLoanEntities);

        return createdLoans;
    }

    public PageDTO<LoanDTO> findAll(int page, int pageSize, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);

        List<Loan> loans = loanRepository.findByCampusId(campus.getId());

        return toPagedLoanDtoResponse(loans, page, pageSize);
    }

    public PageDTO<LoanDTO> findActiveLoans(int page, int pageSize, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);

        List<Loan> loans = loanRepository.findByCampusIdAndReturnedDateIsNull(campus.getId());

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
                        LocalDateTime.now()));
    }

    private int resolveCopiesCount(LoanDTO loanDTO, Long bookId) {
        if (loanDTO.books() == null) {
            return 1;
        }

        return loanDTO.books().stream()
                .filter(book -> bookId.equals(book.id()))
                .findFirst()
                .map(book -> book.copiesCount() != null ? book.copiesCount() : 1)
                .orElse(1);
    }

    private LocalDate calculateDefaultReturnDate(String userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    Integer durationDays = user.getSchool().getDefaultLoanDurationDays();
                    if (durationDays == null) {
                        durationDays = 15;
                    }
                    return LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(durationDays);
                })
                .orElseThrow(() -> new IllegalStateException("Gebruiker niet gevonden."));
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

                    String lookupId = loan.getUser() != null ? loan.getUser().getOneRosterSourcedId() : null;

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

    private int getLoanSortPriority(Loan loan, LocalDate today) {
        if (loan.getReturnedDate() == null && loan.getReturnDate().isBefore(today)) {
            return 1;
        }

        if (loan.getReturnedDate() == null) {
            return 2;
        }

        return 3;
    }

    public LoanStatsDTO getLoanStats() {
        LocalDate today = LocalDate.now();

        long activeLoans = loanRepository
                .countByReturnedDateIsNullAndReturnDateGreaterThanEqual(today);

        long overdueLoans = loanRepository
                .countByReturnedDateIsNullAndReturnDateBefore(today);

        long returnedLoans = loanRepository
                .countByReturnedDateIsNotNull();

        return new LoanStatsDTO(activeLoans, overdueLoans, returnedLoans);
    }

    private LocalDate resolveReturnDate(LoanDTO loanDTO) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));

        if (loanDTO.returnDate() != null && !loanDTO.returnDate().isBlank()) {
            try {
                LocalDate parsedReturnDate = LocalDate.parse(loanDTO.returnDate());

                if (!parsedReturnDate.isAfter(today)) {
                    throw new IllegalStateException("Retourdatum moet minimaal één dag in de toekomst liggen.");
                }

                return parsedReturnDate;
            } catch (DateTimeParseException e) {
                throw new IllegalStateException("Retourdatum heeft een ongeldig formaat.");
            }
        }

        return calculateDefaultReturnDate(loanDTO.userId());
    }

    private List<Long> expandRequestedBookIds(LoanDTO loanDTO) {
        List<Long> expandedBookIds = new java.util.ArrayList<>();

        for (Long bookId : loanDTO.bookIds()) {
            int copiesCount = resolveCopiesCount(loanDTO, bookId);

            for (int i = 0; i < copiesCount; i++) {
                expandedBookIds.add(bookId);
            }
        }

        return expandedBookIds;
    }

    private void sendLoanConfirmationMessage(
            String userId,
            List<Long> requestedBookIds,
            List<Loan> createdLoans) {
        if (createdLoans == null || createdLoans.isEmpty()) {
            return;
        }

        try {
            String token = smartschoolTokenRefreshService.getValidSmartschoolAccessToken(userId);

            if (token == null || token.isBlank()) {
                return;
            }

            log.info(
                    "Sending Smartschool message for {} new loans with user ID: {}",
                    createdLoans.size(),
                    userId);

            String schoolDomain = userRepository.findById(userId)
                    .map(user -> user.getSchool().getDomainName())
                    .orElse(null);

            log.debug(
                    "Fetching recommendations for bookIds={}, schoolDomain={}",
                    requestedBookIds,
                    schoolDomain);

            List<BookDTO> recommendations = bookService.getRelatedBooksForLoan(
                    requestedBookIds,
                    4,
                    schoolDomain);

            recommendations = recommendations.stream()
                    .filter(recommendation -> requestedBookIds.stream()
                            .noneMatch(loanBookId -> loanBookId.equals(recommendation.getId())))
                    .toList();

            log.debug("Recommendations fetched: {} books", recommendations.size());

            String htmlMessage = buildCombinedLoanMessage(createdLoans, recommendations);

            smartschoolMessageService.sendMessage(
                    token,
                    "Bevestiging Nieuwe Uitlening",
                    htmlMessage);
        } catch (Exception e) {
            log.warn(
                    "Could not send confirmation message for loans to user {}: {}",
                    userId,
                    e.getMessage());
        }
    }

    private String buildCombinedLoanMessage(List<Loan> loans, List<BookDTO> recommendations) {
        HtmlMessageBuilder builder = new HtmlMessageBuilder(
                loans.size() == 1
                        ? "Bevestiging nieuwe uitlening"
                        : "Bevestiging nieuwe uitleningen",
                frontendUrl);

        for (Loan loan : loans) {
            builder
                    .addComponent(new LoanGeneralInfoComponent(loan, frontendUrl))
                    .addComponent(new LoanBookListComponent(loan, frontendUrl));
        }

        builder.addComponent(new BookRecommendationComponent(recommendations, frontendUrl));

        return builder.build();
    }
}
