package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartschoolOAuth2UserServiceUnitTest {

    @Mock
    private UserService userService;
    @Mock
    private RoleService roleService;
    @Mock
    private SmartschoolAudienceResolverService audienceResolverService;
    @Mock
    private GroupService groupService;
    @Mock
    private SmartschoolProfileService smartschoolProfileService;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private OneRosterClient oneRosterClient;

    @InjectMocks
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @Test
    void Given_SchoolCode_When_MapSchoolCodeToDomainName_Then_ReturnsCorrectDomain() {
        // This is testing a private method via reflection or testing its side effects
        // But since mapSchoolCodeToDomainName is private, I'll test it via resolveSchool if I could
        // However, I can test extractSchoolNameFromDomain
        
        // Act
        String result = (String) ReflectionTestUtils.invokeMethod(smartschoolOAuth2UserService, "extractSchoolNameFromDomain", "zemermin.smartschool.be");
        
        // Assert
        assertEquals("Zemermin", result);
    }

    @Test
    void Given_UrlSchoolCode_When_MapSchoolCodeToDomainName_Then_ReturnsDomainPart() {
        // Act
        String result = (String) ReflectionTestUtils.invokeMethod(smartschoolOAuth2UserService, "mapSchoolCodeToDomainName", "https://myschool.smartschool.be/");
        
        // Assert
        assertEquals("myschool.smartschool.be", result);
    }
}
