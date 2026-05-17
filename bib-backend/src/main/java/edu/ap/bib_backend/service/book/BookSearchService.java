package edu.ap.bib_backend.service.book;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.specification.BookSpecification;
import edu.ap.bib_backend.mapper.BookMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookSearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MIN_PAGE_SIZE = 1;

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final CurrentUserService currentUserService;

    public BookSearchService(
            BookRepository bookRepository,
            BookMapper bookMapper,
            CurrentUserService currentUserService
    ) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<BookDTO> getFilteredBooks(
            String searchTerm,
            BookAvailabilityFilter availabilityFilter,
            Integer minPages,
            Integer maxPages,
            Long readingLevelId,
            String genreName,
            int page,
            int size,
            BookSortOption sortBy,
            Authentication authentication
    ) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);
        Long schoolId = campus.getSchool().getId();

        String normalizedSearchTerm = normalizeSearchTerm(searchTerm);
        validatePageRange(minPages, maxPages);

        int validatedPage = Math.max(page, 0);
        int validatedSize = Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);

        Sort sort = mapSort(sortBy);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, sort);

        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);

        // School-brede boeken in plaats van campus-specifieke
        Specification<Book> specification = Specification
                .where(BookSpecification.belongsToSchool(schoolId))
                .and(BookSpecification.withFilters(
                        normalizedSearchTerm,
                        minPages,
                        maxPages,
                        readingLevelId,
                        genreName
                ))
                .and(BookSpecification.visibleForUser(canSeeDidacticBooks));

        if (availabilityFilter == BookAvailabilityFilter.AVAILABLE) {
            specification = specification.and(BookSpecification.hasAnyAvailableCopy());
        }

        return bookRepository.findAll(specification, pageable)
                .map(bookMapper::toDTO);
    }

    private String normalizeSearchTerm(String searchTerm) {
        return (searchTerm == null || searchTerm.isBlank()) ? null : searchTerm.trim();
    }

    private void validatePageRange(Integer minPages, Integer maxPages) {
        if (minPages != null && maxPages != null && minPages > maxPages) {
            throw new IllegalArgumentException("minPages mag niet groter zijn dan maxPages.");
        }
    }

private Sort mapSort(BookSortOption sortBy) {
        if (sortBy == null) {
            return Sort.by("title").ascending();
        }

        return switch (sortBy) {
            case AZ -> Sort.by("title").ascending();
            case ZA -> Sort.by("title").descending();
            case NEWEST -> Sort.by("releaseYear").descending();
            case RATING -> Sort.by("reviewScore").descending();
            case AUTHOR_AZ -> Sort.by("author").ascending();
            case AUTHOR_ZA -> Sort.by("author").descending();
        };
    }
}
