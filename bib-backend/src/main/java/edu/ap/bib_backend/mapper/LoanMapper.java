package edu.ap.bib_backend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.ap.bib_backend.dto.loan.LoanBookDTO;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.loan.Loan;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "loanDate", ignore = true)
    @Mapping(target = "returnDate", ignore = true)
    @Mapping(target = "copies", ignore = true)
    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "returnedDate", ignore = true)
    Loan toEntity(LoanDTO dto);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "bookIds", expression = "java(mapBookIds(loan.getCopies()))")
    @Mapping(target = "books", expression = "java(mapBooks(loan.getCopies()))")
    @Mapping(target = "loanDate", expression = "java(loan.getLoanDate() != null ? loan.getLoanDate().toString() : null)")
    @Mapping(target = "returnDate", expression = "java(loan.getReturnDate() != null ? loan.getReturnDate().toString() : null)")
    @Mapping(target = "returnedDate", expression = "java(loan.getReturnedDate() != null ? loan.getReturnedDate().toString() : null)")
    @Mapping(target = "status", expression = "java(loan.getStatus())")
    @Mapping(target = "extensionCount", expression = "java(loan.getExtensions() != null ? loan.getExtensions().size() : 0)")
    LoanDTO toDto(Loan loan);

    default List<Long> mapBookIds(List<BookCopy> copies) {
        if (copies == null) {
            return List.of();
        }

        return copies.stream()
                .filter(copy -> copy.getBook() != null)
                .map(copy -> copy.getBook().getId())
                .toList();
    }

    default List<LoanBookDTO> mapBooks(List<BookCopy> copies) {
        if (copies == null) {
            return List.of();
        }

        return copies.stream()
                .filter(copy -> copy.getBook() != null)
                .map(copy -> new LoanBookDTO(
                        copy.getBook().getId(),
                        copy.getBook().getTitle(),
                        copy.getBook().getAuthor(),
                        copy.getBook().getIsbn(),
                        copy.getBook().getImageUrl()
                ))
                .toList();
    }
}