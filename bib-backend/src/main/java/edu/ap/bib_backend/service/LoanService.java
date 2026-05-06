package edu.ap.bib_backend.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.LoanDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.book.BookService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

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
    private final String frontendUrl;

    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper, BookService bookService,
            ExternalUserDisplayService externalUserDisplayService, BookCopyRepository bookCopyRepository,
            UserRepository userRepository, SmartschoolMessageService smartschoolMessageService,
            SmartschoolTokenRefreshService smartschoolTokenRefreshService,
            @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL:http://localhost:4200}") String frontendUrl) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.bookService = bookService;
        this.externalUserDisplayService = externalUserDisplayService;
        this.bookCopyRepository = bookCopyRepository;
        this.userRepository = userRepository;
        this.smartschoolMessageService = smartschoolMessageService;
        this.smartschoolTokenRefreshService = smartschoolTokenRefreshService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public LoanDTO create(LoanDTO loanDTO) {
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
        loan.setReturnDate(parsedReturnDate);

        // saving to ensure we have an ID for the loan before associating book copies
        loan = loanRepository.save(loan);

        for (var bookId : loanDTO.bookIds()) {
            Book book = bookService.getBookById(bookId, true);
            int copiesNeeded = resolveCopiesCount(loanDTO, bookId);

            List<BookCopy> availableCopies = bookCopyRepository.findAvailableCopiesByBookId(bookId);
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

    private String buildHtmlMessageBody(Loan loan) {
        StringBuilder sb = new StringBuilder();
        // Base container
        sb.append(
                "<div style=\"font-family: Calibri, Arial, sans-serif; max-width: 600px; margin: 0 auto; color: #1a0a10; border: 1px solid #f0e4e8; border-radius: 8px; overflow: hidden;\">");

        // Header
        sb.append("<div style=\"background-color: #8a1e41; color: white; padding: 20px; text-align: center;\">");
        sb.append("<h2 style=\"margin: 0;\">Bevestiging van je uitlening</h2>");
        sb.append("</div>");

        // Body container
        sb.append("<div style=\"padding: 20px; background-color: #ffffff;\">");
        sb.append("<p>Beste lener,</p>");
        sb.append("<p>Je hebt met succes een nieuwe uitlening geregistreerd.</p>");

        // Details box
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

        // Action button for History
        sb.append("<div style=\"text-align: center; margin: 30px 0;\">");
        sb.append("<a href=\"").append(frontendUrl).append(
                "/history\" style=\"background-color: #8a1e41; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block;\">Bekijk mijn uitleenhistoriek</a>");
        sb.append("</div>");

        // Footer
        sb.append("<hr style=\"border: none; border-top: 1px solid #f0e4e8; margin: 30px 0 15px 0;\">");
        sb.append(
                "<p style=\"margin: 0; font-size: 12px; color: #7a5560; text-align: center;\">Dit is een automatisch gegenereerd bericht. Gelieve niet te reageren.</p>");

        sb.append("</div>"); // End body container
        sb.append("</div>"); // End base container

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

    public List<LoanDTO> findAll() {
        List<Loan> loans = loanRepository.findAll();

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

        System.out.println("UserIds per school: " + userIdsBySchoolCode);

        Map<String, String> displayNamesByUserId = new java.util.HashMap<>();

        userIdsBySchoolCode.forEach((schoolCode, userIds) -> {
            System.out.println("SchoolCode: " + schoolCode);
            System.out.println("UserIds naar OneRoster: " + userIds);

            Map<String, String> names = externalUserDisplayService.getDisplayNames(schoolCode, userIds);

            System.out.println("Display names van OneRoster: " + names);

            displayNamesByUserId.putAll(names);
        });

        System.out.println("Alle display names: " + displayNamesByUserId);

        return loans.stream()
                .map(loan -> {
                    LoanDTO dto = loanMapper.toDto(loan);

                    String lookupId = loan.getUser().getOneRosterSourcedId();

                    return LoanDTO.builder()
                            .id(dto.id())
                            .userId(dto.userId())
                            .userDisplayName(
                                    displayNamesByUserId.getOrDefault(lookupId, "Onbekende gebruiker"))
                            .bookIds(dto.bookIds())
                            .books(dto.books())
                            .loanDate(dto.loanDate())
                            .returnDate(dto.returnDate())
                            .build();
                })
                .toList();
    }
}
