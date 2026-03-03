package edu.ap.bib_backend;

import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.service.ReadingListService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BibBackendApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(BibBackendApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
