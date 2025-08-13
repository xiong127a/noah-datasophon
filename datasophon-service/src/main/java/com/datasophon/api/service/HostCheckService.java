package com.datasophon.api.service;

import com.datasophon.common.dto.FixCheckItemDto;
import com.datasophon.common.dto.TaskProgressDto;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.LogEntry;

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
        CheckItem executeCheckItem(HostInfo hostInfo, CheckItem checkItem, Long clusterId);

        /**
         * 修复指定检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemId    检查项ID
         * @return 修复是否成功
         */
        boolean fixCheckItem(Long clusterId, String ip, Integer itemId);

        /**
         * 修复指定检查项（支持跳过确认）
         * 
         * @param clusterId   集群ID
         * @param ip          主机IP
         * @param itemId      检查项ID
         * @param skipConfirm 是否跳过确认
         * @return 修复是否成功
         */
        boolean fixCheckItem(Long clusterId, String ip, Integer itemId, Boolean skipConfirm);

        /**
         * 获取检查项的确认信息
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemId    检查项ID
         * @return 检查项确认信息
         */
        FixCheckItemDto getCheckItemConfirmInfo(Long clusterId, String ip, Integer itemId);

        /**
         * 修复选中的检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemIds   检查项ID列表，逗号分隔
         * @return 修复是否成功
         */
        boolean fixSelectedCheckItems(Long clusterId, String ip, String itemIds);

        /**
         * 修复所有检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @return 修复是否成功
         */
        boolean fixAllCheckItems(Long clusterId, String ip);

        /**
         * 终止指定主机的检查任务
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @return 终止是否成功
         */
        boolean stopHostCheck(Long clusterId, String ip);

        /**
         * 终止指定主机的指定检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemId    检查项ID
         * @return 终止是否成功
         */
        boolean stopItemCheck(Long clusterId, String ip, Integer itemId);

        /**
         * 检查单个主机
         *
         * @param clusterId 集群ID
         * @param ip        主机IP
         */
        void checkSingleHost(Long clusterId, String ip);

        /**
         * 批量检查多个主机
         *
         * @param clusterId 集群ID
         * @param ips       主机IP列表
         * @return 批量检查是否成功
         */
        boolean batchCheckHosts(Long clusterId, List<String> ips);

        /**
         * 获取检查项的实时日志
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemId    检查项ID
         * @return 检查项日志列表
         */
        List<LogEntry> getCheckItemLog(Long clusterId, String ip, Integer itemId);

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
        List<LogEntry> getLog(Long clusterId, String ip, Integer itemId, String logType, String logLevel,
                        String filterMode);

        /**
         * 取消所有当前运行的检查任务
         * 
         * @return 取消是否成功
         */
        boolean cancelAllCheckTasks();

        /**
         * 重试指定的检查项
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param itemNames 检查项名称列表
         * @return 重试是否成功
         */
        boolean retryCheckItems(Long clusterId, String ip, List<String> itemNames);

        /**
         * 获取格式化后的HTML日志
         * 
         * @param clusterId  集群ID
         * @param ip         主机IP
         * @param itemId     检查项ID
         * @param logType    日志类型，支持 "all", "check", "fix"
         * @param logLevel   日志级别，支持 "DEBUG", "INFO", "WARN", "ERROR"
         * @param filterMode 筛选模式，"all"=全部日志, "exact"=精确级别, "min"=指定级别及以上
         * @return 格式化后的HTML日志内容
         */
        String getFormattedLog(Long clusterId, String ip, Integer itemId, String logType, String logLevel,
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
         * @return 跳过是否成功
         */
        boolean skipCheckItem(Long clusterId, String ip, Integer itemId);

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
        List<CheckItem> getHostCheckItems(String ip, Long clusterId);

        /**
         * 开始检查主机
         * 从缓存中获取主机列表并开始检查
         *
         * @param clusterId 集群ID
         * @return 检查是否成功启动
         */
        boolean startHostCheck(Long clusterId);

        /**
         * 更新主机名
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param hostname  新主机名
         * @return 更新是否成功
         */
        boolean updateHostname(Long clusterId, String ip, String hostname);

        /**
         * 更新主机名（可选是否同步hosts文件）
         * 
         * @param clusterId 集群ID
         * @param ip        主机IP
         * @param hostname  新主机名
         * @param syncHosts 是否同步更新hosts文件
         * @return 更新是否成功
         */
        boolean updateHostname(Long clusterId, String ip, String hostname, boolean syncHosts);

        /**
         * 更新hosts文件内容
         * 
         * @param clusterId        集群ID
         * @param ip               主机IP
         * @param hostsFileContent hosts文件新内容
         * @return 更新是否成功
         */
        boolean updateHostsFile(Long clusterId, String ip, String hostsFileContent);

        /**
         * 生成hosts文件预览
         *
         * @param clusterId 集群ID
         * @return hosts文件预览内容
         */
        String generateHostsFilePreview(Long clusterId);

        /**
         * 生成hosts文件预览（带分页）
         *
         * @param clusterId 集群ID
         * @param page      当前页码，从1开始
         * @param pageSize  每页显示数量
         * @return hosts文件预览内容
         */
        String generateHostsFilePreview(Long clusterId, Integer page, Integer pageSize);

        /**
         * 同步hosts文件到所有主机
         * 
         * @param clusterId 集群ID
         * @return 同步是否成功
         */
        boolean syncHostsFile(Long clusterId);

        /**
         * 批量设置主机名
         *
         * @param clusterId 集群ID
         * @param prefix    主机名前缀
         * @param zeroCount 中间0的位数
         * @param separator 分隔符
         * @param suffix    后缀
         * @return 设置是否成功
         */
        boolean batchSetHostname(Long clusterId, String prefix, Integer zeroCount, String separator, String suffix);

        /**
         * 获取任务进度
         * 
         * @param taskId 任务ID
         * @return 任务进度信息
         */
        TaskProgressDto getTaskProgress(String taskId);

        /**
         * 修复集群中所有主机的所有失败项
         * 
         * @param clusterId 集群ID
         * @return 修复是否成功
         */
        boolean fixAllFailedItems(Long clusterId);

        /**
         * 跳过集群中所有主机的所有失败项
         * 
         * @param clusterId 集群ID
         * @return 跳过是否成功
         */
        boolean skipAllFailedItems(Long clusterId);
}