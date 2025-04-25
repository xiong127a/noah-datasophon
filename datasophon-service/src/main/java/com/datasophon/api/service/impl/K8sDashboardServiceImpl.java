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

            // 获取指标数据
            List<Map<String, Object>> cumulativeMetrics = getCumulativeMetrics(client, namespace);

            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            result.put("deployments", deployments);
            result.put("status", status);
            result.put("cumulativeMetrics", cumulativeMetrics);

            return Result.success().put(Constants.DATA, result);
        } catch (Exception e) {
            logger.error("获取Deployments列表出错", e);
            return Result.error("获取Deployments列表出错: " + e.getMessage());
        }
    }

    /**
     * 获取累计指标数据
     * 
     * @param client    Kubernetes客户端
     * @param namespace 命名空间
     * @return 累计指标数据列表
     */
    private List<Map<String, Object>> getCumulativeMetrics(KubernetesClient client, String namespace) {
        try {
            // 确定目标命名空间
            String targetNamespace = namespace != null && !namespace.isEmpty() ? namespace : "default";
            logger.info("使用命名空间获取指标数据: {}", targetNamespace);

            // 从Prometheus API获取真实数据
            // 获取当前集群关联的Prometheus服务地址
            String prometheusUrl = getPrometheusServiceUrl(client);
            if (prometheusUrl == null) {
                logger.error("无法获取Prometheus服务地址");
                return new ArrayList<>();
            }

            // 计算时间范围
            long endTime = System.currentTimeMillis() / 1000; // 当前时间
            long startTime = endTime - 15 * 60; // 15分钟前

            // 获取CPU和内存数据
            List<Map<String, Object>> cumulativeMetrics = new ArrayList<>();

            // 添加CPU指标
            Map<String, Object> cpuMetric = new HashMap<>();
            cpuMetric.put("metricName", "cpu/usage_rate");
            cpuMetric.put("aggregation", "sum");

            // 构建CPU查询
            String cpuQuery = String.format("sum(rate(container_cpu_usage_seconds_total{namespace=\"%s\"}[5m]))",
                    targetNamespace);
            List<List<Object>> cpuResults = queryPrometheus(prometheusUrl, cpuQuery, startTime, endTime, "15s");

            List<Double> cpuValues = new ArrayList<>();
            List<Long> timestamps = new ArrayList<>();

            if (cpuResults != null && !cpuResults.isEmpty()) {
                for (List<Object> point : cpuResults) {
                    if (point.size() >= 2) {
                        long timestamp = ((Double) point.get(0)).longValue();
                        double value = ((Double) point.get(1));
                        timestamps.add(timestamp);
                        cpuValues.add(value);
                    }
                }
            } else {
                // 如果没有数据，使用空时间序列
                logger.warn("没有CPU指标数据，使用空时间序列");
                long now = System.currentTimeMillis() / 1000;
                for (int i = 15; i >= 0; i--) {
                    timestamps.add(now - i * 60);
                    cpuValues.add(0.0);
                }
            }

            // 创建CPU数据点
            List<Map<String, Object>> cpuDataPoints = createDataPoints(timestamps, cpuValues);
            cpuMetric.put("dataPoints", cpuDataPoints);

            // 添加metricPoints字段，与官方格式保持一致
            List<Map<String, Object>> cpuMetricPoints = createMetricPoints(timestamps, cpuValues);
            cpuMetric.put("metricPoints", cpuMetricPoints);

            cumulativeMetrics.add(cpuMetric);

            // 添加内存指标
            Map<String, Object> memoryMetric = new HashMap<>();
            memoryMetric.put("metricName", "memory/usage");
            memoryMetric.put("aggregation", "sum");

            // 构建内存查询
            String memoryQuery = String.format("sum(container_memory_usage_bytes{namespace=\"%s\"})", targetNamespace);
            List<List<Object>> memoryResults = queryPrometheus(prometheusUrl, memoryQuery, startTime, endTime, "15s");

            List<Double> memoryValues = new ArrayList<>();
            List<Long> memoryTimestamps = new ArrayList<>();

            if (memoryResults != null && !memoryResults.isEmpty()) {
                for (List<Object> point : memoryResults) {
                    if (point.size() >= 2) {
                        long timestamp = ((Double) point.get(0)).longValue();
                        double value = ((Double) point.get(1));
                        memoryTimestamps.add(timestamp);
                        memoryValues.add(value);
                    }
                }
            } else {
                // 如果没有数据，使用空时间序列
                logger.warn("没有内存指标数据，使用空时间序列");
                memoryTimestamps = timestamps; // 复用CPU时间戳
                for (int i = 0; i < memoryTimestamps.size(); i++) {
                    memoryValues.add(0.0);
                }
            }

            // 创建内存数据点
            List<Map<String, Object>> memoryDataPoints = createDataPoints(memoryTimestamps, memoryValues);
            memoryMetric.put("dataPoints", memoryDataPoints);

            // 添加metricPoints字段，与官方格式保持一致
            List<Map<String, Object>> memoryMetricPoints = createMetricPoints(memoryTimestamps, memoryValues);
            memoryMetric.put("metricPoints", memoryMetricPoints);

            cumulativeMetrics.add(memoryMetric);

            return cumulativeMetrics;
        } catch (Exception e) {
            logger.error("获取指标数据出错", e);
            return new ArrayList<>(); // 出错时返回空列表
        }
    }

    /**
     * 获取Prometheus服务URL
     * 
     * @param client Kubernetes客户端
     * @return Prometheus服务URL
     */
    private String getPrometheusServiceUrl(KubernetesClient client) {
        try {
            // 查找Prometheus服务
            String prometheusService = client.services()
                    .inAnyNamespace()
                    .withLabel("app", "prometheus")
                    .list()
                    .getItems()
                    .stream()
                    .findFirst()
                    .map(service -> {
                        String namespace = service.getMetadata().getNamespace();
                        String name = service.getMetadata().getName();
                        Integer port = service.getSpec().getPorts().get(0).getPort();
                        return String.format("http://%s.%s.svc.cluster.local:%d", name, namespace, port);
                    })
                    .orElse(null);

            if (prometheusService != null) {
                return prometheusService;
            }

            // 如果找不到带app=prometheus标签的服务，尝试查找名称中包含prometheus的服务
            return client.services()
                    .inAnyNamespace()
                    .list()
                    .getItems()
                    .stream()
                    .filter(service -> service.getMetadata().getName().toLowerCase().contains("prometheus"))
                    .findFirst()
                    .map(service -> {
                        String namespace = service.getMetadata().getNamespace();
                        String name = service.getMetadata().getName();
                        Integer port = service.getSpec().getPorts().get(0).getPort();
                        return String.format("http://%s.%s.svc.cluster.local:%d", name, namespace, port);
                    })
                    .orElse("http://prometheus.datasophon.svc.cluster.local:9090"); // 默认地址
        } catch (Exception e) {
            logger.error("获取Prometheus服务URL出错", e);
            return "http://prometheus.datasophon.svc.cluster.local:9090"; // 默认地址
        }
    }

    /**
     * 查询Prometheus API
     * 
     * @param prometheusUrl Prometheus URL
     * @param query         查询表达式
     * @param start         开始时间（Unix时间戳，秒）
     * @param end           结束时间（Unix时间戳，秒）
     * @param step          步长
     * @return 查询结果
     */
    @SuppressWarnings("unchecked")
    private List<List<Object>> queryPrometheus(String prometheusUrl, String query, long start, long end, String step) {
        try {
            // 构建查询URL
            String url = String.format("%s/api/v1/query_range?query=%s&start=%d&end=%d&step=%s",
                    prometheusUrl, java.net.URLEncoder.encode(query, "UTF-8"), start, end, step);

            // 执行HTTP请求
            java.net.URL apiUrl = new java.net.URL(url);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            // 读取响应
            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            // 解析JSON响应
            com.alibaba.fastjson.JSONObject jsonResponse = com.alibaba.fastjson.JSON.parseObject(response.toString());

            // 检查状态
            String status = jsonResponse.getString("status");
            if (!"success".equals(status)) {
                logger.error("Prometheus查询失败: {}", jsonResponse.getString("error"));
                return null;
            }

            // 提取结果
            com.alibaba.fastjson.JSONObject data = jsonResponse.getJSONObject("data");
            com.alibaba.fastjson.JSONArray results = data.getJSONArray("result");

            if (results.isEmpty()) {
                logger.warn("Prometheus查询没有返回数据");
                return new ArrayList<>();
            }

            // 获取第一个结果的值
            com.alibaba.fastjson.JSONObject firstResult = results.getJSONObject(0);
            List<List> valuesList = firstResult.getJSONArray("values").toJavaList(List.class);

            // 明确进行类型转换
            List<List<Object>> typedList = new ArrayList<>();
            for (List list : valuesList) {
                typedList.add((List<Object>) list);
            }

            return typedList;
        } catch (Exception e) {
            logger.error("查询Prometheus出错", e);
            return null;
        }
    }

    /**
     * 创建数据点列表 (dataPoints格式)
     *
     * @param timestamps 时间戳列表
     * @param values     值列表
     * @return 数据点列表
     */
    private List<Map<String, Object>> createDataPoints(List<Long> timestamps, List<Double> values) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        // 确保两个列表长度一致
        int size = Math.min(timestamps.size(), values.size());

        for (int i = 0; i < size; i++) {
            Map<String, Object> point = new HashMap<>();
            // 时间戳已经是秒级
            point.put("x", timestamps.get(i));
            point.put("y", values.get(i));
            dataPoints.add(point);
        }

        return dataPoints;
    }

    /**
     * 创建指标点列表 (metricPoints格式)
     *
     * @param timestamps 时间戳列表
     * @param values     值列表
     * @return 指标点列表
     */
    private List<Map<String, Object>> createMetricPoints(List<Long> timestamps, List<Double> values) {
        List<Map<String, Object>> metricPoints = new ArrayList<>();

        // 确保两个列表长度一致
        int size = Math.min(timestamps.size(), values.size());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (int i = 0; i < size; i++) {
            Map<String, Object> point = new HashMap<>();
            // 时间戳转换为ISO格式
            point.put("timestamp", sdf.format(new Date(timestamps.get(i) * 1000))); // 秒转毫秒
            point.put("value", values.get(i));
            metricPoints.add(point);
        }

        return metricPoints;
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

    @Override
    public Result getDeploymentMetrics(Integer clusterId, Integer serviceId, String namespace) {
        logger.info("获取Deployment监控数据, clusterId={}, serviceId={}, namespace={}", clusterId, serviceId, namespace);
        try {
            // 使用kubeconfig创建Kubernetes客户端
            KubernetesClient client = getKubernetesClient(clusterId);

            // 获取指标数据
            List<Map<String, Object>> cumulativeMetrics = getCumulativeMetrics(client, namespace);

            return Result.success().put(Constants.DATA, cumulativeMetrics);
        } catch (Exception e) {
            logger.error("获取Deployment监控数据出错", e);
            return Result.error("获取Deployment监控数据出错: " + e.getMessage());
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
            int jobsCount = hasNamespace ? client.batch().v1().jobs().inNamespace(namespace).list().getItems().size()
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