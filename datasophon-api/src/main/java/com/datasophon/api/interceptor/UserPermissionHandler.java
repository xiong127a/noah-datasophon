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

package com.datasophon.api.interceptor;

import com.datasophon.api.enums.Status;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.ClusterRoleUserService;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.dao.entity.UserInfoEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

public class UserPermissionHandler implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UserPermissionHandler.class);

    private final ClusterRoleUserService clusterUserService;

    private final UserInfoService userInfoService;
    @Autowired
    public UserPermissionHandler(ClusterRoleUserService clusterUserService, UserInfoService userInfoService) {
        this.clusterUserService = clusterUserService;
        this.userInfoService = userInfoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            UserPermission annotation = handlerMethod.getMethod().getAnnotation(UserPermission.class);
            if (annotation != null) {
                // 从Spring Security上下文中获取认证信息
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                    throw new ServiceException(Status.USER_NO_OPERATION_PERM);
                }

                // 获取用户信息
                String username = authentication.getName();
                UserInfoEntity authUser = userInfoService.getUserByUsername(username);

                if (authUser == null) {
                    throw new ServiceException(Status.USER_NO_OPERATION_PERM);
                }

                if (!SecurityUtils.isAdmin(authUser)) {
                    logger.info("Step into authorization");
                    Map<String, String[]> parameterMap = request.getParameterMap();
                    if (parameterMap.containsKey("clusterId")) {
                        logger.info("Find clusterId");
                        String[] clusterIds = parameterMap.get("clusterId");
                        if (!clusterUserService.isClusterManager(authUser.getId(), clusterIds[0])) {
                            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
                        }
                        logger.info("{} is cluster manager", authUser.getUsername());
                    }
                }
            }
        }
        return true;
    }
}