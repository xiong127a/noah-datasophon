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

import com.datasophon.plugins.api.HostValidator;
import com.datasophon.plugins.api.HostRepairer;
import com.datasophon.plugins.api.SshConnector;
import com.datasophon.plugins.api.SystemInfoCollector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 插件业务服务类
 * 基于官方pf4j-spring实现，自动注入所有插件扩展
 * 演示如何在业务代码中使用插件
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Service
@DependsOn("extensionsInjector")
public class PluginService {

    // 自动注入所有HostValidator扩展
    @Autowired(required = false)
    private List<HostValidator> hostValidationPlugins;

    // 自动注入所有HostRepairer扩展
    @Autowired(required = false)
    private List<HostRepairer> hostRepairPlugins;

    // 自动注入所有SshConnector扩展
    @Autowired(required = false)
    private List<SshConnector> sshConnectorPlugins;

    // 自动注入所有SystemInfoCollector扩展
    @Autowired(required = false)
    private List<SystemInfoCollector> systemInfoCollectorPlugins;

    @PostConstruct
    public void printPluginInfo() {
        log.info("=== 插件系统加载完成 ===");
        
        int hostValidationCount = hostValidationPlugins != null ? hostValidationPlugins.size() : 0;
        int hostRepairCount = hostRepairPlugins != null ? hostRepairPlugins.size() : 0;
        int sshConnectorCount = sshConnectorPlugins != null ? sshConnectorPlugins.size() : 0;
        int systemInfoCount = systemInfoCollectorPlugins != null ? systemInfoCollectorPlugins.size() : 0;
        
        log.info("发现 {} 个主机验证插件: {}", hostValidationCount,
                hostValidationPlugins != null ? hostValidationPlugins.stream().map(p -> p.getClass().getSimpleName()).toList() : "[]");
        
        log.info("发现 {} 个主机修复插件: {}", hostRepairCount,
                hostRepairPlugins != null ? hostRepairPlugins.stream().map(p -> p.getClass().getSimpleName()).toList() : "[]");
        
        log.info("发现 {} 个SSH连接插件: {}", sshConnectorCount,
                sshConnectorPlugins != null ? sshConnectorPlugins.stream().map(p -> p.getClass().getSimpleName()).toList() : "[]");
        
        log.info("发现 {} 个系统信息收集插件: {}", systemInfoCount,
                systemInfoCollectorPlugins != null ? systemInfoCollectorPlugins.stream().map(p -> p.getClass().getSimpleName()).toList() : "[]");
        
        int totalPlugins = hostValidationCount + hostRepairCount + sshConnectorCount + systemInfoCount;
        log.info("总共加载了 {} 个插件扩展", totalPlugins);
        log.info("========================");
    }

    /**
     * 获取所有主机验证插件
     */
    public List<HostValidator> getHostValidationPlugins() {
        return hostValidationPlugins;
    }

    /**
     * 获取所有主机修复插件
     */
    public List<HostRepairer> getHostRepairPlugins() {
        return hostRepairPlugins;
    }

    /**
     * 获取所有SSH连接插件
     */
    public List<SshConnector> getSshConnectorPlugins() {
        return sshConnectorPlugins;
    }

    /**
     * 获取所有系统信息收集插件
     */
    public List<SystemInfoCollector> getSystemInfoCollectorPlugins() {
        return systemInfoCollectorPlugins;
    }

    /**
     * 根据插件ID获取特定的主机验证插件
     */
    public HostValidator getHostValidationPlugin(String pluginId) {
        return hostValidationPlugins.stream()
                .filter(plugin -> pluginId.equals(plugin.getPluginId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据插件ID获取特定的SSH连接插件
     */
    public SshConnector getSshConnectorPlugin(String pluginId) {
        return sshConnectorPlugins.stream()
                .filter(plugin -> pluginId.equals(plugin.getPluginId()))
                .findFirst()
                .orElse(null);
    }
}
