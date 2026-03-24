package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GenreSeeder {

    @Bean
    CommandLineRunner seedGenres(GenreRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                        new Genre("Fantasy", "#8E44AD",
                                "Boeken met magische werelden, mythische wezens en epische avonturen."),
                        new Genre("Science Fiction", "#2980B9",
                                "Verhalen over technologie, ruimte, toekomst en wetenschap."),
                        new Genre("Thriller", "#C0392B",
                                "Spannende boeken vol mysterie, gevaar en onverwachte wendingen."),
                        new Genre("Detective", "#34495E", "Verhalen rond misdaadonderzoek, speurwerk en geheimen."),
                        new Genre("Romantiek", "#E91E63", "Boeken waarin liefde en relaties centraal staan."),
                        new Genre("Historisch", "#A0522D", "Verhalen die zich afspelen in een historische periode."),
                        new Genre("Horror", "#2C3E50", "Boeken bedoeld om spanning, angst en griezel op te roepen."),
                        new Genre("Avontuur", "#27AE60",
                                "Verhalen met reizen, ontdekkingen en actievolle gebeurtenissen."),
                        new Genre("Biografie", "#F39C12", "Levensverhalen van echte personen."),
                        new Genre("Jeugd", "#F1C40F", "Boeken geschikt voor kinderen en jongeren."),
                        new Genre("Poëzie", "#9B59B6", "Bundels met gedichten en literaire teksten in versvorm."),
                        new Genre("Schoolpublicatie", "#16A085", "Publicaties gemaakt binnen een schoolcontext."),
                        new Genre("Tijdschrift", "#D35400", "Periodieke uitgaven zoals magazines en themabladen."),
                        new Genre("Stripverhaal", "#FF6F61", "Verhalen verteld met illustraties en tekstballonnen."),
                        new Genre("Educatief", "#3498DB", "Boeken met een leerrijk of didactisch karakter.")));
            }
        };
    }
}