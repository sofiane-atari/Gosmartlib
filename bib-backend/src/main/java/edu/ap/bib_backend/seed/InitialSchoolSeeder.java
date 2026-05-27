package edu.ap.bib_backend.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;

@Configuration
@Order(1)
public class InitialSchoolSeeder {

    private static final Logger logger = LoggerFactory.getLogger(InitialSchoolSeeder.class);

    @Bean
    CommandLineRunner seedInitialSchool(
            SchoolRepository schoolRepository,
            CampusRepository campusRepository,
            OneRosterCredentialRepository credentialRepository,
            BookSeeder bookSeeder,
            @Value("${school.seed.domain:}") String schoolDomain,
            @Value("${school.seed.name:}") String schoolName,
            @Value("${school.seed.oneroster-client-id:}") String clientId,
            @Value("${school.seed.oneroster-client-secret:}") String clientSecret,
            @Value("${school.seed.campus-name:}") String campusName,
            @Value("${school.seed.campus-address:}") String campusAddress,
            @Value("${school.seed.campus-postal-code:0}") Integer campusPostalCode,
            @Value("${school.seed.campus-contact-phone:}") String campusContactPhone,
            @Value("${school.seed.campus-contact-email:}") String campusContactEmail) {
        return args -> {
            if (!StringUtils.hasText(schoolDomain)) {
                logger.info("School seed ontbreekt, seed wordt overgeslagen.");
                return;
            }

            String normalizedDomain = schoolDomain.trim();
            School school = schoolRepository.findByDomainName(normalizedDomain)
                    .orElseGet(() -> createSchool(
                            schoolRepository,
                            normalizedDomain,
                            schoolName
                    ));

            if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
                logger.info("OneRoster seed credentials ontbreken, seed wordt overgeslagen.");
                return;
            }

            OneRosterCredential credential = credentialRepository.findBySchoolId(school.getId())
                    .orElseGet(OneRosterCredential::new);
            credential.setSchool(school);
            credential.setClientId(clientId.trim());
            credential.setClientSecret(clientSecret.trim());
            credentialRepository.save(credential);
            logger.info("OneRoster credentials opgeslagen voor school: {}", normalizedDomain);

            seedCampusIfNotExists(
                    campusRepository, bookSeeder, school,
                    campusName, campusAddress, campusPostalCode,
                    campusContactPhone, campusContactEmail
            );
        };
    }

    private School createSchool(
            SchoolRepository schoolRepository,
            String domain,
            String name
    ) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalStateException(
                    "School niet gevonden voor domain: " + domain +
                            ". Voor automatisch aanmaken is school.seed.name verplicht.");
        }

        School school = new School();
        school.setDomainName(domain);
        school.setName(name.trim());

        School savedSchool = schoolRepository.save(school);
        logger.info("School aangemaakt voor domain: {}", domain);
        return savedSchool;
    }

    private void seedCampusIfNotExists(
            CampusRepository campusRepository,
            BookSeeder bookSeeder,
            School school,
            String campusName,
            String campusAddress,
            Integer campusPostalCode,
            String campusContactPhone,
            String campusContactEmail
    ) {
        Campus campus = campusRepository.findFirstBySchoolIdOrderByIdAsc(school.getId())
                .orElseGet(() -> {
                    Campus newCampus = new Campus();
                    newCampus.setSchool(school);
                    newCampus.setName(StringUtils.hasText(campusName)
                            ? campusName.trim()
                            : school.getName() + " - Campus 1");
                    newCampus.setAddress(StringUtils.hasText(campusAddress)
                            ? campusAddress.trim()
                            : "");
                    newCampus.setPostalCode(campusPostalCode != null ? campusPostalCode : 0);
                    newCampus.setContactPhone(StringUtils.hasText(campusContactPhone)
                            ? campusContactPhone.trim()
                            : null);
                    newCampus.setContactEmail(StringUtils.hasText(campusContactEmail)
                            ? campusContactEmail.trim()
                            : null);

                    Campus saved = campusRepository.save(newCampus);
                    logger.info("Campus aangemaakt voor school: {}", school.getDomainName());
                    return saved;
                });

        bookSeeder.seedBooksForCampusIfEmpty(campus);
    }
}
