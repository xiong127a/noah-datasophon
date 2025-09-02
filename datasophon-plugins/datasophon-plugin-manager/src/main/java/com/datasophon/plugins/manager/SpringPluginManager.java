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

import com.datasophon.plugins.api.HostValidationPlugin;
import com.datasophon.plugins.api.HostRepairPlugin;
import com.datasophon.plugins.api.PluginId;
import com.datasophon.plugins.api.SshConnectorPlugin;
import com.datasophon.plugins.api.SystemInfoCollectorPlugin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.*;
import org.pf4j.spring.SpringExtensionFactory;
// import org.pf4j.spring.ExtensionsInjector; // 暂时禁用
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring整合的插件管理器
 * 基于标准pf4j-spring实现，提供优雅的Spring集成和生命周期管理
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
public class SpringPluginManager extends DefaultPluginManager {

    @Getter
    @Autowired
    private ApplicationContext applicationContext;

    // ExtensionsInjector由Spring管理，不需要在这里声明

    // 插件实例存储（支持热插拔）
    private final Map<String, SshConnectorPlugin> sshConnectorPlugins = new ConcurrentHashMap<>();
    private final Map<String, HostValidationPlugin> hostValidationPlugins = new ConcurrentHashMap<>();
    private final Map<String, HostRepairPlugin> hostRepairPlugins = new ConcurrentHashMap<>();
    private final Map<String, SystemInfoCollectorPlugin> systemInfoCollectorPlugins = new ConcurrentHashMap<>();
    private final Map<String, PluginStatus> pluginStatus = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    /**
     * 无参构造函数
     */
    public SpringPluginManager() {
        super();
        log.info("SpringPluginManager 使用默认构造函数初始化");
    }

    /**
     * 带路径参数的构造函数
     */
    public SpringPluginManager(List<Path> pluginRoots) {
        super(pluginRoots);
        log.info("SpringPluginManager 使用指定路径初始化: {}", pluginRoots);
        // 构造完成后，等待Spring的@PostConstruct调用
        log.debug("SpringPluginManager构造函数完成，等待Spring初始化");
    }

    /**
     * 重写initialize方法，允许PF4J初始化核心组件但不自动加载插件
     * 这样pluginRepository等核心组件会被正确初始化
     */
    @Override
    protected void initialize() {
        // 调用父类的初始化逻辑来设置pluginRepository等核心组件
        super.initialize();
        log.debug("PF4J核心组件初始化完成，但插件加载将延迟到Spring @PostConstruct阶段执行");
    }

    /**
     * 重写开发模式检测，启用开发模式以支持未打包的插件
     */
    @Override
    public boolean isDevelopment() {
        return true; // 开发模式下支持加载classes目录
    }

