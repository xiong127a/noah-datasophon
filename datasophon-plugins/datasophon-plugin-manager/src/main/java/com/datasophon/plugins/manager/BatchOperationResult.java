package com.datasophon.plugins.manager;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量操作结果
 * 
 * @author DataSophon Team
 */
@Data
public class BatchOperationResult {
    
    /**
     * 成功的插件列表
     */
    private List<String> successPlugins = new ArrayList<>();
    
    /**
     * 失败的插件及原因
     */
    private Map<String, String> failedPlugins = new HashMap<>();
    
    /**
     * 添加成功结果
     */
    public void addSuccess(String pluginId) {
        successPlugins.add(pluginId);
    }
    
    /**
     * 添加失败结果
     */
    public void addFailure(String pluginId, String reason) {
        failedPlugins.put(pluginId, reason);
    }
    
    /**
     * 获取成功数量
     */
    public int getSuccessCount() {
        return successPlugins.size();
    }
    
    /**
     * 获取失败数量
     */
    public int getFailureCount() {
        return failedPlugins.size();
    }
    
    /**
     * 获取总数量
     */
    public int getTotalCount() {
        return getSuccessCount() + getFailureCount();
    }
    
    /**
     * 是否全部成功
     */
    public boolean isAllSuccess() {
        return getFailureCount() == 0;
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        int total = getTotalCount();
        if (total == 0) {
            return 0.0;
        }
        return (double) getSuccessCount() / total * 100.0;
    }
}