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
    private final org.pf4j.PluginManager pf4jManager;
    
    @Getter
    private final Map<String, HostCheckerPlugin> activePlugins = new ConcurrentHashMap<>();
    
    @Getter
    private final Map<String, PluginStatus> pluginStatus = new ConcurrentHashMap<>();
    
    public PluginManager() {
        // 初始化PF4J插件管理器
        this.pf4jManager = new DefaultPluginManager();
    }
    
    @PostConstruct
    public void init() {
        log.info("初始化插件管理器...");
        
        try {
            // 加载插件
            loadAllPlugins();
            
            // 启动插件
            startAllPlugins();
            
            // 注册检查器插件
            registerHostCheckerPlugins();
            
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
            
            // 停止并卸载插件
            org.pf4j.PluginState stopState = pf4jManager.stopPlugin(pluginId);
            boolean stopped = stopState == org.pf4j.PluginState.STOPPED || stopState == org.pf4j.PluginState.DISABLED;
            
            boolean unloaded = false;
            if (stopped) {
                unloaded = pf4jManager.unloadPlugin(pluginId);
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
        return activePlugins.get(pluginId);
    }
    
    /**
     * 获取所有可用插件
     */
    public List<HostCheckerPlugin> getAllPlugins() {
        return new ArrayList<>(activePlugins.values());
    }
    
    /**
     * 获取插件按优先级排序
     */
    public List<HostCheckerPlugin> getPluginsSortedByPriority() {
        return activePlugins.values().stream()
                .sorted(Comparator.comparingInt(HostCheckerPlugin::getPriority))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据操作系统类型获取支持的插件
     */
    public List<HostCheckerPlugin> getPluginsForOs(String osType) {
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
}