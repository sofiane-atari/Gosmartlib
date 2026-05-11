package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.service.book.BookService;
import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.loan.LoanBookDTO;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanStatsDTO;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private Authentication authentication;

    private final Campus campus = new Campus();

    @InjectMocks
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        campus.setId(1L);
        lenient().when(currentUserService.getCurrentUserCampus(any())).thenReturn(campus);
    }

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
        when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L)).thenReturn(List.of(copy));

        LoanDTO expectedDto = mock(LoanDTO.class);
        when(loanMapper.toDto(loan)).thenReturn(expectedDto);

        // Act
        LoanDTO result = loanService.create(inputDto, authentication);

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
        when(loanRepository.findByCampusId(1L)).thenReturn(List.of(loan));

        LoanDTO basicDto = LoanDTO.builder().id(1L).userId("user123").build();
        when(loanMapper.toDto(loan)).thenReturn(basicDto);

        when(externalUserDisplayService.getDisplayNames(eq("school.be"), anySet()))
                .thenReturn(Map.of("sourced-1", "John Doe"));

        // Act
        PageDTO<LoanDTO> result = loanService.findAll(0, 10, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.data().size());
        assertEquals("John Doe", result.data().get(0).userDisplayName());
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
        when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L)).thenReturn(List.of(new BookCopy()));

        // Act
        loanService.create(inputDto, authentication);

        // Assert
        assertNotNull(loan.getReturnDate());
        assertEquals(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(30), loan.getReturnDate());
    }

    @Test
    void Given_InvalidReturnDateFormat_When_Create_Then_ThrowsIllegalStateException() {
        // Arrange
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookIds(List.of(1L))
                .returnDate("invalid-date-format")
                .build();

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));
        assertEquals("Retourdatum heeft een ongeldig formaat.", exception.getMessage());
    }

    @Test
    void Given_ReturnDateNotInFuture_When_Create_Then_ThrowsIllegalStateException() {
        // Arrange
        String pastDate = LocalDate.now(ZoneId.of("Europe/Brussels")).minusDays(1).toString();
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookIds(List.of(1L))
                .returnDate(pastDate)
                .build();

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));
        assertEquals("Retourdatum moet minimaal één dag in de toekomst liggen.", exception.getMessage());
    }

    @Test
    void Given_TodayReturnDate_When_Create_Then_ThrowsIllegalStateException() {
        // Arrange
        String today = LocalDate.now(ZoneId.of("Europe/Brussels")).toString();
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookIds(List.of(1L))
                .returnDate(today)
                .build();

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));
        assertEquals("Retourdatum moet minimaal één dag in de toekomst liggen.", exception.getMessage());
    }

    @Test
    void Given_NotEnoughCopiesAvailable_When_Create_Then_ThrowsIllegalStateException() {
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

        // Only 1 copy available, but need 2
        when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L)).thenReturn(List.of(new BookCopy()));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));
        assertTrue(exception.getMessage().contains("Niet genoeg beschikbare exemplaren"));
    }

    @Test
    void Given_MultipleBooksWithCopiesCount_When_Create_Then_ReservesCorrectNumberOfCopies() {
        // Arrange
        String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();
        LoanBookDTO book1 = LoanBookDTO.builder().id(1L).title("Book 1").author("Author 1").isbn("isbn1").imageUrl(null)
                .copiesCount(2).build();
        LoanBookDTO book2 = LoanBookDTO.builder().id(2L).title("Book 2").author("Author 2").isbn("isbn2").imageUrl(null)
                .copiesCount(3).build();
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookIds(List.of(1L, 2L))
                .returnDate(returnDate)
                .books(List.of(book1, book2))
                .build();

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        Book b1 = new Book();
        b1.setTitle("Book 1");
        Book b2 = new Book();
        b2.setTitle("Book 2");
        when(bookService.getBookById(1L, true)).thenReturn(b1);
        when(bookService.getBookById(2L, true)).thenReturn(b2);

        when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L))
                .thenReturn(List.of(new BookCopy(), new BookCopy()));
        when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(2L, 1L))
                .thenReturn(List.of(new BookCopy(), new BookCopy(), new BookCopy()));

        // Act
        loanService.create(inputDto, authentication);

        // Assert - should have 5 copies (2 for book1 + 3 for book2)
        assertEquals(5, loan.getCopies().size());
    }

    @Test
    void Given_UserNotFound_When_Create_Then_ThrowsIllegalStateException() {
        // Arrange
        LoanDTO inputDto = LoanDTO.builder()
                .userId("nonexistent")
                .bookIds(List.of(1L))
                .build();

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));
        assertEquals("Gebruiker niet gevonden.", exception.getMessage());
    }

    @Test
    void Given_NullDefaultLoanDuration_When_Create_Then_UsesDefault15Days() {
        // Arrange
        LoanDTO inputDto = LoanDTO.builder().userId("user123").bookIds(List.of(1L)).build();

        User user = new User();
        School school = new School();
        school.setDefaultLoanDurationDays(null); // null duration
        user.setSchool(school);
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        when(bookService.getBookById(1L, true)).thenReturn(new Book());
        when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L)).thenReturn(List.of(new BookCopy()));

        // Act
        loanService.create(inputDto, authentication);

        // Assert
        assertNotNull(loan.getReturnDate());
        assertEquals(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(15), loan.getReturnDate());
    }

    @Test
    void Given_LoansWithoutUser_When_FindAll_Then_HandlesGracefully() {
        // Arrange
        Loan loan = new Loan();
        loan.setUser(null);
        when(loanRepository.findByCampusId(1L)).thenReturn(List.of(loan));

        LoanDTO basicDto = LoanDTO.builder().id(1L).userId("user123").build();
        when(loanMapper.toDto(loan)).thenReturn(basicDto);

        // Act
        PageDTO<LoanDTO> result = loanService.findAll(0, 10, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.data().size());
    }

    @Test
    void Given_LoanWithoutSchool_When_FindAll_Then_HandlesGracefully() {
        // Arrange
        User user = new User();
        user.setOneRosterSourcedId("sourced-1");
        user.setSchool(null); // No school

        Loan loan = new Loan();
        loan.setUser(user);
        when(loanRepository.findByCampusId(1L)).thenReturn(List.of(loan));

        LoanDTO basicDto = LoanDTO.builder().id(1L).userId("user123").build();
        when(loanMapper.toDto(loan)).thenReturn(basicDto);

        // Act
        PageDTO<LoanDTO> result = loanService.findAll(0, 10, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.data().size());
    }

    @Test
    void Given_NullToken_When_Create_Then_StillSavesLoan() {
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

        when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L)).thenReturn(List.of(new BookCopy()));

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken("user123")).thenReturn(null);

        LoanDTO expectedDto = mock(LoanDTO.class);
        when(loanMapper.toDto(loan)).thenReturn(expectedDto);

        // Act
        LoanDTO result = loanService.create(inputDto, authentication);

        // Assert
        assertNotNull(result);
        // Should not throw even when token is null
    }

    @Test
    void givenLoansExist_whenGetLoanStats_thenReturnCorrectStats() {
        // Arrange
        LocalDate today = LocalDate.now();

        given(loanRepository.countByReturnedDateIsNullAndReturnDateGreaterThanEqual(today))
                .willReturn(8L);

        given(loanRepository.countByReturnedDateIsNullAndReturnDateBefore(today))
                .willReturn(3L);

        given(loanRepository.countByReturnedDateIsNotNull())
                .willReturn(12L);

        // Act
        LoanStatsDTO result = loanService.getLoanStats();

        // Assert
        assertThat(result.activeLoans()).isEqualTo(8L);
        assertThat(result.overdueLoans()).isEqualTo(3L);
        assertThat(result.returnedLoans()).isEqualTo(12L);

        then(loanRepository).should()
                .countByReturnedDateIsNullAndReturnDateGreaterThanEqual(today);

        then(loanRepository).should()
                .countByReturnedDateIsNullAndReturnDateBefore(today);

        then(loanRepository).should()
                .countByReturnedDateIsNotNull();
    }
}
