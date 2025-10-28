package com.datasophon.api.event;

import org.springframework.context.ApplicationEvent;

/**
 * 环境检查状态变更事件
 * 当任何检查项或主机状态变化时发布此事件
 * 
 * @author 任相鹏
 * @date 2025-01-28
 */
public class CheckStatusChangeEvent extends ApplicationEvent {
    
    /**
     * 集群ID
     */
    private final Long clusterId;
    
    /**
     * 事件发生时间戳
     */
    private final long eventTimestamp;
    
    /**
     * 构造函数
     * 
     * @param source 事件源
     * @param clusterId 集群ID
     */
    public CheckStatusChangeEvent(Object source, Long clusterId) {
        super(source);
        this.clusterId = clusterId;
        this.eventTimestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取集群ID
     */
    public Long getClusterId() {
        return clusterId;
    }
    
    /**
     * 获取事件时间戳（重命名以避免与父类方法冲突）
     */
    public long getEventTimestamp() {
        return eventTimestamp;
    }
}

