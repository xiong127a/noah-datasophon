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

package com.datasophon.api.controller.v1.user;

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.converter.RoleInfoConverter;
import com.datasophon.api.dto.Result;
import com.datasophon.api.service.RoleInfoService;
import com.datasophon.common.dto.RoleInfoDTO;
import com.datasophon.common.vo.RoleInfoVO;
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
 * 角色信息控制器
 * 按照架构重构规范，使用Result<VO>返回，调用Converter转换
 * 应用JDK21现代特性和Spring Boot 3.5观测性功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@ApiVersion(path = "role/info")
public class RoleInfoController {

    @Autowired
    private RoleInfoService roleInfoService;
    
    @Autowired
    private RoleInfoConverter roleInfoConverter;

    /**
     * 分页查询角色列表
     * 使用JDK21虚拟线程和观测性功能
     */
    @GetMapping("/list")
    @Timed(value = "role.list", description = "获取角色列表的时间")
    public Result<Object> list(
            @RequestParam(value = "roleName", required = false) String roleName,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        
        var threadInfo = getCurrentThreadInfo();
        log.debug("分页查询角色列表 - {}", threadInfo);
        
        var pageResult = roleInfoService.getRoleListByPage(roleName, page, pageSize);
        
        // DTO转VO
        var voList = pageResult.getRecords().stream()
                .map(roleInfoConverter::dtoToVo)
                .toList(); // JDK21特性
        
        return Result.success()
                .put("list", voList)
                .put("total", pageResult.getTotal())
                .put("page", pageResult.getPage())
                .put("pageSize", pageResult.getSize());
    }

    /**
     * 获取所有角色（不分页）
     */
    @GetMapping("/all")
    @Timed(value = "role.all", description = "获取所有角色的时间")
    public Result<List<RoleInfoVO>> all() {
        log.debug("获取所有角色列表");
        
        var roleList = roleInfoService.getAllRoles();
        var voList = roleList.stream()
                .map(roleInfoConverter::dtoToVo)
                .toList(); // JDK21特性
        
        return Result.success(voList);
    }

    /**
     * 根据ID获取角色信息
     */
    @GetMapping("/info/{id}")
    @Timed(value = "role.info", description = "获取角色信息的时间")
    public Result<RoleInfoVO> info(@PathVariable("id") Integer id) {
        log.debug("获取角色信息: {}", id);
        
        var roleDTO = roleInfoService.getRoleById(id);
        var roleVO = roleInfoConverter.dtoToVo(roleDTO);
        
        return Result.success(roleVO);
    }

    /**
     * 创建角色
     */
    @PostMapping("/save")
    @Timed(value = "role.save", description = "创建角色的时间")
    public Result<RoleInfoVO> save(@RequestBody RoleInfoDTO roleInfoDTO) {
        log.debug("创建角色: {}", roleInfoDTO.roleName());
        
        var createdRole = roleInfoService.createRole(roleInfoDTO);
        var roleVO = roleInfoConverter.dtoToVo(createdRole);
        
        return Result.success(roleVO);
    }

    /**
     * 更新角色
     */
    @PutMapping("/update")
    @Timed(value = "role.update", description = "更新角色的时间")
    public Result<RoleInfoVO> update(@RequestBody RoleInfoDTO roleInfoDTO) {
        log.debug("更新角色: {}", roleInfoDTO.id());
        
        var updatedRole = roleInfoService.updateRole(roleInfoDTO);
        var roleVO = roleInfoConverter.dtoToVo(updatedRole);
        
        return Result.success(roleVO);
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/delete/{id}")
    @Timed(value = "role.delete", description = "删除角色的时间")
    public Result<Object> delete(@PathVariable("id") Integer id) {
        log.debug("删除角色: {}", id);
        
        roleInfoService.deleteRole(id);
        
        return Result.success("角色删除成功");
    }

    /**
     * 批量删除角色
     */
    @DeleteMapping("/delete/batch")
    @Timed(value = "role.delete.batch", description = "批量删除角色的时间")
    public Result<Object> deleteBatch(@RequestBody Integer[] ids) {
        log.debug("批量删除角色: {}", List.of(ids)); // JDK21特性
        
        // 使用JDK21 switch表达式处理批量删除
        var deleteCount = switch (ids.length) {
            case 0 -> {
                log.warn("批量删除角色：没有提供要删除的角色ID");
                yield 0;
            }
            case 1 -> {
                roleInfoService.deleteRole(ids[0]);
                yield 1;
            }
            default -> {
                // 批量删除
                for (var id : ids) {
                    roleInfoService.deleteRole(id);
                }
                yield ids.length;
            }
        };
        
        return Result.success("成功删除 " + deleteCount + " 个角色");
    }

    /**
     * 检查角色编码是否存在
     */
    @GetMapping("/check/code")
    @Timed(value = "role.check.code", description = "检查角色编码的时间")
    public Result<Boolean> checkRoleCode(
            @RequestParam("roleCode") String roleCode,
            @RequestParam(value = "excludeId", required = false) Integer excludeId) {
        
        log.debug("检查角色编码是否存在: roleCode={}, excludeId={}", roleCode, excludeId);
        
        var exists = roleInfoService.checkRoleCodeExists(roleCode, excludeId);
        
        return Result.success(exists);
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
