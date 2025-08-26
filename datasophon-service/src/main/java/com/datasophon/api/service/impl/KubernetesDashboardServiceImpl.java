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

import static com.datasophon.common.constants.K8sResourceConstants.PodPhase;
import static com.datasophon.common.constants.K8sResourceConstants.ServiceType;
import static com.datasophon.common.constants.K8sResourceConstants.PersistentVolumePhase;
import static com.datasophon.common.constants.K8sResourceConstants.PersistentVolumeClaimPhase;

import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.KubernetesDashboardService;
import com.datasophon.common.dto.K8sNamespaceDTO;
import com.datasophon.common.dto.K8sResourceStatsDTO;
import com.datasophon.common.dto.KubernetesResourceDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.api.converter.KubernetesResourceConverter;
import com.datasophon.dao.entity.ClusterInfoEntity;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressClass;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Kubernetes仪表盘服务实现类
 * 
 * @author 63588
 */
@Service("kubernetesDashboardService")
@Slf4j
public class KubernetesDashboardServiceImpl implements KubernetesDashboardService {

    /**
     * 分页结果包装类，提供类型安全的分页结果
     */
        public record PaginatedResult<T>(List<T> items, long total, int totalPages) {
    }

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private KubernetesResourceConverter kubernetesResourceConverter;

    /**
     * 使用MapStruct Converter创建分页结果
     */
    private <T extends HasMetadata> PageResult<KubernetesResourceDTO> createPageResult(List<T> resources,
            PaginatedResult<T> paginationResult, int pageNum, int pageSize) {
        List<KubernetesResourceDTO> dtoList = resources.stream()
                .map(resource -> kubernetesResourceConverter.convertToDto(resource))
                .toList();

        return PageResult.<KubernetesResourceDTO>builder()
                .records(dtoList)
                .total(paginationResult.total())
                .current(pageNum)
                .size(pageSize)
                .build();
    }

    /**
     * 通用的分页查询方法
     */
    private <T extends HasMetadata> PaginatedResult<T> paginateResources(
            KubernetesClient client, Class<T> resourceClass, String namespace, Integer pageNum, Integer pageSize) {
        try {
            List<T> allItems;

            // 根据资源类型和命名空间获取资源列表
            if (namespace != null && !namespace.isEmpty()) {
                allItems = client.resources(resourceClass).inNamespace(namespace).list().getItems();
            } else {
                allItems = client.resources(resourceClass).list().getItems();
            }

            // 如果不需要分页，返回所有项目
            if (pageNum == null || pageSize == null) {
                return new PaginatedResult<>(allItems, allItems.size(), 1);
            }

            // 计算分页
            int total = allItems.size();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            int startIndex = (pageNum - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, total);

            List<T> pagedItems = startIndex >= total ? Collections.emptyList() : allItems.subList(startIndex, endIndex);

            return new PaginatedResult<>(pagedItems, total, totalPages);
        } catch (Exception e) {
            log.error("分页查询Kubernetes资源出错: {}", e.getMessage(), e);
            return new PaginatedResult<>(Collections.emptyList(), 0, 0);
        }
    }

    /**
     * 获取集群的kubeconfig配置
     */
    private String getKubeConfig(Long clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        if (clusterInfo == null) {
            return null;
        }
        return clusterInfo.getKubeConfig();
    }

