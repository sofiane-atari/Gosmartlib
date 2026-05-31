package edu.ap.bib_backend.integration.service;

import edu.ap.bib_backend.BibBackendApplication;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.BookCopyService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = BibBackendApplication.class)
@ActiveProfiles("test")
@Transactional
class BookCopyServiceIntegrationTest {

    @Autowired
    private BookCopyService bookCopyService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private CurrentUserService currentUserService;

    @Test
    void Given_BookWithCopiesInSchool_When_GetCopiesByBookIdForSchool_Then_ReturnsCopies() {
        School school = createSchool("bookcopy-test-1.be");
        Book book = createBook("Copy Service Book 1");
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        List<BookCopy> result = bookCopyService.getCopiesByBookIdForSchool(book.getId(), auth);

        assertEquals(2, result.size());
    }

    @Test
    void Given_BookWithCopiesInDifferentSchool_When_GetCopiesByBookIdForSchool_Then_ReturnsOnlySchoolCopies() {
        School school1 = createSchool("bookcopy-test-2a.be");
        School school2 = createSchool("bookcopy-test-2b.be");
        Campus campus1 = createCampus("Campus Own", school1);
        Campus campus2 = createCampus("Campus Other", school2);
        Book book = createBook("Copy Service Book 2");
        createCopy(book, campus1);
        createCopy(book, campus2);
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school1);

        List<BookCopy> result = bookCopyService.getCopiesByBookIdForSchool(book.getId(), auth);

        assertEquals(1, result.size());
    }

    @Test
    void Given_ExistingCopy_When_DeleteCopy_Then_CopyAndLoansAreDeleted() {
        School school = createSchool("bookcopy-test-3.be");
        Campus campus = createCampus("Campus Del", school);
        Book book = createBook("Copy Service Book 3");
        BookCopy copy = createCopy(book, campus);
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(auth.getName()).thenReturn("test-user");

        bookCopyService.deleteCopy(copy.getId(), auth);

        assertFalse(bookCopyRepository.findById(copy.getId()).isPresent());
    }

    @Test
    void Given_CopyFromOtherSchool_When_DeleteCopy_Then_ThrowsException() {
        School school1 = createSchool("bookcopy-test-4a.be");
        School school2 = createSchool("bookcopy-test-4b.be");
        Campus campus1 = createCampus("Campus A", school1);
        Book book = createBook("Copy Service Book 4");
        BookCopy copy = createCopy(book, campus1);

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school2);

        assertThrows(IllegalArgumentException.class, () -> bookCopyService.deleteCopy(copy.getId(), auth));
    }

    @Test
    void Given_NonExistentCopy_When_DeleteCopy_Then_ThrowsException() {
        Authentication auth = mockAuth();
        School school = createSchool("bookcopy-test-5.be");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        assertThrows(IllegalArgumentException.class, () -> bookCopyService.deleteCopy(99999L, auth));
    }

    @Test
    void Given_BookWithCopiesInSchool_When_DeleteAllCopiesByBookIdForSchool_Then_AllCopiesDeleted() {
        School school = createSchool("bookcopy-test-6.be");
        Campus campus = createCampus("Campus Bulk", school);
        Book book = createBook("Copy Service Book 6");
        createCopy(book, campus);
        createCopy(book, campus);
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(auth.getName()).thenReturn("test-user");

        bookCopyService.deleteAllCopiesByBookIdForSchool(book.getId(), auth);

        List<BookCopy> remaining = bookCopyRepository.findAllByBookIdAndCampusSchoolId(book.getId(), school.getId());
        assertTrue(remaining.isEmpty());
    }

    @Test
    void Given_BookWithNoCopiesInSchool_When_DeleteAllCopiesByBookIdForSchool_Then_ThrowsException() {
        School school = createSchool("bookcopy-test-7.be");
        Book book = createBook("Copy Service Book 7");

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        assertThrows(IllegalArgumentException.class,
                () -> bookCopyService.deleteAllCopiesByBookIdForSchool(book.getId(), auth));
    }

    private School createSchool(String domain) {
        School school = new School();
        school.setDomainName(domain);
        school.setName("Test School " + domain);
        return schoolRepository.save(school);
    }

    private Campus createCampus(String name, School school) {
        Campus campus = new Campus();
        campus.setName(name);
        campus.setSchool(school);
        campus.setAddress("Address " + name);
        campus.setPostalCode(2000);
        return campusRepository.save(campus);
    }

    private Book createBook(String title) {
        Book book = new Book();
        book.setIsbn("978" + title.hashCode());
        book.setTitle(title);
        book.setAuthor("Author " + title);
        book.setPages(100);
        book.setReleaseYear(2024);
        return bookRepository.save(book);
    }

    private BookCopy createCopy(Book book, Campus campus) {
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        return bookCopyRepository.save(copy);
    }

    private Authentication mockAuth() {
        return org.mockito.Mockito.mock(Authentication.class);
    }
}
