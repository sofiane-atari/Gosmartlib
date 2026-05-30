package edu.ap.bib_backend.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.projection.report.SystemSchoolActivityProjection;

public interface UserRepository extends JpaRepository<User, String> {
    public List<User> findByIdContaining(String Id);

    Optional<User> findByOneRosterSourcedId(String oneRosterSourcedId);

    @Query("select u from User u where u.id = :userId and u.school.id = :schoolId")
    Optional<User> findByIdAndSchoolId(@Param("userId") String userId, @Param("schoolId") Long schoolId);

    long countBySchoolIdAndRole(Long schoolId, Role role);

    long countByRole(Role role);

    @Query("""
            select u.role.role
            from User u
            where u.id = :userId
            """)
    Optional<Role> findRoleByUserId(@Param("userId") String userId);

    List<User> findBySchoolIdAndRoleRole(Long schoolId, Role role);

    List<User> findBySchoolId(Long schoolId);

    @Query("""
            select distinct u
            from User u
            left join fetch u.groups
            where u.school.id = :schoolId
            """)
    List<User> findBySchoolIdWithGroups(@Param("schoolId") Long schoolId);

    @Query(value = """
            select count(distinct u.id)
            from users u
            where u.role_id = 'STUDENT'
            """, nativeQuery = true)
    long countStudentsForSystem();

    @Query(value = """
            select count(distinct u.id)
            from users u
            join auth_sessions s on s.user_id = u.id
            where s.smartschool_refresh_token is not null
            and (s.smartschool_refresh_token_expires_at is null or s.smartschool_refresh_token_expires_at > current_timestamp)
            """, nativeQuery = true)
    long countActiveUsersForSystem();

    @Query(value = """
            select count(distinct u.id)
            from users u
            join auth_sessions s on s.user_id = u.id
            where u.role_id = 'STUDENT'
            and s.smartschool_refresh_token is not null
            and (s.smartschool_refresh_token_expires_at is null or s.smartschool_refresh_token_expires_at > current_timestamp)
            """, nativeQuery = true)
    long countLinkedStudentsForSystem();

    @Query(value = """
            select s.id as schoolId,
                   s.name as schoolName,
                   count(distinct case when u.role_id = 'STUDENT' then u.id end) as totalStudents,
                   count(distinct case
                       when u.role_id = 'STUDENT'
                       and aus.smartschool_refresh_token is not null
                       and (aus.smartschool_refresh_token_expires_at is null or aus.smartschool_refresh_token_expires_at > current_timestamp)
                       then u.id
                   end) as linkedStudents,
                   count(lc.book_copy_id) as totalLoans,
                   sum(case when l.returned_date is null and lc.book_copy_id is not null then 1 else 0 end) as activeLoans,
                   s.last_sync_at as lastSyncAt
            from schools s
            left join users u on u.school_id = s.id
            left join auth_sessions aus on aus.user_id = u.id
            left join loans l on l.user_id = u.id
            left join loan_copies lc on lc.loan_id = l.id
            group by s.id, s.name, s.last_sync_at
            order by totalLoans desc, s.name asc
            """, nativeQuery = true)
    List<SystemSchoolActivityProjection> findSystemSchoolActivity();
}
