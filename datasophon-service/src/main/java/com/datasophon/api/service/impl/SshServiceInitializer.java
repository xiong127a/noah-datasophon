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

package com.datasophon.api.service.impl;

import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * SSH服务初始化器
 * 在应用完全就绪后初始化SSH连接服务，避免在Spring上下文初始化期间访问
 * 
 * @author 任相鹏
 * @date 2025-10-30
 */
@Component
public class SshServiceInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SshServiceInitializer.class);

    private volatile SshConnectionService sshService;

    /**
     * 监听ApplicationReadyEvent，在应用完全启动后初始化SSH服务
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            logger.info("开始初始化SSH连接服务...");
            this.sshService = SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
            logger.info("SSH连接服务初始化完成");
        } catch (Exception e) {
            logger.warn("SSH连接服务初始化失败，将在首次使用时重试: {}", e.getMessage());
        }
    }

    /**
     * 获取SSH服务实例
     * 如果未初始化，则尝试初始化
     */
    public SshConnectionService getSshService() {
        if (sshService == null) {
            synchronized (this) {
                if (sshService == null) {
                    logger.info("SSH服务未初始化，正在初始化...");
                    sshService = SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
                }
            }
        }
        return sshService;
    }
}

