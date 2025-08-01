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
import com.datasophon.api.service.RoleInstanceQueryService;

import com.datasophon.common.command.GeneratePrometheusConfigCommand;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterAlertHistory;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterAlertHistoryMapper;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 集群告警历史表实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Service("clusterAlertHistoryService")
@Transactional
public class ClusterAlertHistoryServiceImpl implements ClusterAlertHistoryService {

        private static final Logger logger = LoggerFactory.getLogger(ClusterAlertHistoryServiceImpl.class);

        @Autowired
        private ClusterAlertHistoryMapper clusterAlertHistoryMapper;

        @Autowired
        private RoleInstanceQueryService roleInstanceQueryService;

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
        public List<ClusterAlertHistory> getAlertList(Integer serviceInstanceId) {
                return clusterAlertHistoryMapper.selectEnabledByServiceInstanceId(serviceInstanceId);
        }

        @Override
        public PageResult<ClusterAlertHistory> getAllAlertList(Integer clusterId, Integer page, Integer pageSize) {
                return clusterAlertHistoryMapper.selectEnabledByClusterIdWithPage(clusterId, page, pageSize);
        }

        @Override
        public void removeAlertByRoleInstanceIds(List<Integer> ids) {
                ClusterServiceRoleInstanceEntity roleInstanceEntity = roleInstanceQueryService.getById(ids.get(0));
                ClusterInfoEntity clusterInfoEntity = clusterInfoService.getById(roleInstanceEntity.getClusterId());

                // 删除告警历史记录
                clusterAlertHistoryMapper.removeEnabledByRoleInstanceIds(ids);

                // 重新配置prometheus
                ActorRef prometheusActor = ActorUtils.getLocalActor(PrometheusActor.class,
                                ActorUtils.getActorRefName(PrometheusActor.class));
                GeneratePrometheusConfigCommand prometheusConfigCommand = new GeneratePrometheusConfigCommand();
                prometheusConfigCommand.setServiceInstanceId(roleInstanceEntity.getServiceId());
                prometheusConfigCommand.setClusterFrame(clusterInfoEntity.getClusterFrame());
                prometheusConfigCommand.setClusterId(roleInstanceEntity.getClusterId());
                prometheusActor.tell(prometheusConfigCommand, ActorRef.noSender());
        }

        @Override
        public long countEnabledByServiceInstanceId(Integer serviceInstanceId) {
                return clusterAlertHistoryMapper.countEnabledByServiceInstanceId(serviceInstanceId);
        }

        // 标准CRUD方法实现
        @Override
        public ClusterAlertHistory getById(Integer id) {
                return clusterAlertHistoryMapper.selectById(id);
        }

        @Override
        public ClusterAlertHistory save(ClusterAlertHistory entity) {
                clusterAlertHistoryMapper.insert(entity);
                return entity;
        }

        @Override
        public ClusterAlertHistory updateById(ClusterAlertHistory entity) {
                clusterAlertHistoryMapper.updateById(entity);
                return entity;
        }

        @Override
        public boolean removeByIds(List<Integer> ids) {
                return clusterAlertHistoryMapper.deleteByIds(ids) > 0;
        }

        @Override
        public List<ClusterAlertHistory> getAllAlertHistories() {
                return clusterAlertHistoryMapper.selectAll();
        }
}
