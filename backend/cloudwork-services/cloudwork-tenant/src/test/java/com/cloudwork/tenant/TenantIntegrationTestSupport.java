package com.cloudwork.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.system.api.model.LoginUser;

/**
 * Shared isolated MySQL and authenticated-user setup for tenant integration tests.
 */
@Testcontainers
@SpringBootTest(classes = CloudWorkTenantApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.config.import=",
                "mybatis.mapper-locations=classpath*:mapper/**/*.xml",
                "mybatis.type-aliases-package=com.cloudwork.tenant"
        })
@ActiveProfiles("test")
@Sql(scripts = "/sql/schema.sql", executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
public abstract class TenantIntegrationTestSupport
{
    @Container
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @MockitoBean
    @SuppressWarnings("unused")
    private RedisService redisService;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry)
    {
        registry.add("spring.datasource.dynamic.primary", () -> "master");
        registry.add("spring.datasource.dynamic.datasource.master.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.dynamic.datasource.master.username", MYSQL::getUsername);
        registry.add("spring.datasource.dynamic.datasource.master.password", MYSQL::getPassword);
        registry.add("spring.datasource.dynamic.datasource.master.driver-class-name", MYSQL::getDriverClassName);
    }

    @BeforeEach
    void cleanTablesAndSetDefaultUser()
    {
        jdbcTemplate.update("delete from tenant_member");
        jdbcTemplate.update("delete from tenant_role");
        jdbcTemplate.update("delete from tenant");
        setCurrentUser(1001L);
    }

    @AfterEach
    void clearCurrentUser()
    {
        SecurityContextHolder.remove();
    }

    protected void setCurrentUser(Long userId)
    {
        SecurityContextHolder.setUserId(String.valueOf(userId));
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(userId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    protected int count(String table)
    {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    protected void insertTenant(long tenantId, String code, String name, long creatorId)
    {
        jdbcTemplate.update("insert into tenant (tenant_id, tenant_code, tenant_name, status, created_by_user_id) "
                + "values (?, ?, ?, 0, ?)", tenantId, code, name, creatorId);
    }

    protected void insertRole(long roleId, long tenantId, String code)
    {
        jdbcTemplate.update("insert into tenant_role "
                + "(role_id, tenant_id, role_code, role_name, role_type, status, sort_order) "
                + "values (?, ?, ?, ?, 0, 0, ?)", roleId, tenantId, code, code, roleId);
    }

    protected void insertMember(long memberId, long tenantId, long userId, long roleId, int status)
    {
        jdbcTemplate.update("insert into tenant_member "
                + "(member_id, tenant_id, user_id, role_id, member_status, joined_at) "
                + "values (?, ?, ?, ?, ?, current_timestamp)", memberId, tenantId, userId, roleId, status);
    }
}
