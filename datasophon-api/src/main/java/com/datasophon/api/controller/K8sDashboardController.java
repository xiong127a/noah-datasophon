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

package com.datasophon.api.controller;

import com.datasophon.api.service.K8sDashboardService;
import com.datasophon.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * K8S仪表盘控制器
 * 提供Kubernetes资源查询接口
 */
@RestController
@RequestMapping("api/k8s/dashboard")
@Slf4j
public class K8sDashboardController {

    @Autowired
    private K8sDashboardService k8sDashboardService;

    /**
     * 获取Kubernetes命名空间列表
     */
    @RequestMapping("/namespaces")
    public Result getNamespaces(@RequestParam("clusterId") Integer clusterId) {
        return k8sDashboardService.getNamespaces(clusterId);
    }

    /**
     * 获取Deployments列表
     */
    @RequestMapping("/deployments")
    public Result getDeployments(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "serviceId", required = false) Integer serviceId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        if (serviceId != null) {
            return k8sDashboardService.getDeployments(clusterId, serviceId, namespace);
        } else {
            return k8sDashboardService.getDeployments(clusterId, namespace);
        }
    }

    /**
     * 获取Pods列表
     */
    @RequestMapping("/pods")
    public Result getPods(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getPods(clusterId, namespace);
    }

    /**
     * 获取Services列表
     */
    @RequestMapping("/services")
    public Result getServices(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getServices(clusterId, namespace);
    }

    /**
     * 获取ConfigMaps列表
     */
    @RequestMapping("/configmaps")
    public Result getConfigMaps(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getConfigMaps(clusterId, namespace);
    }

    /**
     * 获取Secrets列表
     */
    @RequestMapping("/secrets")
    public Result getSecrets(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getSecrets(clusterId, namespace);
    }

    /**
     * 获取PersistentVolumes列表
     */
    @RequestMapping("/persistentvolumes")
    public Result getPersistentVolumes(
            @RequestParam("clusterId") Integer clusterId) {
        return k8sDashboardService.getPersistentVolumes(clusterId);
    }

    /**
     * 获取PersistentVolumeClaims列表
     */
    @RequestMapping("/pvcs")
    public Result getPersistentVolumeClaims(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getPersistentVolumeClaims(clusterId, namespace);
    }

    /**
     * 获取StorageClasses列表
     */
    @RequestMapping("/storageclasses")
    public Result getStorageClasses(
            @RequestParam("clusterId") Integer clusterId) {
        return k8sDashboardService.getStorageClasses(clusterId);
    }

    /**
     * 获取Ingresses列表
     */
    @RequestMapping("/ingresses")
    public Result getIngresses(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getIngresses(clusterId, namespace);
    }

    /**
     * 获取IngressClasses列表
     */
    @RequestMapping("/ingressclasses")
    public Result getIngressClasses(
            @RequestParam("clusterId") Integer clusterId) {
        return k8sDashboardService.getIngressClasses(clusterId);
    }

    /**
     * 获取DaemonSets列表
     */
    @RequestMapping("/daemonsets")
    public Result getDaemonSets(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getDaemonSets(clusterId, namespace);
    }

    /**
     * 获取StatefulSets列表
     */
    @RequestMapping("/statefulsets")
    public Result getStatefulSets(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getStatefulSets(clusterId, namespace);
    }

    /**
     * 获取ReplicaSets列表
     */
    @RequestMapping("/replicasets")
    public Result getReplicaSets(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getReplicaSets(clusterId, namespace);
    }

    /**
     * 获取ReplicationControllers列表
     */
    @RequestMapping("/replicationcontrollers")
    public Result getReplicationControllers(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getReplicationControllers(clusterId, namespace);
    }

    /**
     * 获取Jobs列表
     */
    @RequestMapping("/jobs")
    public Result getJobs(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getJobs(clusterId, namespace);
    }

    /**
     * 获取CronJobs列表
     */
    @RequestMapping("/cronjobs")
    public Result getCronJobs(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getCronJobs(clusterId, namespace);
    }

    /**
     * 获取Deployment详情
     */
    @RequestMapping("/deployment/detail")
    public Result getDeploymentDetail(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam("namespace") String namespace,
            @RequestParam("name") String name) {
        return k8sDashboardService.getDeploymentDetail(clusterId, namespace, name);
    }

    /**
     * 获取Deployment相关事件
     */
    @RequestMapping("/deployment/events")
    public Result getDeploymentEvents(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam("namespace") String namespace,
            @RequestParam("kind") String kind,
            @RequestParam("name") String name) {
        return k8sDashboardService.getResourceEvents(clusterId, namespace, kind, name);
    }

    /**
     * 获取资源相关事件
     */
    @RequestMapping(value = "/resource/events", method = RequestMethod.GET)
    public Result getResourceEvents(@RequestParam Integer clusterId,
            @RequestParam String namespace,
            @RequestParam String kind,
            @RequestParam String name) {
        return k8sDashboardService.getResourceEvents(clusterId, namespace, kind, name);
    }

    /**
     * 获取资源统计数据
     * 只统计各类资源的数量，不返回资源列表以提高性能
     */
    @RequestMapping("/resource-stats")
    public Result getResourceStats(
            @RequestParam(name = "clusterId", required = false) Integer clusterId,
            @RequestParam(value = "serviceId", required = false) Integer serviceId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        try {
            // 添加调试日志
            log.info("收到resource-stats请求：clusterId={}, serviceId={}, namespace={}", clusterId, serviceId, namespace);

            // 确保clusterId不为空
            if (clusterId == null) {
                log.warn("收到resource-stats请求但缺少clusterId参数");
                return Result.error("缺少clusterId参数");
            }

            // 调用优化后的Service方法，一次性获取所有资源统计数据
            Result result = k8sDashboardService.getResourceStats(clusterId, serviceId, namespace);

            // 日志记录返回结果
            log.info("resource-stats接口返回：code={}, data={}", result.getCode(), result.getData() != null ? "非空" : "空");

            return result;
        } catch (Exception e) {
            log.error("获取K8s资源统计失败", e);
            return Result.error("获取K8s资源统计失败: " + e.getMessage());
        }
    }

    /**
     * 从结果中获取资源数量
     * 提取Result对象中data字段的列表大小
     * 
     * @param result 结果对象
     * @return 资源数量
     */
    private int getCountFromResult(Result result) {
        if (result == null || result.getData() == null) {
            return 0;
        }

        Object data = result.getData();
        if (data instanceof Iterable) {
            int count = 0;
            for (Object ignored : (Iterable<?>) data) {
                count++;
            }
            return count;
        } else if (data instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) data;
            Object items = map.get("items");
            if (items instanceof Iterable) {
                int count = 0;
                for (Object ignored : (Iterable<?>) items) {
                    count++;
                }
                return count;
            }
        }

        return 0;
    }
}