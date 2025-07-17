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
import com.datasophon.api.service.HostCheckService;
import com.datasophon.api.service.InstallService;
import com.datasophon.common.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("host/install")
public class HostInstallController {

    @Autowired
    private InstallService installService;

    @Autowired
    private HostCheckService hostCheckService;

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
            @RequestParam(required = false) String hosts,
            @RequestParam(required = false) String sshUser,
            @RequestParam(required = false) Integer sshPort,
            @RequestParam(required = false) String sshPassword,
            @RequestParam Integer page,
            @RequestParam Integer pageSize) {
        return installService.analysisHostList(clusterId, hosts, sshUser, sshPort,sshPassword, page, pageSize);
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
     * 开始主机检查
     *
     * @param clusterId 集群ID
     * @return 开始检查结果
     */
    @PostMapping("/startHostCheck")
    @UserPermission
    public Result startHostCheck(@RequestParam Integer clusterId) {
        return hostCheckService.startHostCheck(clusterId);
    }

    /**
     * 获取主机最近日志
     * 当鼠标悬浮在主机状态信息上时调用此接口
     *
     * @param ip        主机IP
     * @param clusterId 集群ID
     * @return 主机最近日志内容
     */
    @GetMapping("/getWorkerLog")
    @UserPermission
    public Result getWorkerLog(@RequestParam("ip") String ip,
            @RequestParam("clusterId") Integer clusterId) {
        return installService.getWorkerLog(ip, clusterId);
    }

}
