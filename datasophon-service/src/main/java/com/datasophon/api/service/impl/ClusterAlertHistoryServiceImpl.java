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

import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.PrometheusActor;
import com.datasophon.api.master.alert.AlertActor;
import com.datasophon.api.service.ClusterAlertHistoryService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.GeneratePrometheusConfigCommand;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterAlertHistory;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterAlertHistoryMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service("clusterAlertHistoryService")
@Transactional
public class ClusterAlertHistoryServiceImpl extends ServiceImpl<ClusterAlertHistoryMapper, ClusterAlertHistory>
        implements ClusterAlertHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterAlertHistoryServiceImpl.class);


    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterInfoService clusterInfoService;


    @Override
    public void saveAlertHistory(String alertMessage) {
        logger.warn("Receive Alert Message : {}", alertMessage);
        ActorRef alertActor = ActorUtils.getLocalActor(AlertActor.class, "alertActor");
        ActorUtils.actorSystem.scheduler().scheduleOnce(FiniteDuration.apply(
                2L, TimeUnit.SECONDS),
                alertActor, alertMessage,
                ActorUtils.actorSystem.dispatcher(),
                ActorRef.noSender());
    }

    @Override
    public Result<List<ClusterAlertHistory>> getAlertList(Integer serviceInstanceId) {
        QueryWrapper query = QueryWrapper.create();
        if (serviceInstanceId != null) {
            query.where(Constants.SERVICE_INSTANCE_ID + " = " + serviceInstanceId);
        }
        query.and(Constants.IS_ENABLED + " = 1");
        List<ClusterAlertHistory> list = this.list(query);
        return Result.success(list);
    }

    @Override
    public Result<List<ClusterAlertHistory>> getAllAlertList(Integer clusterId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        QueryWrapper query = QueryWrapper.create()
                .where(Constants.CLUSTER_ID + " = " + clusterId)
                .and(Constants.IS_ENABLED + " = 1")
                .orderBy(Constants.CREATE_TIME + " desc")
                .limit(offset, pageSize);
        List<ClusterAlertHistory> list = this.list(query);

        QueryWrapper countQuery = QueryWrapper.create()
                .where(Constants.CLUSTER_ID + " = " + clusterId)
                .and(Constants.IS_ENABLED + " = 1");
        long count = this.count(countQuery);
        return Result.success(list).put(Constants.TOTAL, count);
    }

    @Override
    public void removeAlertByRoleInstanceIds(List<Integer> ids) {
        ClusterServiceRoleInstanceEntity roleInstanceEntity = roleInstanceService.getById(ids.getFirst());
        ClusterInfoEntity clusterInfoEntity = clusterInfoService.getById(roleInstanceEntity.getClusterId());
        QueryWrapper query = QueryWrapper.create()
                .where(Constants.IS_ENABLED + " = 1")
                .and(Constants.SERVICE_ROLE_INSTANCE_ID + " in ("
                        + String.join(",", ids.stream().map(String::valueOf).toList()) + ")");
        this.remove(query);
        // 重新配置prometheus
        ActorRef prometheusActor = ActorUtils.getLocalActor(PrometheusActor.class,
                ActorUtils.getActorRefName(PrometheusActor.class));
        GeneratePrometheusConfigCommand prometheusConfigCommand = new GeneratePrometheusConfigCommand();
        prometheusConfigCommand.setServiceInstanceId(roleInstanceEntity.getServiceId());
        prometheusConfigCommand.setClusterFrame(clusterInfoEntity.getClusterFrame());
        prometheusConfigCommand.setClusterId(roleInstanceEntity.getClusterId());
        prometheusActor.tell(prometheusConfigCommand, ActorRef.noSender());
    }
}
