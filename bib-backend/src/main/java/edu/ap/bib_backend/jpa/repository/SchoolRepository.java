
package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, String> {
    boolean existsByDomainNameIgnoreCase(String domainName);

    boolean existsByNameIgnoreCase(String name);
}
