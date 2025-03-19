package com.datasophon.api.service.checker;

import com.datasophon.common.model.LogEntry;
import lombok.Setter;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * 检查项日志记录器接口
 * 提供类似log4j的日志记录方法，支持不同日志级别
 */
public interface CheckLogger {
    
    /**
     * 创建通用的日志记录器实例
     * @param logKey 日志缓存键
     * @param className 类名
     * @return 日志记录器实例
     */
    static CheckLogger createLogger(String logKey, String className) {
        return new LoggerImpl(logKey, className);
    }
    
    /**
     * 创建具有特定类型的日志记录器实例
     * @param logKey 日志缓存键
     * @param className 类名
     * @param logType 日志类型
     * @return 日志记录器实例
     */
    static CheckLogger createLogger(String logKey, String className, LogEntry.Type logType) {
        return new LoggerImpl(logKey, className, logType);
    }
    
    /**
     * 日志记录器统一实现
     * 此实现可以同时被AbstractItemChecker和HostCheckServiceImpl共享使用
     */
    class LoggerImpl implements CheckLogger {
        private String logKey;
        private final String className;
        private final org.slf4j.Logger slf4jLogger;
        /**
         * -- SETTER --
         *  设置日志类型
         *
         */
        @Setter
        private LogEntry.Type logType = LogEntry.Type.CHECK;

        public LoggerImpl(String logKey, String className) {
            this.logKey = logKey;
            this.className = className;
            this.slf4jLogger = LoggerFactory.getLogger(className);
        }
        
        public LoggerImpl(String logKey, String className, LogEntry.Type logType) {
            this.logKey = logKey;
            this.className = className;
            this.slf4jLogger = LoggerFactory.getLogger(className);
            this.logType = logType;
        }
        
        /**
         * 更新日志键
         * 用于动态更改日志的存储位置
         * @param newLogKey 新的日志键
         */
        public void updateLogKey(String newLogKey) {
            this.logKey = newLogKey;
        }

        private void addLogEntry(String levelStr, String message) {
            try {
                // 创建结构化日志记录
                Date timestamp = new Date();
                String threadName = Thread.currentThread().getName();
                LogEntry.Level level = LogEntry.Level.valueOf(levelStr);
                
                LogEntry logEntry = new LogEntry(timestamp, level, threadName, className, message, this.logType);
                
                // 获取调用者的行号作为元数据
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                if (stackTrace.length >= 4) {
                    logEntry.setLineNumber(stackTrace[3].getLineNumber());
                    // 设置堆栈信息的日志级别
                    logEntry.setStackTraceLevel(level);
                }
                
                // 添加到日志管理器
                LogEntryManager.addLogEntry(logKey, logEntry);
                
                // 同时输出到标准日志
                switch (level) {
                    case INFO:
                        slf4jLogger.info(message);
                        break;
                    case WARN:
                        slf4jLogger.warn(message);
                        break;
                    case ERROR:
                        slf4jLogger.error(message);
                        break;
                    case DEBUG:
                        slf4jLogger.debug(message);
                        break;
                }
            } catch (Exception e) {
                slf4jLogger.error("添加日志记录失败: {}", e.getMessage(), e);
            }
        }

        @Override
        public void info(String message) {
            addLogEntry("INFO", message);
        }

        @Override
        public void info(String format, Object... args) {
            String message = String.format(format, args);
            addLogEntry("INFO", message);
        }

        @Override
        public void warn(String message) {
            addLogEntry("WARN", message);
        }

        @Override
        public void warn(String format, Object... args) {
            String message = String.format(format, args);
            addLogEntry("WARN", message);
        }

        @Override
        public void error(String message) {
            addLogEntry("ERROR", message);
        }

        @Override
        public void error(String format, Object... args) {
            String message = String.format(format, args);
            addLogEntry("ERROR", message);
        }

        @Override
        public void debug(String message) {
            addLogEntry("DEBUG", message);
        }

        @Override
        public void debug(String format, Object... args) {
            String message = String.format(format, args);
            addLogEntry("DEBUG", message);
        }
    }
    
    /**
     * 记录INFO级别日志
     */
    void info(String message);
    
    /**
     * 记录INFO级别日志，支持格式化
     */
    void info(String format, Object... args);
    
    /**
     * 记录WARN级别日志
     */
    void warn(String message);
    
    /**
     * 记录WARN级别日志，支持格式化
     */
    void warn(String format, Object... args);
    
    /**
     * 记录ERROR级别日志
     */
    void error(String message);
    
    /**
     * 记录ERROR级别日志，支持格式化
     */
    void error(String format, Object... args);
    
    /**
     * 记录DEBUG级别日志
     */
    void debug(String message);
    
    /**
     * 记录DEBUG级别日志，支持格式化
     */
    void debug(String format, Object... args);
} 