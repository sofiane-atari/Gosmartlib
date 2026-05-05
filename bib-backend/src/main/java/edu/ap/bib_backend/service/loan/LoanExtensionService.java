package edu.ap.bib_backend.service.loan;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loanextension.LoanExtensionDTO;
import edu.ap.bib_backend.dto.loanextension.LoanExtensionRequestDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.loan.Loan;
import edu.ap.bib_backend.jpa.entity.loan.LoanExtension;
import edu.ap.bib_backend.jpa.repository.loan.LoanExtensionRepository;
import edu.ap.bib_backend.jpa.repository.loan.LoanRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;

@Service
public class LoanExtensionService {

    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Europe/Brussels");
    private static final int MAX_EXTENSION_COUNT = 2;

    private final LoanRepository loanRepository;
    private final LoanExtensionRepository loanExtensionRepository;
    private final CurrentUserService currentUserService;
    private final LoanMapper loanMapper;

    public LoanExtensionService(
        LoanRepository loanRepository,
        LoanExtensionRepository loanExtensionRepository,
        CurrentUserService currentUserService,
        LoanMapper loanMapper
    ) {
        this.loanRepository = loanRepository;
        this.loanExtensionRepository = loanExtensionRepository;
        this.currentUserService = currentUserService;
        this.loanMapper = loanMapper;
    }

    @Transactional
    public LoanDTO extendLoan(
            Long loanId,
            LoanExtensionRequestDTO request,
            Authentication authentication
    ) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "loanId",
                        "Lening niet gevonden met id: " + loanId
                ));

        validateLoanCanBeExtended(loan);

        String currentUserId = currentUserService.extractCurrentUserId(authentication);
        LocalDate oldReturnDate = loan.getReturnDate();
        LocalDate newReturnDate = calculateNewReturnDate(oldReturnDate, request);

        validateNewReturnDate(oldReturnDate, newReturnDate);

        LoanExtension extension = new LoanExtension();
        extension.setLoan(loan);
        extension.setOldReturnDate(oldReturnDate);
        extension.setNewReturnDate(newReturnDate);
        extension.setExtensionType(request.extensionType());
        extension.setExtendedByUserId(currentUserId);

        loan.setReturnDate(newReturnDate);

        loanExtensionRepository.save(extension);
        Loan savedLoan = loanRepository.save(loan);

        return loanMapper.toDto(savedLoan);
    }

    private LocalDate calculateNewReturnDate(
            LocalDate currentReturnDate,
            LoanExtensionRequestDTO request
    ) {
        return switch (request.extensionType()) {
            case ONE_DAY -> currentReturnDate.plusDays(1);
            case ONE_WEEK -> currentReturnDate.plusWeeks(1);
            case TWO_WEEKS -> currentReturnDate.plusWeeks(2);
            case THREE_WEEKS -> currentReturnDate.plusWeeks(3);
            case CUSTOM_DATE -> parseCustomReturnDate(request.customReturnDate());
        };
    }

    private LocalDate parseCustomReturnDate(String customReturnDate) {
        if (customReturnDate == null || customReturnDate.isBlank()) {
            throw new IllegalArgumentException("Nieuwe retourdatum is verplicht bij een aangepaste datum.");
        }

        try {
            return LocalDate.parse(customReturnDate);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Nieuwe retourdatum heeft een ongeldig formaat.");
        }
    }

    private void validateNewReturnDate(LocalDate oldReturnDate, LocalDate newReturnDate) {
        LocalDate today = LocalDate.now(APPLICATION_ZONE);

        if (!newReturnDate.isAfter(today)) {
            throw new IllegalArgumentException("Nieuwe retourdatum moet in de toekomst liggen.");
        }

        if (!newReturnDate.isAfter(oldReturnDate)) {
            throw new IllegalArgumentException("Nieuwe retourdatum moet na de huidige retourdatum liggen.");
        }
    }

    private void validateLoanCanBeExtended(Loan loan) {
        if (loan.getReturnedDate() != null) {
            throw new IllegalStateException("Een teruggebrachte lening kan niet verlengd worden.");
        }

        if (loan.getReturnDate() == null) {
            throw new IllegalStateException("Deze lening heeft geen retourdatum en kan niet verlengd worden.");
        }

        int extensionCount = loanExtensionRepository.countByLoanId(loan.getId());

        if (extensionCount >= MAX_EXTENSION_COUNT) {
            throw new IllegalStateException("Deze lening werd al maximaal twee keer verlengd.");
        }
    }
}