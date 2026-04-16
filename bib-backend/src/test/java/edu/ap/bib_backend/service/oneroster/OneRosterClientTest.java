package edu.ap.bib_backend.service.oneroster;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OneRosterClientTest {

    private final OneRosterClient client = new OneRosterClient(
            new RestTemplateBuilder(),
            "client-id",
            "client-secret",
            "https://oauth.smartschool.be/OAuth/index/token",
            "/ims/oneroster/v1p1",
            ".smartschool.be",
            1000,
            1000,
            "basic",
            "auto",
            "");

    @Test
    void filterUmbrellaClassLabels_shouldRemoveParentClassWhenConcreteClassesExist() {
        List<String> filtered = client.filterUmbrellaClassLabels(List.of("2ITSOF", "2ITSOF1", "2ITSOF2", "3A"));

        assertEquals(List.of("2ITSOF1", "2ITSOF2", "3A"), filtered);
    }

    @Test
    void filterUmbrellaClassLabels_shouldKeepStandaloneClass() {
        List<String> filtered = client.filterUmbrellaClassLabels(List.of("3A", "4B"));

        assertEquals(List.of("3A", "4B"), filtered);
    }
}

