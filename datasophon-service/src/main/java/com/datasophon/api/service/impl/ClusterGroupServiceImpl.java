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
import com.datasophon.api.converter.ClusterGroupConverter;
import com.datasophon.common.enums.Status;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterGroupService;
import com.datasophon.api.service.ClusterUserGroupService;
import com.datasophon.api.service.HostGroupSyncService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.api.utils.string.validator.WordValidator;
import com.datasophon.common.Constants;
import com.datasophon.common.command.remote.CreateUnixGroupCommand;
import com.datasophon.common.command.remote.DelUnixGroupCommand;
import com.datasophon.common.dto.ClusterGroupDTO;
import com.datasophon.common.dto.ClusterUserDTO;
import com.datasophon.common.enums.UserEnum;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterGroup;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.mapper.ClusterGroupMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;

import org.apache.pekko.actor.ActorRef;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 集群组服务实现类
 * 提供集群组的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterGroupService")
@Transactional
public class ClusterGroupServiceImpl extends ServiceImpl<ClusterGroupMapper, ClusterGroup>
        implements ClusterGroupService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterGroupServiceImpl.class);

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterUserGroupService userGroupService;

    @Autowired
    private ClusterGroupConverter clusterGroupConverter;

    @Autowired
    private HostGroupSyncService hostGroupSyncService;

    @Override
    public ClusterGroupDTO saveClusterGroup(Integer clusterId, String groupName) {
        // 用户组名校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        WordValidator wordValidator = new WordValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(wordValidator);
        wordValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(groupName);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        if (hasRepeatGroupName(clusterId, groupName)) {
            throw new RuntimeException(Status.GROUP_NAME_DUPLICATION.getMsg());
        }
        ClusterGroup clusterGroup = new ClusterGroup();
        clusterGroup.setClusterId(clusterId);
        clusterGroup.setGroupName(groupName);
        this.save(clusterGroup);

        List<ClusterHostDO> hostList = hostService.getHostListByClusterId(clusterId);
        for (ClusterHostDO clusterHost : hostList) {
            ActorRef unixGroupActor = ActorUtils.getRemoteActor(clusterHost.getHostname(), "unixGroupActor");
            CreateUnixGroupCommand createUnixGroupCommand = new CreateUnixGroupCommand();
            createUnixGroupCommand.setGroupName(groupName);
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            Future<Object> execFuture = Patterns.ask(unixGroupActor, createUnixGroupCommand, timeout);
            ExecResult execResult;
            try {
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("create unix group success at {}", clusterHost.getHostname());
                } else {
                    logger.info(execResult.getExecOut());
                    throw new ServiceException(500,
                            "create unix group " + groupName + " failed at " + clusterHost.getHostname());
                }
            } catch (Exception e) {
                throw new ServiceException(500,
                        "create unix group " + groupName + " failed at " + clusterHost.getHostname());
            }
        }

        // Service层：Entity → DTO转换
        return clusterGroupConverter.entityToDto(clusterGroup);
    }

    @Override
    public ClusterGroupDTO saveClusterGroupOnKubernetes(Integer clusterId, String groupName) {
        // 用户组名校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        WordValidator wordValidator = new WordValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(wordValidator);
        wordValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(groupName);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        if (hasRepeatGroupName(clusterId, groupName)) {
            throw new RuntimeException(Status.GROUP_NAME_DUPLICATION.getMsg());
        }
        ClusterGroup clusterGroup = new ClusterGroup();
        clusterGroup.setClusterId(clusterId);
        clusterGroup.setGroupName(groupName);
        this.save(clusterGroup);

        Map<String, UserEnum> groupNameMap = UserEnum.getGroupNameMap();
        Integer systemInitMaxGid = groupNameMap.values().stream()
                .map(UserEnum::getGroupId)
                .max(Integer::compareTo)
                .orElse(0);

        int globalMaxGid = 0;

        List<ClusterHostDO> hostList = hostService.getHostListByClusterId(clusterId);

        for (ClusterHostDO clusterHost : hostList) {
            // 执行命令获取当前主机的最大 GID
            String result = KubernetesMinaUtils.execCmdWithResult(clusterHost.getHostname(),
                    "awk -F: 'BEGIN { max = 0 } { if ($3 < 65000 && $3 > max) max=$3 } END { print max }' /etc/group");

            // 将返回结果转换为 Integer 类型
            int currentMaxGid;
            try {
                currentMaxGid = Integer.parseInt(Objects.requireNonNull(result).trim()); // 解析结果
            } catch (NumberFormatException e) {
                System.err.println("无法解析 GID: " + result);
                continue;
            }

            // 更新全局最大 UID
            if (currentMaxGid > globalMaxGid) {
                globalMaxGid = currentMaxGid;
            }
        }

        Integer createUnixUserGid = Math.max(systemInitMaxGid, globalMaxGid) + 1;

        if (createUnixUserGid > 65535) {
            throw new ServiceException(500,
                    "create unix user " + groupName + " failed at Gid{" + createUnixUserGid + "} > 65535");
        }
        for (ClusterHostDO clusterHost : hostList) {
            try {
                String result = createUnixGroup(groupName, clusterHost.getHostname(), createUnixUserGid);
                if (!result.equals(Constants.FAILED)) {
                    logger.info("create unix group {} success at {}", groupName, clusterHost.getHostname());
                } else {
                    logger.info("create unix group {} failed at {}", groupName, clusterHost.getHostname());
                    throw new ServiceException(500,
                            "create unix group " + groupName + " failed at " + clusterHost.getHostname());
                }
            } catch (Exception e) {
                throw new ServiceException(500,
                        "create unix group " + groupName + " failed at " + clusterHost.getHostname());
            }
        }

        // Service层：Entity → DTO转换
        return clusterGroupConverter.entityToDto(clusterGroup);
    }

    private boolean hasRepeatGroupName(Integer clusterId, String groupName) {
        List<ClusterGroup> list = this.getMapper().selectByClusterIdAndGroupName(clusterId, groupName);
        return CollUtil.isNotEmpty(list);
    }

    @Override
    public void refreshUserGroupToHost(Integer clusterId) {
        List<ClusterHostDO> hostList = hostService.getHostListByClusterId(clusterId);
        List<ClusterGroup> groupList = this.list();
        for (ClusterGroup clusterGroup : groupList) {
            hostGroupSyncService.syncUserGroupToHosts(hostList, clusterGroup.getGroupName(), "groupadd");
        }
    }

    @Override
    public boolean deleteUserGroup(Integer id) {
        ClusterGroup clusterGroup = this.getById(id);
        if (clusterGroup == null) {
            throw new RuntimeException("Group not found with id: " + id);
        }
        long num = userGroupService.countGroupUserNum(id);
        if (num > 0) {
            throw new RuntimeException(Status.USER_GROUP_TIPS_ONE.getMsg());
        }
        this.removeById(id);
        List<ClusterHostDO> hostList = hostService.getHostListByClusterId(clusterGroup.getClusterId());
        for (ClusterHostDO clusterHost : hostList) {
            ActorRef unixGroupActor = ActorUtils.getRemoteActor(clusterHost.getHostname(), "unixGroupActor");
            DelUnixGroupCommand delUnixGroupCommand = new DelUnixGroupCommand();
            delUnixGroupCommand.setGroupName(clusterGroup.getGroupName());
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            Future<Object> execFuture = Patterns.ask(unixGroupActor, delUnixGroupCommand, timeout);
            ExecResult execResult;
            try {
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("del unix group success at {}", clusterHost.getHostname());
                } else {
                    logger.info("del unix group failed at {}", clusterHost.getHostname());
                }
            } catch (Exception e) {
                logger.info("del unix group failed at {}", clusterHost.getHostname());
            }
        }
        return true;
    }

    @Override
    public boolean deleteUserGroupOnKubernetes(Integer id) {
        ClusterGroup clusterGroup = this.getById(id);
        if (clusterGroup == null) {
            throw new RuntimeException("Group not found with id: " + id);
        }
        long num = userGroupService.countGroupUserNum(id);
        if (num > 0) {
            throw new RuntimeException(Status.USER_GROUP_TIPS_ONE.getMsg());
        }
        this.removeById(id);
        List<ClusterHostDO> hostList = hostService.getHostListByClusterId(clusterGroup.getClusterId());
        for (ClusterHostDO clusterHost : hostList) {
            try {
                if (!delUnixGroup(clusterGroup.getGroupName(), clusterHost.getHostname()).equals(Constants.FAILED)) {
                    logger.info("del unix group success at {}", clusterHost.getHostname());
                } else {
                    logger.info("del unix group failed at {}", clusterHost.getHostname());
                }
            } catch (Exception e) {
                logger.info("del unix group failed at {}", clusterHost.getHostname());
            }
        }
        return true;
    }

    @Override
    public PageResult<ClusterGroupDTO> listPage(String groupName, Integer clusterId, Integer page, Integer pageSize) {
        // 使用mapper的分页查询方法
        PageResult<ClusterGroup> pageResult = this.getMapper().selectPageByClusterIdAndGroupName(
                clusterId, groupName, page, pageSize);

        List<ClusterGroup> list = pageResult.getRecords();

        // 填充用户信息并转换为DTO
        List<ClusterGroupDTO> dtoList = new ArrayList<>();
        for (ClusterGroup clusterGroup : list) {
            List<ClusterUserDTO> clusterUserList = userGroupService.listClusterUsers(clusterGroup.getId());
            if (Objects.nonNull(clusterUserList) && !clusterUserList.isEmpty()) {
                String clusterUsers = clusterUserList.stream().map(ClusterUserDTO::username)
                        .collect(java.util.stream.Collectors.joining(","));
                clusterGroup.setClusterUsers(clusterUsers);
            }
            // Entity → DTO转换
            dtoList.add(clusterGroupConverter.entityToDto(clusterGroup));
        }

        return PageResult.of(dtoList, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    @Override
    public List<ClusterGroupDTO> listAllUserGroup(Integer clusterId) {
        List<ClusterGroup> entities = this.getMapper().selectByClusterId(clusterId);
        return entities.stream()
                .map(clusterGroupConverter::entityToDto)
                .toList();
    }

    @Override
    public void createUnixGroupOnHost(String hostname, String groupName) {
        ActorRef unixGroupActor = ActorUtils.getRemoteActor(hostname, "unixGroupActor");
        createUnixGroup(hostname, unixGroupActor, groupName);
    }

    private void createUnixGroup(String hostname, ActorRef unixGroupActor, String groupName) {
        CreateUnixGroupCommand createUnixGroupCommand = new CreateUnixGroupCommand();
        createUnixGroupCommand.setGroupName(groupName);
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> execFuture = Patterns.ask(unixGroupActor, createUnixGroupCommand, timeout);
        ExecResult execResult;
        try {
            execResult = (ExecResult) Await.result(execFuture, timeout.duration());
            if (execResult.getExecResult()) {
                logger.info("create unix group success at {}", hostname);
            } else {
                logger.info(execResult.getExecOut());
                throw new ServiceException(500, "create unix group " + groupName + " failed at " + hostname);
            }
        } catch (Exception e) {
            throw new ServiceException(500, "create unix group " + groupName + " failed at " + hostname);
        }
    }

    public static String createUnixGroup(String groupName, String hostname, Integer createUnixGroupGid) {
        if (isGroupExists(groupName, hostname)) {
            return Constants.FAILED;
        }
        ArrayList<String> commands = new ArrayList<>();
        commands.add("groupadd");
        commands.add(groupName);
        if (createUnixGroupGid != null) {
            commands.add("-g");
            commands.add(String.valueOf(createUnixGroupGid));
        }
        return KubernetesMinaUtils.execCmdWithResult(hostname, String.join(" ", commands));
    }

    public static String delUnixGroup(String groupName, String hostname) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("groupdel");
        commands.add(groupName);
        return KubernetesMinaUtils.execCmdWithResult(hostname, String.join(" ", commands));
    }

    public static boolean isGroupExists(String groupName, String hostname) {
        String result = KubernetesMinaUtils.execCmdWithResult(hostname,
                "egrep \"" + groupName + "\" /etc/group >& /dev/null");
        return !Objects.requireNonNull(result).equals(Constants.FAILED);
    }

    @Override
    public ClusterGroupDTO getByIdAsDto(Integer id) {
        // Service层：Entity → DTO转换
        ClusterGroup entity = this.getById(id);
        return clusterGroupConverter.entityToDto(entity);
    }

    @Override
    public void saveClusterGroupDto(ClusterGroupDTO dto) {
        // Service层：DTO → Entity转换
        ClusterGroup entity = clusterGroupConverter.dtoToEntity(dto);
        this.save(entity);
    }

    @Override
    public void updateClusterGroup(ClusterGroupDTO dto) {
        // Service层：DTO → Entity转换
        ClusterGroup entity = clusterGroupConverter.dtoToEntity(dto);
        this.updateById(entity);
    }
}