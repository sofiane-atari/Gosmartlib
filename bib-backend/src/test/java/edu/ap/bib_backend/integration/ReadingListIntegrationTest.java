package edu.ap.bib_backend.integration;

import edu.ap.bib_backend.BibBackendApplication;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.OneRosterTokenRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BibBackendApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ReadingListIntegrationTest {

    private static final String USER_ID = "user-int-1";
    private static final String OTHER_USER_ID = "user-int-2";
    private static final String CLASS_CODE = "2ITSOF1";
    private static final String YEAR_CODE = "2";
    private static final String SCHOOL_CODE = "https://aphogeschool.smartschool.be";

    private Campus campus;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReadingListRepository readingListRepository;

    @Autowired
    private ReadingListBookRepository readingListBookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private OneRosterCredentialRepository oneRosterCredentialRepository;

    @Autowired
    private OneRosterTokenRepository oneRosterTokenRepository;

    @BeforeEach
    void setUp() {
        readingListBookRepository.deleteAll();
        readingListRepository.deleteAll();
        userRepository.deleteAll();
        roleEntityRepository.deleteAll();
        oneRosterTokenRepository.deleteAll();
        oneRosterCredentialRepository.deleteAll();
        schoolRepository.deleteAll();

        // Create test school
        School school = new School();
        school.setDomainName("test.smartschool.be");
        school.setName("Test School");
        schoolRepository.save(school);

        RoleEntity studentRole = new RoleEntity();
        studentRole.setRole(Role.STUDENT);
        roleEntityRepository.save(studentRole);

        campus = new Campus();
        campus.setName("Test Campus");
        campus.setSchool(school);
        campus = campusRepository.save(campus);

        User user = new User();
        user.setId(USER_ID);
        user.setRole(studentRole);
        user.setSchool(school);
        user.setCampus(campus);
        userRepository.save(user);

        User otherUser = new User();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setRole(studentRole);
        otherUser.setSchool(school);
        otherUser.setCampus(campus);
        userRepository.save(otherUser);

        setSecurityContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenNoLists_whenGetReadingLists_thenAutoCreateSharedLists() throws Exception {
        mockMvc.perform(get("/reading-lists").principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of())))
                .andExpect(status().isOk());

        List<ReadingList> all = readingListRepository.findAll();

        long classCount = all.stream().filter(l -> l.getTargetType() == ReadingListTargetType.CLASS).count();
        long yearCount = all.stream().filter(l -> l.getTargetType() == ReadingListTargetType.YEAR).count();
        long schoolCount = all.stream().filter(l -> l.getTargetType() == ReadingListTargetType.SCHOOL).count();
        long systemCount = all.stream().filter(l -> l.getTargetType() == ReadingListTargetType.SYSTEM).count();

        assertEquals(1, classCount);
        assertEquals(1, yearCount);
        assertEquals(1, schoolCount);
        assertEquals(1, systemCount);
    }

    @Test
    void givenSharedListsExist_whenGetReadingListsTwice_thenNoDuplicates() throws Exception {
        mockMvc.perform(get("/reading-lists").principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reading-lists").principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of())))
                .andExpect(status().isOk());

        List<ReadingList> all = readingListRepository.findAll();
        Map<ReadingListTargetType, Long> counts = all.stream()
                .collect(Collectors.groupingBy(ReadingList::getTargetType, Collectors.counting()));

        assertEquals(1L, counts.getOrDefault(ReadingListTargetType.CLASS, 0L));
        assertEquals(1L, counts.getOrDefault(ReadingListTargetType.YEAR, 0L));
        assertEquals(1L, counts.getOrDefault(ReadingListTargetType.SCHOOL, 0L));
        assertEquals(1L, counts.getOrDefault(ReadingListTargetType.SYSTEM, 0L));
    }

    @Test
    void givenMixedLists_whenGetReadingLists_thenReturnsOnlyVisibleTargets() throws Exception {
        createList("Mijn persoonlijk", USER_ID, ReadingListTargetType.PERSONAL, null);
        createList("Ander persoonlijk", OTHER_USER_ID, ReadingListTargetType.PERSONAL, null);
        createList("Klas gedeeld", OTHER_USER_ID, ReadingListTargetType.CLASS, CLASS_CODE);
        createList("Andere klas", OTHER_USER_ID, ReadingListTargetType.CLASS, "3ABC");
        createList("Jaar gedeeld", OTHER_USER_ID, ReadingListTargetType.YEAR, YEAR_CODE);
        createList("School gedeeld", OTHER_USER_ID, ReadingListTargetType.SCHOOL, SCHOOL_CODE);
        createList("Systeembreed", OTHER_USER_ID, ReadingListTargetType.SYSTEM, null);

        mockMvc.perform(get("/reading-lists").principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Mijn persoonlijk")))
                .andExpect(jsonPath("$[*].name", hasItem("Klas gedeeld")))
                .andExpect(jsonPath("$[*].name", hasItem("Jaar gedeeld")))
                .andExpect(jsonPath("$[*].name", hasItem("School gedeeld")))
                .andExpect(jsonPath("$[*].name", hasItem("Systeembreed")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Ander persoonlijk"))))
                .andExpect(jsonPath("$[*].name", not(hasItem("Andere klas"))));
    }

    private void createList(String name, String ownerId, ReadingListTargetType targetType, String targetValue) {
        ReadingList list = new ReadingList();
        list.setName(name);
        list.setUser(userRepository.findById(ownerId).orElseThrow());
        list.setTargetType(targetType);
        list.setTargetValue(targetValue);
        list.setCampus(campus);
        readingListRepository.save(list);
    }

    private void setSecurityContext() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
        Map<String, Object> details = new HashMap<>();
        details.put("classCode", CLASS_CODE);
        details.put("yearCode", YEAR_CODE);
        details.put("schoolCode", SCHOOL_CODE);
        authentication.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
