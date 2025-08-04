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

import com.datasophon.api.converter.ClusterServiceCommandHostConverter;
import com.datasophon.api.service.ClusterServiceCommandHostService;
import com.datasophon.common.dto.ClusterServiceCommandHostDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.vo.ClusterServiceCommandHostVO;
import com.datasophon.common.vo.Result;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群服务命令主机控制器
 * 提供集群服务命令主机的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "cluster/service/command/host")
public class ClusterServiceCommandHostController {

    @Autowired
    private ClusterServiceCommandHostService clusterServiceCommandHostService;

    @Autowired
    private ClusterServiceCommandHostConverter converter;

    /**
     * 获取命令主机列表（分页）
     */
    @RequestMapping("/list")
    public Result<PageResult<ClusterServiceCommandHostVO>> list(@RequestParam("clusterId") Integer clusterId,
            @RequestParam("commandId") String commandId, @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        PageResult<ClusterServiceCommandHostEntity> pageResult = clusterServiceCommandHostService
                .getCommandHostList(clusterId, commandId, page, pageSize);
        PageResult<ClusterServiceCommandHostVO> voPageResult = converter.pageResultToPageResultVO(pageResult);
        return Result.success(voPageResult);
    }

    /**
     * 根据ID获取命令主机信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterServiceCommandHostVO> info(@PathVariable("id") String id) {
        ClusterServiceCommandHostDTO dto = clusterServiceCommandHostService.getByIdAsDto(id);
        if (dto == null) {
            return Result.error("命令主机不存在");
        }
        ClusterServiceCommandHostVO vo = converter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存命令主机
     */
    @RequestMapping("/save")
    public Result<ClusterServiceCommandHostVO> save(@RequestBody ClusterServiceCommandHostDTO dto) {
        ClusterServiceCommandHostDTO savedDto = clusterServiceCommandHostService.saveCommandHost(dto);
        ClusterServiceCommandHostVO vo = converter.dtoToVo(savedDto);
        return Result.success(vo);
    }

    /**
     * 更新命令主机
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterServiceCommandHostDTO dto) {
        clusterServiceCommandHostService.updateCommandHost(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除命令主机
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody String[] ids) {
        clusterServiceCommandHostService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }

    /**
     * 获取所有命令主机
     */
    @RequestMapping("/all")
    public Result<List<ClusterServiceCommandHostVO>> getAllCommandHosts() {
        List<ClusterServiceCommandHostEntity> entities = clusterServiceCommandHostService.list();
        List<ClusterServiceCommandHostVO> vos = converter.entityListToVoList(entities);
        return Result.success(vos);
    }

    /**
     * 获取失败的命令主机
     */
    @RequestMapping("/failed")
    public Result<List<ClusterServiceCommandHostVO>> getFailedCommandHosts(
            @RequestParam("commandId") String commandId) {
        List<ClusterServiceCommandHostDTO> dtos = clusterServiceCommandHostService.findFailedCommandHost(commandId);
        List<ClusterServiceCommandHostVO> vos = converter.dtoListToVoList(dtos);
        return Result.success(vos);
    }

    /**
     * 获取取消的命令主机
     */
    @RequestMapping("/canceled")
    public Result<List<ClusterServiceCommandHostVO>> getCanceledCommandHosts(
            @RequestParam("commandId") String commandId) {
        List<ClusterServiceCommandHostDTO> dtos = clusterServiceCommandHostService.findCanceledCommandHost(commandId);
        List<ClusterServiceCommandHostVO> vos = converter.dtoListToVoList(dtos);
        return Result.success(vos);
    }
}
