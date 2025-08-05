/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.service;

import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.model.StartWorkerMessage;
import com.datasophon.common.utils.ExecResult;

/**
 * 服务安装管理服务
 * 负责服务安装相关的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ServiceInstallationService {

    /**
     * 保存服务安装信息
     * 包括服务实例、配置、角色实例和WebUI信息的保存
     *
     * @param serviceRoleInfo 服务角色信息
     */
    void saveServiceInstallInfo(ServiceRoleInfo serviceRoleInfo);

    /**
     * 保存主机安装信息
     *
     * @param message     工作节点启动消息
     * @param clusterCode 集群代码
     */
    void saveHostInstallInfo(StartWorkerMessage message, String clusterCode);

    /**
     * 启动安装服务
     * 根据部署模式选择对应的安装处理链
     *
     * @param serviceRoleInfo 服务角色信息
     * @return 执行结果
     * @throws Exception 安装过程中的异常
     */
    ExecResult startInstallService(ServiceRoleInfo serviceRoleInfo) throws Exception;

    /**
     * 启动服务
     *
     * @param serviceRoleInfo 服务角色信息
     * @param needReConfig    是否需要重新配置
     * @return 执行结果
     * @throws Exception 启动过程中的异常
     */
    ExecResult startService(ServiceRoleInfo serviceRoleInfo, boolean needReConfig) throws Exception;

    /**
     * 停止服务
     *
     * @param serviceRoleInfo 服务角色信息
     * @return 执行结果
     * @throws Exception 停止过程中的异常
     */
    ExecResult stopService(ServiceRoleInfo serviceRoleInfo) throws Exception;

    /**
     * 重启服务
     *
     * @param serviceRoleInfo 服务角色信息
     * @param needReConfig    是否需要重新配置
     * @return 执行结果
     * @throws Exception 重启过程中的异常
     */
    ExecResult restartService(ServiceRoleInfo serviceRoleInfo, boolean needReConfig) throws Exception;
}