package edu.ap.bib_backend.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanExtensionRequestDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.LoanExtension;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.LoanExtensionRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    /**
     * Extends a loan after validating ownership, state, and the requested return date.
     *
     * @param loanId loan identifier
     * @param request extension request
     * @param authentication current authenticated user
     * @return updated loan as a DTO
     * @throws ResourceNotFoundException if the loan does not exist
     * @throws AccessDeniedException if the loan belongs to another school
     * @throws IllegalStateException if the loan cannot be extended
     * @throws IllegalArgumentException if the new return date is invalid
     */
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

        verifyLoanBelongsToSchool(loan, authentication);

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

        log.info("loan {} extended by {}: {} → {} (type={})",
                loanId, currentUserId, oldReturnDate, newReturnDate, request.extensionType());

        loanExtensionRepository.save(extension);
        Loan savedLoan = loanRepository.save(loan);

        return loanMapper.toDto(savedLoan);
    }

    /**
     * Calculates the new return date for the requested extension type.
     *
     * @param currentReturnDate current return date
     * @param request extension request
     * @return calculated return date
     */
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

    /**
     * Parses a custom return date from the request.
     *
     * @param customReturnDate custom date string in ISO local-date format
     * @return parsed return date
     * @throws IllegalArgumentException if the date is blank or has an invalid format
     */
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

    /**
     * Verifies that the loan belongs to the current user's school.
     *
     * @param loan loan to verify
     * @param authentication current authenticated user
     * @throws AccessDeniedException if the loan is outside the current user's school
     */
    private void verifyLoanBelongsToSchool(Loan loan, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);

        if (loan.getCampus() == null || loan.getCampus().getSchool() == null
                || !loan.getCampus().getSchool().getId().equals(school.getId())) {
            log.warn("loan {} extension rejected: belongs to different school", loan.getId());
            throw new AccessDeniedException("Lening behoort tot een andere school.");
        }
    }

    /**
     * Validates that a new return date is in the future and after the current return date.
     *
     * @param oldReturnDate current return date
     * @param newReturnDate proposed return date
     * @throws IllegalArgumentException if the proposed date is not allowed
     */
    private void validateNewReturnDate(LocalDate oldReturnDate, LocalDate newReturnDate) {
        LocalDate today = LocalDate.now(APPLICATION_ZONE);

        if (!newReturnDate.isAfter(today)) {
            log.warn("loan extension rejected: new date {} is not in the future", newReturnDate);
            throw new IllegalArgumentException("Nieuwe retourdatum moet in de toekomst liggen.");
        }

        if (!newReturnDate.isAfter(oldReturnDate)) {
            log.warn("loan extension rejected: new date {} is not after current date {}", newReturnDate, oldReturnDate);
            throw new IllegalArgumentException("Nieuwe retourdatum moet na de huidige retourdatum liggen.");
        }
    }

    /**
     * Validates that a loan is eligible for extension.
     *
     * @param loan loan to validate
     * @throws IllegalStateException if the loan is returned, has no return date, or reached the extension limit
     */
    private void validateLoanCanBeExtended(Loan loan) {
        if (loan.getReturnedDate() != null) {
            log.warn("loan {} extension rejected: already returned", loan.getId());
            throw new IllegalStateException("Een teruggebrachte lening kan niet verlengd worden.");
        }

        if (loan.getReturnDate() == null) {
            log.warn("loan {} extension rejected: no return date set", loan.getId());
            throw new IllegalStateException("Deze lening heeft geen retourdatum en kan niet verlengd worden.");
        }

        int extensionCount = loanExtensionRepository.countByLoanId(loan.getId());

        if (extensionCount >= MAX_EXTENSION_COUNT) {
            log.warn("loan {} extension rejected: max {} extensions reached", loan.getId(), MAX_EXTENSION_COUNT);
            throw new IllegalStateException("Deze lening werd al maximaal twee keer verlengd.");
        }
    }
}