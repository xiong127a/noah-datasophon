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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.enums.Status;
import com.datasophon.api.exceptions.BusinessException;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterNodeLabelService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.api.utils.string.validator.GeneralValidator;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterNodeLabelEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterNodeLabelMapper;
import com.datasophon.kubernetes.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.datasophon.api.utils.ProcessUtils.getDepMode;
import static com.datasophon.kubernetes.util.KubernetesUtil.runCmd;

/**
 * 集群节点标签服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Service("clusterNodeLabelService")
@Transactional
public class ClusterNodeLabelServiceImpl implements ClusterNodeLabelService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterNodeLabelServiceImpl.class);

    @Autowired
    private ClusterNodeLabelMapper clusterNodeLabelMapper;

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Override
    public ClusterNodeLabelEntity saveNodeLabel(Integer clusterId, String nodeLabel) {
        // 标签名校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        GeneralValidator generalValidator = new GeneralValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(generalValidator);
        generalValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(nodeLabel);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        if (repeatNodeLable(clusterId, nodeLabel)) {
            throw new RuntimeException(Status.REPEAT_NODE_LABEL.getMsg());
        }
        ClusterNodeLabelEntity nodeLabelEntity = new ClusterNodeLabelEntity();
        nodeLabelEntity.setClusterId(clusterId);
        nodeLabelEntity.setNodeLabel(nodeLabel);
        clusterNodeLabelMapper.insert(nodeLabelEntity);
        // refresh to yarn
        if (!refreshToYarn(clusterId, "-addToClusterNodeLabels", nodeLabel)) {
            throw new BusinessException(
                    Status.ADD_YARN_NODE_LABEL_FAILED.getMsg() + " , 请检查yarn配置页面标签配置项(yarn.node-labels.enabled)是否开启");
        }
        return nodeLabelEntity;
    }

    private boolean refreshToYarn(Integer clusterId, String type, String nodeLabel) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceService
                .getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, "ResourceManager");
        String depMode = getDepMode(clusterId);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(clusterId);
        String namespace = clusterInfo.getNamespace();
        ArrayList<String> commands = new ArrayList<>();
        commands.add(Constants.INSTALL_PATH + Constants.SLASH
                + PackageUtils.getServiceDcPackageName(clusterInfo.getClusterFrame(), "YARN") + "/bin/yarn");
        commands.add("rmadmin");
        commands.add(type);
        commands.add("\"" + nodeLabel + "\"");
        if (CollUtil.isNotEmpty(roleList)) {
            String hostname = roleList.getFirst().getHostname();
            if (depMode.equals(Constants.PVM_MODE)) {
                ActorSelection execCmdActor = ActorUtils.actorSystem
                        .actorSelection("akka.tcp://datasophon@" + hostname + ":2552/user/worker/executeCmdActor");
                ExecuteCmdCommand command = new ExecuteCmdCommand();
                Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
                command.setCommands(commands);
                Future<Object> execFuture = Patterns.ask(execCmdActor, command, timeout);
                try {
                    ExecResult execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                    if (execResult.getExecResult()) {
                        logger.info("add yarn node label success at {}", hostname);
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                logger.info("add yarn node label failed");
                return false;
            } else {
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);

                String enableYARNKerberos = globalVariables.get("${enableYARNKerberos}");
                String cmd = String.join(" ", commands);
                if (StrUtil.isNotEmpty(enableYARNKerberos) && "true".equals(enableYARNKerberos)) {
                    cmd = "kinit -kt /etc/security/keytab/spnego.service.keytab HTTP/" + hostname + "@HADOOP.COM && "
                            + cmd;
                }
                try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
                    runCmd(namespace,
                            client,
                            "yarn-resourcemanager",
                            hostname,
                            cmd);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    @Override
    public boolean deleteNodeLabel(Integer nodeLabelId) {
        ClusterNodeLabelEntity nodeLabelEntity = clusterNodeLabelMapper.selectById(nodeLabelId);
        if (nodeLabelEntity == null) {
            throw new RuntimeException("Node label not found with id: " + nodeLabelId);
        }

        if (nodeLabelInUse(nodeLabelEntity.getNodeLabel())) {
            throw new RuntimeException(Status.NODE_LABEL_IS_USING.getMsg());
        }
        clusterNodeLabelMapper.removeById(nodeLabelId);
        if (!refreshToYarn(nodeLabelEntity.getClusterId(), "-removeFromClusterNodeLabels",
                nodeLabelEntity.getNodeLabel())) {
            throw new BusinessException(Status.REMOVE_YARN_NODE_LABEL_FAILED.getMsg());
        }
        return true;
    }

    @Override
    public boolean assignNodeLabel(Integer nodeLabelId, String hostIds) {
        ClusterNodeLabelEntity nodeLabelEntity = clusterNodeLabelMapper.selectById(nodeLabelId);
        if (nodeLabelEntity == null) {
            throw new RuntimeException("Node label not found with id: " + nodeLabelId);
        }
        List<String> ids = Arrays.asList(hostIds.split(","));
        hostService.updateBatchNodeLabel(ids, nodeLabelEntity.getNodeLabel());

        List<ClusterHostDO> list = hostService.getHostListByIds(ids);
        String assignNodeLabel = list.stream().map(e -> e.getHostname() + "=" + nodeLabelEntity.getNodeLabel())
                .collect(Collectors.joining(" "));
        logger.info("assign node label {}", assignNodeLabel);
        // sync to yarn
        // refresh to yarn
        if (!refreshToYarn(nodeLabelEntity.getClusterId(), "-replaceLabelsOnNode", assignNodeLabel)) {
            throw new BusinessException(Status.ASSIGN_YARN_NODE_LABEL_FAILED.getMsg());
        }
        return true;
    }

    @Override
    public List<ClusterNodeLabelEntity> queryClusterNodeLabel(Integer clusterId) {
        return clusterNodeLabelMapper.selectByClusterId(clusterId);
    }

    @Override
    public void createDefaultNodeLabel(Integer clusterId) {
        ClusterNodeLabelEntity nodeLabelEntity = new ClusterNodeLabelEntity();
        nodeLabelEntity.setNodeLabel("default");
        nodeLabelEntity.setClusterId(clusterId);
        clusterNodeLabelMapper.insert(nodeLabelEntity);
    }

    private boolean nodeLabelInUse(String nodeLabel) {
        // TODO: 待ClusterHostService改造完成后实现具体的查询逻辑
        // List<ClusterHostDO> list = hostService.getHostListByNodeLabel(nodeLabel);
        // 临时返回false，避免编译错误
        return false;
    }

    private boolean repeatNodeLable(Integer clusterId, String nodeLabel) {
        List<ClusterNodeLabelEntity> list = clusterNodeLabelMapper.selectByClusterIdAndNodeLabel(clusterId, nodeLabel);
        return CollUtil.isNotEmpty(list);
    }

    // 标准CRUD方法实现
    @Override
    public ClusterNodeLabelEntity getById(Integer id) {
        return clusterNodeLabelMapper.selectById(id);
    }

    @Override
    public ClusterNodeLabelEntity save(ClusterNodeLabelEntity entity) {
        clusterNodeLabelMapper.insert(entity);
        return entity;
    }

    @Override
    public ClusterNodeLabelEntity updateById(ClusterNodeLabelEntity entity) {
        clusterNodeLabelMapper.updateById(entity);
        return entity;
    }

    @Override
    public boolean removeByIds(List<Integer> ids) {
        return clusterNodeLabelMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<ClusterNodeLabelEntity> getAllNodeLabels() {
        return clusterNodeLabelMapper.selectAll();
    }
}
