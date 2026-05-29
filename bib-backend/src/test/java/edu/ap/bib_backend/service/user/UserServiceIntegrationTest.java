package edu.ap.bib_backend.service.user;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Test
    void Given_UserData_When_CreateUser_Then_UserIsPersisted() {
        // Arrange
        String userId = "int-user-1";
        RoleEntity role = new RoleEntity();
        role.setRole(Role.STUDENT);
        roleEntityRepository.save(role);

        School school = new School();
        school.setDomainName("int-school.be");
        school.setName("Int School");
        school = schoolRepository.save(school);

        Campus campus = new Campus();
        campus.setSchool(school);
        campus.setName("Integration Test Campus");
        campus.setAddress("Test Address");
        campus.setPostalCode(1000);
        campus = campusRepository.save(campus);

        // Act
        User created = userService.createUser(userId, role, school);

        // Assert
        assertNotNull(created);
        assertTrue(userRepository.existsById(userId));
        assertEquals("int-school.be", userRepository.findById(userId).get().getSchool().getDomainName());
    }
}
