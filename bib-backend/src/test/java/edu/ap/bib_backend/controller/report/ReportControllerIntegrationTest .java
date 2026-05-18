package edu.ap.bib_backend.controller.report;

import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.dto.groupinfo.GroupInfoItemDTO;
import edu.ap.bib_backend.dto.groupinfo.SmartschoolGroupInfoDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.smartschool.SmartschoolProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SmartschoolProfileService smartschoolProfileService;

    @MockitoBean
    private OneRosterClient oneRosterClient;

    @Test
    void Given_Teacher_When_GetAvailableClasses_Then_ReturnsClassOptions() throws Exception {
        UsernamePasswordAuthenticationToken authentication = teacherAuthentication();

        when(smartschoolProfileService.fetchProfile("smartschool-access-token"))
                .thenReturn(smartschoolProfile());

        when(smartschoolProfileService.fetchGroupInfo(
                "smartschool-access-token",
                "https://aphogeschool.smartschool.be"))
                .thenReturn(smartschoolGroupInfo("2ITSOF1", "2ITSOF2"));

        when(oneRosterClient.fetchClasses("aphogeschool.smartschool.be"))
                .thenReturn(List.of(
                        new OneRosterClassDTO(
                                "5905_usermanagement_232",
                                "2ITSOF1",
                                "2ITSOF1"),
                        new OneRosterClassDTO(
                                "5905_usermanagement_234",
                                "2ITSOF2",
                                "2ITSOF2"),
                        new OneRosterClassDTO(
                                "5905_usermanagement_248",
                                "2ITSOF",
                                "2ITSOF")));

        mockMvc.perform(get("/reports/classes")
                .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sourcedId").value("5905_usermanagement_232"))
                .andExpect(jsonPath("$[0].title").value("2ITSOF1"))
                .andExpect(jsonPath("$[0].classCode").value("2ITSOF1"))
                .andExpect(jsonPath("$[1].sourcedId").value("5905_usermanagement_234"))
                .andExpect(jsonPath("$[1].title").value("2ITSOF2"))
                .andExpect(jsonPath("$[1].classCode").value("2ITSOF2"));
    }

    @Test
    void Given_TeacherAndClassSourcedId_When_GetClassReport_Then_ReturnsReportForThatClass() throws Exception {
        UsernamePasswordAuthenticationToken authentication = teacherAuthentication();

        String classSourcedId = "5905_usermanagement_232";

        when(smartschoolProfileService.fetchProfile("smartschool-access-token"))
                .thenReturn(smartschoolProfile());

        when(smartschoolProfileService.fetchGroupInfo(
                "smartschool-access-token",
                "https://aphogeschool.smartschool.be"))
                .thenReturn(smartschoolGroupInfo("2ITSOF1"));

        when(oneRosterClient.fetchClasses("aphogeschool.smartschool.be"))
                .thenReturn(List.of(
                        new OneRosterClassDTO(
                                classSourcedId,
                                "2ITSOF1",
                                "2ITSOF1"),
                        new OneRosterClassDTO(
                                "5905_usermanagement_234",
                                "2ITSOF2",
                                "2ITSOF2")));

        when(oneRosterClient.fetchStudentsForClassWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-access-token",
                classSourcedId))
                .thenReturn(List.of(
                        oneRosterUser("student-sourced-id-1", "Leerling Een"),
                        oneRosterUser("student-sourced-id-2", "Leerling Twee")));

        mockMvc.perform(get("/reports/classes/{classSourcedId}/report", classSourcedId)
                .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classSourcedId").value(classSourcedId))
                .andExpect(jsonPath("$.classTitle").value("2ITSOF1"))
                .andExpect(jsonPath("$.studentCount").value(2))
                .andExpect(jsonPath("$.linkedStudentCount").value(0))
                .andExpect(jsonPath("$.totalLoans").value(0))
                .andExpect(jsonPath("$.activeLoans").value(0))
                .andExpect(jsonPath("$.students[0].studentSourcedId").value("student-sourced-id-1"))
                .andExpect(jsonPath("$.students[0].displayName").value("Leerling Een"))
                .andExpect(jsonPath("$.students[1].studentSourcedId").value("student-sourced-id-2"))
                .andExpect(jsonPath("$.students[1].displayName").value("Leerling Twee"));
    }

    @Test
    void Given_TeacherWithoutAccessToClass_When_GetClassReport_Then_ReturnsForbidden() throws Exception {
        UsernamePasswordAuthenticationToken authentication = teacherAuthentication();

        String forbiddenClassSourcedId = "5905_usermanagement_234";

        when(smartschoolProfileService.fetchProfile("smartschool-access-token"))
                .thenReturn(smartschoolProfile());

        when(smartschoolProfileService.fetchGroupInfo(
                "smartschool-access-token",
                "https://aphogeschool.smartschool.be"))
                .thenReturn(smartschoolGroupInfo("2ITSOF1"));

        when(oneRosterClient.fetchClasses("aphogeschool.smartschool.be"))
                .thenReturn(List.of(
                        new OneRosterClassDTO(
                                "5905_usermanagement_232",
                                "2ITSOF1",
                                "2ITSOF1"),
                        new OneRosterClassDTO(
                                forbiddenClassSourcedId,
                                "2ITSOF2",
                                "2ITSOF2")));

        mockMvc.perform(get("/reports/classes/{classSourcedId}/report", forbiddenClassSourcedId)
                .with(authentication(authentication)))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken teacherAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "teacher-user-id",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }

    private SmartschoolProfileDto smartschoolProfile() {
        SmartschoolProfileDto profile = mock(SmartschoolProfileDto.class);

        when(profile.platform())
                .thenReturn("https://aphogeschool.smartschool.be");

        return profile;
    }

    private GroupInfoItemDTO groupInfoItem(String name) {
        GroupInfoItemDTO groupInfoItem = mock(GroupInfoItemDTO.class);

        when(groupInfoItem.name())
                .thenReturn(name);

        return groupInfoItem;
    }

    private OneRosterUserDTO oneRosterUser(String sourcedId, String displayName) {
        OneRosterUserDTO user = mock(OneRosterUserDTO.class);

        when(user.sourcedId())
                .thenReturn(sourcedId);

        when(user.displayName())
                .thenReturn(displayName);

        return user;
    }

    private SmartschoolGroupInfoDTO smartschoolGroupInfo(String... groupNames) {
        SmartschoolGroupInfoDTO groupInfo = mock(SmartschoolGroupInfoDTO.class);

        List<GroupInfoItemDTO> groups = Arrays.stream(groupNames)
                .map(this::groupInfoItem)
                .toList();

        when(groupInfo.groups())
                .thenReturn(groups);

        return groupInfo;
    }
}