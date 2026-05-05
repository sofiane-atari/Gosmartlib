package edu.ap.bib_backend.service.oneroster;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import edu.ap.bib_backend.mapper.OneRosterMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

class OneRosterClientTest {

    private final OneRosterClient client = new OneRosterClient(
            new RestTemplateBuilder(),
            new OneRosterMapper(),
            "client-id",
            "client-secret",
            "https://oauth.smartschool.be/OAuth/index/token",
            "/ims/oneroster/v1p1",
            ".smartschool.be",
            5000,
            7000,
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

    private OneRosterClient createClient() {
        return new OneRosterClient(
                new RestTemplateBuilder(),
                new OneRosterMapper(),
                "client-id",
                "client-secret",
                "https://oauth.smartschool.be/OAuth/index/token",
                "/ims/oneroster/v1p1",
                ".smartschool.be",
                5000,
                7000,
                "basic",
                "auto",
                "");
    }

    @Test
    void filterUmbrellaClassLabels_removesParentClassWhenSpecificClassesExist() {
        OneRosterClient client = createClient();

        List<String> result = client.filterUmbrellaClassLabels(List.of(
                "2ITSOF",
                "2ITSOF1",
                "2ITSOF2"));

        assertThat(result).containsExactly("2ITSOF1", "2ITSOF2");
    }

    @Test
    void filterUmbrellaClassLabels_keepsLabelsWhenNoUmbrellaClassExists() {
        OneRosterClient client = createClient();

        List<String> result = client.filterUmbrellaClassLabels(List.of(
                "1A",
                "2B",
                "3C"));

        assertThat(result).containsExactly("1A", "2B", "3C");
    }
}
