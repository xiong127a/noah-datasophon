/*
 *
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
 *
 */

package com.datasophon.api.master.alert;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ServiceRoleCheckCommand;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 检查指定组件状态
 */
public class ServiceRoleCheckActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ServiceRoleCheckCommand.class, this::handleServiceRoleCheckCommand)
                .matchAny(this::unhandled)
                .build();
    }

    private void handleServiceRoleCheckCommand(ServiceRoleCheckCommand msg) {
        try {
            ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                    .getBean(ClusterServiceRoleInstanceService.class);

            // 查询服务实例
            List<ClusterServiceRoleInstanceEntity> list = roleInstanceService.list(
                    new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                            .in(Constants.SERVICE_ROLE_NAME, Constants.STATUS_CHECK_SERVICES));

            // 集群类型map
            Map<Integer, String> allClusterIdAndType = ProcessUtils.getAllClusterIdAndType();

            if (!list.isEmpty()) {
                Map<String, ClusterServiceRoleInstanceEntity> map = translateListToMap(list);

                for (ClusterServiceRoleInstanceEntity roleInstanceEntity : list) {
                    ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext
                            .getServiceRoleHandler(roleInstanceEntity.getServiceRoleName());

                    // 服务集群部署模式
                    String depType = allClusterIdAndType.get(roleInstanceEntity.getClusterId());
                    switch (depType) {
                        case Constants.PVM_MODE:
                            Optional.ofNullable(serviceRoleHandler)
                                    .ifPresent(handler -> handler.handlerServiceRoleCheck(roleInstanceEntity, map));
                            break;
                        case Constants.KUBERNETES_MODE:
                            handlerKubernetesServiceRoleCheck(roleInstanceEntity, map);
                            Optional.ofNullable(serviceRoleHandler).ifPresent(
                                    handler -> handler.handlerKubernetesServiceRoleCheck(roleInstanceEntity, map));
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("处理ServiceRoleCheckCommand消息时出错", e);
        }
    }

    private void handlerKubernetesServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity,
            Map<String, ClusterServiceRoleInstanceEntity> map) {
        KubernetesServiceRoleStatusService kubernetesServiceRoleStatusService = new KubernetesServiceRoleStatusService();
        kubernetesServiceRoleStatusService.checkStatusAndOpAlert(roleInstanceEntity);
    }

    private Map<String, ClusterServiceRoleInstanceEntity> translateListToMap(
            List<ClusterServiceRoleInstanceEntity> list) {
        return list.stream()
                .collect(
                        Collectors.toMap(
                                e -> e.getHostname() + e.getServiceRoleName(),
                                e -> e,
                                (v1, v2) -> v1));
    }
}
