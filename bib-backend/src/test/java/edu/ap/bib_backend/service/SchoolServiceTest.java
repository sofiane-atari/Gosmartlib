package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import java.time.LocalDateTime;
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

    @Mock
    private BookCopyRepository bookCopyRepository;

    @InjectMocks
    private SchoolService schoolService;

    @Test
    void givenValidSchool_whenCreate_thenSaveAndReturnDto() {
        SchoolDTO input = schoolDto(
                null,
                "  MySchool.be  ",
                "  AP Hogeschool  ",
                "  Noorderplaats 2  ",
                2000);

        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> {
            School school = invocation.getArgument(0);
            school.setId(1L);
            return school;
        });

        SchoolDTO output = schoolService.create(input);

        assertEquals(1L, output.id());
        assertEquals("myschool.be", output.domainName());
        assertEquals("AP Hogeschool", output.name());
        assertEquals("Noorderplaats 2", output.address());
        assertEquals(2000, output.postalCode());
        assertEquals("03 123 45 67", output.contactPhone());
        assertEquals("info@ap.be", output.contactEmail());

        verify(schoolRepository).save(any(School.class));
    }

    @Test
    void givenSchoolsExist_whenGetAll_thenReturnDtos() {
        School school = schoolEntity(
                1L,
                "ap.be",
                "AP",
                "Noorderplaats 2",
                2000);

        when(schoolRepository.findAll()).thenReturn(List.of(school));

        List<SchoolDTO> result = schoolService.getAll();

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().id());
        assertEquals("AP", result.getFirst().name());
        assertEquals("ap.be", result.getFirst().domainName());
    }

    @Test
    void givenValidUpdate_whenUpdate_thenSaveAndReturnUpdatedDto() {
        Long schoolId = 1L;

        School existingSchool = schoolEntity(
                schoolId,
                "ap.be",
                "AP",
                "Noorderplaats 2",
                2000);

        SchoolDTO update = schoolDto(
                null,
                "nieuw.be",
                "AP Campus Noord",
                "Ellermanstraat 33",
                2030);

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchoolDTO result = schoolService.update(schoolId, update);

        assertEquals(1L, result.id());
        assertEquals("nieuw.be", result.domainName());
        assertEquals("AP Campus Noord", result.name());
        assertEquals("Ellermanstraat 33", result.address());
        assertEquals(2030, result.postalCode());
        assertEquals("03 123 45 67", result.contactPhone());
        assertEquals("info@ap.be", result.contactEmail());

        verify(schoolRepository).findById(schoolId);
        verify(schoolRepository).save(existingSchool);
    }

    @Test
    void givenUnknownSchoolId_whenUpdate_thenThrowException() {
        Long schoolId = 999L;

        SchoolDTO update = schoolDto(
                null,
                "ap.be",
                "AP Campus Noord",
                "Ellermanstraat 33",
                2030);

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> schoolService.update(schoolId, update));

        verify(schoolRepository, never()).save(any(School.class));
    }

    @Test
    void givenExistingSchoolId_whenDelete_thenDeleteById() {
        Long schoolId = 1L;

        when(schoolRepository.existsById(schoolId)).thenReturn(true);

        schoolService.delete(schoolId);

        verify(schoolRepository).deleteById(schoolId);
    }

    @Test
    void givenUnknownSchoolId_whenDelete_thenThrowException() {
        Long schoolId = 999L;

        when(schoolRepository.existsById(schoolId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> schoolService.delete(schoolId));

        verify(schoolRepository, never()).deleteById(schoolId);
    }

    @Test
    void givenExistingSchoolId_whenGetBookCountBySchool_thenReturnCount() {
        Long schoolId = 1L;

        when(schoolRepository.existsById(schoolId)).thenReturn(true);
        when(bookCopyRepository.countByCampusSchoolId(schoolId)).thenReturn(12L);

        long result = schoolService.getBookCountBySchool(schoolId);

        assertEquals(12L, result);
        verify(bookCopyRepository).countByCampusSchoolId(schoolId);
    }

    @Test
    void givenUnknownSchoolId_whenGetBookCountBySchool_thenThrowException() {
        Long schoolId = 999L;

        when(schoolRepository.existsById(schoolId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> schoolService.getBookCountBySchool(schoolId));

        verify(bookCopyRepository, never()).countByCampusSchoolId(schoolId);
    }

    @Test
    void whenGetTotalBookCount_thenReturnCount() {
        when(bookCopyRepository.count()).thenReturn(50L);

        long result = schoolService.getTotalBookCount();

        assertEquals(50L, result);
    }

    private SchoolDTO schoolDto(
            Long id,
            String domainName,
            String name,
            String address,
            Integer postalCode) {
        return new SchoolDTO(
                id,
                domainName,
                name,
                address,
                postalCode,
                "03 123 45 67",
                "info@ap.be",
                15,
                LocalDateTime.of(2026, 4, 28, 12, 0));
    }

    private School schoolEntity(
            Long id,
            String domainName,
            String name,
            String address,
            Integer postalCode) {
        School school = new School();
        school.setId(id);
        school.setDomainName(domainName);
        school.setName(name);
        school.setAddress(address);
        school.setPostalCode(postalCode);
        school.setContactPhone("03 123 45 67");
        school.setContactEmail("info@ap.be");
        school.setCreatedAt(LocalDateTime.of(2026, 4, 28, 12, 0));
        return school;
    }
}