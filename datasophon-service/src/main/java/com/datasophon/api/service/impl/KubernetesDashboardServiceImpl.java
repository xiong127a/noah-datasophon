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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * K8S仪表盘服务实现类
 */
@Service("kubernetesDashboardService")
public class KubernetesDashboardServiceImpl implements KubernetesDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesDashboardServiceImpl.class);

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

            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取所有命名空间
            io.fabric8.kubernetes.api.model.NamespaceList namespaceList = client.namespaces().list();

            // 转换为前端需要的数据结构
            List<Map<String, Object>> namespaces = namespaceList.getItems().stream()
                    .map(ns -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", ns.getMetadata().getName());
                        item.put("status", ns.getStatus() != null ? ns.getStatus().getPhase() : "Unknown");
                        item.put("creationTimestamp", ns.getMetadata().getCreationTimestamp());
                        return item;
                    })
                    .collect(Collectors.toList());

            // 创建结果对象
            Map<String, Object> result = new HashMap<>();
            result.put("namespaces", namespaces); // 命名空间列表
            result.put("defaultNamespace", "datasophon"); // 默认命名空间
            result.put("showNamespaceSelector", true); // 是否显示命名空间选择器

            return Result.success().put(Constants.DATA, result);
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

            // 转换为与原生Kubernetes Dashboard兼容的数据结构
            List<Map<String, Object>> deployments = deploymentList.getItems().stream()
                    .map(deployment -> {
                        Map<String, Object> item = new HashMap<>();
                        Map<String, Object> objectMeta = new HashMap<>();
                        Map<String, Object> pods = new HashMap<>();

                        // 部署基本信息
                        if (deployment.getMetadata() != null) {
                            objectMeta.put("name", deployment.getMetadata().getName());
                            objectMeta.put("namespace", deployment.getMetadata().getNamespace());
                            objectMeta.put("labels", deployment.getMetadata().getLabels());
                            objectMeta.put("annotations", deployment.getMetadata().getAnnotations());
                            objectMeta.put("creationTimestamp", deployment.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", deployment.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 提取容器镜像
                        List<String> containerImages = new ArrayList<>();
                        if (deployment.getSpec() != null && deployment.getSpec().getTemplate() != null
                                && deployment.getSpec().getTemplate().getSpec() != null
                                && deployment.getSpec().getTemplate().getSpec().getContainers() != null) {
                            deployment.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                                if (container.getImage() != null) {
                                    containerImages.add(container.getImage());
                                }
                            });
                        }
                        item.put("containerImages", containerImages);

                        // Pod状态
                        if (deployment.getStatus() != null) {
                            pods.put("desired", deployment.getSpec() != null ? deployment.getSpec().getReplicas() : 0);
                            pods.put("running",
                                    deployment.getStatus().getAvailableReplicas() != null
                                            ? deployment.getStatus().getAvailableReplicas()
                                            : 0);
                            pods.put("failed", 0); // 默认值，实际应计算
                            pods.put("pending",
                                    deployment.getStatus().getUnavailableReplicas() != null
                                            ? deployment.getStatus().getUnavailableReplicas()
                                            : 0);
                        } else {
                            pods.put("desired", 0);
                            pods.put("running", 0);
                            pods.put("failed", 0);
                            pods.put("pending", 0);
                        }
                        item.put("pods", pods);

                        // 添加其他必要信息
                        Map<String, String> typeMeta = new HashMap<>();
                        typeMeta.put("kind", "Deployment");
                        item.put("typeMeta", typeMeta);

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建状态统计信息
            Map<String, Integer> status = new HashMap<>();
            status.put("running", (int) deployments.stream().filter(d -> {
                Map<String, Object> pods = (Map<String, Object>) d.get("pods");
                return pods != null && (int) pods.get("running") > 0;
            }).count());
            status.put("failed", 0);
            status.put("pending", (int) deployments.stream().filter(d -> {
                Map<String, Object> pods = (Map<String, Object>) d.get("pods");
                return pods != null && (int) pods.get("pending") > 0;
            }).count());

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            result.put("deployments", deployments);
            result.put("status", status);

            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取Deployments列表出错", e);
            return Result.error("获取Deployments列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getDeployments(Integer clusterId, Integer serviceId, String namespace) {
        // 目前serviceId暂时不使用，留作后期扩展使用
        logger.info("获取Deployments列表, clusterId={}, serviceId={}, namespace={}", clusterId, serviceId, namespace);
        return getDeployments(clusterId, namespace);
    }

    private KubernetesClient getKubernetesClient(Integer clusterId) {
        String kubeConfig = getKubeConfig(clusterId);
        Config config = Config.fromKubeconfig(kubeConfig);
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    @Override
    public Result getServices(Integer clusterId, String namespace) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取Services
            io.fabric8.kubernetes.api.model.ServiceList serviceList;
            if (namespace != null && !namespace.isEmpty()) {
                serviceList = client.services().inNamespace(namespace).list();
            } else {
                serviceList = client.services().inAnyNamespace().list();
            }

            // 按照Kubernetes Dashboard的格式构建结果
            Map<String, Object> result = new HashMap<>();

            // 构建services列表
            List<Map<String, Object>> services = serviceList.getItems().stream()
                    .map(service -> {
                        Map<String, Object> item = new HashMap<>();

                        // 1. objectMeta
                        Map<String, Object> objectMeta = new HashMap<>();
                        if (service.getMetadata() != null) {
                            objectMeta.put("name", service.getMetadata().getName());
                            objectMeta.put("namespace", service.getMetadata().getNamespace());
                            objectMeta.put("labels", service.getMetadata().getLabels());
                            objectMeta.put("creationTimestamp", service.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", service.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 2. typeMeta
                        Map<String, String> typeMeta = new HashMap<>();
                        typeMeta.put("kind", "service");
                        item.put("typeMeta", typeMeta);

                        // 3. internalEndpoint
                        Map<String, Object> internalEndpoint = new HashMap<>();
                        String host = String.format("%s.%s", service.getMetadata().getName(),
                                service.getMetadata().getNamespace());
                        internalEndpoint.put("host", host);

                        List<Map<String, Object>> ports = new ArrayList<>();
                        if (service.getSpec() != null && service.getSpec().getPorts() != null) {
                            service.getSpec().getPorts().forEach(port -> {
                                Map<String, Object> portInfo = new HashMap<>();
                                portInfo.put("port", port.getPort());
                                portInfo.put("protocol", port.getProtocol());
                                if (port.getNodePort() != null && port.getNodePort() > 0) {
                                    portInfo.put("nodePort", port.getNodePort());
                                }
                                ports.add(portInfo);
                            });
                        }
                        internalEndpoint.put("ports", ports);
                        item.put("internalEndpoint", internalEndpoint);

                        // 4. externalEndpoints
                        List<Object> externalEndpoints = new ArrayList<>();
                        // 根据服务类型处理外部访问点
                        if (service.getSpec() != null) {
                            String serviceType = service.getSpec().getType();
                            if ("LoadBalancer".equals(serviceType) && service.getStatus() != null &&
                                    service.getStatus().getLoadBalancer() != null &&
                                    service.getStatus().getLoadBalancer().getIngress() != null) {
                                service.getStatus().getLoadBalancer().getIngress().forEach(ingress -> {
                                    Map<String, Object> endpoint = new HashMap<>();
                                    endpoint.put("host",
                                            ingress.getHostname() != null ? ingress.getHostname() : ingress.getIp());
                                    endpoint.put("ports", ports); // 复用内部端口
                                    externalEndpoints.add(endpoint);
                                });
                            }
                        }
                        item.put("externalEndpoints", externalEndpoints);

                        // 5. selector
                        if (service.getSpec() != null && service.getSpec().getSelector() != null) {
                            item.put("selector", service.getSpec().getSelector());
                        } else {
                            item.put("selector", new HashMap<String, String>());
                        }

                        // 6. type
                        if (service.getSpec() != null) {
                            item.put("type", service.getSpec().getType());
                        }

                        // 7. clusterIP
                        if (service.getSpec() != null) {
                            item.put("clusterIP", service.getSpec().getClusterIP());
                        }

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建listMeta
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", services.size());
            result.put("listMeta", listMeta);

            // 添加services列表
            result.put("services", services);

            // 添加errors数组
            result.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取Services列表出错", e);
            return Result.error("获取Services列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getConfigMaps(Integer clusterId, String namespace) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取ConfigMaps
            io.fabric8.kubernetes.api.model.ConfigMapList configMapList;
            if (namespace != null && !namespace.isEmpty()) {
                configMapList = client.configMaps().inNamespace(namespace).list();
            } else {
                configMapList = client.configMaps().inNamespace("datasophon").list();
            }

            // 按照前端需要的格式构建结果
            Map<String, Object> result = new HashMap<>();

            // 构建items列表
            List<Map<String, Object>> items = configMapList.getItems().stream()
                    .filter(configMap -> configMap.getMetadata() != null)
                    .map(configMap -> {
                        Map<String, Object> item = new HashMap<>();

                        // 1. objectMeta
                        Map<String, Object> objectMeta = new HashMap<>();
                        if (configMap.getMetadata() != null) {
                            objectMeta.put("name", configMap.getMetadata().getName());
                            objectMeta.put("namespace", configMap.getMetadata().getNamespace());
                            objectMeta.put("labels", configMap.getMetadata().getLabels());
                            objectMeta.put("creationTimestamp", configMap.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", configMap.getMetadata().getUid());

                            // 如果有annotations，也添加
                            if (configMap.getMetadata().getAnnotations() != null) {
                                objectMeta.put("annotations", configMap.getMetadata().getAnnotations());
                            }
                        }
                        item.put("objectMeta", objectMeta);

                        // 2. typeMeta
                        Map<String, String> typeMeta = new HashMap<>();
                        typeMeta.put("kind", "configmap");
                        item.put("typeMeta", typeMeta);

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建listMeta
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", items.size());

            // 构建最终结果
            result.put("listMeta", listMeta);
            result.put("items", items);
            result.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取ConfigMaps列表出错", e);
            return Result.error("获取ConfigMaps列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getSecrets(Integer clusterId, String namespace) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取Secrets
            io.fabric8.kubernetes.api.model.SecretList secretList;
            if (namespace != null && !namespace.isEmpty()) {
                secretList = client.secrets().inNamespace(namespace).list();
            } else {
                secretList = client.secrets().inAnyNamespace().list();
            }

            // 转换为前端需要的数据结构
            List<Map<String, Object>> secrets = secretList.getItems().stream()
                    .map(secret -> {
                        Map<String, Object> item = new HashMap<>();
                        Map<String, Object> objectMeta = new HashMap<>();
                        Map<String, Object> typeMeta = new HashMap<>();

                        // 基本信息
                        if (secret.getMetadata() != null) {
                            objectMeta.put("name", secret.getMetadata().getName());
                            objectMeta.put("namespace", secret.getMetadata().getNamespace());
                            objectMeta.put("labels", secret.getMetadata().getLabels());
                            objectMeta.put("annotations", secret.getMetadata().getAnnotations());
                            objectMeta.put("creationTimestamp", secret.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", secret.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 类型信息
                        typeMeta.put("kind", "secret");
                        item.put("typeMeta", typeMeta);

                        // Secret类型
                        item.put("type", secret.getType());

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", secrets.size());
            result.put("listMeta", listMeta);
            result.put("secrets", secrets);
            result.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, result);
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

            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取PersistentVolumes
            io.fabric8.kubernetes.api.model.PersistentVolumeList pvList = client.persistentVolumes().list();

            // 转换为前端需要的数据结构
            List<Map<String, Object>> items = pvList.getItems().stream()
                    .map(pv -> {
                        Map<String, Object> item = new HashMap<>();
                        Map<String, Object> objectMeta = new HashMap<>();
                        Map<String, Object> typeMeta = new HashMap<>();
                        Map<String, Object> capacity = new HashMap<>();

                        // 基本信息
                        if (pv.getMetadata() != null) {
                            objectMeta.put("name", pv.getMetadata().getName());
                            objectMeta.put("labels", pv.getMetadata().getLabels());
                            objectMeta.put("annotations", pv.getMetadata().getAnnotations());
                            objectMeta.put("creationTimestamp", pv.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", pv.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 类型信息
                        typeMeta.put("kind", "persistentvolume");
                        item.put("typeMeta", typeMeta);

                        // 状态
                        if (pv.getStatus() != null) {
                            item.put("status", pv.getStatus().getPhase());
                        }

                        // 容量
                        if (pv.getSpec() != null && pv.getSpec().getCapacity() != null) {
                            capacity.put("storage", pv.getSpec().getCapacity().get("storage").toString());
                            item.put("capacity", capacity);
                        }

                        // 访问模式
                        if (pv.getSpec() != null && pv.getSpec().getAccessModes() != null) {
                            item.put("accessModes", pv.getSpec().getAccessModes());
                        } else {
                            item.put("accessModes", new ArrayList<>());
                        }

                        // 回收策略
                        if (pv.getSpec() != null && pv.getSpec().getPersistentVolumeReclaimPolicy() != null) {
                            item.put("reclaimPolicy", pv.getSpec().getPersistentVolumeReclaimPolicy());
                        }

                        // 存储类
                        if (pv.getSpec() != null && pv.getSpec().getStorageClassName() != null) {
                            item.put("storageClass", pv.getSpec().getStorageClassName());
                        }

                        // 声明信息
                        if (pv.getSpec() != null && pv.getSpec().getClaimRef() != null) {
                            Map<String, Object> claimRef = new HashMap<>();
                            claimRef.put("name", pv.getSpec().getClaimRef().getName());
                            claimRef.put("namespace", pv.getSpec().getClaimRef().getNamespace());
                            item.put("claimRef", claimRef);
                        }

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", items.size());
            result.put("listMeta", listMeta);
            result.put("items", items);
            result.put("errors", new ArrayList<>());

            logger.info("获取PersistentVolumes列表成功，共{}个PV", items.size());
            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取PersistentVolumes列表出错", e);
            return Result.error("获取PersistentVolumes列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getPersistentVolumeClaims(Integer clusterId, String namespace) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取PersistentVolumeClaims
            io.fabric8.kubernetes.api.model.PersistentVolumeClaimList pvcList;
            if (namespace != null && !namespace.isEmpty()) {
                pvcList = client.persistentVolumeClaims().inNamespace(namespace).list();
            } else {
                pvcList = client.persistentVolumeClaims().inAnyNamespace().list();
            }

            // 转换为前端需要的数据结构
            List<Map<String, Object>> items = pvcList.getItems().stream()
                    .map(pvc -> {
                        Map<String, Object> item = new HashMap<>();
                        Map<String, Object> objectMeta = new HashMap<>();
                        Map<String, Object> typeMeta = new HashMap<>();
                        Map<String, Object> capacity = new HashMap<>();

                        // 基本信息
                        if (pvc.getMetadata() != null) {
                            objectMeta.put("name", pvc.getMetadata().getName());
                            objectMeta.put("namespace", pvc.getMetadata().getNamespace());
                            objectMeta.put("labels", pvc.getMetadata().getLabels());
                            objectMeta.put("annotations", pvc.getMetadata().getAnnotations());
                            objectMeta.put("creationTimestamp", pvc.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", pvc.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 类型信息
                        typeMeta.put("kind", "persistentvolumeclaim");
                        item.put("typeMeta", typeMeta);

                        // 状态
                        if (pvc.getStatus() != null) {
                            item.put("status", pvc.getStatus().getPhase());
                        }

                        // Volume名称
                        if (pvc.getSpec() != null && pvc.getSpec().getVolumeName() != null) {
                            item.put("volume", pvc.getSpec().getVolumeName());
                        }

                        // 容量
                        if (pvc.getStatus() != null && pvc.getStatus().getCapacity() != null) {
                            capacity.put("storage", pvc.getStatus().getCapacity().get("storage").toString());
                            item.put("capacity", capacity);
                        }

                        // 访问模式
                        if (pvc.getSpec() != null && pvc.getSpec().getAccessModes() != null) {
                            item.put("accessModes", pvc.getSpec().getAccessModes());
                        } else {
                            item.put("accessModes", new ArrayList<>());
                        }

                        // 存储类
                        if (pvc.getSpec() != null && pvc.getSpec().getStorageClassName() != null) {
                            item.put("storageClass", pvc.getSpec().getStorageClassName());
                        }

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", items.size());
            result.put("listMeta", listMeta);
            result.put("items", items);
            result.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取PersistentVolumeClaims列表出错", e);
            return Result.error("获取PersistentVolumeClaims列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getStorageClasses(Integer clusterId) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取StorageClasses
            io.fabric8.kubernetes.api.model.storage.StorageClassList storageClassList = client.storage()
                    .storageClasses().list();

            // 转换为前端需要的数据结构
            List<Map<String, Object>> items = storageClassList.getItems().stream()
                    .map(storageClass -> {
                        Map<String, Object> item = new HashMap<>();
                        Map<String, Object> objectMeta = new HashMap<>();
                        Map<String, Object> typeMeta = new HashMap<>();

                        // 基本信息
                        if (storageClass.getMetadata() != null) {
                            objectMeta.put("name", storageClass.getMetadata().getName());
                            objectMeta.put("labels", storageClass.getMetadata().getLabels());
                            objectMeta.put("annotations", storageClass.getMetadata().getAnnotations());
                            objectMeta.put("creationTimestamp", storageClass.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", storageClass.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 类型信息
                        typeMeta.put("kind", "storageclass");
                        item.put("typeMeta", typeMeta);

                        // 提供者信息
                        if (storageClass.getProvisioner() != null) {
                            item.put("provisioner", storageClass.getProvisioner());
                        }

                        // 参数信息
                        if (storageClass.getParameters() != null) {
                            item.put("parameters", storageClass.getParameters());
                        } else {
                            item.put("parameters", new HashMap<>());
                        }

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", items.size());
            result.put("listMeta", listMeta);
            result.put("items", items);
            result.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取StorageClasses列表出错", e);
            return Result.error("获取StorageClasses列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getIngresses(Integer clusterId, String namespace) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);
            if (client == null) {
                return Result.error("无法创建Kubernetes客户端");
            }

            // 获取Ingresses
            io.fabric8.kubernetes.api.model.networking.v1.IngressList ingressList;
            if (namespace != null && !namespace.isEmpty() && !"all".equalsIgnoreCase(namespace)) {
                ingressList = client.network().v1().ingresses().inNamespace(namespace).list();
            } else {
                ingressList = client.network().v1().ingresses().inAnyNamespace().list();
            }

            // 转换为前端需要的数据结构
            List<Map<String, Object>> items = ingressList.getItems().stream()
                    .map(ingress -> {
                        Map<String, Object> item = new HashMap<>();
                        Map<String, Object> objectMeta = new HashMap<>();
                        Map<String, Object> typeMeta = new HashMap<>();

                        // 基本信息
                        if (ingress.getMetadata() != null) {
                            objectMeta.put("name", ingress.getMetadata().getName());
                            objectMeta.put("namespace", ingress.getMetadata().getNamespace());
                            objectMeta.put("labels", ingress.getMetadata().getLabels());
                            objectMeta.put("annotations", ingress.getMetadata().getAnnotations());
                            objectMeta.put("creationTimestamp", ingress.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", ingress.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 类型信息
                        typeMeta.put("kind", "ingress");
                        item.put("typeMeta", typeMeta);

                        // 收集Endpoints信息 - 修改为只获取节点IP
                        List<Map<String, Object>> endpoints = new ArrayList<>();
                        try {
                            // 获取集群所有Worker节点IP
                            io.fabric8.kubernetes.api.model.NodeList nodeList = client.nodes().list();
                            // 过滤掉master节点，只保留worker节点
                            List<String> nodeIps = nodeList.getItems().stream()
                                    .filter(node -> {
                                        // 排除具有master标签的节点
                                        if (node.getMetadata() != null && node.getMetadata().getLabels() != null) {
                                            Map<String, String> labels = node.getMetadata().getLabels();
                                            return !labels.containsKey("node-role.kubernetes.io/master") &&
                                                    !labels.containsKey("node-role.kubernetes.io/control-plane");
                                        }
                                        return true;
                                    })
                                    .flatMap(node -> {
                                        List<String> ips = new ArrayList<>();
                                        if (node.getStatus() != null && node.getStatus().getAddresses() != null) {
                                            node.getStatus().getAddresses().forEach(address -> {
                                                if ("InternalIP".equals(address.getType())
                                                        && address.getAddress() != null) {
                                                    ips.add(address.getAddress());
                                                }
                                            });
                                        }
                                        return ips.stream();
                                    })
                                    .collect(Collectors.toList());

                            // 为每个节点IP创建一个endpoint对象
                            for (String ip : nodeIps) {
                                Map<String, Object> endpointInfo = new HashMap<>();
                                endpointInfo.put("host", ip);
                                endpoints.add(endpointInfo);
                            }
                        } catch (Exception e) {
                            logger.warn("获取Ingress Endpoints失败: {}", e.getMessage());
                        }
                        item.put("endpoints", endpoints);

                        // 收集Hosts信息
                        List<String> hosts = new ArrayList<>();
                        if (ingress.getSpec() != null && ingress.getSpec().getRules() != null) {
                            for (io.fabric8.kubernetes.api.model.networking.v1.IngressRule rule : ingress.getSpec()
                                    .getRules()) {
                                if (rule.getHost() != null && !rule.getHost().isEmpty()) {
                                    hosts.add(rule.getHost());
                                }
                            }
                        }
                        item.put("hosts", hosts);

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", items.size());
            result.put("listMeta", listMeta);
            result.put("items", items);
            result.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取Ingresses列表出错", e);
            return Result.error("获取Ingresses列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getIngressClasses(Integer clusterId) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取IngressClasses
            io.fabric8.kubernetes.api.model.networking.v1.IngressClassList ingressClassList = client.network().v1()
                    .ingressClasses().list();

            // 转换为前端需要的数据结构
            List<Map<String, Object>> items = ingressClassList.getItems().stream()
                    .map(ingressClass -> {
                        Map<String, Object> item = new HashMap<>();
                        Map<String, Object> objectMeta = new HashMap<>();
                        Map<String, Object> typeMeta = new HashMap<>();

                        // 基本信息
                        if (ingressClass.getMetadata() != null) {
                            objectMeta.put("name", ingressClass.getMetadata().getName());
                            objectMeta.put("labels", ingressClass.getMetadata().getLabels());
                            objectMeta.put("annotations", ingressClass.getMetadata().getAnnotations());
                            objectMeta.put("creationTimestamp", ingressClass.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", ingressClass.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 类型信息
                        typeMeta.put("kind", "ingressclass");
                        item.put("typeMeta", typeMeta);

                        // 控制器信息
                        if (ingressClass.getSpec() != null && ingressClass.getSpec().getController() != null) {
                            item.put("controller", ingressClass.getSpec().getController());
                        }

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", items.size());
            result.put("listMeta", listMeta);
            result.put("items", items);
            result.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取IngressClasses列表出错", e);
            return Result.error("获取IngressClasses列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getDaemonSets(Integer clusterId, Integer serviceId, String namespace) {
        // 目前serviceId暂时不使用，留作后期扩展使用
        logger.info("获取DaemonSets列表, clusterId={}, serviceId={}, namespace={}", clusterId, serviceId, namespace);
        return getDaemonSets(clusterId, namespace);
    }

    @Override
    public Result getDaemonSets(Integer clusterId, String namespace) {
        try {

            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取DaemonSets
            io.fabric8.kubernetes.api.model.apps.DaemonSetList daemonSetList;
            if (namespace != null && !namespace.isEmpty()) {
                daemonSetList = client.apps().daemonSets().inNamespace(namespace).list();
            } else {
                daemonSetList = client.apps().daemonSets().inAnyNamespace().list();
            }

            // 转换为前端需要的数据结构
            List<Map<String, Object>> daemonSets = daemonSetList.getItems().stream()
                    .map(daemonSet -> {
                        Map<String, Object> item = new HashMap<>();
                        Map<String, Object> objectMeta = new HashMap<>();
                        Map<String, Object> podInfo = new HashMap<>();
                        Map<String, Object> typeMeta = new HashMap<>();

                        // 基本信息
                        if (daemonSet.getMetadata() != null) {
                            objectMeta.put("name", daemonSet.getMetadata().getName());
                            objectMeta.put("namespace", daemonSet.getMetadata().getNamespace());
                            objectMeta.put("labels", daemonSet.getMetadata().getLabels());
                            objectMeta.put("annotations", daemonSet.getMetadata().getAnnotations());
                            objectMeta.put("creationTimestamp", daemonSet.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", daemonSet.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 类型信息
                        typeMeta.put("kind", "daemonset");
                        typeMeta.put("scalable", false); // DaemonSet不可缩放
                        item.put("typeMeta", typeMeta);

                        // Pod信息
                        if (daemonSet.getStatus() != null) {
                            podInfo.put("desired", daemonSet.getStatus().getDesiredNumberScheduled());
                            podInfo.put("current", daemonSet.getStatus().getCurrentNumberScheduled());
                            podInfo.put("ready", daemonSet.getStatus().getNumberReady());
                            podInfo.put("available",
                                    daemonSet.getStatus().getNumberAvailable() != null
                                            ? daemonSet.getStatus().getNumberAvailable()
                                            : 0);

                            // 计算unavailable = current - available
                            int current = daemonSet.getStatus().getCurrentNumberScheduled();
                            int available = daemonSet.getStatus().getNumberAvailable() != null
                                    ? daemonSet.getStatus().getNumberAvailable()
                                    : 0;
                            podInfo.put("unavailable", Math.max(0, current - available));
                        } else {
                            podInfo.put("desired", 0);
                            podInfo.put("current", 0);
                            podInfo.put("ready", 0);
                            podInfo.put("available", 0);
                            podInfo.put("unavailable", 0);
                        }
                        podInfo.put("warnings", new ArrayList<>());
                        item.put("podInfo", podInfo);

                        // 提取容器镜像
                        List<String> containerImages = new ArrayList<>();
                        if (daemonSet.getSpec() != null && daemonSet.getSpec().getTemplate() != null
                                && daemonSet.getSpec().getTemplate().getSpec() != null
                                && daemonSet.getSpec().getTemplate().getSpec().getContainers() != null) {
                            daemonSet.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                                if (container.getImage() != null) {
                                    containerImages.add(container.getImage());
                                }
                            });
                        }
                        item.put("containerImages", containerImages);

                        // 初始化容器镜像
                        List<String> initContainerImages = new ArrayList<>();
                        if (daemonSet.getSpec() != null && daemonSet.getSpec().getTemplate() != null
                                && daemonSet.getSpec().getTemplate().getSpec() != null
                                && daemonSet.getSpec().getTemplate().getSpec().getInitContainers() != null) {
                            daemonSet.getSpec().getTemplate().getSpec().getInitContainers().forEach(container -> {
                                if (container.getImage() != null) {
                                    initContainerImages.add(container.getImage());
                                }
                            });
                        }
                        item.put("initContainerImages", initContainerImages);

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建状态统计信息
            Map<String, Integer> status = new HashMap<>();
            status.put("running", (int) daemonSets.stream().filter(ds -> {
                Map<String, Object> podInfo = (Map<String, Object>) ds.get("podInfo");
                return podInfo != null && (int) podInfo.get("ready") > 0;
            }).count());
            status.put("pending", (int) daemonSets.stream().filter(ds -> {
                Map<String, Object> podInfo = (Map<String, Object>) ds.get("podInfo");
                return podInfo != null && (int) podInfo.get("unavailable") > 0;
            }).count());
            status.put("failed", 0);
            status.put("succeeded", 0);

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", daemonSets.size());
            result.put("listMeta", listMeta);
            result.put("daemonSets", daemonSets);
            result.put("status", status);
            result.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取DaemonSets列表出错", e);
            return Result.error("获取DaemonSets列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getStatefulSets(Integer clusterId, String namespace) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取StatefulSets
            io.fabric8.kubernetes.api.model.apps.StatefulSetList statefulSetList;
            if (namespace != null && !namespace.isEmpty()) {
                statefulSetList = client.apps().statefulSets().inNamespace(namespace).list();
            } else {
                statefulSetList = client.apps().statefulSets().inAnyNamespace().list();
            }

            // 转换为前端需要的数据结构
            List<Map<String, Object>> statefulSets = statefulSetList.getItems().stream()
                    .map(statefulSet -> {
                        Map<String, Object> item = new HashMap<>();
                        Map<String, Object> objectMeta = new HashMap<>();
                        Map<String, Object> podInfo = new HashMap<>();
                        Map<String, Object> typeMeta = new HashMap<>();

                        // 基本信息
                        if (statefulSet.getMetadata() != null) {
                            objectMeta.put("name", statefulSet.getMetadata().getName());
                            objectMeta.put("namespace", statefulSet.getMetadata().getNamespace());
                            objectMeta.put("labels", statefulSet.getMetadata().getLabels());
                            objectMeta.put("annotations", statefulSet.getMetadata().getAnnotations());
                            objectMeta.put("creationTimestamp", statefulSet.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", statefulSet.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 类型信息
                        typeMeta.put("kind", "statefulset");
                        typeMeta.put("scalable", true); // StatefulSet是可缩放的
                        item.put("typeMeta", typeMeta);

                        // Pod信息
                        if (statefulSet.getStatus() != null) {
                            int desired = statefulSet.getSpec() != null && statefulSet.getSpec().getReplicas() != null
                                    ? statefulSet.getSpec().getReplicas()
                                    : 0;
                            int current = statefulSet.getStatus().getReplicas() != null
                                    ? statefulSet.getStatus().getReplicas()
                                    : 0;
                            int ready = statefulSet.getStatus().getReadyReplicas() != null
                                    ? statefulSet.getStatus().getReadyReplicas()
                                    : 0;
                            int running = ready; // 将ready状态的副本视为running

                            podInfo.put("desired", desired);
                            podInfo.put("current", current);
                            podInfo.put("running", running);
                            podInfo.put("pending", current - ready); // 当前副本数减去就绪副本数为等待中的副本数
                            podInfo.put("failed", 0); // 默认没有失败的
                            podInfo.put("succeeded", 0); // 没有成功完成的概念
                            podInfo.put("warnings", new ArrayList<>()); // 空警告列表
                        } else {
                            podInfo.put("desired", 0);
                            podInfo.put("current", 0);
                            podInfo.put("running", 0);
                            podInfo.put("pending", 0);
                            podInfo.put("failed", 0);
                            podInfo.put("succeeded", 0);
                            podInfo.put("warnings", new ArrayList<>());
                        }
                        item.put("podInfo", podInfo);

                        // 提取容器镜像
                        List<String> containerImages = new ArrayList<>();
                        if (statefulSet.getSpec() != null && statefulSet.getSpec().getTemplate() != null
                                && statefulSet.getSpec().getTemplate().getSpec() != null
                                && statefulSet.getSpec().getTemplate().getSpec().getContainers() != null) {
                            statefulSet.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                                if (container.getImage() != null) {
                                    containerImages.add(container.getImage());
                                }
                            });
                        }
                        item.put("containerImages", containerImages);

                        // 初始化容器镜像
                        List<String> initContainerImages = new ArrayList<>();
                        if (statefulSet.getSpec() != null && statefulSet.getSpec().getTemplate() != null
                                && statefulSet.getSpec().getTemplate().getSpec() != null
                                && statefulSet.getSpec().getTemplate().getSpec().getInitContainers() != null) {
                            statefulSet.getSpec().getTemplate().getSpec().getInitContainers().forEach(container -> {
                                if (container.getImage() != null) {
                                    initContainerImages.add(container.getImage());
                                }
                            });
                        }
                        item.put("initContainerImages", initContainerImages);

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建状态统计信息
            Map<String, Integer> status = new HashMap<>();
            status.put("running", (int) statefulSets.stream().filter(sts -> {
                Map<String, Object> podInfo = (Map<String, Object>) sts.get("podInfo");
                return podInfo != null && (int) podInfo.get("running") > 0;
            }).count());
            status.put("pending", (int) statefulSets.stream().filter(sts -> {
                Map<String, Object> podInfo = (Map<String, Object>) sts.get("podInfo");
                return podInfo != null && (int) podInfo.get("pending") > 0;
            }).count());
            status.put("failed", 0);
            status.put("succeeded", 0);

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", statefulSets.size());
            result.put("listMeta", listMeta);
            result.put("statefulSets", statefulSets);
            result.put("status", status);
            result.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取StatefulSets列表出错", e);
            return Result.error("获取StatefulSets列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getReplicaSets(Integer clusterId, String namespace) {
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取ReplicaSets
            io.fabric8.kubernetes.api.model.apps.ReplicaSetList replicaSetList;
            if (namespace != null && !namespace.isEmpty()) {
                replicaSetList = client.apps().replicaSets().inNamespace(namespace).list();
            } else {
                replicaSetList = client.apps().replicaSets().inAnyNamespace().list();
            }

            // 转换为前端需要的数据结构
            List<Map<String, Object>> replicaSets = replicaSetList.getItems().stream()
                    .map(replicaSet -> {
                        Map<String, Object> item = new HashMap<>();
                        Map<String, Object> objectMeta = new HashMap<>();
                        Map<String, Object> podInfo = new HashMap<>();
                        Map<String, Object> typeMeta = new HashMap<>();

                        // 基本信息
                        if (replicaSet.getMetadata() != null) {
                            objectMeta.put("name", replicaSet.getMetadata().getName());
                            objectMeta.put("namespace", replicaSet.getMetadata().getNamespace());
                            objectMeta.put("labels", replicaSet.getMetadata().getLabels());
                            objectMeta.put("annotations", replicaSet.getMetadata().getAnnotations());
                            objectMeta.put("creationTimestamp", replicaSet.getMetadata().getCreationTimestamp());
                            objectMeta.put("uid", replicaSet.getMetadata().getUid());
                        }
                        item.put("objectMeta", objectMeta);

                        // 类型信息
                        typeMeta.put("kind", "replicaset");
                        typeMeta.put("scalable", true);
                        item.put("typeMeta", typeMeta);

                        // Pod信息
                        if (replicaSet.getStatus() != null) {
                            podInfo.put("desired",
                                    replicaSet.getSpec() != null ? replicaSet.getSpec().getReplicas() : 0);
                            podInfo.put("current",
                                    replicaSet.getStatus().getReplicas() != null ? replicaSet.getStatus().getReplicas()
                                            : 0);
                            podInfo.put("running",
                                    replicaSet.getStatus().getAvailableReplicas() != null
                                            ? replicaSet.getStatus().getAvailableReplicas()
                                            : 0);
                            // 计算pending状态的Pod数量 = 当前总数 - 可用数量
                            int current = replicaSet.getStatus().getReplicas() != null
                                    ? replicaSet.getStatus().getReplicas()
                                    : 0;
                            int available = replicaSet.getStatus().getAvailableReplicas() != null
                                    ? replicaSet.getStatus().getAvailableReplicas()
                                    : 0;
                            podInfo.put("pending", Math.max(0, current - available));
                            podInfo.put("failed", 0); // 默认值，需要检查Pod状态计算
                            podInfo.put("succeeded", 0); // 默认值，需要检查Pod状态计算
                        } else {
                            podInfo.put("desired", 0);
                            podInfo.put("current", 0);
                            podInfo.put("running", 0);
                            podInfo.put("pending", 0);
                            podInfo.put("failed", 0);
                            podInfo.put("succeeded", 0);
                        }
                        podInfo.put("warnings", new ArrayList<>());
                        item.put("podInfo", podInfo);

                        // 提取容器镜像
                        List<String> containerImages = new ArrayList<>();
                        if (replicaSet.getSpec() != null && replicaSet.getSpec().getTemplate() != null
                                && replicaSet.getSpec().getTemplate().getSpec() != null
                                && replicaSet.getSpec().getTemplate().getSpec().getContainers() != null) {
                            replicaSet.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                                if (container.getImage() != null) {
                                    containerImages.add(container.getImage());
                                }
                            });
                        }
                        item.put("containerImages", containerImages);
                        item.put("initContainerImages", null);

                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建状态统计信息
            Map<String, Integer> status = new HashMap<>();
            status.put("running", (int) replicaSets.stream().filter(rs -> {
                Map<String, Object> podInfo = (Map<String, Object>) rs.get("podInfo");
                return podInfo != null && (int) podInfo.get("running") > 0;
            }).count());
            status.put("pending", (int) replicaSets.stream().filter(rs -> {
                Map<String, Object> podInfo = (Map<String, Object>) rs.get("podInfo");
                return podInfo != null && (int) podInfo.get("pending") > 0;
            }).count());
            status.put("failed", 0);
            status.put("succeeded", 0);
            status.put("unknown", 0);
            status.put("terminating", 0);

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", replicaSets.size());
            result.put("listMeta", listMeta);
            result.put("replicaSets", replicaSets);
            result.put("status", status);
            result.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, result);
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

            // 创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);
            if (client == null) {
                return Result.error("创建Kubernetes客户端失败");
            }

            // 获取Jobs列表
            List<io.fabric8.kubernetes.api.model.batch.v1.Job> jobsList;
            if (namespace != null && !namespace.equals("all")) {
                jobsList = client.batch().v1().jobs().inNamespace(namespace).list().getItems();
            } else {
                jobsList = client.batch().v1().jobs().inAnyNamespace().list().getItems();
            }

            // 处理Jobs数据
            List<Map<String, Object>> jobs = new ArrayList<>();

            // 统计各种状态数量
            int runningCount = 0;
            int pendingCount = 0;
            int failedCount = 0;
            int succeededCount = 0;
            int unknownCount = 0;
            int terminatingCount = 0;

            for (io.fabric8.kubernetes.api.model.batch.v1.Job job : jobsList) {
                Map<String, Object> jobMap = new HashMap<>();

                // 处理元数据
                Map<String, Object> objectMeta = new HashMap<>();
                objectMeta.put("name", job.getMetadata().getName());
                objectMeta.put("namespace", job.getMetadata().getNamespace());
                objectMeta.put("labels", job.getMetadata().getLabels());
                objectMeta.put("annotations", job.getMetadata().getAnnotations());
                objectMeta.put("creationTimestamp", job.getMetadata().getCreationTimestamp());
                objectMeta.put("uid", job.getMetadata().getUid());
                jobMap.put("objectMeta", objectMeta);

                // 类型元数据
                Map<String, Object> typeMeta = new HashMap<>();
                typeMeta.put("kind", "job");
                jobMap.put("typeMeta", typeMeta);

                // 获取Job的Pod信息
                Map<String, Object> podInfo = new HashMap<>();
                int active = job.getStatus() != null && job.getStatus().getActive() != null
                        ? job.getStatus().getActive()
                        : 0;
                int succeeded = job.getStatus() != null && job.getStatus().getSucceeded() != null
                        ? job.getStatus().getSucceeded()
                        : 0;
                int failed = job.getStatus() != null && job.getStatus().getFailed() != null
                        ? job.getStatus().getFailed()
                        : 0;

                // 计算期望的Pod数量
                int desired = (job.getSpec() != null && job.getSpec().getCompletions() != null)
                        ? job.getSpec().getCompletions()
                        : 1;

                // 设置Pod信息
                podInfo.put("current", active + succeeded + failed);
                podInfo.put("desired", desired);
                podInfo.put("running", active);
                podInfo.put("pending", 0); // 需要进一步查询Pods来获取pending状态的数量
                podInfo.put("failed", failed);
                podInfo.put("succeeded", succeeded);
                podInfo.put("warnings", new ArrayList<>());

                // 查询相关Pod可能的警告信息
                try {
                    List<io.fabric8.kubernetes.api.model.Pod> pods = client.pods()
                            .inNamespace(job.getMetadata().getNamespace())
                            .withLabel("job-name", job.getMetadata().getName())
                            .list()
                            .getItems();

                    // 统计pending状态的Pod
                    int pendingPods = 0;

                    List<Map<String, Object>> warnings = new ArrayList<>();
                    for (io.fabric8.kubernetes.api.model.Pod pod : pods) {
                        // 统计pending状态的Pod
                        if (pod.getStatus() != null && "Pending".equals(pod.getStatus().getPhase())) {
                            pendingPods++;

                            // 检查容器状态是否有警告
                            if (pod.getStatus().getContainerStatuses() != null) {
                                for (io.fabric8.kubernetes.api.model.ContainerStatus cs : pod.getStatus()
                                        .getContainerStatuses()) {
                                    if (cs.getState() != null && cs.getState().getWaiting() != null) {
                                        String reason = cs.getState().getWaiting().getReason();
                                        String message = cs.getState().getWaiting().getMessage();

                                        if (reason != null && !reason.isEmpty()) {
                                            Map<String, Object> warning = new HashMap<>();

                                            Map<String, Object> warnMeta = new HashMap<>();
                                            warnMeta.put("creationTimestamp", null);
                                            warning.put("objectMeta", warnMeta);

                                            warning.put("typeMeta", new HashMap<>());
                                            warning.put("message", message != null ? message : "");
                                            warning.put("sourceComponent", "");
                                            warning.put("sourceHost", "");
                                            warning.put("object", "");
                                            warning.put("count", 0);
                                            warning.put("firstSeen", null);
                                            warning.put("lastSeen", null);
                                            warning.put("reason", reason);
                                            warning.put("type", "Warning");

                                            warnings.add(warning);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 更新pending状态的Pod数量
                    podInfo.put("pending", pendingPods);

                    // 添加警告信息
                    podInfo.put("warnings", warnings);
                } catch (Exception e) {
                    logger.error("获取Job相关Pod警告信息失败", e);
                }

                jobMap.put("podInfo", podInfo);

                // 提取容器镜像
                List<String> containerImages = new ArrayList<>();
                if (job.getSpec() != null &&
                        job.getSpec().getTemplate() != null &&
                        job.getSpec().getTemplate().getSpec() != null &&
                        job.getSpec().getTemplate().getSpec().getContainers() != null) {

                    for (io.fabric8.kubernetes.api.model.Container container : job.getSpec().getTemplate().getSpec()
                            .getContainers()) {
                        if (container.getImage() != null) {
                            containerImages.add(container.getImage());
                        }
                    }
                }
                jobMap.put("containerImages", containerImages);

                // 初始化容器镜像
                List<String> initContainerImages = new ArrayList<>();
                if (job.getSpec() != null &&
                        job.getSpec().getTemplate() != null &&
                        job.getSpec().getTemplate().getSpec() != null &&
                        job.getSpec().getTemplate().getSpec().getInitContainers() != null) {

                    for (io.fabric8.kubernetes.api.model.Container container : job.getSpec().getTemplate().getSpec()
                            .getInitContainers()) {
                        if (container.getImage() != null) {
                            initContainerImages.add(container.getImage());
                        }
                    }
                }
                jobMap.put("initContainerImages", initContainerImages.isEmpty() ? null : initContainerImages);

                // 获取并行度
                if (job.getSpec() != null && job.getSpec().getParallelism() != null) {
                    jobMap.put("parallelism", job.getSpec().getParallelism());
                } else {
                    jobMap.put("parallelism", 1);
                }

                // 获取Job状态
                Map<String, Object> jobStatus = new HashMap<>();
                String status = "Unknown";
                String message = "";

                if (job.getStatus() != null) {
                    if (job.getStatus().getSucceeded() != null && job.getStatus().getSucceeded() > 0) {
                        status = "Complete";
                        succeededCount++;
                    } else if (job.getStatus().getFailed() != null && job.getStatus().getFailed() > 0) {
                        status = "Failed";
                        failedCount++;
                    } else if (job.getStatus().getActive() != null && job.getStatus().getActive() > 0) {
                        status = "Running";
                        runningCount++;
                    }

                    // 获取条件
                    List<Map<String, Object>> conditions = new ArrayList<>();
                    if (job.getStatus().getConditions() != null) {
                        for (io.fabric8.kubernetes.api.model.batch.v1.JobCondition condition : job.getStatus()
                                .getConditions()) {
                            Map<String, Object> conditionMap = new HashMap<>();
                            conditionMap.put("type", condition.getType());
                            conditionMap.put("status", condition.getStatus());
                            conditionMap.put("lastProbeTime", condition.getLastProbeTime());
                            conditionMap.put("lastTransitionTime", condition.getLastTransitionTime());
                            conditionMap.put("reason", condition.getReason() != null ? condition.getReason() : "");
                            conditionMap.put("message", condition.getMessage() != null ? condition.getMessage() : "");
                            conditions.add(conditionMap);
                        }
                    }

                    jobStatus.put("conditions", conditions.isEmpty() ? null : conditions);
                }

                jobStatus.put("status", status);
                jobStatus.put("message", message);

                jobMap.put("jobStatus", jobStatus);

                // 统计各种状态
                if (status.equals("Unknown")) {
                    unknownCount++;
                } else if (status.equals("Running") && pendingCount > 0) {
                    pendingCount++;
                }

                jobs.add(jobMap);
            }

            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();

            // 列表元数据
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", jobs.size());
            responseData.put("listMeta", listMeta);

            // 度量指标（暂时为空）
            List<Map<String, Object>> metrics = new ArrayList<>();
            Map<String, Object> cpuMetric = new HashMap<>();
            cpuMetric.put("dataPoints", new ArrayList<>());
            cpuMetric.put("metricPoints", new ArrayList<>());
            cpuMetric.put("metricName", "cpu/usage_rate");
            cpuMetric.put("aggregation", "sum");
            metrics.add(cpuMetric);

            Map<String, Object> memoryMetric = new HashMap<>();
            memoryMetric.put("dataPoints", new ArrayList<>());
            memoryMetric.put("metricPoints", new ArrayList<>());
            memoryMetric.put("metricName", "memory/usage");
            memoryMetric.put("aggregation", "sum");
            metrics.add(memoryMetric);

            responseData.put("cumulativeMetrics", metrics);

            // 状态统计
            Map<String, Object> status = new HashMap<>();
            status.put("running", runningCount);
            status.put("pending", pendingCount);
            status.put("failed", failedCount);
            status.put("succeeded", succeededCount);
            status.put("unknown", unknownCount);
            status.put("terminating", terminatingCount);
            responseData.put("status", status);

            // 添加Jobs列表
            responseData.put("jobs", jobs);

            // 添加错误列表
            responseData.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, responseData);
        } catch (Exception e) {
            logger.error("获取Jobs列表出错", e);
            return Result.error("获取Jobs列表出错: " + e.getMessage());
        }
    }

    @Override
    public Result getJobs(Integer clusterId, Integer serviceId, String namespace) {
        // 直接调用不带serviceId的方法，因为Jobs资源目前不需要根据serviceId进行过滤
        // 保留serviceId参数是为了保持API一致性
        return getJobs(clusterId, namespace);
    }

    @Override
    public Result getCronJobs(Integer clusterId, String namespace) {
        try {
            // 获取kubeconfig
            String kubeConfig = getKubeConfig(clusterId);
            if (kubeConfig == null) {
                return Result.error("找不到集群Kubernetes配置");
            }

            // 创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);
            if (client == null) {
                return Result.error("创建Kubernetes客户端失败");
            }

            // 获取CronJobs列表
            List<io.fabric8.kubernetes.api.model.batch.v1.CronJob> cronJobsList;
            if (namespace != null && !namespace.equals("all")) {
                cronJobsList = client.batch().v1().cronjobs().inNamespace(namespace).list().getItems();
            } else {
                cronJobsList = client.batch().v1().cronjobs().inAnyNamespace().list().getItems();
            }

            // 处理CronJobs数据
            List<Map<String, Object>> items = new ArrayList<>();
            int runningCount = 0;

            for (io.fabric8.kubernetes.api.model.batch.v1.CronJob cronJob : cronJobsList) {
                Map<String, Object> cronJobMap = new HashMap<>();

                // 处理元数据
                Map<String, Object> objectMeta = new HashMap<>();
                objectMeta.put("name", cronJob.getMetadata().getName());
                objectMeta.put("namespace", cronJob.getMetadata().getNamespace());
                objectMeta.put("annotations", cronJob.getMetadata().getAnnotations());
                objectMeta.put("labels", cronJob.getMetadata().getLabels());
                objectMeta.put("creationTimestamp", cronJob.getMetadata().getCreationTimestamp());
                objectMeta.put("uid", cronJob.getMetadata().getUid());
                cronJobMap.put("objectMeta", objectMeta);

                // 类型元数据
                Map<String, Object> typeMeta = new HashMap<>();
                typeMeta.put("kind", "cronjob");
                cronJobMap.put("typeMeta", typeMeta);

                // 调度、暂停和活动信息
                cronJobMap.put("schedule", cronJob.getSpec().getSchedule());
                cronJobMap.put("suspend",
                        cronJob.getSpec().getSuspend() != null ? cronJob.getSpec().getSuspend() : false);

                // 活动作业数量
                int activeCount = cronJob.getStatus() != null && cronJob.getStatus().getActive() != null
                        ? cronJob.getStatus().getActive().size()
                        : 0;
                cronJobMap.put("active", activeCount);

                // 如果有活动作业，计入运行中的数量
                if (activeCount > 0) {
                    runningCount++;
                }

                // 最后调度时间
                cronJobMap.put("lastSchedule",
                        cronJob.getStatus() != null ? cronJob.getStatus().getLastScheduleTime() : null);

                // 提取容器镜像
                List<String> containerImages = new ArrayList<>();
                if (cronJob.getSpec() != null &&
                        cronJob.getSpec().getJobTemplate() != null &&
                        cronJob.getSpec().getJobTemplate().getSpec() != null &&
                        cronJob.getSpec().getJobTemplate().getSpec().getTemplate() != null &&
                        cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec() != null &&
                        cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers() != null) {

                    for (io.fabric8.kubernetes.api.model.Container container : cronJob.getSpec().getJobTemplate()
                            .getSpec().getTemplate().getSpec().getContainers()) {
                        if (container.getImage() != null) {
                            containerImages.add(container.getImage());
                        }
                    }
                }
                cronJobMap.put("containerImages", containerImages);

                items.add(cronJobMap);
            }

            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();

            // 列表元数据
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", items.size());
            responseData.put("listMeta", listMeta);

            // 度量指标（暂时为空）
            List<Map<String, Object>> metrics = new ArrayList<>();
            Map<String, Object> cpuMetric = new HashMap<>();
            cpuMetric.put("dataPoints", new ArrayList<>());
            cpuMetric.put("metricPoints", new ArrayList<>());
            cpuMetric.put("metricName", "cpu/usage_rate");
            cpuMetric.put("aggregation", "sum");
            metrics.add(cpuMetric);

            Map<String, Object> memoryMetric = new HashMap<>();
            memoryMetric.put("dataPoints", new ArrayList<>());
            memoryMetric.put("metricPoints", new ArrayList<>());
            memoryMetric.put("metricName", "memory/usage");
            memoryMetric.put("aggregation", "sum");
            metrics.add(memoryMetric);

            responseData.put("cumulativeMetrics", metrics);

            // 添加CronJobs列表
            responseData.put("items", items);

            // 添加状态统计
            Map<String, Object> status = new HashMap<>();
            status.put("running", runningCount);
            status.put("pending", 0);
            status.put("failed", 0);
            status.put("succeeded", 0);
            status.put("unknown", 0);
            status.put("terminating", 0);
            responseData.put("status", status);

            // 添加错误列表
            responseData.put("errors", new ArrayList<>());

            return Result.success().put(Constants.DATA, responseData);
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

    @Override
    public Result getResourceStats(Integer clusterId, Integer serviceId, String namespace) {
        logger.info("一次性获取所有K8s资源统计, clusterId={}, serviceId={}, namespace={}", clusterId, serviceId, namespace);
        try {
            // 使用kubeconfig创建Kubernetes客户端（只创建一次连接）
            KubernetesClient client = getKubernetesClient(clusterId);
            if (client == null) {
                return Result.error("创建Kubernetes客户端失败");
            }

            // 创建统计结果Map
            Map<String, Integer> statsMap = new HashMap<>();

            // 确定目标命名空间
            boolean hasNamespace = namespace != null && !namespace.isEmpty();

            // 1. 获取命名空间数量
            int namespacesCount = client.namespaces().list().getItems().size();
            statsMap.put("namespaces", namespacesCount);

            // 2. 获取Deployments数量
            int deploymentsCount = hasNamespace
                    ? client.apps().deployments().inNamespace(namespace).list().getItems().size()
                    : client.apps().deployments().inAnyNamespace().list().getItems().size();
            statsMap.put("deployments", deploymentsCount);

            // 3. 获取Pods数量
            int podsCount = hasNamespace ? client.pods().inNamespace(namespace).list().getItems().size()
                    : client.pods().inAnyNamespace().list().getItems().size();
            statsMap.put("pods", podsCount);

            // 4. 获取Services数量
            int servicesCount = hasNamespace ? client.services().inNamespace(namespace).list().getItems().size()
                    : client.services().inAnyNamespace().list().getItems().size();
            statsMap.put("services", servicesCount);

            // 5. 获取ConfigMaps数量
            int configMapsCount = hasNamespace ? client.configMaps().inNamespace(namespace).list().getItems().size()
                    : client.configMaps().inAnyNamespace().list().getItems().size();
            statsMap.put("configMaps", configMapsCount);

            // 6. 获取Secrets数量
            int secretsCount = hasNamespace ? client.secrets().inNamespace(namespace).list().getItems().size()
                    : client.secrets().inAnyNamespace().list().getItems().size();
            statsMap.put("secrets", secretsCount);

            // 7. 获取PersistentVolumes数量
            int persistentVolumesCount = client.persistentVolumes().list().getItems().size();
            statsMap.put("persistentVolumes", persistentVolumesCount);

            // 8. 获取PersistentVolumeClaims数量
            int pvcsCount = hasNamespace
                    ? client.persistentVolumeClaims().inNamespace(namespace).list().getItems().size()
                    : client.persistentVolumeClaims().inAnyNamespace().list().getItems().size();
            statsMap.put("persistentVolumeClaims", pvcsCount);

            // 9. 获取StorageClasses数量
            int storageClassesCount = client.storage().storageClasses().list().getItems().size();
            statsMap.put("storageClasses", storageClassesCount);

            // 10. 获取Ingresses数量
            int ingressesCount = hasNamespace
                    ? client.network().v1().ingresses().inNamespace(namespace).list().getItems().size()
                    : client.network().v1().ingresses().inAnyNamespace().list().getItems().size();
            statsMap.put("ingresses", ingressesCount);

            // 11. 获取IngressClasses数量
            int ingressClassesCount = client.network().v1().ingressClasses().list().getItems().size();
            statsMap.put("ingressClasses", ingressClassesCount);

            // 12. 获取DaemonSets数量
            int daemonSetsCount = hasNamespace
                    ? client.apps().daemonSets().inNamespace(namespace).list().getItems().size()
                    : client.apps().daemonSets().inAnyNamespace().list().getItems().size();
            statsMap.put("daemonSets", daemonSetsCount);

            // 13. 获取StatefulSets数量
            int statefulSetsCount = hasNamespace
                    ? client.apps().statefulSets().inNamespace(namespace).list().getItems().size()
                    : client.apps().statefulSets().inAnyNamespace().list().getItems().size();
            statsMap.put("statefulSets", statefulSetsCount);

            // 14. 获取ReplicaSets数量
            int replicaSetsCount = hasNamespace
                    ? client.apps().replicaSets().inNamespace(namespace).list().getItems().size()
                    : client.apps().replicaSets().inAnyNamespace().list().getItems().size();
            statsMap.put("replicaSets", replicaSetsCount);

            // 15. 获取ReplicationControllers数量
            int replicationControllersCount = hasNamespace
                    ? client.replicationControllers().inNamespace(namespace).list().getItems().size()
                    : client.replicationControllers().inAnyNamespace().list().getItems().size();
            statsMap.put("replicationControllers", replicationControllersCount);

            // 16. 获取Jobs数量
            int jobsCount = hasNamespace
                    ? client.batch().v1().jobs().inNamespace(namespace).list().getItems().size()
                    : client.batch().v1().jobs().inAnyNamespace().list().getItems().size();
            statsMap.put("jobs", jobsCount);

            // 17. 获取CronJobs数量
            int cronJobsCount = hasNamespace
                    ? client.batch().v1().cronjobs().inNamespace(namespace).list().getItems().size()
                    : client.batch().v1().cronjobs().inAnyNamespace().list().getItems().size();
            statsMap.put("cronJobs", cronJobsCount);

            // 关闭客户端连接
            client.close();

            return Result.success().put(Constants.DATA, statsMap);
        } catch (Exception e) {
            logger.error("获取K8s资源统计出错", e);
            return Result.error("获取K8s资源统计出错: " + e.getMessage());
        }
    }

    /**
     * 获取Pods列表详细信息（包含指标、状态统计等）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间（null或"all"表示所有命名空间）
     * @return 包含Pod列表、状态统计等的详细信息
     */
    @Override
    public Result getPodsInfo(Integer clusterId, String namespace) {
        try {
            logger.info("获取Pods详细信息请求：clusterId={}, namespace={}", clusterId, namespace);

            // 1. 获取Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);
            if (client == null) {
                return Result.error("无法获取Kubernetes客户端");
            }

            // 2. 查询Pod列表
            io.fabric8.kubernetes.api.model.PodList podList;
            if (namespace != null && !namespace.isEmpty() && !"all".equalsIgnoreCase(namespace)) {
                podList = client.pods().inNamespace(namespace).list();
            } else {
                podList = client.pods().inAnyNamespace().list();
            }

            // 3. 获取Pod指标
            io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList podMetricsList;
            if (namespace != null && !namespace.isEmpty() && !"all".equalsIgnoreCase(namespace)) {
                podMetricsList = client.top().pods().inNamespace(namespace).metrics();
            } else {
                podMetricsList = client.top().pods().metrics();
            }

            // 创建一个Map快速查找Pod的指标
            Map<String, io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics> metricsMap = new HashMap<>();
            if (podMetricsList != null && podMetricsList.getItems() != null) {
                for (io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics metrics : podMetricsList.getItems()) {
                    String key = metrics.getMetadata().getNamespace() + "/" + metrics.getMetadata().getName();
                    metricsMap.put(key, metrics);
                }
            }

            // 4. 处理数据并构建响应
            List<Map<String, Object>> podDetails = new ArrayList<>();
            Map<String, Integer> statusCount = new HashMap<>();
            statusCount.put("running", 0);
            statusCount.put("pending", 0);
            statusCount.put("failed", 0);
            statusCount.put("succeeded", 0);
            statusCount.put("unknown", 0);
            statusCount.put("terminating", 0);

            // 5. 处理Pod列表
            for (io.fabric8.kubernetes.api.model.Pod pod : podList.getItems()) {
                // 提取Pod状态
                String status = pod.getStatus().getPhase();

                // 更新状态计数
                updateStatusCount(statusCount, status);

                // 计算重启次数
                int restartCount = 0;
                if (pod.getStatus().getContainerStatuses() != null) {
                    for (io.fabric8.kubernetes.api.model.ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                        restartCount += cs.getRestartCount();
                    }
                }

                // 提取容器镜像
                List<String> containerImages = new ArrayList<>();
                if (pod.getSpec().getContainers() != null) {
                    for (io.fabric8.kubernetes.api.model.Container container : pod.getSpec().getContainers()) {
                        containerImages.add(container.getImage());
                    }
                }

                // 创建ObjectMeta
                Map<String, Object> objectMeta = new HashMap<>();
                objectMeta.put("name", pod.getMetadata().getName());
                objectMeta.put("namespace", pod.getMetadata().getNamespace());
                objectMeta.put("labels", pod.getMetadata().getLabels());
                objectMeta.put("annotations", pod.getMetadata().getAnnotations());
                objectMeta.put("creationTimestamp", pod.getMetadata().getCreationTimestamp());
                objectMeta.put("uid", pod.getMetadata().getUid());

                // 创建TypeMeta
                Map<String, Object> typeMeta = new HashMap<>();
                typeMeta.put("kind", "pod");

                // 获取Pod指标
                Map<String, Object> metricsInfo = new HashMap<>();
                String metricsKey = pod.getMetadata().getNamespace() + "/" + pod.getMetadata().getName();
                io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics podMetrics = metricsMap.get(metricsKey);

                if (podMetrics != null && podMetrics.getContainers() != null) {
                    // 计算CPU和内存使用量
                    int cpuUsageTotal = 0;
                    long memoryUsageTotal = 0;

                    for (io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics containerMetrics : podMetrics
                            .getContainers()) {
                        // CPU单位是"n"，表示纳核，我们需要转换为毫核 (1m = 1000000n)
                        String cpuQuantity = containerMetrics.getUsage().get("cpu").getAmount();
                        if (cpuQuantity != null && !cpuQuantity.isEmpty()) {
                            // 将"n"单位转换为"m"单位 (1m = 1000000n)
                            if (cpuQuantity.endsWith("n")) {
                                cpuQuantity = cpuQuantity.substring(0, cpuQuantity.length() - 1);
                                try {
                                    long cpuNano = Long.parseLong(cpuQuantity);
                                    cpuUsageTotal += (int) (cpuNano / 1000000);
                                } catch (NumberFormatException e) {
                                    logger.warn("无法解析CPU使用量: {}", cpuQuantity, e);
                                }
                            } else {
                                try {
                                    // 如果没有单位，假设是核心数，转换为毫核
                                    double cores = Double.parseDouble(cpuQuantity);
                                    cpuUsageTotal += (int) (cores * 1000);
                                } catch (NumberFormatException e) {
                                    logger.warn("无法解析CPU使用量: {}", cpuQuantity, e);
                                }
                            }
                        }

                        // 内存单位可能是Ki、Mi、Gi等，我们需要转换为字节
                        String memoryQuantity = containerMetrics.getUsage().get("memory").getAmount();
                        if (memoryQuantity != null && !memoryQuantity.isEmpty()) {
                            try {
                                if (memoryQuantity.endsWith("Ki")) {
                                    memoryQuantity = memoryQuantity.substring(0, memoryQuantity.length() - 2);
                                    memoryUsageTotal += Long.parseLong(memoryQuantity) * 1024;
                                } else if (memoryQuantity.endsWith("Mi")) {
                                    memoryQuantity = memoryQuantity.substring(0, memoryQuantity.length() - 2);
                                    memoryUsageTotal += Long.parseLong(memoryQuantity) * 1024 * 1024;
                                } else if (memoryQuantity.endsWith("Gi")) {
                                    memoryQuantity = memoryQuantity.substring(0, memoryQuantity.length() - 2);
                                    memoryUsageTotal += Long.parseLong(memoryQuantity) * 1024 * 1024 * 1024;
                                } else if (memoryQuantity.endsWith("Ti")) {
                                    memoryQuantity = memoryQuantity.substring(0, memoryQuantity.length() - 2);
                                    memoryUsageTotal += Long.parseLong(memoryQuantity) * 1024 * 1024 * 1024 * 1024L;
                                } else {
                                    // 假设是字节
                                    memoryUsageTotal += Long.parseLong(memoryQuantity);
                                }
                            } catch (NumberFormatException e) {
                                logger.warn("无法解析内存使用量: {}", memoryQuantity, e);
                            }
                        }
                    }

                    metricsInfo.put("cpuUsage", cpuUsageTotal);
                    metricsInfo.put("memoryUsage", memoryUsageTotal);
                    // 暂不实现历史数据
                    metricsInfo.put("cpuUsageHistory", null);
                    metricsInfo.put("memoryUsageHistory", null);
                }

                // 创建PodDetail
                Map<String, Object> podDetail = new HashMap<>();
                podDetail.put("objectMeta", objectMeta);
                podDetail.put("typeMeta", typeMeta);
                podDetail.put("status", status);
                podDetail.put("restartCount", restartCount);
                podDetail.put("metrics", metricsInfo);
                podDetail.put("warnings", Collections.emptyList()); // 暂不处理警告
                podDetail.put("nodeName", pod.getSpec().getNodeName());
                podDetail.put("containerImages", containerImages);

                podDetails.add(podDetail);
            }

            // 6. 构建并返回响应
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("totalItems", podDetails.size());

            response.put("listMeta", listMeta);
            response.put("cumulativeMetrics", null); // 暂不实现累计指标
            response.put("status", statusCount);
            response.put("pods", podDetails);
            response.put("errors", Collections.emptyList()); // 暂无错误

            logger.info("获取Pods列表成功，共{}个Pod", podDetails.size());
            return Result.success().put(Constants.DATA, response);

        } catch (Exception e) {
            logger.error("获取Pods列表失败", e);
            return Result.error("获取Pods列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据Pod状态更新计数器
     */
    private void updateStatusCount(Map<String, Integer> statusCount, String status) {
        if (status == null) {
            status = "unknown";
        }

        switch (status.toLowerCase()) {
            case "running":
                statusCount.put("running", statusCount.get("running") + 1);
                break;
            case "pending":
                statusCount.put("pending", statusCount.get("pending") + 1);
                break;
            case "failed":
                statusCount.put("failed", statusCount.get("failed") + 1);
                break;
            case "succeeded":
                statusCount.put("succeeded", statusCount.get("succeeded") + 1);
                break;
            case "terminating":
                statusCount.put("terminating", statusCount.get("terminating") + 1);
                break;
            default:
                statusCount.put("unknown", statusCount.get("unknown") + 1);
        }
    }
}