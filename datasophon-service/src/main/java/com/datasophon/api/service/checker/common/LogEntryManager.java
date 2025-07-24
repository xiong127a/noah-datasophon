package com.datasophon.api.service.checker.common;

import com.datasophon.common.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 日志条目管理器
 * 用于存储和管理各种操作的日志条目
 */
public class LogEntryManager {
    private static final Logger logger = LoggerFactory.getLogger(LogEntryManager.class);
    
    // 使用ConcurrentHashMap存储日志，确保线程安全
    private static final Map<String, List<LogEntry>> logMap = new ConcurrentHashMap<>();
    
    // 日志条目最大数量（每个日志键）
    private static final int MAX_LOG_ENTRIES = 1000;
    
    /**
     * 添加日志条目
     * @param logKey 日志键
     * @param logEntry 日志条目
     */
    public static void addLogEntry(String logKey, LogEntry logEntry) {
        if (logKey == null || logEntry == null) {
            logger.warn("尝试添加无效的日志条目，logKey: {}, logEntry: {}", logKey, logEntry);
            return;
        }
        
        try {
            // 获取或创建日志列表
            List<LogEntry> logEntries = logMap.computeIfAbsent(logKey, k -> 
                Collections.synchronizedList(new ArrayList<>()));
            
            // 添加日志条目
            synchronized (logEntries) {
                // 如果超过最大数量，移除最旧的条目
                if (logEntries.size() >= MAX_LOG_ENTRIES) {
                    logEntries.removeFirst();
                }
                logEntries.add(logEntry);
            }
        } catch (Exception e) {
            logger.error("添加日志条目异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取日志条目列表
     * @param logKey 日志键
     * @return 日志条目列表的副本
     */
    public static List<LogEntry> getLogEntries(String logKey) {
        if (logKey == null) {
            return Collections.emptyList();
        }
        
        List<LogEntry> entries = logMap.get(logKey);
        if (entries == null) {
            return Collections.emptyList();
        }
        
        // 返回副本，避免并发修改
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }
    
    /**
     * 获取按条件筛选的日志条目
     * @param logKey 日志键
     * @param type 日志类型 (可以为null表示全部类型)
     * @param level 日志级别 (可以为null表示全部级别)
     * @param filterMode 过滤模式 ("exact", "min", "all")
     * @return 过滤后的日志条目列表
     */
    public static List<LogEntry> getFilteredLogEntries(String logKey, LogEntry.Type type, 
                                                      LogEntry.Level level, String filterMode) {
        List<LogEntry> entries = getLogEntries(logKey);
        if (entries.isEmpty()) {
            return entries;
        }
        
        // 按类型过滤
        List<LogEntry> filteredEntries = entries;
        if (type != null) {
            filteredEntries = filteredEntries.stream()
                .filter(entry -> entry.getType() == type)
                .collect(Collectors.toList());
        }
        
        // 按级别过滤
        if (level != null && filterMode != null) {
            if ("exact".equals(filterMode)) {
                // 精确匹配级别
                filteredEntries = filteredEntries.stream()
                    .filter(entry -> entry.getLevel() == level)
                    .collect(Collectors.toList());
            } else if ("min".equals(filterMode)) {
                // 最小级别（当前级别及更高级别）
                filteredEntries = filteredEntries.stream()
                    .filter(entry -> entry.getLevel().isHigherOrEqual(level))
                    .collect(Collectors.toList());
            }
            // "all"模式不需要过滤
        }
        
        return filteredEntries;
    }
    
    /**
     * 清除日志
     * @param logKey 日志键
     */
    public static void clearLog(String logKey) {
        if (logKey != null) {
            logMap.remove(logKey);
        }
    }
    
    /**
     * 清除日志条目
     * 别名方法，与clearLog功能相同
     * @param logKey 日志键
     */
    public static void clearLogEntries(String logKey) {
        clearLog(logKey);
    }
    
    /**
     * 获取日志内容
     * @param logKey 日志键
     * @return 格式化的日志内容字符串
     */
    public static String getLogContent(String logKey) {
        List<LogEntry> entries = getLogEntries(logKey);
        if (entries.isEmpty()) {
            return "";
        }
        
        return entries.stream()
            .map(LogEntry::toString)
            .collect(Collectors.joining("\n"));
    }
    
    /**
     * 清理过期的日志（可由定时任务调用）
     * @param expirationTimeMs 过期时间（毫秒）
     */
    public static void cleanupExpiredLogs(long expirationTimeMs) {
        long now = System.currentTimeMillis();
        
        // 记录清理前日志数量
        int totalLogsBefore = 0;
        for (List<LogEntry> entries : logMap.values()) {
            totalLogsBefore += entries.size();
        }
        
        // 清理过期日志
        int removedEntries = 0;
        int removedKeys = 0;
        
        for (Map.Entry<String, List<LogEntry>> entry : logMap.entrySet()) {
            String key = entry.getKey();
            List<LogEntry> logs = entry.getValue();
            
            if (logs.isEmpty()) {
                logMap.remove(key);
                removedKeys++;
                continue;
            }
            
            boolean shouldRemoveKey = false;
            
            synchronized (logs) {
                // 检查最新日志条目是否超时
                LogEntry lastEntry = logs.getLast();
                if (lastEntry.getTimestamp().getTime() + expirationTimeMs < now) {
                    shouldRemoveKey = true;
                    removedEntries += logs.size();
                }
            }
            
            if (shouldRemoveKey) {
                logMap.remove(key);
                removedKeys++;
            }
        }
        
        // 记录清理结果
        if (removedKeys > 0 || removedEntries > 0) {
            logger.info("日志清理完成: 移除 {} 个日志键, {} 条日志条目", removedKeys, removedEntries);
        }
    }
} 