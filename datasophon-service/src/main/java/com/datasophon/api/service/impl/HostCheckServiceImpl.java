package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.service.HostCheckService;
import com.datasophon.api.service.checker.AsyncCheckService;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.common.LogEntryManager;
import com.datasophon.api.service.checker.config.TaskManager;
import com.datasophon.api.service.checker.core.ItemChecker;
import com.datasophon.api.service.checker.core.ItemCheckerFactory;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.api.service.checker.queue.HostCheckQueueManager;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.LogEntry;
import com.datasophon.common.model.LogResponse;
import com.datasophon.common.model.LogStats;
import com.datasophon.common.model.OsInfo;
import com.datasophon.common.model.hardware.DnsInfo;
import com.datasophon.common.utils.HostUtils;
import com.datasophon.common.utils.Result;
import lombok.Data;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 主机检查服务实现类
 */
@Service("hostCheckService")
public class HostCheckServiceImpl implements HostCheckService {
    // 静态常量，日志相关
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(HostCheckServiceImpl.class);
    private static final String CHECK_TASK_STATUS_PREFIX = "CHECK_TASK_STATUS_";

    @Autowired
    private ItemCheckerFactory itemCheckerFactory;

    @Autowired
    private HostCheckQueueManager hostCheckQueueManager;

    @Autowired
    private AsyncCheckService asyncCheckService;


    @Qualifier("checkExecutor")
    @Autowired
    private ExecutorService checkExecutor;

    @Autowired
    private TaskManager taskManager;


    /**
     * 任务执行状态枚举
     */
    public enum TaskStatus {
        /**
         * 进行中
         */
        IN_PROGRESS,
        /**
         * 已完成
         */
        COMPLETED,
        /**
         * 失败
         */
        FAILED
    }

    /**
     * 任务进度信息
     */
    @Data
    public static class TaskProgress {
        /**
         * 任务ID
         */
        private String taskId;
        /**
         * 任务状态
         */
        private TaskStatus status;
        /**
         * 已完成主机IP列表
         */
        private List<String> completedHosts;
        /**
         * 处理中的主机IP
         */
        private String currentHost;
        /**
         * 失败主机信息，IP -> 错误信息
         */
        private Map<String, String> failedHosts;
        /**
         * 待处理主机IP列表
         */
        private List<String> pendingHosts;
        /**
         * 总主机数
         */
        private int totalHosts;
        /**
         * 已完成数量
         */
        private int completedCount;
        /**
         * 失败数量
         */
        private int failedCount;
        /**
         * 完成百分比
         */
        private int percentage;
        /**
         * 任务消息
         */
        private String message;
    }

    /**
     * 存储异步任务进度的缓存
     */
    private static final Map<String, TaskProgress> taskProgressMap = new ConcurrentHashMap<>();

    /**
     * 日志工厂，用于创建日志记录器
     */
    public static class LoggerFactory {
        /**
         * 创建检查项日志记录器
         */
        public static CheckLogger getLogger(HostCheckServiceImpl service, Integer clusterId, String hostname,
                Integer itemId) {
            String logKey = service.getLogKey(clusterId, hostname, itemId);
            return CheckLogger.createLogger(logKey, service.getClass().getSimpleName());
        }

        /**
         * 创建检查项日志记录器，使用自定义类名
         */
        public static CheckLogger getLogger(HostCheckServiceImpl service, Integer clusterId, String hostname,
                Integer itemId, String className) {
            String logKey = service.getLogKey(clusterId, hostname, itemId);
            return CheckLogger.createLogger(logKey, className);
        }

        /**
         * 创建检查日志记录器
         */
        public static CheckLogger getCheckLogger(HostCheckServiceImpl service, Integer clusterId, String hostname,
                Integer itemId) {
            String logKey = service.getLogKey(clusterId, hostname, itemId);
            return CheckLogger.createLogger(logKey, service.getClass().getSimpleName(), LogEntry.Type.CHECK);
        }
    }

    @Override
    public List<CheckItem> getHostCheckItems() {
        List<CheckItem> checkItems = new ArrayList<>();

        // 使用ItemCode中定义的顺序创建检查项
        for (ItemCode itemCode : ItemCode.values()) {
            checkItems.add(createCheckItem(itemCode.getSequence(), itemCode));
        }

        // 按照sequence排序
        checkItems.sort(Comparator.comparingInt(CheckItem::getId));

        return checkItems;
    }

    @Override
    public Result fixCheckItem(Integer clusterId, String hostname, Integer itemId) {
        return fixCheckItem(clusterId, hostname, itemId, false);
    }

    @Override
    public Result fixCheckItem(Integer clusterId, String hostname, Integer itemId, Boolean skipConfirm) {
        try {
            logger.info("收到修复检查项请求: clusterId={}, hostname={}, itemId={}, skipConfirm={}",
                    clusterId, hostname, itemId, skipConfirm);

            // 获取主机信息
            HostInfo hostInfo = getHostInfo(clusterId, hostname);
            if (hostInfo == null) {
                return Result.error("主机信息不存在");
            }

            // 查找指定ID的检查项
            CheckItem checkItem = findCheckItemById(hostInfo, itemId);
            if (checkItem == null) {
                return Result.error("检查项不存在");
            }

            // 如果检查项当前正在修复中，则返回错误
            if (checkItem.getStatus() == CheckItem.Status.FIXING) {
                return Result.error("检查项正在修复中，请稍后重试");
            }
            CheckLogger cacheLog = LoggerFactory.getLogger(this, clusterId, hostname, itemId);
            cacheLog.info("==== 开始修复: " + checkItem.getItemName() + " ====");
            cacheLog.info("时间: " + formatDateToChinese(new Date()));

            // 将修复任务提交到专用队列
            String taskId = hostCheckQueueManager.addFixTask(clusterId, hostInfo, checkItem);

            if (taskId != null) {
                // 更新状态为正在修复
                checkItem.setStatus(CheckItem.Status.FIXING);
                checkItem.setMessage("已加入修复队列，等待处理");

                // 更新主机状态
                hostInfo.calculateStatus();
                asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

                cacheLog.info("修复任务已提交到队列，任务ID: " + taskId);

                return Result.success("修复任务已提交");
            } else {
                cacheLog.error("修复任务提交失败，可能已在队列中");
                return Result.error("修复任务已存在或提交失败");
            }
        } catch (Exception e) {
            logger.error("提交修复任务失败", e);
            return Result.error("提交修复任务失败: " + e.getMessage());
        }
    }

    /**
     * 修复指定主机上的选中检查项（重载方法）
     *
     * @param clusterId  集群ID
     * @param hostInfo   主机信息对象
     * @param itemIdList 需要修复的检查项ID列表
     * @return 操作结果
     */
    public Result fixSelectedCheckItems(Integer clusterId, HostInfo hostInfo, List<Integer> itemIdList) {
        if (Objects.isNull(hostInfo)) {
            return Result.error("主机不存在");
        }

        List<CheckItem> itemsToFix = hostInfo.getCheckItems().stream()
                .filter(item -> itemIdList.contains(item.getId()) &&
                        item.getStatus().equals(CheckItem.Status.FAILED))
                .collect(Collectors.toList());

        if (itemsToFix.isEmpty()) {
            return Result.success("没有可修复的检查项");
        }

        try {
            // 设置主机状态为"修复中"
            hostInfo.setStatus(CheckItem.Status.FIXING);
            hostInfo.setMessage("正在修复中");

            // 将所有要修复的检查项状态设置为"修复中"
            for (CheckItem item : itemsToFix) {
                item.setStatus(CheckItem.Status.FIXING);
                item.setMessage("正在修复...");
            }

            // 立即更新缓存，使前端能看到状态变化
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

            // 使用doHostFix进行批量修复，实现SSH连接复用
            boolean success = doHostFix(clusterId, hostInfo, itemsToFix);

            // 修复完成后根据结果重新计算主机状态
            hostInfo.calculateStatus();
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

            if (success) {
                retriggerHostCheck(hostInfo, clusterId);
                return Result.success("修复成功");
            } else {
                return Result.error("部分检查项修复失败");
            }
        } catch (Exception e) {
            logger.error("修复选中检查项失败", e);

            // 发生异常时，重新计算主机状态
            hostInfo.calculateStatus();
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

            return Result.error("修复失败: " + e.getMessage());
        }
    }

    /**
     * 一键修复所有失败项
     */
    @Override
    public Result fixAllFailedItems(Integer clusterId) {
        Map<String, HostInfo> hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        if (hostMap.isEmpty()) {
            return Result.error("集群未找到或无主机信息");
        }

        // 统计每个主机的失败检查项，同时更新状态为等待修复
        Map<String, List<CheckItem>> failedItemsMap = new HashMap<>();
        for (Map.Entry<String, HostInfo> entry : hostMap.entrySet()) {
            String ip = entry.getKey();
            HostInfo hostInfo = entry.getValue();

            if (hostInfo.getCheckItems() != null) {
                List<CheckItem> failedItems = new ArrayList<>();

                // 找出失败项并更新状态
                for (CheckItem item : hostInfo.getCheckItems()) {
                    if (item.getStatus() == CheckItem.Status.FAILED) {
                        // 更新状态为等待修复
                        item.setStatus(CheckItem.Status.WAITING_FIX);
                        item.setMessage("等待修复...");
                        failedItems.add(item);
                    }
                }

                if (!failedItems.isEmpty()) {
                    failedItemsMap.put(ip, failedItems);
                    // 更新主机状态
                    hostInfo.calculateStatus();
                    // 立即更新缓存，使前端能看到状态变化
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
                }
            }
        }

        if (failedItemsMap.isEmpty()) {
            return Result.success("没有需要修复的检查项");
        }
        if (hostMap.isEmpty()) {
            return Result.error("集群未找到或无主机信息");
        }

        for (Map.Entry<String, HostInfo> entry : hostMap.entrySet()) {
            String ip = entry.getKey();
            HostInfo hostInfo = entry.getValue();

            if (hostInfo.getCheckItems() != null) {
                List<CheckItem> failedItems = hostInfo.getCheckItems().stream()
                        .filter(item -> item.getStatus() == CheckItem.Status.FAILED)
                        .collect(Collectors.toList());

                if (!failedItems.isEmpty()) {
                    failedItemsMap.put(ip, failedItems);
                }
            }
        }

        if (failedItemsMap.isEmpty()) {
            return Result.success("没有需要修复的检查项");
        }

        // 按IP排序
        List<String> sortedIps = HostUtils.sortIpAddresses(new ArrayList<>(failedItemsMap.keySet()));
        logger.info("需要修复的主机列表(已排序): {}", sortedIps);

        // 修复每个主机的失败项
        fixFailedItemsByHost(clusterId, hostMap, failedItemsMap, sortedIps);

        return Result.success("已成功启动对" + sortedIps.size() + "个主机的修复任务");
    }

