package edu.ap.bib_backend.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import edu.ap.bib_backend.service.user.CurrentUserService;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanHistoryDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.enums.LoanStatus;
import edu.ap.bib_backend.jpa.repository.loan.LoanRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.service.book.BookService;
import jakarta.transaction.Transactional;


@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final BookService bookService;
    private final ExternalUserDisplayService externalUserDisplayService;
    private final CurrentUserService currentUserService;

    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper, BookService bookService, ExternalUserDisplayService externalUserDisplayService, CurrentUserService currentUserService) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.bookService = bookService;
        this.externalUserDisplayService = externalUserDisplayService;
        this.currentUserService = currentUserService;
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
                                Collectors.toSet()
                        )
                ));

        System.out.println("UserIds per school: " + userIdsBySchoolCode);

        Map<String, String> displayNamesByUserId = new java.util.HashMap<>();

        userIdsBySchoolCode.forEach((schoolCode, userIds) -> {
            System.out.println("SchoolCode: " + schoolCode);
            System.out.println("UserIds naar OneRoster: " + userIds);

            Map<String, String> names =
                    externalUserDisplayService.getDisplayNames(schoolCode, userIds);

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
                            .status(dto.status())
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

    public LoanHistoryDTO findHistoryForCurrentUser(Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        List<LoanDTO> loans = loanRepository.findByUserIdOrderByLoanDateDesc(currentUserId)
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