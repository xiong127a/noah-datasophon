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

import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.HostInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallJDKHandler implements DispatcherWorkerHandler {

    private static final Logger logger = LoggerFactory.getLogger(InstallJDKHandler.class);

    @Override
    public boolean handle(ClientSession session, HostInfo hostInfo) {
        hostInfo.setProgress(60);
        String arch = MinaUtils.execCmdWithResult(session, "arch");
        
        // 使用Constants.INSTALL_PATH作为基础安装路径
        String installPath = Constants.INSTALL_PATH;
        String jdkInstallPath = installPath + "/datasophon-worker/jdk";
        
        // 创建JDK安装目录
        MinaUtils.execCmdWithResult(session, "mkdir -p " + jdkInstallPath);
        
        // 检查JDK是否已经安装
        String checkJdkCmd = "[ -d " + jdkInstallPath + "/jdk1.8.0_333 ] && echo 'exists' || echo 'not exists'";
        String testResult = MinaUtils.execCmdWithResult(session, checkJdkCmd);
        boolean exists = "exists".equals(testResult.trim());
        
        if (!exists) {
            logger.info("JDK目录不存在，需要安装JDK");
            hostInfo.setMessage(MessageResolverUtils.getMessage("start.install.jdk"));
            // 上传JDK到worker目录
            MinaUtils.uploadFile(session, jdkInstallPath,
                    Constants.MASTER_MANAGE_PACKAGE_PATH + Constants.SLASH + Constants.X86JDK);
            // 解压JDK到worker目录
            MinaUtils.execCmdWithResult(session, "tar -zxvf " + jdkInstallPath + "/jdk-8u333-linux-x64.tar.gz -C " + jdkInstallPath + "/");
            // 创建符号链接便于引用
            MinaUtils.execCmdWithResult(session, "ln -sf " + jdkInstallPath + "/jdk1.8.0_333 " + jdkInstallPath + "/current");
        }
        
        if ("aarch64".equals(arch)) {
            if (!exists) {
                hostInfo.setMessage(MessageResolverUtils.getMessage("start.install.jdk"));
                // 上传JDK到worker目录
                MinaUtils.uploadFile(session, jdkInstallPath,
                        Constants.MASTER_MANAGE_PACKAGE_PATH + Constants.SLASH + Constants.ARMJDK);
                // 解压JDK到worker目录
                MinaUtils.execCmdWithResult(session,
                        "tar -zxvf " + jdkInstallPath + "/jdk-8u333-linux-aarch64.tar.gz -C " + jdkInstallPath + "/");
                // 创建符号链接便于引用
                MinaUtils.execCmdWithResult(session, "ln -sf " + jdkInstallPath + "/jdk1.8.0_333 " + jdkInstallPath + "/current");
            }
        }
        
        return true;
    }
}
