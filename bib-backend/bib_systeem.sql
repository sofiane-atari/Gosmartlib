Sofiane
abu_sofiane
Niet storen

PotatoGuy — 3/03/2026 21:20
here
public database
ied kan  dit gebruiken
Sofiane — 3/03/2026 21:44
Morge bespreken want broer kweet niet meer wat hier allemaal gebeurd😂😂
En kdenk we hebben ook slechte redirect url gegeven man
PotatoGuy — 3/03/2026 21:45
idk hoe het op u methode moet doen
drm gwn global
overal beschickbaar
en de production db is gwn apart mnu
Sofiane — 3/03/2026 21:45
Die redirect url verwijst momenteel naar frontend
PotatoGuy — 3/03/2026 21:45
andes moet ik terug gaan zoeken hoe dat methode werkt percies die je mij over hebt verteld
Sofiane — 3/03/2026 21:45
Kdenk die moet beter naar backend gaan
Ja bro calmm
PotatoGuy — 3/03/2026 21:46
ohhh
shit
Sofiane — 3/03/2026 21:48
Juppp drm lichte cookout
PotatoGuy — 3/03/2026 23:32
alles moet responsive zijn?
Sofiane — 3/03/2026 23:33
Ja
Ale moet niet
Maar liever wel
PotatoGuy — 3/03/2026 23:33
ai  gaat front-end devs job nemen tis ni normaal hoe snel die kan shit generen
Sofiane — 3/03/2026 23:34
Jupp
PotatoGuy — gisteren om 2:43
wrm gebruiken wij javaj 17?
ag ik het 21 makjen
maken
PotatoGuy — gisteren om 16:26
https://aphogeschool.smartschool.be/OAuth?client_id=2ebf496d131b&redirect_uri=http://localhost:4200/login/oauth/smartschool&response_type=164db600cdac&scope=userinfo
https://www.smartschool.be/oauth/
Sofiane — gisteren om 17:06
1. Zijn deze Redirect URL's een goede keuze?
Je hebt twee URL's doorgegeven:

https://gosmartlib.be/login/oauth/smartschool (Voor de live productieomgeving)

http://localhost:4200/login/oauth/smartschool (Voor jouw lokale Angular testomgeving)

message.txt
4 KB
Sofiane — 12:48
Kga wss te laat zijn kheb trein gemist
Dasci — 13:59
K ook paar min
Sofiane — 14:27
https://aphogeschool.smartschool.be/OAuth?client_id=2ebf496d131b&redirect_uri=https%3A%2F%2Fgosmartlib.be%2Flogin%2Foauth%2Fsmartschoolresponse_type%3Dcode&language=nl
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDH7SyBvLIS4AFjrTKOeQ6vCw9tVW1D+ZMy2Dpjkk/u+iqRsmTy26VPTOHyB6Tc9gT2Cui5pAridBKg8tA4GGITzEUj8K/YewwhOk2AzEPb9iPgwf+sVv9MGRt3Sk6mCbkzsCJ5IqpgD9B8n6EYxHL+qOACko2n3vLeJCjjtxh7exUYMQ6xMzUhOGapbEJ19O2FZa6H2zxFKWlwhUumQNoIeQR3oZxSb4YYLTiizL0JJhrR5VuPkVsb9P5PO4gYri6bfqNZ+FNC8FiiZHPopZTDCLfvvsImqUqQyKBkO+lxLc6Bzy1ADNJnHXdsXOuByyFEwldnm5q/HHCW4vn2Xz9eE7BaPgtd/qIZdZvxy3nDmyWsQJ2uDpAyD9KNm2PQ9dAiJqbf9Bd91u/EV9+5OusDpkOBfUolAmejYRq5HvSdyki4xDRZngMrdZdcwhIcF7DOd5WC2iUvRUhbyI911F4NbQJ/JLd62HGg5MvxiZKUQBTSDiTkyO3DXFWwOaXZCoc= sofia@LAPTOP-UE9D91A2
Dasci — 14:41
n 
DOMAIN=localhost
DB_USER=testuser
DB_USER_PASS=testpass
DB_ROOT_PASS=teststrong
DB_HOST=localhost
DB_PORT=3307
DB_NAME=bib_systeem
SPRING_PORT=8080
BOOKS_API_KEY=
Dasci — 14:54
-- MySQL dump 10.13  Distrib 8.0.41, for Linux (x86_64)
--
-- Host: localhost    Database: bib_systeem
-- ------------------------------------------------------
-- Server version	8.0.41

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

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `books` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `isbn` bigint DEFAULT NULL,
  `author` varchar(255) DEFAULT NULL,
  `availability` enum('AVAILABLE','BORROWED','DAMAGED','LOST') NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `pages` int NOT NULL,
  `release_year` int NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `copies` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `books`
--

