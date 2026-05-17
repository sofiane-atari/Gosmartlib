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
import edu.ap.bib_backend.dto.BookCopyConditionUpdateDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanHistoryDTO;
import edu.ap.bib_backend.dto.loan.LoanReturnDTO;
import edu.ap.bib_backend.dto.loan.LoanStatsDTO;
import edu.ap.bib_backend.event.BookReturnedGamificationEvent;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.enums.BookCondition;
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
    public LoanDTO create(LoanDTO loanDTO, Authentication authentication) {
        School currentUserSchool = currentUserService.getCurrentUserSchool(authentication);

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        LocalDate parsedReturnDate;

        if (loanDTO.returnDate() != null && !loanDTO.returnDate().isBlank()) {
            try {
                parsedReturnDate = LocalDate.parse(loanDTO.returnDate());
            } catch (DateTimeParseException e) {
                throw new IllegalStateException("Retourdatum heeft een ongeldig formaat.");
            }

            if (!parsedReturnDate.isAfter(today)) {
                throw new IllegalStateException("Retourdatum moet minimaal één dag in de toekomst liggen.");
            }
        } else {
            parsedReturnDate = calculateDefaultReturnDate(loanDTO.userId());
        }

        List<BookCopy> selectedCopies = resolveSelectedCopies(loanDTO.bookCopyIds(), currentUserSchool);

        Loan loan = loanMapper.toEntity(loanDTO);
        loan.setCampus(selectedCopies.get(0).getCampus());
        loan.setLoanDate(today);
        loan.setReturnDate(parsedReturnDate);

        loan = loanRepository.save(loan);
        loan.getCopies().addAll(selectedCopies);

        List<Long> selectedBookIds = selectedCopies.stream()
                .map(BookCopy::getBook)
                .filter(java.util.Objects::nonNull)
                .map(Book::getId)
                .distinct()
                .toList();

        try {
            String token = smartschoolTokenRefreshService.getValidSmartschoolAccessToken(loanDTO.userId());

            if (token != null && !token.isBlank()) {
                log.info("Sending Smartschool message for new loan with user ID: {}", loanDTO.userId());

                String schoolDomain = userRepository.findById(loanDTO.userId())
                        .map(u -> u.getSchool().getDomainName())
                        .orElse(null);

                log.debug("Fetching recommendations for bookIds={}, schoolDomain={}",
                        selectedBookIds, schoolDomain);
                List<BookDTO> recommendations = bookService.getRelatedBooksForLoan(
                        selectedBookIds, 4, schoolDomain);

                // Remove recommendations that are already in the loan

                recommendations = recommendations.stream()
                        .filter(rec -> selectedBookIds.stream()
                                .noneMatch(loanBookId -> loanBookId.equals(rec.getId())))
                        .toList();

                log.debug("Recommendations fetched: {} books", recommendations.size());

                String htmlMessage = new HtmlMessageBuilder(
                        "Bevestiging nieuwe uitlening", frontendUrl)
                        .addComponent(new LoanGeneralInfoComponent(loan, frontendUrl))
                        .addComponent(new LoanBookListComponent(loan, frontendUrl))
                        .addComponent(new BookRecommendationComponent(recommendations, frontendUrl))
                        .build();

                smartschoolMessageService.sendMessage(token, "Bevestiging Nieuwe Uitlening", htmlMessage);
            }
        } catch (Exception e) {
            log.warn("Could not send confirmation message for loan to user {}: {}", loanDTO.userId(), e.getMessage());
        }

        return loanMapper.toDto(loan);
    }

    public PageDTO<LoanDTO> findAll(int page, int pageSize, Authentication authentication) {
        return findAll(page, pageSize, null, authentication);
    }

    public PageDTO<LoanDTO> findAll(int page, int pageSize, Long campusId, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);

        List<Loan> loans = campusId != null
                ? loanRepository.findByCampusId(campusId).stream()
                        .filter(loan -> loan.getCampus() != null)
                        .filter(loan -> loan.getCampus().getSchool() != null)
                        .filter(loan -> school.getId().equals(loan.getCampus().getSchool().getId()))
                        .toList()
                : loanRepository.findByCampusSchoolId(school.getId());

        return toPagedLoanDtoResponse(loans, page, pageSize);
    }

    public PageDTO<LoanDTO> findActiveLoans(int page, int pageSize, Authentication authentication) {
        return findActiveLoans(page, pageSize, null, authentication);
    }

    public PageDTO<LoanDTO> findActiveLoans(int page, int pageSize, Long campusId, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);

        List<Loan> loans = campusId != null
                ? loanRepository.findByCampusIdAndReturnedDateIsNull(campusId).stream()
                        .filter(loan -> loan.getCampus() != null)
                        .filter(loan -> loan.getCampus().getSchool() != null)
                        .filter(loan -> school.getId().equals(loan.getCampus().getSchool().getId()))
                        .toList()
                : loanRepository.findByCampusSchoolIdAndReturnedDateIsNull(school.getId());

        return toPagedLoanDtoResponse(loans, page, pageSize);
    }

    @Transactional
    public LoanDTO returnLoan(Long loanId, Authentication authentication) {
        return returnLoan(loanId, null, authentication);
    }

    @Transactional
    public LoanDTO returnLoan(Long loanId, LoanReturnDTO request, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalStateException("Lening niet gevonden."));

        if (loan.getCampus() == null || loan.getCampus().getSchool() == null
                || !loan.getCampus().getSchool().getId().equals(school.getId())) {
            throw new IllegalStateException("Lening niet gevonden in deze school.");
        }

        if (loan.getReturnedDate() != null) {
            throw new IllegalStateException("Deze lening is al teruggebracht.");
        }

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        applyReturnedCopyConditions(loan, request);
        loan.setReturnedDate(today);

        Loan savedLoan = loanRepository.save(loan);

        publishBookReturnedEvent(savedLoan, today);

        return loanMapper.toDto(savedLoan);
    }

    public LoanHistoryDTO findHistoryForCurrentUser(Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        List<LoanDTO> loans = loanRepository
                .findByUserIdOrderByLoanDateDesc(currentUserId)
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
                .mapToInt(loan -> loan.books() != null ? loan.books().size() : loan.bookCopyIds().size())
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

    private List<BookCopy> resolveSelectedCopies(List<Long> bookCopyIds, School currentUserSchool) {
        List<BookCopy> selectedCopies = bookCopyRepository.findAllById(bookCopyIds);

        if (selectedCopies.size() != bookCopyIds.size()) {
            throw new IllegalStateException("Eén of meer boekexemplaren werden niet gevonden.");
        }

        Long selectedCampusId = null;

        for (BookCopy copy : selectedCopies) {
            if (copy.getCampus() == null || copy.getCampus().getSchool() == null
                    || !copy.getCampus().getSchool().getId().equals(currentUserSchool.getId())) {
                throw new IllegalStateException("Boekexemplaar behoort tot een andere school.");
            }

            if (selectedCampusId == null) {
                selectedCampusId = copy.getCampus().getId();
            } else if (!selectedCampusId.equals(copy.getCampus().getId())) {
                throw new IllegalStateException("Alle boekexemplaren moeten uit dezelfde campus komen.");
            }

            if (copy.getCondition() == BookCondition.LOST) {
                String title = copy.getBook() != null ? copy.getBook().getTitle() : "onbekend boek";
                throw new IllegalStateException("Boekexemplaar is verloren en kan niet worden uitgeleend voor boek: " + title);
            }

            if (copy.getAvailability() != Availability.AVAILABLE) {
                String title = copy.getBook() != null ? copy.getBook().getTitle() : "onbekend boek";
                throw new IllegalStateException("Boekexemplaar is niet beschikbaar voor boek: " + title);
            }
        }

        return selectedCopies;
    }

    private void applyReturnedCopyConditions(Loan loan, LoanReturnDTO request) {
        if (request == null || request.copyConditions() == null || request.copyConditions().isEmpty()) {
            return;
        }

        Map<Long, BookCopy> loanCopiesById = loan.getCopies().stream()
                .filter(copy -> copy.getId() != null)
                .collect(Collectors.toMap(BookCopy::getId, copy -> copy));

        for (BookCopyConditionUpdateDTO update : request.copyConditions()) {
            if (update.bookCopyId() == null) {
                throw new IllegalStateException("Boekexemplaar ID is verplicht.");
            }

            if (update.condition() == null) {
                throw new IllegalStateException("Boekstaat is verplicht.");
            }

            BookCopy copy = loanCopiesById.get(update.bookCopyId());
            if (copy == null) {
                throw new IllegalStateException("Boekexemplaar hoort niet bij deze lening.");
            }

            copy.setCondition(update.condition());
            bookCopyRepository.save(copy);
        }
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
                            .bookCopyIds(dto.bookCopyIds())
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
}
