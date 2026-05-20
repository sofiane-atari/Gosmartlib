package edu.ap.bib_backend.integration;

import edu.ap.bib_backend.BibBackendApplication;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private Campus otherCampus;
    private School school;
    private RoleEntity studentRole;
    private RoleEntity teacherRole;
    private UsernamePasswordAuthenticationToken currentPrincipal;

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
        cleanDatabase();

        school = new School();
        school.setDomainName("test.smartschool.be");
        school.setName("Test School");
        school = schoolRepository.save(school);

        studentRole = new RoleEntity();
        studentRole.setRole(Role.STUDENT);
        studentRole = roleEntityRepository.save(studentRole);

        teacherRole = new RoleEntity();
        teacherRole.setRole(Role.TEACHER);
        teacherRole = roleEntityRepository.save(teacherRole);

        campus = new Campus();
        campus.setName("Test Campus");
        campus.setSchool(school);
        campus = campusRepository.save(campus);

        otherCampus = new Campus();
        otherCampus.setName("Andere Campus");
        otherCampus.setSchool(school);
        otherCampus = campusRepository.save(otherCampus);

        createUser(USER_ID, studentRole, campus);
        createUser(OTHER_USER_ID, studentRole, campus);

        setAuthenticationAsStudent();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenNoLists_whenGetReadingLists_thenAutoCreateClassAndSystemLists() throws Exception {
        getReadingLists()
                .andExpect(status().isOk());

        List<ReadingList> all = readingListRepository.findAll();

        long classCount = all.stream()
                .filter(list -> list.getTargetType() == ReadingListTargetType.CLASS)
                .count();

        long systemCount = all.stream()
                .filter(list -> list.getTargetType() == ReadingListTargetType.SYSTEM)
                .count();

        assertEquals(1, classCount);
        assertEquals(1, systemCount);
    }

    @Test
    void givenSharedListsExist_whenGetReadingListsTwice_thenNoDuplicates() throws Exception {
        getReadingLists()
                .andExpect(status().isOk());

        getReadingLists()
                .andExpect(status().isOk());

        List<ReadingList> all = readingListRepository.findAll();

        Map<ReadingListTargetType, Long> counts = all.stream()
                .collect(Collectors.groupingBy(ReadingList::getTargetType, Collectors.counting()));

        assertEquals(1L, counts.getOrDefault(ReadingListTargetType.CLASS, 0L));
        assertEquals(1L, counts.getOrDefault(ReadingListTargetType.SYSTEM, 0L));
    }

    @Test
    void givenMixedLists_whenGetReadingLists_thenReturnsOnlyVisibleTargets() throws Exception {
        createList("Mijn persoonlijk", USER_ID, campus, ReadingListTargetType.PERSONAL, null);
        createList("Ander persoonlijk", OTHER_USER_ID, campus, ReadingListTargetType.PERSONAL, null);
        createList("Klas gedeeld", OTHER_USER_ID, campus, ReadingListTargetType.CLASS, CLASS_CODE);
        createList("Andere klas", OTHER_USER_ID, campus, ReadingListTargetType.CLASS, "3ABC");
        createList("Systeembreed", OTHER_USER_ID, campus, ReadingListTargetType.SYSTEM, null);

        getReadingLists()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Mijn persoonlijk")))
                .andExpect(jsonPath("$[*].name", hasItem("Klas gedeeld")))
                .andExpect(jsonPath("$[*].name", hasItem("Systeembreed")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Ander persoonlijk"))))
                .andExpect(jsonPath("$[*].name", not(hasItem("Andere klas"))))
                .andExpect(jsonPath("$[*].name", not(hasItem("Jaar gedeeld"))))
                .andExpect(jsonPath("$[*].name", not(hasItem("School gedeeld"))));
    }

    @Test
    void givenCatalogFeaturedLists_whenGetCatalogFeaturedLists_thenReturnsOnlySelectedListsFromCurrentCampus()
            throws Exception {
        createCatalogList("Eerste selectie", campus, true, 1);
        createCatalogList("Tweede selectie", campus, true, 2);
        createCatalogList("Derde selectie", campus, true, 3);
        createCatalogList("Niet geselecteerd", campus, false, null);
        createCatalogList("Andere campus selectie", otherCampus, true, 1);

        getCatalogFeaturedLists()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", hasItem("Eerste selectie")))
                .andExpect(jsonPath("$[*].name", hasItem("Tweede selectie")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Derde selectie"))))
                .andExpect(jsonPath("$[*].name", not(hasItem("Niet geselecteerd"))))
                .andExpect(jsonPath("$[*].name", not(hasItem("Andere campus selectie"))));
    }

    @Test
    void givenSystemList_whenSelectCatalogFeaturedList_thenListIsShownOnCatalog() throws Exception {
        setAuthenticationAsTeacher();

        ReadingList list = createCatalogList("Nieuwe selectie", campus, false, null);

        selectCatalogFeaturedList(list.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Nieuwe selectie")));

        ReadingList refreshed = readingListRepository.findById(list.getId()).orElseThrow();

        assertTrue(refreshed.isShowOnCatalog());
        assertEquals(1, refreshed.getCatalogFeaturedOrder());
    }

    @Test
    void givenTwoSelectedCatalogLists_whenSelectingThirdList_thenOldestSelectionIsRemoved() throws Exception {
        setAuthenticationAsTeacher();

        ReadingList first = createCatalogList("Eerste", campus, true, 1);
        ReadingList second = createCatalogList("Tweede", campus, true, 2);
        ReadingList third = createCatalogList("Derde", campus, false, null);

        selectCatalogFeaturedList(third.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", hasItem("Tweede")))
                .andExpect(jsonPath("$[*].name", hasItem("Derde")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Eerste"))));

        ReadingList refreshedFirst = readingListRepository.findById(first.getId()).orElseThrow();
        ReadingList refreshedSecond = readingListRepository.findById(second.getId()).orElseThrow();
        ReadingList refreshedThird = readingListRepository.findById(third.getId()).orElseThrow();

        assertFalse(refreshedFirst.isShowOnCatalog());
        assertTrue(refreshedSecond.isShowOnCatalog());
        assertTrue(refreshedThird.isShowOnCatalog());
    }

    @Test
    void givenSelectedCatalogList_whenUnselectCatalogFeaturedList_thenListIsRemovedFromCatalog() throws Exception {
        setAuthenticationAsTeacher();

        ReadingList list = createCatalogList("Te verwijderen", campus, true, 1);

        unselectCatalogFeaturedList(list.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        ReadingList refreshed = readingListRepository.findById(list.getId()).orElseThrow();

        assertFalse(refreshed.isShowOnCatalog());
        assertEquals(null, refreshed.getCatalogFeaturedOrder());
    }

    private org.springframework.test.web.servlet.ResultActions getReadingLists() throws Exception {
        return mockMvc.perform(get("/reading-lists").principal(currentPrincipal));
    }

    private org.springframework.test.web.servlet.ResultActions getCatalogFeaturedLists() throws Exception {
        return mockMvc.perform(get("/reading-lists/featured/catalog").principal(currentPrincipal));
    }

    private org.springframework.test.web.servlet.ResultActions selectCatalogFeaturedList(Long listId) throws Exception {
        return mockMvc.perform(post("/reading-lists/{id}/catalog-featured", listId).principal(currentPrincipal));
    }

    private org.springframework.test.web.servlet.ResultActions unselectCatalogFeaturedList(Long listId)
            throws Exception {
        return mockMvc.perform(delete("/reading-lists/{id}/catalog-featured", listId).principal(currentPrincipal));
    }

    private ReadingList createCatalogList(
            String name,
            Campus listCampus,
            boolean showOnCatalog,
            Integer catalogFeaturedOrder) {
        ReadingList list = createList(name, OTHER_USER_ID, listCampus, ReadingListTargetType.SYSTEM, null);
        list.setShowOnCatalog(showOnCatalog);
        list.setCatalogFeaturedOrder(catalogFeaturedOrder);

        return readingListRepository.save(list);
    }

    private ReadingList createList(
            String name,
            String ownerId,
            Campus listCampus,
            ReadingListTargetType targetType,
            String targetValue) {
        ReadingList list = new ReadingList();
        list.setName(name);
        list.setUser(userRepository.findById(ownerId).orElseThrow());
        list.setTargetType(targetType);
        list.setTargetValue(targetValue);
        list.setCampus(listCampus);

        return readingListRepository.save(list);
    }

    private void createUser(String userId, RoleEntity role, Campus userCampus) {
        User user = new User();
        user.setId(userId);
        user.setRole(role);
        user.setSchool(school);
        user.setCampus(userCampus);

        userRepository.save(user);
    }

    private void setAuthenticationAsStudent() {
        setAuthentication("ROLE_STUDENT");
    }

    private void setAuthenticationAsTeacher() {
        User user = userRepository.findById(USER_ID).orElseThrow();
        user.setRole(teacherRole);
        userRepository.save(user);

        setAuthentication("ROLE_TEACHER");
    }

    private void setAuthentication(String authority) {
        currentPrincipal = new UsernamePasswordAuthenticationToken(
                USER_ID,
                null,
                List.of(new SimpleGrantedAuthority(authority)));

        Map<String, Object> details = new HashMap<>();
        details.put("classCode", CLASS_CODE);
        details.put("yearCode", YEAR_CODE);
        details.put("schoolCode", SCHOOL_CODE);

        currentPrincipal.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(currentPrincipal);
    }

    private void cleanDatabase() {
        readingListBookRepository.deleteAll();
        readingListRepository.deleteAll();
        oneRosterTokenRepository.deleteAll();
        oneRosterCredentialRepository.deleteAll();
        userRepository.deleteAll();
        campusRepository.deleteAll();
        roleEntityRepository.deleteAll();
        schoolRepository.deleteAll();
    }
}