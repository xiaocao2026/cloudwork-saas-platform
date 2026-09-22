package com.cloudwork.tenant.controller;

import com.cloudwork.tenant.service.TenantService;
import com.cloudwork.tenant.vo.TenantAccessVo;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/internal")
public class TenantInternalController {
    @Autowired
    private TenantService tenantService;
    @RequiresLogin
    @GetMapping("/access/{tenantId}")
    public R<TenantAccessVo> access(@PathVariable Long tenantId)
    {
        TenantAccessVo access = tenantService.checkAccess(tenantId);
        return R.ok(access);
    }
}
