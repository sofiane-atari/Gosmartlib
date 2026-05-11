package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanExtensionDTO;
import edu.ap.bib_backend.dto.loan.LoanExtensionRequestDTO;
import edu.ap.bib_backend.dto.loan.LoanHistoryDTO;
import edu.ap.bib_backend.dto.loan.LoanStatsDTO;
import edu.ap.bib_backend.service.LoanExtensionService;
import edu.ap.bib_backend.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanExtensionService loanExtensionService;

    public LoanController(LoanService loanService, LoanExtensionService loanExtensionService) {
        this.loanService = loanService;
        this.loanExtensionService = loanExtensionService;
    }

    @PostMapping
    public ResponseEntity<LoanDTO> createLoan(
            @Valid @RequestBody LoanDTO loanDTO,
            Authentication authentication) {
        return ResponseEntity.ok(loanService.create(loanDTO, authentication));
    }

    @GetMapping
    public ResponseEntity<PageDTO<LoanDTO>> getAllLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            Authentication authentication) {
        return ResponseEntity.ok(loanService.findAll(page, pageSize, authentication));
    }

    @PatchMapping("/{loanId}/return")
    public ResponseEntity<LoanDTO> returnLoan(
            @PathVariable("loanId") Long loanId,
            Authentication authentication) {
        return ResponseEntity.ok(loanService.returnLoan(loanId, authentication));
    }

    @GetMapping("/active")
    public ResponseEntity<PageDTO<LoanDTO>> getActiveLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            Authentication authentication) {
        return ResponseEntity.ok(loanService.findActiveLoans(page, pageSize, authentication));
    }

    @GetMapping("/me/history")
    public ResponseEntity<LoanHistoryDTO> getMyLoanHistory(Authentication authentication) {
        return ResponseEntity.ok(loanService.findHistoryForCurrentUser(authentication));
    }

    @PatchMapping("/{loanId}/extend")
    public ResponseEntity<LoanDTO> extendLoan(
            @PathVariable("loanId") Long loanId,
            @Valid @RequestBody LoanExtensionRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(loanExtensionService.extendLoan(loanId, request, authentication));
    }

    @GetMapping("/stats")
    public ResponseEntity<LoanStatsDTO> getLoanStats() {
        return ResponseEntity.ok(loanService.getLoanStats());
    }
}