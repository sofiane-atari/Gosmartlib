package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.LoanDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.service.book.BookService;
import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private LoanMapper loanMapper;
    @Mock
    private BookService bookService;
    @Mock
    private ExternalUserDisplayService externalUserDisplayService;
    @Mock
    private BookCopyRepository bookCopyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SmartschoolMessageService smartschoolMessageService;
    @Mock
    private SmartschoolTokenRefreshService smartschoolTokenRefreshService;

    @InjectMocks
    private LoanService loanService;

    @Test
    void Given_ValidLoanDTO_When_Create_Then_SavesAndReturnsDto() {
        // Arrange
        String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookIds(List.of(1L))
                .returnDate(returnDate)
                .build();

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        Book book = new Book();
        book.setTitle("Test Book");
        when(bookService.getBookById(1L, true)).thenReturn(book);
        
        BookCopy copy = new BookCopy();
        when(bookCopyRepository.findAvailableCopiesByBookId(1L)).thenReturn(List.of(copy));
        
        LoanDTO expectedDto = mock(LoanDTO.class);
        when(loanMapper.toDto(loan)).thenReturn(expectedDto);

        // Act
        LoanDTO result = loanService.create(inputDto);

        // Assert
        assertNotNull(result);
        assertEquals(expectedDto, result);
        verify(loanRepository, atLeastOnce()).save(loan);
        verify(smartschoolTokenRefreshService).getValidSmartschoolAccessToken("user123");
    }

    @Test
    void Given_LoansExist_When_FindAll_Then_ReturnsPopulatedDtos() {
        // Arrange
        User user = new User();
        user.setOneRosterSourcedId("sourced-1");
        School school = new School();
        school.setDomainName("school.be");
        user.setSchool(school);

        Loan loan = new Loan();
        loan.setUser(user);
        when(loanRepository.findAll()).thenReturn(List.of(loan));

        LoanDTO basicDto = LoanDTO.builder().id(1L).userId("user123").build();
        when(loanMapper.toDto(loan)).thenReturn(basicDto);

        when(externalUserDisplayService.getDisplayNames(eq("school.be"), anySet()))
                .thenReturn(Map.of("sourced-1", "John Doe"));

        // Act
        List<LoanDTO> result = loanService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).userDisplayName());
    }

    @Test
    void Given_NoReturnDate_When_Create_Then_UsesDefaultReturnDateFromSchool() {
        // Arrange
        LoanDTO inputDto = LoanDTO.builder().userId("user123").bookIds(List.of(1L)).build();
        
        User user = new User();
        School school = new School();
        school.setDefaultLoanDurationDays(30);
        user.setSchool(school);
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        
        when(bookService.getBookById(1L, true)).thenReturn(new Book());
        when(bookCopyRepository.findAvailableCopiesByBookId(1L)).thenReturn(List.of(new BookCopy()));

        // Act
        loanService.create(inputDto);

        // Assert
        assertNotNull(loan.getReturnDate());
        assertEquals(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(30), loan.getReturnDate());
    }
}
