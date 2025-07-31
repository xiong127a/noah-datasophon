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

import com.datasophon.api.service.KubernetesDashboardService;
import com.datasophon.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Kubernetes仪表盘控制器
 * 提供Kubernetes资源查询接口
 */
@ApiVersion(path = "kubernetes/dashboard")
@Slf4j
public class KubernetesDashboardController {

    @Autowired
    private KubernetesDashboardService kubernetesDashboardService;


    /**
     * 获取Kubernetes命名空间列表
     */
    @RequestMapping("/namespaces")
    public Result getNamespaces(@RequestParam("clusterId") Integer clusterId) {
        return kubernetesDashboardService.getNamespaces(clusterId);
    }

    /**
     * 获取Deployments列表
     */
    @RequestMapping("/deployments")
    public Result getDeployments(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "serviceId", required = false) Integer serviceId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {

        log.info("获取Deployments列表请求：clusterId={}, serviceId={}, namespace={}, pageNum={}, pageSize={}",
                clusterId, serviceId, namespace, pageNum, pageSize);

        return kubernetesDashboardService.getDeployments(clusterId, serviceId, namespace, pageNum, pageSize);

    }

    /**
     * 获取Pods列表
     */
    @GetMapping("/pods")
    public Result getPodsInfo(@RequestParam(name = "clusterId") Integer clusterId,
            @RequestParam(name = "serviceId", required = false) Integer serviceId,
            @RequestParam(name = "namespace", required = false) String namespace,
            @RequestParam(name = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            log.info("获取Pods列表请求：clusterId={}, serviceId={}, namespace={}, pageNum={}, pageSize={}",
                    clusterId, serviceId, namespace, pageNum, pageSize);
            return kubernetesDashboardService.getPods(clusterId, serviceId, namespace, pageNum, pageSize);
        } catch (Exception e) {
            log.error("获取Pods列表失败", e);
            return Result.error("获取Pods列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取Services列表
     */
    @RequestMapping("/services")
    public Result getServices(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getServices(clusterId, namespace, pageNum, pageSize);
    }

    /**
     * 获取ConfigMaps列表
     */
    @RequestMapping("/configmaps")
    public Result getConfigMaps(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getConfigMaps(clusterId, namespace, pageNum, pageSize);
    }

    /**
     * 获取Secrets列表
     */
    @RequestMapping("/secrets")
    public Result getSecrets(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getSecrets(clusterId, namespace, pageNum, pageSize);
    }

    /**
     * 获取PersistentVolumes列表
     */
    @RequestMapping("/persistentvolumes")
    public Result getPersistentVolumes(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {

        // 直接调用支持分页的服务方法
        return kubernetesDashboardService.getPersistentVolumes(clusterId, pageNum, pageSize);
    }

    /**
     * 获取PersistentVolumeClaims列表
     */
    @RequestMapping("/pvcs")
    public Result getPersistentVolumeClaims(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {

        // 直接调用支持分页的服务方法
        return kubernetesDashboardService.getPersistentVolumeClaims(clusterId, namespace, pageNum, pageSize);
    }

    /**
     * 获取StorageClasses列表
     */
    @RequestMapping("/storageclasses")
    public Result getStorageClasses(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {

        log.info("获取StorageClasses列表请求：clusterId={}, pageNum={}, pageSize={}",
                clusterId, pageNum, pageSize);

        // 直接调用支持分页的服务方法
        return kubernetesDashboardService.getStorageClasses(clusterId, pageNum, pageSize);
    }

    /**
     * 获取Ingresses列表
     */
    @RequestMapping("/ingresses")
    public Result getIngresses(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getIngresses(clusterId, namespace, pageNum, pageSize);
    }

    /**
     * 获取IngressClasses列表
     */
    @RequestMapping("/ingressclasses")
    public Result getIngressClasses(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getIngressClasses(clusterId, pageNum, pageSize);
    }

    /**
     * 获取DaemonSets列表
     */
    @RequestMapping("/daemonsets")
    public Result getDaemonSets(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "serviceId", required = false) Integer serviceId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getDaemonSets(clusterId, serviceId, namespace, pageNum, pageSize);

    }

    /**
     * 获取StatefulSets列表
     */
    @RequestMapping("/statefulsets")
    public Result getStatefulSets(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getStatefulSets(clusterId, namespace, pageNum, pageSize);
    }

    /**
     * 获取ReplicaSets列表
     */
    @RequestMapping("/replicasets")
    public Result getReplicaSets(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getReplicaSets(clusterId, namespace, pageNum, pageSize);
    }

    /**
     * 获取ReplicationControllers列表（带分页）
     */
    @RequestMapping("/replicationcontrollers")
    public Result getReplicationControllers(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getReplicationControllers(clusterId, namespace, pageNum, pageSize);
    }

    /**
     * 获取Jobs列表
     */
    @RequestMapping("/jobs")
    public Result getJobs(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "serviceId", required = false) Integer serviceId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getJobs(clusterId, namespace, pageNum, pageSize);
    }

    /**
     * 获取CronJobs列表
     */
    @RequestMapping("/cronjobs")
    public Result getCronJobs(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return kubernetesDashboardService.getCronJobs(clusterId, namespace, pageNum, pageSize);
    }

    /**
     * 获取Deployment详情
     */
    @RequestMapping("/deployment/detail")
    public Result getDeploymentDetail(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam("namespace") String namespace,
            @RequestParam("name") String name) {
        return kubernetesDashboardService.getDeploymentDetail(clusterId, namespace, name);
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
        return kubernetesDashboardService.getResourceEvents(clusterId, namespace, kind, name);
    }

    /**
     * 获取资源相关事件
     */
    @RequestMapping(value = "/resource/events", method = RequestMethod.GET)
    public Result getResourceEvents(@RequestParam Integer clusterId,
            @RequestParam String namespace,
            @RequestParam String kind,
            @RequestParam String name) {
        return kubernetesDashboardService.getResourceEvents(clusterId, namespace, kind, name);
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
            Result result = kubernetesDashboardService.getResourceStats(clusterId, serviceId, namespace);

            // 日志记录返回结果
            log.info("resource-stats接口返回：code={}, data={}", result.getCode(), result.getData() != null ? "非空" : "空");

            return result;
        } catch (Exception e) {
            log.error("获取Kubernetes资源统计失败", e);
            return Result.error("获取Kubernetes资源统计失败: " + e.getMessage());
        }
    }

}
