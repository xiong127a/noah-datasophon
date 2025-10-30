/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.kubernetes.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.command.KubernetesGenerateHostTagCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.api.utils.ClusterInfoUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Kubernetes主机标签处理器
 * 负责为Kubernetes环境下的服务角色生成主机标签
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-05
 */
public class KubernetesHostTagHandler extends ServiceHandler {

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) {
        KubernetesGenerateHostTagCommand kubernetesGenerateHostTagCommand = new KubernetesGenerateHostTagCommand();
        kubernetesGenerateHostTagCommand.setHostName(serviceRoleInfo.getHostname());
        kubernetesGenerateHostTagCommand.setServiceName(serviceRoleInfo.getParentName());
        kubernetesGenerateHostTagCommand.setServiceRoleName(serviceRoleInfo.getName());
        kubernetesGenerateHostTagCommand.setCommandType(serviceRoleInfo.getCommandType());
        Long clusterId = serviceRoleInfo.getClusterId();
        ClusterInfoService clusterInfoService =
                SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(clusterId);
        kubernetesGenerateHostTagCommand.setClusterId(clusterId);
        kubernetesGenerateHostTagCommand.setKubeConfig(kubeConfig);
        String namespace = ClusterInfoUtils.getKubernetesNamespace(clusterId);
        kubernetesGenerateHostTagCommand.setNamespace(namespace);
        
        // 直接调用KubernetesTagHostHandler处理，无需通过Actor
        try {
            logger.info("start add service tag {}", kubernetesGenerateHostTagCommand.getServiceRoleName());
            
            com.datasophon.kubernetes.actor.handler.KubernetesTagHostHandler serviceHandler = 
                    new com.datasophon.kubernetes.actor.handler.KubernetesTagHostHandler(
                            kubernetesGenerateHostTagCommand.getNamespace(),
                            kubernetesGenerateHostTagCommand.getServiceName(), 
                            kubernetesGenerateHostTagCommand.getServiceRoleName());
            
            ExecResult configResult = serviceHandler.operateTag(
                    kubernetesGenerateHostTagCommand.getClusterId(),
                    kubernetesGenerateHostTagCommand.getHostName(),
                    kubernetesGenerateHostTagCommand.getKubeConfig(),
                    kubernetesGenerateHostTagCommand.getCommandType());
            
            logger.info("{} tag at host {}: {}",
                    kubernetesGenerateHostTagCommand.getServiceRoleName(),
                    kubernetesGenerateHostTagCommand.getHostName(),
                    configResult.getExecResult() ? "success" : "failed");
            
            if (Objects.nonNull(configResult) && configResult.getExecResult()) {
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(serviceRoleInfo);
                }
            }
            return configResult;
        } catch (Exception e) {
            logger.error("主机打标签失败", e);
            return new ExecResult();
        }
    }
}
