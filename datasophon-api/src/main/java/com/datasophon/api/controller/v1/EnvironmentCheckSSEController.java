package com.datasophon.api.controller.v1;

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.checker.CheckStateManager;
import com.datasophon.api.service.EnvironmentCheckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 环境检查SSE控制器
 * 通过Server-Sent Events推送实时检查进度
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@ApiVersion(path = "environment-check-sse")
@RequiredArgsConstructor
public class EnvironmentCheckSSEController {
    
    private final CheckStateManager stateManager;
    private final EnvironmentCheckService environmentCheckService;
    private final ObjectMapper objectMapper;
    
    // 存储所有活跃的SSE连接
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    
    // 存储每个连接对应的定时任务
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    // 定时推送器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * 建立SSE连接，实时推送检查进度
     */
    @GetMapping(path = "/stream/{clusterId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCheckProgress(@PathVariable Long clusterId) {
        
        log.info("建立SSE连接: 集群ID={}", clusterId);
        
        // 创建SSE emitter（5分钟超时）
        var emitter = new SseEmitter(5 * 60 * 1000L);
        var emitterKey = clusterId.toString();
        
        // 注册emitter
        activeEmitters.put(emitterKey, emitter);
        
        // 设置完成和超时回调
        emitter.onCompletion(() -> {
            log.info("SSE连接完成: 集群ID={}", clusterId);
            cleanupConnection(emitterKey);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE连接超时: 集群ID={}", clusterId);
            cleanupConnection(emitterKey);
            emitter.complete();
        });
        
        emitter.onError(e -> {
            log.error("SSE连接错误: 集群ID={}, 错误={}", clusterId, e.getMessage());
            cleanupConnection(emitterKey);
        });
        
        // 发送初始连接消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("message", "连接建立成功", "clusterId", clusterId)));
        } catch (IOException e) {
            log.error("发送初始消息失败: {}", e.getMessage());
        }
        
        // 启动定时推送任务（每秒推送一次状态和验证结果）
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                var status = stateManager.getClusterStatus(clusterId);
                if (!status.isEmpty()) {
                    // 同时计算验证结果（避免前端轮询）
                    var validation = environmentCheckService.validateForNextStep(clusterId);
                    
                    var json = objectMapper.writeValueAsString(Map.of(
                            "type", "progress",
                            "data", status,
                            "validation", validation  // 新增：同时推送验证结果
                    ));
                    
                    emitter.send(SseEmitter.event()
                            .name("progress")
                            .data(json));
                }
            } catch (Exception e) {
                log.error("推送进度失败: {}", e.getMessage());
                cleanupConnection(emitterKey);
                emitter.completeWithError(e);
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        // 保存定时任务引用，以便后续取消
        scheduledTasks.put(emitterKey, task);
        
        return emitter;
    }
    
    /**
     * 清理SSE连接和定时任务
     */
    private void cleanupConnection(String emitterKey) {
        // 移除emitter
        activeEmitters.remove(emitterKey);
        
        // 取消并移除定时任务
        ScheduledFuture<?> task = scheduledTasks.remove(emitterKey);
        if (task != null && !task.isCancelled()) {
            boolean cancelled = task.cancel(true);
            log.info("取消定时任务: key={}, 成功={}", emitterKey, cancelled);
        }
    }
}

