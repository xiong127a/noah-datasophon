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
import com.datasophon.api.converter.ClusterGroupConverter;
import com.datasophon.api.service.ClusterGroupService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.dto.ClusterGroupDTO;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.vo.ClusterGroupVO;
import com.datasophon.api.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群组控制器
 * 提供集群组的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "cluster/group")
public class ClusterGroupController {

    @Autowired
    private ClusterGroupService clusterGroupService;

    @Autowired
    private ClusterGroupConverter clusterGroupConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<PageResult<ClusterGroupVO>> list(@RequestParam("groupName") String groupName,
            @ClusterId Long clusterId, @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        PageResult<ClusterGroupDTO> dtoPageResult = clusterGroupService.listPage(groupName, clusterId, page, pageSize);
        // Controller层：DTO → VO转换
        List<ClusterGroupVO> voList = dtoPageResult.getRecords().stream()
                .map(clusterGroupConverter::dtoToVo)
                .toList();
        PageResult<ClusterGroupVO> voPageResult = PageResult.of(voList, dtoPageResult.getTotal(),
                dtoPageResult.getCurrent(), dtoPageResult.getSize());
        return Result.success(voPageResult);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterGroupVO> info(@PathVariable("id") Integer id) {
        // 调用Service层方法，获取DTO
        ClusterGroupDTO dto = clusterGroupService.getByIdAsDto(id);
        // Controller层：DTO → VO转换
        ClusterGroupVO vo = clusterGroupConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<ClusterGroupVO> save(@ClusterId Long clusterId,
            @RequestParam("groupName") String groupName) {
        ClusterGroupDTO dto = ProcessUtils.getDepMode(clusterId)== ClusterType.PVM
                ? clusterGroupService.saveClusterGroup(clusterId, groupName)
                : clusterGroupService.saveClusterGroupOnKubernetes(clusterId, groupName);
        // Controller层：DTO → VO转换
        ClusterGroupVO vo = clusterGroupConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存（使用DTO）
     */
    @RequestMapping("/saveDto")
    public Result<String> saveDto(@RequestBody ClusterGroupDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterGroupService.saveClusterGroupDto(dto);
        return Result.success("保存成功");
    }

    /**
     * 更新
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterGroupDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterGroupService.updateClusterGroup(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除用户组
     */
    @RequestMapping("/delete")
    public Result<String> delete(@ClusterId Long clusterId, @RequestParam("id") Integer id) {
        boolean success = ProcessUtils.getDepMode(clusterId)== ClusterType.PVM
                ? clusterGroupService.deleteUserGroup(id)
                : clusterGroupService.deleteUserGroupOnKubernetes(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 刷新用户组到主机
     */
    @RequestMapping("/refreshUserGroupToHost")
    public Result<String> refreshUserGroupToHost(@ClusterId Long clusterId) {
        clusterGroupService.refreshUserGroupToHost(clusterId);
        return Result.success("刷新成功");
    }
}
