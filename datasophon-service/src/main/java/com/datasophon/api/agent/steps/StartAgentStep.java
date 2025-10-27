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
            String localHostName = InetAddress.getLocalHost().getHostName();
            
            // 1. 检测Linux发行版
            CommandResult distroResult = sshService.executeCommand(pluginContext,
                    "cat /etc/os-release | grep -E '^ID=' | cut -d'=' -f2 | tr -d '\"'");
            String osId = distroResult.isSuccess() ? distroResult.output().trim() : "linux";
            String distroInfo = osId.isEmpty() ? "linux" : osId;
            
            Map<String, Object> osInfo = new HashMap<>();
            osInfo.put("osId", osId);
            osInfo.put("distro", distroInfo);
            logWriter.logInfo(clusterId, hostIp, "start", 
                    "检测到Linux发行版: " + distroInfo, osInfo);
            log.info("主机 {} 的Linux发行版: {}", hostIp, distroInfo);
            
            // 确定服务脚本路径和管理方式
            String serviceDir = "/etc/rc.d/init.d";
            boolean useSystemd = false;
            
            if (distroInfo.toLowerCase().contains("ubuntu") ||
                    distroInfo.toLowerCase().contains("debian") ||
                    "kylin".equals(osId)) {
                serviceDir = "/etc/init.d";
                useSystemd = true;
            }
            
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("serviceDir", serviceDir);
            serviceInfo.put("useSystemd", useSystemd);
            logWriter.logInfo(clusterId, hostIp, "start", 
                    "服务管理方式: " + (useSystemd ? "systemd" : "SysVinit"), serviceInfo);
            
            // 2. 更新common.properties配置
            logWriter.logInfo(clusterId, hostIp, "start", "更新Worker配置文件", null);
            String updateCommand = Constants.UPDATE_COMMON_CMD +
                    localHostName +
                    Constants.SPACE +
                    configBean.getServerPort() +
                    Constants.SPACE +
                    clusterFrame +
                    Constants.SPACE +
                    clusterId +
                    Constants.SPACE +
                    installPath +
                    Constants.SPACE +
                    hostIp;
            
            logWriter.logCommand(clusterId, hostIp, "start", updateCommand);
            CommandResult updateResult = sshService.executeCommand(pluginContext, updateCommand);
            
            if (!updateResult.isSuccess() || StringUtils.isBlank(updateResult.output())) {
                throw new Exception("更新common.properties配置失败");
            }
            
            logWriter.logInfo(clusterId, hostIp, "start", "配置文件更新完成", null);
            
            // 3. 初始化系统环境
            logWriter.logInfo(clusterId, hostIp, "start", "初始化系统环境", null);
            sshService.executeCommand(pluginContext, "ulimit -n 65535");
            sshService.executeCommand(pluginContext, "sysctl -w vm.max_map_count=2000000");
            
            // 4. 配置服务自启动
            logWriter.logInfo(clusterId, hostIp, "start", "配置Worker服务自启动", null);
            
            // 创建服务目录
            sshService.executeCommand(pluginContext, "sudo mkdir -p " + serviceDir);
            
            // 复制服务脚本
            CommandResult copyResult = sshService.executeCommand(pluginContext,
                    "\\cp " + installPath + "/datasophon-worker/script/datasophon-worker " + serviceDir + "/");
            if (!copyResult.isSuccess()) {
                throw new Exception("复制服务脚本失败: " + copyResult.error());
            }
            
            // 设置执行权限
            CommandResult chmodResult = sshService.executeCommand(pluginContext,
                    "chmod +x " + serviceDir + "/datasophon-worker");
            if (!chmodResult.isSuccess()) {
                throw new Exception("设置服务脚本权限失败: " + chmodResult.error());
            }
            
            // 根据系统类型配置服务
            if (useSystemd) {
                logWriter.logInfo(clusterId, hostIp, "start", "使用systemd配置服务", null);
                
                // 创建systemd服务文件
                String createServiceCommand = getCreateServiceCommand(serviceDir, installPath);
                CommandResult serviceFileResult = sshService.executeCommand(pluginContext, createServiceCommand);
                
                if (serviceFileResult.isSuccess()) {
                    // 重新加载systemd
                    sshService.executeCommand(pluginContext, "systemctl daemon-reload");
                    
                    // 启用服务自启动
                    CommandResult enableResult = sshService.executeCommand(pluginContext,
                            "systemctl enable datasophon-worker");
                    if (!enableResult.isSuccess()) {
                        log.warn("systemctl enable失败: {}", enableResult.error());
                    }
                } else {
                    // 回退到传统方式
                    log.warn("创建systemd服务文件失败，回退到传统方式");
                    CommandResult serviceResult;
                    if ("kylin".equals(osId)) {
                        serviceResult = sshService.executeCommand(pluginContext,
                                "chkconfig --add datasophon-worker");
                    } else {
                        serviceResult = sshService.executeCommand(pluginContext,
                                "update-rc.d datasophon-worker defaults");
                    }
                    if (!serviceResult.isSuccess()) {
                        log.warn("配置服务自启动失败: {}", serviceResult.error());
                    }
                }
            } else {
                // CentOS使用chkconfig
                logWriter.logInfo(clusterId, hostIp, "start", "使用chkconfig配置服务", null);
                CommandResult chkconfigResult = sshService.executeCommand(pluginContext,
                        "chkconfig --add datasophon-worker");
                if (!chkconfigResult.isSuccess()) {
                    log.warn("chkconfig配置失败: {}", chkconfigResult.error());
                }
            }
            
            // 5. 安装环境变量脚本
            logWriter.logInfo(clusterId, hostIp, "start", "安装环境变量脚本", null);
            CommandResult envResult = sshService.executeCommand(pluginContext,
                    "\\cp " + installPath + "/datasophon-worker/script/datasophon-env.sh /etc/profile.d/");
            if (!envResult.isSuccess()) {
                log.warn("安装环境变量脚本失败: {}", envResult.error());
            }
            
            // 加载环境变量
            sshService.executeCommand(pluginContext, "source /etc/profile.d/datasophon-env.sh");
            
            // 6. 启动服务
            logWriter.logInfo(clusterId, hostIp, "start", "启动Worker服务", null);
            CommandResult startResult;
            if (useSystemd) {
                sshService.executeCommand(pluginContext, "systemctl daemon-reload");
                
                // 先用脚本启动，再用systemd管理
                CommandResult restartScriptResult = sshService.executeCommand(pluginContext,
                        installPath + "/datasophon-worker/bin/datasophon-worker.sh restart worker");
                
                if (restartScriptResult.isSuccess()) {
                    startResult = sshService.executeCommand(pluginContext,
                            "systemctl restart datasophon-worker");
                } else {
                    throw new Exception("使用脚本启动Worker失败: " + restartScriptResult.error());
                }
            } else {
                startResult = sshService.executeCommand(pluginContext,
                        "service datasophon-worker restart");
            }
            
            if (!startResult.isSuccess()) {
                throw new Exception("启动Worker服务失败: " + startResult.error());
            }
            
            logWriter.logInfo(clusterId, hostIp, "start", "Worker服务启动成功", null);
            
            // 7. 验证服务状态
            logWriter.logInfo(clusterId, hostIp, "start", "验证服务状态", null);
            CommandResult statusResult;
            if (useSystemd) {
                statusResult = sshService.executeCommand(pluginContext,
                        "systemctl status datasophon-worker");
            } else {
                statusResult = sshService.executeCommand(pluginContext,
                        "service datasophon-worker status");
            }
            
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
     */
    private String getCreateServiceCommand(String serviceDir, String installPath) {
        String serviceContent = String.format(
                """
                        [Unit]
                        Description=DataSophon Worker Service
                        After=network.target
                        
                        [Service]
                        Type=forking
                        ExecStart=%s start
                        ExecStop=%s stop
                        ExecReload=%s restart
                        WorkingDirectory=%s
                        User=root
                        Group=root
                        Restart=on-failure
                        RestartSec=10
                        
                        [Install]
                        WantedBy=multi-user.target
                        """,
                serviceDir + "/datasophon-worker", serviceDir + "/datasophon-worker",
                serviceDir + "/datasophon-worker", installPath);
        
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

