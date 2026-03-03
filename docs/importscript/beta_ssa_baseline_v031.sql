-- MySQL dump 10.13  Distrib 8.0.30, for Win64 (x86_64)
--
-- Host: localhost    Database: dev_ssa
-- ------------------------------------------------------
-- Server version	8.0.30

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
-- Temporary view structure for view `a25_activity_list_op`
--

DROP TABLE IF EXISTS `a25_activity_list_op`;
/*!50001 DROP VIEW IF EXISTS `a25_activity_list_op`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `a25_activity_list_op` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `assigned_to_id`,
 1 AS `due_date`,
 1 AS `max_note_id`,
 1 AS `max_date_done`,
 1 AS `days_since`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `a25_activity_list_open`
--

DROP TABLE IF EXISTS `a25_activity_list_open`;
/*!50001 DROP VIEW IF EXISTS `a25_activity_list_open`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `a25_activity_list_open` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `assigned_to_id`,
 1 AS `due_date`,
 1 AS `max_note_id`,
 1 AS `max_date_done`,
 1 AS `days_since`,
 1 AS `on_us`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `a25_activity_list_participating`
--

DROP TABLE IF EXISTS `a25_activity_list_participating`;
/*!50001 DROP VIEW IF EXISTS `a25_activity_list_participating`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `a25_activity_list_participating` AS SELECT 
 1 AS `id`,
 1 AS `checklist_id`,
 1 AS `full_name`,
 1 AS `act_owner_id`,
 1 AS `todo_id`,
 1 AS `DESCRIPTION`,
 1 AS `tsk_owner_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `a25_checklist_full`
--

DROP TABLE IF EXISTS `a25_checklist_full`;
/*!50001 DROP VIEW IF EXISTS `a25_checklist_full`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `a25_checklist_full` AS SELECT 
 1 AS `id`,
 1 AS `full_name`,
 1 AS `today`,
 1 AS `due_date`,
 1 AS `assigned_to_id`,
 1 AS `is_complete`,
 1 AS `single_task`,
 1 AS `days_ahead`,
 1 AS `sort_key`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `a_base_01`
--

DROP TABLE IF EXISTS `a_base_01`;
/*!50001 DROP VIEW IF EXISTS `a_base_01`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `a_base_01` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `TAXID`,
 1 AS `address_id`,
 1 AS `contact_id`,
 1 AS `email`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `middle_init`,
 1 AS `phone`,
 1 AS `title`,
 1 AS `psp_id`,
 1 AS `setup_id`,
 1 AS `employee_id`,
 1 AS `date_completed`,
 1 AS `date_created`,
 1 AS `due_date`,
 1 AS `is_complete`,
 1 AS `assigned_to_id`,
 1 AS `completed_by_id`,
 1 AS `created_by_id`,
 1 AS `employer_id`,
 1 AS `checklist_id`,
 1 AS `proposal_id`,
 1 AS `person_id`,
 1 AS `description`,
 1 AS `method_id`,
 1 AS `recurring_list_id`,
 1 AS `email_address`,
 1 AS `myRsc`,
 1 AS `primary_contact`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `a_base_02`
--

DROP TABLE IF EXISTS `a_base_02`;
/*!50001 DROP VIEW IF EXISTS `a_base_02`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `a_base_02` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `TAXID`,
 1 AS `address_id`,
 1 AS `contact_id`,
 1 AS `email`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `middle_init`,
 1 AS `phone`,
 1 AS `title`,
 1 AS `psp_id`,
 1 AS `setup_id`,
 1 AS `employee_id`,
 1 AS `date_completed`,
 1 AS `date_created`,
 1 AS `due_date`,
 1 AS `is_complete`,
 1 AS `assigned_to_id`,
 1 AS `completed_by_id`,
 1 AS `created_by_id`,
 1 AS `employer_id`,
 1 AS `checklist_id`,
 1 AS `proposal_id`,
 1 AS `person_id`,
 1 AS `description`,
 1 AS `method_id`,
 1 AS `recurring_list_id`,
 1 AS `email_address`,
 1 AS `myRsc`,
 1 AS `primary_contact`,
 1 AS `last_contact`,
 1 AS `contact_status`,
 1 AS `needs_contact`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `a_base_03`
--

DROP TABLE IF EXISTS `a_base_03`;
/*!50001 DROP VIEW IF EXISTS `a_base_03`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `a_base_03` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `TAXID`,
 1 AS `address_id`,
 1 AS `contact_id`,
 1 AS `email`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `middle_init`,
 1 AS `phone`,
 1 AS `title`,
 1 AS `psp_id`,
 1 AS `setup_id`,
 1 AS `employee_id`,
 1 AS `date_completed`,
 1 AS `date_created`,
 1 AS `due_date`,
 1 AS `is_complete`,
 1 AS `assigned_to_id`,
 1 AS `completed_by_id`,
 1 AS `created_by_id`,
 1 AS `employer_id`,
 1 AS `checklist_id`,
 1 AS `proposal_id`,
 1 AS `person_id`,
 1 AS `description`,
 1 AS `method_id`,
 1 AS `recurring_list_id`,
 1 AS `email_address`,
 1 AS `myRsc`,
 1 AS `primary_contact`,
 1 AS `last_contact`,
 1 AS `contact_status`,
 1 AS `needs_contact`,
 1 AS `c_status_id`,
 1 AS `waiting_on_us`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `a_base_04`
--

DROP TABLE IF EXISTS `a_base_04`;
/*!50001 DROP VIEW IF EXISTS `a_base_04`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `a_base_04` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `TAXID`,
 1 AS `address_id`,
 1 AS `contact_id`,
 1 AS `email`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `middle_init`,
 1 AS `phone`,
 1 AS `title`,
 1 AS `psp_id`,
 1 AS `setup_id`,
 1 AS `employee_id`,
 1 AS `date_completed`,
 1 AS `date_created`,
 1 AS `due_date`,
 1 AS `is_complete`,
 1 AS `assigned_to_id`,
 1 AS `completed_by_id`,
 1 AS `created_by_id`,
 1 AS `employer_id`,
 1 AS `checklist_id`,
 1 AS `proposal_id`,
 1 AS `person_id`,
 1 AS `description`,
 1 AS `method_id`,
 1 AS `recurring_list_id`,
 1 AS `email_address`,
 1 AS `myRsc`,
 1 AS `primary_contact`,
 1 AS `last_contact`,
 1 AS `contact_status`,
 1 AS `needs_contact`,
 1 AS `c_status_id`,
 1 AS `waiting_on_us`,
 1 AS `task_owner_id`,
 1 AS `bpo_registration_id`,
 1 AS `UID`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `a_base_05`
--

DROP TABLE IF EXISTS `a_base_05`;
/*!50001 DROP VIEW IF EXISTS `a_base_05`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `a_base_05` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `TAXID`,
 1 AS `address_id`,
 1 AS `contact_id`,
 1 AS `email`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `middle_init`,
 1 AS `phone`,
 1 AS `title`,
 1 AS `psp_id`,
 1 AS `setup_id`,
 1 AS `employee_id`,
 1 AS `date_completed`,
 1 AS `date_created`,
 1 AS `due_date`,
 1 AS `is_complete`,
 1 AS `assigned_to_id`,
 1 AS `completed_by_id`,
 1 AS `created_by_id`,
 1 AS `employer_id`,
 1 AS `checklist_id`,
 1 AS `proposal_id`,
 1 AS `person_id`,
 1 AS `description`,
 1 AS `method_id`,
 1 AS `recurring_list_id`,
 1 AS `email_address`,
 1 AS `myRsc`,
 1 AS `primary_contact`,
 1 AS `last_contact`,
 1 AS `contact_status`,
 1 AS `needs_contact`,
 1 AS `c_status_id`,
 1 AS `waiting_on_us`,
 1 AS `task_owner_id`,
 1 AS `bpo_registration_id`,
 1 AS `UID`,
 1 AS `full_name_alt`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_01`
--

DROP TABLE IF EXISTS `ac_base_01`;
/*!50001 DROP VIEW IF EXISTS `ac_base_01`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_01` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `employee_id`,
 1 AS `due_date`,
 1 AS `is_complete`,
 1 AS `assigned_to_id`,
 1 AS `employer_id`,
 1 AS `checklist_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_02`
--

DROP TABLE IF EXISTS `ac_base_02`;
/*!50001 DROP VIEW IF EXISTS `ac_base_02`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_02` AS SELECT 
 1 AS `note_id`,
 1 AS `DTYPE`,
 1 AS `date_created`,
 1 AS `date_generated`,
 1 AS `DETAIL`,
 1 AS `activity_id`,
 1 AS `created_by_id`,
 1 AS `reason_id`,
 1 AS `status_id`,
 1 AS `subject`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_03`
--

DROP TABLE IF EXISTS `ac_base_03`;
/*!50001 DROP VIEW IF EXISTS `ac_base_03`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_03` AS SELECT 
 1 AS `activity_id`,
 1 AS `max_note_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_04`
--

DROP TABLE IF EXISTS `ac_base_04`;
/*!50001 DROP VIEW IF EXISTS `ac_base_04`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_04` AS SELECT 
 1 AS `activity_id`,
 1 AS `status_id`,
 1 AS `max_note_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_05`
--

DROP TABLE IF EXISTS `ac_base_05`;
/*!50001 DROP VIEW IF EXISTS `ac_base_05`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_05` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `employee_id`,
 1 AS `due_date`,
 1 AS `is_complete`,
 1 AS `assigned_to_id`,
 1 AS `employer_id`,
 1 AS `status_id`,
 1 AS `checklist_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_06`
--

DROP TABLE IF EXISTS `ac_base_06`;
/*!50001 DROP VIEW IF EXISTS `ac_base_06`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_06` AS SELECT 
 1 AS `note_id`,
 1 AS `DTYPE`,
 1 AS `date_created`,
 1 AS `date_generated`,
 1 AS `DETAIL`,
 1 AS `activity_id`,
 1 AS `created_by_id`,
 1 AS `reason_id`,
 1 AS `status_id`,
 1 AS `subject`,
 1 AS `outbound`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_07`
--

DROP TABLE IF EXISTS `ac_base_07`;
/*!50001 DROP VIEW IF EXISTS `ac_base_07`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_07` AS SELECT 
 1 AS `activity_id`,
 1 AS `max_note_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_08`
--

DROP TABLE IF EXISTS `ac_base_08`;
/*!50001 DROP VIEW IF EXISTS `ac_base_08`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_08` AS SELECT 
 1 AS `activity_id`,
 1 AS `date_created`,
 1 AS `max_note_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_09`
--

DROP TABLE IF EXISTS `ac_base_09`;
/*!50001 DROP VIEW IF EXISTS `ac_base_09`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_09` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `employee_id`,
 1 AS `due_date`,
 1 AS `is_complete`,
 1 AS `assigned_to_id`,
 1 AS `employer_id`,
 1 AS `last_status_id`,
 1 AS `last_outbound`,
 1 AS `checklist_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_10`
--

DROP TABLE IF EXISTS `ac_base_10`;
/*!50001 DROP VIEW IF EXISTS `ac_base_10`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_10` AS SELECT 
 1 AS `todo_id`,
 1 AS `task_id`,
 1 AS `name`,
 1 AS `not_me`,
 1 AS `has_owner`,
 1 AS `is_sourced`,
 1 AS `owner_id`,
 1 AS `allow_non_owner`,
 1 AS `allow_early`,
 1 AS `allow_future`,
 1 AS `sort_order`,
 1 AS `min_sort_top`,
 1 AS `at_top`,
 1 AS `min_sort_block`,
 1 AS `is_blocked`,
 1 AS `checklist_id`,
 1 AS `checklist_name`,
 1 AS `checklist_owner`,
 1 AS `is_checklist_complete`,
 1 AS `checklist_due_date`,
 1 AS `activity_id`,
 1 AS `DTYPE`,
 1 AS `is_activity`,
 1 AS `is_activity_complete`,
 1 AS `activity_owner_id`,
 1 AS `activity_due_date`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `auto_id`,
 1 AS `has_automation`,
 1 AS `bpo_registration_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ac_base_11`
--

DROP TABLE IF EXISTS `ac_base_11`;
/*!50001 DROP VIEW IF EXISTS `ac_base_11`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ac_base_11` AS SELECT 
 1 AS `counts`,
 1 AS `owner_id`,
 1 AS `bpo_registration_id`,
 1 AS `activity_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `activitystatus`
--

DROP TABLE IF EXISTS `activitystatus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activitystatus` (
  `status_id` int NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`status_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `acx_open_activities`
--

DROP TABLE IF EXISTS `acx_open_activities`;
/*!50001 DROP VIEW IF EXISTS `acx_open_activities`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `acx_open_activities` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `employee_id`,
 1 AS `due_date`,
 1 AS `is_complete`,
 1 AS `assigned_to_id`,
 1 AS `employer_id`,
 1 AS `last_status_id`,
 1 AS `last_outbound`,
 1 AS `checklist_id`,
 1 AS `task_owner_id`,
 1 AS `bpo_registration_id`,
 1 AS `UID`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `address`
--

DROP TABLE IF EXISTS `address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `address` (
  `address_id` bigint NOT NULL,
  `address1` varchar(100) DEFAULT NULL,
  `address2` varchar(100) DEFAULT NULL,
  `city` varchar(50) DEFAULT NULL,
  `date_created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `state` varchar(2) DEFAULT NULL,
  `zip_code` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`address_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agency`
--

DROP TABLE IF EXISTS `agency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agency` (
  `agency_id` bigint NOT NULL,
  `agency_name` varchar(200) DEFAULT NULL,
  `phone` varchar(12) DEFAULT NULL,
  `tax_id` varchar(10) DEFAULT NULL,
  `psp_id` bigint DEFAULT NULL,
  `address_id` bigint DEFAULT NULL,
  `contact_id` bigint DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  PRIMARY KEY (`agency_id`),
  KEY `FK_AGENCY_address_id` (`address_id`),
  KEY `FK_AGENCY_contact_id` (`contact_id`),
  KEY `FK_AGENCY_psp_id` (`psp_id`),
  KEY `fk_agency_manager` (`manager_id`),
  CONSTRAINT `FK_AGENCY_address_id` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`),
  CONSTRAINT `FK_AGENCY_contact_id` FOREIGN KEY (`contact_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `fk_agency_manager` FOREIGN KEY (`manager_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_AGENCY_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agencyrates`
--

DROP TABLE IF EXISTS `agencyrates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agencyrates` (
  `agency_id` bigint NOT NULL,
  `rate_id` bigint NOT NULL,
  PRIMARY KEY (`agency_id`,`rate_id`),
  KEY `FK_agencyrates_rate_id` (`rate_id`),
  CONSTRAINT `FK_agencyrates_agency_id` FOREIGN KEY (`agency_id`) REFERENCES `agency` (`agency_id`),
  CONSTRAINT `FK_agencyrates_rate_id` FOREIGN KEY (`rate_id`) REFERENCES `rate` (`rate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agents`
--

DROP TABLE IF EXISTS `agents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agents` (
  `person_id` bigint NOT NULL,
  `agency_id` bigint NOT NULL,
  PRIMARY KEY (`person_id`,`agency_id`),
  KEY `FK_agents_agency_id` (`agency_id`),
  CONSTRAINT `FK_agents_agency_id` FOREIGN KEY (`agency_id`) REFERENCES `agency` (`agency_id`),
  CONSTRAINT `FK_agents_person_id` FOREIGN KEY (`person_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `application`
--

DROP TABLE IF EXISTS `application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `application` (
  `proposal_id` bigint NOT NULL,
  `status` varchar(20) DEFAULT 'IN_PROGRESS',
  `date_started` timestamp NULL DEFAULT NULL,
  `date_submitted` timestamp NULL DEFAULT NULL,
  `date_reviewed` timestamp NULL DEFAULT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `review_notes` text,
  PRIMARY KEY (`proposal_id`),
  CONSTRAINT `FK_APPLICATION_proposal_id` FOREIGN KEY (`proposal_id`) REFERENCES `proposal` (`proposal_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `applicationdata`
--

DROP TABLE IF EXISTS `applicationdata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `applicationdata` (
  `application_id` bigint NOT NULL,
  `data_pair_id` bigint NOT NULL,
  PRIMARY KEY (`application_id`,`data_pair_id`),
  KEY `FK_APPLICATIONDATA_data_pair_id` (`data_pair_id`),
  CONSTRAINT `FK_APPLICATIONDATA_application_id` FOREIGN KEY (`application_id`) REFERENCES `application` (`proposal_id`),
  CONSTRAINT `FK_APPLICATIONDATA_data_pair_id` FOREIGN KEY (`data_pair_id`) REFERENCES `datapair` (`data_pair_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `applicationfield`
--

DROP TABLE IF EXISTS `applicationfield`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `applicationfield` (
  `field_key` varchar(100) NOT NULL,
  `label` varchar(200) DEFAULT NULL,
  `template_purpose_id` int DEFAULT NULL,
  `field_type` varchar(20) DEFAULT 'TEXT',
  `is_required` tinyint DEFAULT '0',
  `sort_order` int DEFAULT '0',
  `select_options` varchar(500) DEFAULT NULL,
  `suppressed` tinyint(1) NOT NULL DEFAULT '0',
  `section_id` bigint DEFAULT NULL,
  `help_text` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`field_key`),
  KEY `FK_DATAKEY_template_purpose_id` (`template_purpose_id`),
  KEY `fk_appfield_section` (`section_id`),
  CONSTRAINT `fk_appfield_section` FOREIGN KEY (`section_id`) REFERENCES `applicationsection` (`section_id`),
  CONSTRAINT `FK_DATAKEY_template_purpose_id` FOREIGN KEY (`template_purpose_id`) REFERENCES `templatepurpose` (`purpose_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `applicationfieldvalue`
--

DROP TABLE IF EXISTS `applicationfieldvalue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `applicationfieldvalue` (
  `field_value_id` bigint NOT NULL AUTO_INCREMENT,
  `application_id` bigint NOT NULL,
  `field_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `field_value` text,
  PRIMARY KEY (`field_value_id`),
  KEY `application_id` (`application_id`),
  KEY `field_key` (`field_key`),
  CONSTRAINT `applicationfieldvalue_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `application` (`proposal_id`),
  CONSTRAINT `applicationfieldvalue_ibfk_2` FOREIGN KEY (`field_key`) REFERENCES `applicationfield` (`field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `applicationmodule`
--

DROP TABLE IF EXISTS `applicationmodule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `applicationmodule` (
  `application_id` bigint NOT NULL,
  `template_purpose_id` int NOT NULL,
  PRIMARY KEY (`application_id`,`template_purpose_id`),
  KEY `FK_APPLICATIONMODULE_template_purpose_id` (`template_purpose_id`),
  CONSTRAINT `FK_APPLICATIONMODULE_application_id` FOREIGN KEY (`application_id`) REFERENCES `application` (`proposal_id`),
  CONSTRAINT `FK_APPLICATIONMODULE_template_purpose_id` FOREIGN KEY (`template_purpose_id`) REFERENCES `templatepurpose` (`purpose_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `applicationsection`
--

DROP TABLE IF EXISTS `applicationsection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `applicationsection` (
  `section_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `scope` varchar(10) NOT NULL DEFAULT 'ALL',
  `sort_order` int NOT NULL DEFAULT '0',
  `suppressed` tinyint(1) NOT NULL DEFAULT '0',
  `psp_id` bigint NOT NULL,
  PRIMARY KEY (`section_id`),
  KEY `psp_id` (`psp_id`),
  CONSTRAINT `applicationsection_ibfk_1` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `applicationsectionenhancement`
--

DROP TABLE IF EXISTS `applicationsectionenhancement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `applicationsectionenhancement` (
  `section_id` bigint NOT NULL,
  `enhancement_id` bigint NOT NULL,
  PRIMARY KEY (`section_id`,`enhancement_id`),
  KEY `enhancement_id` (`enhancement_id`),
  CONSTRAINT `applicationsectionenhancement_ibfk_1` FOREIGN KEY (`section_id`) REFERENCES `applicationsection` (`section_id`),
  CONSTRAINT `applicationsectionenhancement_ibfk_2` FOREIGN KEY (`enhancement_id`) REFERENCES `enhancement` (`enhancement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `applicationsectionlos`
--

DROP TABLE IF EXISTS `applicationsectionlos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `applicationsectionlos` (
  `section_id` bigint NOT NULL,
  `los_id` bigint NOT NULL,
  PRIMARY KEY (`section_id`,`los_id`),
  KEY `los_id` (`los_id`),
  CONSTRAINT `applicationsectionlos_ibfk_1` FOREIGN KEY (`section_id`) REFERENCES `applicationsection` (`section_id`),
  CONSTRAINT `applicationsectionlos_ibfk_2` FOREIGN KEY (`los_id`) REFERENCES `los` (`los_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignee`
--

DROP TABLE IF EXISTS `assignee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignee` (
  `id` bigint NOT NULL,
  `DTYPE` varchar(31) DEFAULT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `TAXID` varchar(10) DEFAULT NULL,
  `address_id` bigint DEFAULT NULL,
  `contact_id` bigint DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `first_name` varchar(50) DEFAULT NULL,
  `last_name` varchar(80) DEFAULT NULL,
  `middle_init` char(1) DEFAULT NULL,
  `phone` varchar(12) DEFAULT NULL,
  `title` varchar(50) DEFAULT NULL,
  `psp_id` bigint DEFAULT NULL,
  `setup_id` bigint DEFAULT NULL,
  `employee_id` int DEFAULT NULL,
  `date_completed` date DEFAULT NULL,
  `date_created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `due_date` date DEFAULT NULL,
  `is_complete` tinyint(1) DEFAULT '0',
  `assigned_to_id` bigint DEFAULT NULL,
  `completed_by_id` bigint DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `employer_id` int DEFAULT NULL,
  `checklist_id` bigint DEFAULT NULL,
  `proposal_id` bigint DEFAULT NULL,
  `person_id` bigint DEFAULT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `method_id` int DEFAULT NULL,
  `recurring_list_id` bigint DEFAULT NULL,
  `email_address` varchar(255) DEFAULT NULL,
  `myRsc` varchar(255) DEFAULT NULL,
  `primary_contact` bigint DEFAULT NULL,
  `prospect_id` bigint DEFAULT NULL,
  `agency_id_opp` bigint DEFAULT NULL,
  `opportunity_stage` varchar(30) DEFAULT NULL,
  `estimated_employees` int DEFAULT NULL,
  `estimated_value` double DEFAULT NULL,
  `expected_close_date` date DEFAULT NULL,
  `managed_by_id` bigint DEFAULT NULL,
  `ticket_service_item_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_ASSIGNEE_proposal_id` (`proposal_id`),
  KEY `FK_ASSIGNEE_completed_by_id` (`completed_by_id`),
  KEY `FK_ASSIGNEE_address_id` (`address_id`),
  KEY `FK_ASSIGNEE_created_by_id` (`created_by_id`),
  KEY `FK_ASSIGNEE_contact_id` (`contact_id`),
  KEY `FK_ASSIGNEE_employer_id` (`employer_id`),
  KEY `FK_ASSIGNEE_person_id` (`person_id`),
  KEY `FK_ASSIGNEE_setup_id` (`setup_id`),
  KEY `FK_ASSIGNEE_psp_id` (`psp_id`),
  KEY `FK_ASSIGNEE_method_id` (`method_id`),
  KEY `FK_ASSIGNEE_recurring_list_id` (`recurring_list_id`),
  KEY `FK_ASSIGNEE_employee_id` (`employee_id`),
  KEY `FK_ASSIGNEE_assigned_to_id` (`assigned_to_id`),
  KEY `FK_ASSIGNEE_checklist_id` (`checklist_id`),
  KEY `fk_opp_prospect` (`prospect_id`),
  KEY `fk_opp_agency` (`agency_id_opp`),
  KEY `fk_opp_managed_by` (`managed_by_id`),
  KEY `FK_ASSIGNEE_ticket_service_item_id` (`ticket_service_item_id`),
  CONSTRAINT `FK_ASSIGNEE_address_id` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`),
  CONSTRAINT `FK_ASSIGNEE_assigned_to_id` FOREIGN KEY (`assigned_to_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_ASSIGNEE_checklist_id` FOREIGN KEY (`checklist_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_ASSIGNEE_completed_by_id` FOREIGN KEY (`completed_by_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_ASSIGNEE_contact_id` FOREIGN KEY (`contact_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_ASSIGNEE_created_by_id` FOREIGN KEY (`created_by_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_ASSIGNEE_employee_id` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`employee_id`),
  CONSTRAINT `FK_ASSIGNEE_employer_id` FOREIGN KEY (`employer_id`) REFERENCES `employer` (`organization_id`),
  CONSTRAINT `FK_ASSIGNEE_method_id` FOREIGN KEY (`method_id`) REFERENCES `contactmethod` (`method_id`),
  CONSTRAINT `FK_ASSIGNEE_person_id` FOREIGN KEY (`person_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_ASSIGNEE_proposal_id` FOREIGN KEY (`proposal_id`) REFERENCES `application` (`proposal_id`),
  CONSTRAINT `FK_ASSIGNEE_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_ASSIGNEE_recurring_list_id` FOREIGN KEY (`recurring_list_id`) REFERENCES `tasksequence` (`sequence_id`),
  CONSTRAINT `FK_ASSIGNEE_setup_id` FOREIGN KEY (`setup_id`) REFERENCES `assignee` (`proposal_id`),
  CONSTRAINT `FK_ASSIGNEE_ticket_service_item_id` FOREIGN KEY (`ticket_service_item_id`) REFERENCES `templatepurpose` (`purpose_id`),
  CONSTRAINT `fk_opp_agency` FOREIGN KEY (`agency_id_opp`) REFERENCES `agency` (`agency_id`),
  CONSTRAINT `fk_opp_managed_by` FOREIGN KEY (`managed_by_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `fk_opp_prospect` FOREIGN KEY (`prospect_id`) REFERENCES `prospect` (`prospect_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignee_contacts`
--

DROP TABLE IF EXISTS `assignee_contacts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignee_contacts` (
  `assignee_id` bigint NOT NULL,
  `person_id` bigint NOT NULL,
  PRIMARY KEY (`assignee_id`,`person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignee_links`
--

DROP TABLE IF EXISTS `assignee_links`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignee_links` (
  `assignee_id` bigint NOT NULL,
  `link_id` bigint NOT NULL,
  PRIMARY KEY (`assignee_id`,`link_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `automation`
--

DROP TABLE IF EXISTS `automation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `automation` (
  `auto_id` int NOT NULL,
  `name` varchar(45) DEFAULT NULL,
  `input_count` int DEFAULT NULL,
  `input_names` varchar(500) DEFAULT NULL,
  `is_link` varchar(45) DEFAULT NULL,
  `content` varchar(10000) DEFAULT NULL,
  PRIMARY KEY (`auto_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `benefit`
--

DROP TABLE IF EXISTS `benefit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `benefit` (
  `benefit_id` int NOT NULL AUTO_INCREMENT,
  `summit_id` int NOT NULL,
  `source_type` varchar(10) NOT NULL DEFAULT 'CDH',
  `effective_date` date DEFAULT NULL,
  `hasCards` tinyint(1) DEFAULT '0',
  `active` tinyint(1) DEFAULT '0',
  `last_renewed` date DEFAULT NULL,
  `next_renewal_due` date DEFAULT NULL,
  `plan_description` varchar(255) DEFAULT NULL,
  `plan_name` varchar(255) DEFAULT NULL,
  `termination_date` date DEFAULT NULL,
  `employer_id` int DEFAULT NULL,
  `plan_type_id` int DEFAULT NULL,
  `benid_pb` int DEFAULT '0',
  `renewal_months` int NOT NULL DEFAULT '12',
  `plan_year_start` date DEFAULT NULL,
  `plan_year_end` date DEFAULT NULL,
  PRIMARY KEY (`benefit_id`),
  UNIQUE KEY `uq_benefit_source_summit` (`source_type`,`summit_id`),
  KEY `FK_BENEFIT_employer_id` (`employer_id`),
  KEY `FK_BENEFIT_plan_type_id` (`plan_type_id`),
  CONSTRAINT `FK_BENEFIT_employer_id` FOREIGN KEY (`employer_id`) REFERENCES `employer` (`organization_id`),
  CONSTRAINT `FK_BENEFIT_plan_type_id` FOREIGN KEY (`plan_type_id`) REFERENCES `plantype` (`PlanType_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `benefittier`
--

DROP TABLE IF EXISTS `benefittier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `benefittier` (
  `benefit_tier_id` varchar(255) NOT NULL,
  `end_date` date DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `tier_description` varchar(255) DEFAULT NULL,
  `tier_name` varchar(255) DEFAULT NULL,
  `benefit_id` int DEFAULT NULL,
  PRIMARY KEY (`benefit_tier_id`),
  KEY `FK_BENEFITTIER_benefit_id` (`benefit_id`),
  CONSTRAINT `FK_BENEFITTIER_benefit_id` FOREIGN KEY (`benefit_id`) REFERENCES `benefit` (`benefit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `benefittype`
--

DROP TABLE IF EXISTS `benefittype`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `benefittype` (
  `benefittype_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `default_billingtype_id` bigint DEFAULT NULL,
  `psp_id` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`benefittype_id`),
  KEY `default_billingtype_id` (`default_billingtype_id`),
  KEY `psp_id` (`psp_id`),
  CONSTRAINT `benefittype_ibfk_1` FOREIGN KEY (`default_billingtype_id`) REFERENCES `billingtype` (`billingtype_id`),
  CONSTRAINT `benefittype_ibfk_2` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `billing_summary`
--

DROP TABLE IF EXISTS `billing_summary`;
/*!50001 DROP VIEW IF EXISTS `billing_summary`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `billing_summary` AS SELECT 
 1 AS `uuid`,
 1 AS `employer_id`,
 1 AS `month_id`,
 1 AS `cobra_tot`,
 1 AS `direct_tot`,
 1 AS `dual_plan_tot`,
 1 AS `fsa_tot`,
 1 AS `hra_tot`,
 1 AS `hsa_tot`,
 1 AS `lsa_tot`,
 1 AS `retiree_tot`,
 1 AS `transit_tot`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `billinggrid`
--

DROP TABLE IF EXISTS `billinggrid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billinggrid` (
  `grid_id` varchar(255) NOT NULL,
  `cobra` tinyint(1) DEFAULT '0',
  `current_status` varchar(255) DEFAULT NULL,
  `direct` tinyint(1) DEFAULT '0',
  `dual_plan` tinyint(1) DEFAULT '0',
  `fsa` tinyint(1) DEFAULT '0',
  `hra` tinyint(1) DEFAULT '0',
  `hsa` tinyint(1) DEFAULT '0',
  `lsa` tinyint(1) DEFAULT '0',
  `retiree` tinyint(1) DEFAULT '0',
  `transit` tinyint(1) DEFAULT '0',
  `month_id` int DEFAULT NULL,
  `employee_id` int DEFAULT NULL,
  `employer_id` int DEFAULT NULL,
  PRIMARY KEY (`grid_id`),
  KEY `FK_BILLINGGRID_employer_id` (`employer_id`),
  KEY `FK_BILLINGGRID_month_id` (`month_id`),
  KEY `FK_BILLINGGRID_employee_id` (`employee_id`),
  CONSTRAINT `FK_BILLINGGRID_employee_id` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`employee_id`),
  CONSTRAINT `FK_BILLINGGRID_employer_id` FOREIGN KEY (`employer_id`) REFERENCES `employer` (`organization_id`),
  CONSTRAINT `FK_BILLINGGRID_month_id` FOREIGN KEY (`month_id`) REFERENCES `billingmonth` (`billing_month_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `billinggroup`
--

DROP TABLE IF EXISTS `billinggroup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billinggroup` (
  `billing_group_id` int NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`billing_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `billingitem`
--

DROP TABLE IF EXISTS `billingitem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billingitem` (
  `billing_id` varchar(255) NOT NULL,
  `benefit` varchar(255) DEFAULT NULL,
  `benefit_group_id` int DEFAULT NULL,
  `employer_id` int DEFAULT NULL,
  `employer` varchar(255) DEFAULT NULL,
  `card` tinyint(1) DEFAULT '0',
  `participant_id` int DEFAULT NULL,
  `participant` varchar(255) DEFAULT NULL,
  `month_id` int DEFAULT NULL,
  PRIMARY KEY (`billing_id`),
  KEY `FK_BILLINGITEM_month_id` (`month_id`),
  CONSTRAINT `FK_BILLINGITEM_month_id` FOREIGN KEY (`month_id`) REFERENCES `billingmonth` (`billing_month_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `billinglink`
--

DROP TABLE IF EXISTS `billinglink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billinglink` (
  `billing_id` varchar(255) NOT NULL,
  `unique_id` varchar(255) DEFAULT NULL,
  `month_id` int DEFAULT NULL,
  `organization_id` int DEFAULT NULL,
  `employer_id` bigint DEFAULT NULL,
  PRIMARY KEY (`billing_id`),
  KEY `FK_BILLINGLINK_organization_id` (`organization_id`),
  KEY `FK_BILLINGLINK_month_id` (`month_id`),
  CONSTRAINT `FK_BILLINGLINK_month_id` FOREIGN KEY (`month_id`) REFERENCES `billingmonth` (`billing_month_id`),
  CONSTRAINT `FK_BILLINGLINK_organization_id` FOREIGN KEY (`organization_id`) REFERENCES `semployer` (`OrganizationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `billingmonth`
--

DROP TABLE IF EXISTS `billingmonth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billingmonth` (
  `billing_month_id` int NOT NULL,
  `full_date` date DEFAULT NULL,
  `month` int DEFAULT NULL,
  `year` int DEFAULT NULL,
  PRIMARY KEY (`billing_month_id`),
  UNIQUE KEY `full_date` (`full_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `billingtype`
--

DROP TABLE IF EXISTS `billingtype`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billingtype` (
  `billingtype_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `psp_id` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`billingtype_id`),
  KEY `psp_id` (`psp_id`),
  CONSTRAINT `billingtype_ibfk_1` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bpo_psp_assignment`
--

DROP TABLE IF EXISTS `bpo_psp_assignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bpo_psp_assignment` (
  `assignment_id` bigint NOT NULL AUTO_INCREMENT,
  `bpo_user_id` bigint NOT NULL,
  `psp_id` bigint NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `date_assigned` date NOT NULL,
  PRIMARY KEY (`assignment_id`),
  UNIQUE KEY `uq_bpo_psp_user` (`bpo_user_id`,`psp_id`),
  KEY `FK_BPOPSP_psp` (`psp_id`),
  CONSTRAINT `FK_BPOPSP_psp` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_BPOPSP_user` FOREIGN KEY (`bpo_user_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bpo_registration`
--

DROP TABLE IF EXISTS `bpo_registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bpo_registration` (
  `bpo_reg_id` bigint NOT NULL AUTO_INCREMENT,
  `psp_id` bigint NOT NULL,
  `bpo_name` varchar(100) NOT NULL,
  `bpo_url` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `date_registered` date NOT NULL,
  `is_approved` tinyint(1) NOT NULL DEFAULT '0',
  `is_requested` tinyint(1) NOT NULL DEFAULT '0',
  `is_accepted` tinyint(1) NOT NULL DEFAULT '0',
  `api_token_outbound` varchar(64) DEFAULT NULL,
  `api_token_inbound` varchar(64) DEFAULT NULL,
  `partner_url` varchar(255) DEFAULT NULL,
  `date_requested` date DEFAULT NULL,
  `date_approved` date DEFAULT NULL,
  `date_disconnected` date DEFAULT NULL,
  PRIMARY KEY (`bpo_reg_id`),
  KEY `FK_BPOREG_psp` (`psp_id`),
  CONSTRAINT `FK_BPOREG_psp` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `ck_base_01`
--

DROP TABLE IF EXISTS `ck_base_01`;
/*!50001 DROP VIEW IF EXISTS `ck_base_01`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ck_base_01` AS SELECT 
 1 AS `todo_count`,
 1 AS `checklist_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ck_base_02`
--

DROP TABLE IF EXISTS `ck_base_02`;
/*!50001 DROP VIEW IF EXISTS `ck_base_02`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ck_base_02` AS SELECT 
 1 AS `todo_count`,
 1 AS `checklist_id`,
 1 AS `full_name`,
 1 AS `due_date`,
 1 AS `assigned_to_id`,
 1 AS `recurring_list_id`,
 1 AS `days_in_advance`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ck_base_03`
--

DROP TABLE IF EXISTS `ck_base_03`;
/*!50001 DROP VIEW IF EXISTS `ck_base_03`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ck_base_03` AS SELECT 
 1 AS `todo_count`,
 1 AS `checklist_id`,
 1 AS `full_name`,
 1 AS `due_date`,
 1 AS `assigned_to_id`,
 1 AS `todo_id`,
 1 AS `task_id`,
 1 AS `name`,
 1 AS `has_owner`,
 1 AS `is_sourced`,
 1 AS `owner_id`,
 1 AS `allow_non_owner`,
 1 AS `allow_early`,
 1 AS `allow_future`,
 1 AS `sort_order`,
 1 AS `min_sort_top`,
 1 AS `at_top`,
 1 AS `min_sort_block`,
 1 AS `is_blocked`,
 1 AS `checklist_assigned_to_id`,
 1 AS `checklist_owner`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `auto_id`,
 1 AS `has_automation`,
 1 AS `bpo_registration_id`,
 1 AS `not_me`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ck_base_04`
--

DROP TABLE IF EXISTS `ck_base_04`;
/*!50001 DROP VIEW IF EXISTS `ck_base_04`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ck_base_04` AS SELECT 
 1 AS `todo_count`,
 1 AS `checklist_id`,
 1 AS `full_name`,
 1 AS `due_date`,
 1 AS `assigned_to_id`,
 1 AS `todo_id`,
 1 AS `task_id`,
 1 AS `name`,
 1 AS `has_owner`,
 1 AS `is_sourced`,
 1 AS `owner_id`,
 1 AS `allow_non_owner`,
 1 AS `allow_early`,
 1 AS `allow_future`,
 1 AS `sort_order`,
 1 AS `min_sort_top`,
 1 AS `at_top`,
 1 AS `min_sort_block`,
 1 AS `is_blocked`,
 1 AS `checklist_assigned_to_id`,
 1 AS `checklist_owner`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `auto_id`,
 1 AS `has_automation`,
 1 AS `bpo_registration_id`,
 1 AS `not_me`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ck_base_05`
--

DROP TABLE IF EXISTS `ck_base_05`;
/*!50001 DROP VIEW IF EXISTS `ck_base_05`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ck_base_05` AS SELECT 
 1 AS `todo_count`,
 1 AS `checklist_id`,
 1 AS `full_name`,
 1 AS `due_date`,
 1 AS `assigned_to_id`,
 1 AS `recurring_list_id`,
 1 AS `days_in_advance`,
 1 AS `todo_count_f`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ckx_open_checklists`
--

DROP TABLE IF EXISTS `ckx_open_checklists`;
/*!50001 DROP VIEW IF EXISTS `ckx_open_checklists`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ckx_open_checklists` AS SELECT 
 1 AS `todo_count`,
 1 AS `checklist_id`,
 1 AS `full_name`,
 1 AS `due_date`,
 1 AS `assigned_to_id`,
 1 AS `days_in_advance`,
 1 AS `owner_id`,
 1 AS `bpo_registration_id`,
 1 AS `has_owner`,
 1 AS `is_sourced`,
 1 AS `UID`,
 1 AS `show_date`,
 1 AS `todo_count_f`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `constant`
--

DROP TABLE IF EXISTS `constant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `constant` (
  `name` varchar(50) NOT NULL,
  `value` varchar(1000) NOT NULL,
  `note` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contactmethod`
--

DROP TABLE IF EXISTS `contactmethod`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contactmethod` (
  `method_id` int NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`method_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coverage`
--

DROP TABLE IF EXISTS `coverage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coverage` (
  `coverage_id` int NOT NULL,
  `benefit_name` varchar(255) DEFAULT NULL,
  `current_month` date DEFAULT NULL,
  `employer_id` int DEFAULT NULL,
  `is_billable` tinyint(1) DEFAULT '0',
  `pb_benefit_id` int DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `tier_name` varchar(255) DEFAULT NULL,
  `plan_type_id` int DEFAULT NULL,
  `billing_group_id` int DEFAULT NULL,
  `participant_id` int DEFAULT NULL,
  `organization_id` int DEFAULT NULL,
  PRIMARY KEY (`coverage_id`),
  KEY `FK_COVERAGE_plan_type_id` (`plan_type_id`),
  KEY `FK_COVERAGE_billing_group_id` (`billing_group_id`),
  CONSTRAINT `FK_COVERAGE_billing_group_id` FOREIGN KEY (`billing_group_id`) REFERENCES `billinggroup` (`billing_group_id`),
  CONSTRAINT `FK_COVERAGE_plan_type_id` FOREIGN KEY (`plan_type_id`) REFERENCES `plantype` (`PlanType_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coveragestatus`
--

DROP TABLE IF EXISTS `coveragestatus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coveragestatus` (
  `coverage_status_id` varchar(255) NOT NULL,
  `cards` tinyint(1) DEFAULT '0',
  `active` tinyint(1) DEFAULT '0',
  `month_for` date DEFAULT NULL,
  `benefit_id` int DEFAULT NULL,
  `billing_group_id` int DEFAULT NULL,
  `employee_id` int DEFAULT NULL,
  `employer_id` int DEFAULT NULL,
  PRIMARY KEY (`coverage_status_id`),
  KEY `FK_COVERAGESTATUS_benefit_id` (`benefit_id`),
  KEY `FK_COVERAGESTATUS_employer_id` (`employer_id`),
  KEY `FK_COVERAGESTATUS_billing_group_id` (`billing_group_id`),
  KEY `FK_COVERAGESTATUS_employee_id` (`employee_id`),
  CONSTRAINT `FK_COVERAGESTATUS_benefit_id` FOREIGN KEY (`benefit_id`) REFERENCES `benefit` (`benefit_id`),
  CONSTRAINT `FK_COVERAGESTATUS_billing_group_id` FOREIGN KEY (`billing_group_id`) REFERENCES `billinggroup` (`billing_group_id`),
  CONSTRAINT `FK_COVERAGESTATUS_employee_id` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`employee_id`),
  CONSTRAINT `FK_COVERAGESTATUS_employer_id` FOREIGN KEY (`employer_id`) REFERENCES `employer` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `datapair`
--

DROP TABLE IF EXISTS `datapair`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `datapair` (
  `data_pair_id` bigint NOT NULL,
  `data_value` varchar(255) DEFAULT NULL,
  `key_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`data_pair_id`),
  KEY `FK_DATAPAIR_key_name` (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `days_of_week`
--

DROP TABLE IF EXISTS `days_of_week`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `days_of_week` (
  `dow_id` int NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`dow_id`,`item_id`),
  KEY `FK_days_of_week_item_id` (`item_id`),
  CONSTRAINT `FK_days_of_week_dow_id` FOREIGN KEY (`dow_id`) REFERENCES `dow` (`dow_id`),
  CONSTRAINT `FK_days_of_week_item_id` FOREIGN KEY (`item_id`) REFERENCES `tasksequence` (`sequence_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `delegated_todo`
--

DROP TABLE IF EXISTS `delegated_todo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delegated_todo` (
  `delegated_id` bigint NOT NULL AUTO_INCREMENT,
  `todo_guid` varchar(36) NOT NULL,
  `psp_client_id` bigint NOT NULL,
  `task_name` varchar(255) NOT NULL,
  `task_description` text,
  `due_date` date DEFAULT NULL,
  `goto_link` varchar(500) DEFAULT NULL,
  `info_link` varchar(500) DEFAULT NULL,
  `activity_type` varchar(30) DEFAULT NULL,
  `activity_name` varchar(255) DEFAULT NULL,
  `employer_name` varchar(255) DEFAULT NULL,
  `assigned_to_id` bigint DEFAULT NULL,
  `is_completed` tinyint(1) NOT NULL DEFAULT '0',
  `completed_by_id` bigint DEFAULT NULL,
  `completed_date` date DEFAULT NULL,
  `is_reverted` tinyint(1) NOT NULL DEFAULT '0',
  `last_sync_timestamp` datetime DEFAULT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'ACTIVE',
  `date_received` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`delegated_id`),
  UNIQUE KEY `uk_delegated_todo_guid` (`todo_guid`),
  KEY `fk_delegated_psp_client` (`psp_client_id`),
  KEY `fk_delegated_assigned_to` (`assigned_to_id`),
  KEY `fk_delegated_completed_by` (`completed_by_id`),
  CONSTRAINT `fk_delegated_assigned_to` FOREIGN KEY (`assigned_to_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `fk_delegated_completed_by` FOREIGN KEY (`completed_by_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `fk_delegated_psp_client` FOREIGN KEY (`psp_client_id`) REFERENCES `psp_clients` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dow`
--

DROP TABLE IF EXISTS `dow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dow` (
  `dow_id` int NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `weekday_int` int DEFAULT NULL,
  PRIMARY KEY (`dow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dpier`
--

DROP TABLE IF EXISTS `dpier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dpier` (
  `er_key` int NOT NULL,
  `tax_id` varchar(45) DEFAULT NULL,
  `er_company` varchar(205) DEFAULT NULL,
  `er_first` varchar(200) DEFAULT NULL,
  `er_last` varchar(200) DEFAULT NULL,
  `er_phone` varchar(45) DEFAULT NULL,
  `email` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`er_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `dup_emails`
--

DROP TABLE IF EXISTS `dup_emails`;
/*!50001 DROP VIEW IF EXISTS `dup_emails`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `dup_emails` AS SELECT 
 1 AS `uuid`,
 1 AS `email`,
 1 AS `email_count`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `dup_names`
--

DROP TABLE IF EXISTS `dup_names`;
/*!50001 DROP VIEW IF EXISTS `dup_names`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `dup_names` AS SELECT 
 1 AS `uuid`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `name_count`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `dup_names_1`
--

DROP TABLE IF EXISTS `dup_names_1`;
/*!50001 DROP VIEW IF EXISTS `dup_names_1`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `dup_names_1` AS SELECT 
 1 AS `uuid`,
 1 AS `first_name`,
 1 AS `last_name`,
 1 AS `name_count`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `dupemailee`
--

DROP TABLE IF EXISTS `dupemailee`;
/*!50001 DROP VIEW IF EXISTS `dupemailee`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `dupemailee` AS SELECT 
 1 AS `email1`,
 1 AS `employer_id`,
 1 AS `e_count`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `email_recipents`
--

DROP TABLE IF EXISTS `email_recipents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_recipents` (
  `recipient_id` bigint NOT NULL,
  `email_id` bigint NOT NULL,
  PRIMARY KEY (`recipient_id`,`email_id`),
  KEY `FK_email_recipents_email_id` (`email_id`),
  CONSTRAINT `FK_email_recipents_email_id` FOREIGN KEY (`email_id`) REFERENCES `note` (`note_id`),
  CONSTRAINT `FK_email_recipents_recipient_id` FOREIGN KEY (`recipient_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `employee`
--

DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `employee_id` int NOT NULL,
  `address1` varchar(255) DEFAULT NULL,
  `address2` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `mm_key` int DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `zip` varchar(255) DEFAULT NULL,
  `employer_id` int DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '0',
  `hr_email` varchar(255) DEFAULT NULL,
  `custom_id` varchar(45) DEFAULT NULL,
  `ee_status_id` int DEFAULT NULL,
  `system_status_id` int DEFAULT NULL,
  `cobra_status_id` int DEFAULT NULL,
  PRIMARY KEY (`employee_id`),
  KEY `FK_EMPLOYEE_employer_id` (`employer_id`),
  CONSTRAINT `FK_EMPLOYEE_employer_id` FOREIGN KEY (`employer_id`) REFERENCES `employer` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `employeev`
--

DROP TABLE IF EXISTS `employeev`;
/*!50001 DROP VIEW IF EXISTS `employeev`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `employeev` AS SELECT 
 1 AS `employee_id`,
 1 AS `f_name`,
 1 AS `l_name`,
 1 AS `email1`,
 1 AS `email2`,
 1 AS `employer_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `employer`
--

DROP TABLE IF EXISTS `employer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employer` (
  `organization_id` int NOT NULL,
  `employer_id` int DEFAULT NULL,
  `contact_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `employer_name` varchar(255) DEFAULT NULL,
  `er_key` int DEFAULT NULL,
  `active` tinyint(1) DEFAULT '0',
  `is_agency` tinyint(1) DEFAULT '0',
  `billable` tinyint(1) DEFAULT '0',
  `phone` varchar(255) DEFAULT NULL,
  `has_pop` tinyint(1) DEFAULT '0',
  `has_cdh` tinyint(1) DEFAULT '0',
  `has_pb` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `employer_contacts`
--

DROP TABLE IF EXISTS `employer_contacts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employer_contacts` (
  `employer_id` int NOT NULL,
  `assignee_id` int NOT NULL,
  PRIMARY KEY (`employer_id`,`assignee_id`),
  KEY `FK_employer_contacts_assignee_id` (`assignee_id`),
  CONSTRAINT `FK_employer_contacts_assignee_id` FOREIGN KEY (`assignee_id`) REFERENCES `employee` (`employee_id`),
  CONSTRAINT `FK_employer_contacts_employer_id` FOREIGN KEY (`employer_id`) REFERENCES `employer` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `enhancement`
--

DROP TABLE IF EXISTS `enhancement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enhancement` (
  `enhancement_id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(200) NOT NULL,
  `short_text` varchar(20) NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `suppressed` tinyint(1) NOT NULL DEFAULT '0',
  `psp_id` bigint NOT NULL,
  `service_item_id` int DEFAULT NULL,
  PRIMARY KEY (`enhancement_id`),
  KEY `psp_id` (`psp_id`),
  KEY `FK_ENHANCEMENT_service_item_id` (`service_item_id`),
  CONSTRAINT `enhancement_ibfk_1` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_ENHANCEMENT_service_item_id` FOREIGN KEY (`service_item_id`) REFERENCES `templatepurpose` (`purpose_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `enhancement_los`
--

DROP TABLE IF EXISTS `enhancement_los`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enhancement_los` (
  `enhancement_id` bigint NOT NULL,
  `los_id` bigint NOT NULL,
  PRIMARY KEY (`enhancement_id`,`los_id`),
  KEY `los_id` (`los_id`),
  CONSTRAINT `enhancement_los_ibfk_1` FOREIGN KEY (`enhancement_id`) REFERENCES `enhancement` (`enhancement_id`),
  CONSTRAINT `enhancement_los_ibfk_2` FOREIGN KEY (`los_id`) REFERENCES `los` (`los_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `enrollment`
--

DROP TABLE IF EXISTS `enrollment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment` (
  `enrollment_id` int NOT NULL,
  `benefit_group` varchar(255) DEFAULT NULL,
  `benefit_name` varchar(255) DEFAULT NULL,
  `billable` tinyint(1) DEFAULT '0',
  `card_enabled` tinyint(1) DEFAULT '0',
  `current_month` date DEFAULT NULL,
  `employer_name` varchar(255) DEFAULT NULL,
  `py_end` date DEFAULT NULL,
  `participant_name` varchar(255) DEFAULT NULL,
  `py_start` date DEFAULT NULL,
  `term_date` date DEFAULT NULL,
  `billing_group_id` int DEFAULT NULL,
  `benefit_id` int DEFAULT NULL,
  `benefit_year_id` int DEFAULT NULL,
  `participant_id` int DEFAULT NULL,
  `organization_id` int DEFAULT NULL,
  PRIMARY KEY (`enrollment_id`),
  KEY `FK_ENROLLMENT_benefit_year_id` (`benefit_year_id`),
  KEY `FK_ENROLLMENT_billing_group_id` (`billing_group_id`),
  KEY `FK_ENROLLMENT_benefit_id` (`benefit_id`),
  KEY `FK_ENROLLMENT_participant_id` (`participant_id`),
  KEY `FK_ENROLLMENT_organization_id` (`organization_id`),
  CONSTRAINT `FK_ENROLLMENT_benefit_id` FOREIGN KEY (`benefit_id`) REFERENCES `sbenefit` (`EmployerPlan_ID`),
  CONSTRAINT `FK_ENROLLMENT_benefit_year_id` FOREIGN KEY (`benefit_year_id`) REFERENCES `sbenefityear` (`EmployerPlanDetailForPlanYear_ID`),
  CONSTRAINT `FK_ENROLLMENT_billing_group_id` FOREIGN KEY (`billing_group_id`) REFERENCES `billinggroup` (`billing_group_id`),
  CONSTRAINT `FK_ENROLLMENT_organization_id` FOREIGN KEY (`organization_id`) REFERENCES `semployer` (`OrganizationID`),
  CONSTRAINT `FK_ENROLLMENT_participant_id` FOREIGN KEY (`participant_id`) REFERENCES `semployee` (`Participant_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `enrollment2`
--

DROP TABLE IF EXISTS `enrollment2`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment2` (
  `enrollment_id` int NOT NULL,
  `benefit_group` varchar(255) DEFAULT NULL,
  `benefit_name` varchar(255) DEFAULT NULL,
  `billable` tinyint(1) DEFAULT '0',
  `card_enabled` tinyint(1) DEFAULT '0',
  `current_month` date DEFAULT NULL,
  `employer_name` varchar(255) DEFAULT NULL,
  `py_end` date DEFAULT NULL,
  `participant_name` varchar(255) DEFAULT NULL,
  `py_start` date DEFAULT NULL,
  `term_date` date DEFAULT NULL,
  `billing_group_id` int DEFAULT NULL,
  `benefit_id` int DEFAULT NULL,
  `benefit_year_id` int DEFAULT NULL,
  `participant_id` int DEFAULT NULL,
  `organization_id` int DEFAULT NULL,
  PRIMARY KEY (`enrollment_id`),
  KEY `FK_ENROLLMENT2_benefit_year_id` (`benefit_year_id`),
  KEY `FK_ENROLLMENT2_billing_group_id` (`billing_group_id`),
  KEY `FK_ENROLLMENT2_benefit_id` (`benefit_id`),
  KEY `FK_ENROLLMENT2_participant_id` (`participant_id`),
  KEY `FK_ENROLLMENT2_organization_id` (`organization_id`),
  CONSTRAINT `FK_ENROLLMENT2_benefit_id` FOREIGN KEY (`benefit_id`) REFERENCES `import4benefitcdh` (`EmployerPlan_ID`),
  CONSTRAINT `FK_ENROLLMENT2_benefit_year_id` FOREIGN KEY (`benefit_year_id`) REFERENCES `import5benefityear` (`EmployerPlanDetailForPlanYear_ID`),
  CONSTRAINT `FK_ENROLLMENT2_billing_group_id` FOREIGN KEY (`billing_group_id`) REFERENCES `billinggroup` (`billing_group_id`),
  CONSTRAINT `FK_ENROLLMENT2_organization_id` FOREIGN KEY (`organization_id`) REFERENCES `import1employer` (`OrganizationID`),
  CONSTRAINT `FK_ENROLLMENT2_participant_id` FOREIGN KEY (`participant_id`) REFERENCES `import2employee` (`Participant_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feature`
--

DROP TABLE IF EXISTS `feature`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feature` (
  `feature_id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(500) NOT NULL,
  `sort_order` int DEFAULT NULL,
  `module_id` bigint NOT NULL,
  `psp_id` bigint NOT NULL,
  `library_resource_id` bigint DEFAULT NULL,
  PRIMARY KEY (`feature_id`),
  KEY `module_id` (`module_id`),
  KEY `psp_id` (`psp_id`),
  KEY `fk_feature_library_resource` (`library_resource_id`),
  CONSTRAINT `feature_ibfk_1` FOREIGN KEY (`module_id`) REFERENCES `servicemodule` (`module_id`),
  CONSTRAINT `feature_ibfk_2` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `fk_feature_library_resource` FOREIGN KEY (`library_resource_id`) REFERENCES `marketingmaterial` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hsaaccount`
--

DROP TABLE IF EXISTS `hsaaccount`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hsaaccount` (
  `hsaid` int NOT NULL,
  `last_name` varchar(45) DEFAULT NULL,
  `first_name` varchar(45) DEFAULT NULL,
  `employer` varchar(200) DEFAULT NULL,
  `address` varchar(400) DEFAULT NULL,
  `city` varchar(200) DEFAULT NULL,
  `state` varchar(45) DEFAULT NULL,
  `zip` varchar(45) DEFAULT NULL,
  `phone` varchar(45) DEFAULT NULL,
  `acct_num` varchar(45) NOT NULL,
  `active` tinyint DEFAULT NULL,
  PRIMARY KEY (`hsaid`,`acct_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hsaee`
--

DROP TABLE IF EXISTS `hsaee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hsaee` (
  `hsa_id` int NOT NULL,
  `last_name` varchar(145) DEFAULT NULL,
  `first_name` varchar(145) DEFAULT NULL,
  `address` varchar(450) DEFAULT NULL,
  `city` varchar(145) DEFAULT NULL,
  `state` varchar(45) DEFAULT NULL,
  `zip` varchar(45) DEFAULT NULL,
  `phone` varchar(45) DEFAULT NULL,
  `hsa_er_id` varchar(500) DEFAULT NULL,
  `employee_id` int DEFAULT NULL,
  PRIMARY KEY (`hsa_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hsaer`
--

DROP TABLE IF EXISTS `hsaer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hsaer` (
  `name` varchar(500) NOT NULL,
  `organization_id` int DEFAULT NULL,
  `billed_direct` tinyint DEFAULT '0',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `import1employer`
--

DROP TABLE IF EXISTS `import1employer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `import1employer` (
  `OrganizationID` int NOT NULL,
  `Agency` varchar(255) DEFAULT NULL,
  `AgencyOrganizationID` varchar(255) DEFAULT NULL,
  `CreatedByUser` varchar(255) DEFAULT NULL,
  `CreatedByUser1` varchar(255) DEFAULT NULL,
  `CreatedDate` varchar(255) DEFAULT NULL,
  `CreatedUser` varchar(255) DEFAULT NULL,
  `CustomID` varchar(255) DEFAULT NULL,
  `Email` varchar(255) DEFAULT NULL,
  `Employer_ID` int DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `ImplementationLead` varchar(255) DEFAULT NULL,
  `ImplementationLeadUserID` varchar(255) DEFAULT NULL,
  `IsSetUpCompleted` varchar(255) DEFAULT NULL,
  `OrganizationStatusID` varchar(255) DEFAULT NULL,
  `Phone` varchar(255) DEFAULT NULL,
  `PhoneNumber` varchar(255) DEFAULT NULL,
  `PrimaryContact` varchar(255) DEFAULT NULL,
  `SetUpComplete` varchar(255) DEFAULT NULL,
  `SetUpCompletionDate` varchar(255) DEFAULT NULL,
  `Setup` varchar(255) DEFAULT NULL,
  `Status` varchar(255) DEFAULT NULL,
  `TaxID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`OrganizationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `import2employee`
--

DROP TABLE IF EXISTS `import2employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `import2employee` (
  `Participant_ID` int NOT NULL,
  `Address1` varchar(255) DEFAULT NULL,
  `Address2` varchar(255) DEFAULT NULL,
  `City` varchar(255) DEFAULT NULL,
  `ParticipantCustomID` varchar(255) DEFAULT NULL,
  `Email` varchar(255) DEFAULT NULL,
  `EmployerCustomID` varchar(255) DEFAULT NULL,
  `Employer_ID` varchar(255) DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `FailedLoginCount` varchar(255) DEFAULT NULL,
  `FirstName` varchar(255) DEFAULT NULL,
  `IsRegisterdToPortal` varchar(255) DEFAULT NULL,
  `LastLoginDate` varchar(255) DEFAULT NULL,
  `LastName` varchar(255) DEFAULT NULL,
  `SetupCompletionDate` varchar(255) DEFAULT NULL,
  `State` varchar(255) DEFAULT NULL,
  `User_ID` varchar(255) DEFAULT NULL,
  `UserStatus` varchar(255) DEFAULT NULL,
  `ZipCode` varchar(255) DEFAULT NULL,
  `Organization_ID` int DEFAULT NULL,
  PRIMARY KEY (`Participant_ID`),
  KEY `FK_IMPORTEMPLOYEE_Organization_ID` (`Organization_ID`),
  CONSTRAINT `FK_IMPORTEMPLOYEE_Organization_ID` FOREIGN KEY (`Organization_ID`) REFERENCES `import1employer` (`OrganizationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `import3employeealt`
--

DROP TABLE IF EXISTS `import3employeealt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `import3employeealt` (
  `Participant_ID` int NOT NULL,
  `EffectiveDate` varchar(45) DEFAULT NULL,
  `TerminationDate` varchar(45) DEFAULT NULL,
  `UserId` varchar(45) DEFAULT NULL,
  `UserStatus` varchar(45) DEFAULT NULL,
  `CreatedDate` varchar(45) DEFAULT NULL,
  `HireDate` varchar(45) DEFAULT NULL,
  `Organization_ID` int DEFAULT NULL,
  `ParticipantStatusId` int DEFAULT NULL,
  `Name` varchar(45) DEFAULT NULL,
  `ERName` varchar(45) DEFAULT NULL,
  `ParticipantName` varchar(45) DEFAULT NULL,
  `Participant_Last` varchar(45) DEFAULT NULL,
  `Participant_First` varchar(45) DEFAULT NULL,
  `SSN` varchar(45) DEFAULT NULL,
  `userStatusID` int DEFAULT NULL,
  `DOB` varchar(45) DEFAULT NULL,
  `ReimbursementMethod` varchar(45) DEFAULT NULL,
  `EmploymentStatus` varchar(45) DEFAULT NULL,
  `EmploymentStatusID` int DEFAULT NULL,
  `No_of_participants` varchar(45) DEFAULT NULL,
  `DivisionName` varchar(45) DEFAULT NULL,
  `Bankname` varchar(45) DEFAULT NULL,
  `RoutingNo` varchar(45) DEFAULT NULL,
  `AccountType` varchar(45) DEFAULT NULL,
  `AccountNumber` varchar(45) DEFAULT NULL,
  `UserBank_ID` varchar(45) DEFAULT NULL,
  `ReimbursementMethod_ID` varchar(45) DEFAULT NULL,
  `ByDivision` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`Participant_ID`),
  KEY `FK_IMPORTEMPLOYEEALT_Organization_ID` (`Organization_ID`),
  CONSTRAINT `FK_IMPORTEMPLOYEEALT_Organization_ID` FOREIGN KEY (`Organization_ID`) REFERENCES `import1employer` (`OrganizationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `import4benefitcdh`
--

DROP TABLE IF EXISTS `import4benefitcdh`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `import4benefitcdh` (
  `EmployerPlan_ID` int NOT NULL,
  `CardEnabled` varchar(255) DEFAULT NULL,
  `EffectiveDate` varchar(255) DEFAULT NULL,
  `Employer_ID` varchar(255) DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `ImportPlanID` varchar(255) DEFAULT NULL,
  `LinkedtoDefaultPlan` varchar(255) DEFAULT NULL,
  `PlanDescription` varchar(255) DEFAULT NULL,
  `PlanName` varchar(255) DEFAULT NULL,
  `PlanStatus` varchar(255) DEFAULT NULL,
  `PlanType` varchar(255) DEFAULT NULL,
  `TerminationDate` varchar(255) DEFAULT NULL,
  `PlanTypeID` int DEFAULT NULL,
  `OrganizationID` int DEFAULT NULL,
  PRIMARY KEY (`EmployerPlan_ID`),
  KEY `FK_IMPORTBENEFITCDH_OrganizationID` (`OrganizationID`),
  KEY `FK_IMPORTBENEFITCDH_PlanTypeID` (`PlanTypeID`),
  CONSTRAINT `FK_IMPORTBENEFITCDH_OrganizationID` FOREIGN KEY (`OrganizationID`) REFERENCES `import1employer` (`OrganizationID`),
  CONSTRAINT `FK_IMPORTBENEFITCDH_PlanTypeID` FOREIGN KEY (`PlanTypeID`) REFERENCES `plantype` (`PlanType_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `import5benefityear`
--

DROP TABLE IF EXISTS `import5benefityear`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `import5benefityear` (
  `EmployerPlanDetailForPlanYear_ID` int NOT NULL,
  `ContributionSchedule` varchar(255) DEFAULT NULL,
  `ContributionScheduleTemplate_ID` varchar(255) DEFAULT NULL,
  `Employer` varchar(255) DEFAULT NULL,
  `Organization_ID` varchar(255) DEFAULT NULL,
  `PlanDescription` varchar(255) DEFAULT NULL,
  `PlanName` varchar(255) DEFAULT NULL,
  `PlanStatus` varchar(255) DEFAULT NULL,
  `PlanYear` varchar(255) DEFAULT NULL,
  `PlanYear_ID` int DEFAULT NULL,
  `EmployerPlan_ID` int DEFAULT NULL,
  PRIMARY KEY (`EmployerPlanDetailForPlanYear_ID`),
  KEY `FK_IMPORTBENEFITYEAR_EmployerPlan_ID` (`EmployerPlan_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `import6enrollment`
--

DROP TABLE IF EXISTS `import6enrollment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `import6enrollment` (
  `ParticipantPlan_ID` int NOT NULL,
  `AccountBalance` varchar(255) DEFAULT NULL,
  `ActiveParticipantCount` varchar(255) DEFAULT NULL,
  `AvailableBalance` varchar(255) DEFAULT NULL,
  `CarryoverAmount` varchar(255) DEFAULT NULL,
  `CoverageEndDate` varchar(255) DEFAULT NULL,
  `CustomID` varchar(255) DEFAULT NULL,
  `DisbursableBalance` varchar(255) DEFAULT NULL,
  `DivisionCustomID` varchar(255) DEFAULT NULL,
  `DivisionOrganizationID` varchar(255) DEFAULT NULL,
  `DivisionOrganizationName` varchar(255) DEFAULT NULL,
  `ElectionAmount` varchar(255) DEFAULT NULL,
  `EmployerID` varchar(255) DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `EmployerPlan_ID` varchar(255) DEFAULT NULL,
  `EmployerYTDContribution` varchar(255) DEFAULT NULL,
  `FirstName` varchar(255) DEFAULT NULL,
  `IsByDivision` varchar(255) DEFAULT NULL,
  `IsCarryOverEnable` varchar(255) DEFAULT NULL,
  `LastName` varchar(255) DEFAULT NULL,
  `OrganizationID` varchar(255) DEFAULT NULL,
  `ParticipantCount` varchar(255) DEFAULT NULL,
  `ParticipantYTDContribution` varchar(255) DEFAULT NULL,
  `PendingCardTransaction` varchar(255) DEFAULT NULL,
  `PlanDescription` varchar(255) DEFAULT NULL,
  `PlanName` varchar(255) DEFAULT NULL,
  `PlanStatus` varchar(255) DEFAULT NULL,
  `PlanType` varchar(255) DEFAULT NULL,
  `PlanYear` varchar(255) DEFAULT NULL,
  `SSN` varchar(255) DEFAULT NULL,
  `Eff_TermDate` varchar(255) DEFAULT NULL,
  `YTDClaim` varchar(255) DEFAULT NULL,
  `YtdDCReturn` varchar(255) DEFAULT NULL,
  `YTDPayments` varchar(255) DEFAULT NULL,
  `YtdReturn` varchar(255) DEFAULT NULL,
  `EmployerPlanDetailForPlanYearID` int DEFAULT NULL,
  `Participant_ID` int DEFAULT NULL,
  PRIMARY KEY (`ParticipantPlan_ID`),
  KEY `FK_IMPORTENROLLMENT_EmployerPlanDetailForPlanYearID` (`EmployerPlanDetailForPlanYearID`),
  KEY `FK_IMPORTENROLLMENT_Participant_ID` (`Participant_ID`),
  CONSTRAINT `FK_IMPORTENROLLMENT_EmployerPlanDetailForPlanYearID` FOREIGN KEY (`EmployerPlanDetailForPlanYearID`) REFERENCES `import5benefityear` (`EmployerPlanDetailForPlanYear_ID`),
  CONSTRAINT `FK_IMPORTENROLLMENT_Participant_ID` FOREIGN KEY (`Participant_ID`) REFERENCES `import2employee` (`Participant_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `import7benefitpb`
--

DROP TABLE IF EXISTS `import7benefitpb`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `import7benefitpb` (
  `TPA` varchar(200) DEFAULT NULL,
  `OrganizationID` int DEFAULT NULL,
  `EmployerID` int DEFAULT NULL,
  `Employer` varchar(45) DEFAULT NULL,
  `BenefitName` varchar(200) DEFAULT NULL,
  `PBBenefitID` int DEFAULT NULL,
  `Type` varchar(45) DEFAULT NULL,
  `RemitTo` varchar(45) DEFAULT NULL,
  `PlanTypeID` int DEFAULT NULL,
  `PBType` varchar(45) DEFAULT NULL,
  `PBTypeID` varchar(45) DEFAULT NULL,
  `BenefitID` int NOT NULL,
  `ImportPlanID` varchar(45) DEFAULT NULL,
  `EffectiveDate` varchar(45) DEFAULT NULL,
  `Carrier` varchar(45) DEFAULT NULL,
  `LastDayofCoverage` varchar(45) DEFAULT NULL,
  `Fee` varchar(45) DEFAULT NULL,
  `PlanYearID` int NOT NULL,
  `StartDate` varchar(45) DEFAULT NULL,
  `EndDate` varchar(45) DEFAULT NULL,
  `TierName` varchar(45) DEFAULT NULL,
  `TierID` varchar(45) DEFAULT NULL,
  `TierAge` varchar(45) DEFAULT NULL,
  `Gender` varchar(45) DEFAULT NULL,
  `Smoker` varchar(45) DEFAULT NULL,
  `Amount` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`BenefitID`,`PlanYearID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `import8cobraqb`
--

DROP TABLE IF EXISTS `import8cobraqb`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `import8cobraqb` (
  `EmployerSystemId` int DEFAULT NULL,
  `EmployerCustomId` varchar(45) DEFAULT NULL,
  `EmployerName` varchar(45) DEFAULT NULL,
  `EmployerPlanSystemID` int NOT NULL,
  `ImportPlanID` varchar(45) DEFAULT NULL,
  `EmployerPlanName` varchar(45) DEFAULT NULL,
  `EmployerDivisionID` varchar(45) DEFAULT NULL,
  `EmployerDivision` varchar(45) DEFAULT NULL,
  `IsIndividuallyRated` varchar(45) DEFAULT NULL,
  `EmployerPlanTierName` varchar(45) DEFAULT NULL,
  `UserId` int DEFAULT NULL,
  `Relationship` varchar(45) DEFAULT NULL,
  `ParticipantSystemId` int NOT NULL,
  `ParticipantCustomId` varchar(45) DEFAULT NULL,
  `ParticipantName` varchar(45) DEFAULT NULL,
  `ParticipantFirstName` varchar(45) DEFAULT NULL,
  `ParticipantLastName` varchar(45) DEFAULT NULL,
  `ParticipantMiddleName` varchar(45) DEFAULT NULL,
  `IsDependent` varchar(45) DEFAULT NULL,
  `CoveredMemberName` varchar(45) DEFAULT NULL,
  `CoveredMemberFirstName` varchar(45) DEFAULT NULL,
  `CoveredMemberLastName` varchar(45) DEFAULT NULL,
  `CoveredMemberMiddleName` varchar(45) DEFAULT NULL,
  `ParticipantAddress1` varchar(45) DEFAULT NULL,
  `ParticipantAddress2` varchar(45) DEFAULT NULL,
  `ParticipantCity` varchar(45) DEFAULT NULL,
  `ParticipantState` varchar(45) DEFAULT NULL,
  `ParticipantZipCode` varchar(45) DEFAULT NULL,
  `ParticipantEmail` varchar(45) DEFAULT NULL,
  `ParticipantPhone` varchar(45) DEFAULT NULL,
  `SSN` varchar(45) DEFAULT NULL,
  `DependentSystemID` varchar(45) DEFAULT NULL,
  `DependentCustomID` varchar(45) DEFAULT NULL,
  `DependentFirstName` varchar(45) DEFAULT NULL,
  `DependentLastName` varchar(45) DEFAULT NULL,
  `DependentMiddleName` varchar(45) DEFAULT NULL,
  `DependentRelationship` varchar(45) DEFAULT NULL,
  `DependentParticipantSystemID` varchar(45) DEFAULT NULL,
  `QualifyingEventReason` varchar(45) DEFAULT NULL,
  `QualifyingEventDate` varchar(45) DEFAULT NULL,
  `CoverageStatus` varchar(45) DEFAULT NULL,
  `CoverageStatusEffectiveDate` varchar(45) DEFAULT NULL,
  `LastDayOfCoverage` varchar(45) DEFAULT NULL,
  `LastDayToAccept` varchar(45) DEFAULT NULL,
  `CobraAccepted` varchar(45) DEFAULT NULL,
  `AcceptedEntered` varchar(45) DEFAULT NULL,
  `CobraAcceptedDate` varchar(45) DEFAULT NULL,
  `CobraStartDate` varchar(45) DEFAULT NULL,
  `CobraTermed` varchar(45) DEFAULT NULL,
  `TermedEntered` varchar(45) DEFAULT NULL,
  `Subsidy` varchar(45) DEFAULT NULL,
  `TransactionStartDate` varchar(45) DEFAULT NULL,
  `TransactionEndDate` varchar(45) DEFAULT NULL,
  `IsByDivision` varchar(45) DEFAULT NULL,
  `CoveredMembersNames` varchar(1005) DEFAULT NULL,
  `PBBillingFrequency` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`EmployerPlanSystemID`,`ParticipantSystemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `import9cobrapart`
--

DROP TABLE IF EXISTS `import9cobrapart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `import9cobrapart` (
  `User_ID` varchar(255) DEFAULT NULL,
  `Participant_id` int NOT NULL,
  `AcceptedDate` varchar(255) DEFAULT NULL,
  `DisplayPremium` varchar(255) DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `ExpirationDate` varchar(255) DEFAULT NULL,
  `PaidThroughDate` varchar(255) DEFAULT NULL,
  `ParticipantName` varchar(255) DEFAULT NULL,
  `CoveredMemberName` varchar(255) DEFAULT NULL,
  `PBTypeName` varchar(255) DEFAULT NULL,
  `QualifyingEvent` varchar(255) DEFAULT NULL,
  `QualifyingEventDate` varchar(255) DEFAULT NULL,
  `StartDate` varchar(255) DEFAULT NULL,
  `TermedDate` varchar(255) DEFAULT NULL,
  `BenefitName` varchar(255) DEFAULT NULL,
  `TransactionStartDate` varchar(255) DEFAULT NULL,
  `TransactionEndDate` varchar(255) DEFAULT NULL,
  `PBCoverageHeaderId` varchar(255) DEFAULT NULL,
  `Tier` varchar(255) DEFAULT NULL,
  `CoveredMembersNames` varchar(255) DEFAULT NULL,
  `IncludeDependentsCoveredUnderEachPlan` varchar(255) DEFAULT NULL,
  `IsDependent` varchar(255) DEFAULT NULL,
  `SSN` varchar(255) DEFAULT NULL,
  `DateOfBirth` varchar(255) DEFAULT NULL,
  `ParticipantAddress1` varchar(255) DEFAULT NULL,
  `ParticipantAddress2` varchar(255) DEFAULT NULL,
  `ParticipantCity` varchar(255) DEFAULT NULL,
  `ParticipantState` varchar(255) DEFAULT NULL,
  `ParticipantZip` varchar(255) DEFAULT NULL,
  `Carrier` varchar(255) DEFAULT NULL,
  `Gender` varchar(255) DEFAULT NULL,
  `CustomID` varchar(255) DEFAULT NULL,
  `Relationship` varchar(255) DEFAULT NULL,
  `PBBillingFrequency` varchar(255) DEFAULT NULL,
  `DivisionName` varchar(255) DEFAULT NULL,
  `DivisionCustomID` varchar(255) DEFAULT NULL,
  `DivisionID` varchar(255) DEFAULT NULL,
  `IsByDivision` varchar(255) DEFAULT NULL,
  `EmployerOrganizationID` int NOT NULL,
  PRIMARY KEY (`Participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `importacoverage`
--

DROP TABLE IF EXISTS `importacoverage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `importacoverage` (
  `PBCoverageHeaderID` int NOT NULL,
  `BenefitID` int DEFAULT NULL,
  `BenefitName` varchar(255) DEFAULT NULL,
  `CoverageStatus` varchar(255) DEFAULT NULL,
  `EffectiveDate` varchar(255) DEFAULT NULL,
  `EmployerCustomID` varchar(255) DEFAULT NULL,
  `EmployerDivision` varchar(255) DEFAULT NULL,
  `EmployerDivisionID` varchar(255) DEFAULT NULL,
  `EmployerID` int DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `ImportPlanID` varchar(255) DEFAULT NULL,
  `ParticipantCustomID` varchar(255) DEFAULT NULL,
  `ParticipantFirstName` varchar(255) DEFAULT NULL,
  `ParticipantLastName` varchar(255) DEFAULT NULL,
  `PlanType` varchar(255) DEFAULT NULL,
  `SSN` varchar(255) DEFAULT NULL,
  `TierName` varchar(255) DEFAULT NULL,
  `TransactionEndDate` varchar(255) DEFAULT NULL,
  `TransactionStartDate` varchar(255) DEFAULT NULL,
  `Participant_ID` int DEFAULT NULL,
  `ProcessedDate` varchar(45) DEFAULT NULL,
  `IsByDivision` varchar(45) DEFAULT NULL,
  `PBBillingFrequency` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`PBCoverageHeaderID`),
  KEY `FK_IMPORTCOVERAGE_Participant_ID` (`Participant_ID`),
  CONSTRAINT `FK_IMPORTCOVERAGE_Participant_ID` FOREIGN KEY (`Participant_ID`) REFERENCES `import2employee` (`Participant_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `importbcobraterm`
--

DROP TABLE IF EXISTS `importbcobraterm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `importbcobraterm` (
  `TransactionStartDate` varchar(255) DEFAULT NULL,
  `TransactionEndDate` varchar(255) DEFAULT NULL,
  `ParticipantName` varchar(255) NOT NULL,
  `ID` varchar(255) DEFAULT NULL,
  `SSN` varchar(255) DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `BenefitName` varchar(255) DEFAULT NULL,
  `Tier` varchar(255) DEFAULT NULL,
  `TerminationReason` varchar(255) DEFAULT NULL,
  `TermedDate` varchar(255) DEFAULT NULL,
  `TermedOn` varchar(255) DEFAULT NULL,
  `PBBillingFrequency` varchar(255) DEFAULT NULL,
  `EmployerOrganizationID` int NOT NULL,
  PRIMARY KEY (`ParticipantName`,`EmployerOrganizationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `importbenefittier`
--

DROP TABLE IF EXISTS `importbenefittier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `importbenefittier` (
  `PlanYearID` int NOT NULL,
  `EmployerID` int NOT NULL,
  `TierID` varchar(255) NOT NULL,
  `PBBenefitID` int NOT NULL,
  `Amount` varchar(255) DEFAULT NULL,
  `BenefitID` int DEFAULT NULL,
  `Carrier` varchar(255) DEFAULT NULL,
  `EffectiveDate` varchar(255) DEFAULT NULL,
  `Employer` varchar(255) DEFAULT NULL,
  `EndDate` varchar(255) DEFAULT NULL,
  `Fee` varchar(255) DEFAULT NULL,
  `Gender` varchar(255) DEFAULT NULL,
  `LastDayofCoverage` varchar(255) DEFAULT NULL,
  `PBType` varchar(255) DEFAULT NULL,
  `PBTypeID` varchar(255) DEFAULT NULL,
  `BenefitName` varchar(255) DEFAULT NULL,
  `ImportPlanID` varchar(255) DEFAULT NULL,
  `RemitTo` varchar(255) DEFAULT NULL,
  `Smoker` varchar(255) DEFAULT NULL,
  `StartDate` varchar(255) DEFAULT NULL,
  `TierAge` varchar(255) DEFAULT NULL,
  `TierName` varchar(255) DEFAULT NULL,
  `TPA` varchar(255) DEFAULT NULL,
  `Type` varchar(255) DEFAULT NULL,
  `PlanTypeID` int DEFAULT NULL,
  `OrganizationID` int DEFAULT NULL,
  PRIMARY KEY (`PlanYearID`,`EmployerID`,`TierID`,`PBBenefitID`),
  KEY `FK_IMPORTBENEFITTIER_OrganizationID` (`OrganizationID`),
  KEY `FK_IMPORTBENEFITTIER_PlanTypeID` (`PlanTypeID`),
  CONSTRAINT `FK_IMPORTBENEFITTIER_OrganizationID` FOREIGN KEY (`OrganizationID`) REFERENCES `import1employer` (`OrganizationID`),
  CONSTRAINT `FK_IMPORTBENEFITTIER_PlanTypeID` FOREIGN KEY (`PlanTypeID`) REFERENCES `plantype` (`PlanType_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invitation`
--

DROP TABLE IF EXISTS `invitation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invitation` (
  `invitation_id` bigint NOT NULL AUTO_INCREMENT,
  `guid` varchar(36) NOT NULL,
  `email` varchar(200) NOT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `agency_id` bigint NOT NULL,
  `role` varchar(20) NOT NULL,
  `invited_by` bigint NOT NULL,
  `person_id` bigint DEFAULT NULL,
  `date_created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `date_expires` timestamp NOT NULL,
  `date_accepted` timestamp NULL DEFAULT NULL,
  `is_used` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`invitation_id`),
  UNIQUE KEY `guid` (`guid`),
  KEY `fk_invitation_agency` (`agency_id`),
  KEY `fk_invitation_invited_by` (`invited_by`),
  KEY `fk_invitation_person` (`person_id`),
  CONSTRAINT `fk_invitation_agency` FOREIGN KEY (`agency_id`) REFERENCES `agency` (`agency_id`),
  CONSTRAINT `fk_invitation_invited_by` FOREIGN KEY (`invited_by`) REFERENCES `assignee` (`id`),
  CONSTRAINT `fk_invitation_person` FOREIGN KEY (`person_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `irslimit`
--

DROP TABLE IF EXISTS `irslimit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `irslimit` (
  `limit_key` varchar(50) NOT NULL,
  `plan_year` int NOT NULL,
  `amount` double NOT NULL,
  `description` varchar(200) NOT NULL,
  PRIMARY KEY (`limit_key`,`plan_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `linktype`
--

DROP TABLE IF EXISTS `linktype`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `linktype` (
  `link_type_id` int NOT NULL,
  `type_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`link_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `los`
--

DROP TABLE IF EXISTS `los`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `los` (
  `los_id` bigint NOT NULL,
  `description` varchar(100) DEFAULT NULL,
  `short_text` varchar(10) DEFAULT NULL,
  `psp_id` bigint DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `suppressed` tinyint(1) NOT NULL DEFAULT '0',
  `service_item_id` int DEFAULT NULL,
  PRIMARY KEY (`los_id`),
  KEY `FK_LOS_psp_id` (`psp_id`),
  KEY `FK_LOS_service_item_id` (`service_item_id`),
  CONSTRAINT `FK_LOS_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_LOS_service_item_id` FOREIGN KEY (`service_item_id`) REFERENCES `templatepurpose` (`purpose_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `losmodules`
--

DROP TABLE IF EXISTS `losmodules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `losmodules` (
  `los_id` bigint NOT NULL,
  `module_id` bigint NOT NULL,
  PRIMARY KEY (`los_id`,`module_id`),
  KEY `FK_losmodules_module_id` (`module_id`),
  CONSTRAINT `FK_losmodules_los_id` FOREIGN KEY (`los_id`) REFERENCES `los` (`los_id`),
  CONSTRAINT `FK_losmodules_module_id` FOREIGN KEY (`module_id`) REFERENCES `servicemodule` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marketingmaterial`
--

DROP TABLE IF EXISTS `marketingmaterial`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketingmaterial` (
  `material_id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `material_type` varchar(20) NOT NULL,
  `url` varchar(500) DEFAULT NULL,
  `storage_guid` varchar(50) DEFAULT NULL,
  `audience` varchar(20) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `psp_id` bigint NOT NULL,
  `category_id` bigint DEFAULT NULL,
  PRIMARY KEY (`material_id`),
  KEY `psp_id` (`psp_id`),
  KEY `fk_mm_category` (`category_id`),
  CONSTRAINT `fk_mm_category` FOREIGN KEY (`category_id`) REFERENCES `resourcecategory` (`category_id`),
  CONSTRAINT `marketingmaterial_ibfk_1` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `materialmodule`
--

DROP TABLE IF EXISTS `materialmodule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `materialmodule` (
  `material_id` bigint NOT NULL,
  `module_id` bigint NOT NULL,
  PRIMARY KEY (`material_id`,`module_id`),
  KEY `module_id` (`module_id`),
  CONSTRAINT `materialmodule_ibfk_1` FOREIGN KEY (`material_id`) REFERENCES `marketingmaterial` (`material_id`),
  CONSTRAINT `materialmodule_ibfk_2` FOREIGN KEY (`module_id`) REFERENCES `servicemodule` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `moduleitems`
--

DROP TABLE IF EXISTS `moduleitems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `moduleitems` (
  `item_id` bigint NOT NULL,
  `module_id` bigint NOT NULL,
  PRIMARY KEY (`item_id`,`module_id`),
  KEY `FK_moduleitems_module_id` (`module_id`),
  CONSTRAINT `FK_moduleitems_item_id` FOREIGN KEY (`item_id`) REFERENCES `serviceitem` (`service_item_id`),
  CONSTRAINT `FK_moduleitems_module_id` FOREIGN KEY (`module_id`) REFERENCES `servicemodule` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `n_base_01`
--

DROP TABLE IF EXISTS `n_base_01`;
/*!50001 DROP VIEW IF EXISTS `n_base_01`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `n_base_01` AS SELECT 
 1 AS `note_id`,
 1 AS `DTYPE`,
 1 AS `date_created`,
 1 AS `date_generated`,
 1 AS `DETAIL`,
 1 AS `activity_id`,
 1 AS `created_by_id`,
 1 AS `reason_id`,
 1 AS `status_id`,
 1 AS `subject`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `n_contact_1`
--

DROP TABLE IF EXISTS `n_contact_1`;
/*!50001 DROP VIEW IF EXISTS `n_contact_1`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `n_contact_1` AS SELECT 
 1 AS `note_id`,
 1 AS `DTYPE`,
 1 AS `date_created`,
 1 AS `date_generated`,
 1 AS `DETAIL`,
 1 AS `activity_id`,
 1 AS `created_by_id`,
 1 AS `reason_id`,
 1 AS `status_id`,
 1 AS `subject`,
 1 AS `outbound`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `n_contact_2`
--

DROP TABLE IF EXISTS `n_contact_2`;
/*!50001 DROP VIEW IF EXISTS `n_contact_2`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `n_contact_2` AS SELECT 
 1 AS `max_id`,
 1 AS `activity_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `n_contact_f`
--

DROP TABLE IF EXISTS `n_contact_f`;
/*!50001 DROP VIEW IF EXISTS `n_contact_f`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `n_contact_f` AS SELECT 
 1 AS `max_id`,
 1 AS `activity_id`,
 1 AS `date_generated`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `n_status_1`
--

DROP TABLE IF EXISTS `n_status_1`;
/*!50001 DROP VIEW IF EXISTS `n_status_1`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `n_status_1` AS SELECT 
 1 AS `note_id`,
 1 AS `DTYPE`,
 1 AS `date_created`,
 1 AS `date_generated`,
 1 AS `DETAIL`,
 1 AS `activity_id`,
 1 AS `created_by_id`,
 1 AS `reason_id`,
 1 AS `status_id`,
 1 AS `subject`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `n_status_f`
--

DROP TABLE IF EXISTS `n_status_f`;
/*!50001 DROP VIEW IF EXISTS `n_status_f`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `n_status_f` AS SELECT 
 1 AS `max_id`,
 1 AS `activity_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `n_status_f1`
--

DROP TABLE IF EXISTS `n_status_f1`;
/*!50001 DROP VIEW IF EXISTS `n_status_f1`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `n_status_f1` AS SELECT 
 1 AS `max_id`,
 1 AS `activity_id`,
 1 AS `status_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `note`
--

DROP TABLE IF EXISTS `note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `note` (
  `note_id` bigint NOT NULL,
  `DTYPE` varchar(31) DEFAULT NULL,
  `date_created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `date_generated` date DEFAULT NULL,
  `DETAIL` varchar(10000) DEFAULT NULL,
  `activity_id` bigint DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `reason_id` int DEFAULT NULL,
  `status_id` int DEFAULT NULL,
  `subject` varchar(255) DEFAULT NULL,
  `is_resolution` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`note_id`),
  KEY `FK_NOTE_created_by_id` (`created_by_id`),
  KEY `FK_NOTE_activity_id` (`activity_id`),
  KEY `FK_NOTE_reason_id` (`reason_id`),
  KEY `FK_NOTE_status_id` (`status_id`),
  CONSTRAINT `FK_NOTE_activity_id` FOREIGN KEY (`activity_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_NOTE_created_by_id` FOREIGN KEY (`created_by_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_NOTE_reason_id` FOREIGN KEY (`reason_id`) REFERENCES `reasoncreated` (`use_id`),
  CONSTRAINT `FK_NOTE_status_id` FOREIGN KEY (`status_id`) REFERENCES `activitystatus` (`status_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `personv`
--

DROP TABLE IF EXISTS `personv`;
/*!50001 DROP VIEW IF EXISTS `personv`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `personv` AS SELECT 
 1 AS `id`,
 1 AS `f_name`,
 1 AS `l_name`,
 1 AS `email1`,
 1 AS `employee_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `plandocs_customer`
--

DROP TABLE IF EXISTS `plandocs_customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_customer` (
  `customer_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `employer_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `plan_year_start` date DEFAULT NULL,
  `plan_year_end` date DEFAULT NULL,
  `effective_date` date DEFAULT NULL,
  `plan_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tokens_json` json DEFAULT NULL,
  PRIMARY KEY (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_entity`
--

DROP TABLE IF EXISTS `plandocs_entity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_entity` (
  `entity_id` bigint NOT NULL AUTO_INCREMENT,
  `organization_id` int DEFAULT NULL,
  `ein` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `employer_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_from` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unknown',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`entity_id`),
  UNIQUE KEY `uq_plandocs_entity_org` (`organization_id`),
  KEY `ix_plandocs_entity_ein` (`ein`)
) ENGINE=InnoDB AUTO_INCREMENT=2844 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_field`
--

DROP TABLE IF EXISTS `plandocs_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_field` (
  `field_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `label` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `data_type` enum('string','date','number','bool','email','phone','ein','zip','state','json') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'string',
  `is_required` tinyint(1) NOT NULL DEFAULT '0',
  `default_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `validation_regex` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `overwrite_mode` enum('ALWAYS','IF_BLANK','NEVER','ASK') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ALWAYS',
  `authoritative_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_field_policy`
--

DROP TABLE IF EXISTS `plandocs_field_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_field_policy` (
  `field_key` varchar(128) NOT NULL,
  `allow_import_override` tinyint NOT NULL DEFAULT '0',
  `notes` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_import_batch`
--

DROP TABLE IF EXISTS `plandocs_import_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_import_batch` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT,
  `source_table` varchar(64) NOT NULL,
  `triggered_by` varchar(255) DEFAULT NULL,
  `started_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` timestamp NULL DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'RUNNING',
  `notes` varchar(1000) DEFAULT NULL,
  `rows_seen` int NOT NULL DEFAULT '0',
  `entities_created` int NOT NULL DEFAULT '0',
  `entities_updated` int NOT NULL DEFAULT '0',
  `values_written` int NOT NULL DEFAULT '0',
  `values_skipped` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`batch_id`),
  KEY `idx_plandocs_import_batch_started_at` (`started_at`),
  KEY `idx_plandocs_import_batch_source` (`source_table`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_import_field_policy`
--

DROP TABLE IF EXISTS `plandocs_import_field_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_import_field_policy` (
  `import_type_id` varchar(64) NOT NULL,
  `field_key` varchar(64) NOT NULL,
  `write_mode` varchar(16) NOT NULL DEFAULT 'IF_BLANK',
  `override_manual` tinyint(1) NOT NULL DEFAULT '0',
  `notes` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`import_type_id`,`field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_import_header`
--

DROP TABLE IF EXISTS `plandocs_import_header`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_import_header` (
  `import_type_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `header_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `seen_count` int NOT NULL DEFAULT '0',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`import_type_id`,`header_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_import_map`
--

DROP TABLE IF EXISTS `plandocs_import_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_import_map` (
  `map_id` bigint NOT NULL AUTO_INCREMENT,
  `source_table` varchar(64) NOT NULL,
  `source_column` varchar(64) NOT NULL,
  `token_key` varchar(128) NOT NULL,
  `mode` varchar(32) NOT NULL,
  `transform` varchar(64) NOT NULL DEFAULT 'NONE',
  `is_active` tinyint NOT NULL DEFAULT '1',
  `notes` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`map_id`),
  UNIQUE KEY `uq_import_map` (`source_table`,`source_column`,`token_key`),
  KEY `idx_import_map_source` (`source_table`,`is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_import_mapping`
--

DROP TABLE IF EXISTS `plandocs_import_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_import_mapping` (
  `import_type_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_column` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `field_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `transform` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_customer_id` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`import_type_id`,`source_column`),
  KEY `fk_plandocs_import_mapping_field` (`field_key`),
  CONSTRAINT `fk_plandocs_import_mapping_field` FOREIGN KEY (`field_key`) REFERENCES `plandocs_field` (`field_key`),
  CONSTRAINT `fk_plandocs_import_mapping_type` FOREIGN KEY (`import_type_id`) REFERENCES `plandocs_import_type` (`import_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_import_type`
--

DROP TABLE IF EXISTS `plandocs_import_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_import_type` (
  `import_type_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`import_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_style`
--

DROP TABLE IF EXISTS `plandocs_style`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_style` (
  `style_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `published_version_id` bigint DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`style_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_style_version`
--

DROP TABLE IF EXISTS `plandocs_style_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_style_version` (
  `style_version_id` bigint NOT NULL AUTO_INCREMENT,
  `style_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `css` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`style_version_id`),
  KEY `idx_plandocs_style_version_style` (`style_id`,`created_at`),
  CONSTRAINT `fk_plandocs_style_version_style` FOREIGN KEY (`style_id`) REFERENCES `plandocs_style` (`style_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_template`
--

DROP TABLE IF EXISTS `plandocs_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_template` (
  `template_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `doc_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `plan_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `style_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `published_version_id` bigint DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`template_id`),
  KEY `idx_plandocs_template_type` (`doc_type`,`plan_type`,`is_active`),
  KEY `fk_plandocs_template_style` (`style_id`),
  CONSTRAINT `fk_plandocs_template_style` FOREIGN KEY (`style_id`) REFERENCES `plandocs_style` (`style_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_template_version`
--

DROP TABLE IF EXISTS `plandocs_template_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_template_version` (
  `template_version_id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `html` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`template_version_id`),
  KEY `idx_plandocs_template_version_template` (`template_id`,`created_at`),
  CONSTRAINT `fk_plandocs_template_version_template` FOREIGN KEY (`template_id`) REFERENCES `plandocs_template` (`template_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_value_current`
--

DROP TABLE IF EXISTS `plandocs_value_current`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_value_current` (
  `entity_id` bigint NOT NULL,
  `field_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `value_text` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `last_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_batch_id` bigint DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`entity_id`,`field_key`),
  KEY `fk_plandocs_value_field` (`field_key`),
  CONSTRAINT `fk_plandocs_value_entity` FOREIGN KEY (`entity_id`) REFERENCES `plandocs_entity` (`entity_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plandocs_value_field` FOREIGN KEY (`field_key`) REFERENCES `plandocs_field` (`field_key`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plandocs_value_history`
--

DROP TABLE IF EXISTS `plandocs_value_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plandocs_value_history` (
  `history_id` bigint NOT NULL AUTO_INCREMENT,
  `entity_id` bigint NOT NULL,
  `field_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_value` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `new_value` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `changed_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_system` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `batch_id` bigint DEFAULT NULL,
  `changed_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`history_id`),
  KEY `ix_hist_entity` (`entity_id`),
  KEY `ix_hist_field` (`field_key`),
  CONSTRAINT `fk_hist_entity` FOREIGN KEY (`entity_id`) REFERENCES `plandocs_entity` (`entity_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_hist_field` FOREIGN KEY (`field_key`) REFERENCES `plandocs_field` (`field_key`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=3534 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plantype`
--

DROP TABLE IF EXISTS `plantype`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plantype` (
  `PlanType_ID` int NOT NULL,
  `Code` varchar(255) DEFAULT NULL,
  `PlanTypeName` varchar(255) DEFAULT NULL,
  `level` varchar(20) DEFAULT NULL,
  `los` varchar(50) DEFAULT NULL,
  `employer_name` varchar(255) DEFAULT NULL,
  `billing_group_id` int DEFAULT NULL,
  `purpose_id` int DEFAULT NULL,
  PRIMARY KEY (`PlanType_ID`),
  KEY `FK_PLANTYPE_billing_group_id` (`billing_group_id`),
  KEY `FK_PLANTYPE_purpose_id` (`purpose_id`),
  CONSTRAINT `FK_PLANTYPE_billing_group_id` FOREIGN KEY (`billing_group_id`) REFERENCES `billinggroup` (`billing_group_id`),
  CONSTRAINT `FK_PLANTYPE_purpose_id` FOREIGN KEY (`purpose_id`) REFERENCES `templatepurpose` (`purpose_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `priceitem`
--

DROP TABLE IF EXISTS `priceitem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `priceitem` (
  `price_item_id` bigint NOT NULL,
  `description` varchar(100) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `SUPPRESSED` tinyint(1) DEFAULT '0',
  `psp_id` bigint DEFAULT NULL,
  `weblink_id` bigint DEFAULT NULL,
  PRIMARY KEY (`price_item_id`),
  KEY `FK_PRICEITEM_psp_id` (`psp_id`),
  KEY `FK_PRICEITEM_weblink_id` (`weblink_id`),
  CONSTRAINT `FK_PRICEITEM_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_PRICEITEM_weblink_id` FOREIGN KEY (`weblink_id`) REFERENCES `weblink` (`link_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `proposal`
--

DROP TABLE IF EXISTS `proposal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `proposal` (
  `proposal_id` bigint NOT NULL,
  `application_guid` varchar(36) DEFAULT NULL,
  `date_created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_inactive` tinyint DEFAULT NULL,
  `prospect_id` bigint NOT NULL,
  `rate_id` bigint NOT NULL,
  `status` varchar(20) DEFAULT 'CREATED',
  `created_by` bigint DEFAULT NULL,
  `date_sent` timestamp NULL DEFAULT NULL,
  `date_viewed` timestamp NULL DEFAULT NULL,
  `date_applied` timestamp NULL DEFAULT NULL,
  `source_activity_id` bigint DEFAULT NULL,
  PRIMARY KEY (`proposal_id`),
  KEY `FK_PROPOSAL_prospect_id` (`prospect_id`),
  KEY `FK_PROPOSAL_rate_id` (`rate_id`),
  CONSTRAINT `FK_PROPOSAL_prospect_id` FOREIGN KEY (`prospect_id`) REFERENCES `prospect` (`prospect_id`),
  CONSTRAINT `FK_PROPOSAL_rate_id` FOREIGN KEY (`rate_id`) REFERENCES `rate` (`rate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `proposalitems`
--

DROP TABLE IF EXISTS `proposalitems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `proposalitems` (
  `los_id` bigint NOT NULL,
  `proposal_id` bigint NOT NULL,
  PRIMARY KEY (`los_id`,`proposal_id`),
  KEY `FK_proposalitems_proposal_id` (`proposal_id`),
  CONSTRAINT `FK_proposalitems_los_id` FOREIGN KEY (`los_id`) REFERENCES `los` (`los_id`),
  CONSTRAINT `FK_proposalitems_proposal_id` FOREIGN KEY (`proposal_id`) REFERENCES `proposal` (`proposal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `prospect`
--

DROP TABLE IF EXISTS `prospect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prospect` (
  `prospect_id` bigint NOT NULL,
  `name` varchar(200) DEFAULT NULL,
  `agent_id` bigint DEFAULT NULL,
  `address_id` bigint DEFAULT NULL,
  `contact_id` bigint DEFAULT NULL,
  PRIMARY KEY (`prospect_id`),
  KEY `FK_PROSPECT_contact_id` (`contact_id`),
  KEY `FK_PROSPECT_address_id` (`address_id`),
  KEY `FK_PROSPECT_agent_id` (`agent_id`),
  CONSTRAINT `FK_PROSPECT_address_id` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`),
  CONSTRAINT `FK_PROSPECT_agent_id` FOREIGN KEY (`agent_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_PROSPECT_contact_id` FOREIGN KEY (`contact_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `psp_clients`
--

DROP TABLE IF EXISTS `psp_clients`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `psp_clients` (
  `client_id` bigint NOT NULL AUTO_INCREMENT,
  `psp_name` varchar(100) NOT NULL,
  `psp_url` varchar(255) NOT NULL,
  `api_token_outbound` varchar(64) DEFAULT NULL,
  `api_token_inbound` varchar(64) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `auto_accept_tasks` tinyint(1) NOT NULL DEFAULT '0',
  `date_requested` date DEFAULT NULL,
  `date_approved` date DEFAULT NULL,
  `date_disconnected` date DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `r_ex`
--

DROP TABLE IF EXISTS `r_ex`;
/*!50001 DROP VIEW IF EXISTS `r_ex`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `r_ex` AS SELECT 
 1 AS `benefit_id`,
 1 AS `effective_date`,
 1 AS `hasCards`,
 1 AS `active`,
 1 AS `last_renewed`,
 1 AS `next_renewal_due`,
 1 AS `plan_description`,
 1 AS `plan_name`,
 1 AS `termination_date`,
 1 AS `employer_id`,
 1 AS `plan_type_id`,
 1 AS `benid_pb`,
 1 AS `employer_name`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `rate`
--

DROP TABLE IF EXISTS `rate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rate` (
  `rate_id` bigint NOT NULL,
  `description` varchar(100) DEFAULT NULL,
  `ISSUPPRESSED` tinyint(1) DEFAULT '0',
  `psp_id` bigint DEFAULT NULL,
  PRIMARY KEY (`rate_id`),
  KEY `FK_RATE_psp_id` (`psp_id`),
  CONSTRAINT `FK_RATE_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ratediscount`
--

DROP TABLE IF EXISTS `ratediscount`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ratediscount` (
  `ratediscount_id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(200) NOT NULL,
  `discount_amount` double NOT NULL,
  `rate_id` bigint NOT NULL,
  `price_item_id` bigint NOT NULL,
  PRIMARY KEY (`ratediscount_id`),
  KEY `rate_id` (`rate_id`),
  KEY `price_item_id` (`price_item_id`),
  CONSTRAINT `ratediscount_ibfk_1` FOREIGN KEY (`rate_id`) REFERENCES `rate` (`rate_id`),
  CONSTRAINT `ratediscount_ibfk_2` FOREIGN KEY (`price_item_id`) REFERENCES `priceitem` (`price_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ratediscountlos`
--

DROP TABLE IF EXISTS `ratediscountlos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ratediscountlos` (
  `ratediscount_id` bigint NOT NULL,
  `los_id` bigint NOT NULL,
  PRIMARY KEY (`ratediscount_id`,`los_id`),
  KEY `los_id` (`los_id`),
  CONSTRAINT `ratediscountlos_ibfk_1` FOREIGN KEY (`ratediscount_id`) REFERENCES `ratediscount` (`ratediscount_id`),
  CONSTRAINT `ratediscountlos_ibfk_2` FOREIGN KEY (`los_id`) REFERENCES `los` (`los_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ratetable`
--

DROP TABLE IF EXISTS `ratetable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ratetable` (
  `price` double DEFAULT NULL,
  `module_id` bigint NOT NULL,
  `price_item_id` bigint NOT NULL,
  `rate_id` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`module_id`,`price_item_id`,`rate_id`),
  KEY `FK_RATETABLE_price_item_id` (`price_item_id`),
  KEY `FK_RATETABLE_rate_id` (`rate_id`),
  CONSTRAINT `FK_RATETABLE_module_id` FOREIGN KEY (`module_id`) REFERENCES `servicemodule` (`module_id`),
  CONSTRAINT `FK_RATETABLE_price_item_id` FOREIGN KEY (`price_item_id`) REFERENCES `priceitem` (`price_item_id`),
  CONSTRAINT `FK_RATETABLE_rate_id` FOREIGN KEY (`rate_id`) REFERENCES `rate` (`rate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reasoncreated`
--

DROP TABLE IF EXISTS `reasoncreated`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reasoncreated` (
  `use_id` int NOT NULL,
  `DESCRIPTION` varchar(100) DEFAULT NULL,
  `outbound` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`use_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recurring_days`
--

DROP TABLE IF EXISTS `recurring_days`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recurring_days` (
  `item_id` bigint NOT NULL,
  `dow_id` int NOT NULL,
  PRIMARY KEY (`item_id`,`dow_id`),
  KEY `FK_recurring_days_dow_id` (`dow_id`),
  CONSTRAINT `FK_recurring_days_dow_id` FOREIGN KEY (`dow_id`) REFERENCES `dow` (`dow_id`),
  CONSTRAINT `FK_recurring_days_item_id` FOREIGN KEY (`item_id`) REFERENCES `recurringitems` (`recurring_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recurringitems`
--

DROP TABLE IF EXISTS `recurringitems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recurringitems` (
  `recurring_id` bigint NOT NULL,
  `DATEONE` date DEFAULT NULL,
  `date_start` date DEFAULT NULL,
  `DATETWO` date DEFAULT NULL,
  `days_in_advance` int DEFAULT NULL,
  `is_inactive` tinyint(1) DEFAULT '0',
  `assignee_id` bigint DEFAULT NULL,
  `frequency_id` int DEFAULT NULL,
  `sequence_id` bigint DEFAULT NULL,
  PRIMARY KEY (`recurring_id`),
  KEY `FK_RECURRINGITEMS_assignee_id` (`assignee_id`),
  KEY `FK_RECURRINGITEMS_sequence_id` (`sequence_id`),
  KEY `FK_RECURRINGITEMS_frequency_id` (`frequency_id`),
  CONSTRAINT `FK_RECURRINGITEMS_assignee_id` FOREIGN KEY (`assignee_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_RECURRINGITEMS_frequency_id` FOREIGN KEY (`frequency_id`) REFERENCES `taskfrequency` (`frequency_id`),
  CONSTRAINT `FK_RECURRINGITEMS_sequence_id` FOREIGN KEY (`sequence_id`) REFERENCES `tasksequence` (`sequence_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `renewalitem`
--

DROP TABLE IF EXISTS `renewalitem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `renewalitem` (
  `renewal_item_id` bigint NOT NULL,
  `date_for` date DEFAULT NULL,
  `benefit_id` int DEFAULT NULL,
  `renewal_id` bigint DEFAULT NULL,
  PRIMARY KEY (`renewal_item_id`),
  KEY `FK_RENEWALITEM_benefit_id` (`benefit_id`),
  KEY `FK_RENEWALITEM_renewal_id` (`renewal_id`),
  CONSTRAINT `FK_RENEWALITEM_benefit_id` FOREIGN KEY (`benefit_id`) REFERENCES `benefit` (`benefit_id`),
  CONSTRAINT `FK_RENEWALITEM_renewal_id` FOREIGN KEY (`renewal_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resourcecategory`
--

DROP TABLE IF EXISTS `resourcecategory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resourcecategory` (
  `category_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `icon_class` varchar(50) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `psp_id` bigint NOT NULL,
  PRIMARY KEY (`category_id`),
  KEY `psp_id` (`psp_id`),
  CONSTRAINT `resourcecategory_ibfk_1` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sbenefit`
--

DROP TABLE IF EXISTS `sbenefit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sbenefit` (
  `EmployerPlan_ID` int NOT NULL,
  `CardEnabled` varchar(255) DEFAULT NULL,
  `EffectiveDate` varchar(255) DEFAULT NULL,
  `Employer_ID` varchar(255) DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `ImportPlanID` varchar(255) DEFAULT NULL,
  `LinkedtoDefaultPlan` varchar(255) DEFAULT NULL,
  `PlanDescription` varchar(255) DEFAULT NULL,
  `PlanName` varchar(255) DEFAULT NULL,
  `PlanStatus` varchar(255) DEFAULT NULL,
  `PlanType` varchar(255) DEFAULT NULL,
  `TerminationDate` varchar(255) DEFAULT NULL,
  `PlanTypeID` int DEFAULT NULL,
  `OrganizationID` int DEFAULT NULL,
  PRIMARY KEY (`EmployerPlan_ID`),
  KEY `FK_SBENEFIT_OrganizationID` (`OrganizationID`),
  KEY `FK_SBENEFIT_PlanTypeID` (`PlanTypeID`),
  CONSTRAINT `FK_SBENEFIT_OrganizationID` FOREIGN KEY (`OrganizationID`) REFERENCES `semployer` (`OrganizationID`),
  CONSTRAINT `FK_SBENEFIT_PlanTypeID` FOREIGN KEY (`PlanTypeID`) REFERENCES `plantype` (`PlanType_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sbenefittierpb`
--

DROP TABLE IF EXISTS `sbenefittierpb`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sbenefittierpb` (
  `PlanYearID` int NOT NULL,
  `EmployerID` int NOT NULL,
  `TierID` varchar(255) NOT NULL,
  `PBBenefitID` int NOT NULL,
  `Amount` varchar(255) DEFAULT NULL,
  `BenefitID` int DEFAULT NULL,
  `Carrier` varchar(255) DEFAULT NULL,
  `EffectiveDate` varchar(255) DEFAULT NULL,
  `Employer` varchar(255) DEFAULT NULL,
  `EndDate` varchar(255) DEFAULT NULL,
  `Fee` varchar(255) DEFAULT NULL,
  `Gender` varchar(255) DEFAULT NULL,
  `LastDayofCoverage` varchar(255) DEFAULT NULL,
  `PBType` varchar(255) DEFAULT NULL,
  `PBTypeID` varchar(255) DEFAULT NULL,
  `BenefitName` varchar(255) DEFAULT NULL,
  `ImportPlanID` varchar(255) DEFAULT NULL,
  `RemitTo` varchar(255) DEFAULT NULL,
  `Smoker` varchar(255) DEFAULT NULL,
  `StartDate` varchar(255) DEFAULT NULL,
  `TierAge` varchar(255) DEFAULT NULL,
  `TierName` varchar(255) DEFAULT NULL,
  `TPA` varchar(255) DEFAULT NULL,
  `Type` varchar(255) DEFAULT NULL,
  `PlanTypeID` int DEFAULT NULL,
  `OrganizationID` int DEFAULT NULL,
  PRIMARY KEY (`PlanYearID`,`EmployerID`,`TierID`,`PBBenefitID`),
  KEY `FK_SBENEFITTIERPB_OrganizationID` (`OrganizationID`),
  KEY `FK_SBENEFITTIERPB_PlanTypeID` (`PlanTypeID`),
  CONSTRAINT `FK_SBENEFITTIERPB_OrganizationID` FOREIGN KEY (`OrganizationID`) REFERENCES `semployer` (`OrganizationID`),
  CONSTRAINT `FK_SBENEFITTIERPB_PlanTypeID` FOREIGN KEY (`PlanTypeID`) REFERENCES `plantype` (`PlanType_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sbenefityear`
--

DROP TABLE IF EXISTS `sbenefityear`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sbenefityear` (
  `EmployerPlanDetailForPlanYear_ID` int NOT NULL,
  `ContributionSchedule` varchar(255) DEFAULT NULL,
  `ContributionScheduleTemplate_ID` varchar(255) DEFAULT NULL,
  `Employer` varchar(255) DEFAULT NULL,
  `Organization_ID` varchar(255) DEFAULT NULL,
  `PlanDescription` varchar(255) DEFAULT NULL,
  `PlanName` varchar(255) DEFAULT NULL,
  `PlanStatus` varchar(255) DEFAULT NULL,
  `PlanYear` varchar(255) DEFAULT NULL,
  `PlanYear_ID` int DEFAULT NULL,
  `EmployerPlan_ID` int DEFAULT NULL,
  PRIMARY KEY (`EmployerPlanDetailForPlanYear_ID`),
  KEY `FK_SBENEFITYEAR_EmployerPlan_ID` (`EmployerPlan_ID`),
  CONSTRAINT `FK_SBENEFITYEAR_EmployerPlan_ID` FOREIGN KEY (`EmployerPlan_ID`) REFERENCES `sbenefit` (`EmployerPlan_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schema_version`
--

DROP TABLE IF EXISTS `schema_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schema_version` (
  `version` varchar(10) NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `script_name` varchar(200) DEFAULT NULL,
  `applied_on` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scobraqb`
--

DROP TABLE IF EXISTS `scobraqb`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scobraqb` (
  `EmployerSystemId` int DEFAULT NULL,
  `EmployerCustomId` varchar(45) DEFAULT NULL,
  `EmployerName` varchar(45) DEFAULT NULL,
  `EmployerPlanSystemID` int NOT NULL,
  `ImportPlanID` varchar(45) DEFAULT NULL,
  `EmployerPlanName` varchar(45) DEFAULT NULL,
  `EmployerDivisionID` varchar(45) DEFAULT NULL,
  `EmployerDivision` varchar(45) DEFAULT NULL,
  `IsIndividuallyRated` varchar(45) DEFAULT NULL,
  `EmployerPlanTierName` varchar(45) DEFAULT NULL,
  `UserId` int DEFAULT NULL,
  `Relationship` varchar(45) DEFAULT NULL,
  `ParticipantSystemId` int NOT NULL,
  `ParticipantCustomId` varchar(45) DEFAULT NULL,
  `ParticipantName` varchar(45) DEFAULT NULL,
  `ParticipantFirstName` varchar(45) DEFAULT NULL,
  `ParticipantLastName` varchar(45) DEFAULT NULL,
  `ParticipantMiddleName` varchar(45) DEFAULT NULL,
  `IsDependent` varchar(45) DEFAULT NULL,
  `CoveredMemberName` varchar(45) DEFAULT NULL,
  `CoveredMemberFirstName` varchar(45) DEFAULT NULL,
  `CoveredMemberLastName` varchar(45) DEFAULT NULL,
  `CoveredMemberMiddleName` varchar(45) DEFAULT NULL,
  `ParticipantAddress1` varchar(45) DEFAULT NULL,
  `ParticipantAddress2` varchar(45) DEFAULT NULL,
  `ParticipantCity` varchar(45) DEFAULT NULL,
  `ParticipantState` varchar(45) DEFAULT NULL,
  `ParticipantZipCode` varchar(45) DEFAULT NULL,
  `ParticipantEmail` varchar(45) DEFAULT NULL,
  `ParticipantPhone` varchar(45) DEFAULT NULL,
  `SSN` varchar(45) DEFAULT NULL,
  `DependentSystemID` varchar(45) DEFAULT NULL,
  `DependentCustomID` varchar(45) DEFAULT NULL,
  `DependentFirstName` varchar(45) DEFAULT NULL,
  `DependentLastName` varchar(45) DEFAULT NULL,
  `DependentMiddleName` varchar(45) DEFAULT NULL,
  `DependentRelationship` varchar(45) DEFAULT NULL,
  `DependentParticipantSystemID` varchar(45) DEFAULT NULL,
  `QualifyingEventReason` varchar(45) DEFAULT NULL,
  `QualifyingEventDate` varchar(45) DEFAULT NULL,
  `CoverageStatus` varchar(45) DEFAULT NULL,
  `CoverageStatusEffectiveDate` varchar(45) DEFAULT NULL,
  `LastDayOfCoverage` varchar(45) DEFAULT NULL,
  `LastDayToAccept` varchar(45) DEFAULT NULL,
  `CobraAccepted` varchar(45) DEFAULT NULL,
  `AcceptedEntered` varchar(45) DEFAULT NULL,
  `CobraAcceptedDate` varchar(45) DEFAULT NULL,
  `CobraStartDate` varchar(45) DEFAULT NULL,
  `CobraTermed` varchar(45) DEFAULT NULL,
  `TermedEntered` varchar(45) DEFAULT NULL,
  `Subsidy` varchar(45) DEFAULT NULL,
  `TransactionStartDate` varchar(45) DEFAULT NULL,
  `TransactionEndDate` varchar(45) DEFAULT NULL,
  `IsByDivision` varchar(45) DEFAULT NULL,
  `CoveredMembersNames` varchar(1005) DEFAULT NULL,
  `PBBillingFrequency` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`EmployerPlanSystemID`,`ParticipantSystemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scoveragepb`
--

DROP TABLE IF EXISTS `scoveragepb`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scoveragepb` (
  `PBCoverageHeaderID` int NOT NULL,
  `BenefitID` int DEFAULT NULL,
  `BenefitName` varchar(255) DEFAULT NULL,
  `CoverageStatus` varchar(255) DEFAULT NULL,
  `EffectiveDate` varchar(255) DEFAULT NULL,
  `EmployerCustomID` varchar(255) DEFAULT NULL,
  `EmployerDivision` varchar(255) DEFAULT NULL,
  `EmployerDivisionID` varchar(255) DEFAULT NULL,
  `EmployerID` int DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `ImportPlanID` varchar(255) DEFAULT NULL,
  `ParticipantCustomID` varchar(255) DEFAULT NULL,
  `ParticipantFirstName` varchar(255) DEFAULT NULL,
  `ParticipantLastName` varchar(255) DEFAULT NULL,
  `PlanType` varchar(255) DEFAULT NULL,
  `SSN` varchar(255) DEFAULT NULL,
  `TierName` varchar(255) DEFAULT NULL,
  `TransactionEndDate` varchar(255) DEFAULT NULL,
  `TransactionStartDate` varchar(255) DEFAULT NULL,
  `Participant_ID` int DEFAULT NULL,
  PRIMARY KEY (`PBCoverageHeaderID`),
  KEY `FK_SCOVERAGEPB_Participant_ID` (`Participant_ID`),
  CONSTRAINT `FK_SCOVERAGEPB_Participant_ID` FOREIGN KEY (`Participant_ID`) REFERENCES `semployee` (`Participant_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `semployee`
--

DROP TABLE IF EXISTS `semployee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semployee` (
  `Participant_ID` int NOT NULL,
  `Address1` varchar(255) DEFAULT NULL,
  `Address2` varchar(255) DEFAULT NULL,
  `City` varchar(255) DEFAULT NULL,
  `ParticipantCustomID` varchar(255) DEFAULT NULL,
  `Email` varchar(255) DEFAULT NULL,
  `EmployerCustomID` varchar(255) DEFAULT NULL,
  `Employer_ID` varchar(255) DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `FailedLoginCount` varchar(255) DEFAULT NULL,
  `FirstName` varchar(255) DEFAULT NULL,
  `IsRegisteredToPortal` varchar(255) DEFAULT NULL,
  `LastLoginDate` varchar(255) DEFAULT NULL,
  `LastName` varchar(255) DEFAULT NULL,
  `SetupCompletionDate` varchar(255) DEFAULT NULL,
  `State` varchar(255) DEFAULT NULL,
  `User_ID` varchar(255) DEFAULT NULL,
  `UserStatus` varchar(255) DEFAULT NULL,
  `ZipCode` varchar(255) DEFAULT NULL,
  `Organization_ID` int DEFAULT NULL,
  PRIMARY KEY (`Participant_ID`),
  KEY `FK_SEMPLOYEE_Organization_ID` (`Organization_ID`),
  CONSTRAINT `FK_SEMPLOYEE_Organization_ID` FOREIGN KEY (`Organization_ID`) REFERENCES `semployer` (`OrganizationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `semployee2`
--

DROP TABLE IF EXISTS `semployee2`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semployee2` (
  `Participant_ID` int NOT NULL,
  `EffectiveDate` varchar(45) DEFAULT NULL,
  `TerminationDate` varchar(45) DEFAULT NULL,
  `UserId` int DEFAULT NULL,
  `UserStatus` varchar(45) DEFAULT NULL,
  `CreatedDate` varchar(45) DEFAULT NULL,
  `HireDate` varchar(45) DEFAULT NULL,
  `Organization_ID` int DEFAULT NULL,
  `ParticipantStatusId` int DEFAULT NULL,
  `Name` varchar(45) DEFAULT NULL,
  `ERName` varchar(45) DEFAULT NULL,
  `ParticipantName` varchar(45) DEFAULT NULL,
  `Participant_Last` varchar(45) DEFAULT NULL,
  `Participant_First` varchar(45) DEFAULT NULL,
  `SSN` varchar(45) DEFAULT NULL,
  `userStatusID` int DEFAULT NULL,
  `DOB` varchar(45) DEFAULT NULL,
  `ReimbursementMethod` varchar(45) DEFAULT NULL,
  `EmploymentStatus` varchar(45) DEFAULT NULL,
  `EmploymentStatusID` int DEFAULT NULL,
  `No_of_participants` varchar(45) DEFAULT NULL,
  `DivisionName` varchar(45) DEFAULT NULL,
  `Bankname` varchar(45) DEFAULT NULL,
  `RoutingNo` varchar(45) DEFAULT NULL,
  `AccountType` varchar(45) DEFAULT NULL,
  `AccountNumber` varchar(45) DEFAULT NULL,
  `UserBank_ID` varchar(45) DEFAULT NULL,
  `ReimbursementMethod_ID` varchar(45) DEFAULT NULL,
  `ByDivision` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`Participant_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `semployer`
--

DROP TABLE IF EXISTS `semployer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semployer` (
  `OrganizationID` int NOT NULL,
  `Agency` varchar(255) DEFAULT NULL,
  `AgencyOrganizationID` varchar(255) DEFAULT NULL,
  `CreatedByUser` varchar(255) DEFAULT NULL,
  `CreatedByUser1` varchar(255) DEFAULT NULL,
  `CreatedDate` varchar(255) DEFAULT NULL,
  `CreatedUser` varchar(255) DEFAULT NULL,
  `CustomID` varchar(255) DEFAULT NULL,
  `Email` varchar(255) DEFAULT NULL,
  `Employer_ID` int DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `ImplementationLead` varchar(255) DEFAULT NULL,
  `ImplementationLeadUserID` varchar(255) DEFAULT NULL,
  `IsSetUpCompleted` varchar(255) DEFAULT NULL,
  `OrganizationStatusID` varchar(255) DEFAULT NULL,
  `Phone` varchar(255) DEFAULT NULL,
  `PhoneNumber` varchar(255) DEFAULT NULL,
  `PrimaryContact` varchar(255) DEFAULT NULL,
  `SetUpComplete` varchar(255) DEFAULT NULL,
  `SetUpCompletionDate` varchar(255) DEFAULT NULL,
  `Setup` varchar(255) DEFAULT NULL,
  `Status` varchar(255) DEFAULT NULL,
  `TaxID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`OrganizationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `semployer2`
--

DROP TABLE IF EXISTS `semployer2`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semployer2` (
  `OrganizationID` int NOT NULL,
  `Agency` varchar(255) DEFAULT NULL,
  `AgencyOrganizationID` varchar(255) DEFAULT NULL,
  `CreatedByUser` varchar(255) DEFAULT NULL,
  `CreatedByUser1` varchar(255) DEFAULT NULL,
  `CreatedDate` varchar(255) DEFAULT NULL,
  `CreatedUser` varchar(255) DEFAULT NULL,
  `CustomID` varchar(255) DEFAULT NULL,
  `Email` varchar(255) DEFAULT NULL,
  `Employer_ID` int DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `ImplementationLead` varchar(255) DEFAULT NULL,
  `ImplementationLeadUserID` varchar(255) DEFAULT NULL,
  `IsSetUpCompleted` varchar(255) DEFAULT NULL,
  `OrganizationStatusID` varchar(255) DEFAULT NULL,
  `Phone` varchar(255) DEFAULT NULL,
  `PhoneNumber` varchar(255) DEFAULT NULL,
  `PrimaryContact` varchar(255) DEFAULT NULL,
  `SetUpComplete` varchar(255) DEFAULT NULL,
  `SetUpCompletionDate` varchar(255) DEFAULT NULL,
  `Setup` varchar(255) DEFAULT NULL,
  `Status` varchar(255) DEFAULT NULL,
  `TaxID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`OrganizationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `senrollment`
--

DROP TABLE IF EXISTS `senrollment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `senrollment` (
  `ParticipantPlan_ID` int NOT NULL,
  `AccountBalance` varchar(255) DEFAULT NULL,
  `ActiveParticipantCount` varchar(255) DEFAULT NULL,
  `AvailableBalance` varchar(255) DEFAULT NULL,
  `CarryoverAmount` varchar(255) DEFAULT NULL,
  `CoverageEndDate` varchar(255) DEFAULT NULL,
  `CustomID` varchar(255) DEFAULT NULL,
  `DisbursableBalance` varchar(255) DEFAULT NULL,
  `DivisionCustomID` varchar(255) DEFAULT NULL,
  `DivisionOrganizationID` varchar(255) DEFAULT NULL,
  `DivisionOrganizationName` varchar(255) DEFAULT NULL,
  `ElectionAmount` varchar(255) DEFAULT NULL,
  `EmployerID` varchar(255) DEFAULT NULL,
  `EmployerName` varchar(255) DEFAULT NULL,
  `EmployerPlan_ID` varchar(255) DEFAULT NULL,
  `EmployerYTDContribution` varchar(255) DEFAULT NULL,
  `FirstName` varchar(255) DEFAULT NULL,
  `IsByDivision` varchar(255) DEFAULT NULL,
  `IsCarryOverEnable` varchar(255) DEFAULT NULL,
  `LastName` varchar(255) DEFAULT NULL,
  `OrganizationID` varchar(255) DEFAULT NULL,
  `ParticipantCount` varchar(255) DEFAULT NULL,
  `ParticipantYTDContribution` varchar(255) DEFAULT NULL,
  `PendingCardTransaction` varchar(255) DEFAULT NULL,
  `PlanDescription` varchar(255) DEFAULT NULL,
  `PlanName` varchar(255) DEFAULT NULL,
  `PlanStatus` varchar(255) DEFAULT NULL,
  `PlanType` varchar(255) DEFAULT NULL,
  `PlanYear` varchar(255) DEFAULT NULL,
  `SSN` varchar(255) DEFAULT NULL,
  `Eff_TermDate` varchar(255) DEFAULT NULL,
  `YTDClaim` varchar(255) DEFAULT NULL,
  `YtdDCReturn` varchar(255) DEFAULT NULL,
  `YTDPayments` varchar(255) DEFAULT NULL,
  `YtdReturn` varchar(255) DEFAULT NULL,
  `EmployerPlanDetailForPlanYearID` int DEFAULT NULL,
  `Participant_ID` int DEFAULT NULL,
  PRIMARY KEY (`ParticipantPlan_ID`),
  KEY `FK_SENROLLMENT_EmployerPlanDetailForPlanYearID` (`EmployerPlanDetailForPlanYearID`),
  KEY `FK_SENROLLMENT_Participant_ID` (`Participant_ID`),
  CONSTRAINT `FK_SENROLLMENT_EmployerPlanDetailForPlanYearID` FOREIGN KEY (`EmployerPlanDetailForPlanYearID`) REFERENCES `sbenefityear` (`EmployerPlanDetailForPlanYear_ID`),
  CONSTRAINT `FK_SENROLLMENT_Participant_ID` FOREIGN KEY (`Participant_ID`) REFERENCES `semployee` (`Participant_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sequence`
--

DROP TABLE IF EXISTS `sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sequence` (
  `SEQ_NAME` varchar(50) NOT NULL,
  `SEQ_COUNT` decimal(38,0) DEFAULT NULL,
  PRIMARY KEY (`SEQ_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `serviceitem`
--

DROP TABLE IF EXISTS `serviceitem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `serviceitem` (
  `service_item_id` bigint NOT NULL,
  `bullet_point` varchar(200) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `SUPPRESSED` tinyint(1) DEFAULT '0',
  `psp_id` bigint DEFAULT NULL,
  PRIMARY KEY (`service_item_id`),
  KEY `FK_SERVICEITEM_psp_id` (`psp_id`),
  CONSTRAINT `FK_SERVICEITEM_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `servicemodule`
--

DROP TABLE IF EXISTS `servicemodule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `servicemodule` (
  `module_id` bigint NOT NULL,
  `app_select` tinyint(1) DEFAULT '0',
  `description` varchar(100) DEFAULT NULL,
  `short_text` varchar(20) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `SUPPRESSED` tinyint(1) DEFAULT '0',
  `psp_id` bigint DEFAULT NULL,
  `los_id` bigint DEFAULT NULL,
  `enhancement_id` bigint DEFAULT NULL,
  PRIMARY KEY (`module_id`),
  KEY `FK_SERVICEMODULE_psp_id` (`psp_id`),
  KEY `fk_sm_los` (`los_id`),
  KEY `fk_sm_enhancement` (`enhancement_id`),
  CONSTRAINT `FK_SERVICEMODULE_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `fk_sm_enhancement` FOREIGN KEY (`enhancement_id`) REFERENCES `enhancement` (`enhancement_id`),
  CONSTRAINT `fk_sm_los` FOREIGN KEY (`los_id`) REFERENCES `los` (`los_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `setupcontacts`
--

DROP TABLE IF EXISTS `setupcontacts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setupcontacts` (
  `setup_id` bigint NOT NULL,
  `person_id` bigint NOT NULL,
  PRIMARY KEY (`setup_id`,`person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task`
--

DROP TABLE IF EXISTS `task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task` (
  `task_id` bigint NOT NULL,
  `DESCRIPTION` varchar(200) DEFAULT NULL,
  `reusable` tinyint(1) DEFAULT '1',
  `psp_id` bigint DEFAULT NULL,
  `has_automation` tinyint(1) DEFAULT '0',
  `servlet_name` varchar(200) DEFAULT NULL,
  `automation_text` varchar(500) DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `has_owner` tinyint(1) DEFAULT '0',
  `has_goto` tinyint(1) DEFAULT '0',
  `has_info` tinyint(1) DEFAULT '0',
  `is_sourced` tinyint(1) DEFAULT '0',
  `goto_link_id` bigint DEFAULT NULL,
  `info_link_id` bigint DEFAULT NULL,
  `allow_non_owner` tinyint(1) DEFAULT '1',
  `allow_early` tinyint(1) DEFAULT '1',
  `allow_future` tinyint(1) DEFAULT '1',
  `auto_id` bigint DEFAULT NULL,
  `task_guid` varchar(36) NOT NULL,
  `bpo_registration_id` bigint DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uq_task_guid` (`task_guid`),
  KEY `FK_TASK_psp_id` (`psp_id`),
  KEY `fk_task_bpo_registration` (`bpo_registration_id`),
  CONSTRAINT `fk_task_bpo_registration` FOREIGN KEY (`bpo_registration_id`) REFERENCES `bpo_registration` (`bpo_reg_id`),
  CONSTRAINT `FK_TASK_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_links`
--

DROP TABLE IF EXISTS `task_links`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_links` (
  `task_id` bigint NOT NULL,
  `link_id` bigint NOT NULL,
  PRIMARY KEY (`task_id`,`link_id`),
  KEY `FK_task_links_link_id` (`link_id`),
  CONSTRAINT `FK_task_links_link_id` FOREIGN KEY (`link_id`) REFERENCES `weblink` (`link_id`),
  CONSTRAINT `FK_task_links_task_id` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `taskfrequency`
--

DROP TABLE IF EXISTS `taskfrequency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `taskfrequency` (
  `frequency_id` int NOT NULL,
  `description` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`frequency_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tasksequence`
--

DROP TABLE IF EXISTS `tasksequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasksequence` (
  `sequence_id` bigint NOT NULL,
  `DTYPE` varchar(31) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `is_inactive` tinyint(1) DEFAULT '0',
  `psp_id` bigint DEFAULT NULL,
  `date_start` date DEFAULT NULL,
  `days_in_advance` int DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `frequency_id` int DEFAULT NULL,
  `unique_id` varchar(255) DEFAULT NULL,
  `purpose_id` int DEFAULT NULL,
  PRIMARY KEY (`sequence_id`),
  KEY `FK_TASKSEQUENCE_purpose_id` (`purpose_id`),
  KEY `FK_TASKSEQUENCE_frequency_id` (`frequency_id`),
  KEY `FK_TASKSEQUENCE_psp_id` (`psp_id`),
  KEY `FK_TASKSEQUENCE_user_id` (`user_id`),
  CONSTRAINT `FK_TASKSEQUENCE_frequency_id` FOREIGN KEY (`frequency_id`) REFERENCES `taskfrequency` (`frequency_id`),
  CONSTRAINT `FK_TASKSEQUENCE_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_TASKSEQUENCE_purpose_id` FOREIGN KEY (`purpose_id`) REFERENCES `templatepurpose` (`purpose_id`),
  CONSTRAINT `FK_TASKSEQUENCE_user_id` FOREIGN KEY (`user_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tasksequencetable`
--

DROP TABLE IF EXISTS `tasksequencetable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasksequencetable` (
  `sort_order` int DEFAULT NULL,
  `task_id` bigint NOT NULL,
  `sequence_id` bigint NOT NULL,
  PRIMARY KEY (`task_id`,`sequence_id`),
  KEY `FK_TASKSEQUENCETABLE_sequence_id` (`sequence_id`),
  CONSTRAINT `FK_TASKSEQUENCETABLE_sequence_id` FOREIGN KEY (`sequence_id`) REFERENCES `tasksequence` (`sequence_id`),
  CONSTRAINT `FK_TASKSEQUENCETABLE_task_id` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `td_all_open`
--

DROP TABLE IF EXISTS `td_all_open`;
/*!50001 DROP VIEW IF EXISTS `td_all_open`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `td_all_open` AS SELECT 
 1 AS `todo_id`,
 1 AS `date_completed`,
 1 AS `is_complete`,
 1 AS `sort_order`,
 1 AS `checklist_id`,
 1 AS `completed_by_id`,
 1 AS `task_id`,
 1 AS `allow_future`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `td_base_01`
--

DROP TABLE IF EXISTS `td_base_01`;
/*!50001 DROP VIEW IF EXISTS `td_base_01`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `td_base_01` AS SELECT 
 1 AS `todo_id`,
 1 AS `date_completed`,
 1 AS `is_complete`,
 1 AS `sort_order`,
 1 AS `checklist_id`,
 1 AS `completed_by_id`,
 1 AS `task_id`,
 1 AS `allow_future`,
 1 AS `min_sort`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `td_base_02`
--

DROP TABLE IF EXISTS `td_base_02`;
/*!50001 DROP VIEW IF EXISTS `td_base_02`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `td_base_02` AS SELECT 
 1 AS `todo_id`,
 1 AS `date_completed`,
 1 AS `is_complete`,
 1 AS `sort_order`,
 1 AS `checklist_id`,
 1 AS `completed_by_id`,
 1 AS `task_id`,
 1 AS `allow_future`,
 1 AS `min_sort_top`,
 1 AS `min_sort_block`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `td_base_03`
--

DROP TABLE IF EXISTS `td_base_03`;
/*!50001 DROP VIEW IF EXISTS `td_base_03`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `td_base_03` AS SELECT 
 1 AS `todo_id`,
 1 AS `date_completed`,
 1 AS `is_complete`,
 1 AS `sort_order`,
 1 AS `checklist_id`,
 1 AS `completed_by_id`,
 1 AS `task_id`,
 1 AS `allow_future`,
 1 AS `min_sort_top`,
 1 AS `min_sort_block`,
 1 AS `owner_id`,
 1 AS `is_sourced`,
 1 AS `bpo_registration_id`,
 1 AS `allow_early`,
 1 AS `has_owner`,
 1 AS `auto_id`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `has_automation`,
 1 AS `allow_non_owner`,
 1 AS `name`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `td_base_04`
--

DROP TABLE IF EXISTS `td_base_04`;
/*!50001 DROP VIEW IF EXISTS `td_base_04`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `td_base_04` AS SELECT 
 1 AS `todo_id`,
 1 AS `task_id`,
 1 AS `has_owner`,
 1 AS `is_sourced`,
 1 AS `owner_id`,
 1 AS `allow_non_owner`,
 1 AS `allow_early`,
 1 AS `allow_future`,
 1 AS `sort_order`,
 1 AS `min_sort_top`,
 1 AS `min_sort_block`,
 1 AS `checklist_id`,
 1 AS `checklist_name`,
 1 AS `is_checklist_complete`,
 1 AS `checklist_assigned_to_id`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `auto_id`,
 1 AS `has_automation`,
 1 AS `bpo_registration_id`,
 1 AS `name`,
 1 AS `checklist_due_date`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `td_base_05`
--

DROP TABLE IF EXISTS `td_base_05`;
/*!50001 DROP VIEW IF EXISTS `td_base_05`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `td_base_05` AS SELECT 
 1 AS `todo_id`,
 1 AS `task_id`,
 1 AS `name`,
 1 AS `has_owner`,
 1 AS `is_sourced`,
 1 AS `owner_id`,
 1 AS `allow_non_owner`,
 1 AS `allow_early`,
 1 AS `allow_future`,
 1 AS `sort_order`,
 1 AS `min_sort_top`,
 1 AS `at_top`,
 1 AS `min_sort_block`,
 1 AS `is_blocked`,
 1 AS `checklist_id`,
 1 AS `checklist_name`,
 1 AS `is_checklist_complete`,
 1 AS `checklist_due_date`,
 1 AS `checklist_assigned_to_id`,
 1 AS `DTYPE`,
 1 AS `checklist_owner`,
 1 AS `is_activity_complete`,
 1 AS `activity_owner_id`,
 1 AS `is_activity`,
 1 AS `activity_due_date`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `auto_id`,
 1 AS `has_automation`,
 1 AS `bpo_registration_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `td_count_1`
--

DROP TABLE IF EXISTS `td_count_1`;
/*!50001 DROP VIEW IF EXISTS `td_count_1`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `td_count_1` AS SELECT 
 1 AS `todo_count`,
 1 AS `checklist_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `td_top_block`
--

DROP TABLE IF EXISTS `td_top_block`;
/*!50001 DROP VIEW IF EXISTS `td_top_block`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `td_top_block` AS SELECT 
 1 AS `id`,
 1 AS `min_sort`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `td_top_block_a`
--

DROP TABLE IF EXISTS `td_top_block_a`;
/*!50001 DROP VIEW IF EXISTS `td_top_block_a`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `td_top_block_a` AS SELECT 
 1 AS `id`,
 1 AS `allow_future`,
 1 AS `sort_order`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `td_top_item`
--

DROP TABLE IF EXISTS `td_top_item`;
/*!50001 DROP VIEW IF EXISTS `td_top_item`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `td_top_item` AS SELECT 
 1 AS `id`,
 1 AS `min_sort`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `tdx_open_activity`
--

DROP TABLE IF EXISTS `tdx_open_activity`;
/*!50001 DROP VIEW IF EXISTS `tdx_open_activity`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `tdx_open_activity` AS SELECT 
 1 AS `todo_id`,
 1 AS `task_id`,
 1 AS `name`,
 1 AS `not_me`,
 1 AS `has_owner`,
 1 AS `is_sourced`,
 1 AS `owner_id`,
 1 AS `allow_non_owner`,
 1 AS `allow_early`,
 1 AS `allow_future`,
 1 AS `sort_order`,
 1 AS `min_sort_top`,
 1 AS `at_top`,
 1 AS `min_sort_block`,
 1 AS `is_blocked`,
 1 AS `checklist_id`,
 1 AS `checklist_name`,
 1 AS `checklist_owner`,
 1 AS `is_checklist_complete`,
 1 AS `checklist_due_date`,
 1 AS `checklist_assigned_to_id`,
 1 AS `DTYPE`,
 1 AS `is_activity`,
 1 AS `is_activity_complete`,
 1 AS `activity_owner_id`,
 1 AS `activity_due_date`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `auto_id`,
 1 AS `has_automation`,
 1 AS `bpo_registration_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `tdx_open_list`
--

DROP TABLE IF EXISTS `tdx_open_list`;
/*!50001 DROP VIEW IF EXISTS `tdx_open_list`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `tdx_open_list` AS SELECT 
 1 AS `todo_id`,
 1 AS `task_id`,
 1 AS `name`,
 1 AS `has_owner`,
 1 AS `is_sourced`,
 1 AS `owner_id`,
 1 AS `allow_non_owner`,
 1 AS `allow_early`,
 1 AS `allow_future`,
 1 AS `sort_order`,
 1 AS `min_sort_top`,
 1 AS `at_top`,
 1 AS `min_sort_block`,
 1 AS `is_blocked`,
 1 AS `checklist_id`,
 1 AS `checklist_name`,
 1 AS `is_checklist_complete`,
 1 AS `checklist_due_date`,
 1 AS `checklist_assigned_to_id`,
 1 AS `DTYPE`,
 1 AS `is_activity_complete`,
 1 AS `activity_owner_id`,
 1 AS `checklist_owner`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `auto_id`,
 1 AS `has_automation`,
 1 AS `bpo_registration_id`,
 1 AS `not_me`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `templategroup`
--

DROP TABLE IF EXISTS `templategroup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `templategroup` (
  `group_id` int NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `templatepurpose`
--

DROP TABLE IF EXISTS `templatepurpose`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `templatepurpose` (
  `purpose_id` int NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `group_id` int DEFAULT NULL,
  `psp_id` bigint DEFAULT NULL,
  `is_suppressed` tinyint(1) NOT NULL DEFAULT '0',
  `provider_ref` varchar(100) DEFAULT NULL,
  `code` varchar(20) DEFAULT NULL,
  `source_type` varchar(20) DEFAULT NULL,
  `default_renewal_months` int DEFAULT NULL,
  `has_required_tasks` tinyint(1) NOT NULL DEFAULT '1',
  `category_id` bigint DEFAULT NULL,
  PRIMARY KEY (`purpose_id`),
  KEY `FK_TEMPLATEPURPOSE_group_id` (`group_id`),
  KEY `FK_TEMPLATEPURPOSE_psp_id` (`psp_id`),
  KEY `FK_TEMPLATEPURPOSE_category_id` (`category_id`),
  CONSTRAINT `FK_TEMPLATEPURPOSE_category_id` FOREIGN KEY (`category_id`) REFERENCES `ticketcategory` (`category_id`),
  CONSTRAINT `FK_TEMPLATEPURPOSE_group_id` FOREIGN KEY (`group_id`) REFERENCES `templategroup` (`group_id`),
  CONSTRAINT `FK_TEMPLATEPURPOSE_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `test_01`
--

DROP TABLE IF EXISTS `test_01`;
/*!50001 DROP VIEW IF EXISTS `test_01`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `test_01` AS SELECT 
 1 AS `id`,
 1 AS `DTYPE`,
 1 AS `full_name`,
 1 AS `employee_id`,
 1 AS `date_completed`,
 1 AS `due_date`,
 1 AS `is_complete`,
 1 AS `assigned_to_id`,
 1 AS `completed_by_id`,
 1 AS `created_by_id`,
 1 AS `employer_id`,
 1 AS `checklist_id`,
 1 AS `max_note`,
 1 AS `activity_id`,
 1 AS `status_id`,
 1 AS `max_note1`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `ticketcategory`
--

DROP TABLE IF EXISTS `ticketcategory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ticketcategory` (
  `category_id` bigint NOT NULL,
  `DESCRIPTION` varchar(200) DEFAULT NULL,
  `short_text` varchar(255) DEFAULT NULL,
  `active` tinyint DEFAULT '0',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `time_correction_request`
--

DROP TABLE IF EXISTS `time_correction_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `time_correction_request` (
  `request_id` bigint NOT NULL AUTO_INCREMENT,
  `requestor_id` bigint NOT NULL,
  `in_log_id` bigint NOT NULL,
  `out_log_id` bigint DEFAULT NULL,
  `original_date` date NOT NULL,
  `original_in_time` time NOT NULL,
  `original_out_time` time DEFAULT NULL,
  `requested_in_time` time DEFAULT NULL,
  `requested_out_time` time DEFAULT NULL,
  `request_note` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `date_requested` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewer_id` bigint DEFAULT NULL,
  `review_comment` varchar(500) DEFAULT NULL,
  `date_reviewed` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`request_id`),
  KEY `reviewer_id` (`reviewer_id`),
  KEY `in_log_id` (`in_log_id`),
  KEY `out_log_id` (`out_log_id`),
  KEY `idx_tcr_status` (`status`),
  KEY `idx_tcr_requestor` (`requestor_id`),
  KEY `idx_tcr_date` (`original_date`),
  CONSTRAINT `time_correction_request_ibfk_1` FOREIGN KEY (`requestor_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `time_correction_request_ibfk_2` FOREIGN KEY (`reviewer_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `time_correction_request_ibfk_3` FOREIGN KEY (`in_log_id`) REFERENCES `timelog` (`log_id`),
  CONSTRAINT `time_correction_request_ibfk_4` FOREIGN KEY (`out_log_id`) REFERENCES `timelog` (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `timelog`
--

DROP TABLE IF EXISTS `timelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `timelog` (
  `log_id` bigint NOT NULL,
  `is_in` tinyint(1) DEFAULT '0',
  `date` date DEFAULT NULL,
  `time` time DEFAULT NULL,
  `person_id` bigint DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `FK_TIMELOG_person_id` (`person_id`),
  CONSTRAINT `FK_TIMELOG_person_id` FOREIGN KEY (`person_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `todo`
--

DROP TABLE IF EXISTS `todo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `todo` (
  `todo_id` bigint NOT NULL,
  `date_completed` date DEFAULT NULL,
  `is_complete` tinyint(1) DEFAULT '0',
  `bpo_completed` tinyint(1) NOT NULL DEFAULT '0',
  `bpo_completed_date` date DEFAULT NULL,
  `bpo_completed_by_id` bigint DEFAULT NULL,
  `bpo_assigned_to_id` bigint DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `checklist_id` bigint DEFAULT NULL,
  `completed_by_id` bigint DEFAULT NULL,
  `task_id` bigint DEFAULT NULL,
  `is_reverted` tinyint(1) NOT NULL DEFAULT '0',
  `todo_guid` varchar(36) NOT NULL,
  PRIMARY KEY (`todo_id`),
  UNIQUE KEY `uq_todo_guid` (`todo_guid`),
  KEY `FK_TODO_task_id` (`task_id`),
  KEY `FK_TODO_completed_by_id` (`completed_by_id`),
  KEY `FK_TODO_checklist_id` (`checklist_id`),
  KEY `fk_todo_bpo_completed_by` (`bpo_completed_by_id`),
  KEY `fk_todo_bpo_assigned_to` (`bpo_assigned_to_id`),
  CONSTRAINT `fk_todo_bpo_assigned_to` FOREIGN KEY (`bpo_assigned_to_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `fk_todo_bpo_completed_by` FOREIGN KEY (`bpo_completed_by_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_TODO_checklist_id` FOREIGN KEY (`checklist_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_TODO_completed_by_id` FOREIGN KEY (`completed_by_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_TODO_task_id` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `todo_note`
--

DROP TABLE IF EXISTS `todo_note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `todo_note` (
  `note_id` bigint NOT NULL AUTO_INCREMENT,
  `todo_id` bigint DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `note_text` text NOT NULL,
  `source_type` varchar(10) NOT NULL DEFAULT 'PSP',
  `todo_guid` varchar(36) DEFAULT NULL,
  `author_name` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`note_id`),
  KEY `FK_TODONOTE_created_by` (`created_by_id`),
  KEY `idx_todonote_todo` (`todo_id`),
  KEY `idx_todonote_created_date` (`created_date`),
  KEY `idx_todo_note_guid` (`todo_guid`),
  CONSTRAINT `FK_TODONOTE_created_by` FOREIGN KEY (`created_by_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_TODONOTE_todo` FOREIGN KEY (`todo_id`) REFERENCES `todo` (`todo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `todo_out_04`
--

DROP TABLE IF EXISTS `todo_out_04`;
/*!50001 DROP VIEW IF EXISTS `todo_out_04`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `todo_out_04` AS SELECT 
 1 AS `sequential_count`,
 1 AS `checklist_id`,
 1 AS `is_complete`,
 1 AS `sort_order`,
 1 AS `todo_id`,
 1 AS `task_id`,
 1 AS `DESCRIPTION`,
 1 AS `has_automation`,
 1 AS `auto_id`,
 1 AS `servlet_name`,
 1 AS `automation_text`,
 1 AS `allow_early`,
 1 AS `allow_future`,
 1 AS `allow_non_owner`,
 1 AS `has_owner`,
 1 AS `owner_id`,
 1 AS `is_sourced`,
 1 AS `bpo_registration_id`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `has_future_block`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `todo_out_05`
--

DROP TABLE IF EXISTS `todo_out_05`;
/*!50001 DROP VIEW IF EXISTS `todo_out_05`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `todo_out_05` AS SELECT 
 1 AS `sequential_count`,
 1 AS `checklist_id`,
 1 AS `is_complete`,
 1 AS `sort_order`,
 1 AS `todo_id`,
 1 AS `task_id`,
 1 AS `DESCRIPTION`,
 1 AS `has_automation`,
 1 AS `auto_id`,
 1 AS `servlet_name`,
 1 AS `automation_text`,
 1 AS `allow_early`,
 1 AS `allow_future`,
 1 AS `allow_non_owner`,
 1 AS `has_owner`,
 1 AS `owner_id`,
 1 AS `is_sourced`,
 1 AS `bpo_registration_id`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `has_future_block`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `todo_out_06`
--

DROP TABLE IF EXISTS `todo_out_06`;
/*!50001 DROP VIEW IF EXISTS `todo_out_06`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `todo_out_06` AS SELECT 
 1 AS `sequential_count`,
 1 AS `checklist_id`,
 1 AS `is_complete`,
 1 AS `sort_order`,
 1 AS `todo_id`,
 1 AS `task_id`,
 1 AS `DESCRIPTION`,
 1 AS `has_automation`,
 1 AS `auto_id`,
 1 AS `servlet_name`,
 1 AS `automation_text`,
 1 AS `allow_early`,
 1 AS `allow_future`,
 1 AS `allow_non_owner`,
 1 AS `has_owner`,
 1 AS `owner_id`,
 1 AS `is_sourced`,
 1 AS `bpo_registration_id`,
 1 AS `has_goto`,
 1 AS `goto_link_id`,
 1 AS `has_info`,
 1 AS `info_link_id`,
 1 AS `has_future_block`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `allow_set_password` tinyint(1) DEFAULT '0',
  `email` varchar(255) DEFAULT NULL,
  `verified_email` tinyint(1) DEFAULT '0',
  `guid_expires` datetime DEFAULT NULL,
  `guid_used` tinyint(1) DEFAULT '0',
  `password_hash` varchar(255) DEFAULT NULL,
  `salt` varchar(255) DEFAULT NULL,
  `temp_guid` varchar(255) DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `person_id` bigint NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`person_id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `user_name` (`user_name`),
  CONSTRAINT `FK_USER_person_id` FOREIGN KEY (`person_id`) REFERENCES `assignee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_filter_preset`
--

DROP TABLE IF EXISTS `user_filter_preset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_filter_preset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `slot_number` int NOT NULL,
  `label` varchar(16) NOT NULL,
  `view_renewal` tinyint(1) NOT NULL DEFAULT '1',
  `view_setup` tinyint(1) NOT NULL DEFAULT '1',
  `view_ticket` tinyint(1) NOT NULL DEFAULT '1',
  `view_opportunity` tinyint(1) NOT NULL DEFAULT '0',
  `ownership_filter` int NOT NULL DEFAULT '0',
  `attention_filter` int NOT NULL DEFAULT '0',
  `sort_alphabetically` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_slot` (`user_id`,`slot_number`),
  CONSTRAINT `fk_preset_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`person_id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userinroles`
--

DROP TABLE IF EXISTS `userinroles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userinroles` (
  `person_id` bigint NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`person_id`,`role_id`),
  KEY `FK_userinroles_role_id` (`role_id`),
  CONSTRAINT `FK_userinroles_person_id` FOREIGN KEY (`person_id`) REFERENCES `user` (`person_id`),
  CONSTRAINT `FK_userinroles_role_id` FOREIGN KEY (`role_id`) REFERENCES `userrole` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userrole`
--

DROP TABLE IF EXISTS `userrole`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userrole` (
  `role_id` int NOT NULL,
  `description` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `weblink`
--

DROP TABLE IF EXISTS `weblink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `weblink` (
  `link_id` bigint NOT NULL,
  `link_path` varchar(2000) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `email_id` bigint DEFAULT NULL,
  `type_id` int DEFAULT NULL,
  `active` tinyint DEFAULT '1',
  PRIMARY KEY (`link_id`),
  KEY `FK_WEBLINK_type_id` (`type_id`),
  KEY `FK_WEBLINK_email_id` (`email_id`),
  CONSTRAINT `FK_WEBLINK_email_id` FOREIGN KEY (`email_id`) REFERENCES `note` (`note_id`),
  CONSTRAINT `FK_WEBLINK_type_id` FOREIGN KEY (`type_id`) REFERENCES `linktype` (`link_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Final view structure for view `a25_activity_list_op`
--

/*!50001 DROP VIEW IF EXISTS `a25_activity_list_op`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `a25_activity_list_op` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`due_date` AS `due_date`,max(`p`.`note_id`) AS `max_note_id`,max(`q`.`date_created`) AS `max_date_done`,(case when (max(`q`.`date_created`) is null) then 999 else (to_days(curdate()) - to_days(max(`q`.`date_created`))) end) AS `days_since` from ((`assignee` `a` left join (select `n`.`note_id` AS `note_id`,`n`.`DTYPE` AS `DTYPE`,`n`.`date_created` AS `date_created`,`n`.`date_generated` AS `date_generated`,`n`.`DETAIL` AS `DETAIL`,`n`.`activity_id` AS `activity_id`,`n`.`created_by_id` AS `created_by_id`,`n`.`reason_id` AS `reason_id`,`n`.`status_id` AS `status_id`,`n`.`subject` AS `subject` from `note` `n` where (`n`.`status_id` <> 2)) `p` on((`a`.`id` = `p`.`activity_id`))) left join (select `n1`.`note_id` AS `note_id`,`n1`.`DTYPE` AS `DTYPE`,`n1`.`date_created` AS `date_created`,`n1`.`date_generated` AS `date_generated`,`n1`.`DETAIL` AS `DETAIL`,`n1`.`activity_id` AS `activity_id`,`n1`.`created_by_id` AS `created_by_id`,`n1`.`reason_id` AS `reason_id`,`n1`.`status_id` AS `status_id`,`n1`.`subject` AS `subject` from (`note` `n1` join `reasoncreated` `r` on((`n1`.`reason_id` = `r`.`use_id`))) where (`r`.`outbound` = true)) `q` on((`a`.`id` = `q`.`activity_id`))) where ((`a`.`is_complete` = false) and (`a`.`DTYPE` in ('Renewal','Setup','Ticket'))) group by `a`.`id`,`a`.`DTYPE`,`a`.`full_name`,`a`.`assigned_to_id`,`a`.`due_date` order by `a`.`full_name`,max(`p`.`note_id`) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `a25_activity_list_open`
--

/*!50001 DROP VIEW IF EXISTS `a25_activity_list_open`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `a25_activity_list_open` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`due_date` AS `due_date`,`a`.`max_note_id` AS `max_note_id`,`a`.`max_date_done` AS `max_date_done`,`a`.`days_since` AS `days_since`,(case when (`p`.`status_id` = 1) then 0 else 1 end) AS `on_us` from (`a25_activity_list_op` `a` left join `note` `p` on((`a`.`max_note_id` = `p`.`note_id`))) order by `a`.`full_name` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `a25_activity_list_participating`
--

/*!50001 DROP VIEW IF EXISTS `a25_activity_list_participating`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `a25_activity_list_participating` AS select `a`.`id` AS `id`,`t`.`checklist_id` AS `checklist_id`,`a`.`full_name` AS `full_name`,`a`.`assigned_to_id` AS `act_owner_id`,`t`.`todo_id` AS `todo_id`,`tsk`.`DESCRIPTION` AS `DESCRIPTION`,`tsk`.`owner_id` AS `tsk_owner_id` from (((`todo` `t` join `assignee` `c` on((`t`.`checklist_id` = `c`.`id`))) join `assignee` `a` on((`c`.`id` = `a`.`checklist_id`))) join `task` `tsk` on((`t`.`task_id` = `tsk`.`task_id`))) where ((`a`.`is_complete` = false) and (`tsk`.`has_owner` = true) and (`tsk`.`owner_id` <> `a`.`assigned_to_id`) and (`t`.`is_complete` = false)) order by `a`.`full_name`,`t`.`sort_order`,`t`.`todo_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `a25_checklist_full`
--

/*!50001 DROP VIEW IF EXISTS `a25_checklist_full`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `a25_checklist_full` AS select `c`.`id` AS `id`,`c`.`full_name` AS `full_name`,curdate() AS `today`,`c`.`due_date` AS `due_date`,`c`.`assigned_to_id` AS `assigned_to_id`,`c`.`is_complete` AS `is_complete`,(case when (count(`td`.`todo_id`) = 1) then 1 else 0 end) AS `single_task`,(case when (`ts`.`days_in_advance` is not null) then `ts`.`days_in_advance` else 0 end) AS `days_ahead`,(case when `c`.`is_complete` then 2 when (cast((`c`.`due_date` - interval (case when (`ts`.`days_in_advance` is not null) then `ts`.`days_in_advance` else 0 end) day) as date) <= curdate()) then 1 else 3 end) AS `sort_key` from (((`assignee` `c` join `assignee` `o` on((`c`.`assigned_to_id` = `o`.`id`))) left join `tasksequence` `ts` on((`c`.`recurring_list_id` = `ts`.`sequence_id`))) join `todo` `td` on((`c`.`id` = `td`.`checklist_id`))) where ((`c`.`DTYPE` = 'CheckList') and (`o`.`DTYPE` = 'Person') and ((`c`.`is_complete` = false) or (`c`.`date_completed` > (curdate() - 1)))) group by `td`.`checklist_id` order by `c`.`due_date` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `a_base_01`
--

/*!50001 DROP VIEW IF EXISTS `a_base_01`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `a_base_01` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`TAXID` AS `TAXID`,`a`.`address_id` AS `address_id`,`a`.`contact_id` AS `contact_id`,`a`.`email` AS `email`,`a`.`first_name` AS `first_name`,`a`.`last_name` AS `last_name`,`a`.`middle_init` AS `middle_init`,`a`.`phone` AS `phone`,`a`.`title` AS `title`,`a`.`psp_id` AS `psp_id`,`a`.`setup_id` AS `setup_id`,`a`.`employee_id` AS `employee_id`,`a`.`date_completed` AS `date_completed`,`a`.`date_created` AS `date_created`,`a`.`due_date` AS `due_date`,`a`.`is_complete` AS `is_complete`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`completed_by_id` AS `completed_by_id`,`a`.`created_by_id` AS `created_by_id`,`a`.`employer_id` AS `employer_id`,`a`.`checklist_id` AS `checklist_id`,`a`.`proposal_id` AS `proposal_id`,`a`.`person_id` AS `person_id`,`a`.`description` AS `description`,`a`.`method_id` AS `method_id`,`a`.`recurring_list_id` AS `recurring_list_id`,`a`.`email_address` AS `email_address`,`a`.`myRsc` AS `myRsc`,`a`.`primary_contact` AS `primary_contact` from `assignee` `a` where ((`a`.`DTYPE` in ('Setup','Renewal','Ticket')) and (`a`.`is_complete` = false)) order by `a`.`full_name` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `a_base_02`
--

/*!50001 DROP VIEW IF EXISTS `a_base_02`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `a_base_02` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`TAXID` AS `TAXID`,`a`.`address_id` AS `address_id`,`a`.`contact_id` AS `contact_id`,`a`.`email` AS `email`,`a`.`first_name` AS `first_name`,`a`.`last_name` AS `last_name`,`a`.`middle_init` AS `middle_init`,`a`.`phone` AS `phone`,`a`.`title` AS `title`,`a`.`psp_id` AS `psp_id`,`a`.`setup_id` AS `setup_id`,`a`.`employee_id` AS `employee_id`,`a`.`date_completed` AS `date_completed`,`a`.`date_created` AS `date_created`,`a`.`due_date` AS `due_date`,`a`.`is_complete` AS `is_complete`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`completed_by_id` AS `completed_by_id`,`a`.`created_by_id` AS `created_by_id`,`a`.`employer_id` AS `employer_id`,`a`.`checklist_id` AS `checklist_id`,`a`.`proposal_id` AS `proposal_id`,`a`.`person_id` AS `person_id`,`a`.`description` AS `description`,`a`.`method_id` AS `method_id`,`a`.`recurring_list_id` AS `recurring_list_id`,`a`.`email_address` AS `email_address`,`a`.`myRsc` AS `myRsc`,`a`.`primary_contact` AS `primary_contact`,`c`.`date_generated` AS `last_contact`,(case when (`c`.`date_generated` is null) then 2 when ((curdate() - interval 7 day) < `c`.`date_generated`) then 0 when ((curdate() - interval 14 day) > `c`.`date_generated`) then 2 else 1 end) AS `contact_status`,(case when (`c`.`date_generated` is null) then 1 when ((curdate() - interval 7 day) < `c`.`date_generated`) then 0 else 1 end) AS `needs_contact` from (`a_base_01` `a` left join `n_contact_f` `c` on((`a`.`id` = `c`.`activity_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `a_base_03`
--

/*!50001 DROP VIEW IF EXISTS `a_base_03`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `a_base_03` AS select `a_base_02`.`id` AS `id`,`a_base_02`.`DTYPE` AS `DTYPE`,`a_base_02`.`full_name` AS `full_name`,`a_base_02`.`TAXID` AS `TAXID`,`a_base_02`.`address_id` AS `address_id`,`a_base_02`.`contact_id` AS `contact_id`,`a_base_02`.`email` AS `email`,`a_base_02`.`first_name` AS `first_name`,`a_base_02`.`last_name` AS `last_name`,`a_base_02`.`middle_init` AS `middle_init`,`a_base_02`.`phone` AS `phone`,`a_base_02`.`title` AS `title`,`a_base_02`.`psp_id` AS `psp_id`,`a_base_02`.`setup_id` AS `setup_id`,`a_base_02`.`employee_id` AS `employee_id`,`a_base_02`.`date_completed` AS `date_completed`,`a_base_02`.`date_created` AS `date_created`,`a_base_02`.`due_date` AS `due_date`,`a_base_02`.`is_complete` AS `is_complete`,`a_base_02`.`assigned_to_id` AS `assigned_to_id`,`a_base_02`.`completed_by_id` AS `completed_by_id`,`a_base_02`.`created_by_id` AS `created_by_id`,`a_base_02`.`employer_id` AS `employer_id`,`a_base_02`.`checklist_id` AS `checklist_id`,`a_base_02`.`proposal_id` AS `proposal_id`,`a_base_02`.`person_id` AS `person_id`,`a_base_02`.`description` AS `description`,`a_base_02`.`method_id` AS `method_id`,`a_base_02`.`recurring_list_id` AS `recurring_list_id`,`a_base_02`.`email_address` AS `email_address`,`a_base_02`.`myRsc` AS `myRsc`,`a_base_02`.`primary_contact` AS `primary_contact`,`a_base_02`.`last_contact` AS `last_contact`,`a_base_02`.`contact_status` AS `contact_status`,`a_base_02`.`needs_contact` AS `needs_contact`,`n`.`status_id` AS `c_status_id`,(case when (`n`.`status_id` is null) then 1 when (`n`.`status_id` = 1) then 0 else 1 end) AS `waiting_on_us` from (`a_base_02` left join `n_status_f1` `n` on((`a_base_02`.`id` = `n`.`activity_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `a_base_04`
--

/*!50001 DROP VIEW IF EXISTS `a_base_04`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `a_base_04` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`TAXID` AS `TAXID`,`a`.`address_id` AS `address_id`,`a`.`contact_id` AS `contact_id`,`a`.`email` AS `email`,`a`.`first_name` AS `first_name`,`a`.`last_name` AS `last_name`,`a`.`middle_init` AS `middle_init`,`a`.`phone` AS `phone`,`a`.`title` AS `title`,`a`.`psp_id` AS `psp_id`,`a`.`setup_id` AS `setup_id`,`a`.`employee_id` AS `employee_id`,`a`.`date_completed` AS `date_completed`,`a`.`date_created` AS `date_created`,`a`.`due_date` AS `due_date`,`a`.`is_complete` AS `is_complete`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`completed_by_id` AS `completed_by_id`,`a`.`created_by_id` AS `created_by_id`,`a`.`employer_id` AS `employer_id`,`a`.`checklist_id` AS `checklist_id`,`a`.`proposal_id` AS `proposal_id`,`a`.`person_id` AS `person_id`,`a`.`description` AS `description`,`a`.`method_id` AS `method_id`,`a`.`recurring_list_id` AS `recurring_list_id`,`a`.`email_address` AS `email_address`,`a`.`myRsc` AS `myRsc`,`a`.`primary_contact` AS `primary_contact`,`a`.`last_contact` AS `last_contact`,`a`.`contact_status` AS `contact_status`,`a`.`needs_contact` AS `needs_contact`,`a`.`c_status_id` AS `c_status_id`,`a`.`waiting_on_us` AS `waiting_on_us`,`t`.`owner_id` AS `task_owner_id`,`t`.`bpo_registration_id` AS `bpo_registration_id`,(case when (`t`.`owner_id` is null) then concat(`a`.`id`,'-',`a`.`assigned_to_id`,'-N') when (`t`.`bpo_registration_id` is null) then concat(`a`.`id`,'-',`a`.`assigned_to_id`,'-',`t`.`owner_id`,'-N') else concat(`a`.`id`,'-',`a`.`assigned_to_id`,'-',`t`.`owner_id`,'-',`t`.`bpo_registration_id`) end) AS `UID` from (`a_base_03` `a` left join `ac_base_11` `t` on((`a`.`id` = `t`.`activity_id`))) order by `a`.`full_name` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `a_base_05`
--

/*!50001 DROP VIEW IF EXISTS `a_base_05`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `a_base_05` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`TAXID` AS `TAXID`,`a`.`address_id` AS `address_id`,`a`.`contact_id` AS `contact_id`,`a`.`email` AS `email`,`a`.`first_name` AS `first_name`,`a`.`last_name` AS `last_name`,`a`.`middle_init` AS `middle_init`,`a`.`phone` AS `phone`,`a`.`title` AS `title`,`a`.`psp_id` AS `psp_id`,`a`.`setup_id` AS `setup_id`,`a`.`employee_id` AS `employee_id`,`a`.`date_completed` AS `date_completed`,`a`.`date_created` AS `date_created`,`a`.`due_date` AS `due_date`,`a`.`is_complete` AS `is_complete`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`completed_by_id` AS `completed_by_id`,`a`.`created_by_id` AS `created_by_id`,`a`.`employer_id` AS `employer_id`,`a`.`checklist_id` AS `checklist_id`,`a`.`proposal_id` AS `proposal_id`,`a`.`person_id` AS `person_id`,`a`.`description` AS `description`,`a`.`method_id` AS `method_id`,`a`.`recurring_list_id` AS `recurring_list_id`,`a`.`email_address` AS `email_address`,`a`.`myRsc` AS `myRsc`,`a`.`primary_contact` AS `primary_contact`,`a`.`last_contact` AS `last_contact`,`a`.`contact_status` AS `contact_status`,`a`.`needs_contact` AS `needs_contact`,`a`.`c_status_id` AS `c_status_id`,`a`.`waiting_on_us` AS `waiting_on_us`,`a`.`task_owner_id` AS `task_owner_id`,`a`.`bpo_registration_id` AS `bpo_registration_id`,`a`.`UID` AS `UID`,(case when (`a`.`DTYPE` = 'Ticket') then concat(`t`.`first_name`,' ',`t`.`last_name`) else `a`.`full_name` end) AS `full_name_alt` from (`a_base_04` `a` left join `assignee` `t` on((`a`.`person_id` = `t`.`id`))) order by `a`.`full_name` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_01`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_01`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_01` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`first_name` AS `first_name`,`a`.`last_name` AS `last_name`,`a`.`employee_id` AS `employee_id`,`a`.`due_date` AS `due_date`,`a`.`is_complete` AS `is_complete`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`employer_id` AS `employer_id`,`a`.`checklist_id` AS `checklist_id` from `assignee` `a` where (((`a`.`DTYPE` = 'Renewal') or (`a`.`DTYPE` = 'Setup') or (`a`.`DTYPE` = 'Ticket')) and (`a`.`is_complete` = false)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_02`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_02`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_02` AS select `n`.`note_id` AS `note_id`,`n`.`DTYPE` AS `DTYPE`,`n`.`date_created` AS `date_created`,`n`.`date_generated` AS `date_generated`,`n`.`DETAIL` AS `DETAIL`,`n`.`activity_id` AS `activity_id`,`n`.`created_by_id` AS `created_by_id`,`n`.`reason_id` AS `reason_id`,`n`.`status_id` AS `status_id`,`n`.`subject` AS `subject` from `note` `n` where (`n`.`status_id` <> 2) order by `n`.`activity_id`,`n`.`note_id` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_03`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_03`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_03` AS select `n`.`activity_id` AS `activity_id`,max(`n`.`note_id`) AS `max_note_id` from `ac_base_02` `n` group by `n`.`activity_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_04`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_04`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_04` AS select `a`.`activity_id` AS `activity_id`,`n`.`status_id` AS `status_id`,`a`.`max_note_id` AS `max_note_id` from (`ac_base_03` `a` join `note` `n` on((`a`.`max_note_id` = `n`.`note_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_05`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_05`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_05` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`first_name` AS `first_name`,`a`.`last_name` AS `last_name`,`a`.`employee_id` AS `employee_id`,`a`.`due_date` AS `due_date`,`a`.`is_complete` AS `is_complete`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`employer_id` AS `employer_id`,`n`.`status_id` AS `status_id`,`a`.`checklist_id` AS `checklist_id` from (`ac_base_01` `a` left join `ac_base_04` `n` on((`a`.`id` = `n`.`activity_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_06`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_06`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_06` AS select `n`.`note_id` AS `note_id`,`n`.`DTYPE` AS `DTYPE`,`n`.`date_created` AS `date_created`,`n`.`date_generated` AS `date_generated`,`n`.`DETAIL` AS `DETAIL`,`n`.`activity_id` AS `activity_id`,`n`.`created_by_id` AS `created_by_id`,`n`.`reason_id` AS `reason_id`,`n`.`status_id` AS `status_id`,`n`.`subject` AS `subject`,`r`.`outbound` AS `outbound` from (`note` `n` left join `reasoncreated` `r` on((`n`.`reason_id` = `r`.`use_id`))) where ((`r`.`outbound` is not null) and (`r`.`outbound` = 1)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_07`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_07`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_07` AS select `n`.`activity_id` AS `activity_id`,max(`n`.`note_id`) AS `max_note_id` from `ac_base_06` `n` group by `n`.`activity_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_08`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_08`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_08` AS select `a`.`activity_id` AS `activity_id`,`n`.`date_created` AS `date_created`,`a`.`max_note_id` AS `max_note_id` from (`ac_base_07` `a` join `note` `n` on((`a`.`max_note_id` = `n`.`note_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_09`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_09`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_09` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`first_name` AS `first_name`,`a`.`last_name` AS `last_name`,`a`.`employee_id` AS `employee_id`,`a`.`due_date` AS `due_date`,`a`.`is_complete` AS `is_complete`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`employer_id` AS `employer_id`,`a`.`status_id` AS `last_status_id`,`n`.`date_created` AS `last_outbound`,`a`.`checklist_id` AS `checklist_id` from (`ac_base_05` `a` left join `ac_base_08` `n` on((`a`.`id` = `n`.`activity_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_10`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_10`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_10` AS select `t`.`todo_id` AS `todo_id`,`t`.`task_id` AS `task_id`,`t`.`name` AS `name`,`t`.`not_me` AS `not_me`,`t`.`has_owner` AS `has_owner`,`t`.`is_sourced` AS `is_sourced`,`t`.`owner_id` AS `owner_id`,`t`.`allow_non_owner` AS `allow_non_owner`,`t`.`allow_early` AS `allow_early`,`t`.`allow_future` AS `allow_future`,`t`.`sort_order` AS `sort_order`,`t`.`min_sort_top` AS `min_sort_top`,`t`.`at_top` AS `at_top`,`t`.`min_sort_block` AS `min_sort_block`,`t`.`is_blocked` AS `is_blocked`,`t`.`checklist_id` AS `checklist_id`,`t`.`checklist_name` AS `checklist_name`,`t`.`checklist_owner` AS `checklist_owner`,`t`.`is_checklist_complete` AS `is_checklist_complete`,`t`.`checklist_due_date` AS `checklist_due_date`,`t`.`checklist_assigned_to_id` AS `activity_id`,`t`.`DTYPE` AS `DTYPE`,`t`.`is_activity` AS `is_activity`,`t`.`is_activity_complete` AS `is_activity_complete`,`t`.`activity_owner_id` AS `activity_owner_id`,`t`.`activity_due_date` AS `activity_due_date`,`t`.`has_goto` AS `has_goto`,`t`.`goto_link_id` AS `goto_link_id`,`t`.`has_info` AS `has_info`,`t`.`info_link_id` AS `info_link_id`,`t`.`auto_id` AS `auto_id`,`t`.`has_automation` AS `has_automation`,`t`.`bpo_registration_id` AS `bpo_registration_id` from `tdx_open_activity` `t` where ((`t`.`not_me` = 1) and (`t`.`is_blocked` = false)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ac_base_11`
--

/*!50001 DROP VIEW IF EXISTS `ac_base_11`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ac_base_11` AS select count(`a`.`todo_id`) AS `counts`,`a`.`owner_id` AS `owner_id`,`a`.`bpo_registration_id` AS `bpo_registration_id`,`a`.`activity_id` AS `activity_id` from `ac_base_10` `a` group by `a`.`activity_id`,`a`.`owner_id`,`a`.`bpo_registration_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `acx_open_activities`
--

/*!50001 DROP VIEW IF EXISTS `acx_open_activities`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `acx_open_activities` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`first_name` AS `first_name`,`a`.`last_name` AS `last_name`,`a`.`employee_id` AS `employee_id`,`a`.`due_date` AS `due_date`,`a`.`is_complete` AS `is_complete`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`employer_id` AS `employer_id`,`a`.`last_status_id` AS `last_status_id`,`a`.`last_outbound` AS `last_outbound`,`a`.`checklist_id` AS `checklist_id`,`n`.`owner_id` AS `task_owner_id`,`n`.`bpo_registration_id` AS `bpo_registration_id`,(case when (`n`.`owner_id` is null) then concat(`a`.`id`,'-',`a`.`assigned_to_id`,'-N') when (`n`.`bpo_registration_id` is null) then concat(`a`.`id`,'-',`a`.`assigned_to_id`,'-',`n`.`owner_id`,'-N') else concat(`a`.`id`,'-',`a`.`assigned_to_id`,'-',`n`.`owner_id`,'-',`n`.`bpo_registration_id`) end) AS `UID` from (`ac_base_09` `a` left join `ac_base_11` `n` on((`a`.`id` = `n`.`activity_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `billing_summary`
--

/*!50001 DROP VIEW IF EXISTS `billing_summary`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `billing_summary` AS select uuid() AS `uuid`,`billinggrid`.`employer_id` AS `employer_id`,`billinggrid`.`month_id` AS `month_id`,sum(`billinggrid`.`cobra`) AS `cobra_tot`,sum(`billinggrid`.`direct`) AS `direct_tot`,sum(`billinggrid`.`dual_plan`) AS `dual_plan_tot`,sum(`billinggrid`.`fsa`) AS `fsa_tot`,sum(`billinggrid`.`hra`) AS `hra_tot`,sum(`billinggrid`.`hsa`) AS `hsa_tot`,sum(`billinggrid`.`lsa`) AS `lsa_tot`,sum(`billinggrid`.`retiree`) AS `retiree_tot`,sum(`billinggrid`.`transit`) AS `transit_tot` from `billinggrid` group by `billinggrid`.`employer_id`,`billinggrid`.`month_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ck_base_01`
--

/*!50001 DROP VIEW IF EXISTS `ck_base_01`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ck_base_01` AS select count(`t`.`todo_id`) AS `todo_count`,`t`.`checklist_id` AS `checklist_id` from `tdx_open_list` `t` group by `t`.`checklist_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ck_base_02`
--

/*!50001 DROP VIEW IF EXISTS `ck_base_02`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ck_base_02` AS select `c`.`todo_count` AS `todo_count`,`c`.`checklist_id` AS `checklist_id`,`a`.`full_name` AS `full_name`,`a`.`due_date` AS `due_date`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`recurring_list_id` AS `recurring_list_id`,`t`.`days_in_advance` AS `days_in_advance` from ((`ck_base_01` `c` join `assignee` `a` on((`c`.`checklist_id` = `a`.`id`))) left join `tasksequence` `t` on((`a`.`recurring_list_id` = `t`.`sequence_id`))) order by `a`.`due_date` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ck_base_03`
--

/*!50001 DROP VIEW IF EXISTS `ck_base_03`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ck_base_03` AS select `c`.`todo_count` AS `todo_count`,`c`.`checklist_id` AS `checklist_id`,`c`.`full_name` AS `full_name`,`c`.`due_date` AS `due_date`,`c`.`assigned_to_id` AS `assigned_to_id`,`t`.`todo_id` AS `todo_id`,`t`.`task_id` AS `task_id`,`t`.`name` AS `name`,`t`.`has_owner` AS `has_owner`,`t`.`is_sourced` AS `is_sourced`,`t`.`owner_id` AS `owner_id`,`t`.`allow_non_owner` AS `allow_non_owner`,`t`.`allow_early` AS `allow_early`,`t`.`allow_future` AS `allow_future`,`t`.`sort_order` AS `sort_order`,`t`.`min_sort_top` AS `min_sort_top`,`t`.`at_top` AS `at_top`,`t`.`min_sort_block` AS `min_sort_block`,`t`.`is_blocked` AS `is_blocked`,`t`.`checklist_assigned_to_id` AS `checklist_assigned_to_id`,`t`.`checklist_owner` AS `checklist_owner`,`t`.`has_goto` AS `has_goto`,`t`.`goto_link_id` AS `goto_link_id`,`t`.`has_info` AS `has_info`,`t`.`info_link_id` AS `info_link_id`,`t`.`auto_id` AS `auto_id`,`t`.`has_automation` AS `has_automation`,`t`.`bpo_registration_id` AS `bpo_registration_id`,(case when (((`t`.`has_owner` = true) or (`t`.`is_sourced` = true)) and (`t`.`owner_id` is not null) and (`t`.`owner_id` <> `c`.`assigned_to_id`)) then 1 else 0 end) AS `not_me` from (`ck_base_02` `c` join `tdx_open_list` `t` on((`c`.`checklist_id` = `t`.`checklist_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ck_base_04`
--

/*!50001 DROP VIEW IF EXISTS `ck_base_04`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ck_base_04` AS select `c`.`todo_count` AS `todo_count`,`c`.`checklist_id` AS `checklist_id`,`c`.`full_name` AS `full_name`,`c`.`due_date` AS `due_date`,`c`.`assigned_to_id` AS `assigned_to_id`,`c`.`todo_id` AS `todo_id`,`c`.`task_id` AS `task_id`,`c`.`name` AS `name`,`c`.`has_owner` AS `has_owner`,`c`.`is_sourced` AS `is_sourced`,`c`.`owner_id` AS `owner_id`,`c`.`allow_non_owner` AS `allow_non_owner`,`c`.`allow_early` AS `allow_early`,`c`.`allow_future` AS `allow_future`,`c`.`sort_order` AS `sort_order`,`c`.`min_sort_top` AS `min_sort_top`,`c`.`at_top` AS `at_top`,`c`.`min_sort_block` AS `min_sort_block`,`c`.`is_blocked` AS `is_blocked`,`c`.`checklist_assigned_to_id` AS `checklist_assigned_to_id`,`c`.`checklist_owner` AS `checklist_owner`,`c`.`has_goto` AS `has_goto`,`c`.`goto_link_id` AS `goto_link_id`,`c`.`has_info` AS `has_info`,`c`.`info_link_id` AS `info_link_id`,`c`.`auto_id` AS `auto_id`,`c`.`has_automation` AS `has_automation`,`c`.`bpo_registration_id` AS `bpo_registration_id`,`c`.`not_me` AS `not_me` from `ck_base_03` `c` where ((`c`.`not_me` = true) and (`c`.`is_blocked` = false)) order by `c`.`due_date`,`c`.`checklist_id`,`c`.`sort_order`,`c`.`todo_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ck_base_05`
--

/*!50001 DROP VIEW IF EXISTS `ck_base_05`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ck_base_05` AS select `c`.`todo_count` AS `todo_count`,`c`.`checklist_id` AS `checklist_id`,`c`.`full_name` AS `full_name`,`c`.`due_date` AS `due_date`,`c`.`assigned_to_id` AS `assigned_to_id`,`c`.`recurring_list_id` AS `recurring_list_id`,`c`.`days_in_advance` AS `days_in_advance`,`t`.`todo_count` AS `todo_count_f` from (`ck_base_02` `c` left join `td_count_1` `t` on((`c`.`checklist_id` = `t`.`checklist_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ckx_open_checklists`
--

/*!50001 DROP VIEW IF EXISTS `ckx_open_checklists`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ckx_open_checklists` AS select `c`.`todo_count` AS `todo_count`,`c`.`checklist_id` AS `checklist_id`,`c`.`full_name` AS `full_name`,`c`.`due_date` AS `due_date`,`c`.`assigned_to_id` AS `assigned_to_id`,`c`.`days_in_advance` AS `days_in_advance`,`a`.`owner_id` AS `owner_id`,`a`.`bpo_registration_id` AS `bpo_registration_id`,`a`.`has_owner` AS `has_owner`,`a`.`is_sourced` AS `is_sourced`,(case when (`a`.`owner_id` is null) then concat(`c`.`checklist_id`,'-',`c`.`assigned_to_id`,'-N') when (`a`.`bpo_registration_id` is null) then concat(`c`.`checklist_id`,'-',`c`.`assigned_to_id`,'-',`a`.`owner_id`,'-N') else concat(`c`.`checklist_id`,'-',`c`.`assigned_to_id`,'-',`a`.`owner_id`,'-',`a`.`bpo_registration_id`) end) AS `UID`,(case when (`c`.`days_in_advance` is null) then `c`.`due_date` else (`c`.`due_date` - interval `c`.`days_in_advance` day) end) AS `show_date`,`c`.`todo_count` AS `todo_count_f` from (`ck_base_05` `c` left join `ck_base_04` `a` on((`c`.`checklist_id` = `a`.`checklist_id`))) order by `c`.`due_date`,`c`.`full_name`,`a`.`sort_order` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `dup_emails`
--

/*!50001 DROP VIEW IF EXISTS `dup_emails`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `dup_emails` AS select uuid() AS `uuid`,`employee`.`email` AS `email`,count(`employee`.`employee_id`) AS `email_count` from `employee` group by `employee`.`email` order by `email_count` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `dup_names`
--

/*!50001 DROP VIEW IF EXISTS `dup_names`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `dup_names` AS select uuid() AS `uuid`,`employee`.`first_name` AS `first_name`,`employee`.`last_name` AS `last_name`,count(`employee`.`employee_id`) AS `name_count` from `employee` group by `employee`.`first_name`,`employee`.`last_name` order by `name_count` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `dup_names_1`
--

/*!50001 DROP VIEW IF EXISTS `dup_names_1`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `dup_names_1` AS select `dup_names`.`uuid` AS `uuid`,`dup_names`.`first_name` AS `first_name`,`dup_names`.`last_name` AS `last_name`,`dup_names`.`name_count` AS `name_count` from `dup_names` where (`dup_names`.`name_count` > 1) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `dupemailee`
--

/*!50001 DROP VIEW IF EXISTS `dupemailee`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `dupemailee` AS select `employeev`.`email1` AS `email1`,`employeev`.`employer_id` AS `employer_id`,count(`employeev`.`employee_id`) AS `e_count` from `employeev` group by `employeev`.`email1`,`employeev`.`employer_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `employeev`
--

/*!50001 DROP VIEW IF EXISTS `employeev`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `employeev` AS select `employee`.`employee_id` AS `employee_id`,trim(upper(`employee`.`first_name`)) AS `f_name`,trim(upper(`employee`.`last_name`)) AS `l_name`,trim(lower(`employee`.`email`)) AS `email1`,trim(lower(`employee`.`hr_email`)) AS `email2`,`employee`.`employer_id` AS `employer_id` from `employee` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `n_base_01`
--

/*!50001 DROP VIEW IF EXISTS `n_base_01`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `n_base_01` AS select `n`.`note_id` AS `note_id`,`n`.`DTYPE` AS `DTYPE`,`n`.`date_created` AS `date_created`,`n`.`date_generated` AS `date_generated`,`n`.`DETAIL` AS `DETAIL`,`n`.`activity_id` AS `activity_id`,`n`.`created_by_id` AS `created_by_id`,`n`.`reason_id` AS `reason_id`,`n`.`status_id` AS `status_id`,`n`.`subject` AS `subject` from `note` `n` where (`n`.`activity_id` is not null) order by `n`.`activity_id`,`n`.`note_id` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `n_contact_1`
--

/*!50001 DROP VIEW IF EXISTS `n_contact_1`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `n_contact_1` AS select `n`.`note_id` AS `note_id`,`n`.`DTYPE` AS `DTYPE`,`n`.`date_created` AS `date_created`,`n`.`date_generated` AS `date_generated`,`n`.`DETAIL` AS `DETAIL`,`n`.`activity_id` AS `activity_id`,`n`.`created_by_id` AS `created_by_id`,`n`.`reason_id` AS `reason_id`,`n`.`status_id` AS `status_id`,`n`.`subject` AS `subject`,`r`.`outbound` AS `outbound` from (`n_base_01` `n` join `reasoncreated` `r` on((`n`.`reason_id` = `r`.`use_id`))) where (`r`.`outbound` = 1) order by `n`.`activity_id`,`n`.`note_id` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `n_contact_2`
--

/*!50001 DROP VIEW IF EXISTS `n_contact_2`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `n_contact_2` AS select max(`n`.`note_id`) AS `max_id`,`n`.`activity_id` AS `activity_id` from `n_contact_1` `n` group by `n`.`activity_id` order by `n`.`activity_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `n_contact_f`
--

/*!50001 DROP VIEW IF EXISTS `n_contact_f`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `n_contact_f` AS select `n`.`max_id` AS `max_id`,`n`.`activity_id` AS `activity_id`,`note`.`date_generated` AS `date_generated` from (`n_contact_2` `n` join `note` on((`n`.`max_id` = `note`.`note_id`))) order by `n`.`activity_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `n_status_1`
--

/*!50001 DROP VIEW IF EXISTS `n_status_1`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `n_status_1` AS select `n`.`note_id` AS `note_id`,`n`.`DTYPE` AS `DTYPE`,`n`.`date_created` AS `date_created`,`n`.`date_generated` AS `date_generated`,`n`.`DETAIL` AS `DETAIL`,`n`.`activity_id` AS `activity_id`,`n`.`created_by_id` AS `created_by_id`,`n`.`reason_id` AS `reason_id`,`n`.`status_id` AS `status_id`,`n`.`subject` AS `subject` from `n_base_01` `n` where ((`n`.`status_id` = 1) or (`n`.`status_id` = 3)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `n_status_f`
--

/*!50001 DROP VIEW IF EXISTS `n_status_f`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `n_status_f` AS select max(`n`.`note_id`) AS `max_id`,`n`.`activity_id` AS `activity_id` from `n_status_1` `n` group by `n`.`activity_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `n_status_f1`
--

/*!50001 DROP VIEW IF EXISTS `n_status_f1`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `n_status_f1` AS select `n`.`max_id` AS `max_id`,`n`.`activity_id` AS `activity_id`,`note`.`status_id` AS `status_id` from (`n_status_f` `n` join `note` on((`n`.`max_id` = `note`.`note_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `personv`
--

/*!50001 DROP VIEW IF EXISTS `personv`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `personv` AS select `assignee`.`id` AS `id`,trim(upper(`assignee`.`first_name`)) AS `f_name`,trim(upper(`assignee`.`last_name`)) AS `l_name`,trim(lower(`assignee`.`email`)) AS `email1`,`assignee`.`employee_id` AS `employee_id` from `assignee` where (`assignee`.`DTYPE` = 'Person') order by trim(upper(`assignee`.`last_name`)),trim(upper(`assignee`.`first_name`)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `r_ex`
--

/*!50001 DROP VIEW IF EXISTS `r_ex`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `r_ex` AS select `b`.`benefit_id` AS `benefit_id`,`b`.`effective_date` AS `effective_date`,`b`.`hasCards` AS `hasCards`,`b`.`active` AS `active`,`b`.`last_renewed` AS `last_renewed`,`b`.`next_renewal_due` AS `next_renewal_due`,`b`.`plan_description` AS `plan_description`,`b`.`plan_name` AS `plan_name`,`b`.`termination_date` AS `termination_date`,`b`.`employer_id` AS `employer_id`,`b`.`plan_type_id` AS `plan_type_id`,`b`.`benid_pb` AS `benid_pb`,`e`.`employer_name` AS `employer_name` from (`benefit` `b` join `employer` `e` on((`b`.`employer_id` = `e`.`organization_id`))) where ((`b`.`active` = true) and (`b`.`next_renewal_due` < '2024-05-01')) order by `b`.`next_renewal_due`,`e`.`employer_name`,`b`.`effective_date` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `td_all_open`
--

/*!50001 DROP VIEW IF EXISTS `td_all_open`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `td_all_open` AS select `t`.`todo_id` AS `todo_id`,`t`.`date_completed` AS `date_completed`,`t`.`is_complete` AS `is_complete`,`t`.`sort_order` AS `sort_order`,`t`.`checklist_id` AS `checklist_id`,`t`.`completed_by_id` AS `completed_by_id`,`t`.`task_id` AS `task_id`,`b`.`allow_future` AS `allow_future` from (`todo` `t` join `task` `b` on((`t`.`task_id` = `b`.`task_id`))) where (`t`.`is_complete` = false) order by `t`.`checklist_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `td_base_01`
--

/*!50001 DROP VIEW IF EXISTS `td_base_01`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `td_base_01` AS select `t`.`todo_id` AS `todo_id`,`t`.`date_completed` AS `date_completed`,`t`.`is_complete` AS `is_complete`,`t`.`sort_order` AS `sort_order`,`t`.`checklist_id` AS `checklist_id`,`t`.`completed_by_id` AS `completed_by_id`,`t`.`task_id` AS `task_id`,`t`.`allow_future` AS `allow_future`,`a`.`min_sort` AS `min_sort` from (`td_all_open` `t` left join `td_top_item` `a` on((`t`.`checklist_id` = `a`.`id`))) order by `t`.`checklist_id`,`t`.`sort_order` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `td_base_02`
--

/*!50001 DROP VIEW IF EXISTS `td_base_02`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `td_base_02` AS select `t`.`todo_id` AS `todo_id`,`t`.`date_completed` AS `date_completed`,`t`.`is_complete` AS `is_complete`,`t`.`sort_order` AS `sort_order`,`t`.`checklist_id` AS `checklist_id`,`t`.`completed_by_id` AS `completed_by_id`,`t`.`task_id` AS `task_id`,`t`.`allow_future` AS `allow_future`,`t`.`min_sort` AS `min_sort_top`,`a`.`min_sort` AS `min_sort_block` from (`td_base_01` `t` left join `td_top_block` `a` on((`t`.`checklist_id` = `a`.`id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `td_base_03`
--

/*!50001 DROP VIEW IF EXISTS `td_base_03`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `td_base_03` AS select `t`.`todo_id` AS `todo_id`,`t`.`date_completed` AS `date_completed`,`t`.`is_complete` AS `is_complete`,`t`.`sort_order` AS `sort_order`,`t`.`checklist_id` AS `checklist_id`,`t`.`completed_by_id` AS `completed_by_id`,`t`.`task_id` AS `task_id`,`t`.`allow_future` AS `allow_future`,`t`.`min_sort_top` AS `min_sort_top`,`t`.`min_sort_block` AS `min_sort_block`,`a`.`owner_id` AS `owner_id`,`a`.`is_sourced` AS `is_sourced`,`a`.`bpo_registration_id` AS `bpo_registration_id`,`a`.`allow_early` AS `allow_early`,`a`.`has_owner` AS `has_owner`,`a`.`auto_id` AS `auto_id`,`a`.`has_goto` AS `has_goto`,`a`.`goto_link_id` AS `goto_link_id`,`a`.`has_info` AS `has_info`,`a`.`info_link_id` AS `info_link_id`,`a`.`has_automation` AS `has_automation`,`a`.`allow_non_owner` AS `allow_non_owner`,`a`.`DESCRIPTION` AS `name` from (`td_base_02` `t` join `task` `a` on((`t`.`task_id` = `a`.`task_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `td_base_04`
--

/*!50001 DROP VIEW IF EXISTS `td_base_04`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `td_base_04` AS select `t`.`todo_id` AS `todo_id`,`t`.`task_id` AS `task_id`,`t`.`has_owner` AS `has_owner`,`t`.`is_sourced` AS `is_sourced`,`t`.`owner_id` AS `owner_id`,`t`.`allow_non_owner` AS `allow_non_owner`,`t`.`allow_early` AS `allow_early`,`t`.`allow_future` AS `allow_future`,`t`.`sort_order` AS `sort_order`,`t`.`min_sort_top` AS `min_sort_top`,`t`.`min_sort_block` AS `min_sort_block`,`t`.`checklist_id` AS `checklist_id`,`a`.`full_name` AS `checklist_name`,`a`.`is_complete` AS `is_checklist_complete`,`a`.`assigned_to_id` AS `checklist_assigned_to_id`,`t`.`has_goto` AS `has_goto`,`t`.`goto_link_id` AS `goto_link_id`,`t`.`has_info` AS `has_info`,`t`.`info_link_id` AS `info_link_id`,`t`.`auto_id` AS `auto_id`,`t`.`has_automation` AS `has_automation`,`t`.`bpo_registration_id` AS `bpo_registration_id`,`t`.`name` AS `name`,`a`.`due_date` AS `checklist_due_date` from (`td_base_03` `t` join `assignee` `a` on((`t`.`checklist_id` = `a`.`id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `td_base_05`
--

/*!50001 DROP VIEW IF EXISTS `td_base_05`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `td_base_05` AS select `t`.`todo_id` AS `todo_id`,`t`.`task_id` AS `task_id`,`t`.`name` AS `name`,`t`.`has_owner` AS `has_owner`,`t`.`is_sourced` AS `is_sourced`,`t`.`owner_id` AS `owner_id`,`t`.`allow_non_owner` AS `allow_non_owner`,`t`.`allow_early` AS `allow_early`,`t`.`allow_future` AS `allow_future`,`t`.`sort_order` AS `sort_order`,`t`.`min_sort_top` AS `min_sort_top`,(case when (`t`.`sort_order` > `t`.`min_sort_top`) then 0 else 1 end) AS `at_top`,`t`.`min_sort_block` AS `min_sort_block`,(case when (((0 <> `t`.`allow_early`) is false) and (`t`.`sort_order` > `t`.`min_sort_top`)) then 1 when (`t`.`min_sort_block` is null) then 0 when (`t`.`sort_order` > `t`.`min_sort_block`) then 1 else 0 end) AS `is_blocked`,`t`.`checklist_id` AS `checklist_id`,`t`.`checklist_name` AS `checklist_name`,`t`.`is_checklist_complete` AS `is_checklist_complete`,`t`.`checklist_due_date` AS `checklist_due_date`,`t`.`checklist_assigned_to_id` AS `checklist_assigned_to_id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `checklist_owner`,`a`.`is_complete` AS `is_activity_complete`,`a`.`assigned_to_id` AS `activity_owner_id`,(case when (`a`.`DTYPE` = 'Renewal') then 1 when (`a`.`DTYPE` = 'Setup') then 1 when (`a`.`DTYPE` = 'Ticket') then 1 else 0 end) AS `is_activity`,`a`.`due_date` AS `activity_due_date`,`t`.`has_goto` AS `has_goto`,`t`.`goto_link_id` AS `goto_link_id`,`t`.`has_info` AS `has_info`,`t`.`info_link_id` AS `info_link_id`,`t`.`auto_id` AS `auto_id`,`t`.`has_automation` AS `has_automation`,`t`.`bpo_registration_id` AS `bpo_registration_id` from (`td_base_04` `t` join `assignee` `a` on((`t`.`checklist_assigned_to_id` = `a`.`id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `td_count_1`
--

/*!50001 DROP VIEW IF EXISTS `td_count_1`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `td_count_1` AS select count(`t`.`todo_id`) AS `todo_count`,`t`.`checklist_id` AS `checklist_id` from `todo` `t` group by `t`.`checklist_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `td_top_block`
--

/*!50001 DROP VIEW IF EXISTS `td_top_block`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `td_top_block` AS select `b`.`id` AS `id`,min(`b`.`sort_order`) AS `min_sort` from `td_top_block_a` `b` group by `b`.`id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `td_top_block_a`
--

/*!50001 DROP VIEW IF EXISTS `td_top_block_a`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `td_top_block_a` AS select `b`.`checklist_id` AS `id`,`b`.`allow_future` AS `allow_future`,`b`.`sort_order` AS `sort_order` from `td_all_open` `b` where (`b`.`allow_future` = false) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `td_top_item`
--

/*!50001 DROP VIEW IF EXISTS `td_top_item`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `td_top_item` AS select `b`.`checklist_id` AS `id`,min(`b`.`sort_order`) AS `min_sort` from `td_all_open` `b` group by `b`.`checklist_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `tdx_open_activity`
--

/*!50001 DROP VIEW IF EXISTS `tdx_open_activity`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `tdx_open_activity` AS select `t`.`todo_id` AS `todo_id`,`t`.`task_id` AS `task_id`,`t`.`name` AS `name`,(case when (((`t`.`has_owner` = true) or (`t`.`is_sourced` = true)) and (`t`.`owner_id` is not null) and (`t`.`owner_id` <> `t`.`activity_owner_id`)) then 1 else 0 end) AS `not_me`,`t`.`has_owner` AS `has_owner`,`t`.`is_sourced` AS `is_sourced`,`t`.`owner_id` AS `owner_id`,`t`.`allow_non_owner` AS `allow_non_owner`,`t`.`allow_early` AS `allow_early`,`t`.`allow_future` AS `allow_future`,`t`.`sort_order` AS `sort_order`,`t`.`min_sort_top` AS `min_sort_top`,`t`.`at_top` AS `at_top`,`t`.`min_sort_block` AS `min_sort_block`,`t`.`is_blocked` AS `is_blocked`,`t`.`checklist_id` AS `checklist_id`,`t`.`checklist_name` AS `checklist_name`,`t`.`checklist_owner` AS `checklist_owner`,`t`.`is_checklist_complete` AS `is_checklist_complete`,`t`.`checklist_due_date` AS `checklist_due_date`,`t`.`checklist_assigned_to_id` AS `checklist_assigned_to_id`,`t`.`DTYPE` AS `DTYPE`,`t`.`is_activity` AS `is_activity`,`t`.`is_activity_complete` AS `is_activity_complete`,`t`.`activity_owner_id` AS `activity_owner_id`,`t`.`activity_due_date` AS `activity_due_date`,`t`.`has_goto` AS `has_goto`,`t`.`goto_link_id` AS `goto_link_id`,`t`.`has_info` AS `has_info`,`t`.`info_link_id` AS `info_link_id`,`t`.`auto_id` AS `auto_id`,`t`.`has_automation` AS `has_automation`,`t`.`bpo_registration_id` AS `bpo_registration_id` from `td_base_05` `t` where (((`t`.`DTYPE` = 'Renewal') or (`t`.`DTYPE` = 'Setup') or (`t`.`DTYPE` = 'Ticket')) and (`t`.`is_activity_complete` = false)) order by `t`.`checklist_due_date`,`t`.`checklist_id`,`t`.`sort_order` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `tdx_open_list`
--

/*!50001 DROP VIEW IF EXISTS `tdx_open_list`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `tdx_open_list` AS select `t`.`todo_id` AS `todo_id`,`t`.`task_id` AS `task_id`,`t`.`name` AS `name`,`t`.`has_owner` AS `has_owner`,`t`.`is_sourced` AS `is_sourced`,`t`.`owner_id` AS `owner_id`,`t`.`allow_non_owner` AS `allow_non_owner`,`t`.`allow_early` AS `allow_early`,`t`.`allow_future` AS `allow_future`,`t`.`sort_order` AS `sort_order`,`t`.`min_sort_top` AS `min_sort_top`,`t`.`at_top` AS `at_top`,`t`.`min_sort_block` AS `min_sort_block`,`t`.`is_blocked` AS `is_blocked`,`t`.`checklist_id` AS `checklist_id`,`t`.`checklist_name` AS `checklist_name`,`t`.`is_checklist_complete` AS `is_checklist_complete`,`t`.`checklist_due_date` AS `checklist_due_date`,`t`.`checklist_assigned_to_id` AS `checklist_assigned_to_id`,`t`.`DTYPE` AS `DTYPE`,`t`.`is_activity_complete` AS `is_activity_complete`,`t`.`activity_owner_id` AS `activity_owner_id`,`t`.`checklist_owner` AS `checklist_owner`,`t`.`has_goto` AS `has_goto`,`t`.`goto_link_id` AS `goto_link_id`,`t`.`has_info` AS `has_info`,`t`.`info_link_id` AS `info_link_id`,`t`.`auto_id` AS `auto_id`,`t`.`has_automation` AS `has_automation`,`t`.`bpo_registration_id` AS `bpo_registration_id`,(case when (((`t`.`has_owner` = true) or (`t`.`is_sourced` = true)) and (`t`.`owner_id` is not null) and (`t`.`owner_id` <> `t`.`checklist_assigned_to_id`)) then 1 else 0 end) AS `not_me` from `td_base_05` `t` where ((`t`.`DTYPE` = 'Person') and (`t`.`is_checklist_complete` = false)) order by `t`.`checklist_due_date`,`t`.`checklist_id`,`t`.`sort_order`,`t`.`task_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `test_01`
--

/*!50001 DROP VIEW IF EXISTS `test_01`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `test_01` AS select `a`.`id` AS `id`,`a`.`DTYPE` AS `DTYPE`,`a`.`full_name` AS `full_name`,`a`.`employee_id` AS `employee_id`,`a`.`date_completed` AS `date_completed`,`a`.`due_date` AS `due_date`,`a`.`is_complete` AS `is_complete`,`a`.`assigned_to_id` AS `assigned_to_id`,`a`.`completed_by_id` AS `completed_by_id`,`a`.`created_by_id` AS `created_by_id`,`a`.`employer_id` AS `employer_id`,`a`.`checklist_id` AS `checklist_id`,`n`.`max_note` AS `max_note`,`n`.`activity_id` AS `activity_id`,`n2`.`status_id` AS `status_id`,`x`.`max_note1` AS `max_note1` from (((`assignee` `a` left join (select max(`n1`.`note_id`) AS `max_note`,`n1`.`activity_id` AS `activity_id`,`n1`.`status_id` AS `status_id` from `note` `n1` group by `n1`.`activity_id` having (`n1`.`status_id` <> 2)) `n` on((`a`.`id` = `n`.`activity_id`))) left join `note` `n2` on((`n`.`max_note` = `n2`.`note_id`))) left join (select max(`n3`.`note_id`) AS `max_note1`,`n3`.`activity_id` AS `act_id`,`n3`.`reason_id` AS `reason_id` from `note` `n3` group by `n3`.`activity_id` having (`n3`.`reason_id` = 1)) `x` on((`a`.`id` = `x`.`act_id`))) where (((`a`.`DTYPE` = 'Setup') or (`a`.`DTYPE` = 'Renewal') or (`a`.`DTYPE` = 'Ticket')) and (`a`.`is_complete` = false)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `todo_out_04`
--

/*!50001 DROP VIEW IF EXISTS `todo_out_04`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `todo_out_04` AS select row_number() OVER (PARTITION BY `t`.`checklist_id` ORDER BY `t`.`checklist_id`,`t`.`is_complete`,`t`.`sort_order`,`t`.`todo_id` )  AS `sequential_count`,`t`.`checklist_id` AS `checklist_id`,`t`.`is_complete` AS `is_complete`,`t`.`sort_order` AS `sort_order`,`t`.`todo_id` AS `todo_id`,`t`.`task_id` AS `task_id`,`tk`.`DESCRIPTION` AS `DESCRIPTION`,`tk`.`has_automation` AS `has_automation`,`tk`.`auto_id` AS `auto_id`,`tk`.`servlet_name` AS `servlet_name`,`tk`.`automation_text` AS `automation_text`,`tk`.`allow_early` AS `allow_early`,`tk`.`allow_future` AS `allow_future`,`tk`.`allow_non_owner` AS `allow_non_owner`,`tk`.`has_owner` AS `has_owner`,`tk`.`owner_id` AS `owner_id`,`tk`.`is_sourced` AS `is_sourced`,`tk`.`bpo_registration_id` AS `bpo_registration_id`,`tk`.`has_goto` AS `has_goto`,`tk`.`goto_link_id` AS `goto_link_id`,`tk`.`has_info` AS `has_info`,`tk`.`info_link_id` AS `info_link_id`,max((case when (`tk`.`allow_future` = 0) then 1 else 0 end)) OVER (PARTITION BY `t`.`checklist_id` ORDER BY `t`.`sort_order` ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)  AS `has_future_block` from (`todo` `t` join `task` `tk` on((`t`.`task_id` = `tk`.`task_id`))) order by `t`.`checklist_id`,`t`.`is_complete`,`t`.`sort_order`,`t`.`todo_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `todo_out_05`
--

/*!50001 DROP VIEW IF EXISTS `todo_out_05`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `todo_out_05` AS select `subquery`.`sequential_count` AS `sequential_count`,`subquery`.`checklist_id` AS `checklist_id`,`subquery`.`is_complete` AS `is_complete`,`subquery`.`sort_order` AS `sort_order`,`subquery`.`todo_id` AS `todo_id`,`subquery`.`task_id` AS `task_id`,`subquery`.`DESCRIPTION` AS `DESCRIPTION`,`subquery`.`has_automation` AS `has_automation`,`subquery`.`auto_id` AS `auto_id`,`subquery`.`servlet_name` AS `servlet_name`,`subquery`.`automation_text` AS `automation_text`,`subquery`.`allow_early` AS `allow_early`,`subquery`.`allow_future` AS `allow_future`,`subquery`.`allow_non_owner` AS `allow_non_owner`,`subquery`.`has_owner` AS `has_owner`,`subquery`.`owner_id` AS `owner_id`,`subquery`.`is_sourced` AS `is_sourced`,`subquery`.`bpo_registration_id` AS `bpo_registration_id`,`subquery`.`has_goto` AS `has_goto`,`subquery`.`goto_link_id` AS `goto_link_id`,`subquery`.`has_info` AS `has_info`,`subquery`.`info_link_id` AS `info_link_id`,max((case when (`subquery`.`prev_allow_future` = 0) then 1 else 0 end)) OVER (PARTITION BY `subquery`.`checklist_id` ORDER BY `subquery`.`sort_order` ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)  AS `has_future_block` from (select row_number() OVER (PARTITION BY `t`.`checklist_id` ORDER BY `t`.`checklist_id`,`t`.`is_complete`,`t`.`sort_order`,`t`.`todo_id` )  AS `sequential_count`,`t`.`checklist_id` AS `checklist_id`,`t`.`is_complete` AS `is_complete`,`t`.`sort_order` AS `sort_order`,`t`.`todo_id` AS `todo_id`,`t`.`task_id` AS `task_id`,`tk`.`DESCRIPTION` AS `DESCRIPTION`,`tk`.`has_automation` AS `has_automation`,`tk`.`auto_id` AS `auto_id`,`tk`.`servlet_name` AS `servlet_name`,`tk`.`automation_text` AS `automation_text`,`tk`.`allow_early` AS `allow_early`,`tk`.`allow_future` AS `allow_future`,`tk`.`allow_non_owner` AS `allow_non_owner`,`tk`.`has_owner` AS `has_owner`,`tk`.`owner_id` AS `owner_id`,`tk`.`is_sourced` AS `is_sourced`,`tk`.`bpo_registration_id` AS `bpo_registration_id`,`tk`.`has_goto` AS `has_goto`,`tk`.`goto_link_id` AS `goto_link_id`,`tk`.`has_info` AS `has_info`,`tk`.`info_link_id` AS `info_link_id`,lag(`tk`.`allow_future`,1,1) OVER (PARTITION BY `t`.`checklist_id` ORDER BY `t`.`sort_order` )  AS `prev_allow_future` from (`todo` `t` join `task` `tk` on((`t`.`task_id` = `tk`.`task_id`)))) `subquery` order by `subquery`.`checklist_id`,`subquery`.`is_complete`,`subquery`.`sort_order`,`subquery`.`todo_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `todo_out_06`
--

/*!50001 DROP VIEW IF EXISTS `todo_out_06`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `todo_out_06` AS select `subquery`.`sequential_count` AS `sequential_count`,`subquery`.`checklist_id` AS `checklist_id`,`subquery`.`is_complete` AS `is_complete`,`subquery`.`sort_order` AS `sort_order`,`subquery`.`todo_id` AS `todo_id`,`subquery`.`task_id` AS `task_id`,`subquery`.`DESCRIPTION` AS `DESCRIPTION`,`subquery`.`has_automation` AS `has_automation`,`subquery`.`auto_id` AS `auto_id`,`subquery`.`servlet_name` AS `servlet_name`,`subquery`.`automation_text` AS `automation_text`,`subquery`.`allow_early` AS `allow_early`,`subquery`.`allow_future` AS `allow_future`,`subquery`.`allow_non_owner` AS `allow_non_owner`,`subquery`.`has_owner` AS `has_owner`,`subquery`.`owner_id` AS `owner_id`,`subquery`.`is_sourced` AS `is_sourced`,`subquery`.`bpo_registration_id` AS `bpo_registration_id`,`subquery`.`has_goto` AS `has_goto`,`subquery`.`goto_link_id` AS `goto_link_id`,`subquery`.`has_info` AS `has_info`,`subquery`.`info_link_id` AS `info_link_id`,max((case when (`subquery`.`prev_allow_future` = 0) then 1 else 0 end)) OVER (PARTITION BY `subquery`.`checklist_id` ORDER BY `subquery`.`sort_order` ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)  AS `has_future_block` from (select row_number() OVER (PARTITION BY `t`.`checklist_id` ORDER BY `t`.`checklist_id`,`t`.`is_complete`,`t`.`sort_order`,`t`.`todo_id` )  AS `sequential_count`,`t`.`checklist_id` AS `checklist_id`,`t`.`is_complete` AS `is_complete`,`t`.`sort_order` AS `sort_order`,`t`.`todo_id` AS `todo_id`,`t`.`task_id` AS `task_id`,`tk`.`DESCRIPTION` AS `DESCRIPTION`,`tk`.`has_automation` AS `has_automation`,`tk`.`auto_id` AS `auto_id`,`tk`.`servlet_name` AS `servlet_name`,`tk`.`automation_text` AS `automation_text`,`tk`.`allow_early` AS `allow_early`,`tk`.`allow_future` AS `allow_future`,`tk`.`allow_non_owner` AS `allow_non_owner`,`tk`.`has_owner` AS `has_owner`,`tk`.`owner_id` AS `owner_id`,`tk`.`is_sourced` AS `is_sourced`,`tk`.`bpo_registration_id` AS `bpo_registration_id`,`tk`.`has_goto` AS `has_goto`,`tk`.`goto_link_id` AS `goto_link_id`,`tk`.`has_info` AS `has_info`,`tk`.`info_link_id` AS `info_link_id`,lag((case when ((`tk`.`allow_future` = 0) and (`t`.`is_complete` = 0)) then 0 else 1 end)) OVER (PARTITION BY `t`.`checklist_id` ORDER BY `t`.`sort_order` )  AS `prev_allow_future` from (`todo` `t` join `task` `tk` on((`t`.`task_id` = `tk`.`task_id`)))) `subquery` order by `subquery`.`checklist_id`,`subquery`.`is_complete`,`subquery`.`sort_order`,`subquery`.`todo_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-02 13:48:52
