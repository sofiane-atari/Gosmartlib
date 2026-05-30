package edu.ap.bib_backend.seed.dev;

import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.LoanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LoanDevSeeder {

    private final LoanService loanService;
    private final UserRepository userRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;

    public LoanDevSeeder(LoanService loanService, UserRepository userRepository,
                          BookCopyRepository bookCopyRepository, LoanRepository loanRepository) {
        this.loanService = loanService;
        this.userRepository = userRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRepository = loanRepository;
    }

    public LoanSeedResult seed(Authentication authentication, Long schoolId, Long campusId,
                                List<Long> bookIds, List<String> studentIds) {
        if (studentIds.isEmpty()) {
            log.info("Geen studenten gevonden, lening seeding overgeslagen");
            return new LoanSeedResult(0, 0, List.of());
        }

        if (bookIds.isEmpty()) {
            log.info("Geen boeken gevonden, lening seeding overgeslagen");
            return new LoanSeedResult(0, 0, List.of());
        }

        List<Long> availableCopyIds = new ArrayList<>();
        for (Long bookId : bookIds) {
            List<Long> copyIds = bookCopyRepository
                    .findAvailableCopiesByBookIdAndCampusId(bookId, campusId)
                    .stream()
                    .map(BookCopy::getId)
                    .toList();
            availableCopyIds.addAll(copyIds);
        }

        if (availableCopyIds.isEmpty()) {
            log.info("Geen beschikbare exemplaren gevonden, lening seeding overgeslagen");
            return new LoanSeedResult(0, 0, List.of());
        }

        int loansCreated = 0;
        int loansReturned = 0;
        List<Long> createdLoanIds = new ArrayList<>();
        int copyIndex = 0;
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));

        int activeStudentCount = Math.min(7, studentIds.size());
        for (int i = 0; i < activeStudentCount && copyIndex < availableCopyIds.size(); i++) {
            String studentId = studentIds.get(i);
            Authentication studentAuth = DevSeedAuthentication.forUser(studentId, Role.STUDENT);

            int copyCount = Math.min(1, availableCopyIds.size() - copyIndex);
            List<Long> loanCopies = availableCopyIds.subList(copyIndex, copyIndex + copyCount)
                    .stream().toList();

            if (loanCopies.isEmpty()) break;

            String returnDate = today.plusDays(14 + i * 2).format(DateTimeFormatter.ISO_LOCAL_DATE);

            try {
                LoanDTO loanDTO = new LoanDTO(studentId, new ArrayList<>(loanCopies), returnDate);
                List<LoanDTO> created = loanService.create(loanDTO, studentAuth);
                loansCreated += created.size();
                created.stream().map(LoanDTO::id).forEach(createdLoanIds::add);
                copyIndex += copyCount;
                log.info("Actieve lening aangemaakt voor student {}", studentId);
            } catch (Exception e) {
                log.warn("Kon actieve lening niet aanmaken voor student {}: {}", studentId, e.getMessage());
            }
        }

        int returnStudentCount = Math.min(4, studentIds.size());
        for (int i = 0; i < returnStudentCount && copyIndex < availableCopyIds.size(); i++) {
            String studentId = studentIds.get(activeStudentCount + i < studentIds.size()
                    ? activeStudentCount + i : i);
            Authentication studentAuth = DevSeedAuthentication.forUser(studentId, Role.STUDENT);

            int copyCount = Math.min(1, availableCopyIds.size() - copyIndex);
            List<Long> loanCopies = availableCopyIds.subList(copyIndex, copyIndex + copyCount)
                    .stream().toList();

            if (loanCopies.isEmpty()) break;

            String returnDate = today.plusDays(14).format(DateTimeFormatter.ISO_LOCAL_DATE);

            try {
                LoanDTO loanDTO = new LoanDTO(studentId, new ArrayList<>(loanCopies), returnDate);
                List<LoanDTO> createdLoans = loanService.create(loanDTO, studentAuth);
                loansCreated += createdLoans.size();
                createdLoans.stream().map(LoanDTO::id).forEach(createdLoanIds::add);

                for (LoanDTO createdLoan : createdLoans) {
                    loanService.returnLoan(createdLoan.id(), studentAuth);
                    loansReturned++;
                }
                copyIndex += copyCount;
                log.info("Geretourneerde lening aangemaakt voor student {}", studentId);
            } catch (Exception e) {
                log.warn("Kon geretourneerde lening niet aanmaken voor student {}: {}", studentId, e.getMessage());
            }
        }

        log.info("Lening seeding voltooid: {} leningen aangemaakt, {} geretourneerd", loansCreated, loansReturned);
        return new LoanSeedResult(loansCreated, loansReturned, createdLoanIds);
    }

    public record LoanSeedResult(int loansCreated, int loansReturned, List<Long> loanIds) {}
}
