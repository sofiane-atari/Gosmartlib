package edu.ap.bib_backend.service;

import edu.ap.bib_backend.event.BookReturnedGamificationEvent;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.enums.BookCondition;
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
import org.springframework.test.util.ReflectionTestUtils;

import edu.ap.bib_backend.dto.BookCopyConditionUpdateDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.loan.LoanBookDTO;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanHistoryDTO;
import edu.ap.bib_backend.dto.loan.LoanReturnDTO;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    private final School school = new School();
    private final Campus campus = new Campus();

    @InjectMocks
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        school.setId(1L);
        campus.setId(1L);
        campus.setSchool(school);
        lenient().when(currentUserService.getCurrentUserCampus(any())).thenReturn(campus);
        lenient().when(currentUserService.getCurrentUserSchool(any())).thenReturn(school);
    }

    private BookCopy availableCopy(Long id, Book book) {
        return availableCopy(id, book, campus);
    }

    private BookCopy availableCopy(Long id, Book book, Campus copyCampus) {
        BookCopy copy = new BookCopy();
        copy.setId(id);
        copy.setBook(book);
        copy.setCampus(copyCampus);
        ReflectionTestUtils.setField(copy, "availability", Availability.AVAILABLE);
        return copy;
    }

    @Test
    void Given_ValidLoanDTO_When_Create_Then_SavesAndReturnsDto() {
        // Arrange
        String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookCopyIds(List.of(1L))
                .returnDate(returnDate)
                .build();

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        BookCopy copy = availableCopy(1L, book);
        when(bookCopyRepository.findAllById(List.of(1L))).thenReturn(List.of(copy));

        LoanDTO expectedDto = mock(LoanDTO.class);
        when(loanMapper.toDto(loan)).thenReturn(expectedDto);

        // Act
        LoanDTO result = loanService.create(inputDto, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(expectedDto, result);
        assertEquals(campus, loan.getCampus());
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
        when(loanRepository.findByCampusSchoolId(1L)).thenReturn(List.of(loan));

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
        LoanDTO inputDto = LoanDTO.builder().userId("user123").bookCopyIds(List.of(1L)).build();

        User user = new User();
        School school = new School();
        school.setDefaultLoanDurationDays(30);
        user.setSchool(school);
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        Book book = new Book();
        book.setId(1L);
        when(bookCopyRepository.findAllById(List.of(1L))).thenReturn(List.of(availableCopy(1L, book)));

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
                .bookCopyIds(List.of(1L))
                .returnDate("invalid-date-format")
                .build();

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
                .bookCopyIds(List.of(1L))
                .returnDate(pastDate)
                .build();

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
                .bookCopyIds(List.of(1L))
                .returnDate(today)
                .build();

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
                .bookCopyIds(List.of(1L, 2L))
                .returnDate(returnDate)
                .build();

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        when(bookCopyRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(availableCopy(1L, book)));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));
        assertEquals("Eén of meer boekexemplaren werden niet gevonden.", exception.getMessage());
    }

    @Test
    void Given_CopyFromDifferentCampusInSameSchool_When_Create_Then_SetsLoanCampusToCopyCampus() {
        String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookCopyIds(List.of(10L))
                .returnDate(returnDate)
                .build();

        Campus otherCampusInSchool = new Campus();
        otherCampusInSchool.setId(2L);
        otherCampusInSchool.setSchool(school);

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        when(bookCopyRepository.findAllById(List.of(10L))).thenReturn(List.of(availableCopy(10L, book, otherCampusInSchool)));
        when(loanMapper.toDto(loan)).thenReturn(LoanDTO.builder().id(1L).build());

        loanService.create(inputDto, authentication);

        assertEquals(otherCampusInSchool, loan.getCampus());
    }

    @Test
    void Given_CopiesFromMultipleCampuses_When_Create_Then_ThrowsIllegalStateException() {
        String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookCopyIds(List.of(1L, 2L))
                .returnDate(returnDate)
                .build();

        Campus otherCampusInSchool = new Campus();
        otherCampusInSchool.setId(2L);
        otherCampusInSchool.setSchool(school);

        Book book = new Book();
        book.setId(1L);
        when(bookCopyRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(availableCopy(1L, book), availableCopy(2L, book, otherCampusInSchool)));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));

        assertEquals("Alle boekexemplaren moeten uit dezelfde campus komen.", exception.getMessage());
    }

    @Test
    void Given_CopyFromDifferentSchool_When_Create_Then_ThrowsIllegalStateException() {
        String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookCopyIds(List.of(1L))
                .returnDate(returnDate)
                .build();

        School otherSchool = new School();
        otherSchool.setId(999L);
        Campus otherSchoolCampus = new Campus();
        otherSchoolCampus.setId(3L);
        otherSchoolCampus.setSchool(otherSchool);

        Book book = new Book();
        book.setId(1L);
        when(bookCopyRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(availableCopy(1L, book, otherSchoolCampus)));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));

        assertEquals("Boekexemplaar behoort tot een andere school.", exception.getMessage());
    }

    @Test
    void Given_MultipleBooksWithCopiesCount_When_Create_Then_ReservesCorrectNumberOfCopies() {
        // Arrange
        String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookCopyIds(List.of(1L, 2L, 3L, 4L, 5L))
                .returnDate(returnDate)
                .build();

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        Book b1 = new Book();
        b1.setId(1L);
        b1.setTitle("Book 1");
        Book b2 = new Book();
        b2.setId(2L);
        b2.setTitle("Book 2");
        when(bookCopyRepository.findAllById(List.of(1L, 2L, 3L, 4L, 5L)))
                .thenReturn(List.of(
                        availableCopy(1L, b1),
                        availableCopy(2L, b1),
                        availableCopy(3L, b2),
                        availableCopy(4L, b2),
                        availableCopy(5L, b2)));

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
                .bookCopyIds(List.of(1L))
                .build();

        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));
        assertEquals("Gebruiker niet gevonden.", exception.getMessage());
    }

    @Test
    void Given_NullDefaultLoanDuration_When_Create_Then_UsesDefault15Days() {
        // Arrange
        LoanDTO inputDto = LoanDTO.builder().userId("user123").bookCopyIds(List.of(1L)).build();

        User user = new User();
        School school = new School();
        school.setDefaultLoanDurationDays(null); // null duration
        user.setSchool(school);
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        Book book = new Book();
        book.setId(1L);
        when(bookCopyRepository.findAllById(List.of(1L))).thenReturn(List.of(availableCopy(1L, book)));

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
        when(loanRepository.findByCampusSchoolId(1L)).thenReturn(List.of(loan));

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
        when(loanRepository.findByCampusSchoolId(1L)).thenReturn(List.of(loan));

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
                .bookCopyIds(List.of(1L))
                .returnDate(returnDate)
                .build();

        Loan loan = new Loan();
        loan.setCopies(new ArrayList<>());
        when(loanMapper.toEntity(inputDto)).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        when(bookCopyRepository.findAllById(List.of(1L))).thenReturn(List.of(availableCopy(1L, book)));

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
    void Given_ExistingLoanNotYetReturned_When_ReturnLoan_Then_SetsReturnedDateAndReturnsDto() {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setCampus(campus);
        loan.setReturnedDate(null);
        loan.setReturnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(5));
        User user = new User();
        user.setId("user123");
        loan.setUser(user);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        LoanDTO expectedDto = LoanDTO.builder().id(1L).build();
        when(loanMapper.toDto(loan)).thenReturn(expectedDto);

        LoanDTO result = loanService.returnLoan(1L, authentication);

        assertNotNull(result);
        assertNotNull(loan.getReturnedDate());
        verify(applicationEventPublisher).publishEvent(any(BookReturnedGamificationEvent.class));
    }

    @Test
    void Given_CampusFilter_When_FindAll_Then_ReturnsCampusLoansWithinCurrentSchool() {
        Loan loan = new Loan();
        loan.setCampus(campus);
        when(loanRepository.findByCampusId(1L)).thenReturn(List.of(loan));

        LoanDTO basicDto = LoanDTO.builder().id(1L).userId("user123").build();
        when(loanMapper.toDto(loan)).thenReturn(basicDto);

        PageDTO<LoanDTO> result = loanService.findAll(0, 10, 1L, authentication);

        assertNotNull(result);
        assertEquals(1, result.data().size());
    }

    @Test
    void Given_CampusFilterOutsideSchool_When_FindAll_Then_FiltersOutLoan() {
        School otherSchool = new School();
        otherSchool.setId(999L);
        Campus otherCampus = new Campus();
        otherCampus.setId(2L);
        otherCampus.setSchool(otherSchool);
        Loan loan = new Loan();
        loan.setCampus(otherCampus);
        when(loanRepository.findByCampusId(2L)).thenReturn(List.of(loan));

        PageDTO<LoanDTO> result = loanService.findAll(0, 10, 2L, authentication);

        assertNotNull(result);
        assertTrue(result.data().isEmpty());
    }

    @Test
    void Given_ReturnedCopyCondition_When_ReturnLoan_Then_UpdatesBookCopyCondition() {
        BookCopy copy = availableCopy(1L, new Book());
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setCampus(campus);
        loan.setReturnedDate(null);
        loan.setReturnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(5));
        loan.setCopies(new ArrayList<>(List.of(copy)));
        User user = new User();
        user.setId("user123");
        loan.setUser(user);

        LoanReturnDTO request = new LoanReturnDTO(List.of(
                new BookCopyConditionUpdateDTO(1L, BookCondition.DAMAGED)));

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toDto(loan)).thenReturn(LoanDTO.builder().id(1L).build());

        loanService.returnLoan(1L, request, authentication);

        assertEquals(BookCondition.DAMAGED, copy.getCondition());
        verify(loanRepository).save(loan);
    }

    @Test
    void Given_ReturnedCopyNotInLoan_When_ReturnLoan_Then_ThrowsIllegalStateException() {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setCampus(campus);
        loan.setReturnedDate(null);
        loan.setReturnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(5));
        loan.setCopies(new ArrayList<>());

        LoanReturnDTO request = new LoanReturnDTO(List.of(
                new BookCopyConditionUpdateDTO(99L, BookCondition.LOST)));

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.returnLoan(1L, request, authentication));

        assertEquals("Boekexemplaar hoort niet bij deze lening.", exception.getMessage());
    }

    @Test
    void Given_NonExistentLoan_When_ReturnLoan_Then_ThrowsIllegalStateException() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.returnLoan(99L, authentication));

        assertEquals("Lening niet gevonden.", exception.getMessage());
    }

    @Test
    void Given_LoanFromDifferentCampus_When_ReturnLoan_Then_ThrowsIllegalStateException() {
        Campus otherCampus = new Campus();
        otherCampus.setId(999L);
        Loan loan = new Loan();
        loan.setCampus(otherCampus);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.returnLoan(1L, authentication));

        assertEquals("Lening niet gevonden in deze school.", exception.getMessage());
    }

    @Test
    void Given_AlreadyReturnedLoan_When_ReturnLoan_Then_ThrowsIllegalStateException() {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setCampus(campus);
        loan.setReturnedDate(LocalDate.now());
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.returnLoan(1L, authentication));

        assertEquals("Deze lening is al teruggebracht.", exception.getMessage());
    }

    @Test
    void Given_LoansExistForCurrentUser_When_FindHistoryForCurrentUser_Then_ReturnsHistoryWithActiveAndReturned() {
        Campus campus = new Campus();
        campus.setId(1L);
        lenient().when(currentUserService.getCurrentUserCampus(authentication)).thenReturn(campus);
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn("user123");

        Loan activeLoan = new Loan();
        activeLoan.setReturnedDate(null);
        activeLoan.setReturnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(5));
        Loan returnedLoan = new Loan();
        returnedLoan.setReturnedDate(LocalDate.now());

        when(loanRepository.findByUserIdOrderByLoanDateDesc("user123"))
                .thenReturn(List.of(activeLoan, returnedLoan));

        when(loanMapper.toDto(activeLoan)).thenReturn(LoanDTO.builder()
                .status(edu.ap.bib_backend.jpa.enums.LoanStatus.ACTIVE)
                .books(List.of(LoanBookDTO.builder().id(1L).build()))
                .build());
        when(loanMapper.toDto(returnedLoan)).thenReturn(LoanDTO.builder()
                .status(edu.ap.bib_backend.jpa.enums.LoanStatus.RETURNED)
                .books(List.of(LoanBookDTO.builder().id(2L).build()))
                .build());

        LoanHistoryDTO result = loanService.findHistoryForCurrentUser(authentication);

        assertNotNull(result);
        assertEquals(1, result.activeLoanCount());
        assertEquals(1, result.returnedLoanCount());
        assertEquals(2, result.totalLoanedBookCount());
    }

    @Test
    void Given_NoLoansForCurrentUser_When_FindHistoryForCurrentUser_Then_ReturnsEmptyHistory() {
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn("user123");
        when(loanRepository.findByUserIdOrderByLoanDateDesc("user123"))
                .thenReturn(List.of());

        LoanHistoryDTO result = loanService.findHistoryForCurrentUser(authentication);

        assertNotNull(result);
        assertEquals(0, result.activeLoanCount());
        assertEquals(0, result.returnedLoanCount());
        assertEquals(0, result.totalLoanedBookCount());
    }

    @Test
    void Given_EmptyLoanList_When_FindAll_Then_ReturnsEmptyPage() {
        when(loanRepository.findByCampusSchoolId(1L)).thenReturn(List.of());

        PageDTO<LoanDTO> result = loanService.findAll(0, 10, authentication);

        assertNotNull(result);
        assertTrue(result.data().isEmpty());
        assertEquals(0, result.meta().totalItems());
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
