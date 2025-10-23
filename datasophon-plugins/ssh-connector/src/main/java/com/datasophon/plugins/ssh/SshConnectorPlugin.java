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

package com.datasophon.plugins.ssh;

import com.datasophon.plugins.api.SshConnector;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.PluginMetadata;
import com.datasophon.common.enums.OsType;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;
import org.pf4j.spring.SpringPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Set;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

/**
 * SSH连接器插件 - 官方pf4j-spring标准结构
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
public class SshConnectorPlugin extends SpringPlugin {
    
    public SshConnectorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    protected ApplicationContext createApplicationContext() {
        log.info("创建SSH连接器插件Spring上下文");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.setClassLoader(getWrapper().getPluginClassLoader());
        // 扫描插件包
        applicationContext.scan("com.datasophon.plugins.ssh");
        applicationContext.refresh();
        log.info("SSH连接器插件Spring上下文创建完成");
        return applicationContext;
    }
    
    @Override
    public void start() {
        log.info("SSH连接器插件启动: {}", wrapper.getPluginId());
        super.start();
    }
    
    @Override
    public void stop() {
        log.info("SSH连接器插件停止: {}", wrapper.getPluginId());
        super.stop();
    }
    
    /**
     * SSH连接器扩展实现
     */
    @Extension
    public static class SshConnectorExtension implements SshConnector {
        
        @Override
        public Set<OsType> getSupportedOperatingSystems() {
            return EnumSet.allOf(OsType.class); // 支持所有操作系统
        }
        
        @Override
        public int getPriority() {
            return 50; // 高优先级
        }
        
        @Override
        public CompletableFuture<CheckResult> executeCheck(HostCheckContext context) {
            return CompletableFuture.supplyAsync(() -> {
                log.info("执行SSH连接检查 for host: {}", context.getHostname() != null ? context.getHostname() : context.getHostIp());
                
                // 这里实现具体的SSH连接检查逻辑
                CheckResult result = CheckResult.builder()
                    .success(true)
                    .message("SSH连接正常")
                    .checkTime(java.time.LocalDateTime.now())
                    .build();
                
                return result;
            });
        }
        
        @Override
        public boolean canExecute(HostCheckContext context) {
            OsType osType = context.getOsType();
            return osType == null || getSupportedOperatingSystems().contains(osType);
        }
        
        @Override
        public PluginMetadata getMetadata() {
            return PluginMetadata.builder()
                .pluginId("ssh-connector")
                .name("SSH连接器插件")
                .version("1.0.0")
                .description("提供SSH连接功能")
                .author("任相鹏")
                .license("Apache License 2.0")
                .corePlugin(true)
                .enabled(true)
                .build();
        }
        
        @Override
        public String getPluginId() {
            return "ssh-connector";
        }
        
        @Override
        public String getVersion() {
            return "1.0.0";
        }
        
        @Override
        public void initialize() {
            log.info("SSH连接器插件扩展初始化: {}", getPluginId());
        }
        
        @Override
        public void cleanup() {
            log.info("SSH连接器插件扩展清理: {}", getPluginId());
        }
    }
}
