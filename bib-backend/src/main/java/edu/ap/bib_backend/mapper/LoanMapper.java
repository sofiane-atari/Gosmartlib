package edu.ap.bib_backend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.ap.bib_backend.dto.loan.LoanBookDTO;
import edu.ap.bib_backend.dto.loan.LoanBookCopyDTO;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "batchId", ignore = true)
    @Mapping(target = "loanDate", ignore = true)
    @Mapping(target = "returnDate", ignore = true)
    @Mapping(target = "copy", ignore = true)
    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "returnedDate", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "extensions", ignore = true)
    Loan toEntity(LoanDTO dto);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "batchId", source = "batchId")
    @Mapping(target = "bookCopyIds", expression = "java(mapBookCopyIds(loan.getCopy()))")
    @Mapping(target = "books", expression = "java(mapBooks(loan.getCopy()))")
    @Mapping(target = "loanDate", expression = "java(loan.getLoanDate() != null ? loan.getLoanDate().toString() : null)")
    @Mapping(target = "returnDate", expression = "java(loan.getReturnDate() != null ? loan.getReturnDate().toString() : null)")
    @Mapping(target = "returnedDate", expression = "java(loan.getReturnedDate() != null ? loan.getReturnedDate().toString() : null)")
    @Mapping(target = "status", expression = "java(loan.getStatus())")
    @Mapping(target = "extensionCount", expression = "java(loan.getExtensions() != null ? loan.getExtensions().size() : 0)")
    LoanDTO toDto(Loan loan);

    List<LoanDTO> toDtoList(List<Loan> loans);
    default List<Long> mapBookCopyIds(BookCopy copy) {
        if (copy == null || copy.getId() == null) {
            return List.of();
        }

        return List.of(copy.getId());
    }

    default List<LoanBookDTO> mapBooks(BookCopy copy) {
        if (copy == null || copy.getBook() == null || copy.getBook().getId() == null) {
            return List.of();
        }

        var book = copy.getBook();
        return List.of(new LoanBookDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getImageUrl(),
                copy.getId() == null ? List.of() : List.of(copy.getId()),
                copy.getId() == null ? List.of() : List.of(new LoanBookCopyDTO(copy.getId(), copy.getPublicIdentifier())),
                1));
    }
}
