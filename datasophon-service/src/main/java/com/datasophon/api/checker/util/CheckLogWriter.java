package com.datasophon.api.checker.util;

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
 * 环境检查JSON结构化日志写入工具
 * 
 * @author 任相鹏
 * @date 2025-01-24
 */
@Slf4j
@Component
public class CheckLogWriter {
    
    // 使用相对路径，日志存储在项目运行目录的 logs/environment-check/ 下
    private static final String LOG_BASE_DIR = "logs/environment-check";
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
     * 日志类型枚举
     */
    public enum LogType {
        CHECK,  // 检查日志
        REPAIR  // 修复日志
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
        private String type;
        private String stage;
        private String message;
        private Map<String, Object> details;
    }
    
    /**
     * 核心日志方法
     */
    public void log(Long clusterId, String hostIp, String checkKey, LogType type, 
                   LogLevel level, String stage, String message, Map<String, Object> details) {
        try {
            // 构建日志条目
            LogEntry entry = LogEntry.builder()
                    .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                    .level(level.name())
                    .type(type.name().toLowerCase())
                    .stage(stage)
                    .message(message)
                    .details(details != null ? details : new HashMap<>())
                    .build();
            
            // 转换为JSON字符串
            String jsonLine = objectMapper.writeValueAsString(entry);
            
            // 写入文件
            Path logFile = getLogFilePath(clusterId, hostIp, checkKey, type);
            Files.write(logFile, (jsonLine + "\n").getBytes(StandardCharsets.UTF_8), 
                       StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            // 推送到SSE客户端（如果有客户端连接）
            pushToSSE(clusterId, hostIp, checkKey, jsonLine);
            
        } catch (Exception e) {
            log.error("写入JSON日志失败: clusterId={}, hostIp={}, checkKey={}", 
                     clusterId, hostIp, checkKey, e);
        }
    }
    
    /**
     * 推送日志到SSE客户端（检查日志和修复日志都推送）
     */
    private void pushToSSE(Long clusterId, String hostIp, String checkKey, String logJson) {
        try {
            // 使用反射调用SSE控制器的静态方法（避免循环依赖）
            Class<?> sseControllerClass = Class.forName(
                "com.datasophon.api.controller.v1.EnvironmentLogsSSEController"
            );
            var pushLogMethod = sseControllerClass.getMethod(
                "pushLog", Long.class, String.class, String.class, String.class
            );
            pushLogMethod.invoke(null, clusterId, hostIp, checkKey, logJson);
        } catch (Exception e) {
            // SSE推送失败不影响日志记录
            log.debug("推送日志到SSE失败（可能客户端未连接）: {}", e.getMessage());
        }
    }
    
    // ==================== 检查日志便捷方法 ====================
    
    public void logCheckStart(Long clusterId, String hostIp, String checkKey, String message) {
        log(clusterId, hostIp, checkKey, LogType.CHECK, LogLevel.INFO, "start", message, null);
    }
    
    public void logCheckCommand(Long clusterId, String hostIp, String checkKey, String command) {
        Map<String, Object> details = new HashMap<>();
        details.put("command", command);
        log(clusterId, hostIp, checkKey, LogType.CHECK, LogLevel.DEBUG, "executing", 
            "执行命令: " + command, details);
    }
    
    public void logCheckOutput(Long clusterId, String hostIp, String checkKey, String output) {
        Map<String, Object> details = new HashMap<>();
        details.put("output", output);
        log(clusterId, hostIp, checkKey, LogType.CHECK, LogLevel.DEBUG, "output", 
            "命令输出", details);
    }
    
    public void logCheckSuccess(Long clusterId, String hostIp, String checkKey, String message, 
                               Map<String, Object> details) {
        log(clusterId, hostIp, checkKey, LogType.CHECK, LogLevel.SUCCESS, "result", message, details);
    }
    
    public void logCheckError(Long clusterId, String hostIp, String checkKey, String message, 
                             Map<String, Object> details) {
        log(clusterId, hostIp, checkKey, LogType.CHECK, LogLevel.ERROR, "error", message, details);
    }
    
    public void logCheckWarning(Long clusterId, String hostIp, String checkKey, String message, 
                               Map<String, Object> details) {
        log(clusterId, hostIp, checkKey, LogType.CHECK, LogLevel.WARNING, "result", message, details);
    }
    
    public void logCheckInfo(Long clusterId, String hostIp, String checkKey, String message, 
                            Map<String, Object> details) {
        log(clusterId, hostIp, checkKey, LogType.CHECK, LogLevel.INFO, "executing", message, details);
    }
    
    // ==================== 修复日志便捷方法 ====================
    
    public void logRepairStart(Long clusterId, String hostIp, String checkKey, String message) {
        log(clusterId, hostIp, checkKey, LogType.REPAIR, LogLevel.INFO, "start", message, null);
    }
    
    public void logRepairCommand(Long clusterId, String hostIp, String checkKey, String command) {
        Map<String, Object> details = new HashMap<>();
        details.put("command", command);
        log(clusterId, hostIp, checkKey, LogType.REPAIR, LogLevel.DEBUG, "executing", 
            "执行修复脚本", details);
    }
    
    public void logRepairOutput(Long clusterId, String hostIp, String checkKey, String output) {
        Map<String, Object> details = new HashMap<>();
        details.put("output", output);
        log(clusterId, hostIp, checkKey, LogType.REPAIR, LogLevel.DEBUG, "output", 
            "脚本输出", details);
    }
    
    public void logRepairSuccess(Long clusterId, String hostIp, String checkKey, String message, 
                                Map<String, Object> details) {
        log(clusterId, hostIp, checkKey, LogType.REPAIR, LogLevel.SUCCESS, "result", message, details);
    }
    
    public void logRepairError(Long clusterId, String hostIp, String checkKey, String message, 
                              Map<String, Object> details) {
        log(clusterId, hostIp, checkKey, LogType.REPAIR, LogLevel.ERROR, "error", message, details);
    }
    
    public void logRepairInfo(Long clusterId, String hostIp, String checkKey, String message, 
                             Map<String, Object> details) {
        log(clusterId, hostIp, checkKey, LogType.REPAIR, LogLevel.INFO, "executing", message, details);
    }
    
    /**
     * 记录修复进度（特别用于文件上传等耗时操作）
     * 前端会识别这种日志并显示进度条
     */
    public void logRepairProgress(Long clusterId, String hostIp, String checkKey, 
                                  int progress, String message, Map<String, Object> details) {
        Map<String, Object> progressDetails = details != null ? new HashMap<>(details) : new HashMap<>();
        progressDetails.put("progress", progress);
        progressDetails.put("isProgress", true);  // 标记为进度日志
        log(clusterId, hostIp, checkKey, LogType.REPAIR, LogLevel.INFO, "progress", message, progressDetails);
    }
    
    // ==================== 读取日志方法 ====================
    
    /**
     * 读取检查日志（返回JSON Lines格式字符串）
     */
    public String readCheckLog(Long clusterId, String hostIp, String checkKey) {
        return readLog(clusterId, hostIp, checkKey, LogType.CHECK);
    }
    
    /**
     * 读取修复日志（返回JSON Lines格式字符串）
     */
    public String readRepairLog(Long clusterId, String hostIp, String checkKey) {
        return readLog(clusterId, hostIp, checkKey, LogType.REPAIR);
    }
    
    private String readLog(Long clusterId, String hostIp, String checkKey, LogType type) {
        try {
            Path logFile = getLogFilePath(clusterId, hostIp, checkKey, type);
            
            if (!Files.exists(logFile)) {
                return null;
            }
            
            // 读取所有行并返回为JSON Lines格式
            return Files.lines(logFile, StandardCharsets.UTF_8)
                    .collect(Collectors.joining("\n"));
            
        } catch (Exception e) {
            log.error("读取日志失败: clusterId={}, hostIp={}, checkKey={}, type={}", 
                     clusterId, hostIp, checkKey, type, e);
            return null;
        }
    }
    
    /**
     * 获取日志文件路径
     */
    private Path getLogFilePath(Long clusterId, String hostIp, String checkKey, LogType type) throws IOException {
        String fileName = String.format("%s.%s.jsonl", checkKey, type.name().toLowerCase());
        Path dir = Paths.get(LOG_BASE_DIR, String.valueOf(clusterId), hostIp);
        
        // 创建目录（如果不存在）
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        
        Path logFile = dir.resolve(fileName);
        
        // 如果是新的检查/修复周期，删除旧日志文件（覆盖策略）
        // 注意：这里通过检查文件是否存在且stage为start来判断是否是新周期
        // 实际使用时，每次startCheck应该清理旧日志
        
        return logFile;
    }
    
    /**
     * 清理指定检查项的日志（用于新的检查周期开始时）
     */
    public void clearLogs(Long clusterId, String hostIp, String checkKey) {
        try {
            Path checkLog = getLogFilePath(clusterId, hostIp, checkKey, LogType.CHECK);
            Path repairLog = getLogFilePath(clusterId, hostIp, checkKey, LogType.REPAIR);
            
            Files.deleteIfExists(checkLog);
            Files.deleteIfExists(repairLog);
            
        } catch (Exception e) {
            log.error("清理日志失败: clusterId={}, hostIp={}, checkKey={}", 
                     clusterId, hostIp, checkKey, e);
        }
    }
    
    /**
     * 清理主机所有日志
     */
    public void clearHostLogs(Long clusterId, String hostIp) {
        try {
            Path hostDir = Paths.get(LOG_BASE_DIR, String.valueOf(clusterId), hostIp);
            
            if (Files.exists(hostDir)) {
                Files.walk(hostDir)
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                Files.delete(file);
                            } catch (IOException e) {
                                log.warn("删除日志文件失败: {}", file, e);
                            }
                        });
            }
            
        } catch (Exception e) {
            log.error("清理主机日志失败: clusterId={}, hostIp={}", clusterId, hostIp, e);
        }
    }
}
