package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.report.SystemReportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SystemReportServiceIntegrationTest {

    static {
        System.setProperty("JASYPT_ENCRYPTOR_PASSWORD", "test-encryption-password");
    }

    @Autowired
    private SystemReportService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedData() {
        jdbcTemplate.update("DELETE FROM loan_copies");
        jdbcTemplate.update("DELETE FROM loans");
        jdbcTemplate.update("DELETE FROM book_copies");
        jdbcTemplate.update("DELETE FROM books");
        jdbcTemplate.update("DELETE FROM auth_sessions");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM campuses");
        jdbcTemplate.update("DELETE FROM schools");
        jdbcTemplate.update("DELETE FROM roles");

        jdbcTemplate.update("INSERT INTO roles (role) VALUES ('STUDENT')");
        jdbcTemplate.update("INSERT INTO roles (role) VALUES ('ADMIN')");

        jdbcTemplate.update("""
                INSERT INTO schools (id, domain_name, name, address, postal_code, default_loan_duration_days, created_at)
                VALUES (1, 'school-a.smartschool.be', 'School A', '', 0, 15, ?)
                """, Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)));
        jdbcTemplate.update("""
                INSERT INTO schools (id, domain_name, name, address, postal_code, default_loan_duration_days, created_at)
                VALUES (2, 'school-b.smartschool.be', 'School B', '', 0, 15, ?)
                """, Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)));

        jdbcTemplate.update("INSERT INTO campuses (id, name, school_id) VALUES (1, 'Campus A', 1)");
        jdbcTemplate.update("INSERT INTO campuses (id, name, school_id) VALUES (2, 'Campus B', 2)");

        jdbcTemplate.update("INSERT INTO users (id, role_id, school_id, campus_id, review_blocked) VALUES ('admin-1', 'ADMIN', 1, 1, false)");
        jdbcTemplate.update("INSERT INTO users (id, role_id, school_id, campus_id, review_blocked) VALUES ('student-1', 'STUDENT', 1, 1, false)");
        jdbcTemplate.update("INSERT INTO users (id, role_id, school_id, campus_id, review_blocked) VALUES ('student-2', 'STUDENT', 1, 1, false)");
        jdbcTemplate.update("INSERT INTO users (id, role_id, school_id, campus_id, review_blocked) VALUES ('student-3', 'STUDENT', 2, 2, false)");

        insertAuthSession("student-1", "ROLE_STUDENT", true);
        insertAuthSession("student-2", "ROLE_STUDENT", false);
        insertAuthSession("student-3", "ROLE_STUDENT", true);

        jdbcTemplate.update("""
                INSERT INTO books (id, isbn, title, author, pages, image_url, description, release_year, added_date, didactic, campus_id)
                VALUES (1, '9781', 'Boek Een', 'Auteur A', 100, null, '', 2024, ?, false, 1)
                """, Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)));
        jdbcTemplate.update("""
                INSERT INTO books (id, isbn, title, author, pages, image_url, description, release_year, added_date, didactic, campus_id)
                VALUES (2, '9782', 'Boek Twee', 'Auteur B', 120, null, '', 2024, ?, false, 2)
                """, Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 0, 0)));

        jdbcTemplate.update("INSERT INTO book_copies (id, book_id, campus_id, physical_condition) VALUES (1, 1, 1, 'GOOD_CONDITION')");
        jdbcTemplate.update("INSERT INTO book_copies (id, book_id, campus_id, physical_condition) VALUES (2, 1, 1, 'GOOD_CONDITION')");
        jdbcTemplate.update("INSERT INTO book_copies (id, book_id, campus_id, physical_condition) VALUES (3, 2, 2, 'GOOD_CONDITION')");

        jdbcTemplate.update("INSERT INTO loans (id, user_id, campus_id, loan_date, return_date, returned_date) VALUES (1, 'student-1', 1, ?, ?, null)",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15));
        jdbcTemplate.update("INSERT INTO loans (id, user_id, campus_id, loan_date, return_date, returned_date) VALUES (2, 'student-3', 2, ?, ?, ?)",
                LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 16), LocalDate.of(2026, 5, 10));

        jdbcTemplate.update("INSERT INTO loan_copies (loan_id, book_copy_id) VALUES (1, 1)");
        jdbcTemplate.update("INSERT INTO loan_copies (loan_id, book_copy_id) VALUES (1, 2)");
        jdbcTemplate.update("INSERT INTO loan_copies (loan_id, book_copy_id) VALUES (2, 3)");
    }

    @Test
    void generateSystemReport_aggregatesPersistedDataAcrossSchools() {
        SystemReportDTO result = service.generateSystemReport(adminAuthentication());

        assertThat(result.totalLoans()).isEqualTo(3);
        assertThat(result.activeLoans()).isEqualTo(2);
        assertThat(result.totalSchools()).isEqualTo(2);
        assertThat(result.totalBooks()).isEqualTo(2);
        assertThat(result.totalCopies()).isEqualTo(3);
        assertThat(result.activeUsers()).isEqualTo(2);
        assertThat(result.linkedUsersPercentage()).isEqualTo(66.67);

        assertThat(result.loanVolume()).hasSize(1);
        assertThat(result.loanVolume().get(0).period()).isEqualTo("2026-05");
        assertThat(result.loanVolume().get(0).loanCount()).isEqualTo(2);

        assertThat(result.popularBooks()).hasSize(2);
        assertThat(result.popularBooks().get(0).title()).isEqualTo("Boek Een");
        assertThat(result.popularBooks().get(0).loanCount()).isEqualTo(2);

        assertThat(result.schools()).hasSize(2);
        assertThat(result.schools().get(0).schoolName()).isEqualTo("School A");
        assertThat(result.schools().get(0).totalStudents()).isEqualTo(2);
        assertThat(result.schools().get(0).linkedStudents()).isEqualTo(1);
        assertThat(result.schools().get(0).totalLoans()).isEqualTo(2);
        assertThat(result.schools().get(0).activeLoans()).isEqualTo(2);
        assertThat(result.schools().get(0).averageLoansPerStudent()).isEqualTo(1.0);
    }

    private UsernamePasswordAuthenticationToken adminAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin-1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
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
