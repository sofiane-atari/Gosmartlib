package edu.ap.bib_backend.service.api;

import java.util.Optional;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.mock.MockSchoolRegistry;

@Service
public class SchoolApiProviderFactory {

    private final SchoolRepository schoolRepository;
    private final MockSchoolApiProvider mockProvider;
    private final MockSchoolRegistry mockSchoolRegistry;

    public SchoolApiProviderFactory(SchoolRepository schoolRepository, MockSchoolApiProvider mockProvider,
                                    MockSchoolRegistry mockSchoolRegistry) {
        this.schoolRepository = schoolRepository;
        this.mockProvider = mockProvider;
        this.mockSchoolRegistry = mockSchoolRegistry;
    }

    public Optional<SchoolApiProvider> getProvider(String schoolCode) {
        if (isMockSchool(schoolCode)) {
            return Optional.of(mockProvider);
        }
        return Optional.empty();
    }

    public boolean isMockSchool(String schoolCode) {
        if (mockSchoolRegistry.getSchoolByCode(schoolCode) != null) {
            return true;
        }
        if (mockSchoolRegistry.getSchoolByDomain(schoolCode) != null) {
            return true;
        }
        School school = schoolRepository.findByDomainName(schoolCode).orElse(null);
        return school != null && school.getProviderType() == ProviderType.MOCK;
    }
}
