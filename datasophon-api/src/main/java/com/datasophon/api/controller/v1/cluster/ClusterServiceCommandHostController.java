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
import com.datasophon.api.converter.ClusterServiceCommandHostConverter;
import com.datasophon.api.dto.Result;
import com.datasophon.api.service.ClusterServiceCommandHostService;
import com.datasophon.common.dto.ClusterServiceCommandHostDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.vo.ClusterServiceCommandHostVO;
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
 * 集群服务命令主机控制器
 * 按照架构重构规范，使用Result<VO>返回，调用Converter转换
 * 应用JDK21现代特性和Spring Boot 3.5观测性功能
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@ApiVersion(path = "cluster/service/command/host")
public class ClusterServiceCommandHostController {

    @Autowired
    private ClusterServiceCommandHostService clusterServiceCommandHostService;

    @Autowired
    private ClusterServiceCommandHostConverter converter;

    /**
     * 获取命令主机列表（分页）
     * 使用JDK21虚拟线程和观测性功能
     */
    @GetMapping("/list")
    @Timed(value = "command.host.list", description = "获取命令主机列表的时间")
    public Result<PageResult<ClusterServiceCommandHostVO>> list(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam("commandId") String commandId, 
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        
        var threadInfo = getCurrentThreadInfo();
        log.debug("获取命令主机列表: clusterId={}, commandId={}, page={}, pageSize={} - {}", 
                 clusterId, commandId, page, pageSize, threadInfo);
        
        // 调用Service获取DTO分页结果
        var dtoPageResult = clusterServiceCommandHostService.getCommandHostList(clusterId, commandId, page, pageSize);
        
        // DTO转VO列表 - 使用JDK21特性
        var voList = dtoPageResult.getRecords().stream()
                .map(converter::dtoToVo)
                .toList();
        
        var voPageResult = PageResult.of(voList, dtoPageResult.getTotal(), page, pageSize);
        return Result.success(voPageResult);
    }

    /**
     * 根据ID获取命令主机信息
     */
    @GetMapping("/info/{id}")
    @Timed(value = "command.host.info", description = "获取命令主机信息的时间")
    public Result<ClusterServiceCommandHostVO> info(@PathVariable("id") String id) {
        log.debug("获取命令主机信息: {}", id);
        
        var dto = clusterServiceCommandHostService.getByIdAsDto(id);
        if (dto == null) {
            return Result.error("命令主机不存在");
        }
        
        var vo = converter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存命令主机
     */
    @PostMapping("/save")
    @Timed(value = "command.host.save", description = "保存命令主机的时间")
    public Result<ClusterServiceCommandHostVO> save(@RequestBody ClusterServiceCommandHostDTO dto) {
        log.debug("保存命令主机: hostname={}", dto.hostname());
        
        var savedDto = clusterServiceCommandHostService.saveCommandHost(dto);
        var vo = converter.dtoToVo(savedDto);
        return Result.success(vo);
    }

    /**
     * 更新命令主机
     */
    @PutMapping("/update")
    @Timed(value = "command.host.update", description = "更新命令主机的时间")
    public Result<String> update(@RequestBody ClusterServiceCommandHostDTO dto) {
        log.debug("更新命令主机: {}", dto.commandHostId());
        
        clusterServiceCommandHostService.updateCommandHost(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除命令主机
     */
    @DeleteMapping("/delete")
    @Timed(value = "command.host.delete", description = "删除命令主机的时间")
    public Result<String> delete(@RequestBody String[] ids) {
        log.debug("批量删除命令主机: {}", List.of(ids)); // JDK21特性
        
        // 使用JDK21 switch表达式处理批量删除
        var deleteCount = switch (ids.length) {
            case 0 -> {
                log.warn("批量删除命令主机：没有提供要删除的主机ID");
                yield 0;
            }
            case 1 -> {
                clusterServiceCommandHostService.removeById(ids[0]);
                yield 1;
            }
            default -> {
                clusterServiceCommandHostService.removeByIds(List.of(ids)); // JDK21特性
                yield ids.length;
            }
        };
        
        return Result.success("成功删除 " + deleteCount + " 个命令主机");
    }

    /**
     * 获取所有命令主机
     * 注意：此方法应该返回DTO，但Service的list()方法返回Entity，需要调整
     */
    @GetMapping("/all")
    @Timed(value = "command.host.all", description = "获取所有命令主机的时间")
    public Result<List<ClusterServiceCommandHostVO>> getAllCommandHosts() {
        log.debug("获取所有命令主机");
        
        // 这里有架构问题：Service的list()返回Entity而不是DTO
        // 暂时使用，但应该在Service中添加getAllCommandHosts()方法返回DTO
        var entities = clusterServiceCommandHostService.list();
        var vos = converter.entityListToVoList(entities);
        return Result.success(vos);
    }

    /**
     * 获取失败的命令主机
     */
    @GetMapping("/failed")
    @Timed(value = "command.host.failed", description = "获取失败命令主机的时间")
    public Result<List<ClusterServiceCommandHostVO>> getFailedCommandHosts(
            @RequestParam("commandId") String commandId) {
        log.debug("获取失败的命令主机: commandId={}", commandId);
        
        var dtos = clusterServiceCommandHostService.findFailedCommandHost(commandId);
        var vos = converter.dtoListToVoList(dtos);
        return Result.success(vos);
    }

    /**
     * 获取取消的命令主机
     */
    @GetMapping("/canceled")
    @Timed(value = "command.host.canceled", description = "获取取消命令主机的时间")
    public Result<List<ClusterServiceCommandHostVO>> getCanceledCommandHosts(
            @RequestParam("commandId") String commandId) {
        log.debug("获取取消的命令主机: commandId={}", commandId);
        
        var dtos = clusterServiceCommandHostService.findCanceledCommandHost(commandId);
        var vos = converter.dtoListToVoList(dtos);
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
