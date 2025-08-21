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
import com.datasophon.api.converter.ClusterServiceInstanceConfigConverter;
import com.datasophon.api.dto.Result;
import com.datasophon.api.service.ClusterServiceInstanceConfigService;
import com.datasophon.common.dto.ClusterServiceInstanceConfigDTO;
import com.datasophon.common.dto.ConfigVersionDTO;
import com.datasophon.common.vo.ClusterServiceInstanceConfigVO;
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
 * 集群服务实例配置控制器
 * 按照架构重构规范，使用Result<VO>返回，调用Converter转换
 * 应用JDK21现代特性和Spring Boot 3.5观测性功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@ApiVersion(path = "cluster/service/instance/config")
public class ClusterServiceInstanceConfigController {

    @Autowired
    private ClusterServiceInstanceConfigService clusterServiceInstanceConfigService;
    
    @Autowired
    private ClusterServiceInstanceConfigConverter configConverter;

    /**
     * 获取配置版本列表
     * 使用JDK21虚拟线程和观测性功能
     */
    @GetMapping("/getConfigVersion")
    @Timed(value = "config.version.list", description = "获取配置版本列表的时间")
    public Result<List<ConfigVersionDTO>> getConfigVersion(
            @RequestParam("serviceInstanceId") Long serviceInstanceId,
            @RequestParam("roleGroupId") Long roleGroupId) {
        
        var threadInfo = getCurrentThreadInfo();
        log.debug("获取配置版本列表: serviceInstanceId={}, roleGroupId={} - {}", 
                 serviceInstanceId, roleGroupId, threadInfo);
        
        // 调用Service获取DTO列表
        var configVersions = clusterServiceInstanceConfigService.getConfigVersion(serviceInstanceId, roleGroupId);
        
        return Result.success(configVersions);
    }

    /**
     * 获取服务实例配置信息
     */
    @GetMapping("/info")
    @Timed(value = "config.instance.info", description = "获取服务实例配置信息的时间")
    public Result<Object> info(
            @RequestParam("serviceInstanceId") Long serviceInstanceId,
            @RequestParam("version") Integer version, 
            @RequestParam("roleGroupId") Long roleGroupId, 
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        
        log.debug("获取服务实例配置信息: serviceInstanceId={}, version={}, roleGroupId={}", 
                 serviceInstanceId, version, roleGroupId);
        
        // 调用Service获取配置结果DTO
        var configResult = clusterServiceInstanceConfigService.getServiceInstanceConfig(
                serviceInstanceId, version, roleGroupId, page, pageSize);
        
        return Result.success(configResult);
    }
    
    /**
     * 分页查询服务实例配置列表
     */
    @GetMapping("/list")
    @Timed(value = "config.instance.list", description = "分页查询服务实例配置列表的时间")
    public Result<Object> list(
            @RequestParam(value = "clusterId", required = false) Long clusterId,
            @RequestParam(value = "serviceId", required = false) Long serviceId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        
        log.debug("分页查询服务实例配置列表: clusterId={}, serviceId={}", clusterId, serviceId);
        
        var pageResult = clusterServiceInstanceConfigService.getServiceInstanceConfigListByPage(
                clusterId, serviceId, page, pageSize);
        
        // DTO转VO列表 - 使用JDK21特性
        var voList = pageResult.getRecords().stream()
                .map(configConverter::dtoToVo)
                .toList();
        
        return Result.success()
                .put("list", voList)
                .put("total", pageResult.getTotal())
                .put("page", pageResult.getCurrent())
                .put("pageSize", pageResult.getSize());
    }

    /**
     * 根据ID获取服务实例配置信息
     */
    @GetMapping("/info/{id}")
    @Timed(value = "config.instance.get", description = "获取服务实例配置信息的时间")
    public Result<ClusterServiceInstanceConfigVO> getById(@PathVariable("id") Long id) {
        log.debug("获取服务实例配置信息: {}", id);
        
        var configDTO = clusterServiceInstanceConfigService.getServiceInstanceConfigById(id);
        var configVO = configConverter.dtoToVo(configDTO);
        
        return Result.success(configVO);
    }

    /**
     * 创建服务实例配置
     */
    @PostMapping("/save")
    @Timed(value = "config.instance.save", description = "创建服务实例配置的时间")
    public Result<ClusterServiceInstanceConfigVO> save(
            @RequestBody ClusterServiceInstanceConfigDTO configDTO) {
        log.debug("创建服务实例配置: serviceId={}", configDTO.serviceId());
        
        var createdConfig = clusterServiceInstanceConfigService.createServiceInstanceConfig(configDTO);
        var configVO = configConverter.dtoToVo(createdConfig);
        
        return Result.success(configVO);
    }

    /**
     * 更新服务实例配置
     */
    @PutMapping("/update")
    @Timed(value = "config.instance.update", description = "更新服务实例配置的时间")
    public Result<ClusterServiceInstanceConfigVO> update(
            @RequestBody ClusterServiceInstanceConfigDTO configDTO) {
        log.debug("更新服务实例配置: {}", configDTO.id());
        
        var updatedConfig = clusterServiceInstanceConfigService.updateServiceInstanceConfig(configDTO);
        var configVO = configConverter.dtoToVo(updatedConfig);
        
        return Result.success(configVO);
    }

    /**
     * 删除服务实例配置
     */
    @DeleteMapping("/delete/{id}")
    @Timed(value = "config.instance.delete", description = "删除服务实例配置的时间")
    public Result<Object> delete(@PathVariable("id") Long id) {
        log.debug("删除服务实例配置: {}", id);
        
        clusterServiceInstanceConfigService.removeById(id);
        
        return Result.success("服务实例配置删除成功");
    }

    /**
     * 批量删除服务实例配置
     */
    @DeleteMapping("/delete/batch")
    @Timed(value = "config.instance.delete.batch", description = "批量删除服务实例配置的时间")
    public Result<Object> deleteBatch(@RequestBody Integer[] ids) {
        log.debug("批量删除服务实例配置: {}", List.of(ids)); // JDK21特性
        
        // 使用JDK21 switch表达式处理批量删除
        var deleteCount = switch (ids.length) {
            case 0 -> {
                log.warn("批量删除服务实例配置：没有提供要删除的配置ID");
                yield 0;
            }
            case 1 -> {
                clusterServiceInstanceConfigService.removeById(ids[0]);
                yield 1;
            }
            default -> {
                // 批量删除
                clusterServiceInstanceConfigService.removeByIds(List.of(ids)); // JDK21特性
                yield ids.length;
            }
        };
        
        return Result.success("成功删除 " + deleteCount + " 个服务实例配置");
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
