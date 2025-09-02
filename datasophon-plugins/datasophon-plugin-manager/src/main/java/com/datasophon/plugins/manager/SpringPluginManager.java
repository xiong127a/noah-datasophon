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

package com.datasophon.plugins.manager;

import com.datasophon.plugins.api.SshConnectorPlugin;
import com.datasophon.plugins.api.HostValidationPlugin;
import com.datasophon.plugins.api.HostRepairPlugin;
import com.datasophon.plugins.api.SystemInfoCollectorPlugin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.ExtensionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring整合的插件管理器
 * 完美结合Spring Boot和PF4J，提供优雅的自动装配和生命周期管理
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
public class SpringPluginManager extends ConfigurablePluginManager {

    @Getter
    @Autowired
    private ApplicationContext applicationContext;

    // 插件实例存储（支持热插拔）
    private final Map<String, SshConnectorPlugin> sshConnectorPlugins = new ConcurrentHashMap<>();
    private final Map<String, HostValidationPlugin> hostValidationPlugins = new ConcurrentHashMap<>();
    private final Map<String, HostRepairPlugin> hostRepairPlugins = new ConcurrentHashMap<>();
    private final Map<String, SystemInfoCollectorPlugin> systemInfoCollectorPlugins = new ConcurrentHashMap<>();
    private final Map<String, PluginStatus> pluginStatus = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;



    public SpringPluginManager(List<Path> pluginRoots) {
        super(pluginRoots);
        log.info("SpringPluginManager 使用指定路径初始化: {}", pluginRoots);
    }



