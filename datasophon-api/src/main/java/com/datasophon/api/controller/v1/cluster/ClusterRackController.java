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

import com.datasophon.api.converter.ClusterRackConverter;
import com.datasophon.api.service.ClusterRackService;
import com.datasophon.common.dto.ClusterRackDTO;
import com.datasophon.common.vo.ClusterRackVO;
import com.datasophon.common.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群机架控制器
 * 提供集群机架的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "cluster/rack")
public class ClusterRackController {

    @Autowired
    private ClusterRackService clusterRackService;

    @Autowired
    private ClusterRackConverter clusterRackConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterRackVO>> list(@RequestParam("clusterId") Integer clusterId) {
        // 调用Service层方法，获取DTO列表
        List<ClusterRackDTO> dtoList = clusterRackService.queryClusterRack(clusterId);
        // Controller层：DTO → VO转换
        List<ClusterRackVO> voList = clusterRackConverter.dtoListToVoList(dtoList);
        return Result.success(voList);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterRackVO> info(@PathVariable("id") Integer id) {
        // 调用Service层方法，获取DTO
        ClusterRackDTO dto = clusterRackService.getByIdAsDto(id);
        // Controller层：DTO → VO转换
        ClusterRackVO vo = clusterRackConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存机架
     */
    @RequestMapping("/save")
    public Result<ClusterRackVO> save(@RequestParam("clusterId") Integer clusterId, @RequestParam("rack") String rack) {
        // 调用Service层方法，获取DTO
        ClusterRackDTO dto = clusterRackService.saveRack(clusterId, rack);
        // Controller层：DTO → VO转换
        ClusterRackVO vo = clusterRackConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存DTO
     */
    @RequestMapping("/saveDto")
    public Result<ClusterRackVO> saveDto(@RequestBody ClusterRackDTO dto) {
        // Controller层直接传递DTO给Service层
        ClusterRackDTO savedDto = clusterRackService.saveRackDto(dto);
        // Controller层：DTO → VO转换
        ClusterRackVO vo = clusterRackConverter.dtoToVo(savedDto);
        return Result.success(vo);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterRackDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterRackService.updateRack(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除机架
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestParam("clusterId") Integer clusterId, @RequestParam("rackId") Integer rackId) {
        boolean result = clusterRackService.deleteRack(rackId);
        return result ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 批量删除
     */
    @RequestMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestBody Integer[] ids) {
        clusterRackService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }

}
