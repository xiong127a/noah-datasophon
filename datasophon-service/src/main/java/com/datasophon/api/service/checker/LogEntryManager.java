package com.datasophon.api.service.checker;

import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.LogEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

/**
 * 日志管理器，用于存储和检索结构化日志
 */
public class LogEntryManager {
    private static final String LOG_ENTRIES_PREFIX = "LOG_ENTRIES_";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 添加日志记录
     * @param logKey 日志键
     * @param logEntry 日志记录
     */
    public static void addLogEntry(String logKey, LogEntry logEntry) {
        String cacheKey = LOG_ENTRIES_PREFIX + logKey;
        List<LogEntry> entries = getLogEntries(logKey);
        if (entries == null) {
            entries = new ArrayList<>();
        }
        entries.add(logEntry);
        
        try {
            // 将日志列表序列化为JSON字符串
            String json = objectMapper.writeValueAsString(entries);
            CacheUtils.put(cacheKey, json);
        } catch (Exception e) {
            // 序列化失败，使用传统方式记录日志
            String logContent = (String) CacheUtils.get(logKey);
            logContent = (logContent == null ? "" : logContent);
            logContent += logEntry.toString() + "\n";
            CacheUtils.put(logKey, logContent);
        }
        
        // 同时保持传统格式的日志，确保向后兼容
        String logContent = (String) CacheUtils.get(logKey);
        logContent = (logContent == null ? "" : logContent);
        logContent += logEntry.toString() + "\n";
        CacheUtils.put(logKey, logContent);
    }
    
