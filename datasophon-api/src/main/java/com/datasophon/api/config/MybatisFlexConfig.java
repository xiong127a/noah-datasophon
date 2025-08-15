/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.config;

import com.datasophon.common.enums.handler.ClusterTypeHandler;
import com.datasophon.dao.entity.base.BaseEntity;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.keygen.KeyGeneratorFactory;
import com.mybatisflex.core.keygen.impl.SnowFlakeIDKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

/**
  MyBatisFlex配置类
*/
@Configuration
@Slf4j
public class MybatisFlexConfig {
    
    @Autowired(required = false)
    private SqlSessionFactory sqlSessionFactory;
    
    // 使用静态代码块确保在类加载时就注册ID生成器，不依赖于Spring生命周期
    static {
        try {
            // 注册雪花算法ID生成器
            KeyGeneratorFactory.register("snowflakeId", new SnowFlakeIDKeyGenerator());
            System.out.println("成功注册雪花算法ID生成器: snowflakeId");
            log.info("成功注册雪花算法ID生成器: snowflakeId");
        } catch (Exception e) {
            System.err.println("注册雪花算法ID生成器失败: " + e.getMessage());
            log.error("注册雪花算法ID生成器失败", e);
        }
    }
    
    /**
     * 注册自定义类型处理器
     */
    @PostConstruct
    public void registerTypeHandlers() {
        if (sqlSessionFactory != null) {
            try {
                // 注册ClusterType枚举的类型处理器
                sqlSessionFactory.getConfiguration().getTypeHandlerRegistry()
                    .register(ClusterTypeHandler.class);
                log.info("成功注册ClusterType类型处理器");
            } catch (Exception e) {
                log.error("注册ClusterType类型处理器失败", e);
            }
        } else {
            log.warn("SqlSessionFactory为null，无法注册类型处理器");
        }
    }

    @Bean
    public FlexGlobalConfig flexGlobalConfig() {
        FlexGlobalConfig config = new FlexGlobalConfig();
        // 设置打印banner为true
        config.setPrintBanner(true);
        
        // 配置审计字段自动填充
        configureAuditFields();
        
        return config;
    }
    
    /**
     * 配置审计字段自动填充
     */
    private void configureAuditFields() {
        // 使用FlexGlobalConfig配置审计字段自动填充
        FlexGlobalConfig globalConfig = FlexGlobalConfig.getDefaultConfig();
        
        // 注册插入监听器
        globalConfig.registerInsertListener((entity) -> {
            if (entity instanceof BaseEntity baseEntity) {
                LocalDateTime now = LocalDateTime.now();
                String currentUser = getCurrentUser();
                
                if (baseEntity.getCreateTime() == null) {
                    baseEntity.setCreateTime(now);
                }
                if (baseEntity.getCreateBy() == null) {
                    baseEntity.setCreateBy(currentUser);
                }
                // 插入时也设置更新时间和更新人
                baseEntity.setUpdateTime(now);
                baseEntity.setUpdateBy(currentUser);
                
                log.debug("Insert audit: user={}, time={}", currentUser, now);
            }
        }, BaseEntity.class);
        
        // 注册更新监听器
        globalConfig.registerUpdateListener((entity) -> {
            if (entity instanceof BaseEntity baseEntity) {
                LocalDateTime now = LocalDateTime.now();
                String currentUser = getCurrentUser();
                
                baseEntity.setUpdateTime(now);
                baseEntity.setUpdateBy(currentUser);
                
                log.debug("Update audit: user={}, time={}", currentUser, now);
            }
        }, BaseEntity.class);
        
        log.info("已配置MyBatis-Flex审计字段自动填充");
    }
    
    /**
     * 获取当前登录用户
     */
    private String getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                // 排除匿名用户
                if (!"anonymousUser".equals(username)) {
                    return username;
                }
            }
        } catch (Exception e) {
            log.warn("获取当前用户失败: {}", e.getMessage());
        }
        // 默认返回系统用户
        return "system";
    }
}