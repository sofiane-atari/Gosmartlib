package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanHistoryDTO;
import edu.ap.bib_backend.dto.loanextension.LoanExtensionDTO;
import edu.ap.bib_backend.dto.loanextension.LoanExtensionRequestDTO;
import edu.ap.bib_backend.service.loan.LoanExtensionService;
import edu.ap.bib_backend.service.loan.LoanService;
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

import java.util.List;

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
    public ResponseEntity<LoanDTO> createLoan(@Valid @RequestBody LoanDTO loanDTO) {
        return ResponseEntity.ok(loanService.create(loanDTO));
    }

    @GetMapping
    public ResponseEntity<PageDTO<LoanDTO>> getAllLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(loanService.findAll(page, pageSize));
    }

    @PatchMapping("/{loanId}/return")
    public ResponseEntity<LoanDTO> returnLoan(@PathVariable("loanId") Long loanId) {
        return ResponseEntity.ok(loanService.returnLoan(loanId));
    }

    @GetMapping("/active")
    public ResponseEntity<PageDTO<LoanDTO>> getActiveLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(loanService.findActiveLoans(page, pageSize));
    }

    @GetMapping("/me/history")
    public ResponseEntity<LoanHistoryDTO> getMyLoanHistory(Authentication authentication) {
        return ResponseEntity.ok(loanService.findHistoryForCurrentUser(authentication));
    }

    @PatchMapping("/{loanId}/extend")
    public ResponseEntity<LoanDTO> extendLoan(
            @PathVariable("loanId") Long loanId,
            @Valid @RequestBody LoanExtensionRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanExtensionService.extendLoan(loanId, request, authentication));
    }
}