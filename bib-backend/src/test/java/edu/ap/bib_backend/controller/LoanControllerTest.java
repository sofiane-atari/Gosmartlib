package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    @Test
    void Given_ValidLoan_When_CreateLoan_Then_ReturnsOk() throws Exception {
        // Arrange
        LoanDTO loanDto = LoanDTO.builder().userId("user1").build();
        when(loanService.create(any(), any())).thenReturn(loanDto);

        // Act & Assert
        mockMvc.perform(post("/loans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"user1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user1"));
    }

    @Test
    void Given_LoansExist_When_GetAllLoans_Then_ReturnsList() throws Exception {
        // Arrange
        when(loanService.findAll(anyInt(), anyInt(), any())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/loans"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
