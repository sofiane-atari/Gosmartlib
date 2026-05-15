package edu.ap.bib_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ap.bib_backend.dto.MailSubscriptionDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.MailSubscription;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.MailType;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.MailSubscriptionRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.MailSubscriptionMapper;

@ExtendWith(MockitoExtension.class)
class MailSubscriptionServiceTest {

    @Mock
    private MailSubscriptionRepository mailSubscriptionRepository;
    @Mock
    private MailSubscriptionMapper mailSubscriptionMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private MailSubscriptionService mailSubscriptionService;

    @Test
    void GivenUserAlreadySubscribedToBook_WhenSubscribe_ThenThrowsIllegalStateException() {
        when(mailSubscriptionRepository.existsByUserIdAndBookIdAndMailType("user1", 1L, MailType.AVAILABLE_BOOK))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> mailSubscriptionService.subscribe("user1", 1L));

        assertEquals("Je bent al geabonneerd op dit boek.", exception.getMessage());
    }

    @Test
    void GivenUserNotFound_WhenSubscribe_ThenThrowsIllegalStateException() {
        when(mailSubscriptionRepository.existsByUserIdAndBookIdAndMailType("user1", 1L, MailType.AVAILABLE_BOOK))
                .thenReturn(false);
        when(userRepository.findById("user1")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> mailSubscriptionService.subscribe("user1", 1L));

        assertEquals("Gebruiker niet gevonden.", exception.getMessage());
    }

    @Test
    void GivenBookNotFound_WhenSubscribe_ThenThrowsIllegalStateException() {
        User user = new User();
        user.setId("user1");

        when(mailSubscriptionRepository.existsByUserIdAndBookIdAndMailType("user1", 1L, MailType.AVAILABLE_BOOK))
                .thenReturn(false);
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> mailSubscriptionService.subscribe("user1", 1L));

        assertEquals("Boek niet gevonden.", exception.getMessage());
    }

    @Test
    void GivenValidUserAndBook_WhenSubscribe_ThenCreatesAndReturnsSubscription() {
        User user = new User();
        user.setId("user1");
        Book book = new Book();
        book.setId(1L);
        MailSubscription subscription = new MailSubscription();
        MailSubscription saved = new MailSubscription();
        MailSubscriptionDTO expectedDto = MailSubscriptionDTO.builder().id(1L).bookId(1L).build();

        when(mailSubscriptionRepository.existsByUserIdAndBookIdAndMailType("user1", 1L, MailType.AVAILABLE_BOOK))
                .thenReturn(false);
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(mailSubscriptionRepository.save(any(MailSubscription.class))).thenReturn(saved);
        when(mailSubscriptionMapper.toDto(saved)).thenReturn(expectedDto);

        MailSubscriptionDTO result = mailSubscriptionService.subscribe("user1", 1L);

        assertNotNull(result);
        assertEquals(1L, result.bookId());
        verify(mailSubscriptionRepository).save(any(MailSubscription.class));
    }

    @Test
    void GivenNoSubscriptionExists_WhenUnsubscribe_ThenThrowsIllegalStateException() {
        when(mailSubscriptionRepository.findByUserIdAndBookIdAndMailType("user1", 1L, MailType.AVAILABLE_BOOK))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> mailSubscriptionService.unsubscribe("user1", 1L));

        assertEquals("Geen abonnement gevonden voor dit boek.", exception.getMessage());
    }

    @Test
    void GivenSubscriptionExists_WhenUnsubscribe_ThenDeletesSubscription() {
        MailSubscription subscription = new MailSubscription();
        subscription.setId(1L);
        when(mailSubscriptionRepository.findByUserIdAndBookIdAndMailType("user1", 1L, MailType.AVAILABLE_BOOK))
                .thenReturn(Optional.of(subscription));

        mailSubscriptionService.unsubscribe("user1", 1L);

        verify(mailSubscriptionRepository).delete(subscription);
    }

    @Test
    void GivenUserHasSubscriptions_WhenFindSubscriptionsByUser_ThenReturnsList() {
        MailSubscription sub1 = new MailSubscription();
        MailSubscriptionDTO dto1 = MailSubscriptionDTO.builder().id(1L).bookId(1L).build();
        when(mailSubscriptionRepository.findByUserId("user1")).thenReturn(List.of(sub1));
        when(mailSubscriptionMapper.toDto(sub1)).thenReturn(dto1);

        List<MailSubscriptionDTO> result = mailSubscriptionService.findSubscriptionsByUser("user1");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).bookId());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 42L, 99L})
    void GivenBookHasSubscribers_WhenFindSubscriptionsByBookId_ThenReturnsList(Long bookId) {
        MailSubscription sub = new MailSubscription();
        when(mailSubscriptionRepository.findByBookIdAndMailType(bookId, MailType.AVAILABLE_BOOK))
                .thenReturn(List.of(sub));

        List<MailSubscription> result = mailSubscriptionService.findSubscriptionsByBookId(bookId);

        assertEquals(1, result.size());
    }

    @Test
    void GivenNoSubscriptions_WhenFindSubscriptionsByUser_ThenReturnsEmptyList() {
        when(mailSubscriptionRepository.findByUserId("user1")).thenReturn(List.of());

        List<MailSubscriptionDTO> result = mailSubscriptionService.findSubscriptionsByUser("user1");

        assertTrue(result.isEmpty());
    }

    @Test
    void GivenNoSubscribers_WhenFindSubscriptionsByBookId_ThenReturnsEmptyList() {
        when(mailSubscriptionRepository.findByBookIdAndMailType(1L, MailType.AVAILABLE_BOOK))
                .thenReturn(List.of());

        List<MailSubscription> result = mailSubscriptionService.findSubscriptionsByBookId(1L);

        assertTrue(result.isEmpty());
    }
}
