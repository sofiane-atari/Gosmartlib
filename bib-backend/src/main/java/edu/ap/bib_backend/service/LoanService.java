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
import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.enums.LoanStatus;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.book.BookService;

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
        Campus campus = currentUserService.getCurrentUserCampus(authentication);

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

        Loan loan = loanMapper.toEntity(loanDTO);
        loan.setCampus(campus);
        loan.setReturnDate(parsedReturnDate);

        loan = loanRepository.save(loan);

        for (var bookId : loanDTO.bookIds()) {
            Book book = bookService.getBookById(bookId, true);
            int copiesNeeded = resolveCopiesCount(loanDTO, bookId);

            List<BookCopy> availableCopies = bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(bookId, campus.getId());
            if (availableCopies.size() < copiesNeeded) {
                throw new IllegalStateException("Niet genoeg beschikbare exemplaren voor boek: " + book.getTitle());
            }

            for (int i = 0; i < copiesNeeded; i++) {
                loan.getCopies().add(availableCopies.get(i));
            }
        }

        try {
            String token = smartschoolTokenRefreshService.getValidSmartschoolAccessToken(loanDTO.userId());

            if (token != null && !token.isBlank()) {
                log.info("Sending Smartschool message for new loan with user ID: {}", loanDTO.userId());
                String htmlMessage = buildHtmlMessageBody(loan);
                smartschoolMessageService.sendMessage(token, "Bevestiging Nieuwe Uitlening", htmlMessage);
            }
        } catch (Exception e) {
            log.warn("Could not send confirmation message for loan to user {}: {}", loanDTO.userId(), e.getMessage());
        }

        return loanMapper.toDto(loan);
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

    private String buildHtmlMessageBody(Loan loan) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<div style=\"font-family: Calibri, Arial, sans-serif; max-width: 600px; margin: 0 auto; color: #1a0a10; border: 1px solid #f0e4e8; border-radius: 8px; overflow: hidden;\">");
        sb.append("<div style=\"background-color: #8a1e41; color: white; padding: 20px; text-align: center;\">");
        sb.append("<h2 style=\"margin: 0;\">Bevestiging van je uitlening</h2>");
        sb.append("</div>");
        sb.append("<div style=\"padding: 20px; background-color: #ffffff;\">");
        sb.append("<p>Beste lener,</p>");
        sb.append("<p>Je hebt met succes een nieuwe uitlening geregistreerd.</p>");
        sb.append(
                "<div style=\"background-color: #fff5f8; padding: 15px; border-left: 4px solid #8a1e41; margin: 20px 0; border-radius: 0 4px 4px 0;\">");
        sb.append("<p style=\"margin: 0 0 10px 0;\"><strong>Uitlening ID:</strong> ").append(loan.getId())
                .append("</p>");

        if (loan.getReturnDate() != null) {
            sb.append("<p style=\"margin: 0 0 10px 0;\"><strong>Verwachte retourdatum:</strong> ")
                    .append(loan.getReturnDate().toString()).append("</p>");
        }

        sb.append("<p style=\"margin: 0 0 5px 0;\"><strong>Geleende boeken:</strong></p>");
        sb.append("<ul style=\"margin: 0; padding-left: 20px;\">");
        if (loan.getCopies() != null && !loan.getCopies().isEmpty()) {
            for (BookCopy copy : loan.getCopies()) {
                if (copy != null && copy.getBook() != null) {
                    Book book = copy.getBook();
                    sb.append("<li style=\"margin-bottom: 5px;\">");
                    sb.append("<a href=\"").append(frontendUrl).append("/book/").append(book.getId())
                            .append("\" style=\"color: #8a1e41; text-decoration: none; font-weight: bold;\">");
                    sb.append(book.getTitle());
                    sb.append("</a>");
                    if (book.getAuthor() != null && !book.getAuthor().isBlank()) {
                        sb.append(" <span style=\"color: #7a5560;\">(").append(book.getAuthor()).append(")</span>");
                    }
                    sb.append("</li>");
                }
            }
        }
        sb.append("</ul>");
        sb.append("</div>");
        sb.append("<p>Alvast veel leesplezier!</p>");
        sb.append("<div style=\"text-align: center; margin: 30px 0;\">");
        sb.append("<a href=\"").append(frontendUrl).append(
                "/history\" style=\"background-color: #8a1e41; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block;\">Bekijk mijn uitleenhistoriek</a>");
        sb.append("</div>");
        sb.append("<hr style=\"border: none; border-top: 1px solid #f0e4e8; margin: 30px 0 15px 0;\">");
        sb.append(
                "<p style=\"margin: 0; font-size: 12px; color: #7a5560; text-align: center;\">Dit is een automatisch gegenereerd bericht. Gelieve niet te reageren.</p>");
        sb.append("</div>");
        sb.append("</div>");

        return sb.toString();
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
