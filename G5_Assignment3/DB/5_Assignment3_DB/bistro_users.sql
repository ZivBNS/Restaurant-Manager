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
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `ID` int NOT NULL AUTO_INCREMENT,
  `FirstName` varchar(25) DEFAULT NULL,
  `LastName` varchar(25) DEFAULT NULL,
  `Phone` varchar(14) DEFAULT NULL,
  `Email` varchar(35) DEFAULT NULL,
  `Username` varchar(20) DEFAULT NULL,
  `Password` varchar(20) DEFAULT NULL,
  `subscriberCode` int DEFAULT NULL,
  `Identity` enum('Subscriber','Manager','Employee','Deleted') NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `Username` (`Username`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Oshri','Smith','0501000000','oshri@mail.com','aaaaa','123456',100000,'Subscriber'),(2,'Dor','Johnson','0501000001','dor@mail.com','bbbbb','123456',100001,'Subscriber'),(3,'Daniel','Williams','0501000002','daniel@mail.com','ccccc','123456',100002,'Subscriber'),(4,'Ziv','Brown','0501000003','ziv@mail.com','ddddd','123456',100003,'Subscriber'),(5,'John','Jones','0501000004','john@mail.com','eeeee','123456',100004,'Subscriber'),(6,'Jennifer','Garcia','0501000005','jennifer@mail.com','fffff','123456',100005,'Subscriber'),(7,'Michael','Miller','0501000006','michael@mail.com','ggggg','123456',100006,'Subscriber'),(8,'Linda','Davis','0501000007','linda@mail.com','hhhhh','123456',100007,'Subscriber'),(9,'William','Rodriguez','0501000008','william@mail.com','iiiii','123456',100008,'Subscriber'),(10,'Elizabeth','Martinez','0501000009','elizabeth@mail.com','jjjjj','123456',100009,'Subscriber'),(11,'David','Hernandez','0501000010','david@mail.com','kkkkk','123456',100010,'Subscriber'),(12,'Barbara','Lopez','0501000011','barbara@mail.com','lllll','123456',100011,'Subscriber'),(13,'Richard','Gonzalez','0501000012','richard@mail.com','mmmmm','123456',100012,'Subscriber'),(14,'Susan','Wilson','0501000013','susan@mail.com','nnnnn','123456',100013,'Subscriber'),(15,'Joseph','Anderson','0501000014','joseph@mail.com','ooooo','123456',100014,'Subscriber'),(16,'Jessica','Thomas','0501000015','jessica@mail.com','ppppp','123456',100015,'Subscriber'),(17,'Thomas','Taylor','0501000016','thomas@mail.com','qqqqq','123456',100016,'Subscriber'),(18,'Sarah','Moore','0501000017','sarah@mail.com','rrrrr','123456',100017,'Subscriber'),(19,'Charles','Jackson','0501000018','charles@mail.com','sssss','123456',100018,'Subscriber'),(20,'Karen','Martin','0501000019','karen@mail.com','ttttt','123456',100019,'Subscriber'),(21,'workerName1','lastName1','0502000000','workername1@mail.com','11111','1',100020,'Employee'),(22,'workerName2','lastName2','0502000001','workername2@mail.com','22222','2',100021,'Employee'),(23,'Manager','Boss','0509999999','manager@bistro.com','33333','3',100022,'Manager');
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

-- Dump completed on 2026-01-17 19:35:00