    /**
     * 重写扩展工厂，支持Spring的依赖注入
     */
    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new SpringExtensionFactory(this);
    }

    /**
     * Spring扩展工厂
     * 支持插件中的扩展点使用Spring的依赖注入
     */
    public static class SpringExtensionFactory implements ExtensionFactory {

        private final SpringPluginManager pluginManager;

        public SpringExtensionFactory(SpringPluginManager pluginManager) {
            this.pluginManager = pluginManager;
        }

        @Override
        public <T> T create(Class<T> extensionClass) {
            try {
                log.debug("创建扩展实例: {}", extensionClass.getName());

                // 1. 首先尝试使用默认构造函数创建实例
                T extension = extensionClass.getDeclaredConstructor().newInstance();

                // 2. 然后尝试注入Spring依赖（如果Spring上下文可用）
                if (pluginManager.getApplicationContext() != null) {
                    try {
                        // 使用Spring的自动装配功能
                        pluginManager.getApplicationContext()
                                .getAutowireCapableBeanFactory()
                                .autowireBean(extension);
                        log.debug("成功为扩展 {} 注入Spring依赖", extensionClass.getName());
                    } catch (Exception e) {
                        log.debug("无法为扩展 {} 注入Spring依赖: {}", extensionClass.getName(), e.getMessage());
                        // 继续使用没有依赖注入的实例
                    }
                }

                return extension;

            } catch (Exception e) {
                log.error("创建扩展实例失败: {}", extensionClass.getName(), e);
                throw new RuntimeException("无法创建扩展实例: " + extensionClass.getName(), e);
            }
        }
    }

    @PostConstruct
    public void initialize() {
        log.info("SpringPluginManager开始初始化...");
        startPluginSystem();
    }

    @PreDestroy
    public void destroy() {
        log.info("正在关闭SpringPluginManager...");
        
        try {
            // 清理所有插件
            cleanupPlugins(sshConnectorPlugins.values(), "SSH连接器");
            cleanupPlugins(hostValidationPlugins.values(), "主机验证");
            cleanupPlugins(hostRepairPlugins.values(), "主机修复");
            cleanupPlugins(systemInfoCollectorPlugins.values(), "信息收集");

            stop();

            sshConnectorPlugins.clear();
            hostValidationPlugins.clear();
            hostRepairPlugins.clear();
            systemInfoCollectorPlugins.clear();
            pluginStatus.clear();

            log.info("SpringPluginManager已关闭");

        } catch (Exception e) {
            log.error("SpringPluginManager关闭时发生异常", e);
        }
    }

    /**
     * 启动插件系统
     */
    public synchronized void startPluginSystem() {
        if (initialized) {
            log.warn("SpringPluginManager已经初始化，跳过重复初始化");
            return;
        }
        
        log.info("开始启动SpringPluginManager...");
        
        try {
            // 启动PF4J
                    start();

        registerAllPlugins();

        initialized = true;
        int totalPlugins = sshConnectorPlugins.size() + hostValidationPlugins.size() + hostRepairPlugins.size() + systemInfoCollectorPlugins.size();
        log.info("SpringPluginManager初始化完成，加载了 {} 个插件 (SSH连接器:{}, 主机验证:{}, 主机修复:{}, 信息收集:{})", 
                totalPlugins, 
                sshConnectorPlugins.size(),
                hostValidationPlugins.size(), 
                hostRepairPlugins.size(),
                systemInfoCollectorPlugins.size());
            
        } catch (Exception e) {
            log.error("SpringPluginManager初始化失败", e);
            throw new RuntimeException("SpringPluginManager初始化失败", e);
        }
    }

    public void start() {
        log.info("SpringPluginManager PF4J 启动开始...");
        loadPlugins();
        startPlugins();
        log.info("SpringPluginManager PF4J 启动完成");
    }

    public void stop() {
        log.info("SpringPluginManager PF4J 停止开始...");
        stopPlugins();
        log.info("SpringPluginManager PF4J 停止完成");
    }

    private void registerAllPlugins() {
        // 注册SSH连接器插件
        registerPlugins(SshConnectorPlugin.class, sshConnectorPlugins, "SSH连接器");

        // 注册验证插件
        registerPlugins(HostValidationPlugin.class, hostValidationPlugins, "主机验证");
        
        // 注册修复插件  
        registerPlugins(HostRepairPlugin.class, hostRepairPlugins, "主机修复");
        
        // 注册信息收集插件
        registerPlugins(SystemInfoCollectorPlugin.class, systemInfoCollectorPlugins, "信息收集");
    }

    /**
     * 通用插件注册方法（支持热插拔）
     */
    private <T> void registerPlugins(Class<T> pluginClass, Map<String, T> pluginMap, String pluginType) {
        List<T> pluginList = getExtensions(pluginClass);
        for (T plugin : pluginList) {
            try {
                // 获取插件ID的通用方法
                String pluginId = getPluginId(plugin);
                
                // 检查是否已存在同ID插件（热替换场景）
                T existingPlugin = pluginMap.get(pluginId);
                if (existingPlugin != null) {
                    log.info("检测到{}插件热替换: {} -> {}", pluginType, pluginId, plugin.getClass().getSimpleName());
                    cleanupSinglePlugin(existingPlugin, pluginType);
                }
                
                // 初始化新插件
                initializePlugin(plugin);
                
                // 注册插件
                pluginMap.put(pluginId, plugin);
                pluginStatus.put(pluginId, PluginStatus.ACTIVE);
                
                log.info("注册{}插件: {} -> {}", pluginType, pluginId, plugin.getClass().getSimpleName());
                
            } catch (Exception e) {
                String pluginId = getPluginId(plugin);
                log.error("注册{}插件失败: {}", pluginType, plugin.getClass().getName(), e);
                if (pluginId != null) {
                    pluginStatus.put(pluginId, PluginStatus.ERROR);
                }
            }
        }
    }

    /**
     * 获取插件ID的通用方法
     */
    private <T> String getPluginId(T plugin) {
        try {
            return plugin.getClass().getMethod("getPluginId").invoke(plugin).toString();
        } catch (Exception e) {
            return plugin.getClass().getSimpleName();
        }
    }

    /**
     * 初始化插件的通用方法
     */
    private <T> void initializePlugin(T plugin) {
        try {
            plugin.getClass().getMethod("initialize").invoke(plugin);
        } catch (Exception e) {
            // 如果没有initialize方法，忽略
            log.debug("插件 {} 没有initialize方法或初始化失败", plugin.getClass().getName());
        }
    }

    /**
     * 清理插件集合
     */
    private <T> void cleanupPlugins(java.util.Collection<T> plugins, String pluginType) {
        for (T plugin : plugins) {
            cleanupSinglePlugin(plugin, pluginType);
        }
    }

    /**
     * 清理单个插件
     */
    private <T> void cleanupSinglePlugin(T plugin, String pluginType) {
        try {
            String pluginId = getPluginId(plugin);
            plugin.getClass().getMethod("cleanup").invoke(plugin);
            log.debug("{}插件清理成功: {}", pluginType, pluginId);
        } catch (Exception e) {
            String pluginId = getPluginId(plugin);
            log.warn("{}插件清理失败: {}", pluginType, pluginId, e);
        }
    }


}
