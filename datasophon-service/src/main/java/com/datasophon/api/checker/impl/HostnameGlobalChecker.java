package com.datasophon.api.checker.impl;

import com.datasophon.api.checker.GlobalCheckItem;
import com.datasophon.api.config.CheckerProperties;
import com.datasophon.common.enums.CheckItemStatus;
import com.datasophon.common.vo.environment.GlobalCheckResult;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 主机名全局检查器
 * 检查集群中所有主机的主机名是否唯一
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Slf4j
@Component
public class HostnameGlobalChecker implements GlobalCheckItem {
    
    @Autowired
    private CheckerProperties checkerProperties;
    
    private SshConnectionService sshService;
    
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
    public String getCheckKey() {
        return "hostname";
    }
    
    @Override
    public String getDisplayName() {
        return "主机名唯一性检查";
    }
    
    @Override
    public int getPriority() {
        return checkerProperties.getHostname().getPriority();
    }
    
    @Override
    public boolean isEnabled() {
        return checkerProperties.getHostname().isEnabled();
    }
    
    @Override
    public GlobalCheckResult execute(List<HostInfo> hosts, Long clusterId, Map<String, Object> connectionParams) {
        log.info("开始主机名唯一性检查: 集群={}, 主机数={}", clusterId, hosts.size());
        
        try {
            // 步骤1：收集所有主机名（如果hosts中没有，通过SSH获取）
            Map<String, List<String>> hostnameToIps = new HashMap<>();
            List<String> emptyHostnames = new ArrayList<>();
            
            for (HostInfo host : hosts) {
                String hostname = host.getHostname();
                
                // 如果主机名为空或未知，尝试通过SSH获取
                if (hostname == null || hostname.trim().isEmpty() || 
                    "unknown".equalsIgnoreCase(hostname) || "localhost".equalsIgnoreCase(hostname)) {
                    
                    try {
                        hostname = getHostnameViaSsh(host, connectionParams);
                        host.setHostname(hostname); // 更新主机名
                    } catch (Exception e) {
                        log.warn("无法获取主机名: ip={}, error={}", host.getIp(), e.getMessage());
                        emptyHostnames.add(host.getIp());
                        continue;
                    }
                }
                
                // 收集主机名到IP的映射
                hostnameToIps.computeIfAbsent(hostname, k -> new ArrayList<>()).add(host.getIp());
            }
            
            // 步骤2：检查是否有重复的主机名
            List<Map<String, Object>> conflicts = hostnameToIps.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1)
                    .map(entry -> {
                        Map<String, Object> conflict = new HashMap<>();
                        conflict.put("hostname", entry.getKey());
                        conflict.put("ips", entry.getValue());
                        conflict.put("count", entry.getValue().size());
                        return conflict;
                    })
                    .collect(Collectors.toList());
            
            // 步骤3：构建检查结果
            Map<String, Object> details = new HashMap<>();
            details.put("totalHosts", hosts.size());
            details.put("uniqueHostnames", hostnameToIps.size());
            details.put("conflicts", conflicts);
            details.put("emptyHostnames", emptyHostnames);
            
            if (!conflicts.isEmpty()) {
                log.warn("发现主机名冲突: 集群={}, 冲突数={}", clusterId, conflicts.size());
                
                String message = String.format("发现 %d 个主机名冲突", conflicts.size());
                String recommendation = "建议使用\"批量修改主机名\"功能统一设置主机名";
                
                return GlobalCheckResult.builder()
                        .checkKey(getCheckKey())
                        .displayName(getDisplayName())
                        .status(CheckItemStatus.FAILED)
                        .message(message)
                        .recommendation(recommendation)
                        .details(details)
                        .timestamp(System.currentTimeMillis())
                        .build();
            }
            
            if (!emptyHostnames.isEmpty()) {
                log.warn("部分主机无法获取主机名: 集群={}, 数量={}", clusterId, emptyHostnames.size());
                
                String message = String.format("所有主机名唯一，但有 %d 台主机无法获取主机名", emptyHostnames.size());
                String recommendation = "请检查这些主机的SSH连接是否正常";
                
                return GlobalCheckResult.builder()
                        .checkKey(getCheckKey())
                        .displayName(getDisplayName())
                        .status(CheckItemStatus.WARNING)
                        .message(message)
                        .recommendation(recommendation)
                        .details(details)
                        .timestamp(System.currentTimeMillis())
                        .build();
            }
            
            log.info("主机名唯一性检查通过: 集群={}", clusterId);
            
            String message = String.format("所有主机名唯一（共 %d 台主机）", hosts.size());
            
            return GlobalCheckResult.builder()
                    .checkKey(getCheckKey())
                    .displayName(getDisplayName())
                    .status(CheckItemStatus.SUCCESS)
                    .message(message)
                    .recommendation(null)
                    .details(details)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
        } catch (Exception e) {
            log.error("主机名唯一性检查失败: 集群={}, error={}", clusterId, e.getMessage(), e);
            
            return GlobalCheckResult.builder()
                    .checkKey(getCheckKey())
                    .displayName(getDisplayName())
                    .status(CheckItemStatus.FAILED)
                    .message("检查失败: " + e.getMessage())
                    .recommendation("请检查集群连接状态")
                    .details(Map.of("error", e.getMessage()))
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }
    
    /**
     * 通过SSH获取主机名
     */
    private String getHostnameViaSsh(HostInfo host, Map<String, Object> connectionParams) {
        var context = com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(host.getIp())
                .sshUser((String) connectionParams.get("sshUser"))
                .sshPassword((String) connectionParams.get("sshPassword"))
                .sshPort((Integer) connectionParams.getOrDefault("sshPort", 22))
                .build();
        
        var result = getSshService().executeCommand(context, "hostname", 10);
        
        if (result.getExitCode() == 0) {
            return result.getStdout().trim();
        } else {
            throw new RuntimeException("无法获取主机名: " + result.getStderr());
        }
    }
}

