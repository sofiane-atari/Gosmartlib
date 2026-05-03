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

@ExtendWith(MockitoExtension.class)
class BookSearchServiceTest {

    @Mock
    private BookRepository br;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookMapper bookMapper;

    private BookSearchService service;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        // create service with mocks
        service = new BookSearchService(br, userRepository, bookMapper);

        authentication = new UsernamePasswordAuthenticationToken("test-user", null);
        User user = new User();
        user.setId("test-user");
        School school = new School();
        school.setDomainName("school1");
        user.setSchool(school);
        // ensure the repository returns the user for the authentication used in tests
        org.mockito.Mockito.lenient().when(userRepository.findById("test-user"))
                .thenReturn(java.util.Optional.of(user));

        // Default mapping behavior for Book -> BookDTO to avoid nulls in tests that
        // assert mapped DTOs
        org.mockito.Mockito.lenient()
                .when(bookMapper.toDTO(any(Book.class)))
                .thenAnswer(invocation -> {
                    Book b = invocation.getArgument(0);

                    BookDTO d = new BookDTO();
                    d.setId(b.getId());
                    d.setTitle(b.getTitle());
                    d.setAuthor(b.getAuthor());
                    d.setReviewScore(b.getReviewScore() == null ? 0.0 : b.getReviewScore());
                    d.setReleaseYear(b.getReleaseYear());

                    return d;
                });
    }

    @Test
    @DisplayName("Given no filters, when getFilteredBooks is called, then use default paging and title ascending sort")
    void givenNoFilters_whenGetFilteredBooks_thenUseDefaultPagingAndSort() {
        Page<Book> pageResult = new PageImpl<>(List.of());
        given(br.findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), any(Pageable.class)))
                .willReturn(pageResult);

        service.getFilteredBooks(authentication, null, null, null, null, null, null, 0, 10, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(br).should().findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        var order = java.util.Objects.requireNonNull(pageable.getSort().getOrderFor("title"));
        assertThat(order).isNotNull();
        assertThat(order.isAscending()).isTrue();
    }

    @Test
    @DisplayName("Given negative page, when getFilteredBooks is called, then normalize page to zero")
    void givenNegativePage_whenGetFilteredBooks_thenNormalizePageToZero() {
        given(br.findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.getFilteredBooks(authentication, null, null, null, null, null, null, -5, 10, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(br).should().findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("Given size below one, when getFilteredBooks is called, then normalize size to one")
    void givenSizeBelowOne_whenGetFilteredBooks_thenNormalizeSizeToOne() {
        given(br.findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.getFilteredBooks(authentication, null, null, null, null, null, null, 0, 0, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(br).should().findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given size above max, when getFilteredBooks is called, then cap size at fifty")
    void givenSizeAboveMax_whenGetFilteredBooks_thenCapSizeAtFifty() {
        given(br.findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.getFilteredBooks(authentication, null, null, null, null, null, null, 0, 999, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(br).should().findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("Given minPages greater than maxPages, when getFilteredBooks is called, then throw exception")
    void givenMinPagesGreaterThanMaxPages_whenGetFilteredBooks_thenThrowException() {
        assertThatThrownBy(
                () -> service.getFilteredBooks(authentication, null, null, 300, 100, null, null, 0, 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("minPages mag niet groter zijn dan maxPages.");
    }

    @Test
    @DisplayName("Given available filter, when getFilteredBooks is called, then query repository")
    void givenAvailableFilter_whenGetFilteredBooks_thenQueryRepository() {
        given(br.findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.getFilteredBooks(authentication, null, BookAvailabilityFilter.AVAILABLE, null, null, null, null, 0, 10,
                null);

        then(br).should().findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Given AUTHOR_ZA sort, when getFilteredBooks is called, then use author descending")
    void givenAuthorZaSort_whenGetFilteredBooks_thenUseAuthorDescending() {
        given(br.findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.getFilteredBooks(authentication, null, null, null, null, null, null, 0, 10, BookSortOption.AUTHOR_ZA);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(br).should().findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), pageableCaptor.capture());

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

        given(br.findAll(org.mockito.ArgumentMatchers.<Specification<Book>>any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(book)));

        Page<BookDTO> result = service.getFilteredBooks(authentication, null, null, null, null, null, null, 0, 10,
                BookSortOption.AZ);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        var first = result.getContent().get(0);
        assertThat(first.getId()).isEqualTo(5L);
        assertThat(first.getTitle()).isEqualTo("Titel");
        assertThat(first.getReviewScore()).isEqualTo(0.0);
    }
}
