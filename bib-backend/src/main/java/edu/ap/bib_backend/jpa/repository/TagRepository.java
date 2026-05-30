package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, Long> {

    @Query("""
                SELECT COUNT(t) > 0
                FROM Tag t
                WHERE LOWER(t.name) = LOWER(:name)
                  AND t.school.id = :schoolId
            """)
    boolean existsByNameInSchool(@Param("name") String name, @Param("schoolId") Long schoolId);

    @Query("""
                SELECT COUNT(t) > 0
                FROM Tag t
                WHERE LOWER(t.name) = LOWER(:name)
                  AND t.id <> :id
                  AND t.school.id = :schoolId
            """)
    boolean existsByNameInSchoolAndIdNot(@Param("name") String name, @Param("schoolId") Long schoolId, @Param("id") Long id);

    @Query("""
                SELECT t
                FROM Tag t
                WHERE t.id = :id
                  AND t.school.id = :schoolId
            """)
    java.util.Optional<Tag> findByIdInSchool(@Param("id") Long id, @Param("schoolId") Long schoolId);

    @Query("""
                SELECT t
                FROM Tag t
                WHERE t.school.id = :schoolId
                  AND (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Tag> findBySearch(
            @Param("schoolId") Long schoolId,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
                SELECT COUNT(t)
                FROM Tag t
                WHERE t.school.id = :schoolId
                  AND SIZE(t.books) = 0
            """)
    Long countUnusedTags(@Param("schoolId") Long schoolId);

    @Query("""
                SELECT COUNT(DISTINCT b.id)
                FROM Tag t
                LEFT JOIN t.books b
                WHERE t.id = :tagId
            """)
    Long countBookAndCopyLinks(@Param("tagId") Long tagId);

    @Query("""
                SELECT COUNT(t)
                FROM Tag t
                WHERE t.school.id = :schoolId
            """)
    Long countBySchoolIdOrNull(@Param("schoolId") Long schoolId);
}
