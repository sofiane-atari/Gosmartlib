package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolServiceTest {

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private SchoolService schoolService;

    @Test
    void givenValidSchool_whenCreateSchool_thenSaveAndReturnDto() {
        SchoolDTO input = new SchoolDTO("  MySchool.be  ", "  AP Hogeschool  ", "  Noorderplaats 2  ", 2000);

        when(schoolRepository.existsByDomainNameIgnoreCase("myschool.be")).thenReturn(false);
        when(schoolRepository.existsByNameIgnoreCase("AP Hogeschool")).thenReturn(false);
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchoolDTO output = schoolService.createSchool(input);

        assertEquals("myschool.be", output.domainName());
        assertEquals("AP Hogeschool", output.name());
        assertEquals("Noorderplaats 2", output.address());
        assertEquals(2000, output.postalCode());
    }

    @Test
    void givenDuplicateDomain_whenCreateSchool_thenThrowConflict() {
        SchoolDTO input = new SchoolDTO("school.be", "AP Hogeschool", "Noorderplaats 2", 2000);
        when(schoolRepository.existsByDomainNameIgnoreCase("school.be")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> schoolService.createSchool(input));

        verify(schoolRepository, never()).save(any(School.class));
    }

    @Test
    void givenSchoolsExist_whenGetAll_thenReturnDtosSortedByRepositoryOrder() {
        School school = new School();
        school.setDomainName("ap.be");
        school.setName("AP");
        school.setAdres("Noorderplaats 2");
        school.setPostalCode(2000);

        when(schoolRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(school));

        List<SchoolDTO> result = schoolService.getAll();

        assertEquals(1, result.size());
        assertEquals("AP", result.getFirst().name());
    }

    @Test
    void givenValidUpdate_whenUpdateSchool_thenSaveAndReturnUpdatedDto() {
        School existingSchool = new School();
        existingSchool.setDomainName("ap.be");
        existingSchool.setName("AP");
        existingSchool.setAdres("Noorderplaats 2");
        existingSchool.setPostalCode(2000);

        SchoolDTO update = new SchoolDTO("ap.be", "AP Campus Noord", "Ellermanstraat 33", 2030);

        when(schoolRepository.findById("ap.be")).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.existsByNameIgnoreCaseAndDomainNameNot("AP Campus Noord", "ap.be"))
                .thenReturn(false);
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchoolDTO result = schoolService.updateSchool("ap.be", update);

        assertEquals("ap.be", result.domainName());
        assertEquals("AP Campus Noord", result.name());
        assertEquals("Ellermanstraat 33", result.address());
        assertEquals(2030, result.postalCode());
    }

    @Test
    void givenDuplicateNameOnUpdate_whenUpdateSchool_thenThrowConflict() {
        School existingSchool = new School();
        existingSchool.setDomainName("ap.be");
        existingSchool.setName("AP");

        SchoolDTO update = new SchoolDTO("ap.be", "KDG", "Noorderplaats 2", 2000);

        when(schoolRepository.findById("ap.be")).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.existsByNameIgnoreCaseAndDomainNameNot("KDG", "ap.be")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> schoolService.updateSchool("ap.be", update));

        verify(schoolRepository, never()).save(any(School.class));
    }
}
