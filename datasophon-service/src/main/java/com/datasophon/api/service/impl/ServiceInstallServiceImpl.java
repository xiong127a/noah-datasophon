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

package com.datasophon.api.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.enums.Status;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.load.ServiceInfoMap;
import com.datasophon.api.load.ServiceRoleMap;
import com.datasophon.api.service.*;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.CommonUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.*;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.*;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.ServiceState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.datasophon.api.utils.CacheOperateUtils.putRemoteServiceConfigMap;
import static com.datasophon.api.utils.ProcessUtils.getDepMode;
import static com.datasophon.common.Constants.*;

@Service("serviceInstallService")
@Transactional
public class ServiceInstallServiceImpl implements ServiceInstallService {

    public static final String PROMETHEUS = "prometheus";
    public static final String ALERTMANAGER = "ALERTMANAGER";
    private static final Logger logger = LoggerFactory.getLogger(ServiceInstallServiceImpl.class);
    private static final List<String> MUST_AT_SAME_NODE_BASIC_SERVICE = Arrays.asList("Grafana", "AlertManager",
            "Prometheus");
    @Autowired
    FrameInfoService frameInfoService;
    @Autowired
    FrameServiceService frameService;
    @Autowired
    FrameServiceRoleService frameServiceRole;
    @Autowired
    ClusterServiceCommandService commandService;
    @Autowired
    private ClusterInfoService clusterInfoService;
    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;
    @Autowired
    private ClusterServiceInstanceConfigService serviceInstanceConfigService;
    @Autowired
    private ClusterServiceCommandHostCommandService hostCommandService;
    @Autowired
    private ClusterVariableService variableService;
    @Autowired
    private ClusterHostService hostService;
    @Autowired
    private ClusterServiceInstanceRoleGroupService roleGroupService;
    @Autowired
    private ClusterServiceRoleGroupConfigService groupConfigService;
    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;
    @Autowired
    private ConfigVersionInfoService configVersionInfoService;

    /**
     * 处理配置列表，根据集群模式修改配置项的hidden和required属性
     *
     * @param list      配置列表
     * @param clusterId 集群ID
     */
    public static void processConfigList(List<ServiceConfig> list, Integer clusterId) {
        if (Constants.K8S_MODE.equals(getDepMode(clusterId))) {
            for (ServiceConfig config : list) {
                if (Constants.K8S_MODE.toLowerCase().equals(config.getConfigType())) {
                    config.setHidden(false);
                    config.setRequired(true);
                }
            }
        }
    }

    @Override
    public Result getServiceConfigOption(Integer clusterId, String serviceName) {
        List<ServiceConfig> list = null;
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        Map<String, String> globalVariables = GlobalVariables.get(clusterId);

        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService
                .getServiceInstanceByClusterIdAndServiceName(
                        clusterId, serviceName);
        if (Objects.nonNull(serviceInstance)) {
            list = listServiceConfigByServiceInstance(serviceInstance);
        } else {
            FrameServiceEntity frameService = this.frameService.getServiceByFrameCodeAndServiceName(
                    clusterInfo.getClusterFrame(), serviceName);
            String serviceConfig = frameService.getServiceConfig();
            serviceConfig = PlaceholderUtils.replacePlaceholders(
                    serviceConfig, globalVariables, Constants.REGEX_VARIABLE);

            list = JSONArray.parseArray(serviceConfig, ServiceConfig.class);

            processConfigList(list, clusterId);
        }

        ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(serviceName);
        if (Objects.nonNull(serviceRoleHandler)) {
            serviceRoleHandler.getConfig(clusterId, list);
        }
        Map<String, List<ServiceConfig>> roleToConfigMap = CommonUtils.groupByConfigTargetRoleOrCommon(serviceName, list);
        return Result.success(roleToConfigMap);
    }

