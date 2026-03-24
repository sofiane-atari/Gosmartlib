package edu.ap.bib_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.service.ReadingListService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReadingListController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReadingListControllerTest {

    private static final String USER_ID = "user-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReadingListService readingListService;

    @MockitoBean
    private edu.ap.bib_backend.service.JwtService jwtService;

    // GET /reading-lists

    @Test
    void givenExistingLists_whenGetAllReadingLists_thenReturnOkWithLists() throws Exception {
        ReadingList list = new ReadingList();
        list.setId(10L);
        list.setName("Test List");

        when(readingListService.getAllLists(USER_ID)).thenReturn(Collections.singletonList(list));

        mockMvc.perform(get("/reading-lists").principal(userAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test List"))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void givenNoLists_whenGetAllReadingLists_thenReturnEmptyArray() throws Exception {
        when(readingListService.getAllLists(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/reading-lists").principal(userAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // POST /reading-lists

    @Test
    void givenValidDTO_whenCreateReadingList_thenReturnCreated() throws Exception {
        ReadingListDTO dto = new ReadingListDTO("New List");
        ReadingList savedList = new ReadingList();
        savedList.setId(10L);
        savedList.setName("New List");

        when(readingListService.createReadingList(eq(USER_ID), any(ReadingListDTO.class))).thenReturn(savedList);

        mockMvc.perform(post("/reading-lists")
                        .principal(userAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New List"))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void givenEmptyName_whenCreateReadingList_thenReturnBadRequest() throws Exception {
        ReadingListDTO dto = new ReadingListDTO("");

        when(readingListService.createReadingList(eq(USER_ID), any(ReadingListDTO.class)))
                .thenThrow(new IllegalArgumentException("name mag niet null of leeg zijn"));

        mockMvc.perform(post("/reading-lists")
                        .principal(userAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // DELETE /reading-lists/{id}

    @Test
    void givenExistingId_whenDeleteReadingList_thenReturnNoContent() throws Exception {
        doNothing().when(readingListService).deleteReadingList(USER_ID, 10L);

        mockMvc.perform(delete("/reading-lists/{id}", 10L).principal(userAuthentication()))
                .andExpect(status().isNoContent());

        verify(readingListService).deleteReadingList(USER_ID, 10L);
    }

    @Test
    void givenNonExistingId_whenDeleteReadingList_thenReturnBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Leeslijst met id 99 bestaat niet"))
                .when(readingListService).deleteReadingList(USER_ID, 99L);

        mockMvc.perform(delete("/reading-lists/{id}", 99L).principal(userAuthentication()))
                .andExpect(status().isBadRequest());
    }

    // GET /reading-lists/{id}

    @Test
    void givenExistingId_whenGetReadingListById_thenReturnOk() throws Exception {
        ReadingList list = new ReadingList();
        list.setId(10L);
        list.setName("Gedeelde lijst");

        when(readingListService.getListById(USER_ID, 10L)).thenReturn(list);

        mockMvc.perform(get("/reading-lists/{id}", 10L).principal(userAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Gedeelde lijst"));
    }

    @Test
    void givenNonExistingId_whenGetReadingListById_thenReturnBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Leeslijst met id 99 bestaat niet"))
                .when(readingListService).getListById(USER_ID, 99L);

        mockMvc.perform(get("/reading-lists/{id}", 99L).principal(userAuthentication()))
                .andExpect(status().isBadRequest());
    }

    // POST /reading-lists/{listId}/books

    @Test
    void givenValidData_whenAddBookToList_thenReturnCreated() throws Exception {
        edu.ap.bib_backend.dto.AddBookDTO dto = new edu.ap.bib_backend.dto.AddBookDTO(100L);
        ReadingList list = new ReadingList();
        list.setId(10L);
        edu.ap.bib_backend.jpa.entity.ReadingListBook savedBook = new edu.ap.bib_backend.jpa.entity.ReadingListBook();
        savedBook.setId(1L);
        savedBook.setBookId(100L);
        savedBook.setReadingList(list);

        when(readingListService.addBookToList(USER_ID, 10L, 100L)).thenReturn(savedBook);

        mockMvc.perform(post("/reading-lists/{listId}/books", 10L)
                        .principal(userAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(100));
    }

    // POST /reading-lists/favorites/books

    @Test
    void givenValidData_whenAddBookToFavorites_thenReturnCreated() throws Exception {
        edu.ap.bib_backend.dto.AddBookDTO dto = new edu.ap.bib_backend.dto.AddBookDTO(100L);
        ReadingList list = new ReadingList();
        list.setId(99L);
        edu.ap.bib_backend.jpa.entity.ReadingListBook savedBook = new edu.ap.bib_backend.jpa.entity.ReadingListBook();
        savedBook.setId(2L);
        savedBook.setBookId(100L);
        savedBook.setReadingList(list);

        when(readingListService.addBookToFavorites(USER_ID, 100L)).thenReturn(savedBook);

        mockMvc.perform(post("/reading-lists/favorites/books")
                        .principal(userAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(100));
    }

    private Authentication userAuthentication() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }
}