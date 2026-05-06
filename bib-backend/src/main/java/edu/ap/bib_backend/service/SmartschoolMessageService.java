package edu.ap.bib_backend.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class SmartschoolMessageService {

    private static final Logger log = LoggerFactory.getLogger(SmartschoolMessageService.class);

    private final RestTemplate restTemplate;
    private final String smartschoolBaseUrl;

    public SmartschoolMessageService(
            @Value("${spring.security.oauth2.client.provider.smartschool.user-info-uri}") String userInfoUrl) {
        this.restTemplate = new RestTemplate();
        this.smartschoolBaseUrl = userInfoUrl.replace("/Api/V1/fulluserinfo", "");
    }

    public void sendMessage(String accessToken, String title, String body) {
        try {
            org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(smartschoolBaseUrl + "/Api/V1/sendmsg")
                    .queryParam("access_token", accessToken)
                    .queryParam("messageTitle", title)
                    .queryParam("messageBody", body);

            log.debug("Sending Smartschool message with title: {}", title);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    builder.build().toUri(),
                    HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("Smartschool API raw response: {}", response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to send Smartschool message: HTTP {}", response.getStatusCode());
                throw new IllegalStateException("Failed to send Smartschool message: " + response.getStatusCode());
            }

            if (response.getBody() != null) {
                Object success = response.getBody().get("success");
                if (success instanceof Boolean && !(Boolean) success) {
                    log.error("Smartschool API returned success=false: {}", response.getBody());
                    throw new IllegalStateException("Smartschool API returned failure: " + response.getBody());
                }
            }

            log.info("Successfully sent Smartschool message with title: {}", title);
        } catch (RestClientException exception) {
            log.error("RestClientException while sending Smartschool message: {}", exception.getMessage(), exception);
            throw new IllegalStateException("Failed to send Smartschool message", exception);
        }
    }
}
