package com.cloudwork.tenant.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cloudwork.tenant.constant.TenantConstants;
import com.cloudwork.tenant.domain.Tenant;
import com.cloudwork.tenant.domain.TenantMember;
import com.cloudwork.tenant.domain.TenantRole;
import com.cloudwork.tenant.dto.CreateTenantRequest;
import com.cloudwork.tenant.mapper.TenantMapper;
import com.cloudwork.tenant.mapper.TenantMemberMapper;
import com.cloudwork.tenant.mapper.TenantRoleMapper;
import com.cloudwork.tenant.service.TenantService;
import com.cloudwork.tenant.vo.TenantSummaryVo;
import com.cloudwork.tenant.vo.TenantAccessVo;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.uuid.IdUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.api.model.LoginUser;

/**
 * Workspace creation and membership-scoped queries.
 */
@Service
public class TenantServiceImpl implements TenantService
{
    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private TenantRoleMapper tenantRoleMapper;

    @Autowired
    private TenantMemberMapper tenantMemberMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantSummaryVo createTenant(CreateTenantRequest request)
    {
        Long userId = currentUserId();
        String tenantName = request.getTenantName().trim();
        if (tenantName.length() < 2)
        {
            throw new ServiceException("Workspace名称长度必须在2到100个字符之间", HttpStatus.BAD_REQUEST);
        }

        Tenant tenant = new Tenant();
        tenant.setTenantCode("cw_" + IdUtils.fastSimpleUUID());
        tenant.setTenantName(tenantName);
        tenant.setStatus(TenantConstants.TENANT_STATUS_ACTIVE);
        tenant.setCreatedByUserId(userId);
        if (tenantMapper.insertTenant(tenant) != 1 || tenant.getTenantId() == null)
        {
            throw new ServiceException("创建Workspace失败");
        }

        TenantRole ownerRole = insertSystemRole(tenant.getTenantId(), TenantConstants.ROLE_CODE_OWNER, "Owner", 1);
        insertSystemRole(tenant.getTenantId(), TenantConstants.ROLE_CODE_ADMIN, "Admin", 2);
        insertSystemRole(tenant.getTenantId(), TenantConstants.ROLE_CODE_MEMBER, "Member", 3);

        TenantMember member = new TenantMember();
        member.setTenantId(tenant.getTenantId());
        member.setUserId(userId);
        member.setRoleId(ownerRole.getRoleId());
        member.setMemberStatus(TenantConstants.MEMBER_STATUS_ACTIVE);
        member.setJoinedAt(new Date());
        if (tenantMemberMapper.insertTenantMember(member) != 1)
        {
            throw new ServiceException("创建Workspace成员失败");
        }

        TenantSummaryVo summary = new TenantSummaryVo();
        summary.setTenantId(tenant.getTenantId());
        summary.setTenantCode(tenant.getTenantCode());
        summary.setTenantName(tenant.getTenantName());
        summary.setStatus(tenant.getStatus());
        summary.setRoleCode(ownerRole.getRoleCode());
        return summary;
    }

    @Override
    public List<TenantSummaryVo> listMine()
    {
        return tenantMapper.selectMine(currentUserId(), TenantConstants.MEMBER_STATUS_ACTIVE);
    }

    @Override
    public TenantSummaryVo getAccessibleTenant(Long tenantId)
    {
        TenantSummaryVo summary = tenantMapper.selectAccessibleById(tenantId, currentUserId(),
                TenantConstants.MEMBER_STATUS_ACTIVE);
        if (summary == null)
        {
            // Use one response for absent workspaces and workspaces owned by others.
            throw new ServiceException("Workspace不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        return summary;
    }

    @Override
    public TenantAccessVo checkAccess(Long tenantId)
    {
        Long userId = currentUserId();
        TenantAccessVo access = tenantMemberMapper.selectAccess(tenantId, userId,
                TenantConstants.MEMBER_STATUS_ACTIVE);
        if (access == null)
        {
            access = new TenantAccessVo();
            access.setTenantId(tenantId);
            access.setUserId(userId);
            access.setAccessible(false);
            return access;
        }
        access.setAccessible(true);
        return access;
    }

    private TenantRole insertSystemRole(Long tenantId, String roleCode, String roleName, int sortOrder)
    {
        TenantRole role = new TenantRole();
        role.setTenantId(tenantId);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setRoleType(TenantConstants.ROLE_TYPE_SYSTEM);
        role.setStatus(TenantConstants.ROLE_STATUS_ACTIVE);
        role.setSortOrder(sortOrder);
        if (tenantRoleMapper.insertTenantRole(role) != 1 || role.getRoleId() == null)
        {
            throw new ServiceException("创建Workspace默认角色失败");
        }
        return role;
    }

    private Long currentUserId()
    {
        Long userId = SecurityUtils.getUserId();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (userId == null || loginUser == null || !userId.equals(loginUser.getUserid()))
        {
            throw new ServiceException("登录身份无效", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }
}
