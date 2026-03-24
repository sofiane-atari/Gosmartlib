package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.ReadingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingListRepository extends JpaRepository<ReadingList, Long> {
    List<ReadingList> findAllByUser_Id(String userId);

    Optional<ReadingList> findByIdAndUser_Id(Long id, String userId);

    Optional<ReadingList> findByNameIgnoreCaseAndUser_Id(String name, String userId);

    boolean existsByIdAndUser_Id(Long id, String userId);
}
