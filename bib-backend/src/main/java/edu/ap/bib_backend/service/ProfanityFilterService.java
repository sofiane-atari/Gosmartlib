package edu.ap.bib_backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ProfanityFilterService {

    private Set<String> bannedWords = new HashSet<>();

    @PostConstruct
    public void loadWords() {
        try {
            ClassPathResource resource = new ClassPathResource("profanity-list.txt");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                bannedWords.add(line.trim().toLowerCase());
            }

        } catch (Exception e) {
            throw new RuntimeException("Couldn't load profanity list", e);
        }
    }

    public boolean containsProfanity(String text) {

        if (text == null) return false;

        String lowerText = text.toLowerCase();

        for (String word : bannedWords) {
            String pattern = "\\b" + Pattern.quote(word) + "\\b";
            if (Pattern.compile(pattern).matcher(lowerText).find()) {
                return true;
            }
        }

        return false;
    }
}