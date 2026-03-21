package edu.ap.bib_backend.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ap.bib_backend.TestSecurityConfig;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.service.BookService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bs;

    @Test
    void getBookById_returnsBook() throws Exception {
        BookDTO book = new BookDTO();
        book.setId(1L);
        book.setTitle("Specifiek Boek");
        book.setAuthor("Bekende Auteur");

        Mockito.when(bs.getBookById(1L)).thenReturn(book);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Specifiek Boek"));
    }

    @Test
    void addBook_returnsSavedBook() throws Exception {
        BookDTO requestDTO = new BookDTO();
        requestDTO.setTitle("Nieuwe Boek");
        requestDTO.setAuthor("Auteur Test");
        requestDTO.setPages(300);
        requestDTO.setImageUrl("https://example.com/image.jpg");
        requestDTO.setDescription("Beschrijving hier");
        requestDTO.setReleaseYear(2024);

        BookDTO savedDTO = new BookDTO();
        savedDTO.setId(1L);
        savedDTO.setTitle("Nieuwe Boek");

        Mockito.when(bs.addBook(Mockito.any(BookDTO.class))).thenReturn(savedDTO);

        mockMvc.perform(post("/api/books")
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Nieuwe Boek"));
    }

    @Test
    void getRelatedBooks_returnsJSONList() throws Exception {
        BookDTO related1 = new BookDTO();
        related1.setId(2L);
        related1.setTitle("Related Book 1");

        BookDTO related2 = new BookDTO();
        related2.setId(3L);
        related2.setTitle("Related Book 2");

        Mockito.when(bs.getRelatedBooks(1L, 3)).thenReturn(List.of(related1, related2));

        mockMvc.perform(get("/api/books/1/related").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Related Book 1"))
                .andExpect(jsonPath("$[1].title").value("Related Book 2"));
    }

    @Test
    void assignLabels_returnsUpdatedBook() throws Exception {
        BookDTO updated = new BookDTO();
        updated.setId(1L);
        updated.setTitle("Book With Labels");
        updated.setLabels(List.of("Fantasy", "Avontuur"));

        Mockito.when(bs.assignLabels(Mockito.eq(1L), Mockito.anyList())).thenReturn(updated);

        mockMvc.perform(put("/api/books/1/labels")
                .contentType("application/json")
                .content("[\"Fantasy\",\"Avontuur\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.labels[0]").value("Fantasy"))
                .andExpect(jsonPath("$.labels[1]").value("Avontuur"));
    }
}
