package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.BookCondition;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import edu.ap.bib_backend.service.BookCopyService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookCopyController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookCopyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookCopyRepository bookCopyRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private BookCopyService bookCopyService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AdminOverrideService adminOverrideService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @MockitoBean
    private ActiveSchoolContext activeSchoolContext;

    @MockitoBean
    private SchoolRepository schoolRepository;

    @Test
    void Given_AdminConditionUpdate_When_UpdateCondition_Then_ReturnsUpdatedCondition() throws Exception {
        School school = new School();
        school.setId(1L);

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setSchool(school);

        BookCopy copy = new BookCopy();
        copy.setId(10L);
        copy.setPublicIdentifier("2800000000100");
        copy.setCampus(campus);

        when(bookCopyRepository.findById(10L)).thenReturn(Optional.of(copy));
        when(bookCopyRepository.save(any(BookCopy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserService.getCurrentUserSchool(any())).thenReturn(school);

        mockMvc.perform(patch("/book-copies/10/condition")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "condition": "GOOD_CONDITION" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookCopyId").value(10))
                .andExpect(jsonPath("$.identifier").value("2800000000100"))
                .andExpect(jsonPath("$.condition").value(BookCondition.GOOD_CONDITION.name()));
    }
}
