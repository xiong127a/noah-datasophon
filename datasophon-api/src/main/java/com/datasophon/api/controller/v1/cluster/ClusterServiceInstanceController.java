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

import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.converter.ClusterServiceInstanceConverter;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.vo.ClusterServiceInstanceVO;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 集群服务实例控制器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/service/instance")
public class ClusterServiceInstanceController {

    @Autowired
    private ClusterServiceInstanceService clusterServiceInstanceService;

    @Autowired
    private ClusterServiceInstanceConverter clusterServiceInstanceConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterServiceInstanceVO>> list(@ClusterId Long clusterId) {
        List<ClusterServiceInstanceDTO> dtoList = clusterServiceInstanceService.listAll(clusterId);
        List<ClusterServiceInstanceVO> voList = clusterServiceInstanceConverter.dtoListToVoList(dtoList);
        return Result.success(voList);
    }

    /**
     * 获取服务角色类型列表
     */
    @RequestMapping("/getServiceRoleType")
    public Result<List<FrameServiceRoleEntity>> getServiceRoleType(
            @RequestParam("serviceInstanceId") Integer serviceInstanceId) {
        List<FrameServiceRoleEntity> serviceRoleTypes = clusterServiceInstanceService
                .getServiceRoleType(serviceInstanceId);
        return Result.success(serviceRoleTypes);
    }

    /**
     * 配置版本比较
     */
    @RequestMapping("/configVersionCompare")
    public Result<Map<String, List<Map<String, Object>>>> configVersionCompare(
            @RequestParam("serviceInstanceId") Integer serviceInstanceId,
            @RequestParam("roleGroupId") Integer roleGroupId,
            @RequestParam("showOnlyDifferences") Boolean showOnlyDifferences) {
        Map<String, List<Map<String, Object>>> compareResult = clusterServiceInstanceService.configVersionCompare(
                serviceInstanceId, roleGroupId, showOnlyDifferences);
        return Result.success(compareResult);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterServiceInstanceVO> info(@PathVariable("id") Integer id) {
        ClusterServiceInstanceEntity clusterServiceInstance = clusterServiceInstanceService.getById(id);
        ClusterServiceInstanceVO clusterServiceInstanceVO = clusterServiceInstanceConverter
                .entityToVo(clusterServiceInstance);
        return Result.success(clusterServiceInstanceVO);
    }

    /**
     * 下载客户端配置
     */
    @RequestMapping("/downloadClientConfig")
    public Result<String> downloadClientConfig(
            @ClusterId Long clusterId,
            @RequestParam("serviceName") String serviceName) {
        String configPath = clusterServiceInstanceService.downloadClientConfig(clusterId, serviceName);
        return Result.success(configPath);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<String> save(@RequestBody ClusterServiceInstanceDTO clusterServiceInstanceDTO) {
        ClusterServiceInstanceEntity clusterServiceInstance = clusterServiceInstanceConverter
                .dtoToEntity(clusterServiceInstanceDTO);
        clusterServiceInstanceService.save(clusterServiceInstance);
        return Result.success("保存成功");
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterServiceInstanceDTO clusterServiceInstanceDTO) {
        ClusterServiceInstanceEntity clusterServiceInstance = clusterServiceInstanceConverter
                .dtoToEntity(clusterServiceInstanceDTO);
        clusterServiceInstanceService.updateById(clusterServiceInstance);
        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestParam("serviceInstanceId") Integer serviceInstanceId) {
        boolean success = clusterServiceInstanceService.delServiceInstance(serviceInstanceId);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 获取服务连接信息
     */
    @RequestMapping("/getConnectionInfo")
    public Result<Object> getConnectionInfo(@RequestParam("serviceInstanceId") Integer serviceInstanceId) {
        Object connectionInfo = clusterServiceInstanceService.getConnectionInfo(serviceInstanceId);
        return Result.success(connectionInfo);
    }

}