LOCK TABLES `books` WRITE;
/*!40000 ALTER TABLE `books` DISABLE KEYS */;
INSERT INTO `books` VALUES (1,9780132350884,'Robert C. Martin','AVAILABLE','A handbook of agile software craftsmanship.','https://covers.openlibrary.org/b/id/9641656-L.jpg',464,2008,'Clean Code',0),(2,9781617294945,'Craig Walls','BORROWED','Comprehensive guide to Spring Framework.','https://covers.openlibrary.org/b/id/10521270-L.jpg',520,2018,'Spring in Action',0),(3,9781491950357,'Martin Kleppmann','AVAILABLE','The big ideas behind reliable and scalable systems.','https://covers.openlibrary.org/b/id/9259256-L.jpg',616,2017,'Designing Data-Intensive Applications',0),(4,9780134685991,'Joshua Bloch','DAMAGED','Best practices for the Java platform.','https://covers.openlibrary.org/b/id/8228691-L.jpg',416,2018,'Effective Java',0),(5,9780596009205,'Eric Freeman','LOST','A brain-friendly guide to design patterns.','https://covers.openlibrary.org/b/id/240726-L.jpg',694,2004,'Head First Design Patterns',0);
/*!40000 ALTER TABLE `books` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reading_list_books`
--

LOCK TABLES `reading_list_books` WRITE;
/*!40000 ALTER TABLE `reading_list_books` DISABLE KEYS */;
/*!40000 ALTER TABLE `reading_list_books` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reading_lists`
--

DROP TABLE IF EXISTS `reading_lists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reading_lists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKa37xwirr81olj9dto5fp30m8m` (`user_id`),
  CONSTRAINT `FKa37xwirr81olj9dto5fp30m8m` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reading_lists`
--

LOCK TABLES `reading_lists` WRITE;
/*!40000 ALTER TABLE `reading_lists` DISABLE KEYS */;
/*!40000 ALTER TABLE `reading_lists` ENABLE KEYS */;
UNLOCK TABLES;
... (59 regels over)

bib_systeem.sql
7 KB
﻿
-- MySQL dump 10.13  Distrib 8.0.41, for Linux (x86_64)
--
-- Host: localhost    Database: bib_systeem
-- ------------------------------------------------------
-- Server version	8.0.41

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

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `books` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `isbn` bigint DEFAULT NULL,
  `author` varchar(255) DEFAULT NULL,
  `availability` enum('AVAILABLE','BORROWED','DAMAGED','LOST') NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `pages` int NOT NULL,
  `release_year` int NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `copies` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `books`
--

LOCK TABLES `books` WRITE;
/*!40000 ALTER TABLE `books` DISABLE KEYS */;
INSERT INTO `books` VALUES (1,9780132350884,'Robert C. Martin','AVAILABLE','A handbook of agile software craftsmanship.','https://covers.openlibrary.org/b/id/9641656-L.jpg',464,2008,'Clean Code',0),(2,9781617294945,'Craig Walls','BORROWED','Comprehensive guide to Spring Framework.','https://covers.openlibrary.org/b/id/10521270-L.jpg',520,2018,'Spring in Action',0),(3,9781491950357,'Martin Kleppmann','AVAILABLE','The big ideas behind reliable and scalable systems.','https://covers.openlibrary.org/b/id/9259256-L.jpg',616,2017,'Designing Data-Intensive Applications',0),(4,9780134685991,'Joshua Bloch','DAMAGED','Best practices for the Java platform.','https://covers.openlibrary.org/b/id/8228691-L.jpg',416,2018,'Effective Java',0),(5,9780596009205,'Eric Freeman','LOST','A brain-friendly guide to design patterns.','https://covers.openlibrary.org/b/id/240726-L.jpg',694,2004,'Head First Design Patterns',0);
/*!40000 ALTER TABLE `books` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reading_list_books`
--

LOCK TABLES `reading_list_books` WRITE;
/*!40000 ALTER TABLE `reading_list_books` DISABLE KEYS */;
/*!40000 ALTER TABLE `reading_list_books` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reading_lists`
--

DROP TABLE IF EXISTS `reading_lists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reading_lists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKa37xwirr81olj9dto5fp30m8m` (`user_id`),
  CONSTRAINT `FKa37xwirr81olj9dto5fp30m8m` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reading_lists`
--

LOCK TABLES `reading_lists` WRITE;
/*!40000 ALTER TABLE `reading_lists` DISABLE KEYS */;
/*!40000 ALTER TABLE `reading_lists` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_message`
--

DROP TABLE IF EXISTS `test_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `message` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_message`
--

LOCK TABLES `test_message` WRITE;
/*!40000 ALTER TABLE `test_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `test_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `display_name` varchar(255) DEFAULT NULL,
  `smartschool_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-05 13:53:15
bib_systeem.sql
7 KB