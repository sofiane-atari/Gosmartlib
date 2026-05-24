package edu.ap.bib_backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.service.api.SchoolApiProviderFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/auth")
public class AuthInitiateController {

    private final SchoolApiProviderFactory providerFactory;

    public AuthInitiateController(SchoolApiProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
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
