package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
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
            WHERE (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.PERSONAL AND rl.user.id = :userId)
               OR (:includeOwnShared = true
                   AND rl.user.id = :userId
                   AND rl.targetType <> edu.ap.bib_backend.jpa.entity.ReadingListTargetType.PERSONAL)
               OR (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.CLASS AND LOWER(COALESCE(rl.targetValue, '')) = LOWER(:classCode))
               OR (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.YEAR AND LOWER(COALESCE(rl.targetValue, '')) = LOWER(:yearCode))
               OR (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.SCHOOL AND LOWER(COALESCE(rl.targetValue, '')) = LOWER(:schoolCode))
               OR (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.SYSTEM)
            """)
    List<ReadingList> findVisibleForUser(@Param("userId") String userId,
            @Param("classCode") String classCode,
            @Param("yearCode") String yearCode,
            @Param("schoolCode") String schoolCode,
            @Param("includeOwnShared") boolean includeOwnShared);

    @Query("""
            SELECT rl
            FROM ReadingList rl
            WHERE rl.id = :id
              AND ((rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.PERSONAL AND rl.user.id = :userId)
                OR (:includeOwnShared = true
                    AND rl.user.id = :userId
                    AND rl.targetType <> edu.ap.bib_backend.jpa.entity.ReadingListTargetType.PERSONAL)
                OR (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.CLASS AND LOWER(COALESCE(rl.targetValue, '')) = LOWER(:classCode))
                OR (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.YEAR AND LOWER(COALESCE(rl.targetValue, '')) = LOWER(:yearCode))
                OR (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.SCHOOL AND LOWER(COALESCE(rl.targetValue, '')) = LOWER(:schoolCode))
                OR (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.SYSTEM))
            """)
    Optional<ReadingList> findVisibleById(@Param("id") Long id,
            @Param("userId") String userId,
            @Param("classCode") String classCode,
            @Param("yearCode") String yearCode,
            @Param("schoolCode") String schoolCode,
            @Param("includeOwnShared") boolean includeOwnShared);

    Optional<ReadingList> findByNameIgnoreCaseAndUser_IdAndTargetType(String name,
            String userId,
            ReadingListTargetType targetType);

    Optional<ReadingList> findFirstByTargetTypeAndTargetValueIgnoreCase(ReadingListTargetType targetType, String targetValue);

    Optional<ReadingList> findFirstByTargetType(ReadingListTargetType targetType);
}
