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

package com.datasophon.api.master.handler.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.service.ParcelRepositoryService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.dto.ParcelRepositoryDTO;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Objects;

public class ServiceInstallHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstallHandler.class);
    
    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        ClusterServiceRoleInstanceService roleInstanceService =
                SpringUtil.getBean(ClusterServiceRoleInstanceService.class);
        ClusterHostService clusterHostService = SpringUtil.getBean(ClusterHostService.class);
        ClusterServiceRoleInstanceDTO serviceRole = roleInstanceService.getOneServiceRole(serviceRoleInfo.getName(),
                serviceRoleInfo.getHostname(), serviceRoleInfo.getClusterId());
        ClusterHostEntity hostEntity = clusterHostService.getClusterHostByHostname(serviceRoleInfo.getHostname());
        if (Objects.nonNull(serviceRole)) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            execResult.setExecOut("already installed");
            return execResult;
        }
        InstallServiceRoleCommand installServiceRoleCommand = new InstallServiceRoleCommand();
        installServiceRoleCommand.setServiceName(serviceRoleInfo.getParentName());
        installServiceRoleCommand.setServiceRoleName(serviceRoleInfo.getName());
        installServiceRoleCommand.setServiceRoleType(serviceRoleInfo.getRoleType());
        installServiceRoleCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        installServiceRoleCommand.setRunAs(serviceRoleInfo.getRunAs());
        installServiceRoleCommand.setServiceRoleType(serviceRoleInfo.getRoleType());
        installServiceRoleCommand.setClusterId(serviceRoleInfo.getClusterId()); // 设置集群ID

        // 获取存储库服务
        ParcelRepositoryService repositoryService = SpringUtil.getBean(ParcelRepositoryService.class);
        ParcelRepositoryDTO repository = repositoryService.getClusterRepository(serviceRoleInfo.getClusterId());
        
        // 确定包名（考虑ARM架构）
        String packageName;
        String packageNameArm = serviceRoleInfo.getDecompressPackageName() + "-arm.tar.gz";
        
        // 检查是否需要使用ARM包
        if ("aarch64".equals(hostEntity.getCpuArchitecture())) {
            // 先检查远程存储库中是否有ARM包
            String armPackagePath = repository.getRepoUrl() + "/" + packageNameArm;
            boolean armPackageExists = checkRemoteFileExists(armPackagePath);
            
            if (armPackageExists) {
                packageName = packageNameArm;
                logger.info("检测到ARM架构，使用ARM包: {}", packageName);
            } else {
                packageName = serviceRoleInfo.getPackageName();
                logger.warn("ARM架构但未找到ARM包，使用通用包: {}", packageName);
            }
        } else {
            packageName = serviceRoleInfo.getPackageName();
        }
        
        installServiceRoleCommand.setPackageName(packageName);
        
        // 构建包的完整URL
        String packageUrl = repository.getRepoUrl() + "/" + packageName;
        String md5Url = packageUrl + ".md5";
        
        // 设置存储库URL，让Worker从远程下载
        installServiceRoleCommand.setRepositoryUrl(repository.getRepoUrl());
        installServiceRoleCommand.setPackageUrl(packageUrl);
        installServiceRoleCommand.setMd5Url(md5Url);
        
        // 获取MD5值
        String md5;
        try {
            // 优先从远程获取MD5
            md5 = HttpUtil.get(md5Url);
            if (md5 != null) {
                md5 = md5.trim();
            }
            logger.info("从远程存储库获取MD5: {}, md5={}", md5Url, md5);
        } catch (Exception e) {
            logger.warn("无法从远程获取MD5文件: {}, 尝试本地路径", md5Url);
            // 降级到本地路径（兼容性）
            String localMd5Path = Constants.MASTER_MANAGE_PACKAGE_PATH + Constants.SLASH + packageName + ".md5";
            if (FileUtil.exist(localMd5Path)) {
                md5 = FileUtil.readString(localMd5Path, Charset.defaultCharset());
                logger.info("从本地路径获取MD5: {}", localMd5Path);
            } else {
                throw new RuntimeException("无法获取包的MD5值，远程和本地路径均失败: " + packageName);
            }
        }
        
        installServiceRoleCommand.setPackageMd5(md5);

        // 使用HTTP方式提交任务到Worker
        ExecResult installResult = WorkerTaskHelper.submitAndWait(
                serviceRoleInfo.getHostname(), installServiceRoleCommand, 180);
        
        if (Objects.nonNull(installResult) && installResult.getExecResult()) {
            if (Objects.nonNull(getNext())) {
                return getNext().handlerRequest(serviceRoleInfo);
            }
        }
        return installResult;
    }
    
    /**
     * 检查远程文件是否存在
     * 
     * @param url 文件URL
     * @return 文件是否存在
     */
    private boolean checkRemoteFileExists(String url) {
        try {
            int responseCode = HttpUtil.createGet(url).execute().getStatus();
            return responseCode == 200;
        } catch (Exception e) {
            logger.debug("检查远程文件失败: {}, error: {}", url, e.getMessage());
            return false;
        }
    }
}
