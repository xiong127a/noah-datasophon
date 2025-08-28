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

package com.datasophon.api.scheduler;

import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.common.command.PingCommand;
import com.datasophon.common.utils.ExecResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * db-scheduler Worker调度服务
 * 
 * 这个服务使用现代化的db-scheduler调度框架来管理Worker节点：
 * 
 * 🎯 核心功能：
 * 1. Worker节点自动发现和注册
 * 2. Worker节点健康状态监控
 * 3. 关键服务实时监控
 * 4. 系统清理和维护
 * 
 * 🚀 技术优势：
 * - 比Quartz更现代化的API设计
 * - 内置集群支持和故障恢复
 * - 零外部服务依赖（仅MySQL）
 * - 支持手动和自动任务调度
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-20
 */
@Service
@Slf4j
public class WorkerScheduleService {

    @Autowired
    private ClusterInfoService clusterInfoService;
    
    @Autowired
    private ClusterHostService clusterHostService;
    
    @Autowired
    private ActorSystem actorSystem;

    // ================== 定时任务业务方法 ==================

    /**
     * 发现所有集群的Worker节点
     * 由db-scheduler定时调用（每5分钟）
     */
    public void discoverAllWorkers() {
        log.info("开始db-scheduler Worker节点发现任务");
        
        try {
            List<ClusterInfoEntity> clusters = clusterInfoService.list();
            
            if (clusters.isEmpty()) {
                log.info("没有找到集群，跳过Worker发现");
                return;
            }
            
            // 现代化并行处理 - 利用CompletableFuture提升性能
            List<CompletableFuture<Void>> futures = clusters.stream()
                .map(cluster -> CompletableFuture.runAsync(() -> {
                    try {
                        discoverClusterWorkers(cluster.getId());
                    } catch (Exception e) {
                        log.error("集群{}Worker发现失败: {}", cluster.getClusterName(), e.getMessage());
                        // 单个集群失败不影响其他集群
                    }
                }))
                .toList();
            
            // 等待所有集群处理完成，设置合理超时
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(8, TimeUnit.MINUTES)
                .join();
                
            log.info("db-scheduler Worker节点发现任务完成，处理{}个集群", clusters.size());
            
        } catch (Exception e) {
            log.error("Worker节点发现任务执行失败", e);
            throw new RuntimeException("Worker发现失败", e);
        }
    }

    /**
     * 执行Worker节点健康检查
     * 由db-scheduler定时调用（每1分钟）
     */
    public void performHealthCheck() {
        log.debug("开始db-scheduler Worker健康检查任务");
        
        try {
            List<ClusterInfoEntity> clusters = clusterInfoService.list();
            
            // 并行健康检查，提升效率
            clusters.parallelStream().forEach(cluster -> {
                try {
                    performClusterHealthCheck(cluster.getId());
                } catch (Exception e) {
                    log.warn("集群{}健康检查失败: {}", cluster.getClusterName(), e.getMessage());
                    // 健康检查失败不中断其他集群
                }
            });
            
            log.debug("db-scheduler Worker健康检查任务完成");
            
        } catch (Exception e) {
            log.error("Health check任务执行失败", e);
            // 健康检查失败不抛异常，避免影响调度
        }
    }

