package com.cloudwork.tenant.constant;

/**
 * Tenant table status and built-in role values from cloudwork_tenant_v1.sql.
 */
public final class TenantConstants
{
    public static final int TENANT_STATUS_ACTIVE = 0;

    public static final int ROLE_TYPE_SYSTEM = 0;

    public static final int ROLE_STATUS_ACTIVE = 0;

    public static final int MEMBER_STATUS_ACTIVE = 1;

    public static final String ROLE_CODE_OWNER = "OWNER";

    public static final String ROLE_CODE_ADMIN = "ADMIN";

    public static final String ROLE_CODE_MEMBER = "MEMBER";

    private TenantConstants()
    {
    }
}
