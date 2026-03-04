package edu.ap.bib_backend;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.service.BookService;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookRepository br;

    @InjectMocks
    private BookService bs;

    public BookServiceTest() {
    }

    @Test
    void getAllBooks_retunMappedDTOs() {

        Book book = new Book();
        long ISBN = 1672587;
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setISBN(ISBN);
        book.setPages(324);
        book.setImage_url("https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");

        when(br.findAll()).thenReturn(List.of(book));

        List<BookDTO> res = bs.getAllBooks();

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getTitle()).isEqualTo("Test Book");
        assertThat(res.get(0).getAuthor()).isEqualTo("Test Author");
        assertThat(res.get(0).getISBN()).isEqualTo(1672587);
        assertThat(res.get(0).getPages()).isEqualTo(324);
        assertThat(res.get(0).getImage_url())
                .isEqualTo("https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");

    }
}
