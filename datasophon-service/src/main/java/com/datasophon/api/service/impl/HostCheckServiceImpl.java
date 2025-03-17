package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
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
import com.datasophon.api.service.CheckItemLogService;
import com.datasophon.common.model.CheckItemLog;

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
import java.util.Optional;

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
            
            // 标记整个主机检查是否被取消
            boolean hostCheckCancelled = false;
            
            while (!itemQueue.isEmpty() && !Thread.currentThread().isInterrupted()) {
                CheckItem item = itemQueue.poll();
                if (item == null) break;
                
                // 只处理状态为"等待检查"或"检查中"的检查项，跳过其他状态
                if (item.getStatus() != CheckItem.Status.WAITING && item.getStatus() != CheckItem.Status.CHECKING) {
                    logger.info("跳过非等待状态的检查项: {}, 状态: {}, 主机: {}", 
                        item.getItemName(), item.getStatus(), hostInfo.getHostname());
                    continue;
                }
                
                try {
                    // 设置当前检查项状态为"检查中"
                    logger.info("设置检查项 {} 状态为检查中", item.getItemName());
                    hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.CHECKING, "检查中");
                    
                    // 立即更新缓存，确保前端能看到"检查中"状态
                    logger.debug("立即更新缓存，使前端能看到检查中状态");
                    updateHostInfoCache(clusterId, hostInfo);

                    // 执行检查，此方法内部会再次更新缓存
                    boolean success = executeHostCheck(clusterId, hostInfo, item);

                    // 如果线程被中断，说明整个主机检查被终止
                    if (Thread.currentThread().isInterrupted()) {
                        logger.info("整个主机检查任务被终止，主机: {}, 检查项: {}", hostInfo.getHostname(), item.getItemName());
                        hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.SKIPPED, "检查已终止");
                        hostCheckCancelled = true;
                        break;
                    }

                    // 检查完成后获取最新状态 - 可能已被手动终止
                    CheckItem updatedItem = findCheckItemById(hostInfo, item.getId());
                    if (updatedItem != null && updatedItem.getStatus() == CheckItem.Status.SKIPPED) {
                        logger.info("检查项在执行过程中被手动终止: {}, 主机: {}", item.getItemName(), hostInfo.getHostname());
                        continue;
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
                    logger.info("主机检查被中断，主机: {}, 检查项: {}", hostInfo.getHostname(), item.getItemName());
                    hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.SKIPPED, "检查已终止");
                    hostCheckCancelled = true;
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("执行检查项 {} 失败: {}", item.getId(), e.getMessage(), e);
                    hostInfo.updateCheckItemStatus(item.getId(), CheckItem.Status.FAILED, "检查失败: " + e.getMessage());
                    updateHostInfoCache(clusterId, hostInfo);
                }
            }
            
            // 如果是整个主机检查被取消，将剩余检查项标记为已跳过
            if (hostCheckCancelled) {
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
            // 确保状态更新已发送到前端
            updateHostInfoCache(clusterId, hostInfo);
            
            // 初始化日志
            String logKey = getLogKey(clusterId, hostInfo.getHostname(), checkItem.getId());
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("==== 开始检查: ").append(checkItem.getItemName()).append(" ====\n");
            logBuilder.append("时间: ").append(new java.util.Date()).append("\n");
            logBuilder.append("主机: ").append(hostInfo.getHostname()).append("\n\n");
            logBuilder.append("准备提交检查任务到异步线程池...\n");
            
            // 保存初始日志
            CacheUtils.put(logKey, logBuilder.toString());
            
            logger.info("提交检查项 {} 到异步线程池执行", checkItem.getItemName());
            
            // 使用检查项线程池异步执行检查，并保存Future以便取消
            Future<Boolean> checkFuture = checkQueueManager.getItemCheckExecutorService().submit(() -> {
                try {
                    ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
                    if (checker == null) {
                        checkItem.setMessage("未知的检查项");
                        appendLog(logKey, "错误: 未知的检查项\n");
                        appendLog(logKey, "==== 检查失败 ====\n");
                        return false;
                    }

                    // 记录检查开始
                    appendLog(logKey, "正在执行检查...\n");
                    
                    // 执行检查并获取结果
                    CheckItem resultItem = checker.check(clusterId, hostInfo, checkItem);
                    
                    // 记录检查结果
                    appendLog(logKey, "检查结果: " + resultItem.getStatus() + "\n");
                    appendLog(logKey, "详细信息: " + resultItem.getMessage() + "\n");
                    appendLog(logKey, "==== 检查完成 ====\n");
                    
                    return resultItem.getStatus() == CheckItem.Status.SUCCESS;
                } catch (Exception e) {
                    logger.error("执行检查失败: {}", e.getMessage(), e);
                    checkItem.setMessage("检查失败: " + e.getMessage());
                    
                    // 记录异常信息
                    appendLog(logKey, "检查过程中发生异常: " + e.getMessage() + "\n");
                    appendLog(logKey, "==== 检查失败 ====\n");
                    
                    return false;
                }
            });

            // 保存Future到缓存，以便后续可以取消
            String futureKey = CHECK_TASK_FUTURE_PREFIX + clusterId + "_" + hostInfo.getHostname() + "_" + checkItem.getId();
            CacheUtils.put(futureKey, checkFuture);
            
            try {
                // 等待任务完成，超时30秒
                Boolean result = checkFuture.get(30, TimeUnit.SECONDS);
                logger.info("检查项 {} 执行完成，结果: {}", checkItem.getItemName(), result ? "成功" : "失败");
                
                // 再次添加一个日志行，表明任务已完成
                appendLog(logKey, "检查任务已完成，异步线程退出\n");
                
                return result != null && result;
            } catch (TimeoutException e) {
                logger.warn("检查项 {} 执行超时", checkItem.getItemName());
                checkItem.setMessage("检查执行超时");
                
                // 记录超时信息
                appendLog(logKey, "检查执行超时，已超过30秒\n");
                
                return false;
            } catch (InterruptedException e) {
                logger.info("检查项 {} 执行被中断", checkItem.getItemName());
                Thread.currentThread().interrupt();
                checkItem.setMessage("检查被中断");
                
                // 记录中断信息
                appendLog(logKey, "检查执行被中断\n");
                
                return false;
            } catch (Exception e) {
                logger.error("检查项 {} 执行异常: {}", checkItem.getItemName(), e.getMessage(), e);
                checkItem.setMessage("检查执行异常: " + e.getMessage());
                
                // 记录异常信息
                appendLog(logKey, "检查执行过程中发生异常: " + e.getMessage() + "\n");
                
                return false;
            }
        } catch (Exception e) {
            logger.error("提交检查任务失败: {}", e.getMessage(), e);
            checkItem.setMessage("提交检查任务失败: " + e.getMessage());
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
                
                // 获取任务Future并尝试取消
                String futureKey = CHECK_TASK_FUTURE_PREFIX + clusterId + "_" + hostname + "_" + itemId;
                Future<?> future = (Future<?>) CacheUtils.get(futureKey);
                
                if (future != null && !future.isDone()) {
                    logger.info("正在取消检查项任务: clusterId={}, hostname={}, itemId={}", clusterId, hostname, itemId);
                    
                    // 使用mayInterruptIfRunning=true尝试中断正在运行的任务
                    boolean cancelled = future.cancel(true);
                    logger.info("取消检查项任务结果: {}", cancelled ? "成功" : "失败");
                    
                    // 如果取消失败，最多重试3次
                    if (!cancelled) {
                        for (int i = 0; i < 3; i++) {
                            logger.info("取消任务失败，尝试第{}次重试", i + 1);
                            // 稍等一下再重试
                            Thread.sleep(200);
                            cancelled = future.cancel(true);
                            if (cancelled) {
                                logger.info("重试后成功取消任务");
                                break;
                            }
                        }
                    }
                    
                    CacheUtils.removeKey(futureKey);
                } else {
                    logger.info("未找到任务Future或任务已完成: clusterId={}, hostname={}, itemId={}", 
                               clusterId, hostname, itemId);
                }
                
                // 调用新方法，只取消单个检查项
                checkQueueManager.cancelItemTask(clusterId, hostname, itemId);
                
                // 等待一小段时间，让前端有时间显示"终止中"状态
                // 同时给任务足够时间响应中断请求
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 最终检查任务是否真的被取消了，如果Future已存在但仍未完成，记录警告日志
                future = (Future<?>) CacheUtils.get(futureKey);
                if (future != null && !future.isDone()) {
                    logger.warn("任务可能未能成功取消，将强制更新状态: clusterId={}, hostname={}, itemId={}", 
                                clusterId, hostname, itemId);
                }
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

    @Override
    public Result getCheckItemLog(Integer clusterId, String hostname, Integer itemId) {
        // 构建日志缓存键
        String logKey = CHECK_ITEM_LOG_PREFIX + clusterId + "_" + hostname + "_" + itemId;
        logger.info("获取检查项日志, clusterId: {}, hostname: {}, itemId: {}, 缓存键: {}", 
            clusterId, hostname, itemId, logKey);
        
        // 从缓存中获取日志
        String log = (String) CacheUtils.get(logKey);
        
        if (log == null || log.isEmpty()) {
            logger.warn("未找到检查项日志, 缓存键: {}", logKey);
            
            // 尝试获取检查项信息，验证检查项是否存在
            HostInfo hostInfo = getHostInfo(clusterId, hostname);
            if (hostInfo == null) {
                logger.warn("未找到主机: {}, clusterId: {}", hostname, clusterId);
                return Result.success("未找到该主机信息，无法获取日志");
            }
            
            CheckItem checkItem = findCheckItemById(hostInfo, itemId);
            if (checkItem == null) {
                logger.warn("未找到检查项: {} 在主机: {}", itemId, hostname);
                return Result.success("未找到该检查项，无法获取日志");
            }
            
            // 检查项存在但没有日志，可能是日志尚未生成或已过期
            logger.info("检查项存在但未找到日志, 检查项: {}, 状态: {}, 消息: {}",
                checkItem.getItemName(), checkItem.getStatus(), checkItem.getMessage());
                
            // 尝试创建一个基本日志
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("==== 检查项信息 ====\n");
            logBuilder.append("检查项: ").append(checkItem.getItemName()).append("\n");
            logBuilder.append("状态: ").append(checkItem.getStatus()).append("\n");
            logBuilder.append("消息: ").append(checkItem.getMessage()).append("\n");
            logBuilder.append("\n注意：此为系统生成的基本信息，未找到详细日志。\n");
            logBuilder.append("可能原因：\n");
            logBuilder.append("1. 检查尚未开始或进行中\n");
            logBuilder.append("2. 日志已过期被清除\n");
            logBuilder.append("3. 检查过程中未产生日志\n");
            
            // 返回基本信息
            return Result.success(logBuilder.toString());
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