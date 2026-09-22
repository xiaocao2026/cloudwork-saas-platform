-- CloudWork Project Service schema V1
CREATE DATABASE IF NOT EXISTS `cloudwork_project` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `cloudwork_project`;

DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `project`;

CREATE TABLE `project` (
  `project_id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `project_code` VARCHAR(64) NOT NULL,
  `project_name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `created_by_user_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_project_tenant_status` (`tenant_id`, `status`),
  KEY `idx_project_tenant_creator` (`tenant_id`, `created_by_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `task` (
  `task_id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `project_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` VARCHAR(1000) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'TODO',
  `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  `assignee_user_id` BIGINT DEFAULT NULL,
  `created_by_user_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`task_id`),
  KEY `idx_task_tenant_project` (`tenant_id`, `project_id`),
  KEY `idx_task_tenant_project_status` (`tenant_id`, `project_id`, `status`),
  KEY `idx_task_tenant_assignee` (`tenant_id`, `assignee_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
