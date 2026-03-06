package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReadingListService {
    private final ReadingListRepository readingListRepository;

    public ReadingListService(ReadingListRepository readingListRepository) {
        this.readingListRepository = readingListRepository;
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

    public void deleteReadingList(Long id) {
        if (!readingListRepository.existsById(id)) {
            throw new IllegalArgumentException("Leeslijst met id " + id + " bestaat niet");
        }
        readingListRepository.deleteById(id);
    }
}
