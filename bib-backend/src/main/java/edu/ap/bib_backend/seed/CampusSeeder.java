package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(1)
public class CampusSeeder implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final CampusRepository campusRepository;
    private final BookSeeder bookSeeder;

    private final String defaultAddress;
    private final Integer defaultPostalCode;
    private final String defaultContactPhone;
    private final String defaultContactEmail;

    public CampusSeeder(
            SchoolRepository schoolRepository,
            CampusRepository campusRepository,
            BookSeeder bookSeeder,
            @Value("${school.seed.address:}") String defaultAddress,
            @Value("${school.seed.postal-code:0}") Integer defaultPostalCode,
            @Value("${school.seed.contact-phone:}") String defaultContactPhone,
            @Value("${school.seed.contact-email:}") String defaultContactEmail) {
        this.schoolRepository = schoolRepository;
        this.campusRepository = campusRepository;
        this.bookSeeder = bookSeeder;
        this.defaultAddress = defaultAddress;
        this.defaultPostalCode = defaultPostalCode;
        this.defaultContactPhone = defaultContactPhone;
        this.defaultContactEmail = defaultContactEmail;
    }

    @Override
    public void run(String... args) {
        for (School school : schoolRepository.findAll()) {
            Campus campus = campusRepository.findFirstBySchoolIdOrderByIdAsc(school.getId())
                    .orElseGet(() -> {
                        Campus newCampus = new Campus();

                        newCampus.setSchool(school);
                        newCampus.setName(school.getName());
                        newCampus.setAddress(StringUtils.hasText(defaultAddress) ? defaultAddress.trim() : "");
                        newCampus.setPostalCode(defaultPostalCode);
                        newCampus.setContactPhone(
                                StringUtils.hasText(defaultContactPhone) ? defaultContactPhone.trim() : null);
                        newCampus.setContactEmail(
                                StringUtils.hasText(defaultContactEmail) ? defaultContactEmail.trim() : null);

                        return campusRepository.save(newCampus);
                    });

            bookSeeder.seedBooksForCampusIfEmpty(campus);
        }
    }
}