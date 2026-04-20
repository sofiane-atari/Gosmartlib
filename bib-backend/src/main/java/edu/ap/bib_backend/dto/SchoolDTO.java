package edu.ap.bib_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SchoolDTO(
        @NotBlank(message = "Domeinnaam is verplicht") String domainName,
        @NotBlank(message = "Schoolnaam is verplicht") String name,
        @NotBlank(message = "Adres is verplicht") String address,
        @Positive(message = "Postcode moet groter zijn dan 0") int postalCode
) {
}

