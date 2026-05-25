package edu.ap.bib_backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ap.bib_backend.dto.oneroster.OneRosterCredentialDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record SchoolDTO(
                @JsonProperty("id") Long id,

                @JsonProperty("domainName") @NotBlank(message = "Domeinnaam is verplicht.") String domainName,

                @JsonProperty("name") @NotBlank(message = "Schoolnaam is verplicht.") String name,

                @JsonProperty("defaultLoanDurationDays") Integer defaultLoanDurationDays,

                @JsonProperty("campuses") List<@Valid CampusDTO> campuses,

                @JsonProperty("createdAt") LocalDateTime createdAt,

                @JsonProperty("hasLibrarian") Boolean hasLibrarian,

                @JsonProperty("onerosterCredential") @Valid OneRosterCredentialDTO onerosterCredential) {
}
