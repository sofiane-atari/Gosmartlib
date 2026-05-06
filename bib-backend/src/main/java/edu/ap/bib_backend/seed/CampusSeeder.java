package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CampusSeeder implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final CampusRepository campusRepository;

    public CampusSeeder(
            SchoolRepository schoolRepository,
            CampusRepository campusRepository
    ) {
        this.schoolRepository = schoolRepository;
        this.campusRepository = campusRepository;
    }

    @Override
    public void run(String... args) {
        for (School school : schoolRepository.findAll()) {
            if (campusRepository.findBySchoolIdAndDefaultCampusTrue(school.getId()).isEmpty()) {
                Campus campus = new Campus();
                campus.setSchool(school);
                campus.setName("Default campus");
                campus.setCode("default-campus");
                campus.setDefaultCampus(true);

                campusRepository.save(campus);
            }
        }
    }
}