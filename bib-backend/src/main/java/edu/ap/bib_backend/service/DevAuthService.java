package edu.ap.bib_backend.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.entity.Role;

@Service
public class DevAuthService implements AuthService {

    @Override
    public AuthUserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Default fallback role
        Role currentRole = Role.STUDENT; 
        String name = "Mock Student";

        if (authentication != null && authentication.getAuthorities().stream().findFirst().isPresent()) {
            // Extract the role from the authority string (e.g., "ROLE_ADMIN" -> "ADMIN")
            String roleString = authentication.getAuthorities().stream().findFirst().get().getAuthority().replace("ROLE_", "");
            currentRole = Role.valueOf(roleString);
            
            // Format name
            name = "Mock " + roleString.charAt(0) + roleString.substring(1).toLowerCase();
        }

        return new AuthUserDto("1", name, currentRole);
    }


}