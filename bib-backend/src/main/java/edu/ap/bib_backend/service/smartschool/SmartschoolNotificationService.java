package edu.ap.bib_backend.service.smartschool;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SmartschoolNotificationService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendNotification(
            String smartschoolBaseUrl,
            String accessToken,
            String title,
            String description,
            String url) {
        String requestUrl = UriComponentsBuilder
                .fromUriString(normalizeBaseUrl(smartschoolBaseUrl))
                .path("/Api/V1/sendnotif")
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("access_token", accessToken);
        body.add("notifTitle", title);
        body.add("notifDesc", description);
        body.add("notifUrl", url);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                requestUrl,
                request,
                String.class);

        System.out.println("Smartschool sendnotif status: " + response.getStatusCode());
        System.out.println("Smartschool sendnotif response: " + response.getBody());
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Smartschool base URL ontbreekt.");
        }

        String normalized = baseUrl.trim();

        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }

        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }
}