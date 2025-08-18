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

import cn.hutool.core.util.StrUtil;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.common.command.HostCheckCommand;
import com.datasophon.common.command.PingCommand;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PromInfoUtils;

import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.HostState;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 节点状态监测
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
public class HostCheckActor extends AbstractActor {

  private static final Logger logger = LoggerFactory.getLogger(HostCheckActor.class);

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
        .match(HostCheckCommand.class, this::handleHostCheck)
        .matchAny(this::unhandled)
        .build();
  }

  private void handleHostCheck(HostCheckCommand hostCheckCommand) {
    try {
      // logger.info("start to check host info");
      ClusterHostService clusterHostService = SpringUtil.getBean(ClusterHostService.class);
      ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
          .getBean(ClusterServiceRoleInstanceService.class);
      ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);

      // Host or cluster
      final HostInfo hostInfo = hostCheckCommand.getHostInfo();

      // 获取当前安装并且正在运行的集群
      List<ClusterInfoDTO> clusterList = clusterInfoService.runningClusterList();

      for (ClusterInfoDTO clusterInfoDto : clusterList) {
        // 获取集群上安装的 Prometheus 服务, 从 Prometheus 获取CPU、磁盘使用量等
        Long clusterId = clusterInfoDto.id();
        ClusterType depType = clusterInfoDto.depType();
        String prometheusPort = "9090";
        if (depType == ClusterType.KUBERNETES) {
          prometheusPort = "30909";
        }

        ClusterServiceRoleInstanceDTO prometheusInstance = roleInstanceService.getOneServiceRole("Prometheus", "",
            clusterId);
        if (Objects.nonNull(prometheusInstance)) {
          // 集群正常安装了 Prometheus
          List<ClusterHostEntity> list = clusterHostService.getHostListByClusterId(clusterId);

          String promUrl = "http://" + prometheusInstance.hostname() + ":" + prometheusPort + "/api/v1/query";
          for (ClusterHostEntity clusterHostEntity : list) {
            if (hostInfo != null && !StrUtil.equals(clusterHostEntity.getHostname(), hostInfo.getIp())) {
              // 指定了节点，直接只处理这一个节点的
              continue;
            }
            try {
              String hostname = clusterHostEntity.getHostname();
              // 查询内存总量
              String totalMemPromQl = "node_memory_MemTotal_bytes{job=~\"node\",instance=\"" + hostname
                  + ":9100\"}/1024/1024/1024";
              String totalMemStr = PromInfoUtils.getSinglePrometheusMetric(promUrl, totalMemPromQl);
              if (StringUtils.isNotBlank(totalMemStr)) {
                int totalMem = Double.valueOf(totalMemStr).intValue();
                clusterHostEntity.setTotalMem(totalMem);
              }

              // 总磁盘容量
              String totalDistPromQl = "sum(node_filesystem_size_bytes{instance=\"" + hostname
                  + ":9100\",fstype=~\"ext4|xfs\",mountpoint !~\".*pod.*\"})/1024/1024/1024";
              String totalDiskStr = PromInfoUtils.getSinglePrometheusMetric(promUrl, totalDistPromQl);
              if (StringUtils.isNotBlank(totalDiskStr)) {
                int totalDisk = Double.valueOf(totalDiskStr).intValue();
                clusterHostEntity.setTotalDisk(totalDisk);
              }

              // 查询cpu负载
              String cpuLoadPromQl = "node_load5{job=~\"node\",instance=\"" + hostname + ":9100\"}";
              String cpuLoad = PromInfoUtils.getSinglePrometheusMetric(promUrl, cpuLoadPromQl);
              if (StringUtils.isNotBlank(cpuLoad)) {
                clusterHostEntity.setAverageLoad(cpuLoad);
              }

              clusterHostEntity.setHostState(HostState.RUNNING);
            } catch (Exception e) {

              logger.warn("check cluster state error, cause: {}", e.getMessage());
            }
          }
          if (!list.isEmpty()) {
            // 批量更新主机状态信息（包含内存、磁盘、CPU等监控数据）
            clusterHostService.updateBatchHostStatus(list);
          }
        } else {
          // 没有 Prometheus？直接获取节点，通过 rpc 检测是否启动
          List<ClusterHostEntity> hosts = clusterHostService.getHostListByClusterId(clusterId);
          List<ClusterHostEntity> checkedHosts = new ArrayList<>(hosts.size());
          for (ClusterHostEntity host : hosts) {
            if (hostInfo != null && !StrUtil.equals(host.getHostname(), hostInfo.getIp())) {
              // 指定了节点，直接只处理这一个节点的
              continue;
            }
            // copy 一个新的，只更新状态
            ClusterHostEntity checkedHost = new ClusterHostEntity();
            checkedHost.setId(host.getId());
            checkedHost.setCheckTime(LocalDateTime.now());
            try {
              // rpc 检测
              ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
              if (clusterInfo == null) {
                logger.warn("Cluster with id {} not found", clusterId);
                continue;
              }
              if (depType == ClusterType.KUBERNETES) {
                try {
                  // 使用Java原生的isReachable方法替代系统ping命令
                  java.net.InetAddress address = java.net.InetAddress.getByName(host.getHostname());
                  boolean reachable = address.isReachable(3000); // 3000毫秒超时

                  if (reachable) {
                    logger.info("检查主机连通性: {} 成功 (Kubernetes模式)", host.getHostname());
                    checkedHost.setHostState(HostState.RUNNING);
                    checkedHost.setManagementStatus(ManagementStatus.MANAGED);
                  } else {
                    logger.warn("检查主机连通性: {} 失败 (Kubernetes模式)", host.getHostname());
                    checkedHost.setHostState(HostState.OFFLINE);
                  }
                } catch (Exception e) {
                  Objects.requireNonNull(logger).warn("Kubernetes模式下检查主机: {} 失败, 原因: {}", host.getHostname(),
                      e.getMessage());
                  checkedHost.setHostState(HostState.OFFLINE);
                }
                continue; // 跳过下面的pingActor检测
              }
              final ActorRef pingActor = ActorUtils.getRemoteActor(host.getHostname(), "pingActor");
              PingCommand pingCommand = new PingCommand();
              pingCommand.setMessage("ping");
              Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
              Future<Object> execFuture = Patterns.ask(pingActor, pingCommand, timeout);
              ExecResult execResult = (ExecResult) Await.result(execFuture, timeout.duration());
              if (execResult.getExecResult()) {
                logger.info("ping host: {} success", host.getHostname());
              } else {
                logger.warn("ping host: {} fail, reason: {}", host.getHostname(), execResult.getExecOut());
                throw new IllegalStateException("ping host: " + host.getHostname() + " failed.");
              }
              checkedHost.setHostState(HostState.RUNNING);
              checkedHost.setManagementStatus(ManagementStatus.MANAGED);
            } catch (Exception e) {
              Objects.requireNonNull(logger).warn("host: {} rpc error, cause: {}", host.getHostname(), e.getMessage());
              checkedHost.setHostState(HostState.OFFLINE);
            }
            checkedHosts.add(checkedHost);
          }
          if (!checkedHosts.isEmpty()) {
            // 批量更新主机状态信息
            clusterHostService.updateBatchHostStatus(checkedHosts);
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error handling HostCheckCommand", e);
    }
  }
}
