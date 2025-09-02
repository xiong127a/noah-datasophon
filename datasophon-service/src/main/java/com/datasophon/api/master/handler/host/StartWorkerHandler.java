/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.master.handler.host;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.ConfigBean;
import com.datasophon.api.utils.CommonUtils;
import com.datasophon.api.utils.MessageResolverUtils;

import com.datasophon.common.Constants;
import com.datasophon.common.enums.InstallState;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import com.datasophon.common.model.HostInfo;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;


public class StartWorkerHandler implements DispatcherWorkerHandler {

    private static final Logger logger = LoggerFactory.getLogger(StartWorkerHandler.class);
    
    // SSH连接服务
    private final SshConnectionService sshService = 
            SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();

    private Long clusterId;
    private String clusterFrame;

    public StartWorkerHandler(Long clusterId, String clusterFrame) {
        this.clusterId = clusterId;
        this.clusterFrame = clusterFrame;
    }
    
    /**
     * 构建SSH检查上下文
     */
    private HostCheckContext buildHostCheckContext(HostInfo hostInfo) {
        return HostCheckContext.builder()
                .hostIp(hostInfo.getIp())
                .sshPort(hostInfo.getSshPort())
                .sshUser(hostInfo.getSshUser())
                .sshPassword(hostInfo.getSshPassword())
                .build();
    }

