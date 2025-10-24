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
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
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
        
        // 清理旧日志
        checkLogWriter.clearLogs(context.getClusterId(), context.getHostIp(), getCheckKey());
        
        // 记录检查开始
        checkLogWriter.logCheckStart(context.getClusterId(), context.getHostIp(), 
                getCheckKey(), "开始检查SELinux状态");
        
        try {
            // 检查SELinux状态
            var pluginContext = toPluginContext(context);
            // 使用 bash -l 模拟登录环境，确保加载最新的配置
            // 虽然 setenforce 0 立即生效，但使用 bash -l 保持与其他检查器的一致性
            var command = "bash -l -c 'getenforce 2>/dev/null || echo Disabled'";
            
            // 记录执行命令
            checkLogWriter.logCheckCommand(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "检查SELinux状态（模拟登录环境）: " + command);
            
            var result = getSshService().executeCommand(pluginContext, command);
            
            if (!result.isSuccess()) {
                String errorMsg = "无法获取SELinux状态: " + result.error();
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("error", result.error());
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), errorMsg, errorDetails);
                return CheckResult.failure(
                        errorMsg,
                        "请检查SSH连接和系统命令是否可用",
                        true,
                        false
                );
            }
            
            // 记录命令输出
            checkLogWriter.logCheckOutput(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), result.output());
            
            var selinuxStatus = result.output().trim();
            var details = new HashMap<String, Object>();
            details.put("status", selinuxStatus);
            details.put("allowedModes", ALLOWED_MODES);
            
            // 记录状态解析
            checkLogWriter.logCheckInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), String.format("SELinux当前状态: %s", selinuxStatus), details);
            
            // 检查是否在允许的模式中（不区分大小写）
            boolean isAllowed = ALLOWED_MODES.stream()
                    .anyMatch(mode -> mode.equalsIgnoreCase(selinuxStatus));
            
            if (!isAllowed) {
                String failMsg = String.format("SELinux处于非推荐状态: %s", selinuxStatus);
                details.put("recommendation", "大数据集群部署建议禁用SELinux或设置为permissive模式");
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), failMsg, details);
                
                var checkResult = CheckResult.failure(
                        failMsg,
                        "大数据集群部署建议禁用SELinux或设置为permissive模式",
                        true, // 可以跳过
                        true  // 可以自动修复
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            String successMsg = String.format("SELinux检查通过：当前状态 %s", selinuxStatus);
            checkLogWriter.logCheckSuccess(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), successMsg, details);
            
            var checkResult = CheckResult.success(successMsg);
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (Exception e) {
            log.error("检查SELinux时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", e.getMessage());
            checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "检查SELinux时发生异常", errorDetails);
            return CheckResult.failure(
                    "检查SELinux时发生异常: " + e.getMessage(),
                    "请检查SSH连接和系统状态",
                    true,
                    false
            );
        }
    }
    
    /**
     * 修复SELinux配置
     * 
     * 注意：此方法仅执行修复操作（临时禁用SELinux + 修改配置文件），不包含验证逻辑。
     * 验证由框架在修复成功后自动调用 execute() 方法完成。
     * execute() 使用 bash -l 模拟登录环境来验证配置。
     * 
     * @param context 主机检查上下文
     * @param params 修复参数（当前未使用）
     * @return RepairResult 修复结果（仅表示修复操作是否成功）
     */
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        log.info("开始修复主机 {} 的SELinux配置", context.getHostIp());
        
        // 记录修复开始
        checkLogWriter.logRepairStart(context.getClusterId(), context.getHostIp(),
                getCheckKey(), "开始修复SELinux配置");
        
        try {
            var pluginContext = toPluginContext(context);
            
            // 临时设置为permissive模式
            var tempCommand = "sudo setenforce 0";
            checkLogWriter.logRepairCommand(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), tempCommand);
            
            var tempResult = getSshService().executeCommand(pluginContext, tempCommand);
            checkLogWriter.logRepairOutput(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), tempResult.output());
            
            if (!tempResult.isSuccess()) {
                String errorMsg = "临时禁用SELinux失败: " + tempResult.error();
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("error", tempResult.error());
                checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), errorMsg, errorDetails);
                return RepairResult.builder()
                        .success(false)
                        .message(errorMsg)
                        .build();
            }
            
            checkLogWriter.logRepairInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "临时禁用SELinux成功，正在修改配置文件", null);
            
            // 永久禁用：修改配置文件
            var permanentCommand = "sudo sed -i 's/^SELINUX=enforcing/SELINUX=disabled/' /etc/selinux/config && " +
                    "sudo sed -i 's/^SELINUX=permissive/SELINUX=disabled/' /etc/selinux/config";
            checkLogWriter.logRepairCommand(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), permanentCommand);
            
            var permanentResult = getSshService().executeCommand(pluginContext, permanentCommand);
            checkLogWriter.logRepairOutput(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), permanentResult.output());
            
            if (!permanentResult.isSuccess()) {
                String warnMsg = "修改SELinux配置文件失败，但已临时禁用";
                Map<String, Object> warnDetails = new HashMap<>();
                warnDetails.put("error", permanentResult.error());
                warnDetails.put("note", "临时禁用成功，永久禁用失败。重启后SELinux可能会重新启用");
                checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), warnMsg, warnDetails);
                return RepairResult.builder()
                        .success(false)
                        .message(warnMsg)
                        .details("临时禁用成功，永久禁用失败。重启后SELinux可能会重新启用")
                        .build();
            }
            
            String successMsg = "SELinux已成功禁用";
            Map<String, Object> successDetails = new HashMap<>();
            successDetails.put("note", "已临时禁用SELinux并修改配置文件，重启后永久生效");
            checkLogWriter.logRepairSuccess(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), successMsg, successDetails);
            
            return RepairResult.builder()
                    .success(true)
                    .message(successMsg)
                    .details("已临时禁用SELinux并修改配置文件，重启后永久生效")
                    .build();
                    
        } catch (Exception e) {
            log.error("修复SELinux配置时发生异常: {}", e.getMessage(), e);
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

