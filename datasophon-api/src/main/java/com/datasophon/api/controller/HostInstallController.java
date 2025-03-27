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

package com.datasophon.api.controller;


import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.InstallService;
import com.datasophon.common.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.*;

@Validated
@RestController
@RequestMapping("host/install")
public class HostInstallController {

    @Autowired
    private InstallService installService;

    /**
     * 获取安装步骤
     */
    @GetMapping("/getInstallStep")
    public Result getInstallStep(Integer type) {
        return installService.getInstallStep(type);
    }

    /**
     * 解析主机列表
     */
    @PostMapping("/analysisHostList")
    @UserPermission
    public Result analysisHostList(@RequestParam Integer clusterId,
            @RequestParam @NotBlank(message = "主机列表不能为空") String ips,
            @RequestParam @Pattern(regexp = "(?=.*?[a-z_])[a-zA-Z0-9._\\-]{1,30}", message = "非法的SSH用户名") String sshUser,
            @RequestParam @NotNull(message = "SSH端口必填") @Min(value = 1, message = "非法的SSH端口") @Max(value = 65535, message = "非法的SSH端口") Integer sshPort,
            @RequestParam @NotBlank(message = "SSH密码不能为空") String sshPassword,
            @RequestParam Integer page,
            @RequestParam Integer pageSize) {
        return installService.analysisHostList(clusterId, ips, sshUser, sshPort, sshPassword, page, pageSize);
    }

    /**
     * 查询主机校验状态
     */
    @PostMapping("/getHostCheckStatus")
    @UserPermission
    public Result getHostCheckStatus(Integer clusterId, String sshUser, Integer sshPort) {
        return installService.getHostCheckStatus(clusterId, sshUser, sshPort);
    }

    /**
     * 查询主机校验是否全部完成
     */
    @PostMapping("/hostCheckCompleted")
    @UserPermission
    public Result hostCheckCompleted(@RequestParam("clusterId") Integer clusterId) {
        return installService.hostCheckCompleted(clusterId);
    }

    /**
     * 清理主机检查资源
     * 在hostCheckCompleted返回成功且hostCheckCompleted为true后调用
     * 用于释放与检查任务和修复任务相关的资源
     * 
     * @param clusterId 集群ID
     * @return 清理结果
     */
    @PostMapping("/cleanupHostCheckResources")
    @UserPermission
    public Result cleanupHostCheckResources(@RequestParam("clusterId") Integer clusterId) {
        return installService.cleanupHostCheckResources(clusterId);
    }

    /**
     * 清理主机环境校验缓存
     *
     * @return 清理结果
     */
    @GetMapping("/clearHostEnvCheckCache")
    @UserPermission
    public Result clearHostEnvCheckCache() {
        return installService.clearHostEnvCheckCache();
    }

    /**
     * 主机管理agent分发安装进度列表
     */
    @PostMapping("/dispatcherHostAgentList")
    @UserPermission
    public Result dispatcherHostAgentList(Integer clusterId, Integer installStateCode, Integer page, Integer pageSize) {
        return installService.dispatcherHostAgentList(clusterId, installStateCode, page, pageSize);
    }

    @PostMapping("/dispatcherHostAgentCompleted")
    public Result dispatcherHostAgentCompleted(Integer clusterId) {
        return installService.dispatcherHostAgentCompleted(clusterId);
    }

    /**
     * 主机管理agent分发取消
     */
    @PostMapping("/cancelDispatcherHostAgent")
    public Result cancelDispatcherHostAgent(Integer clusterId, String ip, Integer installStateCode) {
        return installService.cancelDispatcherHostAgent(clusterId, ip, installStateCode);
    }

    /**
     * 主机管理agent分发安装重试
     *
     * @param clusterId
     * @param ips
     * @return
     */
    @PostMapping("/reStartDispatcherHostAgent")
    public Result reStartDispatcherHostAgent(Integer clusterId, String ips) {
        return installService.reStartDispatcherHostAgent(clusterId, ips);
    }

    /**
     * 主机管理agent操作(启动(start)、停止(stop)、重启(restart))
     * 
     * @param clusterHostIds
     * @param commandType
     * @return
     */
    @PostMapping("/generateHostAgentCommand")
    public Result generateHostAgentCommand(
            @RequestParam String clusterHostIds,
            @RequestParam String commandType) throws Exception {
        return installService.generateHostAgentCommand(clusterHostIds, commandType);
    }

    /**
     * 启动/停止 主机上服务启动
     * 
     * @param clusterHostIds
     * @param commandType
     * @return
     */
    @PostMapping("/generateHostServiceCommand")
    public Result generateHostServiceCommand(
            @RequestParam String clusterHostIds,
            @RequestParam String commandType) throws Exception {
        return installService.generateHostServiceCommand(clusterHostIds, commandType);
    }

    /**
     * 修复单个检查项
     */
    @PostMapping("/fixCheckItem")
    @UserPermission
    public Result fixCheckItem(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam("hostname") String hostname,
            @RequestParam("itemId") Integer itemId,
            @RequestParam(value = "skipConfirm", required = false, defaultValue = "false") Boolean skipConfirm) {
        return installService.fixCheckItem(clusterId, hostname, itemId, skipConfirm);
    }

    /**
     * 修复选中的多个检查项
     */
    @PostMapping("/fixSelectedCheckItems")
    @UserPermission
    public Result fixSelectedCheckItems(Integer clusterId, String hostname, String itemIds) {
        return installService.fixSelectedCheckItems(clusterId, hostname, itemIds);
    }

    /**
     * 修复所有检查项
     */
    @PostMapping("/fixAllCheckItems")
    @UserPermission
    public Result fixAllCheckItems(Integer clusterId, String hostname) {
        return installService.fixAllCheckItems(clusterId, hostname);
    }

}
