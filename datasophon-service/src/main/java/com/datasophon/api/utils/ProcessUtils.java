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

package com.datasophon.api.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONArray;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfigEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.common.enums.ServiceRoleState;
import com.mybatisflex.core.query.QueryChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工具类
 * 重构后只包含纯工具方法，不涉及业务逻辑
 * 业务逻辑已移至相应的服务类中：
 * - ServiceInstallationService: 服务安装相关
 * - CommandExecutionService: 命令执行管理
 * - ClusterVariableManagementService: 集群变量管理
 * - ServiceStateManagementService: 服务状态管理
 */
public class ProcessUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

    /**
     * 获取异常信息的字符串表示
     */
    public static String getExceptionMessage(Exception ex) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(out);
        ex.printStackTrace(pout);
        String ret = out.toString();
        pout.close();
        try {
            out.close();
        } catch (Exception ignored) {
        }
        return ret;
    }

    /**
     * 获取服务配置
     */
    public static List<ServiceConfig> getServiceConfig(ClusterServiceRoleGroupConfigEntity config) {
        return JSONArray.parseArray(config.getConfigJson(), ServiceConfig.class);
    }

    /**
     * 获取服务配置列表（DTO版本）
     * 
     * @param config 角色组配置DTO
     * @return 服务配置列表
     */
    public static List<ServiceConfig> getServiceConfig(ClusterServiceRoleGroupConfigDTO config) {
        return JSONArray.parseArray(config.configJson(), ServiceConfig.class);
    }

    /**
     * 创建服务配置
     */
    public static ServiceConfig createServiceConfig(String configName, Object configValue, String type) {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName(configName);
        serviceConfig.setLabel(configName);
        serviceConfig.setValue(configValue);
        serviceConfig.setRequired(true);
        serviceConfig.setHidden(false);
        serviceConfig.setType(type);
        return serviceConfig;
    }

    /**
     * 获取所有集群ID和类型的映射
     */
    public static Map<Long, ClusterType> getAllClusterIdAndType() {
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        return clusterInfoService.list().stream()
                .collect(Collectors.toMap(ClusterInfoEntity::getId, ClusterInfoEntity::getDepType));
    }

    /**
     * 并集：左边集合与右边集合合并
     */
    public static void addAll(List<ServiceConfig> left, List<ServiceConfig> right) {
        if (left == null) {
            return;
        }
        if (right == null) {
            return;
        }
        // 使用LinkedList方便插入和删除
        List<ServiceConfig> res = new LinkedList<>(right);
        Set<String> set = new HashSet<>();
        //
        for (ServiceConfig item : left) {
            set.add(item.getName());
        }
        // 迭代器遍历listA
        for (ServiceConfig item : res) {
            // 如果set中包含id则remove
            if (!set.contains(item.getName())) {
                left.add(item);
            }
        }
    }

    /**
     * 转换为Map
     */
    public static Map<String, ServiceConfig> translateToMap(List<ServiceConfig> list) {
        return list.stream()
                .collect(Collectors.toMap(ServiceConfig::getName, serviceConfig -> serviceConfig, (v1, v2) -> v1));
    }

    /**
     * 获取部署模式
     */
    public static ClusterType getDepMode(Long clusterId) {
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        return clusterInfoService.getById(clusterId).getDepType();
    }

    /**
     * 是否启用Kerberos
     */
    public static Boolean enableKerberos(Long clusterId, String serviceParentName) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        return Boolean.parseBoolean(globalVariables.get("${enable" + serviceParentName + "Kerberos}"));
    }

    /**
     * 是否启用Ranger插件
     */
    public static boolean enableRangerPlugin(Long clusterId, String serviceParentName) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        return Boolean.parseBoolean(globalVariables.get("${enable" + serviceParentName + "Plugin}"));
    }

    /**
     * 获取指定服务角色的主机名
     * 如果有多个实例，返回第一个运行中的实例
     */
    public static String getServiceRoleHostname(Long clusterId, String serviceName, String servicRoleName) {
        // 查询指定服务角色的实例
        List<ClusterServiceRoleInstanceEntity> serviceRoles = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getServiceName).eq(serviceName)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(servicRoleName)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.RUNNING)
                .list();

        if (serviceRoles != null && !serviceRoles.isEmpty()) {
            // 返回第一个运行中的实例
            return serviceRoles.getFirst().getHostname();
        }

        // 如果没有运行中的实例，尝试获取任意状态的实例
        ClusterServiceRoleInstanceEntity anyRole = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getServiceName).eq(serviceName)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(servicRoleName)
                .one();

        return anyRole != null ? anyRole.getHostname() : null;
    }

    /**
     * 从URL中提取端口号
     */
    public static Integer extractPortFromUrl(String url) {
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
    public static String replacePortInUrl(String url, String newPort) {
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

    /**
     * HDFS EC方法 - 已迁移到HdfsEcService
     * 此方法保留作为兼容性适配器，实际逻辑已移至HdfsEcServiceImpl
     * 
     * @deprecated 使用 HdfsEcService.handleHdfsEcCommand() 代替
     */
    @Deprecated
    public static void hdfsEcMethond(Long serviceInstanceId, java.util.TreeSet<String> hosts,
            String type, String roleName) {
        logger.info("调用HDFS EC方法（已迁移至HdfsEcService）: serviceInstanceId={}, type={}, roleName={}", 
                serviceInstanceId, type, roleName);
        
        try {
            // 获取HdfsEcService实例并委托调用
            com.datasophon.api.service.HdfsEcService hdfsEcService = 
                    cn.hutool.extra.spring.SpringUtil.getBean(com.datasophon.api.service.HdfsEcService.class);
            
            hdfsEcService.handleHdfsEcCommand(serviceInstanceId, hosts, type, roleName);
            
        } catch (Exception e) {
            logger.error("HDFS EC操作失败: serviceInstanceId={}", serviceInstanceId, e);
            throw new RuntimeException("HDFS EC操作失败", e);
        }
    }

    /**
     * 为集群创建Service Actor（已改造为无操作）
     * 原MasterServiceActor功能已迁移到Spring Service，不再需要创建Actor
     * 
     * @param clusterInfo 集群信息实体
     */
    public static void createServiceActor(ClusterInfoEntity clusterInfo) {
        // Actor功能已迁移到Spring Service，此方法保留仅为兼容性
        logger.info("集群 {} 无需创建Actor（已使用Spring Service替代）", clusterInfo.getClusterCode());
    }
}