    /**
     * 修复指定主机的失败检查项
     */
    private void fixFailedItemsByHost(Integer clusterId, Map<String, HostInfo> hostMap,
            Map<String, List<CheckItem>> failedItemsMap, List<String> sortedIps) {
        for (String ip : sortedIps) {
            HostInfo hostInfo = hostMap.get(ip);
            List<CheckItem> failedItems = failedItemsMap.get(ip);

            // 更新主机状态
            hostInfo.calculateStatus();
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

            // 执行修复
            fixWaitingItems(clusterId, ip, hostInfo, failedItems);
        }
    }

    /**
     * 修复指定的等待修复状态的检查项
     *
     * @param clusterId  集群ID
     * @param ip         主机IP
     * @param hostInfo   主机信息
     * @param itemsToFix 需要修复的检查项列表
     */
    private void fixWaitingItems(Integer clusterId, String ip, HostInfo hostInfo, List<CheckItem> itemsToFix) {
        try {
            // 设置主机状态为"修复中"
            hostInfo.setStatus(CheckItem.Status.FIXING);
            hostInfo.setMessage("正在修复中");

            // 将所有要修复的检查项状态设置为"修复中"
            for (CheckItem item : itemsToFix) {
                item.setStatus(CheckItem.Status.FIXING);
                item.setMessage("正在修复...");
            }

            // 立即更新缓存，使前端能看到状态变化
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

            // 使用doHostFix进行批量修复，实现SSH连接复用
            boolean success = doHostFix(clusterId, hostInfo, itemsToFix);

            // 修复完成后根据结果重新计算主机状态
            hostInfo.calculateStatus();
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

            if (success) {
                // 修复成功后触发主机重新检查
                retriggerHostCheck(hostInfo, clusterId);
                logger.info("主机 {} 的检查项修复成功", ip);
            } else {
                logger.error("主机 {} 的部分检查项修复失败", ip);
            }
        } catch (Exception e) {
            logger.error("修复主机 {} 的检查项失败: {}", ip, e.getMessage(), e);

            // 发生异常时，重新计算主机状态
            hostInfo.calculateStatus();
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
        }
    }

