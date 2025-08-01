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

import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.common.vo.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.mybatisflex.core.query.QueryChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@ApiVersion(path = "cluster/service/instance/role/group")
public class ClusterServiceInstanceRoleGroupController {

    @Autowired
    private ClusterServiceInstanceRoleGroupService clusterServiceInstanceRoleGroupService;


    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterServiceInstanceRoleGroup>> list(@RequestParam("serviceInstanceId") Integer serviceInstanceId) {
        List<ClusterServiceInstanceRoleGroup> list = QueryChain.of(ClusterServiceInstanceRoleGroup.class)
                .where(ClusterServiceInstanceRoleGroup::getServiceInstanceId).eq(serviceInstanceId)
                .list();
        return Result.success(list);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterServiceInstanceRoleGroup> info(@PathVariable("id") Integer id) {
        ClusterServiceInstanceRoleGroup clusterServiceInstanceRoleGroup = clusterServiceInstanceRoleGroupService
                .getById(id);

        return Result.success(clusterServiceInstanceRoleGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<Void> save(@RequestParam("serviceInstanceId") Integer serviceInstanceId,
            @RequestParam("roleGroupId") Integer roleGroupId, @RequestParam("roleGroupName") String roleGroupName) {
        clusterServiceInstanceRoleGroupService.saveRoleGroup(serviceInstanceId, roleGroupId, roleGroupName);
        return Result.success();
    }

    /**
     * 分配角色组
     */
    @RequestMapping("/bind")
    public Result<Boolean> bind(@RequestParam("serviceRoleInstancesIds") String serviceRoleInstancesIds,
            @RequestParam("roleGroupId") Integer roleGroupId) {
        return Result.success(clusterServiceInstanceRoleGroupService.bind(serviceRoleInstancesIds, roleGroupId));
    }

    /**
     * 修改
     */
    @RequestMapping("/rename")
    public Result<Boolean> update(@RequestParam("roleGroupId") Integer roleGroupId,
            @RequestParam("roleGroupName") String roleGroupName) {

        return Result.success(clusterServiceInstanceRoleGroupService.rename(roleGroupId, roleGroupName));

    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<Boolean> delete(@RequestParam("roleGroupId") Integer roleGroupId) {
        // clusterServiceInstanceRoleGroupService.removeByIds(Arrays.asList(ids));

        return Result.success(clusterServiceInstanceRoleGroupService.deleteRoleGroup(roleGroupId));
    }

}
