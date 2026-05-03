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

                Genre fictie = new Genre(
                        "Fictie algemeen",
                        "#95A5A6",
                        "Diverse verzonnen verhalen die niet onder één specifiek subgenre vallen.");

                Genre nonFictie = new Genre(
                        "Non-fictie algemeen",
                        "#16A085",
                        "Informatieve boeken gebaseerd op de werkelijkheid en feiten.");

                repository.save(fictie);
                repository.save(nonFictie);

                repository.saveAll(List.of(
                        createSubGenre("Literaire roman", "#6C5B7B",
                                "Verhalen met diepgang, focus op stijl en psychologische ontwikkeling.", fictie),
                        createSubGenre("Spanning / thriller", "#C0392B",
                                "Spannende boeken vol mysterie, gevaar en onverwachte wendingen.", fictie),
                        createSubGenre("Detective / misdaad", "#34495E",
                                "Verhalen rond misdaadonderzoek, speurwerk en geheimen.", fictie),
                        createSubGenre("Fantasy", "#8E44AD",
                                "Boeken met magische werelden, mythische wezens en epische avonturen.", fictie),
                        createSubGenre("Sciencefiction", "#2980B9",
                                "Verhalen over technologie, ruimte, toekomst en wetenschap.", fictie),
                        createSubGenre("Dystopie", "#434343",
                                "Verhalen over een denkbeeldige samenleving met angstaanjagende kenmerken.", fictie),
                        createSubGenre("Historische roman", "#A0522D",
                                "Verhalen die zich afspelen tegen een authentieke historische achtergrond.", fictie),
                        createSubGenre("Romantiek", "#E91E63",
                                "Boeken waarin de liefde en interpersoonlijke relaties centraal staan.", fictie),
                        createSubGenre("Coming-of-age", "#F39C12",
                                "Verhalen over de groei van kindertijd naar volwassenheid.", fictie),
                        createSubGenre("Avontuur", "#27AE60",
                                "Verhalen met reizen, ontdekkingen en actievolle gebeurtenissen.", fictie),
                        createSubGenre("Oorlog & conflict", "#4B5320",
                                "Verhalen die zich afspelen tijdens gewapende conflicten of oorlogssituaties.", fictie),
                        createSubGenre("Horror", "#000000",
                                "Boeken bedoeld om spanning, angst en griezel op te roepen.", fictie),
                        createSubGenre("Humor", "#F1C40F",
                                "Verhalen geschreven met het doel de lezer te vermaken en te laten lachen.", fictie),
                        createSubGenre("Graphic novel / strip", "#FF6F61",
                                "Verhalen verteld door middel van een combinatie van tekst en illustraties.", fictie),
                        createSubGenre("Poëzie", "#9B59B6",
                                "Bundels met gedichten en literaire teksten in versvorm.", fictie),

                        createSubGenre("Biografie / autobiografie", "#1ABC9C",
                                "Levensverhalen van personen, door henzelf of door anderen geschreven.", nonFictie),
                        createSubGenre("Wetenschap & technologie", "#3498DB",
                                "Boeken over wetenschappelijke inzichten, innovatie en technologische ontwikkelingen.", nonFictie),
                        createSubGenre("Filosofie", "#9B59B6",
                                "Boeken over denken, ethiek, zingeving en fundamentele levensvragen.", nonFictie),
                        createSubGenre("Maatschappij & politiek", "#E67E22",
                                "Boeken over sociale structuren, actualiteit, beleid en politieke thema's.", nonFictie),
                        createSubGenre("Psychologie", "#F39C12",
                                "Boeken over gedrag, emoties, cognitie en de menselijke geest.", nonFictie),
                        createSubGenre("Geschiedenis", "#8D6E63",
                                "Boeken over gebeurtenissen, perioden en ontwikkelingen uit het verleden.", nonFictie),
                        createSubGenre("Kunst & cultuur", "#E84393",
                                "Boeken over beeldende kunst, muziek, erfgoed, media en culturele stromingen.", nonFictie)
                ));
            }
        };
    }

    private Genre createSubGenre(String name, String color, String description, Genre parentGenre) {
        Genre genre = new Genre(name, color, description);
        genre.setParentGenre(parentGenre);
        return genre;
    }
}