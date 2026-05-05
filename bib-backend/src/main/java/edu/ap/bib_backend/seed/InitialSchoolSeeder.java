package edu.ap.bib_backend.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;

@Configuration
public class InitialSchoolSeeder {

    private static final Logger logger = LoggerFactory.getLogger(InitialSchoolSeeder.class);

    @Bean
    CommandLineRunner seedInitialSchool(
            OneRosterCredentialRepository credentialRepository,
            SchoolRepository schoolRepository,
            @Value("${school.seed.domain:}") String schoolDomain,
            @Value("${school.seed.name:}") String schoolName,
            @Value("${school.seed.address:}") String schoolAddress,
            @Value("${school.seed.postal-code:}") Integer schoolPostalCode,
            @Value("${school.seed.contact-phone:}") String schoolContactPhone,
            @Value("${school.seed.contact-email:}") String schoolContactEmail,
            @Value("${school.seed.oneroster-client-id:}") String clientId,
            @Value("${school.seed.oneroster-client-secret:}") String clientSecret) {
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
                            schoolName,
                            schoolAddress,
                            schoolPostalCode,
                            schoolContactPhone,
                            schoolContactEmail
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
        };
    }

    private School createSchool(
            SchoolRepository schoolRepository,
            String domain,
            String name,
            String address,
            Integer postalCode,
            String contactPhone,
            String contactEmail
    ) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(address) || postalCode == null) {
            throw new IllegalStateException(
                    "School niet gevonden voor domain: " + domain +
                            ". Voor automatisch aanmaken zijn school.seed.name, " +
                            "school.seed.address en school.seed.postal-code verplicht.");
        }

        School school = new School();
        school.setDomainName(domain);
        school.setName(name.trim());
        school.setAddress(address.trim());
        school.setPostalCode(postalCode);
        if (StringUtils.hasText(contactPhone)) {
            school.setContactPhone(contactPhone.trim());
        }
        if (StringUtils.hasText(contactEmail)) {
            school.setContactEmail(contactEmail.trim());
        }

        School savedSchool = schoolRepository.save(school);
        logger.info("School aangemaakt voor domain: {}", domain);
        return savedSchool;
    }
}
