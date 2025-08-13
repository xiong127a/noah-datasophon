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

import com.datasophon.api.converter.ClusterInfoConverter;
import com.datasophon.api.dto.KubeConfigUpdateRequest;
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.vo.ClusterInfoVO;
import com.datasophon.api.dto.Result;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.annotation.ClusterId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群信息控制器
 * 按照架构重构规范，使用Result<VO>返回，调用Converter转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster")
public class ClusterInfoController {

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ClusterInfoConverter clusterInfoConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterInfoVO>> list() {
        try {
            List<ClusterInfoDTO> clusterDTOList = clusterInfoService.getClusterList();
            List<ClusterInfoVO> clusterVOList = clusterInfoConverter.dtoListToVoList(clusterDTOList);
            return Result.success(clusterVOList);
        } catch (Exception e) {
            return Result.error("获取集群列表失败: " + e.getMessage());
        }
    }

    /**
     * 配置好的集群列表
     */
    @RequestMapping("/runningClusterList")
    public Result<List<ClusterInfoVO>> runningClusterList() {
        try {
            List<ClusterInfoDTO> runningClusterDTOList = clusterInfoService.runningClusterList();
            List<ClusterInfoVO> runningClusterVOList = clusterInfoConverter.dtoListToVoList(runningClusterDTOList);
            return Result.success(runningClusterVOList);
        } catch (Exception e) {
            return Result.error("获取运行中集群列表失败: " + e.getMessage());
        }
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterInfoVO> info(@PathVariable("id") Integer id) {
        try {
            ClusterInfoDTO clusterInfoDTO = clusterInfoService.getClusterById(id);
            ClusterInfoVO clusterInfoVO = clusterInfoConverter.dtoToVo(clusterInfoDTO);
            return Result.success(clusterInfoVO);
        } catch (Exception e) {
            return Result.error("获取集群信息失败: " + e.getMessage());
        }
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @UserPermission
    public Result<ClusterInfoVO> save(@RequestBody ClusterInfoDTO clusterInfoDTO) {
        try {
            ClusterInfoDTO savedClusterDTO = clusterInfoService.saveCluster(clusterInfoDTO);
            ClusterInfoVO savedClusterVO = clusterInfoConverter.dtoToVo(savedClusterDTO);
            return Result.success(savedClusterVO);
        } catch (Exception e) {
            return Result.error("保存集群失败: " + e.getMessage());
        }
    }

    @RequestMapping("/updateClusterState")
    public Result<String> updateClusterState(@ClusterId Long clusterId,
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
    public Result<ClusterInfoVO> update(@RequestBody ClusterInfoDTO clusterInfoDTO) {
        try {
            ClusterInfoDTO updatedClusterDTO = clusterInfoService.updateCluster(clusterInfoDTO);
            ClusterInfoVO updatedClusterVO = clusterInfoConverter.dtoToVo(updatedClusterDTO);
            return Result.success(updatedClusterVO);
        } catch (Exception e) {
            return Result.error("修改集群失败: " + e.getMessage());
        }
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @UserPermission
    public Result<String> delete(@RequestBody Long[] ids) {
        try {
            clusterInfoService.deleteCluster(List.of(ids));
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
    public Result<ClusterInfoVO> getClusterDetail(@ClusterId Long clusterId) {
        try {
            ClusterInfoDTO clusterDetailDTO = clusterInfoService.getClusterById(clusterId);
            ClusterInfoVO clusterDetailVO = clusterInfoConverter.dtoToVo(clusterDetailDTO);
            return Result.success(clusterDetailVO);
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
    public Result<String> updateClusterKubeConfig(@ClusterId Long clusterId,
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
