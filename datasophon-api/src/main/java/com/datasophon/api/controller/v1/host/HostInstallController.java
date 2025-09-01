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

package com.datasophon.api.controller.v1.host;

import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.HostCheckService;
import com.datasophon.api.service.InstallService;
import com.datasophon.common.dto.HostCheckStatusDto;
import com.datasophon.common.dto.InstallStepDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.model.HostInfo;
import com.datasophon.api.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.annotation.ClusterId;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 主机安装控制器
 * 负责处理主机安装相关的API接口
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Slf4j
@Validated
@ApiVersion(path = "host/install")
@RequiredArgsConstructor
public class HostInstallController {

    private final InstallService installService;
    private HostCheckService hostCheckService;

    /**
     * 获取安装步骤
     */
    @GetMapping("/getInstallStep")
    public Result<InstallStepDTO> getInstallStep(@RequestParam("type") Integer type) {
        try {
            if (type == null) {
                return Result.error("安装类型不能为空");
            }
            log.debug("获取安装步骤，类型: {}", type);
            InstallStepDTO installSteps = installService.getInstallStep(type);
            return Result.success(installSteps);
        } catch (Exception e) {
            log.error("获取安装步骤失败，类型: {}, 错误: {}", type, e.getMessage(), e);
            return Result.error("获取安装步骤失败: " + e.getMessage());
        }
    }



    /**
     * 查询主机校验状态
     */
    @PostMapping("/getHostCheckStatus")
    @UserPermission
    public Result<HostCheckStatusDto> getHostCheckStatus(@ClusterId Long clusterId,
            @RequestParam("sshUser") String sshUser,
            @RequestParam("sshPort") Integer sshPort) {
        try {
            if (clusterId == null) {
                return Result.error("集群ID不能为空");
            }
            log.debug("查询主机校验状态，集群ID: {}", clusterId);
            HostCheckStatusDto statusDto = installService.getHostCheckStatus(clusterId, sshUser, sshPort);
            return Result.success(statusDto);
        } catch (Exception e) {
            log.error("查询主机校验状态失败，集群ID: {}, 错误: {}", clusterId, e.getMessage(), e);
            return Result.error("查询主机校验状态失败: " + e.getMessage());
        }
    }

    /**
     * 查询主机校验是否全部完成
     */
    @PostMapping("/hostCheckCompleted")
    @UserPermission
    public Result<Boolean> hostCheckCompleted(@ClusterId Long clusterId) {
        try {
            boolean completed = installService.hostCheckCompleted(clusterId);
            return Result.success(completed);
        } catch (Exception e) {
            return Result.error("查询主机校验完成状态失败: " + e.getMessage());
        }
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
    public Result<Boolean> cleanupHostCheckResources(@ClusterId Long clusterId) {
        try {
            boolean success = installService.cleanupHostCheckResources(clusterId);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("主机检查资源清理失败");
            }
        } catch (Exception e) {
            return Result.error("清理主机检查资源失败: " + e.getMessage());
        }
    }

    /**
     * 清理主机环境校验缓存
     *
     * @return 清理结果
     */
    @GetMapping("/clearHostEnvCheckCache")
    @UserPermission
    public Result<Boolean> clearHostEnvCheckCache() {
        try {
            boolean success = installService.clearHostEnvCheckCache();
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("主机环境校验缓存清理失败");
            }
        } catch (Exception e) {
            return Result.error("清理主机环境校验缓存失败: " + e.getMessage());
        }
    }

    /**
     * 主机管理agent分发安装进度列表
     */
    @PostMapping("/dispatcherHostAgentList")
    @UserPermission
    public Result<PageResult<HostInfo>> dispatcherHostAgentList(@ClusterId Long clusterId,
            @RequestParam("installStateCode") Integer installStateCode, @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        try {
            PageResult<HostInfo> pageResult = installService.dispatcherHostAgentList(clusterId, installStateCode, page,
                    pageSize);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("获取主机代理分发列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/dispatcherHostAgentCompleted")
    public Result<Boolean> dispatcherHostAgentCompleted(@ClusterId Long clusterId) {
        try {
            boolean completed = installService.dispatcherHostAgentCompleted(clusterId);
            return Result.success(completed);
        } catch (Exception e) {
            return Result.error("查询主机代理分发完成状态失败: " + e.getMessage());
        }
    }

    /**
     * 主机管理agent分发取消
     */
    @PostMapping("/cancelDispatcherHostAgent")
    public Result<Boolean> cancelDispatcherHostAgent(@ClusterId Long clusterId, @RequestParam("ip") String ip,
            @RequestParam("installStateCode") Integer installStateCode) {
        try {
            boolean success = installService.cancelDispatcherHostAgent(clusterId, ip, installStateCode);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("取消主机代理分发失败");
            }
        } catch (Exception e) {
            return Result.error("取消主机代理分发失败: " + e.getMessage());
        }
    }

    /**
     * 主机管理agent分发安装重试
     *
     */
    @PostMapping("/reStartDispatcherHostAgent")
    public Result<Boolean> reStartDispatcherHostAgent(@ClusterId Long clusterId, @RequestParam("ips") String ips) {
        try {
            boolean success = installService.reStartDispatcherHostAgent(clusterId, ips);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("重启主机代理分发失败");
            }
        } catch (Exception e) {
            return Result.error("重启主机代理分发失败: " + e.getMessage());
        }
    }

    /**
     * 主机管理agent操作(启动(start)、停止(stop)、重启(restart))
     *
     */
    @PostMapping("/generateHostAgentCommand")
    public Result<List<Map<String, Object>>> generateHostAgentCommand(
            @RequestParam String clusterHostIds,
            @RequestParam String commandType) throws Exception {
        try {
            List<Map<String, Object>> commands = installService.generateHostAgentCommand(clusterHostIds, commandType);
            return Result.success(commands);
        } catch (Exception e) {
            return Result.error("生成主机代理命令失败: " + e.getMessage());
        }
    }

    /**
     * 启动/停止 主机上服务启动
     *
     */
    @PostMapping("/generateHostServiceCommand")
    public Result<List<Map<String, Object>>> generateHostServiceCommand(
            @RequestParam String clusterHostIds,
            @RequestParam String commandType) {
        try {
            List<Map<String, Object>> commands = installService.generateHostServiceCommand(clusterHostIds, commandType);
            return Result.success(commands);
        } catch (Exception e) {
            return Result.error("生成主机服务命令失败: " + e.getMessage());
        }
    }

    /**
     * 开始主机检查
     *
     * @param clusterId 集群ID
     * @return 开始检查结果
     */
    @PostMapping("/startHostCheck")
    @UserPermission
    public Result<Boolean> startHostCheck(@ClusterId Long clusterId) {
        try {
            boolean success = hostCheckService.startHostCheck(clusterId);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("主机检查启动失败");
            }
        } catch (Exception e) {
            return Result.error("启动主机检查失败: " + e.getMessage());
        }
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
    public Result<String> getWorkerLog(@RequestParam("ip") String ip,
            @ClusterId Long clusterId) {
        try {
            String logContent = installService.getWorkerLog(ip, clusterId);
            return Result.success(logContent);
        } catch (Exception e) {
            return Result.error("获取主机日志失败: " + e.getMessage());
        }
    }

}
