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
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
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

/**
 * 集群告警历史服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Service("clusterAlertHistoryService")
@Transactional
public class ClusterAlertHistoryServiceImpl extends ServiceImpl<ClusterAlertHistoryMapper, ClusterAlertHistory>
                implements ClusterAlertHistoryService {

        private static final Logger logger = LoggerFactory.getLogger(ClusterAlertHistoryServiceImpl.class);

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
                try {
                        return QueryChain.of(ClusterAlertHistory.class)
                                        .where(ClusterAlertHistory::getServiceInstanceId).eq(serviceInstanceId)
                                        .and(ClusterAlertHistory::getIsEnabled).eq(1) // 启用的告警
                                        .orderBy(ClusterAlertHistory::getCreateTime, false)
                                        .list();
                } catch (Exception e) {
                        logger.error("根据服务实例ID查询告警历史失败: {}", e.getMessage(), e);
                        throw new RuntimeException("查询告警历史失败: " + e.getMessage());
                }
        }

        @Override
        public PageResult<ClusterAlertHistory> getAllAlertList(Integer clusterId, Integer page, Integer pageSize) {
                try {
                        Page<ClusterAlertHistory> result = QueryChain.of(ClusterAlertHistory.class)
                                        .where(ClusterAlertHistory::getClusterId).eq(clusterId)
                                        .and(ClusterAlertHistory::getIsEnabled).eq(1) // 启用的告警
                                        .orderBy(ClusterAlertHistory::getCreateTime, false)
                                        .page(Page.of(page, pageSize));

                        return PageResult.of(
                                        result.getRecords(),
                                        result.getTotalRow(),
                                        page,
                                        pageSize);
                } catch (Exception e) {
                        logger.error("分页查询告警历史失败: {}", e.getMessage(), e);
                        throw new RuntimeException("查询告警历史失败: " + e.getMessage());
                }
        }

        @Override
        public void removeAlertByRoleInstanceIds(List<Integer> ids) {
                try {
                        if (ids == null || ids.isEmpty()) {
                                return;
                        }

                        ClusterServiceRoleInstanceEntity roleInstanceEntity = roleInstanceQueryService
                                        .getById(ids.getFirst());
                        ClusterInfoEntity clusterInfoEntity = clusterInfoService
                                        .getById(roleInstanceEntity.getClusterId());

                        // 删除告警历史记录 - 查询符合条件的记录并删除
                        List<ClusterAlertHistory> entitiesToDelete = QueryChain.of(ClusterAlertHistory.class)
                                        .where(ClusterAlertHistory::getServiceRoleInstanceId).in(ids)
                                        .list();

                        if (!entitiesToDelete.isEmpty()) {
                                List<Integer> idsToDelete = entitiesToDelete.stream()
                                                .map(ClusterAlertHistory::getId)
                                                .toList();
                                this.removeByIds(idsToDelete);
                        }

                        // 重新配置prometheus
                        ActorRef prometheusActor = ActorUtils.getLocalActor(PrometheusActor.class,
                                        ActorUtils.getActorRefName(PrometheusActor.class));
                        GeneratePrometheusConfigCommand prometheusConfigCommand = new GeneratePrometheusConfigCommand();
                        prometheusConfigCommand.setServiceInstanceId(roleInstanceEntity.getServiceId());
                        prometheusConfigCommand.setClusterFrame(clusterInfoEntity.getClusterFrame());
                        prometheusConfigCommand.setClusterId(roleInstanceEntity.getClusterId());
                        prometheusActor.tell(prometheusConfigCommand, ActorRef.noSender());

                        logger.info("删除角色实例相关告警历史成功，角色实例ID: {}", ids);
                } catch (Exception e) {
                        logger.error("删除角色实例相关告警历史失败: {}", e.getMessage(), e);
                        throw new RuntimeException("删除角色实例相关告警历史失败: " + e.getMessage());
                }
        }
}