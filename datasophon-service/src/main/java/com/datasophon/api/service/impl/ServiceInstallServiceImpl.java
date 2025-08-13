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
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.TypeReference;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.load.ServiceInfoMap;
import com.datasophon.api.load.ServiceRoleMap;
import com.datasophon.api.service.*;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.api.utils.ProcessUtils;
// Service层不返回Result，按照架构重构规范进行改造
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.FrameServiceDTO;
import com.datasophon.common.dto.ClusterServiceInstanceRoleGroupDTO;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.common.dto.ClusterServiceCommandHostCommandDTO;
import com.datasophon.common.dto.ClusterVariableDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.dto.ServiceConfigGroupDTO;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.common.enums.TypeRefs;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.Status;
// 添加必要的Converter依赖
import com.datasophon.api.converter.*;
import com.datasophon.api.converter.ServiceConfigGroupConverter;

import com.datasophon.common.model.DAG;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.HostServiceRoleMapping;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceInfo;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceNodeEdge;
import com.datasophon.common.model.ServiceRoleHostMapping;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.*;
import com.datasophon.common.enums.NeedRestart;
import com.datasophon.common.enums.ServiceState;
import com.datasophon.kubernetes.strategy.KubernetesServiceRoleStrategy;
import com.datasophon.kubernetes.strategy.KubernetesServiceRoleStrategyContext;
// QueryChain已迁移到DAO层，不再在Service层使用
import com.datasophon.dao.mapper.ClusterServiceInstanceRoleGroupMapper;
import com.datasophon.dao.mapper.ClusterHostMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.datasophon.api.utils.CacheOperateUtils.putRemoteServiceConfigMap;
import static com.datasophon.common.Constants.CONFIG;
import static com.datasophon.common.Constants.GENERAL;
import static com.datasophon.common.Constants.HOST_SERVICE_ROLE_MAPPING;
import static com.datasophon.common.Constants.INPUT;
import static com.datasophon.common.Constants.MASTER;
import static com.datasophon.common.Constants.MASTER_MANAGE_PACKAGE_PATH;
import static com.datasophon.common.Constants.SERVICE_ROLE_HOST_MAPPING;
import static com.datasophon.common.Constants.SLASH;
import static com.datasophon.common.Constants.UNDERLINE;

@Service("serviceInstallService")
@Transactional
public class ServiceInstallServiceImpl implements ServiceInstallService {

    public static final String PROMETHEUS = "prometheus";
    private static final Logger logger = LoggerFactory.getLogger(ServiceInstallServiceImpl.class);

    @Autowired
    private FrameServiceService frameService;
    @Autowired
    private ClusterServiceCommandService commandService;
    @Autowired
    private ClusterInfoService clusterInfoService;
    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;
    @Autowired
    private ClusterServiceCommandHostCommandService hostCommandService;
    @Autowired
    private ClusterVariableService variableService;
    @Autowired
    private ClusterServiceInstanceRoleGroupService roleGroupService;
    @Autowired
    private ClusterServiceRoleGroupConfigService groupConfigService;
    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;
    @Autowired
    private ConfigVersionInfoService configVersionInfoService;
    @Autowired
    private ClusterServiceInstanceRoleGroupMapper roleGroupMapper;
    @Autowired
    private ClusterHostMapper clusterHostMapper;

    // Converter依赖注入 - 按照架构重构规范
    @Autowired
    private ClusterServiceInstanceConverter clusterServiceInstanceConverter;
    @Autowired
    private FrameServiceConverter frameServiceConverter;
    @Autowired
    private ClusterServiceInstanceRoleGroupConverter roleGroupConverter;
    @Autowired
    private ClusterServiceRoleGroupConfigConverter roleGroupConfigConverter;
    @Autowired
    private ClusterServiceCommandHostCommandConverter hostCommandConverter;
    @Autowired
    private ClusterVariableConverter clusterVariableConverter;
    @Autowired
    private ClusterServiceRoleInstanceConverter roleInstanceConverter;
    @Autowired
    private ServiceConfigGroupConverter serviceConfigGroupConverter;

