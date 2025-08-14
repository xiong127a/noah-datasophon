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
import com.datasophon.api.converter.ClusterUserConverter;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.common.dto.ClusterUserDTO;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.vo.ClusterUserVO;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.ClusterUserEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.datasophon.api.utils.ProcessUtils.getDepMode;

/**
 * 集群用户控制器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/users")
public class ClusterUserController {

    @Autowired
    private ClusterUserService clusterUserService;

    @Autowired
    private ClusterUserConverter clusterUserConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<PageResult<ClusterUserVO>> list(@ClusterId Long clusterId,
            @RequestParam("username") String username,
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {

        PageResult<ClusterUserDTO> pageResult = clusterUserService.listPagedUsers(clusterId, username, page, pageSize);
        List<ClusterUserVO> voList = clusterUserConverter.dtoListToVoList(pageResult.getRecords());
        PageResult<ClusterUserVO> voPageResult = PageResult.of(voList, pageResult.getTotal(), page, pageSize);
        return Result.success(voPageResult);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterUserVO> info(@PathVariable("id") Long id) {
        // 直接从Service获取Entity，然后转换为VO
        ClusterUserEntity clusterUserEntity = clusterUserService.getById(id);
        ClusterUserVO clusterUserVO = clusterUserConverter.entityToVo(clusterUserEntity);

        return Result.success(clusterUserVO);
    }

    /**
     * 保存
     */
    @RequestMapping("/create")
    public Result<ClusterUserVO> save(@ClusterId Long clusterId,
            @RequestParam("username") String username,
            @RequestParam("mainGroupId") Long mainGroupId,
            @RequestParam("otherGroupIds") String otherGroupIds) {
        ClusterUserDTO clusterUserDTO = getDepMode(clusterId) == ClusterType.PVM
                ? clusterUserService.createClusterUser(clusterId, username, mainGroupId, otherGroupIds)
                : clusterUserService.createClusterUserOnKubernetes(clusterId, username, mainGroupId, otherGroupIds);
        ClusterUserVO clusterUserVO = clusterUserConverter.dtoToVo(clusterUserDTO);
        return Result.success(clusterUserVO);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterUserDTO clusterUserDTO) {
        // 使用Entity层面的更新，因为当前Service继承IService<Entity>
        ClusterUserEntity clusterUserEntity = clusterUserConverter.dtoToEntity(clusterUserDTO);
        clusterUserService.updateById(clusterUserEntity);

        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<String> delete(@ClusterId Long clusterId,
            @RequestParam("id") Long id) {
        boolean success = getDepMode(clusterId) == ClusterType.PVM
                ? clusterUserService.deleteClusterUser(id)
                : clusterUserService.deleteClusterUserOnKubernetes(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

}
