package com.datasophon.api.controller.v1;

import com.datasophon.api.hostvalidation.manager.HostValidationStateManager;
import com.datasophon.api.hostvalidation.service.HostValidationService;
import com.datasophon.common.dto.HostValidationRequestDTO;
import com.datasophon.common.utils.Result;
import com.datasophon.common.vo.HostValidationStatusVO;
import com.datasophon.common.annotation.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
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
@ApiVersion(path = "host-validation/stream")
@RequiredArgsConstructor
@Tag(name = "主机校验实时数据", description = "主机校验实时状态和日志推送")
public class HostValidationSSEController {
    
    private final HostValidationService hostValidationService;
    private final HostValidationStateManager stateManager;
    
    /**
     * 建立SSE连接，接收实时状态更新
     */
    @GetMapping(value = "/status/{clusterId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立SSE连接", description = "建立Server-Sent Events连接，实时接收主机校验状态和日志")
    public SseEmitter stream(
            @Parameter(description = "集群ID") @PathVariable Long clusterId) {
        
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
    @Operation(summary = "获取实时日志", description = "建立SSE连接接收实时日志信息")
    public SseEmitter streamLogs(
            @Parameter(description = "集群ID") @PathVariable Long clusterId,
            @Parameter(description = "主机IP，为空则获取所有主机日志") @RequestParam(required = false) String hostIp) {
        
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
