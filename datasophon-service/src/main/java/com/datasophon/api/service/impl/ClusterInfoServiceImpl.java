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

import com.datasophon.api.converter.ClusterInfoConverter;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.enums.Status;
import com.datasophon.api.load.ConfigBean;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.ClusterActor;
import com.datasophon.api.service.*;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ClusterCommand;
import com.datasophon.common.enums.ClusterCommandType;
import com.datasophon.common.model.kubernetes.KubernetesNamespaceDto;

import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.enums.ClusterState;
import com.datasophon.dao.mapper.ClusterInfoMapper;
import com.datasophon.kubernetes.util.KubeUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.ActorRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Objects;

/**
 * 集群信息服务实现类
 * 继承ServiceImpl提供基础CRUD操作，使用Converter进行对象转换
 * 按照架构重构规范，Service层返回DTO，不返回Result
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Slf4j
@Service("clusterInfoService")
@Transactional
public class ClusterInfoServiceImpl extends ServiceImpl<ClusterInfoMapper, ClusterInfoEntity>
        implements ClusterInfoService {

    @Autowired
    private ClusterInfoConverter clusterInfoConverter;

    @Autowired
    private ClusterRoleUserService clusterUserService;

    @Autowired
    private ConfigBean configBean;

    @Autowired
    private FrameServiceService frameServiceService;

    @Autowired
    private ClusterHostService clusterHostService;

    @Autowired
    private ClusterYarnSchedulerService yarnSchedulerService;

    @Autowired
    private ClusterNodeLabelService nodeLabelService;

    @Autowired
    private ClusterQueueCapacityService queueCapacityService;

    @Autowired
    private ClusterRackService rackService;

    @org.springframework.context.annotation.Lazy
    @Autowired
    private ClusterServiceInstanceService clusterServiceInstanceService;

    @Override
    public ClusterInfoDTO getClusterByClusterCode(String clusterCode) {
        ClusterInfoEntity entity = getMapper().getClusterByClusterCode(clusterCode);
        return entity != null ? clusterInfoConverter.entityToDto(entity) : null;
    }

    @Override
    public ClusterInfoDTO saveCluster(ClusterInfoDTO clusterInfoDTO) {
        // DTO转Entity
        ClusterInfoEntity clusterInfo = clusterInfoConverter.dtoToEntity(clusterInfoDTO);

        // 检查集群编码是否已存在
        ClusterInfoEntity existingCluster = getMapper()
                .selectByClusterCode(clusterInfo.getClusterCode());
        if (existingCluster != null) {
            throw new RuntimeException(Status.CLUSTER_CODE_EXISTS.getMsg());
        }

        clusterInfo.setCreateTime(new Date());
        clusterInfo.setCreateBy(Objects.requireNonNull(SecurityUtils.getAuthUser()).getUsername());
        clusterInfo.setClusterState(ClusterState.NEED_CONFIG);

        // 检查集群管理员列表是否为空
        if (clusterInfoDTO.clusterManagerList() == null || clusterInfoDTO.clusterManagerList().isEmpty()) {
            throw new RuntimeException("集群管理员不能为空，请指定至少一个管理员");
        }

        // 保存集群信息
        save(clusterInfo);

        // 从 UserInfoDTO 对象列表中提取用户 ID
        String managerIds = clusterInfoDTO.clusterManagerList().stream()
                .map(user -> user.getId().toString())
                .collect(java.util.stream.Collectors.joining(","));

        // 保存集群管理员关系
        clusterUserService.saveClusterManager(clusterInfo.getId(), managerIds);

        ProcessUtils.createServiceActor(clusterInfo);
        yarnSchedulerService.createDefaultYarnScheduler(clusterInfo.getId());
        nodeLabelService.createDefaultNodeLabel(clusterInfo.getId());
        queueCapacityService.createDefaultQueue(clusterInfo.getId());
        rackService.createDefaultRack(clusterInfo.getId());
        putClusterVariable(clusterInfo);

        return clusterInfoConverter.entityToDto(clusterInfo);
    }

    private void putClusterVariable(ClusterInfoEntity clusterInfo) {
        HashMap<String, String> globalVariables = new HashMap<>();
        List<FrameServiceEntity> frameServiceList = frameServiceService
                .getAllFrameServiceByFrameCode(clusterInfo.getClusterFrame());
        for (FrameServiceEntity frameServiceEntity : frameServiceList) {
            globalVariables.put("${" + frameServiceEntity.getServiceName() + "_HOME}",
                    Constants.INSTALL_PATH + Constants.SLASH + frameServiceEntity.getDecompressPackageName());
        }
        globalVariables.put("${INSTALL_PATH}", Constants.INSTALL_PATH);
        globalVariables.put("${apiHost}", CacheUtils.getString("hostname"));
        globalVariables.put("${apiPort}", configBean.getServerPort());
        globalVariables.put("${HADOOP_HOME}", Constants.INSTALL_PATH + Constants.SLASH
                + PackageUtils.getServiceDcPackageName(clusterInfo.getClusterFrame(), "HDFS"));

        GlobalVariables.put(clusterInfo.getId(), globalVariables);
    }

    @Override
    public List<ClusterInfoDTO> getClusterList() {
        List<ClusterInfoEntity> entities = getMapper().selectAll();
        return entities.stream()
                .map(entity -> {
                    List<UserInfoEntity> userList = clusterUserService
                            .getAllClusterManagerByClusterId(entity.getId());
                    entity.setClusterManagerList(userList);
                    entity.setClusterStateCode(entity.getClusterState().getValue());
                    return clusterInfoConverter.entityToDto(entity);
                })
                .toList();
    }

    @Override
    public List<ClusterInfoDTO> runningClusterList() {
        List<ClusterInfoEntity> entities = getMapper().selectByClusterState(ClusterState.RUNNING);
        return clusterInfoConverter.entityListToDtoList(entities);
    }

    @Override
    public boolean updateClusterState(Integer clusterId, Integer clusterState) {
        ClusterInfoEntity clusterInfo = getById(clusterId);
        ClusterState state = ClusterState.of(clusterState);
        if (state != null) {
            clusterInfo.setClusterState(state);
            return updateById(clusterInfo);
        } else {
            throw new RuntimeException("未知状态");
        }
    }

    @Override
    public List<ClusterInfoDTO> getClusterByFrameCode(String frameCode) {
        List<ClusterInfoEntity> entities = getMapper().selectByFrameCode(frameCode);
        return clusterInfoConverter.entityListToDtoList(entities);
    }

    @Override
    public ClusterInfoDTO updateCluster(ClusterInfoDTO clusterInfoDTO) {
        // DTO转Entity
        ClusterInfoEntity clusterInfo = clusterInfoConverter.dtoToEntity(clusterInfoDTO);

        // 集群编码判重
        ClusterInfoEntity existingCluster = getMapper()
                .selectByClusterCode(clusterInfo.getClusterCode());

        if (existingCluster != null && !existingCluster.getId().equals(clusterInfo.getId())) {
            throw new RuntimeException(Status.CLUSTER_CODE_EXISTS.getMsg());
        }

        ClusterInfoEntity cluster = getById(clusterInfo.getId());
        if (!cluster.getClusterCode().equals(clusterInfo.getClusterCode())) {
            ProcessUtils.createServiceActor(clusterInfo);
        }

        updateById(clusterInfo);
        return clusterInfoConverter.entityToDto(clusterInfo);
    }

    @Override
    public void deleteCluster(List<Integer> ids) {
        Integer id = ids.getFirst();
        ClusterInfoEntity clusterInfo = getById(id);

        if (ClusterState.STOP.equals(clusterInfo.getClusterState())) {
            List<ClusterServiceInstanceEntity> serviceInstanceList = clusterServiceInstanceService.listAll(id);
            if (serviceInstanceList.stream()
                    .noneMatch(instance -> clusterServiceInstanceService.hasRunningRoleInstance(instance.getId()))) {
                ActorUtils.getLocalActor(
                        ClusterActor.class, "clusterActor")
                        .tell(new ClusterCommand(ClusterCommandType.DELETE, id), ActorRef.noSender());

                updateClusterState(id, ClusterState.DELETING.getValue());
            }
        }
        if (ClusterState.NEED_CONFIG.equals(clusterInfo.getClusterState())) {
            removeByIds(ids);
            // delete host
            clusterHostService.removeHostByClusterId(id);
        }
    }

    @Override
    public String getKubeConfigByClusterId(Integer clusterId) {
        return getById(clusterId).getKubeConfig();
    }

    @Override
    public String getServiceRoleMetrics() {
        // 获取所有运行中的服务角色实例
        List<ClusterServiceRoleInstanceEntity> roleInstances = getMapper()
                .selectRunningRoleInstances();

        // 按服务角色名称分组并计数
        Map<String, Long> roleCountMap = roleInstances.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ClusterServiceRoleInstanceEntity::getServiceRoleName,
                        java.util.stream.Collectors.counting()));

        // 构建Prometheus格式的响应
        StringBuilder prometheusMetrics = new StringBuilder();

        // 添加帮助信息和类型信息
        prometheusMetrics
                .append("# HELP service_role_instance_count The number of running instances for each service role\n");
        prometheusMetrics.append("# TYPE service_role_instance_count gauge\n");

        // 为每个服务角色添加指标行
        roleCountMap.forEach((roleName, count) -> prometheusMetrics
                .append(String.format("service_role_instance_count{role=\"%s\"} %d\n",
                        roleName.replace("\"", "\\\""), count)));

        // 添加EOF标记
        prometheusMetrics.append("# EOF\n");

        return prometheusMetrics.toString();
    }

    @Override
    public ClusterInfoDTO getClusterById(Integer clusterId) {
        ClusterInfoEntity clusterInfo = getById(clusterId);
        if (clusterInfo == null) {
            throw new RuntimeException("集群不存在");
        }
        return clusterInfoConverter.entityToDto(clusterInfo);
    }

    @Override
    public KubernetesNamespaceDto getKubernetesNamespaces(String kubeConfig) {
        if (kubeConfig == null || kubeConfig.trim().isEmpty()) {
            throw new RuntimeException("Kubernetes配置不能为空");
        }

        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            // 测试连接
            KubeUtil.testConnect(client);

            // 获取所有命名空间
            List<io.fabric8.kubernetes.api.model.Namespace> namespaces = client.namespaces().list().getItems();

            // 转换为简单的名称列表并排序
            List<String> namespaceNames = namespaces.stream()
                    .map(ns -> ns.getMetadata().getName())
                    .sorted()
                    .toList();

            // 构建返回结果
            KubernetesNamespaceDto kubernetesNamespaceDto = new KubernetesNamespaceDto();
            kubernetesNamespaceDto.setNamespaces(namespaceNames);
            kubernetesNamespaceDto.setDefaultNamespace("datasophon");
            kubernetesNamespaceDto.setShowNamespaceSelector(true);
            kubernetesNamespaceDto.setClusterVersion(client.getKubernetesVersion().getGitVersion());
            return kubernetesNamespaceDto;
        } catch (io.fabric8.kubernetes.client.KubernetesClientException e) {
            log.error("Kubernetes客户端异常", e);
            String errorMsg = "连接Kubernetes集群失败";
            if (e.getMessage().contains("Unauthorized")) {
                errorMsg = "认证失败，请检查Kubernetes配置文件的凭证信息";
            } else if (e.getMessage().contains("refused")) {
                errorMsg = "连接被拒绝，请检查Kubernetes集群地址和端口";
            } else if (e.getMessage().contains("timeout")) {
                errorMsg = "连接超时，请检查网络连接和集群状态";
            }
            throw new RuntimeException(errorMsg + ": " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("获取Kubernetes命名空间失败", e);
            throw new RuntimeException("处理Kubernetes配置时出错: " + e.getMessage(), e);
        }
    }

    @Override
    public String updateClusterKubeConfig(Integer clusterId, String kubeConfig, String namespace,
            String customNamespace) {
        try {
            ClusterInfoEntity clusterInfo = getById(clusterId);
            if (clusterInfo == null) {
                throw new RuntimeException("集群不存在");
            }

            // 使用传入的命名空间名称
            if (namespace == null || namespace.trim().isEmpty()) {
                throw new RuntimeException("命名空间名称不能为空");
            }

            // 验证配置有效性
            try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
                KubeUtil.testConnect(client);

                // 检查命名空间是否存在，不存在就创建
                if (!KubeUtil.checkNamespace(client, namespace)) {
                    log.info("命名空间 '{}' 不存在，开始创建", namespace);

                    // 创建命名空间
                    if (!KubeUtil.createNamespace(client, namespace)) {
                        throw new RuntimeException("创建命名空间 '" + namespace + "' 失败");
                    }

                    log.info("成功创建命名空间：{}", namespace);
                } else {
                    log.info("命名空间 '{}' 已存在，直接使用", namespace);
                }
            }

            // 更新集群信息
            clusterInfo.setKubeConfig(kubeConfig);
            clusterInfo.setNamespace(namespace);
            updateById(clusterInfo);

            return "Kubernetes配置更新成功";
        } catch (Exception e) {
            log.error("更新Kubernetes配置失败", e);
            throw new RuntimeException("更新Kubernetes配置失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getKubernetesNamespace(Integer clusterId) {
        return getById(clusterId).getNamespace();
    }

}
