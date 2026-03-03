package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReadingListService {
    private final ReadingListRepository readingListRepository;
    private final UserRepository userRepository;

    public ReadingListService(ReadingListRepository readingListRepository, UserRepository userRepository) {
        this.readingListRepository = readingListRepository;
        this.userRepository = userRepository;
    }

    public ReadingList createReadingList(ReadingListDTO dto) {
        // Haal de user entity op uit de DTO
        User user = userRepository.getReferenceById(dto.userId());

        // ✅ Check voor null of lege naam
        if (dto.name() == null || dto.name().trim().isEmpty()) {
            throw new IllegalArgumentException("name mag niet null of leeg zijn");
        }

        ReadingList list = new ReadingList();
        list.setName(dto.name().trim());
        list.setUser(user);

        return readingListRepository.save(list);
    }

    public List<ReadingList> getListByUser(Long userId) {
        return readingListRepository.findByUserId(userId);
    }
}
