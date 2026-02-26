package edu.ap.bib_backend.dto;

import edu.ap.bib_backend.jpa.entity.Role;

public record AuthUserDto(String id, String name, Role role) {



} 