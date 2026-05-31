package edu.ap.bib_backend.service.book;

import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BookCopyIdentifierService {

    private static final String INTERNAL_PREFIX = "28";
    private static final long MAX_COPY_ID = 9_999_999_999L;

    private final BookCopyRepository bookCopyRepository;

    public BookCopyIdentifierService(BookCopyRepository bookCopyRepository) {
        this.bookCopyRepository = bookCopyRepository;
    }

    @Transactional
    public BookCopy assignIdentifier(BookCopy copy) {
        if (copy == null || copy.getPublicIdentifier() != null) {
            return copy;
        }

        if (copy.getId() == null) {
            throw new IllegalStateException("Boekexemplaar moet eerst opgeslagen worden voor een publieke code kan worden gemaakt.");
        }

        copy.setPublicIdentifier(generateIdentifier(copy.getId()));
        return bookCopyRepository.save(copy);
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void backfillMissingIdentifiers() {
        var copiesWithoutIdentifier = bookCopyRepository.findByPublicIdentifierIsNull();

        if (copiesWithoutIdentifier.isEmpty()) {
            return;
        }

        copiesWithoutIdentifier.forEach(copy -> copy.setPublicIdentifier(generateIdentifier(copy.getId())));
        bookCopyRepository.saveAll(copiesWithoutIdentifier);
        log.info("{} boekexemplaarcodes aangevuld.", copiesWithoutIdentifier.size());
    }

    public String generateIdentifier(Long copyId) {
        if (copyId == null || copyId <= 0 || copyId > MAX_COPY_ID) {
            throw new IllegalArgumentException("Ongeldig boekexemplaar-id voor publieke code: " + copyId);
        }

        String payload = INTERNAL_PREFIX + String.format("%010d", copyId);
        return payload + calculateEan13CheckDigit(payload);
    }

    private int calculateEan13CheckDigit(String payload) {
        int sum = 0;

        for (int index = 0; index < payload.length(); index++) {
            int digit = Character.digit(payload.charAt(index), 10);
            sum += (index % 2 == 0) ? digit : digit * 3;
        }

        return (10 - (sum % 10)) % 10;
    }
}
