package edu.ap.bib_backend.seed.dev;

import edu.ap.bib_backend.jpa.enums.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class DevSeedAuthentication {

    public static Authentication forUser(String userId, Role role) {
        String roleAuthority = "ROLE_" + role.name();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleAuthority));
        return new UsernamePasswordAuthenticationToken(userId, "", authorities);
    }

    public static void setContext(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }
}