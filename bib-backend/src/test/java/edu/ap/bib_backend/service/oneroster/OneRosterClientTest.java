package edu.ap.bib_backend.service.oneroster;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OneRosterClientTest {

    private final OneRosterLabelFilter labelFilter = new OneRosterLabelFilter();

    @Test
    void filterUmbrellaClassLabels_shouldRemoveParentClassWhenConcreteClassesExist() {
        List<String> filtered = labelFilter.filterUmbrellaClassLabels(List.of("2ITSOF", "2ITSOF1", "2ITSOF2", "3A"));

        assertEquals(List.of("2ITSOF1", "2ITSOF2", "3A"), filtered);
    }

    @Test
    void filterUmbrellaClassLabels_shouldKeepStandaloneClass() {
        List<String> filtered = labelFilter.filterUmbrellaClassLabels(List.of("3A", "4B"));

        assertEquals(List.of("3A", "4B"), filtered);
    }
}
