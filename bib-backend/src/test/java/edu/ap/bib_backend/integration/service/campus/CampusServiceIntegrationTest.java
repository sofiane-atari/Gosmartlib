package edu.ap.bib_backend.integration.service.campus;

import edu.ap.bib_backend.BibBackendApplication;
import edu.ap.bib_backend.dto.CampusDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.CampusService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = BibBackendApplication.class)
@ActiveProfiles("test")
@Transactional
class CampusServiceIntegrationTest {

    @Autowired
    private CampusService campusService;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @Test
    void Given_SchoolWithCampuses_When_GetCampusesBySchool_Then_ReturnsCampuses() {
        School school = createSchool("campus-test-1.be");
        Campus campus1 = createCampus("Campus A", school, "Adres A", 2000);
        Campus campus2 = createCampus("Campus B", school, "Adres B", 3000);

        List<CampusDTO> result = campusService.getCampusesBySchool(school.getId());

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(c -> c.getName().equals("Campus A")));
        assertTrue(result.stream().anyMatch(c -> c.getName().equals("Campus B")));
    }

    @Test
    void Given_UserSchoolWithCampuses_When_GetCampusesForCurrentUserSchool_Then_ReturnsCampuses() {
        School school = createSchool("campus-test-2.be");
        createCampus("Campus X", school, "Adres X", 2000);
        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        List<CampusDTO> result = campusService.getCampusesForCurrentUserSchool(auth);

        assertEquals(1, result.size());
        assertEquals("Campus X", result.get(0).getName());
    }

    @Test
    void Given_AdminUser_When_GetCampus_Then_ReturnsCampusRegardlessOfSchool() {
        School school = createSchool("campus-test-3.be");
        Campus campus = createCampus("Admin Campus", school, "Adres", 2000);
        Authentication auth = mockAuthWithRole("ROLE_ADMIN");

        CampusDTO result = campusService.getCampus(campus.getId(), auth);

        assertEquals("Admin Campus", result.getName());
    }

    @Test
    void Given_NonAdminUserSameSchool_When_GetCampus_Then_ReturnsCampus() {
        School school = createSchool("campus-test-4.be");
        Campus campus = createCampus("My Campus", school, "Adres", 2000);
        User user = createUser("campus-user-4", Role.STUDENT, school);
        Authentication auth = mockAuthWithUserId("campus-user-4");
        when(currentUserService.extractCurrentUserId(auth)).thenReturn("campus-user-4");

        CampusDTO result = campusService.getCampus(campus.getId(), auth);

        assertEquals("My Campus", result.getName());
    }

    @Test
    void Given_NonAdminUserDifferentSchool_When_GetCampus_Then_ThrowsAccessDenied() {
        School school1 = createSchool("campus-test-5a.be");
        School school2 = createSchool("campus-test-5b.be");
        Campus campus = createCampus("Other Campus", school1, "Adres", 2000);
        createUser("campus-user-5", Role.STUDENT, school2);
        Authentication auth = mockAuthWithUserId("campus-user-5");
        when(currentUserService.extractCurrentUserId(auth)).thenReturn("campus-user-5");

        assertThrows(AccessDeniedException.class, () -> campusService.getCampus(campus.getId(), auth));
    }

    @Test
    void Given_ValidData_When_CreateCampus_Then_CampusIsPersisted() {
        School school = createSchool("campus-test-6.be");
        CampusDTO dto = new CampusDTO();
        dto.setName("New Campus");
        dto.setAddress("New Address");
        dto.setPostalCode(9000);

        CampusDTO result = campusService.createCampus(school.getId(), dto);

        assertNotNull(result.getId());
        assertEquals("New Campus", result.getName());
        assertEquals("New Address", result.getAddress());
        assertEquals(9000, result.getPostalCode());
    }

    @Test
    void Given_DuplicateName_When_CreateCampus_Then_ThrowsException() {
        School school = createSchool("campus-test-7.be");
        createCampus("Duplicaat", school, "Adres", 2000);
        CampusDTO dto = new CampusDTO();
        dto.setName("Duplicaat");
        dto.setAddress("Ander Adres");
        dto.setPostalCode(3000);

        assertThrows(ResourceAlreadyExistsException.class, () -> campusService.createCampus(school.getId(), dto));
    }

    @Test
    void Given_BlankName_When_CreateCampus_Then_ThrowsException() {
        School school = createSchool("campus-test-8.be");
        CampusDTO dto = new CampusDTO();
        dto.setName("  ");
        dto.setAddress("Adres");
        dto.setPostalCode(2000);

        assertThrows(IllegalArgumentException.class, () -> campusService.createCampus(school.getId(), dto));
    }

    @Test
    void Given_ValidData_When_UpdateCampus_Then_CampusIsUpdated() {
        School school = createSchool("campus-test-9.be");
        Campus campus = createCampus("Old Name", school, "Old Address", 2000);
        CampusDTO dto = new CampusDTO();
        dto.setName("Updated Name");
        dto.setAddress("Updated Address");
        dto.setPostalCode(4000);

        CampusDTO result = campusService.updateCampus(campus.getId(), dto);

        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Address", result.getAddress());
        assertEquals(4000, result.getPostalCode());
    }

    @Test
    void Given_DuplicateNameInSameSchool_When_UpdateCampus_Then_ThrowsException() {
        School school = createSchool("campus-test-10.be");
        Campus campus1 = createCampus("First", school, "Address 1", 2000);
        Campus campus2 = createCampus("Second", school, "Address 2", 3000);

        CampusDTO dto = new CampusDTO();
        dto.setName("Second");
        dto.setAddress("Updated Address");
        dto.setPostalCode(4000);

        assertThrows(ResourceAlreadyExistsException.class, () -> campusService.updateCampus(campus1.getId(), dto));
    }

    @Test
    void Given_NonExistentSchool_When_CreateCampus_Then_ThrowsException() {
        CampusDTO dto = new CampusDTO();
        dto.setName("Campus");
        dto.setAddress("Adres");
        dto.setPostalCode(2000);

        assertThrows(ResourceNotFoundException.class, () -> campusService.createCampus(99999L, dto));
    }

    @Test
    void Given_NonExistentCampus_When_GetCampus_Then_ThrowsException() {
        Authentication auth = mockAuth();
        assertThrows(ResourceNotFoundException.class, () -> campusService.getCampus(99999L, auth));
    }

    private School createSchool(String domain) {
        School school = new School();
        school.setDomainName(domain);
        school.setName("Test School " + domain);
        return schoolRepository.save(school);
    }

    private Campus createCampus(String name, School school, String address, int postalCode) {
        Campus campus = new Campus();
        campus.setName(name);
        campus.setSchool(school);
        campus.setAddress(address);
        campus.setPostalCode(postalCode);
        return campusRepository.save(campus);
    }

    private User createUser(String userId, Role role, School school) {
        RoleEntity roleEntity = roleEntityRepository.findByRole(role).orElseGet(() -> {
            RoleEntity newRole = new RoleEntity();
            newRole.setRole(role);
            return roleEntityRepository.save(newRole);
        });
        User user = new User();
        user.setId(userId);
        user.setRole(roleEntity);
        user.setSchool(school);
        return userRepository.save(user);
    }

    private Authentication mockAuth() {
        return org.mockito.Mockito.mock(Authentication.class);
    }

    private Authentication mockAuthWithRole(String role) {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.getAuthorities()).thenAnswer(inv -> List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(role)));
        return auth;
    }

    private Authentication mockAuthWithUserId(String userId) {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.getAuthorities()).thenAnswer(inv -> List.of());
        return auth;
    }
}