    @Override
    public Result fixAllCheckItems(Integer clusterId, String ip) {
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

        HostInfo hostInfo = map.get(ip);
        if (Objects.isNull(hostInfo)) {
            return Result.error("主机不存在");
        }

        try {
            boolean allSuccess = true;
            for (CheckItem item : hostInfo.getCheckItems()) {
                if (item.getStatus().equals(CheckItem.Status.FAILED)) {
                    if (doFix(clusterId, hostInfo, item)) {
                        allSuccess = false;
                        break;
                    }
                }
            }

            if (allSuccess) {
                retriggerHostCheck(hostInfo, clusterId);
                return Result.success();
            } else {
                return Result.error("部分检查项修复失败");
            }
        } catch (Exception e) {
            logger.error("修复所有检查项失败", e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }

    /**
     * 检查单个主机
     *
     * @param clusterId 集群ID
     * @param hostname  主机名
     */
    @Override
    public void checkSingleHost(Integer clusterId, String hostname) {
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        if (!map.containsKey(hostname)) {
            logger.error("找不到主机: {}", hostname);
            return;
        }

        HostInfo hostInfo = map.get(hostname);

        // 检查主机是否已经在检查中，如果是则直接返回，避免重复检查
        if (hostInfo.getStatus() != null && CheckItem.Status.CHECKING.equals(hostInfo.getStatus())) {
            logger.info("主机 {} 正在检查中，跳过本次检查请求", hostname);
            return;
        }

        // 清空该主机的检查项状态
        List<CheckItem> checkItems = getHostCheckItems();
        for (CheckItem item : checkItems) {
            // 清除该检查项的旧日志
            String logKey = getLogKey(clusterId, hostname, item.getId());
            LogEntryManager.clearLogEntries(logKey);

            // 将所有检查项状态设置为"等待检查" - 第一个检查项会在processHostCheck中更新为"检查中"
            item.setStatus(CheckItem.Status.WAITING);
            item.setMessage("等待检查...");
        }
        hostInfo.setCheckItems(checkItems);

        // 设置主机状态为"检查中"
        hostInfo.setStatus(CheckItem.Status.CHECKING);
        hostInfo.setMessage("正在检查中");

        // 立即更新缓存，使前端能立即看到状态变化
        map.put(hostname, hostInfo);
        CacheUtils.put(clusterId + Constants.HOST_MAP, map);

        // 将检查任务添加到队列
        hostCheckQueueManager.addCheckTask(clusterId, hostInfo, this);
    }

    /**
     * 执行主机检查流程
     * 包级别访问权限，允许队列管理器调用
     */
    public void processHostCheck(Integer clusterId, HostInfo hostInfo) {
        try {
            logger.info("开始检查主机: {}", hostInfo.getIp());

            // 记录当前线程名称和检查项总数
            logger.debug("检查线程: {}, 主机: {}, 集群ID: {}",
                    Thread.currentThread().getName(),
                    hostInfo.getIp(),
                    clusterId);

            // 获取需要检查的项
            List<CheckItem> checkItems = new ArrayList<>(hostInfo.getCheckItems());
            if (checkItems.isEmpty()) {
                logger.warn("主机 {} 没有可执行的检查项，检查提前结束", hostInfo.getIp());
                return;
            }

            // 将所有检查项状态设置为"等待检查"
            for (CheckItem item : checkItems) {
                if (item.getStatus() != CheckItem.Status.SUCCESS && item.getStatus() != CheckItem.Status.SKIPPED) {
                    item.setStatus(CheckItem.Status.WAITING);
                    item.setMessage("等待检查");
                }
            }

            // 过滤出需要执行的检查项（状态为WAITING的项）
            List<CheckItem> itemsToCheck = checkItems.stream()
                    .filter(item -> item.getStatus() == CheckItem.Status.WAITING)
                    .collect(Collectors.toList());

            if (itemsToCheck.isEmpty()) {
                logger.info("主机 {} 没有需要执行的检查项，检查提前结束", hostInfo.getIp());
                asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
                return;
            }

            // 将第一个检查项状态设置为"检查中"
            CheckItem firstItem = itemsToCheck.getFirst();
            firstItem.setStatus(CheckItem.Status.CHECKING);
            firstItem.setMessage("正在检查中");

            // 立即更新缓存，使前端能看到状态变化
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

            // 使用新增的doHostCheck方法进行批量检查，实现SSH连接复用
            doHostCheck(clusterId, hostInfo, itemsToCheck);

            logger.info("主机 {} 的所有检查项执行完成", hostInfo.getIp());

        } catch (Exception e) {
            logger.error("检查主机 {} 时发生错误: {}", hostInfo.getIp(), e.getMessage(), e);
            // 将所有检查项设置为失败
            if (hostInfo.getCheckItems() != null) {
                for (CheckItem item : hostInfo.getCheckItems()) {
                    if (item.getStatus() != CheckItem.Status.SUCCESS && item.getStatus() != CheckItem.Status.SKIPPED) {
                        hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.FAILED,
                                "检查过程中发生错误: " + e.getMessage());
                    }
                }
                asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
            }
        }
    }

    /**
     * 异步执行检查项，返回结果为是否成功
     */
    private void executeHostCheck(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            logger.info("准备串行执行检查项: {}，主机: {}, 线程: {}",
                    checkItem.getItemName(),
                    hostInfo.getIp(),
                    Thread.currentThread().getName());

            // 确保状态更新已发送到前端
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

            // 初始化日志
            String logKey = getLogKey(clusterId, hostInfo.getIp(), checkItem.getId());
            logger.debug("检查项日志键: {}", logKey);

            // 清除可能存在的旧日志
            LogEntryManager.clearLogEntries(logKey);

            // 创建日志记录器
            CheckLogger cacheLog = LoggerFactory.getLogger(this, clusterId, hostInfo.getIp(), checkItem.getId());

            // 添加日志头部信息
            cacheLog.info("==== 开始检查: " + checkItem.getItemName() + " ====");
            cacheLog.info("主机: " + hostInfo.getIp());
            cacheLog.info("检查项ID: " + checkItem.getId());
            cacheLog.debug("检查项代码: " + checkItem.getItemCode());
            cacheLog.debug("集群ID: " + clusterId);
            cacheLog.debug("执行时间: " + formatDateToChinese(new Date()));
            cacheLog.debug("执行线程: " + Thread.currentThread().getName());

            logger.debug("主机 {} 开始执行检查项 {}", hostInfo.getIp(), checkItem.getItemName());
            cacheLog.info("正在执行检查任务...");

            // 从工厂获取检查器实例
            try {
                // 注意检查ItemCode的转换过程
                ItemCode itemCodeEnum;
                try {
                    itemCodeEnum = ItemCode.valueOf(checkItem.getItemCode());
                } catch (IllegalArgumentException e) {
                    String errorMsg = "无法将字符串 '" + checkItem.getItemCode() + "' 转换为ItemCode枚举";
                    logger.error(errorMsg, e);
                    cacheLog.error(errorMsg);
                    cacheLog.error("详细错误: " + e.getMessage());
                    hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, errorMsg);
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
                    return;
                }

                logger.debug("正在获取检查器: {}", itemCodeEnum);
                cacheLog.debug("正在获取检查器: " + itemCodeEnum);

                ItemChecker checker = itemCheckerFactory.getChecker(itemCodeEnum);

                if (checker == null) {
                    String errorMsg = "未找到检查项 " + checkItem.getItemCode() + " 对应的检查器实现";
                    logger.error(errorMsg);
                    cacheLog.error(errorMsg);
                    hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, errorMsg);
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
                    return;
                }

                logger.debug("成功获取检查器: {}, 类型: {}",
                        itemCodeEnum,
                        checker.getClass().getSimpleName());
                cacheLog.debug("成功获取检查器: " + checker.getClass().getSimpleName());

                // 执行检查，并捕获所有可能的异常
                try {
                    // 日志记录检查开始时间
                    long startTime = System.currentTimeMillis();
                    cacheLog.info("检查开始执行...");

                    // 执行实际检查
                    CheckItem resultItem = checker.check(clusterId, hostInfo, checkItem);

                    // 记录执行耗时
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    cacheLog.info("检查执行完成，耗时: " + duration + "ms");

                    // 根据检查结果更新状态
                    boolean result = (resultItem.getStatus() == CheckItem.Status.SUCCESS);
                    if (result) {
                        logger.info("检查项 {} 检查通过", checkItem.getItemName());
                        cacheLog.info("检查结果: 通过");
                        // 状态已经由check方法更新，不需要再次更新
                    } else {
                        logger.info("检查项 {} 检查未通过", checkItem.getItemName());
                        cacheLog.info("检查结果: 未通过");

                        // 确保状态已更新
                        if (resultItem.getStatus() != CheckItem.Status.FAILED &&
                                resultItem.getStatus() != CheckItem.Status.SKIPPED) {
                            hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, "检查未通过");
                        }
                    }

                    // 更新缓存，确保前端能看到最新状态
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

                } catch (Exception e) {
                    String errorMsg = "执行检查项 " + checkItem.getItemName() + " 时发生异常: " + e.getMessage();
                    logger.error(errorMsg, e);
                    cacheLog.error(errorMsg);
                    cacheLog.error("异常堆栈: " + getStackTraceAsString(e));

                    hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED,
                            "执行检查时发生异常: " + e.getMessage());
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
                }
            } catch (Exception e) {
                String errorMsg = "处理检查项 " + checkItem.getItemName() + " 的检查器时发生异常: " + e.getMessage();
                logger.error(errorMsg, e);
                cacheLog.error(errorMsg);
                cacheLog.error("异常堆栈: " + getStackTraceAsString(e));

                hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, "系统错误: " + e.getMessage());
                asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
            }
        } catch (Exception e) {
            logger.error("执行检查失败: {}", e.getMessage(), e);

            checkItem.setMessage("检查失败: " + e.getMessage());
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

            // 记录异常信息
            CheckLogger cacheLog = LoggerFactory.getLogger(this, clusterId, hostInfo.getIp(), checkItem.getId());
            cacheLog.error("执行检查时发生异常: " + e.getMessage());
            cacheLog.error("异常堆栈: " + getStackTraceAsString(e));
            cacheLog.error("==== 检查失败 ====");

        }
    }

    @Override
    public Result stopHostCheck(Integer clusterId, String ip) {
        // 获取主机信息
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        if (StrUtil.equals(ip, "-1")) {
            // 统计有检查项处于检查中或等待检查状态的主机数量
            int stoppedCount = 0;
            Set<String> ips = map.keySet();

            for (String s : ips) {
                HostInfo hostInfo = map.get(s);
                // 检查该主机是否有检查中或等待检查的项目
                boolean hasCheckingItems = hostInfo.getCheckItems().stream()
                        .anyMatch(item -> item.getStatus() == CheckItem.Status.CHECKING
                                || item.getStatus() == CheckItem.Status.WAITING);

                if (hasCheckingItems) {
                    // 终止该主机的检查
                    stopCheck(clusterId, s, map);
                    stoppedCount++;
                }
            }

            return Result.success("已成功终止" + stoppedCount + "个主机的检查任务");
        } else {
            return stopCheck(clusterId, ip, map);
        }
    }

    private Result stopCheck(Integer clusterId, String ip, Map<String, HostInfo> map) {
        try {
            HostInfo hostInfo = map.get(ip);
            if (Objects.isNull(hostInfo)) {
                return Result.error("主机不存在");
            }

            // 保存原始操作系统信息，避免丢失
            OsInfo originalOsInfo = hostInfo.getOsInfo();
            OsInfoStatusEnum originalOsInfoStatus = hostInfo.getOsInfoStatus();
            OsInfoStatusEnum originalSshConnectStatus = hostInfo.getSshConnectStatus();
            String originalHostname = hostInfo.getHostname();
            String originalFqdn = hostInfo.getFqdn();

            // 取消队列中的整个主机检查任务
            hostCheckQueueManager.cancelTask(clusterId, ip);

            // 将该主机的所有检查项状态设为已跳过
            // 使用批量更新提高性能
            Map<Integer, CheckItem.Status> updates = new HashMap<>();
            for (CheckItem item : hostInfo.getCheckItems()) {
                if (item.getStatus() == CheckItem.Status.CHECKING || item.getStatus() == CheckItem.Status.WAITING) {
                    updates.put(item.getId(), CheckItem.Status.SKIPPED);
                    item.setMessage("已手动跳过此检查项");
                }
            }

            if (!updates.isEmpty()) {
                hostInfo.batchUpdateCheckItems(updates);
            }

            // 确保操作系统信息不丢失
            if (hostInfo.getOsInfo() == null && originalOsInfo != null) {
                hostInfo.setOsInfo(originalOsInfo);
            }
            if (hostInfo.getOsInfoStatus() == null && originalOsInfoStatus != null) {
                hostInfo.setOsInfoStatus(originalOsInfoStatus);
            }
            if (hostInfo.getSshConnectStatus() == null && originalSshConnectStatus != null) {
                hostInfo.setSshConnectStatus(originalSshConnectStatus);
            }
            if (hostInfo.getHostname() == null && originalHostname != null) {
                hostInfo.setHostname(originalHostname);
            }
            if (hostInfo.getFqdn() == null && originalFqdn != null) {
                hostInfo.setFqdn(originalFqdn);
            }

            // 更新缓存
            map.put(ip, hostInfo);
            CacheUtils.put(clusterId + Constants.HOST_MAP, map);

            return Result.success("主机检查已终止");
        } catch (Exception e) {
            logger.error("终止主机检查失败: ", e);
            return Result.error("终止主机检查失败: " + e.getMessage());
        }
    }

    @Override
    public Result stopItemCheck(Integer clusterId, String hostname, Integer itemId) {
        try {
            // 获取主机信息
            Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

            HostInfo hostInfo = map.get(hostname);
            if (Objects.isNull(hostInfo)) {
                return Result.error("主机不存在");
            }

            // 查找检查项
            CheckItem checkItem = findCheckItemById(hostInfo, itemId);
            if (checkItem == null) {
                return Result.error("检查项不存在");
            }

            // 如果检查项正在检查中，取消任务
            if (checkItem.getStatus() == CheckItem.Status.CHECKING) {
                // 首先更新状态为"终止中"
                logger.info("检查项状态更新为终止中: clusterId={}, hostname={}, itemId={}",
                        clusterId, hostname, itemId);

                hostInfo.updateCheckItemStatus(
                        itemId,
                        CheckItem.Status.TERMINATING,
                        "正在终止检查...");

                // 更新缓存，使前端立即看到"终止中"状态
                map.put(hostname, hostInfo);
                CacheUtils.put(clusterId + Constants.HOST_MAP, map);

                // 在串行模式下，我们需要依赖线程中断机制来取消正在执行的检查项
                // 调用队列管理器的取消方法，它需要负责中断正在执行检查的线程
                hostCheckQueueManager.cancelItemTask(clusterId, hostname, itemId);

                // 等待一小段时间，让前端有时间显示"终止中"状态
                // 同时给任务足够时间响应中断请求
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                logger.info("已请求终止检查项：clusterId={}, hostname={}, itemId={}",
                        clusterId, hostname, itemId);
            }

            // 无论如何，都将检查项状态设为已跳过
            boolean updated = hostInfo.updateCheckItemStatus(
                    itemId,
                    CheckItem.Status.SKIPPED,
                    "已手动跳过此检查项");

            if (!updated) {
                return Result.error("检查项状态更新失败");
            }

            // 更新缓存
            map.put(hostname, hostInfo);
            CacheUtils.put(clusterId + Constants.HOST_MAP, map);

            return Result.success("成功终止检查项");
        } catch (Exception e) {
            logger.error("终止检查项失败", e);
            return Result.error("终止检查项失败: " + e.getMessage());
        }
    }

    private boolean doFix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {

        try {
            ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
            if (checker == null) {
                return true;
            }

            return !checker.fix(clusterId, hostInfo, checkItem);
        } catch (Exception e) {
            logger.error("执行修复失败", e);
            return true;
        }
    }

    private CheckItem createCheckItem(int id, ItemCode itemCode) {
        CheckItem item = new CheckItem();
        item.setId(id);
        item.setItemCode(itemCode.getCode());
        item.setItemName(itemCode.getName());
        item.setStatus(CheckItem.Status.WAITING);
        item.setMessage("等待检查");
        return item;
    }

    private void retriggerHostCheck(HostInfo hostInfo, Integer clusterId) {
        // 不再使用HostConnectActor，直接调用processHostCheck
        CompletableFuture.runAsync(() -> {
            try {
                // 重置所有检查项状态为"等待检查"
                if (hostInfo.getCheckItems() != null) {
                    for (CheckItem item : hostInfo.getCheckItems()) {
                        item.setStatus(CheckItem.Status.WAITING);
                        item.setMessage("等待检查");
                    }
                    // 通过检查项自动计算主机状态
                    hostInfo.calculateStatus();

                    // 更新缓存
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
                }

                // 执行检查
                processHostCheck(clusterId, hostInfo);
            } catch (Exception e) {
                logger.error("重新检查主机 {} 时发生错误: {}", hostInfo.getIp(), e.getMessage(), e);

                // 将所有检查项设置为失败
                if (hostInfo.getCheckItems() != null) {
                    for (CheckItem item : hostInfo.getCheckItems()) {
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("重新检查时发生错误: " + e.getMessage());
                    }
                    // 通过检查项自动计算主机状态
                    hostInfo.calculateStatus();

                    // 捕获到异常后更新缓存
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

                }
            }
        }, checkExecutor);
    }

    /**
     * 获取主机信息
     */
    private HostInfo getHostInfo(Integer clusterId, String hostname) {
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        return map.get(hostname);
    }

    /**
     * 根据ID查找检查项
     */
    private CheckItem findCheckItemById(HostInfo hostInfo, Integer itemId) {
        if (hostInfo == null || hostInfo.getCheckItems() == null) {
            return null;
        }

        return hostInfo.getCheckItems().stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 批量检查多个主机
     *
     * @param clusterId 集群ID
     * @param ips       主机IP列表
     * @return 操作结果
     */
    @Override
    public Result batchCheckHosts(Integer clusterId, List<String> ips) {
        if (clusterId == null) {
            return Result.error("集群ID不能为空");
        }

        if (ips == null || ips.isEmpty()) {
            return Result.error("主机列表不能为空");
        }

        // 取消所有当前运行的检查任务
        logger.info("收到批量检查请求，取消当前所有检查任务");
        hostCheckQueueManager.cancelAllTasks();

        // 等待短暂时间确保所有任务已终止
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 对IP地址进行排序
        ips = HostUtils.sortIpAddresses(ips);

        logger.info("开始执行新的批量检查，主机数量: {}, 排序后第一个IP: {}, 最后一个IP: {}",
                ips.size(),
                ips.isEmpty() ? "无" : ips.getFirst(),
                ips.isEmpty() ? "无" : ips.getLast());

        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

        List<String> successHosts = new ArrayList<>();
        List<String> failedHosts = new ArrayList<>();

        // 将IP转换为hostname的映射表
        Map<String, String> ipToHostnameMap = new HashMap<>();
        for (Map.Entry<String, HostInfo> entry : map.entrySet()) {
            HostInfo hostInfo = entry.getValue();
            if (hostInfo != null && hostInfo.getIp() != null) {
                ipToHostnameMap.put(hostInfo.getIp(), entry.getKey());
            }
        }

        for (String ip : ips) {
            try {
                // 通过IP查找对应的主机名
                String hostname = ipToHostnameMap.get(ip);

                if (hostname != null && map.containsKey(hostname)) {
                    HostInfo hostInfo = map.get(hostname);

                    // 将所有检查项重置为WAITING状态
                    if (hostInfo.getCheckItems() != null) {
                        for (CheckItem item : hostInfo.getCheckItems()) {
                            item.setStatus(CheckItem.Status.WAITING);
                            item.setMessage("等待检查");
                        }
                    }

                    // 更新缓存
                    map.put(hostname, hostInfo);

                    // 对单个主机启动检查
                    checkSingleHost(clusterId, hostname);
                    successHosts.add(ip);
                } else {
                    failedHosts.add(ip);
                    logger.warn("IP {} 不存在对应的主机或不在集群缓存中", ip);
                }
            } catch (Exception e) {
                logger.error("启动主机IP {} 检查失败: {}", ip, e.getMessage(), e);
                failedHosts.add(ip);
            }
        }

        // 更新主机信息缓存
        CacheUtils.put(clusterId + Constants.HOST_MAP, map);

        String message;
        if (failedHosts.isEmpty()) {
            message = "成功启动所有主机的检查";
        } else {
            message = String.format("成功启动 %d 台主机的检查, %d 台主机启动失败",
                    successHosts.size(), failedHosts.size());
        }

        return Result.success(message);
    }

    /**
     * 构建日志缓存键
     */
    private String getLogKey(Integer clusterId, String hostname, Integer itemId) {
        return "CHECK_ITEM_LOG_" + clusterId + "_" + hostname + "_" + itemId;
    }

    /**
     * 获取检查日志的缓存键
     */
    private String getCheckLogKey(Integer clusterId, String hostname, Integer itemId) {
        return "CHECK_LOG_" + clusterId + "_" + hostname + "_" + itemId;
    }

    @Override
    public Result getCheckItemLog(Integer clusterId, String hostname, Integer itemId) {
        // 构建检查日志缓存键
        String checkLogKey = getCheckLogKey(clusterId, hostname, itemId);
        logger.info("获取检查项日志, clusterId: {}, hostname: {}, itemId: {}, 缓存键: {}",
                clusterId, hostname, itemId, checkLogKey);

        // 从日志管理器获取检查日志
        String log = LogEntryManager.getLogContent(checkLogKey);

        if (log.isEmpty()) {
            logger.warn("未找到检查项日志, 缓存键: {}", checkLogKey);

            // 尝试获取检查项信息，验证检查项是否存在
            HostInfo hostInfo = getHostInfo(clusterId, hostname);
            if (hostInfo == null) {
                logger.warn("未找到主机: {}, clusterId: {}", hostname, clusterId);
                return Result.success("");
            }

            CheckItem checkItem = findCheckItemById(hostInfo, itemId);
            if (checkItem == null) {
                logger.warn("未找到检查项: {} 在主机: {}", itemId, hostname);
                return Result.success("");
            }

            // 检查项存在但没有日志，可能是日志尚未生成或已过期
            logger.info("检查项存在但未找到日志, 检查项: {}, 状态: {}, 消息: {}",
                    checkItem.getItemName(), checkItem.getStatus(), checkItem.getMessage());

            // 返回空字符串，不再生成假日志
            return Result.success("");
        }

        logger.info("成功获取检查项日志, 长度: {}", log.length());
        return Result.success(log);
    }

    /**
     * 取消所有当前运行的检查任务
     */
    @Override
    public Result cancelAllCheckTasks() {
        logger.info("收到取消所有检查任务请求");
        hostCheckQueueManager.cancelAllTasks();

        // 等待短暂时间确保所有任务已终止
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return Result.success("已成功取消所有检查任务");
    }

    /**
     * 重试指定的检查项
     */
    @Override
    public Result retryCheckItems(Integer clusterId, String hostname, List<String> itemNames) {
        if (clusterId == null) {
            return Result.error("集群ID不能为空");
        }

        if (StrUtil.isBlank(hostname)) {
            return Result.error("主机名不能为空");
        }

        if (itemNames == null || itemNames.isEmpty()) {
            return Result.error("检查项列表不能为空");
        }

        logger.info("收到重试检查项请求：主机 {}, 检查项 {}", hostname, itemNames);

        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        if (!map.containsKey(hostname)) {
            return Result.error("找不到主机：" + hostname);
        }

        HostInfo hostInfo = map.get(hostname);
        List<CheckItem> checkItems = hostInfo.getCheckItems();
        if (checkItems == null || checkItems.isEmpty()) {
            return Result.error("主机没有检查项");
        }

        boolean hasChanges = false;
        List<CheckItem> itemsToRetry = new ArrayList<>();

        for (String itemId : itemNames) {
            try {
                Integer id = Integer.parseInt(itemId);

                // 找到检查项
                Optional<CheckItem> optionalItem = checkItems.stream()
                        .filter(item -> item.getId().equals(id))
                        .findFirst();

                if (optionalItem.isPresent()) {
                    CheckItem item = optionalItem.get();

                    // 如果检查项正在检查中，则跳过
                    if (item.getStatus() == CheckItem.Status.CHECKING) {
                        logger.info("检查项 {} 正在检查中，跳过重试", item.getId());
                        continue;
                    }

                    // 清除旧日志
                    String logKey = getLogKey(clusterId, hostname, id);
                    LogEntryManager.clearLogEntries(logKey);

                    // 将检查项状态设置为等待检查
                    item.setStatus(CheckItem.Status.WAITING);
                    item.setMessage("等待检查");
                    itemsToRetry.add(item);
                    hasChanges = true;
                    logger.info("重置检查项 {}: {} 状态为等待检查", item.getId(), item.getItemName());
                } else {
                    logger.warn("未找到检查项ID: {}", id);
                }
            } catch (NumberFormatException e) {
                logger.warn("无效的检查项ID: {}", itemId);
            }
        }

        if (hasChanges) {
            // 更新缓存
            map.put(hostname, hostInfo);
            CacheUtils.put(clusterId + Constants.HOST_MAP, map);

            // 仅对指定的检查项进行重试
            // 注意：这里不能传入itemsToRetry，否则会导致其他检查项消失
            // 而是使用doHostCheckForItems直接处理这些项
            doHostCheckForItems(clusterId, hostInfo, itemsToRetry);

            return Result.success("成功添加" + itemsToRetry.size() + "个检查项到检查队列");
        } else {
            return Result.error("未找到需要重试的检查项");
        }
    }

    /**
     * 专门用于重试特定检查项的方法
     * 这个方法直接执行指定的检查项，而不修改主机的完整检查项列表
     */
    private void doHostCheckForItems(Integer clusterId, HostInfo hostInfo, List<CheckItem> itemsToCheck) {
        if (itemsToCheck == null || itemsToCheck.isEmpty()) {
            logger.warn("没有需要检查的项目");
            return;
        }

        logger.info("开始重试主机: {} 的 {} 个检查项", hostInfo.getIp(), itemsToCheck.size());

        // 使用线程池并行执行每个检查项
        for (CheckItem item : itemsToCheck) {
            // 使用线程池执行检查
            hostCheckQueueManager.getItemCheckExecutorService().submit(() -> {
                String originalThreadName = Thread.currentThread().getName();
                try {
                    // 设置线程名，方便日志识别
                    Thread.currentThread().setName("check-" + hostInfo.getIp() + "-item-" + item.getId());

                    logger.info("开始重试检查项: {}", item.getItemName());

                    // 标记为正在检查
                    item.setStatus(CheckItem.Status.CHECKING);
                    item.setMessage("正在检查中");

                    // 立即更新缓存，使前端能看到状态变化
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

                    // 执行检查
                    executeHostCheck(clusterId, hostInfo, item);

                    // 检查完成后再次更新缓存
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

                    logger.info("检查项 {} 重试完成", item.getItemName());
                } catch (Exception e) {
                    logger.error("检查项 {} 执行失败: {}", item.getItemName(), e.getMessage(), e);
                    // 设置状态为失败
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("检查失败: " + e.getMessage());
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
                } finally {
                    // 恢复线程名
                    Thread.currentThread().setName(originalThreadName);
                }
            });
        }
    }

    /**
     * 创建日志记录器
     * 此方法现在返回CheckLogger接口，而不是旧的ItemLogger
     *
     * @param clusterId 集群ID
     * @param hostname  主机名
     * @param itemId    检查项ID
     * @return 日志记录器
     */
    private CheckLogger createLogger(Integer clusterId, String hostname, Integer itemId) {
        return LoggerFactory.getLogger(this, clusterId, hostname, itemId);
    }

    // 添加格式化日期的工具方法
    private String formatDateToChinese(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * 获取异常堆栈的字符串表示
     */
    private String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return "无法获取详细堆栈: " + e.getMessage();
        }
    }

    @Override
    public CheckItem executeCheckItem(HostInfo hostInfo, CheckItem checkItem, Integer clusterId) {
        try {
            // 检查项日志键和运行状态键
            String logKey = getLogKey(clusterId, hostInfo.getIp(), checkItem.getId());
            String statusKey = CHECK_TASK_STATUS_PREFIX + clusterId + "_" + hostInfo.getIp() + "_"
                    + checkItem.getId();

            // 检查是否已经有任务在执行
            Boolean isRunning = (Boolean) CacheUtils.get(statusKey);
            if (isRunning != null && isRunning) {
                logger.warn("检查项已在执行中: {} - {}", hostInfo.getIp(), checkItem.getItemName());
                return checkItem; // 返回原检查项，表示已在执行中
            }

            // 创建日志记录器
            CheckLogger cacheLog = createLogger(clusterId, hostInfo.getIp(), checkItem.getId());

            // 清空之前的日志
            CacheUtils.put(logKey, "");

            // 添加日志头部
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());
            cacheLog.info("开始检查项: " + checkItem.getItemName());
            cacheLog.info("主机: " + hostInfo.getIp());
            cacheLog.info("检查项ID: " + checkItem.getId());
            cacheLog.info("开始时间: " + timestamp);
            cacheLog.info("===============================================");

            // 设置运行状态
            CacheUtils.put(statusKey, Boolean.TRUE);

            try {
                // 获取适合的检查器
                ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
                if (checker == null) {
                    cacheLog.error("未找到合适的检查器: " + checkItem.getItemCode());
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("配置错误：未找到合适的检查器");
                    return checkItem;
                }

                // 执行检查
                cacheLog.info("使用检查器: " + checker.getClass().getSimpleName());
                CheckItem resultItem = checker.check(clusterId, hostInfo, checkItem);

                // 记录检查结果
                cacheLog.info("检查完成，状态: " + resultItem.getStatus());
                cacheLog.info("检查结果: " + resultItem.getMessage());

                return resultItem;
            } catch (Exception e) {
                cacheLog.error("检查执行异常: " + e.getMessage());
                logger.error("检查执行异常", e);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("执行异常: " + e.getMessage());
                return checkItem;
            } finally {
                cacheLog.info("===============================================");
                cacheLog.info("检查项执行结束: " + checkItem.getItemName());
                cacheLog.info("结束时间: " + sdf.format(new Date()));
                cacheLog.info("===============================================");

                // 重置运行状态
                CacheUtils.put(statusKey, Boolean.FALSE);
            }
        } catch (Exception e) {
            logger.error("执行检查项时发生未预期的异常", e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("系统异常: " + e.getMessage());
            return checkItem;
        }
    }

    /**
     * 获取检查项日志（统一接口）
     * 整合了所有日志筛选功能
     *
     * @param clusterId  集群ID
     * @param hostname   主机名
     * @param itemId     检查项ID
     * @param logType    日志类型，支持 "all", "check", "fix"
     * @param logLevel   日志级别，支持 "DEBUG", "INFO", "WARN", "ERROR"
     * @param filterMode 筛选模式，"all"=全部日志, "exact"=精确级别, "min" - 指定级别及以上
     * @return 筛选后的LogEntry列表
     */
    @Override
    public List<LogEntry> getLog(Integer clusterId, String hostname, Integer itemId, String logType, String logLevel,
            String filterMode) {
        if (clusterId == null) {
            logger.error("获取日志失败：集群ID不能为空");
            return Collections.emptyList();
        }

        if (StrUtil.isBlank(hostname)) {
            logger.error("获取日志失败：主机名不能为空");
            return Collections.emptyList();
        }

        if (itemId == null) {
            logger.error("获取日志失败：检查项ID不能为空");
            return Collections.emptyList();
        }

        // 设置默认值
        if (logType == null || logType.isEmpty()) {
            logType = "all";
        }

        if (filterMode == null || filterMode.isEmpty()) {
            filterMode = "all";
        }

        logger.info("获取检查项日志, clusterId: {}, hostname: {}, itemId: {}, 日志类型: {}, 日志级别: {}, 筛选模式: {}",
                clusterId, hostname, itemId, logType, logLevel, filterMode);

        // 获取主机和检查项信息
        HostInfo hostInfo = getHostInfo(clusterId, hostname);
        CheckItem checkItem = hostInfo != null ? findCheckItemById(hostInfo, itemId) : null;

        if (checkItem == null) {
            logger.error("获取日志失败：未找到指定的检查项");
            return Collections.emptyList();
        }

        // 构建日志键 - 使用统一的日志键
        String logKey = getLogKey(clusterId, hostname, itemId);

        // 转换日志类型
        LogEntry.Type type = null;
        if (!"all".equals(logType)) {
            try {
                type = LogEntry.Type.valueOf(logType.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error("不支持的日志类型: {}", logType);
                return Collections.emptyList();
            }
        }

        // 转换日志级别
        LogEntry.Level level = null;
        if (!"all".equals(filterMode) && logLevel != null && !logLevel.isEmpty()) {
            try {
                level = LogEntry.Level.valueOf(logLevel.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error("不支持的日志级别: {}", logLevel);
                return Collections.emptyList();
            }
        }

        // 获取过滤后的日志条目
        List<LogEntry> logEntries = LogEntryManager.getFilteredLogEntries(logKey, type, level, filterMode);

        if (logEntries.isEmpty()) {
            // 如果没有找到日志，使用默认日志响应
            logger.warn("未找到任何日志, clusterId: {}, hostname: {}, itemId: {}",
                    clusterId, hostname, itemId);
            return Collections.emptyList();
        }

        logger.info("成功获取检查项日志, 条目数: {}", logEntries.size());
        return logEntries;
    }

    /**
     * 创建默认的日志响应（当没有找到日志时）
     */
    private Result createDefaultLogResponse(Integer clusterId, String hostname, Integer itemId) {
        // 记录日志，便于追踪
        logger.warn("未找到任何日志, clusterId: {}, hostname: {}, itemId: {}",
                clusterId, hostname, itemId);

        // 创建空的LogResponse对象
        LogResponse emptyResponse = new LogResponse();
        emptyResponse.setLogContent("<div class=\"empty-log\">暂无日志数据</div>");
        // 默认统计信息全为0，已在构造函数中初始化

        return Result.success(emptyResponse);
    }

    /**
     * 实现获取格式化后的HTML日志
     */
    @Override
    public Result getFormattedLog(Integer clusterId, String hostname, Integer itemId, String logType, String logLevel,
            String filterMode) {
        if (clusterId == null || hostname == null || itemId == null) {
            return Result.error("参数不能为空");
        }

        try {
            // 获取日志条目
            List<LogEntry> logEntries = getLog(clusterId, hostname, itemId, logType, logLevel, filterMode);
            if (logEntries == null || logEntries.isEmpty()) {
                // 如果没有日志，返回默认响应
                return createDefaultLogResponse(clusterId, hostname, itemId);
            }

            // 转换LogLevel和LogType (如果存在) - 恢复原始逻辑
            LogEntry.Level level = null;
            LogEntry.Type type = null;

            if (logLevel != null && !logLevel.isEmpty() && !"all".equals(logLevel)) {
                try {
                    level = LogEntry.Level.valueOf(logLevel.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warn("无效的日志级别: {}", logLevel);
                }
            }

            if (logType != null && !logType.isEmpty() && !"all".equals(logType)) {
                try {
                    type = LogEntry.Type.valueOf(logType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warn("无效的日志类型: {}", logType);
                }
            }

            // 统计各类日志数量
            int totalCount = logEntries.size();
            int errorCount = 0;
            int infoCount = 0;
            int warnCount = 0;
            int debugCount = 0;

            for (LogEntry entry : logEntries) {
                switch (entry.getLevel()) {
                    case ERROR:
                        errorCount++;
                        break;
                    case INFO:
                        infoCount++;
                        break;
                    case WARN:
                        warnCount++;
                        break;
                    case DEBUG:
                        debugCount++;
                        break;
                }
            }

            // 使用修改后的方法格式化日志（不包含统计信息）
            String formattedHtml = formatFilteredLogsToColoredHtmlWithoutStats(logEntries, type, level, filterMode);

            // 创建日志统计信息对象
            LogStats logStats = new LogStats(totalCount, errorCount, infoCount, warnCount, debugCount);

            // 使用LogResponse实体类而不是Map
            LogResponse logResponse = new LogResponse(formattedHtml, logStats);

            return Result.success(logResponse);
        } catch (Exception e) {
            logger.error("获取格式化日志失败", e);
            return Result.error("获取日志失败: " + e.getMessage());
        }
    }

    // 新增方法：格式化日志为HTML，但不包含统计信息部分
    private String formatFilteredLogsToColoredHtmlWithoutStats(List<LogEntry> logEntries,
            LogEntry.Type type, LogEntry.Level level, String filterMode) {

        StringBuilder html = new StringBuilder();

        // 开始容器，但不添加统计摘要部分
        html.append(
                "<div class=\"colored-log-container\" style=\"font-family: 'Consolas', 'Monaco', monospace; line-height: 1.5; padding: 16px; background-color: #fff; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);\">");

        // 过滤日志条目
        List<LogEntry> filteredEntries = filterLogEntries(logEntries, type, level, filterMode);

        // 添加日志条目
        for (LogEntry entry : filteredEntries) {
            html.append(formatLogEntryToHtml(entry));
        }

        html.append("</div>");
        return html.toString();
    }

    // 过滤日志条目
    private List<LogEntry> filterLogEntries(List<LogEntry> entries, LogEntry.Type type, LogEntry.Level level,
            String filterMode) {
        // 检查是否需要过滤
        if ((type == null || "ALL".equals(type.name())) &&
                ("all".equals(filterMode) || level == null || "ALL".equals(level.name()))) {
            return entries; // 不需要过滤
        }

        List<LogEntry> filtered = new ArrayList<>();
        for (LogEntry entry : entries) {
            // 按类型过滤
            if (type != null && !"ALL".equals(type.name()) && entry.getType() != type) {
                continue;
            }

            // 按级别过滤
            if (level != null && !"ALL".equals(level.name())) {
                if ("exact".equals(filterMode) && entry.getLevel() != level) {
                    continue;
                } else if ("min".equals(filterMode) && !entry.getLevel().isHigherOrEqual(level)) {
                    continue;
                }
            }

            filtered.add(entry);
        }

        return filtered;
    }

    // 格式化单个日志条目为HTML
    private String formatLogEntryToHtml(LogEntry entry) {
        String borderColor;
        String bgColor;
        String levelColor;

        // 根据日志级别设置样式
        switch (entry.getLevel()) {
            case ERROR:
                levelColor = "#f5222d";
                bgColor = "#fff1f0";
                borderColor = "#ffccc7";
                break;
            case WARN:
                levelColor = "#faad14";
                bgColor = "#fffbe6";
                borderColor = "#ffe58f";
                break;
            case INFO:
                levelColor = "#389e0d";
                bgColor = "#f8fff0";
                borderColor = "#b7eb8f";
                break;
            case DEBUG:
                levelColor = "#1890ff";
                bgColor = "#e6f7ff";
                borderColor = "#91d5ff";
                break;
            default:
                levelColor = "#595959";
                bgColor = "#f8f8f8";
                borderColor = "#d9d9d9";
        }

        StringBuilder entryHtml = new StringBuilder();

        // 日志条目容器
        entryHtml.append("<div class=\"log-entry\" style=\"margin-bottom: 8px; padding: 8px 12px; background-color: ")
                .append(bgColor)
                .append("; border-left: 3px solid ")
                .append(borderColor)
                .append(";\">");

        // 日志头部
        entryHtml.append(
                "<div class=\"log-header\" style=\"display: flex; justify-content: space-between; margin-bottom: 4px;\">");

        // 日志信息（时间戳、级别、线程）
        entryHtml.append("<div class=\"log-info\" style=\"display: flex; align-items: center; gap: 8px;\">");

        // 时间戳
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        entryHtml.append("<span class=\"log-time\" style=\"color: #595959;\">")
                .append(sdf.format(entry.getTimestamp()))
                .append("</span>");

        // 日志级别
        entryHtml.append("<span class=\"log-level\" style=\"color: ")
                .append(levelColor)
                .append("; font-weight: 500; padding: 1px 6px; border-radius: 2px; background-color: ")
                .append(bgColor)
                .append("; font-size: 12px;\">")
                .append(entry.getLevel())
                .append("</span>");

        // 线程信息
        entryHtml.append("<span class=\"log-thread\" style=\"color: #8c8c8c; font-size: 12px;\">")
                .append("[")
                .append(entry.getThreadName())
                .append("]</span>");

        entryHtml.append("</div>");

        // 源信息
        String source = entry.getClassName();
        if (entry.getLineNumber() > 0) {
            source += ":" + entry.getLineNumber();
        }

        if (source != null && !source.isEmpty()) {
            entryHtml.append("<div class=\"log-source\" style=\"color: #8c8c8c; font-size: 12px; cursor: pointer;\" ")
                    .append("data-clipboard=\"")
                    .append(source)
                    .append("\" onclick=\"copyToClipboard(this.getAttribute('data-clipboard'))\" ")
                    .append("title=\"点击复制\">")
                    .append(source)
                    .append("</div>");
        }

        entryHtml.append("</div>");

        // 日志内容
        entryHtml.append("<div class=\"log-content\" style=\"color: ")
                .append(levelColor)
                .append(";\">")
                .append(escapeHtml(entry.getMessage()))
                .append("</div>");

        entryHtml.append("</div>");

        return entryHtml.toString();
    }

    /**
     * HTML特殊字符转义
     */
    private String escapeHtml(String content) {
        if (content == null) {
            return "";
        }

        return content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 获取可用的日志级别
     *
     * @return 日志级别数组
     */
    @Override
    public LogEntry.Level[] getLogLevels() {
        return LogEntry.Level.values();
    }

    /**
     * 获取可用的日志类型
     *
     * @return 日志类型映射，key为类型编码，value为显示名称
     */
    @Override
    public Map<String, String> getLogTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("CHECK", "检查日志");
        types.put("FIX", "修复日志");
        return types;
    }

    /**
     * 跳过指定检查项
     *
     * @param clusterId 集群ID
     * @param hostname  主机名
     * @param itemId    检查项ID
     * @return 跳过结果
     */
    @Override
    public Result skipCheckItem(Integer clusterId, String hostname, Integer itemId) {
        // 获取主机信息
        HostInfo hostInfo = getHostInfo(clusterId, hostname);
        if (hostInfo == null) {
            return Result.error("找不到指定主机: " + hostname);
        }

        // 查找对应的检查项
        CheckItem checkItem = findCheckItemById(hostInfo, itemId);
        if (checkItem == null) {
            return Result.error("找不到检查项: " + itemId);
        }

        try {
            // 先尝试停止检查项如果正在运行
            stopItemCheck(clusterId, hostname, itemId);

            // 更新检查项状态为已跳过
            hostInfo.updateCheckItemStatus(itemId, CheckItem.Status.SKIPPED, "用户已跳过该检查项");

            // 更新缓存
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

            // 记录日志
            LoggerFactory.getCheckLogger(this, clusterId, hostname, itemId)
                    .info("用户跳过了该检查项");

            return Result.success("已跳过该检查项");
        } catch (Exception e) {
            logger.error("跳过检查项时出错: {}", e.getMessage(), e);
            return Result.error("跳过检查项失败: " + e.getMessage());
        }
    }

    @Override
    public Result getCheckItemConfirmInfo(Integer clusterId, String hostname, Integer itemId) {
        String message = "";
        try {

            ItemCode itemCode = ItemCode.getBySequence(itemId);
            // 封装结果返回
            if (itemCode != null) {
                return Objects.requireNonNull(Objects.requireNonNull(Result.success()
                        .put("needConfirm", itemCode.isNeedConfirm()))
                        .put("confirmMessage", itemCode.isNeedConfirm() ? itemCode.getConfirmMessage() : "确定要修复该检查项吗？"))
                        .put("itemName", itemCode.getName());
            }
        } catch (Exception e) {
            message = e.getMessage();
            logger.error("获取检查项确认信息失败: {}", message, e);

        }
        return Result.error("获取确认信息失败: " + message);
    }

    /**
     * 获取指定主机的检查项列表
     *
     * @param ip        主机IP
     * @param clusterId 集群ID
     * @return 指定主机的检查项列表
     */
    @Override
    public Result getHostCheckItems(String ip, Integer clusterId) {
        // 从缓存中获取指定主机的检查项
        Map<String, HostInfo> hostInfoMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

        // 根据IP查找主机信息
        HostInfo targetHost = null;
        for (HostInfo hostInfo : hostInfoMap.values()) {
            if (ip.equals(hostInfo.getIp())) {
                targetHost = hostInfo;
                break;
            }
        }

        if (targetHost == null) {
            return Result.error("找不到主机信息: " + ip);
        }

        return Result.success(targetHost.getCheckItems());
    }

    /**
     * 开始检查主机
     * 从缓存中获取主机列表并开始检查
     *
     * @param clusterId 集群ID
     * @return 操作结果
     */
    @Override
    public Result startHostCheck(Integer clusterId) {
        if (clusterId == null) {
            return Result.error("集群ID不能为空");
        }

        // 从缓存中获取主机列表
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        if (map.isEmpty()) {
            return Result.error("找不到集群的主机信息，请先解析主机列表");
        }

        // 筛选出需要检查的主机（未受管主机）
        List<String> ipsToCheck = map.values().stream()
                .map(HostInfo::getIp)
                .collect(Collectors.toList());

        if (ipsToCheck.isEmpty()) {
            return Result.error("没有需要检查的主机");
        }

        // 使用HostUtils中的统一排序方法对IP地址进行排序
        ipsToCheck = HostUtils.sortIpAddresses(ipsToCheck);

        logger.info("开始执行全局检查，未受管主机数量: {}, 排序后第一个IP: {}, 最后一个IP: {}",
                ipsToCheck.size(),
                ipsToCheck.isEmpty() ? "无" : ipsToCheck.getFirst(),
                ipsToCheck.isEmpty() ? "无" : ipsToCheck.getLast());

        // 调用批量检查方法执行检查
        return batchCheckHosts(clusterId, ipsToCheck);
    }

    /**
     * 批量执行主机修复
     * 使用SSH连接复用功能
     */
    public boolean doHostFix(Integer clusterId, HostInfo hostInfo, List<CheckItem> fixItems) {
        if (fixItems == null || fixItems.isEmpty()) {
            logger.info("没有需要修复的检查项");
            return true;
        }

        try {
            logger.info("使用SSH连接复用机制批量执行 {} 个修复项", fixItems.size());

            // 使用AsyncCheckService的批量执行方法执行所有修复，实现SSH连接复用
            List<CheckItem> results = asyncCheckService.batchExecuteFix(clusterId, hostInfo, fixItems);

            // 更新修复结果
            if (results != null && !results.isEmpty()) {
                boolean allSuccess = true;

                for (CheckItem result : results) {
                    // 在原始修复项列表中找到对应项并更新
                    for (CheckItem item : fixItems) {
                        if (item.getId().equals(result.getId())) {
                            // 复制状态和消息
                            item.setStatus(result.getStatus());
                            item.setMessage(result.getMessage());

                            // 检查是否成功
                            if (item.getStatus() != CheckItem.Status.SUCCESS) {
                                allSuccess = false;
                            }
                            break;
                        }
                    }
                }

                // 更新缓存
                asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
                logger.info("所有修复项执行完成，结果: {}", allSuccess ? "全部成功" : "部分失败");

                return allSuccess;
            } else {
                logger.error("批量执行修复项失败，结果为空");
                // 将所有修复项设置为失败
                for (CheckItem item : fixItems) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("执行修复时发生内部错误");
                }
                // 更新缓存
                asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
                return false;
            }
        } catch (Exception e) {
            logger.error("执行修复时发生错误: {}", e.getMessage(), e);
            // 将所有修复项设置为失败
            for (CheckItem item : fixItems) {
                item.setStatus(CheckItem.Status.FAILED);
                item.setMessage("执行修复时发生错误: " + e.getMessage());
            }
            // 更新缓存
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
            return false;
        }
    }

    /**
     * 批量执行主机检查
     * 使用SSH连接复用功能
     */
    public void doHostCheck(Integer clusterId, HostInfo hostInfo, List<CheckItem> checkItems) {
        if (checkItems == null || checkItems.isEmpty()) {
            logger.info("没有需要检查的项目");
            return;
        }

        try {
            logger.info("使用SSH连接复用机制批量执行 {} 个检查项", checkItems.size());

            // 使用AsyncCheckService的批量执行方法执行所有检查，实现SSH连接复用
            List<CheckItem> results = asyncCheckService.batchExecuteCheck(clusterId, hostInfo, checkItems);

            // 更新检查结果
            if (results != null && !results.isEmpty()) {
                for (CheckItem result : results) {
                    // 在原始检查项列表中找到对应项并更新
                    for (CheckItem item : checkItems) {
                        if (item.getId().equals(result.getId())) {
                            item.setStatus(result.getStatus());
                            item.setMessage(result.getMessage());
                            break;
                        }
                    }
                }
            } else {
                logger.error("批量执行检查失败，结果为空");
                // 将所有检查项设置为失败
                for (CheckItem item : checkItems) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("执行检查时发生内部错误");
                }
            }

            // 更新缓存
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
            logger.info("所有检查项执行完成");

        } catch (Exception e) {
            logger.error("执行检查时发生错误: {}", e.getMessage(), e);
            // 将所有检查项设置为失败
            for (CheckItem item : checkItems) {
                item.setStatus(CheckItem.Status.FAILED);
                item.setMessage("执行检查时发生错误: " + e.getMessage());
            }
            // 更新缓存
            asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
        }
    }

    /**
     * 更新主机名
     */
    @Override
    public Result updateHostname(Integer clusterId, String ip, String newHostname) {
        // 默认不同步hosts文件
        return updateHostname(clusterId, ip, newHostname, false);
    }

    /**
     * 更新主机名 - 支持可选的hosts文件同步
     *
     * @param clusterId   集群ID
     * @param ip          主机IP地址
     * @param newHostname 新主机名
     * @param syncHosts   是否同步更新hosts文件
     * @return 操作结果
     */
    @Override
    public Result updateHostname(Integer clusterId, String ip, String newHostname, boolean syncHosts) {
        return asyncCheckService.updateHostname(clusterId, ip, newHostname, syncHosts);
    }

    @Override
    public Result updateHostsFile(Integer clusterId, String ip, String hostsFileContent) {
        logger.info("开始更新hosts文件: clusterId={}, ip={}", clusterId, ip);

        // 校验hosts文件内容
        if (StrUtil.isBlank(hostsFileContent)) {
            return Result.error("hosts文件内容不能为空");
        }

        // 获取主机信息和SSH连接
        try {
            // 获取存储在缓存中的主机信息
            Map<String, HostInfo> hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
            if (!hostMap.containsKey(ip)) {
                return Result.error("主机不存在");
            }

            HostInfo hostInfo = hostMap.get(ip);

            // 建立SSH连接
            ClientSession session = null;
            try {
                session = MinaUtils.openConnectionWithPassword(hostInfo);
                if (session == null) {
                    return Result.error("无法连接到主机");
                }

                // 检查是否有sudo命令
                String checkSudoCmd = "which sudo || echo 'nosudo'";
                String checkSudoResult = MinaUtils.execCmdWithResult(session, checkSudoCmd);
                boolean hasSudo = !checkSudoResult.trim().contains("nosudo");
                logger.info("检查主机是否有sudo命令: {}", hasSudo ? "有" : "没有");

                // 根据是否有sudo命令决定使用的前缀
                String sudoPrefix = hasSudo ? "sudo " : "";

                // 创建备份目录
                String backupDir = "/opt/datasophon/backup/hosts";
                String createBackupDirCmd = sudoPrefix + "mkdir -p " + backupDir + " && " + sudoPrefix + "chmod 755 "
                        + backupDir;
                MinaUtils.execCmdWithResult(session, createBackupDirCmd);

                // 生成备份文件名（包含时间戳）
                String timestamp = MinaUtils.execCmdWithResult(session, "date +%Y%m%d_%H%M%S").trim();
                String hostname = hostInfo.getHostname();
                String backupFileName = String.format("%s/hosts_%s_%s.bak", backupDir, hostname, timestamp);

                // 备份当前hosts文件
                String backupCmd = sudoPrefix + "cp /etc/hosts " + backupFileName + " && " + sudoPrefix + "chmod 644 "
                        + backupFileName;
                MinaUtils.execCmdWithResult(session, backupCmd);
                logger.info("已备份hosts文件到: {}", backupFileName);

                // 读取当前hosts文件内容
                String getCurrentHostsCmd = "cat /etc/hosts";
                String currentHostsContent = MinaUtils.execCmdWithResult(session, getCurrentHostsCmd);
                logger.info("获取到当前hosts文件内容，长度：{}", currentHostsContent.length());

                // 定义标记，用于标识由系统管理的部分
                String startMark = "### BEGIN DATASOPHON MANAGED HOSTS ###";
                String endMark = "### END DATASOPHON MANAGED HOSTS ###";

                // 准备新的hosts文件内容
                StringBuilder newHostsContent = new StringBuilder();

                // 检查当前文件是否已经包含我们的标记
                if (currentHostsContent.contains(startMark) && currentHostsContent.contains(endMark)) {
                    // 文件已经包含我们的标记，替换这部分内容
                    int startIndex = currentHostsContent.indexOf(startMark);
                    int endIndex = currentHostsContent.indexOf(endMark) + endMark.length();

                    // 保留标记前的内容
                    newHostsContent.append(currentHostsContent, 0, startIndex);

                    // 添加我们的内容（包含标记）
                    newHostsContent.append(startMark).append("\n");
                    newHostsContent.append(hostsFileContent).append("\n");
                    newHostsContent.append(endMark);

                    // 如果标记后还有内容，也保留
                    if (endIndex < currentHostsContent.length()) {
                        newHostsContent.append(currentHostsContent.substring(endIndex));
                    }
                } else {
                    // 文件不包含我们的标记，追加到末尾
                    newHostsContent.append(currentHostsContent);

                    // 如果最后一行不是空行，添加一个空行
                    if (!currentHostsContent.endsWith("\n")) {
                        newHostsContent.append("\n");
                    }

                    // 再添加一个空行作为分隔
                    newHostsContent.append("\n");

                    // 添加我们的内容（包含标记）
                    newHostsContent.append(startMark).append("\n");
                    newHostsContent.append(hostsFileContent).append("\n");
                    newHostsContent.append(endMark).append("\n");
                }

                // 创建临时文件
                String tempFile = "/tmp/hosts_" + System.currentTimeMillis();
                // 这里需要注意特殊字符的处理，使用单引号包裹并转义内部的单引号
                String createTempCommand = "cat > " + tempFile + " << 'EOL'\n" +
                        newHostsContent +
                        "\nEOL";
                MinaUtils.execCmdWithResult(session, createTempCommand);

                // 使用sudo将临时文件复制到/etc/hosts
                String updateCommand;
                if (hasSudo) {
                    updateCommand = sudoPrefix + "cp " + tempFile + " /etc/hosts && " + sudoPrefix
                            + "chmod 644 /etc/hosts && rm " + tempFile;
                } else {
                    updateCommand = "cp " + tempFile + " /etc/hosts && chmod 644 /etc/hosts && rm " + tempFile;
                }
                String result = MinaUtils.execCmdWithResult(session, updateCommand);
                logger.info("执行命令结果: {}", result);

                // 更新主机信息中的hosts文件内容，使用DnsInfo对象
                if (hostInfo.getOsInfo() == null) {
                    hostInfo.setOsInfo(new OsInfo());
                }

                if (hostInfo.getOsInfo().getDnsInfo() == null) {
                    hostInfo.getOsInfo().setDnsInfo(new DnsInfo());
                }

                // 设置hosts文件内容到DnsInfo对象 - 这里只保存我们添加的部分
                hostInfo.getOsInfo().getDnsInfo().setHostsFileContent(hostsFileContent);

                // 设置DNS状态为成功
                hostInfo.getOsInfo().setDnsStatus(OsInfoStatusEnum.SUCCESS);

                // 更新主机信息缓存
                asyncCheckService.updateHostInfoCache(clusterId, hostInfo);

                // 立即刷新全局缓存
                asyncCheckService.updateHostMapInCache(clusterId);

                return Result.success("hosts文件已成功更新，备份文件: " + backupFileName);
            } finally {
                if (session != null && session.isOpen()) {
                    MinaUtils.closeConnection(session);
                }
            }
        } catch (Exception e) {
            logger.error("更新hosts文件时发生错误", e);
            return Result.error("更新hosts文件失败: " + e.getMessage());
        }
    }

    /**
     * Hosts文件条目类型
     */
    public enum HostsEntryType {
        /**
         * 注释
         */
        COMMENT,
        /**
         * IP映射
         */
        MAPPING
    }

    /**
     * Hosts文件条目
     */
    @Data
    public static class HostsEntry {
        /**
         * 条目类型：COMMENT(注释)或MAPPING(IP映射)
         */
        private HostsEntryType type;

        /**
         * 当type=COMMENT时，存储注释内容
         */
        private String comment;

        /**
         * 当type=MAPPING时，存储IP地址
         */
        private String ip;

        /**
         * 当type=MAPPING时，存储主机名列表
         */
        private List<String> hostnames;

        /**
         * 创建注释条目
         */
        public static HostsEntry createComment(String comment) {
            HostsEntry entry = new HostsEntry();
            entry.setType(HostsEntryType.COMMENT);
            entry.setComment(comment);
            return entry;
        }

        /**
         * 创建IP映射条目
         */
        public static HostsEntry createMapping(String ip, String... hostnames) {
            HostsEntry entry = new HostsEntry();
            entry.setType(HostsEntryType.MAPPING);
            entry.setIp(ip);
            entry.setHostnames(Arrays.asList(hostnames));
            return entry;
        }
    }

    /**
     * Hosts文件预览VO
     */
    @Data
    public static class HostsFilePreviewVO {
        /**
         * hosts文件条目列表
         */
        private List<HostsEntry> hostsEntries;

        /**
         * hosts文件内容（保留以兼容旧代码）
         */
        private String hostsContent;

        /**
         * 主机数量
         */
        private Integer hostCount;

        /**
         * 当前页显示的主机数
         */
        private Integer currentPageCount;

        /**
         * 根据条目列表生成hosts文件内容
         */
        public void generateHostsContent() {
            if (hostsEntries == null || hostsEntries.isEmpty()) {
                this.hostsContent = "";
                return;
            }

            StringBuilder builder = new StringBuilder();
            for (HostsEntry entry : hostsEntries) {
                if (entry.getType() == HostsEntryType.COMMENT) {
                    builder.append(entry.getComment()).append("\n");
                } else if (entry.getType() == HostsEntryType.MAPPING) {
                    builder.append(entry.getIp()).append("\t");
                    builder.append(String.join(" ", entry.getHostnames())).append("\n");
                }
            }
            this.hostsContent = builder.toString();
        }
    }

    /**
     * 内部方法，直接返回VO对象，不包装Result
     *
     * @param clusterId 集群ID
     * @param page      当前页码，从1开始
     * @param pageSize  每页显示数量
     * @return 预览VO对象
     */
    private HostsFilePreviewVO generateHostsFilePreviewInner(Integer clusterId, Integer page, Integer pageSize) {
        logger.info("开始生成hosts文件预览(内部): clusterId={}, page={}, pageSize={}", clusterId, page, pageSize);

        try {
            // 获取存储在缓存中的主机信息
            Map<String, HostInfo> hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
            if (hostMap.isEmpty()) {
                return null;
            }

            // 创建结构化的hosts条目列表
            List<HostsEntry> hostsEntries = new ArrayList<>();

            // 添加头部注释，不再添加localhost映射
            hostsEntries.add(HostsEntry.createComment("# Hosts file generated by Datasophon"));
            hostsEntries.add(HostsEntry.createComment(""));
            hostsEntries.add(HostsEntry.createComment("# Cluster hosts"));

            // 提取IP列表并使用HostUtils进行排序
            List<String> ipList = hostMap.values().stream()
                    .map(HostInfo::getIp)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());

            // 使用项目统一的IP排序方法
            List<String> sortedIps = HostUtils.sortIpAddresses(ipList);

            // 计算分页
            int totalHosts = sortedIps.size();
            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalHosts);

            // 如果起始索引超出范围，则返回空列表
            if (startIndex >= totalHosts) {
                startIndex = 0;
                endIndex = 0;
            }

            // 获取当前页的IP列表
            List<String> pagedIps = startIndex < endIndex ? sortedIps.subList(startIndex, endIndex) : new ArrayList<>();

            logger.info("主机总数: {}, 当前页显示: {} (从索引 {} 到 {})", totalHosts, pagedIps.size(), startIndex, endIndex - 1);

            // 按排序后的顺序添加主机映射
            for (String ip : pagedIps) {
                // 查找该IP对应的主机信息
                Optional<HostInfo> hostInfoOpt = hostMap.values().stream()
                        .filter(hi -> ip.equals(hi.getIp()))
                        .findFirst();

                if (hostInfoOpt.isPresent()) {
                    HostInfo hostInfo = hostInfoOpt.get();

                    String hostname = null;
                    // 添加空判断，避免空指针异常
                    if (hostInfo.getOsInfo() != null) {
                        hostname = hostInfo.getHostname();
                    }

                    // 添加逻辑：如果hostname为空，则生成error-xxx格式的默认名称
                    if (StrUtil.isBlank(hostname)) {
                        // 计算当前是第几个IP
                        int index = sortedIps.indexOf(ip) + 1;
                        // 生成error-001格式的主机名
                        hostname = "error-" + String.format("%03d", index);
                        logger.warn("主机 {} 没有主机名，使用默认名称: {}", ip, hostname);
                    }

                    hostsEntries.add(HostsEntry.createMapping(ip, hostname));
                }
            }

            // 创建返回实体
            HostsFilePreviewVO previewVO = new HostsFilePreviewVO();
            previewVO.setHostsEntries(hostsEntries);
            previewVO.setHostCount(hostMap.size()); // 总主机数
            previewVO.setCurrentPageCount(pagedIps.size()); // 当前页显示的主机数

            // 同时生成hostsContent字符串以兼容旧代码
            previewVO.generateHostsContent();

            return previewVO;
        } catch (Exception e) {
            logger.error("生成hosts文件预览时发生错误", e);
            return null;
        }
    }

    @Override
    public Result generateHostsFilePreview(Integer clusterId) {
        return generateHostsFilePreview(clusterId, 1, 10); // 默认第1页，每页10条
    }

    /**
     * 生成hosts文件预览（带分页）
     *
     * @param clusterId 集群ID
     * @param page      当前页码，从1开始
     * @param pageSize  每页显示数量
     * @return 操作结果
     */
    @Override
    public Result generateHostsFilePreview(Integer clusterId, Integer page, Integer pageSize) {
        logger.info("开始生成hosts文件预览: clusterId={}, page={}, pageSize={}", clusterId, page, pageSize);

        try {
            // 参数校验
            if (page == null || page < 1) {
                page = 1;
            }
            if (pageSize == null || pageSize < 1) {
                pageSize = 10;
            }

            // 调用内部方法获取VO
            HostsFilePreviewVO previewVO = generateHostsFilePreviewInner(clusterId, page, pageSize);
            if (previewVO == null) {
                return Result.error("未找到主机信息或生成预览失败");
            }

            // 返回包装好的对象
            return Result.success(previewVO);
        } catch (Exception e) {
            logger.error("生成hosts文件预览时发生错误", e);
            return Result.error("生成hosts文件预览失败: " + e.getMessage());
        }
    }

    @Override
    public Result syncHostsFile(Integer clusterId) {
        try {
            logger.info("开始同步hosts文件，集群ID：{}", clusterId);

            // 生成hosts文件预览内容
            HostsFilePreviewVO hostsFilePreview = generateHostsFilePreviewInner(clusterId, 1, 10);
            if (hostsFilePreview == null) {
                return Result.error("生成hosts文件预览失败");
            }

            logger.info("获取到预览内容，总主机数：{}", hostsFilePreview.getHostCount());

            // 获取集群主机信息
            Map<String, HostInfo> hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
            if (hostMap.isEmpty()) {
                return Result.error("未找到集群的主机信息");
            }

            // 初始化任务进度
            String taskId = TaskProgressHelper.initSyncHostsFileTask(clusterId, hostMap);

            // 使用异步服务执行同步任务
            asyncCheckService.syncHostsFileTask(taskId, clusterId, hostMap, hostsFilePreview);

            // 返回任务ID
            return Result.success(taskId);
        } catch (Exception e) {
            logger.error("同步hosts文件任务启动时发生错误", e);
            return Result.error("同步hosts文件任务启动失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务进度
     *
     * @param taskId 任务ID
     * @return 任务进度信息
     */
    @Override
    public Result getTaskProgress(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return Result.error("任务ID不能为空");
        }

        // 尝试从TaskProgressHelper获取任务进度
        TaskProgress progress = TaskProgressHelper.getTaskProgress(taskId);

        // 如果在新的Helper中找不到，则从原来的映射中查找
        if (progress == null) {
            progress = taskProgressMap.get(taskId);
        }

        if (progress == null) {
            return Result.error("未找到任务：" + taskId + "，可能已完成或已过期");
        }

        return Result.success(progress);
    }

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
    @Override
    public Result batchSetHostname(Integer clusterId, String prefix, Integer zeroCount, String separator,
            String suffix) {
        try {
            // 获取存储在缓存中的主机信息
            Map<String, HostInfo> hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
            if (hostMap.isEmpty()) {
                return Result.error("未找到主机信息");
            }

            // 检查前缀是否合法
            if (StrUtil.isBlank(prefix)) {
                return Result.error("主机名前缀不能为空");
            }

            // 如果分隔符为null，设置为空字符串
            if (separator == null) {
                separator = "";
            }

            // 如果后缀为null，设置为空字符串
            if (suffix == null) {
                suffix = "";
            }

            // 确保零填充数字的最小位数
            if (zeroCount == null || zeroCount < 1) {
                zeroCount = 1;
            }

            // 将IP地址排序（使用HostUtils中的通用排序方法）
            List<String> sortedIps = HostUtils.sortIpAddresses(new ArrayList<>(hostMap.keySet()));
            logger.info("按IP排序后的主机列表: {}", sortedIps);

            // 准备主机名预览列表
            List<Map<String, String>> hostnamePreview = new ArrayList<>();
            int index = 1;

            // 生成主机名预览，使用排序后的IP列表
            for (String ip : sortedIps) {
                HostInfo hostInfo = hostMap.get(ip);
                // 生成数字部分，使用零填充
                String numberPart = String.format("%0" + zeroCount + "d", index);
                // 组合完整主机名
                String newHostname = prefix + separator + numberPart + suffix;

                Map<String, String> hostItem = new HashMap<>();
                hostItem.put("ip", ip);
                hostItem.put("currentHostname", hostInfo.getHostname());
                hostItem.put("newHostname", newHostname);
                hostnamePreview.add(hostItem);
                index++;
            }

            // 创建任务进度对象
            String taskId = TaskProgressHelper.initBatchSetHostnameTask(clusterId, hostnamePreview);

            // 异步执行批量设置主机名任务
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    logger.info("开始执行批量设置主机名任务，集群ID: {}, 任务ID: {}", clusterId, taskId);

                    // 将主机列表分成10个一批的批次
                    int batchSize = 10; // 并发处理的主机数
                    int totalHosts = hostnamePreview.size();

                    for (int i = 0; i < totalHosts; i += batchSize) {
                        // 创建当前批次的任务列表
                        List<CompletableFuture<Void>> batchTasks = new ArrayList<>();

                        // 计算当前批次的结束索引
                        int endIndex = Math.min(i + batchSize, totalHosts);
                        logger.info("开始处理第{}批主机，范围: {}-{}, 共{}台主机",
                                (i / batchSize) + 1, i + 1, endIndex, endIndex - i);

                        // 为当前批次的每个主机创建异步任务
                        for (int j = i; j < endIndex; j++) {
                            Map<String, String> hostItem = hostnamePreview.get(j);
                            String ip = hostItem.get("ip");
                            String newHostname = hostItem.get("newHostname");

                            // 跳过无效的主机信息
                            if (ip == null || ip.isEmpty() || newHostname == null || newHostname.isEmpty()) {
                                logger.warn("主机信息不完整，跳过该主机: {}", hostItem);
                                continue;
                            }

                            // 为每个主机创建异步任务
                            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                                try {
                                    // 直接复用单个主机名更新方法
                                    Result result = updateHostname(clusterId, ip, newHostname, true);

                                    // 根据结果更新任务状态
                                    if (result.getCode() == 200) {
                                        TaskProgressHelper.updateHostProcessStatus(
                                                taskId, ip, true, null);
                                    } else {
                                        TaskProgressHelper.updateHostProcessStatus(
                                                taskId, ip, false, result.getMsg());
                                    }
                                } catch (Exception e) {
                                    logger.error("为主机 {} 设置主机名时出错", ip, e);
                                    TaskProgressHelper.updateHostProcessStatus(
                                            taskId, ip, false, e.getMessage());
                                }
                            }, checkExecutor);

                            batchTasks.add(task);
                        }

                        // 等待当前批次的所有任务完成
                        try {
                            CompletableFuture.allOf(batchTasks.toArray(new CompletableFuture[0])).get();
                            logger.info("第{}批主机名设置完成", (i / batchSize) + 1);
                        } catch (Exception e) {
                            logger.error("等待批处理任务完成时发生错误", e);
                        }
                    }

                    // 完成任务
                    TaskProgressHelper.completeTask(
                            taskId,
                            "所有主机名设置成功",
                            "部分主机名设置失败，请检查详情");

                    // 更新主机信息缓存
                    asyncCheckService.updateHostMapInCache(clusterId);

                    logger.info("批量设置主机名任务完成，集群ID: {}, 任务ID: {}", clusterId, taskId);

                } catch (Exception e) {
                    logger.error("执行批量设置主机名任务时发生错误", e);
                }
            }, checkExecutor);

            // 注册任务
            taskManager.registerTask("batch_set_hostname", "批量设置主机名 - 集群ID: " + clusterId, future);

            // 返回任务ID
            return Result.success(taskId);

        } catch (Exception e) {
            logger.error("批量设置主机名任务启动时发生错误", e);
            return Result.error("批量设置主机名任务启动失败: " + e.getMessage());
        }
    }

    @Override
    public Result skipAllFailedItems(Integer clusterId) {
        Map<String, HostInfo> hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        if (hostMap.isEmpty()) {
            return Result.error("集群未找到或无主机信息");
        }

        StringBuilder resultMessage = new StringBuilder();
        boolean hasErrors = false;
        int totalSkippedItems = 0;

        // 遍历所有主机
        for (Map.Entry<String, HostInfo> entry : hostMap.entrySet()) {
            String hostname = entry.getKey();
            HostInfo hostInfo = entry.getValue();
            int hostSkippedItems = 0;

            try {
                // 筛选该主机上所有失败状态的检查项
                List<CheckItem> failedItems = hostInfo.getCheckItems().stream()
                        .filter(item -> item.getStatus() == CheckItem.Status.FAILED)
                        .toList();

                // 先尝试停止所有正在运行的检查项
                for (CheckItem item : failedItems) {
                    try {
                        stopItemCheck(clusterId, hostname, item.getId());
                    } catch (Exception e) {
                        logger.warn("停止检查项 {} 失败: {}", item.getId(), e.getMessage());
                    }
                }

                // 将所有失败项状态更新为已跳过
                for (CheckItem item : failedItems) {
                    hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.SKIPPED, "用户已跳过该检查项");

                    // 记录日志
                    LoggerFactory.getCheckLogger(this, clusterId, hostname, item.getId())
                            .info("用户批量跳过了该检查项");

                    hostSkippedItems++;
                }

                totalSkippedItems += hostSkippedItems;

                // 更新主机的缓存信息
                if (hostSkippedItems > 0) {
                    asyncCheckService.updateHostInfoCache(clusterId, hostInfo);
                }
            } catch (Exception e) {
                hasErrors = true;
                logger.error("跳过主机 {} 的检查项时发生错误: {}", hostname, e.getMessage(), e);
                resultMessage.append("主机 ").append(hostname).append(" 跳过失败: ").append(e.getMessage()).append("; ");
            }
        }

        // 更新主机映射缓存
        asyncCheckService.updateHostMapInCache(clusterId);

        if (hasErrors) {
            return Result.error("部分跳过失败: " + resultMessage + "已跳过 " + totalSkippedItems + " 个项目");
        } else if (totalSkippedItems == 0) {
            return Result.success("没有发现需要跳过的失败项");
        } else {
            return Result.success("已成功跳过所有失败项，共 " + totalSkippedItems + " 个");
        }
    }

    /**
     * 修复选中的检查项
     */
    @Override
    public Result fixSelectedCheckItems(Integer clusterId, String hostname, String itemIds) {
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

        HostInfo hostInfo = map.get(hostname);
        if (Objects.isNull(hostInfo)) {
            return Result.error("主机不存在");
        }

        List<Integer> itemIdList = Arrays.stream(itemIds.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        return fixSelectedCheckItems(clusterId, hostInfo, itemIdList);
    }
}