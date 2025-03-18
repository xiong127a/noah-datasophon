package com.datasophon.api.controller;

import com.datasophon.api.service.checker.LogEntryManager;
import com.datasophon.common.utils.Result;
import com.datasophon.common.model.LogEntry;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 检查日志控制器
 */
@RestController
@RequestMapping("/host/check-log")
public class CheckLogController {
    private static final Logger logger = LoggerFactory.getLogger(CheckLogController.class);
    private static final String LOG_KEY_PREFIX = "CHECK_ITEM_LOG_";
    
    /**
     * 获取检查项的日志内容
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @return 日志内容
     */
    @GetMapping("/content")
    public Result getLogContent(
            @RequestParam Integer clusterId,
            @RequestParam String hostname,
            @RequestParam Integer itemId) {
        String logKey = LOG_KEY_PREFIX + clusterId + "_" + hostname + "_" + itemId;
        logger.debug("获取日志内容, logKey: {}", logKey);
        
        try {
            String logContent = LogEntryManager.getLogContent(logKey);
            return Result.success(logContent == null ? "" : logContent);
        } catch (Exception e) {
            logger.error("获取日志内容失败: {}", e.getMessage(), e);
            return Result.error("获取日志内容失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取按日志级别筛选的日志内容
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @param level 日志级别（DEBUG/INFO/WARN/ERROR）
     * @param exactMatch 是否精确匹配级别（true表示只显示指定级别，false表示显示指定级别及以上）
     * @return 筛选后的日志内容
     */
    @GetMapping("/filtered")
    public Result getFilteredLogContent(
            @RequestParam Integer clusterId,
            @RequestParam String hostname,
            @RequestParam Integer itemId,
            @RequestParam String level,
            @RequestParam Boolean exactMatch) {
        String logKey = LOG_KEY_PREFIX + clusterId + "_" + hostname + "_" + itemId;
        logger.debug("获取筛选日志内容, logKey: {}, level: {}, exactMatch: {}", logKey, level, exactMatch);
        
        try {
            LogEntry.Level logLevel;
            try {
                logLevel = LogEntry.Level.valueOf(level.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("无效的日志级别: {}, 使用默认级别INFO", level);
                return Result.error("无效的日志级别: " + level);
            }
            
            String filteredContent = LogEntryManager.getFilteredLogContent(logKey, logLevel, exactMatch);
            logger.debug("筛选结果长度: {} 字符", filteredContent == null ? 0 : filteredContent.length());
            return Result.success(filteredContent == null ? "" : filteredContent);
        } catch (Exception e) {
            logger.error("筛选日志内容失败: {}", e.getMessage(), e);
            return Result.error("筛选日志内容失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取可用的日志级别
     * @return 日志级别列表
     */
    @GetMapping("/levels")
    public Result getLogLevels() {
        try {
            return Result.success(LogEntry.Level.values());
        } catch (Exception e) {
            logger.error("获取日志级别列表失败: {}", e.getMessage(), e);
            return Result.error("获取日志级别列表失败: " + e.getMessage());
        }
    }
} 