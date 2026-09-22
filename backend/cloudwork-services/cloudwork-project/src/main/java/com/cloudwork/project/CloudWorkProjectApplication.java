package com.cloudwork.project;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.security.annotation.EnableRyFeignClients;

@EnableCustomConfig
@EnableRyFeignClients(basePackages = "com.cloudwork.project.remote")
@MapperScan("com.cloudwork.project.mapper")
@SpringBootApplication
public class CloudWorkProjectApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(CloudWorkProjectApplication.class, args);
    }
}
