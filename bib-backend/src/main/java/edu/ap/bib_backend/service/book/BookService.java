package edu.ap.bib_backend.service.book;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.dto.ReadingLevelDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Genre;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    public BookDTO addCopyToBook(Long bookId, Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);
        Book book = findBookInCampusOrThrow(bookId, campus.getId());

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
        return bookMapper.toDTO(book);
    }

    public Book getBookById(Long id, boolean returnEntity) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + id));
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
