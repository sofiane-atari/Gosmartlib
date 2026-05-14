package edu.ap.bib_backend.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
        log.debug("sendMessage: title='{}', body length={} chars, body preview (first 200)='{}'",
                title, body != null ? body.length() : 0, body != null ? body.substring(0, Math.min(200, body.length())) : "null");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
            formParams.add("access_token", accessToken);
            formParams.add("messageTitle", title);
            formParams.add("messageBody", body);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formParams, headers);

            log.debug("Sending POST to Smartschool API, body size={} chars", body != null ? body.length() : 0);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    smartschoolBaseUrl + "/Api/V1/sendmsg",
                    HttpMethod.POST,
                    requestEntity,
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
