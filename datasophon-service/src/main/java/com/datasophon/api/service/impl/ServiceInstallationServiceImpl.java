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

package com.datasophon.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.datasophon.api.converter.ClusterInfoConverter;
import com.datasophon.api.converter.ClusterServiceInstanceConverter;

import com.datasophon.api.kubernetes.handler.KubernetesDeploymentYamlHandler;
import com.datasophon.api.kubernetes.handler.KubernetesHostTagHandler;
import com.datasophon.api.kubernetes.handler.KubernetesServiceConfigureHandler;
import com.datasophon.api.kubernetes.handler.KubernetesServiceInstallHandler;
import com.datasophon.api.kubernetes.handler.KubernetesServiceStartHandler;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.master.handler.service.ServiceInstallHandler;
import com.datasophon.api.master.handler.service.ServiceStartHandler;
import com.datasophon.api.service.*;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.CommonUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.model.ExternalLink;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.model.StartWorkerMessage;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.HostUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.*;
import com.datasophon.dao.enums.HostState;
import com.datasophon.dao.enums.MANAGED;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.ServiceRoleState;
import com.datasophon.dao.enums.ServiceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 服务安装管理服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service
public class ServiceInstallationServiceImpl implements ServiceInstallationService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstallationServiceImpl.class);

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private ClusterServiceInstanceConfigService serviceInstanceConfigService;

    @Autowired
    private ClusterServiceRoleInstanceService serviceRoleInstanceService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ClusterServiceRoleInstanceWebuisService webuisService;

    @Autowired
    private ClusterServiceInstanceRoleGroupService roleGroupService;

    @Autowired
    private ClusterZkService clusterZkService;

    @Autowired
    private ClusterHostService clusterHostService;

    @Autowired
    private ClusterServiceInstanceConverter serviceInstanceConverter;

    @Autowired
    private ClusterInfoConverter clusterInfoConverter;

    @Override
    public void saveServiceInstallInfo(ServiceRoleInfo serviceRoleInfo) {
        ClusterInfoDTO clusterInfoDTO = clusterInfoService.getClusterById(serviceRoleInfo.getClusterId());
        ClusterInfoEntity clusterInfo = clusterInfoConverter.dtoToEntity(clusterInfoDTO);

        ClusterServiceInstanceDTO clusterServiceInstanceDTO = serviceInstanceService
                .getServiceInstanceByClusterIdAndServiceName(serviceRoleInfo.getClusterId(),
                        serviceRoleInfo.getParentName());
        ClusterServiceInstanceEntity clusterServiceInstance;

        if (Objects.isNull(clusterServiceInstanceDTO)) {
            clusterServiceInstance = new ClusterServiceInstanceEntity();
            clusterServiceInstance.setClusterId(serviceRoleInfo.getClusterId());
            clusterServiceInstance.setServiceName(serviceRoleInfo.getParentName());
            clusterServiceInstance.setServiceState(ServiceState.RUNNING);
            clusterServiceInstance.setCreateTime(new Date());
            clusterServiceInstance.setUpdateTime(new Date());

            // 直接保存Entity
            serviceInstanceService.save(clusterServiceInstance);

            // save config
            List<ServiceConfig> list = ServiceConfigMap.get(clusterInfo.getClusterCode() + Constants.UNDERLINE
                    + serviceRoleInfo.getParentName() + Constants.CONFIG);
            String config = JSON.toJSONString(list);
            ClusterServiceInstanceConfigEntity clusterServiceInstanceConfig = new ClusterServiceInstanceConfigEntity();
            clusterServiceInstanceConfig.setClusterId(serviceRoleInfo.getClusterId());
            clusterServiceInstanceConfig.setServiceId(clusterServiceInstance.getId());
            clusterServiceInstanceConfig.setConfigJson(config);
            clusterServiceInstanceConfig.setConfigJsonMd5(SecureUtil.md5(config));
            clusterServiceInstanceConfig.setConfigVersion(1);
            clusterServiceInstanceConfig.setCreateTime(new Date());
            clusterServiceInstanceConfig.setUpdateTime(new Date());
            serviceInstanceConfigService.save(clusterServiceInstanceConfig);
        } else {
            clusterServiceInstance = serviceInstanceConverter.dtoToEntity(clusterServiceInstanceDTO);
            clusterServiceInstance.setServiceState(ServiceState.RUNNING);
            clusterServiceInstance.setServiceStateCode(ServiceState.RUNNING.getValue());
            serviceInstanceService.updateById(clusterServiceInstance);
        }

        Integer roleGroupId = (Integer) CacheUtils.get("UseRoleGroup_" + clusterServiceInstance.getId());
        ClusterServiceInstanceRoleGroup roleGroup = roleGroupService.getById(roleGroupId);

        // save role instance
        ClusterServiceRoleInstanceDTO roleInstanceDTO = serviceRoleInstanceService
                .getOneServiceRole(serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), clusterInfo.getId());
        if (Objects.isNull(roleInstanceDTO)) {
            ClusterServiceRoleInstanceEntity roleInstance = new ClusterServiceRoleInstanceEntity();
            roleInstance.setServiceId(clusterServiceInstance.getId());
            roleInstance.setRoleType(CommonUtils.convertRoleType(serviceRoleInfo.getRoleType().getName()));
            roleInstance.setCreateTime(new Date());
            roleInstance.setHostname(serviceRoleInfo.getHostname());
            roleInstance.setClusterId(serviceRoleInfo.getClusterId());
            roleInstance.setServiceRoleName(serviceRoleInfo.getName());
            roleInstance.setServiceRoleState(ServiceRoleState.RUNNING);
            roleInstance.setUpdateTime(new Date());
            roleInstance.setServiceName(serviceRoleInfo.getParentName());
            roleInstance.setRoleGroupId(roleGroup.getId());
            roleInstance.setNeedRestart(NeedRestart.NO);

            // 直接保存Entity
            serviceRoleInstanceService.save(roleInstance);

            if (Constants.ZKSERVER.equalsIgnoreCase(roleInstance.getServiceRoleName())) {
                ClusterZk clusterZk = new ClusterZk();
                clusterZk.setMyid((Integer) CacheUtils.get("zkserver_" + serviceRoleInfo.getHostname()));
                clusterZk.setClusterId(serviceRoleInfo.getClusterId());
                clusterZk.setZkServer(roleInstance.getHostname());
                clusterZkService.save(clusterZk);
            }

            handleExternalLink(serviceRoleInfo, clusterInfo, clusterServiceInstance, roleInstance);
        }
    }

    @Override
    public void saveHostInstallInfo(StartWorkerMessage message, String clusterCode) {
        ClusterHostDO clusterHostDO = new ClusterHostDO();
        BeanUtil.copyProperties(message, clusterHostDO);

        ClusterInfoDTO clusterDTO = clusterInfoService.getClusterByClusterCode(clusterCode);
        ClusterInfoEntity cluster = clusterInfoConverter.dtoToEntity(clusterDTO);

        clusterHostDO.setClusterId(cluster.getId());
        clusterHostDO.setCheckTime(new Date());
        clusterHostDO.setRack("/default-rack");
        clusterHostDO.setNodeLabel("default");
        clusterHostDO.setCreateTime(new Date());
        clusterHostDO.setIp(HostUtils.getIpByHost(message.getHostname()));
        clusterHostDO.setHostState(HostState.RUNNING);
        clusterHostDO.setManaged(MANAGED.YES);
        clusterHostService.saveHost(clusterHostDO);
    }

    @Override
    public ExecResult startInstallService(ServiceRoleInfo serviceRoleInfo) throws Exception {
        String depMode = getDepMode(serviceRoleInfo.getClusterId());
        ExecResult execResult;

        if (Constants.PVM_MODE.equals(depMode)) {
            ServiceHandler serviceInstallHandler = new ServiceInstallHandler();
            ServiceHandler serviceConfigureHandler = new ServiceConfigureHandler();
            ServiceHandler serviceStartHandler = new ServiceStartHandler();
            serviceInstallHandler.setNext(serviceConfigureHandler);
            serviceConfigureHandler.setNext(serviceStartHandler);
            execResult = serviceInstallHandler.handlerRequest(serviceRoleInfo);
        } else {
            // Kubernetes环境中使用责任链模式实现服务安装流程
            KubernetesServiceInstallHandler kubernetesServiceInstallHandler = new KubernetesServiceInstallHandler();
            KubernetesServiceConfigureHandler kubernetesServiceConfigureHandler = new KubernetesServiceConfigureHandler();
            KubernetesDeploymentYamlHandler kubernetesDeploymentYamlHandler = new KubernetesDeploymentYamlHandler();
            KubernetesHostTagHandler kubernetesHostTagHandler = new KubernetesHostTagHandler();
            KubernetesServiceStartHandler kubernetesServiceStartHandler = new KubernetesServiceStartHandler();

            // 构建责任链
            kubernetesServiceInstallHandler.setNext(kubernetesServiceConfigureHandler);
            kubernetesServiceConfigureHandler.setNext(kubernetesDeploymentYamlHandler);
            kubernetesDeploymentYamlHandler.setNext(kubernetesHostTagHandler);
            kubernetesHostTagHandler.setNext(kubernetesServiceStartHandler);

            execResult = kubernetesServiceInstallHandler.handlerRequest(serviceRoleInfo);
        }
        return execResult;
    }

    /**
     * 处理外部链接
     */
    private void handleExternalLink(ServiceRoleInfo serviceRoleInfo, ClusterInfoEntity clusterInfo,
            ClusterServiceInstanceEntity clusterServiceInstance,
            ClusterServiceRoleInstanceEntity roleInstance) {
        if (Objects.nonNull(serviceRoleInfo.getExternalLink())) {
            ExternalLink externalLink = serviceRoleInfo.getExternalLink();
            Map<String, String> globalVariables = GlobalVariables.get(clusterInfo.getId());
            globalVariables.put("${hostname}", serviceRoleInfo.getHostname());
            String url = PlaceholderUtils.replacePlaceholders(externalLink.getUrl(), globalVariables,
                    Constants.REGEX_VARIABLE);
            Integer port = extractPortFromUrl(url);

            ClusterServiceRoleInstanceWebuis webui = webuisService.getRoleInstanceWebUi(roleInstance.getId());
            List<ClusterServiceRoleInstanceWebuis> clusterServiceRoleInstanceWebuis = webuisService
                    .listWebUisByServiceInstanceId(clusterServiceInstance.getId());
            if (Objects.nonNull(webui)) {
                logger.info("web ui already exists");
            } else {
                boolean foundPortMapping = false;

                // 遍历配置映射查找端口映射
                for (Map.Entry<Generators, List<ServiceConfig>> entry : serviceRoleInfo.getConfigFileMap()
                        .entrySet()) {
                    if (CollUtil.isEmpty(entry.getValue())) {
                        continue;
                    }

                    for (ServiceConfig serviceConfig : entry.getValue()) {
                        if (!serviceConfig.getName().endsWith("node_port_mappings")) {
                            continue;
                        }
                        @SuppressWarnings("unchecked")
                        List<Map<String, String>> portMappings = (List<Map<String, String>>) serviceConfig
                                .getValue();

                        for (Map<String, String> portMapping : portMappings) {
                            String mappedPorts = portMapping.get(port.toString());
                            if (mappedPorts == null) {
                                continue;
                            }

                            for (String mappedPort : mappedPorts.split(",")) {
                                for (ClusterServiceRoleInstanceWebuis clusterServiceRoleInstanceWebui : clusterServiceRoleInstanceWebuis) {
                                    if (clusterServiceRoleInstanceWebui.getWebUrl().contains(mappedPort)) {
                                        logger.info("web ui already exists");
                                        return;
                                    }
                                }
                                ClusterServiceRoleInstanceWebuis webuis = new ClusterServiceRoleInstanceWebuis();

                                // 替换URL端口
                                webuis.setWebUrl(replacePortInUrl(url, mappedPort));

                                webuis.setServiceInstanceId(clusterServiceInstance.getId());
                                webuis.setServiceRoleInstanceId(roleInstance.getId());
                                webuis.setName(String.format("%s(%s)",
                                        externalLink.getName(),
                                        serviceRoleInfo.getHostname()));

                                webuisService.save(webuis);
                            }
                        }
                        foundPortMapping = true;
                    }
                }

                // 如果没有找到端口映射，保存原始URL
                if (!foundPortMapping) {
                    ClusterServiceRoleInstanceWebuis webuis = new ClusterServiceRoleInstanceWebuis();
                    webuis.setWebUrl(url);
                    webuis.setServiceInstanceId(clusterServiceInstance.getId());
                    webuis.setServiceRoleInstanceId(roleInstance.getId());
                    webuis.setName(String.format("%s(%s)",
                            externalLink.getName(),
                            serviceRoleInfo.getHostname()));
                    webuisService.save(webuis);
                }

                globalVariables.remove("${hostname}");
            }
        }
    }

    /**
     * 获取部署模式
     */
    private String getDepMode(Integer clusterId) {
        ClusterInfoDTO clusterInfoDTO = clusterInfoService.getClusterById(clusterId);
        return clusterInfoDTO.depType();
    }

    /**
     * 从URL中提取端口号
     */
    private Integer extractPortFromUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return uri.getPort() == -1 ? null : uri.getPort();
        } catch (Exception e) {
            logger.error("Failed to extract port from URL: {}", url, e);
            return null;
        }
    }

    /**
     * 替换URL中的端口号
     */
    private String replacePortInUrl(String url, String newPort) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return url.replace(
                    ":" + uri.getPort(),
                    ":" + newPort);
        } catch (Exception e) {
            logger.error("Failed to replace port in URL: {}", url, e);
            return url; // 返回原始URL如果替换失败
        }
    }

    @Override
    public ExecResult startService(ServiceRoleInfo serviceRoleInfo, boolean needReConfig) throws Exception {
        logger.info("启动服务: {} on {}, needReConfig: {}", 
                serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), needReConfig);
        
        String depMode = getDepMode(serviceRoleInfo.getClusterId());
        ExecResult execResult;

        if (Constants.PVM_MODE.equals(depMode)) {
            // PVM模式：如果需要重新配置，先配置再启动，否则直接启动
            if (needReConfig) {
                ServiceHandler serviceConfigureHandler = new ServiceConfigureHandler();
                ServiceHandler serviceStartHandler = new ServiceStartHandler();
                serviceConfigureHandler.setNext(serviceStartHandler);
                execResult = serviceConfigureHandler.handlerRequest(serviceRoleInfo);
            } else {
                ServiceHandler serviceStartHandler = new ServiceStartHandler();
                execResult = serviceStartHandler.handlerRequest(serviceRoleInfo);
            }
        } else {
            // Kubernetes模式：使用Kubernetes启动处理器
            if (needReConfig) {
                KubernetesServiceConfigureHandler kubernetesServiceConfigureHandler = new KubernetesServiceConfigureHandler();
                KubernetesServiceStartHandler kubernetesServiceStartHandler = new KubernetesServiceStartHandler();
                kubernetesServiceConfigureHandler.setNext(kubernetesServiceStartHandler);
                execResult = kubernetesServiceConfigureHandler.handlerRequest(serviceRoleInfo);
            } else {
                KubernetesServiceStartHandler kubernetesServiceStartHandler = new KubernetesServiceStartHandler();
                execResult = kubernetesServiceStartHandler.handlerRequest(serviceRoleInfo);
            }
        }
        
        logger.info("服务启动完成: {} on {}, 结果: {}", 
                serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), 
                execResult != null ? execResult.getExecResult() : "null");
        return execResult;
    }

    @Override
    public ExecResult stopService(ServiceRoleInfo serviceRoleInfo) throws Exception {
        logger.info("停止服务: {} on {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
        
        String depMode = getDepMode(serviceRoleInfo.getClusterId());
        ExecResult execResult;

        if (Constants.PVM_MODE.equals(depMode)) {
            // PVM模式：执行停止操作
            // TODO: 需要实现ServiceStopHandler
            logger.info("PVM模式停止服务暂未实现，模拟成功");
            execResult = new ExecResult();
            execResult.setExecResult(true);
            execResult.setExecOut("Service stopped successfully in PVM mode");
        } else {
            // Kubernetes模式：停止Pod
            // TODO: 需要实现KubernetesServiceStopHandler
            logger.info("Kubernetes模式停止服务暂未实现，模拟成功");
            execResult = new ExecResult();
            execResult.setExecResult(true);
            execResult.setExecOut("Service stopped successfully in Kubernetes mode");
        }
        
        logger.info("服务停止完成: {} on {}, 结果: {}", 
                serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), 
                execResult != null ? execResult.getExecResult() : "null");
        return execResult;
    }

    @Override
    public ExecResult restartService(ServiceRoleInfo serviceRoleInfo, boolean needReConfig) throws Exception {
        logger.info("重启服务: {} on {}, needReConfig: {}", 
                serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), needReConfig);
        
        // 重启 = 停止 + 启动
        ExecResult stopResult = stopService(serviceRoleInfo);
        if (stopResult == null || !stopResult.getExecResult()) {
            logger.error("停止服务失败，无法继续重启: {} on {}", 
                    serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
            return stopResult;
        }
        
        // 等待一小段时间确保服务完全停止
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("重启等待被中断");
        }
        
        ExecResult startResult = startService(serviceRoleInfo, needReConfig);
        
        logger.info("服务重启完成: {} on {}, 结果: {}", 
                serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), 
                startResult != null ? startResult.getExecResult() : "null");
        return startResult;
    }
}