package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.service.HostCheckService;
import com.datasophon.api.service.checker.CheckLogger;
import com.datasophon.api.service.checker.ItemChecker;
import com.datasophon.api.service.checker.ItemCheckerFactory;
import com.datasophon.api.service.checker.LogEntryManager;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import com.datasophon.common.model.LogEntry;
import com.datasophon.common.utils.Result;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * 主机检查服务实现类
 */
@Service
public class HostCheckServiceImpl implements HostCheckService {
    // 静态常量，日志相关
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(HostCheckServiceImpl.class);
    private static final String CHECK_TASK_STATUS_PREFIX = "CHECK_TASK_STATUS_";
    private static final String CHECK_ITEM_LOG_PREFIX = CheckLogger.CHECK_ITEM_LOG_PREFIX;
    private static final String CHECK_LOG_PREFIX = CheckLogger.CHECK_LOG_PREFIX;
    private static final String FIX_LOG_PREFIX = CheckLogger.FIX_LOG_PREFIX;

    @Autowired
    private ItemCheckerFactory itemCheckerFactory;

    @Autowired
    private HostCheckQueueManager checkQueueManager;

    /**
     * 日志工厂，用于创建日志记录器
     */
    public static class LoggerFactory {
        /**
         * 创建检查项日志记录器
         */
        public static CheckLogger getLogger(HostCheckServiceImpl service, Integer clusterId, String hostname, Integer itemId) {
            String logKey = service.getLogKey(clusterId, hostname, itemId);
            return CheckLogger.createLogger(logKey, service.getClass().getSimpleName());
        }

        /**
         * 创建检查项日志记录器，使用自定义类名
         */
        public static CheckLogger getLogger(HostCheckServiceImpl service, Integer clusterId, String hostname, Integer itemId, String className) {
            String logKey = service.getLogKey(clusterId, hostname, itemId);
            return CheckLogger.createLogger(logKey, className);
        }
    }

    @Override
    public List<CheckItem> getHostCheckItems() {
        List<CheckItem> checkItems = new ArrayList<>();

        // 创建所有检查项
        checkItems.add(createCheckItem(1, ItemCode.PASSWORD_FREE));
        checkItems.add(createCheckItem(2, ItemCode.JAVA_ENV));
        checkItems.add(createCheckItem(3, ItemCode.FILE_HANDLE));
        checkItems.add(createCheckItem(4, ItemCode.FIREWALL));
        checkItems.add(createCheckItem(5, ItemCode.SELINUX));
        checkItems.add(createCheckItem(6, ItemCode.TIME_SYNC));

        return checkItems;
    }