    @Override
    public List<K8sNamespaceDTO> getNamespaces(Long clusterId) {
        try {
            if (clusterId == null) {
                throw new RuntimeException("集群ID不能为空");
            }

            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                throw new RuntimeException("找不到集群Kubernetes配置");
            }

            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取所有命名空间
            NamespaceList namespaceList = client.namespaces().list();

            log.info("获取{}个命名空间列表", namespaceList.getItems().size());

            // 🚀 极简版本：只返回命名空间基础信息，无统计数据
            return namespaceList.getItems().stream()
                    .map(ns -> {
                        String name = ns.getMetadata().getName();
                        String phase = ns.getStatus() != null ? ns.getStatus().getPhase() : "Unknown";
                        String creationTime = ns.getMetadata().getCreationTimestamp();
                        Integer resourceVersion = ns.getMetadata().getResourceVersion() != null
                                ? Integer.parseInt(ns.getMetadata().getResourceVersion())
                                : null;

                        // 只返回基础信息，无统计数据，性能最佳
                        return K8sNamespaceDTO.of(name, phase, creationTime, resourceVersion);
                    })
                    .toList();
        } catch (Exception e) {
            log.error("获取命名空间列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取命名空间列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getDeployments(Long clusterId, Long serviceId, String namespace,
            Integer pageNum, Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取Deployment列表
            PaginatedResult<Deployment> paginationResult = paginateResources(
                    client,
                    Deployment.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取Deployments列表出错", e);
            throw new RuntimeException("获取Deployments列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getPods(Long clusterId, Long serviceId, String namespace,
            String searchTerm, String statusFilter, Integer pageNum, Integer pageSize) {
        try {
            log.info("获取Pods列表请求：clusterId={}, serviceId={}, namespace={}, searchTerm={}, statusFilter={}, pageNum={}, pageSize={}",
                    clusterId, serviceId, namespace, searchTerm, statusFilter, pageNum, pageSize);

            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取所有Pod（不分页，用于搜索和筛选）
            List<Pod> allPods;
            if (namespace != null && !namespace.isEmpty()) {
                allPods = client.pods().inNamespace(namespace).list().getItems();
            } else {
                allPods = client.pods().inAnyNamespace().list().getItems();
            }

            log.info("从Kubernetes集群获取到{}个Pod", allPods.size());

            // 应用搜索和筛选
            List<Pod> filteredPods = applyPodsSearchAndFilter(allPods, searchTerm, statusFilter);
            
            log.info("搜索和筛选后剩余{}个Pod", filteredPods.size());

            // 应用分页
            PaginatedResult<Pod> paginationResult = applyPagination(filteredPods, pageNum, pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取Pods列表出错", e);
            throw new RuntimeException("获取Pods列表出错: " + e.getMessage());
        }
    }

    /**
     * 应用Pod搜索和筛选条件
     */
    private List<Pod> applyPodsSearchAndFilter(List<Pod> pods, String searchTerm, String statusFilter) {
        return pods.stream()
                .filter(pod -> {
                    // 状态筛选
                    if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equals("all")) {
                        String podPhase = pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown";
                        // 支持前端传递的小写状态值，映射到Kubernetes的实际状态值
                        String normalizedFilter = normalizeStatusFilter(statusFilter);
                        if (!normalizedFilter.equalsIgnoreCase(podPhase)) {
                            return false;
                        }
                    }

                    // 搜索关键词筛选
                    if (searchTerm != null && !searchTerm.isEmpty()) {
                        String searchLower = searchTerm.toLowerCase();
                        
                        // 搜索Pod名称
                        String podName = pod.getMetadata().getName();
                        if (podName != null && podName.toLowerCase().contains(searchLower)) {
                            return true;
                        }

                        // 搜索节点名称
                        String nodeName = pod.getSpec() != null ? pod.getSpec().getNodeName() : null;
                        if (nodeName != null && nodeName.toLowerCase().contains(searchLower)) {
                            return true;
                        }

                        // 搜索命名空间
                        String namespace = pod.getMetadata().getNamespace();
                        if (namespace != null && namespace.toLowerCase().contains(searchLower)) {
                            return true;
                        }

                        // 搜索标签
                        if (pod.getMetadata().getLabels() != null) {
                            boolean labelMatch = pod.getMetadata().getLabels().entrySet().stream()
                                    .anyMatch(entry -> 
                                        (entry.getKey() != null && entry.getKey().toLowerCase().contains(searchLower)) ||
                                        (entry.getValue() != null && entry.getValue().toLowerCase().contains(searchLower))
                                    );
                            if (labelMatch) {
                                return true;
                            }
                        }

                        // 搜索IP地址
                        if (pod.getStatus() != null) {
                            String podIP = pod.getStatus().getPodIP();
                            String hostIP = pod.getStatus().getHostIP();
                            if ((podIP != null && podIP.contains(searchTerm)) ||
                                (hostIP != null && hostIP.contains(searchTerm))) {
                                return true;
                            }
                        }

                        // 如果没有匹配任何搜索条件，过滤掉
                        return false;
                    }

                    return true;
                })
                .toList();
    }

    /**
     * 标准化状态筛选值，将前端传递的小写状态映射到Kubernetes的实际状态值
     */
    private String normalizeStatusFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isEmpty()) {
            return statusFilter;
        }
        
        return switch (statusFilter.toLowerCase()) {
            case "running" -> PodPhase.RUNNING;
            case "pending" -> PodPhase.PENDING;
            case "failed" -> PodPhase.FAILED;
            case "succeeded" -> PodPhase.SUCCEEDED;
            case "unknown" -> PodPhase.UNKNOWN;
            default -> statusFilter; // 返回原值，以防有其他状态
        };
    }

    /**
     * 对已过滤的资源列表应用分页
     */
    private <T> PaginatedResult<T> applyPagination(List<T> items, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageSize == null) {
            return new PaginatedResult<>(items, items.size(), 1);
        }

        int total = items.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, total);

