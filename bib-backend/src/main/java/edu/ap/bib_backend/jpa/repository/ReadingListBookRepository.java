package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ReadingListBookRepository extends JpaRepository<ReadingListBook, Long> {

    void deleteByBookId(Long bookId);
    boolean existsByReadingList_IdAndBookId(Long readingListId, Long bookId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ReadingListBook rlb WHERE rlb.readingList.id = :readingListId AND rlb.bookId = :bookId")
    int deleteAllByReadingListIdAndBookId(@Param("readingListId") Long readingListId,
            @Param("bookId") Long bookId);
}
