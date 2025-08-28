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

public class DecompressWorkerHandler implements DispatcherWorkerHandler {

    private static final Logger logger = LoggerFactory.getLogger(DecompressWorkerHandler.class);

    @Override
    public boolean handle(HostInfo hostInfo) {
        try {
            logger.info("【解压Worker处理器】开始解压Worker包: {}", hostInfo.getIp());
            
            SshPluginAdapterService sshAdapter = SpringUtil.getBean(SshPluginAdapterService.class);
            
            // 通过SSH插件适配器执行解压命令
            String decompressResult = sshAdapter.executeCommand(hostInfo, Constants.UNZIP_DDH_WORKER_CMD);
            
            if (Constants.FAILED.equals(decompressResult)) {
                logger.error("【解压Worker处理器】tar -zxvf datasophon-worker.tar.gz failed");
                hostInfo.setErrMsg("tar -zxvf datasophon-worker.tar.gz failed");
                hostInfo.setMessage(MessageResolverUtils.getMessage("decompress.installation.package.fail"));
                CommonUtils.updateInstallState(InstallState.FAILED, hostInfo);
                return false;
            }
            
            logger.info("【解压Worker处理器】decompress datasophon-worker.tar.gz success");
            hostInfo.setProgress(50);
            hostInfo.setMessage(MessageResolverUtils
                    .getMessage("installation.package.decompressed.success.and.modify.configuration.file"));
            
            logger.info("【解压Worker处理器】解压完成: {}", hostInfo.getIp());
            return true;
            
        } catch (Exception e) {
            logger.error("【解压Worker处理器】处理异常: {} -> {}", hostInfo.getIp(), e.getMessage(), e);
            hostInfo.setErrorMessage("解压Worker异常: " + e.getMessage());
            return false;
        }
    }
}
