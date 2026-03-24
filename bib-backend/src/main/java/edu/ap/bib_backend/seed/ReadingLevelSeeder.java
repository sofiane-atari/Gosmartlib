package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReadingLevelSeeder {

    @Bean
    CommandLineRunner seedReadingLevels(ReadingLevelRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new ReadingLevel("A", "eenvoudig taalgebruik, korte zinnen, sterk verhalend, weinig abstractie"));
                repository.save(new ReadingLevel("B", "gemiddeld taalgebruik, herkenbare structuren, beperkte complexiteit"));
                repository.save(new ReadingLevel("C", "complexer, meerdere verhaallijnen of perspectieven, rijkere woordenschat"));
                repository.save(new ReadingLevel("D", "literair, gelaagd, abstracte thema’s, impliciete betekenis"));
            }
        };
    }
}
