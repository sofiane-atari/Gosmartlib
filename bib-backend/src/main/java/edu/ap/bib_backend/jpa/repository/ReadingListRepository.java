package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.ReadingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingListRepository extends JpaRepository<ReadingList, Long> {

}
