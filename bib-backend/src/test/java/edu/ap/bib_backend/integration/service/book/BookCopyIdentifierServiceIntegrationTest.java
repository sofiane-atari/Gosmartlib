package edu.ap.bib_backend.integration.service.book;

import edu.ap.bib_backend.BibBackendApplication;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.book.BookCopyIdentifierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BibBackendApplication.class)
@ActiveProfiles("test")
@Transactional
class BookCopyIdentifierServiceIntegrationTest {

    @Autowired
    private BookCopyIdentifierService service;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CampusRepository campusRepository;

    private Campus defaultCampus;

    @BeforeEach
    void setUp() {
        School school = new School();
        school.setDomainName("bookcopy-id-test.be");
        school.setName("BookCopy ID Test");
        school = schoolRepository.save(school);

        Campus campus = new Campus();
        campus.setName("Default Campus");
        campus.setSchool(school);
        campus.setAddress("Address");
        campus.setPostalCode(2000);
        defaultCampus = campusRepository.save(campus);
    }

    @Test
    void Given_NullCopy_When_AssignIdentifier_Then_ReturnsNull() {
        assertNull(service.assignIdentifier(null));
    }

    @Test
    void Given_CopyWithExistingIdentifier_When_AssignIdentifier_Then_ReturnsSameCopy() {
        BookCopy copy = createUnsavedCopy();
        copy.setPublicIdentifier("2800000000017");
        BookCopy saved = bookCopyRepository.save(copy);

        BookCopy result = service.assignIdentifier(saved);

        assertEquals("2800000000017", result.getPublicIdentifier());
    }

    @Test
    void Given_CopyWithoutId_When_AssignIdentifier_Then_ThrowsException() {
        BookCopy copy = new BookCopy();
        copy.setBook(createAndSaveBook());

        assertThrows(IllegalStateException.class, () -> service.assignIdentifier(copy));
    }

    @Test
    void Given_SavedCopyWithoutIdentifier_When_AssignIdentifier_Then_GeneratesEan13Identifier() {
        BookCopy copy = createUnsavedCopy();
        copy = bookCopyRepository.save(copy);
        Long copyId = copy.getId();

        BookCopy result = service.assignIdentifier(copy);

        assertNotNull(result.getPublicIdentifier());
        assertTrue(result.getPublicIdentifier().startsWith("28"));
        assertEquals(13, result.getPublicIdentifier().length());
        assertTrue(result.getPublicIdentifier().endsWith(computeCheckDigit(result.getPublicIdentifier().substring(0, 12))));
    }

    @Test
    void Given_InvalidCopyId_When_GenerateIdentifier_Then_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.generateIdentifier(-1L));
        assertThrows(IllegalArgumentException.class, () -> service.generateIdentifier(0L));
        assertThrows(IllegalArgumentException.class, () -> service.generateIdentifier(null));
        assertThrows(IllegalArgumentException.class, () -> service.generateIdentifier(10_000_000_000L));
    }

    @Test
    void Given_ValidCopyId_When_GenerateIdentifier_Then_ReturnsEan13() {
        String identifier = service.generateIdentifier(1L);
        assertNotNull(identifier);
        assertEquals(13, identifier.length());
        assertTrue(identifier.startsWith("280000000001"));
    }

    @Test
    void Given_CopiesWithoutIdentifiers_When_BackfillMissingIdentifiers_Then_AllAreAssigned() {
        BookCopy copy1 = bookCopyRepository.save(createUnsavedCopy());
        BookCopy copy2 = bookCopyRepository.save(createUnsavedCopy());
        entityManager.flush();
        entityManager.clear();

        assertNull(bookCopyRepository.findById(copy1.getId()).get().getPublicIdentifier());
        assertNull(bookCopyRepository.findById(copy2.getId()).get().getPublicIdentifier());

        service.backfillMissingIdentifiers();

        entityManager.flush();
        entityManager.clear();

        assertNotNull(bookCopyRepository.findById(copy1.getId()).get().getPublicIdentifier());
        assertNotNull(bookCopyRepository.findById(copy2.getId()).get().getPublicIdentifier());
    }

    @Test
    void Given_AllCopiesHaveIdentifiers_When_BackfillMissingIdentifiers_Then_NoChanges() {
        BookCopy copy = bookCopyRepository.save(createUnsavedCopy());
        service.assignIdentifier(copy);
        entityManager.flush();
        entityManager.clear();

        service.backfillMissingIdentifiers();

        String identifier = bookCopyRepository.findById(copy.getId()).get().getPublicIdentifier();
        assertNotNull(identifier);
    }

    private BookCopy createUnsavedCopy() {
        Book book = createAndSaveBook();
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(defaultCampus);
        return copy;
    }

    private Book createAndSaveBook() {
        Book book = new Book();
        book.setIsbn("9781234567897");
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setPages(100);
        book.setReleaseYear(2020);
        return bookRepository.save(book);
    }

    private static String computeCheckDigit(String payload) {
        int sum = 0;
        for (int i = 0; i < payload.length(); i++) {
            int digit = Character.digit(payload.charAt(i), 10);
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return String.valueOf((10 - (sum % 10)) % 10);
    }
}
