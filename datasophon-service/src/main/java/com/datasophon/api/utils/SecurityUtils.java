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

package com.datasophon.api.utils;

import com.datasophon.dao.entity.UserInfoEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * JWT认证安全工具类
 */
@Component
public class SecurityUtils implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SecurityUtils.applicationContext = applicationContext;
    }

    /**
     * 获取当前登录用户名
     * 
     * @return 用户名，如果未认证则返回null
     */
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            logger.debug("获取当前用户名: {}", username);
            return username;
        }
        logger.warn("未获取到当前用户名");
        return null;
    }

    /**
     * 获取当前登录用户ID
     * 
     * @return 用户ID，如果未找到则返回null
     */
    public static Integer getUserId() {
        UserInfoEntity user = getAuthUser();
        if (user != null && user.getId() != null) {
            return user.getId();
        }
        logger.warn("未获取到当前用户ID");
        return null;
    }

    /**
     * 判断用户是否为管理员
     * 
     * @param userInfoEntity 用户实体
     * @return 是否为管理员
     */
    public static boolean isAdmin(UserInfoEntity userInfoEntity) {
        if (userInfoEntity == null || userInfoEntity.getId() == null) {
            return false;
        }
        // 根据实际业务逻辑调整管理员的判定条件
        return userInfoEntity.getId() == 1;
    }

    /**
     * 获取当前认证用户的完整信息
     * 
     * @return 用户实体，如果未认证则返回null
     */
    public static UserInfoEntity getAuthUser() {
        try {
            // 从Security上下文获取认证信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("未获取到认证信息或用户未认证");
                return null;
            }

            // 获取Principal
            Object principal = authentication.getPrincipal();

            // 如果Principal直接是UserInfoEntity
            if (principal instanceof UserInfoEntity) {
                return (UserInfoEntity) principal;
            }

            // 获取用户名
            String username = null;
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                username = (String) principal;
            } else {
                logger.warn("无法从Principal中获取用户名: {}", principal);
                return null;
            }

            // 通过用户名查询用户信息
            if (username != null && applicationContext != null) {
                try {
                    // 通过Spring上下文获取用户服务
                    Object userService = applicationContext.getBean("userInfoService");
                    if (userService != null) {
                        // 通过反射调用getUserByUsername方法
                        java.lang.reflect.Method method = userService.getClass().getMethod("getUserByUsername",
                                String.class);
                        Object result = method.invoke(userService, username);
                        if (result instanceof UserInfoEntity) {
                            return (UserInfoEntity) result;
                        }
                    }
                } catch (Exception e) {
                    logger.error("通过用户名获取用户信息失败: {}", username, e);
                }
            }

            logger.warn("未能获取用户信息: {}", username);
        } catch (Exception e) {
            logger.error("获取当前认证用户失败", e);
        }

        return null;
    }

    /**
     * 判断当前用户是否已认证
     * 
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
