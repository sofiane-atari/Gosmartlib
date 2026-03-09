package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.Label;
import edu.ap.bib_backend.projection.LabelWithCount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LabelRepository extends JpaRepository<Label, String> {
    Page<Label> findAll(Pageable pageable);

    @Query(value = "SELECT l.name as name, l.color as color, l.description as description, COUNT(b) as bookCount " +
            "FROM Label l LEFT JOIN l.books b GROUP BY l.name, l.color, l.description", countQuery = "SELECT COUNT(l) FROM Label l")
    Page<LabelWithCount> findAllIncludeBookCount(Pageable pageable);

}