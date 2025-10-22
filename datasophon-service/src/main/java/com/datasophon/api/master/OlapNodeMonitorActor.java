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

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.common.command.OlapNodeCheckCommand;
import com.datasophon.common.command.OlapOpsType;
import com.datasophon.common.command.OlapSqlExecCommand;
import com.datasophon.common.enums.ServiceRoleState;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.OlapUtils;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

import static com.datasophon.dao.entity.table.ClusterServiceRoleInstanceEntityTableDef.CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY;

/**
 * OLAP 节点监控 Actor
 * 定时检查 StarRocks/Doris 的 BE/FE 节点，自动将新启动的节点添加到集群
 */
public class OlapNodeMonitorActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(OlapNodeMonitorActor.class);

    // OLAP 服务角色名称
    private static final String DORIS_BE = "BE";
    private static final String DORIS_FE = "FE";
    private static final String DORIS_FE_OBSERVER = "FEObserver";
    private static final String STARROCKS_BE = "StarRocksBE";
    private static final String STARROCKS_FE = "StarRocksFE";
    private static final String STARROCKS_FE_OBSERVER = "StarRocksFEObserver";
    private static final String STARROCKS_CN = "StarRocksCN";

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(OlapNodeCheckCommand.class, this::handleOlapNodeCheck)
                .matchAny(this::unhandled)
                .build();
    }

    /**
     * 处理 OLAP 节点检查命令
     */
    private void handleOlapNodeCheck(OlapNodeCheckCommand command) {
        try {
            logger.debug("开始检查需要添加到集群的 OLAP 节点");
            
            ClusterServiceRoleInstanceMapper mapper = SpringUtil.getBean(ClusterServiceRoleInstanceMapper.class);
            
            // 查询所有运行中但未添加到集群的 OLAP 节点
            QueryWrapper wrapper = QueryWrapper.create()
                    .where(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.SERVICE_ROLE_STATE.eq(ServiceRoleState.RUNNING.getValue()))
                    .and(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.ADDED_TO_CLUSTER.eq(0))
                    .and(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.SERVICE_ROLE_NAME.in(
                            DORIS_BE, DORIS_FE, DORIS_FE_OBSERVER,
                            STARROCKS_BE, STARROCKS_FE, STARROCKS_FE_OBSERVER, STARROCKS_CN
                    ));
            
            List<ClusterServiceRoleInstanceEntity> needAddNodes = mapper.selectListByQuery(wrapper);
            
            if (needAddNodes.isEmpty()) {
                logger.debug("没有发现需要添加到集群的 OLAP 节点");
                return;
            }
            
            logger.info("发现 {} 个需要添加到集群的 OLAP 节点", needAddNodes.size());
            
            // 处理每个节点
            for (ClusterServiceRoleInstanceEntity node : needAddNodes) {
                try {
                    addNodeToCluster(node, mapper);
                } catch (Exception e) {
                    logger.error("添加节点 {} 到集群失败", node.getHostname(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("检查 OLAP 节点时发生异常", e);
        }
    }

    /**
     * 将节点添加到集群
     */
    private void addNodeToCluster(ClusterServiceRoleInstanceEntity node, ClusterServiceRoleInstanceMapper mapper) {
        String roleName = node.getServiceRoleName();
        String hostname = node.getHostname();
        
        logger.info("准备将节点 {} ({}) 添加到集群", hostname, roleName);
        
        // 获取 FE Master 地址
        String feMasterHost = getFeMasterHost(node.getClusterId(), node.getServiceId(), mapper);
        if (feMasterHost == null) {
            logger.warn("无法获取 FE Master 地址，跳过节点 {}", hostname);
            return;
        }
        
        // 确定操作类型
        OlapOpsType opsType = determineOpsType(roleName);
        if (opsType == null) {
            logger.warn("未知的角色类型: {}", roleName);
            return;
        }
        
        // 执行添加操作
        ExecResult result = executeAddNode(feMasterHost, hostname, opsType);
        
        if (result != null && result.getExecResult()) {
            // 更新数据库标记
            node.setAddedToCluster(true);
            node.setAddToClusterTime(LocalDateTime.now());
            mapper.update(node);
            logger.info("成功将节点 {} ({}) 添加到集群", hostname, roleName);
        } else {
            logger.warn("添加节点 {} ({}) 到集群失败", hostname, roleName);
        }
    }

    /**
     * 获取 FE Master 主机地址
     */
    private String getFeMasterHost(Long clusterId, Long serviceId, ClusterServiceRoleInstanceMapper mapper) {
        // 查找同服务的 FE/StarRocksFE 角色作为 Master
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.CLUSTER_ID.eq(clusterId))
                .and(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.SERVICE_ID.eq(serviceId))
                .and(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.SERVICE_ROLE_NAME.in(DORIS_FE, STARROCKS_FE))
                .and(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.SERVICE_ROLE_STATE.eq(ServiceRoleState.RUNNING.getValue()))
                .limit(1);
        
        ClusterServiceRoleInstanceEntity feMaster = mapper.selectOneByQuery(wrapper);
        return feMaster != null ? feMaster.getHostname() : null;
    }

    /**
     * 确定操作类型
     */
    private OlapOpsType determineOpsType(String roleName) {
        return switch (roleName) {
            case DORIS_BE, STARROCKS_BE -> OlapOpsType.ADD_BE;
            case DORIS_FE, STARROCKS_FE -> OlapOpsType.ADD_FE_FOLLOWER;
            case DORIS_FE_OBSERVER, STARROCKS_FE_OBSERVER -> OlapOpsType.ADD_FE_OBSERVER;
            case STARROCKS_CN -> OlapOpsType.ADD_CN;
            default -> null;
        };
    }

    /**
     * 执行添加节点操作
     */
    private ExecResult executeAddNode(String feMasterHost, String hostname, OlapOpsType opsType) {
        try {
            ExecResult result = switch (opsType) {
                case ADD_BE -> OlapUtils.addBackend(feMasterHost, hostname);
                case ADD_FE_FOLLOWER -> OlapUtils.addFollower(feMasterHost, hostname);
                case ADD_FE_OBSERVER -> OlapUtils.addObserver(feMasterHost, hostname);
                case ADD_CN -> OlapUtils.addCn(feMasterHost, hostname);
            };
            
            // 如果第一次失败，使用 SQL 客户端重试
            if (result != null && !result.getExecResult()) {
                logger.info("使用 SQL 客户端重试添加节点: {}", hostname);
                result = switch (opsType) {
                    case ADD_BE -> OlapUtils.addBackendBySqlClient(feMasterHost, hostname);
                    case ADD_FE_FOLLOWER -> OlapUtils.addFollowerBySqlClient(feMasterHost, hostname);
                    case ADD_FE_OBSERVER -> OlapUtils.addObserverBySqlClient(feMasterHost, hostname);
                    case ADD_CN -> OlapUtils.addCnBySqlClient(feMasterHost, hostname);
                };
            }
            
            return result;
        } catch (Exception e) {
            logger.error("执行添加节点操作失败", e);
            return null;
        }
    }
}

