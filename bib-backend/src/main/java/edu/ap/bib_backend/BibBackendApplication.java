package edu.ap.bib_backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BibBackendApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(BibBackendApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
