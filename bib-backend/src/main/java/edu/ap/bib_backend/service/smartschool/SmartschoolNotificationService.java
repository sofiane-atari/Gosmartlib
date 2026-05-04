package edu.ap.bib_backend.service.smartschool;

import org.springframework.stereotype.Service;
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
                .fromUriString(normalizeBaseUrl(smartschoolBaseUrl) + "/Api/V1/sendnotif")
                .queryParam("access_token", accessToken)
                .queryParam("notifTitle", title)
                .queryParam("notifDesc", description)
                .queryParam("notifUrl", url)
                .toUriString();

        restTemplate.getForObject(requestUrl, String.class);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Smartschool base URL ontbreekt.");
        }

        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            return "https://" + baseUrl;
        }

        return baseUrl;
    }
}