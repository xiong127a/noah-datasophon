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

package com.datasophon.plugins.api.factory;

import com.datasophon.common.spring.SpringContextUtils;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;

import java.util.List;

/**
 * SSH连接服务工厂 - 通过 PF4J 扩展点机制提供 SshConnectionService 实例
 * 
 * 设计原则（符合 PF4J 插件化架构）：
 * 1. 通过 PF4J PluginManager 获取 SshConnectionService 扩展点实现
 * 2. 插件实现类加载器隔离，主应用不直接依赖插件实现类
 * 3. 支持插件的动态加载/卸载
 * 4. 提供单例缓存机制，提高性能
 * 
 * 与其他插件保持一致：
 * - HostValidator → HostValidationExtension (PF4J 扩展点)
 * - HostRepairer → HostRepairExtension (PF4J 扩展点)
 * - SystemInfoCollector → SystemInfoExtension (PF4J 扩展点)
 * - SshConnectionService → SshConnectionServiceExtension (PF4J 扩展点) ✅
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Slf4j
public class SshConnectionServiceFactory {
    
    private static volatile SshConnectionServiceFactory instance;
    private static final Object lock = new Object();
    
    // 服务实例缓存（避免重复查找）
    private volatile SshConnectionService cachedService;
    
    /**
     * 获取工厂单例
     */
    public static SshConnectionServiceFactory getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new SshConnectionServiceFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取默认的SSH连接服务实例
     * 通过 PF4J 扩展点机制获取（符合插件化设计）
     */
    public SshConnectionService getDefaultSshConnectionService() {
        // 使用缓存避免重复查找
        if (cachedService != null) {
            return cachedService;
        }
        
        synchronized (lock) {
            if (cachedService != null) {
                return cachedService;
            }
            
            try {
                // 从 Spring 容器获取 PluginManager
                var applicationContext = SpringContextUtils.getApplicationContext();
                if (applicationContext != null) {
                    var pluginManager = applicationContext.getBean(PluginManager.class);
                    // 通过 PF4J 扩展点机制获取所有 SshConnectionService 实现
                    List<SshConnectionService> extensions = pluginManager.getExtensions(SshConnectionService.class);
                    if (!extensions.isEmpty()) {
                        cachedService = extensions.getFirst(); // 获取第一个实现
                        log.info("【SSH服务工厂】通过PF4J扩展点成功获取SSH连接服务: {}",
                                cachedService.getClass().getName());
                        return cachedService;
                    } else {
                        log.warn("【SSH服务工厂】未找到任何SshConnectionService扩展点实现，请检查ssh-connector插件是否已加载");
                    }
                } else {
                    log.warn("【SSH服务工厂】无法获取Spring应用上下文");
                }
            } catch (Exception e) {
                log.error("【SSH服务工厂】通过PF4J获取SSH连接服务失败: {}", e.getMessage(), e);
            }
            
            return null;
        }
    }
    
    /**
     * 检查SSH连接服务是否可用
     * 
     * @return true如果可用，false如果不可用
     */
    public boolean isSshConnectionServiceAvailable() {
        var service = getDefaultSshConnectionService();
        return service != null;
    }
    
    /**
     * 清除缓存（用于插件重新加载）
     */
    public void clearCache() {
        synchronized (lock) {
            cachedService = null;
            log.info("【SSH服务工厂】清除SSH连接服务缓存");
        }
    }
    
    /**
     * 获取插件信息（用于日志和监控）
     */
    public String getPluginInfo() {
        var service = getDefaultSshConnectionService();
        if (service != null) {
            return String.format("SSH连接服务: %s (可用)", service.getClass().getSimpleName());
        } else {
            return "SSH连接服务: 未加载";
        }
    }
}
