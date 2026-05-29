package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.dto.CampusDTO;
import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterCredentialDTO;
import edu.ap.bib_backend.service.SchoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.util.List;

@Configuration
@Order(1)
public class InitialSchoolSeeder {

    private static final Logger logger = LoggerFactory.getLogger(InitialSchoolSeeder.class);

    @Bean
    CommandLineRunner seedInitialSchool(
            SchoolService schoolService,
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

            String normalizedDomain = schoolDomain.trim().toLowerCase();
            boolean hasCredentials = StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);

            var existingSchool = schoolService.findSchoolByDomainName(normalizedDomain);

            if (existingSchool.isPresent()) {
                SchoolDTO school = existingSchool.get();

                if (hasCredentials) {
                    OneRosterCredentialDTO credentialDTO = new OneRosterCredentialDTO(
                            clientId.trim(), clientSecret.trim());
                    SchoolDTO updateDTO = new SchoolDTO(
                            school.id(),
                            normalizedDomain,
                            StringUtils.hasText(schoolName) ? schoolName.trim() : school.name(),
                            null,
                            null,
                            null, null,
                            credentialDTO,
                            null, null, null
                    );
                    schoolService.update(school.id(), updateDTO);
                }
            } else {
                if (!StringUtils.hasText(schoolName)) {
                    throw new IllegalStateException(
                            "School niet gevonden voor domain: " + normalizedDomain +
                            ". Voor automatisch aanmaken is school.seed.name verplicht.");
                }

                String campusNameValue = StringUtils.hasText(campusName)
                        ? campusName.trim()
                        : schoolName.trim() + " - Campus 1";

                CampusDTO campus = new CampusDTO(
                        null,
                        campusNameValue, null, null,
                        StringUtils.hasText(campusAddress) ? campusAddress.trim() : "Onbekend adres",
                        campusPostalCode != null ? campusPostalCode : 0,
                        StringUtils.hasText(campusContactPhone) ? campusContactPhone.trim() : null,
                        StringUtils.hasText(campusContactEmail) ? campusContactEmail.trim() : null
                );

                SchoolDTO schoolDTO = new SchoolDTO(
                        null,
                        normalizedDomain,
                        schoolName.trim(),
                        null,
                        List.of(campus),
                        null, null,
                        hasCredentials
                                ? new OneRosterCredentialDTO(clientId.trim(), clientSecret.trim())
                                : null,
                        null, null, null
                );

                schoolService.create(schoolDTO);
            }
        };
    }
}