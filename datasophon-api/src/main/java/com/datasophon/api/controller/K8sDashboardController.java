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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * K8S仪表盘控制器
 * 提供Kubernetes资源查询接口
 */
@RestController
@RequestMapping("api/k8s/dashboard")
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
            @RequestParam(value = "namespace", required = false) String namespace) {
        return k8sDashboardService.getDeployments(clusterId, namespace);
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
}