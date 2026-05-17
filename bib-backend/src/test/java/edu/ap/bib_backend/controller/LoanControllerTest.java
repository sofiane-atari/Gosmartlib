package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanGroupDTO;
import edu.ap.bib_backend.dto.loan.LoanStatsDTO;
import edu.ap.bib_backend.jpa.enums.LoanStatus;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.LoanExtensionService;
import edu.ap.bib_backend.service.LoanService;
import edu.ap.bib_backend.service.TokenStorageService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import edu.ap.bib_backend.dto.PaginatedMeta;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.eq;

@WebMvcTest(LoanController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoanControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private LoanService loanService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private AdminOverrideService adminOverrideService;

        @MockitoBean
        private TokenStorageService tokenStorageService;

        @MockitoBean
        private LoanExtensionService loanExtensionService;

        @Test
        void Given_ValidLoan_When_CreateLoan_Then_ReturnsOkWithCreatedLoans() throws Exception {
                // Arrange
                LoanDTO loanDto = LoanDTO.builder()
                                .id(1L)
                                .userId("user1")
                                .bookCopyIds(List.of(1L))
                                .returnDate("2099-12-31")
                                .build();

                when(loanService.create(any(), any())).thenReturn(List.of(loanDto));

                // Act & Assert
                mockMvc.perform(post("/loans")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "userId": "user1",
                                                  "bookCopyIds": [1],
                                                  "returnDate": "2099-12-31"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].userId").value("user1"))
                                .andExpect(jsonPath("$[0].bookCopyIds[0]").value(1))
                                .andExpect(jsonPath("$[0].returnDate").value("2099-12-31"));
        }

        @Test
        void Given_MultipleBooks_When_CreateLoan_Then_ReturnsMultipleCreatedLoans() throws Exception {
                // Arrange
                LoanDTO firstLoan = LoanDTO.builder()
                                .id(1L)
                                .userId("user1")
                                .bookIds(List.of(2L))
                                .returnDate("2099-12-31")
                                .build();

                LoanDTO secondLoan = LoanDTO.builder()
                                .id(2L)
                                .userId("user1")
                                .bookIds(List.of(9L))
                                .returnDate("2099-12-31")
                                .build();

                when(loanService.create(any(), any())).thenReturn(List.of(firstLoan, secondLoan));

                // Act & Assert
                mockMvc.perform(post("/loans")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "userId": "user1",
                                                  "bookIds": [2, 9],
                                                  "returnDate": "2099-12-31"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].bookIds[0]").value(2))
                                .andExpect(jsonPath("$[1].bookIds[0]").value(9));
        }

        @Test
        void Given_LoansExist_When_GetAllLoans_Then_ReturnsPagedLoans() throws Exception {
                // Arrange
                LoanDTO loanDto = LoanDTO.builder()
                                .id(1L)
                                .userId("user1")
                                .bookIds(List.of(1L))
                                .returnDate("2099-12-31")
                                .build();

                PaginatedMeta meta = new PaginatedMeta(0, 10, 1, 1);
                PageDTO<LoanDTO> page = new PageDTO<>(List.of(loanDto), meta);

                when(loanService.findAll(anyInt(), anyInt(), any(), any())).thenReturn(page);

                // Act & Assert
                mockMvc.perform(get("/loans"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.data[0].id").value(1))
                                .andExpect(jsonPath("$.data[0].userId").value("user1"))
                                .andExpect(jsonPath("$.meta.currentPage").value(0))
                                .andExpect(jsonPath("$.meta.pageSize").value(10))
                                .andExpect(jsonPath("$.meta.totalItems").value(1))
                                .andExpect(jsonPath("$.meta.totalPages").value(1));
        }

        @Test
        void Given_GroupedLoansExist_When_GetGroupedLoans_Then_ReturnsPagedLoanGroups() throws Exception {
                // Arrange
                LoanDTO loanDto = LoanDTO.builder()
                                .id(1L)
                                .userId("user1")
                                .bookIds(List.of(2L))
                                .returnDate("2099-12-31")
                                .status(LoanStatus.ACTIVE)
                                .build();

                LoanGroupDTO groupDto = new LoanGroupDTO(
                                "user1-ACTIVE",
                                "user1",
                                "Jan Peeters",
                                LoanStatus.ACTIVE,
                                1,
                                1,
                                "2099-12-31",
                                List.of(loanDto));

                PaginatedMeta meta = new PaginatedMeta(0, 10, 1, 1);
                PageDTO<LoanGroupDTO> page = new PageDTO<>(List.of(groupDto), meta);

                when(loanService.findAllGrouped(anyInt(), anyInt(), any())).thenReturn(page);

                // Act & Assert
                mockMvc.perform(get("/loans/grouped")
                                .param("page", "0")
                                .param("pageSize", "10"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.data[0].groupId").value("user1-ACTIVE"))
                                .andExpect(jsonPath("$.data[0].userId").value("user1"))
                                .andExpect(jsonPath("$.data[0].userDisplayName").value("Jan Peeters"))
                                .andExpect(jsonPath("$.data[0].groupStatus").value("ACTIVE"))
                                .andExpect(jsonPath("$.data[0].loanCount").value(1))
                                .andExpect(jsonPath("$.data[0].bookCount").value(1))
                                .andExpect(jsonPath("$.data[0].earliestReturnDate").value("2099-12-31"))
                                .andExpect(jsonPath("$.data[0].loans[0].id").value(1))
                                .andExpect(jsonPath("$.meta.totalItems").value(1));

                then(loanService).should().findAllGrouped(anyInt(), anyInt(), any());
        }

        @Test
        void givenLoanStatsExist_whenGetLoanStats_thenReturnOkWithStats() throws Exception {
                // Arrange
                LoanStatsDTO stats = new LoanStatsDTO(8L, 3L, 12L);

                given(loanService.getLoanStats()).willReturn(stats);

                // Act
                ResultActions result = mockMvc.perform(get("/loans/stats"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.activeLoans").value(8))
                                .andExpect(jsonPath("$.overdueLoans").value(3))
                                .andExpect(jsonPath("$.returnedLoans").value(12));

                then(loanService).should().getLoanStats();
        }

    @Test
    void Given_ReturnRequestWithCondition_When_ReturnLoan_Then_ReturnsOk() throws Exception {
        LoanDTO loanDto = LoanDTO.builder()
                .id(1L)
                .userId("user1")
                .build();

        when(loanService.returnLoan(eq(1L), any(), any())).thenReturn(loanDto);

        mockMvc.perform(patch("/loans/1/return")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "copyConditions": [
                                    { "bookCopyId": 10, "condition": "DAMAGED" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}