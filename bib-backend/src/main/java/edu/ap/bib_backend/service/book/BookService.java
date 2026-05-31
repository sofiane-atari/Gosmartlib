package edu.ap.bib_backend.service.book;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.dto.BookCopyCampusLocationDTO;
import edu.ap.bib_backend.dto.BookCopyLocationDTO;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.dto.ReadingLevelDTO;
import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.Tag;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.enums.BookCondition;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;
import edu.ap.bib_backend.jpa.repository.TagRepository;
import edu.ap.bib_backend.jpa.repository.BookLoanRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import edu.ap.bib_backend.jpa.specification.BookSpecification;
import edu.ap.bib_backend.mapper.BookCandidateMapper;
import edu.ap.bib_backend.mapper.BookMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;
import edu.ap.bib_backend.util.IsbnUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final GenreRepository genreRepository;
    private final ReviewRepository reviewRepository;
    private final ReadingListRepository readingListRepository;
    private final BookLoanRepository bookLoanRepository;
    private final ReadingLevelRepository readingLevelRepository;
    private final CampusRepository campusRepository;
    private final TagRepository tagRepository;
    private final BookMapper bookMapper;
    private final BookCandidateMapper bookCandidateMapper;
    private final OpenLibraryClient openLibraryClient;
    private final CurrentUserService currentUserService;

    public BookService(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            GenreRepository genreRepository,
            ReviewRepository reviewRepository,
            BookLoanRepository bookLoanRepository,
            ReadingLevelRepository readingLevelRepository,
            CampusRepository campusRepository,
            TagRepository tagRepository,
            BookMapper bookMapper,
            BookCandidateMapper bookCandidateMapper,
            OpenLibraryClient openLibraryClient,
            CurrentUserService currentUserService,
            ReadingListRepository readingListRepository
    ) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.genreRepository = genreRepository;
        this.reviewRepository = reviewRepository;
        this.bookLoanRepository = bookLoanRepository;
        this.readingLevelRepository = readingLevelRepository;
        this.campusRepository = campusRepository;
        this.tagRepository = tagRepository;
        this.bookMapper = bookMapper;
        this.bookCandidateMapper = bookCandidateMapper;
        this.openLibraryClient = openLibraryClient;
        this.currentUserService = currentUserService;
        this.readingListRepository = readingListRepository;
    }

    @Transactional
    public List<BookDTO> getAllBooks(Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);

        Specification<Book> spec = Specification.where(BookSpecification.belongsToSchool(school.getId()));

        if (!currentUserService.isModerator(authentication)) {
            spec = spec.and(BookSpecification.hasNonLostCopyInSchool(school.getId()));
        }

        return bookMapper.toDTOList(bookRepository.findAll(spec));
    }

    @Transactional
    public BookDTO addCopyToBook(Long bookId, Long campusId, Authentication authentication) {
        Campus campus = resolveCampusForCurrentUserSchool(campusId, authentication);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Boek niet gevonden met id: " + bookId));
        validateBookVisibility(book, authentication);

        createAvailableCopy(book, campus);

        return bookMapper.toDTO(book);
    }

    @Transactional
    public BookDTO addBook(BookDTO dto, Long campusId, Authentication authentication) {
        Campus campus = resolveCampusForCurrentUserSchool(campusId, authentication);

        String isbn = IsbnUtils.normalizeIsbn(dto.getIsbn());
        dto.setIsbn(isbn);

        Book book = findExistingByIsbn(isbn)
                .orElseGet(() -> createBook(dto, authentication));

        createAvailableCopy(book, campus);

        return bookMapper.toDTO(book);
    }

    private Book createBook(BookDTO dto, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);
        Book book = bookMapper.toEntity(dto);
        book.setGenres(genresFromDTO(dto.getGenres()));
        book.setTags(tagsFromDTO(dto.getTags(), school.getId()));
        book.setReadingLevel(resolveReadingLevel(dto.getReadingLevel()));
        return bookRepository.save(book);
    }

    @Transactional
    public BookDTO updateBook(Long id, BookDTO dto, Authentication authentication) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Boek niet gevonden met id: " + id));

        String isbn = IsbnUtils.normalizeIsbn(dto.getIsbn());
        validateUniqueIsbn(isbn, id);

        dto.setIsbn(isbn);
        bookMapper.updateEntity(book, dto);
        book.setGenres(genresFromDTO(dto.getGenres()));
        book.setReadingLevel(resolveReadingLevel(dto.getReadingLevel()));

        return bookMapper.toDTO(bookRepository.save(book));
    }

    @Transactional
    public BookDTO getBookById(Long id, Authentication authentication) {
        return getBookById(id, authentication, null);
    }

    public BookDTO getBookById(Long id, Authentication authentication, Long schoolId) {
        School school = currentUserService.resolveSchool(schoolId, authentication);
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Boek niet gevonden met id: " + id));
        validateBookVisibility(book, authentication);

        BookDTO dto = bookMapper.toDTO(book);
        dto.setCopyLocations(buildCopyLocations(book, school.getId(), authentication));
        applySchoolCopySummary(dto, List.of(book), school.getId());

        try {
            User user = currentUserService.getCurrentUser(authentication);
            Long campusId = campusRepository.findFirstBySchoolIdOrderByIdAsc(user.getSchool().getId())
                    .map(Campus::getId)
                    .orElse(null);
            if (campusId != null) {
                readingListRepository
                        .findByNameIgnoreCaseAndUser_IdAndTargetTypeAndCampusId(
                                "Favorieten", currentUserService.extractCurrentUserId(authentication),
                                ReadingListTargetType.PERSONAL, campusId)
                        .ifPresent(list -> dto.setFavorite(
                                list.getBooks().stream().anyMatch(b -> b.getBookId().equals(id))));
            }
        } catch (Exception e) {
            log.debug("Kon favoriet status niet laden voor boek {}", id, e);
        }

        return dto;
    }

    public Book getBookById(Long id, boolean returnEntity) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + id));
        log.debug("getBookById({}): found '{}' | ISBN: {} | author: {} | genres: {} | tags: {}",
                id, book.getTitle(), book.getIsbn(), book.getAuthor(),
                book.getGenres() != null ? book.getGenres().stream().map(g -> g.getName()).toList() : "[]",
                book.getTags() != null ? book.getTags().stream().map(t -> t.getName()).toList() : "[]");
        return book;
    }

    public Book getBookByIdForCurrentCampus(Long id, Authentication authentication) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Boek niet gevonden met id: " + id));
    }

    public Integer getMaxPages() {
        Integer max = bookRepository.findMaxPages();
        return max != null ? max : 1500;
    }

    public Integer getMinPages() {
        Integer min = bookRepository.findMinPages();
        return min != null ? min : 0;
    }

    @Transactional
    public List<BookDTO> getMostReadBooks(int limit, Authentication authentication) {
        return getMostReadBooks(limit, authentication, null);
    }

    @Transactional
    public List<BookDTO> getMostReadBooks(int limit, Authentication authentication, Long schoolId) {
        Long resolvedSchoolId = currentUserService.resolveSchool(schoolId, authentication).getId();
        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);
        int safeLimit = Math.max(1, Math.min(limit, 20));

        List<Book> books = bookRepository.findMostReadBookBySchool(
                resolvedSchoolId,
                PageRequest.of(0, safeLimit)
        );

        return books.stream()
                .filter(book -> canSeeDidacticBooks || !book.isDidactic())
                .map(bookMapper::toDTO)
                .toList();
    }

    @Transactional
    public List<BookDTO> getNewestBooks(int limit, Authentication authentication) {
        return getNewestBooks(limit, authentication, null);
    }

    @Transactional
    public List<BookDTO> getNewestBooks(int limit, Authentication authentication, Long schoolId) {
        Long resolvedSchoolId = currentUserService.resolveSchool(schoolId, authentication).getId();
        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);
        int safeLimit = Math.max(1, Math.min(limit, 20));

        List<Book> books = bookRepository.findNewestBooksBySchool(
                resolvedSchoolId,
                PageRequest.of(0, safeLimit)
        );

        return books.stream()
                .filter(book -> canSeeDidacticBooks || !book.isDidactic())
                .map(bookMapper::toDTO)
                .toList();
    }

    public List<BookDTO> getRelatedBooksCarousel(Long bookId, int limit, Authentication authentication) {
        return getRelatedBooksCarousel(bookId, limit, authentication, null);
    }

    public List<BookDTO> getRelatedBooksCarousel(Long bookId, int limit, Authentication authentication, Long schoolId) {
        School school = currentUserService.resolveSchool(schoolId, authentication);
        Book sourceBook = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Boek niet gevonden met id: " + bookId));
        validateBookVisibility(sourceBook, authentication);

        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);

        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Object[]> results = bookRepository.findRelatedBooksWithScoreBySchool(
                bookId,
                school.getId(),
                PageRequest.of(0, safeLimit)
        );

        return results.stream()
                .map(row -> (Book) row[0])
                .filter(book -> canSeeDidacticBooks || !book.isDidactic())
                .map(bookMapper::toDTO)
                .toList();
    }

    public List<BookDTO> getRelatedBooksForLoan(List<Long> bookIds, int limit, String schoolDomain) {
        if (bookIds == null || bookIds.isEmpty()) {
            log.debug("getRelatedBooksForLoan: no bookIds provided, skip");
            return List.of();
        }

        log.debug("getRelatedBooksForLoan: source bookIds={}, limit={}, schoolDomain={}",
                bookIds, limit, schoolDomain);

        Map<Long, Integer> bookScores = new HashMap<>();

        for (Long bookId : bookIds) {
            List<Object[]> related = bookRepository.findRelatedBooksWithScoreBySchoolDomain(
                    bookId, schoolDomain, PageRequest.of(0, 5));

            log.debug("-> getRelatedBooksForLoan: bookId={} found {} related candidates", bookId, related.size());

            for (Object[] row : related) {
                Book relatedBook = (Book) row[0];
                int score = ((Number) row[1]).intValue();
                bookScores.merge(relatedBook.getId(), score, Integer::sum);
                log.trace("   candidate bookId={} (title='{}') score={}", relatedBook.getId(), relatedBook.getTitle(), score);
            }
        }

        if (bookScores.isEmpty()) {
            log.debug("getRelatedBooksForLoan: no candidates found across any source book");
            return List.of();
        }

        List<Long> sortedIds = bookScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, Book> bookMap = bookRepository.findAllById(sortedIds).stream()
                .collect(Collectors.toMap(Book::getId, b -> b));

        List<BookDTO> result = sortedIds.stream()
                .map(bookMap::get)
                .filter(Objects::nonNull)
                .map(bookMapper::toDTO)
                .toList();

        return result;
    }

    public Optional<Book> findExistingByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }
        List<Book> books = bookRepository.findAllByIsbn(isbn);
        return books.isEmpty() ? Optional.empty() : Optional.of(books.get(0));
    }

    @Transactional
    public List<BookCandidateDTO> searchBooksInDb(String title, String author, Integer releaseYear, Authentication authentication) {
        String cleanTitle = normalize(title);
        String cleanAuthor = normalize(author);

        boolean noUsefulFilters = cleanTitle == null &&
                cleanAuthor == null &&
                releaseYear == null;

        if (noUsefulFilters) {
            return List.of();
        }

        Specification<Book> spec = BookSpecification.withCandidateFilters(cleanTitle, cleanAuthor, releaseYear);

        return bookRepository.findAll(spec).stream()
                .limit(10)
                .map(bookCandidateMapper::toDBCandidateDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookDTO getBookByISBN(String rawIsbn, Authentication authentication) {
        return getBookByISBN(rawIsbn, authentication, null);
    }

    @Transactional
    public BookDTO getBookByISBN(String rawIsbn, Authentication authentication, Long schoolId) {
        School school = currentUserService.resolveSchool(schoolId, authentication);
        String normalizedIsbn = IsbnUtils.normalizeIsbn(rawIsbn);

        if (normalizedIsbn == null || normalizedIsbn.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ongeldig ISBN");
        }

        List<Book> existingBooks = bookRepository.findAllByIsbn(normalizedIsbn);
        if (!existingBooks.isEmpty()) {
            BookDTO dto = bookMapper.toDTO(existingBooks.get(0));
            applySchoolCopySummary(dto, existingBooks, school.getId());
            dto.setCopyLocations(existingBooks.stream()
                    .flatMap(book -> buildCopyLocations(book, school.getId(), authentication).stream())
                    .toList());
            return dto;
        }

        return openLibraryClient.fetchBookByISBN(normalizedIsbn);
    }

    private Campus resolveCampusForCurrentUserSchool(Long campusId, Authentication authentication) {
        if (campusId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "campusId is verplicht.");
        }

        School school = currentUserService.getCurrentUserSchool(authentication);
        return campusRepository.findByIdAndSchoolId(campusId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("campusId", "Campus niet gevonden."));
    }

    private void createAvailableCopy(Book book, Campus campus) {
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        bookCopyRepository.save(copy);
    }

    private List<BookCopyCampusLocationDTO> buildCopyLocations(Book book, Long schoolId, Authentication authentication) {
        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);

        return book.getBookCopies().stream()
                .filter(copy -> copy.getCampus() != null && copy.getCampus().getSchool().getId().equals(schoolId))
                .filter(copy -> canSeeDidacticBooks || !book.isDidactic())
                .filter(copy -> !isStudentOrTeacher(authentication) || copy.getCondition() != BookCondition.LOST)
                .collect(Collectors.groupingBy(
                        copy -> copy.getCampus(),
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {
                    Campus campus = entry.getKey();
                    List<BookCopy> copies = entry.getValue();
                    return BookCopyCampusLocationDTO.builder()
                            .campusId(campus.getId())
                            .campusName(campus.getName())
                            .address(campus.getAddress())
                            .postalCode(campus.getPostalCode())
                            .copies(copies.stream()
                                    .map(this::toCopyLocationDTO)
                                    .toList())
                            .build();
                })
                .filter(location -> !location.copies().isEmpty())
                .toList();
    }

    private void applySchoolCopySummary(BookDTO dto, List<Book> books, Long schoolId) {
        List<BookCopy> schoolCopies = books.stream()
                .flatMap(book -> book.getBookCopies().stream())
                .filter(copy -> copy.getCampus() != null)
                .filter(copy -> copy.getCampus().getSchool() != null)
                .filter(copy -> Objects.equals(copy.getCampus().getSchool().getId(), schoolId))
                .toList();

        dto.setTotalCopies(schoolCopies.size());
        dto.setAvailableCopies((int) schoolCopies.stream()
                .filter(copy -> copy.getAvailability() == Availability.AVAILABLE)
                .filter(copy -> copy.getCondition() != BookCondition.LOST)
                .count());
        dto.setAvailableCampuses(schoolCopies.stream()
                .filter(copy -> copy.getAvailability() == Availability.AVAILABLE)
                .filter(copy -> copy.getCondition() != BookCondition.LOST)
                .map(BookCopy::getCampus)
                .filter(Objects::nonNull)
                .map(Campus::getName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList());
    }

    private BookCopyLocationDTO toCopyLocationDTO(BookCopy copy) {
        return BookCopyLocationDTO.builder()
                .id(copy.getId())
                .availability(copy.getAvailability())
                .condition(copy.getCondition())
                .expectedReturnDate(getExpectedReturnDate(copy))
                .build();
    }

    private boolean isStudentOrTeacher(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(a -> a.equals("ROLE_STUDENT") || a.equals("ROLE_TEACHER"));
    }

    private LocalDate getExpectedReturnDate(BookCopy copy) {
        if (copy == null || copy.getId() == null) {
            return null;
        }

        return bookLoanRepository
                .findFirstByCopyIdAndReturnedDateIsNullOrderByReturnDateAsc(copy.getId())
                .map(Loan::getReturnDate)
                .orElse(null);
    }

    private void validateUniqueIsbn(String isbn, Long currentBookId) {
        if (isbn == null || isbn.isBlank()) {
            return;
        }

        findExistingByIsbn(isbn).ifPresent(existing -> {
            if (!existing.getId().equals(currentBookId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Er bestaat al een ander boek met ISBN: " + isbn
                );
            }
        });
    }

    private Set<Genre> genresFromDTO(List<GenreDTO> genresDto) {
        Set<Genre> genres = new HashSet<>();
        if (genresDto == null) {
            return genres;
        }

        for (GenreDTO genreDto : genresDto) {
            Genre genre = genreRepository.findByName(genreDto.name())
                    .orElseThrow(() -> new IllegalArgumentException("Genre bestaat niet: " + genreDto.name()));
            genres.add(genre);
        }
        return genres;
    }

    private Set<Tag> tagsFromDTO(List<TagDTO> tagsDto, Long schoolId) {
        Set<Tag> tags = new HashSet<>();
        if (tagsDto == null) {
            return tags;
        }

        for (TagDTO tagDto : tagsDto) {
            if (tagDto.id() == null) {
                continue;
            }

            Tag tag = tagRepository.findByIdInSchool(tagDto.id(), schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("tagIds", "Tag bestaat niet: " + tagDto.id()));
            tags.add(tag);
        }

        return tags;
    }

    private ReadingLevel resolveReadingLevel(ReadingLevelDTO readingLevelDto) {
        if (readingLevelDto == null || readingLevelDto.getId() == null) {
            return null;
        }

        return readingLevelRepository.findById(readingLevelDto.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reading level bestaat niet: " + readingLevelDto.getId()));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private void validateBookVisibility(Book book, Authentication authentication) {
        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);

        if (book.isDidactic() && !canSeeDidacticBooks) {
            throw new ResourceNotFoundException("id", "Boek niet gevonden.");
        }
    }
}
