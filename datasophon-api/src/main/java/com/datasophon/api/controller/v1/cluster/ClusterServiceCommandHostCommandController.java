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

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.converter.ClusterServiceCommandHostCommandConverter;
import com.datasophon.api.dto.Result;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.common.dto.ClusterServiceCommandHostCommandDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.vo.ClusterServiceCommandHostCommandVO;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群服务命令主机命令控制器
 * 按照架构重构规范，使用Result<VO>返回，调用Converter转换
 * 应用JDK21现代特性和Spring Boot 3.5观测性功能
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@ApiVersion(path = "cluster/service/command/host/command")
public class ClusterServiceCommandHostCommandController {

    @Autowired
    private ClusterServiceCommandHostCommandService clusterServiceCommandHostCommandService;
    
    @Autowired
    private ClusterServiceCommandHostCommandConverter commandConverter;

    /**
     * 获取主机命令列表（分页）
     * 使用JDK21虚拟线程和观测性功能
     */
    @GetMapping("/list")
    @Timed(value = "command.host.command.list", description = "获取主机命令列表的时间")
    public Result<PageResult<ClusterServiceCommandHostCommandVO>> list(
            @RequestParam("hostname") String hostname, 
            @RequestParam("commandHostId") String commandHostId, 
            @RequestParam("page") Integer page, 
            @RequestParam("pageSize") Integer pageSize) {
        
        var threadInfo = getCurrentThreadInfo();
        log.debug("获取主机命令列表: hostname={}, commandHostId={}, page={}, pageSize={} - {}", 
                 hostname, commandHostId, page, pageSize, threadInfo);
        
        // 调用Service获取DTO分页结果
        var dtoPageResult = clusterServiceCommandHostCommandService.getHostCommandList(
                hostname, commandHostId, page, pageSize);
        
        // DTO转VO列表 - 使用JDK21特性
        var voList = dtoPageResult.getRecords().stream()
                .map(commandConverter::dtoToVo)
                .toList();
        
        var voPageResult = PageResult.of(voList, dtoPageResult.getTotal(), page, pageSize);
        return Result.success(voPageResult);
    }

    /**
     * 获取主机命令日志
     */
    @GetMapping("/getHostCommandLog")
    @Timed(value = "command.host.command.log", description = "获取主机命令日志的时间")
    public Result<String> getHostCommandLog(
            @RequestParam("clusterId") Integer clusterId, 
            @RequestParam("hostCommandId") String hostCommandId) throws Exception {
        
        log.debug("获取主机命令日志: clusterId={}, hostCommandId={}", clusterId, hostCommandId);
        
        var logContent = clusterServiceCommandHostCommandService.getHostCommandLog(clusterId, hostCommandId);
        return Result.success(logContent);
    }

    /**
     * 根据ID获取主机命令信息
     */
    @GetMapping("/info/{id}")
    @Timed(value = "command.host.command.info", description = "获取主机命令信息的时间")
    public Result<ClusterServiceCommandHostCommandVO> info(@PathVariable("id") String id) {
        log.debug("获取主机命令信息: {}", id);
        
        var dto = clusterServiceCommandHostCommandService.getByIdAsDto(id);
        if (dto == null) {
            return Result.error("主机命令不存在");
        }
        
        var vo = commandConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存主机命令
     */
    @PostMapping("/save")
    @Timed(value = "command.host.command.save", description = "保存主机命令的时间")
    public Result<ClusterServiceCommandHostCommandVO> save(@RequestBody ClusterServiceCommandHostCommandDTO dto) {
        log.debug("保存主机命令: commandName={}, hostname={}", dto.commandName(), dto.hostname());
        
        var savedDto = clusterServiceCommandHostCommandService.saveHostCommand(dto);
        var vo = commandConverter.dtoToVo(savedDto);
        return Result.success(vo);
    }

    /**
     * 更新主机命令
     */
    @PutMapping("/update")
    @Timed(value = "command.host.command.update", description = "更新主机命令的时间")
    public Result<String> update(@RequestBody ClusterServiceCommandHostCommandDTO dto) {
        log.debug("更新主机命令: {}", dto.hostCommandId());
        
        clusterServiceCommandHostCommandService.updateHostCommand(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除主机命令
     */
    @DeleteMapping("/delete")
    @Timed(value = "command.host.command.delete", description = "删除主机命令的时间")
    public Result<String> delete(@RequestBody String[] ids) {
        log.debug("批量删除主机命令: {}", List.of(ids)); // JDK21特性
        
        // 使用JDK21 switch表达式处理批量删除
        var deleteCount = switch (ids.length) {
            case 0 -> {
                log.warn("批量删除主机命令：没有提供要删除的命令ID");
                yield 0;
            }
            case 1 -> {
                clusterServiceCommandHostCommandService.removeById(ids[0]);
                yield 1;
            }
            default -> {
                clusterServiceCommandHostCommandService.removeByIds(List.of(ids)); // JDK21特性
                yield ids.length;
            }
        };
        
        return Result.success("成功删除 " + deleteCount + " 个主机命令");
    }
    
    /**
     * 获取失败的主机命令
     */
    @GetMapping("/failed")
    @Timed(value = "command.host.command.failed", description = "获取失败主机命令的时间")
    public Result<List<ClusterServiceCommandHostCommandVO>> getFailedHostCommands(
            @RequestParam("hostname") String hostname,
            @RequestParam("commandHostId") String commandHostId) {
        log.debug("获取失败的主机命令: hostname={}, commandHostId={}", hostname, commandHostId);
        
        var dtos = clusterServiceCommandHostCommandService.findFailedHostCommand(hostname, commandHostId);
        var vos = commandConverter.dtoListToVoList(dtos);
        return Result.success(vos);
    }

    /**
     * 获取取消的主机命令
     */
    @GetMapping("/canceled")
    @Timed(value = "command.host.command.canceled", description = "获取取消主机命令的时间")
    public Result<List<ClusterServiceCommandHostCommandVO>> getCanceledHostCommands(
            @RequestParam("hostname") String hostname,
            @RequestParam("commandHostId") String commandHostId) {
        log.debug("获取取消的主机命令: hostname={}, commandHostId={}", hostname, commandHostId);
        
        var dtos = clusterServiceCommandHostCommandService.findCanceledHostCommand(hostname, commandHostId);
        var vos = commandConverter.dtoListToVoList(dtos);
        return Result.success(vos);
    }
    
    /**
     * 获取当前线程信息 - 兼容JDK 21特性
     */
    private String getCurrentThreadInfo() {
        var thread = Thread.currentThread();
        if (thread.isVirtual()) {
            return String.format("虚拟线程[%s]", thread.getName());
        } else {
            return String.format("平台线程[%s]", thread.getName());
        }
    }
}
