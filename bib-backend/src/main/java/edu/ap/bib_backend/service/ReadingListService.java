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

        ReadingList list = new ReadingList();
        list.setName(dto.name().trim());
        list.setUser(getUserOrThrow(userId));

        return readingListRepository.save(list);
    }

    public List<ReadingList> getAllLists(String userId) {
        return readingListRepository.findAllByUser_Id(userId);
    }

    public ReadingList getListById(String userId, Long id) {
        return readingListRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst met id " + id + " bestaat niet"));
    }

    public void deleteReadingList(String userId, Long id) {
        ReadingList list = getListById(userId, id);

        if ("Favorieten".equalsIgnoreCase(list.getName().trim())) {
            throw new IllegalArgumentException("De leeslijst 'Favorieten' kan niet verwijderd worden");
        }

        readingListRepository.deleteById(id);
    }

    public ReadingListBook addBookToList(String userId, Long listId, Long bookId) {
        if (bookId == null) {
            throw new IllegalArgumentException("Boek ID mag niet null zijn");
        }

        ReadingList list = getListById(userId, listId);

        boolean exists = list.getBooks() != null && list.getBooks().stream()
                .anyMatch(rlb -> rlb.getBookId().equals(bookId));
        if (exists) {
            throw new IllegalArgumentException("Boek zit al in deze leeslijst");
        }

        ReadingListBook newBook = new ReadingListBook();
        newBook.setReadingList(list);
        newBook.setBookId(bookId);

        return readingListBookRepository.save(newBook);
    }

    public ReadingListBook addBookToFavorites(String userId, Long bookId) {
        ReadingList favoritesList = readingListRepository.findByNameIgnoreCaseAndUser_Id("Favorieten", userId)
                .orElseGet(() -> {
                    ReadingList newFavorites = new ReadingList();
                    newFavorites.setName("Favorieten");
                    newFavorites.setUser(getUserOrThrow(userId));
                    return readingListRepository.save(newFavorites);
                });

        return addBookToList(userId, favoritesList.getId(), bookId);
    }

    public void removeBookFromList(String userId, Long listId, Long bookId) {
        if (bookId == null) {
            throw new IllegalArgumentException("Boek ID mag niet null zijn");
        }

        getListById(userId, listId);

        ReadingListBook readingListBook = readingListBookRepository.findByReadingList_IdAndBookId(listId, bookId)
                .orElseThrow(() -> new IllegalArgumentException("Boek met id " + bookId + " zit niet in deze leeslijst"));

        readingListBookRepository.delete(readingListBook);
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Gebruiker met id " + userId + " bestaat niet"));
    }
}
