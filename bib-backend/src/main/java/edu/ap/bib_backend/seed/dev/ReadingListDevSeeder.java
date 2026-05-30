package edu.ap.bib_backend.seed.dev;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import edu.ap.bib_backend.service.ReadingListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ReadingListDevSeeder {

    private final ReadingListService readingListService;

    public ReadingListDevSeeder(ReadingListService readingListService) {
        this.readingListService = readingListService;
    }

    private static final List<String> READING_LIST_NAMES = List.of(
            "Mijn favoriete programmeerboeken",
            "Leeslijst voor dit semester",
            "Boeken over software-architectuur",
            "Voorbereiding op examens",
            "Must-reads voor developers"
    );

    public ReadingListSeedResult seed(String userId, List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            log.info("Geen boeken gevonden, leeslijst seeding overgeslagen");
            return new ReadingListSeedResult(0, 0, List.of());
        }

        int listsCreated = 0;
        int favoritesAdded = 0;
        List<Long> createdListIds = new ArrayList<>();

        org.springframework.security.core.Authentication auth =
                DevSeedAuthentication.forUser(userId, edu.ap.bib_backend.jpa.enums.Role.STUDENT);
        DevSeedAuthentication.setContext(auth);

        try {
            for (int i = 0; i < Math.min(2, READING_LIST_NAMES.size()); i++) {
                String listName = READING_LIST_NAMES.get(i);
                try {
                    ReadingListDTO dto = new ReadingListDTO(listName, ReadingListTargetType.PERSONAL, null);
                    ReadingList list = readingListService.createReadingList(userId, dto);
                    listsCreated++;
                    createdListIds.add(list.getId());

                    int maxBooks = Math.min(3 + i * 2, bookIds.size());
                    int startIdx = i * 3 % bookIds.size();
                    for (int j = 0; j < maxBooks; j++) {
                        Long bookId = bookIds.get((startIdx + j) % bookIds.size());
                        try {
                            readingListService.addBookToList(userId, list.getId(), bookId);
                            log.debug("Boek toegevoegd aan leeslijst '{}': boekId={}", listName, bookId);
                        } catch (IllegalArgumentException e) {
                            if (e.getMessage() != null && e.getMessage().contains("al")) {
                                log.debug("Boek zit al in leeslijst '{}'", listName);
                            } else {
                                log.warn("Kon boek niet toevoegen aan leeslijst: {}", e.getMessage());
                            }
                        }
                    }
                    log.info("Leeslijst aangemaakt: '{}'", listName);
                } catch (Exception e) {
                    log.warn("Kon leeslijst niet aanmaken '{}': {}", listName, e.getMessage());
                }
            }

            int maxFavorites = Math.min(3, bookIds.size());
            for (int i = 0; i < maxFavorites; i++) {
                Long bookId = bookIds.get(i);
                try {
                    readingListService.addBookToFavorites(userId, bookId);
                    favoritesAdded++;
                    log.debug("Boek toegevoegd aan favorieten: boekId={}", bookId);
                } catch (IllegalArgumentException e) {
                    if (e.getMessage() != null && e.getMessage().contains("al")) {
                        log.debug("Boek zit al in favorieten");
                    } else {
                        log.warn("Kon boek niet toevoegen aan favorieten: {}", e.getMessage());
                    }
                } catch (Exception e) {
                    log.warn("Kon boek niet toevoegen aan favorieten: {}", e.getMessage());
                }
            }
        } finally {
            DevSeedAuthentication.clearContext();
        }

        log.info("Leeslijst seeding voltooid voor gebruiker {}: {} lijsten, {} favorieten",
                userId, listsCreated, favoritesAdded);
        return new ReadingListSeedResult(listsCreated, favoritesAdded, createdListIds);
    }

    public record ReadingListSeedResult(int listsCreated, int favoritesAdded, List<Long> readingListIds) {}
}