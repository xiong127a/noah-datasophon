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

package com.datasophon.plugins.systeminfo;

import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.api.SystemInfoCollectorPlugin;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.SystemInfo;
import com.datasophon.plugins.api.service.SshConnectionService;
import com.datasophon.plugins.systeminfo.strategy.OsInfoCollectionStrategy;
import com.datasophon.plugins.systeminfo.strategy.OsInfoCollectionStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 系统信息收集插件实现
 * 负责收集各种系统信息，供检查和修复插件使用
 * 
 * 分层调用架构：
 * 1. 检查插件或修复插件调用信息收集插件
 * 2. 信息收集插件调用SSH插件执行命令
 * 3. 信息收集插件解析命令结果并返回结构化数据
 * 
 * 设计原则：
 * - 信息收集插件不直接处理SSH连接，通过SSH插件执行命令
 * - 信息收集插件专注于命令生成、结果解析和数据结构化
 * - 支持多种操作系统，内部使用策略模式处理OS差异
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Extension
@Component
@RequiredArgsConstructor
public class SystemInfoCollectorPluginImpl implements SystemInfoCollectorPlugin {

    private final SshConnectionService sshConnectionService;
    private final OsInfoCollectionStrategyFactory strategyFactory;

    @Override
    public CompletableFuture<SystemInfo> collectSystemInfo(HostCheckContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("开始收集系统信息: hostIp={}", context.getHostIp());
                
                // 使用上下文中的操作系统类型，如果没有则检测
                OsType osType = context.getOsType();
                if (osType == null) {
                    osType = detectOperatingSystem(context);
                    if (osType == null) {
                        throw new IllegalStateException("无法检测操作系统类型");
                    }
                    // 更新上下文中的OS类型
                    context.setOsType(osType);
                }
                
                // 获取对应的策略
                OsInfoCollectionStrategy strategy = strategyFactory.getStrategy(osType);
                if (strategy == null) {
                    throw new IllegalStateException("不支持的操作系统类型: " + osType);
                }
                
                // 收集系统信息
                SystemInfo systemInfo = strategy.collectSystemInfo(context, sshConnectionService);
                
                log.debug("系统信息收集完成: hostIp={}, osType={}", context.getHostIp(), osType);
                return systemInfo;
                
            } catch (Exception e) {
                log.error("收集系统信息失败: hostIp={}, error={}", context.getHostIp(), e.getMessage(), e);
                throw new RuntimeException("系统信息收集失败: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public boolean isHealthy() {
        try {
            return sshConnectionService != null && strategyFactory != null;
        } catch (Exception e) {
            log.error("检查插件健康状态失败", e);
            return false;
        }
    }

    @Override
    public String getPluginId() {
        return "system-info-collector";
    }

    /**
     * 检测操作系统类型
     */
    private OsType detectOperatingSystem(HostCheckContext context) {
        try {
            // 执行操作系统检测命令
            String command = "cat /etc/os-release || cat /etc/redhat-release || uname -a";
            var result = sshConnectionService.executeCommand(context, command);
            
            if (result == null || !result.isSuccess() || result.output().trim().isEmpty()) {
                log.warn("无法获取操作系统信息: hostIp={}", context.getHostIp());
                return null;
            }
            
            String osInfo = result.output().toLowerCase();
            
            // 根据输出内容判断操作系统类型
            if (osInfo.contains("centos") || osInfo.contains("red hat") || osInfo.contains("rhel")) {
                return OsType.CENTOS;
            } else if (osInfo.contains("ubuntu")) {
                return OsType.UBUNTU;
            } else if (osInfo.contains("kylin") || osInfo.contains("neokylin")) {
                return OsType.KYLIN;
            }
            
            log.warn("未识别的操作系统类型: hostIp={}, osInfo={}", context.getHostIp(), osInfo);
            return null;
            
        } catch (Exception e) {
            log.error("检测操作系统类型失败: hostIp={}, error={}", context.getHostIp(), e.getMessage(), e);
            return null;
        }
    }
}