    @Override
    public Result saveServiceConfig(
            Integer clusterId, String serviceName, List<ServiceConfig> list,
            Integer roleGroupId, String description) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        ServiceConfigMap.put(clusterInfo.getClusterCode() + UNDERLINE + serviceName + CONFIG,
                list);
        putRemoteServiceConfigMap(
                clusterInfo.getClusterCode() + UNDERLINE + serviceName + CONFIG,
                list);
        HashMap<String, ServiceConfig> map = new HashMap<>();
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        // handler config
        ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(serviceName);
        if (Objects.nonNull(serviceRoleHandler)) {
            serviceRoleHandler.handlerConfig(clusterId, list);
        }
        // add variable
        FrameServiceEntity frameServiceEntity = frameService.getServiceByFrameCodeAndServiceName(
                clusterInfo.getClusterFrame(), serviceName);

        for (ServiceConfig serviceConfig : list) {
            String configName = serviceConfig.getName();
            String variableName = "${" + configName + "}";
            String variableValue = String.valueOf(serviceConfig.getValue());
            // add to global variable
            if (INPUT.equals(serviceConfig.getType())) {
                addToGlobalVariable(clusterId, variableName, variableValue);
            }
            globalVariables.put(variableName, variableValue);
            map.put(serviceConfig.getName(), serviceConfig);
        }
        // update config-file
        HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
        buildConfigFileMap(serviceName, clusterInfo, map, configFileMap);
        if (PROMETHEUS.equalsIgnoreCase(serviceName)) {
            logger.info("add worker and node to prometheus");
            // add host node to prometheus
            addHostNodeToPrometheus(clusterId, configFileMap);
        }

