-- MySQL dump 10.13  Distrib 8.0.38, for Win64 (x86_64)
--
-- Host: localhost    Database: bib_systeem
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `book_copies`
--

DROP TABLE IF EXISTS `book_copies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_copies` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `availability` enum('AVAILABLE','BORROWED','DAMAGED','LOST') NOT NULL,
                               `book_id` bigint DEFAULT NULL,
                               `school_id` bigint DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FKhlawea8y2e2dv0ta58vc6f5nr` (`book_id`),
                               KEY `FK7wne1ulpak4q2toi661r8t5sb` (`school_id`),
                               CONSTRAINT `FK7wne1ulpak4q2toi661r8t5sb` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
                               CONSTRAINT `FKhlawea8y2e2dv0ta58vc6f5nr` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book_genres`
--

DROP TABLE IF EXISTS `book_genres`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_genres` (
                               `book_id` bigint NOT NULL,
                               `genre_id` bigint NOT NULL,
                               PRIMARY KEY (`book_id`,`genre_id`),
                               KEY `FK6ce5ct13whb85xqu83jmceboj` (`genre_id`),
                               CONSTRAINT `FK6ce5ct13whb85xqu83jmceboj` FOREIGN KEY (`genre_id`) REFERENCES `genres` (`id`),
                               CONSTRAINT `FKtqnlma9c5byf3gfqsuu0ebrl5` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `books` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `added_date` datetime(6) DEFAULT NULL,
                         `author` varchar(255) DEFAULT NULL,
                         `description` text,
                         `image_url` varchar(255) DEFAULT NULL,
                         `isbn` varchar(255) DEFAULT NULL,
                         `pages` int NOT NULL,
                         `release_year` int DEFAULT NULL,
                         `title` varchar(255) DEFAULT NULL,
                         `reading_level_id` bigint DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         KEY `FK99gy4hmr52fqktmd1ra85a2rl` (`reading_level_id`),
                         CONSTRAINT `FK99gy4hmr52fqktmd1ra85a2rl` FOREIGN KEY (`reading_level_id`) REFERENCES `reading_levels` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `classes`
--

DROP TABLE IF EXISTS `classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classes` (
                           `name` varchar(255) NOT NULL,
                           PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `didactic_material_attachments`
--

DROP TABLE IF EXISTS `didactic_material_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `didactic_material_attachments` (
                                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                                 `content_type` varchar(150) NOT NULL,
                                                 `file_size` bigint NOT NULL,
                                                 `original_file_name` varchar(255) NOT NULL,
                                                 `stored_file_name` varchar(255) NOT NULL,
                                                 `uploaded_at` datetime(6) NOT NULL,
                                                 `didactic_material_id` bigint NOT NULL,
                                                 PRIMARY KEY (`id`),
                                                 UNIQUE KEY `UK754qieowxuvs8e44wu6j0a1bu` (`stored_file_name`),
                                                 KEY `FKpvj31ho04rjdt65f4s8v0lb8f` (`didactic_material_id`),
                                                 CONSTRAINT `FKpvj31ho04rjdt65f4s8v0lb8f` FOREIGN KEY (`didactic_material_id`) REFERENCES `didactic_materials` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `didactic_material_book_links`
--

DROP TABLE IF EXISTS `didactic_material_book_links`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `didactic_material_book_links` (
                                                `id` bigint NOT NULL AUTO_INCREMENT,
                                                `book_id` bigint NOT NULL,
                                                `didactic_material_id` bigint NOT NULL,
                                                PRIMARY KEY (`id`),
                                                UNIQUE KEY `UK7r857luaul9vv4lqvxfmpp75d` (`didactic_material_id`,`book_id`),
                                                KEY `FKhor1lsy94sd7cfmnt4dfadh04` (`book_id`),
                                                CONSTRAINT `FKhor1lsy94sd7cfmnt4dfadh04` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`),
                                                CONSTRAINT `FKt0hl9bw5x5e4dnbdebcbmkq54` FOREIGN KEY (`didactic_material_id`) REFERENCES `didactic_materials` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `didactic_materials`
--

DROP TABLE IF EXISTS `didactic_materials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `didactic_materials` (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `created_at` datetime(6) NOT NULL,
                                      `created_by_user_id` varchar(100) NOT NULL,
                                      `description` text NOT NULL,
                                      `education_level` varchar(100) DEFAULT NULL,
                                      `keywords` varchar(255) DEFAULT NULL,
                                      `material_type` enum('DIDACTIC_HANDBOOK','EVALUATION_MATERIAL','LESSON_IDEA','LESSON_METHOD','OTHER','PRESENTATION','REFERENCE_WORK','WORKSHEET') NOT NULL,
                                      `subject` varchar(100) DEFAULT NULL,
                                      `title` varchar(150) NOT NULL,
                                      `updated_at` datetime(6) NOT NULL,
                                      PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `genres`
--

DROP TABLE IF EXISTS `genres`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `genres` (
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `color` varchar(255) NOT NULL,
                          `description` varchar(255) DEFAULT NULL,
                          `name` varchar(255) NOT NULL,
                          `parent_genre_id` bigint DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `UKpe1a9woik1k97l87cieguyhh4` (`name`),
                          KEY `FKi7fdm7ys1elteq7u1lgyds7w9` (`parent_genre_id`),
                          CONSTRAINT `FKi7fdm7ys1elteq7u1lgyds7w9` FOREIGN KEY (`parent_genre_id`) REFERENCES `genres` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `loan_copies`
--

DROP TABLE IF EXISTS `loan_copies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loan_copies` (
                               `loan_id` bigint NOT NULL,
                               `book_copy_id` bigint NOT NULL,
                               KEY `FK4abgt48lhcie4d305ov8x0p05` (`book_copy_id`),
                               KEY `FKa0y1doc1q9lsyik4ymjweom77` (`loan_id`),
                               CONSTRAINT `FK4abgt48lhcie4d305ov8x0p05` FOREIGN KEY (`book_copy_id`) REFERENCES `book_copies` (`id`),
                               CONSTRAINT `FKa0y1doc1q9lsyik4ymjweom77` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `loans`
--

DROP TABLE IF EXISTS `loans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loans` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `loan_date` date DEFAULT NULL,
                         `return_date` date DEFAULT NULL,
                         `user_id` varchar(255) DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         KEY `FK6xxlcjc0rqtn5nq28vjnx5t9d` (`user_id`),
                         CONSTRAINT `FK6xxlcjc0rqtn5nq28vjnx5t9d` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reading_levels`
--

DROP TABLE IF EXISTS `reading_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reading_levels` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `description` varchar(255) DEFAULT NULL,
                                  `niveau_name` varchar(255) DEFAULT NULL,
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reading_list_books`
--

DROP TABLE IF EXISTS `reading_list_books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reading_list_books` (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `book_id` bigint NOT NULL,
                                      `reading_list_id` bigint NOT NULL,
                                      PRIMARY KEY (`id`),
                                      KEY `FKjdsu7rnp4sii37klutiwhkh6m` (`reading_list_id`),
                                      CONSTRAINT `FKjdsu7rnp4sii37klutiwhkh6m` FOREIGN KEY (`reading_list_id`) REFERENCES `reading_lists` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reading_lists`
--

DROP TABLE IF EXISTS `reading_lists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reading_lists` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `name` varchar(255) NOT NULL,
                                 `target_type` enum('CLASS','PERSONAL','SCHOOL','SYSTEM','YEAR') NOT NULL,
                                 `target_value` varchar(255) DEFAULT NULL,
                                 `user_id` varchar(255) NOT NULL,
                                 `public_share_id` varchar(64) DEFAULT NULL,
                                 `share_enabled` tinyint(1) NOT NULL DEFAULT '0',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `public_share_id` (`public_share_id`),
                                 KEY `FKa37xwirr81olj9dto5fp30m8m` (`user_id`),
                                 CONSTRAINT `FKa37xwirr81olj9dto5fp30m8m` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `review_reports`
--

DROP TABLE IF EXISTS `review_reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_reports` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `reported_at` datetime(6) NOT NULL,
                                  `reported_by_user_id` varchar(255) NOT NULL,
                                  `review_id` bigint NOT NULL,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `UK85ftwr6qvm153otjdc3vi57bl` (`review_id`,`reported_by_user_id`),
                                  KEY `FKd14roeteog3ca7k4elob84raw` (`reported_by_user_id`),
                                  CONSTRAINT `FKbrdboxyyutcd8bd1igkd5t8cj` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`),
                                  CONSTRAINT `FKd14roeteog3ca7k4elob84raw` FOREIGN KEY (`reported_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
                           `id` bigint NOT NULL AUTO_INCREMENT,
                           `comment` varchar(1000) DEFAULT NULL,
                           `created_at` datetime(6) NOT NULL,
                           `flagged_at` datetime(6) DEFAULT NULL,
                           `moderated_at` datetime(6) DEFAULT NULL,
                           `moderation_status` enum('ACTIVE','FLAGGED','KEPT') NOT NULL,
                           `rating` int NOT NULL,
                           `report_count` int NOT NULL,
                           `book_id` bigint NOT NULL,
                           `user_id` varchar(255) NOT NULL,
                           PRIMARY KEY (`id`),
                           KEY `FK6a9k6xvev80se5rreqvuqr7f9` (`book_id`),
                           KEY `FKcgy7qjc1r99dp117y9en6lxye` (`user_id`),
                           CONSTRAINT `FK6a9k6xvev80se5rreqvuqr7f9` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`),
                           CONSTRAINT `FKcgy7qjc1r99dp117y9en6lxye` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
                         `role` enum('ADMIN','LIBRARIAN','STUDENT','TEACHER') NOT NULL,
                         PRIMARY KEY (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schools`
--

DROP TABLE IF EXISTS `schools`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schools` (
                           `id` bigint NOT NULL AUTO_INCREMENT,
                           `address` varchar(255) NOT NULL,
                           `contact_email` varchar(255) DEFAULT NULL,
                           `contact_phone` varchar(50) DEFAULT NULL,
                           `created_at` datetime(6) NOT NULL,
                           `domain_name` varchar(255) NOT NULL,
                           `name` varchar(255) NOT NULL,
                           `postal_code` int NOT NULL,
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
                         `id` varchar(255) NOT NULL,
                         `review_blocked` bit(1) NOT NULL,
                         `group_name` varchar(255) DEFAULT NULL,
                         `role_id` enum('ADMIN','LIBRARIAN','STUDENT','TEACHER') NOT NULL,
                         `school_id` bigint NOT NULL,
                         PRIMARY KEY (`id`),
                         KEY `FK5jl2qagohmex1auw848v03l3t` (`group_name`),
                         KEY `FKp56c1712k691lhsyewcssf40f` (`role_id`),
                         KEY `FK3gj5j7vnsoxf1wp9n5hsqdiq3` (`school_id`),
                         CONSTRAINT `FK3gj5j7vnsoxf1wp9n5hsqdiq3` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
                         CONSTRAINT `FK5jl2qagohmex1auw848v03l3t` FOREIGN KEY (`group_name`) REFERENCES `classes` (`name`),
                         CONSTRAINT `FKp56c1712k691lhsyewcssf40f` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-29 17:25:10
