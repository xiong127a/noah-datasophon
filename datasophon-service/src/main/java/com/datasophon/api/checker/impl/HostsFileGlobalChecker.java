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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Hosts文件全局检查器
 * 检查集群中所有主机的hosts文件是否一致且包含所有集群主机
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Slf4j
@Component
public class HostsFileGlobalChecker implements GlobalCheckItem {
    
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
        return "hosts-file";
    }
    
    @Override
    public String getDisplayName() {
        return "Hosts文件一致性检查";
    }
    
    @Override
    public int getPriority() {
        return checkerProperties.getHostsFile().getPriority();
    }
    
    @Override
    public boolean isEnabled() {
        return checkerProperties.getHostsFile().isEnabled();
    }
    
    @Override
    public GlobalCheckResult execute(List<HostInfo> hosts, Long clusterId, Map<String, Object> connectionParams) {
        log.info("开始Hosts文件一致性检查: 集群={}, 主机数={}", clusterId, hosts.size());
        
        var config = checkerProperties.getHostsFile();
        
        try {
            // 步骤1：从所有主机读取hosts文件内容
            Map<String, String> hostsFileContents = new HashMap<>();
            List<String> failedHosts = new ArrayList<>();
            
            for (HostInfo host : hosts) {
                try {
                    String content = getHostsFileContent(host, connectionParams);
                    hostsFileContents.put(host.getIp(), content);
                } catch (Exception e) {
                    log.warn("无法读取hosts文件: ip={}, error={}", host.getIp(), e.getMessage());
                    failedHosts.add(host.getIp());
                }
            }
            
            if (hostsFileContents.isEmpty()) {
                log.error("无法读取任何主机的hosts文件: 集群={}", clusterId);
                return buildErrorResult("无法读取任何主机的hosts文件", 
                        "请检查SSH连接和文件权限");
            }
            
            // 步骤2：提取DataSophon管理的hosts段
            Map<String, String> managedSections = new HashMap<>();
            for (Map.Entry<String, String> entry : hostsFileContents.entrySet()) {
                String managedContent = extractManagedSection(entry.getValue(), config);
                managedSections.put(entry.getKey(), managedContent);
            }
            
            // 步骤3：检查各主机的管理段是否一致
            Set<String> uniqueSections = new HashSet<>(managedSections.values());
            
            // 步骤4：检查是否包含所有集群主机
            Set<String> allClusterIps = hosts.stream()
                    .map(HostInfo::getIp)
                    .collect(Collectors.toSet());
            
            Set<String> allClusterHostnames = hosts.stream()
                    .map(HostInfo::getHostname)
                    .filter(h -> h != null && !h.trim().isEmpty())
                    .collect(Collectors.toSet());
            
            Map<String, Set<String>> missingHosts = new HashMap<>();
            for (Map.Entry<String, String> entry : managedSections.entrySet()) {
                Set<String> missingInThis = findMissingHosts(
                        entry.getValue(), 
                        allClusterIps, 
                        allClusterHostnames
                );
                if (!missingInThis.isEmpty()) {
                    missingHosts.put(entry.getKey(), missingInThis);
                }
            }
            
            // 步骤5：构建检查结果
            Map<String, Object> details = new HashMap<>();
            details.put("totalHosts", hosts.size());
            details.put("checkedHosts", hostsFileContents.size());
            details.put("failedHosts", failedHosts);
            details.put("uniqueSections", uniqueSections.size());
            details.put("missingHosts", missingHosts);
            
            // 判断检查状态
            if (!failedHosts.isEmpty()) {
                log.warn("部分主机hosts文件读取失败: 集群={}, 数量={}", clusterId, failedHosts.size());
                
                String message = String.format("有 %d 台主机无法读取hosts文件", failedHosts.size());
                String recommendation = "请检查这些主机的SSH连接和文件权限";
                
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
            
            if (uniqueSections.size() > 1) {
                log.warn("Hosts文件不一致: 集群={}, 不同版本数={}", clusterId, uniqueSections.size());
                
                String message = String.format("Hosts文件不一致（发现 %d 个不同版本）", uniqueSections.size());
                String recommendation = "建议使用\"同步hosts文件\"功能统一所有主机的hosts文件";
                
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
            
            if (!missingHosts.isEmpty()) {
                log.warn("部分主机hosts文件不完整: 集群={}, 数量={}", clusterId, missingHosts.size());
                
                String message = String.format("有 %d 台主机的hosts文件缺少集群主机条目", missingHosts.size());
                String recommendation = "建议使用\"同步hosts文件\"功能添加缺失的主机条目";
                
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
            
            log.info("Hosts文件一致性检查通过: 集群={}", clusterId);
            
            String message = String.format("所有主机的hosts文件一致且完整（共 %d 台主机）", hosts.size());
            
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
            log.error("Hosts文件一致性检查失败: 集群={}, error={}", clusterId, e.getMessage(), e);
            return buildErrorResult("检查失败: " + e.getMessage(), "请检查集群连接状态");
        }
    }
    
    /**
     * 获取主机的hosts文件内容
     */
    private String getHostsFileContent(HostInfo host, Map<String, Object> connectionParams) {
        var context = com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(host.getIp())
                .sshUser((String) connectionParams.get("sshUser"))
                .sshPassword((String) connectionParams.get("sshPassword"))
                .sshPort((Integer) connectionParams.getOrDefault("sshPort", 22))
                .build();
        
        var result = getSshService().executeCommand(context, "cat /etc/hosts", 10);
        
        if (result.isSuccess()) {
            return result.output();
        } else {
            throw new RuntimeException("无法读取hosts文件: " + result.error());
        }
    }
    
    /**
     * 提取DataSophon管理的hosts段
     */
    private String extractManagedSection(String hostsContent, CheckerProperties.HostsFileConfig config) {
        String startMarker = config.getManagedMarkerStart();
        String endMarker = config.getManagedMarkerEnd();
        
        int startIndex = hostsContent.indexOf(startMarker);
        int endIndex = hostsContent.indexOf(endMarker);
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return hostsContent.substring(startIndex, endIndex + endMarker.length());
        }
        
        return ""; // 没有找到管理段
    }
    
    /**
     * 查找管理段中缺失的集群主机
     */
    private Set<String> findMissingHosts(String managedSection, Set<String> clusterIps, Set<String> clusterHostnames) {
        Set<String> missing = new HashSet<>();
        
        // 检查每个IP是否在管理段中
        for (String ip : clusterIps) {
            if (!managedSection.contains(ip)) {
                missing.add(ip);
            }
        }
        
        return missing;
    }
    
    /**
     * 构建错误结果
     */
    private GlobalCheckResult buildErrorResult(String message, String recommendation) {
        return GlobalCheckResult.builder()
                .checkKey(getCheckKey())
                .displayName(getDisplayName())
                .status(CheckItemStatus.FAILED)
                .message(message)
                .recommendation(recommendation)
                .details(Map.of())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}

