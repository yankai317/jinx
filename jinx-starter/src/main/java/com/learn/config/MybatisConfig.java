package com.learn.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis配置类
 * 确保能正确扫描到MyBatis-Plus的配置
 */
@Configuration
@MapperScan("com.learn.infrastructure.repository.mapper")
@EnableTransactionManagement
public class MybatisConfig {
    // 配置已经足够，不需要额外配置
} 
