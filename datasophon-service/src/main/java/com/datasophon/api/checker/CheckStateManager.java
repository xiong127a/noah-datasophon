package com.datasophon.api.checker;

import com.datasophon.common.vo.environment.CheckItemStatusVO;
import com.datasophon.common.vo.environment.EnvironmentCheckStatusVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检查状态管理器
 * 使用Caffeine Cache存储检查状态，支持实时更新和查询
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@Component
public class CheckStateManager {
    
    // 集群级别的检查状态缓存
    // Key: clusterId, Value: Map<hostIp, EnvironmentCheckStatusVO>
    private final Cache<Long, Map<String, EnvironmentCheckStatusVO>> clusterStateCache;
    
    // 连接参数缓存
    // Key: clusterId, Value: connectionParams (包含SSH认证信息)
    private final Cache<Long, Map<String, Object>> connectionParamsCache;
    
    // 暂停状态缓存
    // Key: clusterId, Value: isPaused
    private final Map<Long, Boolean> pausedClusters = new ConcurrentHashMap<>();
    
    // 主机信息缓存（主机名、hosts文件等）
    // Key: clusterId, Value: Map<hostIp, Map<String, Object>>
    private final Cache<Long, Map<String, Map<String, Object>>> hostInfoCache;
    
    public CheckStateManager() {
        this.clusterStateCache = Caffeine.newBuilder()
                .maximumSize(100) // 最多缓存100个集群
                .expireAfterWrite(Duration.ofHours(1)) // 1小时后过期
                .expireAfterAccess(Duration.ofMinutes(30)) // 30分钟不访问则过期
                .build();
        
        this.connectionParamsCache = Caffeine.newBuilder()
                .maximumSize(100) // 最多缓存100个集群的连接参数
                .expireAfterWrite(Duration.ofHours(1)) // 1小时后过期
                .expireAfterAccess(Duration.ofMinutes(30)) // 30分钟不访问则过期
                .build();
        
        this.hostInfoCache = Caffeine.newBuilder()
                .maximumSize(100) // 最多缓存100个集群的主机信息
                .expireAfterWrite(Duration.ofHours(1)) // 1小时后过期
                .expireAfterAccess(Duration.ofMinutes(30)) // 30分钟不访问则过期
                .build();
    }
    
    /**
     * 更新主机状态
     */
    public void updateHostStatus(Long clusterId, String hostIp, EnvironmentCheckStatusVO statusVO) {
        var hostMap = clusterStateCache.get(clusterId, k -> new ConcurrentHashMap<>());
        if (hostMap != null) {
            hostMap.put(hostIp, statusVO);
        }
        log.debug("更新主机状态: 集群={}, 主机={}, 状态={}", 
                clusterId, hostIp, statusVO.getOverallStatus());
    }
    
    /**
     * 更新检查项状态
     */
    public void updateCheckItemStatus(Long clusterId, String hostIp, String checkKey, CheckItemStatusVO itemVO) {
        var hostMap = clusterStateCache.getIfPresent(clusterId);
        if (hostMap != null) {
            var hostStatus = hostMap.get(hostIp);
            if (hostStatus != null) {
                // 更新或添加检查项
                var existingItem = hostStatus.getCheckItems().stream()
                        .filter(item -> item.getCheckKey().equals(checkKey))
                        .findFirst();
                
                if (existingItem.isPresent()) {
                    // 更新现有项
                    int index = hostStatus.getCheckItems().indexOf(existingItem.get());
                    hostStatus.getCheckItems().set(index, itemVO);
                } else {
                    // 添加新项
                    hostStatus.getCheckItems().add(itemVO);
                }
            }
        }
        log.debug("更新检查项状态: 集群={}, 主机={}, 检查项={}, 状态={}", 
                clusterId, hostIp, checkKey, itemVO.getStatus());
    }
    
    /**
     * 获取集群所有主机的检查状态
     */
    public List<EnvironmentCheckStatusVO> getClusterStatus(Long clusterId) {
        var hostMap = clusterStateCache.getIfPresent(clusterId);
        if (hostMap == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(hostMap.values());
    }
    
    /**
     * 获取单台主机的检查状态
     */
    public EnvironmentCheckStatusVO getHostStatus(Long clusterId, String hostIp) {
        var hostMap = clusterStateCache.getIfPresent(clusterId);
        if (hostMap != null) {
            return hostMap.get(hostIp);
        }
        return null;
    }
    
    /**
     * 标记检查项为已跳过
     */
    public void markItemSkipped(Long clusterId, String hostIp, String checkKey) {
        var hostStatus = getHostStatus(clusterId, hostIp);
        if (hostStatus != null) {
            hostStatus.getCheckItems().stream()
                    .filter(item -> item.getCheckKey().equals(checkKey))
                    .findFirst()
                    .ifPresent(item -> {
                        item.setStatus(com.datasophon.common.enums.CheckItemStatus.SKIPPED);
                        item.setMessage("用户选择跳过此检查项");
                        log.info("检查项已跳过: 集群={}, 主机={}, 检查项={}", clusterId, hostIp, checkKey);
                    });
        }
    }
    
    /**
     * 暂停检查
     */
    public void pauseCheck(Long clusterId) {
        pausedClusters.put(clusterId, true);
        log.info("暂停集群 {} 的环境检查", clusterId);
    }
    
    /**
     * 恢复检查
     */
    public void resumeCheck(Long clusterId) {
        pausedClusters.remove(clusterId);
        log.info("恢复集群 {} 的环境检查", clusterId);
    }
    
    /**
     * 检查是否暂停
     */
    public boolean isPaused(Long clusterId) {
        return pausedClusters.getOrDefault(clusterId, false);
    }
    
    /**
     * 保存连接参数
     */
    public void saveConnectionParams(Long clusterId, Map<String, Object> connectionParams) {
        connectionParamsCache.put(clusterId, new ConcurrentHashMap<>(connectionParams));
        log.debug("保存集群 {} 的连接参数", clusterId);
    }
    
    /**
     * 获取连接参数
     */
    public Map<String, Object> getConnectionParams(Long clusterId) {
        return connectionParamsCache.getIfPresent(clusterId);
    }
    
    /**
     * 存储主机信息（主机名、hosts文件等）
     */
    public void storeHostInfo(Long clusterId, String hostIp, Map<String, Object> info) {
        var hostMap = hostInfoCache.get(clusterId, k -> new ConcurrentHashMap<>());
        if (hostMap != null) {
            hostMap.put(hostIp, info);
        }
        log.debug("存储主机信息: 集群={}, 主机={}", clusterId, hostIp);
    }
    
    /**
     * 获取单台主机的信息
     */
    public Map<String, Object> getHostInfo(Long clusterId, String hostIp) {
        var hostMap = hostInfoCache.getIfPresent(clusterId);
        if (hostMap != null) {
            return hostMap.get(hostIp);
        }
        return null;
    }
    
    /**
     * 获取集群所有主机的信息
     */
    public Map<String, Map<String, Object>> getAllHostInfo(Long clusterId) {
        var hostMap = hostInfoCache.getIfPresent(clusterId);
        if (hostMap != null) {
            return new ConcurrentHashMap<>(hostMap);
        }
        return new ConcurrentHashMap<>();
    }
    
    /**
     * 清除集群状态
     */
    public void clearClusterState(Long clusterId) {
        clusterStateCache.invalidate(clusterId);
        connectionParamsCache.invalidate(clusterId);
        hostInfoCache.invalidate(clusterId);
        pausedClusters.remove(clusterId);
        log.info("清除集群 {} 的检查状态", clusterId);
    }
}