    /**
     * 处理配置列表，根据集群模式修改配置项的hidden和required属性
     *
     * @param list      配置列表
     * @param clusterId 集群ID
     */
    public static void processConfigList(List<ServiceConfig> list, Long clusterId) {
        if (ProcessUtils.getDepMode(clusterId) == ClusterType.KUBERNETES) {
            for (ServiceConfig config : list) {
                if (StrUtil.equals(config.getConfigType(),ClusterType.KUBERNETES.getCode())) {
                    config.setHidden(false);
                    config.setRequired(true);
                }
            }
        }
    }

    @Override
    public ServiceConfigGroupDTO getServiceConfigOption(Long clusterId, String serviceName) {
        List<ServiceConfig> list;
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        Map<String, String> globalVariables = GlobalVariables.get(clusterId);

        // Service层：获取DTO后转换为Entity
        ClusterServiceInstanceDTO serviceInstanceDTO = serviceInstanceService
                .getServiceInstanceByClusterIdAndServiceName(
                        clusterId, serviceName);
        ClusterServiceInstanceEntity serviceInstance = null;
        if (serviceInstanceDTO != null) {
            serviceInstance = clusterServiceInstanceConverter.dtoToEntity(serviceInstanceDTO);
        }
        if (Objects.nonNull(serviceInstance)) {
            list = listServiceConfigByServiceInstance(serviceInstance);
        } else {
            // Service层：获取DTO后转换为Entity
            FrameServiceDTO frameServiceDTO = this.frameService.getServiceByFrameCodeAndServiceName(
                    clusterInfo.getClusterFrame(), serviceName);
            FrameServiceEntity frameServiceEntity = frameServiceConverter.dtoToEntity(frameServiceDTO);
            String serviceConfig = frameServiceEntity.getServiceConfig();
            serviceConfig = PlaceholderUtils.replacePlaceholders(
                    serviceConfig, globalVariables, Constants.REGEX_VARIABLE);

            list = JSONArray.parseArray(serviceConfig, ServiceConfig.class);

            processConfigList(list, clusterId);
        }

        // 预处理Kubernetes配置
        if (list != null) {
            // 使用ConfigGroupUtils处理Kubernetes配置
            list = ConfigGroupUtils.preprocessKubernetesConfigs(list, clusterInfo.getClusterFrame(), serviceName);
        }

        ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(serviceName);
        if (Objects.nonNull(serviceRoleHandler)) {
            serviceRoleHandler.getConfig(clusterId, list);
        }

        ClusterInfoEntity clusterInfoEntity = SpringUtil.getBean(ClusterInfoService.class).getById(clusterId);
        ClusterType depType = clusterInfoEntity.getDepType();
        if (depType == ClusterType.KUBERNETES) {
            // 获取Kubernetes服务角色处理类
            KubernetesServiceRoleStrategy kubernetesServiceRoleStrategy = KubernetesServiceRoleStrategyContext
                    .getServiceRoleHandler(serviceName);
            if (Objects.nonNull(kubernetesServiceRoleStrategy)) {
                // 通过ClusterInfoUtils获取namespace，避免kubernetes模块直接访问数据库
                String namespace = ClusterInfoUtils.getKubernetesNamespace(clusterId);
                kubernetesServiceRoleStrategy.getConfig(clusterId, namespace, list);
            }
        }

        // 使用分组逻辑处理配置数据，返回分组结构提升前端用户体验
        var groupedConfigs = ConfigGroupUtils.groupByConfigTargetRoleOrCommon(list);
        return serviceConfigGroupConverter.toDto(groupedConfigs);
    }

