package com.datasophon.api.controller.v1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 环境检查日志SSE控制器
 * 提供实时推送检查日志和修复日志
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/environment-logs-sse")
public class EnvironmentLogsSSEController {
    
    // 存储SSE连接：key为 "clusterId:hostIp:checkKey"
    private static final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    
    /**
     * 创建SSE连接，接收实时日志（检查日志 + 修复日志）
     * 日志通过type字段区分：check / repair
     */
    @GetMapping(value = "/stream/{clusterId}/{hostIp}/{checkKey}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(
            @PathVariable Long clusterId,
            @PathVariable String hostIp,
            @PathVariable String checkKey) {
        
        String key = buildKey(clusterId, hostIp, checkKey);
        log.info("创建日志SSE连接: key={}", key);
        
        // 创建SSE发射器（30分钟超时）
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 保存连接
        sseEmitters.put(key, emitter);
        
        // 连接完成时清理
        emitter.onCompletion(() -> {
            log.info("SSE连接完成: key={}", key);
            sseEmitters.remove(key);
        });
        
        // 连接超时时清理
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: key={}", key);
            sseEmitters.remove(key);
        });
        
        // 连接错误时清理
        emitter.onError((e) -> {
            log.error("SSE连接错误: key={}, error={}", key, e.getMessage());
            sseEmitters.remove(key);
        });
        
        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"SSE连接已建立\"}"));
        } catch (IOException e) {
            log.error("发送连接成功消息失败", e);
        }
        
        return emitter;
    }
    
    /**
     * 推送日志消息到SSE客户端
     */
    public static void pushLog(Long clusterId, String hostIp, String checkKey, String logJson) {
        String key = buildKey(clusterId, hostIp, checkKey);
        SseEmitter emitter = sseEmitters.get(key);
        
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("log")
                        .data(logJson));
            } catch (IOException e) {
                log.error("推送日志失败: key={}", key, e);
                sseEmitters.remove(key);
            }
        }
    }
    
    /**
     * 推送修复完成消息
     */
    public static void pushComplete(Long clusterId, String hostIp, String checkKey, boolean success, String message) {
        String key = buildKey(clusterId, hostIp, checkKey);
        SseEmitter emitter = sseEmitters.get(key);
        
        if (emitter != null) {
            try {
                String completeData = String.format(
                    "{\"type\":\"REPAIR_COMPLETE\",\"success\":%s,\"message\":\"%s\"}",
                    success, message
                );
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(completeData));
                
                // 完成后关闭连接
                emitter.complete();
                sseEmitters.remove(key);
            } catch (IOException e) {
                log.error("推送完成消息失败: key={}", key, e);
                sseEmitters.remove(key);
            }
        }
    }
    
    private static String buildKey(Long clusterId, String hostIp, String checkKey) {
        return clusterId + ":" + hostIp + ":" + checkKey;
    }
}

