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
import com.datasophon.common.enums.Status;
import com.datasophon.api.kubernetes.handler.KubernetesServiceStopHandler;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceInstanceService;
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
import com.datasophon.api.vo.Result;
import com.datasophon.dao.entity.ClusterAlertHistory;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceDashboard;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.ServiceRoleState;
import com.datasophon.dao.enums.ServiceState;
import com.datasophon.dao.mapper.ClusterServiceInstanceMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

@Service("clusterServiceInstanceService")
@Transactional
@Slf4j
public class ClusterServiceInstanceServiceImpl
        extends
        ServiceImpl<ClusterServiceInstanceMapper, ClusterServiceInstanceEntity>
        implements
        ClusterServiceInstanceService {

    @Autowired
    private ClusterServiceInstanceMapper serviceInstanceMapper;

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

    // 创建一个定时缓存，缓存时间为10秒
    private static final TimedCache<String, ConnectionInfo> CONNECTION_INFO_CACHE = CacheUtil.newTimedCache(5000);

    // 开启缓存定时清理
    static {
        // 每5秒检查一次过期缓存
        CONNECTION_INFO_CACHE.schedulePrune(2000);
    }

    @Override
    public ClusterServiceInstanceEntity getServiceInstanceByClusterIdAndServiceName(Integer clusterId,
            String serviceName) {
        return QueryChain.of(ClusterServiceInstanceEntity.class)
                .where(ClusterServiceInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceInstanceEntity::getServiceName).eq(serviceName)
                .one();
    }

    @Override
    public String getServiceConfigByClusterIdAndServiceName(Integer clusterId, String serviceName) {
        return serviceInstanceMapper.getServiceConfigByClusterIdAndServiceName(clusterId, serviceName);
    }

    @Override
    public List<ClusterServiceInstanceEntity> listAll(Integer clusterId) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);

        // 查询集群下所有服务实例并按排序号升序排列
        List<ClusterServiceInstanceEntity> serviceInstances = QueryChain.of(ClusterServiceInstanceEntity.class)
                .where(ClusterServiceInstanceEntity::getClusterId).eq(clusterId)
                .orderBy(ClusterServiceInstanceEntity::getSortNum).asc()
                .list();

        // 处理每个服务实例
        for (ClusterServiceInstanceEntity serviceInstance : serviceInstances) {
            serviceInstance.setServiceStateCode(serviceInstance.getServiceState().getValue());
            boolean needUpdate = false;

            // 查询仪表盘
            ClusterServiceDashboard dashboard = QueryChain.of(ClusterServiceDashboard.class)
                    .where(ClusterServiceDashboard::getServiceName).eq(serviceInstance.getServiceName())
                    .one();

            if (Objects.nonNull(dashboard)) {
                String dashboardUrl = PlaceholderUtils.replacePlaceholders(dashboard.getDashboardUrl(), globalVariables,
                        Constants.REGEX_VARIABLE);
                serviceInstance.setDashboardUrl(dashboardUrl);
            }

            // 查询启用的告警数量
            long alertNum = QueryChain.of(ClusterAlertHistory.class)
                    .where(ClusterAlertHistory::getServiceInstanceId).eq(serviceInstance.getId())
                    .and(ClusterAlertHistory::getIsEnabled).eq(1)
                    .count();

            serviceInstance.setAlertNum(alertNum);

            // 查询该服务的所有角色实例
            List<ClusterServiceRoleInstanceEntity> totalRoleList = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                    .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstance.getId())
                    .list();

            if (Objects.nonNull(totalRoleList) && totalRoleList.isEmpty()) {
                serviceInstance.setServiceState(ServiceState.WAIT_INSTALL);
                needUpdate = true;
            }

            // 查询停止状态角色
            List<ClusterServiceRoleInstanceEntity> roleList = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                    .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstance.getId())
                    .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.STOP)
                    .list();

            if (Objects.nonNull(roleList) && !roleList.isEmpty()) {
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

            // 查询告警状态角色
            List<ClusterServiceRoleInstanceEntity> alarmRoleList = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                    .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstance.getId())
                    .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.EXISTS_ALARM)
                    .list();

            if (Objects.nonNull(alarmRoleList) && !alarmRoleList.isEmpty()) {
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
            List<ClusterServiceRoleInstanceEntity> obsoleteRoleList = roleInstanceService
                    .getObsoleteService(serviceInstance.getId());

            if (Objects.nonNull(obsoleteRoleList) && obsoleteRoleList.isEmpty()
                    && serviceInstance.getNeedRestart() == NeedRestart.YES) {
                serviceInstance.setNeedRestart(NeedRestart.NO);
                needUpdate = true;
            }

            if (needUpdate) {
                this.updateById(serviceInstance);
            }
        }
        return serviceInstances;
    }

    @Override
    public Result downloadClientConfig(Integer clusterId, String serviceName) {

        return null;
    }

    @Override
    public Result getServiceRoleType(Integer serviceInstanceId) {
        ClusterServiceInstanceEntity serviceInstanceEntity = this.getById(serviceInstanceId);
        Integer frameServiceId = serviceInstanceEntity.getFrameServiceId();
        List<FrameServiceRoleEntity> list = frameServiceRoleService.getAllServiceRoleList(frameServiceId);
        return Result.success(list);
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
    public Result configVersionCompare(Integer serviceInstanceId, Integer roleGroupId, Boolean showOnlyDifferences) {
        List<ClusterServiceRoleGroupConfig> list = QueryChain.of(ClusterServiceRoleGroupConfig.class)
                .where(ClusterServiceRoleGroupConfig::getRoleGroupId).eq(roleGroupId)
                .orderBy(ClusterServiceRoleGroupConfig::getConfigVersion).desc()
                .limit(2)
                .list();

        // 如果没有足够的版本进行比较，返回空结果
        if (list == null || list.size() < 2) {
            return Result.success(new HashMap<>());
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
                groupName = config.getConfigTargetRoles() != null ? config.getConfigTargetRoles() : Constants.GENERAL;
            }

            // 创建对比项
            Map<String, Object> compareItem = new HashMap<>();

            // 设置名称（对于Kubernetes配置，使用不带前缀的基础名称）
            if (isKubernetesConfig) {
                String baseConfigName = key.substring(key.lastIndexOf('.') + 1);
                compareItem.put("name", baseConfigName);
            } else {
                ServiceConfig config = configA_item != null ? configA_item : configB_item;
                compareItem.put("name", config.getName());
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

        return Result.success(result);
    }

    @Override
    public Result delServiceInstance(Integer serviceInstanceId) {
        if (hasRunningRoleInstance(serviceInstanceId)) {
            return Result.error(Status.EXIT_RUNNING_ROLE_INSTANCE.getMsg());
        }
        List<ClusterServiceInstanceRoleGroup> roleGroups = roleGroupService
                .listRoleGroupByServiceInstanceId(serviceInstanceId);
        List<Integer> roleGroupIds = roleGroups.stream().map(ClusterServiceInstanceRoleGroup::getId)
                .collect(Collectors.toList());
        List<ClusterServiceRoleGroupConfig> roleGroupConfigList = roleGroupConfigService
                .listRoleGroupConfigsByRoleGroupIds(roleGroupIds);
        List<ClusterServiceRoleInstanceEntity> roleInstanceList = roleInstanceService
                .getServiceRoleInstanceListByServiceId(serviceInstanceId);
        ClusterServiceInstanceEntity clusterServiceInstance = this.getById(serviceInstanceId);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterServiceInstance.getClusterId());

        if (Constants.KUBERNETES_MODE.equals(clusterInfo.getDepType())) {
            List<String> serviceRoleList = roleInstanceList.stream()
                    .map(ClusterServiceRoleInstanceEntity::getServiceRoleName).distinct().toList();
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
                    return Result.error(e.getMessage());
                }
            }
        }

        // del role group
        roleGroupService.removeByIds(roleGroupIds);
        // del role group config
        roleGroupConfigService
                .removeByIds(roleGroupConfigList.stream().map(ClusterServiceRoleGroupConfig::getId)
                        .collect(Collectors.toList()));
        // del service role instance
        if (!roleInstanceList.isEmpty()) {
            List<String> roleInsIds = roleInstanceList.stream().map(e -> e.getId().toString())
                    .collect(Collectors.toList());
            roleInstanceService.deleteServiceRole(roleInsIds);
        }
        // del web uis
        webuisService.removeByServiceInsId(serviceInstanceId);

        // del service instance
        this.removeById(serviceInstanceId);
        return Result.success();
    }

    @Override
    public List<ClusterServiceInstanceEntity> listRunningServiceInstance(Integer clusterId) {
        return QueryChain.of(ClusterServiceInstanceEntity.class)
                .where(ClusterServiceInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceInstanceEntity::getServiceState).eq(ServiceState.RUNNING)
                .list();
    }

    @Override
    public boolean hasRunningRoleInstance(Integer serviceInstanceId) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstanceId)
                .count() > 0;
    }

    @Override
    public Boolean hasRoleInstance(Integer clusterId, String serviceName) {
        // 先获取服务实例ID
        ClusterServiceInstanceEntity serviceInstance = getServiceInstanceByClusterIdAndServiceName(clusterId,
                serviceName);
        if (serviceInstance == null) {
            return false;
        }

        // 查询该服务实例下是否有角色实例
        long count = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstance.getId())
                .count();

        return count > 0;
    }

    @Override
    public Result getConnectionInfo(Integer serviceInstanceId) {
        // 构建缓存键，使用serviceInstanceId作为唯一标识
        String cacheKey = "connectionInfo:" + serviceInstanceId;

        // 先从缓存中获取
        ConnectionInfo connectionInfo = CONNECTION_INFO_CACHE.get(cacheKey);
        if (connectionInfo != null) {
            log.info("从缓存获取服务[{}]的连接信息", serviceInstanceId);
            return Result.success(connectionInfo);
        }

        // 缓存中没有，执行原逻辑获取连接信息
        log.info("缓存中无数据，开始获取服务[{}]的连接信息", serviceInstanceId);

        // 获取服务实例信息
        ClusterServiceInstanceEntity serviceInstance = this.getById(serviceInstanceId);
        if (serviceInstance == null) {
            return Result.error(Status.SERVICE_NOT_FOUND.getMsg());
        }

        // 获取集群ID
        Integer clusterId = serviceInstance.getClusterId();

        // 获取服务名称
        String serviceName = serviceInstance.getServiceName();

        // 使用策略模式获取对应服务的连接信息
        ServiceRoleStrategy strategy = ServiceRoleStrategyContext.getServiceRoleHandler(serviceName);
        if (strategy == null) {
            log.warn("没有找到服务{}的连接信息处理策略", serviceName);
            return Result.error("暂不支持该服务的连接信息");
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
            return Result.error("该服务未提供连接信息");
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

        return Result.success(connectionInfo);
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

}
