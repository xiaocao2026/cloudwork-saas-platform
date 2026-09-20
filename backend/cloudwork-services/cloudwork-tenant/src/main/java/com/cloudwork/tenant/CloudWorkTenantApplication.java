package com.cloudwork.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.security.annotation.EnableRyFeignClients;

/**
 * CloudWork租户服务
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class CloudWorkTenantApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(CloudWorkTenantApplication.class, args);
    }
}
