package edu.ap.bib_backend.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.MailSubscription;
import edu.ap.bib_backend.jpa.enums.MailType;

public interface MailSubscriptionRepository extends JpaRepository<MailSubscription, Long> {

    List<MailSubscription> findByBookIdAndMailType(Long bookId, MailType mailType);

    List<MailSubscription> findByUserId(String userId);

    Optional<MailSubscription> findByUserIdAndBookIdAndMailType(String userId, Long bookId, MailType mailType);

    boolean existsByUserIdAndBookIdAndMailType(String userId, Long bookId, MailType mailType);

    void deleteByUserIdAndBookIdAndMailType(String userId, Long bookId, MailType mailType);
}
