package com.datasophon.plugins.api;

import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.common.enums.CheckType;
import com.datasophon.common.enums.OsType;
import org.pf4j.ExtensionPoint;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 主机校验插件接口
 * 负责执行各种主机检查项
 * 
 * 分层调用架构：
 * 1. 主程序调用校验插件
 * 2. 校验插件调用系统信息收集插件获取数据
 * 3. 系统信息收集插件调用SSH插件执行命令
 * 4. 校验插件基于收集到的数据执行检查逻辑
 * 
 * 设计原则：
 * - 校验插件不直接处理SSH连接，通过系统信息收集插件获取数据
 * - 校验插件专注于检查逻辑和规则判断
 * - 支持多种检查项，每个插件可以包含多个相关检查项
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
public interface HostValidationPlugin extends ExtensionPoint {
    
    /**
     * 支持的操作系统类型
     * 
     * @return 支持的OS类型集合
     */
    Set<OsType> getSupportedOperatingSystems();
    
    /**
     * 检查优先级（数字越小优先级越高）
     * 
     * @return 优先级数值
     */
    int getPriority();
    
    /**
     * 获取支持的检查项类型
     * 
     * @return 检查项类型列表
     */
    List<CheckType> getSupportedCheckTypes();
    
    /**
     * 执行指定类型的检查
     * 
     * @param context 检查上下文
     * @param checkType 检查类型
     * @return 检查结果的Future
     */
    CompletableFuture<CheckResult> executeCheck(HostCheckContext context, CheckType checkType);
    
    /**
     * 检查前置条件
     * 
     * @param context 检查上下文
     * @param checkType 检查类型
     * @return 是否可以执行检查
     */
    boolean canExecute(HostCheckContext context, CheckType checkType);
    
    /**
     * 获取插件唯一标识符
     * 
     * @return 插件ID
     */
    default String getPluginId() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 获取插件版本
     * 
     * @return 版本号
     */
    default String getVersion() {
        return "1.0.0";
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
}
