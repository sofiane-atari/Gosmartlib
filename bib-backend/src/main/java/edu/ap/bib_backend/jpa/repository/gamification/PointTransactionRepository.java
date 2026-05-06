package edu.ap.bib_backend.jpa.repository.gamification;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.entity.gamification.PointTransaction;
import edu.ap.bib_backend.jpa.enums.gamification.PointTransactionType;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    boolean existsByProfileAndTypeAndSourceTypeAndSourceId(
            GamificationProfile profile,
            PointTransactionType type,
            String sourceType,
            String sourceId
    );

    List<PointTransaction> findTop10ByProfileOrderByCreatedAtDesc(GamificationProfile profile);

    Page<PointTransaction> findByProfileOrderByCreatedAtDesc(
            GamificationProfile profile,
            Pageable pageable
    );
}