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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.TenantRangerActor;
import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.api.service.ClusterUserTenantService;
import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.enums.RangerOpType;
import com.datasophon.common.utils.ExecResult;

import com.datasophon.dao.entity.ClusterTenant;
import com.datasophon.dao.entity.ClusterUser;
import com.datasophon.dao.entity.ClusterUserTenant;
import com.datasophon.dao.mapper.ClusterUserTenantMapper;
import com.datasophon.dao.mapper.ClusterTenantMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("clusterUserTenantService")
@Transactional
public class ClusterUserTenantServiceImpl extends ServiceImpl<ClusterUserTenantMapper, ClusterUserTenant>
        implements ClusterUserTenantService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterUserTenantServiceImpl.class);

    @Autowired
    private ClusterUserService clusterUserService;

    @Autowired
    private ClusterTenantService clusterTenantService;

    @Override
    public void addUserToTenant(Integer clusterId, Integer userId, String tenantIds) {
        List<Integer> tenantIdList = StrUtil.split(tenantIds, ",").stream().map(Convert::toInt).toList();

        // SQL逻辑已迁移到DAO层
        List<ClusterUserTenant> list = getMapper().selectByClusterIdAndUserIdAndTenantIds(clusterId, userId,
                tenantIdList);
        if (CollUtil.isNotEmpty(list)) {
            throw new RuntimeException("当前用户授权已存在");
        }
        List<ClusterUserTenant> addUserTenant = tenantIdList.stream()
                .map(t -> ClusterUserTenant.builder().tenantId(t).clusterId(clusterId).userId(userId).build())
                .toList();
        this.saveOrUpdateBatch(addUserTenant);
        operateTenantUser(clusterId, userId, tenantIdList);
    }

    @Override
    public void deleteUser(Integer clusterId, Integer userId, String tenantIds) {
        List<Integer> tenantIdList = StrUtil.split(tenantIds, ",").stream().map(Convert::toInt).toList();

        // SQL逻辑已迁移到DAO层
        getMapper().deleteByClusterIdAndUserIdAndTenantIds(clusterId, userId, tenantIdList);
        operateTenantUser(clusterId, userId, tenantIdList);
    }

    @Override
    public List<ClusterUserTenant> getListByUserId(Integer clusterId, Integer userId) {
        Map<Integer, String> tenantMap = clusterTenantService.list()
                .stream()
                .collect(Collectors.toMap(ClusterTenant::getId, ClusterTenant::getTenantName));

        // SQL逻辑已迁移到DAO层
        List<ClusterUserTenant> userTenantList = getMapper().selectByClusterIdAndUserId(clusterId, userId);
        userTenantList.forEach(t -> t.setTenantName(tenantMap.get(t.getTenantId())));
        return userTenantList;
    }

    private void operateTenantUser(Integer clusterId, Integer userId, List<Integer> tenantIdList) {
        List<ClusterUserTenant> allUserTenants = this.list();
        List<ClusterUser> allUsers = clusterUserService.list();
        List<ClusterUser> users = allUsers.stream()
                .filter(t -> t.getClusterId().equals(clusterId) && t.getId().equals(userId))
                .toList();
        if (CollUtil.isEmpty(users)) {
            logger.warn("用户不存在");
            return;
        }
        // SQL逻辑已迁移到DAO层，通过ClusterTenantService调用
        List<ClusterTenant> tenantList = ((ClusterTenantMapper) clusterTenantService.getMapper())
                .selectByClusterIdAndIds(clusterId, tenantIdList);
        ActorRef tenantRangerActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");

        for (ClusterTenant clusterTenant : tenantList) {
            List<Integer> exitsUserIds = allUserTenants.stream()
                    .filter(t -> t.getClusterId().equals(clusterId) && t.getTenantId().equals(clusterTenant.getId()))
                    .map(ClusterUserTenant::getUserId)
                    .toList();
            List<String> exitsUserNames = allUsers.stream()
                    .filter(t -> exitsUserIds.contains(t.getId()))
                    .map(ClusterUser::getUsername)
                    .toList();

            TenantRangerCommand tenantRangerCommand = new TenantRangerCommand();
            tenantRangerCommand.setClusterId(clusterId);
            tenantRangerCommand.setRoleName(clusterTenant.getTenantName());
            tenantRangerCommand.setOperateType(RangerOpType.OP_USER_TO_ROLE);
            tenantRangerCommand.setUserList(exitsUserNames);
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            Future<Object> execFuture = Patterns.ask(tenantRangerActor, tenantRangerCommand, timeout);
            ExecResult execResult;
            try {
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("operate user to ranger role success");
                } else {
                    logger.error(execResult.getExecOut());
                    throw new ServiceException(500, "operate user to ranger role failed");
                }
            } catch (Exception e) {
                throw new ServiceException(500, "operate user to ranger role failed");
            }

        }
    }

}
