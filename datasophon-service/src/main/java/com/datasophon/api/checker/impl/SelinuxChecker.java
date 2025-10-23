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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SELinux检查器
 * 检查SELinux状态，支持自动禁用
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@Component
public class SelinuxChecker implements EnvironmentCheckItem {
    
    @Value("${datasophon.checker.selinux.enabled:true}")
    private boolean enabled;
    
    @Value("${datasophon.checker.selinux.auto-disable:false}")
    private boolean autoDisable;
    
    private SshConnectionService sshService;
    
    // 允许的SELinux模式
    private static final List<String> ALLOWED_MODES = List.of("disabled", "permissive");
    
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
        return "selinux";
    }
    
    @Override
    public String getDisplayName() {
        return "SELinux检查";
    }
    
    @Override
    public int getPriority() {
        return 123; // 参考配置
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public CheckResult execute(HostCheckContext context) {
        log.info("开始检查主机 {} 的SELinux状态", context.getHostIp());
        
        try {
            // 检查SELinux状态
            var pluginContext = toPluginContext(context);
            var result = getSshService().executeCommand(pluginContext, "getenforce 2>/dev/null || echo 'Disabled'");
            
            if (!result.isSuccess()) {
                return CheckResult.failure(
                        "无法获取SELinux状态: " + result.error(),
                        "请检查SSH连接和系统命令是否可用",
                        true,
                        false
                );
            }
            
            var selinuxStatus = result.output().trim();
            var details = new HashMap<String, Object>();
            details.put("status", selinuxStatus);
            details.put("allowedModes", ALLOWED_MODES);
            
            // 检查是否在允许的模式中（不区分大小写）
            boolean isAllowed = ALLOWED_MODES.stream()
                    .anyMatch(mode -> mode.equalsIgnoreCase(selinuxStatus));
            
            if (!isAllowed) {
                var checkResult = CheckResult.failure(
                        String.format("SELinux处于非推荐状态: %s", selinuxStatus),
                        "大数据集群部署建议禁用SELinux或设置为permissive模式",
                        true, // 可以跳过
                        true  // 可以自动修复
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            var checkResult = CheckResult.success(
                    String.format("SELinux检查通过：当前状态 %s", selinuxStatus)
            );
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (Exception e) {
            log.error("检查SELinux时发生异常: {}", e.getMessage(), e);
            return CheckResult.failure(
                    "检查SELinux时发生异常: " + e.getMessage(),
                    "请检查SSH连接和系统状态",
                    true,
                    false
            );
        }
    }
    
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        log.info("开始修复主机 {} 的SELinux配置", context.getHostIp());
        
        try {
            var pluginContext = toPluginContext(context);
            
            // 临时设置为permissive模式
            var tempResult = getSshService().executeCommand(pluginContext, "sudo setenforce 0");
            
            if (!tempResult.isSuccess()) {
                return RepairResult.builder()
                        .success(false)
                        .message("临时禁用SELinux失败: " + tempResult.error())
                        .build();
            }
            
            // 永久禁用：修改配置文件
            var permanentResult = getSshService().executeCommand(pluginContext,
                    "sudo sed -i 's/^SELINUX=enforcing/SELINUX=disabled/' /etc/selinux/config && " +
                    "sudo sed -i 's/^SELINUX=permissive/SELINUX=disabled/' /etc/selinux/config");
            
            if (!permanentResult.isSuccess()) {
                return RepairResult.builder()
                        .success(false)
                        .message("修改SELinux配置文件失败，但已临时禁用")
                        .details("临时禁用成功，永久禁用失败。重启后SELinux可能会重新启用")
                        .build();
            }
            
            return RepairResult.builder()
                    .success(true)
                    .message("SELinux已成功禁用")
                    .details("已临时禁用SELinux并修改配置文件，重启后永久生效")
                    .build();
                    
        } catch (Exception e) {
            log.error("修复SELinux配置时发生异常: {}", e.getMessage(), e);
            return RepairResult.builder()
                    .success(false)
                    .message("修复失败: " + e.getMessage())
                    .build();
        }
    }
}

