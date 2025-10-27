package com.datasophon.api.controller.v1;

import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.api.event.RepairCompleteEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
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
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
    @Autowired
    private ObjectMapper objectMapper;
    
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
            
            // 异步推送历史日志
            new Thread(() -> pushHistoricalLogs(emitter, clusterId, hostIp, checkKey)).start();
            
        } catch (IOException e) {
            log.error("发送连接成功消息失败", e);
        }
        
        return emitter;
    }
    
    /**
     * 推送历史日志
     */
    private void pushHistoricalLogs(SseEmitter emitter, Long clusterId, String hostIp, String checkKey) {
        try {
            log.info("开始推送历史日志: clusterId={}, hostIp={}, checkKey={}", clusterId, hostIp, checkKey);
            
            // 读取检查日志
            String checkLog = checkLogWriter.readCheckLog(clusterId, hostIp, checkKey);
            if (checkLog != null && !checkLog.isEmpty() && !checkLog.equals("[]")) {
                try {
                    // 使用ObjectMapper解析JSON数组
                    List<Map<String, Object>> checkLogEntries = objectMapper.readValue(
                        checkLog, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                    );
                    
                    // 逐条发送日志
                    for (Map<String, Object> logEntry : checkLogEntries) {
                        try {
                            String logJson = objectMapper.writeValueAsString(logEntry);
                            emitter.send(SseEmitter.event()
                                    .name("log")
                                    .data(logJson));
                        } catch (IOException e) {
                            log.error("推送检查日志条目失败", e);
                            return;
                        }
                    }
                    log.info("成功推送 {} 条检查日志", checkLogEntries.size());
                } catch (Exception e) {
                    log.error("解析检查日志JSON失败: {}", checkLog, e);
                }
            }
            
            // 读取修复日志
            String repairLog = checkLogWriter.readRepairLog(clusterId, hostIp, checkKey);
            if (repairLog != null && !repairLog.isEmpty() && !repairLog.equals("[]")) {
                try {
                    // 使用ObjectMapper解析JSON数组
                    List<Map<String, Object>> repairLogEntries = objectMapper.readValue(
                        repairLog,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                    );
                    
                    // 逐条发送日志
                    for (Map<String, Object> logEntry : repairLogEntries) {
                        try {
                            String logJson = objectMapper.writeValueAsString(logEntry);
                            emitter.send(SseEmitter.event()
                                    .name("log")
                                    .data(logJson));
                        } catch (IOException e) {
                            log.error("推送修复日志条目失败", e);
                            return;
                        }
                    }
                    log.info("成功推送 {} 条修复日志", repairLogEntries.size());
                } catch (Exception e) {
                    log.error("解析修复日志JSON失败: {}", repairLog, e);
                }
            }
            
            // 发送历史日志加载完成事件
            emitter.send(SseEmitter.event()
                    .name("history-loaded")
                    .data("{\"message\":\"历史日志加载完成\"}"));
            
            log.info("历史日志推送完成: clusterId={}, hostIp={}, checkKey={}", clusterId, hostIp, checkKey);
            
        } catch (Exception e) {
            log.error("推送历史日志异常: clusterId={}, hostIp={}, checkKey={}", clusterId, hostIp, checkKey, e);
        }
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
     * 监听修复完成事件并推送到SSE
     */
    @EventListener
    public void onRepairComplete(RepairCompleteEvent event) {
        log.info("收到修复完成事件: clusterId={}, hostIp={}, checkKey={}, success={}", 
                event.getClusterId(), event.getHostIp(), event.getCheckKey(), event.isSuccess());
        pushComplete(event.getClusterId(), event.getHostIp(), event.getCheckKey(), 
                event.isSuccess(), event.getMessage());
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
    
    /**
     * 创建主机级SSE连接，接收该主机所有检查项的日志
     */
    @GetMapping(value = "/host/{clusterId}/{hostIp}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamHostLogs(
            @PathVariable Long clusterId,
            @PathVariable String hostIp) {
        
        String key = "host:" + clusterId + ":" + hostIp;
        log.info("创建主机日志SSE连接: key={}", key);
        
        // 创建SSE发射器（30分钟超时）
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 暂时不保存到 sseEmitters（因为key格式不同）
        // 主机级SSE主要用于历史日志查看
        
        // 连接完成时清理
        emitter.onCompletion(() -> log.info("主机SSE连接完成: key={}", key));
        emitter.onTimeout(() -> log.warn("主机SSE连接超时: key={}", key));
        emitter.onError((e) -> log.error("主机SSE连接错误: key={}, error={}", key, e.getMessage()));
        
        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"SSE连接已建立\"}"));
            
            // 异步推送主机所有历史日志
            new Thread(() -> pushHostHistoricalLogs(emitter, clusterId, hostIp)).start();
            
        } catch (IOException e) {
            log.error("发送连接成功消息失败", e);
        }
        
        return emitter;
    }
    
    /**
     * 推送主机所有历史日志（所有检查项）
     */
    private void pushHostHistoricalLogs(SseEmitter emitter, Long clusterId, String hostIp) {
        try {
            log.info("开始推送主机历史日志: clusterId={}, hostIp={}", clusterId, hostIp);
            
            // 获取主机的所有日志文件
            List<Map<String, String>> logFiles = checkLogWriter.listHostLogFiles(clusterId, hostIp);
            
            // 逐个文件读取并推送
            for (Map<String, String> fileInfo : logFiles) {
                String checkKey = fileInfo.get("checkKey");
                String type = fileInfo.get("type");
                
                // 根据类型读取日志
                String logContent = null;
                if ("check".equals(type)) {
                    logContent = checkLogWriter.readCheckLog(clusterId, hostIp, checkKey);
                } else if ("repair".equals(type)) {
                    logContent = checkLogWriter.readRepairLog(clusterId, hostIp, checkKey);
                }
                
                if (logContent != null && !logContent.isEmpty() && !logContent.equals("[]")) {
                    try {
                        // 使用ObjectMapper解析JSON数组
                        List<Map<String, Object>> logEntries = objectMapper.readValue(
                            logContent,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                        );
                        
                        // 为每条日志添加checkKey和checkName字段（从检查器注册表获取）
                        for (Map<String, Object> logEntry : logEntries) {
                            // 添加checkKey（如果没有）
                            if (!logEntry.containsKey("checkKey")) {
                                logEntry.put("checkKey", checkKey);
                            }
                            // 添加checkName（如果没有，使用checkKey作为fallback）
                            if (!logEntry.containsKey("checkName")) {
                                logEntry.put("checkName", getCheckName(checkKey));
                            }
                            
                            try {
                                String logJson = objectMapper.writeValueAsString(logEntry);
                                emitter.send(SseEmitter.event()
                                        .name("log")
                                        .data(logJson));
                            } catch (IOException e) {
                                log.error("推送日志条目失败", e);
                                return;
                            }
                        }
                        log.info("成功推送 {} 条 {} 日志，checkKey={}", logEntries.size(), type, checkKey);
                    } catch (Exception e) {
                        log.error("解析日志JSON失败: checkKey={}, type={}", checkKey, type, e);
                    }
                }
            }
            
            // 发送历史日志加载完成事件
            emitter.send(SseEmitter.event()
                    .name("history-loaded")
                    .data("{\"message\":\"历史日志加载完成\"}"));
            
            log.info("主机历史日志推送完成: clusterId={}, hostIp={}", clusterId, hostIp);
            
        } catch (Exception e) {
            log.error("推送主机历史日志异常: clusterId={}, hostIp={}", clusterId, hostIp, e);
        }
    }
    
    /**
     * 根据checkKey获取显示名称
     */
    private String getCheckName(String checkKey) {
        // 简单的映射，可以从配置或注册表获取
        Map<String, String> nameMap = Map.ofEntries(
            Map.entry("cpu", "CPU核心数检查"),
            Map.entry("memory", "内存检查"),
            Map.entry("java", "JDK环境检查"),
            Map.entry("disk", "磁盘空间检查"),
            Map.entry("firewall", "防火墙检查"),
            Map.entry("selinux", "SELinux检查"),
            Map.entry("file-handle", "文件句柄检查"),
            Map.entry("ssh-passwordless", "SSH免密登录检查"),
            Map.entry("time-sync", "时间同步检查")
        );
        return nameMap.getOrDefault(checkKey, checkKey);
    }
    
    private static String buildKey(Long clusterId, String hostIp, String checkKey) {
        return clusterId + ":" + hostIp + ":" + checkKey;
    }
}

