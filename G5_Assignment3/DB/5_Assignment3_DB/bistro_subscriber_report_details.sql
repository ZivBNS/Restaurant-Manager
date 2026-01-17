-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: bistro
-- ------------------------------------------------------
-- Server version	8.0.44

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
-- Table structure for table `subscriber_report_details`
--

DROP TABLE IF EXISTS `subscriber_report_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscriber_report_details` (
  `report_id` int NOT NULL,
  `day_index` int NOT NULL,
  `total_orders` int DEFAULT NULL,
  `waiting_list_count` int DEFAULT NULL,
  PRIMARY KEY (`report_id`,`day_index`),
  CONSTRAINT `subscriber_report_details_ibfk_1` FOREIGN KEY (`report_id`) REFERENCES `reports_management` (`report_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subscriber_report_details`
--

LOCK TABLES `subscriber_report_details` WRITE;
/*!40000 ALTER TABLE `subscriber_report_details` DISABLE KEYS */;
INSERT INTO `subscriber_report_details` VALUES (1,1,40,8),(1,2,52,7),(1,3,27,2),(1,4,23,1),(1,5,17,2),(1,6,51,9),(1,7,20,7),(1,8,16,6),(1,9,18,1),(1,10,35,4),(1,11,27,7),(1,12,32,4),(1,13,46,5),(1,14,47,8),(1,15,20,0),(1,16,15,1),(1,17,48,3),(1,18,34,3),(1,19,30,9),(1,20,46,6),(1,21,45,5),(1,22,28,6),(1,23,39,7),(1,24,29,3),(1,25,44,6),(1,26,49,5),(1,27,50,2),(1,28,51,9),(1,29,18,5),(1,30,20,5),(2,1,52,1),(2,2,30,4),(2,3,34,0),(2,4,18,4),(2,5,41,7),(2,6,41,8),(2,7,40,7),(2,8,54,7),(2,9,21,8),(2,10,44,0),(2,11,54,6),(2,12,23,0),(2,13,16,5),(2,14,54,4),(2,15,40,5),(2,16,29,5),(2,17,24,0),(2,18,31,4),(2,19,26,9),(2,20,34,3),(2,21,21,1),(2,22,53,6),(2,23,38,5),(2,24,51,4),(2,25,32,9),(2,26,51,3),(2,27,30,0),(2,28,17,0),(2,29,40,7),(2,30,22,2);
/*!40000 ALTER TABLE `subscriber_report_details` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-17 19:35:00
