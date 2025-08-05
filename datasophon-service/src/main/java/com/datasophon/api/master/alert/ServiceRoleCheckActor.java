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

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ServiceRoleCheckCommand;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import org.apache.pekko.actor.AbstractActor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 服务角色状态检查Actor
 * 负责定期检查指定组件的状态和处理相关告警
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-05
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
            ClusterServiceRoleInstanceService roleInstanceService = SpringUtil.getBean(ClusterServiceRoleInstanceService.class);
            ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
            
            // 查询需要状态检查的服务实例
            List<ClusterServiceRoleInstanceDTO> list = roleInstanceService.getServiceRolesByNames(Constants.STATUS_CHECK_SERVICES);

            // 获取所有集群信息
            Map<Integer, String> allClusterIdAndType = clusterInfoService.getAllClusterIdAndType();

            if (!list.isEmpty()) {
                Map<String, ClusterServiceRoleInstanceDTO> map = translateListToMap(list);

                for (ClusterServiceRoleInstanceDTO roleInstanceDto : list) {
                    ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext
                            .getServiceRoleHandler(roleInstanceDto.serviceRoleName());

                    // 服务集群部署模式
                    String depType = allClusterIdAndType.get(roleInstanceDto.clusterId());
                    switch (depType) {
                        case Constants.PVM_MODE:
                            Optional.ofNullable(serviceRoleHandler)
                                    .ifPresent(handler -> handler.handlerServiceRoleCheck(roleInstanceDto, map));
                            break;
                        case Constants.KUBERNETES_MODE:
                            handlerKubernetesServiceRoleCheck(roleInstanceDto);
                            Optional.ofNullable(serviceRoleHandler).ifPresent(
                                    handler -> handler.handlerKubernetesServiceRoleCheck(roleInstanceDto, map));
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

    private void handlerKubernetesServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto) {
        KubernetesServiceRoleStatusService kubernetesServiceRoleStatusService = new KubernetesServiceRoleStatusService();
        kubernetesServiceRoleStatusService.checkStatusAndOpAlert(roleInstanceDto);
    }

    private Map<String, ClusterServiceRoleInstanceDTO> translateListToMap(
            List<ClusterServiceRoleInstanceDTO> list) {
        return list.stream()
                .collect(
                        Collectors.toMap(
                                e -> e.hostname() + e.serviceRoleName(),
                                e -> e,
                                (v1, v2) -> v1));
    }
}
