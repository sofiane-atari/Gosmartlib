package edu.ap.bib_backend.jpa.repository.lessontips;


import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;

import java.util.List;
import java.util.Optional;

public interface LessonTipRepository extends JpaRepository<LessonTip, Long> {

    @EntityGraph(attributePaths = {"bookLinks", "bookLinks.book"})
    List<LessonTip> findAllByOrderByUpdatedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"bookLinks", "bookLinks.book"})
    Optional<LessonTip> findById(Long id);

    List<LessonTip> findTop3ByBookLinksBookIdOrderByUpdatedAtDesc(Long bookId);

    long countByBookLinksBookId(Long bookId);

    List<LessonTip> findAllByBookLinksBookIdOrderByUpdatedAtDesc(Long bookId);

}