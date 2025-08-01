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
import com.datasophon.api.vo.Result;
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
import java.util.List;

/**
 * 集群信息控制器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@ApiVersion(path = "cluster")
public class ClusterInfoController {

    @Autowired
    private ClusterInfoService clusterInfoService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterInfoEntity>> list() {
        try {
            List<ClusterInfoEntity> clusterList = clusterInfoService.getClusterList();
            return Result.success(clusterList);
        } catch (Exception e) {
            return Result.error("获取集群列表失败: " + e.getMessage());
        }
    }

    /**
     * 配置好的集群列表
     */
    @RequestMapping("/runningClusterList")
    public Result<List<ClusterInfoEntity>> runningClusterList() {
        try {
            List<ClusterInfoEntity> runningClusters = clusterInfoService.runningClusterList();
            return Result.success(runningClusters);
        } catch (Exception e) {
            return Result.error("获取运行中集群列表失败: " + e.getMessage());
        }
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterInfoEntity> info(@PathVariable("id") Integer id) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(id);
            return Result.success(clusterInfo);
        } catch (Exception e) {
            return Result.error("获取集群信息失败: " + e.getMessage());
        }
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @UserPermission
    public Result<ClusterInfoEntity> save(@RequestBody ClusterInfoEntity clusterInfo) {
        try {
            ClusterInfoEntity savedCluster = clusterInfoService.saveCluster(clusterInfo);
            return Result.success(savedCluster);
        } catch (Exception e) {
            return Result.error("保存集群失败: " + e.getMessage());
        }
    }

    @RequestMapping("/updateClusterState")
    public Result<String> updateClusterState(@ClusterId Integer clusterId,
            @RequestParam("clusterState") Integer clusterState) {
        try {
            boolean success = clusterInfoService.updateClusterState(clusterId, clusterState);
            if (success) {
                return Result.success("集群状态更新成功");
            } else {
                return Result.error("集群状态更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新集群状态失败: " + e.getMessage());
        }
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @UserPermission
    public Result<ClusterInfoEntity> update(@RequestBody ClusterInfoEntity clusterInfo) {
        try {
            ClusterInfoEntity updatedCluster = clusterInfoService.updateCluster(clusterInfo);
            return Result.success(updatedCluster);
        } catch (Exception e) {
            return Result.error("修改集群失败: " + e.getMessage());
        }
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @UserPermission
    public Result<String> delete(@RequestBody Integer[] ids) {
        try {
            clusterInfoService.deleteCluster(Arrays.asList(ids));
            return Result.success("删除集群成功");
        } catch (Exception e) {
            return Result.error("删除集群失败: " + e.getMessage());
        }
    }

    @RequestMapping("/grafana/metrics")
    public String getServiceRoleMetrics() {
        return clusterInfoService.getServiceRoleMetrics();
    }

    /**
     * 获取集群详细信息
     */
    @RequestMapping("/detail")
    public Result<ClusterInfoEntity> getClusterDetail(@ClusterId Integer clusterId) {
        try {
            ClusterInfoEntity clusterDetail = clusterInfoService.getClusterById(clusterId);
            return Result.success(clusterDetail);
        } catch (Exception e) {
            return Result.error("获取集群详细信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取Kubernetes命名空间列表
     */
    @PostMapping("/namespaces")
    public Result<Object> getKubernetesNamespaces(@RequestBody JsonNode jsonNode) {
        try {
            String kubeConfigContent = jsonNode.get("kubeConfigContent").asText();
            Object namespaces = clusterInfoService.getKubernetesNamespaces(kubeConfigContent);
            return Result.success(namespaces);
        } catch (Exception e) {
            return Result.error("获取Kubernetes命名空间列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新集群Kubernetes配置
     */
    @PostMapping("/kube-config")
    public Result<String> updateClusterKubeConfig(@ClusterId Integer clusterId,
            @RequestBody KubeConfigUpdateRequest request) {
        try {
            String result = clusterInfoService.updateClusterKubeConfig(
                    clusterId,
                    request.getKubeConfig(),
                    request.getNamespace(),
                    request.getCustomNamespace());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("更新Kubernetes配置失败: " + e.getMessage());
        }
    }

}
