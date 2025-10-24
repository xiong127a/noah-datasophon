package com.datasophon.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 修复完成事件
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
@Getter
public class RepairCompleteEvent extends ApplicationEvent {
    
    private final Long clusterId;
    private final String hostIp;
    private final String checkKey;
    private final boolean success;
    private final String message;
    
    public RepairCompleteEvent(Object source, Long clusterId, String hostIp, String checkKey, boolean success, String message) {
        super(source);
        this.clusterId = clusterId;
        this.hostIp = hostIp;
        this.checkKey = checkKey;
        this.success = success;
        this.message = message;
    }
}

