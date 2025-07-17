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
         * 
         * @return 检查项列表
         */
        List<CheckItem> getHostCheckItems();

        /**
         * 执行单个检查项
         * 
         * @param hostInfo  主机信息
         * @param checkItem 检查项
         * @return 检查结果
         */
        CheckItem executeCheckItem(HostInfo hostInfo, CheckItem checkItem, Integer clusterId);

        /**
         * 修复指定检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemId    检查项ID
         * @return 修复结果
         */
        Result fixCheckItem(Integer clusterId, String ip, Integer itemId);

        /**
         * 修复指定检查项（支持跳过确认）
         * 
         * @param clusterId   集群ID
         * @param ip          主机IP
         * @param itemId      检查项ID
         * @param skipConfirm 是否跳过确认
         * @return 修复结果
         */
        Result fixCheckItem(Integer clusterId, String ip, Integer itemId, Boolean skipConfirm);

        /**
         * 获取检查项的确认信息
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemId    检查项ID
         * @return 确认信息
         */
        Result getCheckItemConfirmInfo(Integer clusterId, String ip, Integer itemId);

        /**
         * 修复选中的检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemIds   检查项ID列表，逗号分隔
         * @return 修复结果
         */
        Result fixSelectedCheckItems(Integer clusterId, String ip, String itemIds);

        /**
         * 修复所有检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @return 修复结果
         */
        Result fixAllCheckItems(Integer clusterId, String ip);

        /**
         * 终止指定主机的检查任务
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @return 终止结果
         */
        Result stopHostCheck(Integer clusterId, String ip);

        /**
         * 终止指定主机的指定检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemId    检查项ID
         * @return 终止结果
         */
        Result stopItemCheck(Integer clusterId, String ip, Integer itemId);

        /**
         * 检查单个主机
         *
         * @param clusterId 集群ID
         * @param ip        主机IP
         */
        void checkSingleHost(Integer clusterId, String ip);

        /**
         * 批量检查多个主机
         *
         * @param clusterId 集群ID
         * @param ips       主机IP列表
         * @return 操作结果
         */
        Result batchCheckHosts(Integer clusterId, List<String> ips);

        /**
         * 获取检查项的实时日志
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemId    检查项ID
         * @return 检查项日志
         */
        Result getCheckItemLog(Integer clusterId, String ip, Integer itemId);

        /**
         * 获取检查项日志（统一接口）
         * 整合了所有日志筛选功能
         * 
         * @param clusterId  集群ID
         * @param ip         主机IP
         * @param itemId     检查项ID
         * @param logType    日志类型，支持 "all", "check", "fix"
         * @param logLevel   日志级别，支持 "DEBUG", "INFO", "WARN", "ERROR"
         * @param filterMode 筛选模式，"all"=全部日志, "exact"=精确级别, "min"=指定级别及以上
         * @return 筛选后的LogEntry列表
         */
        List<LogEntry> getLog(Integer clusterId, String ip, Integer itemId, String logType, String logLevel,
                        String filterMode);

        /**
         * 取消所有当前运行的检查任务
         * 
         * @return 操作结果
         */
        Result cancelAllCheckTasks();

        /**
         * 重试指定的检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemNames 检查项名称列表
         * @return 操作结果
         */
        Result retryCheckItems(Integer clusterId, String ip, List<String> itemNames);

        /**
         * 获取格式化后的HTML日志
         * 
         * @param clusterId  集群ID
         * @param ip         主机IP
         * @param itemId     检查项ID
         * @param logType    日志类型，支持 "all", "check", "fix"
         * @param logLevel   日志级别，支持 "DEBUG", "INFO", "WARN", "ERROR"
         * @param filterMode 筛选模式，"all"=全部日志, "exact"=精确级别, "min"=指定级别及以上
         * @return Result 包含格式化后的HTML日志
         */
        Result getFormattedLog(Integer clusterId, String ip, Integer itemId, String logType, String logLevel,
                        String filterMode);

        /**
         * 获取系统支持的日志类型
         * 
         * @return 日志类型映射，key为类型编码，value为显示名称
         */
        Map<String, String> getLogTypes();

        /**
         * 跳过指定检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemId    检查项ID
         * @return 跳过结果
         */
        Result skipCheckItem(Integer clusterId, String ip, Integer itemId);

        /**
         * 获取可用的日志级别
         * 
         * @return 日志级别数组
         */
        LogEntry.Level[] getLogLevels();

        /**
         * 获取主机检查项列表
         * 
         * @param ip        主机IP
         * @param clusterId 集群ID
         * @return 检查项列表
         */
        Result getHostCheckItems(String ip, Integer clusterId);

        /**
         * 开始检查主机
         * 从缓存中获取主机列表并开始检查
         *
         * @param clusterId 集群ID
         * @return 操作结果
         */
        Result startHostCheck(Integer clusterId);

        /**
         * 更新主机名
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param hostname  新主机名
         * @return 操作结果
         */
        Result updateHostname(Integer clusterId, String ip, String hostname);

        /**
         * 更新主机名（可选是否同步hosts文件）
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param hostname  新主机名
         * @param syncHosts 是否同步更新hosts文件
         * @return 操作结果
         */
        Result updateHostname(Integer clusterId, String ip, String hostname, boolean syncHosts);

        /**
         * 更新hosts文件内容
         * 
         * @param clusterId        集群ID
         * @param ip               主机IP
         * @param hostsFileContent hosts文件新内容
         * @return 操作结果
         */
        Result updateHostsFile(Integer clusterId, String ip, String hostsFileContent);

        /**
         * 生成hosts文件预览
         *
         * @param clusterId 集群ID
         * @return 包含所有主机名和IP的预览内容
         */
        Result generateHostsFilePreview(Integer clusterId);

        /**
         * 生成hosts文件预览（带分页）
         *
         * @param clusterId 集群ID
         * @param page      当前页码，从1开始
         * @param pageSize  每页显示数量
         * @return 包含当前页主机名和IP的预览内容
         */
        Result generateHostsFilePreview(Integer clusterId, Integer page, Integer pageSize);

        /**
         * 同步hosts文件到所有主机
         * 
         * @param clusterId 集群ID
         * @return 操作结果
         */
        Result syncHostsFile(Integer clusterId);

        /**
         * 批量设置主机名
         *
         * @param clusterId 集群ID
         * @param prefix    主机名前缀
         * @param zeroCount 中间0的位数
         * @param separator 分隔符
         * @param suffix    后缀
         * @return 操作结果
         */
        Result batchSetHostname(Integer clusterId, String prefix, Integer zeroCount, String separator, String suffix);

        /**
         * 获取任务进度
         * 
         * @param taskId 任务ID
         * @return 任务进度信息
         */
        Result getTaskProgress(String taskId);

        /**
         * 修复集群中所有主机的所有失败项
         * 
         * @param clusterId 集群ID
         * @return 操作结果
         */
        Result fixAllFailedItems(Integer clusterId);

        /**
         * 跳过集群中所有主机的所有失败项
         * 
         * @param clusterId 集群ID
         * @return 操作结果
         */
        Result skipAllFailedItems(Integer clusterId);
}