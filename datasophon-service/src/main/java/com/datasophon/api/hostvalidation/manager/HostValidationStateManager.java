package com.datasophon.api.hostvalidation.manager;

import com.datasophon.common.dto.HostValidationRequestDTO;
import com.datasophon.common.enums.CheckType;
import com.datasophon.common.enums.ValidationStatus;
import com.datasophon.common.vo.CheckItemStatusVO;
import com.datasophon.common.vo.HostValidationStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 主机校验状态管理器
 * 纯内存存储，负责管理校验状态和SSE推送
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Component
public class HostValidationStateManager {
    
    // 校验会话存储 - clusterId -> HostValidationSession
    private final Map<Long, HostValidationSession> validationSessions = new ConcurrentHashMap<>();
    
    // SSE连接管理 - clusterId -> List<SseEmitter>
    private final Map<Long, List<SseEmitter>> sseConnections = new ConcurrentHashMap<>();
    
    // 日志SSE连接管理 - clusterId -> hostIp -> List<SseEmitter>
    private final Map<Long, Map<String, List<SseEmitter>>> logConnections = new ConcurrentHashMap<>();
    
    /**
     * 校验会话数据结构
     */
    public static class HostValidationSession {
        private final Long clusterId;
        private final HostValidationRequestDTO request;
        private final Map<String, HostValidationStatusVO> hostStatuses;
        private final LocalDateTime startTime;
        private volatile boolean completed;
        
        public HostValidationSession(Long clusterId, HostValidationRequestDTO request) {
            this.clusterId = clusterId;
            this.request = request;
            this.hostStatuses = new ConcurrentHashMap<>();
            this.startTime = LocalDateTime.now();
            this.completed = false;
            
            // 初始化主机状态
            initializeHostStatuses(request);
        }
        
        private void initializeHostStatuses(HostValidationRequestDTO request) {
            for (String hostIp : request.hostIps()) {
                List<CheckItemStatusVO> checkItems = Arrays.stream(CheckType.values())
                    .sorted(Comparator.comparing(CheckType::getPriority))
                    .map(checkType -> new CheckItemStatusVO(
                        checkType.getCode(),
                        checkType.getDisplayName(),
                        ValidationStatus.PENDING,
                        "等待检查",
                        Map.of(),
                        LocalDateTime.now(),
                        checkType.isRepairAvailable(),
                        checkType.isRepairAvailable() ? "支持自动修复" : "不支持修复"
                    ))
                    .toList();
                
                HostValidationStatusVO hostStatus = new HostValidationStatusVO(
                    hostIp,
                    "", // hostname will be filled during validation
                    ValidationStatus.PENDING,
                    checkItems,
                    new CopyOnWriteArrayList<>(),
                    LocalDateTime.now(),
                    false
                );
                
                hostStatuses.put(hostIp, hostStatus);
            }
        }
        
