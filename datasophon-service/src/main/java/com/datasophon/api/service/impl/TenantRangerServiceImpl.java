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

import com.datasophon.api.service.TenantRangerService;
import com.datasophon.api.utils.ranger.client.RangerClient;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.strategy.AbstractRangerStrategy;
import com.datasophon.api.utils.ranger.strategy.RangerStrategyFactory;
import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.enums.RangerOpType;
import com.datasophon.common.model.tenant.resource.TenantResource;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static com.datasophon.api.utils.ranger.client.RangerUtil.getRangerClient;

/**
 * 租户Ranger服务实现
 * 替代原TenantRangerActor，使用Spring Service实现
 */
@Service
public class TenantRangerServiceImpl implements TenantRangerService {

    private static final Logger logger = LoggerFactory.getLogger(TenantRangerServiceImpl.class);

    private static final List<String> SUPPORT_SERVICE = Arrays.asList("HDFS", "HIVE", "HBASE", "YARN");

    @Override
    public ExecResult handleTenantRangerCommand(TenantRangerCommand rangerCommand) {
        try {
            ExecResult execResult;
            RangerOpType operateType = rangerCommand.getOperateType();
            
            if (operateType == RangerOpType.CREATE_SERVICE) {
                execResult = createRangerService(rangerCommand.getClusterId(), rangerCommand.getServiceName());
            } else if (operateType == RangerOpType.OP_USER_TO_ROLE) {
                execResult = addRoleUser(rangerCommand);
            } else if (operateType == RangerOpType.DELETE_TENANT) {
                execResult = deleteRangerPolicy(rangerCommand.getTenantName(), rangerCommand.getClusterId());
                deleteRangerRole(rangerCommand.getTenantName(), rangerCommand.getClusterId());
            } else {
                execResult = new ExecResult();
                execResult.setExecResult(false);
                execResult.setExecErrOut("Unsupported operation type: " + operateType);
            }
            return execResult;
        } catch (Exception e) {
            logger.error("Error handling TenantRangerCommand", e);
            ExecResult errorResult = new ExecResult();
            errorResult.setExecResult(false);
            errorResult.setExecErrOut(e.getMessage());
            return errorResult;
        }
    }

    @Override
    public ExecResult handleTenantResource(TenantResource resource) {
        try {
            return operateRangerPolicy(resource);
        } catch (Exception e) {
            logger.error("Error handling TenantResource", e);
            ExecResult errorResult = new ExecResult();
            errorResult.setExecResult(false);
            errorResult.setExecErrOut(e.getMessage());
            return errorResult;
        }
    }

    private ExecResult addRoleUser(TenantRangerCommand rangerCommand) {
        ExecResult execResult = new ExecResult();
        RangerClient rangerClient;
        try {
            rangerClient = getRangerClient(rangerCommand.getClusterId());
            RangerUtil.setRoleUser(rangerClient, rangerCommand.getRoleName(), rangerCommand.getUserList());
            execResult.setExecResult(true);
            return execResult;
        } catch (Exception e) {
            logger.error("add ranger role user failed");
            logger.error(e.getMessage());
            return execResult;
        }
    }

    private ExecResult createRangerService(Long clusterId, String serviceName) throws Exception {
        RangerClient rangerClient = getRangerClient(clusterId);
        RangerUtil.createSuperRole(rangerClient);
        AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy(serviceName, clusterId);
        return rangerStrategy.createService();
    }

    private ExecResult operateRangerPolicy(TenantResource resource) throws Exception {
        ExecResult execResult = new ExecResult();
        
        // 处理各种资源类型
        if (resource.getHdfsResourceList() != null && !resource.getHdfsResourceList().isEmpty()) {
            AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy("HDFS", resource.getClusterId());
            rangerStrategy.operatePolicy(resource);
        }
        
        if (resource.getYarnResourceList() != null && !resource.getYarnResourceList().isEmpty()) {
            AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy("YARN", resource.getClusterId());
            rangerStrategy.operatePolicy(resource);
        }
        
        if (resource.getHiveResourceList() != null && !resource.getHiveResourceList().isEmpty()) {
            AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy("HIVE", resource.getClusterId());
            rangerStrategy.operatePolicy(resource);
        }
        
        if (resource.getHbaseResourceList() != null && !resource.getHbaseResourceList().isEmpty()) {
            AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy("HBASE", resource.getClusterId());
            rangerStrategy.operatePolicy(resource);
        }
        
        execResult.setExecResult(true);
        return execResult;
    }

    private ExecResult deleteRangerPolicy(String roleName, Long clusterId) throws Exception {
        ExecResult execResult = new ExecResult();
        for (String service : SUPPORT_SERVICE) {
            AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy(service, clusterId);
            rangerStrategy.deletePolicy(roleName);
        }
        execResult.setExecResult(true);
        return execResult;
    }

    private void deleteRangerRole(String roleName, Long clusterId) throws Exception {
        RangerClient rangerClient = getRangerClient(clusterId);
        try {
            rangerClient.getRoles().deleteRoleByName(roleName);
            logger.info("成功删除Ranger角色: {}", roleName);
        } catch (Exception e) {
            logger.error("删除Ranger角色失败: {}", roleName, e);
            throw e;
        }
    }
}

