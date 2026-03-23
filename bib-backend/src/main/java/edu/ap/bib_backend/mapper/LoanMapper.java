package edu.ap.bib_backend.mapper;

import org.mapstruct.Mapper;

import edu.ap.bib_backend.dto.LoanDTO;
import edu.ap.bib_backend.jpa.entity.Loan;

@Mapper(componentModel = "spring")
public interface LoanMapper {
    
    Loan toEntity(LoanDTO dto);
    LoanDTO toDto(Loan loan);
}