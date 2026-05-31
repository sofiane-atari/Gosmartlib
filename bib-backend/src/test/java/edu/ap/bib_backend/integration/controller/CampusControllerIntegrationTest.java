package edu.ap.bib_backend.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.BibBackendApplication;
import edu.ap.bib_backend.dto.CampusDTO;
import edu.ap.bib_backend.jpa.entity.*;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.*;
import edu.ap.bib_backend.jpa.repository.review.ReviewReportRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BibBackendApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CampusControllerIntegrationTest {

    private static final String USER_ID = "campus-ctrl-user";

    private UsernamePasswordAuthenticationToken currentPrincipal;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanExtensionRepository loanExtensionRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewReportRepository reviewReportRepository;

    @Autowired
    private MailSubscriptionRepository mailSubscriptionRepository;

    @Autowired
    private OneRosterCredentialRepository oneRosterCredentialRepository;

    @Autowired
    private OneRosterTokenRepository oneRosterTokenRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private StudentClassStatisticRepository studentClassStatisticRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ReadingListRepository readingListRepository;

    @Autowired
    private ReadingListBookRepository readingListBookRepository;

    private School school;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        school = new School();
        school.setDomainName("campus-ctrl-test.be");
        school.setName("Campus Ctrl Test School");
        school = schoolRepository.save(school);

        RoleEntity role = getOrCreateRole(Role.STUDENT);

        User user = new User();
        user.setId(USER_ID);
        user.setRole(role);
        user.setSchool(school);
        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenCampusesExist_whenGetCampusesForCurrentUserSchool_thenReturnsCampuses() throws Exception {
        createCampus("Campus A", school);
        createCampus("Campus B", school);
        setAuthentication("ROLE_STUDENT");

        mockMvc.perform(get("/campuses/me").principal(currentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void givenCampusExists_whenGetCampus_thenReturnsCampus() throws Exception {
        Campus campus = createCampus("My Campus", school);
        setAuthentication("ROLE_STUDENT");

        mockMvc.perform(get("/campuses/{id}", campus.getId()).principal(currentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Campus"));
    }

    @Test
    void givenValidData_whenCreateCampus_thenReturnsCreated() throws Exception {
        setAuthentication("ROLE_ADMIN");
        CampusDTO dto = new CampusDTO();
        dto.setName("New Campus Ctrl");
        dto.setAddress("New Address");
        dto.setPostalCode(9000);

        mockMvc.perform(post("/campuses")
                        .param("schoolId", school.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .principal(currentPrincipal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Campus Ctrl"));
    }

    @Test
    void givenCampusExists_whenUpdateCampus_thenReturnsUpdated() throws Exception {
        Campus campus = createCampus("Old Name", school);
        setAuthentication("ROLE_STUDENT");
        CampusDTO dto = new CampusDTO();
        dto.setName("Updated Name");
        dto.setAddress("Updated Address");
        dto.setPostalCode(4000);

        mockMvc.perform(put("/campuses/{id}", campus.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .principal(currentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void givenSchoolWithCampuses_whenGetCampusesBySchoolAsAdmin_thenReturnsCampuses() throws Exception {
        createCampus("Admin Campus 1", school);
        createCampus("Admin Campus 2", school);
        setAuthentication("ROLE_ADMIN");

        mockMvc.perform(get("/campuses")
                        .param("schoolId", school.getId().toString())
                        .principal(currentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private Campus createCampus(String name, School school) {
        Campus campus = new Campus();
        campus.setName(name);
        campus.setSchool(school);
        campus.setAddress("Address " + name);
        campus.setPostalCode(2000);
        return campusRepository.save(campus);
    }

    private void setAuthentication(String authority) {
        currentPrincipal = new UsernamePasswordAuthenticationToken(
                USER_ID, null,
                List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(currentPrincipal);
    }

    private RoleEntity getOrCreateRole(Role role) {
        return roleEntityRepository.findByRole(role)
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setRole(role);
                    return roleEntityRepository.save(newRole);
                });
    }

    private void cleanDatabase() {
        readingListBookRepository.deleteAll();
        readingListRepository.deleteAll();
        reviewReportRepository.deleteAll();
        reviewRepository.deleteAll();
        mailSubscriptionRepository.deleteAll();
        loanExtensionRepository.deleteAll();
        loanRepository.deleteAll();
        oneRosterTokenRepository.deleteAll();
        oneRosterCredentialRepository.deleteAll();
        studentClassStatisticRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
        groupRepository.deleteAll();
        campusRepository.deleteAll();
        schoolRepository.deleteAll();
    }
}
