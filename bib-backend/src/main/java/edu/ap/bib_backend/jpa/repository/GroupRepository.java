
package edu.ap.bib_backend.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findBySchoolIdAndName(Long schoolId, String name);

    boolean existsBySchoolIdAndName(Long schoolId, String name);

    @Query("select g.name from Group g where g.school.id = :schoolId order by g.name")
    List<String> findDistinctNamesBySchoolId(@Param("schoolId") Long schoolId);
}
