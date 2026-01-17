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
-- Table structure for table `time_report_details`
--

DROP TABLE IF EXISTS `time_report_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `time_report_details` (
  `report_id` int NOT NULL,
  `day_index` int NOT NULL,
  `avg_lateness` double DEFAULT NULL,
  `avg_overstay` double DEFAULT NULL,
  PRIMARY KEY (`report_id`,`day_index`),
  CONSTRAINT `time_report_details_ibfk_1` FOREIGN KEY (`report_id`) REFERENCES `reports_management` (`report_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `time_report_details`
--

LOCK TABLES `time_report_details` WRITE;
/*!40000 ALTER TABLE `time_report_details` DISABLE KEYS */;
INSERT INTO `time_report_details` VALUES (1,1,10.45,-6.02),(1,2,18.64,-3.68),(1,3,-9.54,-14.35),(1,4,-7.27,-6.77),(1,5,16.52,4.16),(1,6,3.4,-8.44),(1,7,2.35,-14.68),(1,8,-4.04,15.33),(1,9,-8.71,24.7),(1,10,18.67,-4.81),(1,11,-9.73,5.48),(1,12,10.1,-7.71),(1,13,-2.54,4.8),(1,14,17.56,15.08),(1,15,-4.7,-9.87),(1,16,-8.16,-14.39),(1,17,17.57,7.13),(1,18,-9.24,21.69),(1,19,16.96,-9.79),(1,20,11.71,11.32),(1,21,16.97,17.41),(1,22,0.77,-14.23),(1,23,19.55,0.82),(1,24,-6.47,7.79),(1,25,17.25,14.11),(1,26,-9.76,21.25),(1,27,16.4,-11.55),(1,28,15.42,-1.3),(1,29,15.04,-1.68),(1,30,9.5,13.27),(2,1,7.04,8.5),(2,2,17.61,-8.85),(2,3,1.29,3.81),(2,4,8.26,-8.63),(2,5,-7.72,-7.17),(2,6,11.81,-11.09),(2,7,5.97,-12.83),(2,8,16.55,-12.13),(2,9,-7.72,-12.46),(2,10,-2.63,20.89),(2,11,3.17,18.18),(2,12,9.74,1.35),(2,13,-7.5,20.92),(2,14,19.04,4.7),(2,15,-8.31,-9.66),(2,16,12.85,5.32),(2,17,12.56,21.49),(2,18,-8.56,-14.62),(2,19,-4.01,-8.55),(2,20,8.91,22.03),(2,21,13.11,6.25),(2,22,-4.78,9.34),(2,23,2.6,-5.51),(2,24,-3.34,1.47),(2,25,15.57,-0.91),(2,26,9.9,13.36),(2,27,-3.02,8.56),(2,28,2.27,5.48),(2,29,-0.21,19.28),(2,30,-1.67,-11.15);
/*!40000 ALTER TABLE `time_report_details` ENABLE KEYS */;
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
