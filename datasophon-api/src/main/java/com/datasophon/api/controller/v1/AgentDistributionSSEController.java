package com.datasophon.api.controller.v1;

import com.datasophon.api.agent.util.AgentLogWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Agent分发SSE Controller
 * 提供实时日志推送功能
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-distribution-sse")
@RequiredArgsConstructor
public class AgentDistributionSSEController {
    
    private final AgentLogWriter logWriter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储活动的SSE连接
    // Key: "clusterId:hostIp"
    private static final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    
    // 定时任务调度器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    
    /**
     * 建立SSE连接，推送Agent分发日志
     * 
     * @param clusterId 集群ID（从query parameter获取，因为EventSource无法发送自定义header）
     * @param hostIp 主机IP
     * @return SSE Emitter
     */
    @GetMapping(value = "/stream/{hostIp}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(
            @RequestParam Long clusterId,
            @PathVariable String hostIp) {
        
        log.info("建立Agent分发日志SSE连接: 集群={}, 主机={}", clusterId, hostIp);
        
        String emitterKey = clusterId + ":" + hostIp;
        SseEmitter emitter = new SseEmitter(0L); // 无超时限制
        
        // 移除旧连接
        SseEmitter oldEmitter = activeEmitters.remove(emitterKey);
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (Exception e) {
                log.debug("关闭旧SSE连接失败: {}", e.getMessage());
            }
        }
        
        // 保存新连接
        activeEmitters.put(emitterKey, emitter);
        
        // 设置连接回调
        emitter.onCompletion(() -> {
            activeEmitters.remove(emitterKey);
            log.info("SSE连接正常关闭: 集群={}, 主机={}", clusterId, hostIp);
        });
        
        emitter.onTimeout(() -> {
            activeEmitters.remove(emitterKey);
            log.warn("SSE连接超时: 集群={}, 主机={}", clusterId, hostIp);
        });
        
        emitter.onError(e -> {
            activeEmitters.remove(emitterKey);
            log.error("SSE连接错误: 集群={}, 主机={}, 错误={}", 
                    clusterId, hostIp, e.getMessage());
        });
        
        // 异步推送历史日志
        scheduler.schedule(() -> pushHistoricalLogs(emitter, clusterId, hostIp), 100, TimeUnit.MILLISECONDS);
        
        return emitter;
    }
    
    /**
     * 推送历史日志
     */
    private void pushHistoricalLogs(SseEmitter emitter, Long clusterId, String hostIp) {
        try {
            // 读取Agent分发日志
            String logs = logWriter.readLog(clusterId, hostIp);
            
            if (logs != null && !logs.isEmpty()) {
                String[] logLines = logs.split("\n");
                
                log.info("推送历史日志: 集群={}, 主机={}, 日志行数={}", 
                        clusterId, hostIp, logLines.length);
                
                for (String line : logLines) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    emitter.send(SseEmitter.event()
                            .name("log")
                            .data(line));
                }
            }
            
            // 推送历史日志加载完成事件
            emitter.send(SseEmitter.event()
                    .name("history-loaded")
                    .data("{}"));
            
            log.info("历史日志推送完成: 集群={}, 主机={}", clusterId, hostIp);
            
        } catch (Exception e) {
            log.error("推送历史日志失败: 集群={}, 主机={}, 错误={}", 
                    clusterId, hostIp, e.getMessage(), e);
        }
    }
    
    /**
     * 推送实时日志（由AgentLogWriter调用）
     */
    public static void pushLog(Long clusterId, String hostIp, String logJson) {
        String emitterKey = clusterId + ":" + hostIp;
        SseEmitter emitter = activeEmitters.get(emitterKey);
        
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("log")
                        .data(logJson));
            } catch (IOException e) {
                // 连接已断开，移除
                activeEmitters.remove(emitterKey);
            }
        }
    }
}