    /**
     * 获取所有日志记录
     * @param logKey 日志键
     * @return 日志记录列表
     */
    public static List<LogEntry> getLogEntries(String logKey) {
        String cacheKey = LOG_ENTRIES_PREFIX + logKey;
        String json = (String) CacheUtils.get(cacheKey);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<LogEntry>>() {});
        } catch (Exception e) {
            // 反序列化失败，返回空列表
            return new ArrayList<>();
        }
    }
    
    /**
     * 按日志级别筛选日志
     * @param logKey 日志键
     * @param level 日志级别
     * @return 筛选后的日志记录列表
     */
    public static List<LogEntry> filterByExactLevel(String logKey, LogEntry.Level level) {
        List<LogEntry> allEntries = getLogEntries(logKey);
        return allEntries.stream()
                .filter(entry -> entry.getLevel() == level)
                .collect(Collectors.toList());
    }
    
    /**
     * 显示指定日志级别及更高级别的日志
     * @param logKey 日志键
     * @param level 日志级别
     * @return 筛选后的日志记录列表
     */
    public static List<LogEntry> filterByMinLevel(String logKey, LogEntry.Level level) {
        List<LogEntry> allEntries = getLogEntries(logKey);
        return allEntries.stream()
                .filter(entry -> entry.getLevel().isHigherOrEqual(level))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取日志内容的字符串形式
     * @param logKey 日志键
     * @return 日志内容字符串
     */
    public static String getLogContent(String logKey) {
        List<LogEntry> entries = getLogEntries(logKey);
        if (entries == null || entries.isEmpty()) {
            // 尝试从传统缓存获取日志内容
            String oldLogContent = (String) CacheUtils.get(logKey);
            return oldLogContent != null ? oldLogContent : "";
        }
        
        // 计算最长的类名和线程名
        int maxClassNameLength = 0;
        int maxThreadNameLength = 0;
        for (LogEntry entry : entries) {
            if (entry.getClassName() != null && entry.getClassName().length() > maxClassNameLength) {
                maxClassNameLength = entry.getClassName().length();
            }
            if (entry.getThreadName() != null && entry.getThreadName().length() > maxThreadNameLength) {
                maxThreadNameLength = entry.getThreadName().length();
            }
        }
        
        // 根据实际最大长度生成格式化字符串
        maxClassNameLength = Math.min(maxClassNameLength, 30); // 限制最大宽度为30
        maxThreadNameLength = Math.min(maxThreadNameLength, 20); // 限制最大宽度为20
        
        StringBuilder sb = new StringBuilder();
        for (LogEntry entry : entries) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String formattedLog = String.format("%s [%-5s] [%-" + maxThreadNameLength + "s] %-" + maxClassNameLength + "s - %s",
                    sdf.format(entry.getTimestamp()),
                    entry.getLevel(),
                    entry.getThreadName(),
                    entry.getClassName(),
                    entry.getMessage());
            sb.append(formattedLog).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 清理日志内容，移除可能混入的HTML或UI元素
     * @param content 原始日志内容
     * @return 清理后的日志内容
     */
    private static String cleanupLogContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        // 分行处理
        String[] lines = content.split("\n");
        StringBuilder cleanContent = new StringBuilder();
        
        for (String line : lines) {
            // 跳过HTML标签和UI元素描述行
            if (line.contains("<") && line.contains(">")) {
                continue;
            }
            
            // 保留有效的日志行和堆栈跟踪行
            boolean isLogLine = line.matches("^\\d{4}-\\d{2}-\\d{2}.*") || // 时间戳开头
                              line.startsWith("[") || 
                              line.contains("===") ||
                              line.trim().startsWith("at ") || // 堆栈行
                              line.contains("Exception:") ||
                              line.contains("Exception") ||
                              line.contains("Error:") ||
                              line.contains("Caused by:");
            
            if (!line.trim().isEmpty() && isLogLine) {
                if (cleanContent.length() > 0) {
                    cleanContent.append("\n");
                }
                cleanContent.append(line);
            }
        }
        
        return cleanContent.toString();
    }
    
    /**
     * 获取筛选后的日志内容字符串
     * @param logKey 日志键
     * @param level 日志级别
     * @param exactMatch 是否精确匹配级别
     * @return 筛选后的日志内容字符串
     */
    public static String getFilteredLogContent(String logKey, LogEntry.Level level, boolean exactMatch) {
        // 优先使用增强的原始文本过滤方法处理日志，确保堆栈信息能够被包含
        try {
            // 优先使用改进的文本筛选方法，保留堆栈信息
            String rawFilteredContent = filterRawLogContent(logKey, level, exactMatch);
            if (rawFilteredContent != null && !rawFilteredContent.trim().isEmpty()) {
                return rawFilteredContent;
            }
        } catch (Exception e) {
            // 如果处理过程中出现异常，尝试使用结构化日志处理
        }
        
        // 如果原始文本过滤没有结果，尝试使用结构化日志筛选
        List<LogEntry> entries;
        try {
            // 尝试使用结构化日志筛选
            if (exactMatch) {
                entries = filterByExactLevel(logKey, level);
            } else {
                entries = filterByMinLevel(logKey, level);
            }
            
            // 检查结果是否为空
            if (entries == null || entries.isEmpty()) {
                return "";
            }
            
            // 计算最长的类名和线程名
            int maxClassNameLength = 0;
            int maxThreadNameLength = 0;
            for (LogEntry entry : entries) {
                if (entry.getClassName() != null && entry.getClassName().length() > maxClassNameLength) {
                    maxClassNameLength = entry.getClassName().length();
                }
                if (entry.getThreadName() != null && entry.getThreadName().length() > maxThreadNameLength) {
                    maxThreadNameLength = entry.getThreadName().length();
                }
            }
            
            // 根据实际最大长度生成格式化字符串
            maxClassNameLength = Math.min(maxClassNameLength, 30); // 限制最大宽度为30
            maxThreadNameLength = Math.min(maxThreadNameLength, 20); // 限制最大宽度为20
            
            StringBuilder sb = new StringBuilder();
            for (LogEntry entry : entries) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                String formattedLog = String.format("%s [%-5s] [%-" + maxThreadNameLength + "s] %-" + maxClassNameLength + "s - %s",
                        sdf.format(entry.getTimestamp()),
                        entry.getLevel(),
                        entry.getThreadName(),
                        entry.getClassName(),
                        entry.getMessage());
                sb.append(formattedLog).append("\n");
            }
            String logContent = sb.toString();
            
            // 确保返回干净的日志内容
            return cleanupLogContent(logContent);
        } catch (Exception e) {
            // 如果所有处理都失败，返回空字符串
            return "";
        }
    }
    
    /**
     * 从原始日志文本中筛选内容
     * @param logKey 日志键
     * @param level 日志级别
     * @param exactMatch 是否精确匹配级别
     * @return 筛选后的日志内容
     */
    private static String filterRawLogContent(String logKey, LogEntry.Level level, boolean exactMatch) {
        String logContent = (String) CacheUtils.get(logKey);
        if (logContent == null || logContent.isEmpty()) {
            // 尝试从结构化日志生成的内容中获取
            logContent = getLogContent(logKey);
            if (logContent == null || logContent.isEmpty()) {
                return "";
            }
        }
        
        // 分行处理日志文本
        String[] lines = logContent.split("\n");
        StringBuilder result = new StringBuilder();
        
        boolean inStackTrace = false; // 标记是否在堆栈跟踪中
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // 尝试解析日志级别
            boolean shouldInclude = false;
            boolean isStackTraceLine = line.trim().startsWith("at ") || 
                                      line.contains("Exception") || 
                                      line.contains("Error") ||
                                      (line.trim().startsWith("Caused by:")) ||
                                      (!line.contains("[") && line.contains("堆栈")) || // "堆栈"相关内容
                                      (line.contains(": ") && !line.contains("[") && !line.matches("^\\d{4}-\\d{2}-\\d{2}.*"));
            
            // 如果当前在堆栈跟踪中，保留堆栈行
            if (inStackTrace && isStackTraceLine) {
                shouldInclude = true;
            } else if (inStackTrace && line.trim().isEmpty()) {
                // 空行可能是堆栈之间的分隔，保留它
                shouldInclude = true;
            } else {
                // 不是堆栈行或者不在堆栈跟踪中
                inStackTrace = false; // 重置堆栈跟踪标记
                
                // 检查行中是否包含日志级别标记 [INFO], [WARN], [ERROR], [DEBUG]
                String levelStr = level.toString();
                if (exactMatch) {
                    // 精确匹配：只包含指定级别
                    shouldInclude = line.contains("[" + levelStr + " ]") || 
                                    line.contains("[" + levelStr + "]");
                } else {
                    // 非精确匹配：包含指定级别及以上级别
                    if (level == LogEntry.Level.DEBUG) {
                        shouldInclude = true; // 包含所有级别
                    } else if (level == LogEntry.Level.INFO) {
                        shouldInclude = line.contains("[INFO") || 
                                       line.contains("[WARN") || 
                                       line.contains("[ERROR");
                    } else if (level == LogEntry.Level.WARN) {
                        shouldInclude = line.contains("[WARN") || 
                                       line.contains("[ERROR");
                    } else if (level == LogEntry.Level.ERROR) {
                        shouldInclude = line.contains("[ERROR");
                    }
                }
                
                // 如果这是一个错误日志行，或者包含错误相关关键词，标记开始堆栈跟踪
                if (shouldInclude && (line.contains("[ERROR") || 
                                     line.contains("Exception") || 
                                     line.contains("Error") ||
                                     line.contains("错误") ||
                                     line.contains("堆栈"))) {
                    inStackTrace = true;
                }
            }
            
            if (shouldInclude) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                result.append(line);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 清除日志记录
     * @param logKey 日志键
     */
    public static void clearLogEntries(String logKey) {
        String cacheKey = LOG_ENTRIES_PREFIX + logKey;
        CacheUtils.removeKey(cacheKey);
        CacheUtils.removeKey(logKey);
    }
    
    /**
     * 清除指定键的所有日志记录
     * @param logKey 日志键
     */
    public static void clearLog(String logKey) {
        if (logKey == null || logKey.isEmpty()) {
            return;
        }
        CacheUtils.removeKey(logKey);
    }
} 