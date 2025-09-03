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

import com.datasophon.plugins.api.SystemInfoCollector;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.SystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;
import org.pf4j.spring.SpringPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.CompletableFuture;

/**
 * 系统信息收集插件 - 官方pf4j-spring标准结构
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
public class SystemInfoCollectorPlugin extends SpringPlugin {
    
    public SystemInfoCollectorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    protected ApplicationContext createApplicationContext() {
        log.info("创建系统信息收集插件Spring上下文");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.setClassLoader(getWrapper().getPluginClassLoader());
        applicationContext.scan("com.datasophon.plugins.systeminfo");
        applicationContext.refresh();
        log.info("系统信息收集插件Spring上下文创建完成");
        return applicationContext;
    }
    
    @Override
    public void start() {
        log.info("系统信息收集插件启动: {}", wrapper.getPluginId());
        super.start();
    }
    
    @Override
    public void stop() {
        log.info("系统信息收集插件停止: {}", wrapper.getPluginId());
        super.stop();
    }
    
    /**
     * 系统信息收集扩展实现
     */
    @Extension
    public static class SystemInfoExtension implements SystemInfoCollector {
        
        @Override
        public CompletableFuture<SystemInfo> collectSystemInfo(HostCheckContext context) {
            return CompletableFuture.supplyAsync(() -> {
                log.info("收集系统信息 for host: {}", context.getHostname() != null ? context.getHostname() : context.getHostIp());
                
                // 这里实现具体的系统信息收集逻辑
                SystemInfo systemInfo = new SystemInfo();
                // 设置系统信息...
                
                return systemInfo;
            });
        }
        
        @Override
        public String getPluginId() {
            return "system-info-collector";
        }
        
        @Override
        public void initialize() {
            log.info("系统信息收集插件扩展初始化: {}", getPluginId());
        }
        
        @Override
        public void cleanup() {
            log.info("系统信息收集插件扩展清理: {}", getPluginId());
        }
    }
}
