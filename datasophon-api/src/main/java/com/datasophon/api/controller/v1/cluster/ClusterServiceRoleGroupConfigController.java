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

import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.common.vo.ClusterServiceRoleGroupConfigVO;
import com.datasophon.api.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 集群服务角色组配置控制器
 * 提供集群服务角色组配置的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "cluster/service/role/group/config")
public class ClusterServiceRoleGroupConfigController {

    @Autowired
    private ClusterServiceRoleGroupConfigService clusterServiceRoleGroupConfigService;

    @Autowired
    private ClusterServiceRoleGroupConfigConverter clusterServiceRoleGroupConfigConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterServiceRoleGroupConfigVO>> list() {
        // 这里需要根据实际业务需求实现列表查询逻辑
        return Result.success();
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterServiceRoleGroupConfigVO> info(@PathVariable("id") Long id) {
        // 调用Service层方法，获取DTO
        ClusterServiceRoleGroupConfigDTO dto = clusterServiceRoleGroupConfigService.getByIdAsDto(id);
        // Controller层：DTO → VO转换
        ClusterServiceRoleGroupConfigVO vo = clusterServiceRoleGroupConfigConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<String> save(@RequestBody ClusterServiceRoleGroupConfigDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterServiceRoleGroupConfigService.saveConfig(dto);
        return Result.success("保存成功");
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterServiceRoleGroupConfigDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterServiceRoleGroupConfigService.updateConfig(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Integer[] ids) {
        clusterServiceRoleGroupConfigService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }
}
