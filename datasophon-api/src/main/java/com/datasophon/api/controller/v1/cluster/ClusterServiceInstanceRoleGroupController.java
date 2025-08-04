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

import com.datasophon.api.converter.ClusterServiceInstanceRoleGroupConverter;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.common.dto.ClusterServiceInstanceRoleGroupDTO;
import com.datasophon.common.vo.ClusterServiceInstanceRoleGroupVO;
import com.datasophon.common.vo.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群服务实例角色组控制器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/service/instance/role/group")
public class ClusterServiceInstanceRoleGroupController {

    @Autowired
    private ClusterServiceInstanceRoleGroupService clusterServiceInstanceRoleGroupService;

    @Autowired
    private ClusterServiceInstanceRoleGroupConverter clusterServiceInstanceRoleGroupConverter;


    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterServiceInstanceRoleGroupVO>> list(@RequestParam("serviceInstanceId") Integer serviceInstanceId) {
        List<ClusterServiceInstanceRoleGroupDTO> dtoList = clusterServiceInstanceRoleGroupService.listRoleGroupByServiceInstanceId(serviceInstanceId);
        List<ClusterServiceInstanceRoleGroupVO> voList = clusterServiceInstanceRoleGroupConverter.dtoListToVoList(dtoList);
        return Result.success(voList);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterServiceInstanceRoleGroupVO> info(@PathVariable("id") Integer id) {
        ClusterServiceInstanceRoleGroup entity = clusterServiceInstanceRoleGroupService.getById(id);
        ClusterServiceInstanceRoleGroupVO vo = clusterServiceInstanceRoleGroupConverter.entityToVo(entity);
        return Result.success(vo);
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
