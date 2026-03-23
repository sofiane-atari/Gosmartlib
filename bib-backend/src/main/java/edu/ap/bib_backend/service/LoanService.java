package edu.ap.bib_backend.service;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.LoanDTO;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.mapper.LoanMapper;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;


    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
    }
    
    public LoanDTO create(LoanDTO loanDTO){
        try {
            Loan loan = loanMapper.toEntity(loanDTO);
            loan = loanRepository.save(loan);

            return loanMapper.toDto(loan);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Uitlening kon niet worden aangemaakt", e);
        }
    }
}