    @Override
    public boolean saveServiceConfig(
            Long clusterId, String serviceName, List<ServiceConfig> list,
            Integer roleGroupId, String description, Integer userId, String username) {
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

        // add variable - Service层：获取DTO后转换为Entity
        FrameServiceDTO frameServiceDTO2 = frameService.getServiceByFrameCodeAndServiceName(
                clusterInfo.getClusterFrame(), serviceName);
        FrameServiceEntity frameServiceEntity = frameServiceConverter.dtoToEntity(frameServiceDTO2);

        // TODO 检查是否为重复逻辑
        for (ServiceConfig serviceConfig : list) {
            String configName = serviceConfig.getName();

            // 处理Kubernetes配置项，添加角色前缀
            if (serviceConfig.getConfigGroup() != null
                    && serviceConfig.getConfigGroup().startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
                // 从配置组名称中提取角色名
                String extractedRoleName = getKubernetesRole(serviceConfig.getConfigGroup());
                if (extractedRoleName != null) {
                    // 将角色名转换为小写下划线格式，与ConfigGroupUtils.addRolePrefixForKubernetesConfig保持一致
                    String normRoleName = extractedRoleName.toLowerCase();

                    // 检查是否已经添加了前缀，只有未添加时才添加
                    if (!configName.startsWith(normRoleName + "_")) {
                        configName = ConfigGroupUtils.addRolePrefixForKubernetesConfig(
                                extractedRoleName, configName, serviceConfig.getConfigGroup());
                        serviceConfig.setName(configName);
                    }
                }
            }

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

        // Service层：获取DTO后转换为Entity
        ClusterServiceInstanceDTO serviceInstanceDTO = serviceInstanceService
                .getServiceInstanceByClusterIdAndServiceName(
                        clusterId, serviceName);
        ClusterServiceInstanceEntity serviceInstanceEntity = null;
        if (serviceInstanceDTO != null) {
            serviceInstanceEntity = clusterServiceInstanceConverter.dtoToEntity(serviceInstanceDTO);
        }

        boolean versionCreated = false; // 标记是否创建了新版本

        if (Objects.isNull(serviceInstanceEntity)) {
            serviceInstanceEntity = saveServiceInstance(clusterId, serviceName, frameServiceEntity);
            ClusterServiceInstanceRoleGroupEntity clusterServiceInstanceRoleGroupEntity = saveServiceInstanceRoleGroup(clusterId,
                    serviceName, serviceInstanceEntity);

            // 如果描述为空，使用默认描述
            String finalDescription = description;
            if (StringUtils.isBlank(finalDescription)) {
                finalDescription = "初始配置";
            }

            boolean initialSaveResult = saveServiceRoleGroupConfig(
                    clusterId, serviceName, list, configFileMap, clusterServiceInstanceRoleGroupEntity, finalDescription,
                    userId, username);
            CacheUtils.put(
                    "UseRoleGroup_" + serviceInstanceEntity.getId(),
                    clusterServiceInstanceRoleGroupEntity.getId());

            versionCreated = initialSaveResult; // 只有当成功保存到数据库时才标记为创建了新版本
        } else {
            Set<String> configUpdateRoleSet = new HashSet<>();
            List<ServiceConfig> originalConfigs = listServiceConfigByServiceInstance(serviceInstanceEntity);

            // 如果描述为空，生成修改内容的描述
            String finalDescription = description;
            if (StringUtils.isBlank(finalDescription)) {
                finalDescription = generateChangeDescription(originalConfigs, list);
            }

            configNeedUpdate(serviceInstanceEntity, list, configUpdateRoleSet);
            ClusterServiceRoleGroupConfigEntity roleGroupConfig;
            if (Objects.isNull(roleGroupId)) {
                // Service层：获取DTO后转换为Entity
                ClusterServiceInstanceRoleGroupDTO roleGroupDTO = roleGroupService.getRoleGroupByServiceInstanceId(
                        serviceInstanceEntity.getId());
                ClusterServiceInstanceRoleGroupEntity roleGroup = roleGroupConverter.dtoToEntity(roleGroupDTO);
                ClusterServiceRoleGroupConfigDTO configDTO = groupConfigService
                        .getConfigByRoleGroupId(roleGroup.getId());
                roleGroupConfig = roleGroupConfigConverter.dtoToEntity(configDTO);
            } else {
                // Service层：获取DTO后转换为Entity
                ClusterServiceRoleGroupConfigDTO configDTO = groupConfigService.getConfigByRoleGroupId(roleGroupId);
                roleGroupConfig = roleGroupConfigConverter.dtoToEntity(configDTO);
            }
            CacheUtils.put(
                    "UseRoleGroup_" + serviceInstanceEntity.getId(),
                    roleGroupConfig.getRoleGroupId());
            if (!configUpdateRoleSet.isEmpty()) {
                ClusterServiceRoleGroupConfigEntity newRoleGroupConfig = new ClusterServiceRoleGroupConfigEntity();
                if (Objects.isNull(roleGroupId)) {
                    ClusterServiceInstanceRoleGroupEntity roleGroup = saveNewRoleGroup(serviceInstanceEntity);
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
                buildConfig(list, configFileMap, newRoleGroupConfig, finalDescription);

                // 保存配置并检查是否成功插入数据库
                boolean saveResult = groupConfigService.save(newRoleGroupConfig);

                // 只有当数据库操作确实成功时才标记为创建了新版本
                if (saveResult) {
                    // 保存配置版本信息，包含用户信息
                    saveConfigVersionInfo(newRoleGroupConfig, "GROUP_CONFIG", newRoleGroupConfig.getId(), userId,
                            username, finalDescription);
                    versionCreated = true; // 有配置更新且成功保存到数据库时创建了新版本
                } else {
                    logger.warn("Configuration was not updated in database for service: {}, roleGroupId: {}",
                            serviceName, newRoleGroupConfig.getRoleGroupId());
                }
            }
            // update service instance
            serviceInstanceEntity.setUpdateTime(new Date());
            serviceInstanceEntity.setLabel(frameServiceEntity.getLabel());
            serviceInstanceService.updateById(serviceInstanceEntity);
        }

        // 返回是否创建了新版本
        return versionCreated;
    }

    @Override
    public boolean saveServiceRoleHostMapping(Long clusterId, List<ServiceRoleHostMapping> list) {

        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String hostMapKey = clusterInfo.getClusterCode()
                + UNDERLINE
                + SERVICE_ROLE_HOST_MAPPING;
        // 使用 getGeneric 自动返回空Map，避免空指针异常
        Map<String, List<String>> map = CacheOperateUtils.getGeneric(hostMapKey,
                TypeRefs.MAP_STRING_LIST_STRING);

        for (ServiceRoleHostMapping serviceRoleHostMapping : list) {
            serviceValidation(serviceRoleHostMapping);

            map.put(serviceRoleHostMapping.getServiceRole(), serviceRoleHostMapping.getHosts());

            ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(
                    serviceRoleHostMapping.getServiceRole());
            if (Objects.nonNull(serviceRoleHandler)) {
                serviceRoleHandler.handler(clusterId, serviceRoleHostMapping.getHosts());
            }
        }

        // 缓存zookeeper节点数量
        ClusterServiceRoleInstanceService clusterServiceRoleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        // Service层：获取DTO列表后转换为Entity列表
        List<ClusterServiceRoleInstanceDTO> zookeeperNodeDTOs = clusterServiceRoleInstanceService
                .getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, "ZkServer");
        List<ClusterServiceRoleInstanceEntity> zookeeperNodes = roleInstanceConverter
                .dtoListToEntityList(zookeeperNodeDTOs);
        int zkNodeCount = CollUtil.size(zookeeperNodes);
        String zkNodeCountKey = "zookeeper_node_count";
        CacheUtils.put(clusterId + UNDERLINE + zkNodeCountKey, zkNodeCount);
        logger.info("已缓存 Zookeeper 节点数量: {}，集群ID: {}", zkNodeCount, clusterId);

        CacheUtils.put(
                clusterInfo.getClusterCode()
                        + UNDERLINE
                        + SERVICE_ROLE_HOST_MAPPING,
                map);
        CacheUtils.put(
                clusterInfo.getId()
                        + UNDERLINE
                        + SERVICE_ROLE_HOST_MAPPING,
                map);
        return true; // 成功完成操作
    }

    @Override
    public void saveHostServiceRoleMapping(Long clusterId, List<HostServiceRoleMapping> list) {
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
        // 方法无返回值，操作完成
    }

    @Override
    public Map<String, List<String>> getServiceRoleDeployOverview(Long clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        // 使用 getGeneric 自动返回空Map，避免空指针异常
        return CacheOperateUtils.getGeneric(
                clusterInfo.getClusterCode()
                        + UNDERLINE
                        + SERVICE_ROLE_HOST_MAPPING,
                TypeRefs.MAP_STRING_LIST_STRING);
    }

    /**
     */
    @Override
    public Map<String, Object> startInstallService(Long clusterId, List<String> commandIds) {
        Collection<ClusterServiceCommandEntity> commands = commandService.listByIds(commandIds);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        DAG<String, ServiceNode, ServiceNodeEdge> dag = new DAG<>();
        for (ClusterServiceCommandEntity command : commands) {
            // Service层：获取DTO列表后转换为Entity列表
            List<ClusterServiceCommandHostCommandDTO> commandHostDTOList = hostCommandService
                    .getHostCommandListByCommandId(command.getCommandId());
            List<ClusterServiceCommandHostCommandEntity> commandHostList = hostCommandConverter
                    .dtoListToEntityList(commandHostDTOList);
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
                    dag.addEdge(dependency, command.getServiceName(), null, false);
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("dag", dag);
        return result;
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
    public void getServiceRoleHostMapping(Long clusterId) {
        // TODO 实现获取服务角色主机映射逻辑
        // 方法无返回值，操作完成
    }

    @Override
    public void checkServiceDependency(Long clusterId, String serviceIds) {
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
        // 方法无返回值，操作完成
    }

    private ClusterServiceInstanceRoleGroupEntity saveNewRoleGroup(
            ClusterServiceInstanceEntity serviceInstanceEntity) {
        // DAO层：使用Mapper统计角色组数量
        long count = roleGroupMapper.countByRoleGroupTypeAndServiceInstanceId("auto", serviceInstanceEntity.getId());
        ClusterServiceInstanceRoleGroupEntity roleGroup = new ClusterServiceInstanceRoleGroupEntity();
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

    private boolean saveServiceRoleGroupConfig(
            Long clusterId,
            String serviceName,
            List<ServiceConfig> list,
            HashMap<Generators, List<ServiceConfig>> configFileMap,
            ClusterServiceInstanceRoleGroupEntity clusterServiceInstanceRoleGroupEntity,
            String description,
            Integer userId,
            String username) {
        ClusterServiceRoleGroupConfigEntity roleGroupConfig = new ClusterServiceRoleGroupConfigEntity();
        roleGroupConfig.setRoleGroupId(clusterServiceInstanceRoleGroupEntity.getId());
        roleGroupConfig.setClusterId(clusterId);
        roleGroupConfig.setServiceName(serviceName);
        roleGroupConfig.setCreateTime(new Date());
        roleGroupConfig.setUpdateTime(new Date());
        buildConfig(list, configFileMap, roleGroupConfig, description);
        roleGroupConfig.setConfigVersion(1);
        boolean saveResult = groupConfigService.save(roleGroupConfig);

        if (saveResult) {
            // 保存配置版本信息，包含用户信息
            saveConfigVersionInfo(roleGroupConfig, "GROUP_CONFIG", roleGroupConfig.getId(), userId, username,
                    description);
            return true;
        } else {
            logger.warn("Failed to save initial configuration for service: {}, roleGroupId: {}",
                    serviceName, roleGroupConfig.getRoleGroupId());
            return false;
        }
    }

    private ClusterServiceInstanceRoleGroupEntity saveServiceInstanceRoleGroup(
            Long clusterId,
            String serviceName,
            ClusterServiceInstanceEntity serviceInstanceEntity) {
        ClusterServiceInstanceRoleGroupEntity clusterServiceInstanceRoleGroupEntity = new ClusterServiceInstanceRoleGroupEntity();
        clusterServiceInstanceRoleGroupEntity.setServiceInstanceId(serviceInstanceEntity.getId());
        clusterServiceInstanceRoleGroupEntity.setClusterId(clusterId);
        clusterServiceInstanceRoleGroupEntity.setRoleGroupName("默认");
        clusterServiceInstanceRoleGroupEntity.setServiceName(serviceName);
        clusterServiceInstanceRoleGroupEntity.setRoleGroupType("default");
        boolean saveResult = roleGroupService.save(clusterServiceInstanceRoleGroupEntity);

        if (!saveResult) {
            logger.warn("Failed to save role group for service: {}, serviceInstanceId: {}",
                    serviceName, serviceInstanceEntity.getId());
        }

        return clusterServiceInstanceRoleGroupEntity;
    }

    private ClusterServiceInstanceEntity saveServiceInstance(
            Long clusterId, String serviceName,
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
            Long clusterId, HashMap<Generators, List<ServiceConfig>> configFileMap) {
        // DAO层：使用Mapper查询受管理的主机列表
        List<ClusterHostEntity> hostList = clusterHostMapper.selectByClusterId(clusterId);

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
        for (ClusterHostEntity clusterHostEntity : hostList) {
            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setName("worker_" + clusterHostEntity.getHostname());
            serviceConfig.setValue(clusterHostEntity.getHostname() + ":8585");
            serviceConfig.setRequired(true);
            workerServiceConfigs.add(serviceConfig);

            ServiceConfig nodeServiceConfig = new ServiceConfig();
            nodeServiceConfig.setName("node_" + clusterHostEntity.getHostname());
            nodeServiceConfig.setValue(clusterHostEntity.getHostname() + ":9100");
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

        // Service层：获取DTO后转换为Entity
        final FrameServiceDTO frameServiceDTO = this.frameService.getServiceByFrameCodeAndServiceName(
                clusterInfo.getClusterFrame(), serviceName);
        final FrameServiceEntity frameService = frameServiceConverter.dtoToEntity(frameServiceDTO);

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
                new TypeReference<>() {
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

    private void addToGlobalVariable(Long clusterId, String variableName, String value) {
        // Service层：获取DTO后转换为Entity
        ClusterVariableDTO clusterVariableDTO = variableService.getVariableByVariableName(variableName, clusterId);
        ClusterVariableEntity clusterVariableEntity = null;
        if (clusterVariableDTO != null) {
            clusterVariableEntity = clusterVariableConverter.dtoToEntity(clusterVariableDTO);
        }
        if (Objects.nonNull(clusterVariableEntity)) {
            if (!value.equals(clusterVariableEntity.getVariableValue())) {
                clusterVariableEntity.setVariableValue(value);
                variableService.updateById(clusterVariableEntity);
            }
        } else {
            clusterVariableEntity = new ClusterVariableEntity();
            clusterVariableEntity.setClusterId(clusterId);
            clusterVariableEntity.setVariableName(variableName);
            clusterVariableEntity.setVariableValue(value);
            variableService.save(clusterVariableEntity);
        }
    }

    private void buildConfig(
            List<ServiceConfig> list,
            HashMap<Generators, List<ServiceConfig>> configFileMap,
            ClusterServiceRoleGroupConfigEntity roleGroupConfig,
            String description) {
        roleGroupConfig.setConfigJson(JSONArray.toJSONString(list));
        roleGroupConfig.setConfigJsonMd5(SecureUtil.md5(JSONArray.toJSONString(list)));
        roleGroupConfig.setConfigFileJson(JSON.toJSONString(configFileMap));
        roleGroupConfig.setConfigFileJsonMd5(SecureUtil.md5(JSON.toJSONString(configFileMap)));

        // 同步保存配置版本详情
        saveConfigVersionInfo(roleGroupConfig, "ROLE_GROUP", roleGroupConfig.getRoleGroupId(), null, "system",
                description);
    }

    /**
     * 保存配置版本详情信息
     *
     * @param roleGroupConfig 角色组配置
     * @param refType         版本类型
     * @param refId           版本ID
     * @param userId          用户ID
     * @param username        用户名
     */
    private void saveConfigVersionInfo(ClusterServiceRoleGroupConfigEntity roleGroupConfig, String refType, Integer refId,
                                       Integer userId, String username, String description) {
        ConfigVersionInfoEntity configVersionInfo = new ConfigVersionInfoEntity();
        // 获取当前最大版本号并加1
        Integer currentMaxVersion = configVersionInfoService.getMaxVersion(refType, refId);
        configVersionInfo.setVersion(currentMaxVersion + 1);
        configVersionInfo.setRefType(refType);
        configVersionInfo.setRefId(refId);
        configVersionInfo.setDescription(description); // 使用传入的描述
        configVersionInfo.setEditor(username != null ? username : "system"); // 使用用户名作为编辑者，如果为空则使用默认值
        configVersionInfo.setUserId(userId); // 添加用户ID
        configVersionInfo.setEditTime(new Date());
        configVersionInfo.setIsCurrent(true);
        configVersionInfo.setServiceCode(roleGroupConfig.getServiceName());
        configVersionInfoService.save(configVersionInfo);
        // 更新其他版本为非当前版本
        configVersionInfoService.updateCurrentVersion(configVersionInfo.getVersion(), refType, refId);
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
        // Service层：获取DTO后转换为Entity
        ClusterServiceInstanceRoleGroupDTO roleGroupDTO = roleGroupService
                .getRoleGroupByServiceInstanceId(serviceInstance.getId());
        ClusterServiceInstanceRoleGroupEntity roleGroup = roleGroupConverter.dtoToEntity(roleGroupDTO);
        ClusterServiceRoleGroupConfigDTO configDTO = groupConfigService.getConfigByRoleGroupId(roleGroup.getId());
        ClusterServiceRoleGroupConfigEntity config = roleGroupConfigConverter.dtoToEntity(configDTO);
        return JSONArray.parseArray(config.getConfigJson(), ServiceConfig.class);
    }

    /**
     * Determines if a config is a Kubernetes configuration
     */
    private boolean isKubernetesConfig(ServiceConfig config) {
        return config != null && config.getConfigGroup() != null &&
                config.getConfigGroup().startsWith(Constants.KUBERNETES_CONFIG_PREFIX);
    }

    /**
     * Extracts the Kubernetes subgroup from a config group name
     * E.g., from "kubernetes.config.persistent-volume-claims.ZkServer" returns
     * "persistentVolumeClaims"
     */
    private String getKubernetesSubgroup(String configGroup) {
        if (configGroup == null || !configGroup.startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
            return null;
        }

        String[] parts = configGroup.split("\\.");
        if (parts.length >= 3) {
            return parts[2];
        }
        return null;
    }

    /**
     * Extracts the role name from a kubernetes config group
     * E.g., from "kubernetes.config.persistent-volume-claims.ZkServer" returns
     * "ZkServer"
     */
    private String getKubernetesRole(String configGroup) {
        if (configGroup == null || !configGroup.startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
            return null;
        }

        String[] parts = configGroup.split("\\.");
        if (parts.length >= 4) {
            return parts[3];
        }
        return null;
    }

    /**
     * 获取Kubernetes子组的友好显示名称
     */
    private String getFriendlySubgroupName(String subgroup) {
        Map<String, String> friendlyNames = new HashMap<>();
        friendlyNames.put("persistentVolumeClaims", "存储");
        friendlyNames.put("resources", "资源");
        friendlyNames.put("services", "服务");

        return friendlyNames.getOrDefault(subgroup, subgroup);
    }

    /**
     * 生成修改内容的描述
     *
     * @param originalConfigs 原始配置
     * @param newConfigs      新配置
     * @return 修改内容的描述
     */
    private String generateChangeDescription(List<ServiceConfig> originalConfigs, List<ServiceConfig> newConfigs) {
        // 创建原始配置的Map，便于查找
        Map<String, ServiceConfig> originalConfigMap = originalConfigs.stream()
                .collect(Collectors.toMap(ServiceConfig::getName, config -> config, (v1, v2) -> v1));

        // 创建新配置的Map，便于查找
        Map<String, ServiceConfig> newConfigMap = newConfigs.stream()
                .collect(Collectors.toMap(ServiceConfig::getName, config -> config, (v1, v2) -> v1));

        // 收集修改的配置项
        List<String> regularChangedConfigs = new ArrayList<>();
        Map<String, Set<String>> kubernetesChangedConfigsByRole = new HashMap<>(); // Role -> Set of subgroups

        // 检查修改的配置项
        for (ServiceConfig newConfig : newConfigs) {
            String configName = newConfig.getName();
            Object newValue = newConfig.getValue();

            if (originalConfigMap.containsKey(configName)) {
                ServiceConfig originalConfig = originalConfigMap.get(configName);
                Object originalValue = originalConfig.getValue();

                // 如果值不相等，添加到修改列表
                if (!Objects.equals(newValue, originalValue)) {
                    if (isKubernetesConfig(newConfig)) {
                        // 处理Kubernetes配置
                        String configGroup = newConfig.getConfigGroup();
                        String role = getKubernetesRole(configGroup);
                        String subgroup = getKubernetesSubgroup(configGroup);

                        if (role != null && subgroup != null) {
                            kubernetesChangedConfigsByRole.computeIfAbsent(role, k -> new HashSet<>()).add(subgroup);
                        } else {
                            // 如果无法确定角色或子组，作为常规配置处理
                            String label = StringUtils.isNotBlank(newConfig.getLabel()) ? newConfig.getLabel()
                                    : configName;
                            regularChangedConfigs.add(label);
                        }
                    } else {
                        // 处理常规配置
                        String label = StringUtils.isNotBlank(newConfig.getLabel()) ? newConfig.getLabel() : configName;
                        regularChangedConfigs.add(label);
                    }
                }
            } else {
                // 新增的配置项
                if (isKubernetesConfig(newConfig)) {
                    // 处理Kubernetes配置
                    String configGroup = newConfig.getConfigGroup();
                    String role = getKubernetesRole(configGroup);
                    String subgroup = getKubernetesSubgroup(configGroup);

                    if (role != null && subgroup != null) {
                        kubernetesChangedConfigsByRole.computeIfAbsent(role, k -> new HashSet<>()).add(subgroup);
                    } else {
                        String label = StringUtils.isNotBlank(newConfig.getLabel()) ? newConfig.getLabel() : configName;
                        regularChangedConfigs.add(label);
                    }
                } else {
                    String label = StringUtils.isNotBlank(newConfig.getLabel()) ? newConfig.getLabel() : configName;
                    regularChangedConfigs.add(label);
                }
            }
        }

        // 检查删除的配置项
        for (ServiceConfig originalConfig : originalConfigs) {
            String configName = originalConfig.getName();
            if (!newConfigMap.containsKey(configName)) {
                if (isKubernetesConfig(originalConfig)) {
                    // 处理Kubernetes配置
                    String configGroup = originalConfig.getConfigGroup();
                    String role = getKubernetesRole(configGroup);
                    String subgroup = getKubernetesSubgroup(configGroup);

                    if (role != null && subgroup != null) {
                        kubernetesChangedConfigsByRole.computeIfAbsent(role, k -> new HashSet<>()).add(subgroup);
                    } else {
                        String label = StringUtils.isNotBlank(originalConfig.getLabel()) ? originalConfig.getLabel()
                                : configName;
                        regularChangedConfigs.add(label);
                    }
                } else {
                    String label = StringUtils.isNotBlank(originalConfig.getLabel()) ? originalConfig.getLabel()
                            : configName;
                    regularChangedConfigs.add(label);
                }
            }
        }

        // 构建描述
        StringBuilder sb = new StringBuilder();

        // 如果没有修改，返回默认描述
        if (regularChangedConfigs.isEmpty() && kubernetesChangedConfigsByRole.isEmpty()) {
            return "配置更新";
        }

        // 1. 先添加Kubernetes配置的变更
        if (!kubernetesChangedConfigsByRole.isEmpty()) {
            sb.append("修改了 ");
            int roleCount = 0;
            for (Map.Entry<String, Set<String>> entry : kubernetesChangedConfigsByRole.entrySet()) {
                String role = entry.getKey();
                Set<String> subgroups = entry.getValue();

                if (roleCount > 0) {
                    sb.append(", ");
                }

                sb.append(role).append(" 的 ");

                // 映射子组名称为更友好的显示名称
                List<String> friendlySubgroupNames = subgroups.stream()
                        .map(this::getFriendlySubgroupName)
                        .toList();

                for (int i = 0; i < friendlySubgroupNames.size(); i++) {
                    if (i > 0) {
                        sb.append("、");
                    }
                    sb.append(friendlySubgroupNames.get(i));
                }
                sb.append("配置");

                roleCount++;
                if (roleCount >= 2) { // 最多只显示2个角色的Kubernetes配置变更
                    break;
                }
            }
        }

        // 2. 再添加常规配置的变更
        if (!regularChangedConfigs.isEmpty()) {
            if (StrUtil.isNotBlank(sb)) {
                sb.append(", ");
            } else {
                sb.append("修改了 ");
            }

            // 限制最多显示3个常规配置项
            int maxItems = Math.min(regularChangedConfigs.size(), 3);
            for (int i = 0; i < maxItems; i++) {
                sb.append(regularChangedConfigs.get(i));
                if (i < maxItems - 1) {
                    sb.append(", ");
                }
            }

            // 如果有更多修改项，添加省略号
            if (regularChangedConfigs.size() > maxItems) {
                sb.append(" 等");
            }
        }

        return sb.toString();
    }

}
