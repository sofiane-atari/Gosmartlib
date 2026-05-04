package edu.ap.bib_backend.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.dto.ReadingLevelDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Genre;

@Component
public class BookMapper {
    public BookDTO toDTO(Book book) {
        if (book == null)
            return null;

        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setIsbn(book.getIsbn());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setPages(book.getPages());
        dto.setImageUrl(book.getImageUrl());
        dto.setDescription(book.getDescription());
        dto.setReleaseYear(book.getReleaseYear());
        dto.setReviewScore(book.getReviewScore() != null ? book.getReviewScore() : 0.0);
        dto.setAddedDate(book.getAddedDate());
        dto.setDidactic(book.isDidactic());
        dto.setTotalCopies(book.getTotalCopies() != null ? book.getTotalCopies() : 0);
        dto.setAvailableCopies(book.getAvailableCopies() != null ? book.getAvailableCopies() : 0);

        // Simpele mapping voor Genre (zonder DB call)
        dto.setGenres(book.getGenres().stream()
                .map(this::genreToDTO)
                .toList());

        if (book.getReadingLevel() != null) {
            dto.setReadingLevel(new ReadingLevelDTO(
                    book.getReadingLevel().getId(),
                    book.getReadingLevel().getNiveauName(),
                    book.getReadingLevel().getDescription()));
        }
        return dto;
    }

    public Book toEntity(BookDTO dto) {
        if (dto == null) {
            return null;
        }

        Book book = new Book();
        updateEntity(book, dto);
        return book;
    }

    public void updateEntity(Book book, BookDTO dto) {
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPages(dto.getPages());
        book.setImageUrl(dto.getImageUrl());
        book.setDescription(dto.getDescription());
        book.setReleaseYear(dto.getReleaseYear());
        book.setDidactic(dto.isDidactic());
    }

    public List<BookDTO> toDTOList(List<Book> books) {
        return books.stream()
                .map(this::toDTO)
                .toList();
    }

    private GenreDTO genreToDTO(Genre genre) {
        return GenreDTO.builder()
                .name(genre.getName())
                .color(genre.getColor())
                .build();
    }
}
