package com.datasophon.api.service.impl;

import com.datasophon.api.service.HostCheckService;
import com.datasophon.api.service.checker.ItemChecker;
import com.datasophon.api.service.checker.ItemCheckerFactory;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import com.datasophon.common.utils.Result;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

/**
 * 主机检查服务实现类
 */
@Service
public class HostCheckServiceImpl implements HostCheckService {
    private static final Logger logger = LoggerFactory.getLogger(HostCheckServiceImpl.class);
    private static final String CHECK_ITEMS_CACHE_PREFIX = "CHECK_ITEMS_";
    private static final String CHECK_TASK_STATUS_PREFIX = "CHECK_TASK_STATUS_";
    private static final String CHECK_TASK_FUTURE_PREFIX = "CHECK_TASK_FUTURE_";
    private static final String CHECK_ITEM_LOG_PREFIX = "CHECK_ITEM_LOG_";

    @Autowired
    private ItemCheckerFactory itemCheckerFactory;

    @Autowired
    private HostCheckQueueManager checkQueueManager;

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
    public CheckItem executeCheckItem(HostInfo hostInfo, CheckItem checkItem, Integer clusterId) {
        ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
        if (checker == null) {
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("未知的检查项");
            return checkItem;
        }

        // 初始化日志
        String logKey = getLogKey(clusterId, hostInfo.getHostname(), checkItem.getId());
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("==== 开始检查: ").append(checkItem.getItemName()).append(" ====\n");
        logBuilder.append("时间: ").append(new java.util.Date()).append("\n");
        logBuilder.append("主机: ").append(hostInfo.getHostname()).append("\n\n");
        
        // 保存初始日志
        CacheUtils.put(logKey, logBuilder.toString());
        
        try {
            // 设置检查状态为"检查中"
            checkItem.setStatus(CheckItem.Status.CHECKING);
            
            // 记录检查开始
            appendLog(logKey, "正在执行检查...\n");
            
            // 执行检查
            CheckItem result = checker.check(clusterId, hostInfo, checkItem);
            
            // 记录检查结果
            appendLog(logKey, "检查结果: " + result.getStatus() + "\n");
            appendLog(logKey, "详细信息: " + result.getMessage() + "\n");
            appendLog(logKey, "==== 检查完成 ====\n");
            
            return result;
        } catch (Exception e) {
            // 记录异常
            appendLog(logKey, "检查过程中发生异常: " + e.getMessage() + "\n");
            appendLog(logKey, "==== 检查失败 ====\n");
            
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("检查失败: " + e.getMessage());
            return checkItem;
        }
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

            // 记录日志
            String logKey = getLogKey(clusterId, hostname, itemId);
            appendLog(logKey, "\n==== 开始修复: " + checkItem.getItemName() + " ====\n");
            appendLog(logKey, "时间: " + new java.util.Date() + "\n");

            // 获取Checker
            ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
            if (checker == null) {
                appendLog(logKey, "修复失败: 未找到对应的检查器\n");
                return Result.error("未找到对应的检查器");
            }

            // 执行修复
            appendLog(logKey, "正在执行修复...\n");
            boolean success = checker.fix(clusterId, hostInfo, checkItem);

            if (success) {
                appendLog(logKey, "修复成功，将重新检查项目状态\n");
                
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
                        appendLog(logKey, "正在重新检查...\n");
                        executeCheckItem(hostInfo, checkItem, clusterId);
                        
                        // 更新缓存
                        updateHostInfoCache(clusterId, hostInfo);
                    } catch (Exception e) {
                        appendLog(logKey, "重新检查失败: " + e.getMessage() + "\n");
                        logger.error("修复后重新检查失败: {}", e.getMessage(), e);
                    }
                });
                
                return Result.success("修复指令已发送，请稍后查看结果");
            } else {
                appendLog(logKey, "修复失败\n");
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
            
            // 创建检查项队列
            BlockingQueue<CheckItem> itemQueue = new LinkedBlockingQueue<>(hostInfo.getCheckItems());
            
            while (!itemQueue.isEmpty() && !Thread.currentThread().isInterrupted()) {
                CheckItem item = itemQueue.poll();
                if (item == null) break;
                
                try {
                    // 设置当前检查项状态为"检查中"
                    hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.CHECKING, "检查中");
                    // 立即更新缓存，使前端能看到"检查中"状态
                    updateHostInfoCache(clusterId, hostInfo);

                    // 执行检查
                    boolean success = executeHostCheck(clusterId, hostInfo, item);

                    // 如果线程被中断，标记为已跳过
                    if (Thread.currentThread().isInterrupted()) {
                        logger.info("检查任务被终止，主机: {}, 检查项: {}", hostInfo.getHostname(), item.getItemName());
                        hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.SKIPPED, "检查已终止");
                        break;
                    }

                    // 设置检查结果
                    if (success) {
                        hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.SUCCESS, "检查通过");
                    } else {
                        hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.FAILED, "检查未通过");
                    }

                    // 更新缓存，使前端能立即看到检查结果
                    updateHostInfoCache(clusterId, hostInfo);

                    // 为了避免CPU高负载，添加短暂延迟
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.info("检查被中断，主机: {}, 检查项: {}", hostInfo.getHostname(), item.getItemName());
                    hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.SKIPPED, "检查已终止");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("执行检查项 {} 失败: {}", item.getId(), e.getMessage(), e);
                    hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.FAILED, "检查失败: " + e.getMessage());
                    updateHostInfoCache(clusterId, hostInfo);
                }
            }
            
            // 如果是因为中断而退出，将剩余检查项标记为已跳过
            if (Thread.currentThread().isInterrupted()) {
                while (!itemQueue.isEmpty()) {
                    CheckItem item = itemQueue.poll();
                    if (item != null) {
                        hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.SKIPPED, "检查已终止");
                    }
                }
                updateHostInfoCache(clusterId, hostInfo);
            }
            
        } catch (Exception e) {
            logger.error("检查主机 {} 时发生错误: {}", hostInfo.getHostname(), e.getMessage(), e);
            // 将所有检查项设置为失败
            if (hostInfo.getCheckItems() != null) {
                for (CheckItem item : hostInfo.getCheckItems()) {
                    if (item.getStatus() != CheckItem.Status.SUCCESS) {
                        hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.FAILED, "检查过程中发生错误: " + e.getMessage());
                    }
                }
                updateHostInfoCache(clusterId, hostInfo);
            }
        }
    }

    /**
     * 执行具体的主机检查项
     */
    private boolean executeHostCheck(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        Future<Boolean> checkFuture = null;
        try {
            // 在单独的线程中执行检查，这样可以被中断
            checkFuture = checkQueueManager.getExecutorService().submit(() -> {
                try {
                    // 使用工厂模式获取对应的检查器
                    ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
                    if (checker == null) {
                        checkItem.setMessage("未知的检查项");
                        return false;
                    }

                    // 执行检查并获取结果
                    CheckItem resultItem = checker.check(clusterId, hostInfo, checkItem);
                    return resultItem.getStatus() == CheckItem.Status.SUCCESS;
                } catch (Exception e) {
                    logger.error("执行检查失败: {}", e.getMessage(), e);
                    checkItem.setMessage("检查失败: " + e.getMessage());
                    return false;
                }
            });

            // 等待检查完成或被中断
            try {
                return checkFuture.get(30, TimeUnit.SECONDS); // 设置超时时间为30秒
            } catch (TimeoutException e) {
                logger.error("检查超时: {}", checkItem.getItemName());
                checkItem.setMessage("检查超时");
                return false;
            } catch (CancellationException e) {
                logger.info("检查被取消: {}", checkItem.getItemName());
                checkItem.setMessage("检查已终止");
                return false;
            }
        } catch (Exception e) {
            logger.error("执行检查失败", e);
            checkItem.setMessage("检查失败: " + e.getMessage());
            return false;
        } finally {
            // 如果任务还在运行，尝试取消它
            if (checkFuture != null && !checkFuture.isDone()) {
                checkFuture.cancel(true);
            }
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

            // 查找并更新检查项状态
            boolean updated = hostInfo.updateCheckItemStatus(
                    itemId,
                    CheckItem.Status.SKIPPED,
                    "检查已终止"
            );

            if (!updated) {
                return Result.error("检查项不存在或已完成检查");
            }

            // 更新缓存
            map.put(hostname, hostInfo);
            CacheUtils.put(clusterId + Constants.HOST_MAP, map);

            return Result.success("检查项已终止");
        } catch (Exception e) {
            logger.error("终止检查项失败: ", e);
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

        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        if (map == null) {
            return Result.error("找不到集群的主机信息");
        }

        List<String> successHosts = new ArrayList<>();
        List<String> failedHosts = new ArrayList<>();

        for (String hostname : hostnames) {
            try {
                if (map.containsKey(hostname)) {
                    // 对单个主机启动检查
                    checkSingleHost(clusterId, hostname);
                    successHosts.add(hostname);
                } else {
                    failedHosts.add(hostname);
                    logger.warn("主机 {} 不存在于集群缓存中", hostname);
                }
            } catch (Exception e) {
                logger.error("启动检查主机 {} 时发生错误: {}", hostname, e.getMessage());
                failedHosts.add(hostname);
            }
        }

        if (failedHosts.isEmpty()) {
            return Result.success(String.format("成功启动对%d个主机的检查", successHosts.size()));
        } else {
            return Result.error(String.format("成功启动%d个主机的检查，%d个主机失败: %s",
                    successHosts.size(), failedHosts.size(), String.join(", ", failedHosts)));
        }
    }

    @Override
    public Result getCheckItemLog(Integer clusterId, String hostname, Integer itemId) {
        try {
            // 从缓存中获取日志
            String logKey = getLogKey(clusterId, hostname, itemId);
            String logContent = (String) CacheUtils.get(logKey);
            
            if (logContent == null) {
                // 如果没有日志，返回空消息
                return Result.success("暂无日志数据");
            }
            
            return Result.success(logContent);
        } catch (Exception e) {
            logger.error("获取检查项日志失败: {}", e.getMessage(), e);
            return Result.error("获取日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建日志缓存的Key
     */
    private String getLogKey(Integer clusterId, String hostname, Integer itemId) {
        return CHECK_ITEM_LOG_PREFIX + clusterId + "_" + hostname + "_" + itemId;
    }

    /**
     * 追加日志内容
     */
    private void appendLog(String logKey, String content) {
        String logContent = (String) CacheUtils.get(logKey);
        if (logContent == null) {
            logContent = "";
        }
        logContent += content;
        CacheUtils.put(logKey, logContent);
    }
}