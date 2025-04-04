package com.datasophon.api.service.impl;

import com.datasophon.api.service.impl.HostCheckServiceImpl.TaskProgress;
import com.datasophon.api.service.impl.HostCheckServiceImpl.TaskStatus;
import com.datasophon.common.model.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.HashMap;

/**
 * 任务进度帮助类
 * 用于在不同任务类型中统一处理待处理主机列表
 */
public class TaskProgressHelper {
    private static final Logger logger = LoggerFactory.getLogger(TaskProgressHelper.class);

    /**
     * 存储任务进度的缓存
     */
    private static final Map<String, TaskProgress> taskProgressMap = new ConcurrentHashMap<>();

    /**
     * 初始化同步hosts文件任务的任务进度
     * 
     * @param clusterId 集群ID
     * @param hostMap   主机信息
     * @return 任务ID
     */
    public static String initSyncHostsFileTask(Integer clusterId, Map<String, HostInfo> hostMap) {
        String taskId = "sync_hosts_" + clusterId + "_" + System.currentTimeMillis();

        // 创建任务进度对象
        TaskProgress progress = new TaskProgress();
        progress.setTaskId(taskId);
        progress.setStatus(TaskStatus.IN_PROGRESS);
        progress.setCompletedHosts(new ArrayList<>());
        progress.setFailedHosts(new ConcurrentHashMap<>());
        progress.setTotalHosts(hostMap.size());
        progress.setCompletedCount(0);
        progress.setFailedCount(0);
        progress.setPercentage(0);
        progress.setMessage("同步hosts文件任务已启动");

        // 初始化待处理主机列表
        List<String> allHosts = new ArrayList<>(hostMap.keySet());
        progress.setPendingHosts(allHosts);

        // 存储任务进度
        taskProgressMap.put(taskId, progress);

        return taskId;
    }

    /**
     * 初始化批量设置主机名任务的任务进度
     * 
     * @param hostnamePreview 主机名预览信息
     * @return 任务ID
     */
    public static String initBatchSetHostnameTask(Integer clusterId, List<Map<String, String>> hostnamePreview) {
        String taskId = "set_hostname_" + clusterId + "_" + System.currentTimeMillis();

        // 创建任务进度对象
        TaskProgress progress = new TaskProgress();
        progress.setTaskId(taskId);
        progress.setStatus(TaskStatus.IN_PROGRESS);
        progress.setCompletedHosts(new ArrayList<>());
        progress.setFailedHosts(new ConcurrentHashMap<>());
        progress.setTotalHosts(hostnamePreview.size());
        progress.setCompletedCount(0);
        progress.setFailedCount(0);
        progress.setPercentage(0);
        progress.setMessage("批量设置主机名任务已启动");

        // 初始化待处理主机列表
        List<String> pendingHosts = hostnamePreview.stream()
                .map(item -> item.get("ip"))
                .collect(Collectors.toList());
        progress.setPendingHosts(pendingHosts);

        // 存储任务进度
        taskProgressMap.put(taskId, progress);

        return taskId;
    }

    /**
     * 更新主机处理状态
     * 
     * @param taskId       任务ID
     * @param ip           主机IP
     * @param success      是否成功
     * @param errorMessage 错误消息
     */
    public static void updateHostProcessStatus(String taskId, String ip, boolean success, String errorMessage) {
        // 参数检查
        if (taskId == null || taskId.trim().isEmpty()) {
            logger.warn("任务ID为空，无法更新主机处理状态");
            return;
        }

        if (ip == null || ip.trim().isEmpty()) {
            logger.warn("主机IP为空，无法更新处理状态，任务ID: {}", taskId);
            return;
        }

        if (taskProgressMap == null) {
            logger.warn("任务进度映射为null，无法更新主机{}的处理状态", ip);
            return;
        }

        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            logger.warn("任务{}不存在，无法更新主机{}的处理状态", taskId, ip);
            return;
        }

        try {
            // 更新当前处理的主机
            progress.setCurrentHost(ip);

            if (success) {
                // 添加到成功列表
                if (progress.getCompletedHosts() == null) {
                    progress.setCompletedHosts(new ArrayList<>());
                }
                progress.getCompletedHosts().add(ip);
                progress.setCompletedCount(progress.getCompletedCount() + 1);
            } else {
                // 添加到失败列表
                if (progress.getFailedHosts() == null) {
                    progress.setFailedHosts(new ConcurrentHashMap<>());
                }
                progress.getFailedHosts().put(ip, errorMessage);
                progress.setFailedCount(progress.getFailedCount() + 1);
            }

            // 从待处理列表中移除
            if (progress.getPendingHosts() != null) {
                progress.getPendingHosts().remove(ip);
            }

            // 计算完成百分比
            int totalHosts = progress.getTotalHosts();
            if (totalHosts > 0) {
                progress.setPercentage(
                        (int) (((double) (progress.getCompletedCount() + progress.getFailedCount())
                                / totalHosts) * 100));
            } else {
                progress.setPercentage(100); // 如果总主机数为0，则设置为100%完成
            }
        } catch (Exception e) {
            logger.error("更新主机处理状态时发生异常: {}, 任务ID: {}, 主机IP: {}", e.getMessage(), taskId, ip, e);
        }
    }

    /**
     * 完成任务
     * 
     * @param taskId                任务ID
     * @param successMessage        成功消息
     * @param partialSuccessMessage 部分成功消息
     */
    public static void completeTask(String taskId, String successMessage, String partialSuccessMessage) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            logger.warn("任务{}不存在，无法标记为完成", taskId);
            return;
        }

        // 更新任务状态
        progress.setStatus(TaskStatus.COMPLETED);
        if (progress.getFailedCount() == 0) {
            progress.setMessage(successMessage);
        } else {
            progress.setMessage(partialSuccessMessage);
        }
    }

    /**
     * 获取任务进度
     * 
     * @param taskId 任务ID
     * @return 任务进度对象，如果不存在则返回null
     */
    public static TaskProgress getTaskProgress(String taskId) {
        return taskProgressMap.get(taskId);
    }

    /**
     * 移除任务进度
     * 
     * @param taskId 任务ID
     */
    public static void removeTaskProgress(String taskId) {
        taskProgressMap.remove(taskId);
    }
}