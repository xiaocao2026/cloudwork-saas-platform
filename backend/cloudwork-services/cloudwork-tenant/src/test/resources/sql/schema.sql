-- This file mirrors backend/sql/cloudwork_tenant_v1.sql.
-- Keep it synchronized whenever the production schema changes.

CREATE TABLE tenant
(
    tenant_id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    tenant_name VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    created_by_user_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (tenant_id),
    UNIQUE KEY uk_tenant_code (tenant_code),
    KEY idx_tenant_status (status),
    KEY idx_tenant_creator (created_by_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tenant_role
(
    role_id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    role_type TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    description VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_tenant_role_code (tenant_id, role_code),
    KEY idx_tenant_role_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tenant_member
(
    member_id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    member_status TINYINT NOT NULL DEFAULT 1,
    invited_by_user_id BIGINT DEFAULT NULL,
    invited_at DATETIME DEFAULT NULL,
    joined_at DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_tenant_member (tenant_id, user_id),
    KEY idx_member_user_status (user_id, member_status),
    KEY idx_member_tenant_status (tenant_id, member_status),
    KEY idx_member_tenant_role (tenant_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