    @Override
    public Result fixCheckItem(Integer clusterId, String hostname, Integer itemId) {
        try {
            HostInfo hostInfo = getHostInfo(clusterId, hostname);
            if (hostInfo == null) {
                return Result.error("未找到主机信息");
            }

            CheckItem checkItem = findCheckItemById(hostInfo, itemId);
            if (checkItem == null) {
                return Result.error("未找到检查项");
            }

            // 创建日志记录器
            String logKey = getLogKey(clusterId, hostname, itemId);
            CheckLogger cacheLog = LoggerFactory.getLogger(this, clusterId, hostname, itemId);
            
            // 记录日志
            cacheLog.info("==== 开始修复: " + checkItem.getItemName() + " ====");
            cacheLog.info("时间: " + formatDateToChinese(new Date()));

            // 获取Checker
            ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
            if (checker == null) {
                cacheLog.error("修复失败: 未找到对应的检查器");
                return Result.error("未找到对应的检查器");
            }

            // 执行修复
            cacheLog.info("正在执行修复...");
            boolean success = checker.fix(clusterId, hostInfo, checkItem);

            if (success) {
                cacheLog.info("修复成功，将重新检查项目状态");
                
                // 修改状态为待检查，等待下次检查
                checkItem.setStatus(CheckItem.Status.WAITING);
                checkItem.setMessage("等待重新检查");
                
                // 更新缓存
                updateHostInfoCache(clusterId, hostInfo);
                
                // 执行异步检查
                CompletableFuture.runAsync(() -> {
                    try {
                        // 暂停一小段时间，让修改生效
                        Thread.sleep(3000);
                        
                        // 重新执行检查
                        cacheLog.info("正在重新检查...");
                        executeCheckItem(hostInfo, checkItem, clusterId);
                        
                        // 更新缓存
                        updateHostInfoCache(clusterId, hostInfo);
                    } catch (Exception e) {
                        cacheLog.error("重新检查失败: " + e.getMessage());
                        logger.error("修复后重新检查失败: {}", e.getMessage(), e);
                    }
                });
                
                return Result.success("修复指令已发送，请稍后查看结果");
            } else {
                cacheLog.error("修复失败");
                return Result.error("修复失败");
            }
        } catch (Exception e) {
            logger.error("修复检查项失败: {}", e.getMessage(), e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }

    @Override
    public Result fixSelectedCheckItems(Integer clusterId, String hostname, String itemIds) {
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);

        HostInfo hostInfo = map.get(hostname);
        if (Objects.isNull(hostInfo)) {
            return Result.error("主机不存在");
        }

        List<Integer> itemIdList = Arrays.stream(itemIds.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        List<CheckItem> itemsToFix = hostInfo.getCheckItems().stream()
                .filter(item -> itemIdList.contains(item.getId()) &&
                        item.getStatus().equals(CheckItem.Status.FAILED))
                .collect(Collectors.toList());

        if (itemsToFix.isEmpty()) {
            return Result.error("没有可修复的检查项");
        }

        try {
            boolean allSuccess = true;
            for (CheckItem item : itemsToFix) {
                if (!doFix(clusterId, hostInfo, item)) {
                    allSuccess = false;
                    break;
                }
            }

            if (allSuccess) {
                retriggerHostCheck(hostInfo, clusterId);
                return Result.success();
            } else {
                return Result.error("部分检查项修复失败");
            }
        } catch (Exception e) {
            logger.error("修复选中检查项失败", e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }

    @Override
    public Result fixAllCheckItems(Integer clusterId, String hostname) {
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);

        HostInfo hostInfo = map.get(hostname);
        if (Objects.isNull(hostInfo)) {
            return Result.error("主机不存在");
        }

        try {
            boolean allSuccess = true;
            for (CheckItem item : hostInfo.getCheckItems()) {
                if (item.getStatus().equals(CheckItem.Status.FAILED)) {
                    if (!doFix(clusterId, hostInfo, item)) {
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
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        if (Objects.isNull(map) || !map.containsKey(hostname)) {
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
            
            item.setStatus(CheckItem.Status.WAITING);
            item.setMessage("等待检查");
        }
        hostInfo.setCheckItems(checkItems);
        hostInfo.setStatus(CheckItem.Status.WAITING);
        
        // 更新缓存
        map.put(hostname, hostInfo);
        CacheUtils.put(clusterId + Constants.HOST_MAP, map);

        // 将检查任务添加到队列
        checkQueueManager.addCheckTask(clusterId, hostInfo, this);
    }

    /**
     * 执行主机检查流程
     * 包级别访问权限，允许队列管理器调用
     */
    void processHostCheck(Integer clusterId, HostInfo hostInfo) {
        try {
            logger.info("开始检查主机: {}", hostInfo.getHostname());
            
            // 记录当前线程名称和检查项总数
            logger.debug("检查线程: {}, 主机: {}, 集群ID: {}", 
                Thread.currentThread().getName(), 
                hostInfo.getHostname(), 
                clusterId);
            
            // 创建检查项队列
            BlockingQueue<CheckItem> itemQueue = new LinkedBlockingQueue<>(hostInfo.getCheckItems());
            
            // 标记整个主机检查是否被取消
            boolean hostCheckCancelled = false;
            
            // 按顺序遍历检查项，确保串行执行
            logger.debug("主机 {} 队列中待检查项数量: {}", hostInfo.getHostname(), itemQueue.size());
            
            // 记录队列中所有检查项的状态
            if (logger.isDebugEnabled()) {
                StringBuilder itemStatus = new StringBuilder();
                for (CheckItem item : hostInfo.getCheckItems()) {
                    itemStatus.append(item.getItemName())
                             .append("(").append(item.getStatus()).append("), ");
                }
                logger.debug("主机 {} 检查项列表: {}", hostInfo.getHostname(), itemStatus.toString());
            }
            
            if (itemQueue.isEmpty()) {
                logger.warn("主机 {} 没有可执行的检查项，检查提前结束", hostInfo.getHostname());
                return;
            }
            
            while (!itemQueue.isEmpty()) {
                CheckItem item = itemQueue.poll();
                if (item == null) {
                    logger.warn("从队列获取的检查项为null，可能是队列已清空");
                    break;
                }
                
                // 只处理状态为"等待检查"或"检查中"的检查项，跳过其他状态
                if (item.getStatus() != CheckItem.Status.WAITING && item.getStatus() != CheckItem.Status.CHECKING) {
                    logger.debug("跳过非等待状态的检查项: {}, 状态: {}, 主机: {}",
                        item.getItemName(), item.getStatus(), hostInfo.getHostname());
                    continue;
                }
                
                try {
                    // 设置当前检查项状态为"检查中"
                    logger.debug("设置检查项 {} 状态为检查中", item.getItemName());
                    hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.CHECKING, "检查中");
                    
                    // 立即更新缓存，确保前端能看到"检查中"状态
                    logger.debug("立即更新缓存，使前端能看到检查中状态");
                    updateHostInfoCache(clusterId, hostInfo);

                    // 执行检查，此方法内部不再使用线程池
                    logger.info("开始执行检查项: {}", item.getItemName());
                    boolean success = executeHostCheck(clusterId, hostInfo, item);
                    logger.info("检查项 {} 执行完成，结果: {}", item.getItemName(), success ? "成功" : "失败");

                    // 检查完成后获取最新状态 - 可能已被手动终止
                    CheckItem updatedItem = findCheckItemById(hostInfo, item.getId());
                    if (updatedItem != null && updatedItem.getStatus() == CheckItem.Status.SKIPPED) {
                        logger.debug("检查项在执行过程中被手动终止: {}, 主机: {}", item.getItemName(), hostInfo.getHostname());
                        continue;
                    }

                    // 更新缓存，使前端能立即看到检查结果
                    updateHostInfoCache(clusterId, hostInfo);

                    // 每个检查项执行完后等待3秒
                    logger.debug("检查项 {} 执行完成，等待3秒后继续执行下一个检查项", item.getItemName());
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    // 如果主机检查被中断，放弃后续检查项
                    logger.warn("主机 {} 的检查被中断", hostInfo.getHostname());
                    hostCheckCancelled = true;
                    break;
                } catch (Exception e) {
                    logger.error("执行检查项 {} 时发生错误: {}", item.getItemName(), e.getMessage(), e);
                    
                    // 设置检查项状态为失败
                    hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.FAILED, "执行检查时发生错误: " + e.getMessage());
                    updateHostInfoCache(clusterId, hostInfo);
            }
            
                // 检查是否被取消
            if (Thread.currentThread().isInterrupted()) {
                    logger.warn("检测到中断信号，取消主机 {} 的检查", hostInfo.getHostname());
                    hostCheckCancelled = true;
                    break;
                }
            }
            
            // 如果被取消，将剩余检查项标记为"已跳过"
            if (hostCheckCancelled) {
                logger.info("标记主机 {} 剩余检查项为已跳过", hostInfo.getHostname());
                while (!itemQueue.isEmpty()) {
                    CheckItem item = itemQueue.poll();
                    if (item != null) {
                        hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.SKIPPED, "检查已终止");
                    }
                }
                updateHostInfoCache(clusterId, hostInfo);
            }
            
            logger.info("主机 {} 的所有检查项执行完成", hostInfo.getHostname());
        } catch (Exception e) {
            logger.error("检查主机 {} 时发生错误: {}", hostInfo.getHostname(), e.getMessage(), e);
            // 将所有检查项设置为失败
            if (hostInfo.getCheckItems() != null) {
                for (CheckItem item : hostInfo.getCheckItems()) {
                    if (item.getStatus() != CheckItem.Status.SUCCESS && item.getStatus() != CheckItem.Status.SKIPPED) {
                        hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.FAILED, "检查过程中发生错误: " + e.getMessage());
                    }

                }
                updateHostInfoCache(clusterId, hostInfo);
            }
        }
    }

    /**
     * 异步执行检查项，返回结果为是否成功
     */
    private boolean executeHostCheck(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            logger.info("准备串行执行检查项: {}，主机: {}, 线程: {}", 
                      checkItem.getItemName(), 
                      hostInfo.getHostname(), 
                      Thread.currentThread().getName());
            
            // 确保状态更新已发送到前端
            updateHostInfoCache(clusterId, hostInfo);
            
            // 初始化日志
            String logKey = getLogKey(clusterId, hostInfo.getHostname(), checkItem.getId());
            logger.debug("检查项日志键: {}", logKey);
            
            // 清除可能存在的旧日志
            LogEntryManager.clearLogEntries(logKey);
            
            // 创建日志记录器
            CheckLogger cacheLog = LoggerFactory.getLogger(this, clusterId, hostInfo.getHostname(), checkItem.getId());
            
            // 添加日志头部信息
            cacheLog.info("==== 开始检查: " + checkItem.getItemName() + " ====");
            cacheLog.info("主机: " + hostInfo.getHostname());
            cacheLog.info("检查项ID: " + checkItem.getId());
            cacheLog.debug("检查项代码: " + checkItem.getItemCode());
            cacheLog.debug("集群ID: " + clusterId);
            cacheLog.debug("执行时间: " + formatDateToChinese(new Date()));
            cacheLog.debug("执行线程: " + Thread.currentThread().getName());
            
            logger.debug("主机 {} 开始执行检查项 {}", hostInfo.getHostname(), checkItem.getItemName());
            cacheLog.info("正在执行检查任务...");
            
            // 从工厂获取检查器实例
            try {
                // 注意检查ItemCode的转换过程
                ItemCode itemCodeEnum = null;
                try {
                    itemCodeEnum = ItemCode.valueOf(checkItem.getItemCode());
                } catch (IllegalArgumentException e) {
                    String errorMsg = "无法将字符串 '" + checkItem.getItemCode() + "' 转换为ItemCode枚举";
                    logger.error(errorMsg, e);
                    cacheLog.error(errorMsg);
                    cacheLog.error("详细错误: " + e.getMessage());
                    hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, errorMsg);
                    updateHostInfoCache(clusterId, hostInfo);
                    return false;
                }
                
                logger.debug("正在获取检查器: {}", itemCodeEnum);
                cacheLog.debug("正在获取检查器: " + itemCodeEnum);
                
                ItemChecker checker = itemCheckerFactory.getChecker(itemCodeEnum);
                
                    if (checker == null) {
                    String errorMsg = "未找到检查项 " + checkItem.getItemCode() + " 对应的检查器实现";
                    logger.error(errorMsg);
                    cacheLog.error(errorMsg);
                    hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, errorMsg);
                    updateHostInfoCache(clusterId, hostInfo);
                        return false;
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
                    updateHostInfoCache(clusterId, hostInfo);
                    
                    return result;
                } catch (Exception e) {
                    String errorMsg = "执行检查项 " + checkItem.getItemName() + " 时发生异常: " + e.getMessage();
                    logger.error(errorMsg, e);
                    cacheLog.error(errorMsg);
                    cacheLog.error("异常堆栈: " + getStackTraceAsString(e));
                    
                    hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, "执行检查时发生异常: " + e.getMessage());
                    updateHostInfoCache(clusterId, hostInfo);
                    return false;
                }
            } catch (Exception e) {
                String errorMsg = "处理检查项 " + checkItem.getItemName() + " 的检查器时发生异常: " + e.getMessage();
                logger.error(errorMsg, e);
                cacheLog.error(errorMsg);
                cacheLog.error("异常堆栈: " + getStackTraceAsString(e));
                
                hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, "系统错误: " + e.getMessage());
                updateHostInfoCache(clusterId, hostInfo);
                return false;
            }
        } catch (Exception e) {
            logger.error("执行检查失败: " + e.getMessage(), e);
            
            checkItem.setMessage("检查失败: " + e.getMessage());
            updateHostInfoCache(clusterId, hostInfo);
            
            // 记录异常信息
            CheckLogger cacheLog = LoggerFactory.getLogger(this, clusterId, hostInfo.getHostname(), checkItem.getId());
            cacheLog.error("执行检查时发生异常: " + e.getMessage());
            cacheLog.error("异常堆栈: " + getStackTraceAsString(e));
            cacheLog.error("==== 检查失败 ====");
            
            return false;
        }
    }

    @Override
    public Result stopHostCheck(Integer clusterId, String hostname) {
        try {
            // 获取主机信息
            Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
            if (Objects.isNull(map)) {
                return Result.error("主机信息不存在");
            }

            HostInfo hostInfo = map.get(hostname);
            if (Objects.isNull(hostInfo)) {
                return Result.error("主机不存在");
            }

            // 取消队列中的整个主机检查任务
            checkQueueManager.cancelTask(clusterId, hostname);

            // 将该主机的所有检查项状态设为已跳过
            // 使用批量更新提高性能
            Map<Integer, CheckItem.Status> updates = new HashMap<>();
            for (CheckItem item : hostInfo.getCheckItems()) {
                if (item.getStatus() == CheckItem.Status.CHECKING || item.getStatus() == CheckItem.Status.WAITING) {
                    updates.put(item.getId(), CheckItem.Status.SKIPPED);
                    item.setMessage("检查已终止");
                }
            }

            if (!updates.isEmpty()) {
                hostInfo.batchUpdateCheckItems(updates);
            }

            // 更新缓存
            map.put(hostname, hostInfo);
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
            Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
            if (Objects.isNull(map)) {
                return Result.error("主机信息不存在");
            }

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
                    "正在终止检查..."
                );
                
                // 更新缓存，使前端立即看到"终止中"状态
                map.put(hostname, hostInfo);
                CacheUtils.put(clusterId + Constants.HOST_MAP, map);
                
                // 在串行模式下，我们需要依赖线程中断机制来取消正在执行的检查项
                // 调用队列管理器的取消方法，它需要负责中断正在执行检查的线程
                checkQueueManager.cancelItemTask(clusterId, hostname, itemId);
                
                // 等待一小段时间，让前端有时间显示"终止中"状态
                // 同时给任务足够时间响应中断请求
                try {
                    Thread.sleep(2000);
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
                    "检查已终止"
            );

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

    private boolean isCheckTaskRunning(Integer clusterId) {
        Boolean status = (Boolean) CacheUtils.get(CHECK_TASK_STATUS_PREFIX + clusterId);
        return status != null && status;
    }

    private boolean doFix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        ClientSession session = null;
        try {
            session = MinaUtils.openConnection(
                    hostInfo.getHostname(),
                    hostInfo.getSshPort(),
                    hostInfo.getSshUser());

            if (session == null) {
                return false;
            }

            ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
            if (checker == null) {
                return false;
            }

            return checker.fix(clusterId, hostInfo, checkItem);
        } catch (Exception e) {
            logger.error("执行修复失败", e);
            return false;
        } finally {
            if (session != null) {
                MinaUtils.closeConnection(session);
            }
        }
    }

    private CheckItem createCheckItem(int id, ItemCode itemCode) {
        CheckItem item = new CheckItem();
        item.setId(id);
        item.setItemCode(itemCode.getCode());
        item.setItemName(itemCode.getDesc());
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
                    updateHostInfoCache(clusterId, hostInfo);
                }

                // 执行检查
                processHostCheck(clusterId, hostInfo);
            } catch (Exception e) {
                logger.error("重新检查主机 {} 时发生错误: {}", hostInfo.getHostname(), e.getMessage(), e);

                // 将所有检查项设置为失败
                if (hostInfo.getCheckItems() != null) {
                    for (CheckItem item : hostInfo.getCheckItems()) {
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("重新检查时发生错误: " + e.getMessage());
                    }
                    // 通过检查项自动计算主机状态
                    hostInfo.calculateStatus();

                    // 捕获到异常后更新缓存
                    updateHostInfoCache(clusterId, hostInfo);

                }
            }
        });
    }

    /**
     * 获取主机信息
     */
    private HostInfo getHostInfo(Integer clusterId, String hostname) {
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        if (map == null) {
            return null;
        }
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
     * 更新主机信息缓存
     */
    private void updateHostInfoCache(Integer clusterId, HostInfo hostInfo) {
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        if (map != null && hostInfo != null) {
            map.put(hostInfo.getHostname(), hostInfo);
            CacheUtils.put(clusterId + Constants.HOST_MAP, map);
        }
    }

    /**
     * 批量检查多个主机
     *
     * @param clusterId 集群ID
     * @param hostnames 主机名列表
     * @return 操作结果
     */
    @Override
    public Result batchCheckHosts(Integer clusterId, List<String> hostnames) {
        if (clusterId == null) {
            return Result.error("集群ID不能为空");
        }

        if (hostnames == null || hostnames.isEmpty()) {
            return Result.error("主机列表不能为空");
        }

        // 取消所有当前运行的检查任务
        logger.info("收到批量检查请求，取消当前所有检查任务");
        checkQueueManager.cancelAllTasks();
        
        // 等待短暂时间确保所有任务已终止
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("开始执行新的批量检查，主机数量: {}", hostnames.size());

        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        if (map == null) {
            return Result.error("找不到集群的主机信息");
        }

        List<String> successHosts = new ArrayList<>();
        List<String> failedHosts = new ArrayList<>();

        for (String hostname : hostnames) {
            try {
                if (map.containsKey(hostname)) {
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
                    successHosts.add(hostname);
                } else {
                    failedHosts.add(hostname);
                    logger.warn("主机 {} 不存在于集群缓存中", hostname);
                }
            } catch (Exception e) {
                logger.error("启动主机 {} 检查失败: {}", hostname, e.getMessage(), e);
                failedHosts.add(hostname);
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
    
    /**
     * 获取修复日志的缓存键
     */
    private String getFixLogKey(Integer clusterId, String hostname, Integer itemId) {
        return "FIX_LOG_" + clusterId + "_" + hostname + "_" + itemId;
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
        checkQueueManager.cancelAllTasks();
        
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
        
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        if (map == null || !map.containsKey(hostname)) {
            return Result.error("找不到主机：" + hostname);
        }

        HostInfo hostInfo = map.get(hostname);
        List<CheckItem> checkItems = hostInfo.getCheckItems();
        if (checkItems == null || checkItems.isEmpty()) {
            return Result.error("主机没有检查项");
        }

        boolean hasChanges = false;
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
                    hasChanges = true;
                    logger.info("重置检查项 {}: {} 状态为等待检查", item.getId(), item.getItemName());
                }
            } catch (NumberFormatException e) {
                logger.warn("无效的检查项ID: {}", itemId);
            }
        }

        if (hasChanges) {
            // 更新缓存
            map.put(hostname, hostInfo);
            CacheUtils.put(clusterId + Constants.HOST_MAP, map);
            
            // 将主机添加到检查队列
            checkQueueManager.addCheckTask(clusterId, hostInfo, this);
            
            return Result.success("成功将检查项添加到检查队列");
        } else {
            return Result.error("未找到需要重试的检查项");
        }
    }
    
    /**
     * 创建日志记录器
     * 此方法现在返回CheckLogger接口，而不是旧的ItemLogger
     *
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
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
        
        StringBuilder sb = new StringBuilder();
        try (java.io.StringWriter sw = new java.io.StringWriter();
             java.io.PrintWriter pw = new java.io.PrintWriter(sw)) {
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
            String logKey = getLogKey(clusterId, hostInfo.getHostname(), checkItem.getId());
            String statusKey = CHECK_TASK_STATUS_PREFIX + clusterId + "_" + hostInfo.getHostname() + "_" + checkItem.getId();

            // 检查是否已经有任务在执行
            Boolean isRunning = (Boolean) CacheUtils.get(statusKey);
            if (isRunning != null && isRunning) {
                logger.warn("检查项已在执行中: {} - {}", hostInfo.getHostname(), checkItem.getItemName());
                return checkItem; // 返回原检查项，表示已在执行中
            }

            // 创建日志记录器
            CheckLogger cacheLog = createLogger(clusterId, hostInfo.getHostname(), checkItem.getId());
            
            // 清空之前的日志
            CacheUtils.put(logKey, "");
            
            // 添加日志头部
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());
            cacheLog.info("开始检查项: " + checkItem.getItemName());
            cacheLog.info("主机: " + hostInfo.getHostname());
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

    @Override
    public Result batchFixCheckItem(Integer clusterId, String hostname, List<Integer> itemIds) {
        try {
            if (itemIds == null || itemIds.isEmpty()) {
                return Result.error("未选择需要修复的检查项");
            }

            HostInfo hostInfo = getHostInfo(clusterId, hostname);
            if (hostInfo == null) {
                return Result.error("未找到主机信息");
            }

            int fixCount = 0;
            for (Integer itemId : itemIds) {
                CheckItem checkItem = findCheckItemById(hostInfo, itemId);
                if (checkItem == null) {
                    continue;
                }

                // 创建日志记录器
                String logKey = getLogKey(clusterId, hostname, itemId);
                CheckLogger cacheLog = LoggerFactory.getLogger(this, clusterId, hostname, itemId);
                
                // 记录日志
                cacheLog.info("==== 开始修复: " + checkItem.getItemName() + " ====");
                cacheLog.info("时间: " + formatDateToChinese(new Date()));

                // 获取Checker
                ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
                if (checker == null) {
                    cacheLog.error("修复失败: 未找到对应的检查器");
                    continue;
                }

                // 执行修复
                cacheLog.info("正在执行修复...");
                boolean success = checker.fix(clusterId, hostInfo, checkItem);

                if (success) {
                    cacheLog.info("修复成功，将重新检查项目状态");
                    
                    // 修改状态为待检查，等待下次检查
                    checkItem.setStatus(CheckItem.Status.WAITING);
                    checkItem.setMessage("等待重新检查");
                    
                    // 添加到成功计数
                    fixCount += 1;
                    
                    // 执行异步检查
                    final Integer checkedItemId = itemId;
                    CompletableFuture.runAsync(() -> {
                        try {
                            // 暂停一小段时间，让修改生效
                            Thread.sleep(3000);
                            
                            // 获取最新的主机信息
                            HostInfo latestHostInfo = getHostInfo(clusterId, hostname);
                            if (latestHostInfo == null) {
                                cacheLog.error("重新检查失败: 未找到主机信息");
                                return;
                            }
                            
                            // 获取最新的检查项
                            CheckItem latestCheckItem = findCheckItemById(latestHostInfo, checkedItemId);
                            if (latestCheckItem == null) {
                                cacheLog.error("重新检查失败: 未找到检查项");
                                return;
                            }
                            
                            // 重新执行检查
                            cacheLog.info("正在重新检查...");
                            executeCheckItem(latestHostInfo, latestCheckItem, clusterId);
                            
                            // 更新缓存
                            updateHostInfoCache(clusterId, latestHostInfo);
                        } catch (Exception e) {
                            cacheLog.error("重新检查失败: " + e.getMessage());
                            logger.error("修复后重新检查失败: {}", e.getMessage(), e);
                        }
                    });
                } else {
                    cacheLog.error("修复失败");
                }
            }

            // 更新缓存
            updateHostInfoCache(clusterId, hostInfo);

            if (fixCount > 0) {
                return Result.success("已发送" + fixCount + "条修复指令，请稍后查看结果");
            } else {
                return Result.error("所有修复任务均执行失败");
            }
        } catch (Exception e) {
            logger.error("批量修复检查项失败: {}", e.getMessage(), e);
            return Result.error("批量修复失败: " + e.getMessage());
        }
    }

    /**
     * 实现统一的日志获取API
     */
    @Override
    public Result getLog(Integer clusterId, String hostname, Integer itemId, String logType, String logLevel, String filterMode) {
        if (clusterId == null) {
            return Result.error("集群ID不能为空");
        }
        
        if (StrUtil.isBlank(hostname)) {
            return Result.error("主机名不能为空");
        }
        
        if (itemId == null) {
            return Result.error("检查项ID不能为空");
        }
        
        // 设置默认值
        if (logType == null || logType.isEmpty()) {
            logType = "all"; // 默认获取全部日志
        }
        
        if (filterMode == null || filterMode.isEmpty()) {
            filterMode = "all"; // 默认不筛选日志级别
        }
        
        logger.info("获取检查项日志(统一接口), clusterId: {}, hostname: {}, itemId: {}, 日志类型: {}, 日志级别: {}, 筛选模式: {}",
            clusterId, hostname, itemId, logType, logLevel, filterMode);
        
        // 获取主机和检查项信息（用于默认响应）
        HostInfo hostInfo = getHostInfo(clusterId, hostname);
        CheckItem checkItem = hostInfo != null ? findCheckItemById(hostInfo, itemId) : null;
        
        // 构建日志键
        List<String> logKeys = new ArrayList<>();
        
        // 根据日志类型选择不同的处理方式
        if ("all".equals(logType) || logType == null) {
            // 构建所有可能的日志类型
            logKeys.add(CHECK_LOG_PREFIX + clusterId + "_" + hostname + "_" + itemId);
            logKeys.add(FIX_LOG_PREFIX + clusterId + "_" + hostname + "_" + itemId);
        } else if ("check".equals(logType)) {
            logKeys.add(CHECK_LOG_PREFIX + clusterId + "_" + hostname + "_" + itemId);
        } else if ("fix".equals(logType)) {
            logKeys.add(FIX_LOG_PREFIX + clusterId + "_" + hostname + "_" + itemId);
        } else {
            return Result.error("不支持的日志类型: " + logType + "，支持的类型有: check, fix, all");
        }
        
        // 是否需要基于级别筛选
        boolean needLevelFilter = !"all".equals(filterMode);
        
        // 如果需要级别筛选，但未提供级别，则使用INFO作为默认级别
        if (needLevelFilter && (logLevel == null || logLevel.isEmpty())) {
            logLevel = "INFO";
        }
        
        // 转换日志级别字符串为枚举值
        LogEntry.Level level = null;
        if (needLevelFilter) {
            try {
                level = LogEntry.Level.valueOf(logLevel.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Result.error("不支持的日志级别: " + logLevel + "，支持的级别有: DEBUG, INFO, WARN, ERROR");
            }
        }
        
        // 组合日志
        StringBuilder combinedLog = new StringBuilder();
        boolean hasAnyLog = false;
        
        for (String logKey : logKeys) {
            String logContent;
            
            // 根据筛选模式获取日志内容
            if ("all".equals(filterMode)) {
                // 不筛选级别
                logContent = LogEntryManager.getLogContent(logKey);
            } else {
                // 筛选级别（exact=精确匹配，min=指定级别及以上）
                boolean exactMatch = "exact".equals(filterMode);
                logContent = LogEntryManager.getFilteredLogContent(logKey, level, exactMatch);
            }
            
            if (!logContent.isEmpty()) {
                if (hasAnyLog) combinedLog.append("\n\n");
                
                // 添加日志类型标题
                String logTypeTitle = logKey.startsWith(CHECK_LOG_PREFIX) ? "检查日志" : "修复日志";
                combinedLog.append("===== ").append(logTypeTitle).append(" =====\n");
                combinedLog.append(logContent);
                hasAnyLog = true;
            }
        }
        
        if (!hasAnyLog) {
            // 如果都没有日志，返回基本信息
            return createDefaultLogResponse(clusterId, hostname, itemId);
        }
        
        logger.info("成功获取检查项日志, 长度: {}", combinedLog.length());
        return Result.success(combinedLog.toString());
    }

    /**
     * 创建默认的日志响应（当没有找到日志时）
     */
    private Result createDefaultLogResponse(Integer clusterId, String hostname, Integer itemId) {
        // 记录日志，便于追踪
        logger.warn("未找到任何日志, clusterId: {}, hostname: {}, itemId: {}", 
            clusterId, hostname, itemId);
        
        // 直接返回空字符串，不再生成假日志
        return Result.success("");
    }
}