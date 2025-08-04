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

import com.datasophon.api.converter.ClusterServiceRoleInstanceConverter;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;

import com.datasophon.common.vo.Result;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群服务角色实例控制器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/service/role/instance")
public class ClusterServiceRoleInstanceController {

    @Autowired
    private ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;

    @Autowired
    private ClusterServiceRoleInstanceConverter clusterServiceRoleInstanceConverter;


    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(@RequestParam("serviceInstanceId") Integer serviceInstanceId, @RequestParam(name = "hostname",required = false) String hostname, @RequestParam(name = "serviceRoleState",required = false) Integer serviceRoleState, @RequestParam("serviceRoleName") String serviceRoleName,
                       @RequestParam(name = "roleGroupId",required = false) Integer roleGroupId,@RequestParam("page") Integer page, @RequestParam("pageSize") Integer pageSize) {
        return clusterServiceRoleInstanceService.listAll(serviceInstanceId, hostname, serviceRoleState, serviceRoleName,
                roleGroupId, page, pageSize);
    }

    /**
     * 信息
     */
    @RequestMapping("/getLog")
    public Result getLog(@RequestParam("serviceRoleInstanceId") Integer serviceRoleInstanceId) throws Exception {
        return clusterServiceRoleInstanceService.getLog(serviceRoleInstanceId);
    }

    /**
     * 退役
     */
    @RequestMapping("/decommissionNode")
    public Result decommissionNode(@RequestParam("serviceRoleInstanceIds") String serviceRoleInstanceIds, @RequestParam("serviceName") String serviceName) throws Exception {
        return clusterServiceRoleInstanceService.decommissionNode(serviceRoleInstanceIds, serviceName);
    }

    /**
     * 重启过时服务
     */
    @RequestMapping("/restartObsoleteService")
    public Result restartObsoleteService(@RequestParam("roleGroupId") Integer roleGroupId) {
        return clusterServiceRoleInstanceService.restartObsoleteService(roleGroupId);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<String> save(@RequestBody ClusterServiceRoleInstanceDTO clusterServiceRoleInstanceDTO) {
        ClusterServiceRoleInstanceEntity clusterServiceRoleInstance = clusterServiceRoleInstanceConverter.dtoToEntity(clusterServiceRoleInstanceDTO);
        clusterServiceRoleInstanceService.save(clusterServiceRoleInstance);
        return Result.success("保存成功");
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterServiceRoleInstanceDTO clusterServiceRoleInstanceDTO) {
        ClusterServiceRoleInstanceEntity clusterServiceRoleInstance = clusterServiceRoleInstanceConverter.dtoToEntity(clusterServiceRoleInstanceDTO);
        clusterServiceRoleInstanceService.updateById(clusterServiceRoleInstance);
        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestParam("serviceRoleInstancesIds") String serviceRoleInstancesIds) {
        List<String> idList = List.of(serviceRoleInstancesIds.split(","));
        return clusterServiceRoleInstanceService.deleteServiceRole(idList);
    }

}
