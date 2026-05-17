package edu.ap.bib_backend.service.book;

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
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;
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
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final GenreRepository genreRepository;
    private final ReviewRepository reviewRepository;
    private final BookLoanRepository bookLoanRepository;
    private final ReadingLevelRepository readingLevelRepository;
    private final CampusRepository campusRepository;
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
            BookMapper bookMapper,
            BookCandidateMapper bookCandidateMapper,
            OpenLibraryClient openLibraryClient,
            CurrentUserService currentUserService
    ) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.genreRepository = genreRepository;
        this.reviewRepository = reviewRepository;
        this.bookLoanRepository = bookLoanRepository;
        this.readingLevelRepository = readingLevelRepository;
        this.campusRepository = campusRepository;
        this.bookMapper = bookMapper;
        this.bookCandidateMapper = bookCandidateMapper;
        this.openLibraryClient = openLibraryClient;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public List<BookDTO> getAllBooks(Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);
        return bookMapper.toDTOList(bookRepository.findByCampusId(campus.getId()));
    }

    @Transactional
    public BookDTO addCopyToBook(Long bookId, Long campusId, Authentication authentication) {
        Campus campus = campusId == null
                ? currentUserService.getCurrentUserCampus(authentication)
                : resolveCampusForCurrentUserSchool(campusId, authentication);
        Book book = findBookForCopyTarget(bookId, campus, authentication);

        createAvailableCopy(book, campus);

        return bookMapper.toDTO(book);
    }

    @Transactional
    public BookDTO addBook(BookDTO dto, Authentication authentication) {
        Campus campus = resolveCampusForCurrentUserSchool(dto.getCampusId(), authentication);

        String isbn = IsbnUtils.normalizeIsbn(dto.getIsbn());
        dto.setIsbn(isbn);

        Book book = findExistingByIsbnAndCampus(isbn, campus.getId())
                .orElseGet(() -> createBook(dto, campus));

        createAvailableCopy(book, campus);

        return bookMapper.toDTO(book);
    }

    private Book createBook(BookDTO dto, Campus campus) {
        Book book = bookMapper.toEntity(dto);
        book.setCampus(campus);
        book.setGenres(genresFromDTO(dto.getGenres()));
        book.setReadingLevel(resolveReadingLevel(dto.getReadingLevel()));
        return bookRepository.save(book);
    }

    private Book findBookForCopyTarget(Long bookId, Campus targetCampus, Authentication authentication) {
        Optional<Book> targetCampusBook = bookRepository.findByIdAndCampusId(bookId, targetCampus.getId());

        if (targetCampusBook.isPresent()) {
            return targetCampusBook.get();
        }

        School school = currentUserService.getCurrentUserSchool(authentication);
        Book sourceBook = findBookInSchoolOrThrow(bookId, school.getId());
        validateBookVisibility(sourceBook, authentication);

        return findExistingTargetBook(sourceBook, targetCampus)
                .orElseGet(() -> createBookForCampus(sourceBook, targetCampus));
    }

    private Optional<Book> findExistingTargetBook(Book sourceBook, Campus targetCampus) {
        String isbn = sourceBook.getIsbn();

        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }

        return bookRepository.findByIsbnAndCampusId(isbn, targetCampus.getId());
    }

    private Book createBookForCampus(Book sourceBook, Campus targetCampus) {
        Book book = new Book();
        book.setIsbn(sourceBook.getIsbn());
        book.setTitle(sourceBook.getTitle());
        book.setAuthor(sourceBook.getAuthor());
        book.setPages(sourceBook.getPages());
        book.setImageUrl(sourceBook.getImageUrl());
        book.setDescription(sourceBook.getDescription());
        book.setReleaseYear(sourceBook.getReleaseYear());
        book.setDidactic(sourceBook.isDidactic());
        book.setGenres(new HashSet<>(sourceBook.getGenres()));
        book.setReadingLevel(sourceBook.getReadingLevel());
        book.setCampus(targetCampus);

        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long id, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);
        Book book = findBookInCampusOrThrow(id, campus.getId());

        if (bookCopyRepository.existsByBookIdAndCampusIdAndAvailability(id, campus.getId(), Availability.BORROWED)) {
            throw new IllegalStateException(
                    "Dit boek kan niet verwijderd worden omdat minstens één exemplaar momenteel is uitgeleend.");
        }

        reviewRepository.deleteByBookId(id);
        bookLoanRepository.deleteByCopiesBookId(id);
        bookCopyRepository.deleteByBookId(id);

        book.getGenres().clear();
        bookRepository.delete(book);
    }

    @Transactional
    public BookDTO updateBook(Long id, BookDTO dto, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);
        Book book = findBookInCampusOrThrow(id, campus.getId());

        String isbn = IsbnUtils.normalizeIsbn(dto.getIsbn());
        validateUniqueIsbnInCampus(isbn, id, campus.getId());

        dto.setIsbn(isbn);
        bookMapper.updateEntity(book, dto);
        book.setCampus(campus);
        book.setGenres(genresFromDTO(dto.getGenres()));
        book.setReadingLevel(resolveReadingLevel(dto.getReadingLevel()));

        return bookMapper.toDTO(bookRepository.save(book));
    }

    @Transactional
    public BookDTO getBookById(Long id, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);
        Book book = findBookInSchoolOrThrow(id, school.getId());
        validateBookVisibility(book, authentication);

        BookDTO dto = bookMapper.toDTO(book);
        dto.setCopyLocations(buildCopyLocations(book, school.getId(), authentication));
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
        Campus campus = currentUserService.getCurrentUserCampus(authentication);
        return findBookInCampusOrThrow(id, campus.getId());
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
        Campus campus = currentUserService.getCurrentUserCampus(authentication);
        Long schoolId = campus.getSchool().getId();
        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);
        int safeLimit = Math.max(1, Math.min(limit, 20));

        List<Book> books = bookRepository.findMostReadBookBySchool(
                schoolId,
                PageRequest.of(0, safeLimit)
        );

        return books.stream()
                .filter(book -> canSeeDidacticBooks || !book.isDidactic())
                .map(bookMapper::toDTO)
                .toList();
    }

    @Transactional
    public List<BookDTO> getNewestBooks(int limit, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);
        Long schoolId = campus.getSchool().getId();
        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);
        int safeLimit = Math.max(1, Math.min(limit, 20));

        List<Book> books = bookRepository.findNewestBooksBySchool(
                schoolId,
                PageRequest.of(0, safeLimit)
        );

        return books.stream()
                .filter(book -> canSeeDidacticBooks || !book.isDidactic())
                .map(bookMapper::toDTO)
                .toList();
    }

    public List<BookDTO> getRelatedBooksCarousel(Long bookId, int limit, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);
        Book sourceBook = findBookInSchoolOrThrow(bookId, school.getId());
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

        log.debug("getRelatedBooksForLoan: top {} candidate IDs: {} (scores: {})",
                limit, sortedIds,
                sortedIds.stream().map(bookScores::get).toList());

        Map<Long, Book> bookMap = bookRepository.findAllById(sortedIds).stream()
                .collect(Collectors.toMap(Book::getId, b -> b));

        List<BookDTO> result = sortedIds.stream()
                .map(bookMap::get)
                .filter(Objects::nonNull)
                .map(bookMapper::toDTO)
                .toList();

        log.debug("getRelatedBooksForLoan: returning {} recommendations: [{}]",
                result.size(),
                result.stream().map(b -> b.getId() + ":" + b.getTitle()).toList());

        return result;
    }

    public Optional<Book> findExistingByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(bookRepository.findByIsbn(isbn));
    }

    public Optional<Book> findExistingByIsbnAndCampus(String isbn, Long campusId) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }

        return bookRepository.findByIsbnAndCampusId(isbn, campusId);
    }

    @Transactional
    public List<BookCandidateDTO> searchBooksInDb(String title, String author, Integer releaseYear, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);

        String cleanTitle = normalize(title);
        String cleanAuthor = normalize(author);

        boolean noUsefulFilters = cleanTitle == null &&
                cleanAuthor == null &&
                releaseYear == null;

        if (noUsefulFilters) {
            return List.of();
        }

        Specification<Book> spec = BookSpecification.withCandidateFilters(cleanTitle, cleanAuthor, releaseYear)
                .and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("campus").get("id"), campus.getId()));

        return bookRepository.findAll(spec).stream()
                .limit(10)
                .map(bookCandidateMapper::toDBCandidateDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookDTO getBookByISBN(String rawIsbn, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);
        String normalizedIsbn = IsbnUtils.normalizeIsbn(rawIsbn);

        if (normalizedIsbn == null || normalizedIsbn.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ongeldig ISBN");
        }

        return bookRepository.findByIsbnAndCampusSchoolId(normalizedIsbn, school.getId())
                .map(bookMapper::toDTO)
                .orElseGet(() -> openLibraryClient.fetchBookByISBN(normalizedIsbn));
    }

    private Campus resolveCampusForCurrentUserSchool(Long campusId, Authentication authentication) {
        if (campusId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "campusId is verplicht.");
        }

        School school = currentUserService.getCurrentUserSchool(authentication);
        return campusRepository.findByIdAndSchoolId(campusId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("campusId", "Campus niet gevonden."));
    }

    private Book findBookInCampusOrThrow(Long id, Long campusId) {
        return bookRepository.findByIdAndCampusId(id, campusId)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Boek niet gevonden met id: " + id));
    }

    private Book findBookInSchoolOrThrow(Long id, Long schoolId) {
        return bookRepository.findByIdAndCampusSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Boek niet gevonden met id: " + id));
    }

    private void createAvailableCopy(Book book, Campus campus) {
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        bookCopyRepository.save(copy);
    }

    private List<BookCopyCampusLocationDTO> buildCopyLocations(Book book, Long schoolId, Authentication authentication) {
        List<Book> booksToShow = getSameBookAcrossSchoolCampuses(book, schoolId);
        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);

        return booksToShow.stream()
                .filter(locationBook -> canSeeDidacticBooks || !locationBook.isDidactic())
                .map(locationBook -> {
                    Campus campus = locationBook.getCampus();
                    return BookCopyCampusLocationDTO.builder()
                            .campusId(campus.getId())
                            .campusName(campus.getName())
                            .address(campus.getAddress())
                            .postalCode(campus.getPostalCode())
                            .copies(locationBook.getBookCopies().stream()
                                    .map(this::toCopyLocationDTO)
                                    .toList())
                            .build();
                })
                .filter(location -> !location.copies().isEmpty())
                .toList();
    }

    private List<Book> getSameBookAcrossSchoolCampuses(Book book, Long schoolId) {
        String isbn = book.getIsbn();

        if (isbn == null || isbn.isBlank()) {
            return List.of(book);
        }

        return bookRepository.findAllByIsbnAndCampusSchoolId(isbn, schoolId);
    }

    private BookCopyLocationDTO toCopyLocationDTO(BookCopy copy) {
        return BookCopyLocationDTO.builder()
                .id(copy.getId())
                .availability(copy.getAvailability())
                .condition(copy.getCondition())
                .expectedReturnDate(getExpectedReturnDate(copy))
                .build();
    }

    private java.time.LocalDate getExpectedReturnDate(BookCopy copy) {
        return copy.getLoans().stream()
                .filter(loan -> loan.getReturnedDate() == null)
                .map(Loan::getReturnDate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void validateUniqueIsbnInCampus(String isbn, Long currentBookId, Long campusId) {
        if (isbn == null || isbn.isBlank()) {
            return;
        }

        Optional<Book> existing = bookRepository.findByIsbnAndCampusId(isbn, campusId);
        if (existing.isPresent() && !existing.get().getId().equals(currentBookId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Er bestaat al een ander boek met ISBN in deze campus: " + isbn
            );
        }
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
