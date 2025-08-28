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

import com.datasophon.api.utils.CommonUtils;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.service.SshPluginAdapterService;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.HostInfo;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadWorkerHandler implements DispatcherWorkerHandler {

    private static final Logger logger = LoggerFactory.getLogger(UploadWorkerHandler.class);

    @Override
    public boolean handle(HostInfo hostInfo) {
        try {
            logger.info("【上传Worker处理器】开始上传Worker包: {}", hostInfo.getIp());
            
            SshPluginAdapterService sshAdapter = SpringUtil.getBean(SshPluginAdapterService.class);
            
            String localPath = Constants.MASTER_MANAGE_PACKAGE_PATH + Constants.SLASH + Constants.WORKER_PACKAGE_NAME;
            boolean uploadFile = sshAdapter.uploadFile(hostInfo, localPath, Constants.INSTALL_PATH);
            
            if (uploadFile) {
                hostInfo.setMessage(
                        MessageResolverUtils.getMessage("distribution.successful.and.starts.md5.authentication"));
                hostInfo.setProgress(25);
                logger.info("【上传Worker处理器】文件上传成功: {}", hostInfo.getIp());
            } else {
                hostInfo.setMessage(
                        MessageResolverUtils.getMessage("distributed.host.management.agent.installation.package.fail"));
                hostInfo.setErrMsg("dispatcher host agent to " + hostInfo.getIp() + " failed");
                CommonUtils.updateInstallState(InstallState.FAILED, hostInfo);
                logger.error("【上传Worker处理器】文件上传失败: {}", hostInfo.getIp());
            }
            return uploadFile;
            
        } catch (Exception e) {
            logger.error("【上传Worker处理器】处理异常: {} -> {}", hostInfo.getIp(), e.getMessage(), e);
            hostInfo.setErrorMessage("上传Worker异常: " + e.getMessage());
            return false;
        }
    }
}
