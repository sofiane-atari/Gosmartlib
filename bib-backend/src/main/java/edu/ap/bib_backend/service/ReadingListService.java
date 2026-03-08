package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReadingListService {
    private final ReadingListRepository readingListRepository;
    private final ReadingListBookRepository readingListBookRepository;

    public ReadingListService(ReadingListRepository readingListRepository,
            ReadingListBookRepository readingListBookRepository) {
        this.readingListRepository = readingListRepository;
        this.readingListBookRepository = readingListBookRepository;
    }

    public ReadingList createReadingList(ReadingListDTO dto) {
        if (dto.name() == null || dto.name().trim().isEmpty()) {
            throw new IllegalArgumentException("name mag niet null of leeg zijn");
        }

        ReadingList list = new ReadingList();
        list.setName(dto.name().trim());

        return readingListRepository.save(list);
    }

    public List<ReadingList> getAllLists() {
        return readingListRepository.findAll();
    }

    public ReadingList getListById(Long id) {
        return readingListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst met id " + id + " bestaat niet"));
    }

    public void deleteReadingList(Long id) {
        if (!readingListRepository.existsById(id)) {
            throw new IllegalArgumentException("Leeslijst met id " + id + " bestaat niet");
        }
        readingListRepository.deleteById(id);
    }

    public ReadingListBook addBookToList(Long listId, Long bookId) {
        if (bookId == null) {
            throw new IllegalArgumentException("Boek ID mag niet null zijn");
        }

        ReadingList list = getListById(listId);

        // Check if book is already in the list
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

    public ReadingListBook addBookToFavorites(Long bookId) {
        // Find existing 'Favorieten' list, or create it if not found
        ReadingList favoritesList = readingListRepository.findByNameIgnoreCase("Favorieten")
                .orElseGet(() -> {
                    ReadingList newFavorites = new ReadingList();
                    newFavorites.setName("Favorieten");
                    return readingListRepository.save(newFavorites);
                });

        return addBookToList(favoritesList.getId(), bookId);
    }
}
