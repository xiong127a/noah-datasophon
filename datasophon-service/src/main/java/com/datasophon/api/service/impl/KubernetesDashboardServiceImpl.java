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

            // 转换为DTO列表
            return namespaceList.getItems().stream()
                    .map(ns -> {
                        String name = ns.getMetadata().getName();
                        String phase = ns.getStatus() != null ? ns.getStatus().getPhase() : "Unknown";
                        String creationTime = ns.getMetadata().getCreationTimestamp();
                        Integer resourceVersion = ns.getMetadata().getResourceVersion() != null
                                ? Integer.parseInt(ns.getMetadata().getResourceVersion())
                                : null;
                        return K8sNamespaceDTO.of(name, phase, creationTime, resourceVersion);
                    })
                    .toList();
        } catch (Exception e) {
            log.error("获取命名空间列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取命名空间列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PageResult<KubernetesResourceDTO> getDeployments(Long clusterId, Integer serviceId, String namespace,
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

    private KubernetesClient getKubernetesClient(Long clusterId) {
        String kubeConfig = getKubeConfig(clusterId);
        Config config = Config.fromKubeconfig(kubeConfig);
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    @Override
    public PageResult<KubernetesResourceDTO> getPods(Long clusterId, Integer serviceId, String namespace,
            Integer pageNum, Integer pageSize) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 使用通用分页方法获取Pod列表
            PaginatedResult<Pod> paginationResult = paginateResources(
                    client,
                    Pod.class,
                    namespace,
                    pageNum,
                    pageSize);

            // 使用通用createPageResult方法
            return createPageResult(paginationResult.items(), paginationResult, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取Pods列表出错", e);
            throw new RuntimeException("获取Pods列表出错: " + e.getMessage());
        }
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
    public PageResult<KubernetesResourceDTO> getDaemonSets(Long clusterId, Integer serviceId, String namespace,
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
    public PageResult<KubernetesResourceDTO> getStatefulSets(Long clusterId, String namespace, Integer pageNum,
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
    public PageResult<KubernetesResourceDTO> getReplicaSets(Long clusterId, String namespace, Integer pageNum,
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
    public PageResult<KubernetesResourceDTO> getJobs(Long clusterId, String namespace, Integer pageNum,
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
    public PageResult<KubernetesResourceDTO> getCronJobs(Long clusterId, String namespace, Integer pageNum,
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
    public K8sResourceStatsDTO getResourceStats(Long clusterId, Integer serviceId, String namespace) {
        try {
            KubernetesClient client = getKubernetesClient(clusterId);

            // 统计各种资源数量
            int podsCount = client.pods().inNamespace(namespace).list().getItems().size();
            int deploymentsCount = client.apps().deployments().inNamespace(namespace).list().getItems().size();
            int servicesCount = client.services().inNamespace(namespace).list().getItems().size();
            int configMapsCount = client.configMaps().inNamespace(namespace).list().getItems().size();
            int secretsCount = client.secrets().inNamespace(namespace).list().getItems().size();

            return new K8sResourceStatsDTO(
                    1, // namespaceCount (默认1个命名空间)
                    deploymentsCount, // deploymentCount
                    podsCount, // podCount
                    servicesCount, // serviceCount
                    configMapsCount, // configMapCount
                    secretsCount, // secretCount
                    0, // persistentVolumeCount
                    0, // persistentVolumeClaimCount
                    0, // storageClassCount
                    0, // ingressCount
                    0, // ingressClassCount
                    0, // daemonSetCount
                    0, // statefulSetCount
                    0, // replicaSetCount
                    0, // replicationControllerCount
                    0, // jobCount
                    0, // cronJobCount
                    podsCount, // runningPodCount (简化处理，假设所有Pod都在运行)
                    0, // pendingPodCount
                    0 // failedPodCount
            );
        } catch (Exception e) {
            log.error("获取资源统计出错", e);
            throw new RuntimeException("获取资源统计出错: " + e.getMessage());
        }
    }
}