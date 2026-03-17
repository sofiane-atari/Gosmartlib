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

import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import java.util.Collections;

@Service
public class SmartschoolOAuth2UserService 
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final RestTemplate restTemplate = new RestTemplate();

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

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_STUDENT")),
                attributes,
                "userID"   // attribute that identifies the user
        );
    }
}
