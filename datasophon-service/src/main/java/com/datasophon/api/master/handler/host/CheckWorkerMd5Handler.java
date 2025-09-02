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

import cn.hutool.core.io.FileUtil;
import com.datasophon.api.utils.CommonUtils;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.utils.SshPluginHelper;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.HostInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

public class CheckWorkerMd5Handler implements DispatcherWorkerHandler {

    private static final Logger logger = LoggerFactory.getLogger(CheckWorkerMd5Handler.class);
    @Override
    public boolean handle(HostInfo hostInfo) {
        try {
            logger.info("【MD5检查处理器】开始MD5校验: {}", hostInfo.getIp());
            
            // 使用SSH插件辅助工具
            
            // 通过SSH插件辅助工具执行MD5检查命令
            String checkWorkerMd5Result = SshPluginHelper.executeCommand(hostInfo, Constants.CHECK_WORKER_MD5_CMD).trim();
            
            // 读取本地MD5文件
            String md5 = FileUtil.readString(
                    Constants.MASTER_MANAGE_PACKAGE_PATH +
                            Constants.SLASH +
                            Constants.WORKER_PACKAGE_NAME + ".md5",
                    Charset.defaultCharset()).trim();
            
            logger.info("【MD5检查处理器】{} worker package md5 value is : {}", hostInfo.getIp(), md5);
            logger.debug("【MD5检查处理器】远程MD5: {}, 本地MD5: {}", checkWorkerMd5Result, md5);
            
            if (!md5.equals(checkWorkerMd5Result)) {
                logger.error("【MD5检查处理器】worker package md5 check failed");
                hostInfo.setErrMsg("worker package md5 check failed");
                hostInfo.setMessage(MessageResolverUtils.getMessage("md5.check.failed"));
                CommonUtils.updateInstallState(InstallState.FAILED, hostInfo);
                return false;
            }
            
            hostInfo.setProgress(35);
            hostInfo.setMessage(
                    MessageResolverUtils.getMessage("md5.verification.successful.and.installation.package.decompressed"));
            
            logger.info("【MD5检查处理器】MD5校验成功: {}", hostInfo.getIp());
            return true;
            
        } catch (Exception e) {
            logger.error("【MD5检查处理器】处理异常: {} -> {}", hostInfo.getIp(), e.getMessage(), e);
            hostInfo.setErrorMessage("MD5检查异常: " + e.getMessage());
            return false;
        }
    }
}
