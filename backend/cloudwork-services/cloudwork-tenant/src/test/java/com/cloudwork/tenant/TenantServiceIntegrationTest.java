package com.cloudwork.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.cloudwork.tenant.constant.TenantConstants;
import com.cloudwork.tenant.dto.CreateTenantRequest;
import com.cloudwork.tenant.service.TenantService;
import com.cloudwork.tenant.vo.TenantSummaryVo;
import com.ruoyi.common.core.exception.ServiceException;

/**
 * MySQL-backed tests for creation and membership-scoped reads.
 */
class TenantServiceIntegrationTest extends TenantIntegrationTestSupport
{
    @Autowired
    private TenantService tenantService;

    @Test
    void createTenantPersistsTenantDefaultRolesAndOwnerMember()
    {
        CreateTenantRequest request = new CreateTenantRequest();
        request.setTenantName("Integration Workspace");

        TenantSummaryVo result = tenantService.createTenant(request);

        assertThat(count("tenant")).isEqualTo(1);
        assertThat(count("tenant_role")).isEqualTo(3);
        assertThat(count("tenant_member")).isEqualTo(1);
        assertThat(result.getTenantId()).isNotNull();
        assertThat(result.getTenantCode()).startsWith("cw_");
        assertThat(result.getTenantName()).isEqualTo("Integration Workspace");
        assertThat(result.getStatus()).isEqualTo(TenantConstants.TENANT_STATUS_ACTIVE);
        assertThat(result.getRoleCode()).isEqualTo(TenantConstants.ROLE_CODE_OWNER);

        Integer ownerCount = jdbcTemplate.queryForObject("select count(*) from tenant_member m "
                + "inner join tenant_role r on r.role_id = m.role_id and r.tenant_id = m.tenant_id "
                + "where m.tenant_id = ? and m.user_id = 1001 and m.member_status = 1 and r.role_code = 'OWNER'",
                Integer.class, result.getTenantId());
        assertThat(ownerCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("select role_code from tenant_role where tenant_id = ? order by role_code",
                String.class, result.getTenantId())).containsExactly("ADMIN", "MEMBER", "OWNER");
    }

    @Test
    void detailRequiresActiveMembershipAndCurrentUser()
    {
        insertTenant(10L, "cw_tenant_a", "Tenant A", 1001L);
        insertRole(100L, 10L, "OWNER");
        insertMember(1000L, 10L, 1001L, 100L, TenantConstants.MEMBER_STATUS_ACTIVE);
        insertMember(1001L, 10L, 2002L, 100L, 0);

        setCurrentUser(2002L);
        assertThatThrownBy(() -> tenantService.getAccessibleTenant(10L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Workspace不存在或无权访问");

        setCurrentUser(1001L);
        TenantSummaryVo result = tenantService.getAccessibleTenant(10L);
        assertThat(result.getTenantId()).isEqualTo(10L);
        assertThat(result.getRoleCode()).isEqualTo("OWNER");
    }

    @Test
    void listMineReturnsOnlyActiveMembershipsForCurrentUser()
    {
        insertTenant(10L, "cw_tenant_a", "Tenant A", 1001L);
        insertTenant(11L, "cw_tenant_b", "Tenant B", 1001L);
        insertTenant(12L, "cw_tenant_c", "Tenant C", 2002L);
        insertTenant(13L, "cw_tenant_d", "Tenant D", 1001L);
        insertRole(100L, 10L, "OWNER");
        insertRole(101L, 11L, "ADMIN");
        insertRole(102L, 12L, "MEMBER");
        insertRole(103L, 13L, "MEMBER");
        insertMember(1000L, 10L, 1001L, 100L, 1);
        insertMember(1001L, 11L, 1001L, 101L, 1);
        insertMember(1002L, 12L, 2002L, 102L, 1);
        insertMember(1003L, 13L, 1001L, 103L, 2);

        List<TenantSummaryVo> result = tenantService.listMine();

        assertThat(result).extracting(TenantSummaryVo::getTenantName)
                .containsExactlyInAnyOrder("Tenant A", "Tenant B");
        assertThat(result).extracting(TenantSummaryVo::getTenantName)
                .doesNotContain("Tenant C", "Tenant D");
    }
}
