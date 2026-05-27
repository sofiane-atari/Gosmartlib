package edu.ap.bib_backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolAwareAuthorizationRequestResolverTest {

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    private SchoolAwareAuthorizationRequestResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SchoolAwareAuthorizationRequestResolver(clientRegistrationRepository);
    }

    @Test
    void Given_RequestWithNoSchoolParam_When_Resolve_Then_ReturnsDefaultRequest() {
        ClientRegistration registration = registration();
        when(clientRegistrationRepository.findByRegistrationId(anyString())).thenReturn(registration);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/oauth2/authorization/smartschool");
        request.setRequestURI("/oauth2/authorization/smartschool");

        OAuth2AuthorizationRequest result = resolver.resolve(request);

        assertNotNull(result);
    }

    @Test
    void Given_RequestWithSchoolParam_When_Resolve_Then_OverridesAuthorizationUri() {
        ClientRegistration registration = registration();
        when(clientRegistrationRepository.findByRegistrationId(anyString())).thenReturn(registration);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/oauth2/authorization/smartschool");
        request.setRequestURI("/oauth2/authorization/smartschool");
        request.setParameter("school", "myschool.smartschool.be");

        OAuth2AuthorizationRequest result = resolver.resolve(request);

        assertNotNull(result);
        assertEquals("https://myschool.smartschool.be/OAuth/index/signin", result.getAuthorizationUri());
    }

    @Test
    void Given_RequestWithSchoolParam_When_ResolveWithRegistrationId_Then_OverridesAuthorizationUri() {
        ClientRegistration registration = registration();
        when(clientRegistrationRepository.findByRegistrationId("smartschool")).thenReturn(registration);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("school", "other.smartschool.be");

        OAuth2AuthorizationRequest result = resolver.resolve(request, "smartschool");

        assertNotNull(result);
        assertEquals("https://other.smartschool.be/OAuth/index/signin", result.getAuthorizationUri());
    }

    @Test
    void Given_RequestWithBlankSchoolParam_When_Resolve_Then_ReturnsDefaultRequest() {
        ClientRegistration registration = registration();
        when(clientRegistrationRepository.findByRegistrationId(anyString())).thenReturn(registration);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/oauth2/authorization/smartschool");
        request.setRequestURI("/oauth2/authorization/smartschool");
        request.setParameter("school", "   ");

        OAuth2AuthorizationRequest result = resolver.resolve(request);

        assertNotNull(result);
        assertTrue(result.getAuthorizationUri().contains("smartschool.be"));
    }

    @Test
    void Given_NoMatchingRegistration_When_Resolve_Then_ThrowsException() {
        when(clientRegistrationRepository.findByRegistrationId(anyString())).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/oauth2/authorization/nonexistent");
        request.setRequestURI("/oauth2/authorization/nonexistent");

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(request));
    }

    @Test
    void Given_SchoolParamWithWhitespace_When_Resolve_Then_NormalizesAndOverridesUri() {
        ClientRegistration registration = registration();
        when(clientRegistrationRepository.findByRegistrationId(anyString())).thenReturn(registration);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/oauth2/authorization/smartschool");
        request.setRequestURI("/oauth2/authorization/smartschool");
        request.setParameter("school", "  MySchool.Smartschool.Be  ");

        OAuth2AuthorizationRequest result = resolver.resolve(request);

        assertNotNull(result);
        assertEquals("https://myschool.smartschool.be/OAuth/index/signin", result.getAuthorizationUri());
    }

    private ClientRegistration registration() {
        return ClientRegistration.withRegistrationId("smartschool")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .redirectUri("http://localhost/callback")
                .authorizationUri("https://default.smartschool.be/OAuth/index/signin")
                .tokenUri("https://default.smartschool.be/OAuth/token")
                .scope("openid")
                .build();
    }
}
