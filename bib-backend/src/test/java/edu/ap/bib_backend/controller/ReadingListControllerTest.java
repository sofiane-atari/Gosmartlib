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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReadingListController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReadingListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReadingListService readingListService;

    // GET /api/reading-lists

    @Test
    void givenExistingLists_whenGetAllReadingLists_thenReturnOkWithLists() throws Exception {
        ReadingList list = new ReadingList();
        list.setId(10L);
        list.setName("Test List");

        when(readingListService.getAllLists()).thenReturn(Collections.singletonList(list));

        mockMvc.perform(get("/api/reading-lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test List"))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void givenNoLists_whenGetAllReadingLists_thenReturnEmptyArray() throws Exception {
        when(readingListService.getAllLists()).thenReturn(List.of());

        mockMvc.perform(get("/api/reading-lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // POST /api/reading-lists

    @Test
    void givenValidDTO_whenCreateReadingList_thenReturnCreated() throws Exception {
        ReadingListDTO dto = new ReadingListDTO("New List");
        ReadingList savedList = new ReadingList();
        savedList.setId(10L);
        savedList.setName("New List");

        when(readingListService.createReadingList(any(ReadingListDTO.class))).thenReturn(savedList);

        mockMvc.perform(post("/api/reading-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New List"))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void givenEmptyName_whenCreateReadingList_thenReturnBadRequest() throws Exception {
        ReadingListDTO dto = new ReadingListDTO("");

        when(readingListService.createReadingList(any(ReadingListDTO.class)))
                .thenThrow(new IllegalArgumentException("name mag niet null of leeg zijn"));

        mockMvc.perform(post("/api/reading-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // DELETE /api/reading-lists/{id}

    @Test
    void givenExistingId_whenDeleteReadingList_thenReturnNoContent() throws Exception {
        doNothing().when(readingListService).deleteReadingList(10L);

        mockMvc.perform(delete("/api/reading-lists/{id}", 10L))
                .andExpect(status().isNoContent());

        verify(readingListService).deleteReadingList(10L);
    }

    @Test
    void givenNonExistingId_whenDeleteReadingList_thenReturnBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Leeslijst met id 99 bestaat niet"))
                .when(readingListService).deleteReadingList(99L);

        mockMvc.perform(delete("/api/reading-lists/{id}", 99L))
                .andExpect(status().isBadRequest());
    }

    // GET /api/reading-lists/{id}

    @Test
    void givenExistingId_whenGetReadingListById_thenReturnOk() throws Exception {
        ReadingList list = new ReadingList();
        list.setId(10L);
        list.setName("Gedeelde lijst");

        when(readingListService.getListById(10L)).thenReturn(list);

        mockMvc.perform(get("/api/reading-lists/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Gedeelde lijst"));
    }

    @Test
    void givenNonExistingId_whenGetReadingListById_thenReturnBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Leeslijst met id 99 bestaat niet"))
                .when(readingListService).getListById(99L);

        mockMvc.perform(get("/api/reading-lists/{id}", 99L))
                .andExpect(status().isBadRequest());
    }
}