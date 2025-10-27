package com.datasophon.api.agent.steps;

import com.datasophon.api.agent.AgentDistributionContext;
import com.datasophon.api.agent.AgentDistributionStep;
import com.datasophon.api.agent.util.AgentLogWriter;
import com.datasophon.api.load.ConfigBean;
import com.datasophon.common.Constants;
import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 启动Agent服务步骤
 * 配置Worker服务自启动并启动服务
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
@RequiredArgsConstructor
public class StartAgentStep implements AgentDistributionStep {
    
    private final SshConnectionService sshService;
    private final ConfigBean configBean;
    private final String clusterFrame;
    
    @Override
    public String getStepName() {
        return "启动Agent服务";
    }
    
    @Override
    public void execute(AgentDistributionContext context) throws Exception {
        AgentLogWriter logWriter = context.getLogWriter();
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        
        logWriter.logInfo(clusterId, hostIp, "start", "开始配置和启动Agent服务", null);
        log.info("开始配置和启动Agent服务: {}", hostIp);
        
        try {
            // 转换为Plugin需要的Context
            HostCheckContext pluginContext = toPluginContext(context);
            
            String installPath = context.getRemoteInstallPath();
            
            // 1. 检测Linux发行版
            CommandResult distroResult = sshService.executeCommand(pluginContext,
                    "cat /etc/os-release | grep -E '^ID=' | cut -d'=' -f2 | tr -d '\"'");
            String distroInfo = distroResult.isSuccess() && !distroResult.output().trim().isEmpty() 
                    ? distroResult.output().trim() 
                    : "linux";
            
            Map<String, Object> osInfo = new HashMap<>();
            osInfo.put("distro", distroInfo);
            osInfo.put("serviceManager", "systemd");
            osInfo.put("architecture", "API→Worker单向通信");
            logWriter.logInfo(clusterId, hostIp, "start", 
                    "Linux发行版: " + distroInfo + ", 服务管理: systemd", osInfo);
            log.info("主机 {} Linux发行版: {}, 使用systemd管理Worker服务", hostIp, distroInfo);
            
            // 2. 初始化系统环境
            logWriter.logInfo(clusterId, hostIp, "start", "初始化系统环境", null);
            sshService.executeCommand(pluginContext, "ulimit -n 65535");
            sshService.executeCommand(pluginContext, "sysctl -w vm.max_map_count=2000000");
            
            // 3. 创建systemd服务文件
            logWriter.logInfo(clusterId, hostIp, "start", "创建systemd服务文件", null);
            String createServiceCommand = getCreateServiceCommand(installPath);
            CommandResult serviceFileResult = sshService.executeCommand(pluginContext, createServiceCommand);
            
            if (!serviceFileResult.isSuccess()) {
                throw new Exception("创建systemd服务文件失败: " + serviceFileResult.error());
            }
            
            // 4. 重新加载systemd并启用服务
            logWriter.logInfo(clusterId, hostIp, "start", "配置Worker服务自启动", null);
            sshService.executeCommand(pluginContext, "systemctl daemon-reload");
            
            CommandResult enableResult = sshService.executeCommand(pluginContext,
                    "systemctl enable datasophon-worker");
            if (!enableResult.isSuccess()) {
                log.warn("systemctl enable失败: {}", enableResult.error());
            }
            
            // 5. 安装环境变量脚本
            logWriter.logInfo(clusterId, hostIp, "start", "安装环境变量脚本", null);
            CommandResult envResult = sshService.executeCommand(pluginContext,
                    "\\cp " + installPath + "/datasophon-worker/script/datasophon-env.sh /etc/profile.d/");
            if (!envResult.isSuccess()) {
                log.warn("安装环境变量脚本失败: {}", envResult.error());
            }
            
            // 6. 启动Worker服务
            logWriter.logInfo(clusterId, hostIp, "start", "启动Worker服务", null);
            CommandResult startResult = sshService.executeCommand(pluginContext,
                    "systemctl restart datasophon-worker");
            
            if (!startResult.isSuccess()) {
                throw new Exception("启动Worker服务失败: " + startResult.error());
            }
            
            logWriter.logInfo(clusterId, hostIp, "start", "Worker服务启动成功", null);
            
            // 7. 验证服务状态
            logWriter.logInfo(clusterId, hostIp, "start", "验证服务状态", null);
            CommandResult statusResult = sshService.executeCommand(pluginContext,
                    "systemctl status datasophon-worker");
            
            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("status", statusResult.isSuccess() ? "running" : "unknown");
            statusInfo.put("output", statusResult.output());
            
            if (statusResult.isSuccess()) {
                logWriter.logSuccess(clusterId, hostIp, "start",
                        "Agent服务启动并运行成功", statusInfo);
                log.info("Agent服务启动成功: {}", hostIp);
            } else {
                logWriter.logWarning(clusterId, hostIp, "start",
                        "服务状态验证失败，但服务可能已启动", statusInfo);
                log.warn("服务状态验证失败: {}, output: {}", hostIp, statusResult.output());
            }
            
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            logWriter.logError(clusterId, hostIp, "start",
                    "启动Agent服务失败: " + e.getMessage(), errorInfo);
            throw new Exception("启动Agent服务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建systemd服务文件命令
     * 新架构: API→Worker单向通信，无需Worker主动上报
     */
    private String getCreateServiceCommand(String installPath) {
        String startScript = installPath + "/datasophon-worker/bin/datasophon-worker.sh";
        
        String serviceContent = String.format(
                """
                        [Unit]
                        Description=DataSophon Worker Service
                        Documentation=https://github.com/datasophon/datasophon
                        After=network-online.target
                        Wants=network-online.target
                        
                        [Service]
                        Type=forking
                        ExecStart=%s start worker
                        ExecStop=%s stop worker
                        ExecReload=%s restart worker
                        WorkingDirectory=%s/datasophon-worker
                        User=root
                        Group=root
                        Restart=on-failure
                        RestartSec=10
                        LimitNOFILE=65535
                        
                        [Install]
                        WantedBy=multi-user.target
                        """,
                startScript, startScript, startScript, installPath);
        
        return String.format("echo '%s' | tee /etc/systemd/system/datasophon-worker.service > /dev/null",
                serviceContent.replace("'", "'\"'\"'"));
    }
    
    /**
     * 将AgentDistributionContext转换为HostCheckContext
     */
    private HostCheckContext toPluginContext(AgentDistributionContext context) {
        return HostCheckContext.builder()
                .clusterId(context.getClusterId())
                .hostIp(context.getHostIp())
                .hostname(context.getHostname())
                .sshUser(context.getSshUser())
                .sshPort(context.getSshPort())
                .sshPassword(context.getSshPassword())
                .build();
    }
}

