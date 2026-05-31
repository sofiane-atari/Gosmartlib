package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.service.api.SchoolApiProviderFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AuthInitiateControllerTest {

    @Mock
    private SchoolApiProviderFactory providerFactory;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    private AuthInitiateController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthInitiateController(providerFactory, false, clientRegistrationRepository, false);
    }

    @Test
    void Given_MockModeEnabled_When_InitiateLogin_Then_RedirectsToMock() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getContextPath()).thenReturn("");

        AuthInitiateController mockController = new AuthInitiateController(
                providerFactory, true, clientRegistrationRepository, true);

        mockController.initiateLogin(request, response);

        verify(response).sendRedirect("/oauth2/authorization/smartschool-mock");
    }

    @Test
    void Given_MockModeEnabledButDevModeDisabled_When_InitiateLogin_Then_DoesNotRedirectToMock() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(clientRegistrationRepository.findByRegistrationId("smartschool")).thenReturn(null);

        AuthInitiateController mockController = new AuthInitiateController(
                providerFactory, true, clientRegistrationRepository, false);

        mockController.initiateLogin(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Smartschool OAuth2 client not configured");
    }

    @Test
    void Given_MockModeDisabledAndNoRegistration_When_InitiateLogin_Then_ReturnsServerError() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(clientRegistrationRepository.findByRegistrationId("smartschool")).thenReturn(null);

        controller.initiateLogin(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Smartschool OAuth2 client not configured");
    }

    @Test
    void Given_MockModeDisabledWithRegistration_When_InitiateLogin_Then_RedirectsToSmartschool() throws Exception {
        ClientRegistration registration = ClientRegistration.withRegistrationId("smartschool")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .redirectUri("http://localhost/callback")
                .authorizationUri("https://smartschool.be/OAuth/index/signin")
                .tokenUri("https://smartschool.be/OAuth/token")
                .scope("openid", "profile")
                .build();
        when(clientRegistrationRepository.findByRegistrationId("smartschool")).thenReturn(registration);

        HttpServletRequest request = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);

        HttpServletResponse response = mock(HttpServletResponse.class);

        controller.initiateLogin(request, response);

        verify(response).sendRedirect(contains("https://smartschool.be/OAuth/index/signin"));
    }

    @Test
    void Given_MockSchool_When_InitiateWithSchoolParam_Then_RedirectsToMock() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getContextPath()).thenReturn("");
        when(providerFactory.isMockSchool("mock-school")).thenReturn(true);

        controller.initiateLogin("mock-school", request, response);

        verify(response).sendRedirect("/oauth2/authorization/smartschool-mock");
    }

    @Test
    void Given_NonMockSchool_When_InitiateWithSchoolParam_Then_RedirectsToSmartschool() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getContextPath()).thenReturn("");
        when(providerFactory.isMockSchool("real-school")).thenReturn(false);

        controller.initiateLogin("real-school", request, response);

        verify(response).sendRedirect("/oauth2/authorization/smartschool");
    }
}
