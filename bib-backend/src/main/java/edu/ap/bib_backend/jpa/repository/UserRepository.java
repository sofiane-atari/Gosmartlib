package edu.ap.bib_backend.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;

public interface UserRepository extends JpaRepository<User, String> {
    public List<User> findByIdContaining(String Id);

    Optional<User> findByOneRosterSourcedId(String oneRosterSourcedId);

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
}