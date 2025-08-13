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

package com.datasophon.api.controller.v1.service;

import com.alibaba.fastjson2.JSONArray;
import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.ServiceInstallService;
import com.datasophon.common.model.HostServiceRoleMapping;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleHostMapping;
import com.datasophon.common.dto.ServiceConfigGroupDTO;
import com.datasophon.api.dto.Result;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 服务安装控制器
 * 负责服务配置、角色映射、安装等操作
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@ApiVersion(path = "service/install")
public class ServiceInstallController {

    @Autowired
    private ServiceInstallService serviceInstallService;

    /**
     * 获取当前线程信息（虚拟线程支持）
     */
    private String getCurrentThreadInfo() {
        var thread = Thread.currentThread();
        return String.format("Thread[%s, virtual=%s]", 
                thread.getName(), thread.isVirtual());
    }

    /**
     * 根据服务角色名称查询服务配置选项
     * 返回按分组组织的配置数据，提升前端用户体验
     */
    @GetMapping("/getServiceConfigOption")
    @Timed(value = "service.install.config.option", description = "获取服务配置选项的时间")
    public Result<ServiceConfigGroupDTO> getServiceConfigOption(@ClusterId Long clusterId,
                                                               @RequestParam("serviceName") String serviceName) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.debug("获取服务配置选项: clusterId={}, serviceName={} - {}", 
                 clusterId, serviceName, threadInfo);
        
        var configOptions = serviceInstallService.getServiceConfigOption(clusterId, serviceName);
        return Result.success(configOptions);
    }

    /**
     * 保存服务配置
     */
    @PostMapping("/saveServiceConfig")
    @UserPermission
    @Timed(value = "service.install.config.save", description = "保存服务配置的时间")
    public Result<Boolean> saveServiceConfig(@ClusterId Long clusterId,
                                            @RequestParam("serviceName") String serviceName, 
                                            @RequestParam("serviceConfig") String serviceConfig, 
                                            @RequestParam(name = "roleGroupId", required = false) Integer roleGroupId,
                                            @RequestParam(name = "description", required = false) String description, 
                                            @RequestParam(name = "userId", required = false) Integer userId, 
                                            @RequestParam(name = "username", required = false) String username) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.debug("保存服务配置: clusterId={}, serviceName={} - {}", 
                 clusterId, serviceName, threadInfo);
        
        var jsonArray = JSONArray.parseArray(serviceConfig); // JDK21特性
        var configList = jsonArray.toJavaList(ServiceConfig.class);
        var result = serviceInstallService.saveServiceConfig(clusterId, serviceName, configList, 
                                                            roleGroupId, description, userId, username);
        return Result.success(result);
    }

    /**
     * 保存服务角色与主机对应关系
     */
    @PostMapping("/saveServiceRoleHostMapping/{clusterId}")
    @Timed(value = "service.install.role.host.mapping.save", description = "保存服务角色主机映射的时间")
    public Result<Boolean> saveServiceRoleHostMapping(@RequestBody List<ServiceRoleHostMapping> list,
                                                     @PathVariable("clusterId") Long clusterId) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.debug("保存服务角色主机映射: clusterId={}, mappingCount={} - {}", 
                 clusterId, list.size(), threadInfo);
        
        var result = serviceInstallService.saveServiceRoleHostMapping(clusterId, list);
        return Result.success(result);
    }

    /**
     * 查询服务角色与主机对应关系
     */
    @GetMapping("/getServiceRoleHostMapping")
    @Timed(value = "service.install.role.host.mapping.get", description = "获取服务角色主机映射的时间")
    @UserPermission
    public Result<String> getServiceRoleHostMapping(@ClusterId Long clusterId) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.debug("获取服务角色主机映射: clusterId={} - {}", clusterId, threadInfo);
        
        serviceInstallService.getServiceRoleHostMapping(clusterId);
        return Result.success("获取成功");
    }

    /**
     * 保存主机与服务角色对应关系
     */
    @PostMapping("/saveHostServiceRoleMapping/{clusterId}")
    @Timed(value = "service.install.host.role.mapping.save", description = "保存主机服务角色映射的时间")
    public Result<String> saveHostServiceRoleMapping(@PathVariable("clusterId") Long clusterId,
                                                    @RequestBody List<HostServiceRoleMapping> list) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.debug("保存主机服务角色映射: clusterId={}, mappingCount={} - {}", 
                 clusterId, list.size(), threadInfo);
        
        serviceInstallService.saveHostServiceRoleMapping(clusterId, list);
        return Result.success("保存成功");
    }

    /**
     * 服务部署总览
     */
    @GetMapping("/getServiceRoleDeployOverview")
    @Timed(value = "service.install.deploy.overview", description = "获取服务部署总览的时间")
    public Result<Map<String, List<String>>> getServiceRoleDeployOverview(@ClusterId Long clusterId) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.debug("获取服务部署总览: clusterId={} - {}", clusterId, threadInfo);
        
        var overview = serviceInstallService.getServiceRoleDeployOverview(clusterId);
        return Result.success(overview);
    }

    /**
     * 开始安装服务
     */
    @PostMapping("/startInstallService/{clusterId}")
    @Timed(value = "service.install.start", description = "开始安装服务的时间")
    public Result<Map<String, Object>> startInstallService(@PathVariable("clusterId") Long clusterId,
                                                          @RequestBody List<String> commandIds) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.info("开始安装服务: clusterId={}, commandIds={} - {}", 
                clusterId, commandIds, threadInfo);
        
        var result = serviceInstallService.startInstallService(clusterId, commandIds);
        return Result.success(result);
    }

    /**
     * 下载安装包
     */
    @GetMapping("/downloadPackage")
    @Timed(value = "service.install.package.download", description = "下载安装包的时间")
    public void downloadPackage(@RequestParam("packageName") String packageName, 
                               @RequestParam("cpuArchitecture") String cpuArchitecture,
                               HttpServletResponse response) throws IOException {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.info("下载安装包: packageName={}, cpuArchitecture={} - {}", 
                packageName, cpuArchitecture, threadInfo);
        
        serviceInstallService.downloadPackage(packageName, response);
    }

    /**
     * 检查服务依赖关系
     */
    @GetMapping("/checkServiceDependency")
    @Timed(value = "service.install.dependency.check", description = "检查服务依赖关系的时间")
    public Result<String> checkServiceDependency(@ClusterId Long clusterId,
                                                @RequestParam("serviceIds") String serviceIds) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.debug("检查服务依赖关系: clusterId={}, serviceIds={} - {}", 
                 clusterId, serviceIds, threadInfo);
        
        serviceInstallService.checkServiceDependency(clusterId, serviceIds);
        return Result.success("检查完成");
    }

}