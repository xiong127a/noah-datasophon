package com.datasophon.api.service;

import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.utils.Result;

import java.util.List;

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
     * 获取检查项的实时日志（带类型过滤）
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @param logType 日志类型：check(检查日志)、fix(修复日志)、all(全部)
     * @return 过滤后的检查项日志
     */
    Result getCheckItemLogWithType(Integer clusterId, String hostname, Integer itemId, String logType);

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
} 