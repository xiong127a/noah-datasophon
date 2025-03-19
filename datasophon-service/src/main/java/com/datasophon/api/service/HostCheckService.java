package com.datasophon.api.service;

import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.LogEntry;
import com.datasophon.common.utils.Result;

import java.util.List;
import java.util.Map;

/**
 * 主机检查服务
 */
public interface HostCheckService {


    /**
     * 获取主机检查项列表
     * @return 检查项列表
     */
    List<CheckItem> getHostCheckItems();

    /**
     * 执行单个检查项
     * @param hostInfo 主机信息
     * @param checkItem 检查项
     * @return 检查结果
     */
    CheckItem executeCheckItem(HostInfo hostInfo, CheckItem checkItem,Integer clusterId);

    /**
     * 修复指定检查项
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @return 修复结果
     */
    Result fixCheckItem(Integer clusterId, String hostname, Integer itemId);

    /**
     * 修复指定检查项（支持跳过确认）
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @param skipConfirm 是否跳过确认
     * @return 修复结果
     */
    Result fixCheckItem(Integer clusterId, String hostname, Integer itemId, Boolean skipConfirm);

    /**
     * 获取检查项的确认信息
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @return 确认信息
     */
    Result getCheckItemConfirmInfo(Integer clusterId, String hostname, Integer itemId);

    /**
     * 修复选中的检查项
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemIds 检查项ID列表，逗号分隔
     * @return 修复结果
     */
    Result fixSelectedCheckItems(Integer clusterId, String hostname, String itemIds);

    /**
     * 修复所有检查项
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @return 修复结果
     */
    Result fixAllCheckItems(Integer clusterId, String hostname);

    /**
     * 终止指定主机的检查任务
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @return 终止结果
     */
    Result stopHostCheck(Integer clusterId, String hostname);
    
    /**
     * 终止指定主机的指定检查项
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @return 终止结果
     */
    Result stopItemCheck(Integer clusterId, String hostname, Integer itemId);

    /**
     * 检查单个主机
     *
     * @param clusterId 集群ID
     * @param hostname 主机名
     */
    void checkSingleHost(Integer clusterId, String hostname);

    /**
     * 批量检查多个主机
     *
     * @param clusterId 集群ID
     * @param hostnames 主机名列表
     * @return 操作结果
     */
    Result batchCheckHosts(Integer clusterId, List<String> hostnames);

    /**
     * 获取检查项的实时日志
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @return 检查项日志
     */
    Result getCheckItemLog(Integer clusterId, String hostname, Integer itemId);

    /**
     * 获取检查项日志（统一接口）
     * 整合了所有日志筛选功能
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @param logType 日志类型，支持 "all", "check", "fix"
     * @param logLevel 日志级别，支持 "DEBUG", "INFO", "WARN", "ERROR"
     * @param filterMode 筛选模式，"all"=全部日志, "exact"=精确级别, "min"=指定级别及以上
     * @return 筛选后的LogEntry列表
     */
    List<LogEntry> getLog(Integer clusterId, String hostname, Integer itemId, String logType, String logLevel, String filterMode);

    /**
     * 取消所有当前运行的检查任务
     * 
     * @return 操作结果
     */
    Result cancelAllCheckTasks();
    
    /**
     * 重试指定的检查项
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemNames 检查项名称列表
     * @return 操作结果
     */
    Result retryCheckItems(Integer clusterId, String hostname, List<String> itemNames);

    /**
     * 批量修复检查项
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemIds 检查项ID列表
     * @return 修复结果
     */
    Result batchFixCheckItem(Integer clusterId, String hostname, List<Integer> itemIds);

    /**
     * 获取格式化后的HTML日志
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @param logType 日志类型，支持 "all", "check", "fix"
     * @param logLevel 日志级别，支持 "DEBUG", "INFO", "WARN", "ERROR"
     * @param filterMode 筛选模式，"all"=全部日志, "exact"=精确级别, "min"=指定级别及以上
     * @return Result 包含格式化后的HTML日志
     */
    Result getFormattedLog(Integer clusterId, String hostname, Integer itemId, String logType, String logLevel, String filterMode);

    /**
     * 获取系统支持的日志类型
     * @return 日志类型映射，key为类型编码，value为显示名称
     */
    Map<String, String> getLogTypes();

    /**
     * 跳过指定检查项
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @return 跳过结果
     */
    Result skipCheckItem(Integer clusterId, String hostname, Integer itemId);

    /**
     * 获取可用的日志级别
     * @return 日志级别数组
     */
    LogEntry.Level[] getLogLevels();
} 