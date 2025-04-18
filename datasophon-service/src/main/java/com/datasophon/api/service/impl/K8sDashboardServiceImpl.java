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
import com.datasophon.api.service.K8sDashboardService;
import com.datasophon.common.Constants;
import com.datasophon.common.model.k8s.DeploymentInfo;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * K8S仪表盘服务实现类
 */
@Service("k8sDashboardService")
public class K8sDashboardServiceImpl implements K8sDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(K8sDashboardServiceImpl.class);

    @Autowired
    private ClusterInfoService clusterInfoService;

    /**
     * 获取集群的kubeconfig配置
     */
    private String getKubeConfig(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        if (clusterInfo == null) {
            return null;
        }
        return clusterInfo.getKubeConfig();
    }

    @Override
    public Result getNamespaces(Integer clusterId) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取命名空间逻辑
            List<Object> namespaces = new ArrayList<>();

            return Result.success().put(Constants.DATA, namespaces);
        } catch (Exception e) {
            logger.error("获取命名空间列表出错", e);
            return Result.error("获取命名空间列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getDeployments(Integer clusterId, String namespace) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取Deployments
            DeploymentList deploymentList;
            if (namespace != null && !namespace.isEmpty()) {
                deploymentList = client.apps().deployments().inNamespace(namespace).list();
            } else {
                deploymentList = client.apps().deployments().inAnyNamespace().list();
            }

            // 转换为前端需要的数据结构
            List<DeploymentInfo> deployments = deploymentList.getItems().stream()
                    .map(deployment -> {
                        DeploymentInfo info = new DeploymentInfo();
                        info.setName(deployment.getMetadata().getName());
                        info.setNamespace(deployment.getMetadata().getNamespace());
                        info.setLabels(deployment.getMetadata().getLabels());

                        if (deployment.getSpec() != null) {
                            info.setReplicas(
                                    deployment.getSpec().getReplicas() != null ? deployment.getSpec().getReplicas()
                                            : 0);

                            if (deployment.getSpec().getSelector() != null) {
                                info.setSelector(deployment.getSpec().getSelector().getMatchLabels());
                            }

                            // 获取第一个容器的镜像
                            if (deployment.getSpec().getTemplate() != null &&
                                    deployment.getSpec().getTemplate().getSpec() != null &&
                                    deployment.getSpec().getTemplate().getSpec().getContainers() != null &&
                                    !deployment.getSpec().getTemplate().getSpec().getContainers().isEmpty()) {
                                info.setImage(
                                        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());
                            }
                        }

                        if (deployment.getStatus() != null) {
                            info.setAvailableReplicas(deployment.getStatus().getAvailableReplicas() != null
                                    ? deployment.getStatus().getAvailableReplicas()
                                    : 0);
                            info.setReadyReplicas(deployment.getStatus().getReadyReplicas() != null
                                    ? deployment.getStatus().getReadyReplicas()
                                    : 0);
                        }

                        if (deployment.getMetadata().getCreationTimestamp() != null) {
                            try {
                                // 将K8s时间字符串转换为Java Date对象
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                Date createDate = sdf.parse(deployment.getMetadata().getCreationTimestamp());
                                info.setCreateTime(createDate);
                            } catch (ParseException e) {
                                // 转换失败时记录日志并使用当前时间
                                logger.error("解析创建时间失败: " + e.getMessage());
                                info.setCreateTime(new Date());
                            }
                        }

                        return info;
                    })
                    .collect(Collectors.toList());

            return Result.success().put(Constants.DATA, deployments);
        } catch (Exception e) {
            logger.error("获取Deployments列表出错", e);
            return Result.error("获取Deployments列表出错: " + e.getMessage());
        }
    }

    private KubernetesClient getKubernetesClient(Integer clusterId) {
        String kubeConfig = getKubeConfig(clusterId);
        Config config = Config.fromKubeconfig(kubeConfig);
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    @Override
    public Result getPods(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取Pods逻辑
            List<Object> pods = new ArrayList<>();

            return Result.success().put(Constants.DATA, pods);
        } catch (Exception e) {
            logger.error("获取Pods列表出错", e);
            return Result.error("获取Pods列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getServices(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取Services逻辑
            List<Object> services = new ArrayList<>();

            return Result.success().put(Constants.DATA, services);
        } catch (Exception e) {
            logger.error("获取Services列表出错", e);
            return Result.error("获取Services列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getConfigMaps(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取ConfigMaps逻辑
            List<Object> configMaps = new ArrayList<>();

            return Result.success().put(Constants.DATA, configMaps);
        } catch (Exception e) {
            logger.error("获取ConfigMaps列表出错", e);
            return Result.error("获取ConfigMaps列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getSecrets(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取Secrets逻辑
            List<Object> secrets = new ArrayList<>();

            return Result.success().put(Constants.DATA, secrets);
        } catch (Exception e) {
            logger.error("获取Secrets列表出错", e);
            return Result.error("获取Secrets列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getPersistentVolumes(Integer clusterId) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取PersistentVolumes逻辑
            List<Object> persistentVolumes = new ArrayList<>();

            return Result.success().put(Constants.DATA, persistentVolumes);
        } catch (Exception e) {
            logger.error("获取PersistentVolumes列表出错", e);
            return Result.error("获取PersistentVolumes列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getPersistentVolumeClaims(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取PersistentVolumeClaims逻辑
            List<Object> persistentVolumeClaims = new ArrayList<>();

            return Result.success().put(Constants.DATA, persistentVolumeClaims);
        } catch (Exception e) {
            logger.error("获取PersistentVolumeClaims列表出错", e);
            return Result.error("获取PersistentVolumeClaims列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getStorageClasses(Integer clusterId) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取StorageClasses逻辑
            List<Object> storageClasses = new ArrayList<>();

            return Result.success().put(Constants.DATA, storageClasses);
        } catch (Exception e) {
            logger.error("获取StorageClasses列表出错", e);
            return Result.error("获取StorageClasses列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getIngresses(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取Ingresses逻辑
            List<Object> ingresses = new ArrayList<>();

            return Result.success().put(Constants.DATA, ingresses);
        } catch (Exception e) {
            logger.error("获取Ingresses列表出错", e);
            return Result.error("获取Ingresses列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getIngressClasses(Integer clusterId) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取IngressClasses逻辑
            List<Object> ingressClasses = new ArrayList<>();

            return Result.success().put(Constants.DATA, ingressClasses);
        } catch (Exception e) {
            logger.error("获取IngressClasses列表出错", e);
            return Result.error("获取IngressClasses列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getDaemonSets(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取DaemonSets逻辑
            List<Object> daemonSets = new ArrayList<>();

            return Result.success().put(Constants.DATA, daemonSets);
        } catch (Exception e) {
            logger.error("获取DaemonSets列表出错", e);
            return Result.error("获取DaemonSets列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getStatefulSets(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取StatefulSets逻辑
            List<Object> statefulSets = new ArrayList<>();

            return Result.success().put(Constants.DATA, statefulSets);
        } catch (Exception e) {
            logger.error("获取StatefulSets列表出错", e);
            return Result.error("获取StatefulSets列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getReplicaSets(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取ReplicaSets逻辑
            List<Object> replicaSets = new ArrayList<>();

            return Result.success().put(Constants.DATA, replicaSets);
        } catch (Exception e) {
            logger.error("获取ReplicaSets列表出错", e);
            return Result.error("获取ReplicaSets列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getReplicationControllers(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取ReplicationControllers逻辑
            List<Object> replicationControllers = new ArrayList<>();

            return Result.success().put(Constants.DATA, replicationControllers);
        } catch (Exception e) {
            logger.error("获取ReplicationControllers列表出错", e);
            return Result.error("获取ReplicationControllers列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getJobs(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取Jobs逻辑
            List<Object> jobs = new ArrayList<>();

            return Result.success().put(Constants.DATA, jobs);
        } catch (Exception e) {
            logger.error("获取Jobs列表出错", e);
            return Result.error("获取Jobs列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getCronJobs(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // TODO: 实现获取CronJobs逻辑
            List<Object> cronJobs = new ArrayList<>();

            return Result.success().put(Constants.DATA, cronJobs);
        } catch (Exception e) {
            logger.error("获取CronJobs列表出错", e);
            return Result.error("获取CronJobs列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getDeploymentDetail(Integer clusterId, String namespace, String name) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取指定的Deployment
            io.fabric8.kubernetes.api.model.apps.Deployment deployment = client.apps()
                    .deployments()
                    .inNamespace(namespace)
                    .withName(name)
                    .get();

            if (deployment == null) {
                return Result.error("找不到指定的Deployment");
            }

            // 转换为前端需要的数据结构
            DeploymentInfo info = new DeploymentInfo();
            info.setName(deployment.getMetadata().getName());
            info.setNamespace(deployment.getMetadata().getNamespace());
            info.setLabels(deployment.getMetadata().getLabels());

            if (deployment.getSpec() != null) {
                info.setReplicas(
                        deployment.getSpec().getReplicas() != null ? deployment.getSpec().getReplicas() : 0);

                // 记录更新策略
                if (deployment.getSpec().getStrategy() != null) {
                    info.setStrategy(deployment.getSpec().getStrategy().getType());
                }

                if (deployment.getSpec().getSelector() != null) {
                    info.setSelector(deployment.getSpec().getSelector().getMatchLabels());
                }

                // 获取容器信息和资源配额
                if (deployment.getSpec().getTemplate() != null &&
                        deployment.getSpec().getTemplate().getSpec() != null &&
                        deployment.getSpec().getTemplate().getSpec().getContainers() != null &&
                        !deployment.getSpec().getTemplate().getSpec().getContainers().isEmpty()) {

                    // 获取第一个容器的镜像
                    info.setImage(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());

                    // 提取容器的资源请求和限制
                    io.fabric8.kubernetes.api.model.Container container = deployment.getSpec().getTemplate().getSpec()
                            .getContainers().get(0);
                    if (container.getResources() != null) {
                        DeploymentInfo.ResourceQuota resourceQuota = new DeploymentInfo.ResourceQuota();

                        Map<String, io.fabric8.kubernetes.api.model.Quantity> requests = container.getResources()
                                .getRequests();
                        if (requests != null) {
                            resourceQuota
                                    .setCpuRequest(requests.get("cpu") != null ? requests.get("cpu").toString() : null);
                            resourceQuota.setMemoryRequest(
                                    requests.get("memory") != null ? requests.get("memory").toString() : null);
                        }

                        Map<String, io.fabric8.kubernetes.api.model.Quantity> limits = container.getResources()
                                .getLimits();
                        if (limits != null) {
                            resourceQuota.setCpuLimit(limits.get("cpu") != null ? limits.get("cpu").toString() : null);
                            resourceQuota.setMemoryLimit(
                                    limits.get("memory") != null ? limits.get("memory").toString() : null);
                        }

                        info.setResources(resourceQuota);
                    }
                }
            }

            if (deployment.getStatus() != null) {
                info.setAvailableReplicas(deployment.getStatus().getAvailableReplicas() != null
                        ? deployment.getStatus().getAvailableReplicas()
                        : 0);
                info.setReadyReplicas(
                        deployment.getStatus().getReadyReplicas() != null ? deployment.getStatus().getReadyReplicas()
                                : 0);
            }

            if (deployment.getMetadata().getCreationTimestamp() != null) {
                try {
                    // 将K8s时间字符串转换为Java Date对象
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date createDate = sdf.parse(deployment.getMetadata().getCreationTimestamp());
                    info.setCreateTime(createDate);
                } catch (ParseException e) {
                    // 转换失败时记录日志并使用当前时间
                    logger.error("解析创建时间失败: " + e.getMessage());
                    info.setCreateTime(new Date());
                }
            }

            return Result.success().put(Constants.DATA, info);
        } catch (Exception e) {
            logger.error("获取Deployment详情出错", e);
            return Result.error("获取Deployment详情出错: " + e.getMessage());
        }
    }

    @Override
    public Result getResourceEvents(Integer clusterId, String namespace, String kind, String name) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取与指定资源相关的事件
            List<io.fabric8.kubernetes.api.model.Event> events = client.v1().events().inNamespace(namespace).list()
                    .getItems().stream()
                    .filter(event -> {
                        io.fabric8.kubernetes.api.model.ObjectReference involvedObject = event.getInvolvedObject();
                        return involvedObject != null &&
                                kind.equals(involvedObject.getKind()) &&
                                name.equals(involvedObject.getName()) &&
                                namespace.equals(involvedObject.getNamespace());
                    })
                    .collect(Collectors.toList());

            return Result.success().put(Constants.DATA, events);
        } catch (Exception e) {
            logger.error("获取资源事件出错", e);
            return Result.error("获取资源事件出错: " + e.getMessage());
        }
    }
}