package com.datasophon.plugins.api;

import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.PluginMetadata;
import com.datasophon.common.enums.OsType;
import org.pf4j.Plugin;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 主机检查器插件基础接口
 * 所有检查器插件都需要实现此接口
 * 
 * @author DataSophon Team
 */
public interface HostCheckerPlugin extends Plugin {
    
    /**
     * 支持的操作系统类型
     * @return 支持的OS类型集合
     */
    Set<OsType> getSupportedOperatingSystems();
    
    /**
     * 检查优先级（数字越小优先级越高）
     * @return 优先级数值
     */
    int getPriority();
    
    /**
     * 执行检查（异步）
     * @param context 检查上下文
     * @return 检查结果的Future
     */
    CompletableFuture<CheckResult> executeCheck(HostCheckContext context);
    
    /**
     * 检查前置条件
     * @param context 检查上下文
     * @return 是否可以执行检查
     */
    boolean canExecute(HostCheckContext context);
    
    /**
     * 获取插件元数据
     * @return 插件元数据
     */
    PluginMetadata getMetadata();
    
    /**
     * 插件健康检查
     * @return 插件健康状态
     */
    default boolean isHealthy() {
        return true;
    }
    
    /**
     * 插件启动时的初始化方法
     */
    default void initialize() {
        // 默认空实现
    }
    
    /**
     * 插件停止时的清理方法
     */
    default void cleanup() {
        // 默认空实现
    }
    
    /**
     * 获取插件唯一标识符
     * @return 插件ID
     */
    default String getPluginId() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 获取插件版本
     * @return 版本号
     */
    default String getVersion() {
        return "1.0.0";
    }
}