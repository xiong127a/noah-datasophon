package com.datasophon.common.model;

import lombok.Data;
import java.io.Serializable;

/**
 * 日志统计信息实体类
 * 用于统计不同级别日志的数量
 */
@Data
public class LogStats implements Serializable {
    private static final long serialVersionUID = 1L;

    // 总日志数量
    private Integer total;

    // 错误日志数量
    private Integer error;

    // 信息日志数量
    private Integer info;

    // 警告日志数量
    private Integer warn;

    // 调试日志数量
    private Integer debug;

    /**
     * 默认构造函数
     */
    public LogStats() {
        this.total = 0;
        this.error = 0;
        this.info = 0;
        this.warn = 0;
        this.debug = 0;
    }

    /**
     * 带参构造函数
     * 
     * @param total 总日志数量
     * @param error 错误日志数量
     * @param info  信息日志数量
     * @param warn  警告日志数量
     * @param debug 调试日志数量
     */
    public LogStats(Integer total, Integer error, Integer info, Integer warn, Integer debug) {
        this.total = total;
        this.error = error;
        this.info = info;
        this.warn = warn;
        this.debug = debug;
    }
}