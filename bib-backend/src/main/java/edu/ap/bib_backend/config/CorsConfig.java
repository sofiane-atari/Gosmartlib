package edu.ap.bib_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${APP_CORS_ALLOWED_ORIGINS}")
    private String[] allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Sta alles toe van localhost:4200
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("Authorization", "Content-Type", "X-Mock-Role")
                        .allowCredentials(true);
                        // .allowPrivateNetwork(true)
                        // .exposedHeaders("Access-Control-Allow-Private-Network")
                        // .allowedOrigins("https://frontend-yazan.gosmartlib.be");

            }
        };
    }
}