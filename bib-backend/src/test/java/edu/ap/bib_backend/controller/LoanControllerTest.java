// package edu.ap.bib_backend.controller;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import edu.ap.bib_backend.TestSecurityConfig;
// import edu.ap.bib_backend.dto.LoanDTO;
// import edu.ap.bib_backend.service.JwtService;
// import edu.ap.bib_backend.service.LoanService;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.context.annotation.Import;
// import org.springframework.http.MediaType;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.test.web.servlet.ResultActions;

// import java.util.List;

// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.verifyNoInteractions;
// import static org.mockito.Mockito.when;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest(LoanController.class)
// @AutoConfigureMockMvc(addFilters = false)
// @Import(TestSecurityConfig.class)
// class LoanControllerTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @Autowired
//     private ObjectMapper objectMapper;

//     @MockitoBean
//     private LoanService loanService;

//     @MockitoBean
//     private JwtService jwtService;

//     @Test
//     void Given_ValidLoanBody_When_CreateLoan_Then_ReturnsOkAndCreatedLoan() throws Exception {
//         // Arrange
//         LoanDTO requestBody = new LoanDTO("user-1", List.of(101L, 202L), "2099-12-31");
//         LoanDTO createdLoan = new LoanDTO("user-1", List.of(101L, 202L), "2099-12-31");

//         when(loanService.create(requestBody)).thenReturn(createdLoan);

//         // Act
//         ResultActions result = mockMvc.perform(post("/loans")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(objectMapper.writeValueAsString(requestBody)));

//         // Assert
//         result.andExpect(status().isOk())
//                 .andExpect(jsonPath("$.userId").value("user-1"))
//                 .andExpect(jsonPath("$.bookIds[0]").value(101L))
//                 .andExpect(jsonPath("$.bookIds[1]").value(202L))
//                 .andExpect(jsonPath("$.returnDate").value("2099-12-31"));

//         verify(loanService).create(requestBody);
//     }

//     @Test
//     void Given_InvalidLoanBody_When_CreateLoan_Then_ReturnsBadRequest() throws Exception {
//         // Arrange
//         String invalidRequestBody = """
//                 {
//                   \"userId\": \"\",
//                   \"bookIds\": [],
//                   \"returnDate\": \"\"
//                 }
//                 """;

//         // Act
//         ResultActions result = mockMvc.perform(post("/loans")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(invalidRequestBody));

//         // Assert
//         result.andExpect(status().isBadRequest());
//         verifyNoInteractions(loanService);
//     }
// }
