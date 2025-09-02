package com.datasophon.plugins.api;

import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.SystemInfo;
import org.pf4j.ExtensionPoint;

import java.util.concurrent.CompletableFuture;

/**
 * 系统信息收集插件接口
 * 负责收集各种系统信息，供检查和修复插件使用
 * 
 * 分层调用架构：
 * 1. 检查插件或修复插件调用信息收集插件
 * 2. 信息收集插件调用SSH插件执行命令
 * 3. 信息收集插件解析命令结果并返回结构化数据
 * 
 * 设计原则：
 * - 信息收集插件不直接处理SSH连接，通过SSH插件执行命令
 * - 信息收集插件专注于命令生成、结果解析和数据结构化
 * - 支持多种操作系统，内部使用策略模式处理OS差异
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
public interface SystemInfoCollectorPlugin extends ExtensionPoint {
    

    
    /**
     * 收集完整的系统信息
     * 包含CPU、内存、磁盘、OS、Java、网络等各种信息
     * 
     * @param context 主机检查上下文
     * @return 完整的系统信息
     */
    CompletableFuture<SystemInfo> collectSystemInfo(HostCheckContext context);
    
    /**
     * 获取插件唯一标识符
     * 
     * @return 插件ID
     */
    default String getPluginId() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 插件健康检查
     * 
     * @return 插件健康状态
     */
    default boolean isHealthy() {
        return true;
    }

    /**
     * 插件初始化方法
     */
    default void initialize() {
        // 默认空实现
    }

    /**
     * 插件清理方法
     */
    default void cleanup() {
        // 默认空实现
    }
}
