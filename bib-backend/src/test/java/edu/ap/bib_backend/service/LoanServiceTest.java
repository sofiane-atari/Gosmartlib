package edu.ap.bib_backend.service;

import edu.ap.bib_backend.event.BookAvailableEvent;
import edu.ap.bib_backend.event.BookReturnedGamificationEvent;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.loan.LoanBookDTO;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanHistoryDTO;
import edu.ap.bib_backend.dto.loan.LoanStatsDTO;

import java.time.LocalDate;
import java.time.ZoneId;
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

        private final Campus campus = new Campus();

        @InjectMocks
        private LoanService loanService;

        @BeforeEach
        void setUp() {
                campus.setId(1L);
                lenient().when(currentUserService.getCurrentUserCampus(any())).thenReturn(campus);
        }

        @Test
        void Given_ValidLoanDTO_When_Create_Then_SavesAndReturnsDtos() {
                // Arrange
                String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();

                LoanDTO inputDto = LoanDTO.builder()
                                .userId("user123")
                                .bookIds(List.of(1L))
                                .returnDate(returnDate)
                                .build();

                User user = new User();
                user.setId("user123");
                when(userRepository.findById("user123")).thenReturn(Optional.of(user));

                Book book = new Book();
                book.setId(1L);
                book.setTitle("Test Book");
                when(bookService.getBookById(1L, true)).thenReturn(book);

                BookCopy copy = new BookCopy();
                copy.setId(10L);
                copy.setBook(book);
                when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L))
                                .thenReturn(List.of(copy));

                Loan savedLoan = new Loan();
                savedLoan.setId(100L);
                savedLoan.setUser(user);
                savedLoan.setCampus(campus);
                savedLoan.setReturnDate(LocalDate.parse(returnDate));
                savedLoan.getCopies().add(copy);

                when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);

                LoanDTO expectedDto = LoanDTO.builder()
                                .id(100L)
                                .userId("user123")
                                .bookIds(List.of(1L))
                                .returnDate(returnDate)
                                .build();

                when(loanMapper.toDto(savedLoan)).thenReturn(expectedDto);

                // Act
                List<LoanDTO> result = loanService.create(inputDto, authentication);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).id()).isEqualTo(100L);
                assertThat(result.get(0).userId()).isEqualTo("user123");

                verify(loanRepository).save(any(Loan.class));
                verify(loanMapper).toDto(savedLoan);
                verify(loanMapper, never()).toEntity(any());
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
                LoanDTO inputDto = LoanDTO.builder()
                                .userId("user123")
                                .bookIds(List.of(1L))
                                .build();

                School school = new School();
                school.setDefaultLoanDurationDays(30);

                User user = new User();
                user.setId("user123");
                user.setSchool(school);

                when(userRepository.findById("user123")).thenReturn(Optional.of(user));

                Book book = new Book();
                book.setId(1L);
                book.setTitle("Test Book");

                BookCopy copy = new BookCopy();
                copy.setId(10L);
                copy.setBook(book);

                when(bookService.getBookById(1L, true)).thenReturn(book);
                when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L))
                                .thenReturn(List.of(copy));

                when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
                        Loan savedLoan = invocation.getArgument(0);
                        savedLoan.setId(1L);
                        return savedLoan;
                });

                when(loanMapper.toDto(any(Loan.class))).thenAnswer(invocation -> {
                        Loan savedLoan = invocation.getArgument(0);

                        return LoanDTO.builder()
                                        .id(savedLoan.getId())
                                        .userId(savedLoan.getUser().getId())
                                        .bookIds(List.of(1L))
                                        .returnDate(savedLoan.getReturnDate().toString())
                                        .build();
                });

                // Act
                List<LoanDTO> result = loanService.create(inputDto, authentication);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).returnDate())
                                .isEqualTo(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(30).toString());

                var loanCaptor = org.mockito.ArgumentCaptor.forClass(Loan.class);
                verify(loanRepository).save(loanCaptor.capture());

                Loan savedLoan = loanCaptor.getValue();

                assertNotNull(savedLoan.getReturnDate());
                assertEquals(
                                LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(30),
                                savedLoan.getReturnDate());
        }

        @Test
        void Given_InvalidReturnDateFormat_When_Create_Then_ThrowsIllegalStateException() {
                // Arrange
                LoanDTO inputDto = LoanDTO.builder()
                                .userId("user123")
                                .bookIds(List.of(1L))
                                .returnDate("invalid-date-format")
                                .build();

                User user = new User();
                user.setId("user123");
                when(userRepository.findById("user123")).thenReturn(Optional.of(user));

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

                User user = new User();
                user.setId("user123");
                when(userRepository.findById("user123")).thenReturn(Optional.of(user));

                // Act & Assert
                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> loanService.create(inputDto, authentication));

                assertEquals(
                                "Retourdatum moet minimaal één dag in de toekomst liggen.",
                                exception.getMessage());

                verify(loanRepository, never()).save(any());
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

                User user = new User();
                user.setId("user123");
                when(userRepository.findById("user123")).thenReturn(Optional.of(user));

                // Act & Assert
                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> loanService.create(inputDto, authentication));

                assertEquals(
                                "Retourdatum moet minimaal één dag in de toekomst liggen.",
                                exception.getMessage());

                verify(loanRepository, never()).save(any());
        }

        @Test
        void Given_NotEnoughCopiesAvailable_When_Create_Then_ThrowsIllegalStateException() {
                // Arrange
                String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();

                LoanBookDTO bookDto = LoanBookDTO.builder()
                                .id(1L)
                                .copiesCount(2)
                                .build();

                LoanDTO inputDto = LoanDTO.builder()
                                .userId("user123")
                                .bookIds(List.of(1L))
                                .books(List.of(bookDto))
                                .returnDate(returnDate)
                                .build();

                User user = new User();
                user.setId("user123");
                when(userRepository.findById("user123")).thenReturn(Optional.of(user));

                Book book = new Book();
                book.setId(1L);
                book.setTitle("Test Book");
                when(bookService.getBookById(1L, true)).thenReturn(book);

                BookCopy onlyCopy = createCopy(101L, book);

                when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L))
                                .thenReturn(List.of(onlyCopy));

                // Act & Assert
                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> loanService.create(inputDto, authentication));

                assertTrue(exception.getMessage().contains("Geen beschikbaar exemplaar voor boek: Test Book"));
                verify(loanRepository, times(1)).save(any(Loan.class));
        }

        @Test
        void Given_MultipleBooksWithCopiesCount_When_Create_Then_CreatesOneLoanPerCopy() {
                // Arrange
                String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();

                LoanBookDTO book1 = LoanBookDTO.builder()
                                .id(1L)
                                .copiesCount(2)
                                .build();

                LoanBookDTO book2 = LoanBookDTO.builder()
                                .id(2L)
                                .copiesCount(3)
                                .build();

                LoanDTO inputDto = LoanDTO.builder()
                                .userId("user123")
                                .bookIds(List.of(1L, 2L))
                                .books(List.of(book1, book2))
                                .returnDate(returnDate)
                                .build();

                User user = new User();
                user.setId("user123");
                when(userRepository.findById("user123")).thenReturn(Optional.of(user));

                Book b1 = new Book();
                b1.setId(1L);
                b1.setTitle("Book 1");

                Book b2 = new Book();
                b2.setId(2L);
                b2.setTitle("Book 2");

                when(bookService.getBookById(1L, true)).thenReturn(b1);
                when(bookService.getBookById(2L, true)).thenReturn(b2);

                BookCopy copy1 = createCopy(101L, b1);
                BookCopy copy2 = createCopy(102L, b1);
                BookCopy copy3 = createCopy(201L, b2);
                BookCopy copy4 = createCopy(202L, b2);
                BookCopy copy5 = createCopy(203L, b2);

                when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L))
                                .thenReturn(List.of(copy1, copy2));

                when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(2L, 1L))
                                .thenReturn(List.of(copy3, copy4, copy5));

                when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
                        Loan loan = invocation.getArgument(0);
                        loan.setId(System.nanoTime());
                        return loan;
                });

                when(loanMapper.toDto(any(Loan.class))).thenAnswer(invocation -> {
                        Loan loan = invocation.getArgument(0);
                        Long bookId = loan.getCopies().get(0).getBook().getId();

                        return LoanDTO.builder()
                                        .id(loan.getId())
                                        .userId("user123")
                                        .bookIds(List.of(bookId))
                                        .returnDate(returnDate)
                                        .build();
                });

                // Act
                List<LoanDTO> result = loanService.create(inputDto, authentication);

                // Assert
                assertThat(result).hasSize(5);

                verify(loanRepository, times(5)).save(any(Loan.class));
                verify(loanMapper, times(5)).toDto(any(Loan.class));
        }

        @Test
        void Given_UserNotFound_When_Create_Then_ThrowsIllegalStateException() {
                // Arrange
                LoanDTO inputDto = LoanDTO.builder()
                                .userId("nonexistent")
                                .bookIds(List.of(1L))
                                .returnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString())
                                .build();

                when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

                // Act & Assert
                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> loanService.create(inputDto, authentication));

                assertEquals(
                                "Deze gebruiker bestaat nog niet in het systeem. Laat de gebruiker eerst inloggen.",
                                exception.getMessage());

                verify(loanRepository, never()).save(any());
        }

        @Test
        void Given_NullDefaultLoanDuration_When_Create_Then_UsesDefault15Days() {
                // Arrange
                LoanDTO inputDto = LoanDTO.builder()
                                .userId("user123")
                                .bookIds(List.of(1L))
                                .build();

                School school = new School();
                school.setDefaultLoanDurationDays(null);

                User user = new User();
                user.setId("user123");
                user.setSchool(school);

                when(userRepository.findById("user123")).thenReturn(Optional.of(user));

                Book book = new Book();
                book.setId(1L);
                book.setTitle("Test Book");

                BookCopy copy = new BookCopy();
                copy.setId(10L);
                copy.setBook(book);

                when(bookService.getBookById(1L, true)).thenReturn(book);
                when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L))
                                .thenReturn(List.of(copy));

                when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
                        Loan savedLoan = invocation.getArgument(0);
                        savedLoan.setId(1L);
                        return savedLoan;
                });

                when(loanMapper.toDto(any(Loan.class))).thenAnswer(invocation -> {
                        Loan savedLoan = invocation.getArgument(0);

                        return LoanDTO.builder()
                                        .id(savedLoan.getId())
                                        .userId(savedLoan.getUser().getId())
                                        .bookIds(List.of(1L))
                                        .returnDate(savedLoan.getReturnDate().toString())
                                        .build();
                });

                // Act
                List<LoanDTO> result = loanService.create(inputDto, authentication);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).returnDate())
                                .isEqualTo(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(15).toString());

                var loanCaptor = org.mockito.ArgumentCaptor.forClass(Loan.class);
                verify(loanRepository).save(loanCaptor.capture());

                Loan savedLoan = loanCaptor.getValue();

                assertNotNull(savedLoan.getReturnDate());
                assertEquals(
                                LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(15),
                                savedLoan.getReturnDate());
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
        void Given_NullToken_When_Create_Then_StillSavesLoans() {
                // Arrange
                String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();

                LoanDTO inputDto = LoanDTO.builder()
                                .userId("user123")
                                .bookIds(List.of(1L))
                                .returnDate(returnDate)
                                .build();

                User user = new User();
                user.setId("user123");
                when(userRepository.findById("user123")).thenReturn(Optional.of(user));

                Book book = new Book();
                book.setId(1L);
                book.setTitle("Test Book");
                when(bookService.getBookById(1L, true)).thenReturn(book);

                BookCopy copy = createCopy(101L, book);

                when(bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(1L, 1L))
                                .thenReturn(List.of(copy));

                Loan savedLoan = new Loan();
                savedLoan.setId(1L);
                savedLoan.setUser(user);
                savedLoan.setCampus(campus);
                savedLoan.setReturnDate(LocalDate.parse(returnDate));
                savedLoan.getCopies().add(copy);

                when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);
                when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken("user123")).thenReturn(null);

                LoanDTO expectedDto = LoanDTO.builder()
                                .id(1L)
                                .userId("user123")
                                .bookIds(List.of(1L))
                                .returnDate(returnDate)
                                .build();

                when(loanMapper.toDto(savedLoan)).thenReturn(expectedDto);

                // Act
                List<LoanDTO> result = loanService.create(inputDto, authentication);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).id()).isEqualTo(1L);
                verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
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

                assertEquals("Lening niet gevonden in deze campus.", exception.getMessage());
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
        void givenLoansExistForCurrentUser_whenFindHistoryForCurrentUser_thenReturnsHistoryWithActiveAndReturned() {
                // Arrange
                Campus campus = new Campus();
                campus.setId(1L);

                Loan activeLoan = new Loan();
                activeLoan.setReturnedDate(null);
                activeLoan.setReturnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(5));

                Loan returnedLoan = new Loan();
                returnedLoan.setReturnedDate(LocalDate.now());

                LoanDTO activeLoanDto = LoanDTO.builder()
                                .status(edu.ap.bib_backend.jpa.enums.LoanStatus.ACTIVE)
                                .books(List.of(LoanBookDTO.builder().id(1L).build()))
                                .build();

                LoanDTO returnedLoanDto = LoanDTO.builder()
                                .status(edu.ap.bib_backend.jpa.enums.LoanStatus.RETURNED)
                                .books(List.of(LoanBookDTO.builder().id(2L).build()))
                                .build();

                when(currentUserService.getCurrentUserCampus(authentication)).thenReturn(campus);
                when(currentUserService.extractCurrentUserId(authentication)).thenReturn("user123");

                when(loanRepository.findActiveHistoryLoans("user123", 1L))
                                .thenReturn(List.of(activeLoan));

                when(loanRepository.findReturnedHistoryLoans(
                                eq("user123"),
                                eq(1L),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(returnedLoan), PageRequest.of(0, 10), 1));

                when(loanRepository.countActiveHistoryLoans("user123", 1L)).thenReturn(1L);
                when(loanRepository.countReturnedHistoryLoans("user123", 1L)).thenReturn(1L);
                when(loanRepository.countLoanedBookCopiesForUserHistory("user123", 1L)).thenReturn(2L);

                when(loanMapper.toDtoList(List.of(activeLoan))).thenReturn(List.of(activeLoanDto));
                when(loanMapper.toDto(returnedLoan)).thenReturn(returnedLoanDto);

                // Act
                LoanHistoryDTO result = loanService.findHistoryForCurrentUser(0, 10, authentication);

                // Assert
                assertNotNull(result);
                assertEquals(1, result.activeLoanCount());
                assertEquals(1, result.returnedLoanCount());
                assertEquals(2, result.totalLoanedBookCount());
                assertEquals(1, result.activeLoans().size());
                assertEquals(1, result.returnedLoans().data().size());
                assertEquals(0, result.returnedLoans().meta().currentPage());
                assertEquals(10, result.returnedLoans().meta().pageSize());
                assertEquals(1, result.returnedLoans().meta().totalItems());
                assertEquals(1, result.returnedLoans().meta().totalPages());
        }

        @Test
        void givenNoLoansForCurrentUser_whenFindHistoryForCurrentUser_thenReturnsEmptyHistory() {
                // Arrange
                Campus campus = new Campus();
                campus.setId(1L);

                when(currentUserService.getCurrentUserCampus(authentication)).thenReturn(campus);
                when(currentUserService.extractCurrentUserId(authentication)).thenReturn("user123");

                when(loanRepository.findActiveHistoryLoans("user123", 1L))
                                .thenReturn(List.of());

                when(loanRepository.findReturnedHistoryLoans(
                                eq("user123"),
                                eq(1L),
                                any(Pageable.class)))
                                .thenReturn(Page.empty(PageRequest.of(0, 10)));

                when(loanRepository.countActiveHistoryLoans("user123", 1L)).thenReturn(0L);
                when(loanRepository.countReturnedHistoryLoans("user123", 1L)).thenReturn(0L);
                when(loanRepository.countLoanedBookCopiesForUserHistory("user123", 1L)).thenReturn(0L);

                when(loanMapper.toDtoList(List.of())).thenReturn(List.of());

                // Act
                LoanHistoryDTO result = loanService.findHistoryForCurrentUser(0, 10, authentication);

                // Assert
                assertNotNull(result);
                assertEquals(0, result.activeLoanCount());
                assertEquals(0, result.returnedLoanCount());
                assertEquals(0, result.totalLoanedBookCount());
                assertTrue(result.activeLoans().isEmpty());
                assertTrue(result.returnedLoans().data().isEmpty());
                assertEquals(0, result.returnedLoans().meta().totalItems());
        }

        @Test
        void Given_EmptyLoanList_When_FindAll_Then_ReturnsEmptyPage() {
                when(loanRepository.findByCampusId(1L)).thenReturn(List.of());

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

        private BookCopy createCopy(Long id, Book book) {
                BookCopy copy = new BookCopy();
                copy.setId(id);
                copy.setBook(book);
                copy.setCampus(campus);
                return copy;
        }

        @Test
        void Given_LoanWithBookCopies_When_ReturnLoan_Then_PublishesBookAvailableEventForEachDistinctBook() {
                Loan loan = new Loan();
                loan.setId(1L);
                loan.setCampus(campus);
                loan.setReturnedDate(null);
                loan.setReturnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(5));

                User user = new User();
                user.setId("user123");
                loan.setUser(user);

                Book book1 = new Book();
                book1.setId(100L);

                BookCopy copy1 = new BookCopy();
                copy1.setBook(book1);

                BookCopy copy2 = new BookCopy();
                copy2.setBook(book1);

                Book book2 = new Book();
                book2.setId(200L);

                BookCopy copy3 = new BookCopy();
                copy3.setBook(book2);

                loan.setCopies(List.of(copy1, copy2, copy3));

                when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
                when(loanRepository.save(any(Loan.class))).thenReturn(loan);

                LoanDTO expectedDto = LoanDTO.builder().id(1L).build();
                when(loanMapper.toDto(loan)).thenReturn(expectedDto);

                LoanDTO result = loanService.returnLoan(1L, authentication);

                assertNotNull(result);
                verify(applicationEventPublisher).publishEvent(any(BookReturnedGamificationEvent.class));
                verify(applicationEventPublisher, times(2)).publishEvent(any(BookAvailableEvent.class));
        }

        @Test
        void Given_LoanWithCopyHavingNullBook_When_ReturnLoan_Then_SkipsNullBookInAvailableEvent() {
                Loan loan = new Loan();
                loan.setId(1L);
                loan.setCampus(campus);
                loan.setReturnedDate(null);
                loan.setReturnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(5));

                User user = new User();
                user.setId("user123");
                loan.setUser(user);

                Book book = new Book();
                book.setId(100L);

                BookCopy copyWithBook = new BookCopy();
                copyWithBook.setBook(book);

                BookCopy copyWithNullBook = new BookCopy();

                loan.setCopies(List.of(copyWithBook, copyWithNullBook));

                when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
                when(loanRepository.save(any(Loan.class))).thenReturn(loan);

                LoanDTO expectedDto = LoanDTO.builder().id(1L).build();
                when(loanMapper.toDto(loan)).thenReturn(expectedDto);

                LoanDTO result = loanService.returnLoan(1L, authentication);

                assertNotNull(result);
                verify(applicationEventPublisher).publishEvent(any(BookAvailableEvent.class));
        }
}
