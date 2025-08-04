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

import com.datasophon.api.converter.ClusterAlertHistoryConverter;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.PrometheusActor;
import com.datasophon.api.master.alert.AlertActor;
import com.datasophon.api.service.ClusterAlertHistoryService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.RoleInstanceQueryService;
import com.datasophon.common.command.GeneratePrometheusConfigCommand;
import com.datasophon.common.dto.ClusterAlertHistoryDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterAlertHistory;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterAlertHistoryMapper;
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
 * 集群告警历史服务实现类
 * 提供集群告警历史的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterAlertHistoryService")
@Transactional
public class ClusterAlertHistoryServiceImpl extends ServiceImpl<ClusterAlertHistoryMapper, ClusterAlertHistory>
                implements ClusterAlertHistoryService {

        private static final Logger logger = LoggerFactory.getLogger(ClusterAlertHistoryServiceImpl.class);

        @Autowired
        private ClusterAlertHistoryConverter clusterAlertHistoryConverter;

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
        public List<ClusterAlertHistoryDTO> getAlertList(Integer serviceInstanceId) {
                try {
                        // DAO层：使用Mapper查询
                        List<ClusterAlertHistory> entities = getMapper()
                                        .selectEnabledByServiceInstanceId(serviceInstanceId);
                        // Service层：Entity → DTO转换
                        return entities.stream()
                                        .map(clusterAlertHistoryConverter::entityToDto)
                                        .toList();
                } catch (Exception e) {
                        logger.error("根据服务实例ID查询告警历史失败: {}", e.getMessage(), e);
                        throw new RuntimeException("查询告警历史失败: " + e.getMessage());
                }
        }

        @Override
        public PageResult<ClusterAlertHistoryDTO> getAllAlertList(Integer clusterId, Integer page, Integer pageSize) {
                try {
                        // DAO层：使用Mapper分页查询
                        PageResult<ClusterAlertHistory> entityPageResult = getMapper()
                                        .selectEnabledByClusterIdWithPage(clusterId, page, pageSize);

                        // Service层：Entity → DTO转换
                        List<ClusterAlertHistoryDTO> dtoList = entityPageResult.getRecords().stream()
                                        .map(clusterAlertHistoryConverter::entityToDto)
                                        .toList();

                        return PageResult.of(
                                        dtoList,
                                        entityPageResult.getTotal(),
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

                        // DAO层：直接删除符合条件的记录
                        getMapper().removeEnabledByRoleInstanceIds(ids);

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

        @Override
        public ClusterAlertHistoryDTO getByIdAsDto(Integer id) {
                // Service层：Entity → DTO转换
                ClusterAlertHistory entity = this.getById(id);
                return clusterAlertHistoryConverter.entityToDto(entity);
        }

        @Override
        public void saveAlertHistoryDto(ClusterAlertHistoryDTO dto) {
                // Service层：DTO → Entity转换
                ClusterAlertHistory entity = clusterAlertHistoryConverter.dtoToEntity(dto);
                this.save(entity);
        }

        @Override
        public void updateAlertHistory(ClusterAlertHistoryDTO dto) {
                // Service层：DTO → Entity转换
                ClusterAlertHistory entity = clusterAlertHistoryConverter.dtoToEntity(dto);
                this.updateById(entity);
        }

        @Override
        public long countEnabledByServiceInstanceId(Integer serviceInstanceId) {
                try {
                        return getMapper().countEnabledByServiceInstanceId(serviceInstanceId);
                } catch (Exception e) {
                        logger.error("统计启用告警数量失败: {}", e.getMessage(), e);
                        return 0;
                }
        }

        @Override
        public List<ClusterAlertHistoryDTO> getStoppedRolesByServiceId(Integer serviceInstanceId) {
                try {
                        // 这里假设我们有一个mapper方法来查询停止状态的角色
                        // 实际实现时需要根据具体的数据模型来调整
                        List<ClusterAlertHistory> entities = getMapper()
                                        .selectStoppedRolesByServiceId(serviceInstanceId);
                        return entities.stream()
                                        .map(clusterAlertHistoryConverter::entityToDto)
                                        .toList();
                } catch (Exception e) {
                        logger.error("查询停止状态角色失败: {}", e.getMessage(), e);
                        return java.util.Collections.emptyList();
                }
        }

        @Override
        public List<ClusterAlertHistoryDTO> getAlarmRolesByServiceId(Integer serviceInstanceId) {
                try {
                        // 这里假设我们有一个mapper方法来查询告警状态的角色
                        // 实际实现时需要根据具体的数据模型来调整
                        List<ClusterAlertHistory> entities = getMapper().selectAlarmRolesByServiceId(serviceInstanceId);
                        return entities.stream()
                                        .map(clusterAlertHistoryConverter::entityToDto)
                                        .toList();
                } catch (Exception e) {
                        logger.error("查询告警状态角色失败: {}", e.getMessage(), e);
                        return java.util.Collections.emptyList();
                }
        }
}