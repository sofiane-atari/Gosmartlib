package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
                SELECT t
                FROM Tag t
                WHERE (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Tag> findBySearch(
            @Param("search") String search,
            Pageable pageable);

    @Query("""
                SELECT COUNT(t)
                FROM Tag t
                WHERE SIZE(t.books) = 0
            """)
    Long countUnusedTags();
}