        ClusterServiceInstanceEntity serviceInstanceEntity = serviceInstanceService
                .getServiceInstanceByClusterIdAndServiceName(
                        clusterId, serviceName);
        if (Objects.isNull(serviceInstanceEntity)) {
            serviceInstanceEntity = saveServiceInstance(clusterId, serviceName, frameServiceEntity);
            ClusterServiceInstanceRoleGroup clusterServiceInstanceRoleGroup = saveServiceInstanceRoleGroup(clusterId,
                    serviceName, serviceInstanceEntity);
            saveServiceRoleGroupConfig(
                    clusterId, serviceName, list, configFileMap, clusterServiceInstanceRoleGroup, description);
            CacheUtils.put(
                    "UseRoleGroup_" + serviceInstanceEntity.getId(),
                    clusterServiceInstanceRoleGroup.getId());
        } else {
            Set<String> configUpdateRoleSet = new HashSet<>();
            configNeedUpdate(serviceInstanceEntity, list, configUpdateRoleSet);
            ClusterServiceRoleGroupConfig roleGroupConfig;
            if (Objects.isNull(roleGroupId)) {
                ClusterServiceInstanceRoleGroup roleGroup = roleGroupService.getRoleGroupByServiceInstanceId(
                        serviceInstanceEntity.getId());
                roleGroupConfig = groupConfigService.getConfigByRoleGroupId(roleGroup.getId());
            } else {
                roleGroupConfig = groupConfigService.getConfigByRoleGroupId(roleGroupId);
            }
            CacheUtils.put(
                    "UseRoleGroup_" + serviceInstanceEntity.getId(),
                    roleGroupConfig.getRoleGroupId());
            if (configUpdateRoleSet.size() > 0) {
                ClusterServiceRoleGroupConfig newRoleGroupConfig = new ClusterServiceRoleGroupConfig();
                if (Objects.isNull(roleGroupId)) {
                    ClusterServiceInstanceRoleGroup roleGroup = saveNewRoleGroup(serviceInstanceEntity);
                    newRoleGroupConfig.setConfigVersion(1);
                    newRoleGroupConfig.setRoleGroupId(roleGroup.getId());
                    CacheUtils.put(
                            "UseRoleGroup_" + serviceInstanceEntity.getId(), roleGroup.getId());
                } else {
                    newRoleGroupConfig.setConfigVersion(roleGroupConfig.getConfigVersion() + 1);
                    newRoleGroupConfig.setRoleGroupId(roleGroupConfig.getRoleGroupId());
                    roleGroupService.updateToNeedRestart(roleGroupId);

                    boolean hasCommonConfig = configUpdateRoleSet.contains(GENERAL);
                    if (hasCommonConfig) {
                        // 通用配置更新，重启整个角色组
                        roleInstanceService.updateToNeedRestart(roleGroupId);
                        serviceInstanceEntity.setNeedRestart(NeedRestart.YES);
                    } else {
                        // 仅更新特定服务角色
                        for (String serviceRoleName : configUpdateRoleSet) {
                            roleInstanceService.updateToNeedRestart(roleGroupId, serviceRoleName);
                        }
                    }
                }
                newRoleGroupConfig.setClusterId(clusterId);
                newRoleGroupConfig.setCreateTime(new Date());
                newRoleGroupConfig.setUpdateTime(new Date());
                newRoleGroupConfig.setServiceName(serviceInstanceEntity.getServiceName());
                buildConfig(list, configFileMap, newRoleGroupConfig, description);
                groupConfigService.save(newRoleGroupConfig);
            }
            // update service instance
            serviceInstanceEntity.setUpdateTime(new Date());
            serviceInstanceEntity.setLabel(frameServiceEntity.getLabel());
            serviceInstanceService.updateById(serviceInstanceEntity);
        }
        return Result.success();
    }

    private void buildConfigFileMapAlertManager(String serviceName, ClusterInfoEntity clusterInfo,
                                                HashMap<String, ServiceConfig> map, HashMap<Generators, List<ServiceConfig>> configFileMap) {

    }

    @Override
    public Result saveServiceRoleHostMapping(Integer clusterId, List<ServiceRoleHostMapping> list) {

        checkOnSameNode(clusterId, list);

        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String hostMapKey = clusterInfo.getClusterCode()
                + UNDERLINE
                + SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> map = new HashMap<>();
        if (CacheOperateUtils.containsKey(hostMapKey)) {
            map = CacheOperateUtils.getWithType(hostMapKey,
                    new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, List<String>>>() {
                    });
        }

        for (ServiceRoleHostMapping serviceRoleHostMapping : list) {
            serviceValidation(serviceRoleHostMapping);

            map.put(serviceRoleHostMapping.getServiceRole(), serviceRoleHostMapping.getHosts());

            ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(
                    serviceRoleHostMapping.getServiceRole());
            if (Objects.nonNull(serviceRoleHandler)) {
                serviceRoleHandler.handler(clusterId, serviceRoleHostMapping.getHosts());
            }
        }

        CacheUtils.put(
                clusterInfo.getClusterCode()
                        + UNDERLINE
                        + SERVICE_ROLE_HOST_MAPPING,
                map);
        return Result.success();
    }

    @Override
    public Result saveHostServiceRoleMapping(Integer clusterId, List<HostServiceRoleMapping> list) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        HashMap<String, List<String>> map = new HashMap<>();
        for (HostServiceRoleMapping hostServiceRoleMapping : list) {
            map.put(hostServiceRoleMapping.getHost(), hostServiceRoleMapping.getServiceRoles());
        }
        CacheUtils.put(
                clusterInfo.getClusterCode()
                        + UNDERLINE
                        + HOST_SERVICE_ROLE_MAPPING,
                map);
        return Result.success();
    }

    @Override
    public Result getServiceRoleDeployOverview(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        HashMap<String, List<String>> map = CacheOperateUtils.getWithType(
                clusterInfo.getClusterCode()
                        + UNDERLINE
                        + SERVICE_ROLE_HOST_MAPPING,
                new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, List<String>>>() {
                });
        return Result.success(map);
    }

    /**
     * @param clusterId
     * @param commandIds
     * @return
     */
    @Override
    public Result startInstallService(Integer clusterId, List<String> commandIds) {
        Collection<ClusterServiceCommandEntity> commands = commandService.listByIds(commandIds);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        DAG<String, ServiceNode, ServiceNodeEdge> dag = new DAG<>();
        for (ClusterServiceCommandEntity command : commands) {
            List<ClusterServiceCommandHostCommandEntity> commandHostList = hostCommandService
                    .getHostCommandListByCommandId(command.getCommandId());
            List<ServiceRoleInfo> masterRoles = new ArrayList<>();
            List<ServiceRoleInfo> elseRoles = new ArrayList<>();
            ServiceNode serviceNode = new ServiceNode();
            String serviceKey = clusterInfo.getClusterFrame() + UNDERLINE + command.getServiceName();
            ServiceInfo serviceInfo = ServiceInfoMap.get(serviceKey);
            for (ClusterServiceCommandHostCommandEntity hostCommand : commandHostList) {
                String key = clusterInfo.getClusterFrame()
                        + UNDERLINE
                        + command.getServiceName()
                        + UNDERLINE
                        + hostCommand.getServiceRoleName();
                ServiceRoleInfo serviceRoleInfo = ServiceRoleMap.get(key);
                serviceRoleInfo.setHostname(hostCommand.getHostname());
                serviceRoleInfo.setHostCommandId(hostCommand.getHostCommandId());
                serviceRoleInfo.setClusterId(clusterId);
                serviceRoleInfo.setParentName(command.getServiceName());
                if (MASTER.equals(serviceRoleInfo.getRoleType().getName())) {
                    masterRoles.add(serviceRoleInfo);
                } else {
                    elseRoles.add(serviceRoleInfo);
                }
            }
            serviceNode.setMasterRoles(masterRoles);
            serviceNode.setElseRoles(elseRoles);
            dag.addNode(command.getServiceName(), serviceNode);
            if (CollUtil.isNotEmpty(serviceInfo.getDependencies())) {
                for (String dependency : serviceInfo.getDependencies()) {
                    dag.addEdge(dependency, command.getServiceName());
                }
            }
        }
        return Result.success();
    }

    @Override
    public void downloadPackage(String packageName, HttpServletResponse response) throws IOException {
        File file = new File(MASTER_MANAGE_PACKAGE_PATH + SLASH + packageName);

        response.reset();
        response.setContentType("application/octet-stream");
        // 支持中文名称文件,需要对header进行单独设置，不然下载的文件名会出现乱码或者无法显示的情况
        // 设置响应头，控制浏览器下载该文件
        response.addHeader("Content-Length", "" + file.length());
        response.setHeader("Content-Disposition", "attachment;filename=" + packageName);

        try (FileInputStream inputStream = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
        }
    }

    @Override
    public Result getServiceRoleHostMapping(Integer clusterId) {
        return null;
    }

    @Override
    public Result checkServiceDependency(Integer clusterId, String serviceIds) {
        // TODO 解除注释
        // //
        // List<ClusterServiceInstanceEntity> serviceInstanceList =
        // serviceInstanceService.listRunningServiceInstance(clusterId);
        // Map<String, ClusterServiceInstanceEntity> instanceMap =
        // serviceInstanceList.stream()
        // .collect(
        // Collectors.toMap(
        // ClusterServiceInstanceEntity::getServiceName,
        // e -> e,
        // (v1, v2) -> v1));
        //
        // List<FrameServiceEntity> list = frameService.listServices(serviceIds);
        // Map<String, FrameServiceEntity> serviceMap =
        // list.stream()
        // .collect(
        // Collectors.toMap(
        // FrameServiceEntity::getServiceName,
        // e -> e,
        // (v1, v2) -> v1));
        // if (!instanceMap.containsKey("ALERTMANAGER") &&
        // !serviceMap.containsKey("ALERTMANAGER")) {
        // return Result.error(
        // "service install depends on alertmanager ,please make sure you have selected
        // it or that alertmanager is normal and running");
        // }
        // if (!instanceMap.containsKey("GRAFANA") &&
        // !serviceMap.containsKey("GRAFANA")) {
        // return Result.error(
        // "service install depends on grafana ,please make sure you have selected it or
        // that grafana is normal and running");
        // }
        // if (!instanceMap.containsKey("PROMETHEUS") &&
        // !serviceMap.containsKey("PROMETHEUS")) {
        // return Result.error(
        // "service install depends on prometheus ,please make sure you have selected it
        // or that prometheus is normal and running");
        // }
        //
        // for (FrameServiceEntity frameServiceEntity : list) {
        // for (String dependService : frameServiceEntity.getDependencies().split(","))
        // {
        // if (StringUtils.isNotBlank(dependService)
        // && !instanceMap.containsKey(dependService)
        // && !serviceMap.containsKey(dependService)) {
        // return Result.error(
        // ""
        // + frameServiceEntity.getServiceName()
        // + " install depends on "
        // + dependService
        // + ",please make sure that you have selected it or that "
        // + dependService
        // + " is normal and running");
        // }
        // }
        // }
        return Result.success();
    }

    private ClusterServiceInstanceRoleGroup saveNewRoleGroup(
            ClusterServiceInstanceEntity serviceInstanceEntity) {
        long count = roleGroupService.count(
                new QueryWrapper<ClusterServiceInstanceRoleGroup>()
                        .eq(ROLE_GROUP_TYPE, "auto")
                        .eq(SERVICE_INSTANCE_ID, serviceInstanceEntity.getId()));
        ClusterServiceInstanceRoleGroup roleGroup = new ClusterServiceInstanceRoleGroup();
        long num = count + 1;
        roleGroup.setRoleGroupName("RoleGroup" + num);
        roleGroup.setServiceInstanceId(serviceInstanceEntity.getId());
        roleGroup.setServiceName(serviceInstanceEntity.getServiceName());
        roleGroup.setClusterId(serviceInstanceEntity.getClusterId());
        roleGroup.setRoleGroupType("auto");
        roleGroupService.save(roleGroup);
        return roleGroup;
    }

    private void configNeedUpdate(
            ClusterServiceInstanceEntity serviceInstanceEntity, List<ServiceConfig> list,
            Set<String> configUpdateRoleSet) {
        List<ServiceConfig> originalConfigs = listServiceConfigByServiceInstance(serviceInstanceEntity);
        Map<String, Object> originalConfigMap = originalConfigs.stream()
                .collect(
                        Collectors.toMap(
                                ServiceConfig::getName,
                                ServiceConfig::getValue,
                                (v1, v2) -> v1));
        for (ServiceConfig serviceConfig : list) {
            String configName = serviceConfig.getName();
            String variableValue = String.valueOf(serviceConfig.getValue());
            if (originalConfigMap.containsKey(configName)) {
                String configValue = String.valueOf(originalConfigMap.get(configName));
                if (!variableValue.equals(configValue)) {
                    configUpdateRoleSet.add(serviceConfig.getConfigTargetRoles());
                }
            } else {
                configUpdateRoleSet.add(serviceConfig.getConfigTargetRoles());
            }
        }
    }

    private void saveServiceRoleGroupConfig(
            Integer clusterId,
            String serviceName,
            List<ServiceConfig> list,
            HashMap<Generators, List<ServiceConfig>> configFileMap,
            ClusterServiceInstanceRoleGroup clusterServiceInstanceRoleGroup,
            String description) {
        ClusterServiceRoleGroupConfig roleGroupConfig = new ClusterServiceRoleGroupConfig();
        roleGroupConfig.setRoleGroupId(clusterServiceInstanceRoleGroup.getId());
        roleGroupConfig.setClusterId(clusterId);
        roleGroupConfig.setCreateTime(new Date());
        roleGroupConfig.setUpdateTime(new Date());
        roleGroupConfig.setServiceName(serviceName);
        buildConfig(list, configFileMap, roleGroupConfig, description);
        roleGroupConfig.setConfigVersion(1);
        groupConfigService.save(roleGroupConfig);
    }

    private ClusterServiceInstanceRoleGroup saveServiceInstanceRoleGroup(
            Integer clusterId,
            String serviceName,
            ClusterServiceInstanceEntity serviceInstanceEntity) {
        ClusterServiceInstanceRoleGroup clusterServiceInstanceRoleGroup = new ClusterServiceInstanceRoleGroup();
        clusterServiceInstanceRoleGroup.setServiceInstanceId(serviceInstanceEntity.getId());
        clusterServiceInstanceRoleGroup.setClusterId(clusterId);
        clusterServiceInstanceRoleGroup.setRoleGroupName("默认角色组");
        clusterServiceInstanceRoleGroup.setServiceName(serviceName);
        clusterServiceInstanceRoleGroup.setRoleGroupType("default");
        roleGroupService.save(clusterServiceInstanceRoleGroup);
        return clusterServiceInstanceRoleGroup;
    }

    private ClusterServiceInstanceEntity saveServiceInstance(
            Integer clusterId, String serviceName,
            FrameServiceEntity frameServiceEntity) {
        ClusterServiceInstanceEntity serviceInstanceEntity;
        serviceInstanceEntity = new ClusterServiceInstanceEntity();
        serviceInstanceEntity.setClusterId(clusterId);
        serviceInstanceEntity.setServiceState(ServiceState.WAIT_INSTALL);
        serviceInstanceEntity.setServiceName(serviceName);
        serviceInstanceEntity.setLabel(frameServiceEntity.getLabel());
        serviceInstanceEntity.setCreateTime(new Date());
        serviceInstanceEntity.setUpdateTime(new Date());
        serviceInstanceEntity.setNeedRestart(NeedRestart.NO);
        serviceInstanceEntity.setFrameServiceId(frameServiceEntity.getId());
        serviceInstanceEntity.setSortNum(frameServiceEntity.getSortNum());
        serviceInstanceService.save(serviceInstanceEntity);
        return serviceInstanceEntity;
    }

    private void addHostNodeToPrometheus(
            Integer clusterId, HashMap<Generators, List<ServiceConfig>> configFileMap) {
        List<ClusterHostDO> hostList = hostService.list(
                new QueryWrapper<ClusterHostDO>()
                        .eq(MANAGED, 1)
                        .eq(CLUSTER_ID, clusterId));
        Generators workerGenerators = new Generators();
        workerGenerators.setFilename("worker.json");
        workerGenerators.setOutputDirectory("configs");
        workerGenerators.setConfigFormat("custom");
        workerGenerators.setTemplateName("scrape.ftl");

        Generators nodeGenerators = new Generators();
        nodeGenerators.setFilename("linux.json");
        nodeGenerators.setOutputDirectory("configs");
        nodeGenerators.setConfigFormat("custom");
        nodeGenerators.setTemplateName("scrape.ftl");
        ArrayList<ServiceConfig> workerServiceConfigs = new ArrayList<>();
        ArrayList<ServiceConfig> nodeServiceConfigs = new ArrayList<>();
        for (ClusterHostDO clusterHostDO : hostList) {
            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setName("worker_" + clusterHostDO.getHostname());
            serviceConfig.setValue(clusterHostDO.getHostname() + ":8585");
            serviceConfig.setRequired(true);
            workerServiceConfigs.add(serviceConfig);

            ServiceConfig nodeServiceConfig = new ServiceConfig();
            nodeServiceConfig.setName("node_" + clusterHostDO.getHostname());
            nodeServiceConfig.setValue(clusterHostDO.getHostname() + ":9100");
            nodeServiceConfig.setRequired(true);
            nodeServiceConfigs.add(nodeServiceConfig);
        }
        configFileMap.put(workerGenerators, workerServiceConfigs);
        configFileMap.put(nodeGenerators, nodeServiceConfigs);
    }

    private void buildConfigFileMap(
            String serviceName,
            ClusterInfoEntity clusterInfo,
            Map<String, ServiceConfig> existingConfigMap,
            Map<Generators, List<ServiceConfig>> resultConfigMap) {

        final FrameServiceEntity frameService = this.frameService.getServiceByFrameCodeAndServiceName(
                clusterInfo.getClusterFrame(), serviceName);

        if (frameService == null || StringUtils.isBlank(frameService.getConfigFileJson())) {
            return;
        }

        try {
            final Map<Generators, List<ServiceConfig>> parsedConfigMap = parseConfigJson(
                    frameService.getConfigFileJson());
            processConfigEntries(serviceName, existingConfigMap, resultConfigMap, parsedConfigMap);
        } catch (JSONException e) {
            logger.error("Failed to parse config JSON for service: {}", serviceName, e);
        }
    }

    /**
     * 解析JSON配置为结构化Map
     */
    private Map<Generators, List<ServiceConfig>> parseConfigJson(String configJson) {
        return JSON.parseObject(configJson,
                new TypeReference<Map<Generators, List<ServiceConfig>>>() {
                });
    }

    /**
     * 处理配置条目
     */
    private void processConfigEntries(
            String serviceName,
            Map<String, ServiceConfig> existingConfigMap,
            Map<Generators, List<ServiceConfig>> resultConfigMap,
            Map<Generators, List<ServiceConfig>> parsedConfigMap) {

        for (Map.Entry<Generators, List<ServiceConfig>> entry : parsedConfigMap.entrySet()) {
            final Generators generator = entry.getKey();
            final List<ServiceConfig> configs = new ArrayList<>(entry.getValue());
            updateConfigsWithExistingValues(existingConfigMap, configs);

            if (isAlertManagerService(serviceName)) {
                resultConfigMap.put(generator, new ArrayList<>(existingConfigMap.values()));
            } else {
                resultConfigMap.put(generator, configs);
            }
        }
    }

    /**
     * 用已有配置更新当前配置
     */
    private void updateConfigsWithExistingValues(
            Map<String, ServiceConfig> existingConfigMap,
            List<ServiceConfig> currentConfigs) {

        for (ServiceConfig config : currentConfigs) {
            final String configName = config.getName();
            if (logger.isDebugEnabled()) {
                logger.debug("Processing config: {}", configName);
            }

            if (existingConfigMap.containsKey(configName)) {
                final ServiceConfig existingConfig = existingConfigMap.get(configName);
                config.setValue(existingConfig.getValue());
                config.setHidden(existingConfig.isHidden());
                config.setRequired(existingConfig.isRequired());
            }
        }
    }

    private boolean isAlertManagerService(String serviceName) {
        return "ALERTMANAGER".equalsIgnoreCase(serviceName);
    }

    private void addToGlobalVariable(Integer clusterId, String variableName, String value) {
        ClusterVariable clusterVariable = variableService.getVariableByVariableName(variableName, clusterId);
        if (Objects.nonNull(clusterVariable)) {
            if (!value.equals(clusterVariable.getVariableValue())) {
                clusterVariable.setVariableValue(value);
                variableService.updateById(clusterVariable);
            }
        } else {
            clusterVariable = new ClusterVariable();
            clusterVariable.setClusterId(clusterId);
            clusterVariable.setVariableName(variableName);
            clusterVariable.setVariableValue(value);
            variableService.save(clusterVariable);
        }
    }

    private void buildConfig(
            List<ServiceConfig> list,
            HashMap<Generators, List<ServiceConfig>> configFileMap,
            ClusterServiceRoleGroupConfig roleGroupConfig,
            String description) {
        roleGroupConfig.setConfigJson(JSONArray.toJSONString(list));
        roleGroupConfig.setConfigJsonMd5(SecureUtil.md5(JSONArray.toJSONString(list)));
        roleGroupConfig.setConfigFileJson(JSON.toJSONString(configFileMap));
        roleGroupConfig.setConfigFileJsonMd5(SecureUtil.md5(JSON.toJSONString(configFileMap)));

        // 同步保存配置版本详情
        saveConfigVersionInfo(roleGroupConfig, description);
    }

    /**
     * 保存配置版本详情信息
     *
     * @param roleGroupConfig 角色组配置
     * @param description     版本描述
     */
    private void saveConfigVersionInfo(ClusterServiceRoleGroupConfig roleGroupConfig, String description) {
        ConfigVersionInfoEntity versionInfo = new ConfigVersionInfoEntity();
        versionInfo.setVersion(roleGroupConfig.getConfigVersion());
        versionInfo.setRefType("ROLE_GROUP");
        versionInfo.setRefId(roleGroupConfig.getRoleGroupId());
        versionInfo.setDescription(description); // 设置版本描述信息
        versionInfo.setEditTime(new Date());
        versionInfo.setIsCurrent(true);
        versionInfo.setServiceCode(roleGroupConfig.getServiceName());

        // 先将所有版本设为非当前版本
        configVersionInfoService.updateCurrentVersion(
                roleGroupConfig.getConfigVersion(),
                "ROLE_GROUP",
                roleGroupConfig.getRoleGroupId());

        // 保存新版本信息
        configVersionInfoService.save(versionInfo);
    }

    private void checkOnSameNode(Integer clusterId, List<ServiceRoleHostMapping> list) {
        Set<String> hostnameSet = list.stream()
                .filter(s -> MUST_AT_SAME_NODE_BASIC_SERVICE.contains(s.getServiceRole()))
                .map(ServiceRoleHostMapping::getHosts)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(hostnameSet)) {
            return;
        }

        Set<String> installedHostnameSet = roleInstanceService.lambdaQuery()
                .eq(ClusterServiceRoleInstanceEntity::getClusterId, clusterId)
                .in(
                        ClusterServiceRoleInstanceEntity::getServiceName,
                        MUST_AT_SAME_NODE_BASIC_SERVICE)
                .list().stream()
                .map(ClusterServiceRoleInstanceEntity::getHostname)
                .collect(Collectors.toSet());
        hostnameSet.addAll(installedHostnameSet);

        if (hostnameSet.size() > 1) {
            throw new ServiceException(Status.BASIC_SERVICE_SELECT_MOST_ONE_HOST.getMsg());
        }
    }

    private void serviceValidation(ServiceRoleHostMapping serviceRoleHostMapping) {
        String serviceRole = serviceRoleHostMapping.getServiceRole();
        List<String> hosts = serviceRoleHostMapping.getHosts();

        if ("JournalNode".equals(serviceRole) && hosts.size() != 3) {
            throw new ServiceException(Status.THREE_JOURNALNODE_DEPLOYMENTS_REQUIRED.getMsg());
        }
        if ("NameNode".equals(serviceRole) && hosts.size() != 2) {
            throw new ServiceException(Status.TWO_NAMENODES_NEED_TO_BE_DEPLOYED.getMsg());
        }
        if ("ZKFC".equals(serviceRole) && hosts.size() != 2) {
            throw new ServiceException(Status.TWO_ZKFC_DEVICES_ARE_REQUIRED.getMsg());
        }
        if ("ResourceManager".equals(serviceRole) && hosts.size() != 2) {
            throw new ServiceException(Status.TWO_RESOURCEMANAGER_ARE_DEPLOYED.getMsg());
        }
        if ("ZkServer".equals(serviceRole) && (hosts.size() & 1) == 0) {
            throw new ServiceException(Status.ODD_NUMBER_ARE_REQUIRED_FOR_ZKSERVER.getMsg());
        }
        if ("DorisFE".equals(serviceRole) && (hosts.size() & 1) == 0) {
            throw new ServiceException(Status.ODD_NUMBER_ARE_REQUIRED_FOR_DORISFE.getMsg());
        }
        if ("KyuubiServer".equals(serviceRole) && hosts.size() != 2) {
            throw new ServiceException(Status.TWO_KYUUBISERVERS_NEED_TO_BE_DEPLOYED.getMsg());
        }
    }

    private List<ServiceConfig> listServiceConfigByServiceInstance(
            ClusterServiceInstanceEntity serviceInstance) {
        ClusterServiceInstanceRoleGroup roleGroup = roleGroupService
                .getRoleGroupByServiceInstanceId(serviceInstance.getId());
        ClusterServiceRoleGroupConfig config = groupConfigService.getConfigByRoleGroupId(roleGroup.getId());
        return JSONArray.parseArray(config.getConfigJson(), ServiceConfig.class);
    }

}
