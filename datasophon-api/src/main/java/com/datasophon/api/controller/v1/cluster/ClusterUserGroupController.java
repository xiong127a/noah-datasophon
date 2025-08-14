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

import com.datasophon.api.converter.ClusterUserGroupConverter;
import com.datasophon.api.service.ClusterUserGroupService;
import com.datasophon.common.dto.ClusterUserGroupDTO;
import com.datasophon.common.vo.ClusterUserGroupVO;
import com.datasophon.api.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 集群用户组关联控制器
 * 提供集群用户组关联的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "cluster/user/group")
public class ClusterUserGroupController {

    @Autowired
    private ClusterUserGroupService clusterUserGroupService;

    @Autowired
    private ClusterUserGroupConverter clusterUserGroupConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterUserGroupVO>> list() {
        // 这里需要根据实际业务需求实现列表查询逻辑
        return Result.success();
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterUserGroupVO> info(@PathVariable("id") Long id) {
        // 调用Service层方法，获取DTO
        ClusterUserGroupDTO dto = clusterUserGroupService.getByIdAsDto(id);
        // Controller层：DTO → VO转换
        ClusterUserGroupVO vo = clusterUserGroupConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<String> save(@RequestBody ClusterUserGroupDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterUserGroupService.saveUserGroup(dto);
        return Result.success("保存成功");
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterUserGroupDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterUserGroupService.updateUserGroup(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Integer[] ids) {
        clusterUserGroupService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }
}
