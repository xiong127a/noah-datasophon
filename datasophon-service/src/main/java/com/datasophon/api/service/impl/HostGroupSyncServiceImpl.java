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

package com.datasophon.api.service.impl;

import com.datasophon.api.service.HostGroupSyncService;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.dao.entity.ClusterHostEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 主机用户组同步服务实现
 * 从ProcessUtils迁移而来的主机组同步功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Slf4j
@Service
public class HostGroupSyncServiceImpl implements HostGroupSyncService {

    @Override
    public void syncUserGroupToHosts(List<ClusterHostEntity> hostList, String groupName, String command) {
        if (hostList == null || hostList.isEmpty()) {
            log.warn("主机列表为空，无法同步用户组 {}", groupName);
            return;
        }
        
        if (groupName == null || groupName.trim().isEmpty()) {
            log.warn("组名为空，无法执行同步操作");
            return;
        }
        
        String cmdStr = command + " " + groupName;
        log.info("开始同步用户组 {} 到 {} 个主机", groupName, hostList.size());
        
        for (ClusterHostEntity host : hostList) {
            try {
                ExecuteCmdCommand execCmdCommand = new ExecuteCmdCommand();
                execCmdCommand.setCommandLine(cmdStr);
                
                // 注意：这里需要实际的SSH执行逻辑，暂时只记录日志
                log.debug("在主机 {} 上执行命令: {}", host.getHostname(), cmdStr);
                
                // 使用WorkerHttpClient提交SSH命令执行任务
                com.datasophon.api.client.WorkerHttpClient workerHttpClient = 
                        cn.hutool.extra.spring.SpringUtil.getBean(com.datasophon.api.client.WorkerHttpClient.class);
                
                // 提交任务到Worker执行SSH命令
                String taskId = workerHttpClient.submitTask(host.getHostname(), execCmdCommand);
                log.info("已提交SSH命令执行任务到Worker: hostname={}, taskId={}", host.getHostname(), taskId);
                
            } catch (Exception e) {
                log.error("在主机 {} 上同步用户组 {} 失败: {}", host.getHostname(), groupName, e.getMessage(), e);
            }
        }
        
        log.info("用户组 {} 同步完成", groupName);
    }
}