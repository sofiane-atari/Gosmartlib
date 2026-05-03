package edu.ap.bib_backend.controller;

import org.springframework.web.bind.annotation.*;

import edu.ap.bib_backend.dto.LoanDTO;
import edu.ap.bib_backend.service.LoanService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import java.util.List;


@RestController
@RequestMapping("/loans") 
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanDTO> createLoan(@Valid @RequestBody LoanDTO loanDTO) {
        return ResponseEntity.ok(loanService.create(loanDTO));
    }

    @GetMapping
    public ResponseEntity<List<LoanDTO>> getAllLoans() {
        return ResponseEntity.ok(loanService.findAll());
    }
}