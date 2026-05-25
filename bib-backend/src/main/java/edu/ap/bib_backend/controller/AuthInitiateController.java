package edu.ap.bib_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.service.api.SchoolApiProviderFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthInitiateController {

    private final SchoolApiProviderFactory providerFactory;
    private final boolean mockModeEnabled;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final HttpSessionOAuth2AuthorizationRequestRepository authRequestRepository;

    public AuthInitiateController(SchoolApiProviderFactory providerFactory,
                                  @Value("${app.mock-mode-enabled}") boolean mockModeEnabled,
                                  ClientRegistrationRepository clientRegistrationRepository) {
        this.providerFactory = providerFactory;
        this.mockModeEnabled = mockModeEnabled;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @GetMapping("/initiate-login")
    public void initiateLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (mockModeEnabled) {
            response.sendRedirect(request.getContextPath() + "/oauth2/authorization/smartschool-mock");
            return;
        }

        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("smartschool");
        if (registration == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Smartschool OAuth2 client not configured");
            return;
        }

        String state = UUID.randomUUID().toString();

        OAuth2AuthorizationRequest authRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                .clientId(registration.getClientId())
                .redirectUri(registration.getRedirectUri())
                .scopes(Set.copyOf(registration.getScopes()))
                .state(state)
                .attributes(attrs -> attrs.put(OAuth2ParameterNames.REGISTRATION_ID, "smartschool"))
                .build();

        authRequestRepository.saveAuthorizationRequest(authRequest, request, response);

        String redirectUrl = authRequest.getAuthorizationUri()
                + "?response_type=code"
                + "&client_id=" + authRequest.getClientId()
                + "&redirect_uri=" + java.net.URLEncoder.encode(authRequest.getRedirectUri(), java.nio.charset.StandardCharsets.UTF_8)
                + "&scope=" + java.net.URLEncoder.encode(String.join(" ", authRequest.getScopes()), java.nio.charset.StandardCharsets.UTF_8)
                + "&state=" + authRequest.getState();

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/initiate")
    public void initiateLogin(@RequestParam String school, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (providerFactory.isMockSchool(school)) {
            response.sendRedirect(request.getContextPath() + "/oauth2/authorization/smartschool-mock");
        } else {
            response.sendRedirect(request.getContextPath() + "/oauth2/authorization/smartschool");
        }
    }
}
