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

package com.datasophon.plugins.hostrepair;

import com.datasophon.plugins.api.HostRepairer;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.common.enums.CheckType;
import com.datasophon.common.enums.OsType;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;
import org.pf4j.spring.SpringPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * 主机修复插件 - 官方pf4j-spring标准结构
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
public class HostRepairPlugin extends SpringPlugin {
    
    public HostRepairPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    protected ApplicationContext createApplicationContext() {
        log.info("创建主机修复插件Spring上下文");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.setClassLoader(getWrapper().getPluginClassLoader());
        applicationContext.scan("com.datasophon.plugins.hostrepair");
        applicationContext.refresh();
        log.info("主机修复插件Spring上下文创建完成");
        return applicationContext;
    }
    
    @Override
    public void start() {
        log.info("主机修复插件启动: {}", wrapper.getPluginId());
        super.start();
    }
    
    @Override
    public void stop() {
        log.info("主机修复插件停止: {}", wrapper.getPluginId());
        super.stop();
    }
    
    /**
     * 主机修复扩展实现
     */
    @Extension
    public static class HostRepairExtension implements HostRepairer {
        
        @Override
        public Set<OsType> getSupportedOperatingSystems() {
            return EnumSet.of(OsType.CENTOS, OsType.UBUNTU);
        }
        
        @Override
        public List<CheckType> getSupportedRepairTypes() {
            return Arrays.asList(
                CheckType.DISK_SPACE_CHECK,
                CheckType.MEMORY_CHECK,
                CheckType.NETWORK_CONNECTIVITY
            );
        }
        
        @Override
        public CompletableFuture<CheckResult> executeRepair(HostCheckContext context, CheckType repairType, Map<String, Object> repairParams) {
            return CompletableFuture.supplyAsync(() -> {
                log.info("执行主机修复: {} for host: {}", repairType, context.getHostname() != null ? context.getHostname() : context.getHostIp());
                
                // 这里实现具体的修复逻辑
                CheckResult result = CheckResult.builder()
                    .success(true)
                    .checkType(repairType)
                    .message("修复完成")
                    .checkTime(java.time.LocalDateTime.now())
                    .build();
                
                return result;
            });
        }
        
        @Override
        public boolean canRepair(HostCheckContext context, CheckType repairType) {
            OsType osType = context.getOsType();
            return getSupportedRepairTypes().contains(repairType) &&
                   (osType == null || getSupportedOperatingSystems().contains(osType));
        }
        
        @Override
        public String getRepairSuggestion(HostCheckContext context, CheckType repairType) {
            return switch (repairType) {
                case DISK_SPACE_CHECK -> "清理磁盘空间或扩容";
                case MEMORY_CHECK -> "释放内存或增加内存";
                case NETWORK_CONNECTIVITY -> "检查网络连接和防火墙设置";
                default -> "请联系管理员";
            };
        }
        
        @Override
        public String getPluginId() {
            return "host-repair";
        }
        
        @Override
        public String getVersion() {
            return "1.0.0";
        }
        
        @Override
        public void initialize() {
            log.info("主机修复插件扩展初始化: {}", getPluginId());
        }
        
        @Override
        public void cleanup() {
            log.info("主机修复插件扩展清理: {}", getPluginId());
        }
    }
}