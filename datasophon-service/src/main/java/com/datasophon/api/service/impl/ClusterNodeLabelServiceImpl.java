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
import com.datasophon.api.converter.ClusterNodeLabelConverter;
import com.datasophon.common.dto.ClusterNodeLabelDTO;
import com.datasophon.common.enums.ClusterType;
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
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterNodeLabelEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterNodeLabelMapper;
import com.datasophon.kubernetes.util.KubeUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.datasophon.api.utils.ProcessUtils.getDepMode;
import static com.datasophon.kubernetes.util.KubernetesUtil.runCmd;

/**
 * 集群节点标签服务实现类
 * 提供集群节点标签的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterNodeLabelService")
@Transactional
public class ClusterNodeLabelServiceImpl extends ServiceImpl<ClusterNodeLabelMapper, ClusterNodeLabelEntity>
        implements ClusterNodeLabelService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterNodeLabelServiceImpl.class);

    @Autowired
    private ClusterNodeLabelConverter clusterNodeLabelConverter;

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Override
    public ClusterNodeLabelDTO saveNodeLabel(Long clusterId, String nodeLabel) {
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
        this.save(nodeLabelEntity);
        // refresh to yarn
        if (!refreshToYarn(clusterId, "-addToClusterNodeLabels", nodeLabel)) {
            throw new BusinessException(
                    Status.ADD_YARN_NODE_LABEL_FAILED.getMsg() + " , 请检查yarn配置页面标签配置项(yarn.node-labels.enabled)是否开启");
        }
        // Service层：Entity → DTO转换
        return clusterNodeLabelConverter.entityToDto(nodeLabelEntity);
    }

    private boolean refreshToYarn(Long clusterId, String type, String nodeLabel) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        // 由于ClusterServiceRoleInstanceService已重构，直接使用getMapper()查询Entity
        List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceService
                .getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, "ResourceManager").stream()
                .map(dto -> {
                    ClusterServiceRoleInstanceEntity entity = new ClusterServiceRoleInstanceEntity();
                    entity.setHostname(dto.hostname());
                    return entity;
                })
                .toList();
        ClusterType depMode = getDepMode(clusterId);
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
            if (depMode == ClusterType.PVM) {
                ExecuteCmdCommand command = new ExecuteCmdCommand();
                command.setCommands(commands);
                
                // 使用HTTP方式提交任务到Worker
                ExecResult execResult = WorkerTaskHelper.submitAndWait(hostname, command, 180);
                if (execResult.getExecResult()) {
                    logger.info("add yarn node label success at {}", hostname);
                    return true;
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
        ClusterNodeLabelEntity nodeLabelEntity = this.getById(nodeLabelId);
        if (nodeLabelEntity == null) {
            throw new RuntimeException("Node label not found with id: " + nodeLabelId);
        }

        if (nodeLabelInUse(nodeLabelEntity.getNodeLabel())) {
            throw new RuntimeException(Status.NODE_LABEL_IS_USING.getMsg());
        }
        this.removeById(nodeLabelId);
        if (!refreshToYarn(nodeLabelEntity.getClusterId(), "-removeFromClusterNodeLabels",
                nodeLabelEntity.getNodeLabel())) {
            throw new BusinessException(Status.REMOVE_YARN_NODE_LABEL_FAILED.getMsg());
        }
        return true;
    }

    @Override
    public boolean assignNodeLabel(Integer nodeLabelId, String hostIds) {
        ClusterNodeLabelEntity nodeLabelEntity = this.getById(nodeLabelId);
        if (nodeLabelEntity == null) {
            throw new RuntimeException("Node label not found with id: " + nodeLabelId);
        }
        List<String> ids = List.of(hostIds.split(","));
        hostService.updateBatchNodeLabel(ids, nodeLabelEntity.getNodeLabel());

        List<ClusterHostEntity> list = hostService.getHostListByIds(ids);
        String assignNodeLabel = list.stream().map(e -> e.getHostname() + "=" + nodeLabelEntity.getNodeLabel())
                .collect(java.util.stream.Collectors.joining(" "));
        logger.info("assign node label {}", assignNodeLabel);
        // sync to yarn
        // refresh to yarn
        if (!refreshToYarn(nodeLabelEntity.getClusterId(), "-replaceLabelsOnNode", assignNodeLabel)) {
            throw new BusinessException(Status.ASSIGN_YARN_NODE_LABEL_FAILED.getMsg());
        }
        return true;
    }

    @Override
    public List<ClusterNodeLabelDTO> queryClusterNodeLabel(Long clusterId) {
        List<ClusterNodeLabelEntity> entities = getMapper().selectByClusterId(clusterId);
        return clusterNodeLabelConverter.entityListToDtoList(entities);
    }

    @Override
    public void createDefaultNodeLabel(Long clusterId) {
        ClusterNodeLabelEntity nodeLabelEntity = new ClusterNodeLabelEntity();
        nodeLabelEntity.setNodeLabel("default");
        nodeLabelEntity.setClusterId(clusterId);
        this.save(nodeLabelEntity);
    }

    private boolean nodeLabelInUse(String nodeLabel) {
        // TODO: 待ClusterHostService改造完成后实现具体的查询逻辑
        // List<ClusterHostEntity> list = hostService.getHostListByNodeLabel(nodeLabel);
        // 临时返回false，避免编译错误
        return false;
    }

    private boolean repeatNodeLable(Long clusterId, String nodeLabel) {
        List<ClusterNodeLabelEntity> list = getMapper().selectByClusterIdAndNodeLabel(clusterId, nodeLabel);
        return CollUtil.isNotEmpty(list);
    }

    // 新增DTO方法实现
    @Override
    public ClusterNodeLabelDTO getByIdAsDto(Long id) {
        ClusterNodeLabelEntity entity = this.getById(id);
        return clusterNodeLabelConverter.entityToDto(entity);
    }

    @Override
    public ClusterNodeLabelDTO saveNodeLabelDto(ClusterNodeLabelDTO dto) {
        ClusterNodeLabelEntity entity = clusterNodeLabelConverter.dtoToEntity(dto);
        this.save(entity);
        return clusterNodeLabelConverter.entityToDto(entity);
    }

    @Override
    public void updateNodeLabel(ClusterNodeLabelDTO dto) {
        ClusterNodeLabelEntity entity = clusterNodeLabelConverter.dtoToEntity(dto);
        this.updateById(entity);
    }
}
