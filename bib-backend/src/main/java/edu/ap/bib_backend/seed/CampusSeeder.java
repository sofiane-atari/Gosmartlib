package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CampusSeeder implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final CampusRepository campusRepository;
    private final String defaultAddress;
    private final Integer defaultPostalCode;
    private final String defaultContactPhone;
    private final String defaultContactEmail;

    public CampusSeeder(
            SchoolRepository schoolRepository,
            CampusRepository campusRepository,
            @Value("${school.seed.address:}") String defaultAddress,
            @Value("${school.seed.postal-code:0}") Integer defaultPostalCode,
            @Value("${school.seed.contact-phone:}") String defaultContactPhone,
            @Value("${school.seed.contact-email:}") String defaultContactEmail) {
        this.schoolRepository = schoolRepository;
        this.campusRepository = campusRepository;
        this.defaultAddress = defaultAddress;
        this.defaultPostalCode = defaultPostalCode;
        this.defaultContactPhone = defaultContactPhone;
        this.defaultContactEmail = defaultContactEmail;
    }

    @Override
    public void run(String... args) {
        for (School school : schoolRepository.findAll()) {
            if (campusRepository.findFirstBySchoolIdOrderByIdAsc(school.getId()).isEmpty()) {
                Campus campus = new Campus();
                campus.setSchool(school);
                campus.setName(school.getName());
                campus.setAddress(StringUtils.hasText(defaultAddress) ? defaultAddress.trim() : "");
                campus.setPostalCode(defaultPostalCode);
                campus.setContactPhone(StringUtils.hasText(defaultContactPhone) ? defaultContactPhone.trim() : null);
                campus.setContactEmail(StringUtils.hasText(defaultContactEmail) ? defaultContactEmail.trim() : null);

                campusRepository.save(campus);
            }
        }
    }
}
