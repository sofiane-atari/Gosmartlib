package edu.ap.bib_backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProfanityFilterServiceTest {

    private ProfanityFilterService profanityFilterService;

    @BeforeEach
    void setUp() {
        profanityFilterService = new ProfanityFilterService();
        
        Set<String> testBannedWords = new HashSet<>(Set.of("badword", "uglyword", "curse"));
        ReflectionTestUtils.setField(profanityFilterService, "bannedWords", testBannedWords);
    }

    @Test
    @DisplayName("Given null text, When containsProfanity is called, Then return false")
    void givenNullText_whenContainsProfanity_thenReturnFalse() {
        // Given
        String text = null;

        // When
        boolean result = profanityFilterService.containsProfanity(text);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Given clean text, When containsProfanity is called, Then return false")
    void givenCleanText_whenContainsProfanity_thenReturnFalse() {
        // Given
        String text = "This is a completely clean sentence.";

        // When
        boolean result = profanityFilterService.containsProfanity(text);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Given text with a banned word, When containsProfanity is called, Then return true")
    void givenTextWithProfanity_whenContainsProfanity_thenReturnTrue() {
        // Given
        String text = "This sentence has a badword in it.";

        // When
        boolean result = profanityFilterService.containsProfanity(text);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given text with a banned word in uppercase, When containsProfanity is called, Then return true")
    void givenTextWithProfanityDifferentCase_whenContainsProfanity_thenReturnTrue() {
        // Given
        String text1 = "THIS SENTENCE HAS A BADWORD IN IT.";
        String text2 = "This has an UgLyWoRd.";

        // When
        boolean result1 = profanityFilterService.containsProfanity(text1);
        boolean result2 = profanityFilterService.containsProfanity(text2);

        // Then
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
    }

    @Test
    @DisplayName("Given banned word as a substring of an allowed word, When containsProfanity is called, Then return false")
    void givenTextWithProfanityAsSubstring_whenContainsProfanity_thenReturnFalse() {
        // Given
        String text = "The cursive writing looks beautiful.";

        // When
        boolean result = profanityFilterService.containsProfanity(text);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Given actual resource file exists, When loadWords is called, Then bannedWords set is populated")
    void givenValidResourceFile_whenLoadWords_thenBannedWordsArePopulated() {
        // Given
        ProfanityFilterService realService = new ProfanityFilterService();
        
        // When
        realService.loadWords();
        
        // Then
        @SuppressWarnings("unchecked")
        Set<String> loadedWords = (Set<String>) ReflectionTestUtils.getField(realService, "bannedWords");
        
        assertThat(loadedWords).isNotNull();
        // As long as the file exists in your project, the set will no longer be empty
        assertThat(loadedWords).isNotEmpty();
    }
}