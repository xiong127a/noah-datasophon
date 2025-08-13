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
import com.datasophon.api.converter.ClusterNodeLabelConverter;
import com.datasophon.api.service.ClusterNodeLabelService;
import com.datasophon.common.dto.ClusterNodeLabelDTO;
import com.datasophon.common.vo.ClusterNodeLabelVO;
import com.datasophon.api.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 集群节点标签控制器
 * 提供集群节点标签的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "cluster/node/label")
public class ClusterNodeLabelController {

    @Autowired
    private ClusterNodeLabelService nodeLabelService;

    @Autowired
    private ClusterNodeLabelConverter clusterNodeLabelConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterNodeLabelVO>> list(@ClusterId Long clusterId) {
        // 调用Service层方法，获取DTO列表
        List<ClusterNodeLabelDTO> dtoList = nodeLabelService.queryClusterNodeLabel(clusterId);
        // Controller层：DTO → VO转换
        List<ClusterNodeLabelVO> voList = clusterNodeLabelConverter.dtoListToVoList(dtoList);
        return Result.success(voList);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterNodeLabelVO> info(@PathVariable("id") Integer id) {
        // 调用Service层方法，获取DTO
        ClusterNodeLabelDTO dto = nodeLabelService.getByIdAsDto(id);
        // Controller层：DTO → VO转换
        ClusterNodeLabelVO vo = clusterNodeLabelConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存节点标签
     */
    @RequestMapping("/save")
    public Result<ClusterNodeLabelVO> save(@ClusterId Long clusterId,
            @RequestParam("nodeLabel") String nodeLabel) {
        // 调用Service层方法，获取DTO
        ClusterNodeLabelDTO dto = nodeLabelService.saveNodeLabel(clusterId, nodeLabel);
        // Controller层：DTO → VO转换
        ClusterNodeLabelVO vo = clusterNodeLabelConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存DTO
     */
    @RequestMapping("/saveDto")
    public Result<ClusterNodeLabelVO> saveDto(@RequestBody ClusterNodeLabelDTO dto) {
        // Controller层直接传递DTO给Service层
        ClusterNodeLabelDTO savedDto = nodeLabelService.saveNodeLabelDto(dto);
        // Controller层：DTO → VO转换
        ClusterNodeLabelVO vo = clusterNodeLabelConverter.dtoToVo(savedDto);
        return Result.success(vo);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterNodeLabelDTO dto) {
        // Controller层直接传递DTO给Service层
        nodeLabelService.updateNodeLabel(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除节点标签
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestParam("nodeLabelId") Integer nodeLabelId) {
        boolean result = nodeLabelService.deleteNodeLabel(nodeLabelId);
        return result ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 分配节点标签
     */
    @RequestMapping("/assign")
    public Result<String> assign(@RequestParam("nodeLabelId") Integer nodeLabelId,
            @RequestParam("hostIds") String hostIds) {
        boolean result = nodeLabelService.assignNodeLabel(nodeLabelId, hostIds);
        return result ? Result.success("分配成功") : Result.error("分配失败");
    }

    /**
     * 删除
     */
    @RequestMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestBody Integer[] ids) {
        nodeLabelService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }
}
