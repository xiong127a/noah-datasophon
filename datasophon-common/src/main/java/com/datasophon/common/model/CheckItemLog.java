package com.datasophon.common.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 检查项日志实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckItemLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private String id;

    /**
     * 集群ID
     */
    private Integer clusterId;

    /**
     * 主机名
     */
    private String hostname;

    /**
     * 检查项ID
     */
    private Integer itemId;

    /**
     * 检查项名称
     */
    private String itemName;

    /**
     * 日志级别
     */
    private LogLevel level;

    /**
     * 日志消息
     */
    private String message;

    /**
     * 日志时间
     */
    private Date timestamp;

    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        INFO,
        WARNING,
        ERROR,
        SUCCESS
    }
} 