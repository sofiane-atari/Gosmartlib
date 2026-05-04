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
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.loan.BookLoanRepository;
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
    private final BookMapper bookMapper;
    private final BookCandidateMapper bookCandidateMapper;
    private final OpenLibraryClient openLibraryClient;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public BookService(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            GenreRepository genreRepository,
            ReviewRepository reviewRepository,
            BookLoanRepository bookLoanRepository,
            ReadingLevelRepository readingLevelRepository,
            BookMapper bookMapper,
            BookCandidateMapper bookCandidateMapper,
            OpenLibraryClient openLibraryClient,
            CurrentUserService currentUserService,
            UserRepository userRepository
    ) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.genreRepository = genreRepository;
        this.reviewRepository = reviewRepository;
        this.bookLoanRepository = bookLoanRepository;
        this.readingLevelRepository = readingLevelRepository;
        this.bookMapper = bookMapper;
        this.bookCandidateMapper = bookCandidateMapper;
        this.openLibraryClient = openLibraryClient;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<BookDTO> getAllBooks() {
        return bookMapper.toDTOList(bookRepository.findAll());
    }

    @Transactional
    public BookDTO addCopyToBook(Long bookId, Authentication authentication) {
        Book book = findBookOrThrow(bookId);
        School school = getCurrentUserSchool(authentication);

        createAvailableCopy(book, school);

        return bookMapper.toDTO(book);
    }

    @Transactional
    public BookDTO addBook(BookDTO dto, Authentication authentication) {
        String isbn = IsbnUtils.normalizeIsbn(dto.getIsbn());
        dto.setIsbn(isbn);

        Book book = findExistingByIsbn(isbn)
                .orElseGet(() -> createBook(dto));

        School school = getCurrentUserSchool(authentication);
        createAvailableCopy(book, school);

        return bookMapper.toDTO(book);
    }

    private Book createBook(BookDTO dto) {
        Book book = bookMapper.toEntity(dto);
        book.setGenres(genresFromDTO(dto.getGenres()));
        book.setReadingLevel(resolveReadingLevel(dto.getReadingLevel()));
        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = findBookOrThrow(id);

        if (bookCopyRepository.existsByBookIdAndAvailability(id, Availability.BORROWED)) {
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
    public BookDTO updateBook(Long id, BookDTO dto) {
        Book book = findBookOrThrow(id);
        String isbn = IsbnUtils.normalizeIsbn(dto.getIsbn());

        validateUniqueIsbn(isbn, id);

        dto.setIsbn(isbn);
        bookMapper.updateEntity(book, dto);
        book.setGenres(genresFromDTO(dto.getGenres()));
        book.setReadingLevel(resolveReadingLevel(dto.getReadingLevel()));

        return bookMapper.toDTO(bookRepository.save(book));
    }

    @Transactional
    public BookDTO getBookById(Long id) {
        return bookMapper.toDTO(findBookOrThrow(id));
    }

    public Book getBookById(Long id, boolean returnEntity) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + id));
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
        School school = getCurrentUserSchool(authentication);
        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);
        int safeLimit = Math.max(1, Math.min(limit, 20));

        List<Book> books = bookRepository.findMostReadBookBySchool(
                school.getDomainName(),
                PageRequest.of(0, safeLimit)
        );

        return books.stream()
                .filter(book -> canSeeDidacticBooks || !book.isDidactic())
                .map(bookMapper::toDTO)
                .toList();
    }

    @Transactional
    public List<BookDTO> getNewestBooks(int limit, Authentication authentication) {
        School school = getCurrentUserSchool(authentication);
        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);
        int safeLimit = Math.max(1, Math.min(limit, 20));

        List<Book> books = bookRepository.findNewestBooksBySchool(
                school.getDomainName(),
                PageRequest.of(0, safeLimit)
        );

        return books.stream()
                .filter(book -> canSeeDidacticBooks || !book.isDidactic())
                .map(bookMapper::toDTO)
                .toList();
    }

    public List<BookDTO> getRelatedBooksCarousel(Long bookId, int limit, Authentication authentication) {
        Book sourceBook = findBookOrThrow(bookId);
        validateBookVisibility(sourceBook, authentication);

        boolean canSeeDidacticBooks = currentUserService.canSeeDidacticBooks(authentication);

        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Object[]> results = bookRepository.findRelatedBooksWithScore(
                bookId,
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

    @Transactional
    public List<BookCandidateDTO> searchBooksInDb(String title, String author, Integer releaseYear) {
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
    public BookDTO getBookByISBN(String rawIsbn) {
        String normalizedIsbn = IsbnUtils.normalizeIsbn(rawIsbn);

        if (normalizedIsbn == null || normalizedIsbn.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ongeldig ISBN");
        }

        return findExistingByIsbn(normalizedIsbn)
                .map(bookMapper::toDTO)
                .orElseGet(() -> openLibraryClient.fetchBookByISBN(normalizedIsbn));
    }

    private Book findBookOrThrow(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Boek niet gevonden met id: " + id));
    }

    private void createAvailableCopy(Book book, School school) {
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setSchool(school);
        copy.setAvailability(Availability.AVAILABLE);
        bookCopyRepository.save(copy);
    }

    private void validateUniqueIsbn(String isbn, Long currentBookId) {
        if (isbn == null || isbn.isBlank()) {
            return;
        }

        Book existing = bookRepository.findByIsbn(isbn);
        if (existing != null && !existing.getId().equals(currentBookId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Er bestaat al een ander boek met ISBN: " + isbn
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

    private School getCurrentUserSchool(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Geen geauthenticeerde gebruiker gevonden.");
        }

        User user = userRepository.findById(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Gebruiker niet gevonden: " + authentication.getName()
                ));

        if (user.getSchool() == null) {
            throw new IllegalStateException("Gebruiker heeft geen school gekoppeld.");
        }

        return user.getSchool();
    }
}