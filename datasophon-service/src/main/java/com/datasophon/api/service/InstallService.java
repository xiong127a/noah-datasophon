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

package com.datasophon.api.service;

import com.datasophon.common.utils.Result;

public interface InstallService {

    Result getInstallStep(Integer type);

    Result analysisHostList(Integer clusterId, String hosts, String sshUser, Integer sshPort, String sshPassword, Integer page,
                            Integer pageSize);

    Result getHostCheckStatus(Integer clusterId, String sshUser, Integer sshPort);

    Result dispatcherHostAgentList(Integer id, Integer installStateCode, Integer page, Integer clusterId);

    Result reStartDispatcherHostAgent(Integer clusterId, String hostnames);

    Result hostCheckCompleted(Integer clusterId);

    Result cancelDispatcherHostAgent(Integer clusterId, String hostname, Integer installStateCode);

    Result dispatcherHostAgentCompleted(Integer clusterId);

    Result generateHostAgentCommand(String clusterHostIds, String commandType) throws Exception;

    /**
     * 修复单个检查项
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @return 修复结果
     */
    Result fixCheckItem(Integer clusterId, String hostname, Integer itemId);

    /**
     * 修复单个检查项（支持跳过确认）
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @param skipConfirm 是否跳过确认提示
     * @return 修复结果
     */
    Result fixCheckItem(Integer clusterId, String hostname, Integer itemId, Boolean skipConfirm);

    /**
     * 修复选中的多个检查项
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemIds 检查项ID列表,逗号分隔
     * @return 修复结果
     */
    Result fixSelectedCheckItems(Integer clusterId, String hostname, String itemIds);

    /**
     * 修复主机上所有可自动修复的检查项
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @return 修复结果
     */
    Result fixAllCheckItems(Integer clusterId, String hostname);

    /**
     * 启动/停止 主机上安装的服务启动
     * @param clusterHostIds
     * @param commandType
     * @return
     * @throws Exception
     */
    Result generateHostServiceCommand(String clusterHostIds, String commandType) throws Exception;

}
