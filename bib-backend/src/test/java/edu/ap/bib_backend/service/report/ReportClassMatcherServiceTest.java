package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReportClassMatcherServiceTest {

    private final ReportClassMatcherService service = new ReportClassMatcherService();

    private final OneRosterClassDTO classOne = new OneRosterClassDTO("class-1", "2ITSOF1", "2ITSOF1");

    private final OneRosterClassDTO classTwo = new OneRosterClassDTO("class-2", "2ITSOF2", "2ITSOF2");

    private final OneRosterClassDTO classWithSpaces = new OneRosterClassDTO("class-3", " 3 IT SOF 1 ", "3ITSOF1");

    @Test
    void findMatch_returnsEmpty_whenInputIsNull() {
        Optional<OneRosterClassDTO> result = service.findMatch(
                null,
                List.of(classOne, classTwo));

        assertThat(result).isEmpty();
    }

    @Test
    void findMatch_returnsEmpty_whenInputIsBlank() {
        Optional<OneRosterClassDTO> result = service.findMatch(
                "   ",
                List.of(classOne, classTwo));

        assertThat(result).isEmpty();
    }

    @Test
    void findMatch_matchesByClassCode() {
        Optional<OneRosterClassDTO> result = service.findMatch(
                "2ITSOF1",
                List.of(classOne, classTwo));

        assertThat(result).contains(classOne);
    }

    @Test
    void findMatch_matchesByTitle() {
        Optional<OneRosterClassDTO> result = service.findMatch(
                "2ITSOF2",
                List.of(classOne, classTwo));

        assertThat(result).contains(classTwo);
    }

    @Test
    void findMatch_matchesBySourcedId() {
        Optional<OneRosterClassDTO> result = service.findMatch(
                "class-1",
                List.of(classOne, classTwo));

        assertThat(result).contains(classOne);
    }

    @Test
    void findMatch_isCaseInsensitive() {
        Optional<OneRosterClassDTO> result = service.findMatch(
                "2itsof1",
                List.of(classOne, classTwo));

        assertThat(result).contains(classOne);
    }

    @Test
    void findMatch_trimsInput() {
        Optional<OneRosterClassDTO> result = service.findMatch(
                "   2ITSOF1   ",
                List.of(classOne, classTwo));

        assertThat(result).contains(classOne);
    }

    @Test
    void findMatch_normalizesMultipleSpaces() {
        Optional<OneRosterClassDTO> result = service.findMatch(
                "3   it   sof   1",
                List.of(classWithSpaces));

        assertThat(result).contains(classWithSpaces);
    }

    @Test
    void findMatch_returnsEmpty_whenNoMatchFound() {
        Optional<OneRosterClassDTO> result = service.findMatch(
                "UNKNOWN",
                List.of(classOne, classTwo));

        assertThat(result).isEmpty();
    }

    @Test
    void findMatches_returnsAllMatchingClasses() {
        List<OneRosterClassDTO> result = service.findMatches(
                List.of("2ITSOF1", "2ITSOF2"),
                List.of(classOne, classTwo));

        assertThat(result).containsExactly(classOne, classTwo);
    }

    @Test
    void findMatches_ignoresUnknownValues() {
        List<OneRosterClassDTO> result = service.findMatches(
                List.of("UNKNOWN", "2ITSOF1"),
                List.of(classOne, classTwo));

        assertThat(result).containsExactly(classOne);
    }

    @Test
    void findMatches_removesDuplicateMatches() {
        List<OneRosterClassDTO> result = service.findMatches(
                List.of("2ITSOF1", "class-1", " 2itsof1 "),
                List.of(classOne, classTwo));

        assertThat(result).containsExactly(classOne);
    }

    @Test
    void findMatches_returnsEmptyList_whenNoMatches() {
        List<OneRosterClassDTO> result = service.findMatches(
                List.of("UNKNOWN", "NOT_FOUND"),
                List.of(classOne, classTwo));

        assertThat(result).isEmpty();
    }

    @Test
    void findMatches_returnsEmptyList_whenInputListIsEmpty() {
        List<OneRosterClassDTO> result = service.findMatches(
                List.of(),
                List.of(classOne, classTwo));

        assertThat(result).isEmpty();
    }
}