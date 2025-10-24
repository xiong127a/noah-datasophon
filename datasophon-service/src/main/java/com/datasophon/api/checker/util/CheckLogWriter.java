package com.datasophon.api.checker.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 环境检查日志写入工具
 * 负责将检查和修复日志写入本地文件
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@Component
public class CheckLogWriter {
    
    // 使用相对路径，日志存储在项目运行目录的 logs/environment-check/ 下
    private static final String LOG_BASE_DIR = "logs/environment-check";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 写入检查日志
     */
    public void writeCheckLog(Long clusterId, String hostIp, String checkKey, String content) {
        String logPath = getOrCreateLogPath(clusterId, hostIp, checkKey, "check");
        appendLog(logPath, content);
    }
    
    /**
     * 写入修复日志
     */
    public void writeRepairLog(Long clusterId, String hostIp, String checkKey, String content) {
        String logPath = getOrCreateLogPath(clusterId, hostIp, checkKey, "repair");
        appendLog(logPath, content);
    }
    
    /**
     * 读取检查日志
     */
    public String readCheckLog(Long clusterId, String hostIp, String checkKey) {
        return readLatestLog(clusterId, hostIp, checkKey, "check");
    }
    
    /**
     * 读取修复日志
     */
    public String readRepairLog(Long clusterId, String hostIp, String checkKey) {
        return readLatestLog(clusterId, hostIp, checkKey, "repair");
    }
    
    /**
     * 获取或创建日志文件路径
     */
    private String getOrCreateLogPath(Long clusterId, String hostIp, String checkKey, String type) {
        try {
            // 创建目录结构
            Path dirPath = Paths.get(LOG_BASE_DIR, clusterId.toString(), hostIp);
            Files.createDirectories(dirPath);
            
            // 检查今天是否已有日志文件
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String filePattern = String.format("%s-%s-%s", checkKey, type, today);
            
            try (Stream<Path> files = Files.list(dirPath)) {
                var existingFile = files
                        .filter(p -> p.getFileName().toString().startsWith(filePattern))
                        .findFirst();
                
                if (existingFile.isPresent()) {
                    return existingFile.get().toString();
                }
            }
            
            // 创建新的日志文件
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String fileName = String.format("%s-%s-%s.log", checkKey, type, timestamp);
            Path logFile = dirPath.resolve(fileName);
            Files.createFile(logFile);
            
            return logFile.toString();
        } catch (IOException e) {
            log.error("创建日志文件失败: clusterId={}, hostIp={}, checkKey={}, type={}", 
                    clusterId, hostIp, checkKey, type, e);
            return null;
        }
    }
    
    /**
     * 追加日志内容
     */
    private void appendLog(String logPath, String content) {
        if (logPath == null) {
            return;
        }
        
        try {
            String timestamp = LocalDateTime.now().format(LOG_TIME_FORMATTER);
            String logEntry = String.format("[%s] %s\n", timestamp, content);
            
            Files.writeString(
                    Paths.get(logPath),
                    logEntry,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            log.error("写入日志失败: logPath={}", logPath, e);
        }
    }
    
    /**
     * 读取最新的日志文件
     */
    private String readLatestLog(Long clusterId, String hostIp, String checkKey, String type) {
        try {
            Path dirPath = Paths.get(LOG_BASE_DIR, clusterId.toString(), hostIp);
            
            if (!Files.exists(dirPath)) {
                return null;
            }
            
            String filePattern = String.format("%s-%s-", checkKey, type);
            
            try (Stream<Path> files = Files.list(dirPath)) {
                var latestFile = files
                        .filter(p -> p.getFileName().toString().startsWith(filePattern))
                        .max(Comparator.comparing(p -> p.getFileName().toString()));
                
                if (latestFile.isPresent()) {
                    return Files.readString(latestFile.get(), StandardCharsets.UTF_8);
                }
            }
            
            return null;
        } catch (IOException e) {
            log.error("读取日志失败: clusterId={}, hostIp={}, checkKey={}, type={}", 
                    clusterId, hostIp, checkKey, type, e);
            return null;
        }
    }
    
    /**
     * 清理过期日志（保留7天）
     */
    public void cleanupOldLogs() {
        try {
            Path basePath = Paths.get(LOG_BASE_DIR);
            
            if (!Files.exists(basePath)) {
                return;
            }
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            
            Files.walk(basePath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            var lastModified = Files.getLastModifiedTime(path);
                            var fileTime = LocalDateTime.ofInstant(
                                    lastModified.toInstant(),
                                    java.time.ZoneId.systemDefault()
                            );
                            return fileTime.isBefore(cutoffTime);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("删除过期日志文件: {}", path);
                        } catch (IOException e) {
                            log.error("删除日志文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("清理过期日志失败", e);
        }
    }
}