        // Getters
        public Long getClusterId() { return clusterId; }
        public HostValidationRequestDTO getRequest() { return request; }
        public Map<String, HostValidationStatusVO> getHostStatuses() { return hostStatuses; }
        public LocalDateTime getStartTime() { return startTime; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
    
    /**
     * 创建校验会话
     */
    public void createValidationSession(Long clusterId, HostValidationRequestDTO request) {
        HostValidationSession session = new HostValidationSession(clusterId, request);
        validationSessions.put(clusterId, session);
        
        log.info("创建主机校验会话: clusterId={}, 主机数量={}", 
                clusterId, request.hostIps().size());
        
        // 推送初始状态
        pushStatusUpdate(clusterId);
    }
    
    /**
     * 获取校验会话
     */
    public Optional<HostValidationSession> getValidationSession(Long clusterId) {
        return Optional.ofNullable(validationSessions.get(clusterId));
    }
    
    /**
     * 更新主机检查项状态
     */
    public void updateCheckItemStatus(Long clusterId, String hostIp, CheckType checkType, 
                                    ValidationStatus status, String message, 
                                    Map<String, Object> details) {
        HostValidationSession session = validationSessions.get(clusterId);
        if (session == null) {
            log.warn("校验会话不存在: clusterId={}", clusterId);
            return;
        }
        
        HostValidationStatusVO hostStatus = session.getHostStatuses().get(hostIp);
        if (hostStatus == null) {
            log.warn("主机状态不存在: clusterId={}, hostIp={}", clusterId, hostIp);
            return;
        }
        
        // 更新检查项状态
        List<CheckItemStatusVO> updatedCheckItems = hostStatus.checkItems().stream()
            .map(item -> {
                if (item.checkType().equals(checkType.getCode())) {
                    return new CheckItemStatusVO(
                        item.checkType(),
                        item.displayName(),
                        status,
                        message,
                        details,
                        LocalDateTime.now(),
                        item.repairAvailable(),
                        item.repairAction()
                    );
                }
                return item;
            })
            .toList();
        
        // 计算整体状态
        ValidationStatus overallStatus = calculateOverallStatus(updatedCheckItems);
        boolean canRepair = updatedCheckItems.stream()
            .anyMatch(item -> item.status() == ValidationStatus.FAILED && item.repairAvailable());
        
        // 添加日志
        String logMessage = String.format("[%s] %s: %s - %s", 
                LocalDateTime.now().toString(), 
                checkType.getDisplayName(),
                status.getDescription(), 
                message);
        
        List<String> updatedLogs = new ArrayList<>(hostStatus.logs());
        updatedLogs.add(logMessage);
        
        // 创建新的主机状态
        HostValidationStatusVO updatedHostStatus = new HostValidationStatusVO(
            hostStatus.hostIp(),
            hostStatus.hostname(),
            overallStatus,
            updatedCheckItems,
            updatedLogs,
            LocalDateTime.now(),
            canRepair
        );
        
        session.getHostStatuses().put(hostIp, updatedHostStatus);
        
        log.debug("更新检查项状态: clusterId={}, hostIp={}, checkType={}, status={}", 
                clusterId, hostIp, checkType, status);
        
        // 推送状态更新
        pushStatusUpdate(clusterId);
    }
    
    /**
     * 更新主机基础信息（hostname等）
     */
    public void updateHostInfo(Long clusterId, String hostIp, String hostname) {
        HostValidationSession session = validationSessions.get(clusterId);
        if (session == null) return;
        
        HostValidationStatusVO hostStatus = session.getHostStatuses().get(hostIp);
        if (hostStatus == null) return;
        
        HostValidationStatusVO updatedHostStatus = new HostValidationStatusVO(
            hostStatus.hostIp(),
            hostname,
            hostStatus.overallStatus(),
            hostStatus.checkItems(),
            hostStatus.logs(),
            hostStatus.lastUpdateTime(),
            hostStatus.canRepair()
        );
        
        session.getHostStatuses().put(hostIp, updatedHostStatus);
        
        // 推送状态更新
        pushStatusUpdate(clusterId);
    }
    
    /**
     * 添加日志
     */
    public void addLog(Long clusterId, String hostIp, String logMessage) {
        HostValidationSession session = validationSessions.get(clusterId);
        if (session == null) return;
        
        HostValidationStatusVO hostStatus = session.getHostStatuses().get(hostIp);
        if (hostStatus == null) return;
        
        String timestampedLog = String.format("[%s] %s", 
                LocalDateTime.now().toString(), logMessage);
        
        List<String> updatedLogs = new ArrayList<>(hostStatus.logs());
        updatedLogs.add(timestampedLog);
        
        HostValidationStatusVO updatedHostStatus = new HostValidationStatusVO(
            hostStatus.hostIp(),
            hostStatus.hostname(),
            hostStatus.overallStatus(),
            hostStatus.checkItems(),
            updatedLogs,
            LocalDateTime.now(),
            hostStatus.canRepair()
        );
        
        session.getHostStatuses().put(hostIp, updatedHostStatus);
        
        // 推送日志更新
        pushLogUpdate(clusterId, hostIp, timestampedLog);
    }
    
    /**
     * 标记会话完成
     */
    public void completeValidationSession(Long clusterId) {
        HostValidationSession session = validationSessions.get(clusterId);
        if (session != null) {
            session.setCompleted(true);
            log.info("主机校验会话完成: clusterId={}, 耗时={}ms", 
                    clusterId, 
                    java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).toMillis());
            
            // 推送完成状态
            pushCompletionUpdate(clusterId);
        }
    }
    
