package edu.ap.bib_backend.util;

public class IsbnUtils {

    /**
     * Verwijdert streepjes en spaties voor database opslag.
     */
    public static String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }
        return isbn.replaceAll("[-\\s]", "").trim().toUpperCase();
    }

    /**
     * Valideert het formaat en normaliseert voor API calls.
     */
    public static String normalizeAndValidateIsbn(String rawIsbn) {
        String normalized = normalizeIsbn(rawIsbn);

        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("ISBN is verplicht");
        }

        boolean validFormat = normalized.matches("^[0-9X]{10}$")
                || normalized.matches("^[0-9]{13}$");

        if (!validFormat) {
            throw new IllegalArgumentException("Ongeldig ISBN-formaat: " + rawIsbn);
        }

        return normalized;
    }
}