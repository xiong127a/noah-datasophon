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

package com.datasophon.api.service;

import com.datasophon.common.web.HeaderContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 请求头上下文服务类
 * 演示如何在服务层中获取和使用请求头信息
 */
@Service
public class HeaderContextService {

    private static final Logger logger = LoggerFactory.getLogger(HeaderContextService.class);

    /**
     * 获取当前请求的用户ID
     *
     * @return 用户ID，如果不存在则返回null
     */
    public String getCurrentUserId() {
        return HeaderContextHolder.getHeader("X-User-Id");
    }

    /**
     * 获取租户ID
     *
     * @return 租户ID，如果不存在则返回null
     */
    public String getCurrentTenantId() {
        return HeaderContextHolder.getHeader("X-Tenant-Id");
    }

    /**
     * 获取Grafana主机
     *
     * @return Grafana主机，如果不存在则返回null
     */
    public String getGrafanaHost() {
        return HeaderContextHolder.getHeader("grafanaHost");
    }

    /**
     * 记录所有请求头，演示在服务层中访问所有请求头
     */
    public void logAllHeaders() {
        Map<String, String> headers = HeaderContextHolder.getAllHeaders();
        logger.info("当前请求头: {}", headers);
    }

    /**
     * 演示在事务中使用请求头
     */
    @Transactional(rollbackFor = Exception.class)
    public void processWithTransaction() {
        String userId = getCurrentUserId();
        String tenantId = getCurrentTenantId();

        logger.info("在事务中处理请求，用户ID: {}, 租户ID: {}", userId, tenantId);

        // 可以在这里添加实际的业务逻辑
        // 例如保存用户操作日志、更新数据等
    }
}