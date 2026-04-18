package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.projection.GenreWithCount;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenreRepository extends JpaRepository<Genre, Long> {

        Optional<Genre> findByName(String name);

        boolean existsByName(String name);

        Page<Genre> findAll(Pageable pageable);

        @Query(value = "SELECT l.name as name, l.color as color, l.description as description, COUNT(b) as bookCount "
                        + "FROM Genre l LEFT JOIN l.books b GROUP BY l.name, l.color, l.description", countQuery = "SELECT COUNT(l) FROM Genre l")
        Page<GenreWithCount> findAllIncludeBookCount(Pageable pageable);

        @Query(value = "SELECT l.name as name, l.color as color, l.description as description, " +
                        "COUNT(b) as bookCount " +
                        "FROM Genre l LEFT JOIN l.books b " +
                        "WHERE (l.name LIKE %:search% OR l.description LIKE %:search% OR l.color LIKE %:search%) " +
                        "GROUP BY l.name, l.color, l.description " +
                        "HAVING (:bookCount IS NULL OR COUNT(b) = :bookCount)", countQuery = "SELECT COUNT(l) FROM Genre l LEFT JOIN l.books b "
                                        +
                                        "WHERE (l.name LIKE %:search% OR l.description LIKE %:search% OR l.color LIKE %:search%) "
                                        +
                                        "GROUP BY l HAVING (:bookCount IS NULL OR COUNT(b) = :bookCount)")
        Page<GenreWithCount> findAllFiltered(
                        @Param("search") String search,
                        @Param("bookCount") Integer bookCount,
                        Pageable pageable);

        @Query(value = "SELECT l " +
                        "FROM Genre l LEFT JOIN l.books b " +
                        "WHERE l.name LIKE %:search% " +
                        "OR l.description LIKE %:search% " +
                        "OR l.color LIKE %:search% ", countQuery = "SELECT COUNT(l) FROM Genre l WHERE l.name LIKE %:search% "
                                        +
                                        "OR l.description LIKE %:search% OR l.color LIKE %:search%")
        Page<Genre> searchByNameOrDescriptionOrColor(@Param("search") String search,
                        Pageable pageable);
}