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
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.enums.Status;
import com.datasophon.api.k8s.handler.K8sServiceStopHandler;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterAlertHistoryService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceDashboardService;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.api.utils.CommonUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.Result;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private ClusterServiceDashboardService dashboardService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ClusterAlertHistoryService alertHistoryService;

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
        return this.getOne(new QueryWrapper<ClusterServiceInstanceEntity>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.SERVICE_NAME, serviceName));
    }

    @Override
    public String getServiceConfigByClusterIdAndServiceName(Integer clusterId, String serviceName) {
        return serviceInstanceMapper.getServiceConfigByClusterIdAndServiceName(clusterId, serviceName);
    }

    @Override
    public List<ClusterServiceInstanceEntity> listAll(Integer clusterId) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        List<ClusterServiceInstanceEntity> list = this.list(new QueryWrapper<ClusterServiceInstanceEntity>()
                .eq(Constants.CLUSTER_ID, clusterId).orderByAsc(Constants.SORT_NUM));
        for (ClusterServiceInstanceEntity serviceInstance : list) {
            serviceInstance.setServiceStateCode(serviceInstance.getServiceState().getValue());
            boolean needUpdate = false;
            // 查询dashboard
            ClusterServiceDashboard dashboard = dashboardService.getOne(new QueryWrapper<ClusterServiceDashboard>()
                    .eq(Constants.SERVICE_NAME, serviceInstance.getServiceName()));
            if (Objects.nonNull(dashboard)) {
                String dashboardUrl = PlaceholderUtils.replacePlaceholders(dashboard.getDashboardUrl(), globalVariables,
                        Constants.REGEX_VARIABLE);
                serviceInstance.setDashboardUrl(dashboardUrl);
            }
            // 查询告警数量
            long alertNum = alertHistoryService.count(new QueryWrapper<ClusterAlertHistory>()
                    .eq(Constants.SERVICE_INSTANCE_ID, serviceInstance.getId()).eq(Constants.IS_ENABLED, 1));
            serviceInstance.setAlertNum(alertNum);
            List<ClusterServiceRoleInstanceEntity> totalRoleList = roleInstanceService.lambdaQuery()
                    .eq(ClusterServiceRoleInstanceEntity::getServiceId, serviceInstance.getId())
                    .list();
            if (Objects.nonNull(totalRoleList) && totalRoleList.isEmpty()) {
                serviceInstance.setServiceState(ServiceState.WAIT_INSTALL);
                needUpdate = true;
            }

            // 查询停止状态角色
            List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceService.lambdaQuery()
                    .eq(ClusterServiceRoleInstanceEntity::getServiceId, serviceInstance.getId())
                    .eq(ClusterServiceRoleInstanceEntity::getServiceRoleState, ServiceRoleState.STOP)
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
            List<ClusterServiceRoleInstanceEntity> alarmRoleList = roleInstanceService.lambdaQuery()
                    .eq(ClusterServiceRoleInstanceEntity::getServiceId, serviceInstance.getId())
                    .eq(ClusterServiceRoleInstanceEntity::getServiceRoleState, ServiceRoleState.EXISTS_ALARM)
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
        return list;
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

    @Override
    public Result configVersionCompare(Integer serviceInstanceId, Integer roleGroupId, Boolean showOnlyDifferences) {
        List<ClusterServiceRoleGroupConfig> list = roleGroupConfigService
                .list(new QueryWrapper<ClusterServiceRoleGroupConfig>()
                        .eq(Constants.ROLE_GROUP_ID, roleGroupId)
                        .orderByDesc(Constants.CONFIG_VERSION).last("limit 2"));
        
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
        
        // 创建配置项映射，用于快速查找
        Map<String, Object> configMapB = new HashMap<>();
        for (ServiceConfig config : configListB) {
            configMapB.put(config.getName(), config.getValue());
        }
        
        // 处理configA中的配置项
        List<ServiceConfig> serviceConfigList = new ArrayList<>();
        for (ServiceConfig configA_item : configListA) {
            // 设置服务名称，用于排序
            configA_item.setServiceName(configA.getServiceName());
            
            // 检查是否有差异
            Object valueB = configMapB.get(configA_item.getName());
            boolean isDifferent = !Objects.equals(configA_item.getValue(), valueB);
            
            // 根据showOnlyDifferences参数决定是否只添加有差异的配置项
            if (!Boolean.TRUE.equals(showOnlyDifferences) || isDifferent) {
                // 添加差异标记
                ServiceConfig configToAdd = configA_item;
                configToAdd.setConfigType(isDifferent ? "DIFFERENT" : "SAME"); // 使用configType字段标记差异状态
                serviceConfigList.add(configToAdd);
            }
        }
        
        // 处理configB中有但configA中没有的项（这些项本身就是差异项）
        for (ServiceConfig configB_item : configListB) {
            String name = configB_item.getName();
            // 如果configA中已经包含该项，则跳过
            if (configListA.stream().anyMatch(c -> c.getName().equals(name))) {
                continue;
            }
            
            // 这些项始终是差异项，无论showOnlyDifferences如何都应该添加
            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setName(name);
            serviceConfig.setValue(null); // configA中不存在该项
            
            // 复制configB_item中的其他属性
            serviceConfig.setLabel(configB_item.getLabel());
            serviceConfig.setDescription(configB_item.getDescription());
            serviceConfig.setRequired(configB_item.isRequired());
            serviceConfig.setType(configB_item.getType());
            serviceConfig.setConfigurableInWizard(configB_item.isConfigurableInWizard());
            serviceConfig.setDefaultValue(configB_item.getDefaultValue());
            serviceConfig.setMinValue(configB_item.getMinValue());
            serviceConfig.setMaxValue(configB_item.getMaxValue());
            serviceConfig.setUnit(configB_item.getUnit());
            serviceConfig.setHidden(configB_item.isHidden());
            serviceConfig.setSelectValue(configB_item.getSelectValue());
            serviceConfig.setConfigType("DIFFERENT"); // 标记为差异项
            serviceConfig.setConfigWithKerberos(configB_item.isConfigWithKerberos());
            serviceConfig.setConfigWithRack(configB_item.isConfigWithRack());
            serviceConfig.setConfigWithHA(configB_item.isConfigWithHA());
            serviceConfig.setSeparator(configB_item.getSeparator());
            serviceConfig.setOpen(configB_item.getOpen());
            serviceConfig.setClose(configB_item.getClose());
            serviceConfig.setConfigTargetRoles(configB_item.getConfigTargetRoles());
            serviceConfig.setConfigCategory(configB_item.getConfigCategory());
            serviceConfig.setConfigGroup(configB_item.getConfigGroup());
            serviceConfig.setConfigLevel(configB_item.getConfigLevel());
            serviceConfig.setTemplateName(configB_item.getTemplateName());
            serviceConfig.setTemplateContent(configB_item.getTemplateContent());
            serviceConfig.setDisplayName(configB_item.getDisplayName());
            serviceConfig.setHeightMultiple(configB_item.getHeightMultiple());
            
            // 设置服务名称，用于排序
            serviceConfig.setServiceName(configA.getServiceName());
            
            // 将差异信息和版本值添加到serviceConfigList
            serviceConfigList.add(serviceConfig);
        }
        
        // 如果没有配置项，返回空结果
        if (serviceConfigList.isEmpty()) {
            return Result.success(new HashMap<>());
        }
        
        // 使用公共分组逻辑进行分组
        String serviceName = configA.getServiceName();
        Map<String, List<ServiceConfig>> groupedConfigs = CommonUtils.groupByConfigTargetRoleOrCommon(serviceName, serviceConfigList);
        
        // 将分组后的数据转换为前端需要的格式
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        for (Map.Entry<String, List<ServiceConfig>> entry : groupedConfigs.entrySet()) {
            String groupName = entry.getKey();
            List<ServiceConfig> configs = entry.getValue();
            
            List<Map<String, Object>> configItems = new ArrayList<>();
            for (ServiceConfig config : configs) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", config.getName());
                
                // 设置是否有差异
                boolean isDifferent = "DIFFERENT".equals(config.getConfigType());
                item.put("isDifferent", isDifferent);
                
                // 添加版本值
                Object valueB = configMapB.get(config.getName());
                item.put(String.valueOf(configA.getConfigVersion()), config.getValue());
                item.put(String.valueOf(configB.getConfigVersion()), valueB);
                
                configItems.add(item);
            }
            
            result.put(groupName, configItems);
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

        if (Constants.K8S_MODE.equals(clusterInfo.getDepType())) {
            List<String> serviceRoleList = roleInstanceList.stream()
                    .map(ClusterServiceRoleInstanceEntity::getServiceRoleName).distinct().collect(Collectors.toList());
            for (String serviceRoleName : serviceRoleList) {
                K8sServiceStopHandler k8sServiceStopHandler = new K8sServiceStopHandler();
                ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
                serviceRoleInfo.setClusterId(clusterServiceInstance.getClusterId());
                serviceRoleInfo.setParentName(clusterServiceInstance.getServiceName());
                serviceRoleInfo.setName(serviceRoleName);
                try {
                    k8sServiceStopHandler.handlerRequest(serviceRoleInfo);
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
        return this.list(new QueryWrapper<ClusterServiceInstanceEntity>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.SERVICE_STATE, ServiceState.RUNNING));
    }

    public boolean hasRunningRoleInstance(Integer serviceInstanceId) {
        List<ClusterServiceRoleInstanceEntity> list = roleInstanceService
                .getRunningServiceRoleInstanceListByServiceId(serviceInstanceId);
        return !list.isEmpty();
    }

    @Override
    public Boolean hasRoleInstance(Integer clusterId, String serviceName) {
        long count = roleInstanceService
                .count(new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                        .eq(Constants.CLUSTER_ID, clusterId)
                        .eq(Constants.SERVICE_NAME, serviceName));
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
