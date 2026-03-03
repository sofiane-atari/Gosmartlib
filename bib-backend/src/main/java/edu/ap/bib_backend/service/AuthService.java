package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AuthUserDto;

public interface AuthService {
    AuthUserDto getCurrentUser();
}
