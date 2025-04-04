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
     * @param errorMessage 错误消息（如果失败）
     */
    public static void updateHostProcessStatus(String taskId, String ip, boolean success, String errorMessage) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            logger.warn("任务{}不存在，无法更新主机{}的处理状态", taskId, ip);
            return;
        }

        // 更新当前处理的主机
        progress.setCurrentHost(ip);

        if (success) {
            // 添加到成功列表
            progress.getCompletedHosts().add(ip);
            progress.setCompletedCount(progress.getCompletedCount() + 1);
        } else {
            // 添加到失败列表
            progress.getFailedHosts().put(ip, errorMessage);
            progress.setFailedCount(progress.getFailedCount() + 1);
        }

        // 从待处理列表中移除
        if (progress.getPendingHosts() != null) {
            progress.getPendingHosts().remove(ip);
        }

        // 计算完成百分比
        progress.setPercentage(
                (int) (((double) (progress.getCompletedCount() + progress.getFailedCount())
                        / progress.getTotalHosts()) * 100));
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