package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.Label;
import edu.ap.bib_backend.projection.LabelWithCount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabelRepository extends JpaRepository<Label, String> {

    Page<Label> findAll(Pageable pageable);

    @Query(value = "SELECT l.name as name, l.color as color, l.description as description, COUNT(b) as bookCount "
            + "FROM Label l LEFT JOIN l.books b GROUP BY l.name, l.color, l.description", countQuery = "SELECT COUNT(l) FROM Label l")
    Page<LabelWithCount> findAllIncludeBookCount(Pageable pageable);

    @Query(value = "SELECT l.name as name, l.color as color, l.description as description, " +
            "COUNT(b) as bookCount " +
            "FROM Label l LEFT JOIN l.books b " +
            "WHERE (l.name LIKE %:search% OR l.description LIKE %:search% OR l.color LIKE %:search%) " +
            "GROUP BY l.name, l.color, l.description " +
            "HAVING (:bookCount IS NULL OR COUNT(b) = :bookCount)", countQuery = "SELECT COUNT(l) FROM Label l LEFT JOIN l.books b "
                    +
                    "WHERE (l.name LIKE %:search% OR l.description LIKE %:search% OR l.color LIKE %:search%) " +
                    "GROUP BY l HAVING (:bookCount IS NULL OR COUNT(b) = :bookCount)")
    Page<LabelWithCount> findAllFiltered(
            @Param("search") String search,
            @Param("bookCount") Integer bookCount,
            Pageable pageable);

    @Query(value = "SELECT l " +
            "FROM Label l LEFT JOIN l.books b " +
            "WHERE l.name LIKE %:search% " +
            "OR l.description LIKE %:search% " +
            "OR l.color LIKE %:search% ", countQuery = "SELECT COUNT(l) FROM Label l WHERE l.name LIKE %:search% "
                    +
                    "OR l.description LIKE %:search% OR l.color LIKE %:search%")
    Page<Label> searchByNamerOrDescriptionOrColor(@Param("search") String search,
            Pageable pageable);
}