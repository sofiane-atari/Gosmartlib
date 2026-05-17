// package edu.ap.bib_backend.controller;

// import edu.ap.bib_backend.dto.PageDTO;
// import edu.ap.bib_backend.dto.loan.LoanDTO;
// import edu.ap.bib_backend.dto.loan.LoanStatsDTO;
// import edu.ap.bib_backend.service.AdminOverrideService;
// import edu.ap.bib_backend.service.JwtService;
// import edu.ap.bib_backend.service.LoanExtensionService;
// import edu.ap.bib_backend.service.LoanService;
// import edu.ap.bib_backend.service.TokenStorageService;

// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.http.MediaType;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.test.web.servlet.ResultActions;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyInt;
// import static org.mockito.Mockito.when;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// import java.util.List;
// import edu.ap.bib_backend.dto.PaginatedMeta;

// import static org.mockito.BDDMockito.given;
// import static org.mockito.BDDMockito.then;

// @WebMvcTest(LoanController.class)
// @AutoConfigureMockMvc(addFilters = false)
// class LoanControllerTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @MockitoBean
//     private LoanService loanService;

//     @MockitoBean
//     private JwtService jwtService;

//     @MockitoBean
//     private AdminOverrideService adminOverrideService;

//     @MockitoBean
//     private TokenStorageService tokenStorageService;

//     @MockitoBean
//     private LoanExtensionService loanExtensionService;

//     @Test
//     void Given_ValidLoan_When_CreateLoan_Then_ReturnsOk() throws Exception {
//         // Arrange
//         LoanDTO loanDto = LoanDTO.builder()
//                 .userId("user1")
//                 .bookIds(List.of(1L))
//                 .returnDate("2099-12-31")
//                 .build();

//         when(loanService.create(any(), any())).thenReturn(loanDto);

//         // Act & Assert
//         mockMvc.perform(post("/loans")
//                 .with(csrf())
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content("""
//                         {
//                           "userId": "user1",
//                           "bookIds": [1],
//                           "returnDate": "2099-12-31"
//                         }
//                         """))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.userId").value("user1"))
//                 .andExpect(jsonPath("$.bookIds[0]").value(1))
//                 .andExpect(jsonPath("$.returnDate").value("2099-12-31"));
//     }

//     @Test
//     void Given_LoansExist_When_GetAllLoans_Then_ReturnsList() throws Exception {
//         // Arrange
//         LoanDTO loanDto = LoanDTO.builder()
//                 .userId("user1")
//                 .build();

//         PaginatedMeta meta = new PaginatedMeta(
//                 0,
//                 10,
//                 1,
//                 1);

//         PageDTO<LoanDTO> page = new PageDTO<>(
//                 List.of(loanDto),
//                 meta);

//         when(loanService.findAll(anyInt(), anyInt(), any()))
//                 .thenReturn(page);

//         // Act & Assert
//         mockMvc.perform(get("/loans"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//                 .andExpect(jsonPath("$.data[0].userId").value("user1"));
//     }

//     @Test
//     void givenLoanStatsExist_whenGetLoanStats_thenReturnOkWithStats() throws Exception {
//         // Arrange
//         LoanStatsDTO stats = new LoanStatsDTO(8L, 3L, 12L);

//         given(loanService.getLoanStats()).willReturn(stats);

//         // Act
//         ResultActions result = mockMvc.perform(get("/loans/stats"));

//         // Assert
//         result.andExpect(status().isOk())
//                 .andExpect(jsonPath("$.activeLoans").value(8))
//                 .andExpect(jsonPath("$.overdueLoans").value(3))
//                 .andExpect(jsonPath("$.returnedLoans").value(12));

//         then(loanService).should().getLoanStats();
//     }
// }
