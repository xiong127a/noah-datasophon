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

package com.datasophon.api.test;

import com.datasophon.api.service.PluginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 插件系统测试类
 * 基于官方pf4j-spring结构验证插件加载是否正常
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "datasophon.plugins.test.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class PluginSystemTest implements CommandLineRunner {

    @Autowired
    private PluginService pluginService;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== 开始测试官方pf4j-spring插件系统 ===");
        
        try {
            // 测试插件是否正确加载
            testPluginLoading();
            
            log.info("=== 插件系统测试完成 ✅ ===");
        } catch (Exception e) {
            log.error("=== 插件系统测试失败 ❌ ===", e);
        }
    }

    private void testPluginLoading() {
        log.info("📊 测试插件加载情况:");
        
        var hostValidationPlugins = pluginService.getHostValidationPlugins();
        var hostRepairPlugins = pluginService.getHostRepairPlugins();
        var sshConnectorPlugins = pluginService.getSshConnectorPlugins();
        var systemInfoPlugins = pluginService.getSystemInfoCollectorPlugins();
        
        log.info("✅ 主机验证插件: {} 个", hostValidationPlugins.size());
        log.info("✅ 主机修复插件: {} 个", hostRepairPlugins.size());
        log.info("✅ SSH连接插件: {} 个", sshConnectorPlugins.size());
        log.info("✅ 系统信息插件: {} 个", systemInfoPlugins.size());
        
        int totalPlugins = hostValidationPlugins.size() + hostRepairPlugins.size() + 
                          sshConnectorPlugins.size() + systemInfoPlugins.size();
        
        log.info("🎯 总计加载插件: {} 个", totalPlugins);
        
        if (totalPlugins > 0) {
            log.info("🎉 插件系统运行正常 - 官方pf4j-spring结构工作正常！");
        } else {
            log.warn("⚠️ 没有发现任何插件，请检查插件配置");
        }
    }
}
