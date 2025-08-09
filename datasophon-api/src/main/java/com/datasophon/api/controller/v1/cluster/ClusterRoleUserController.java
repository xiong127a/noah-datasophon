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
import com.datasophon.api.converter.ClusterRoleUserConverter;
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.ClusterRoleUserService;
import com.datasophon.common.dto.ClusterRoleUserDTO;
import com.datasophon.common.vo.ClusterRoleUserVO;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.ClusterRoleUserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 集群角色用户控制器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/user")
public class ClusterRoleUserController {

    @Autowired
    private ClusterRoleUserService clusterRoleUserService;

    @Autowired
    private ClusterRoleUserConverter clusterRoleUserConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterRoleUserVO>> list(@RequestParam Map<String, Object> params) {
        List<ClusterRoleUserDTO> dtoList = clusterRoleUserService.getAllClusterRoleUsers();
        List<ClusterRoleUserVO> voList = clusterRoleUserConverter.dtoListToVoList(dtoList);
        return Result.success(voList);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterRoleUserVO> info(@PathVariable("id") Integer id) {
        ClusterRoleUserEntity clusterRoleUser = clusterRoleUserService.getById(id);
        ClusterRoleUserVO clusterRoleUserVO = clusterRoleUserConverter.entityToVo(clusterRoleUser);
        return Result.success(clusterRoleUserVO);
    }

    /**
     * 保存集群管理员
     */
    @RequestMapping("/saveClusterManager")
    @UserPermission
    public Result<String> saveClusterManager(@ClusterId Integer clusterId,
            @RequestParam("userIds") String userIds) {
        boolean success = clusterRoleUserService.saveClusterManager(clusterId, userIds);
        return success ? Result.success("保存成功") : Result.error("保存失败");
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterRoleUserDTO clusterRoleUserDTO) {
        ClusterRoleUserEntity clusterRoleUser = clusterRoleUserConverter.dtoToEntity(clusterRoleUserDTO);
        clusterRoleUserService.updateById(clusterRoleUser);
        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Integer[] ids) {
        clusterRoleUserService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }

}
