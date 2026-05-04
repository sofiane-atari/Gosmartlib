package edu.ap.bib_backend.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import edu.ap.bib_backend.service.smartschool.SmartschoolNotificationService;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.LoanDTO;
import edu.ap.bib_backend.jpa.entity.AuthSession;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.repository.AuthSessionRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.service.book.BookService;
import jakarta.transaction.Transactional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final BookService bookService;
    private final ExternalUserDisplayService externalUserDisplayService;
    private final SmartschoolNotificationService smartschoolNotificationService;
    private final AuthSessionRepository authSessionRepository;

    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper, BookService bookService,
            ExternalUserDisplayService externalUserDisplayService,
            SmartschoolNotificationService smartschoolNotificationService,
            AuthSessionRepository authSessionRepository) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.bookService = bookService;
        this.externalUserDisplayService = externalUserDisplayService;
        this.smartschoolNotificationService = smartschoolNotificationService;
        this.authSessionRepository = authSessionRepository;
    }

    @Transactional
    public LoanDTO create(LoanDTO loanDTO) {
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
        loan.setReturnDate(parsedReturnDate);

        // saving to ensure we have an ID for the loan before associating book copies
        loan = loanRepository.save(loan);

        for (var id : loanDTO.bookIds()) {

            Book book = bookService.getBookById(id, true);
            BookCopy copy = book.getBookCopies().getFirst();
            loan.getCopies().add(copy);
        }

        loan.setLoanDate(today);
        loan = loanRepository.save(loan);

        sendSmartschoolLoanNotification(loan);

        return loanMapper.toDto(loan);
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
                            .userDisplayName(displayNamesByUserId.getOrDefault(lookupId, "Onbekende gebruiker"))
                            .bookIds(dto.bookIds())
                            .books(dto.books())
                            .loanDate(dto.loanDate())
                            .returnDate(dto.returnDate())
                            .returnedDate(dto.returnedDate())
                            .build();
                })
                .toList();
    }

    @Transactional
    public LoanDTO returnLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalStateException("Lening niet gevonden."));

        if (loan.getReturnedDate() != null) {
            throw new IllegalStateException("Deze lening is al teruggebracht.");
        }

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        loan.setReturnedDate(today);

        return loanMapper.toDto(loanRepository.save(loan));
    }

    private void sendSmartschoolLoanNotification(Loan loan) {
        try {
            System.out.println("START Smartschool notification voor loanId: " + loan.getId());

            if (loan.getUser() == null) {
                System.err.println("STOP: loan.getUser() is null");
                return;
            }

            String userId = loan.getUser().getId();
            System.out.println("UserId voor notification: " + userId);

            AuthSession authSession = authSessionRepository.findById(userId)
                    .orElse(null);

            if (authSession == null) {
                System.err.println("STOP: Geen AuthSession gevonden voor userId: " + userId);
                return;
            }

            System.out.println("AuthSession gevonden voor userId: " + userId);

            String accessToken = authSession.getSmartschoolAccessToken();

            if (accessToken == null || accessToken.isBlank()) {
                System.err.println("STOP: Geen Smartschool access token gevonden voor userId: " + userId);
                return;
            }

            System.out.println("Smartschool access token gevonden");

            String smartschoolBaseUrl = loan.getCopies().stream()
                    .findFirst()
                    .map(copy -> copy.getSchool())
                    .map(school -> school.getDomainName())
                    .orElse(null);

            System.out.println("Smartschool base URL: " + smartschoolBaseUrl);

            if (smartschoolBaseUrl == null || smartschoolBaseUrl.isBlank()) {
                System.err.println("STOP: Geen Smartschool domainName gevonden via book copy school.");
                return;
            }

            String bookTitles = loan.getCopies().stream()
                    .map(copy -> copy.getBook().getTitle())
                    .collect(Collectors.joining(", "));

            String title = "Boek uitgeleend";

            String description = "Je hebt het volgende boek uitgeleend: "
                    + bookTitles
                    + ". Terugbrengen voor "
                    + loan.getReturnDate()
                    + ".";

            String notificationUrl = "https://jouw-frontend-url.be/loans/" + loan.getId();

            System.out.println("Smartschool notification wordt verstuurd...");

            smartschoolNotificationService.sendNotification(
                    smartschoolBaseUrl,
                    accessToken,
                    title,
                    description,
                    notificationUrl);

            System.out.println("Smartschool notificatie verstuurd voor lening " + loan.getId());

        } catch (Exception e) {
            System.err.println("Smartschool notificatie kon niet verstuurd worden: " + e.getMessage());
            e.printStackTrace();
        }
    }
}