    /**
     * 重写扩展工厂，使用标准的SpringExtensionFactory
     */
    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new SpringExtensionFactory(this);
    }

    @PostConstruct
    public void springInitialize() {
        log.info("SpringPluginManager开始Spring初始化...");
        // 延迟初始化，避免与构造函数冲突
        if (!initialized) {
            startPluginSystem();
        } else {
            log.info("SpringPluginManager已经初始化，跳过重复初始化");
        }
    }

    @PreDestroy 
    public void destroy() {
        log.info("正在关闭SpringPluginManager...");
        
        try {
            // 清理所有插件
            cleanupAllPlugins();
            
            // 停止插件系统
            stopPlugins();
            
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
            // 确保插件系统准备就绪
            log.debug("检查插件系统状态...");

            // 1. 启动PF4J插件系统（如果还没有启动的话）
            if (getPlugins().isEmpty()) {
                 log.info("开始加载插件...");
                 loadPlugins();
                 startPlugins();
             } else {
                 log.info("插件已在父类初始化时加载，跳过重复加载");
             }

            // 2. Spring集成将通过ExtensionsInjector Bean自动处理
            if (applicationContext != null) {
                log.info("ApplicationContext已注入，ExtensionsInjector将自动处理Spring集成");
            } else {
                log.warn("ApplicationContext未注入，将跳过Spring集成");
            }

            // 3. 注册所有插件到业务缓存
            registerAllPlugins();

            initialized = true;
            int totalPlugins = sshConnectorPlugins.size() + hostValidationPlugins.size() + 
                              hostRepairPlugins.size() + systemInfoCollectorPlugins.size();
            log.info("SpringPluginManager初始化完成，加载了 {} 个插件 (SSH连接器:{}, 主机验证:{}, 主机修复:{}, 信息收集:{})", 
                    totalPlugins, 
                    sshConnectorPlugins.size(),
                    hostValidationPlugins.size(), 
                    hostRepairPlugins.size(),
                    systemInfoCollectorPlugins.size());
            
        } catch (Exception e) {
            log.error("SpringPluginManager初始化失败", e);
            // 重置状态，允许重试
            initialized = false;
            throw new RuntimeException("SpringPluginManager初始化失败", e);
        }
    }



    /**
     * 注册所有插件到业务缓存
     */
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
                // 获取插件ID
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
            log.debug("插件 {} 初始化成功", plugin.getClass().getName());
        } catch (NoSuchMethodException e) {
            // 如果没有initialize方法，忽略
            log.debug("插件 {} 没有initialize方法", plugin.getClass().getName());
        } catch (Exception e) {
            log.warn("插件 {} 初始化失败", plugin.getClass().getName(), e);
        }
    }

    /**
     * 清理所有插件
     */
    private void cleanupAllPlugins() {
        cleanupPlugins(sshConnectorPlugins.values(), "SSH连接器");
        cleanupPlugins(hostValidationPlugins.values(), "主机验证");
        cleanupPlugins(hostRepairPlugins.values(), "主机修复");
        cleanupPlugins(systemInfoCollectorPlugins.values(), "信息收集");

        sshConnectorPlugins.clear();
        hostValidationPlugins.clear();
        hostRepairPlugins.clear();
        systemInfoCollectorPlugins.clear();
        pluginStatus.clear();
    }

    /**
     * 清理插件集合
     */
    private <T> void cleanupPlugins(Collection<T> plugins, String pluginType) {
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
        } catch (NoSuchMethodException e) {
            // 如果没有cleanup方法，忽略
            log.debug("插件 {} 没有cleanup方法", plugin.getClass().getName());
        } catch (Exception e) {
            String pluginId = getPluginId(plugin);
            log.warn("{}插件清理失败: {}", pluginType, pluginId, e);
        }
    }

    // ================== 业务代码专用API ==================
    
    /**
     * 根据插件类型枚举获取插件列表（用于业务代码）
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getPluginsByType(PluginId pluginType) {
        return switch (pluginType) {
            case HOST_VALIDATION -> (List<T>) new ArrayList<>(hostValidationPlugins.values());
            case HOST_REPAIR -> (List<T>) new ArrayList<>(hostRepairPlugins.values());
            case SSH_CONNECTOR -> (List<T>) new ArrayList<>(sshConnectorPlugins.values());
            case SYSTEM_INFO_COLLECTOR -> (List<T>) new ArrayList<>(systemInfoCollectorPlugins.values());
        };
    }
    
    /**
     * 根据插件ID获取特定插件（用于业务代码）
     */
    @SuppressWarnings("unchecked")
    public <T> T getPlugin(PluginId pluginType, String pluginId) {
        return switch (pluginType) {
            case HOST_VALIDATION -> (T) hostValidationPlugins.get(pluginId);
            case HOST_REPAIR -> (T) hostRepairPlugins.get(pluginId);
            case SSH_CONNECTOR -> (T) sshConnectorPlugins.get(pluginId);
            case SYSTEM_INFO_COLLECTOR -> (T) systemInfoCollectorPlugins.get(pluginId);
        };
    }
    
    /**
     * 检查是否有可用的插件（用于业务代码）
     */
    public boolean hasPlugins(PluginId pluginType) {
        return !getPluginsByType(pluginType).isEmpty();
    }
    
    /**
     * 兼容现有代码的API（用于业务代码）
     */
    public <T> List<T> getPlugins(Class<T> pluginClass) {
        if (pluginClass == HostValidationPlugin.class) {
            return getPluginsByType(PluginId.HOST_VALIDATION);
        } else if (pluginClass == HostRepairPlugin.class) {
            return getPluginsByType(PluginId.HOST_REPAIR);
        } else if (pluginClass == SshConnectorPlugin.class) {
            return getPluginsByType(PluginId.SSH_CONNECTOR);
        } else if (pluginClass == SystemInfoCollectorPlugin.class) {
            return getPluginsByType(PluginId.SYSTEM_INFO_COLLECTOR);
        } else {
            log.warn("不支持的插件类型: {}", pluginClass.getName());
            return new ArrayList<>();
        }
    }

    /**
     * 获取插件状态
     */
    public PluginStatus getPluginStatus(String pluginId) {
        return pluginStatus.get(pluginId);
    }

    /**
     * 获取所有插件状态
     */
    public Map<String, PluginStatus> getAllPluginStatus() {
        return new HashMap<>(pluginStatus);
    }



    /**
     * 手动刷新插件（用于热重载）
     */
    public synchronized void refreshPlugins() {
        log.info("开始刷新插件...");
        try {
            // 清理现有插件
            cleanupAllPlugins();
            
            // 重新加载插件
            unloadPlugins();
            loadPlugins();
            startPlugins();
            
            // 重新注册插件
            registerAllPlugins();
            
            log.info("插件刷新完成");
        } catch (Exception e) {
            log.error("插件刷新失败", e);
            throw new RuntimeException("插件刷新失败", e);
        }
    }
}