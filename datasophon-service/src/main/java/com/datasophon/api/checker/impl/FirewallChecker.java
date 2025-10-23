package com.datasophon.api.checker.impl;

import com.datasophon.api.checker.CheckResult;
import com.datasophon.api.checker.EnvironmentCheckItem;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.common.vo.environment.RepairResult;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 防火墙检查器
 * 检查防火墙状态，支持自动禁用
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@Component
public class FirewallChecker implements EnvironmentCheckItem {
    
    @Value("${datasophon.checker.firewall.enabled:true}")
    private boolean enabled;
    
    @Value("${datasophon.checker.firewall.auto-disable:false}")
    private boolean autoDisable;
    
    private SshConnectionService sshService;
    
    // 需要检查的防火墙服务
    private static final List<String> FIREWALL_SERVICES = List.of("firewalld", "iptables", "ufw");
    
    /**
     * 获取SSH服务（延迟加载）
     */
    private SshConnectionService getSshService() {
        if (sshService == null) {
            sshService = SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
        }
        return sshService;
    }
    
    /**
     * 转换为插件API的HostCheckContext
     */
    private com.datasophon.plugins.api.model.HostCheckContext toPluginContext(HostCheckContext context) {
        return com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(context.getHostIp())
                .clusterId(context.getClusterId() != null ? context.getClusterId().toString() : null)
                .sshUser(context.getSshUser())
                .sshPort(context.getSshPort())
                .sshPassword(context.getSshPassword())
                .build();
    }
    
    @Override
    public String getCheckKey() {
        return "firewall";
    }
    
    @Override
    public String getDisplayName() {
        return "防火墙检查";
    }
    
    @Override
    public int getPriority() {
        return 112; // 参考配置
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public CheckResult execute(HostCheckContext context) {
        log.info("开始检查主机 {} 的防火墙状态", context.getHostIp());
        
        try {
            var pluginContext = toPluginContext(context);
            var activeFirewalls = new ArrayList<String>();
            var details = new HashMap<String, Object>();
            
            // 检查各个防火墙服务
            for (var service : FIREWALL_SERVICES) {
                var result = getSshService().executeCommand(pluginContext,
                        String.format("systemctl is-active %s 2>/dev/null || echo 'inactive'", service));
                
                if (result.isSuccess()) {
                    var status = result.output().trim();
                    details.put(service, status);
                    
                    if ("active".equals(status)) {
                        activeFirewalls.add(service);
                    }
                }
            }
            
            details.put("activeFirewalls", activeFirewalls);
            
            if (!activeFirewalls.isEmpty()) {
                var checkResult = CheckResult.failure(
                        String.format("检测到以下防火墙服务正在运行: %s", String.join(", ", activeFirewalls)),
                        "大数据集群部署建议关闭防火墙，或者配置相应的端口规则",
                        true, // 可以跳过
                        true  // 可以自动修复
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            var checkResult = CheckResult.success("防火墙检查通过：所有防火墙服务已关闭");
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (Exception e) {
            log.error("检查防火墙时发生异常: {}", e.getMessage(), e);
            return CheckResult.failure(
                    "检查防火墙时发生异常: " + e.getMessage(),
                    "请检查SSH连接和系统状态",
                    true,
                    false
            );
        }
    }
    
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        log.info("开始修复主机 {} 的防火墙配置", context.getHostIp());
        
        try {
            var pluginContext = toPluginContext(context);
            var disabledServices = new ArrayList<String>();
            var failedServices = new ArrayList<String>();
            
            // 禁用各个防火墙服务
            for (var service : FIREWALL_SERVICES) {
                // 先检查服务是否存在
                var checkResult = getSshService().executeCommand(pluginContext,
                        String.format("systemctl list-unit-files | grep -q %s && echo 'exists' || echo 'not-exists'", service));
                
                if (checkResult.isSuccess() && checkResult.output().trim().equals("exists")) {
                    // 服务存在，尝试停止并禁用
                    var stopResult = getSshService().executeCommand(pluginContext,
                            String.format("sudo systemctl stop %s && sudo systemctl disable %s", service, service));
                    
                    if (stopResult.isSuccess()) {
                        disabledServices.add(service);
                        log.info("成功禁用防火墙服务: {}", service);
                    } else {
                        failedServices.add(service);
                        log.warn("禁用防火墙服务失败: {}, 错误: {}", service, stopResult.error());
                    }
                }
            }
            
            if (failedServices.isEmpty()) {
                return RepairResult.builder()
                        .success(true)
                        .message(String.format("成功禁用防火墙服务: %s", String.join(", ", disabledServices)))
                        .details(String.format("已禁用 %d 个防火墙服务", disabledServices.size()))
                        .build();
            } else {
                return RepairResult.builder()
                        .success(false)
                        .message(String.format("部分防火墙服务禁用失败: %s", String.join(", ", failedServices)))
                        .details(String.format("成功: %s, 失败: %s", 
                                String.join(", ", disabledServices),
                                String.join(", ", failedServices)))
                        .build();
            }
            
        } catch (Exception e) {
            log.error("修复防火墙配置时发生异常: {}", e.getMessage(), e);
            return RepairResult.builder()
                    .success(false)
                    .message("修复失败: " + e.getMessage())
                    .build();
        }
    }
}

