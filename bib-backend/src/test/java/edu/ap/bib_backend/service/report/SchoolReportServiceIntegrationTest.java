package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.report.SchoolReportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchoolReportServiceIntegrationTest {

    static {
        System.setProperty("JASYPT_ENCRYPTOR_PASSWORD", "test-encryption-password");
    }

    @Autowired
    private SchoolReportService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedData() {
        jdbcTemplate.update("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("DELETE FROM loan_copies");
        jdbcTemplate.update("DELETE FROM loans");
        jdbcTemplate.update("DELETE FROM book_copies");
        jdbcTemplate.update("DELETE FROM books");
        jdbcTemplate.update("DELETE FROM auth_sessions");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM oneroster_credentials");
        jdbcTemplate.update("DELETE FROM campuses");
        jdbcTemplate.update("DELETE FROM schools");
        jdbcTemplate.update("DELETE FROM roles");
        jdbcTemplate.update("SET REFERENTIAL_INTEGRITY TRUE");

        jdbcTemplate.update("INSERT INTO roles (role) VALUES ('STUDENT')");
        jdbcTemplate.update("INSERT INTO roles (role) VALUES ('TEACHER')");

        jdbcTemplate.update("""
                INSERT INTO schools (id, domain_name, name, address, postal_code, default_loan_duration_days, created_at, provider_type, status, catalog_visible)
                VALUES (1, 'school-a.smartschool.be', 'School A', '', 0, 15, ?, 'REAL', 'ACTIVE', true)
                """, Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)));

        jdbcTemplate.update("INSERT INTO campuses (id, name, school_id) VALUES (1, 'Campus A', 1)");

        jdbcTemplate.update("INSERT INTO users (id, role_id, school_id, campus_id, review_blocked) VALUES ('teacher-1', 'TEACHER', 1, 1, false)");
        jdbcTemplate.update("INSERT INTO users (id, role_id, school_id, campus_id, review_blocked) VALUES ('student-1', 'STUDENT', 1, 1, false)");
        jdbcTemplate.update("INSERT INTO users (id, role_id, school_id, campus_id, review_blocked) VALUES ('student-2', 'STUDENT', 1, 1, false)");

        insertAuthSession("teacher-1", "ROLE_TEACHER", true);
        insertAuthSession("student-1", "ROLE_STUDENT", true);
        insertAuthSession("student-2", "ROLE_STUDENT", true);

        jdbcTemplate.update("""
                INSERT INTO books (id, isbn, title, author, pages, image_url, description, release_year, added_date, didactic, campus_id)
                VALUES (1, '9781', 'Boek Een', 'Auteur A', 100, null, '', 2024, ?, false, 1)
                """, Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)));

        jdbcTemplate.update("INSERT INTO book_copies (id, book_id, campus_id, physical_condition) VALUES (1, 1, 1, 'GOOD_CONDITION')");
        jdbcTemplate.update("INSERT INTO book_copies (id, book_id, campus_id, physical_condition) VALUES (2, 1, 1, 'GOOD_CONDITION')");
        jdbcTemplate.update("INSERT INTO book_copies (id, book_id, campus_id, physical_condition) VALUES (3, 1, 1, 'GOOD_CONDITION')");
        jdbcTemplate.update("INSERT INTO book_copies (id, book_id, campus_id, physical_condition) VALUES (4, 1, 1, 'GOOD_CONDITION')");

        jdbcTemplate.update("INSERT INTO loans (id, user_id, campus_id, loan_date, return_date, returned_date) VALUES (1, 'student-1', 1, ?, ?, null)",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15));
        jdbcTemplate.update("INSERT INTO loans (id, user_id, campus_id, loan_date, return_date, returned_date) VALUES (2, 'student-2', 1, ?, ?, ?)",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 10));
        jdbcTemplate.update("INSERT INTO loans (id, user_id, campus_id, loan_date, return_date, returned_date) VALUES (3, 'student-1', 1, ?, ?, ?)",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 20));

        jdbcTemplate.update("INSERT INTO loan_copies (loan_id, book_copy_id) VALUES (1, 1)");
        jdbcTemplate.update("INSERT INTO loan_copies (loan_id, book_copy_id) VALUES (1, 2)");
        jdbcTemplate.update("INSERT INTO loan_copies (loan_id, book_copy_id) VALUES (2, 3)");
        jdbcTemplate.update("INSERT INTO loan_copies (loan_id, book_copy_id) VALUES (3, 4)");
    }

    @Test
    void generateSchoolReport_computesReturnStatisticsCorrectly() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "teacher-1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));

        SchoolReportDTO result = service.generateSchoolReport(auth, null, 10, 0);

        assertThat(result.schoolId()).isEqualTo(1L);
        assertThat(result.schoolName()).isEqualTo("School A");

        assertThat(result.totalLoans()).isEqualTo(4);
        assertThat(result.activeLoans()).isEqualTo(2);
        assertThat(result.returnedOnTimeLoans()).isEqualTo(1);
        assertThat(result.returnedLateLoans()).isEqualTo(1);
        assertThat(result.onTimePercentage()).isEqualTo(50.0);
        assertThat(result.latePercentage()).isEqualTo(50.0);
    }

    @Test
    void generateSchoolReport_computesReturnStatisticsWhenNoReturns() {
        jdbcTemplate.update("DELETE FROM loan_copies WHERE loan_id IN (2, 3)");
        jdbcTemplate.update("DELETE FROM loans WHERE id IN (2, 3)");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "teacher-1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));

        SchoolReportDTO result = service.generateSchoolReport(auth, null, 10, 0);

        assertThat(result.returnedOnTimeLoans()).isZero();
        assertThat(result.returnedLateLoans()).isZero();
        assertThat(result.onTimePercentage()).isZero();
        assertThat(result.latePercentage()).isZero();
    }

    private UsernamePasswordAuthenticationToken teacherAuthentication(String userId) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }

    private void insertAuthSession(String userId, String role, boolean linked) {
        jdbcTemplate.update("""
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
                userId,
                false,
                null,
                Timestamp.valueOf(LocalDateTime.of(2099, 1, 1, 0, 0)),
                "jwt-" + userId,
                role,
                linked ? "access-" + userId : null,
                linked ? Timestamp.valueOf(LocalDateTime.of(2099, 1, 1, 0, 0)) : null,
                linked ? "refresh-" + userId : null,
                linked ? Timestamp.valueOf(LocalDateTime.of(2099, 1, 1, 0, 0)) : null);
    }
}