    /**
     * 清理校验会话（可选，用于释放内存）
     */
    public boolean cleanupValidationSession(Long clusterId) {
        HostValidationSession session = validationSessions.remove(clusterId);
        if (session != null) {
            // 清理所有连接
            removeAllConnections(clusterId);
            removeAllLogConnections(clusterId);
            
            log.info("校验会话数据已清理: clusterId={}, 主机数量={}", 
                    clusterId, session.getHostStatuses().size());
            return true;
        } else {
            log.debug("未找到需要清理的校验会话: clusterId={}", clusterId);
            return false;
        }
    }
    
    // ==================== SSE连接管理 ====================
    
    /**
     * 添加SSE连接
     */
    public void addSseConnection(Long clusterId, SseEmitter emitter) {
        sseConnections.computeIfAbsent(clusterId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        // 设置超时和完成回调
        emitter.onTimeout(() -> removeSseConnection(clusterId, emitter));
        emitter.onCompletion(() -> removeSseConnection(clusterId, emitter));
        emitter.onError(throwable -> {
            log.warn("SSE连接错误: clusterId={}, error={}", clusterId, throwable.getMessage());
            removeSseConnection(clusterId, emitter);
        });
        
        log.debug("添加SSE连接: clusterId={}, 当前连接数={}", 
                clusterId, sseConnections.get(clusterId).size());
        
        // 发送当前状态
        pushStatusUpdate(clusterId);
    }
    
    /**
     * 移除SSE连接
     */
    public void removeSseConnection(Long clusterId, SseEmitter emitter) {
        List<SseEmitter> connections = sseConnections.get(clusterId);
        if (connections != null) {
            connections.remove(emitter);
            if (connections.isEmpty()) {
                sseConnections.remove(clusterId);
            }
        }
    }
    
    /**
     * 推送状态更新
     */
    private void pushStatusUpdate(Long clusterId) {
        HostValidationSession session = validationSessions.get(clusterId);
        List<SseEmitter> connections = sseConnections.get(clusterId);
        
        if (session == null || connections == null || connections.isEmpty()) {
            return;
        }
        
        Map<String, Object> statusData = Map.of(
            "type", "status_update",
            "clusterId", clusterId,
            "hosts", session.getHostStatuses().values(),
            "timestamp", LocalDateTime.now().toString()
        );
        
        sendSseData(connections, statusData);
    }
    
    /**
     * 推送日志更新
     */
    private void pushLogUpdate(Long clusterId, String hostIp, String logMessage) {
        List<SseEmitter> connections = sseConnections.get(clusterId);
        if (connections == null || connections.isEmpty()) {
            return;
        }
        
        Map<String, Object> logData = Map.of(
            "type", "log_update",
            "clusterId", clusterId,
            "hostIp", hostIp,
            "log", logMessage,
            "timestamp", LocalDateTime.now().toString()
        );
        
        sendSseData(connections, logData);
    }
    
    /**
     * 推送完成更新
     */
    private void pushCompletionUpdate(Long clusterId) {
        List<SseEmitter> connections = sseConnections.get(clusterId);
        if (connections == null || connections.isEmpty()) {
            return;
        }
        
        Map<String, Object> completionData = Map.of(
            "type", "validation_completed",
            "clusterId", clusterId,
            "timestamp", LocalDateTime.now().toString()
        );
        
        sendSseData(connections, completionData);
    }
    
    /**
     * 发送SSE数据
     */
    private void sendSseData(List<SseEmitter> connections, Map<String, Object> data) {
        List<SseEmitter> deadConnections = new ArrayList<>();
        
        for (SseEmitter emitter : connections) {
            try {
                emitter.send(SseEmitter.event()
                    .name("host-validation")
                    .data(data));
            } catch (Exception e) {
                log.warn("SSE发送失败: {}", e.getMessage());
                deadConnections.add(emitter);
            }
        }
        
        // 清理失效连接
        for (SseEmitter deadConnection : deadConnections) {
            connections.remove(deadConnection);
            try {
                deadConnection.complete();
            } catch (Exception ignored) {
            }
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 计算整体状态
     */
    private ValidationStatus calculateOverallStatus(List<CheckItemStatusVO> checkItems) {
        boolean hasChecking = checkItems.stream()
            .anyMatch(item -> item.status() == ValidationStatus.CHECKING);
        boolean hasFailed = checkItems.stream()
            .anyMatch(item -> item.status() == ValidationStatus.FAILED);
        boolean hasRepairing = checkItems.stream()
            .anyMatch(item -> item.status() == ValidationStatus.REPAIRING);
        boolean allCompleted = checkItems.stream()
            .allMatch(item -> item.status() == ValidationStatus.SUCCESS || 
                            item.status() == ValidationStatus.FAILED ||
                            item.status() == ValidationStatus.REPAIRED);
        
        if (hasRepairing) {
            return ValidationStatus.REPAIRING;
        } else if (hasChecking) {
            return ValidationStatus.CHECKING;
        } else if (hasFailed) {
            return ValidationStatus.FAILED;
        } else if (allCompleted) {
            return ValidationStatus.SUCCESS;
        } else {
            return ValidationStatus.PENDING;
        }
    }

    /**
     * 添加日志SSE连接
     */
    public void addLogConnection(Long clusterId, String hostIp, SseEmitter emitter) {
        logConnections.computeIfAbsent(clusterId, k -> new ConcurrentHashMap<>())
                      .computeIfAbsent(hostIp != null ? hostIp : "all", k -> new CopyOnWriteArrayList<>())
                      .add(emitter);
        
        // 设置连接断开时的清理
        emitter.onCompletion(() -> removeLogConnection(clusterId, hostIp, emitter));
        emitter.onTimeout(() -> removeLogConnection(clusterId, hostIp, emitter));
        emitter.onError((ex) -> {
            log.warn("日志SSE连接发生错误: clusterId={}, hostIp={}, error={}", 
                    clusterId, hostIp, ex.getMessage());
            removeLogConnection(clusterId, hostIp, emitter);
        });
        
        log.debug("添加日志SSE连接: clusterId={}, hostIp={}", clusterId, hostIp);
    }

    /**
     * 移除日志SSE连接
     */
    private void removeLogConnection(Long clusterId, String hostIp, SseEmitter emitter) {
        Map<String, List<SseEmitter>> clusterLogConnections = logConnections.get(clusterId);
        if (clusterLogConnections != null) {
            String key = hostIp != null ? hostIp : "all";
            List<SseEmitter> connections = clusterLogConnections.get(key);
            if (connections != null) {
                connections.remove(emitter);
                if (connections.isEmpty()) {
                    clusterLogConnections.remove(key);
                }
            }
            if (clusterLogConnections.isEmpty()) {
                logConnections.remove(clusterId);
            }
        }
        log.debug("移除日志SSE连接: clusterId={}, hostIp={}", clusterId, hostIp);
    }

    /**
     * 推送日志信息
     */
    public void sendLogMessage(Long clusterId, String hostIp, String logLevel, String message, String source) {
        Map<String, List<SseEmitter>> clusterLogConnections = logConnections.get(clusterId);
        if (clusterLogConnections == null) {
            return;
        }

        Map<String, Object> logData = Map.of(
            "type", "log",
            "clusterId", clusterId,
            "hostIp", hostIp,
            "logLevel", logLevel,
            "message", message,
            "source", source,
            "timestamp", LocalDateTime.now().toString()
        );

        // 发送给该主机的专用连接
        List<SseEmitter> hostConnections = clusterLogConnections.get(hostIp);
        if (hostConnections != null) {
            sendLogToConnections(hostConnections, logData);
        }

        // 发送给全局连接
        List<SseEmitter> allConnections = clusterLogConnections.get("all");
        if (allConnections != null) {
            sendLogToConnections(allConnections, logData);
        }
    }

    /**
     * 向指定连接发送日志
     */
    private void sendLogToConnections(List<SseEmitter> connections, Map<String, Object> logData) {
        Iterator<SseEmitter> iterator = connections.iterator();
        while (iterator.hasNext()) {
            SseEmitter emitter = iterator.next();
            try {
                emitter.send(SseEmitter.event()
                    .name("log")
                    .data(logData));
            } catch (Exception e) {
                log.warn("推送日志消息失败，移除连接: {}", e.getMessage());
                iterator.remove();
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    // 忽略关闭连接时的异常
                }
            }
        }
    }

    /**
     * 暂停主机校验
     */
    public void pauseHost(Long clusterId, String hostIp) {
        HostValidationSession session = validationSessions.get(clusterId);
        if (session != null) {
            if (hostIp != null) {
                HostValidationStatusVO status = session.getHostStatuses().get(hostIp);
                if (status != null) {
                    // 由于HostValidationStatusVO是record，无法直接修改，这里需要创建新的状态对象
                    // TODO: 如果需要暂停功能，需要重新设计状态结构
                    sendLogMessage(clusterId, hostIp, "INFO", "主机校验已暂停", "StateManager");
                }
            } else {
                // 暂停所有主机
                session.getHostStatuses().forEach((ip, status) -> {
                    // 由于HostValidationStatusVO是record，无法直接修改，这里需要创建新的状态对象
                    // TODO: 如果需要暂停功能，需要重新设计状态结构
                    sendLogMessage(clusterId, ip, "INFO", "主机校验已暂停", "StateManager");
                });
            }
        }
    }

    /**
     * 继续主机校验
     */
    public void resumeHost(Long clusterId, String hostIp) {
        HostValidationSession session = validationSessions.get(clusterId);
        if (session != null) {
            if (hostIp != null) {
                HostValidationStatusVO status = session.getHostStatuses().get(hostIp);
                if (status != null) {
                    // 由于HostValidationStatusVO是record，无法直接修改，这里需要创建新的状态对象
                    // TODO: 如果需要继续功能，需要重新设计状态结构
                    sendLogMessage(clusterId, hostIp, "INFO", "主机校验已继续", "StateManager");
                }
            } else {
                // 继续所有主机
                session.getHostStatuses().forEach((ip, status) -> {
                    // 由于HostValidationStatusVO是record，无法直接修改，这里需要创建新的状态对象
                    // TODO: 如果需要继续功能，需要重新设计状态结构
                    sendLogMessage(clusterId, ip, "INFO", "主机校验已继续", "StateManager");
                });
            }
        }
    }
    
    /**
     * 获取失败的检查项类型（返回枚举类型）
     */
    public List<CheckType> getFailedCheckTypes(Long clusterId, String hostIp) {
        return getValidationSession(clusterId)
                .map(session -> session.getHostStatuses().get(hostIp))
                .map(hostStatus -> hostStatus.checkItems().stream()
                        .filter(item -> item.status() == ValidationStatus.FAILED)
                        .map(item -> CheckType.fromCode(item.checkType()))
                        .filter(Objects::nonNull)
                        .toList())
                .orElse(List.of());
    }
    
    /**
     * 获取所有活跃的校验会话
     */
    public List<Long> getActiveValidationSessions() {
        return validationSessions.entrySet().stream()
            .filter(entry -> !entry.getValue().isCompleted())
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * 清理所有状态连接
     */
    private void removeAllConnections(Long clusterId) {
        List<SseEmitter> connections = sseConnections.remove(clusterId);
        if (connections != null) {
            connections.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("关闭SSE连接异常", e);
                }
            });
        }
    }
    
    /**
     * 广播状态更新
     */
    private void broadcastStatusUpdate(Long clusterId, HostValidationStatusVO status) {
        List<SseEmitter> connections = sseConnections.get(clusterId);
        if (connections != null) {
            connections.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("status-update")
                        .data(status));
                    return false;
                } catch (Exception e) {
                    log.debug("发送状态更新失败", e);
                    return true;
                }
            });
        }
    }
    
    /**
     * 清理所有日志连接
     */
    private void removeAllLogConnections(Long clusterId) {
        Map<String, List<SseEmitter>> hostLogConnections = logConnections.remove(clusterId);
        if (hostLogConnections != null) {
            hostLogConnections.values().forEach(emitters -> 
                emitters.forEach(emitter -> {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        log.debug("完成日志SSE连接时出错", e);
                    }
                }));
            log.debug("清理日志连接: clusterId={}, 主机数量={}", clusterId, hostLogConnections.size());
        }
    }
}
