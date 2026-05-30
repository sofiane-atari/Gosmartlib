package edu.ap.bib_backend.repository;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Test
    void Given_SchoolId_When_FindBySchoolId_Then_ReturnsMatchingUsers() {
        // Arrange
        School school = new School();
        school.setDomainName("school.test");
        school.setName("School");
        school = schoolRepository.save(school);

        RoleEntity teacherRole = new RoleEntity();
        teacherRole.setRole(Role.TEACHER);
        roleEntityRepository.save(teacherRole);

        Campus campus = new Campus();
        campus.setName("Campus A");
        campus.setSchool(school);
        campus.setAddress("Street 1");
        campus.setPostalCode(1000);
        campus = campusRepository.save(campus);

        User user = new User();
        user.setId("user-1");
        user.setSchool(school);
        user.setRole(teacherRole);
        userRepository.save(user);

        // Act
        List<User> result = userRepository.findBySchoolId(school.getId());

        // Assert
        assertThat(result).extracting(User::getId).contains("user-1");
    }

    @Test
    void Given_SchoolIdAndRole_When_FindBySchoolIdAndRoleRole_Then_ReturnsMatchingUsers() {
        // Arrange
        School school = new School();
        school.setDomainName("school.test");
        school.setName("School");
        school = schoolRepository.save(school);

        RoleEntity teacherRole = new RoleEntity();
        teacherRole.setRole(Role.TEACHER);
        roleEntityRepository.save(teacherRole);

        Campus campus = new Campus();
        campus.setName("Campus B");
        campus.setSchool(school);
        campus.setAddress("Street 2");
        campus.setPostalCode(2000);
        campus = campusRepository.save(campus);

        User user = new User();
        user.setId("user-2");
        user.setSchool(school);
        user.setRole(teacherRole);
        userRepository.save(user);

        // Act
        List<User> result = userRepository.findBySchoolIdAndRoleRole(school.getId(), Role.TEACHER);

        // Assert
        assertThat(result).extracting(User::getId).contains("user-2");
    }
}
