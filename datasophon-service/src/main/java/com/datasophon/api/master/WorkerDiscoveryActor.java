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

package com.datasophon.api.master;

import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.service.ServiceInstallationService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.dto.ClusterInfoDTO;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.common.command.PingCommand;
import com.datasophon.common.model.StartWorkerMessage;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.common.enums.ManagementStatus;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Worker发现和管理Actor
 * 
 * 职责：
 * 1. 定期发现Worker节点
 * 2. 主动连接Worker并进行健康检查
 * 3. 管理Worker节点状态
 * 4. 处理Worker节点的上线/下线
 */
public class WorkerDiscoveryActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(WorkerDiscoveryActor.class);
    
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(DiscoverWorkerCommand.class, this::handleDiscoverWorker)
                .match(HealthCheckCommand.class, this::handleHealthCheck)
                .match(WorkerStateUpdateCommand.class, this::handleWorkerStateUpdate)
                .matchAny(this::unhandled)
                .build();
    }

    /**
     * 处理Worker发现命令
     * 从数据库获取集群主机列表，主动连接Worker进行健康检查
     */
    private void handleDiscoverWorker(DiscoverWorkerCommand command) {
        logger.info("开始发现集群{}的Worker节点", command.getClusterId());
        
        ClusterHostService clusterHostService = SpringUtil.getBean(ClusterHostService.class);
        List<ClusterHostEntity> hosts = clusterHostService.getHostListByClusterIdAndManaged(command.getClusterId());
        
        for (ClusterHostEntity host : hosts) {
            // 对每个主机进行异步健康检查
            CompletableFuture.runAsync(() -> performHealthCheck(host, command.getClusterId()));
        }
    }

    /**
     * 执行Worker健康检查
     */
    private void performHealthCheck(ClusterHostEntity host, Long clusterId) {
        try {
            String hostname = host.getHostname();
            logger.info("对Worker节点{}执行健康检查", hostname);
            
            // 构建ActorSelection连接到Worker
            ActorSelection workerActor = getContext().getSystem().actorSelection(
                    "akka://datasophon@" + hostname + ":2552/user/worker");
            
            // 发送Ping命令检查Worker状态
            PingCommand pingCommand = new PingCommand();
            pingCommand.setMessage("health_check");
            
            Timeout timeout = new Timeout(Duration.create(30, TimeUnit.SECONDS));
            Future<Object> pingFuture = Patterns.ask(workerActor, pingCommand, timeout);
            
            pingFuture.onComplete(result -> {
                if (result.isSuccess()) {
                    ExecResult execResult = (ExecResult) result.get();
                    if (execResult.getExecResult()) {
                        logger.info("Worker节点{}健康检查成功", hostname);
                        // 更新Worker状态为在线
                        updateWorkerStatus(host, true, clusterId);
                        // 收集Worker系统信息
                        collectWorkerInfo(hostname, clusterId);
                    } else {
                        logger.warn("Worker节点{}健康检查失败: {}", hostname, execResult.getExecOut());
                        updateWorkerStatus(host, false, clusterId);
                    }
                } else {
                    logger.error("连接Worker节点{}失败", hostname);
                    updateWorkerStatus(host, false, clusterId);
                }
                return null;
            }, getContext().getDispatcher());
            
        } catch (Exception e) {
            logger.error("对Worker节点{}执行健康检查时发生异常", host.getHostname(), e);
            updateWorkerStatus(host, false, clusterId);
        }
    }

    /**
     * 收集Worker系统信息
     */
    private void collectWorkerInfo(String hostname, Long clusterId) {
        try {
            // 向Worker请求系统信息
            ActorSelection workerActor = getContext().getSystem().actorSelection(
                    "akka://datasophon@" + hostname + ":2552/user/worker/executeCmdActor");
            
            // 构建系统信息收集命令
            CollectSystemInfoCommand collectCommand = new CollectSystemInfoCommand();
            collectCommand.setClusterId(clusterId);
            collectCommand.setHostname(hostname);
            
            Timeout timeout = new Timeout(Duration.create(60, TimeUnit.SECONDS));
            Future<Object> infoFuture = Patterns.ask(workerActor, collectCommand, timeout);
            
            infoFuture.onComplete(result -> {
                if (result.isSuccess()) {
                    SystemInfoResult systemInfo = (SystemInfoResult) result.get();
                    logger.info("收集到Worker节点{}的系统信息: {}", hostname, systemInfo);
                    
                    // 构造StartWorkerMessage用于更新数据库
                    var workerMessage = new StartWorkerMessage();
                    workerMessage.setHostname(hostname);
                    workerMessage.setIp(systemInfo.getIpAddress());
                    workerMessage.setCpuArchitecture(systemInfo.getCpuArchitecture());
                    workerMessage.setClusterId(clusterId);
                    
                    // 直接调用Service保存主机安装信息
                    try {
                        var serviceInstallationService = SpringUtil.getBean(ServiceInstallationService.class);
                        var clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
                        var clusterHostService = SpringUtil.getBean(ClusterHostService.class);
                        var clusterHost = clusterHostService.getClusterHostByHostname(hostname);
                        if (clusterHost != null && clusterHost.getClusterId() != null) {
                            var cluster = clusterInfoService.getClusterById(clusterHost.getClusterId());
                            if (cluster != null && cluster.clusterCode() != null) {
                                serviceInstallationService.saveHostInstallInfo(workerMessage, cluster.clusterCode());
                                logger.info("保存Worker节点{}的安装信息成功", hostname);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("保存Worker节点{}安装信息失败", hostname, e);
                    }
                }
                return null;
            }, getContext().getDispatcher());
            
        } catch (Exception e) {
            logger.error("收集Worker节点{}系统信息时发生异常", hostname, e);
        }
    }

    /**
     * 更新Worker状态
     */
    private void updateWorkerStatus(ClusterHostEntity host, boolean isOnline, Long clusterId) {
        // 更新数据库中的主机状态
        ClusterHostService clusterHostService = SpringUtil.getBean(ClusterHostService.class);
        host.setManagementStatus(isOnline ? ManagementStatus.MANAGED : ManagementStatus.UNMANAGED);
        clusterHostService.saveHost(host);
        
        logger.info("更新Worker节点{}状态: {}", host.getHostname(), isOnline ? "在线" : "离线");
    }

    /**
     * 处理健康检查命令
     */
    private void handleHealthCheck(HealthCheckCommand command) {
        logger.info("执行定时健康检查，集群ID: {}", command.getClusterId());
        
        DiscoverWorkerCommand discoverCommand = new DiscoverWorkerCommand();
        discoverCommand.setClusterId(command.getClusterId());
        getSelf().tell(discoverCommand, getSelf());
    }

    /**
     * 处理Worker状态更新命令
     */
    private void handleWorkerStateUpdate(WorkerStateUpdateCommand command) {
        logger.info("更新Worker节点{}状态", command.getHostname());
        // 处理Worker状态变化逻辑
    }

    // 内部命令类
    public static class DiscoverWorkerCommand {
        private Long clusterId;
        
        public Long getClusterId() { return clusterId; }
        public void setClusterId(Long clusterId) { this.clusterId = clusterId; }
    }

    public static class HealthCheckCommand {
        private Long clusterId;
        
        public Long getClusterId() { return clusterId; }
        public void setClusterId(Long clusterId) { this.clusterId = clusterId; }
    }

    public static class WorkerStateUpdateCommand {
        private String hostname;
        private boolean online;
        
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        public boolean isOnline() { return online; }
        public void setOnline(boolean online) { this.online = online; }
    }

    public static class CollectSystemInfoCommand {
        private Long clusterId;
        private String hostname;
        
        public Long getClusterId() { return clusterId; }
        public void setClusterId(Long clusterId) { this.clusterId = clusterId; }
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
    }

    public static class SystemInfoResult {
        private String ipAddress;
        private String cpuArchitecture;
        private String osVersion;
        private String memoryInfo;
        
        // Getters and setters
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getCpuArchitecture() { return cpuArchitecture; }
        public void setCpuArchitecture(String cpuArchitecture) { this.cpuArchitecture = cpuArchitecture; }
        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
        public String getMemoryInfo() { return memoryInfo; }
        public void setMemoryInfo(String memoryInfo) { this.memoryInfo = memoryInfo; }
        
        @Override
        public String toString() {
            return String.format("SystemInfo{ip='%s', arch='%s', os='%s', memory='%s'}", 
                    ipAddress, cpuArchitecture, osVersion, memoryInfo);
        }
    }
}
