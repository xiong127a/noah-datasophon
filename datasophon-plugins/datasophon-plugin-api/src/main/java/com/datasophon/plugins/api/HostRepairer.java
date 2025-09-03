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
 * 主机修复接口
 * 负责执行各种主机问题修复
 * 
 * 分层调用架构：
 * 1. 主程序调用修复插件
 * 2. 修复插件调用系统信息收集插件获取当前状态
 * 3. 修复插件调用SSH插件执行修复命令
 * 4. 修复完成后可以重新调用检查插件验证修复结果
 * 
 * 设计原则：
 * - 修复插件不直接处理SSH连接，通过SSH插件执行命令
 * - 修复插件专注于修复逻辑和命令生成
 * - 支持多种修复类型，每个插件可以包含多个相关修复项
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
public interface HostRepairer extends ExtensionPoint {
    
    /**
     * 支持的操作系统类型
     * 
     * @return 支持的OS类型集合
     */
    Set<OsType> getSupportedOperatingSystems();
    
    /**
     * 获取支持修复的检查项类型
     * 
     * @return 可修复的检查项类型列表
     */
    List<CheckType> getSupportedRepairTypes();
    
    /**
     * 执行指定类型的修复
     * 
     * @param context 检查上下文
     * @param repairType 修复类型
     * @param repairParams 修复参数
     * @return 修复结果的Future
     */
    CompletableFuture<CheckResult> executeRepair(HostCheckContext context, CheckType repairType, 
                                               java.util.Map<String, Object> repairParams);
    
    /**
     * 检查是否可以修复
     * 
     * @param context 检查上下文
     * @param repairType 修复类型
     * @return 是否可以执行修复
     */
    boolean canRepair(HostCheckContext context, CheckType repairType);
    
    /**
     * 获取修复建议
     * 
     * @param context 检查上下文
     * @param repairType 修复类型
     * @return 修复建议描述
     */
    String getRepairSuggestion(HostCheckContext context, CheckType repairType);
    
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