    @Override
    public boolean handle(HostInfo hostInfo) throws UnknownHostException {
        try {
            ConfigBean configBean = SpringUtil.getBean(ConfigBean.class);
            // 使用SSH插件辅助工具
            String installPath = Constants.INSTALL_PATH;
            String localHostName = InetAddress.getLocalHost().getHostName();

            logger.info("【StartWorker处理器】开始启动Worker: {}", hostInfo.getIp());

            // 检测Linux发行版
            // 检测Linux发行版和系统ID
            HostCheckContext context = buildHostCheckContext(hostInfo);
            CommandResult distroResult = sshService.executeCommand(context, 
                    "cat /etc/os-release | grep -E '^ID=' | cut -d'=' -f2 | tr -d '\"'");
            String osId = distroResult.isSuccess() ? distroResult.output().trim() : "linux";
            String distroInfo = osId.isEmpty() ? "linux" : osId;
            logger.info("【StartWorker处理器】主机 {} 的Linux发行版: {}", hostInfo.getIp(), distroInfo);
            logger.info("【StartWorker处理器】系统ID: {}", osId);

        // 确定服务脚本路径
        String serviceDir = "/etc/rc.d/init.d";
        boolean useSystemd = false;

        // 根据系统类型决定服务管理方式
        if (distroInfo.toLowerCase().contains("ubuntu") ||
                distroInfo.toLowerCase().contains("debian") ||
                "kylin".equals(osId)) {
            serviceDir = "/etc/init.d";
            useSystemd = true;
        }
        logger.info("使用服务目录: {}, 使用systemd: {}", serviceDir, useSystemd);

            String updateCommand = Constants.UPDATE_COMMON_CMD +
                    localHostName +
                    Constants.SPACE +
                    configBean.getServerPort() +
                    Constants.SPACE +
                    this.clusterFrame +
                    Constants.SPACE +
                    this.clusterId +
                    Constants.SPACE +
                    Constants.INSTALL_PATH +
                    Constants.SPACE +
                    hostInfo.getIp();
            
            CommandResult updateResult = sshService.executeCommand(context, updateCommand);
            String updateCommonPropertiesResult = updateResult.isSuccess() ? updateResult.output() : "failed";
        if (StringUtils.isBlank(updateCommonPropertiesResult) || "failed".equals(updateCommonPropertiesResult)) {
            logger.error("common.properties update failed");
            hostInfo.setErrMsg("common.properties update failed");
            hostInfo.setMessage(MessageResolverUtils.getMessage("modify.configuration.file.fail"));
            CommonUtils.updateInstallState(InstallState.FAILED, hostInfo);
        } else {
            logger.info("准备安装和启动Worker服务: {}", hostInfo.getIp());

            // 初始化环境
            logger.info("【StartWorker处理器】初始化系统环境");
            sshService.executeCommand(context, "ulimit -n 65535");
            sshService.executeCommand(context, "sysctl -w vm.max_map_count=2000000");

            // 配置Worker服务自启动
            logger.info("【StartWorker处理器】配置Worker服务自启动");

            // 使用安全的命令执行方法，自动适应不同的Linux发行版
            boolean success = true;
            String result;

            // 1. 检查并创建服务目录（如果需要）
            sshService.executeCommand(context, "sudo mkdir -p " + serviceDir);

            // 2. 复制服务脚本
            CommandResult copyResult = sshService.executeCommand(context,
                    "\\cp " + installPath + "/datasophon-worker/script/datasophon-worker " + serviceDir + "/");
            success &= copyResult.isSuccess();
            result = copyResult.isSuccess() ? "SUCCESS" : "ERROR: " + copyResult.error();

            // 3. 设置执行权限
            CommandResult chmodResult = sshService.executeCommand(context, "chmod +x " + serviceDir + "/datasophon-worker");
            success &= chmodResult.isSuccess();
            result = chmodResult.isSuccess() ? "SUCCESS" : "ERROR: " + chmodResult.error();

            // 4. 根据系统类型配置服务
            if (useSystemd) {
                logger.info("【StartWorker处理器】使用systemd配置服务");
                // 创建systemd服务文件
                String createServiceCommand = getCreateServiceCommand(serviceDir, installPath);
                CommandResult serviceFileResult = sshService.executeCommand(context, createServiceCommand);
                boolean createResult = serviceFileResult.isSuccess();
                logger.info("【StartWorker处理器】systemd服务文件创建结果: {}", createResult ? "成功" : "失败");

                if (createResult) {
                    // 如果创建成功，使用systemctl管理服务
                    sshService.executeCommand(context, "systemctl daemon-reload");
                    CommandResult enableResult = sshService.executeCommand(context, "systemctl enable datasophon-worker");
                    result = enableResult.isSuccess() ? "SUCCESS" : "ERROR: " + enableResult.error();
                } else {
                    // 回退到传统方式
                    CommandResult serviceResult;
                    if ("kylin".equals(osId)) {
                        serviceResult = sshService.executeCommand(context, "chkconfig --add datasophon-worker");
                    } else {
                        serviceResult = sshService.executeCommand(context, "update-rc.d datasophon-worker defaults");
                    }
                    result = serviceResult.isSuccess() ? "SUCCESS" : "ERROR: " + serviceResult.error();
                }
            } else {
                // CentOS使用chkconfig
                CommandResult chkconfigResult = sshService.executeCommand(context, "chkconfig --add datasophon-worker");
                result = chkconfigResult.isSuccess() ? "SUCCESS" : "ERROR: " + chkconfigResult.error();
            }
            success &= !result.startsWith("ERROR:");

            // 6. 安装环境变量脚本
            CommandResult envResult = sshService.executeCommand(context,
                    "\\cp " + installPath + "/datasophon-worker/script/datasophon-env.sh /etc/profile.d/");
            success &= envResult.isSuccess();
            result = envResult.isSuccess() ? "SUCCESS" : "ERROR: " + envResult.error();

            // 7. 加载环境变量
            CommandResult sourceResult = sshService.executeCommand(context, "source /etc/profile.d/datasophon-env.sh");
            result = sourceResult.isSuccess() ? "SUCCESS" : "ERROR: " + sourceResult.error();
            success &= !result.startsWith("ERROR:");

            hostInfo.setMessage(MessageResolverUtils.getMessage("start.host.management.agent"));

            // 8. 启动服务
            logger.info("【StartWorker处理器】启动Worker服务: {}", hostInfo.getIp());
            if (useSystemd) {
                sshService.executeCommand(context, "systemctl daemon-reload");
                // 使用restart替代start命令启动
                CommandResult restartScriptResult = sshService.executeCommand(context,
                        installPath + "/datasophon-worker/bin/datasophon-worker.sh restart worker");
                // 如果直接调用成功，再通过systemd重启确保服务被正确管理
                if (restartScriptResult.isSuccess()) {
                    CommandResult systemdRestartResult = sshService.executeCommand(context, "systemctl restart datasophon-worker");
                    result = systemdRestartResult.isSuccess() ? "SUCCESS" : "ERROR: " + systemdRestartResult.error();
                } else {
                    result = "ERROR: " + restartScriptResult.error();
                }
            } else {
                CommandResult serviceRestartResult = sshService.executeCommand(context, "service datasophon-worker restart");
                result = serviceRestartResult.isSuccess() ? "SUCCESS" : "ERROR: " + serviceRestartResult.error();
            }
            success &= !result.startsWith("ERROR:");

            if (!success) {
                logger.warn("【StartWorker处理器】Worker服务安装或启动过程中出现警告，但将继续处理");
            }

            // 9. 验证服务状态
            logger.info("【StartWorker处理器】验证服务状态...");
            CommandResult statusResult;
            if (useSystemd) {
                statusResult = sshService.executeCommand(context, "systemctl status datasophon-worker");
            } else {
                statusResult = sshService.executeCommand(context, "service datasophon-worker status");
            }
            result = statusResult.isSuccess() ? "SUCCESS" : "ERROR: " + statusResult.error();
            logger.info("【StartWorker处理器】服务状态: {}", result);

            hostInfo.setProgress(75);
            hostInfo.setCreateTime(LocalDateTime.now());
            }

            logger.info("【StartWorker处理器】完成主机代理分发: {}", hostInfo.getIp());
            return true;
            
        } catch (Exception e) {
            logger.error("【StartWorker处理器】处理异常: {} -> {}", hostInfo.getIp(), e.getMessage(), e);
            hostInfo.setErrorMessage("StartWorker处理异常: " + e.getMessage());
            return false;
        }
    }

    private static String getCreateServiceCommand(String serviceDir, String installPath) {
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
}
