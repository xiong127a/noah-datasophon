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

    private KubernetesClient getKubernetesClient(Long clusterId) {
        String kubeConfig = getKubeConfig(clusterId);
        Config config = Config.fromKubeconfig(kubeConfig);
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    @Override
    public PageResult<KubernetesResourceDTO> getPods(Long clusterId, Long serviceId, String namespace,
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
    public K8sResourceStatsDTO getResourceStats(Long clusterId, Long serviceId, String namespace) {
        try {
            KubernetesClient client = getKubernetesClient(clusterId);
            
            log.info("开始统计Kubernetes资源，clusterId={}, serviceId={}, namespace={}", clusterId, serviceId, namespace);
            
            // 构建统计结果
            K8sResourceStatsDTO.K8sResourceStatsDTOBuilder builder = K8sResourceStatsDTO.builder();
            
            // 统计Pods
            List<Pod> pods = namespace != null ? 
                client.pods().inNamespace(namespace).list().getItems() :
                client.pods().inAnyNamespace().list().getItems();
            
            builder.podCount(pods.size());
            
            long runningPods = pods.stream().filter(pod -> 
                "Running".equals(pod.getStatus().getPhase())).count();
            long pendingPods = pods.stream().filter(pod -> 
                "Pending".equals(pod.getStatus().getPhase())).count();
            long failedPods = pods.stream().filter(pod -> 
                "Failed".equals(pod.getStatus().getPhase())).count();
            long succeededPods = pods.stream().filter(pod -> 
                "Succeeded".equals(pod.getStatus().getPhase())).count();
                
            builder.runningPodCount((int) runningPods)
                   .pendingPodCount((int) pendingPods)
                   .failedPodCount((int) failedPods)
                   .succeededPodCount((int) succeededPods);

            // 统计Services
            List<io.fabric8.kubernetes.api.model.Service> services = namespace != null ?
                client.services().inNamespace(namespace).list().getItems() :
                client.services().inAnyNamespace().list().getItems();
            
            builder.serviceCount(services.size());
            
            long clusterIpServices = services.stream().filter(svc -> 
                "ClusterIP".equals(svc.getSpec().getType())).count();
            long nodePortServices = services.stream().filter(svc -> 
                "NodePort".equals(svc.getSpec().getType())).count();
            long loadBalancerServices = services.stream().filter(svc -> 
                "LoadBalancer".equals(svc.getSpec().getType())).count();
                
            builder.clusterIpServiceCount((int) clusterIpServices)
                   .nodePortServiceCount((int) nodePortServices)
                   .loadBalancerServiceCount((int) loadBalancerServices);

            // 统计Deployments
            List<Deployment> deployments = namespace != null ?
                client.apps().deployments().inNamespace(namespace).list().getItems() :
                client.apps().deployments().inAnyNamespace().list().getItems();
            
            builder.deploymentCount(deployments.size());
            
            long availableDeployments = deployments.stream().filter(dep -> {
                Integer replicas = dep.getSpec().getReplicas();
                Integer availableReplicas = dep.getStatus() != null ? dep.getStatus().getAvailableReplicas() : 0;
                return replicas != null && availableReplicas != null && replicas.equals(availableReplicas);
            }).count();
            
            builder.availableDeploymentCount((int) availableDeployments)
                   .unavailableDeploymentCount((int) (deployments.size() - availableDeployments));

            // 统计ConfigMaps
            int configMapsCount = namespace != null ?
                client.configMaps().inNamespace(namespace).list().getItems().size() :
                client.configMaps().inAnyNamespace().list().getItems().size();
            builder.configMapCount(configMapsCount);

            // 统计Secrets
            int secretsCount = namespace != null ?
                client.secrets().inNamespace(namespace).list().getItems().size() :
                client.secrets().inAnyNamespace().list().getItems().size();
            builder.secretCount(secretsCount);

            // 统计StatefulSets
            List<StatefulSet> statefulSets = namespace != null ?
                client.apps().statefulSets().inNamespace(namespace).list().getItems() :
                client.apps().statefulSets().inAnyNamespace().list().getItems();
            
            builder.statefulSetCount(statefulSets.size());
            
            long readyStatefulSets = statefulSets.stream().filter(sts -> {
                Integer replicas = sts.getSpec().getReplicas();
                Integer readyReplicas = sts.getStatus() != null ? sts.getStatus().getReadyReplicas() : 0;
                return replicas != null && readyReplicas != null && replicas.equals(readyReplicas);
            }).count();
            
            builder.readyStatefulSetCount((int) readyStatefulSets);

            // 统计DaemonSets
            List<DaemonSet> daemonSets = namespace != null ?
                client.apps().daemonSets().inNamespace(namespace).list().getItems() :
                client.apps().daemonSets().inAnyNamespace().list().getItems();
            
            builder.daemonSetCount(daemonSets.size());
            
            long readyDaemonSets = daemonSets.stream().filter(ds -> {
                Integer desired = ds.getStatus() != null ? ds.getStatus().getDesiredNumberScheduled() : 0;
                Integer ready = ds.getStatus() != null ? ds.getStatus().getNumberReady() : 0;
                return desired != null && ready != null && desired.equals(ready);
            }).count();
            
            builder.readyDaemonSetCount((int) readyDaemonSets);

            // 统计Jobs
            List<Job> jobs = namespace != null ?
                client.batch().v1().jobs().inNamespace(namespace).list().getItems() :
                client.batch().v1().jobs().inAnyNamespace().list().getItems();
            
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
            List<CronJob> cronJobs = namespace != null ?
                client.batch().v1().cronjobs().inNamespace(namespace).list().getItems() :
                client.batch().v1().cronjobs().inAnyNamespace().list().getItems();
            
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
                "Bound".equals(pv.getStatus().getPhase())).count();
            long availablePVs = persistentVolumes.stream().filter(pv -> 
                "Available".equals(pv.getStatus().getPhase())).count();
                
            builder.boundPvCount((int) boundPVs)
                   .availablePvCount((int) availablePVs);

            // 统计PersistentVolumeClaims
            List<PersistentVolumeClaim> persistentVolumeClaims = namespace != null ?
                client.persistentVolumeClaims().inNamespace(namespace).list().getItems() :
                client.persistentVolumeClaims().inAnyNamespace().list().getItems();
            
            builder.persistentVolumeClaimCount(persistentVolumeClaims.size());
            
            long boundPVCs = persistentVolumeClaims.stream().filter(pvc -> 
                "Bound".equals(pvc.getStatus().getPhase())).count();
            long pendingPVCs = persistentVolumeClaims.stream().filter(pvc -> 
                "Pending".equals(pvc.getStatus().getPhase())).count();
                
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
                Integer replicas = rs.getSpec().getReplicas();
                Integer readyReplicas = rs.getStatus() != null ? rs.getStatus().getReadyReplicas() : 0;
                return replicas != null && readyReplicas != null && replicas.equals(readyReplicas);
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