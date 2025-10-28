package com.datasophon.api.controller.v1;

import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.service.impl.HostManagementServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 主机管理SSE控制器
 * 提供主机名批量修改和hosts文件同步的实时进度推送
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sse/host-management")
public class HostManagementSSEController {
    
    /**
     * 创建SSE连接，接收主机操作的实时进度
     * 
     * @param taskId 任务ID
     * @return SSE Emitter
     */
    @GetMapping(value = "/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(@PathVariable String taskId) {
        
        log.info("创建主机管理SSE连接: taskId={}", taskId);
        
        // 创建SSE发射器（30分钟超时）
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 注册到服务中
        HostManagementServiceImpl.registerSseEmitter(taskId, emitter);
        
        // 连接完成时清理
        emitter.onCompletion(() -> {
            log.info("SSE连接完成: taskId={}", taskId);
            HostManagementServiceImpl.unregisterSseEmitter(taskId);
        });
        
        // 连接超时时清理
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: taskId={}", taskId);
            HostManagementServiceImpl.unregisterSseEmitter(taskId);
        });
        
        // 连接错误时清理
        emitter.onError((e) -> {
            log.error("SSE连接错误: taskId={}, error={}", taskId, e.getMessage());
            HostManagementServiceImpl.unregisterSseEmitter(taskId);
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
}

