package edu.ap.bib_backend.config;

import edu.ap.bib_backend.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CorsConfigTest.CorsTestController.class)
@Import({ CorsConfig.class, TestSecurityConfig.class })
@TestPropertySource(properties = {
        "APP_CORS_ALLOWED_ORIGINS=http://localhost:4200,http://example.com"
})
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenAllowedOrigin_WhenPreflightRequest_ThenReturnCorsHeaders() throws Exception {
        // Arrange
        String allowedOrigin = "http://localhost:4200";

        // Act
        var result = mockMvc.perform(options("/cors-test")
                .header("Origin", allowedOrigin)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", allowedOrigin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void givenDisallowedOrigin_WhenPreflightRequest_ThenReturnForbidden() throws Exception {
        // Arrange
        String disallowedOrigin = "http://not-allowed.example.com";

        // Act
        var result = mockMvc.perform(options("/cors-test")
                .header("Origin", disallowedOrigin)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"));

        // Assert
        result.andExpect(status().isForbidden());
    }

    @RestController
    static class CorsTestController {
        @GetMapping("/cors-test")
        public String corsTest() {
            return "ok";
        }
    }
}