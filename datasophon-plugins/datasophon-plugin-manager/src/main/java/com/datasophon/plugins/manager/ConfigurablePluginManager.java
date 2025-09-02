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
import org.pf4j.*;

import java.nio.file.Path;
import java.util.ArrayList;
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
     * 无参构造函数
     */
    public ConfigurablePluginManager() {
        super();
        log.debug("ConfigurablePluginManager 使用默认构造函数初始化");
    }
    
    /**
     * 带路径参数的构造函数
     * 使用此构造函数可以避免PF4J内部路径缓存问题
     */
    public ConfigurablePluginManager(List<Path> pluginRoots) {
        super(pluginRoots);
        log.info("ConfigurablePluginManager 使用指定路径初始化: {}", pluginRoots);
        // 复制一份路径作为自定义根目录
        this.customPluginRoots = new ArrayList<>(pluginRoots);
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
     * 修复路径重复问题：如果构造函数传入了路径，则不再添加自定义路径，避免重复
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
        
        // 只有在父类没有路径时才添加自定义根目录，避免重复
        if (superRoots == null || superRoots.isEmpty()) {
            allRoots.addAll(customPluginRoots);
            log.debug("添加自定义插件根目录: {}", customPluginRoots.size());
        } else {
            log.debug("父类已有插件根目录，跳过自定义根目录添加以避免重复");
        }
        
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
     * 创建开发模式专用的插件仓库
     * 该仓库不会扫描子目录，直接将指定路径作为插件目录
     */
    @Override
    protected PluginRepository createPluginRepository() {
        if (isDevelopment()) {
            log.debug("开发模式：使用简化的插件仓库，避免子目录扫描");
            
            return new PluginRepository() {
                @Override
                public List<Path> getPluginPaths() {
                    List<Path> pluginPaths = new ArrayList<>();
                    
                    // 直接返回配置的插件根目录，不进行子目录扫描
                    for (Path pluginRoot : getPluginsRoots()) {
                        // 检查是否为有效的插件目录（包含plugin.properties）
                        Path metaInfProperties = pluginRoot.resolve("META-INF").resolve("plugin.properties");
                        Path rootProperties = pluginRoot.resolve("plugin.properties");
                        
                        if (metaInfProperties.toFile().exists() || rootProperties.toFile().exists()) {
                            log.debug("发现开发模式插件: {}", pluginRoot);
                            pluginPaths.add(pluginRoot);
                        } else {
                            log.debug("跳过非插件目录: {}", pluginRoot);
                        }
                    }
                    
                    log.debug("开发模式插件发现完成，找到 {} 个插件", pluginPaths.size());
                    return pluginPaths;
                }
                
                @Override
                public boolean deletePluginPath(Path pluginPath) {
                    log.debug("开发模式不支持删除插件路径: {}", pluginPath);
                    return false;
                }
            };
        } else {
            log.debug("生产模式：使用默认插件仓库");
            return super.createPluginRepository();
        }
    }

    /**
     * 重写插件加载器创建方法
     * 使用Parent First策略解决类加载器约束违规问题
     * <p>
     * 参考PF4J官方文档: <a href="https://pf4j.org/doc/class-loading.html">官方文档</a>
     * Parent First策略确保插件优先使用父类加载器中的类，
     * 避免共享类（如CheckType枚举）在不同类加载器中产生冲突
     */
    @Override
    protected PluginLoader createPluginLoader() {
        return new DefaultPluginLoader(this) {
            @Override
            protected PluginClassLoader createPluginClassLoader(Path pluginPath, PluginDescriptor pluginDescriptor) {
                log.info("为插件 {} 创建Parent First类加载器", pluginDescriptor.getPluginId());
                
                // 按照PF4J源码，使用Parent First策略 (ClassLoadingStrategy.APD)
                // APD = Application, Plugin, Dependencies (应用优先，即Parent First)
                return new PluginClassLoader(pluginManager, pluginDescriptor, getClass().getClassLoader(), ClassLoadingStrategy.APD);
            }
        };
    }
    

}
