package edu.ap.bib_backend.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import org.springframework.security.core.Authentication;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.mapper.BookMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class BookSearchServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private Authentication authentication;

    private BookSearchService bookSearchService;

    @BeforeEach
    void setUp() {
        bookSearchService = new BookSearchService(
                bookRepository,
                new BookMapper(),
                currentUserService
        );
    }

    @Test
    void givenNoFilters_whenGetFilteredBooks_thenUseDefaultPagingAndSort() {
        // Arrange
        Page<Book> pageResult = new PageImpl<>(List.of());
        given(currentUserService.canSeeDidacticBooks(authentication)).willReturn(true);
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pageResult);

        // Act
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

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(bookRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("title")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("title").isAscending()).isTrue();
    }

    @Test
    void givenNegativePage_whenGetFilteredBooks_thenNormalizePageToZero() {
        // Arrange
        given(currentUserService.canSeeDidacticBooks(authentication)).willReturn(true);
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // Act
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

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(bookRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    void givenSizeBelowOne_whenGetFilteredBooks_thenNormalizeSizeToOne() {
        // Arrange
        given(currentUserService.canSeeDidacticBooks(authentication)).willReturn(true);
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // Act
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

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(bookRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void givenSizeAboveMax_whenGetFilteredBooks_thenCapSizeAtFifty() {
        // Arrange
        given(currentUserService.canSeeDidacticBooks(authentication)).willReturn(true);
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // Act
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

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(bookRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void givenMinPagesGreaterThanMaxPages_whenGetFilteredBooks_thenThrowException() {
        // Arrange

        // Act

        // Assert
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
        // Arrange
        given(currentUserService.canSeeDidacticBooks(authentication)).willReturn(true);
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // Act
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

        // Assert
        then(bookRepository).should().findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void givenAuthorZaSort_whenGetFilteredBooks_thenUseAuthorDescending() {
        // Arrange
        given(currentUserService.canSeeDidacticBooks(authentication)).willReturn(true);
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // Act
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

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(bookRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("author");
        assertThat(order).isNotNull();
        assertThat(order.isDescending()).isTrue();
    }

    @Test
    void givenRepositoryReturnsBooks_whenGetFilteredBooks_thenMapPageContentToDto() {
        // Arrange
        Book book = new Book();
        book.setId(5L);
        book.setTitle("Titel");
        book.setAuthor("Auteur");
        book.setPages(200);
        book.setGenres(Set.of());
        book.setReviewScore(null);

        given(currentUserService.canSeeDidacticBooks(authentication)).willReturn(true);
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(book)));

        // Act
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

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(5L);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Titel");
        assertThat(result.getContent().get(0).getReviewScore()).isEqualTo(0.0);
    }
}