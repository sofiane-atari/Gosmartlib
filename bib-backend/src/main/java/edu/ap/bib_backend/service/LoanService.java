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
import edu.ap.bib_backend.service.book.BookService;
import jakarta.transaction.Transactional;


@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final BookService bookService;
    private final ExternalUserDisplayService externalUserDisplayService;
    private final BookCopyRepository bookCopyRepository;

    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper, BookService bookService, ExternalUserDisplayService externalUserDisplayService, BookCopyRepository bookCopyRepository) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.bookService = bookService;
        this.externalUserDisplayService = externalUserDisplayService;
        this.bookCopyRepository = bookCopyRepository;
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

        loan.setLoanDate(today);
        loan = loanRepository.save(loan);
        return loanMapper.toDto(loan);
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
                            .userDisplayName(
                                    displayNamesByUserId.getOrDefault(lookupId, "Onbekende gebruiker")
                            )
                            .bookIds(dto.bookIds())
                            .books(dto.books())
                            .loanDate(dto.loanDate())
                            .returnDate(dto.returnDate())
                            .build();
                })
                .toList();
    }
}
