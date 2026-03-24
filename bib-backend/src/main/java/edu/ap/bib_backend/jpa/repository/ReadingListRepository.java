package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.ReadingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingListRepository extends JpaRepository<ReadingList, Long> {
    @Query("""
            SELECT rl
            FROM ReadingList rl
            WHERE ((COALESCE(rl.isClassList, false) = false) AND rl.user.id = :userId)
               OR ((COALESCE(rl.isClassList, false) = true) AND rl.classCode = :classCode)
            """)
    List<ReadingList> findVisibleForUser(@Param("userId") String userId, @Param("classCode") String classCode);

    @Query("""
            SELECT rl
            FROM ReadingList rl
            WHERE rl.id = :id
              AND (((COALESCE(rl.isClassList, false) = false) AND rl.user.id = :userId)
                OR ((COALESCE(rl.isClassList, false) = true) AND rl.classCode = :classCode))
            """)
    Optional<ReadingList> findVisibleById(@Param("id") Long id,
            @Param("userId") String userId,
            @Param("classCode") String classCode);

    Optional<ReadingList> findByNameIgnoreCaseAndUser_Id(String name, String userId);

    Optional<ReadingList> findByClassCodeAndIsClassListTrue(String classCode);
}
