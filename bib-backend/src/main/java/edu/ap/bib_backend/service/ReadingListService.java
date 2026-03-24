package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReadingListService {
    private final ReadingListRepository readingListRepository;
    private final ReadingListBookRepository readingListBookRepository;
    private final UserRepository userRepository;
    private static final String FAVORITES_NAME = "Favorieten";
    private static final String FALLBACK_CLASS_CODE = "GENERAL";

    public ReadingListService(ReadingListRepository readingListRepository,
            ReadingListBookRepository readingListBookRepository,
            UserRepository userRepository) {
        this.readingListRepository = readingListRepository;
        this.readingListBookRepository = readingListBookRepository;
        this.userRepository = userRepository;
    }

    public ReadingList createReadingList(String userId, ReadingListDTO dto) {
        if (dto.name() == null || dto.name().trim().isEmpty()) {
            throw new IllegalArgumentException("name mag niet null of leeg zijn");
        }

        User user = getUserOrThrow(userId);

        ReadingList list = new ReadingList();
        list.setName(dto.name().trim());
        list.setUser(user);
        list.setClassCode(resolveClassCode(user));
        list.setIsClassList(false);

        return readingListRepository.save(list);
    }

    public List<ReadingList> getAllLists(String userId) {
        User user = getUserOrThrow(userId);
        ensureClassReadingListExists(user);
        return readingListRepository.findVisibleForUser(userId, resolveClassCode(user));
    }

    public ReadingList getListById(String userId, Long id) {
        User user = getUserOrThrow(userId);
        return readingListRepository.findVisibleById(id, userId, resolveClassCode(user))
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst met id " + id + " bestaat niet"));
    }

    public void deleteReadingList(String userId, Long id) {
        ReadingList list = getListById(userId, id);

        if (isFavoritesList(list.getName()) || isClassList(list)) {
            throw new IllegalArgumentException("Deze leeslijst kan niet verwijderd worden");
        }

        readingListRepository.deleteById(id);
    }

    public ReadingListBook addBookToList(String userId, Long listId, Long bookId) {
        if (bookId == null) {
            throw new IllegalArgumentException("Boek ID mag niet null zijn");
        }

        ReadingList list = getListById(userId, listId);

        boolean exists = readingListBookRepository.existsByReadingList_IdAndBookId(listId, bookId);
        if (exists) {
            throw new IllegalArgumentException("Boek zit al in deze leeslijst");
        }

        ReadingListBook newBook = new ReadingListBook();
        newBook.setReadingList(list);
        newBook.setBookId(bookId);

        return readingListBookRepository.save(newBook);
    }

    public ReadingListBook addBookToFavorites(String userId, Long bookId) {
        User user = getUserOrThrow(userId);
        ReadingList favoritesList = readingListRepository.findByNameIgnoreCaseAndUser_Id(FAVORITES_NAME, userId)
                .orElseGet(() -> {
                    ReadingList newFavorites = new ReadingList();
                    newFavorites.setName(FAVORITES_NAME);
                    newFavorites.setUser(user);
                    newFavorites.setClassCode(resolveClassCode(user));
                    newFavorites.setIsClassList(false);
                    return readingListRepository.save(newFavorites);
                });

        return addBookToList(userId, favoritesList.getId(), bookId);
    }

    public void removeBookFromList(String userId, Long listId, Long bookId) {
        if (bookId == null) {
            throw new IllegalArgumentException("Boek ID mag niet null zijn");
        }

        ReadingList list = getListById(userId, listId);
        if (isClassList(list)) {
            throw new IllegalArgumentException("Boeken uit de klassikale leeslijst kunnen niet verwijderd worden");
        }

        int deleted = readingListBookRepository.deleteAllByReadingListIdAndBookId(listId, bookId);
        if (deleted == 0) {
            throw new IllegalArgumentException("Boek met id " + bookId + " zit niet in deze leeslijst");
        }
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Gebruiker met id " + userId + " bestaat niet"));
    }

    private String resolveClassCode(User user) {
        if (user.getClassCode() == null || user.getClassCode().isBlank()) {
            return FALLBACK_CLASS_CODE;
        }
        return user.getClassCode().trim();
    }

    private boolean isFavoritesList(String name) {
        return name != null && FAVORITES_NAME.equalsIgnoreCase(name.trim());
    }

    private boolean isClassList(ReadingList list) {
        return Boolean.TRUE.equals(list.getIsClassList());
    }

    private void ensureClassReadingListExists(User user) {
        String classCode = resolveClassCode(user);
        if (readingListRepository.findByClassCodeAndIsClassListTrue(classCode).isPresent()) {
            return;
        }

        ReadingList classList = new ReadingList();
        classList.setName(formatClassListName(classCode));
        classList.setUser(user);
        classList.setClassCode(classCode);
        classList.setIsClassList(true);
        readingListRepository.save(classList);
    }

    private String formatClassListName(String classCode) {
        String normalized = classCode.trim().replace('-', ' ').replace('_', ' ');
        if (!normalized.contains(" ")) {
            int splitIndex = 0;
            while (splitIndex < normalized.length() && Character.isDigit(normalized.charAt(splitIndex))) {
                splitIndex++;
            }
            if (splitIndex > 0 && splitIndex < normalized.length()) {
                return normalized.substring(0, splitIndex) + " " + normalized.substring(splitIndex);
            }
        }
        return normalized;
    }
}
