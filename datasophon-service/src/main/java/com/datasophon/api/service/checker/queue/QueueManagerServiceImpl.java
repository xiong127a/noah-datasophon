package com.datasophon.api.service.checker.queue;

import com.datasophon.api.service.checker.AsyncCheckService;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.SshConnectionPoolManager;
import com.datasophon.common.enums.ScopeCode;
import com.datasophon.common.model.AsyncServiceStatus;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OperationResult;
import com.datasophon.common.model.QueueManagerStatus;
import com.datasophon.common.model.QueueSystemStatus;
import com.datasophon.common.model.QueueTaskDetailResult;
import com.datasophon.common.model.QueueTaskInfo;
import com.datasophon.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 统一队列管理服务
 * 
 * 提供SSH连接池管理和队列系统状态管理
 */
@Slf4j
@Service
public class QueueManagerServiceImpl implements QueueManagerService {

    @Autowired
    private HostCheckQueueManager hostCheckQueueManager;

    @Autowired
    @Lazy
    private AsyncCheckService asyncCheckService;

    @Autowired
    private SshConnectionPoolManager sshConnectionPoolManager;

    // 添加各种执行器
    @Autowired
    @Qualifier("checkExecutor")
    private ExecutorService checkExecutor;

    @Autowired
    @Qualifier("fixExecutor")
    private ExecutorService fixExecutor;

    @Autowired
    @Qualifier("osInfoExecutor")
    private ExecutorService osInfoExecutor;

    @Autowired
    @Qualifier("hardwareInfoExecutor")
    private ExecutorService hardwareInfoExecutor;

    @Autowired
    @Qualifier("hostsFileExecutor")
    private ExecutorService hostsFileExecutor;

    @Autowired
    @Qualifier("hostnameExecutor")
    private ExecutorService hostnameExecutor;

