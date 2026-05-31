package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import org.springframework.data.domain.Pageable;
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
                        WHERE (
                               (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.SYSTEM
                                AND rl.campus.school.id = :schoolId)
                            OR (rl.campus.id = :campusId
                                AND (
                                     (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.PERSONAL AND rl.user.id = :userId)
                                  OR (:includeOwnShared = true
                                      AND rl.user.id = :userId
                                      AND rl.targetType <> edu.ap.bib_backend.jpa.entity.ReadingListTargetType.PERSONAL)
                                  OR (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.CLASS
                                      AND LOWER(COALESCE(rl.targetValue, '')) IN (:classCodes))
                                ))
                        )
                        """)
        List<ReadingList> findVisibleForUser(
                        @Param("userId") String userId,
                        @Param("campusId") Long campusId,
                        @Param("classCodes") List<String> classCodes,
                        @Param("yearCode") String yearCode,
                        @Param("schoolCode") String schoolCode,
                        @Param("schoolId") Long schoolId,
                        @Param("includeOwnShared") boolean includeOwnShared);

        @Query("""
                        SELECT rl
                        FROM ReadingList rl
                        WHERE rl.id = :id
                          AND (
                               (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.SYSTEM
                                AND rl.campus.school.id = :schoolId)
                            OR (rl.campus.id = :campusId
                                AND (
                                     (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.PERSONAL AND rl.user.id = :userId)
                                  OR (:includeOwnShared = true
                                      AND rl.user.id = :userId
                                      AND rl.targetType <> edu.ap.bib_backend.jpa.entity.ReadingListTargetType.PERSONAL)
                                  OR (rl.targetType = edu.ap.bib_backend.jpa.entity.ReadingListTargetType.CLASS
                                      AND LOWER(COALESCE(rl.targetValue, '')) IN (:classCodes))
                                ))
                          )
                        """)
        Optional<ReadingList> findVisibleById(
                        @Param("id") Long id,
                        @Param("userId") String userId,
                        @Param("campusId") Long campusId,
                        @Param("classCodes") List<String> classCodes,
                        @Param("yearCode") String yearCode,
                        @Param("schoolCode") String schoolCode,
                        @Param("schoolId") Long schoolId,
                        @Param("includeOwnShared") boolean includeOwnShared);

        Optional<ReadingList> findByNameIgnoreCaseAndUser_IdAndTargetTypeAndCampusId(
                        String name,
                        String userId,
                        ReadingListTargetType targetType,
                        Long campusId);

        Optional<ReadingList> findFirstByTargetTypeAndTargetValueIgnoreCaseAndCampusId(
                        ReadingListTargetType targetType,
                        String targetValue,
                        Long campusId);

        Optional<ReadingList> findFirstByTargetTypeAndCampusId(
                        ReadingListTargetType targetType,
                        Long campusId);

        Optional<ReadingList> findFirstByTargetTypeAndCampus_School_Id(
                        ReadingListTargetType targetType,
                        Long schoolId);

        Optional<ReadingList> findByPublicShareIdAndShareEnabledTrue(String publicShareId);

        boolean existsByPublicShareId(String publicShareId);

        List<ReadingList> findByTargetTypeAndCampus_IdOrderByNameAsc(
                        ReadingListTargetType targetType,
                        Long campusId);

        @Query("""
                        SELECT rl
                        FROM ReadingList rl
                        WHERE rl.targetType = :targetType
                          AND rl.campus.school.id = :schoolId
                          AND rl.showOnCatalog = true
                        ORDER BY rl.catalogFeaturedOrder ASC
                        """)
        List<ReadingList> findFeaturedBySchoolId(
                        @Param("targetType") ReadingListTargetType targetType,
                        @Param("schoolId") Long schoolId);

        @Query("""
                        SELECT rl
                        FROM ReadingList rl
                        WHERE rl.targetType = :targetType
                          AND rl.campus.school.id = :schoolId
                          AND rl.showOnCatalog = true
                        ORDER BY rl.catalogFeaturedOrder ASC
                        """)
        List<ReadingList> findTop2FeaturedBySchoolId(
                        @Param("targetType") ReadingListTargetType targetType,
                        @Param("schoolId") Long schoolId,
                        Pageable pageable);

}
