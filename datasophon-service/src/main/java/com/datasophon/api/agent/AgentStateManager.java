package com.datasophon.api.agent;

import com.datasophon.api.event.AgentDistributionStatusChangeEvent;
import com.datasophon.common.vo.agent.AgentDistributionStatusVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent分发状态管理器
 * 管理集群级别的Agent分发状态
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
@Component
public class AgentStateManager {
    
    private final ApplicationEventPublisher eventPublisher;
    
    // 集群级别的分发状态缓存
    // Key: clusterId, Value: Map<hostIp, AgentDistributionStatusVO>
    private final Cache<Long, Map<String, AgentDistributionStatusVO>> clusterStateCache;
    
    // 取消标志缓存
    // Key: clusterId, Value: isCancelled
    private final Map<Long, Boolean> cancelledClusters = new ConcurrentHashMap<>();
    
    @Autowired
    public AgentStateManager(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.clusterStateCache = Caffeine.newBuilder()
                .maximumSize(100) // 最多缓存100个集群
                .expireAfterWrite(Duration.ofHours(2)) // 2小时后过期
                .expireAfterAccess(Duration.ofMinutes(30)) // 30分钟不访问则过期
                .build();
    }
    
    /**
     * 初始化主机分发状态
     */
    public void initHostStatus(Long clusterId, String hostIp, String hostname) {
        Map<String, AgentDistributionStatusVO> hostStatuses = clusterStateCache.get(clusterId,
                k -> new ConcurrentHashMap<>());
        
        AgentDistributionStatusVO status = AgentDistributionStatusVO.builder()
                .hostIp(hostIp)
                .hostname(hostname)
                .status("PENDING")
                .progress(0)
                .currentStep("")
                .message("等待开始分发")
                .startTime(null)
                .endTime(null)
                .build();
        
        hostStatuses.put(hostIp, status);
        log.info("初始化主机分发状态: 集群={}, 主机={}", clusterId, hostIp);
    }
    
    /**
     * 更新主机分发状态
     */
    public void updateHostStatus(Long clusterId, String hostIp, String status, Integer progress,
                                  String currentStep, String message) {
        Map<String, AgentDistributionStatusVO> hostStatuses = clusterStateCache.getIfPresent(clusterId);
        if (hostStatuses == null) {
            log.warn("集群状态不存在: {}", clusterId);
            return;
        }
        
        AgentDistributionStatusVO hostStatus = hostStatuses.get(hostIp);
        if (hostStatus == null) {
            log.warn("主机状态不存在: 集群={}, 主机={}", clusterId, hostIp);
            return;
        }
        
        hostStatus.setStatus(status);
        hostStatus.setProgress(progress);
        hostStatus.setCurrentStep(currentStep);
        hostStatus.setMessage(message);
        
        // 设置时间戳
        if ("RUNNING".equals(status) && hostStatus.getStartTime() == null) {
            hostStatus.setStartTime(System.currentTimeMillis());
        }
        
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            hostStatus.setEndTime(System.currentTimeMillis());
        }
        
        log.debug("更新主机分发状态: 集群={}, 主机={}, 状态={}, 进度={}%, 步骤={}",
                clusterId, hostIp, status, progress, currentStep);
        
        // 发布状态变更事件，触发SSE推送
        publishStatusChangeEvent(clusterId, hostIp);
    }
    
    /**
     * 发布Agent分发状态变更事件
     */
    private void publishStatusChangeEvent(Long clusterId, String hostIp) {
        eventPublisher.publishEvent(new AgentDistributionStatusChangeEvent(this, clusterId, hostIp));
        log.debug("发布Agent分发状态变更事件: 集群={}, 主机={}", clusterId, hostIp);
    }
    
    /**
     * 获取集群所有主机的分发状态
     */
    public List<AgentDistributionStatusVO> getClusterStatus(Long clusterId) {
        Map<String, AgentDistributionStatusVO> hostStatuses = clusterStateCache.getIfPresent(clusterId);
        if (hostStatuses == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(hostStatuses.values());
    }
    
    /**
     * 获取单个主机的分发状态
     */
    public AgentDistributionStatusVO getHostStatus(Long clusterId, String hostIp) {
        Map<String, AgentDistributionStatusVO> hostStatuses = clusterStateCache.getIfPresent(clusterId);
        if (hostStatuses == null) {
            return null;
        }
        
        return hostStatuses.get(hostIp);
    }
    
    /**
     * 清理集群分发状态
     */
    public void clearClusterState(Long clusterId) {
        clusterStateCache.invalidate(clusterId);
        cancelledClusters.remove(clusterId);
        log.info("清理集群分发状态: {}", clusterId);
    }
    
    /**
     * 标记集群分发为已取消
     */
    public void cancelCluster(Long clusterId) {
        cancelledClusters.put(clusterId, true);
        log.info("标记集群分发已取消: {}", clusterId);
    }
    
    /**
     * 检查集群分发是否已取消
     */
    public boolean isCancelled(Long clusterId) {
        return cancelledClusters.getOrDefault(clusterId, false);
    }
}

