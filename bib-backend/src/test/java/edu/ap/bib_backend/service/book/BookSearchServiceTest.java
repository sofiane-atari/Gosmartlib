package edu.ap.bib_backend.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.enums.BookCondition;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.BookMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class BookSearchServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ReadingListRepository readingListRepository;

    @Mock
    private CampusRepository campusRepository;

    private BookSearchService bookSearchService;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        bookSearchService = new BookSearchService(
                bookRepository,
                bookMapper,
                currentUserService,
                readingListRepository,
                campusRepository
        );

        authentication = new UsernamePasswordAuthenticationToken("test-user", null);

        User user = new User();
        user.setId("test-user");

        School school = new School();
        school.setId(1L);
        school.setDomainName("school1");
        user.setSchool(school);

        Campus userCampus = new Campus();
        userCampus.setId(1L);
        userCampus.setSchool(school);
        user.setCampus(userCampus);

        lenient().when(userRepository.findById("test-user"))
                .thenReturn(java.util.Optional.of(user));

        lenient().when(currentUserService.getCurrentUser(authentication))
                .thenReturn(user);
        lenient().when(currentUserService.canSeeDidacticBooks(authentication))
                .thenReturn(true);

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setSchool(school);
        lenient().when(currentUserService.getCurrentUserCampus(authentication))
                .thenReturn(campus);
        lenient().when(currentUserService.getCurrentUserSchool(authentication))
                .thenReturn(school);

        lenient().when(bookMapper.toDTO(any(Book.class)))
                .thenAnswer(invocation -> {
                    Book book = invocation.getArgument(0);

                    BookDTO dto = new BookDTO();
                    dto.setId(book.getId());
                    dto.setTitle(book.getTitle());
                    dto.setAuthor(book.getAuthor());
                    dto.setReviewScore(book.getReviewScore() == null ? 0.0 : book.getReviewScore());
                    dto.setReleaseYear(book.getReleaseYear());

                    return dto;
                });
    }

    @Test
    void givenNoFilters_whenGetFilteredBooks_thenUseDefaultPagingAndSort() {
        given(bookRepository.findAll(any(Specification.class), any(Sort.class)))
                .willReturn(List.of());

        bookSearchService.getFilteredBooks(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                null,
                authentication
        );

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        then(bookRepository).should().findAll(any(Specification.class), sortCaptor.capture());

        Sort.Order order = sortCaptor.getValue().getOrderFor("title");
        assertThat(order).isNotNull();
        assertThat(order.isAscending()).isTrue();
    }

    @Test
    void givenNegativePage_whenGetFilteredBooks_thenNormalizePageToZero() {
        given(bookRepository.findAll(any(Specification.class), any(Sort.class)))
                .willReturn(List.of());

        Page<BookDTO> result = bookSearchService.getFilteredBooks(
                null, null, null, null, null, null, null, -5, 10, null, authentication);

        assertThat(result.getPageable().getPageNumber()).isEqualTo(0);
    }

    @Test
    void givenSizeBelowOne_whenGetFilteredBooks_thenNormalizeSizeToOne() {
        given(bookRepository.findAll(any(Specification.class), any(Sort.class)))
                .willReturn(List.of());

        Page<BookDTO> result = bookSearchService.getFilteredBooks(
                null, null, null, null, null, null, null, 0, 0, null, authentication);

        assertThat(result.getPageable().getPageSize()).isEqualTo(1);
    }

    @Test
    void givenSizeAboveMax_whenGetFilteredBooks_thenCapSizeAtFifty() {
        given(bookRepository.findAll(any(Specification.class), any(Sort.class)))
                .willReturn(List.of());

        Page<BookDTO> result = bookSearchService.getFilteredBooks(
                null, null, null, null, null, null, null, 0, 999, null, authentication);

        assertThat(result.getPageable().getPageSize()).isEqualTo(50);
    }

    @Test
    void givenMinPagesGreaterThanMaxPages_whenGetFilteredBooks_thenThrowException() {
        assertThatThrownBy(() -> bookSearchService.getFilteredBooks(
                null,
                null,
                300,
                100,
                null,
                null,
                null,
                0,
                10,
                null,
                authentication
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("minPages mag niet groter zijn dan maxPages.");
    }

    @Test
    void givenAvailableFilter_whenGetFilteredBooks_thenQueryRepository() {
        given(bookRepository.findAll(any(Specification.class), any(Sort.class)))
                .willReturn(List.of());

        bookSearchService.getFilteredBooks(
                null,
                BookAvailabilityFilter.AVAILABLE,
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                null,
                authentication
        );

        then(bookRepository).should().findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void givenAuthorZaSort_whenGetFilteredBooks_thenUseAuthorDescending() {
        given(bookRepository.findAll(any(Specification.class), any(Sort.class)))
                .willReturn(List.of());

        bookSearchService.getFilteredBooks(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                BookSortOption.AUTHOR_ZA,
                authentication
        );

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        then(bookRepository).should().findAll(any(Specification.class), sortCaptor.capture());

        Sort.Order order = sortCaptor.getValue().getOrderFor("author");

        assertThat(order).isNotNull();
        assertThat(order.isDescending()).isTrue();
    }

    @Test
    void givenRepositoryReturnsBooks_whenGetFilteredBooks_thenMapPageContentToDto() {
        Book book = new Book();
        book.setId(5L);
        book.setTitle("Titel");
        book.setAuthor("Auteur");
        book.setPages(200);
        book.setGenres(Set.of());
        book.setReviewScore(null);

        given(bookRepository.findAll(any(Specification.class), any(Sort.class)))
                .willReturn(List.of(book));

        Page<BookDTO> result = bookSearchService.getFilteredBooks(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                BookSortOption.AZ,
                authentication
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);

        BookDTO first = result.getContent().get(0);

        assertThat(first.getId()).isEqualTo(5L);
        assertThat(first.getTitle()).isEqualTo("Titel");
        assertThat(first.getReviewScore()).isEqualTo(0.0);
    }

    @Test
    void givenSameIsbnInMultipleCampuses_whenGetFilteredBooks_thenReturnOneBookWithAllAvailableCampuses() {
        School school = new School();
        school.setId(1L);

        Campus campusA = new Campus();
        campusA.setId(1L);
        campusA.setName("Campus A");
        campusA.setSchool(school);

        Campus campusB = new Campus();
        campusB.setId(2L);
        campusB.setName("Campus B");
        campusB.setSchool(school);

        Book bookA = createBook(1L, "9781234567890", campusA);
        Book bookB = createBook(2L, "9781234567890", campusB);
        bookA.setBookCopies(List.of(createAvailableCopy(10L, bookA, campusA)));
        bookB.setBookCopies(List.of(createAvailableCopy(20L, bookB, campusB)));

        given(bookRepository.findAll(any(Specification.class), any(Sort.class)))
                .willReturn(List.of(bookA, bookB));

        Page<BookDTO> result = bookSearchService.getFilteredBooks(
                null,
                BookAvailabilityFilter.AVAILABLE,
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                BookSortOption.AZ,
                authentication
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAvailableCampuses())
                .containsExactly("Campus A", "Campus B");
    }

    private Book createBook(Long id, String isbn, Campus campus) {
        Book book = new Book();
        book.setId(id);
        book.setIsbn(isbn);
        book.setTitle("Titel");
        book.setAuthor("Auteur");
        book.setPages(200);
        book.setGenres(Set.of());
        book.setCampus(campus);
        return book;
    }

    private BookCopy createAvailableCopy(Long id, Book book, Campus campus) {
        BookCopy copy = new BookCopy();
        copy.setId(id);
        copy.setBook(book);
        copy.setCampus(campus);
        copy.setCondition(BookCondition.GOOD_CONDITION);
        org.springframework.test.util.ReflectionTestUtils.setField(copy, "availability", Availability.AVAILABLE);
        return copy;
    }
}
