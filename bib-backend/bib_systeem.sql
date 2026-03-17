-- MySQL dump 10.13  Distrib 8.0.31, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: bib_systeem
-- ------------------------------------------------------
-- Server version	8.0.31

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
  `id` bigint NOT NULL,
  `book_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhlawea8y2e2dv0ta58vc6f5nr` (`book_id`),
  CONSTRAINT `FKhlawea8y2e2dv0ta58vc6f5nr` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_copies`
--

LOCK TABLES `book_copies` WRITE;
/*!40000 ALTER TABLE `book_copies` DISABLE KEYS */;
INSERT INTO `book_copies` VALUES (1,1),(2,1),(3,1),(4,2),(5,2),(6,3),(7,3),(8,3),(9,3),(10,4),(11,4),(12,4),(13,5),(14,5),(15,6),(16,6),(17,6),(18,7),(19,7),(20,7),(21,7),(22,7),(23,8),(24,8),(25,9),(26,9),(27,9),(28,9),(29,10),(30,10),(31,10),(32,11),(33,11),(34,11),(35,11),(36,12),(37,12),(38,12),(39,13),(40,13),(41,14),(42,14),(43,15),(44,15),(45,15);
/*!40000 ALTER TABLE `book_copies` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `book_labels`
--

LOCK TABLES `book_labels` WRITE;
/*!40000 ALTER TABLE `book_labels` DISABLE KEYS */;
/*!40000 ALTER TABLE `book_labels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_loans`
--

DROP TABLE IF EXISTS `book_loans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_loans` (
  `id` bigint NOT NULL,
  `book_copy_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKoyld3dbrnunaj75c7gy1w5qay` (`book_copy_id`),
  KEY `FKjs7dicejnhi0pm0d4yeo07p39` (`user_id`),
  CONSTRAINT `FKjs7dicejnhi0pm0d4yeo07p39` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKoyld3dbrnunaj75c7gy1w5qay` FOREIGN KEY (`book_copy_id`) REFERENCES `book_copies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_loans`
--

