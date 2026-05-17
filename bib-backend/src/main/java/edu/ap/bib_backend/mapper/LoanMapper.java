package edu.ap.bib_backend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.ap.bib_backend.dto.loan.LoanBookDTO;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "loanDate", ignore = true)
    @Mapping(target = "returnDate", ignore = true)
    @Mapping(target = "copies", ignore = true)
    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "returnedDate", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "extensions", ignore = true)
    Loan toEntity(LoanDTO dto);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "bookCopyIds", expression = "java(mapBookCopyIds(loan.getCopies()))")
    @Mapping(target = "books", expression = "java(mapBooks(loan.getCopies()))")
    @Mapping(target = "loanDate", expression = "java(loan.getLoanDate() != null ? loan.getLoanDate().toString() : null)")
    @Mapping(target = "returnDate", expression = "java(loan.getReturnDate() != null ? loan.getReturnDate().toString() : null)")
    @Mapping(target = "returnedDate", expression = "java(loan.getReturnedDate() != null ? loan.getReturnedDate().toString() : null)")
    @Mapping(target = "status", expression = "java(loan.getStatus())")
    @Mapping(target = "extensionCount", expression = "java(loan.getExtensions() != null ? loan.getExtensions().size() : 0)")
    LoanDTO toDto(Loan loan);

    default List<Long> mapBookCopyIds(List<BookCopy> copies) {
        if (copies == null) {
            return List.of();
        }

        return copies.stream()
                .map(BookCopy::getId)
                .toList();
    }

    default List<LoanBookDTO> mapBooks(List<BookCopy> copies) {
        if (copies == null) {
            return List.of();
        }

        return copies.stream()
                .filter(copy -> copy.getBook() != null)
                .collect(java.util.stream.Collectors.groupingBy(BookCopy::getBook))
                .entrySet()
                .stream()
                .map(entry -> new LoanBookDTO(
                        entry.getKey().getId(),
                        entry.getKey().getTitle(),
                        entry.getKey().getAuthor(),
                        entry.getKey().getIsbn(),
                        entry.getKey().getImageUrl(),
                        entry.getValue().stream().map(BookCopy::getId).toList(),
                        entry.getValue().size()))
                .toList();
    }
}
