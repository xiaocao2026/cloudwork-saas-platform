package com.cloudwork.tenant.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cloudwork.tenant.dto.CreateTenantRequest;
import com.cloudwork.tenant.service.TenantService;
import com.cloudwork.tenant.vo.TenantAccessVo;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresLogin;

/**
 * Workspace endpoints, reachable through the future /tenant Gateway route.
 */
@RestController
@RequestMapping("/workspaces")
public class TenantController
{
    @Autowired
    private TenantService tenantService;

    @RequiresLogin
    @PostMapping
    public AjaxResult create(@Validated @RequestBody CreateTenantRequest request)
    {
        return AjaxResult.success(tenantService.createTenant(request));
    }

    @RequiresLogin
    @GetMapping("/mine")
    public AjaxResult mine()
    {
        return AjaxResult.success(tenantService.listMine());
    }

    @RequiresLogin
    @GetMapping("/{tenantId}")
    public AjaxResult detail(@PathVariable Long tenantId)
    {
        return AjaxResult.success(tenantService.getAccessibleTenant(tenantId));
    }
}
