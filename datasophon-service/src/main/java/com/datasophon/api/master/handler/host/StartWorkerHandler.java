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
import com.datasophon.common.model.HostInfo;
import org.apache.commons.lang3.StringUtils;
import com.datasophon.api.utils.SshPluginHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;


public class StartWorkerHandler implements DispatcherWorkerHandler {

    private static final Logger logger = LoggerFactory.getLogger(StartWorkerHandler.class);


    private Long clusterId;


    private String clusterFrame;

    public StartWorkerHandler(Long clusterId, String clusterFrame) {
        this.clusterId = clusterId;
        this.clusterFrame = clusterFrame;
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
            String distroInfo = SshPluginHelper.detectLinuxDistro(hostInfo);
            logger.info("【StartWorker处理器】主机 {} 的Linux发行版: {}", hostInfo.getIp(), distroInfo);

            // 获取系统ID
            String osId = SshPluginHelper.executeCommand(hostInfo, 
                    "cat /etc/os-release | grep -E '^ID=' | cut -d'=' -f2 | tr -d '\"'").trim();
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
            
            String updateCommonPropertiesResult = SshPluginHelper.executeCommand(hostInfo, updateCommand);
        if (StringUtils.isBlank(updateCommonPropertiesResult) || "failed".equals(updateCommonPropertiesResult)) {
            logger.error("common.properties update failed");
            hostInfo.setErrMsg("common.properties update failed");
            hostInfo.setMessage(MessageResolverUtils.getMessage("modify.configuration.file.fail"));
            CommonUtils.updateInstallState(InstallState.FAILED, hostInfo);
        } else {
            logger.info("准备安装和启动Worker服务: {}", hostInfo.getIp());

            // 初始化环境
            logger.info("【StartWorker处理器】初始化系统环境");
            SshPluginHelper.safeExecuteCommand(hostInfo, "ulimit -n 65535");
            SshPluginHelper.safeExecuteCommand(hostInfo, "sysctl -w vm.max_map_count=2000000");

            // 配置Worker服务自启动
            logger.info("【StartWorker处理器】配置Worker服务自启动");

            // 使用安全的命令执行方法，自动适应不同的Linux发行版
            boolean success = true;
            String result;

            // 1. 检查并创建服务目录（如果需要）
            SshPluginHelper.safeExecuteCommand(hostInfo, "sudo mkdir -p " + serviceDir);

            // 2. 复制服务脚本
            result = SshPluginHelper.safeExecuteCommand(hostInfo,
                    "\\cp " + installPath + "/datasophon-worker/script/datasophon-worker " + serviceDir + "/");
            success &= !result.startsWith("ERROR:");

            // 3. 设置执行权限
            result = SshPluginHelper.safeExecuteCommand(hostInfo, "chmod +x " + serviceDir + "/datasophon-worker");
            success &= !result.startsWith("ERROR:");

            // 4. 根据系统类型配置服务
            if (useSystemd) {
                logger.info("【StartWorker处理器】使用systemd配置服务");
                boolean createResult = SshPluginHelper.createSystemdServiceForDebian(hostInfo,
                        serviceDir + "/datasophon-worker", installPath);
                logger.info("【StartWorker处理器】systemd服务文件创建结果: {}", createResult ? "成功" : "失败");

                if (createResult) {
                    // 如果创建成功，使用systemctl管理服务
                    SshPluginHelper.safeExecuteCommand(hostInfo, "systemctl daemon-reload");
                    result = SshPluginHelper.safeExecuteCommand(hostInfo, "systemctl enable datasophon-worker");
                } else {
                    // 回退到传统方式
                    if ("kylin".equals(osId)) {
                        result = SshPluginHelper.safeExecuteCommand(hostInfo, "chkconfig --add datasophon-worker");
                    } else {
                        result = SshPluginHelper.safeExecuteCommand(hostInfo, "update-rc.d datasophon-worker defaults");
                    }
                }
            } else {
                // CentOS使用chkconfig
                result = SshPluginHelper.safeExecuteCommand(hostInfo, "chkconfig --add datasophon-worker");
            }
            success &= !result.startsWith("ERROR:");

            // 6. 安装环境变量脚本
            result = SshPluginHelper.safeExecuteCommand(hostInfo,
                    "\\cp " + installPath + "/datasophon-worker/script/datasophon-env.sh /etc/profile.d/");
            success &= !result.startsWith("ERROR:");

            // 7. 加载环境变量
            result = SshPluginHelper.safeExecuteCommand(hostInfo, "source /etc/profile.d/datasophon-env.sh");
            success &= !result.startsWith("ERROR:");

            hostInfo.setMessage(MessageResolverUtils.getMessage("start.host.management.agent"));

            // 8. 启动服务
            logger.info("【StartWorker处理器】启动Worker服务: {}", hostInfo.getIp());
            if (useSystemd) {
                SshPluginHelper.safeExecuteCommand(hostInfo, "systemctl daemon-reload");
                // 使用restart替代start命令启动
                result = SshPluginHelper.safeExecuteCommand(hostInfo,
                        installPath + "/datasophon-worker/bin/datasophon-worker.sh restart worker");
                // 如果直接调用成功，再通过systemd重启确保服务被正确管理
                if (!result.startsWith("ERROR:")) {
                    result = SshPluginHelper.safeExecuteCommand(hostInfo, "systemctl restart datasophon-worker");
                }
            } else {
                result = SshPluginHelper.safeExecuteCommand(hostInfo, "service datasophon-worker restart");
            }
            success &= !result.startsWith("ERROR:");

            if (!success) {
                logger.warn("【StartWorker处理器】Worker服务安装或启动过程中出现警告，但将继续处理");
            }

            // 9. 验证服务状态
            logger.info("【StartWorker处理器】验证服务状态...");
            if (useSystemd) {
                result = SshPluginHelper.safeExecuteCommand(hostInfo, "systemctl status datasophon-worker");
            } else {
                result = SshPluginHelper.safeExecuteCommand(hostInfo, "service datasophon-worker status");
            }
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
}
