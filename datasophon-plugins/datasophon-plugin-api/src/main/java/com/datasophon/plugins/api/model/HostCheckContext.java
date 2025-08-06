package com.datasophon.plugins.api.model;

import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import lombok.Builder;
import lombok.Data;
import org.apache.sshd.client.session.ClientSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主机检查上下文
 * 包含检查过程中需要的所有信息和共享数据
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class HostCheckContext {
    
    /**
     * 主机信息
     */
    private HostInfo hostInfo;
    
    /**
     * 操作系统信息
     */
    private OsInfo osInfo;
    
    /**
     * 检查配置
     */
    private CheckConfiguration configuration;
    
    /**
     * 插件间共享数据
     */
    @Builder.Default
    private Map<String, Object> sharedData = new ConcurrentHashMap<>();
    
    /**
     * SSH会话
     */
    private ClientSession sshSession;
    
    /**
     * 检查开始时间
     */
    @Builder.Default
    private long startTime = System.currentTimeMillis();
    
    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private long timeout = 300000; // 5分钟
    
    /**
     * 是否快速失败
     */
    @Builder.Default
    private boolean failFast = false;
    
    /**
     * 检查请求ID
     */
    private String requestId;
    
    /**
     * 添加共享数据
     */
    public void addSharedData(String key, Object value) {
        sharedData.put(key, value);
    }
    
    /**
     * 获取共享数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getSharedData(String key, Class<T> type) {
        return (T) sharedData.get(key);
    }
    
    /**
     * 检查是否超时
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - startTime > timeout;
    }
    
    /**
     * 获取已用时间
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 获取剩余时间
     */
    public long getRemainingTime() {
        return Math.max(0, timeout - getElapsedTime());
    }
}