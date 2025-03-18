package com.datasophon.api.service.checker;

/**
 * 检查项日志记录器接口
 * 提供类似log4j的日志记录方法，支持不同日志级别
 */
public interface CheckLogger {
    
    /**
     * 日志分隔符常量
     */
    String LOG_SEPARATOR = "====";
    String LOG_SECTION_BEGIN = LOG_SEPARATOR + " ";
    String LOG_SECTION_END = " " + LOG_SEPARATOR;
    
    /**
     * 日志前缀常量，用于缓存键等
     */
    String CHECK_ITEM_LOG_PREFIX = "CHECK_ITEM_LOG_";
    
    /**
     * 记录INFO级别日志
     */
    void info(String message);
    
    /**
     * 记录WARN级别日志
     */
    void warn(String message);
    
    /**
     * 记录ERROR级别日志
     */
    void error(String message);
    
    /**
     * 记录DEBUG级别日志
     */
    void debug(String message);
    
    /**
     * 带格式化的debug级别日志
     * @param format 格式化字符串
     * @param args 参数
     */
    default void debug(String format, Object... args) {
        debug(String.format(format, args));
    }
    
    /**
     * 带格式化的info级别日志
     * @param format 格式化字符串
     * @param args 参数
     */
    default void info(String format, Object... args) {
        info(String.format(format, args));
    }
    
    /**
     * 带格式化的warn级别日志
     * @param format 格式化字符串
     * @param args 参数
     */
    default void warn(String format, Object... args) {
        warn(String.format(format, args));
    }
    
    /**
     * 带格式化的error级别日志
     * @param format 格式化字符串
     * @param args 参数
     */
    default void error(String format, Object... args) {
        error(String.format(format, args));
    }
} 