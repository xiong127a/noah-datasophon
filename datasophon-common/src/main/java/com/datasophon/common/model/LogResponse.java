package com.datasophon.common.model;

import lombok.Data;
import java.io.Serializable;

/**
 * 日志响应实体类
 * 包含日志内容和日志统计信息
 */
@Data
public class LogResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    // 日志HTML内容
    private String logContent;

    // 日志统计信息
    private LogStats logStats;

    /**
     * 默认构造函数
     */
    public LogResponse() {
        this.logContent = "";
        this.logStats = new LogStats();
    }

    /**
     * 带参构造函数
     * 
     * @param logContent 日志HTML内容
     * @param logStats   日志统计信息
     */
    public LogResponse(String logContent, LogStats logStats) {
        this.logContent = logContent;
        this.logStats = logStats;
    }
}