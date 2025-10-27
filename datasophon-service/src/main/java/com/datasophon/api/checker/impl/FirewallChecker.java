package com.datasophon.api.checker.impl;

import com.datasophon.api.checker.CheckResult;
import com.datasophon.api.checker.EnvironmentCheckItem;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.common.vo.environment.RepairResult;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
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
                .clusterId(context.getClusterId() != null ? context.getClusterId() : null)
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
        
        // 清理旧日志
        checkLogWriter.clearLogs(context.getClusterId(), context.getHostIp(), getCheckKey());
        
        // 记录检查开始
        checkLogWriter.logCheckStart(context.getClusterId(), context.getHostIp(), 
                getCheckKey(), "开始检查防火墙状态");
        
        try {
            var pluginContext = toPluginContext(context);
            var activeFirewalls = new ArrayList<String>();
            var details = new HashMap<String, Object>();
            
            // 检查各个防火墙服务
            for (var service : FIREWALL_SERVICES) {
                var command = String.format("systemctl is-active %s 2>/dev/null || echo 'inactive'", service);
                checkLogWriter.logCheckCommand(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), command);
                
                var result = getSshService().executeCommand(pluginContext, command);
                
                if (result.isSuccess()) {
                    var status = result.output().trim();
                    details.put(service, status);
                    checkLogWriter.logCheckOutput(context.getClusterId(), context.getHostIp(),
                            getCheckKey(), String.format("%s: %s", service, status));
                    
                    if ("active".equals(status)) {
                        activeFirewalls.add(service);
                    }
                }
            }
            
            details.put("activeFirewalls", activeFirewalls);
            
            // 记录检查结果详情
            checkLogWriter.logCheckInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), String.format("防火墙检查完成: 运行中的服务=%s", activeFirewalls), details);
            
            if (!activeFirewalls.isEmpty()) {
                String failMsg = String.format("检测到以下防火墙服务正在运行: %s", String.join(", ", activeFirewalls));
                details.put("recommendation", "大数据集群部署建议关闭防火墙，或者配置相应的端口规则");
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), failMsg, details);
                
                var checkResult = CheckResult.failure(
                        failMsg,
                        "大数据集群部署建议关闭防火墙，或者配置相应的端口规则",
                        true, // 可以跳过
                        true  // 可以自动修复
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            String successMsg = "防火墙检查通过：所有防火墙服务已关闭";
            checkLogWriter.logCheckSuccess(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), successMsg, details);
            
            var checkResult = CheckResult.success(successMsg);
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (Exception e) {
            log.error("检查防火墙时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", e.getMessage());
            checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "检查防火墙时发生异常", errorDetails);
            return CheckResult.failure(
                    "检查防火墙时发生异常: " + e.getMessage(),
                    "请检查SSH连接和系统状态",
                    true,
                    false
            );
        }
    }
    
    /**
     * 修复防火墙配置
     * 
     * 注意：此方法仅执行修复操作（停止并禁用防火墙服务），不包含验证逻辑。
     * 验证由框架在修复成功后自动调用 execute() 方法完成。
     * 
     * @param context 主机检查上下文
     * @param params 修复参数（当前未使用）
     * @return RepairResult 修复结果（仅表示禁用防火墙是否成功）
     */
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        log.info("开始修复主机 {} 的防火墙配置", context.getHostIp());
        
        // 记录修复开始
        checkLogWriter.logRepairStart(context.getClusterId(), context.getHostIp(),
                getCheckKey(), "开始修复防火墙配置");
        
        try {
            var pluginContext = toPluginContext(context);
            var disabledServices = new ArrayList<String>();
            var failedServices = new ArrayList<String>();
            
            // 禁用各个防火墙服务
            for (var service : FIREWALL_SERVICES) {
                // 先检查服务是否存在
                var checkCommand = String.format("systemctl list-unit-files | grep -q %s && echo 'exists' || echo 'not-exists'", service);
                checkLogWriter.logRepairCommand(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), checkCommand);
                
                var checkResult = getSshService().executeCommand(pluginContext, checkCommand);
                checkLogWriter.logRepairOutput(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), String.format("%s exists check: %s", service, checkResult.output()));
                
                if (checkResult.isSuccess() && checkResult.output().trim().equals("exists")) {
                    // 服务存在，尝试停止并禁用
                    var stopCommand = String.format("sudo systemctl stop %s && sudo systemctl disable %s", service, service);
                    checkLogWriter.logRepairCommand(context.getClusterId(), context.getHostIp(),
                            getCheckKey(), stopCommand);
                    
                    var stopResult = getSshService().executeCommand(pluginContext, stopCommand);
                    checkLogWriter.logRepairOutput(context.getClusterId(), context.getHostIp(),
                            getCheckKey(), String.format("%s disable result: %s", service, stopResult.output()));
                    
                    if (stopResult.isSuccess()) {
                        disabledServices.add(service);
                        checkLogWriter.logRepairInfo(context.getClusterId(), context.getHostIp(),
                                getCheckKey(), String.format("成功禁用防火墙服务: %s", service), null);
                    } else {
                        failedServices.add(service);
                        Map<String, Object> errorDetails = new HashMap<>();
                        errorDetails.put("service", service);
                        errorDetails.put("error", stopResult.error());
                        checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                                getCheckKey(), String.format("禁用防火墙服务失败: %s", service), errorDetails);
                    }
                }
            }
            
            if (failedServices.isEmpty()) {
                String successMsg = String.format("成功禁用防火墙服务: %s", String.join(", ", disabledServices));
                Map<String, Object> successDetails = new HashMap<>();
                successDetails.put("disabledServices", disabledServices);
                successDetails.put("count", disabledServices.size());
                checkLogWriter.logRepairSuccess(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), successMsg, successDetails);
                
                return RepairResult.builder()
                        .success(true)
                        .message(successMsg)
                        .details(String.format("已禁用 %d 个防火墙服务", disabledServices.size()))
                        .build();
            } else {
                String errorMsg = String.format("部分防火墙服务禁用失败: %s", String.join(", ", failedServices));
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("disabledServices", disabledServices);
                errorDetails.put("failedServices", failedServices);
                checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), errorMsg, errorDetails);
                
                return RepairResult.builder()
                        .success(false)
                        .message(errorMsg)
                        .details(String.format("成功: %s, 失败: %s", 
                                String.join(", ", disabledServices),
                                String.join(", ", failedServices)))
                        .build();
            }
            
        } catch (Exception e) {
            log.error("修复防火墙配置时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("exception", e.getMessage());
            checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "修复失败", errorDetails);
            return RepairResult.builder()
                    .success(false)
                    .message("修复失败: " + e.getMessage())
                    .build();
        }
    }
}

