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
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterTenant;
import com.datasophon.dao.entity.ClusterUser;
import com.datasophon.dao.entity.ClusterUserTenant;
import com.datasophon.dao.mapper.ClusterUserTenantMapper;
import com.mybatisflex.core.query.QueryChain;
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
    public Result addUserToTenant(Integer clusterId, Integer userId, String tenantIds) {
        List<Integer> tenantIdList = StrUtil.split(tenantIds, ",").stream().map(Convert::toInt)
                .collect(Collectors.toList());
        List<ClusterUserTenant> list = QueryChain.of(ClusterUserTenant.class)
                .where(ClusterUserTenant::getClusterId).eq(clusterId)
                .and(ClusterUserTenant::getUserId).eq(userId)
                .and(ClusterUserTenant::getTenantId).in(tenantIdList)
                .list();
        if (CollUtil.isNotEmpty(list)) {
            return Result.error("当前用户授权已存在");
        }
        List<ClusterUserTenant> addUserTenant = tenantIdList.stream()
                .map(t -> ClusterUserTenant.builder().tenantId(t).clusterId(clusterId).userId(userId).build())
                .collect(Collectors.toList());
        this.saveOrUpdateBatch(addUserTenant);
        operateTenantUser(clusterId, userId, tenantIdList);
        return Result.success();
    }

    @Override
    public Result deleteUser(Integer clusterId, Integer userId, String tenantIds) {
        List<Integer> tenantIdList = StrUtil.split(tenantIds, ",").stream().map(Convert::toInt)
                .collect(Collectors.toList());
        QueryChain<ClusterUserTenant> query = QueryChain.of(ClusterUserTenant.class)
                .where(ClusterUserTenant::getClusterId).eq(clusterId)
                .and(ClusterUserTenant::getUserId).eq(userId)
                .and(ClusterUserTenant::getTenantId).in(tenantIdList);
        this.remove(query);
        operateTenantUser(clusterId, userId, tenantIdList);
        return Result.success();
    }

    @Override
    public Result getListByUserId(Integer clusterId, Integer userId) {
        Map<Integer, String> tenantMap = clusterTenantService.list()
                .stream()
                .collect(Collectors.toMap(ClusterTenant::getId, ClusterTenant::getTenantName));
        List<ClusterUserTenant> userTenantList = QueryChain.of(ClusterUserTenant.class)
                .where(ClusterUserTenant::getClusterId).eq(clusterId)
                .and(ClusterUserTenant::getUserId).eq(userId)
                .list();
        userTenantList.forEach(t -> t.setTenantName(tenantMap.get(t.getTenantId())));
        return Result.success(userTenantList);
    }

    private void operateTenantUser(Integer clusterId, Integer userId, List<Integer> tenantIdList) {
        List<ClusterUserTenant> allUserTenants = this.list();
        List<ClusterUser> allUsers = clusterUserService.list();
        List<ClusterUser> users = allUsers.stream()
                .filter(t -> t.getClusterId().equals(clusterId) && t.getId().equals(userId))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(users)) {
            Result.error("用户不存在");
            return;
        }
        List<ClusterTenant> tenantList = QueryChain.of(ClusterTenant.class)
                .where(ClusterTenant::getClusterId).eq(clusterId)
                .and(ClusterTenant::getId).in(tenantIdList)
                .list();
        ActorRef tenantRangerActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");

        for (ClusterTenant clusterTenant : tenantList) {
            List<Integer> exitsUserIds = allUserTenants.stream()
                    .filter(t -> t.getClusterId().equals(clusterId) && t.getTenantId().equals(clusterTenant.getId()))
                    .map(ClusterUserTenant::getUserId)
                    .toList();
            List<String> exitsUserNames = allUsers.stream()
                    .filter(t -> exitsUserIds.contains(t.getId()))
                    .map(ClusterUser::getUsername)
                    .collect(Collectors.toList());

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
