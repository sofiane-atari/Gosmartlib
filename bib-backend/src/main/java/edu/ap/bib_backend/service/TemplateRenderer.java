package edu.ap.bib_backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class TemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(TemplateRenderer.class);

    public String render(String templatePath, Map<String, String> placeholders) {
        String template = loadTemplate(templatePath);
        if (template == null) {
            log.error("Template not found: {}", templatePath);
            return "";
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        return template;
    }

    private String loadTemplate(String templatePath) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            if (!resource.exists()) {
                log.warn("Template resource does not exist: {}", templatePath);
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to load template: {}", templatePath, e);
            return null;
        }
    }
}
