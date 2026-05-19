
package edu.ap.bib_backend.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findBySchoolIdAndName(Long schoolId, String name);

    boolean existsBySchoolIdAndName(Long schoolId, String name);
}