    /**
     * 监控关键服务
     * 由db-scheduler定时调用（每30秒）
     */
    public void monitorCriticalServices() {
        log.debug("开始db-scheduler关键服务监控");
        
        try {
            List<ClusterInfoEntity> clusters = clusterInfoService.list();
            
            for (ClusterInfoEntity cluster : clusters) {
                List<ClusterHostEntity> managedHosts = clusterHostService
                    .getHostListByClusterIdAndManaged(cluster.getId())
                    .stream()
                    .filter(host -> ManagementStatus.MANAGED.equals(host.getManagementStatus()))
                    .limit(5) // 限制监控数量，避免过度负载
                    .toList();
                    
                for (ClusterHostEntity host : managedHosts) {
                    try {
                        boolean isHealthy = quickHealthCheck(host.getHostname());
                        if (!isHealthy) {
                            log.warn("关键Worker节点{}状态异常", host.getHostname());
                            // 这里可以触发告警或自动修复
                            scheduleWorkerReconnect(host.getHostname());
                        }
                    } catch (Exception e) {
                        log.debug("检查关键Worker节点{}失败: {}", host.getHostname(), e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("关键服务监控任务失败", e);
        }
    }

    /**
     * 执行系统清理
     * 由db-scheduler定时调用（每天凌晨2点）
     */
    public void performSystemCleanup() {
        log.info("开始db-scheduler系统清理任务");
        
        try {
            // 清理过期的任务执行记录
            cleanupOldTaskRecords();
            
            // 清理过期的Worker连接记录
            cleanupStaleWorkerConnections();
            
            // 清理临时文件和缓存
            cleanupTempFilesAndCache();
            
            log.info("系统清理任务完成");
            
        } catch (Exception e) {
            log.error("系统清理任务失败", e);
            throw new RuntimeException("系统清理失败", e);
        }
    }

    // ================== 手动触发方法 ==================

    /**
     * 手动触发特定集群Worker发现
     * 立即返回，任务异步执行
     */
    public void triggerWorkerDiscovery(Long clusterId) {
        log.info("手动触发集群{}的Worker发现", clusterId);
        
        try {
            // 直接异步执行，不使用db-scheduler的一次性任务
            CompletableFuture.runAsync(() -> {
                log.info("执行手动Worker发现任务，集群ID: {}", clusterId);
                discoverClusterWorkers(clusterId);
                log.info("手动Worker发现任务完成，集群ID: {}", clusterId);
            });
            
            log.info("手动Worker发现任务已启动，集群ID: {}", clusterId);
            
        } catch (Exception e) {
            log.error("启动手动Worker发现任务失败", e);
            throw new RuntimeException("启动失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发健康检查
     */
    public void triggerHealthCheck(Long clusterId) {
        log.info("手动触发集群{}的健康检查", clusterId);
        
        try {
            // 直接异步执行健康检查
            CompletableFuture.runAsync(() -> {
                log.info("执行手动健康检查，集群ID: {}", clusterId);
                performClusterHealthCheck(clusterId);
                log.info("手动健康检查完成，集群ID: {}", clusterId);
            });
            
            log.info("手动健康检查任务已启动，集群ID: {}", clusterId);
            
        } catch (Exception e) {
            log.error("启动手动健康检查任务失败", e);
            throw new RuntimeException("启动失败: " + e.getMessage());
        }
    }

    // ================== 私有业务方法 ==================

    /**
     * 发现单个集群的Worker节点
     */
    public void discoverClusterWorkers(Long clusterId) {
        ClusterInfoEntity cluster = clusterInfoService.getById(clusterId);
        if (cluster == null) {
            log.warn("集群{}不存在，跳过Worker发现", clusterId);
            return;
        }
        
        log.info("发现集群{}的Worker节点", cluster.getClusterName());
        
        List<ClusterHostEntity> hosts = clusterHostService.getHostListByClusterIdAndManaged(clusterId);
        
        if (hosts.isEmpty()) {
            log.info("集群{}没有配置主机节点", cluster.getClusterName());
            return;
        }
        
        // 并行检查每个Worker
        List<CompletableFuture<Void>> checkFutures = hosts.stream()
            .map(host -> CompletableFuture.runAsync(() -> {
                try {
                    boolean isOnline = checkWorkerConnection(host.getHostname());
                    updateWorkerStatus(host, isOnline);
                    
                    if (isOnline) {
                        log.debug("Worker节点{}已上线", host.getHostname());
                        // 可以触发系统信息收集
                        scheduleSystemInfoCollection(host.getHostname());
                    } else {
                        log.debug("Worker节点{}离线", host.getHostname());
                    }
                    
                } catch (Exception e) {
                    log.warn("检查Worker节点{}失败: {}", host.getHostname(), e.getMessage());
                    updateWorkerStatus(host, false);
                }
            }))
            .toList();
        
        // 等待所有检查完成
        try {
            CompletableFuture.allOf(checkFutures.toArray(new CompletableFuture[0]))
                .orTimeout(2, TimeUnit.MINUTES)
                .join();
        } catch (Exception e) {
            log.warn("等待Worker检查完成时出现超时: {}", e.getMessage());
        }
        
        log.info("集群{}的Worker节点发现完成", cluster.getClusterName());
    }

    /**
     * 执行集群健康检查
     */
    private void performClusterHealthCheck(Long clusterId) {
        List<ClusterHostEntity> hosts = clusterHostService.getHostListByClusterIdAndManaged(clusterId);
        
        for (ClusterHostEntity host : hosts) {
            try {
                boolean isHealthy = quickHealthCheck(host.getHostname());
                
                // 仅在状态变化时更新数据库，减少DB负载
                ManagementStatus currentStatus = host.getManagementStatus();
                ManagementStatus newStatus = isHealthy ? ManagementStatus.MANAGED : ManagementStatus.UNMANAGED;
                
                if (currentStatus != newStatus) {
                    updateWorkerStatus(host, isHealthy);
                    log.info("Worker节点{}状态变更: {} -> {}", 
                        host.getHostname(), currentStatus, newStatus);
                }
                
            } catch (Exception e) {
                log.debug("健康检查Worker节点{}失败: {}", host.getHostname(), e.getMessage());
            }
        }
    }

    // ================== 工具方法 ==================

    /**
     * 检查Worker连接状态
     */
    private boolean checkWorkerConnection(String hostname) {
        try {
            ActorSelection workerActor = actorSystem.actorSelection(
                "akka://datasophon@" + hostname + ":2552/user/worker");
            
            PingCommand pingCommand = new PingCommand();
            pingCommand.setMessage("db_scheduler_discovery");
            
            Timeout timeout = new Timeout(Duration.create(30, TimeUnit.SECONDS));
            Future<Object> future = Patterns.ask(workerActor, pingCommand, timeout);
            
            ExecResult result = (ExecResult) Await.result(future, timeout.duration());
            return result.getExecResult();
            
        } catch (Exception e) {
            log.debug("Worker节点{}连接检查失败: {}", hostname, e.getMessage());
            return false;
        }
    }

    /**
     * 快速健康检查
     */
    private boolean quickHealthCheck(String hostname) {
        try {
            ActorSelection pingActor = actorSystem.actorSelection(
                "akka://datasophon@" + hostname + ":2552/user/worker/pingActor");
            
            PingCommand pingCommand = new PingCommand();
            pingCommand.setMessage("quick_health");
            
            Timeout timeout = new Timeout(Duration.create(10, TimeUnit.SECONDS));
            Future<Object> future = Patterns.ask(pingActor, pingCommand, timeout);
            
            ExecResult result = (ExecResult) Await.result(future, timeout.duration());
            return result.getExecResult();
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 更新Worker状态
     */
    private void updateWorkerStatus(ClusterHostEntity host, boolean isOnline) {
        try {
            host.setManagementStatus(isOnline ? ManagementStatus.MANAGED : ManagementStatus.UNMANAGED);
            clusterHostService.saveHost(host);
            
            log.debug("更新Worker节点{}状态: {}", host.getHostname(), 
                isOnline ? "在线" : "离线");
                
        } catch (Exception e) {
            log.error("更新Worker节点{}状态失败", host.getHostname(), e);
        }
    }

    /**
     * 调度系统信息收集
     */
    private void scheduleSystemInfoCollection(String hostname) {
        try {
            // 使用线程池延迟执行
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(30000); // 30秒后执行
                    collectWorkerSystemInfo(hostname);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("系统信息收集任务被中断: {}", hostname);
                } catch (Exception e) {
                    log.debug("系统信息收集失败: {}", hostname, e);
                }
            });
        } catch (Exception e) {
            log.debug("启动系统信息收集任务失败: {}", hostname, e);
        }
    }

    /**
     * 调度Worker重连
     */
    private void scheduleWorkerReconnect(String hostname) {
        try {
            // 使用线程池延迟执行重连
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(60000); // 1分钟后重试
                    log.info("尝试重连Worker节点: {}", hostname);
                    checkWorkerConnection(hostname);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Worker重连任务被中断: {}", hostname);
                } catch (Exception e) {
                    log.warn("Worker重连失败: {}", hostname, e);
                }
            });
        } catch (Exception e) {
            log.warn("启动Worker重连任务失败: {}", hostname, e);
        }
    }

    /**
     * 收集Worker系统信息
     */
    private void collectWorkerSystemInfo(String hostname) {
        try {
            ActorSelection systemInfoActor = actorSystem.actorSelection(
                "akka://datasophon@" + hostname + ":2552/user/worker/systemInfoActor");
            
            // 系统信息收集命令
            var collectCommand = new Object();
            
            Timeout timeout = new Timeout(Duration.create(60, TimeUnit.SECONDS));
            Future<Object> future = Patterns.ask(systemInfoActor, collectCommand, timeout);
            
            Await.result(future, timeout.duration());
            log.debug("成功收集Worker节点{}的系统信息", hostname);
            
        } catch (Exception e) {
            log.debug("收集Worker节点{}系统信息失败: {}", hostname, e.getMessage());
        }
    }

    // 清理方法

    private void cleanupOldTaskRecords() {
        log.debug("清理过期的任务执行记录");
        // 实现清理逻辑
    }

    private void cleanupStaleWorkerConnections() {
        log.debug("清理过期的Worker连接记录");
        // 实现清理逻辑
    }

    private void cleanupTempFilesAndCache() {
        log.debug("清理临时文件和缓存");
        // 实现清理逻辑
    }
}