LOCK TABLES `book_loans` WRITE;
/*!40000 ALTER TABLE `book_loans` DISABLE KEYS */;
INSERT INTO `book_loans` VALUES (1,1,1),(2,2,2),(3,3,3),(4,1,4),(5,2,5),(6,3,6),(7,1,7),(8,2,8),(9,4,1),(10,5,2),(11,4,3),(12,5,4),(13,4,5),(14,6,1),(15,7,2),(16,8,3),(17,9,4),(18,6,5),(19,7,6),(20,10,1),(21,11,2),(22,12,3),(23,13,4),(24,14,5),(25,13,6),(26,14,7),(27,15,1),(28,16,2),(29,18,1),(30,19,2),(31,20,3),(32,21,4),(33,22,5),(34,18,6),(35,19,7),(36,23,8),(37,25,1),(38,26,2),(39,27,3),(40,28,4),(41,25,5),(42,29,6),(43,30,7),(44,31,8),(45,32,1),(46,33,2),(47,34,3),(48,35,4),(49,32,5),(50,33,6),(51,36,7),(52,37,8),(53,39,9),(54,41,1),(55,42,2),(56,41,3),(57,42,4),(58,43,5),(59,44,6),(60,45,7);
/*!40000 ALTER TABLE `book_loans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `books` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `isbn` varchar(255) DEFAULT NULL,
  `author` varchar(255) DEFAULT NULL,
  `availability` enum('AVAILABLE','BORROWED','DAMAGED','LOST') NOT NULL,
  `copies` int NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `pages` int NOT NULL,
  `release_year` int DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `added_date` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `books`
--

LOCK TABLES `books` WRITE;
/*!40000 ALTER TABLE `books` DISABLE KEYS */;
INSERT INTO `books` VALUES (1,'978-0-06-112008-4','Harper Lee','AVAILABLE',3,'A classic novel about justice','https://picsum.photos/200',281,1960,'To Kill a Mockingbird',NULL),(2,'978-0-7432-7356-5','F. Scott Fitzgerald','AVAILABLE',2,'A story of the Jazz Age','https://picsum.photos/201',180,1925,'The Great Gatsby',NULL),(3,'978-0-14-028329-7','George Orwell','AVAILABLE',4,'A dystopian novel','https://picsum.photos/202',328,1949,'1984',NULL),(4,'978-0-14-303943-3','Aldous Huxley','AVAILABLE',3,'A futuristic dystopia','https://picsum.photos/203',311,1932,'Brave New World',NULL),(5,'978-0-7432-7357-2','J.D. Salinger','AVAILABLE',2,'A story of teenage rebellion','https://picsum.photos/204',277,1951,'The Catcher in the Rye',NULL),(6,'978-0-06-093546-9','Alexandra Christo','AVAILABLE',3,'A dark fantasy romance','https://picsum.photos/205',352,2018,'To Kill a Kingdom',NULL),(7,'978-0-14-028330-3','George Orwell','AVAILABLE',5,'A political allegory','https://picsum.photos/206',112,1945,'Animal Farm',NULL),(8,'978-0-7432-7358-9','John Steinbeck','AVAILABLE',2,'A story of friendship','https://picsum.photos/207',187,1937,'Of Mice and Men',NULL),(9,'978-0-06-112009-1','Paulo Coelho','AVAILABLE',4,'A journey of self-discovery','https://picsum.photos/208',197,1988,'The Alchemist',NULL),(10,'978-0-14-028331-0','William Golding','AVAILABLE',3,'Survival on a deserted island','https://picsum.photos/209',224,1954,'Lord of the Flies',NULL),(11,'978-0-06-112010-7','J.R.R. Tolkien','AVAILABLE',4,'A fantasy adventure','https://picsum.photos/210',310,1937,'The Hobbit',NULL),(12,'978-0-7432-7359-6','Ray Bradbury','AVAILABLE',3,'A world where books are banned','https://picsum.photos/211',158,1953,'Fahrenheit 451',NULL),(13,'978-0-14-028332-7','Homer','AVAILABLE',2,'An ancient Greek epic','https://picsum.photos/212',541,-800,'The Odyssey',NULL),(14,'978-0-06-112011-4','Fyodor Dostoevsky','AVAILABLE',2,'A psychological thriller','https://picsum.photos/213',671,1866,'Crime and Punishment',NULL),(15,'978-0-7432-7360-2','Albert Camus','AVAILABLE',3,'An existentialist novel','https://picsum.photos/214',123,1942,'The Stranger',NULL),(26,NULL,'dadas','DAMAGED',1213,'asdasdas','dadasdasdas',12312312,2026,'dasda','2026-03-11 10:41:30.208678');
/*!40000 ALTER TABLE `books` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `labels`
--

LOCK TABLES `labels` WRITE;
/*!40000 ALTER TABLE `labels` DISABLE KEYS */;
INSERT INTO `labels` VALUES ('Biography','#8B4513','descriptionsdasdas'),('Children','#FFB6C1','Books for young readers'),('Classics','#C0C0C0','Timeless literary classics'),('clear','#ffffff','potato'),('Comics','#FFD700','Graphic novels and comics'),('Cooking','#FF8C00','Recipes and culinary arts'),('Crime','#8B4513','Crime stories and investigations'),('Drama','#9370DB','Dramatic fictional narratives'),('Dystopia','#696969','Dark futuristic and oppressive societies'),('Economics','#4682B4','Financial systems and economic theory'),('Education','#87CEEB','Learning, teaching and academic topics'),('Environment','#2E8B57','Climate, nature and environmental issues'),('Fantasy','#FF33F5','Fantasy worlds and magic'),('Fiction','#FF5733','Fictional stories and novels'),('Health','#98FB98','Medicine, wellness and health guides'),('History','#3357FF','Historical events and biographies'),('Horror','#800000','Dark and frightening stories'),('horrorr','#bd5656',''),('Humor','#FFD700','Funny and comedic writing'),('Journalism','#D2691E','Investigative journalism and reporting'),('Legal','#A0522D','Legal dramas and courtroom stories'),('Music','#FF1493','Music history and musician biographies'),('Mystery','#8B0000','Mysterious plots and detective stories'),('Mythology','#9400D3','Ancient myths and legendary tales'),('Nature','#6B8E23','Wildlife, ecology and natural world'),('Parenting','#FFB6C1','Guides and stories about raising children'),('Philosophy','#4B0082','Philosophical thoughts and theories'),('Poetry','#DDA0DD','Poems and poetic collections'),('Politics','#B22222','Political systems and governance'),('Psychology','#008080','Human mind and behavioral studies'),('Romance','#FF69B4','Love stories and romantic novels'),('Sci-Fi','#00BFFF','Science fiction and futuristic worlds'),('Science','#33FF57','Scientific books and research'),('Self-Help','#FFD700','Personal development and growth'),('Short Stories','#F4A460','Collections of short fiction'),('Sociology','#BC8F8F','Human society and social behavior'),('Spirituality','#E0E0E0','Spiritual journeys and religious texts'),('Sports','#00CED1','Sports stories and athlete biographies'),('Technology','#1E90FF','Modern tech and innovation'),('TEst','#dd6e6e','dadsad'),('test123123123','#b60c0c','dsadas'),('Thriller','#2F4F4F','Suspenseful and thrilling narratives'),('Travel','#32CD32','Travel stories and guides'),('War','#556B2F','War stories and military history'),('zoutside','#933e3e','label'),('ZZZZZZ','#d95959','');
/*!40000 ALTER TABLE `labels` ENABLE KEYS */;
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reading_lists`
--

LOCK TABLES `reading_lists` WRITE;
/*!40000 ALTER TABLE `reading_lists` DISABLE KEYS */;
/*!40000 ALTER TABLE `reading_lists` ENABLE KEYS */;
UNLOCK TABLES;

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
  `book_id` bigint NOT NULL,
  `report_count` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6a9k6xvev80se5rreqvuqr7f9` (`book_id`),
  CONSTRAINT `FK6a9k6xvev80se5rreqvuqr7f9` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10);
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

-- Dump completed on 2026-03-11 11:02:27
