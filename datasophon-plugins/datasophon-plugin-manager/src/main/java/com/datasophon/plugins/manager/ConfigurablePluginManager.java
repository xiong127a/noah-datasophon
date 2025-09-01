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

import lombok.extern.slf4j.Slf4j;
import org.pf4j.DefaultPluginManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 可配置的插件管理器
 * 继承PF4J的DefaultPluginManager，支持动态添加插件根目录
 * 
 * @author 任相鹏
 * @email 635887935@qq.com  
 * @date 2025-08-28
 */
@Slf4j
public class ConfigurablePluginManager extends DefaultPluginManager {
    
    private List<Path> customPluginRoots;
    
    /**
     * 需要从父类加载器加载的包列表（解决类加载器约束冲突）
     */
    private final List<String> parentFirstPackages;
    
    /**
     * 无参构造函数
     */
    public ConfigurablePluginManager() {
        super();
        this.parentFirstPackages = initializeDefaultParentFirstPackages();
        log.debug("ConfigurablePluginManager 使用默认构造函数初始化");
    }
    
    /**
     * 带路径参数的构造函数
     * 使用此构造函数可以避免PF4J内部路径缓存问题
     */
    public ConfigurablePluginManager(List<Path> pluginRoots) {
        super(pluginRoots);
        this.parentFirstPackages = initializeDefaultParentFirstPackages();
        log.info("ConfigurablePluginManager 使用指定路径初始化: {}", pluginRoots);
        // 复制一份路径作为自定义根目录
        this.customPluginRoots = new ArrayList<>(pluginRoots);
    }
    
    /**
     * 初始化默认的父优先包列表
     */
    private List<String> initializeDefaultParentFirstPackages() {
        return new ArrayList<>(Arrays.asList(
            "com.datasophon.common.enums",
            "com.datasophon.plugins.api.model", 
            "com.datasophon.plugins.api"
        ));
    }
    
    /**
     * 确保customPluginRoots已初始化
     */
    private void ensureCustomPluginRootsInitialized() {
        if (customPluginRoots == null) {
            customPluginRoots = new ArrayList<>();
            log.debug("延迟初始化customPluginRoots");
        }
    }
    
    /**
     * 添加插件根目录
     */
    public void addPluginRoot(Path pluginRoot) {
        ensureCustomPluginRootsInitialized();
        if (pluginRoot != null && !customPluginRoots.contains(pluginRoot)) {
            customPluginRoots.add(pluginRoot);
            log.debug("添加插件根目录: {}", pluginRoot);
        }
    }
    
    /**
     * 批量添加插件根目录
     */
    public void addPluginRoots(List<Path> pluginRoots) {
        if (pluginRoots != null) {
            for (Path root : pluginRoots) {
                addPluginRoot(root);
            }
        }
    }
    
    /**
     * 重写getPluginsRoots方法，返回包含自定义路径的列表
     */
    @Override
    public List<Path> getPluginsRoots() {
        ensureCustomPluginRootsInitialized();
        
        List<Path> allRoots = new ArrayList<>();
        
        // 防止父类返回null的情况
        List<Path> superRoots = super.getPluginsRoots();
        if (superRoots != null) {
            allRoots.addAll(superRoots);
        }
        
        // 添加自定义根目录（现在保证不为null）
        allRoots.addAll(customPluginRoots);
        
        log.debug("插件根目录总数: {}, 默认: {}, 自定义: {}", 
                allRoots.size(), 
                (superRoots != null ? superRoots.size() : 0), 
                customPluginRoots.size());
        
        return allRoots;
    }
    
    /**
     * 获取自定义插件根目录数量
     */
    public int getCustomPluginRootsCount() {
        ensureCustomPluginRootsInitialized();
        return customPluginRoots.size();
    }
    
    /**
     * 清理自定义插件根目录
     */
    public void clearCustomPluginRoots() {
        ensureCustomPluginRootsInitialized();
        customPluginRoots.clear();
        log.debug("已清理所有自定义插件根目录");
    }
    
    /**
     * 强制刷新插件根目录缓存
     * 在动态添加插件根目录后调用此方法，确保PF4J使用最新的根目录列表
     */
    public void refreshPluginRoots() {
        ensureCustomPluginRootsInitialized();
        log.info("刷新插件根目录缓存，当前自定义根目录数量: {}", customPluginRoots.size());
        
        // 强制重新获取插件根目录
        List<Path> allRoots = getPluginsRoots();
        log.info("刷新后的插件根目录: {}", allRoots);
        
        // 清空已加载的插件，强制重新扫描
        // 注意：这会停止并卸载所有已加载的插件
        stopPlugins();
        unloadPlugins();
        
        log.info("插件根目录缓存已刷新，准备重新加载插件");
    }
    
    /**
     * 添加父优先包
     */
    public void addParentFirstPackage(String packageName) {
        if (packageName != null && !parentFirstPackages.contains(packageName)) {
            parentFirstPackages.add(packageName);
            log.debug("添加父优先包: {}", packageName);
        }
    }
    
    /**
     * 批量添加父优先包
     */
    public void addParentFirstPackages(List<String> packages) {
        if (packages != null) {
            for (String pkg : packages) {
                addParentFirstPackage(pkg);
            }
        }
    }
    
    /**
     * 获取父优先包列表
     */
    public List<String> getParentFirstPackages() {
        return new ArrayList<>(parentFirstPackages);
    }
}
