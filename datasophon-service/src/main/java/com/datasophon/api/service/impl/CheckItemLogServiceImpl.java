package com.datasophon.api.service.impl;

import com.datasophon.api.service.CheckItemLogService;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.CheckItemLog;
import com.datasophon.common.utils.Result;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 检查项日志管理服务实现
 */
@Service
public class CheckItemLogServiceImpl implements CheckItemLogService {
    private static final Logger logger = LoggerFactory.getLogger(CheckItemLogServiceImpl.class);
    private static final String CHECK_ITEM_LOGS_CACHE_PREFIX = "CHECK_ITEM_LOGS_";
    
    // 存储所有日志的内存结构（生产环境应该替换为数据库）
    private final Map<String, List<CheckItemLog>> logsCache = new ConcurrentHashMap<>();
    
    // 日志正则表达式模式
    private static final Pattern INFO_PATTERN = Pattern.compile("(.*?)正在执行检查.*");
    private static final Pattern SUCCESS_PATTERN = Pattern.compile("(.*?)检查通过.*");
    private static final Pattern ERROR_PATTERN = Pattern.compile("(.*?)检查失败.*|.*?错误:.*|.*?异常:.*");
    private static final Pattern WARNING_PATTERN = Pattern.compile("(.*?)等待.*|.*?超时.*");

    @Override
    public boolean addLog(CheckItemLog log) {
        if (log == null) {
            return false;
        }
        
        // 确保日志ID唯一
        if (StringUtils.isBlank(log.getId())) {
            log.setId(UUID.randomUUID().toString());
        }
        
        // 确保时间戳存在
        if (log.getTimestamp() == null) {
            log.setTimestamp(new Date());
        }
        
        // 获取日志缓存键
        String cacheKey = getCacheKey(log.getClusterId(), log.getHostname(), log.getItemId());
        
        // 从缓存获取日志列表，如果不存在则创建新列表
        List<CheckItemLog> logs = logsCache.computeIfAbsent(cacheKey, k -> new ArrayList<>());
        
        // 添加日志
        logs.add(log);
        
        // 更新缓存
        logsCache.put(cacheKey, logs);
        
        return true;
    }

    @Override
    public boolean batchAddLogs(List<CheckItemLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return false;
        }
        
        boolean allSuccess = true;
        for (CheckItemLog log : logs) {
            if (!addLog(log)) {
                allSuccess = false;
            }
        }
        
        return allSuccess;
    }

    @Override
    public Result getCheckItemLogs(Integer clusterId, String hostname, Integer itemId, 
                                 CheckItemLog.LogLevel level, Date startTime, Date endTime, 
                                 String keyword, Integer page, Integer pageSize) {
        if (clusterId == null) {
            return Result.error("集群ID不能为空");
        }
        
        // 获取日志缓存键
        String cacheKey = getCacheKey(clusterId, hostname, itemId);
        
        // 获取日志列表
        List<CheckItemLog> logs = logsCache.getOrDefault(cacheKey, new ArrayList<>());
        
        // 根据条件筛选日志
        List<CheckItemLog> filteredLogs = logs.stream()
            .filter(log -> {
                // 筛选日志级别
                if (level != null && log.getLevel() != level) {
                    return false;
                }
                
                // 筛选时间范围
                if (startTime != null && log.getTimestamp().before(startTime)) {
                    return false;
                }
                if (endTime != null && log.getTimestamp().after(endTime)) {
                    return false;
                }
                
                // 筛选关键字
                if (StringUtils.isNotBlank(keyword) && !log.getMessage().contains(keyword)) {
                    return false;
                }
                
                return true;
            })
            .sorted(Comparator.comparing(CheckItemLog::getTimestamp).reversed())
            .collect(Collectors.toList());
        
        // 分页处理
        int total = filteredLogs.size();
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, total);
        
        if (startIndex >= total) {
            return Result.success().put("logs", new ArrayList<>()).put("total", total);
        }
        
        List<CheckItemLog> pageData = filteredLogs.subList(startIndex, endIndex);
        
        return Result.success().put("logs", pageData).put("total", total);
    }

    @Override
    public boolean cleanupLogs(int days) {
        if (days <= 0) {
            return false;
        }
        
        // 计算清理日期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        Date cutoffDate = calendar.getTime();
        
        // 遍历所有日志缓存
        for (Map.Entry<String, List<CheckItemLog>> entry : logsCache.entrySet()) {
            List<CheckItemLog> logs = entry.getValue();
            
            // 保留未过期的日志
            List<CheckItemLog> validLogs = logs.stream()
                .filter(log -> log.getTimestamp().after(cutoffDate))
                .collect(Collectors.toList());
            
            // 更新缓存
            logsCache.put(entry.getKey(), validLogs);
        }
        
        return true;
    }

    @Override
    public List<CheckItemLog> parseAndSaveLogs(Integer clusterId, String hostname, Integer itemId, 
                                              String itemName, String logContent) {
        if (clusterId == null || StringUtils.isBlank(hostname) || itemId == null || StringUtils.isBlank(logContent)) {
            return Collections.emptyList();
        }
        
        List<CheckItemLog> parsedLogs = new ArrayList<>();
        String[] lines = logContent.split("\n");
        
        for (String line : lines) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            
            // 解析日志级别和消息
            CheckItemLog.LogLevel level = parseLogLevel(line);
            
            // 创建日志对象
            CheckItemLog log = CheckItemLog.builder()
                .id(UUID.randomUUID().toString())
                .clusterId(clusterId)
                .hostname(hostname)
                .itemId(itemId)
                .itemName(itemName)
                .level(level)
                .message(line)
                .timestamp(new Date()) // 在实际生产环境中应该尝试从日志中解析时间
                .build();
            
            parsedLogs.add(log);
            addLog(log);
        }
        
        return parsedLogs;
    }
    
    /**
     * 根据日志内容解析日志级别
     */
    private CheckItemLog.LogLevel parseLogLevel(String logContent) {
        if (StringUtils.isBlank(logContent)) {
            return CheckItemLog.LogLevel.INFO;
        }
        
        // 检查是否匹配成功模式
        Matcher successMatcher = SUCCESS_PATTERN.matcher(logContent);
        if (successMatcher.find()) {
            return CheckItemLog.LogLevel.SUCCESS;
        }
        
        // 检查是否匹配错误模式
        Matcher errorMatcher = ERROR_PATTERN.matcher(logContent);
        if (errorMatcher.find()) {
            return CheckItemLog.LogLevel.ERROR;
        }
        
        // 检查是否匹配警告模式
        Matcher warningMatcher = WARNING_PATTERN.matcher(logContent);
        if (warningMatcher.find()) {
            return CheckItemLog.LogLevel.WARNING;
        }
        
        // 默认为INFO级别
        return CheckItemLog.LogLevel.INFO;
    }
    
    /**
     * 获取日志缓存键
     */
    private String getCacheKey(Integer clusterId, String hostname, Integer itemId) {
        StringBuilder keyBuilder = new StringBuilder(CHECK_ITEM_LOGS_CACHE_PREFIX);
        
        if (clusterId != null) {
            keyBuilder.append(clusterId).append("_");
        }
        
        if (StringUtils.isNotBlank(hostname)) {
            keyBuilder.append(hostname).append("_");
        }
        
        if (itemId != null) {
            keyBuilder.append(itemId);
        }
        
        return keyBuilder.toString();
    }
} 