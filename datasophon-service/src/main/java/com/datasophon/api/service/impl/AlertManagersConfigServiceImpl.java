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

import cn.hutool.http.HttpUtil;
import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.scheduler.AsyncTaskScheduler;
import com.datasophon.api.service.AlertManagersConfigService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfigEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AlertManager配置管理服务实现
 * 替代AlertManagersActor，处理AlertManager配置生成
 * 从旧的Actor模型迁移到HTTP REST API + db-scheduler架构
 * 
 * @author DataSophon Team
 */
@Service
public class AlertManagersConfigServiceImpl implements AlertManagersConfigService {

    private static final Logger logger = LoggerFactory.getLogger(AlertManagersConfigServiceImpl.class);
    
    private static final String SERVICENAME = "ALERTMANAGER";
    private static final String ALERTMANAGER_RELOAD_PATH = "/-/reload";
    private static final int ALERTMANAGER_PORT = 9093;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;
    
    @Autowired
    private ClusterInfoService clusterInfoService;
    
    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;
    
    @Autowired
    private ClusterServiceRoleGroupConfigConverter roleGroupConfigConverter;
    
    @Autowired
    private AsyncTaskScheduler asyncTaskScheduler;

    @Override
    public void generateAlertManagerConfig() {
        // 使用 db-scheduler 异步执行，避免Spring @Async线程池卡死问题
        asyncTaskScheduler.executeAsync("alert-manager-config-gen", () -> {
            try {
                logger.info("开始生成AlertManager配置（db-scheduler异步任务）");
                generateConfigInternal();
                logger.info("AlertManager配置生成任务提交完成");
            } catch (Exception e) {
                logger.error("生成AlertManager配置失败", e);
                throw new RuntimeException("生成AlertManager配置失败", e);
            }
        });
    }
    
    /**
     * 内部实际执行配置生成逻辑
     * 从AlertManagersActor恢复的业务逻辑
     */
    private void generateConfigInternal() {
        // 更新所有集群的通知组
        // 获取alertManager的所有实例
        List<ClusterServiceRoleInstanceDTO> roleInstanceEntitys = roleInstanceService
                .listServiceRoleByName("AlertManager");
        
        if (CollectionUtils.isEmpty(roleInstanceEntitys)) {
            logger.warn("未找到任何AlertManager实例，跳过配置生成");
            return;
        }
        
        logger.info("找到 {} 个AlertManager实例，开始生成配置", roleInstanceEntitys.size());

        // 分集群更新
        Map<Long, List<ClusterServiceRoleInstanceDTO>> clusterRoles = roleInstanceEntitys.stream()
                .collect(Collectors.groupingBy(ClusterServiceRoleInstanceDTO::clusterId));
        
        for (Map.Entry<Long, List<ClusterServiceRoleInstanceDTO>> entry : clusterRoles.entrySet()) {
            Long clusterId = entry.getKey();
            List<ClusterServiceRoleInstanceDTO> instances = entry.getValue();
            
            try {
                generateConfigForCluster(clusterId, instances);
            } catch (Exception e) {
                logger.error("为集群 {} 生成AlertManager配置失败", clusterId, e);
                // 继续处理其他集群
            }
        }
    }
    
    /**
     * 为特定集群生成AlertManager配置
     */
    private void generateConfigForCluster(Long clusterId, List<ClusterServiceRoleInstanceDTO> instances) {
        // 查询集群框架信息
        ClusterInfoDTO clusterInfo = clusterInfoService.getClusterById(clusterId);
        if (clusterInfo == null) {
            logger.warn("未找到集群信息: clusterId={}", clusterId);
            return;
        }
        
        String servicePackageName = PackageUtils.getServiceDcPackageName(
                clusterInfo.clusterFrame(), SERVICENAME);
        
        logger.info("集群 {} 框架: {}, AlertManager包名: {}", 
                clusterId, clusterInfo.clusterFrame(), servicePackageName);

        // 分服务实例更新，一般alertmanager只需要一个实例
        for (ClusterServiceRoleInstanceDTO alertManager : instances) {
            try {
                generateConfigForInstance(clusterId, alertManager, servicePackageName);
            } catch (Exception e) {
                logger.error("为AlertManager实例 {} 生成配置失败", alertManager.hostname(), e);
                // 继续处理其他实例
            }
        }
    }
    
    /**
     * 为特定AlertManager实例生成配置
     */
    private void generateConfigForInstance(Long clusterId, 
                                           ClusterServiceRoleInstanceDTO alertManager,
                                           String servicePackageName) throws Exception {
        logger.info("开始为AlertManager实例生成配置: hostname={}, roleGroupId={}", 
                alertManager.hostname(), alertManager.roleGroupId());
        
        // 通过实例的配置组id查询配置的详细信息
        ClusterServiceRoleGroupConfigDTO roleGroupConfigDto = roleGroupConfigService
                .getConfigByRoleGroupId(alertManager.roleGroupId());
        
        if (roleGroupConfigDto == null) {
            logger.warn("未找到角色组配置: roleGroupId={}", alertManager.roleGroupId());
            return;
        }

        // 使用MapStruct Converter进行转换 - 符合架构规范
        ClusterServiceRoleGroupConfigEntity roleGroupConfig = roleGroupConfigConverter
                .dtoToEntity(roleGroupConfigDto);

        // 准备配置参数
        Map<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
        ConfigGroupUtils.generateConfigFileMap(configFileMap, roleGroupConfig, clusterId);

        // 准备调用参数
        ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setConfigFileMap(configFileMap);
        serviceRoleInfo.setHostname(alertManager.hostname());
        serviceRoleInfo.setDecompressPackageName(servicePackageName);

        // 执行配置生成操作
        ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
        ExecResult execResult = configureHandler.handlerRequest(serviceRoleInfo);

        // 返回结果处理
        if (execResult.getExecResult()) {
            logger.info("AlertManager配置生成成功，准备重新加载配置: hostname={}", alertManager.hostname());
            // 刷新配置 - 使用HTTP POST请求到AlertManager的reload端点
            reloadAlertManagerConfig(alertManager.hostname());
        } else {
            logger.error("AlertManager配置生成失败: hostname={}, error={}", 
                    alertManager.hostname(), execResult.getExecOut());
        }
    }
    
    /**
     * 重新加载AlertManager配置
     * 通过HTTP POST请求到AlertManager的 /-/reload 端点
     */
    private void reloadAlertManagerConfig(String hostname) {
        try {
            String reloadUrl = String.format("http://%s:%d%s", 
                    hostname, ALERTMANAGER_PORT, ALERTMANAGER_RELOAD_PATH);
            
            logger.info("发送配置重载请求到AlertManager: {}", reloadUrl);
            String response = HttpUtil.post(reloadUrl, "");
            
            logger.info("AlertManager配置重载成功: hostname={}, response={}", hostname, response);
        } catch (Exception e) {
            logger.error("重新加载AlertManager配置失败: hostname={}", hostname, e);
            throw new RuntimeException("重新加载AlertManager配置失败: " + hostname, e);
        }
    }
}

