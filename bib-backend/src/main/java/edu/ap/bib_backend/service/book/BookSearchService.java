package edu.ap.bib_backend.service.book;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import lombok.extern.slf4j.Slf4j;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.enums.BookCondition;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BookSearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MIN_PAGE_SIZE = 1;

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final CurrentUserService currentUserService;
    private final ReadingListRepository readingListRepository;
    private final CampusRepository campusRepository;

    public BookSearchService(
            BookRepository bookRepository,
            BookMapper bookMapper,
            CurrentUserService currentUserService,
            ReadingListRepository readingListRepository,
            CampusRepository campusRepository
    ) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
        this.currentUserService = currentUserService;
        this.readingListRepository = readingListRepository;
        this.campusRepository = campusRepository;
    }

    @Transactional(readOnly = true)
    public Page<BookDTO> getFilteredBooks(
            String searchTerm,
            BookAvailabilityFilter availabilityFilter,
            Integer minPages,
            Integer maxPages,
            Long readingLevelId,
            String genreName,
            Boolean didactic,
            int page,
            int size,
            BookSortOption sortBy,
            Authentication authentication
    ) {
        School school = currentUserService.getCurrentUserSchool(authentication);
        Long schoolId = school.getId();

        String normalizedSearchTerm = normalizeSearchTerm(searchTerm);
        validatePageRange(minPages, maxPages);

        int validatedPage = Math.max(page, 0);
        int validatedSize = Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);

        Sort sort = mapSort(sortBy);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, sort);

        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);
        boolean isModerator = currentUserService.isModerator(authentication);

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

        if (!isModerator) {
            specification = specification.and(BookSpecification.hasNonLostCopyInSchool(schoolId));
        }

        if (availabilityFilter == BookAvailabilityFilter.AVAILABLE) {
            specification = specification.and(BookSpecification.hasAnyAvailableCopy());
        }

        if (Boolean.TRUE.equals(didactic)) {
            specification = specification.and(BookSpecification.onlyDidactic());
        }

        Set<Long> favoriteBookIds = resolveFavoriteBookIds(authentication);

        List<BookDTO> uniqueBooks = bookRepository.findAll(specification, sort).stream()
                .collect(
                        LinkedHashMap<String, List<Book>>::new,
                        (groups, book) -> groups.computeIfAbsent(getCatalogKey(book), ignored -> new ArrayList<>()).add(book),
                        Map::putAll
                )
                .values()
                .stream()
                .map(books -> toCatalogDto(books, favoriteBookIds))
                .toList();

        int start = Math.min(validatedPage * validatedSize, uniqueBooks.size());
        int end = Math.min(start + validatedSize, uniqueBooks.size());

        return new PageImpl<>(uniqueBooks.subList(start, end), pageable, uniqueBooks.size());
    }

    private BookDTO toCatalogDto(List<Book> books, Set<Long> favoriteBookIds) {
        BookDTO dto = bookMapper.toDTO(books.get(0));
        dto.setTotalCopies(books.stream()
                .map(Book::getTotalCopies)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());
        dto.setAvailableCopies((int) books.stream()
                .flatMap(book -> book.getBookCopies().stream())
                .filter(this::isAvailableCopy)
                .count());
        dto.setAvailableCampuses(books.stream()
                .flatMap(book -> book.getBookCopies().stream())
                .filter(this::isAvailableCopy)
                .map(BookCopy::getCampus)
                .filter(Objects::nonNull)
                .map(Campus::getName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList());

        if (!favoriteBookIds.isEmpty()) {
            dto.setFavorite(books.stream().anyMatch(b -> favoriteBookIds.contains(b.getId())));
        }

        return dto;
    }

    private boolean isAvailableCopy(BookCopy copy) {
        return copy.getAvailability() == Availability.AVAILABLE && copy.getCondition() != BookCondition.LOST;
    }

    private Set<Long> resolveFavoriteBookIds(Authentication authentication) {
        try {
            String userId = currentUserService.extractCurrentUserId(authentication);
            User user = currentUserService.getCurrentUser(authentication);
            Long campusId = campusRepository.findFirstBySchoolIdOrderByIdAsc(user.getSchool().getId())
                    .map(Campus::getId)
                    .orElse(null);
            if (campusId == null) return Set.of();

            return readingListRepository
                    .findByNameIgnoreCaseAndUser_IdAndTargetTypeAndCampusId(
                            "Favorieten", userId, ReadingListTargetType.PERSONAL, campusId)
                    .map(list -> list.getBooks().stream()
                            .map(b -> b.getBookId())
                            .collect(Collectors.toSet()))
                    .orElse(Set.of());
        } catch (Exception e) {
            log.debug("Kon favorieten niet laden voor catalogus", e);
            return Set.of();
        }
    }

    private String getCatalogKey(Book book) {
        if (book.getIsbn() != null && !book.getIsbn().isBlank()) {
            return "isbn:" + book.getIsbn().trim().toLowerCase(Locale.ROOT);
        }

        return String.join("|",
                "book",
                normalizeKeyPart(book.getTitle()),
                normalizeKeyPart(book.getAuthor()),
                String.valueOf(book.getReleaseYear())
        );
    }

    private String normalizeKeyPart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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
