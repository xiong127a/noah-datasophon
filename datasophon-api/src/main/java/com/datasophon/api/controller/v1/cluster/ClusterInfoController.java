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

package com.datasophon.api.controller.v1.cluster;

import com.datasophon.api.dto.KubeConfigUpdateRequest;
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.annotation.ClusterId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;

@ApiVersion(path = "cluster")
public class ClusterInfoController {

    @Autowired
    private ClusterInfoService clusterInfoService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list() {
        return clusterInfoService.getClusterList();
    }

    /**
     * 配置好的集群列表
     */
    @RequestMapping("/runningClusterList")
    public Result runningClusterList() {
        return clusterInfoService.runningClusterList();
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(id);

        return Result.success(clusterInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @UserPermission
    public Result save(@RequestBody ClusterInfoEntity clusterInfo) {
        return clusterInfoService.saveCluster(clusterInfo);
    }

    @RequestMapping("/updateClusterState")
    public Result updateClusterState(@ClusterId Integer clusterId,
            @RequestParam("clusterState") Integer clusterState) {

        return clusterInfoService.updateClusterState(clusterId, clusterState);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @UserPermission
    public Result update(@RequestBody ClusterInfoEntity clusterInfo) {
        return clusterInfoService.updateCluster(clusterInfo);

    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @UserPermission
    public Result delete(@RequestBody Integer[] ids) {
        clusterInfoService.deleteCluster(Arrays.asList(ids));

        return Result.success();
    }

    @RequestMapping("/grafana/metrics")
    public String getServiceRoleMetrics() {
        return clusterInfoService.getServiceRoleMetrics();
    }

    /**
     * 获取集群详细信息
     */
    @RequestMapping("/detail")
    public Result getClusterDetail(@ClusterId Integer clusterId) {
        return clusterInfoService.getClusterById(clusterId);
    }

    /**
     * 获取Kubernetes命名空间列表
     */
    @PostMapping("/namespaces")
    public Result getKubernetesNamespaces(@RequestBody JsonNode jsonNode) {
        String kubeConfigContent = jsonNode.get("kubeConfigContent").asText();
        return clusterInfoService.getKubernetesNamespaces(kubeConfigContent);
    }

    /**
     * 更新集群Kubernetes配置
     */
    @PostMapping("/kube-config")
    public Result updateClusterKubeConfig(@ClusterId Integer clusterId, 
                                        @RequestBody KubeConfigUpdateRequest request) {
        return clusterInfoService.updateClusterKubeConfig(
                clusterId,
                request.getKubeConfig(),
                request.getNamespace(),
                request.getCustomNamespace());
    }

}