    /**
     * 获取队列系统状态
     * 
     * @return 队列系统状态
     */
    @Override
    public Result getQueueSystemStatus() {
        try {
            // 调用内部方法直接获取数据对象
            QueueSystemStatus queueSystemStatus = getQueueSystemStatusDirect();
            return Result.success(queueSystemStatus);
        } catch (Exception e) {
            log.error("获取队列系统状态失败", e);
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：直接获取队列系统状态，不包装Result
     * 
     * @return 队列系统状态对象
     */
    @Override
    public QueueSystemStatus getQueueSystemStatusDirect() {
        // 直接获取实体类对象
        QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
        AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();

        // 创建QueueSystemStatus对象
        QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
        queueSystemStatus.setQueueManager(queueManagerStatus);
        queueSystemStatus.setAsyncService(asyncServiceStatus);

        return queueSystemStatus;
    }

    /**
     * 暂停队列系统
     * 
     * @param scope 范围代码
     * @return 操作结果
     */
    @Override
    public Result pauseQueueSystem(String scope) {
        try {
            // 调用内部方法
            OperationResult result = pauseQueueSystemDirect(scope);
            return Result.success(result);
        } catch (Exception e) {
            log.error("暂停队列系统失败", e);
            return Result.error("暂停操作失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：直接暂停队列系统
     * 
     * @param scope 范围代码
     * @return 操作结果对象
     */
    private OperationResult pauseQueueSystemDirect(String scope) {
        ScopeCode scopeCode = ScopeCode.valueOf(scope.toUpperCase());
        StringBuilder messageBuilder = new StringBuilder("已暂停");

        // 根据作用范围执行暂停操作
        if (scopeCode == ScopeCode.ALL || scopeCode == ScopeCode.SCHEDULER) {
            // 暂停AsyncCheckService的定时任务
            asyncCheckService.stopScheduledTasks();
            if (scopeCode == ScopeCode.SCHEDULER) {
                messageBuilder.append("定时任务");
            }
        }

        if (scopeCode == ScopeCode.ALL || scopeCode == ScopeCode.QUEUE) {
            // 暂停队列处理
            hostCheckQueueManager.pauseQueueProcessing();
            if (scopeCode == ScopeCode.QUEUE) {
                messageBuilder.append("队列处理");
            } else if (scopeCode == ScopeCode.ALL) {
                messageBuilder.append("队列处理和定时任务");
            }
        }

        // 获取最新状态
        QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
        AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();

        // 创建QueueSystemStatus对象
        QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
        queueSystemStatus.setQueueManager(queueManagerStatus);
        queueSystemStatus.setAsyncService(asyncServiceStatus);

        // 创建操作结果
        OperationResult result = new OperationResult();
        result.setSuccess(true);
        result.setMessage(messageBuilder.toString());
        result.setStatus(queueSystemStatus);
        result.setTimestamp(System.currentTimeMillis());

        return result;
    }

    /**
     * 恢复队列系统
     * 
     * @param scope 范围代码
     * @return 操作结果
     */
    @Override
    public Result resumeQueueSystem(String scope) {
        try {
            // 调用内部方法
            OperationResult result = resumeQueueSystemDirect(scope);
            return Result.success(result);
        } catch (Exception e) {
            log.error("恢复队列系统失败", e);
            return Result.error("恢复操作失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：直接恢复队列系统
     * 
     * @param scope 范围代码
     * @return 操作结果对象
     */
    private OperationResult resumeQueueSystemDirect(String scope) {
        ScopeCode scopeCode = ScopeCode.valueOf(scope.toUpperCase());
        StringBuilder messageBuilder = new StringBuilder("已恢复");

        // 根据作用范围执行恢复操作
        if (scopeCode == ScopeCode.ALL || scopeCode == ScopeCode.SCHEDULER) {
            // 恢复AsyncCheckService的定时任务
            asyncCheckService.startScheduledTasks();
            if (scopeCode == ScopeCode.SCHEDULER) {
                messageBuilder.append("定时任务");
            }
        }

        if (scopeCode == ScopeCode.ALL || scopeCode == ScopeCode.QUEUE) {
            // 恢复队列处理
            hostCheckQueueManager.resumeQueueProcessing();
            if (scopeCode == ScopeCode.QUEUE) {
                messageBuilder.append("队列处理");
            } else if (scopeCode == ScopeCode.ALL) {
                messageBuilder.append("队列处理和定时任务");
            }
        }

        // 获取最新状态
        QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
        AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();

        // 创建QueueSystemStatus对象
        QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
        queueSystemStatus.setQueueManager(queueManagerStatus);
        queueSystemStatus.setAsyncService(asyncServiceStatus);

        // 创建操作结果
        OperationResult result = new OperationResult();
        result.setSuccess(true);
        result.setMessage(messageBuilder.toString());
        result.setStatus(queueSystemStatus);
        result.setTimestamp(System.currentTimeMillis());

        return result;
    }

    /**
     * 关闭队列系统
     * 
     * @return 操作结果
     */
    @Override
    public Result shutdownQueueSystem() {
        try {
            // 调用内部方法
            OperationResult result = shutdownQueueSystemDirect();
            return Result.success(result);
        } catch (Exception e) {
            log.error("关闭队列系统失败", e);
            return Result.error("关闭操作失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：直接关闭队列系统
     * 
     * @return 操作结果对象
     */
    private OperationResult shutdownQueueSystemDirect() {
        // 为安全起见，先取消所有任务
        hostCheckQueueManager.cancelAllTasks();

        // 完全关闭队列管理器
        hostCheckQueueManager.shutdown();

        // 禁用AsyncCheckService的定时任务
        asyncCheckService.disableScheduledTasks();

        // 创建状态对象
        QueueSystemStatus queueSystemStatus = new QueueSystemStatus();

        QueueManagerStatus queueManagerStatus = new QueueManagerStatus();
        queueManagerStatus.setRunning(false);

        AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();

        queueSystemStatus.setQueueManager(queueManagerStatus);
        queueSystemStatus.setAsyncService(asyncServiceStatus);

        // 创建操作结果
        OperationResult result = new OperationResult();
        result.setSuccess(true);
        result.setMessage("已完全关闭队列管理器，定时任务已禁用");
        result.setStatus(queueSystemStatus);
        result.setTimestamp(System.currentTimeMillis());

        return result;
    }

    /**
     * 管理队列系统
     * 
     * @param action 动作
     * @param scope  范围代码
     * @return 操作结果
     */
    @Override
    public Result manageQueueSystem(String action, String scope) {
        log.info("处理队列管理请求: action={}, scope={}", action, scope);

        try {
            // 根据action类型执行不同操作
            if ("status".equalsIgnoreCase(action)) {
                return getQueueSystemStatus();
            } else if ("pause".equalsIgnoreCase(action)) {
                return pauseQueueSystem(scope);
            } else if ("resume".equalsIgnoreCase(action)) {
                return resumeQueueSystem(scope);
            } else if ("shutdown".equalsIgnoreCase(action)) {
                return shutdownQueueSystem();
            } else {
                return Result.error(400, "不支持的操作: " + action);
            }
        } catch (Exception e) {
            log.error("控制队列管理器时发生错误", e);
            return Result.error(500, "操作异常: " + e.getMessage());
        }
    }

    /**
     * 清理不活跃的SSH连接
     * 
     * @return 操作结果，包含清理的连接数
     */
    @Override
    public Result cleanupConnections() {
        try {
            // 调用内部方法
            OperationResult result = cleanupConnectionsDirect();
            return Result.success(result);
        } catch (Exception e) {
            log.error("清理SSH连接失败", e);
            return Result.error("清理SSH连接失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：直接清理不活跃的SSH连接
     * 
     * @return 操作结果对象
     */
    private OperationResult cleanupConnectionsDirect() {
        // 手动触发连接清理
        sshConnectionPoolManager.manualCleanupConnections();

        // 获取清理后的连接信息
        AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();

        // 创建操作结果
        OperationResult result = new OperationResult();
        result.setSuccess(true);
        result.setMessage("已清理不活跃SSH连接");

        // 创建状态对象
        QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
        QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
        queueSystemStatus.setQueueManager(queueManagerStatus);
        queueSystemStatus.setAsyncService(asyncServiceStatus);
        result.setStatus(queueSystemStatus);

        result.setTimestamp(System.currentTimeMillis());

        return result;
    }

    /**
     * 暂停指定的定时任务
     * 
     * @param taskId 任务ID
     * @return 操作结果
     */
    @Override
    public Result pauseScheduledTask(String taskId) {
        try {
            // 调用内部方法
            OperationResult result = pauseScheduledTaskDirect(taskId);
            if (result.isSuccess()) {
                return Result.success(result);
            } else {
                return Result.error(result.getMessage());
            }
        } catch (Exception e) {
            log.error("暂停定时任务失败", e);
            return Result.error("暂停定时任务失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：直接暂停指定的定时任务
     * 
     * @param taskId 任务ID
     * @return 操作结果对象
     */
    private OperationResult pauseScheduledTaskDirect(String taskId) {
        boolean success = false;
        String message = "";

        // 根据任务ID执行相应操作
        switch (taskId) {
            case "taskCleanup":
                asyncCheckService.stopTaskCleanup();
                message = "已暂停任务清理定时任务";
                success = true;
                break;
            case "connectionCleanup":
                asyncCheckService.stopConnectionCleanup();
                message = "已暂停连接清理定时任务";
                success = true;
                break;
            case "queueHealthMonitor":
                hostCheckQueueManager.stopQueueHealthMonitor();
                message = "已暂停队列健康监控定时任务";
                success = true;
                break;
            case "taskTimeoutMonitor":
                hostCheckQueueManager.stopTaskTimeoutMonitor();
                message = "已暂停任务超时监控定时任务";
                success = true;
                break;
            default:
                message = "无效的任务ID: " + taskId;
                success = false;
        }

        // 如果任务ID无效直接返回错误
        if (!success) {
            OperationResult result = new OperationResult();
            result.setSuccess(false);
            result.setMessage(message);
            result.setTimestamp(System.currentTimeMillis());
            return result;
        }

        // 获取最新状态
        QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
        AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();

        // 创建状态对象
        QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
        queueSystemStatus.setQueueManager(queueManagerStatus);
        queueSystemStatus.setAsyncService(asyncServiceStatus);

        // 创建操作结果
        OperationResult result = new OperationResult();
        result.setSuccess(success);
        result.setMessage(message);
        result.setStatus(queueSystemStatus);
        result.setTimestamp(System.currentTimeMillis());

        return result;
    }

    /**
     * 恢复指定的定时任务
     * 
     * @param taskId 任务ID
     * @return 操作结果
     */
    @Override
    public Result resumeScheduledTask(String taskId) {
        try {
            // 调用内部方法
            OperationResult result = resumeScheduledTaskDirect(taskId);
            if (result.isSuccess()) {
                return Result.success(result);
            } else {
                return Result.error(result.getMessage());
            }
        } catch (Exception e) {
            log.error("恢复定时任务失败", e);
            return Result.error("恢复定时任务失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：直接恢复指定的定时任务
     * 
     * @param taskId 任务ID
     * @return 操作结果对象
     */
    private OperationResult resumeScheduledTaskDirect(String taskId) {
        boolean success = false;
        String message = "";

        // 根据任务ID执行相应操作
        switch (taskId) {
            case "taskCleanup":
                asyncCheckService.startTaskCleanup();
                message = "已恢复任务清理定时任务";
                success = true;
                break;
            case "connectionCleanup":
                asyncCheckService.startConnectionCleanup();
                message = "已恢复连接清理定时任务";
                success = true;
                break;
            case "queueHealthMonitor":
                hostCheckQueueManager.startQueueHealthMonitor();
                message = "已恢复队列健康监控定时任务";
                success = true;
                break;
            case "taskTimeoutMonitor":
                hostCheckQueueManager.startTaskTimeoutMonitor();
                message = "已恢复任务超时监控定时任务";
                success = true;
                break;
            default:
                message = "无效的任务ID: " + taskId;
                success = false;
        }

        // 如果任务ID无效直接返回错误
        if (!success) {
            OperationResult result = new OperationResult();
            result.setSuccess(false);
            result.setMessage(message);
            result.setTimestamp(System.currentTimeMillis());
            return result;
        }

        // 获取最新状态
        QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
        AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();

        // 创建状态对象
        QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
        queueSystemStatus.setQueueManager(queueManagerStatus);
        queueSystemStatus.setAsyncService(asyncServiceStatus);

        // 创建操作结果
        OperationResult result = new OperationResult();
        result.setSuccess(success);
        result.setMessage(message);
        result.setStatus(queueSystemStatus);
        result.setTimestamp(System.currentTimeMillis());

        return result;
    }

    /**
     * 获取检查任务队列详情
     * 
     * @return 任务队列中的详细任务信息
     */
    @Override
    public Result getCheckQueueTasks() {
        try {
            // 调用内部方法直接获取数据
            List<QueueTaskInfo> checkTasks = getCheckQueueTasksDirect();
            return Result.success(checkTasks);
        } catch (Exception e) {
            log.error("获取检查任务队列详情失败", e);
            return Result.error("获取队列详情失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：直接获取检查任务队列详情，不包装Result
     * 
     * @return 任务队列中的详细任务信息
     */
    @Override
    public List<QueueTaskInfo> getCheckQueueTasksDirect() {
        return hostCheckQueueManager.getQueueTasksDetails();
    }

    /**
     * 获取修复任务队列详情
     * 
     * @return 修复任务队列中的详细任务信息
     */
    @Override
    public Result getFixQueueTasks() {
        try {
            // 调用内部方法直接获取数据
            List<QueueTaskInfo> fixTasks = getFixQueueTasksDirect();
            return Result.success(fixTasks);
        } catch (Exception e) {
            log.error("获取修复任务队列详情失败", e);
            return Result.error("获取队列详情失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：直接获取修复任务队列详情，不包装Result
     * 
     * @return 修复任务队列中的详细任务信息
     */
    @Override
    public List<QueueTaskInfo> getFixQueueTasksDirect() {
        return hostCheckQueueManager.getFixQueueTasksDetails();
    }

    /**
     * 统一处理队列管理器请求，并在状态请求时自动添加队列任务详情
     * 
     * @param action    操作类型
     * @param scopeCode 作用范围
     * @param taskId    定时任务ID
     * @return 操作结果
     */
    @Override
    public Result manageQueueManagerWithDetails(String action, String scopeCode, String taskId) {
        try {
            // 根据action类型执行相应操作
            if ("status".equalsIgnoreCase(action)) {
                // 获取队列系统状态
                QueueSystemStatus queueSystemStatus = getQueueSystemStatusDirect();

                try {
                    // 直接使用内部方法获取数据，避免重复的错误处理
                    List<QueueTaskInfo> checkQueueTasks = getCheckQueueTasksDirect();
                    List<QueueTaskInfo> fixQueueTasks = getFixQueueTasksDirect();

                    // 添加队列任务详情
                    QueueTaskDetailResult queueTaskDetailResult = new QueueTaskDetailResult();
                    queueTaskDetailResult.setQueueManager(queueSystemStatus.getQueueManager());
                    queueTaskDetailResult.setAsyncService(queueSystemStatus.getAsyncService());
                    queueTaskDetailResult.setQueueTasks(checkQueueTasks);
                    queueTaskDetailResult.setFixQueueTasks(fixQueueTasks);

                    return Result.success(queueTaskDetailResult);
                } catch (Exception e) {
                    log.error("获取队列任务详情失败", e);
                    // 如果获取任务详情失败，至少返回系统状态
                    return Result.success(queueSystemStatus);
                }
            } else if ("pauseTask".equalsIgnoreCase(action)) {
                if (taskId == null || taskId.isEmpty()) {
                    return Result.error("暂停定时任务时需要提供taskId");
                }
                OperationResult result = pauseScheduledTaskDirect(taskId);
                if (result.isSuccess()) {
                    return Result.success(result);
                } else {
                    return Result.error(result.getMessage());
                }
            } else if ("resumeTask".equalsIgnoreCase(action)) {
                if (taskId == null || taskId.isEmpty()) {
                    return Result.error("恢复定时任务时需要提供taskId");
                }
                OperationResult result = resumeScheduledTaskDirect(taskId);
                if (result.isSuccess()) {
                    return Result.success(result);
                } else {
                    return Result.error(result.getMessage());
                }
            } else if ("cleanupConnections".equalsIgnoreCase(action)) {
                OperationResult result = cleanupConnectionsDirect();
                return Result.success(result);
            } else {
                // 其他操作通过manageQueueSystem处理
                return manageQueueSystem(action, scopeCode);
            }
        } catch (Exception e) {
            log.error("处理队列管理器请求失败", e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务间隔
     * 
     * @param taskId     任务ID
     * @param intervalMs 新的执行间隔（毫秒）
     * @return 操作结果
     */
    @Override
    public Result updateTaskInterval(String taskId, long intervalMs) {
        try {
            // 调用内部方法
            OperationResult result = updateTaskIntervalDirect(taskId, intervalMs);
            if (result.isSuccess()) {
                return Result.success(result);
            } else {
                return Result.error(result.getMessage());
            }
        } catch (Exception e) {
            log.error("修改定时任务执行间隔失败", e);
            return Result.error("修改执行间隔失败: " + e.getMessage());
        }
    }

    /**
     * 内部方法：直接修改定时任务执行间隔
     * 
     * @param taskId     任务ID
     * @param intervalMs 新的执行间隔（毫秒）
     * @return 操作结果对象
     */
    private OperationResult updateTaskIntervalDirect(String taskId, long intervalMs) {
        boolean success = false;
        String message = "";

        // 根据任务ID执行相应操作
        try {
            switch (taskId) {
                case "taskCleanup":
                    asyncCheckService.updateTaskCleanupInterval(intervalMs);
                    message = "已更新任务清理定时任务执行间隔为: " + (intervalMs / 1000) + "秒";
                    success = true;
                    break;
                case "connectionCleanup":
                    asyncCheckService.updateConnectionCleanupInterval(intervalMs);
                    message = "已更新连接清理定时任务执行间隔为: " + (intervalMs / 1000) + "秒";
                    success = true;
                    break;
                case "queueHealthMonitor":
                    // 检查HostCheckQueueManager是否提供此方法
                    if (hostCheckQueueManager != null) {
                        // 此方法可能不存在，包装在try-catch中
                        try {
                            hostCheckQueueManager.updateQueueHealthMonitorInterval(intervalMs);
                            message = "已更新队列健康监控定时任务执行间隔为: " + (intervalMs / 1000) + "秒";
                            success = true;
                        } catch (Exception e) {
                            message = "无法更新队列健康监控间隔: " + e.getMessage();
                            success = false;
                        }
                    } else {
                        message = "队列管理器不可用";
                        success = false;
                    }
                    break;
                case "taskTimeoutMonitor":
                    // 检查HostCheckQueueManager是否提供此方法
                    if (hostCheckQueueManager != null) {
                        // 此方法可能不存在，包装在try-catch中
                        try {
                            hostCheckQueueManager.updateTaskTimeoutMonitorInterval(intervalMs);
                            message = "已更新任务超时监控定时任务执行间隔为: " + (intervalMs / 1000) + "秒";
                            success = true;
                        } catch (Exception e) {
                            message = "无法更新任务超时监控间隔: " + e.getMessage();
                            success = false;
                        }
                    } else {
                        message = "队列管理器不可用";
                        success = false;
                    }
                    break;
                default:
                    message = "无效的任务ID: " + taskId;
                    success = false;
            }
        } catch (Exception e) {
            message = "更新任务间隔时发生错误: " + e.getMessage();
            success = false;
        }

        // 如果任务ID无效直接返回错误
        if (!success) {
            OperationResult result = new OperationResult();
            result.setSuccess(false);
            result.setMessage(message);
            result.setTimestamp(System.currentTimeMillis());
            return result;
        }

        // 获取最新状态
        QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
        AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();

        // 创建状态对象
        QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
        queueSystemStatus.setQueueManager(queueManagerStatus);
        queueSystemStatus.setAsyncService(asyncServiceStatus);

        // 创建操作结果
        OperationResult result = new OperationResult();
        result.setSuccess(success);
        result.setMessage(message);
        result.setStatus(queueSystemStatus);
        result.setTimestamp(System.currentTimeMillis());

        return result;
    }

    /**
     * 执行检查任务
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @return 任务ID
     */
    public String executeCheckTask(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        return asyncCheckService.executeCheckItemAsync(clusterId, hostInfo, checkItem);
    }

    /**
     * 执行修复任务
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param fixItem   修复项
     * @return 任务ID
     */
    public String executeFixTask(Integer clusterId, HostInfo hostInfo, CheckItem fixItem) {
        return asyncCheckService.executeFixItemAsync(clusterId, hostInfo, fixItem);
    }

    /**
     * 批量检查主机
     * 
     * @param clusterId  集群ID
     * @param hostInfo   主机信息
     * @param checkItems 检查项列表
     * @return 检查结果
     */
    public List<CheckItem> batchCheckHost(Integer clusterId, HostInfo hostInfo, List<CheckItem> checkItems) {
        // 获取SSH连接
        ClientSession session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);

        if (session == null) {
            // 无法创建连接，标记所有检查项为失败
            for (CheckItem item : checkItems) {
                item.setStatus(CheckItem.Status.FAILED);
                item.setMessage("无法创建SSH连接");
            }
            return checkItems;
        }

        // 设置使用现有会话
        hostInfo.setUseExistingSession(true);
        hostInfo.setExternalSession(session);

        try {
            // 执行批量检查
            return asyncCheckService.batchExecuteCheck(clusterId, hostInfo, checkItems);
        } finally {
            // 清理，但不关闭连接
            hostInfo.setUseExistingSession(false);
            hostInfo.setExternalSession(null);
        }
    }

    /**
     * 批量修复主机
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param fixItems  修复项列表
     * @return 修复结果
     */
    public List<CheckItem> batchFixHost(Integer clusterId, HostInfo hostInfo, List<CheckItem> fixItems) {
        // 检查是否包含免密登录检查项
        boolean containsPasswordFreeChecker = fixItems.stream()
                .anyMatch(item -> "PASSWORD_FREE".equals(item.getItemCode()));

        ClientSession session = null;

        // 如果不是免密修复，则获取连接
        if (!containsPasswordFreeChecker) {
            session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);

            if (session == null) {
                // 无法创建连接，标记所有修复项为失败
                for (CheckItem item : fixItems) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("无法创建SSH连接");
                }
                return fixItems;
            }

            // 设置使用现有会话
            hostInfo.setUseExistingSession(true);
            hostInfo.setExternalSession(session);
        }

        try {
            // 执行批量修复
            return asyncCheckService.batchExecuteFix(clusterId, hostInfo, fixItems);
        } finally {
            if (session != null) {
                // 清理，但不关闭连接
                hostInfo.setUseExistingSession(false);
                hostInfo.setExternalSession(null);
            }
        }
    }

    /**
     * 执行命令
     * 
     * @param hostInfo 主机信息
     * @param command  命令
     * @return 命令执行结果
     */
    public CompletableFuture<CommandResult> executeCommand(HostInfo hostInfo, String command) {
        // 获取SSH连接
        ClientSession session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);

        if (session == null) {
            CompletableFuture<CommandResult> future = new CompletableFuture<>();
            future.complete(new CommandResult("", "无法创建SSH连接", -1));
            return future;
        }

        // 根据命令类型选择合适的执行器执行命令
        return sshConnectionPoolManager.execCommandByType(
                session,
                command,
                hardwareInfoExecutor,
                osInfoExecutor,
                checkExecutor);
    }

    /**
     * 同步主机文件
     * 
     * @param taskId           任务ID
     * @param clusterId        集群ID
     * @param hostMap          主机映射
     * @param hostsFilePreview 主机文件预览
     */
    public void syncHostsFileTask(String taskId, Integer clusterId, Map<String, HostInfo> hostMap,
            Object hostsFilePreview) {
        asyncCheckService.syncHostsFileTask(taskId, clusterId, hostMap, hostsFilePreview);
    }

    /**
     * 批量设置主机名
     * 
     * @param taskId          任务ID
     * @param clusterId       集群ID
     * @param hostMap         主机映射
     * @param hostnamePreview 主机名预览
     */
    public void batchSetHostnameTask(String taskId, Integer clusterId, Map<String, HostInfo> hostMap,
            List<Map<String, String>> hostnamePreview) {
        asyncCheckService.batchSetHostnameTask(taskId, clusterId, hostMap, hostnamePreview);
    }
}