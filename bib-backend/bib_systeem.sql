-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: bib_systeem

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP TABLE IF EXISTS `book_copies`;
CREATE TABLE `book_copies` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `availability` enum('AVAILABLE','BORROWED','DAMAGED','LOST') NOT NULL,
                               `book_id` bigint DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FKhlawea8y2e2dv0ta58vc6f5nr` (`book_id`),
                               CONSTRAINT `FKhlawea8y2e2dv0ta58vc6f5nr` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `book_copies` WRITE;
INSERT INTO `book_copies` VALUES (1,'AVAILABLE',1),(2,'AVAILABLE',2);
UNLOCK TABLES;

DROP TABLE IF EXISTS `book_genres`;
CREATE TABLE `book_genres` (
                               `book_id` bigint NOT NULL,
                               `genre_name` varchar(255) NOT NULL,
                               KEY `FKjoo9dmvm5l6fth5t1j5adak7u` (`genre_name`),
                               KEY `FKtqnlma9c5byf3gfqsuu0ebrl5` (`book_id`),
                               CONSTRAINT `FKjoo9dmvm5l6fth5t1j5adak7u` FOREIGN KEY (`genre_name`) REFERENCES `genres` (`name`),
                               CONSTRAINT `FKtqnlma9c5byf3gfqsuu0ebrl5` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `book_genres` WRITE;
INSERT INTO `book_genres` VALUES (1,'Biografie'),(1,'Pozie'),(1,'Tijdschrift'),(2,'Biografie');
UNLOCK TABLES;

DROP TABLE IF EXISTS `book_loans`;
CREATE TABLE `book_loans` (
                              `id` bigint NOT NULL,
                              `book_copy_id` bigint DEFAULT NULL,
                              `user_id` varchar(255) DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              KEY `FKoyld3dbrnunaj75c7gy1w5qay` (`book_copy_id`),
                              KEY `FKjs7dicejnhi0pm0d4yeo07p39` (`user_id`),
                              CONSTRAINT `FKjs7dicejnhi0pm0d4yeo07p39` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                              CONSTRAINT `FKoyld3dbrnunaj75c7gy1w5qay` FOREIGN KEY (`book_copy_id`) REFERENCES `book_copies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `books`;
CREATE TABLE `books` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `isbn` varchar(255) DEFAULT NULL,
                         `added_date` datetime(6) DEFAULT NULL,
                         `author` varchar(255) DEFAULT NULL,
                         `description` text,
                         `image_url` varchar(255) DEFAULT NULL,
                         `pages` int NOT NULL,
                         `release_year` int DEFAULT NULL,
                         `title` varchar(255) DEFAULT NULL,
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `books` WRITE;
INSERT INTO `books` VALUES
                        (1,'9780135957059','2026-03-22 21:18:27.292771','Andy Hunt','dsdsdsd','https://covers.openlibrary.org/b/id/10143650-M.jpg',352,2019,'The Pragmatic Programmer'),
                        (2,'9781408883761','2026-03-22 21:18:51.680717','J. K. Rowling','test','https://covers.openlibrary.org/b/id/8788413-M.jpg',368,2018,'Harry Potter and the Philosopher\'s Stone: Slytherin Edition; Black and Green');
UNLOCK TABLES;

DROP TABLE IF EXISTS `genres`;
CREATE TABLE `genres` (
  `name` varchar(255) NOT NULL,
  `color` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `genres` WRITE;
INSERT INTO `genres` VALUES
  ('Avontuur','#27AE60','Verhalen met reizen, ontdekkingen en actievolle gebeurtenissen.'),
  ('Biografie','#F39C12','Levensverhalen van echte personen.'),
  ('Detective','#34495E','Verhalen rond misdaadonderzoek, speurwerk en geheimen.'),
  ('Educatief','#3498DB','Boeken met een leerrijk of didactisch karakter.'),
  ('Fantasy','#8E44AD','Boeken met magische werelden, mythische wezens en epische avonturen.'),
  ('Historisch','#A0522D','Verhalen die zich afspelen in een historische periode.'),
  ('Horror','#2C3E50','Boeken bedoeld om spanning, angst en griezel op te roepen.'),
  ('Jeugd','#F1C40F','Boeken geschikt voor kinderen en jongeren.'),
  ('Pozie','#9B59B6','Bundels met gedichten en literaire teksten in versvorm.'),
  ('Romantiek','#E91E63','Boeken waarin liefde en relaties centraal staan.'),
  ('Schoolpublicatie','#16A085','Publicaties gemaakt binnen een schoolcontext.'),
  ('Science Fiction','#2980B9','Verhalen over technologie, ruimte, toekomst en wetenschap.'),
  ('Stripverhaal','#FF6F61','Verhalen verteld met illustraties en tekstballonnen.'),
  ('Thriller','#C0392B','Spannende boeken vol mysterie, gevaar en onverwachte wendingen.'),
  ('Tijdschrift','#D35400','Periodieke uitgaven zoals magazines en themabladen.');
UNLOCK TABLES;

DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `role` enum('ADMIN','LIBRARIAN','STUDENT','TEACHER') NOT NULL,
  PRIMARY KEY (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `roles` WRITE;
INSERT INTO `roles` VALUES ('ADMIN'),('LIBRARIAN'),('STUDENT'),('TEACHER');
UNLOCK TABLES;

DROP TABLE IF EXISTS `schools`;
CREATE TABLE `schools` (
  `domain_name` varchar(255) NOT NULL,
  `adres` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `postal_code` int NOT NULL,
  PRIMARY KEY (`domain_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` varchar(255) NOT NULL,
  `role_id` enum('ADMIN','LIBRARIAN','STUDENT','TEACHER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKp56c1712k691lhsyewcssf40f` (`role_id`),
  CONSTRAINT `FKp56c1712k691lhsyewcssf40f` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `users` WRITE;
INSERT INTO `users` VALUES ('kWKGeclacHlfpTnlDfqbUA==','STUDENT');
UNLOCK TABLES;

-- reading_lists: gecombineerde versie (teammate had user_id, target_type, target_value)
DROP TABLE IF EXISTS `reading_list_books`;
DROP TABLE IF EXISTS `reading_lists`;
CREATE TABLE `reading_lists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `target_type` enum('CLASS','PERSONAL','SCHOOL','YEAR') NOT NULL,
  `target_value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_reading_lists_user` (`user_id`),
  CONSTRAINT `FK_reading_lists_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reading_list_books` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL,
  `reading_list_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjdsu7rnp4sii37klutiwhkh6m` (`reading_list_id`),
  CONSTRAINT `FKjdsu7rnp4sii37klutiwhkh6m` FOREIGN KEY (`reading_list_id`) REFERENCES `reading_lists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `reviews`;
CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` varchar(1000) DEFAULT NULL,
  `rating` int NOT NULL,
  `report_count` int NOT NULL,
  `book_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6a9k6xvev80se5rreqvuqr7f9` (`book_id`),
  CONSTRAINT `FK6a9k6xvev80se5rreqvuqr7f9` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;