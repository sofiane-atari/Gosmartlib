package edu.ap.bib_backend.service.didactic;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterial;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.service.user.CurrentUserService;

@Service
public class DidacticMaterialAuthorizationService {

    private final CurrentUserService currentUserService;

    public DidacticMaterialAuthorizationService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public void assertCanModify(DidacticMaterial material, Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        if (hasRole(authentication, Role.ADMIN)) {
            return;
        }

        boolean isOwner = currentUserId.equals(material.getCreatedByUserId());
        boolean isTeacherOrLibrarian = hasRole(authentication, Role.TEACHER)
                || hasRole(authentication, Role.LIBRARIAN);

        if (isOwner && isTeacherOrLibrarian) {
            return;
        }

        throw new AccessDeniedException("Je hebt geen toestemming om dit didactisch materiaal te wijzigen.");
    }

    private boolean hasRole(Authentication authentication, Role role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }
}