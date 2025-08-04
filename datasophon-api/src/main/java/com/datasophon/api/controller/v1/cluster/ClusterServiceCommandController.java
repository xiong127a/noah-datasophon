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

import cn.hutool.core.util.EnumUtil;
import com.datasophon.api.converter.ClusterServiceCommandConverter;
import com.datasophon.common.dto.ClusterServiceCommandDTO;
import com.datasophon.common.enums.Status;
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.model.RollingRestartInfo;
import com.datasophon.common.vo.ClusterServiceCommandVO;
import com.datasophon.api.dto.Result;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

/**
 * 集群服务命令控制器
 * 提供集群服务命令的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "cluster/service/command")
public class ClusterServiceCommandController {

    @Autowired
    private ClusterServiceCommandService clusterServiceCommandService;

    @Autowired
    private ClusterServiceCommandConverter converter;

    /**
     * 查询集群服务指令列表
     */
    @RequestMapping("/getServiceCommandlist")
    public Result<PageResult<ClusterServiceCommandVO>> list(@RequestParam("clusterId") Integer clusterId,
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        PageResult<ClusterServiceCommandDTO> pageResult = clusterServiceCommandService
                .getServiceCommandlist(clusterId, page, pageSize);

        List<ClusterServiceCommandVO> voList = converter.dtoListToVoList(pageResult.getRecords());
        PageResult<ClusterServiceCommandVO> voPageResult = PageResult.of(voList, pageResult.getTotal(),
                pageResult.getPage(), pageResult.getSize());

        return Result.success(voPageResult);
    }

    /**
     * 生成服务安装操作指令
     */
    @UserPermission
    @RequestMapping("/generateCommand")
    public Result<String> generateCommand(@RequestParam("clusterId") Integer clusterId,
            @RequestParam("commandType") String commandType,
            @RequestParam("serviceNames") String serviceNames) {
        CommandType command = EnumUtil.fromString(CommandType.class, commandType);
        List<String> list = Arrays.asList(serviceNames.split(","));
        return clusterServiceCommandService.generateCommand(clusterId, command, list);
    }

    /**
     * 生成服务实例操作指令
     */
    @RequestMapping("/generateServiceCommand")
    @UserPermission
    public Result<String> generateServiceCommand(@RequestParam("clusterId") Integer clusterId,
            @RequestParam("commandType") String commandType,
            @RequestParam("serviceInstanceIds") String serviceInstanceIds) {
        CommandType command = EnumUtil.fromString(CommandType.class, commandType);
        if (StringUtils.isNotBlank(serviceInstanceIds)) {
            List<String> ids = Arrays.asList(serviceInstanceIds.split(","));
            return clusterServiceCommandService.generateServiceCommand(clusterId, command, ids);
        } else {
            return Result.error(Status.NO_SERVICE_EXECUTE.getMsg());
        }
    }

    /**
     * 生成服务角色实例操作指令
     */
    @RequestMapping("/generateServiceRoleCommand")
    @UserPermission
    public Result<String> generateServiceRoleCommand(@RequestParam("clusterId") Integer clusterId,
            @RequestParam("commandType") String commandType,
            @RequestParam("serviceInstanceId") Integer serviceInstanceId,
            @RequestParam("serviceRoleInstancesIds") String serviceRoleInstancesIds,
            @RequestParam(value = "rollingParam", required = false) String rollingParam) {
        CommandType command = EnumUtil.fromString(CommandType.class, commandType);
        List<String> ids = Arrays.asList(serviceRoleInstancesIds.split(","));
        RollingRestartInfo rollingRestartInfo = RollingRestartInfo.parse(rollingParam);

        return clusterServiceCommandService.generateServiceRoleCommand(clusterId, command, serviceInstanceId, ids,
                rollingRestartInfo);
    }

    /**
     * 启动执行指令
     */
    @RequestMapping("/startExecuteCommand")
    @UserPermission
    public Result<String> startExecuteCommand(@RequestParam("clusterId") Integer clusterId,
            @RequestParam("commandType") String commandType,
            @RequestParam("commandIds") String commandIds) {
        clusterServiceCommandService.startExecuteCommand(clusterId, commandType, commandIds);
        return Result.success("命令执行启动成功");
    }

    @RequestMapping("/cancelCommand")
    public Result<String> cancelCommand(@RequestParam("commandId") String commandId) {
        clusterServiceCommandService.cancelCommand(commandId);
        return Result.success("命令取消成功");
    }

    /**
     * 获取命令信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterServiceCommandVO> info(@PathVariable("id") String id) {
        ClusterServiceCommandDTO dto = clusterServiceCommandService.getByIdAsDto(id);
        if (dto == null) {
            return Result.error("命令不存在");
        }
        ClusterServiceCommandVO vo = converter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存命令
     */
    @RequestMapping("/save")
    public Result<ClusterServiceCommandVO> save(@RequestBody ClusterServiceCommandDTO dto) {
        ClusterServiceCommandDTO savedDto = clusterServiceCommandService.saveCommand(dto);
        ClusterServiceCommandVO vo = converter.dtoToVo(savedDto);
        return Result.success(vo);
    }

    /**
     * 更新命令
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterServiceCommandDTO dto) {
        clusterServiceCommandService.updateCommand(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除命令
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody String[] ids) {
        clusterServiceCommandService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }

    /**
     * 获取最后重启命令
     */
    @RequestMapping("/lastRestart/{serviceInstanceId}")
    public Result<ClusterServiceCommandVO> getLastRestartCommand(
            @PathVariable("serviceInstanceId") Integer serviceInstanceId) {
        ClusterServiceCommandDTO dto = clusterServiceCommandService.getLastRestartCommand(serviceInstanceId);
        if (dto == null) {
            return Result.error("未找到重启命令");
        }
        ClusterServiceCommandVO vo = converter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 根据命令ID获取命令
     */
    @RequestMapping("/byCommandId/{commandId}")
    public Result<ClusterServiceCommandVO> getCommandById(@PathVariable("commandId") String commandId) {
        ClusterServiceCommandDTO dto = clusterServiceCommandService.getCommandById(commandId);
        if (dto == null) {
            return Result.error("命令不存在");
        }
        ClusterServiceCommandVO vo = converter.dtoToVo(dto);
        return Result.success(vo);
    }

}
