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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.jpa.repository.BookRepository;
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

    private BookSearchService bookSearchService;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        bookSearchService = new BookSearchService(
                bookRepository,
                userRepository,
                bookMapper,
                currentUserService
        );

        authentication = new UsernamePasswordAuthenticationToken("test-user", null);

        User user = new User();
        user.setId("test-user");

        School school = new School();
        school.setDomainName("school1");
        user.setSchool(school);

        lenient().when(userRepository.findById("test-user"))
                .thenReturn(java.util.Optional.of(user));

        lenient().when(currentUserService.canSeeDidacticBooks(authentication))
                .thenReturn(true);

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
        Page<Book> pageResult = new PageImpl<>(List.of());

        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(pageResult);

        bookSearchService.getFilteredBooks(
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

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(bookRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);

        Sort.Order order = pageable.getSort().getOrderFor("title");
        assertThat(order).isNotNull();
        assertThat(order.isAscending()).isTrue();
    }

    @Test
    void givenNegativePage_whenGetFilteredBooks_thenNormalizePageToZero() {
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        bookSearchService.getFilteredBooks(
                null,
                null,
                null,
                null,
                null,
                null,
                -5,
                10,
                null, 
                authentication
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(bookRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    void givenSizeBelowOne_whenGetFilteredBooks_thenNormalizeSizeToOne() {
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        bookSearchService.getFilteredBooks(
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                authentication
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(bookRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void givenSizeAboveMax_whenGetFilteredBooks_thenCapSizeAtFifty() {
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        bookSearchService.getFilteredBooks(
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                999,
                null,
                authentication
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(bookRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
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
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        bookSearchService.getFilteredBooks(
                null,
                BookAvailabilityFilter.AVAILABLE,
                null,
                null,
                null,
                null,
                0,
                10,
                null,
                authentication
        );

        then(bookRepository).should().findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void givenAuthorZaSort_whenGetFilteredBooks_thenUseAuthorDescending() {
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        bookSearchService.getFilteredBooks(
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

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(bookRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("author");

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

        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(book)));

        Page<BookDTO> result = bookSearchService.getFilteredBooks(
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
}