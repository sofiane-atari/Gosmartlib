package edu.ap.bib_backend.service.mock;

import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevSchoolSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevSchoolSeeder.class);

    private final SchoolRepository schoolRepository;
    private final OneRosterCredentialRepository credentialRepository;
    private final MockSchoolRegistry registry;

    public DevSchoolSeeder(
            SchoolRepository schoolRepository,
            OneRosterCredentialRepository credentialRepository,
            MockSchoolRegistry registry
    ) {
        this.schoolRepository = schoolRepository;
        this.credentialRepository = credentialRepository;
        this.registry = registry;
    }

    @PostConstruct
    public void seedMockSchools() {
        for (MockSchoolData mockSchool : registry.getAllSchools()) {
            School school = schoolRepository.findByDomainName(mockSchool.getDomainName())
                    .orElseGet(() -> {
                        School s = new School();
                        s.setDomainName(mockSchool.getDomainName());
                        s.setName(mockSchool.getSchoolName());
                        s.setAddress("Mock Address 1");
                        s.setPostalCode(1000);
                        s.setContactPhone("+32 000 00 00 00");
                        s.setContactEmail("info@" + mockSchool.getDomainName());
                        s.setDefaultLoanDurationDays(15);
                        schoolRepository.save(s);
                        log.info("Seeded mock school: {} ({})", mockSchool.getSchoolName(), mockSchool.getDomainName());
                        return s;
                    });

            credentialRepository.findBySchoolId(school.getId())
                    .orElseGet(() -> {
                        OneRosterCredential cred = new OneRosterCredential();
                        cred.setSchool(school);
                        cred.setClientId(mockSchool.getOnRosterClientId());
                        cred.setClientSecret(mockSchool.getOnRosterClientSecret());
                        credentialRepository.save(cred);
                        log.info("Seeded OneRoster credentials for: {}", mockSchool.getSchoolName());
                        return cred;
                    });
        }

        log.info("Mock school seeding complete. {} schools available.", registry.getAllSchools().size());
    }
}
