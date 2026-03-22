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
                               `book_id` bigint DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FKhlawea8y2e2dv0ta58vc6f5nr` (`book_id`),
                               CONSTRAINT `FKhlawea8y2e2dv0ta58vc6f5nr` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book_labels`
--

DROP TABLE IF EXISTS `book_labels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_labels` (
                               `book_id` bigint NOT NULL,
                               `label_name` varchar(255) NOT NULL,
                               KEY `FK3bn8ggi42qwu4fu0tvknbho4k` (`label_name`),
                               KEY `FKepyudwtw2vmxtqobhuvau0192` (`book_id`),
                               CONSTRAINT `FK3bn8ggi42qwu4fu0tvknbho4k` FOREIGN KEY (`label_name`) REFERENCES `labels` (`name`),
                               CONSTRAINT `FKepyudwtw2vmxtqobhuvau0192` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book_loans`
--

DROP TABLE IF EXISTS `book_loans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `labels`
--

DROP TABLE IF EXISTS `labels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `labels` (
                          `name` varchar(255) NOT NULL,
                          `color` varchar(255) NOT NULL,
                          `description` varchar(255) DEFAULT NULL,
                          PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
                           `rating` int NOT NULL,
                           `report_count` int NOT NULL,
                           `book_id` bigint NOT NULL,
                           PRIMARY KEY (`id`),
                           KEY `FK6a9k6xvev80se5rreqvuqr7f9` (`book_id`),
                           CONSTRAINT `FK6a9k6xvev80se5rreqvuqr7f9` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
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
                           `domain_name` varchar(255) NOT NULL,
                           `adres` varchar(255) DEFAULT NULL,
                           `name` varchar(255) DEFAULT NULL,
                           `postal_code` int NOT NULL,
                           PRIMARY KEY (`domain_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
                         `id` varchar(255) NOT NULL,
                         `role_id` enum('ADMIN','LIBRARIAN','STUDENT','TEACHER') NOT NULL,
                         PRIMARY KEY (`id`),
                         KEY `FKp56c1712k691lhsyewcssf40f` (`role_id`),
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

-- Dump completed on 2026-03-22 14:09:56
