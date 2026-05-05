package edu.ap.bib_backend.jpa.repository.gamification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.gamification.Badge;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findByCode(String code);

    boolean existsByCode(String code);

    List<Badge> findByActiveTrueOrderByDisplayOrderAsc();
}