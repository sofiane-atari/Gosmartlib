package edu.ap.bib_backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Arrays;
import java.util.List;

public class SchoolAwareAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;
    private final List<String> realSchoolDomains;

    public SchoolAwareAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            String realSchoolDomainsStr) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
        this.realSchoolDomains = realSchoolDomainsStr == null || realSchoolDomainsStr.isBlank()
                ? List.of()
                : Arrays.stream(realSchoolDomainsStr.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = delegate.resolve(request);
        return overrideAuthorizationUri(request, req);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        OAuth2AuthorizationRequest req = delegate.resolve(request, registrationId);
        return overrideAuthorizationUri(request, req);
    }

    private OAuth2AuthorizationRequest overrideAuthorizationUri(HttpServletRequest request, OAuth2AuthorizationRequest req) {
        if (req == null) {
            return null;
        }

        String school = request.getParameter("school");
        if (school == null || school.isBlank()) {
            return req;
        }

        String normalized = school.trim().toLowerCase();
        if (!realSchoolDomains.contains(normalized)) {
            return req;
        }

        return OAuth2AuthorizationRequest.from(req)
                .authorizationUri("https://" + normalized + "/OAuth/index/signin")
                .build();
    }
}
