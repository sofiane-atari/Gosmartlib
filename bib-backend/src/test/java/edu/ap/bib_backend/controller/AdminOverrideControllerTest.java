package edu.ap.bib_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.dto.AdminOverrideStatusDto;
import edu.ap.bib_backend.service.AdminOverrideService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminOverrideControllerTest {

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;
        private AdminOverrideService adminOverrideService;

        @BeforeEach
        void setUp() {
                adminOverrideService = mock(AdminOverrideService.class);
                objectMapper = new ObjectMapper();

                AdminOverrideController controller = new AdminOverrideController(adminOverrideService);

                mockMvc = MockMvcBuilders
                                .standaloneSetup(controller)
                                .setControllerAdvice(new TestExceptionHandler())
                                .build();
        }

        @Test
        void givenAuthenticatedUser_whenGetStatus_thenReturnCurrentStatus() throws Exception {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                "student-1",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

                when(adminOverrideService.getStatus("student-1"))
                                .thenReturn(new AdminOverrideStatusDto(true, 120L));

                mockMvc.perform(get("/auth/admin-override/status")
                                .principal(authentication))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.enabled").value(true))
                                .andExpect(jsonPath("$.expiresInSeconds").value(120));

                verify(adminOverrideService).getStatus("student-1");
        }

        @Test
        void givenAuthenticatedUser_whenToggleOverride_thenReturnUpdatedStatus() throws Exception {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                "student-2",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

                when(adminOverrideService.setOverride(eq("student-2"), eq(true), startsWith("127.")))
                                .thenReturn(new AdminOverrideStatusDto(true, 1800L));

                mockMvc.perform(post("/auth/admin-override")
                                .principal(authentication)
                                .with(request -> {
                                        request.setRemoteAddr("127.0.0.1");
                                        return request;
                                })
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new TogglePayload(true))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.enabled").value(true))
                                .andExpect(jsonPath("$.expiresInSeconds").value(1800));

                verify(adminOverrideService)
                                .setOverride(eq("student-2"), eq(true), startsWith("127."));
        }

        @Test
        void givenUnauthenticatedUser_whenGetStatus_thenReturn401() throws Exception {
                mockMvc.perform(get("/auth/admin-override/status"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void givenUnauthenticatedUser_whenToggleOverride_thenReturn401() throws Exception {
                mockMvc.perform(post("/auth/admin-override")
                                .with(request -> {
                                        request.setRemoteAddr("127.0.0.1");
                                        return request;
                                })
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new TogglePayload(true))))
                                .andExpect(status().isUnauthorized());
        }

        private record TogglePayload(boolean enabled) {
        }
}