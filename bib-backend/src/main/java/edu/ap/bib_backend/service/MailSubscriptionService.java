package edu.ap.bib_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.MailSubscriptionDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.MailSubscription;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.MailType;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.MailSubscriptionRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.MailSubscriptionMapper;

@Service
public class MailSubscriptionService {

    private final MailSubscriptionRepository mailSubscriptionRepository;
    private final MailSubscriptionMapper mailSubscriptionMapper;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public MailSubscriptionService(
            MailSubscriptionRepository mailSubscriptionRepository,
            MailSubscriptionMapper mailSubscriptionMapper,
            UserRepository userRepository,
            BookRepository bookRepository) {
        this.mailSubscriptionRepository = mailSubscriptionRepository;
        this.mailSubscriptionMapper = mailSubscriptionMapper;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public MailSubscriptionDTO subscribe(String userId, Long bookId) {
        if (mailSubscriptionRepository.existsByUserIdAndBookIdAndMailType(userId, bookId, MailType.AVAILABLE_BOOK)) {
            throw new IllegalStateException("Je bent al geabonneerd op dit boek.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Gebruiker niet gevonden."));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalStateException("Boek niet gevonden."));

        if (!user.getSchool().getId().equals(book.getCampus().getSchool().getId())) {
            throw new IllegalStateException("Je kunt je niet abonneren op boeken van een andere school.");
        }

        MailSubscription subscription = new MailSubscription();
        subscription.setUser(user);
        subscription.setBook(book);
        subscription.setMailType(MailType.AVAILABLE_BOOK);
        MailSubscription saved = mailSubscriptionRepository.save(subscription);
        return mailSubscriptionMapper.toDto(saved);
    }

    @Transactional
    public void unsubscribe(String userId, Long bookId) {
        MailSubscription subscription = mailSubscriptionRepository
                .findByUserIdAndBookIdAndMailType(userId, bookId, MailType.AVAILABLE_BOOK)
                .orElseThrow(() -> new IllegalStateException("Geen abonnement gevonden voor dit boek."));

        mailSubscriptionRepository.delete(subscription);
    }

    @Transactional(readOnly = true)
    public List<MailSubscriptionDTO> findSubscriptionsByUser(String userId) {
        return mailSubscriptionRepository.findByUserId(userId)
                .stream()
                .map(mailSubscriptionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MailSubscription> findSubscriptionsByBookId(Long bookId) {
        return mailSubscriptionRepository.findByBookIdAndMailType(bookId, MailType.AVAILABLE_BOOK);
    }
}
