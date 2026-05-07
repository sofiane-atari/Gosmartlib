package edu.ap.bib_backend.dto;

import java.util.List;

import edu.ap.bib_backend.jpa.enums.Role;
import lombok.Builder;

@Builder
public record AuthUserDto(
        String id,
        String firstName,
        String lastName,
        String fullName,
        Role role,
        String email,
        String smartschoolId,
        String platform,
        String primaryGroupName,
        List<String> parentGroupNames,
        boolean reviewBlocked,

        Long schoolId,
        String schoolName,
        Long campusId,
        String campusName,
        SchoolSummaryDto school
) {}