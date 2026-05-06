package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.LoanDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoanServiceIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Test
    void Given_AvailableBookAndUser_When_CreateLoan_Then_LoanIsPersisted() {
        // Arrange
        School school = new School();
        school.setDomainName("loan-school.be");
        school.setName("Loan School");
        school.setAddress("Addr");
        school.setPostalCode(2000);
        school = schoolRepository.save(school);

        User user = new User();
        user.setId("loan-user-1");
        user.setSchool(school);
        userRepository.save(user);

        Book book = new Book();
        book.setTitle("Loanable Book");
        book.setIsbn("1234567890");
        book = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        bookCopyRepository.save(copy);

        LoanDTO loanDto = LoanDTO.builder()
                .userId("loan-user-1")
                .bookIds(List.of(book.getId()))
                .build();

        // Act
        LoanDTO result = loanService.create(loanDto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("loan-user-1", result.userId());
    }
}
