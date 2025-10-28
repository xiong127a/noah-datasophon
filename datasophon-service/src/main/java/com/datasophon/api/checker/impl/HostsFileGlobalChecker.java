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
                log.info("主机 {} 的管理段内容长度: {}, 前100字符: {}", 
                        entry.getKey(), 
                        managedContent.length(), 
                        managedContent.length() > 100 ? managedContent.substring(0, 100) : managedContent);
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
            log.info("开始检查缺失主机: 集群IP列表={}, 集群主机名列表={}", allClusterIps, allClusterHostnames);
            for (Map.Entry<String, String> entry : managedSections.entrySet()) {
                Set<String> missingInThis = findMissingHosts(
                        entry.getValue(), 
                        allClusterIps, 
                        allClusterHostnames
                );
                log.info("主机 {} 缺失的条目: {}", entry.getKey(), missingInThis);
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
                .sshPort(getSshPort(connectionParams))
                .build();
        
        var result = getSshService().executeCommand(context, "cat /etc/hosts", 10);
        
        if (result.isSuccess()) {
            return result.output();
        } else {
            throw new RuntimeException("无法读取hosts文件: " + result.error());
        }
    }
    
    /**
     * 安全地获取SSH端口（处理String/Integer类型）
     */
    private Integer getSshPort(Map<String, Object> connectionParams) {
        Object port = connectionParams.get("sshPort");
        if (port instanceof Integer) {
            return (Integer) port;
        } else if (port instanceof String) {
            try {
                return Integer.parseInt((String) port);
            } catch (NumberFormatException e) {
                log.warn("无效的SSH端口格式: {}, 使用默认端口22", port);
                return 22;
            }
        }
        return 22; // 默认值
    }
    
    /**
     * 提取DataSophon管理的hosts段
     */
    private String extractManagedSection(String hostsContent, CheckerProperties.HostsFileConfig config) {
        String startMarker = config.getManagedMarkerStart();
        String endMarker = config.getManagedMarkerEnd();
        
        log.info("提取管理段: 开始标记=[{}], 结束标记=[{}]", startMarker, endMarker);
        log.info("hosts内容总长度: {} 字符，总行数: {} 行", 
                hostsContent.length(), 
                hostsContent.split("\\r?\\n").length);
        
        // 打印完整hosts内容（使用DEBUG级别，避免刷屏）
        log.debug("完整hosts内容:\n{}", hostsContent);
        
        int startIndex = hostsContent.indexOf(startMarker);
        int endIndex = hostsContent.indexOf(endMarker);
        
        log.info("标记查找结果: startIndex={}, endIndex={}", startIndex, endIndex);
        
        if (startIndex == -1) {
            log.error("❌ 未找到开始标记 [{}] in hosts文件", startMarker);
            // 打印开头和结尾，帮助定位问题
            String preview = hostsContent.length() > 500 ? 
                    hostsContent.substring(0, 250) + "\n...\n" + hostsContent.substring(hostsContent.length() - 250) :
                    hostsContent;
            log.error("hosts文件预览（头尾各250字符）:\n{}", preview);
        }
        
        if (endIndex == -1) {
            log.error("❌ 未找到结束标记 [{}] in hosts文件", endMarker);
        }
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            String managedSection = hostsContent.substring(startIndex, endIndex + endMarker.length());
            log.info("✅ 成功提取管理段，长度: {} 字符，行数: {} 行", 
                    managedSection.length(), 
                    managedSection.split("\\r?\\n").length);
            log.debug("管理段完整内容:\n{}", managedSection);
            return managedSection;
        }
        
        log.warn("❌ 未找到管理段标记或标记位置错误");
        return ""; // 没有找到管理段
    }
    
    /**
     * 查找管理段中缺失的集群主机
     */
    private Set<String> findMissingHosts(String managedSection, Set<String> clusterIps, Set<String> clusterHostnames) {
        Set<String> missing = new HashSet<>();
        
        log.debug("检查管理段内容: 长度={}, 内容={}", managedSection.length(), managedSection);
        
        // 解析hosts文件内容，提取所有IP地址（第一列）
        Set<String> ipsInHostsFile = parseHostsFileIps(managedSection);
        log.info("从管理段解析出的IP列表: {}", ipsInHostsFile);
        
        // 检查每个集群IP是否在hosts文件中
        for (String ip : clusterIps) {
            if (!ipsInHostsFile.contains(ip)) {
                missing.add(ip);
                log.warn("管理段中缺失IP: {}", ip);
            }
        }
        
        return missing;
    }
    
    /**
     * 解析hosts文件内容，提取所有IP地址（每行的第一列）
     * @param hostsContent hosts文件内容
     * @return IP地址集合
     */
    private Set<String> parseHostsFileIps(String hostsContent) {
        Set<String> ips = new HashSet<>();
        
        if (hostsContent == null || hostsContent.trim().isEmpty()) {
            log.warn("hosts内容为空，无法解析");
            return ips;
        }
        
        log.info("开始解析hosts内容，长度={}, 内容:\n{}", hostsContent.length(), hostsContent);
        
        // 按行分割
        String[] lines = hostsContent.split("\\r?\\n");
        log.info("分割后的行数: {}", lines.length);
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // 去除首尾空白
            line = line.trim();
            
            log.debug("第{}行原始内容: [{}]", i + 1, line);
            
            // 跳过空行和注释行
            if (line.isEmpty()) {
                log.debug("第{}行: 空行，跳过", i + 1);
                continue;
            }
            if (line.startsWith("#")) {
                log.debug("第{}行: 注释行，跳过", i + 1);
                continue;
            }
            
            // 按空白符分割（空格或制表符）
            String[] parts = line.split("\\s+");
            log.debug("第{}行分割结果: 列数={}, 内容={}", i + 1, parts.length, String.join(" | ", parts));
            
            if (parts.length >= 2) {
                // 第一列是IP地址
                String ip = parts[0];
                
                // 简单验证IP格式（支持IPv4和IPv6）
                boolean valid = isValidIp(ip);
                log.debug("第{}行IP: [{}], 有效={}", i + 1, ip, valid);
                
                if (valid) {
                    ips.add(ip);
                    log.info("✅ 成功解析IP: {}", ip);
                } else {
                    log.warn("❌ IP格式无效: {}", ip);
                }
            } else {
                log.warn("第{}行: 列数不足({}<2)，跳过", i + 1, parts.length);
            }
        }
        
        return ips;
    }
    
    /**
     * 简单验证IP地址格式
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // IPv4格式：xxx.xxx.xxx.xxx
        if (ip.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")) {
            return true;
        }
        
        // IPv6格式（简化判断）
        if (ip.contains(":")) {
            return true;
        }
        
        return false;
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

