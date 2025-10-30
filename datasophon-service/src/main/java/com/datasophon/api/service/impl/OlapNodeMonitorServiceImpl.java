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

package com.datasophon.api.service.impl;

import com.datasophon.api.service.OlapNodeMonitorService;
import com.datasophon.api.service.OlapSqlExecutionService;
import com.datasophon.common.command.OlapNodeCheckCommand;
import com.datasophon.common.command.OlapOpsType;
import com.datasophon.common.command.OlapSqlExecCommand;
import com.datasophon.common.enums.ServiceRoleState;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.datasophon.dao.entity.table.ClusterServiceRoleInstanceEntityTableDef.CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY;

/**
 * OLAP节点监控服务实现
 * 替代OlapNodeMonitorActor，定时检查StarRocks/Doris的BE/FE节点，自动将新启动的节点添加到集群
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-10-30
 */
@Service
public class OlapNodeMonitorServiceImpl implements OlapNodeMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(OlapNodeMonitorServiceImpl.class);

    // OLAP 服务角色名称
    private static final String DORIS_BE = "BE";
    private static final String DORIS_FE = "FE";
    private static final String DORIS_FE_OBSERVER = "FEObserver";
    private static final String STARROCKS_BE = "StarRocksBE";
    private static final String STARROCKS_FE = "StarRocksFE";
    private static final String STARROCKS_FE_OBSERVER = "StarRocksFEObserver";
    private static final String STARROCKS_CN = "StarRocksCN";

    @Autowired
    private ClusterServiceRoleInstanceMapper roleInstanceMapper;

    @Autowired
    private OlapSqlExecutionService olapSqlExecutionService;

    @Override
    @Async("taskExecutor")
    public void checkAndAddOlapNodes(OlapNodeCheckCommand command) {
        try {
            logger.debug("开始检查需要添加到集群的 OLAP 节点");

            // 查询所有运行中但未添加到集群的 OLAP 节点
            QueryWrapper wrapper = QueryWrapper.create()
                    .where(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.SERVICE_ROLE_STATE.eq(ServiceRoleState.RUNNING.getValue()))
                    .and(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.ADDED_TO_CLUSTER.eq(0))
                    .and(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.SERVICE_ROLE_NAME.in(
                            DORIS_BE, DORIS_FE, DORIS_FE_OBSERVER,
                            STARROCKS_BE, STARROCKS_FE, STARROCKS_FE_OBSERVER, STARROCKS_CN
                    ));

            List<ClusterServiceRoleInstanceEntity> needAddNodes = roleInstanceMapper.selectListByQuery(wrapper);

            if (needAddNodes.isEmpty()) {
                logger.debug("没有找到需要添加到集群的 OLAP 节点");
                return;
            }

            logger.info("发现 {} 个需要添加到集群的 OLAP 节点", needAddNodes.size());

            // 为每个节点创建添加命令
            for (ClusterServiceRoleInstanceEntity node : needAddNodes) {
                try {
                    processOlapNode(node);
                } catch (Exception e) {
                    logger.error("处理OLAP节点 {} 时发生错误", node.getHostname(), e);
                }
            }

        } catch (Exception e) {
            logger.error("检查OLAP节点时发生错误", e);
        }
    }

    /**
     * 处理单个OLAP节点
     */
    private void processOlapNode(ClusterServiceRoleInstanceEntity node) {
        String roleName = node.getServiceRoleName();
        String hostname = node.getHostname();

        logger.info("准备将 {} 节点 {} 添加到集群", roleName, hostname);

        // 查询FE Master节点作为操作入口
        String feMaster = findFeMaster(node.getClusterId(), node.getServiceName());
        if (feMaster == null) {
            logger.warn("未找到 FE Master 节点，无法添加 {} 节点", hostname);
            return;
        }

        // 构建SQL执行命令
        OlapSqlExecCommand sqlCommand = new OlapSqlExecCommand();
        sqlCommand.setFeMaster(feMaster);
        sqlCommand.setHostName(hostname);
        sqlCommand.setOpsType(determineOpsType(roleName));

        // 异步执行SQL命令
        olapSqlExecutionService.executeOlapSqlCommand(sqlCommand);

        // 更新节点状态为已添加到集群
        node.setAddedToCluster(true);
        node.setUpdateTime(LocalDateTime.now());
        roleInstanceMapper.update(node);

        logger.info("成功标记 {} 节点 {} 为已添加到集群", roleName, hostname);
    }

    /**
     * 查找FE Master节点
     */
    private String findFeMaster(Long clusterId, String serviceName) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.CLUSTER_ID.eq(clusterId))
                .and(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.SERVICE_NAME.eq(serviceName))
                .and(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.SERVICE_ROLE_STATE.eq(ServiceRoleState.RUNNING.getValue()))
                .and(CLUSTER_SERVICE_ROLE_INSTANCE_ENTITY.SERVICE_ROLE_NAME.in(
                        DORIS_FE, STARROCKS_FE
                ))
                .limit(1);

        ClusterServiceRoleInstanceEntity feMaster = roleInstanceMapper.selectOneByQuery(wrapper);
        return feMaster != null ? feMaster.getHostname() : null;
    }

    /**
     * 根据角色名称确定操作类型
     */
    private OlapOpsType determineOpsType(String roleName) {
        return switch (roleName) {
            case DORIS_BE, STARROCKS_BE -> OlapOpsType.ADD_BE;
            case DORIS_FE, STARROCKS_FE -> OlapOpsType.ADD_FE_FOLLOWER;
            case DORIS_FE_OBSERVER, STARROCKS_FE_OBSERVER -> OlapOpsType.ADD_FE_OBSERVER;
            case STARROCKS_CN -> OlapOpsType.ADD_CN;
            default -> {
                logger.warn("未知的OLAP角色类型: {}", roleName);
                yield OlapOpsType.ADD_BE; // 默认值
            }
        };
    }
}

