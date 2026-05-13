package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class CampusSeeder implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final CampusRepository campusRepository;
    private final BookSeeder bookSeeder;

    public CampusSeeder(
            SchoolRepository schoolRepository,
            CampusRepository campusRepository,
            BookSeeder bookSeeder) {
        this.schoolRepository = schoolRepository;
        this.campusRepository = campusRepository;
        this.bookSeeder = bookSeeder;
    }

    @Override
    public void run(String... args) {
        for (School school : schoolRepository.findAll()) {
            Campus campus = campusRepository.findBySchoolIdAndDefaultCampusTrue(school.getId())
                    .orElseGet(() -> {
                        Campus newCampus = new Campus();
                        newCampus.setSchool(school);
                        newCampus.setName("Default campus");
                        newCampus.setCode("default-campus");
                        newCampus.setDefaultCampus(true);

                        return campusRepository.save(newCampus);
                    });

            bookSeeder.seedBooksForCampusIfEmpty(campus);
        }
    }
}