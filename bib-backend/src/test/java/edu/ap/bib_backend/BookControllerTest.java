package edu.ap.bib_backend;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.ap.bib_backend.controller.BookController;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.service.BookService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class) 
public class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bs;

    @Test
    void getAllBooks_returnJSONList() throws Exception {
        BookDTO book = new BookDTO();
        long ISBN = 1672587;
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setISBN(ISBN);
        book.setPages(324);
        book.setImage_url("https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");

        Mockito.when(bs.getAllBooks()).thenReturn(List.of(book));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].author").value("Test Author"))
                .andExpect(jsonPath("$[0].isbn").value(1672587))
                .andExpect(jsonPath("$[0].pages").value(324))
                .andExpect(jsonPath("$[0].image_url").value(
                        "https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1"));

    }
}
