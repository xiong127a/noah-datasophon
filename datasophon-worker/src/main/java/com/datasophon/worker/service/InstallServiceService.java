/*
 *
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
 *
 */

package com.datasophon.worker.service;

import com.datasophon.common.Constants;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.InstallServiceHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;

/**
 * 服务安装服务
 * 替代原来的InstallServiceActor
 */
@Service
public class InstallServiceService {

    private static final Logger logger = LoggerFactory.getLogger(InstallServiceService.class);

    /**
     * 安装服务
     */
    public ExecResult install(InstallServiceRoleCommand command) {
        ExecResult installResult = new ExecResult();
        
        try {
            InstallServiceHandler serviceHandler = new InstallServiceHandler(
                    command.getServiceName(),
                    command.getServiceRoleName());

            logger.info("Start install package {}", command.getPackageName());
            
            // Kerberos特殊处理
            if (command.getDecompressPackageName().contains("kerberos")) {
                installResult = installKerberos(command, serviceHandler);
            } else {
                // 普通服务安装
                installResult = serviceHandler.install(command);
                
                // 创建软连接
                createSymbolicLink(command);
            }
            
            logger.info("Install {} {}", command.getPackageName(),
                    installResult.getExecResult() ? "success" : "failed");
                    
        } catch (Exception e) {
            logger.error("Failed to install service", e);
            installResult.setExecResult(false);
            installResult.setExecOut("Installation failed: " + e.getMessage());
        }
        
        return installResult;
    }

    /**
     * 安装Kerberos服务
     */
    private ExecResult installKerberos(InstallServiceRoleCommand command, InstallServiceHandler serviceHandler) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("yum");
        commands.add("install");
        commands.add("-y");
        
        if (ServiceRoleType.MASTER == command.getServiceRoleType()) {
            logger.info("Installing Kerberos Master");
            commands.add("krb5-server");
        }
        
        commands.add("krb5-workstation");
        commands.add("krb5-libs");
        
        if ("aarch64".equals(ShellUtils.getCpuArchitecture())) {
            commands.add("--skip-broken");
        }
        
        ExecResult execResult = ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 180, logger);
        
        if (execResult.getExecResult()) {
            if ("aarch64".equals(ShellUtils.getCpuArchitecture())) {
                ShellUtils.exceShell("sudo sed -i 's/^/#/' /etc/krb5.conf.d/kcm_default_ccache");
                ShellUtils.exceShell("sudo systemctl restart krb5kdc");
                ShellUtils.exceShell("sudo systemctl restart kadmin");
            }
            return serviceHandler.install(command);
        }
        
        return execResult;
    }

    /**
     * 创建服务软连接
     */
    private void createSymbolicLink(InstallServiceRoleCommand command) {
        String appHome = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        String appLinkHome = Constants.INSTALL_PATH + Constants.SLASH
                + StringUtils.lowerCase(command.getServiceName());
                
        if (!new File(appLinkHome).exists()) {
            ShellUtils.exceShell("ln -s " + appHome + " " + appLinkHome);
            logger.info("Create symbolic dir: {}", appLinkHome);
        }
    }
}

