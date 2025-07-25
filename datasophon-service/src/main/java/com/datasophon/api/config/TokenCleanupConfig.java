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

import com.datasophon.api.service.AuthTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 令牌清理配置
 * 定期清理过期的令牌
 */
@Configuration
@EnableScheduling
public class TokenCleanupConfig {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupConfig.class);

    @Autowired
    private AuthTokenService authTokenService;

    /**
     * 每天凌晨3点执行清理过期令牌的任务
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredTokens() {
        try {
            int deletedCount = authTokenService.cleanupExpiredTokens();
            logger.info("清理过期令牌任务执行完成，共删除{}条记录", deletedCount);
        } catch (Exception e) {
            logger.error("清理过期令牌任务执行失败", e);
        }
    }
}