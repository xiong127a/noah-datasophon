package com.datasophon.api.service.impl;

import com.datasophon.api.checker.CheckStateManager;
import com.datasophon.api.config.CheckerProperties;
import com.datasophon.api.service.HostManagementService;
import com.datasophon.common.dto.host.BatchHostnameChangeRequest;
import com.datasophon.common.dto.host.HostsSyncRequest;
import com.datasophon.common.vo.environment.EnvironmentCheckStatusVO;
import com.datasophon.common.vo.host.HostOperationProgressVO;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 主机管理服务实现
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Slf4j
@Service
public class HostManagementServiceImpl implements HostManagementService {
    
    @Autowired
    private CheckerProperties checkerProperties;
    
    @Autowired
    private CheckStateManager checkStateManager;
    
    private SshConnectionService sshService;
    
    // 存储SSE连接：key为taskId
    private static final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    
    /**
     * 获取SSH服务（延迟加载）
     */
    private SshConnectionService getSshService() {
        if (sshService == null) {
            sshService = SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
        }
        return sshService;
    }
    
    @Override
    public Map<String, Object> getHostnameConfig(Long clusterId) {
        log.info("获取主机名配置: clusterId={}", clusterId);
        
        var config = checkerProperties.getHostname();
        
        Map<String, Object> result = new HashMap<>();
        result.put("recommendedPrefixes", config.getRecommendedPrefixes());
        result.put("defaultPrefix", config.getDefaultPrefix());
        result.put("defaultFormatIndex", config.getDefaultFormatIndex());
        
        // 转换后缀格式
        List<Map<String, String>> formats = config.getSuffixFormats().stream()
                .map(format -> {
                    Map<String, String> formatMap = new HashMap<>();
                    formatMap.put("name", format.getName());
                    formatMap.put("pattern", format.getPattern());
                    formatMap.put("example", format.getExample());
                    return formatMap;
                })
                .collect(Collectors.toList());
        
        result.put("suffixFormats", formats);
        
        return result;
    }
    
    @Override
    public Map<String, String> previewHostnameChanges(BatchHostnameChangeRequest request) {
        log.info("预览主机名变更: prefix={}, formatIndex={}, startIndex={}, hosts={}", 
                request.getPrefix(), request.getSuffixFormatIndex(), 
                request.getStartIndex(), request.getHostIps().size());
        
        Map<String, String> preview = new LinkedHashMap<>();
        
        var suffixFormats = checkerProperties.getHostname().getSuffixFormats();
        int formatIndex = request.getSuffixFormatIndex();
        
        if (formatIndex < 0 || formatIndex >= suffixFormats.size()) {
            log.warn("无效的格式索引: {}, 使用默认索引", formatIndex);
            formatIndex = checkerProperties.getHostname().getDefaultFormatIndex();
        }
        
        var format = suffixFormats.get(formatIndex);
        String pattern = format.getPattern();
        
        int currentIndex = request.getStartIndex();
        for (String hostIp : request.getHostIps()) {
            String newHostname = generateHostname(request.getPrefix(), pattern, currentIndex);
            preview.put(hostIp, newHostname);
            currentIndex++;
        }
        
        return preview;
    }
    
    @Override
    public String batchChangeHostnames(BatchHostnameChangeRequest request) {
        String taskId = UUID.randomUUID().toString();
        log.info("开始批量修改主机名: taskId={}, clusterId={}, hosts={}", 
                taskId, request.getClusterId(), request.getHostIps().size());
        
        // 异步执行修改操作
        CompletableFuture.runAsync(() -> executeBatchHostnameChange(taskId, request));
        
        return taskId;
    }
    
    @Override
    public String syncHostsFile(HostsSyncRequest request) {
        String taskId = UUID.randomUUID().toString();
        log.info("开始同步hosts文件: taskId={}, clusterId={}, hosts={}", 
                taskId, request.getClusterId(), request.getHostIps().size());
        
        // 异步执行同步操作
        CompletableFuture.runAsync(() -> executeHostsFileSync(taskId, request));
        
        return taskId;
    }
    
