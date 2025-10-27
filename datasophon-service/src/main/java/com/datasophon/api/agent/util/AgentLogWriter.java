package com.datasophon.api.agent.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent分发JSON结构化日志写入工具
 * 参考CheckLogWriter实现
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
@Component
public class AgentLogWriter {
    
    // 日志存储在项目运行目录的 logs/agent-distribution/ 下
    private static final String LOG_BASE_DIR = "logs/agent-distribution";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        DEBUG,   // 调试信息（灰色）
        INFO,    // 普通信息（蓝色）
        SUCCESS, // 成功（绿色）
        WARNING, // 警告（黄色）
        ERROR    // 错误（红色）
    }
    
    /**
     * 日志条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntry {
        private String timestamp;
        private String level;
        private String stage;
        private String message;
        private Map<String, Object> details;
    }
    
    /**
     * 核心日志方法
     */
    public void log(Long clusterId, String hostIp, LogLevel level, String stage, 
                   String message, Map<String, Object> details) {
        try {
            // 构建日志条目
            LogEntry entry = LogEntry.builder()
                    .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                    .level(level.name())
                    .stage(stage)
                    .message(message)
                    .details(details != null ? details : new HashMap<>())
                    .build();
            
            // 转换为JSON字符串
            String jsonLine = objectMapper.writeValueAsString(entry);
            
            // 写入文件
            Path logFile = getLogFilePath(clusterId, hostIp);
            Files.write(logFile, (jsonLine + "\n").getBytes(StandardCharsets.UTF_8), 
                       StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            // 推送到SSE客户端（如果有客户端连接）
            pushToSSE(clusterId, hostIp, jsonLine);
            
        } catch (Exception e) {
            log.error("写入Agent分发日志失败: clusterId={}, hostIp={}", clusterId, hostIp, e);
        }
    }
    
    /**
     * 推送日志到SSE客户端
     */
    private void pushToSSE(Long clusterId, String hostIp, String logJson) {
        try {
            // 使用反射调用SSE控制器的静态方法（避免循环依赖）
            Class<?> sseControllerClass = Class.forName(
                "com.datasophon.api.controller.v1.AgentDistributionSSEController"
            );
            var pushLogMethod = sseControllerClass.getMethod(
                "pushLog", Long.class, String.class, String.class
            );
            pushLogMethod.invoke(null, clusterId, hostIp, logJson);
        } catch (Exception e) {
            // SSE推送失败不影响日志记录
            log.debug("推送Agent分发日志到SSE失败（可能客户端未连接）: {}", e.getMessage());
        }
    }
    
    // ==================== 便捷日志方法 ====================
    
    public void logStart(Long clusterId, String hostIp, String message) {
        log(clusterId, hostIp, LogLevel.INFO, "start", message, null);
    }
    
    public void logCommand(Long clusterId, String hostIp, String stage, String command) {
        Map<String, Object> details = new HashMap<>();
        details.put("command", command);
        log(clusterId, hostIp, LogLevel.DEBUG, stage, "执行命令: " + command, details);
    }
    
    public void logOutput(Long clusterId, String hostIp, String stage, String output) {
        Map<String, Object> details = new HashMap<>();
        details.put("output", output);
        log(clusterId, hostIp, LogLevel.DEBUG, stage, "命令输出", details);
    }
    
    public void logSuccess(Long clusterId, String hostIp, String stage, String message, 
                          Map<String, Object> details) {
        log(clusterId, hostIp, LogLevel.SUCCESS, stage, message, details);
    }
    
    public void logError(Long clusterId, String hostIp, String stage, String message, 
                        Map<String, Object> details) {
        log(clusterId, hostIp, LogLevel.ERROR, stage, message, details);
    }
    
    public void logWarning(Long clusterId, String hostIp, String stage, String message, 
                          Map<String, Object> details) {
        log(clusterId, hostIp, LogLevel.WARNING, stage, message, details);
    }
    
    public void logInfo(Long clusterId, String hostIp, String stage, String message, 
                       Map<String, Object> details) {
        log(clusterId, hostIp, LogLevel.INFO, stage, message, details);
    }
    
    /**
     * 记录分发进度（特别用于文件下载、上传等耗时操作）
     * 前端会识别这种日志并显示进度条
     */
    public void logProgress(Long clusterId, String hostIp, String stage,
                           int progress, long uploadedSize, long totalSize, 
                           String message) {
        Map<String, Object> details = new HashMap<>();
        details.put("progress", progress);
        details.put("uploadedSize", uploadedSize);
        details.put("totalSize", totalSize);
        details.put("isProgress", true);  // 标记为进度日志
        log(clusterId, hostIp, LogLevel.INFO, stage, message, details);
    }
    
    // ==================== 读取日志方法 ====================
    
    /**
     * 读取Agent分发日志（返回JSON Lines格式字符串）
     */
    public String readLog(Long clusterId, String hostIp) {
        try {
            Path logFile = getLogFilePath(clusterId, hostIp);
            
            if (!Files.exists(logFile)) {
                return null;
            }
            
            // 读取所有行并返回为JSON Lines格式
            return Files.lines(logFile, StandardCharsets.UTF_8)
                    .collect(Collectors.joining("\n"));
            
        } catch (Exception e) {
            log.error("读取Agent分发日志失败: clusterId={}, hostIp={}", clusterId, hostIp, e);
            return null;
        }
    }
    
    /**
     * 获取日志文件路径
     */
    private Path getLogFilePath(Long clusterId, String hostIp) throws IOException {
        String fileName = "agent-distribution.jsonl";
        Path dir = Paths.get(LOG_BASE_DIR, String.valueOf(clusterId), hostIp);
        
        // 创建目录（如果不存在）
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        
        return dir.resolve(fileName);
    }
    
    /**
     * 清理指定主机的Agent分发日志
     */
    public void clearLog(Long clusterId, String hostIp) {
        try {
            Path logFile = getLogFilePath(clusterId, hostIp);
            Files.deleteIfExists(logFile);
            log.info("清理Agent分发日志: clusterId={}, hostIp={}", clusterId, hostIp);
        } catch (Exception e) {
            log.error("清理Agent分发日志失败: clusterId={}, hostIp={}", clusterId, hostIp, e);
        }
    }
    
    /**
     * 清理集群所有Agent分发日志
     */
    public void clearClusterLogs(Long clusterId) {
        try {
            Path clusterDir = Paths.get(LOG_BASE_DIR, String.valueOf(clusterId));
            
            if (Files.exists(clusterDir)) {
                Files.walk(clusterDir)
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                Files.delete(file);
                            } catch (IOException e) {
                                log.warn("删除Agent分发日志文件失败: {}", file, e);
                            }
                        });
            }
            
        } catch (Exception e) {
            log.error("清理集群Agent分发日志失败: clusterId={}", clusterId, e);
        }
    }
}

