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

import com.datasophon.common.dto.HostCheckStatusDto;
import com.datasophon.common.dto.InstallStepDTO;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.InstallStepEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 安装服务接口
 * 继承IService提供基础CRUD操作，返回DTO进行数据传输
 * 按照架构重构规范，Service层不返回Result，抛出业务异常
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface InstallService extends IService<InstallStepEntity> {

    /**
     * 根据安装类型获取安装步骤列表
     * 
     * @param installType 安装类型
     * @return 安装步骤DTO列表
     */
    List<InstallStepDTO> getInstallStepsByType(Integer installType);


    /**
     * 根据安装类型获取安装步骤
     * 
     * @param type 安装类型
     * @return 安装步骤DTO
     */
    InstallStepDTO getInstallStep(Integer type);

    /**
     * 获取主机检查状态
     * 
     * @param clusterId 集群ID
     * @param sshUser   SSH用户
     * @param sshPort   SSH端口
     * @return 主机检查状态
     */
    HostCheckStatusDto getHostCheckStatus(Long clusterId, String sshUser, Integer sshPort);

    /**
     * 获取主机代理分发列表
     * 
     * @param clusterId        集群ID
     * @param installStateCode 安装状态码
     * @param page             页码
     * @param pageSize         每页大小
     * @return 主机代理分发列表分页结果
     */
    PageResult<HostInfo> dispatcherHostAgentList(Long clusterId, Integer installStateCode, Integer page,
            Integer pageSize);

    /**
     * 重启主机代理分发
     * 
     * @param clusterId 集群ID
     * @param ips       主机IP列表
     * @return 操作是否成功
     */
    boolean reStartDispatcherHostAgent(Long clusterId, String ips);
    /**
     * 检查主机代理分发是否完成
     * 
     * @param clusterId 集群ID
     * @return 是否完成
     */
    boolean dispatcherHostAgentCompleted(Long clusterId);

    /**
     * 生成主机代理命令
     * 
     * @param clusterHostIds 集群主机ID列表
     * @param commandType    命令类型
     * @return 生成的命令列表
     * @throws Exception 操作异常
     */
    List<Map<String, Object>> generateHostAgentCommand(String clusterHostIds, String commandType) throws Exception;

    /**
     * 生成主机服务命令
     * 
     * @param clusterHostIds 集群主机ID列表
     * @param commandType    命令类型
     * @return 生成的命令列表
     */
    List<Map<String, Object>> generateHostServiceCommand(String clusterHostIds, String commandType);

    /**
     * 获取主机最近日志
     * 
     * @param ip        主机IP
     * @param clusterId 集群ID
     * @return 主机最近日志内容
     */
    String getWorkerLog(String ip, Long clusterId);

}
