package com.datasophon.api.workflow.activity;

import com.datasophon.common.model.OsInfo;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.common.model.HostInfo;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

/**
 * 主机检查活动接口
 * 定义工作流中需要执行的具体活动
 * 
 * @author DataSophon Team
 */
@ActivityInterface
public interface HostCheckActivities {
    
    /**
     * 收集主机信息
     * @param hostInfo 基础主机信息
     * @return 详细的主机信息
     */
    @ActivityMethod
    HostInfo collectHostInfo(HostInfo hostInfo);
    
    /**
     * 检测操作系统信息
     * @param hostInfo 主机信息
     * @return 操作系统信息
     */
    @ActivityMethod
    OsInfo detectOperatingSystem(HostInfo hostInfo);
    
    /**
     * 发现可用的插件
     * @param osInfo 操作系统信息
     * @param requiredCheckTypes 需要的检查类型
     * @return 可用的插件ID列表
     */
    @ActivityMethod
    List<String> discoverPlugins(OsInfo osInfo, List<String> requiredCheckTypes);
    
    /**
     * 执行单个插件检查
     * @param pluginId 插件ID
     * @param context 检查上下文
     * @return 检查结果
     */
    @ActivityMethod
    CheckResult executePlugin(String pluginId, HostCheckContext context);
    
    /**
     * 批量执行插件检查
     * @param pluginIds 插件ID列表
     * @param context 检查上下文
     * @return 检查结果列表
     */
    @ActivityMethod
    List<CheckResult> executePlugins(List<String> pluginIds, HostCheckContext context);
    
    /**
     * 聚合检查结果
     * @param results 检查结果列表
     * @return 聚合后的结果摘要
     */
    @ActivityMethod
    String aggregateResults(List<CheckResult> results);
    
    /**
     * 发送通知
     * @param hostInfo 主机信息
     * @param results 检查结果
     */
    @ActivityMethod
    void sendNotification(HostInfo hostInfo, List<CheckResult> results);
    
    /**
     * 保存检查结果到数据库
     * @param hostInfo 主机信息
     * @param results 检查结果
     */
    @ActivityMethod
    void saveCheckResults(HostInfo hostInfo, List<CheckResult> results);
    
    /**
     * 创建SSH会话
     * @param hostInfo 主机信息
     * @return 是否创建成功
     */
    @ActivityMethod
    boolean createSshSession(HostInfo hostInfo);
    
    /**
     * 关闭SSH会话
     * @param hostInfo 主机信息
     */
    @ActivityMethod
    void closeSshSession(HostInfo hostInfo);
    
    /**
     * 健康检查
     * @param hostInfo 主机信息
     * @return 主机是否健康可达
     */
    @ActivityMethod
    boolean healthCheck(HostInfo hostInfo);
}