        List<T> pagedItems = startIndex >= total ? Collections.emptyList() : items.subList(startIndex, endIndex);

        return new PaginatedResult<>(pagedItems, total, totalPages);
    }

    private KubernetesClient getKubernetesClient(Long clusterId) {
        String kubeConfig = getKubeConfig(clusterId);
        Config config = Config.fromKubeconfig(kubeConfig);
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    @Override
    public PageResult<KubernetesResourceDTO> getServices(Long clusterId, String namespace, Integer pageNum,
            Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取Service列表
            PaginatedResult<io.fabric8.kubernetes.api.model.Service> servicePaginationResult = paginateResources(
                    client,
                    io.fabric8.kubernetes.api.model.Service.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用MapStruct转换为DTO并返回分页结果
            return createPageResult(servicePaginationResult.items(), servicePaginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取Services列表出错", e);
            throw new RuntimeException("获取Services列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getConfigMaps(Long clusterId, String namespace, Integer pageNum,
            Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

                    // 使用通用分页方法获取ConfigMap列表
                    PaginatedResult<io.fabric8.kubernetes.api.model.ConfigMap> paginationResult = paginateResources(
                            client,
                            io.fabric8.kubernetes.api.model.ConfigMap.class,
                            namespace,
                            pageNum,
                            pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取ConfigMaps列表出错", e);
            throw new RuntimeException("获取ConfigMaps列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getSecrets(Long clusterId, String namespace, Integer pageNum,
            Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

                    // 使用通用分页方法获取Secret列表
                    PaginatedResult<io.fabric8.kubernetes.api.model.Secret> paginationResult = paginateResources(
                            client,
                            io.fabric8.kubernetes.api.model.Secret.class,
                            namespace,
                            pageNum,
                            pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取Secrets列表出错", e);
            throw new RuntimeException("获取Secrets列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getPersistentVolumes(Long clusterId, Integer pageNum,
            Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取PersistentVolume列表
            PaginatedResult<PersistentVolume> paginationResult = paginateResources(
                    client,
                    PersistentVolume.class,
                    null, // PersistentVolume不是命名空间资源
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取PersistentVolumes列表出错", e);
            throw new RuntimeException("获取PersistentVolumes列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getPersistentVolumeClaims(Long clusterId, String namespace,
            Integer pageNum, Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取PersistentVolumeClaim列表
            PaginatedResult<io.fabric8.kubernetes.api.model.PersistentVolumeClaim> paginationResult = paginateResources(
                    client,
                    io.fabric8.kubernetes.api.model.PersistentVolumeClaim.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取PersistentVolumeClaims列表出错", e);
            throw new RuntimeException("获取PersistentVolumeClaims列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getStorageClasses(Long clusterId, Integer pageNum, Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取StorageClass列表
            PaginatedResult<StorageClass> paginationResult = paginateResources(
                    client,
                    StorageClass.class,
                    null, // StorageClass不是命名空间资源
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取StorageClasses列表出错", e);
            throw new RuntimeException("获取StorageClasses列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getIngresses(Long clusterId, String namespace, Integer pageNum,
            Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取Ingress列表
            PaginatedResult<Ingress> paginationResult = paginateResources(
                    client,
                    Ingress.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取Ingresses列表出错", e);
            throw new RuntimeException("获取Ingresses列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getIngressClasses(Long clusterId, Integer pageNum, Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取IngressClass列表
                    PaginatedResult<IngressClass> paginationResult = paginateResources(
                            client,
                            IngressClass.class,
                    null, // IngressClass不是命名空间资源
                            pageNum,
                            pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取IngressClasses列表出错", e);
            throw new RuntimeException("获取IngressClasses列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getDaemonSets(Long clusterId, Long serviceId, String namespace,
            Integer pageNum, Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取DaemonSet列表
            PaginatedResult<DaemonSet> paginationResult = paginateResources(
                    client,
                    DaemonSet.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取DaemonSets列表出错", e);
            throw new RuntimeException("获取DaemonSets列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getStatefulSets(Long clusterId, Long serviceId, String namespace, Integer pageNum,
            Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取StatefulSet列表
            PaginatedResult<StatefulSet> paginationResult = paginateResources(
                    client,
                    StatefulSet.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取StatefulSets列表出错", e);
            throw new RuntimeException("获取StatefulSets列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getReplicaSets(Long clusterId, Long serviceId, String namespace, Integer pageNum,
            Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取ReplicaSet列表
            PaginatedResult<ReplicaSet> paginationResult = paginateResources(
                    client,
                    ReplicaSet.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取ReplicaSets列表出错", e);
            throw new RuntimeException("获取ReplicaSets列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getReplicationControllers(Long clusterId, String namespace,
            Integer pageNum, Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取ReplicationController列表
            PaginatedResult<ReplicationController> paginationResult = paginateResources(
                    client,
                    ReplicationController.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取ReplicationControllers列表出错", e);
            throw new RuntimeException("获取ReplicationControllers列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getJobs(Long clusterId, Long serviceId, String namespace, Integer pageNum,
            Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取Job列表
            PaginatedResult<Job> paginationResult = paginateResources(
                    client,
                    Job.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取Jobs列表出错", e);
            throw new RuntimeException("获取Jobs列表出错: " + e.getMessage());
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getCronJobs(Long clusterId, Long serviceId, String namespace, Integer pageNum,
            Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取CronJob列表
            PaginatedResult<CronJob> paginationResult = paginateResources(
                    client,
                    CronJob.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取CronJobs列表出错", e);
            throw new RuntimeException("获取CronJobs列表出错: " + e.getMessage());
        }
    }

    // === 非分页方法 ===

    @Override
    public KubernetesResourceDTO getDeploymentDetail(Long clusterId, String namespace, String name) {
        try {
            KubernetesClient client = getKubernetesClient(clusterId);
            Deployment deployment = client.apps().deployments().inNamespace(namespace).withName(name).get();
            if (deployment == null) {
                throw new RuntimeException("Deployment not found: " + name);
            }
            return kubernetesResourceConverter.convertToDto(deployment);
        } catch (Exception e) {
            log.error("获取Deployment详情出错", e);
            throw new RuntimeException("获取Deployment详情出错: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getResourceEvents(Long clusterId, String namespace, String kind, String name) {
        try {
            KubernetesClient client = getKubernetesClient(clusterId);
            return client.v1().events().inNamespace(namespace).list().getItems().stream()
                    .filter(event -> name.equals(event.getInvolvedObject().getName()) &&
                            kind.equals(event.getInvolvedObject().getKind()))
                    .map(event -> {
                        Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put("type", event.getType());
                        eventMap.put("reason", event.getReason());
                        eventMap.put("message", event.getMessage());
                        eventMap.put("firstTimestamp", event.getFirstTimestamp());
                        eventMap.put("lastTimestamp", event.getLastTimestamp());
                        return eventMap;
                    })
                    .toList();
        } catch (Exception e) {
            log.error("获取资源事件出错", e);
            throw new RuntimeException("获取资源事件出错: " + e.getMessage());
        }
    }

    @Override
    public K8sResourceStatsDTO getResourceStats(Long clusterId, Long serviceId, String namespace) {
        try {
            KubernetesClient client = getKubernetesClient(clusterId);

            log.info("开始统计Kubernetes资源，clusterId={}, serviceId={}, namespace={}", clusterId, serviceId, namespace);
            
            // 构建统计结果
            K8sResourceStatsDTO.K8sResourceStatsDTOBuilder builder = K8sResourceStatsDTO.builder();
            
            // 为了准确反映集群状态，优先查询所有命名空间的资源
            // 只有在明确指定namespace且不为空时才限制到特定命名空间
            boolean queryAllNamespaces = namespace == null || namespace.isEmpty() || "all".equals(namespace);
            
            if (queryAllNamespaces) {
                log.info("查询所有命名空间的资源统计");
            } else {
                log.info("查询命名空间 '{}' 的资源统计", namespace);
            }
            
            // 统计Pods
            List<Pod> pods = queryAllNamespaces ? 
                client.pods().inAnyNamespace().list().getItems() :
                client.pods().inNamespace(namespace).list().getItems();
            
            log.info("查询到 {} 个Pod", pods.size());
            builder.podCount(pods.size());
            
            long runningPods = pods.stream().filter(pod -> {
                try {
                    return pod.getStatus() != null && PodPhase.RUNNING.equals(pod.getStatus().getPhase());
                } catch (Exception e) {
                    log.warn("处理Pod {}状态时出错: {}", pod.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
            
            long pendingPods = pods.stream().filter(pod -> {
                try {
                    return pod.getStatus() != null && PodPhase.PENDING.equals(pod.getStatus().getPhase());
                } catch (Exception e) {
                    log.warn("处理Pod {}状态时出错: {}", pod.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
            
            long failedPods = pods.stream().filter(pod -> {
                try {
                    return pod.getStatus() != null && PodPhase.FAILED.equals(pod.getStatus().getPhase());
                } catch (Exception e) {
                    log.warn("处理Pod {}状态时出错: {}", pod.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
            
            long succeededPods = pods.stream().filter(pod -> {
                try {
                    return pod.getStatus() != null && PodPhase.SUCCEEDED.equals(pod.getStatus().getPhase());
                } catch (Exception e) {
                    log.warn("处理Pod {}状态时出错: {}", pod.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
                
            log.info("Pod统计: 总数={}, 运行中={}, 等待中={}, 失败={}, 成功={}", 
                pods.size(), runningPods, pendingPods, failedPods, succeededPods);
                
            builder.runningPodCount((int) runningPods)
                   .pendingPodCount((int) pendingPods)
                   .failedPodCount((int) failedPods)
                   .succeededPodCount((int) succeededPods);

            // 统计Services
            List<io.fabric8.kubernetes.api.model.Service> services = queryAllNamespaces ?
                client.services().inAnyNamespace().list().getItems() :
                client.services().inNamespace(namespace).list().getItems();
            
            log.info("查询到 {} 个Service", services.size());
            builder.serviceCount(services.size());
            
            long clusterIpServices = services.stream().filter(svc -> {
                try {
                    String type = svc.getSpec() != null ? svc.getSpec().getType() : null;
                    return ServiceType.CLUSTER_IP.equals(type) || type == null; // 默认type是ClusterIP
                } catch (Exception e) {
                    log.warn("处理Service {}类型时出错: {}", svc.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
            
            long nodePortServices = services.stream().filter(svc -> {
                try {
                    return svc.getSpec() != null && ServiceType.NODE_PORT.equals(svc.getSpec().getType());
                } catch (Exception e) {
                    log.warn("处理Service {}类型时出错: {}", svc.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
            
            long loadBalancerServices = services.stream().filter(svc -> {
                try {
                    return svc.getSpec() != null && ServiceType.LOAD_BALANCER.equals(svc.getSpec().getType());
                } catch (Exception e) {
                    log.warn("处理Service {}类型时出错: {}", svc.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
                
            log.info("Service统计: 总数={}, ClusterIP={}, NodePort={}, LoadBalancer={}", 
                services.size(), clusterIpServices, nodePortServices, loadBalancerServices);
                
            builder.clusterIpServiceCount((int) clusterIpServices)
                   .nodePortServiceCount((int) nodePortServices)
                   .loadBalancerServiceCount((int) loadBalancerServices);

            // 统计Deployments
            List<Deployment> deployments = queryAllNamespaces ?
                client.apps().deployments().inAnyNamespace().list().getItems() :
                client.apps().deployments().inNamespace(namespace).list().getItems();
            
            log.info("查询到 {} 个Deployment", deployments.size());
            builder.deploymentCount(deployments.size());
            
            long availableDeployments = deployments.stream().filter(dep -> {
                try {
                    Integer replicas = dep.getSpec() != null ? dep.getSpec().getReplicas() : null;
                    Integer availableReplicas = dep.getStatus() != null ? dep.getStatus().getAvailableReplicas() : null;
                    // 如果任一值为null，视为不可用
                    if (replicas == null || availableReplicas == null) {
                        return false;
                    }
                    return replicas.equals(availableReplicas);
                } catch (Exception e) {
                    log.warn("处理Deployment {}状态时出错: {}", dep.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
            
            builder.availableDeploymentCount((int) availableDeployments)
                   .unavailableDeploymentCount((int) (deployments.size() - availableDeployments));

            // 统计ConfigMaps
            int configMapsCount = queryAllNamespaces ?
                client.configMaps().inAnyNamespace().list().getItems().size() :
                client.configMaps().inNamespace(namespace).list().getItems().size();
            log.info("查询到 {} 个ConfigMap", configMapsCount);
            builder.configMapCount(configMapsCount);

            // 统计Secrets
            int secretsCount = queryAllNamespaces ?
                client.secrets().inAnyNamespace().list().getItems().size() :
                client.secrets().inNamespace(namespace).list().getItems().size();
            log.info("查询到 {} 个Secret", secretsCount);
            builder.secretCount(secretsCount);

            // 统计StatefulSets
            List<StatefulSet> statefulSets = queryAllNamespaces ?
                client.apps().statefulSets().inAnyNamespace().list().getItems() :
                client.apps().statefulSets().inNamespace(namespace).list().getItems();
            
            log.info("查询到 {} 个StatefulSet", statefulSets.size());
            builder.statefulSetCount(statefulSets.size());
            
            long readyStatefulSets = statefulSets.stream().filter(sts -> {
                try {
                    Integer replicas = sts.getSpec() != null ? sts.getSpec().getReplicas() : null;
                    Integer readyReplicas = sts.getStatus() != null ? sts.getStatus().getReadyReplicas() : null;
                    if (replicas == null || readyReplicas == null) {
                        return false;
                    }
                    return replicas.equals(readyReplicas);
                } catch (Exception e) {
                    log.warn("处理StatefulSet {}状态时出错: {}", sts.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
            
            builder.readyStatefulSetCount((int) readyStatefulSets);

            // 统计DaemonSets
            List<DaemonSet> daemonSets = queryAllNamespaces ?
                client.apps().daemonSets().inAnyNamespace().list().getItems() :
                client.apps().daemonSets().inNamespace(namespace).list().getItems();
            
            log.info("查询到 {} 个DaemonSet", daemonSets.size());
            builder.daemonSetCount(daemonSets.size());
            
            long readyDaemonSets = daemonSets.stream().filter(ds -> {
                try {
                    Integer desired = ds.getStatus() != null ? ds.getStatus().getDesiredNumberScheduled() : null;
                    Integer ready = ds.getStatus() != null ? ds.getStatus().getNumberReady() : null;
                    if (desired == null || ready == null) {
                        return false;
                    }
                    return desired.equals(ready);
                } catch (Exception e) {
                    log.warn("处理DaemonSet {}状态时出错: {}", ds.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
            
            builder.readyDaemonSetCount((int) readyDaemonSets);

            // 统计Jobs
            List<Job> jobs = queryAllNamespaces ?
                client.batch().v1().jobs().inAnyNamespace().list().getItems() :
                client.batch().v1().jobs().inNamespace(namespace).list().getItems();
            
            log.info("查询到 {} 个Job", jobs.size());
            builder.jobCount(jobs.size());
            
            long completedJobs = jobs.stream().filter(job -> {
                return job.getStatus() != null && job.getStatus().getCompletionTime() != null;
            }).count();
            long activeJobs = jobs.stream().filter(job -> {
                return job.getStatus() != null && job.getStatus().getActive() != null && job.getStatus().getActive() > 0;
            }).count();
            long failedJobs = jobs.stream().filter(job -> {
                return job.getStatus() != null && job.getStatus().getFailed() != null && job.getStatus().getFailed() > 0;
            }).count();
            
            builder.completedJobCount((int) completedJobs)
                   .activeJobCount((int) activeJobs)
                   .failedJobCount((int) failedJobs);

            // 统计CronJobs
            List<CronJob> cronJobs = queryAllNamespaces ?
                client.batch().v1().cronjobs().inAnyNamespace().list().getItems() :
                client.batch().v1().cronjobs().inNamespace(namespace).list().getItems();
            
            log.info("查询到 {} 个CronJob", cronJobs.size());
            builder.cronJobCount(cronJobs.size());
            
            long activeCronJobs = cronJobs.stream().filter(cj -> {
                return cj.getStatus() != null && cj.getStatus().getActive() != null && !cj.getStatus().getActive().isEmpty();
            }).count();
            long suspendedCronJobs = cronJobs.stream().filter(cj -> {
                return cj.getSpec() != null && Boolean.TRUE.equals(cj.getSpec().getSuspend());
            }).count();
            
            builder.activeCronJobCount((int) activeCronJobs)
                   .suspendedCronJobCount((int) suspendedCronJobs);

            // 统计PersistentVolumes (集群级别资源，不受namespace限制)
            List<PersistentVolume> persistentVolumes = client.persistentVolumes().list().getItems();
            
            builder.persistentVolumeCount(persistentVolumes.size());
            
            long boundPVs = persistentVolumes.stream().filter(pv -> 
                PersistentVolumePhase.BOUND.equals(pv.getStatus().getPhase())).count();
            long availablePVs = persistentVolumes.stream().filter(pv -> 
                PersistentVolumePhase.AVAILABLE.equals(pv.getStatus().getPhase())).count();
                
            builder.boundPvCount((int) boundPVs)
                   .availablePvCount((int) availablePVs);

            // 统计PersistentVolumeClaims
            List<PersistentVolumeClaim> persistentVolumeClaims = namespace != null ?
                client.persistentVolumeClaims().inNamespace(namespace).list().getItems() :
                client.persistentVolumeClaims().inAnyNamespace().list().getItems();
            
            builder.persistentVolumeClaimCount(persistentVolumeClaims.size());
            
            long boundPVCs = persistentVolumeClaims.stream().filter(pvc -> 
                PersistentVolumeClaimPhase.BOUND.equals(pvc.getStatus().getPhase())).count();
            long pendingPVCs = persistentVolumeClaims.stream().filter(pvc -> 
                PersistentVolumeClaimPhase.PENDING.equals(pvc.getStatus().getPhase())).count();
                
            builder.boundPvcCount((int) boundPVCs)
                   .pendingPvcCount((int) pendingPVCs);

            // 统计StorageClasses (集群级别资源)
            int storageClassesCount = client.storage().v1().storageClasses().list().getItems().size();
            builder.storageClassCount(storageClassesCount);

            // 统计Ingresses
            int ingressesCount = namespace != null ?
                client.network().v1().ingresses().inNamespace(namespace).list().getItems().size() :
                client.network().v1().ingresses().inAnyNamespace().list().getItems().size();
            builder.ingressCount(ingressesCount);

            // 统计IngressClasses (集群级别资源)
            int ingressClassesCount = client.network().v1().ingressClasses().list().getItems().size();
            builder.ingressClassCount(ingressClassesCount);

            // 统计ReplicaSets
            List<ReplicaSet> replicaSets = namespace != null ?
                client.apps().replicaSets().inNamespace(namespace).list().getItems() :
                client.apps().replicaSets().inAnyNamespace().list().getItems();
            
            builder.replicaSetCount(replicaSets.size());
            
            long readyReplicaSets = replicaSets.stream().filter(rs -> {
                try {
                    Integer replicas = rs.getSpec() != null ? rs.getSpec().getReplicas() : null;
                    Integer readyReplicas = rs.getStatus() != null ? rs.getStatus().getReadyReplicas() : null;
                    if (replicas == null || readyReplicas == null) {
                        return false;
                    }
                    return replicas.equals(readyReplicas);
                } catch (Exception e) {
                    log.warn("处理ReplicaSet {}状态时出错: {}", rs.getMetadata().getName(), e.getMessage());
                    return false;
                }
            }).count();
            
            builder.readyReplicaSetCount((int) readyReplicaSets);

            K8sResourceStatsDTO result = builder.build();
            log.info("完成Kubernetes资源统计: pods={}, services={}, deployments={}", 
                result.getPodCount(), result.getServiceCount(), result.getDeploymentCount());
            
            return result;
        } catch (Exception e) {
            log.error("获取资源统计出错", e);
            throw new RuntimeException("获取资源统计出错: " + e.getMessage());
        }
    }
}