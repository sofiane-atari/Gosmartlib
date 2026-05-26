package edu.ap.bib_backend.service.api;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.mock.MockSchoolData;
import edu.ap.bib_backend.service.mock.MockSchoolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolApiProviderFactoryTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private MockSchoolApiProvider mockProvider;

    @Mock
    private MockSchoolRegistry mockSchoolRegistry;

    private SchoolApiProviderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new SchoolApiProviderFactory(schoolRepository, mockProvider, mockSchoolRegistry, true);
    }

    @Test
    void Given_MockModeDisabled_When_IsMockSchool_Then_ReturnsFalse() {
        SchoolApiProviderFactory factoryDisabled = new SchoolApiProviderFactory(
                schoolRepository, mockProvider, mockSchoolRegistry, false);

        assertFalse(factoryDisabled.isMockSchool("any-school"));
    }

    @Test
    void Given_MockSchoolInRegistryByCode_When_IsMockSchool_Then_ReturnsTrue() {
        when(mockSchoolRegistry.getSchoolByCode("mock-school")).thenReturn(mock(MockSchoolData.class));

        assertTrue(factory.isMockSchool("mock-school"));
    }

    @Test
    void Given_MockSchoolInRegistryByDomain_When_IsMockSchool_Then_ReturnsTrue() {
        when(mockSchoolRegistry.getSchoolByDomain("mock.smartschool.be")).thenReturn(mock(MockSchoolData.class));

        assertTrue(factory.isMockSchool("mock.smartschool.be"));
    }

    @Test
    void Given_SchoolInDbWithMockProviderType_When_IsMockSchool_Then_ReturnsTrue() {
        School school = new School();
        school.setProviderType(ProviderType.MOCK);
        when(schoolRepository.findByDomainName("mock-school")).thenReturn(Optional.of(school));

        assertTrue(factory.isMockSchool("mock-school"));
    }

    @Test
    void Given_SchoolInDbWithRealProviderType_When_IsMockSchool_Then_ReturnsFalse() {
        School school = new School();
        school.setProviderType(ProviderType.REAL);
        when(schoolRepository.findByDomainName("real-school")).thenReturn(Optional.of(school));

        assertFalse(factory.isMockSchool("real-school"));
    }

    @Test
    void Given_MockSchool_When_GetProvider_Then_ReturnsMockProvider() {
        when(mockSchoolRegistry.getSchoolByCode("mock-school")).thenReturn(mock(MockSchoolData.class));

        Optional<SchoolApiProvider> provider = factory.getProvider("mock-school");

        assertTrue(provider.isPresent());
        assertEquals(mockProvider, provider.get());
    }

    @Test
    void Given_NonMockSchool_When_GetProvider_Then_ReturnsEmpty() {
        assertTrue(factory.getProvider("unknown-school").isEmpty());
    }

    @Test
    void Given_SchoolNotInRegistryOrDb_When_IsMockSchool_Then_ReturnsFalse() {
        assertFalse(factory.isMockSchool("non-existent"));
    }
}
