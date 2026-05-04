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
public class OneRosterCredentialSeeder {

    private static final Logger logger = LoggerFactory.getLogger(OneRosterCredentialSeeder.class);

    @Bean
    CommandLineRunner seedOneRosterCredentials(
            OneRosterCredentialRepository credentialRepository,
            SchoolRepository schoolRepository,
            @Value("${oneroster.seed.school-domain:}") String schoolDomain,
            @Value("${oneroster.seed.client-id:}") String clientId,
            @Value("${oneroster.seed.client-secret:}") String clientSecret) {
        return args -> {
            if (!StringUtils.hasText(schoolDomain)
                    || !StringUtils.hasText(clientId)
                    || !StringUtils.hasText(clientSecret)) {
                logger.info("OneRoster seed credentials ontbreken, seed wordt overgeslagen.");
                return;
            }

            String normalizedDomain = schoolDomain.trim();
            School school = schoolRepository.findByDomainName(normalizedDomain)
                    .orElseThrow(() -> new IllegalStateException(
                            "School niet gevonden voor domain: " + normalizedDomain));

            OneRosterCredential credential = credentialRepository.findBySchoolId(school.getId())
                    .orElseGet(OneRosterCredential::new);
            credential.setSchool(school);
            credential.setClientId(clientId.trim());
            credential.setClientSecret(clientSecret.trim());

            credentialRepository.save(credential);
            logger.info("OneRoster credentials opgeslagen voor school: {}", normalizedDomain);
        };
    }
}
