package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReadingListBookRepository extends JpaRepository<ReadingListBook, Long> {
    Optional<ReadingListBook> findByReadingList_IdAndBookId(Long readingListId, Long bookId);
}


