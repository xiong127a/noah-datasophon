package com.datasophon.plugins.manager;

import com.datasophon.plugins.api.HostCheckerPlugin;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginDependency;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 插件管理器扩展功能
 * 提供动态加载、卸载、重载等高级功能
 * 
 * @author DataSophon Team
 */
@Component
@Slf4j
public class PluginManagerExtensions {
    
    private final PluginManager pluginManager;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public PluginManagerExtensions(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }
    
    /**
     * 重载插件
     */
    public boolean reloadPlugin(String pluginId) {
        lock.writeLock().lock();
        try {
            log.info("开始重载插件: {}", pluginId);
            
            // 先卸载
            if (!pluginManager.unloadPlugin(pluginId)) {
                log.error("卸载插件失败，无法重载: {}", pluginId);
                return false;
            }
            
            // 获取原插件路径（这里需要保存路径信息）
            String pluginPath = getPluginPath(pluginId);
            if (pluginPath == null) {
                log.error("无法获取插件路径: {}", pluginId);
                return false;
            }
            
            // 重新加载
            boolean success = pluginManager.loadPlugin(pluginPath);
            
            if (success) {
                log.info("插件重载成功: {}", pluginId);
            } else {
                log.error("插件重载失败: {}", pluginId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("重载插件异常: {}", pluginId, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 启用插件
     */
    public boolean enablePlugin(String pluginId) {
        lock.writeLock().lock();
        try {
            log.info("启用插件: {}", pluginId);
            
            PluginStatus currentStatus = pluginManager.getPluginStatus(pluginId);
            if (currentStatus == PluginStatus.ACTIVE) {
                log.info("插件已经是启用状态: {}", pluginId);
                return true;
            }
            
            // 启用插件
            boolean success = pluginManager.getPf4jManager().enablePlugin(pluginId);
            
            if (success) {
                // 启动插件
                org.pf4j.PluginState startState = pluginManager.getPf4jManager().startPlugin(pluginId);
                success = startState == org.pf4j.PluginState.STARTED;
                
                if (success) {
                    // 重新注册插件
                    registerPlugin(pluginId);
                    log.info("插件启用成功: {}", pluginId);
                } else {
                    log.error("启动插件失败: {}", pluginId);
                }
            } else {
                log.error("启用插件失败: {}", pluginId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("启用插件异常: {}", pluginId, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 禁用插件
     */
    public boolean disablePlugin(String pluginId) {
        lock.writeLock().lock();
        try {
            log.info("禁用插件: {}", pluginId);
            
            PluginStatus currentStatus = pluginManager.getPluginStatus(pluginId);
            if (currentStatus == PluginStatus.DISABLED) {
                log.info("插件已经是禁用状态: {}", pluginId);
                return true;
            }
            
            // 从活跃插件中移除
            pluginManager.getActivePlugins().remove(pluginId);
            
            // 停止插件
            org.pf4j.PluginState stopState = pluginManager.getPf4jManager().stopPlugin(pluginId);
            boolean success = stopState == org.pf4j.PluginState.STOPPED || stopState == org.pf4j.PluginState.DISABLED;
            
            if (success) {
                // 禁用插件
                success = pluginManager.getPf4jManager().disablePlugin(pluginId);
                
                if (success) {
                    // 更新状态
                    pluginManager.getPluginStatus().put(pluginId, PluginStatus.DISABLED);
                    log.info("插件禁用成功: {}", pluginId);
                } else {
                    log.error("禁用插件失败: {}", pluginId);
                }
            } else {
                log.error("停止插件失败: {}", pluginId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("禁用插件异常: {}", pluginId, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取插件健康状态
     */
    public PluginHealthInfo getPluginHealth(String pluginId) {
        lock.readLock().lock();
        try {
            HostCheckerPlugin plugin = pluginManager.getPlugin(pluginId);
            if (plugin == null) {
                return PluginHealthInfo.notFound(pluginId);
            }
            
            PluginStatus status = pluginManager.getPluginStatus(pluginId);
            boolean healthy = plugin.isHealthy();
            
            return PluginHealthInfo.builder()
                    .pluginId(pluginId)
                    .status(status)
                    .healthy(healthy)
                    .lastCheckTime(System.currentTimeMillis())
                    .message(healthy ? "插件健康" : "插件异常")
                    .build();
                    
        } catch (Exception e) {
            log.error("获取插件健康状态异常: {}", pluginId, e);
            return PluginHealthInfo.error(pluginId, e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 批量操作插件
     */
    public BatchOperationResult batchOperatePlugins(List<String> pluginIds, PluginOperation operation) {
        BatchOperationResult result = new BatchOperationResult();
        
        for (String pluginId : pluginIds) {
            try {
                boolean success = switch (operation) {
                    case ENABLE -> enablePlugin(pluginId);
                    case DISABLE -> disablePlugin(pluginId);
                    case RELOAD -> reloadPlugin(pluginId);
                    case UNLOAD -> pluginManager.unloadPlugin(pluginId);
                };

                if (success) {
                    result.addSuccess(pluginId);
                } else {
                    result.addFailure(pluginId, "操作失败");
                }
                
            } catch (Exception e) {
                result.addFailure(pluginId, e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * 获取插件依赖关系
     */
    public PluginDependencyGraph getDependencyGraph() {
        lock.readLock().lock();
        try {
            PluginDependencyGraph graph = new PluginDependencyGraph();
            
            List<PluginWrapper> plugins = pluginManager.getPf4jManager().getPlugins();
            for (PluginWrapper plugin : plugins) {
                String pluginId = plugin.getPluginId();
                List<String> dependencies = plugin.getDescriptor().getDependencies()
                        .stream()
                        .map(PluginDependency::getPluginId)
                        .toList();
                
                graph.addPlugin(pluginId, dependencies);
            }
            
            return graph;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 注册单个插件
     */
    private void registerPlugin(String pluginId) {
        try {
            List<HostCheckerPlugin> plugins = pluginManager.getPf4jManager().getExtensions(HostCheckerPlugin.class);
            
            for (HostCheckerPlugin plugin : plugins) {
                if (pluginId.equals(plugin.getPluginId())) {
                    plugin.initialize();
                    pluginManager.getActivePlugins().put(pluginId, plugin);
                    pluginManager.getPluginStatus().put(pluginId, PluginStatus.ACTIVE);
                    
                    log.info("重新注册插件: {}", pluginId);
                    break;
                }
            }
            
        } catch (Exception e) {
            log.error("注册插件失败: {}", pluginId, e);
        }
    }
    
    /**
     * 获取插件路径（需要在插件管理器中维护路径信息）
     */
    private String getPluginPath(String pluginId) {
        // 这里需要在PluginManager中维护pluginId -> path的映射
        // 或者从PF4J的PluginWrapper中获取路径信息
        PluginWrapper wrapper = pluginManager.getPf4jManager().getPlugin(pluginId);
        return wrapper != null ? wrapper.getPluginPath().toString() : null;
    }
    
    /**
     * 插件操作类型
     */
    public enum PluginOperation {
        ENABLE, DISABLE, RELOAD, UNLOAD
    }
}