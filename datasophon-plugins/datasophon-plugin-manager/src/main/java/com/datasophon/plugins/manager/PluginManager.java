package com.datasophon.plugins.manager;

import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.PluginMetadata;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginWrapper;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 插件管理器
 * 负责插件的加载、卸载、更新和生命周期管理
 * 
 * @author DataSophon Team
 */
@Component
@Slf4j
public class PluginManager {
    
    @Getter
    private org.pf4j.PluginManager pf4jManager;
    
    @Getter
    private final Map<String, HostCheckerPlugin> activePlugins = new ConcurrentHashMap<>();
    
    @Getter
    private final Map<String, PluginStatus> pluginStatus = new ConcurrentHashMap<>();
    
    /**
     * 插件管理器是否已初始化
     */
    @Getter
    private volatile boolean initialized = false;
    
    /**
     * 检查插件管理器是否已初始化
     */

    
    /**
     * 延迟加载配置，通过构造函数或setter注入
     */
    private boolean lazyLoading = false;
    
    /**
     * 插件路径列表（通过配置注入）
     */
    private List<String> pluginScanPaths;
    
    public PluginManager() {
        // 延迟初始化PF4J管理器，等收集完所有路径后再创建
        this.pf4jManager = null;
        this.pluginScanPaths = new ArrayList<>();
    }
    

    
    /**
     * 配置PF4J插件管理器路径（由配置类调用）
     */
    public void configurePluginPaths() {
        configurePF4JManager();
        
        // 如果是立即模式且尚未初始化，现在进行初始化
        if (!lazyLoading && !initialized) {
            log.info("插件路径配置完成，开始初始化插件管理器...");
            initializePlugins();
        }
    }
    
    private void configurePF4JManager() {
        // 收集所有有效的插件路径
        List<Path> allPluginPaths = new ArrayList<>();
        
        for (String pluginPath : pluginScanPaths) {
            Path path = Paths.get(pluginPath);
            if (path.toFile().exists()) {
                log.info("添加插件扫描路径: {}", pluginPath);
                // 对于目录，添加到插件路径
                if (path.toFile().isDirectory()) {
                    allPluginPaths.add(path);
                }
            } else {
                log.debug("插件路径不存在，跳过: {}", pluginPath);
            }
        }
        
        // 使用收集到的路径创建PF4J管理器
        if (this.pf4jManager == null) {
            log.info("创建ConfigurablePluginManager，插件路径: {}", allPluginPaths);
            this.pf4jManager = new ConfigurablePluginManager(allPluginPaths);
        } else {
            log.warn("PF4J管理器已存在，跳过重新创建");
        }
        
        log.info("PF4J插件管理器配置完成，扫描路径数量: {}", pluginScanPaths.size());
    }
    

    
    /**
     * 设置插件扫描路径（用于配置注入）
     */
    public void setPluginScanPaths(List<String> pluginScanPaths) {
        this.pluginScanPaths = pluginScanPaths;
    }
    
    /**
     * 设置延迟加载配置
     */
    public void setLazyLoading(boolean lazyLoading) {
        this.lazyLoading = lazyLoading;
    }
    
    @PostConstruct
    public void init() {
        if (!lazyLoading) {
            log.info("立即模式：初始化插件管理器...");
            // 检查是否已配置插件路径，如果没有则推迟到配置完成后
            if (pluginScanPaths.isEmpty()) {
                log.info("插件路径尚未配置，将在配置完成后初始化");
            } else {
                initializePlugins();
            }
        } else {
            log.info("延迟模式：插件管理器已就绪，等待手动初始化");
        }
    }
    
