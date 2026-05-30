package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.BookCopyConditionUpdateDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanReturnDTO;
import edu.ap.bib_backend.dto.loan.LoanStatsDTO;
import edu.ap.bib_backend.event.BookReturnedGamificationEvent;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private LoanService loanService;
    private School school;
    private Campus campus;

    @BeforeEach
    void setUp() {
        school = new School();
        school.setId(1L);
        school.setDomainName("school.be");
        school.setDefaultLoanDurationDays(15);

        campus = new Campus();
        campus.setId(1L);
        campus.setSchool(school);

        loanService = new LoanService(
                loanRepository,
                loanMapper,
                bookService,
                externalUserDisplayService,
                bookCopyRepository,
                userRepository,
                smartschoolMessageService,
                smartschoolTokenRefreshService,
                currentUserService,
                applicationEventPublisher,
                "http://localhost:4200"
        );
    }

    @Test
    void Given_ValidBookCopyIds_When_Create_Then_SavesSingleLoan() {
        String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString();
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookCopyIds(List.of(10L))
                .returnDate(returnDate)
                .build();

        User user = new User();
        user.setId("user123");
        user.setSchool(school);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        BookCopy copy = availableCopy(10L, book, campus);
        LoanDTO expectedDto = LoanDTO.builder()
                .id(100L)
                .userId("user123")
                .bookCopyIds(List.of(10L))
                .returnDate(returnDate)
                .build();

        when(currentUserService.getCurrentUserSchool(authentication)).thenReturn(school);
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(bookCopyRepository.findAllById(List.of(10L))).thenReturn(List.of(copy));
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken("user123")).thenReturn(null);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        when(loanMapper.toDtoList(any())).thenReturn(List.of(expectedDto));

        List<LoanDTO> result = loanService.create(inputDto, authentication);

        assertEquals(List.of(expectedDto), result);
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void Given_CopiesFromMultipleCampuses_When_Create_Then_ThrowsIllegalStateException() {
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookCopyIds(List.of(10L, 20L))
                .returnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString())
                .build();

        Campus otherCampus = new Campus();
        otherCampus.setId(2L);
        otherCampus.setSchool(school);

        Book book = new Book();
        when(currentUserService.getCurrentUserSchool(authentication)).thenReturn(school);
        when(userRepository.findById("user123")).thenReturn(Optional.of(new User()));
        when(bookCopyRepository.findAllById(List.of(10L, 20L)))
                .thenReturn(List.of(availableCopy(10L, book, campus), availableCopy(20L, book, otherCampus)));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));

        assertEquals("Alle boekexemplaren moeten uit dezelfde campus komen.", exception.getMessage());
    }

    @Test
    void Given_CopyFromDifferentSchool_When_Create_Then_ThrowsIllegalStateException() {
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookCopyIds(List.of(10L))
                .returnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString())
                .build();

        School otherSchool = new School();
        otherSchool.setId(99L);
        Campus otherCampus = new Campus();
        otherCampus.setId(2L);
        otherCampus.setSchool(otherSchool);

        when(currentUserService.getCurrentUserSchool(authentication)).thenReturn(school);
        when(userRepository.findById("user123")).thenReturn(Optional.of(new User()));
        when(bookCopyRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(availableCopy(10L, new Book(), otherCampus)));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));

        assertEquals("Boekexemplaar behoort tot een andere school.", exception.getMessage());
    }

    @Test
    void Given_LostCopy_When_Create_Then_ThrowsIllegalStateException() {
        LoanDTO inputDto = LoanDTO.builder()
                .userId("user123")
                .bookCopyIds(List.of(10L))
                .returnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(7).toString())
                .build();

        BookCopy copy = availableCopy(10L, new Book(), campus);
        copy.setCondition(BookCondition.LOST);

        when(currentUserService.getCurrentUserSchool(authentication)).thenReturn(school);
        when(userRepository.findById("user123")).thenReturn(Optional.of(new User()));
        when(bookCopyRepository.findAllById(List.of(10L))).thenReturn(List.of(copy));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputDto, authentication));

        assertTrue(exception.getMessage().startsWith("Boekexemplaar is verloren"));
    }

    @Test
    void Given_ReturnedCopyCondition_When_ReturnLoan_Then_UpdatesBookCopyCondition() {
        BookCopy copy = availableCopy(10L, new Book(), campus);
        User user = new User();
        user.setId("user123");

        Loan loan = new Loan();
        loan.setId(1L);
        loan.setUser(user);
        loan.setCampus(campus);
        loan.setReturnDate(LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(1));
        loan.setCopy(copy);

        LoanReturnDTO request = new LoanReturnDTO(List.of(
                new BookCopyConditionUpdateDTO(10L, BookCondition.DAMAGED)
        ));
        LoanDTO dto = LoanDTO.builder().id(1L).userId("user123").build();

        when(currentUserService.getCurrentUserSchool(authentication)).thenReturn(school);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(loanMapper.toDto(loan)).thenReturn(dto);

        LoanDTO result = loanService.returnLoan(1L, request, authentication);

        assertEquals(dto, result);
        assertEquals(BookCondition.DAMAGED, copy.getCondition());
        verify(bookCopyRepository).save(copy);
        verify(applicationEventPublisher, atLeastOnce()).publishEvent(any(BookReturnedGamificationEvent.class));
    }

    @Test
    void Given_LoansExist_When_FindAll_Then_ReturnsSchoolLoans() {
        User user = new User();
        user.setOneRosterSourcedId("sourced-1");
        user.setSchool(school);
        Loan loan = new Loan();
        loan.setUser(user);
        loan.setReturnDate(LocalDate.now().plusDays(1));

        when(currentUserService.getCurrentUserSchool(authentication)).thenReturn(school);
        when(loanRepository.findByCampusSchoolId(1L)).thenReturn(List.of(loan));
        when(loanMapper.toDto(loan)).thenReturn(LoanDTO.builder().id(1L).userId("user123").build());
        when(externalUserDisplayService.getDisplayNames(eq("school.be"), anySet()))
                .thenReturn(Map.of("sourced-1", "John Doe"));

        PageDTO<LoanDTO> result = loanService.findAll(0, 10, authentication);

        assertNotNull(result);
        assertEquals(1, result.data().size());
        assertEquals("John Doe", result.data().get(0).userDisplayName());
    }

    @Test
    void Given_Stats_When_GetLoanStats_Then_ReturnsCounts() {
        LocalDate today = LocalDate.now();
        when(currentUserService.getCurrentUserSchool(authentication)).thenReturn(school);
        when(loanRepository.countByCampusSchoolIdAndReturnedDateIsNullAndReturnDateGreaterThanEqual(1L, today)).thenReturn(5L);
        when(loanRepository.countByCampusSchoolIdAndReturnedDateIsNullAndReturnDateBefore(1L, today)).thenReturn(2L);
        when(loanRepository.countByCampusSchoolIdAndReturnedDateIsNotNull(1L)).thenReturn(10L);

        LoanStatsDTO result = loanService.getLoanStats(authentication);

        assertEquals(5L, result.activeLoans());
        assertEquals(2L, result.overdueLoans());
        assertEquals(10L, result.returnedLoans());
    }

    private BookCopy availableCopy(Long id, Book book, Campus copyCampus) {
        BookCopy copy = new BookCopy();
        copy.setId(id);
        copy.setBook(book);
        copy.setCampus(copyCampus);
        copy.setCondition(BookCondition.GOOD_CONDITION);
        ReflectionTestUtils.setField(copy, "availability", Availability.AVAILABLE);
        return copy;
    }
}
