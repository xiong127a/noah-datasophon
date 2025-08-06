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

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.alibaba.fastjson2.JSONArray;
import com.datasophon.common.dto.ClusterAlertHistoryDTO;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.enums.Status;
import com.datasophon.api.kubernetes.handler.KubernetesServiceStopHandler;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceDashboardService;
import com.datasophon.api.service.ClusterAlertHistoryService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;

import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.PlaceholderUtils;

import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceDashboard;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.dao.enums.NeedRestart;

import com.datasophon.dao.enums.ServiceState;
import com.datasophon.dao.mapper.ClusterServiceInstanceMapper;
import com.datasophon.api.converter.ClusterServiceInstanceConverter;
import com.datasophon.api.converter.ClusterServiceRoleInstanceConverter;
import com.datasophon.api.converter.FrameServiceRoleConverter;
import com.datasophon.api.converter.ClusterServiceInstanceRoleGroupConverter;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.dto.FrameServiceRoleDTO;
import com.datasophon.common.dto.ClusterServiceInstanceRoleGroupDTO;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 集群服务实例服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("clusterServiceInstanceService")
@Transactional
@Slf4j
public class ClusterServiceInstanceServiceImpl
        extends ServiceImpl<ClusterServiceInstanceMapper, ClusterServiceInstanceEntity>
        implements ClusterServiceInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceInstanceServiceImpl.class);

    @Autowired
    private ClusterServiceInstanceConverter clusterServiceInstanceConverter;

    @Autowired
    private ClusterServiceRoleInstanceConverter clusterServiceRoleInstanceConverter;

    @Autowired
    private FrameServiceRoleConverter frameServiceRoleConverter;

    @Autowired
    private ClusterServiceInstanceRoleGroupConverter clusterServiceInstanceRoleGroupConverter;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private FrameServiceRoleService frameServiceRoleService;

    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;

    @Autowired
    private ClusterServiceInstanceRoleGroupService roleGroupService;

    @Autowired
    private ClusterServiceRoleInstanceWebuisService webuisService;

    @Autowired
    private ClusterServiceDashboardService clusterServiceDashboardService;

    @Autowired
    private ClusterAlertHistoryService clusterAlertHistoryService;

    // 创建一个定时缓存，缓存时间为10秒
    private static final TimedCache<String, ConnectionInfo> CONNECTION_INFO_CACHE = CacheUtil.newTimedCache(5000);

    // 开启缓存定时清理
    static {
        // 每5秒检查一次过期缓存
        CONNECTION_INFO_CACHE.schedulePrune(2000);
    }

    @Override
    public ClusterServiceInstanceDTO getServiceInstanceByClusterIdAndServiceName(Integer clusterId,
            String serviceName) {
        ClusterServiceInstanceEntity entity = getMapper().selectByClusterIdAndServiceName(clusterId, serviceName);
        return clusterServiceInstanceConverter.entityToDto(entity);
    }

    @Override
    public String getServiceConfigByClusterIdAndServiceName(Integer clusterId, String serviceName) {
        return getMapper().getServiceConfigByClusterIdAndServiceName(clusterId, serviceName);
    }

    @Override
    public List<ClusterServiceInstanceDTO> listAll(Integer clusterId) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);

        // 查询集群下所有服务实例并按排序号升序排列
        List<ClusterServiceInstanceEntity> serviceInstances = getMapper()
                .selectByClusterIdOrderBySortNum(clusterId);

        // 处理每个服务实例
        for (ClusterServiceInstanceEntity serviceInstance : serviceInstances) {
            serviceInstance.setServiceStateCode(serviceInstance.getServiceState().getValue());
            boolean needUpdate = false;

            // 查询仪表盘
            ClusterServiceDashboard dashboard = clusterServiceDashboardService
                    .getByServiceName(serviceInstance.getServiceName());

            if (Objects.nonNull(dashboard)) {
                String dashboardUrl = PlaceholderUtils.replacePlaceholders(dashboard.getDashboardUrl(), globalVariables,
                        Constants.REGEX_VARIABLE);
                serviceInstance.setDashboardUrl(dashboardUrl);
            }

            // 查询启用的告警数量
            long alertNum = clusterAlertHistoryService.countEnabledByServiceInstanceId(serviceInstance.getId());

            serviceInstance.setAlertNum(alertNum);

            // 查询该服务的所有角色实例
            List<ClusterServiceRoleInstanceDTO> totalRoleDTOList = roleInstanceService
                    .getServiceRoleInstanceListByServiceId(serviceInstance.getId());
            List<ClusterServiceRoleInstanceEntity> totalRoleList = clusterServiceRoleInstanceConverter
                    .dtoListToEntityList(totalRoleDTOList);

            if (Objects.nonNull(totalRoleList) && totalRoleList.isEmpty()) {
                serviceInstance.setServiceState(ServiceState.WAIT_INSTALL);
                needUpdate = true;
            }

            // 查询停止状态角色（通过告警历史判断）
            List<ClusterAlertHistoryDTO> stoppedRoleDTOList = clusterAlertHistoryService
                    .getStoppedRolesByServiceId(serviceInstance.getId());

            // 如果有停止状态的告警，设置服务状态为异常
            if (Objects.nonNull(stoppedRoleDTOList) && !stoppedRoleDTOList.isEmpty()) {
                if (!ServiceState.EXISTS_EXCEPTION.equals(serviceInstance.getServiceState())) {
                    serviceInstance.setServiceState(ServiceState.EXISTS_EXCEPTION);
                    needUpdate = true;
                }
            } else {
                if (!ServiceState.RUNNING.equals(serviceInstance.getServiceState())
                        && serviceInstance.getServiceState() != ServiceState.WAIT_INSTALL
                        && serviceInstance.getServiceState() != ServiceState.EXISTS_ALARM) {
                    serviceInstance.setServiceState(ServiceState.RUNNING);
                    needUpdate = true;
                }
            }

            // 查询告警状态角色（通过告警历史判断）
            List<ClusterAlertHistoryDTO> alarmRoleDTOList = clusterAlertHistoryService
                    .getAlarmRolesByServiceId(serviceInstance.getId());

            // 如果有告警状态的告警，设置服务状态为告警
            if (Objects.nonNull(alarmRoleDTOList) && !alarmRoleDTOList.isEmpty()) {
                if (!ServiceState.EXISTS_ALARM.equals(serviceInstance.getServiceState())
                        && !ServiceState.EXISTS_EXCEPTION.equals(serviceInstance.getServiceState())) {
                    serviceInstance.setServiceState(ServiceState.EXISTS_ALARM);
                    needUpdate = true;
                }
            } else {
                if (serviceInstance.getServiceState() == ServiceState.EXISTS_ALARM) {
                    serviceInstance.setServiceState(ServiceState.RUNNING);
                    needUpdate = true;
                }
            }

            // 查询是否进行了配置更新
            List<ClusterServiceRoleInstanceDTO> obsoleteRoleDTOList = roleInstanceService
                    .getObsoleteService(serviceInstance.getId());
            List<ClusterServiceRoleInstanceEntity> obsoleteRoleList = clusterServiceRoleInstanceConverter
                    .dtoListToEntityList(obsoleteRoleDTOList);

            if (Objects.nonNull(obsoleteRoleList) && obsoleteRoleList.isEmpty()
                    && serviceInstance.getNeedRestart() == NeedRestart.YES) {
                serviceInstance.setNeedRestart(NeedRestart.NO);
                needUpdate = true;
            }

            if (needUpdate) {
                updateById(serviceInstance);
            }
        }
        return clusterServiceInstanceConverter.entityListToDtoList(serviceInstances);
    }

    @Override
    public String downloadClientConfig(Integer clusterId, String serviceName) {
        // 实现下载客户端配置逻辑
        try {
            // 获取集群信息
            ClusterInfoDTO clusterInfo = clusterInfoService.getClusterById(clusterId);
            if (clusterInfo == null) {
                throw new RuntimeException("Cluster not found with id: " + clusterId);
            }

            // 获取服务实例
            ClusterServiceInstanceDTO serviceInstance = getServiceInstanceByClusterIdAndServiceName(clusterId,
                    serviceName);
            if (serviceInstance == null) {
                throw new RuntimeException("Service instance not found: " + serviceName);
            }

            // 根据服务类型生成客户端配置文件路径
            return generateClientConfigPath(clusterInfo, serviceName);
        } catch (Exception e) {
            logger.error("Failed to generate client config for service: {}", serviceName, e);
            return null;
        }
    }

    @Override
    public List<FrameServiceRoleEntity> getServiceRoleType(Integer serviceInstanceId) {
        ClusterServiceInstanceEntity serviceInstanceEntity = getById(serviceInstanceId);
        if (serviceInstanceEntity == null) {
            throw new RuntimeException("Service instance not found with id: " + serviceInstanceId);
        }
        Integer frameServiceId = serviceInstanceEntity.getFrameServiceId();
        List<FrameServiceRoleDTO> frameServiceRoleDTOList = frameServiceRoleService
                .getAllServiceRoleList(frameServiceId);
        return frameServiceRoleConverter.dtoListToEntityList(frameServiceRoleDTOList);
    }

    /**
     * 判断是否为Kubernetes配置
     */
    private boolean isKubernetesConfig(ServiceConfig config) {
        return config != null && config.getConfigGroup() != null &&
                config.getConfigGroup().startsWith(Constants.KUBERNETES_CONFIG_PREFIX);
    }

    /**
     * 从Kubernetes配置名称中提取基础名称（去除角色前缀）
     * 例如：从 "ZkServer_默认_storage_classes" 提取出 "storage_classes"
     */
    private String extractKubernetesBaseConfigName(String fullName) {
        if (fullName == null) {
            return null;
        }

        // 查找最后一个下划线
        int lastUnderscoreIndex = fullName.lastIndexOf("_");
        if (lastUnderscoreIndex > 0) {
            return fullName.substring(lastUnderscoreIndex + 1);
        }
        return fullName;
    }

    /**
     * 从Kubernetes配置组获取角色名称
     */
    private String getKubernetesRole(String configGroup) {
        if (configGroup == null || !configGroup.startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
            return null;
        }

        String[] parts = configGroup.split("\\.");
        if (parts.length >= 4) {
            return parts[3]; // 返回角色名称，如"ZkServer"
        }
        return null;
    }

    /**
     * 从Kubernetes配置组获取子组名称
     */
    private String getKubernetesSubgroup(String configGroup) {
        if (configGroup == null || !configGroup.startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
            return null;
        }

        String[] parts = configGroup.split("\\.");
        if (parts.length >= 3) {
            return parts[2]; // 返回子组名称，如"persistentVolumeClaims"
        }
        return null;
    }

    @Override
    public Map<String, List<Map<String, Object>>> configVersionCompare(Integer serviceInstanceId, Integer roleGroupId,
            Boolean showOnlyDifferences) {
        // 获取最新的两个配置版本进行比较
        List<ClusterServiceRoleGroupConfig> list = roleGroupConfigService.getLatestTwoConfigsByRoleGroupId(roleGroupId);

        // 如果没有足够的版本进行比较，返回空结果
        if (list == null || list.size() < 2) {
            return new HashMap<>();
        }

        // 获取配置版本
        ClusterServiceRoleGroupConfig configA = list.get(0); // 新版本
        ClusterServiceRoleGroupConfig configB = list.get(1); // 旧版本

        // 解析配置JSON
        String configJsonA = configA.getConfigJson();
        String configJsonB = configB.getConfigJson();
        List<ServiceConfig> configListA = JSONArray.parseArray(configJsonA, ServiceConfig.class);
        List<ServiceConfig> configListB = JSONArray.parseArray(configJsonB, ServiceConfig.class);

        // 创建配置项映射，用于快速查找（使用规范化的Kubernetes配置名称）
        Map<String, ServiceConfig> configMapA = new HashMap<>();
        Map<String, ServiceConfig> configMapB = new HashMap<>();

        // 处理configA
        for (ServiceConfig config : configListA) {
            if (isKubernetesConfig(config)) {
                // 对于Kubernetes配置，使用组合键（role.subgroup.baseConfigName）
                String configGroup = config.getConfigGroup();
                String role = getKubernetesRole(configGroup);
                String subgroup = getKubernetesSubgroup(configGroup);

                if (role != null && subgroup != null) {
                    String baseConfigName = extractKubernetesBaseConfigName(config.getName());
                    String keyName = role + "." + subgroup + "." + baseConfigName;
                    config.setConfigCategory("kubernetes"); // 标记为Kubernetes配置
                    configMapA.put(keyName, config);
                } else {
                    configMapA.put(config.getName(), config);
                }
            } else {
                configMapA.put(config.getName(), config);
            }
        }

        // 处理configB
        for (ServiceConfig config : configListB) {
            if (isKubernetesConfig(config)) {
                String configGroup = config.getConfigGroup();
                String role = getKubernetesRole(configGroup);
                String subgroup = getKubernetesSubgroup(configGroup);

                if (role != null && subgroup != null) {
                    String baseConfigName = extractKubernetesBaseConfigName(config.getName());
                    String keyName = role + "." + subgroup + "." + baseConfigName;
                    config.setConfigCategory("kubernetes"); // 标记为Kubernetes配置
                    configMapB.put(keyName, config);
                } else {
                    configMapB.put(config.getName(), config);
                }
            } else {
                configMapB.put(config.getName(), config);
            }
        }

        // 合并所有配置项键
        Set<String> allConfigKeys = new HashSet<>();
        allConfigKeys.addAll(configMapA.keySet());
        allConfigKeys.addAll(configMapB.keySet());

        // 结果映射：分组名 -> 配置项列表
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        // 处理每个配置项
        for (String key : allConfigKeys) {
            ServiceConfig configA_item = configMapA.get(key);
            ServiceConfig configB_item = configMapB.get(key);

            // 检查是否有差异
            boolean isDifferent = configA_item == null || configB_item == null ||
                    !Objects.equals(configA_item.getValue(), configB_item.getValue());

            // 如果只显示差异项且没有差异，则跳过
            if (Boolean.TRUE.equals(showOnlyDifferences) && !isDifferent) {
                continue;
            }

            // 确定此配置项的分组
            String groupName;
            boolean isKubernetesConfig = false;

            if (configA_item != null && "kubernetes".equals(configA_item.getConfigCategory())) {
                isKubernetesConfig = true;
                // 从组合键中提取角色和子组
                String[] parts = key.split("\\.");
                if (parts.length >= 3) {
                    String role = parts[0];
                    String subgroup = parts[1];
                    groupName = Constants.KUBERNETES_CONFIG_PREFIX + subgroup + "." + role;
                } else {
                    // 如果键格式异常，使用configGroup
                    groupName = configA_item.getConfigGroup();
                }
            } else if (configB_item != null && "kubernetes".equals(configB_item.getConfigCategory())) {
                isKubernetesConfig = true;
                // 从组合键中提取角色和子组
                String[] parts = key.split("\\.");
                if (parts.length >= 3) {
                    String role = parts[0];
                    String subgroup = parts[1];
                    groupName = Constants.KUBERNETES_CONFIG_PREFIX + subgroup + "." + role;
                } else {
                    // 如果键格式异常，使用configGroup
                    groupName = configB_item.getConfigGroup();
                }
            } else {
                // 普通配置项，使用configTargetRoles或GENERAL作为分组
                ServiceConfig config = configA_item != null ? configA_item : configB_item;
                groupName = (config != null && config.getConfigTargetRoles() != null) ? config.getConfigTargetRoles()
                        : Constants.GENERAL;
            }

            // 创建对比项
            Map<String, Object> compareItem = new HashMap<>();

            // 设置名称（对于Kubernetes配置，使用不带前缀的基础名称）
            if (isKubernetesConfig) {
                String baseConfigName = key.substring(key.lastIndexOf('.') + 1);
                compareItem.put("name", baseConfigName);
            } else {
                ServiceConfig config = configA_item != null ? configA_item : configB_item;
                String configName = (config != null && config.getName() != null) ? config.getName() : key;
                compareItem.put("name", configName);
            }

            // 添加差异标记
            compareItem.put("isDifferent", isDifferent);

            // 添加版本值
            compareItem.put(String.valueOf(configA.getConfigVersion()),
                    configA_item != null ? configA_item.getValue() : null);
            compareItem.put(String.valueOf(configB.getConfigVersion()),
                    configB_item != null ? configB_item.getValue() : null);

            // 添加到对应分组
            result.computeIfAbsent(groupName, k -> new ArrayList<>()).add(compareItem);
        }

        return result;
    }

    @Override
    public boolean delServiceInstance(Integer serviceInstanceId) {
        if (hasRunningRoleInstance(serviceInstanceId)) {
            throw new RuntimeException(Status.EXIT_RUNNING_ROLE_INSTANCE.getMsg());
        }
        List<ClusterServiceInstanceRoleGroupDTO> roleGroupDTOList = roleGroupService
                .listRoleGroupByServiceInstanceId(serviceInstanceId);
        List<ClusterServiceInstanceRoleGroup> roleGroups = clusterServiceInstanceRoleGroupConverter
                .dtoListToEntityList(roleGroupDTOList);
        List<Integer> roleGroupIds = roleGroups.stream().map(ClusterServiceInstanceRoleGroup::getId)
                .toList();
        // List<ClusterServiceRoleGroupConfig> roleGroupConfigList =
        // roleGroupConfigService
        // .listRoleGroupConfigsByRoleGroupIds(roleGroupIds);
        List<ClusterServiceRoleInstanceDTO> roleInstanceDTOList = roleInstanceService
                .getServiceRoleInstanceListByServiceId(serviceInstanceId);
        List<ClusterServiceRoleInstanceEntity> roleInstanceList = clusterServiceRoleInstanceConverter
                .dtoListToEntityList(roleInstanceDTOList);
        ClusterServiceInstanceEntity clusterServiceInstance = getById(serviceInstanceId);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterServiceInstance.getClusterId());

        if (Constants.KUBERNETES_MODE.equals(clusterInfo.getDepType())) {
            List<String> serviceRoleList = roleInstanceList.stream()
                    .map(ClusterServiceRoleInstanceEntity::getServiceRoleName).distinct()
                    .toList();
            for (String serviceRoleName : serviceRoleList) {
                KubernetesServiceStopHandler kubernetesServiceStopHandler = new KubernetesServiceStopHandler();
                ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
                serviceRoleInfo.setClusterId(clusterServiceInstance.getClusterId());
                serviceRoleInfo.setParentName(clusterServiceInstance.getServiceName());
                serviceRoleInfo.setName(serviceRoleName);
                try {
                    kubernetesServiceStopHandler.handlerRequest(serviceRoleInfo);
                    log.info("remove {} deployment success", serviceRoleName);
                } catch (Exception e) {
                    log.error("remove {} deployment failed", serviceRoleName);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

        // del role group
        roleGroupService.removeByIds(roleGroupIds);
        // del role group config
        // List<Integer> configIds =
        // roleGroupConfigList.stream().map(ClusterServiceRoleGroupConfig::getId).collect(Collectors.toList());
        // roleGroupConfigService.removeByIds(configIds);
        // del service role instance
        if (!roleInstanceList.isEmpty()) {
            List<String> roleInsIds = roleInstanceList.stream().map(e -> e.getId().toString())
                    .toList();
            roleInstanceService.deleteServiceRole(roleInsIds);
        }
        // del web uis
        webuisService.removeByServiceInsId(serviceInstanceId);

        // del service instance
        removeById(serviceInstanceId);
        return true;
    }

    @Override
    public List<ClusterServiceInstanceDTO> listRunningServiceInstance(Integer clusterId) {
        List<ClusterServiceInstanceEntity> entities = getMapper().selectRunningServicesByClusterId(clusterId);
        return clusterServiceInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public boolean hasRunningRoleInstance(Integer serviceInstanceId) {
        // 检查是否有运行中的角色实例
        // return roleInstanceService.countByServiceId(serviceInstanceId) > 0;
        List<ClusterServiceRoleInstanceDTO> roleInstanceDTOList = roleInstanceService
                .getServiceRoleInstanceListByServiceId(serviceInstanceId);
        List<ClusterServiceRoleInstanceEntity> roleInstances = clusterServiceRoleInstanceConverter
                .dtoListToEntityList(roleInstanceDTOList);
        return roleInstances != null && !roleInstances.isEmpty();
    }

    @Override
    public Boolean hasRoleInstance(Integer clusterId, String serviceName) {
        // 先获取服务实例ID
        ClusterServiceInstanceDTO serviceInstance = getServiceInstanceByClusterIdAndServiceName(clusterId,
                serviceName);
        if (serviceInstance == null) {
            return false;
        }

        // 查询该服务实例下是否有角色实例
        // 使用现有的方法来判断是否有角色实例
        List<ClusterServiceRoleInstanceDTO> roleInstanceDTOList = roleInstanceService
                .getServiceRoleInstanceListByServiceId(serviceInstance.id());
        return roleInstanceDTOList != null && !roleInstanceDTOList.isEmpty();
    }

    /**
     * 生成客户端配置路径
     * 
     * @param clusterInfo 集群信息
     * @param serviceName 服务名称
     * @return 配置文件路径
     */
    private String generateClientConfigPath(ClusterInfoDTO clusterInfo, String serviceName) {
        // 根据服务类型生成对应的客户端配置文件路径
        String configDir = "/opt/datasophon/" + clusterInfo.clusterCode() + "/config/" + serviceName.toLowerCase();

        return switch (serviceName.toUpperCase()) {
            case "HADOOP", "HDFS", "YARN" -> configDir + "/hadoop-client-config.tar.gz";
            case "HIVE" -> configDir + "/hive-client-config.tar.gz";
            case "SPARK" -> configDir + "/spark-client-config.tar.gz";
            case "FLINK" -> configDir + "/flink-client-config.tar.gz";
            case "KAFKA" -> configDir + "/kafka-client-config.tar.gz";
            case "HBASE" -> configDir + "/hbase-client-config.tar.gz";
            default -> configDir + "/" + serviceName.toLowerCase() + "-client-config.tar.gz";
        };
    }

    @Override
    public ConnectionInfo getConnectionInfo(Integer serviceInstanceId) {
        // 构建缓存键，使用serviceInstanceId作为唯一标识
        String cacheKey = "connectionInfo:" + serviceInstanceId;

        // 先从缓存中获取
        ConnectionInfo connectionInfo = CONNECTION_INFO_CACHE.get(cacheKey);
        if (connectionInfo != null) {
            log.info("从缓存获取服务[{}]的连接信息", serviceInstanceId);
            return connectionInfo;
        }

        // 缓存中没有，执行原逻辑获取连接信息
        log.info("缓存中无数据，开始获取服务[{}]的连接信息", serviceInstanceId);

        // 获取服务实例信息
        ClusterServiceInstanceEntity serviceInstance = getById(serviceInstanceId);
        if (serviceInstance == null) {
            throw new RuntimeException(Status.SERVICE_NOT_FOUND.getMsg());
        }

        // 获取集群ID
        Integer clusterId = serviceInstance.getClusterId();

        // 获取服务名称
        String serviceName = serviceInstance.getServiceName();

        // 使用策略模式获取对应服务的连接信息
        ServiceRoleStrategy strategy = ServiceRoleStrategyContext.getServiceRoleHandler(serviceName);
        if (strategy == null) {
            log.warn("没有找到服务{}的连接信息处理策略", serviceName);
            throw new RuntimeException("暂不支持该服务的连接信息");
        }

        // 获取连接信息
        Map.Entry<String, Map<String, String>> serviceConfigMap = strategy.getServiceConfigMap(serviceInstanceId);
        Map<String, String> configMap = serviceConfigMap.getValue();
        String serviceHome = serviceConfigMap.getKey();
        connectionInfo = strategy.getConnectionInfo(clusterId, serviceInstanceId, serviceHome, configMap);

        // 检查是否有有效的连接信息（使用新的InfoItem列表结构）
        if (connectionInfo == null
                || (connectionInfo.getBasicInfoItems() == null || connectionInfo.getBasicInfoItems().isEmpty())) {
            log.warn("服务{}未提供连接信息", serviceName);
            throw new RuntimeException("该服务未提供连接信息");
        }

        String camelServiceName = toCamelCase(serviceName);
        // 如果标题和文件名未设置，则根据服务名称设置默认值
        if (connectionInfo.getJavaTitle() == null) {
            connectionInfo.setJavaTitle(camelServiceName + " Java连接示例");
        }
        if (connectionInfo.getPythonTitle() == null) {
            connectionInfo.setPythonTitle(camelServiceName + " Python连接示例");
        }
        if (connectionInfo.getCommandTitle() == null) {
            connectionInfo.setCommandTitle(camelServiceName + "常用命令");
        }
        if (connectionInfo.getJavaFileName() == null) {
            // 将服务名转换为驼峰命名(首字母大写，其余小写)
            connectionInfo.setJavaFileName(camelServiceName + "Example.java");
        }
        if (connectionInfo.getPythonFileName() == null) {
            // 将服务名转换为驼峰命名(首字母小写，其余小写)
            String lowercaseServiceName = serviceName.toLowerCase();
            connectionInfo.setPythonFileName(lowercaseServiceName + "_example.py");
        }
        if (connectionInfo.getServiceHome() == null) {
            connectionInfo.setServiceHome(serviceHome);
        }

        // 将获取到的连接信息放入缓存
        CONNECTION_INFO_CACHE.put(cacheKey, connectionInfo);
        log.info("将服务[{}]的连接信息存入缓存，缓存时间5秒", serviceInstanceId);

        return connectionInfo;
    }

    /**
     * 将全大写的服务名转换为驼峰命名格式
     * 例如：HDFS -> Hdfs, HIVESERVER2 -> Hiveserver2
     *
     * @param serviceName 服务名称
     * @return 驼峰命名格式的服务名
     */
    private String toCamelCase(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            return "";
        }
        // 先转为小写
        String lowercase = serviceName.toLowerCase();
        // 首字母大写
        return Character.toUpperCase(lowercase.charAt(0)) + lowercase.substring(1);
    }

    // 注意：标准CRUD方法已由IService提供，这里不需要重复实现

    @Override
    public List<ClusterServiceInstanceDTO> getAllServiceInstances() {
        List<ClusterServiceInstanceEntity> entities = list();
        return clusterServiceInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public void updateServiceInstanceState(Integer serviceInstanceId, ServiceState serviceState) {
        ClusterServiceInstanceEntity entity = getById(serviceInstanceId);
        if (entity != null) {
            entity.setServiceState(serviceState);
            updateById(entity);
        }
    }

    @Override
    public boolean existsByFrameServiceId(Integer frameServiceId) {
        if (frameServiceId == null) {
            return false;
        }
        
        return getMapper().existsByFrameServiceId(frameServiceId);
    }
}
