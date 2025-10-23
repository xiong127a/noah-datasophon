package com.datasophon.api.checker;

import com.datasophon.common.vo.environment.RepairResult;

import java.util.Map;

/**
 * 环境检查项接口
 * 所有检查器都需要实现此接口
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
public interface EnvironmentCheckItem {
    
    /**
     * 获取检查项键名
     * 例如: "cpu", "memory", "java", "disk"等
     * 
     * @return 检查项键名
     */
    String getCheckKey();
    
    /**
     * 获取检查项显示名称
     * 例如: "CPU核心数检查", "内存检查", "JDK环境检查"
     * 
     * @return 显示名称
     */
    String getDisplayName();
    
    /**
     * 获取优先级
     * 数字越小优先级越高，检查顺序按优先级执行
     * 
     * @return 优先级
     */
    int getPriority();
    
    /**
     * 执行检查
     * 
     * @param context 主机检查上下文
     * @return 检查结果
     */
    CheckResult execute(HostCheckContext context);
    
    /**
     * 修复检查失败项
     * 
     * @param context 主机检查上下文
     * @param params 修复参数
     * @return 修复结果
     */
    RepairResult repair(HostCheckContext context, Map<String, Object> params);
    
    /**
     * 是否启用此检查项
     * 可以从配置中读取
     * 
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}

