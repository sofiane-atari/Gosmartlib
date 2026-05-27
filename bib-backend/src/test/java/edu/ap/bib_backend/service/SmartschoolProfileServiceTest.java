package edu.ap.bib_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.api.SchoolApiProviderFactory;
import edu.ap.bib_backend.service.mock.MockSchoolRegistry;
import edu.ap.bib_backend.service.smartschool.SmartschoolProfileService;

@ExtendWith(MockitoExtension.class)
class SmartschoolProfileServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SchoolApiProviderFactory providerFactory;

    @Mock
    private MockSchoolRegistry mockSchoolRegistry;

    @Mock
    private JwtService jwtService;

    private SmartschoolProfileService smartschoolProfileService;

    private final String FULL_USER_INFO_URL = "https://fake.smartschool.be/userinfo";
    private final String GROUP_INFO_URL = "https://fake.smartschool.be/groupinfo";
    private final String DUMMY_TOKEN = "dummy-access-token";

    @BeforeEach
    void setUp() {
        smartschoolProfileService = new SmartschoolProfileService(
                restTemplate,
                FULL_USER_INFO_URL,
                GROUP_INFO_URL,
                providerFactory,
                mockSchoolRegistry,
                jwtService
        );
    }

    @Test
    @DisplayName("Given valid response from API, When fetchProfile is called, Then return mapped ProfileDto")
    void givenValidResponse_whenFetchProfile_thenReturnMappedProfileDto() {
        // Arrange
        Map<String, Object> mockBody = Map.of(
                "name", "John",
                "surname", "Doe",
                "fullname", "John Doe",
                "email", "john.doe@school.be",
                "platform", "https://school.smartschool.be",
                "userID", "user-123"
        );
        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(mockBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(FULL_USER_INFO_URL),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<Void>>any(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(mockResponse);

        // Act
        SmartschoolProfileDto result = smartschoolProfileService.fetchProfile(DUMMY_TOKEN);

        // Assert
        assertNotNull(result);
        assertEquals("John", result.firstName());
        assertEquals("Doe", result.lastName());
        assertEquals("John Doe", result.fullName());
        assertEquals("john.doe@school.be", result.email());
        assertEquals("https://school.smartschool.be", result.platform());
        assertEquals("user-123", result.userId());
    }

    @Test
    @DisplayName("Given empty body from API, When fetchProfile is called, Then throw IllegalStateException")
    void givenEmptyBody_whenFetchProfile_thenThrowIllegalStateException() {
        // Arrange
        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(FULL_USER_INFO_URL),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<Void>>any(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(mockResponse);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            smartschoolProfileService.fetchProfile(DUMMY_TOKEN);
        });
        assertEquals("Smartschool profile response was empty", exception.getMessage());
    }

    @Test
    @DisplayName("Given RestClientException, When fetchProfile is called, Then throw wrapped IllegalStateException")
    void givenRestClientException_whenFetchProfile_thenThrowWrappedException() {
        // Arrange
        when(restTemplate.exchange(
                eq(FULL_USER_INFO_URL),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<Void>>any(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(new RestClientException("Connection timeout"));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            smartschoolProfileService.fetchProfile(DUMMY_TOKEN);
        });
        assertEquals("Failed to fetch Smartschool profile", exception.getMessage());
        assertTrue(exception.getCause() instanceof RestClientException);
    }


    @Test
    @DisplayName("Given valid full response from API, When fetchGroups is called, Then return mapped GroupDto")
    void givenValidResponse_whenFetchGroups_thenReturnMappedGroupDto() {
        // Arrange
        Map<String, Object> mockBody = Map.of(
                "groups", List.of(Map.of("name", "Teachers")),
                "parentGroups", List.of(
                        Map.of("name", "Staff"),
                        Map.of("name", "All Users")
                )
        );
        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(mockBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(GROUP_INFO_URL),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<Void>>any(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(mockResponse);

        // Act
        SmartschoolGroupDto result = smartschoolProfileService.fetchGroups(DUMMY_TOKEN);

        // Assert
        assertNotNull(result);
        assertEquals("Teachers", result.primaryGroupName());
        assertEquals(2, result.parentGroupNames().size());
        assertTrue(result.parentGroupNames().contains("Staff"));
        assertTrue(result.parentGroupNames().contains("All Users"));
    }

    @Test
    @DisplayName("Given response with empty lists from API, When fetchGroups is called, Then return empty GroupDto")
    void givenEmptyListsResponse_whenFetchGroups_thenReturnEmptyGroupDto() {
        // Arrange
        Map<String, Object> mockBody = Map.of(
                "groups", List.of(),
                "parentGroups", List.of()
        );
        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(mockBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(GROUP_INFO_URL),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<Void>>any(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(mockResponse);

        // Act
        SmartschoolGroupDto result = smartschoolProfileService.fetchGroups(DUMMY_TOKEN);

        // Assert
        assertNotNull(result);
        assertNull(result.primaryGroupName());
        assertTrue(result.parentGroupNames().isEmpty());
    }

    @Test
    @DisplayName("Given empty body from API, When fetchGroups is called, Then throw IllegalStateException")
    void givenEmptyBody_whenFetchGroups_thenThrowIllegalStateException() {
        // Arrange
        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(GROUP_INFO_URL),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<Void>>any(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(mockResponse);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            smartschoolProfileService.fetchGroups(DUMMY_TOKEN);
        });
        assertEquals("Smartschool group response was empty", exception.getMessage());
    }

    @Test
    @DisplayName("Given RestClientException, When fetchGroups is called, Then throw wrapped IllegalStateException")
    void givenRestClientException_whenFetchGroups_thenThrowWrappedException() {
        // Arrange
        when(restTemplate.exchange(
                eq(GROUP_INFO_URL),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<Void>>any(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(new RestClientException("Connection timeout"));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            smartschoolProfileService.fetchGroups(DUMMY_TOKEN);
        });
        assertEquals("Failed to fetch Smartschool groups", exception.getMessage());
        assertTrue(exception.getCause() instanceof RestClientException);
    }
}