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

    Result analysisHostList(Integer clusterId, String ips, String sshUser, Integer sshPort, String sshPassword,String kubeConfigContent,
            Integer page,
            Integer pageSize);

    Result getHostCheckStatus(Integer clusterId, String sshUser, Integer sshPort);

    Result dispatcherHostAgentList(Integer id, Integer installStateCode, Integer page, Integer clusterId);

    Result reStartDispatcherHostAgent(Integer clusterId, String ips);

    Result hostCheckCompleted(Integer clusterId);

    /**
     * 清理主机检查资源
     * 在hostCheckCompleted返回成功且hostCheckCompleted为true后调用
     * 用于释放与检查任务和修复任务相关的资源
     * 
     * @param clusterId 集群ID
     * @return 清理结果
     */
    Result cleanupHostCheckResources(Integer clusterId);

    /**
     * 清理主机环境校验缓存
     * 
     * @return 清理结果
     */
    Result clearHostEnvCheckCache();

    Result cancelDispatcherHostAgent(Integer clusterId, String ip, Integer installStateCode);

    Result dispatcherHostAgentCompleted(Integer clusterId);

    Result generateHostAgentCommand(String clusterHostIds, String commandType) throws Exception;

    /**
     * 启动/停止 主机上安装的服务启动
     *
     */
    Result generateHostServiceCommand(String clusterHostIds, String commandType) throws Exception;

    /**
     * 获取主机最近日志
     * 
     * @param ip        主机IP
     * @param clusterId 集群ID
     * @return 主机最近日志内容
     */
    Result getWorkerLog(String ip, Integer clusterId);

}
