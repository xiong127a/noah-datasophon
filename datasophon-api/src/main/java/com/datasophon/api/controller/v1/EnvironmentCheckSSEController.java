package com.datasophon.api.controller.v1;

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.checker.CheckStateManager;
import com.datasophon.api.event.CheckStatusChangeEvent;
import com.datasophon.api.service.EnvironmentCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 环境检查SSE控制器 - 事件驱动版本
 * 通过Server-Sent Events推送实时检查进度
 * 监听CheckStatusChangeEvent，状态变化时立即推送
 * 
 * @author 任相鹏
 * @date 2025-01-28
 */
@Slf4j
@ApiVersion(path = "sse/environment-check")
@RequiredArgsConstructor
public class EnvironmentCheckSSEController implements ApplicationListener<CheckStatusChangeEvent> {
    
    private final CheckStateManager stateManager;
    private final EnvironmentCheckService environmentCheckService;
    
    // 存储所有活跃的SSE连接
    // Key: clusterId, Value: SseEmitter
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    
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
            
            // 立即推送一次当前状态
            pushStatusUpdate(clusterId, emitter);
        } catch (IOException e) {
            log.error("发送初始消息失败: {}", e.getMessage());
            cleanupConnection(emitterKey);
        }
        
        return emitter;
    }
    
    /**
     * 监听状态变更事件
     */
    @Override
    public void onApplicationEvent(CheckStatusChangeEvent event) {
        Long clusterId = event.getClusterId();
        String emitterKey = clusterId.toString();
        SseEmitter emitter = activeEmitters.get(emitterKey);
        
        if (emitter != null) {
            log.debug("收到状态变更事件: 集群={}, 时间={}", clusterId, event.getEventTimestamp());
            pushStatusUpdate(clusterId, emitter);
        } else {
            log.trace("没有活跃的SSE连接: 集群={}", clusterId);
        }
    }
    
    /**
     * 推送状态更新
     */
    private void pushStatusUpdate(Long clusterId, SseEmitter emitter) {
        try {
            var status = stateManager.getClusterStatus(clusterId);
            var validation = environmentCheckService.validateForNextStep(clusterId);
            
            var data = Map.of(
                    "type", "progress",
                    "data", status,
                    "validation", validation
            );
            
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(data));
            
            log.debug("推送状态更新: 集群={}, 主机数={}", clusterId, status.size());
        } catch (IOException e) {
            log.warn("SSE连接已断开: 集群={}", clusterId);
            activeEmitters.remove(clusterId.toString());
        } catch (Exception e) {
            log.error("推送状态失败: 集群={}, 错误={}", clusterId, e.getMessage(), e);
        }
    }
    
    /**
     * 清理SSE连接
     */
    private void cleanupConnection(String emitterKey) {
        activeEmitters.remove(emitterKey);
        log.debug("清理SSE连接: key={}", emitterKey);
    }
}
