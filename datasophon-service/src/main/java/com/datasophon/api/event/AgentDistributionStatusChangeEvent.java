package com.datasophon.api.event;

import org.springframework.context.ApplicationEvent;

/**
 * Agent分发状态变更事件
 * 当Agent分发状态发生变化时触发（用于SSE实时推送）
 * 
 * @author DataSophon Team
 * @date 2025-10-28
 */
public class AgentDistributionStatusChangeEvent extends ApplicationEvent {

    private final Long clusterId;
    private final String hostIp;
    private final long eventTimestamp;

    public AgentDistributionStatusChangeEvent(Object source, Long clusterId, String hostIp) {
        super(source);
        this.clusterId = clusterId;
        this.hostIp = hostIp;
        this.eventTimestamp = System.currentTimeMillis();
    }

    public Long getClusterId() {
        return clusterId;
    }

    public String getHostIp() {
        return hostIp;
    }

    public long getEventTimestamp() {
        return eventTimestamp;
    }
}

