package com.datasophon.api.service;

import com.datasophon.common.model.CheckItemLog;
import com.datasophon.common.utils.Result;

import java.util.Date;
import java.util.List;

/**
 * 检查项日志管理服务接口
 */
public interface CheckItemLogService {

    /**
     * 添加日志
     *
     * @param log 日志对象
     * @return 添加结果
     */
    boolean addLog(CheckItemLog log);

    /**
     * 批量添加日志
     *
     * @param logs 日志对象列表
     * @return 添加结果
     */
    boolean batchAddLogs(List<CheckItemLog> logs);

    /**
     * 查询主机检查项日志
     *
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @param level 日志级别
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param keyword 关键字
     * @param page 页码
     * @param pageSize 每页数量
     * @return 日志结果
     */
    Result getCheckItemLogs(
        Integer clusterId,
        String hostname,
        Integer itemId,
        CheckItemLog.LogLevel level,
        Date startTime,
        Date endTime,
        String keyword,
        Integer page,
        Integer pageSize
    );

    /**
     * 清理过期日志
     *
     * @param days 保留天数
     * @return 清理结果
     */
    boolean cleanupLogs(int days);

    /**
     * 解析日志文本并保存为结构化日志
     *
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @param itemName 检查项名称
     * @param logContent 日志内容
     * @return 解析结果
     */
    List<CheckItemLog> parseAndSaveLogs(
        Integer clusterId,
        String hostname,
        Integer itemId,
        String itemName,
        String logContent
    );
} 