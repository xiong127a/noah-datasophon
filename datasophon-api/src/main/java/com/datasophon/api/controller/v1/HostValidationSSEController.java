package com.datasophon.api.controller.v1;

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.hostvalidation.manager.HostValidationStateManager;
import com.datasophon.api.hostvalidation.service.HostValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 主机校验SSE实时通信控制器
 * 专门负责实时数据推送（SSE）
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@RestController
@ApiVersion(path = "sse/host-validation")
@RequiredArgsConstructor
public class HostValidationSSEController {
    
    private final HostValidationService hostValidationService;
    private final HostValidationStateManager stateManager;
    
    /**
     * 建立SSE连接，接收实时状态更新
     */
    @GetMapping(value = "/status/{clusterId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long clusterId) {
        
        log.info("建立主机校验SSE连接: clusterId={}", clusterId);
        
        // 创建SSE连接，设置超时时间为30分钟
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        try {
            // 添加到状态管理器
            stateManager.addSseConnection(clusterId, emitter);
            
            // 发送连接成功消息
            emitter.send(SseEmitter.event()
                .name("connection")
                .data(Map.of(
                    "type", "connected",
                    "clusterId", clusterId,
                    "message", "SSE连接建立成功",
                    "timestamp", java.time.LocalDateTime.now().toString()
                )));
            
        } catch (Exception e) {
            log.error("建立SSE连接失败: clusterId={}, error={}", clusterId, e.getMessage(), e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    


    /**
     * 获取实时日志
     */
    @GetMapping(value = "/logs/{clusterId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(
            @PathVariable Long clusterId,
            @RequestParam(required = false) String hostIp) {
        
        log.info("建立日志SSE连接: clusterId={}, hostIp={}", clusterId, hostIp);
        
        // 创建SSE连接，设置超时时间为30分钟
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        try {
            // 添加到状态管理器（日志流）
            stateManager.addLogConnection(clusterId, hostIp, emitter);
            
            // 发送连接成功消息
            emitter.send(SseEmitter.event()
                .name("log-connection")
                .data(Map.of(
                    "type", "log-connected",
                    "clusterId", clusterId,
                    "hostIp", hostIp != null ? hostIp : "all",
                    "message", "日志SSE连接建立成功",
                    "timestamp", java.time.LocalDateTime.now().toString()
                )));
            
        } catch (Exception e) {
            log.error("建立日志SSE连接失败: clusterId={}, hostIp={}, error={}", 
                    clusterId, hostIp, e.getMessage(), e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    

}