    /**
     * 手动初始化插件管理器
     * 支持延迟加载模式下的按需初始化
     */
    public synchronized void initializePlugins() {
        if (initialized) {
            log.warn("插件管理器已经初始化，跳过重复初始化");
            return;
        }
        
        log.info("开始初始化插件管理器...");
        
        // 确保PF4J管理器已创建
        if (pf4jManager == null) {
            log.info("PF4J管理器未初始化，先进行配置...");
            configurePF4JManager();
        }
        
        if (pf4jManager == null) {
            log.error("无法创建PF4J管理器，初始化失败");
            return;
        }
        
        try {
            // 加载插件
            loadAllPlugins();
            
            // 启动插件
            startAllPlugins();
            
            // 注册检查器插件
            registerHostCheckerPlugins();
            
            initialized = true;
            log.info("插件管理器初始化完成，加载了 {} 个插件", activePlugins.size());
            
        } catch (Exception e) {
            log.error("插件管理器初始化失败", e);
            throw new RuntimeException("插件管理器初始化失败", e);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭插件管理器...");
        
        try {
            // 清理所有插件
            for (HostCheckerPlugin plugin : activePlugins.values()) {
                try {
                    plugin.cleanup();
                } catch (Exception e) {
                    log.warn("插件清理失败: {}", plugin.getPluginId(), e);
                }
            }
            
            // 停止PF4J管理器
            pf4jManager.stopPlugins();
            pf4jManager.unloadPlugins();
            
            activePlugins.clear();
            pluginStatus.clear();
            
            log.info("插件管理器已关闭");
            
        } catch (Exception e) {
            log.error("插件管理器关闭时发生异常", e);
        }
    }
    
    /**
     * 加载所有插件
     */
    private void loadAllPlugins() {
        log.info("开始加载插件...");
        pf4jManager.loadPlugins();
        
        List<PluginWrapper> loadedPlugins = pf4jManager.getPlugins();
        log.info("成功加载 {} 个插件", loadedPlugins.size());
        
        for (PluginWrapper plugin : loadedPlugins) {
            log.info("已加载插件: {} v{}", plugin.getPluginId(), plugin.getDescriptor().getVersion());
            pluginStatus.put(plugin.getPluginId(), PluginStatus.LOADED);
        }
    }
    
    /**
     * 启动所有插件
     */
    private void startAllPlugins() {
        log.info("开始启动插件...");
        pf4jManager.startPlugins();
        
        List<PluginWrapper> startedPlugins = pf4jManager.getStartedPlugins();
        log.info("成功启动 {} 个插件", startedPlugins.size());
        
        for (PluginWrapper plugin : startedPlugins) {
            pluginStatus.put(plugin.getPluginId(), PluginStatus.STARTED);
        }
    }
    
    /**
     * 注册主机检查器插件
     */
    private void registerHostCheckerPlugins() {
        List<HostCheckerPlugin> plugins = pf4jManager.getExtensions(HostCheckerPlugin.class);
        
        for (HostCheckerPlugin plugin : plugins) {
            try {
                String pluginId = plugin.getPluginId();
                
                // 初始化插件
                plugin.initialize();
                
                // 注册插件
                activePlugins.put(pluginId, plugin);
                pluginStatus.put(pluginId, PluginStatus.ACTIVE);
                
                log.info("注册主机检查器插件: {} -> {}", pluginId, plugin.getClass().getSimpleName());
                
            } catch (Exception e) {
                log.error("注册插件失败: {}", plugin.getClass().getName(), e);
                pluginStatus.put(plugin.getPluginId(), PluginStatus.ERROR);
            }
        }
    }
    
    /**
     * 动态加载插件
     */
    public boolean loadPlugin(String pluginPath) {
        try {
            log.info("正在加载插件: {}", pluginPath);
            
            Path path = Paths.get(pluginPath);
            String pluginId = pf4jManager.loadPlugin(path);
            
            if (pluginId != null) {
                org.pf4j.PluginState startState = pf4jManager.startPlugin(pluginId);
                boolean started = startState == org.pf4j.PluginState.STARTED;
                
                if (started) {
                    // 重新注册插件
                    registerHostCheckerPlugins();
                    
                    log.info("成功加载插件: {}", pluginId);
                    return true;
                } else {
                    log.error("启动插件失败: {}, state: {}", pluginId, startState);
                    pf4jManager.unloadPlugin(pluginId);
                    return false;
                }
            } else {
                log.error("插件加载失败，返回的pluginId为null: {}", pluginPath);
                return false;
            }
            
        } catch (Exception e) {
            log.error("加载插件失败: {}", pluginPath, e);
            return false;
        }
    }
    
    /**
     * 卸载插件
     */
    public boolean unloadPlugin(String pluginId) {
        try {
            log.info("正在卸载插件: {}", pluginId);
            
            // 清理插件
            HostCheckerPlugin plugin = activePlugins.get(pluginId);
            if (plugin != null) {
                plugin.cleanup();
                activePlugins.remove(pluginId);
            }
            
            // 获取当前插件状态
            org.pf4j.PluginWrapper pluginWrapper = pf4jManager.getPlugin(pluginId);
            if (pluginWrapper == null) {
                log.warn("插件不存在或已被卸载: {}", pluginId);
                pluginStatus.remove(pluginId);
                return true; // 插件已不存在，视为卸载成功
            }
            
            org.pf4j.PluginState currentState = pluginWrapper.getPluginState();
            log.info("开始卸载插件: {}, 当前状态: {}", pluginId, currentState);
            
            // 先停止插件（如果还在运行）
            boolean stopped = true;
            if (currentState == org.pf4j.PluginState.STARTED) {
                org.pf4j.PluginState stopState = pf4jManager.stopPlugin(pluginId);
                stopped = stopState == org.pf4j.PluginState.STOPPED;
                log.info("插件停止操作结果: {}, stopped={}, newState={}", pluginId, stopped, stopState);
            } else if (currentState == org.pf4j.PluginState.STOPPED || currentState == org.pf4j.PluginState.DISABLED) {
                log.info("插件已处于停止状态: {}, state={}", pluginId, currentState);
            }
            
            // 然后卸载插件
            boolean unloaded = false;
            if (stopped) {
                try {
                    unloaded = pf4jManager.unloadPlugin(pluginId);
                    log.info("插件卸载操作结果: {}, unloaded={}", pluginId, unloaded);
                    
                    // 验证插件是否真的被卸载了
                    if (unloaded) {
                        org.pf4j.PluginWrapper afterUnload = pf4jManager.getPlugin(pluginId);
                        if (afterUnload == null) {
                            log.info("插件确认已完全卸载: {}", pluginId);
                        } else {
                            log.warn("插件卸载后仍存在: {}, state={}", pluginId, afterUnload.getPluginState());
                            unloaded = false;
                        }
                    }
                } catch (Exception e) {
                    log.error("执行插件卸载操作失败: {}", pluginId, e);
                    unloaded = false;
                }
            } else {
                log.error("插件停止失败，无法继续卸载: {}", pluginId);
            }
            
            if (stopped && unloaded) {
                pluginStatus.remove(pluginId);
                log.info("成功卸载插件: {}", pluginId);
                return true;
            } else {
                log.error("卸载插件失败: {}, stopped={}, unloaded={}", pluginId, stopped, unloaded);
                return false;
            }
            
        } catch (Exception e) {
            log.error("卸载插件失败: {}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 获取插件
     */
    public HostCheckerPlugin getPlugin(String pluginId) {
        ensureInitialized();
        return activePlugins.get(pluginId);
    }
    
    /**
     * 获取所有可用插件
     */
    public List<HostCheckerPlugin> getAllPlugins() {
        ensureInitialized();
        return new ArrayList<>(activePlugins.values());
    }
    
    /**
     * 获取插件按优先级排序
     */
    public List<HostCheckerPlugin> getPluginsSortedByPriority() {
        ensureInitialized();
        return activePlugins.values().stream()
                .sorted(Comparator.comparingInt(HostCheckerPlugin::getPriority))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据操作系统类型获取支持的插件
     */
    public List<HostCheckerPlugin> getPluginsForOs(String osType) {
        ensureInitialized();
        return activePlugins.values().stream()
                .filter(plugin -> plugin.getSupportedOperatingSystems().stream()
                        .anyMatch(os -> os.name().equalsIgnoreCase(osType)))
                .sorted(Comparator.comparingInt(HostCheckerPlugin::getPriority))
                .collect(Collectors.toList());
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
     * 检查插件是否健康
     */
    public boolean isPluginHealthy(String pluginId) {
        HostCheckerPlugin plugin = activePlugins.get(pluginId);
        return plugin != null && plugin.isHealthy();
    }
    
    /**
     * 获取插件元数据
     */
    public PluginMetadata getPluginMetadata(String pluginId) {
        HostCheckerPlugin plugin = activePlugins.get(pluginId);
        return plugin != null ? plugin.getMetadata() : null;
    }
    
    /**
     * 获取插件数量
     */
    public int getPluginCount() {
        return activePlugins.size();
    }
    
    /**
     * 检查插件管理器是否已初始化
     */

    
    /**
     * 检查是否启用延迟加载
     */
    public boolean isLazyLoading() {
        return lazyLoading;
    }
    
    /**
     * 确保插件管理器已初始化
     * 在延迟加载模式下自动初始化
     */
    private void ensureInitialized() {
        if (!initialized && lazyLoading) {
            log.info("延迟加载模式：首次使用时自动初始化插件管理器");
            initializePlugins();
        }
    }
}