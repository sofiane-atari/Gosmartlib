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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReadingListController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReadingListControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private ReadingListService readingListService;

    @Autowired
    ReadingListControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void getAllReadingLists_ShouldReturnLists() throws Exception {
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
    void createReadingList_ValidDTO_ShouldReturnCreated() throws Exception {
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
}