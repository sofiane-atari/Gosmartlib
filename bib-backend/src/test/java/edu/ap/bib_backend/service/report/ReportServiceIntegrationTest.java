package edu.ap.bib_backend.service.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.dto.groupinfo.SmartschoolGroupInfoDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.dto.report.ReportClassOptionDTO;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.smartschool.SmartschoolProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportServiceIntegrationTest {

        private static final String TEACHER_USER_ID = "teacher-user-id";
        private static final String TEACHER_ROLE = "ROLE_TEACHER";

        private static final String SMARTSCHOOL_ACCESS_TOKEN = "smartschool-access-token";
        private static final String SMARTSCHOOL_REFRESH_TOKEN = "smartschool-refresh-token";
        private static final String JWT_TOKEN = "test-jwt-token";

        private static final String PLATFORM = "https://aphogeschool.smartschool.be";
        private static final String SCHOOL_CODE = "aphogeschool.smartschool.be";

        private static final String CLASS_2ITSOF1_SOURCED_ID = "5905_usermanagement_232";
        private static final String CLASS_2ITSOF2_SOURCED_ID = "5905_usermanagement_234";
        private static final String CLASS_2ITSOF_SOURCED_ID = "5905_usermanagement_248";

        private static final String CLASS_2ITSOF1 = "2ITSOF1";
        private static final String CLASS_2ITSOF2 = "2ITSOF2";
        private static final String CLASS_2ITSOF = "2ITSOF";

        private static final String STUDENT_ONE_SOURCED_ID = "student-sourced-id-1";
        private static final String STUDENT_TWO_SOURCED_ID = "student-sourced-id-2";

        private static final String STUDENT_ONE_NAME = "Leerling Een";
        private static final String STUDENT_TWO_NAME = "Leerling Twee";

        @Autowired
        private ReportService reportService;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private SmartschoolProfileService smartschoolProfileService;

        @MockitoBean
        private OneRosterClient oneRosterClient;

        @BeforeEach
        void seedAuthSession() {
                jdbcTemplate.update(
                                "DELETE FROM auth_sessions WHERE user_id = ?",
                                TEACHER_USER_ID);

                jdbcTemplate.update(
                                """
                                                INSERT INTO auth_sessions (
                                                    user_id,
                                                    admin_override,
                                                    admin_override_expires_at,
                                                    jwt_expires_at,
                                                    jwt_token,
                                                    role,
                                                    smartschool_access_token,
                                                    smartschool_access_token_expires_at,
                                                    smartschool_refresh_token,
                                                    smartschool_refresh_token_expires_at
                                                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                                """,
                                TEACHER_USER_ID,
                                false,
                                null,
                                Timestamp.valueOf(LocalDateTime.of(2099, 1, 1, 0, 0)),
                                JWT_TOKEN,
                                TEACHER_ROLE,
                                SMARTSCHOOL_ACCESS_TOKEN,
                                Timestamp.valueOf(LocalDateTime.of(2099, 1, 1, 0, 0)),
                                SMARTSCHOOL_REFRESH_TOKEN,
                                Timestamp.valueOf(LocalDateTime.of(2099, 1, 1, 0, 0)));
        }

        @Test
        void Given_TeacherGroups_When_GetAvailableClasses_Then_ReturnsOnlyMatchedClasses() {
                // Arrange
                UsernamePasswordAuthenticationToken authentication = teacherAuthentication();

                SmartschoolProfileDto profile = smartschoolProfile();
                SmartschoolGroupInfoDTO groupInfo = smartschoolGroupInfo(CLASS_2ITSOF1, CLASS_2ITSOF2);

                List<OneRosterClassDTO> classes = List.of(
                                oneRosterClass(CLASS_2ITSOF1_SOURCED_ID, CLASS_2ITSOF1),
                                oneRosterClass(CLASS_2ITSOF2_SOURCED_ID, CLASS_2ITSOF2),
                                oneRosterClass(CLASS_2ITSOF_SOURCED_ID, CLASS_2ITSOF));

                when(smartschoolProfileService.fetchProfile(SMARTSCHOOL_ACCESS_TOKEN))
                                .thenReturn(profile);

                when(smartschoolProfileService.fetchGroupInfo(SMARTSCHOOL_ACCESS_TOKEN, PLATFORM))
                                .thenReturn(groupInfo);

                when(oneRosterClient.fetchClasses(SCHOOL_CODE))
                                .thenReturn(classes);

                // Act
                List<ReportClassOptionDTO> result = reportService.getAvailableClasses(authentication);

                // Assert
                assertThat(result).hasSize(2);

                assertThat(result)
                                .extracting(ReportClassOptionDTO::sourcedId)
                                .containsExactly(
                                                CLASS_2ITSOF1_SOURCED_ID,
                                                CLASS_2ITSOF2_SOURCED_ID);

                assertThat(result)
                                .extracting(ReportClassOptionDTO::title)
                                .containsExactly(
                                                CLASS_2ITSOF1,
                                                CLASS_2ITSOF2);

                assertThat(result)
                                .extracting(ReportClassOptionDTO::classCode)
                                .containsExactly(
                                                CLASS_2ITSOF1,
                                                CLASS_2ITSOF2);
        }

        @Test
        void Given_TeacherAndAllowedClass_When_GetClassReport_Then_ReturnsReportForSelectedClass() {
                // Arrange
                UsernamePasswordAuthenticationToken authentication = teacherAuthentication();

                String classSourcedId = CLASS_2ITSOF1_SOURCED_ID;

                SmartschoolProfileDto profile = smartschoolProfile();
                SmartschoolGroupInfoDTO groupInfo = smartschoolGroupInfo(CLASS_2ITSOF1);

                OneRosterUserDTO studentOne = oneRosterUser(STUDENT_ONE_SOURCED_ID, STUDENT_ONE_NAME);
                OneRosterUserDTO studentTwo = oneRosterUser(STUDENT_TWO_SOURCED_ID, STUDENT_TWO_NAME);

                List<OneRosterClassDTO> classes = List.of(
                                oneRosterClass(classSourcedId, CLASS_2ITSOF1),
                                oneRosterClass(CLASS_2ITSOF2_SOURCED_ID, CLASS_2ITSOF2));

                List<OneRosterUserDTO> students = List.of(studentOne, studentTwo);

                when(smartschoolProfileService.fetchProfile(SMARTSCHOOL_ACCESS_TOKEN))
                                .thenReturn(profile);

                when(smartschoolProfileService.fetchGroupInfo(SMARTSCHOOL_ACCESS_TOKEN, PLATFORM))
                                .thenReturn(groupInfo);

                when(oneRosterClient.fetchClasses(SCHOOL_CODE))
                                .thenReturn(classes);

                when(oneRosterClient.fetchStudentsForClassWithBearer(
                                SCHOOL_CODE,
                                SMARTSCHOOL_ACCESS_TOKEN,
                                classSourcedId)).thenReturn(students);

                // Act
                ClassReportDTO result = reportService.getClassReport(classSourcedId, authentication);

                // Assert
                assertThat(result.classSourcedId()).isEqualTo(classSourcedId);
                assertThat(result.classTitle()).isEqualTo(CLASS_2ITSOF1);

                assertThat(result.studentCount()).isEqualTo(2);
                assertThat(result.linkedStudentCount()).isZero();

                assertThat(result.totalLoans()).isZero();
                assertThat(result.activeLoans()).isZero();
                assertThat(result.averageLoansPerLinkedStudent()).isZero();

                assertThat(result.students()).hasSize(2);

                assertThat(result.students())
                                .extracting(row -> row.studentSourcedId())
                                .containsExactly(
                                                STUDENT_ONE_SOURCED_ID,
                                                STUDENT_TWO_SOURCED_ID);

                assertThat(result.students())
                                .extracting(row -> row.displayName())
                                .containsExactly(
                                                STUDENT_ONE_NAME,
                                                STUDENT_TWO_NAME);
        }

        @Test
        void Given_TeacherWithoutAccessToClass_When_GetClassReport_Then_ThrowsForbidden() {
                // Arrange
                UsernamePasswordAuthenticationToken authentication = teacherAuthentication();

                String forbiddenClassSourcedId = CLASS_2ITSOF2_SOURCED_ID;

                SmartschoolProfileDto profile = smartschoolProfile();
                SmartschoolGroupInfoDTO groupInfo = smartschoolGroupInfo(CLASS_2ITSOF1);

                List<OneRosterClassDTO> classes = List.of(
                                oneRosterClass(CLASS_2ITSOF1_SOURCED_ID, CLASS_2ITSOF1),
                                oneRosterClass(forbiddenClassSourcedId, CLASS_2ITSOF2));

                when(smartschoolProfileService.fetchProfile(SMARTSCHOOL_ACCESS_TOKEN))
                                .thenReturn(profile);

                when(smartschoolProfileService.fetchGroupInfo(SMARTSCHOOL_ACCESS_TOKEN, PLATFORM))
                                .thenReturn(groupInfo);

                when(oneRosterClient.fetchClasses(SCHOOL_CODE))
                                .thenReturn(classes);

                // Act & Assert
                assertThatThrownBy(() -> reportService.getClassReport(
                                forbiddenClassSourcedId,
                                authentication)).hasMessageContaining("Je hebt geen toegang tot deze klas");
        }

        @Test
        void Given_UnknownClassSourcedId_When_GetClassReport_Then_ThrowsNotFound() {
                // Arrange
                UsernamePasswordAuthenticationToken authentication = teacherAuthentication();

                String unknownClassSourcedId = "unknown-class-id";

                SmartschoolProfileDto profile = smartschoolProfile();

                List<OneRosterClassDTO> classes = List.of(
                                oneRosterClass(CLASS_2ITSOF1_SOURCED_ID, CLASS_2ITSOF1));

                when(smartschoolProfileService.fetchProfile(SMARTSCHOOL_ACCESS_TOKEN))
                                .thenReturn(profile);

                when(oneRosterClient.fetchClasses(SCHOOL_CODE))
                                .thenReturn(classes);

                // Act & Assert
                assertThatThrownBy(() -> reportService.getClassReport(
                                unknownClassSourcedId,
                                authentication)).hasMessageContaining("Klas niet gevonden");
        }

        private UsernamePasswordAuthenticationToken teacherAuthentication() {
                return new UsernamePasswordAuthenticationToken(
                                TEACHER_USER_ID,
                                null,
                                List.of(new SimpleGrantedAuthority(TEACHER_ROLE)));
        }

        private OneRosterClassDTO oneRosterClass(String sourcedId, String className) {
                return new OneRosterClassDTO(
                                sourcedId,
                                className,
                                className);
        }

        private SmartschoolProfileDto smartschoolProfile() {
                return objectMapper.convertValue(
                                Map.of("platform", PLATFORM),
                                SmartschoolProfileDto.class);
        }

        private SmartschoolGroupInfoDTO smartschoolGroupInfo(String... groupNames) {
                List<Map<String, String>> groups = Arrays.stream(groupNames)
                                .map(groupName -> Map.of("name", groupName))
                                .toList();

                return objectMapper.convertValue(
                                Map.of("groups", groups),
                                SmartschoolGroupInfoDTO.class);
        }

        private OneRosterUserDTO oneRosterUser(String sourcedId, String displayName) {
                return objectMapper.convertValue(
                                Map.of(
                                                "sourcedId", sourcedId,
                                                "displayName", displayName),
                                OneRosterUserDTO.class);
        }
}