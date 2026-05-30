package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
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

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private CurrentUserService currentUserService;

    @Test
    void Given_AvailableBookAndUser_When_CreateLoan_Then_LoanIsPersisted() {
        // Arrange
        School school = new School();
        school.setDomainName("loan-school.be");
        school.setName("Loan School");
        school = schoolRepository.save(school);

        Campus campus = new Campus();
        campus.setName("Test Campus");
        campus.setSchool(school);
        campus.setAddress("Test Address");
        campus.setPostalCode(2000);
        campus = campusRepository.save(campus);

        RoleEntity role = new RoleEntity();
        role.setRole(Role.STUDENT);
        roleEntityRepository.save(role);

        User user = new User();
        user.setId("loan-user-1");
        user.setRole(role);
        user.setSchool(school);
        userRepository.save(user);

        Book book = new Book();
        book.setTitle("Loanable Book");
        book.setIsbn("1234567890");
        book.setCampus(campus);
        book = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        copy = bookCopyRepository.save(copy);
        Long copyId = copy.getId();
        entityManager.flush();
        entityManager.clear();

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(currentUserService.getCurrentUserSchool(authentication)).thenReturn(school);

        LoanDTO loanDto = LoanDTO.builder()
                .userId("loan-user-1")
                .bookCopyIds(List.of(copyId))
                .build();

        // Act
        List<LoanDTO> result = loanService.create(loanDto, authentication);
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).id());
        assertEquals("loan-user-1", result.get(0).userId());
        assertEquals(List.of(copyId), result.get(0).bookCopyIds());
    }
}
