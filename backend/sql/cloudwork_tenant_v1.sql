-- ============================================================
-- CloudWork Tenant Service
-- Schema Version: V1
-- Database: cloudwork_tenant
-- ============================================================

CREATE DATABASE IF NOT EXISTS `cloudwork_tenant`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `cloudwork_tenant`;


-- ============================================================
-- 1. Tenant / Workspace
-- ============================================================

DROP TABLE IF EXISTS `tenant`;

CREATE TABLE `tenant`
(
    `tenant_id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    `tenant_code`         VARCHAR(64)  NOT NULL COMMENT '租户稳定业务编码，全局唯一',
    `tenant_name`         VARCHAR(100) NOT NULL COMMENT '租户/Workspace名称',

    `status`              TINYINT      NOT NULL DEFAULT 0
        COMMENT '状态：0正常 1停用 2归档',

    `created_by_user_id`  BIGINT       NOT NULL COMMENT '创建者平台用户ID',

    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    `version`             INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `remark`              VARCHAR(500)          DEFAULT NULL,

    PRIMARY KEY (`tenant_id`),

    UNIQUE KEY `uk_tenant_code` (`tenant_code`),

    KEY `idx_tenant_status` (`status`),
    KEY `idx_tenant_creator` (`created_by_user_id`)
)
    ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci
COMMENT = 'CloudWork租户表';


-- ============================================================
-- 2. Tenant Role
-- ============================================================

DROP TABLE IF EXISTS `tenant_role`;

CREATE TABLE `tenant_role`
(
    `role_id`       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '租户角色ID',
    `tenant_id`     BIGINT      NOT NULL COMMENT '租户ID',

    `role_code`     VARCHAR(32) NOT NULL COMMENT '角色编码，如OWNER/ADMIN/MEMBER',
    `role_name`     VARCHAR(50) NOT NULL COMMENT '角色名称',

    `role_type`     TINYINT     NOT NULL DEFAULT 0
        COMMENT '角色类型：0系统角色 1自定义角色',

    `status`        TINYINT     NOT NULL DEFAULT 0
        COMMENT '状态：0正常 1停用',

    `sort_order`    INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `description`   VARCHAR(255)         DEFAULT NULL COMMENT '角色描述',

    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    `version`       INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本',

    PRIMARY KEY (`role_id`),

    UNIQUE KEY `uk_tenant_role_code`
        (`tenant_id`, `role_code`),

    KEY `idx_tenant_role_status`
        (`tenant_id`, `status`)
)
    ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci
COMMENT = 'CloudWork租户角色表';


-- ============================================================
-- 3. Tenant Member
-- ============================================================

DROP TABLE IF EXISTS `tenant_member`;

CREATE TABLE `tenant_member`
(
    `member_id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '租户成员ID',
    `tenant_id`           BIGINT   NOT NULL COMMENT '租户ID',
    `user_id`             BIGINT   NOT NULL COMMENT '平台用户ID',
    `role_id`             BIGINT   NOT NULL COMMENT '租户角色ID',

    `member_status`       TINYINT  NOT NULL DEFAULT 1
        COMMENT '成员状态：0待接受邀请 1正常 2停用 3已退出',

    `invited_by_user_id`  BIGINT            DEFAULT NULL COMMENT '邀请人用户ID',
    `invited_at`          DATETIME          DEFAULT NULL COMMENT '邀请时间',
    `joined_at`           DATETIME          DEFAULT NULL COMMENT '正式加入时间',

    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    `version`             INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',

    PRIMARY KEY (`member_id`),

    UNIQUE KEY `uk_tenant_member`
        (`tenant_id`, `user_id`),

    KEY `idx_member_user_status`
        (`user_id`, `member_status`),

    KEY `idx_member_tenant_status`
        (`tenant_id`, `member_status`),

    KEY `idx_member_tenant_role`
        (`tenant_id`, `role_id`)
)
    ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci
COMMENT = 'CloudWork租户成员表';