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
    CommandLineRunner initGenres(GenreRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                    new Genre("Fictie algemeen", "#95A5A6", "Diverse verzonnen verhalen die niet onder één specifiek subgenre vallen."),
                    new Genre("Literaire roman", "#6C5B7B", "Verhalen met diepgang, focus op stijl en psychologische ontwikkeling."),
                    new Genre("Spanning / thriller", "#C0392B", "Spannende boeken vol mysterie, gevaar en onverwachte wendingen."),
                    new Genre("Detective / misdaad", "#34495E", "Verhalen rond misdaadonderzoek, speurwerk en geheimen."),
                    new Genre("Fantasy", "#8E44AD", "Boeken met magische werelden, mythische wezens en epische avonturen."),
                    new Genre("Sciencefiction", "#2980B9", "Verhalen over technologie, ruimte, toekomst en wetenschap."),
                    new Genre("Dystopie", "#434343", "Verhalen over een denkbeeldige samenleving met angstaanjagende kenmerken."),
                    new Genre("Historische roman", "#A0522D", "Verhalen die zich afspelen tegen een authentieke historische achtergrond."),
                    new Genre("Romantiek", "#E91E63", "Boeken waarin de liefde en interpersoonlijke relaties centraal staan."),
                    new Genre("Coming-of-age", "#F39C12", "Verhalen over de groei van kindertijd naar volwassenheid."),
                    new Genre("Avontuur", "#27AE60", "Verhalen met reizen, ontdekkingen en actievolle gebeurtenissen."),
                    new Genre("Oorlog & conflict", "#4B5320", "Verhalen die zich afspelen tijdens gewapende conflicten of oorlogssituaties."),
                    new Genre("Horror", "#000000", "Boeken bedoeld om spanning, angst en griezel op te roepen."),
                    new Genre("Humor", "#F1C40F", "Verhalen geschreven met het doel de lezer te vermaken en te laten lachen."),
                    new Genre("Graphic novel / strip", "#FF6F61", "Verhalen verteld door middel van een combinatie van tekst en illustraties."),
                    new Genre("Poëzie", "#9B59B6", "Bundels met gedichten en literaire teksten in versvorm."),
                    new Genre("Non-fictie algemeen", "#16A085", "Informatieve boeken gebaseerd op de werkelijkheid en feiten.")
                ));
                System.out.println("Genres succesvol geseedt!");
            } else {
                System.out.println("Genres waren al aanwezig, overslaan seeder.");
            }
        };
    }
}