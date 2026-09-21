package com.cloudwork.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.cloudwork.tenant.dto.CreateTenantRequest;
import com.cloudwork.tenant.mapper.TenantMemberMapper;
import com.cloudwork.tenant.service.TenantService;

/**
 * Verifies that a member insert failure rolls back real MySQL writes.
 */
class TenantServiceRollbackIntegrationTest extends TenantIntegrationTestSupport
{
    @Autowired
    private TenantService tenantService;

    @MockitoBean
    private TenantMemberMapper tenantMemberMapper;

    @Test
    void memberInsertFailureRollsBackTenantAndRoles()
    {
        doThrow(new RuntimeException("forced member insert failure"))
                .when(tenantMemberMapper).insertTenantMember(any());

        CreateTenantRequest request = new CreateTenantRequest();
        request.setTenantName("Rollback Workspace");

        assertThatThrownBy(() -> tenantService.createTenant(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("forced member insert failure");
        assertThat(count("tenant")).isZero();
        assertThat(count("tenant_role")).isZero();
        assertThat(count("tenant_member")).isZero();
    }
}
