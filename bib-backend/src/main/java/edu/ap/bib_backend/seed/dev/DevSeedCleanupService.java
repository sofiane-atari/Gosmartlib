package edu.ap.bib_backend.seed.dev;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Tag;
import edu.ap.bib_backend.jpa.enums.MailType;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookLoanRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.MailSubscriptionRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.TagRepository;
import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DevSeedCleanupService {

    private final MailSubscriptionRepository mailSubscriptionRepository;
    private final LessonTipRepository lessonTipRepository;
    private final ReadingListRepository readingListRepository;
    private final ReadingListBookRepository readingListBookRepository;
    private final BookLoanRepository bookLoanRepository;
    private final BookCopyRepository bookCopyRepository;
    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final TagRepository tagRepository;

    @Transactional
    public void clear(List<Long> bookIds, Map<String, Long> tagNameToId,
                       List<Long> readingListIds, List<Long> lessonTipIds,
                       List<MailSubscriptionDevSeeder.SubscriptionKey> mailSubscriptions) {
        log.info("=== Dev seed cleanup gestart ===");

        int deletedMailSubs = 0;
        for (MailSubscriptionDevSeeder.SubscriptionKey sub : mailSubscriptions) {
            mailSubscriptionRepository.deleteByUserIdAndBookIdAndMailType(
                    sub.userId(), sub.bookId(), MailType.AVAILABLE_BOOK);
            deletedMailSubs++;
        }
        log.info("{} mailabonnementen verwijderd", deletedMailSubs);

        int deletedLessonTips = 0;
        for (Long tipId : lessonTipIds) {
            lessonTipRepository.deleteById(tipId);
            deletedLessonTips++;
        }
        log.info("{} lestips verwijderd", deletedLessonTips);

        int deletedReadingListBooks = 0;
        for (Long bookId : bookIds) {
            readingListBookRepository.deleteByBookId(bookId);
            deletedReadingListBooks++;
        }
        log.info("{} leeslijst-boek relaties verwijderd", deletedReadingListBooks);

        int deletedReadingLists = 0;
        for (Long listId : readingListIds) {
            readingListRepository.deleteById(listId);
            deletedReadingLists++;
        }
        log.info("{} leeslijsten verwijderd", deletedReadingLists);

        for (Long bookId : bookIds) {
            bookLoanRepository.deleteByCopyBookId(bookId);
        }
        log.info("Leningen verwijderd voor {} boeken", bookIds.size());

        for (Long bookId : bookIds) {
            bookCopyRepository.deleteByBookId(bookId);
        }
        log.info("Exemplaren verwijderd voor {} boeken", bookIds.size());

        for (Long bookId : bookIds) {
            reviewRepository.deleteByBookId(bookId);
        }
        log.info("Reviews verwijderd voor {} boeken", bookIds.size());

        int deletedBooks = 0;
        for (Long bookId : bookIds) {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book != null) {
                bookRepository.delete(book);
                deletedBooks++;
            }
        }
        log.info("{} boeken verwijderd", deletedBooks);

        int deletedTags = 0;
        for (Map.Entry<String, Long> entry : tagNameToId.entrySet()) {
            Long tagId = entry.getValue();
            Tag tag = tagRepository.findById(tagId).orElse(null);
            if (tag != null && tag.getBooks().isEmpty()) {
                tagRepository.delete(tag);
                deletedTags++;
            } else if (tag != null) {
                log.info("Tag '{}' niet verwijderd: nog gekoppeld aan {} boeken",
                        entry.getKey(), tag.getBooks().size());
            }
        }
        log.info("{} tags verwijderd", deletedTags);

        log.info("=== Dev seed cleanup voltooid ===");
    }
}
