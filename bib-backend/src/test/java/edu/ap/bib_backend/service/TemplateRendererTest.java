package edu.ap.bib_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateRendererTest {

    private TemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new TemplateRenderer();
    }

    @Test
    void Given_ValidTemplateAndPlaceholders_When_Render_Then_ReplacesAllPlaceholders() {
        Map<String, String> placeholders = Map.of(
                "USER_NAME", "Jan Janssens",
                "DAYS_UNTIL_RETURN", "2"
        );

        String result = renderer.render("templates/messages/test-template.html", placeholders);

        assertNotNull(result);
        assertTrue(result.contains("Jan Janssens"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("Hallo"));
        assertTrue(result.contains("retour over"));
    }

    @Test
    void Given_NonExistentTemplate_When_Render_Then_ReturnsEmptyString() {
        String result = renderer.render("templates/messages/non-existent.html", Map.of());

        assertEquals("", result);
    }

    @Test
    void Given_NullPlaceholderValue_When_Render_Then_ReplacesWithEmptyString() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("USER_NAME", null);
        placeholders.put("DAYS_UNTIL_RETURN", "3");

        String result = renderer.render("templates/messages/test-template.html", placeholders);

        assertNotNull(result);
        assertFalse(result.contains("{{USER_NAME}}"));
        assertTrue(result.contains("3"));
    }

    @Test
    void Given_NoPlaceholdersInTemplate_When_Render_WithPlaceholders_Then_ReturnsUnchanged() {
        String result = renderer.render("templates/messages/test-template.html", Map.of("UNUSED_KEY", "value"));

        assertNotNull(result);
        assertTrue(result.contains("Geen placeholder hier"));
        assertFalse(result.contains("{{UNUSED_KEY}}"));
    }

    @Test
    void Given_EmptyPlaceholders_When_Render_Then_ReturnsTemplateAsIs() {
        String result = renderer.render("templates/messages/test-template.html", Map.of());

        assertNotNull(result);
        assertTrue(result.contains("{{USER_NAME}}"));
        assertTrue(result.contains("{{DAYS_UNTIL_RETURN}}"));
    }
}
