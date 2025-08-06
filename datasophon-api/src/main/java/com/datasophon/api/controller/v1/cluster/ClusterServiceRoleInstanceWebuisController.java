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
import com.datasophon.api.converter.ClusterServiceRoleInstanceWebuisConverter;
import com.datasophon.api.dto.Result;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import com.datasophon.common.dto.ClusterServiceRoleInstanceWebuisDTO;
import com.datasophon.common.vo.ClusterServiceRoleInstanceWebuisVO;
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
 * 集群服务角色实例WebUI控制器
 * 按照架构重构规范，使用Result<VO>返回，调用Converter转换
 * 应用JDK21现代特性和Spring Boot 3.5观测性功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@ApiVersion(path = "cluster/webuis")
public class ClusterServiceRoleInstanceWebuisController {

    @Autowired
    private ClusterServiceRoleInstanceWebuisService clusterServiceRoleInstanceWebuisService;
    
    @Autowired
    private ClusterServiceRoleInstanceWebuisConverter webuisConverter;

    /**
     * 根据服务实例ID获取WebUI列表
     * 使用JDK21虚拟线程和观测性功能
     */
    @GetMapping("/getWebUis")
    @Timed(value = "webuis.list", description = "获取WebUI列表的时间")
    public Result<List<ClusterServiceRoleInstanceWebuisVO>> getWebUis(
            @RequestParam("serviceInstanceId") Integer serviceInstanceId) {
        
        var threadInfo = getCurrentThreadInfo();
        log.debug("获取服务实例WebUI列表: serviceInstanceId={} - {}", serviceInstanceId, threadInfo);
        
        // 调用Service获取DTO列表
        var webuisDTOList = clusterServiceRoleInstanceWebuisService.getWebUis(serviceInstanceId);
        
        // DTO转VO列表 - 使用JDK21特性
        var webuisVOList = webuisDTOList.stream()
                .map(webuisConverter::dtoToVo)
                .toList();
        
        return Result.success(webuisVOList);
    }

    /**
     * 根据ID获取WebUI信息
     */
    @GetMapping("/info/{id}")
    @Timed(value = "webuis.info", description = "获取WebUI信息的时间")
    public Result<ClusterServiceRoleInstanceWebuisVO> info(@PathVariable("id") Integer id) {
        log.debug("获取WebUI信息: {}", id);
        
        var webuisDTO = clusterServiceRoleInstanceWebuisService.getWebUIById(id);
        var webuisVO = webuisConverter.dtoToVo(webuisDTO);
        
        return Result.success(webuisVO);
    }

    /**
     * 创建WebUI
     */
    @PostMapping("/save")
    @Timed(value = "webuis.save", description = "创建WebUI的时间")
    public Result<ClusterServiceRoleInstanceWebuisVO> save(
            @RequestBody ClusterServiceRoleInstanceWebuisDTO webuisDTO) {
        log.debug("创建WebUI: {}", webuisDTO.name());
        
        var createdWebUI = clusterServiceRoleInstanceWebuisService.createWebUI(webuisDTO);
        var webuisVO = webuisConverter.dtoToVo(createdWebUI);
        
        return Result.success(webuisVO);
    }

    /**
     * 更新WebUI
     */
    @PutMapping("/update")
    @Timed(value = "webuis.update", description = "更新WebUI的时间")
    public Result<ClusterServiceRoleInstanceWebuisVO> update(
            @RequestBody ClusterServiceRoleInstanceWebuisDTO webuisDTO) {
        log.debug("更新WebUI: {}", webuisDTO.id());
        
        var updatedWebUI = clusterServiceRoleInstanceWebuisService.updateWebUI(webuisDTO);
        var webuisVO = webuisConverter.dtoToVo(updatedWebUI);
        
        return Result.success(webuisVO);
    }

    /**
     * 删除WebUI
     */
    @DeleteMapping("/delete/{id}")
    @Timed(value = "webuis.delete", description = "删除WebUI的时间")
    public Result<Object> delete(@PathVariable("id") Integer id) {
        log.debug("删除WebUI: {}", id);
        
        clusterServiceRoleInstanceWebuisService.removeById(id);
        
        return Result.success("WebUI删除成功");
    }

    /**
     * 批量删除WebUI
     */
    @DeleteMapping("/delete/batch")
    @Timed(value = "webuis.delete.batch", description = "批量删除WebUI的时间")
    public Result<Object> deleteBatch(@RequestBody Integer[] ids) {
        log.debug("批量删除WebUI: {}", List.of(ids)); // JDK21特性
        
        // 使用JDK21 switch表达式处理批量删除
        var deleteCount = switch (ids.length) {
            case 0 -> {
                log.warn("批量删除WebUI：没有提供要删除的WebUI ID");
                yield 0;
            }
            case 1 -> {
                clusterServiceRoleInstanceWebuisService.removeById(ids[0]);
                yield 1;
            }
            default -> {
                // 批量删除
                clusterServiceRoleInstanceWebuisService.removeByIds(List.of(ids)); // JDK21特性
                yield ids.length;
            }
        };
        
        return Result.success("成功删除 " + deleteCount + " 个WebUI");
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
