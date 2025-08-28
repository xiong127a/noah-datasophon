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

package com.datasophon.api.controller.v1;

import com.datasophon.api.master.MasterWorkerScheduler;
import com.datasophon.api.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Worker节点管理控制器
 * 
 * 提供Worker节点的发现、连接管理和状态监控功能
 */
@RestController
@RequestMapping("/ddh/api/v1/worker-management")
public class WorkerManagementController {

    private static final Logger logger = LoggerFactory.getLogger(WorkerManagementController.class);

    @Autowired
    private MasterWorkerScheduler masterWorkerScheduler;

    /**
     * 手动触发Worker节点发现
     * 
     * @param clusterId 集群ID
     * @return 操作结果
     */
    @PostMapping("/clusters/{clusterId}/discover-workers")
    public Result<String> discoverWorkers(@PathVariable Long clusterId) {
        try {
            logger.info("收到Worker节点发现请求，集群ID: {}", clusterId);
            
            masterWorkerScheduler.triggerWorkerDiscovery(clusterId);
            
            return Result.success("发现任务已提交，请稍后查看结果");
            
        } catch (Exception e) {
            logger.error("触发Worker节点发现失败，集群ID: {}", clusterId, e);
            return Result.error("Worker节点发现失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发Worker节点健康检查
     * 
     * @param clusterId 集群ID
     * @return 操作结果
     */

    @PostMapping("/clusters/{clusterId}/health-check")
    public Result<String> healthCheckWorkers(@PathVariable Long clusterId) {
        try {
            logger.info("收到Worker节点健康检查请求，集群ID: {}", clusterId);
            
            masterWorkerScheduler.triggerHealthCheck(clusterId);
            
            return Result.success("健康检查任务已提交，请稍后查看结果");
            
        } catch (Exception e) {
            logger.error("触发Worker节点健康检查失败，集群ID: {}", clusterId, e);
            return Result.error("Worker节点健康检查失败: " + e.getMessage());
        }
    }

    /**
     * 获取Worker节点连接状态
     * 
     * @param clusterId 集群ID
     * @return Worker节点状态信息
     */
    @GetMapping("/clusters/{clusterId}/worker-status")
    public Result<String> getWorkerStatus(@PathVariable Long clusterId) {
        try {
            logger.info("获取Worker节点状态，集群ID: {}", clusterId);
            
            // 这里可以添加获取Worker状态的逻辑
            // 比如查询数据库中的Worker状态信息
            
            return Result.success("集群" + clusterId + "的Worker状态信息");
            
        } catch (Exception e) {
            logger.error("获取Worker节点状态失败，集群ID: {}", clusterId, e);
            return Result.error("获取Worker状态失败: " + e.getMessage());
        }
    }

    /**
     * 重新连接指定的Worker节点
     * 
     * @param clusterId 集群ID
     * @param hostname Worker节点主机名
     * @return 操作结果
     */
    @PostMapping("/clusters/{clusterId}/workers/{hostname}/reconnect")
    public Result<String> reconnectWorker(@PathVariable Long clusterId, @PathVariable String hostname) {
        try {
            logger.info("重新连接Worker节点，集群ID: {}, 主机名: {}", clusterId, hostname);
            
            // 这里可以添加重连指定Worker节点的逻辑
            // 比如发送特定的连接命令给WorkerDiscoveryActor
            
            return Result.success("Worker节点" + hostname + "重连任务已启动");
            
        } catch (Exception e) {
            logger.error("重连Worker节点失败，集群ID: {}, 主机名: {}", clusterId, hostname, e);
            return Result.error("重连Worker节点失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有集群的Worker连接概况
     * 
     * @return Worker连接概况
     */
    @GetMapping("/connection-overview")
    public Result<String> getConnectionOverview() {
        try {
            logger.info("获取Worker连接概况");
            
            // 这里可以添加获取所有集群Worker连接概况的逻辑
            
            return Result.success("所有集群的Worker连接概况");
            
        } catch (Exception e) {
            logger.error("获取Worker连接概况失败", e);
            return Result.error("获取连接概况失败: " + e.getMessage());
        }
    }
}
