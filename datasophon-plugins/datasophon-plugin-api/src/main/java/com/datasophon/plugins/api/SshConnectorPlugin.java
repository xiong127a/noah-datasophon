package com.datasophon.plugins.api;

import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.PluginMetadata;
import com.datasophon.common.enums.OsType;
import org.pf4j.ExtensionPoint;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * SSH连接器插件接口
 * 负责SSH连接检查和管理
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
public interface SshConnectorPlugin extends ExtensionPoint {
    
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
     * 执行SSH连接检查
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
