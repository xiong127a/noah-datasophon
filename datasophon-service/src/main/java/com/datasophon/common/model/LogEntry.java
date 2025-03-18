package com.datasophon.common.model;

import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * 结构化日志记录
 */
public class LogEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 日志级别枚举
     */
    public enum Level {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3);
        
        private int value;
        
        Level(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        /**
         * 判断当前级别是否包含目标级别
         * 例如: INFO.includes(DEBUG) 返回 false
         *      INFO.includes(INFO) 返回 true
         *      INFO.includes(WARN) 返回 true
         */
        public boolean includes(Level targetLevel) {
            return this.value <= targetLevel.value;
        }
        
        /**
         * 判断当前级别是否高于或等于目标级别
         */
        public boolean isHigherOrEqual(Level targetLevel) {
            return this.value >= targetLevel.value;
        }
    }
    
    // 日志时间
    private Date timestamp;
    // 日志级别
    private Level level;
    // 线程名称
    private String threadName;
    // 日志来源类名
    private String className;
    // 日志内容
    private String message;
    
    public LogEntry() {
    }
    
    public LogEntry(Date timestamp, Level level, String threadName, String className, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.threadName = threadName;
        this.className = className;
        this.message = message;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public Level getLevel() {
        return level;
    }
    
    public void setLevel(Level level) {
        this.level = level;
    }
    
    public String getThreadName() {
        return threadName;
    }
    
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * 将日志格式化为字符串
     * 包含完整信息：时间戳、级别、线程名、类名和消息
     */
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return String.format("%s [%-5s] [%-20s] %-30s - %s", 
                sdf.format(timestamp), level, threadName, className, message);
    }
} 