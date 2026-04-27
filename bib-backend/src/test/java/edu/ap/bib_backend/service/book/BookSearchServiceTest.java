package edu.ap.bib_backend.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.mapper.BookMapper;

@ExtendWith(MockitoExtension.class)
class BookSearchServiceTest {

    @Mock
    private BookRepository br;

    private BookSearchService service;

    @BeforeEach
    void setUp() {
//        service = new BookSearchService(br, new BookMapper());
    }

    @Test
    @DisplayName("Given no filters, when getFilteredBooks is called, then use default paging and title ascending sort")
    void givenNoFilters_whenGetFilteredBooks_thenUseDefaultPagingAndSort() {
        Page<Book> pageResult = new PageImpl<>(List.of());
        given(br.findAll(any(Specification.class), any(Pageable.class))).willReturn(pageResult);

        service.getFilteredBooks(null, null, null, null,null, null, 0, 10, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(br).should().findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("title")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("title").isAscending()).isTrue();
    }

    @Test
    @DisplayName("Given negative page, when getFilteredBooks is called, then normalize page to zero")
    void givenNegativePage_whenGetFilteredBooks_thenNormalizePageToZero() {
        given(br.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.getFilteredBooks(null, null, null, null, null, null, -5, 10, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(br).should().findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("Given size below one, when getFilteredBooks is called, then normalize size to one")
    void givenSizeBelowOne_whenGetFilteredBooks_thenNormalizeSizeToOne() {
        given(br.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.getFilteredBooks(null, null, null, null, null, null,  0, 0, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(br).should().findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given size above max, when getFilteredBooks is called, then cap size at fifty")
    void givenSizeAboveMax_whenGetFilteredBooks_thenCapSizeAtFifty() {
        given(br.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.getFilteredBooks(null, null, null, null, null, null,  0, 999, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(br).should().findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("Given minPages greater than maxPages, when getFilteredBooks is called, then throw exception")
    void givenMinPagesGreaterThanMaxPages_whenGetFilteredBooks_thenThrowException() {
        assertThatThrownBy(() -> service.getFilteredBooks(null, null, 300, 100, null, null,  0, 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("minPages mag niet groter zijn dan maxPages.");
    }

    @Test
    @DisplayName("Given available filter, when getFilteredBooks is called, then query repository")
    void givenAvailableFilter_whenGetFilteredBooks_thenQueryRepository() {
        given(br.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.getFilteredBooks(null, BookAvailabilityFilter.AVAILABLE, null, null, null, null,  0, 10, null);

        then(br).should().findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Given AUTHOR_ZA sort, when getFilteredBooks is called, then use author descending")
    void givenAuthorZaSort_whenGetFilteredBooks_thenUseAuthorDescending() {
        given(br.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.getFilteredBooks(null, null, null, null, null, null,  0, 10, BookSortOption.AUTHOR_ZA);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(br).should().findAll(any(Specification.class), pageableCaptor.capture());

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("author");
        assertThat(order).isNotNull();
        assertThat(order.isDescending()).isTrue();
    }

    @Test
    @DisplayName("Given repository returns books, when getFilteredBooks is called, then map page content to dto")
    void givenRepositoryReturnsBooks_whenGetFilteredBooks_thenMapPageContentToDto() {
        Book book = new Book();
        book.setId(5L);
        book.setTitle("Titel");
        book.setAuthor("Auteur");
        book.setPages(200);
        book.setGenres(Set.of());
        book.setReviewScore(null);

        given(br.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(book)));

        Page<BookDTO> result = service.getFilteredBooks(null, null, null, null, null, null,  0, 10, BookSortOption.AZ);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(5L);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Titel");
        assertThat(result.getContent().get(0).getReviewScore()).isEqualTo(0.0);
    }
}