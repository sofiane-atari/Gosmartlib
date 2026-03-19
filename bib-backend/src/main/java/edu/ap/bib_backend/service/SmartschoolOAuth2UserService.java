package edu.ap.bib_backend.service;


import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.ap.bib_backend.jpa.entity.Role;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;

import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import java.util.Collections;

@Service
public class SmartschoolOAuth2UserService 
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final RestTemplate restTemplate = new RestTemplate();
    private final RoleService roleService;
    private final UserRepository userRepository;
    private final RoleEntityRepository roleEntityRepository;

    public SmartschoolOAuth2UserService(RoleService roleService, 
                                         UserRepository userRepository,
                                         RoleEntityRepository roleEntityRepository) {
        this.roleService = roleService;
        this.userRepository = userRepository;
        this.roleEntityRepository = roleEntityRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        String userInfoEndpoint = userRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoEndpoint,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> attributes = response.getBody();

        String userId = (String) attributes.get("userID");
        String basisrol = (String) attributes.get("basisrol");

        String roleString = getOrCreateUserRole(userId, basisrol);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(roleString)),
                attributes,
                "userID"
        );
    }

    private String getOrCreateUserRole(String userId, String basisrol) {
        return userRepository.findById(userId)
                .map(user -> "ROLE_" + user.getRole().getRole().name())
                .orElseGet(() -> {
                    Role mappedRole = mapDutchRole(basisrol);

                    RoleEntity roleEntity = roleEntityRepository.findByRole(mappedRole)
                            .orElseThrow(() -> new RuntimeException("Role not found: " + mappedRole));

                    User newUser = new User();
                    newUser.setId(userId);
                    newUser.setRole(roleEntity);
                    userRepository.save(newUser);

                    return "ROLE_" + mappedRole.name();
                });
    }

    private Role mapDutchRole(String basisrol) {
        if (basisrol == null) return Role.STUDENT;
        return switch (basisrol.toLowerCase()) {
            case "leerling"     -> Role.STUDENT;
            case "leerkracht"   -> Role.TEACHER;
            case "directie"     -> Role.ADMIN;
            case "bibliothecaris" -> Role.LIBRARIAN;
            default             -> Role.STUDENT;
        };
    }
}