    /**
     * 执行批量主机名修改
     */
    private void executeBatchHostnameChange(String taskId, BatchHostnameChangeRequest request) {
        log.info("执行批量主机名修改任务: taskId={}", taskId);
        
        try {
            // 生成主机名映射
            Map<String, String> hostnameMap = previewHostnameChanges(request);
            
            // 逐个修改主机名
            for (Map.Entry<String, String> entry : hostnameMap.entrySet()) {
                String hostIp = entry.getKey();
                String newHostname = entry.getValue();
                
                // 发送"正在处理"状态
                pushProgress(taskId, HostOperationProgressVO.builder()
                        .hostIp(hostIp)
                        .status("processing")
                        .message("正在修改主机名...")
                        .newHostname(newHostname)
                        .timestamp(System.currentTimeMillis())
                        .build());
                
                try {
                    // 执行主机名修改
                    changeHostname(hostIp, newHostname, request.getConnectionParams());
                    
                    // 发送"成功"状态
                    pushProgress(taskId, HostOperationProgressVO.builder()
                            .hostIp(hostIp)
                            .status("success")
                            .message("主机名修改成功")
                            .newHostname(newHostname)
                            .timestamp(System.currentTimeMillis())
                            .build());
                    
                } catch (Exception e) {
                    log.error("修改主机名失败: hostIp={}, error={}", hostIp, e.getMessage(), e);
                    
                    // 发送"失败"状态
                    pushProgress(taskId, HostOperationProgressVO.builder()
                            .hostIp(hostIp)
                            .status("failed")
                            .message("主机名修改失败")
                            .error(e.getMessage())
                            .timestamp(System.currentTimeMillis())
                            .build());
                }
            }
            
            // 发送完成事件
            pushCompletion(taskId, true, "批量修改主机名完成");
            
        } catch (Exception e) {
            log.error("批量修改主机名任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
            pushCompletion(taskId, false, "任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行hosts文件同步
     */
    private void executeHostsFileSync(String taskId, HostsSyncRequest request) {
        log.info("执行hosts文件同步任务: taskId={}", taskId);
        
        try {
            // 构建统一的hosts文件内容
            String hostsContent = buildHostsFileContent(request);
            
            // 逐个同步到主机
            for (String hostIp : request.getHostIps()) {
                // 发送"正在处理"状态
                pushProgress(taskId, HostOperationProgressVO.builder()
                        .hostIp(hostIp)
                        .status("processing")
                        .message("正在同步hosts文件...")
                        .timestamp(System.currentTimeMillis())
                        .build());
                
                try {
                    // 执行hosts文件同步
                    syncHostsToHost(hostIp, hostsContent, request.getConnectionParams());
                    
                    // 发送"成功"状态
                    pushProgress(taskId, HostOperationProgressVO.builder()
                            .hostIp(hostIp)
                            .status("success")
                            .message("Hosts文件同步成功")
                            .timestamp(System.currentTimeMillis())
                            .build());
                    
                } catch (Exception e) {
                    log.error("同步hosts文件失败: hostIp={}, error={}", hostIp, e.getMessage(), e);
                    
                    // 发送"失败"状态
                    pushProgress(taskId, HostOperationProgressVO.builder()
                            .hostIp(hostIp)
                            .status("failed")
                            .message("Hosts文件同步失败")
                            .error(e.getMessage())
                            .timestamp(System.currentTimeMillis())
                            .build());
                }
            }
            
            // 发送完成事件
            pushCompletion(taskId, true, "Hosts文件同步完成");
            
        } catch (Exception e) {
            log.error("Hosts文件同步任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
            pushCompletion(taskId, false, "任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 修改单台主机的主机名
     */
    private void changeHostname(String hostIp, String newHostname, Map<String, Object> connectionParams) {
        var context = com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(hostIp)
                .sshUser((String) connectionParams.get("sshUser"))
                .sshPassword((String) connectionParams.get("sshPassword"))
                .sshPort((Integer) connectionParams.getOrDefault("sshPort", 22))
                .build();
        
        // 修改主机名的命令
        String command = String.format(
            "sudo hostnamectl set-hostname %s && " +
            "sudo sed -i 's/^127.0.1.1.*/127.0.1.1\\t%s/' /etc/hosts",
            newHostname, newHostname
        );
        
        var result = getSshService().executeCommand(context, command, 30);
        
        if (result.getExitCode() != 0) {
            throw new RuntimeException("修改主机名失败: " + result.getStderr());
        }
    }
    
    /**
     * 构建hosts文件内容
     */
    private String buildHostsFileContent(HostsSyncRequest request) {
        var config = checkerProperties.getHostsFile();
        
        StringBuilder content = new StringBuilder();
        content.append(config.getManagedMarkerStart()).append("\n");
        
        // 从环境检查状态中获取主机名
        List<EnvironmentCheckStatusVO> checkStatuses = checkStateManager.getCheckStatusSnapshot(request.getClusterId());
        Map<String, String> hostnameMap = new HashMap<>();
        
        for (EnvironmentCheckStatusVO status : checkStatuses) {
            if (status.getHostname() != null && !status.getHostname().isEmpty()) {
                hostnameMap.put(status.getHostIp(), status.getHostname());
            }
        }
        
        // 如果从检查状态中没有获取到主机名，尝试通过SSH获取
        for (String hostIp : request.getHostIps()) {
            String hostname = hostnameMap.get(hostIp);
            
            if (hostname == null || hostname.isEmpty()) {
                // 尝试通过SSH获取主机名
                try {
                    var context = com.datasophon.plugins.api.model.HostCheckContext.builder()
                            .hostIp(hostIp)
                            .sshUser((String) request.getConnectionParams().get("sshUser"))
                            .sshPassword((String) request.getConnectionParams().get("sshPassword"))
                            .sshPort((Integer) request.getConnectionParams().getOrDefault("sshPort", 22))
                            .build();
                    
                    var result = getSshService().executeCommand(context, "hostname", 10);
                    if (result.getExitCode() == 0 && result.getStdout() != null) {
                        hostname = result.getStdout().trim();
                        if (!hostname.isEmpty()) {
                            hostnameMap.put(hostIp, hostname);
                        }
                    }
                } catch (Exception e) {
                    log.warn("无法获取主机名: hostIp={}, error={}", hostIp, e.getMessage());
                }
            }
            
            // 如果仍然没有主机名，使用IP作为主机名
            if (hostname == null || hostname.isEmpty()) {
                hostname = hostIp.replace(".", "-");
                log.warn("使用IP作为主机名: hostIp={}, hostname={}", hostIp, hostname);
            }
            
            content.append(hostIp).append("\t").append(hostname).append("\n");
        }
        
        content.append(config.getManagedMarkerEnd()).append("\n");
        
        log.info("构建hosts文件内容完成，包含{}个主机条目", hostnameMap.size());
        return content.toString();
    }
    
    /**
     * 同步hosts文件到单台主机
     */
    private void syncHostsToHost(String hostIp, String hostsContent, Map<String, Object> connectionParams) {
        var context = com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(hostIp)
                .sshUser((String) connectionParams.get("sshUser"))
                .sshPassword((String) connectionParams.get("sshPassword"))
                .sshPort((Integer) connectionParams.getOrDefault("sshPort", 22))
                .build();
        
        var config = checkerProperties.getHostsFile();
        
        // 智能合并hosts文件的脚本
        String script = String.format(
            "#!/bin/bash\n" +
            "HOSTS_FILE='/etc/hosts'\n" +
            "BACKUP_FILE='%s%s'\n" +
            "START_MARKER='%s'\n" +
            "END_MARKER='%s'\n" +
            "NEW_CONTENT='%s'\n" +
            "\n" +
            "# 备份原文件\n" +
            "sudo cp $HOSTS_FILE $BACKUP_FILE\n" +
            "\n" +
            "# 检查是否存在管理段标记\n" +
            "if grep -q \"$START_MARKER\" $HOSTS_FILE && grep -q \"$END_MARKER\" $HOSTS_FILE; then\n" +
            "  # 存在标记，替换管理段\n" +
            "  sudo sed -i \"/$START_MARKER/,/$END_MARKER/c\\\\$NEW_CONTENT\" $HOSTS_FILE\n" +
            "else\n" +
            "  # 不存在标记，追加到文件末尾\n" +
            "  echo \"$NEW_CONTENT\" | sudo tee -a $HOSTS_FILE > /dev/null\n" +
            "fi\n" +
            "echo 'Hosts file synced successfully'",
            config.getBackupBeforeModify() ? "/etc/hosts" : "/tmp/hosts",
            config.getBackupSuffix(),
            config.getManagedMarkerStart(),
            config.getManagedMarkerEnd(),
            hostsContent.replace("\n", "\\n").replace("'", "\\'")
        );
        
        var result = getSshService().executeCommand(context, script, 30);
        
        if (result.getExitCode() != 0) {
            throw new RuntimeException("同步hosts文件失败: " + result.getStderr());
        }
    }
    
    /**
     * 生成主机名
     */
    private String generateHostname(String prefix, String pattern, int index) {
        // 替换pattern中的{prefix}和{index}
        String hostname = pattern.replace("{prefix}", prefix);
        
        // 处理格式化的index（如{index:02d}）
        if (hostname.contains("{index:")) {
            int start = hostname.indexOf("{index:");
            int end = hostname.indexOf("}", start);
            String formatSpec = hostname.substring(start + 7, end);
            
            // 解析格式（如02d -> 2位补零）
            String formatted = String.format("%" + formatSpec, index);
            hostname = hostname.substring(0, start) + formatted + hostname.substring(end + 1);
        } else {
            hostname = hostname.replace("{index}", String.valueOf(index));
        }
        
        return hostname;
    }
    
    /**
     * 推送进度更新
     */
    private void pushProgress(String taskId, HostOperationProgressVO progress) {
        SseEmitter emitter = sseEmitters.get(taskId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(progress));
            } catch (IOException e) {
                log.error("推送进度失败: taskId={}", taskId, e);
                sseEmitters.remove(taskId);
            }
        }
    }
    
    /**
     * 推送完成事件
     */
    private void pushCompletion(String taskId, boolean success, String message) {
        SseEmitter emitter = sseEmitters.get(taskId);
        if (emitter != null) {
            try {
                Map<String, Object> completionData = new HashMap<>();
                completionData.put("success", success);
                completionData.put("message", message);
                
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(completionData));
                
                emitter.complete();
            } catch (IOException e) {
                log.error("推送完成事件失败: taskId={}", taskId, e);
            } finally {
                sseEmitters.remove(taskId);
            }
        }
    }
    
    /**
     * 注册SSE连接
     */
    public static void registerSseEmitter(String taskId, SseEmitter emitter) {
        sseEmitters.put(taskId, emitter);
    }
    
    /**
     * 取消注册SSE连接
     */
    public static void unregisterSseEmitter(String taskId) {
        sseEmitters.remove(taskId);
    }
}

