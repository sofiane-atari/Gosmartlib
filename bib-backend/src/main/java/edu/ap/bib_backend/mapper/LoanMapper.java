package edu.ap.bib_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.ap.bib_backend.dto.LoanDTO;
import edu.ap.bib_backend.jpa.entity.Loan;

@Mapper(componentModel = "spring")
public interface LoanMapper {
    
    @Mapping(target = "user.id", source = "userId")
    Loan toEntity(LoanDTO dto);

    @Mapping(target = "userId", source = "user.id")
    LoanDTO toDto(Loan loan);
}