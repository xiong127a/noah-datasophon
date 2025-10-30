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
import cn.hutool.core.util.NumberUtil;
import com.datasophon.common.enums.Status;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.handler.service.WorkerTaskHelper;
import com.datasophon.api.service.ClusterGroupService;
import com.datasophon.api.service.ClusterUserGroupService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.api.utils.string.validator.WordValidator;
import com.datasophon.common.Constants;
import com.datasophon.common.command.LdapCommand;
import com.datasophon.common.command.remote.CreateUnixUserCommand;
import com.datasophon.common.command.remote.DelUnixUserCommand;
import com.datasophon.common.enums.UserEnum;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.api.converter.ClusterUserConverter;
import com.datasophon.common.dto.ClusterUserDTO;
import com.datasophon.common.dto.ClusterGroupDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterGroupEntity;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterUserEntity;
import com.datasophon.dao.entity.ClusterUserGroupEntity;
import com.datasophon.dao.mapper.ClusterUserMapper;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.datasophon.common.utils.OpenldapUtils.openldapProcess;

/**
 * 集群用户服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("clusterUserService")
@Transactional
public class ClusterUserServiceImpl extends ServiceImpl<ClusterUserMapper, ClusterUserEntity> implements ClusterUserService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterUserServiceImpl.class);
    @Autowired
    private ClusterGroupService groupService;

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterUserGroupService userGroupService;

    @Autowired
    private SimpleClusterVariableService simpleClusterVariableService;

    @Autowired
    private ClusterUserConverter clusterUserConverter;

    @Override
    public ClusterUserDTO createClusterUser(Long clusterId, String username, Long mainGroupId, String groupIds) {

        // 用户名校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        WordValidator wordValidator = new WordValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(wordValidator);
        wordValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(username);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        if (hasRepeatUserName(clusterId, username)) {
            throw new RuntimeException(Status.DUPLICATE_USER_NAME.getMsg());
        }
        List<ClusterHostEntity> hostList = hostService.getHostListByClusterIdAndManaged(clusterId);

        ClusterUserEntity clusterUserEntity = new ClusterUserEntity();
        clusterUserEntity.setUsername(username);
        clusterUserEntity.setClusterId(clusterId);
        this.save(clusterUserEntity);
        buildClusterUserGroup(clusterId, clusterUserEntity.getId(), mainGroupId, 1);

        String otherGroup = null;
        if (StringUtils.isNotBlank(groupIds)) {
            List<Long> otherGroupIds = Arrays.stream(groupIds.split(",")).map(Long::parseLong)
                    .toList();
            for (Long id : otherGroupIds) {
                buildClusterUserGroup(clusterId, clusterUserEntity.getId(), id, 2);
            }
            Collection<ClusterGroupEntity> clusterGroupEntities = groupService.listByIds(otherGroupIds);
            otherGroup = clusterGroupEntities.stream().map(ClusterGroupEntity::getGroupName)
                    .collect(java.util.stream.Collectors.joining(","));
        }

        ClusterGroupEntity mainGroup = groupService.getById(mainGroupId);
        // sync to all hosts
        for (ClusterHostEntity clusterHost : hostList) {
            CreateUnixUserCommand createUnixUserCommand = new CreateUnixUserCommand();
            createUnixUserCommand.setUsername(username);
            createUnixUserCommand.setMainGroup(mainGroup.getGroupName());
            createUnixUserCommand.setOtherGroups(otherGroup);

            // 使用HTTP方式提交任务到Worker
            ExecResult execResult = WorkerTaskHelper.submitAndWait(
                    clusterHost.getHostname(), createUnixUserCommand, 180);
            
            if (execResult.getExecResult()) {
                logger.info("create unix user {} success at {}", username, clusterHost.getHostname());
            } else {
                logger.info(execResult.getExecOut());
                throw new ServiceException(500,
                        "create unix user " + username + " failed at " + clusterHost.getHostname());
            }
        }

        // create ldap user
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        String openldapIp = globalVariables.get("${openldapIp}");

        LdapCommand ldapCommand = new LdapCommand();
        ldapCommand.setOperation("add");
        ldapCommand.setLdapUrl(globalVariables.get("${syncLdapUrl}"));
        ldapCommand.setUsername(username);
        ldapCommand.setMail("");
        ldapCommand.setDescription("");
        ldapCommand.setRootDn(globalVariables.get("${syncLdapBindDn}"));
        ldapCommand.setUserRootDn(globalVariables.get("${syncLdapUserSearchBase}"));
        ldapCommand.setLdapPwd(globalVariables.get("${syncLdapBindPassword}"));
        ldapCommand.setUserPwd(globalVariables.get("${syncLdapBindPassword}"));
        String uid = globalVariables.get("${syncLdapUidNumber}");
        if (StringUtils.isBlank(uid)) {
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${syncLdapUidNumber}", "2000");
            ldapCommand.setUidNumber("2000");
        } else {
            String nextUid = NumberUtil.toStr(NumberUtil.add("2000", "1").longValue());
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${syncLdapUidNumber}", nextUid);
            ldapCommand.setUidNumber(nextUid);
        }
        ldapCommand.setGidNumber("55");

        // 使用HTTP方式提交任务到Worker (LDAP服务所在主机)
        ExecResult execResult = WorkerTaskHelper.submitAndWait(openldapIp, ldapCommand, 180);
        if (execResult.getExecResult()) {
            logger.info("create ldap user {} success", username);
        } else {
            logger.error("create ldap user {} failed", username);
            logger.error(execResult.getExecOut());
            logger.error(execResult.getExecErrOut());
        }

        return clusterUserConverter.entityToDto(clusterUserEntity);
    }

    @Override
    public ClusterUserDTO createClusterUserOnKubernetes(Long clusterId, String username, Long mainGroupId,
            String groupIds) {

        // 用户名校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        WordValidator wordValidator = new WordValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(wordValidator);
        wordValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(username);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        if (hasRepeatUserName(clusterId, username)) {
            throw new RuntimeException(Status.DUPLICATE_USER_NAME.getMsg());
        }
        List<ClusterHostEntity> hostList = hostService.getHostListByClusterIdAndManaged(clusterId);

        ClusterUserEntity clusterUserEntity = new ClusterUserEntity();
        clusterUserEntity.setUsername(username);
        clusterUserEntity.setClusterId(clusterId);
        this.save(clusterUserEntity);
        buildClusterUserGroup(clusterId, clusterUserEntity.getId(), mainGroupId, 1);

        String otherGroup = null;
        if (StringUtils.isNotBlank(groupIds)) {
            List<Long> otherGroupIds = Arrays.stream(groupIds.split(",")).map(Long::parseLong)
                    .toList();
            for (Long id : otherGroupIds) {
                buildClusterUserGroup(clusterId, clusterUserEntity.getId(), id, 2);
            }
            Collection<ClusterGroupEntity> clusterGroupEntities = groupService.listByIds(otherGroupIds);
            otherGroup = clusterGroupEntities.stream().map(ClusterGroupEntity::getGroupName)
                    .collect(java.util.stream.Collectors.joining(","));
        }
        ClusterGroupEntity mainGroup = groupService.getById(mainGroupId);
        Map<String, UserEnum> userNameMap = UserEnum.getUserNameMap();
        Integer systemInitMaxUid = userNameMap.values().stream()
                .map(UserEnum::getUserId)
                .max(Integer::compareTo)
                .orElse(0);
        int globalMaxUid = 0; // 声明一个全局变量来存储最大 UID

        for (ClusterHostEntity clusterHost : hostList) {
            // 执行命令获取当前主机的最大 UID
            String result = KubernetesMinaUtils.execCmdWithResult(clusterHost.getHostname(),
                    "awk -F: 'BEGIN { max = 0 } { if ($3 < 65000 && $3 > max) max=$3 } END { print max }' /etc/passwd");

            // 将返回结果转换为 Integer 类型
            int currentMaxUid;
            try {
                currentMaxUid = Integer.parseInt(Objects.requireNonNull(result).trim()); // 解析结果
            } catch (NumberFormatException e) {
                System.err.println("无法解析 UID: " + result);
                continue;
            }

            // 更新全局最大 UID
            if (currentMaxUid > globalMaxUid) {
                globalMaxUid = currentMaxUid;
            }
        }
        Integer createUnixUserUid = Math.max(systemInitMaxUid, globalMaxUid) + 1;

        if (createUnixUserUid > 65535) {
            throw new ServiceException(500,
                    "create unix user " + username + " failed at Uid{" + createUnixUserUid + "} > 65535");
        }

        for (ClusterHostEntity clusterHost : hostList) {
            try {
                if (!createUnixUser(username, mainGroup.getGroupName(), otherGroup, clusterHost.getHostname(),
                        createUnixUserUid).equals(Constants.FAILED)) {
                    logger.info("create unix user {} success at {}", username, clusterHost.getHostname());
                } else {
                    throw new ServiceException(500,
                            "create unix user " + username + " failed at " + clusterHost.getHostname());
                }
            } catch (Exception e) {
                throw new ServiceException(500,
                        "create unix user " + username + " failed at " + clusterHost.getHostname());
            }
        }

        // create ldap user
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);

        LdapCommand ldapCommand = new LdapCommand();
        ldapCommand.setOperation("add");
        ldapCommand.setLdapUrl(globalVariables.get("${syncLdapUrl}"));
        ldapCommand.setUsername(username);
        ldapCommand.setMail("");
        ldapCommand.setDescription("");
        ldapCommand.setRootDn(globalVariables.get("${syncLdapBindDn}"));
        ldapCommand.setUserRootDn(globalVariables.get("${syncLdapUserSearchBase}"));
        ldapCommand.setLdapPwd(globalVariables.get("${syncLdapBindPassword}"));
        ldapCommand.setUserPwd(globalVariables.get("${syncLdapBindPassword}"));
        ldapCommand.setUidNumber(String.valueOf(createUnixUserUid));
        ldapCommand.setGidNumber("55");

        try {
            if (openldapProcess(ldapCommand)) {
                logger.info("create ldap user {} success", username);
            } else {
                logger.error("create ldap user {} failed", username);

            }
        } catch (Exception e) {
            logger.error("create ldap user {} failed", username);
            logger.error(e.getMessage());
        }

        return clusterUserConverter.entityToDto(clusterUserEntity);
    }

    private void buildClusterUserGroup(Long clusterId, Long userId, Long groupId, Integer userGroupType) {
        ClusterUserGroupEntity clusterUserGroupEntity = new ClusterUserGroupEntity();
        clusterUserGroupEntity.setUserId(userId);
        clusterUserGroupEntity.setGroupId(groupId);
        clusterUserGroupEntity.setClusterId(clusterId);
        clusterUserGroupEntity.setUserGroupType(userGroupType);
        userGroupService.save(clusterUserGroupEntity);
    }

    private boolean hasRepeatUserName(Long clusterId, String username) {
        List<ClusterUserEntity> list = getMapper().selectByClusterIdAndUsername(clusterId, username);
        return CollUtil.isNotEmpty(list);
    }

    @Override
    public PageResult<ClusterUserDTO> listPagedUsers(Long clusterId, String username, Integer page,
            Integer pageSize) {
        Integer offset = (page - 1) * pageSize;

        List<ClusterUserEntity> list = getMapper().selectByClusterIdWithPagination(clusterId, username, offset, pageSize);

        for (ClusterUserEntity clusterUserEntity : list) {
            ClusterGroupDTO mainGroup = userGroupService.queryMainGroup(clusterUserEntity.getId());
            List<ClusterGroupDTO> otherGroupList = userGroupService.listOtherGroups(clusterUserEntity.getId());
            if (Objects.nonNull(otherGroupList) && !otherGroupList.isEmpty()) {
                String otherGroups = otherGroupList.stream().map(ClusterGroupDTO::groupName)
                        .collect(java.util.stream.Collectors.joining(","));
                clusterUserEntity.setOtherGroups(otherGroups);
            }
            clusterUserEntity.setMainGroup(mainGroup.groupName());
        }

        long total = getMapper().countByClusterIdAndUsername(clusterId, username);
        List<ClusterUserDTO> dtoList = clusterUserConverter.entityListToDtoList(list);
        return PageResult.of(dtoList, total, page, pageSize);
    }

    @Override
    public boolean deleteClusterUser(Long id) {
        ClusterUserEntity clusterUserEntity = this.getById(id);
        // delete user and group
        userGroupService.deleteByUser(id);
        List<ClusterHostEntity> hostList = hostService.getHostListByClusterIdAndManaged(clusterUserEntity.getClusterId());
        // sync to all hosts
        for (ClusterHostEntity clusterHost : hostList) {
            DelUnixUserCommand createUnixUserCommand = new DelUnixUserCommand();
            createUnixUserCommand.setUsername(clusterUserEntity.getUsername());
            
            // 使用HTTP方式提交任务到Worker
            ExecResult execResult = WorkerTaskHelper.submitAndWait(
                    clusterHost.getHostname(), createUnixUserCommand, 180);
            
            if (execResult.getExecResult()) {
                logger.info("del unix user success at {}", clusterHost.getHostname());
            } else {
                logger.info("del unix user failed at {}", clusterHost.getHostname());
            }
        }

        // delete ldap user
        Map<String, String> globalVariables = GlobalVariables.get(clusterUserEntity.getClusterId());
        String openldapIp = globalVariables.get("${openldapIp}");

        LdapCommand ldapCommand = new LdapCommand();
        ldapCommand.setOperation("delete");
        ldapCommand.setLdapUrl(globalVariables.get("${syncLdapUrl}"));
        ldapCommand.setRootDn(globalVariables.get("${syncLdapBindDn}"));
        ldapCommand.setLdapPwd(globalVariables.get("${syncLdapBindPassword}"));
        ldapCommand.setUsername(clusterUserEntity.getUsername());
        ldapCommand.setUserRootDn(globalVariables.get("${syncLdapUserSearchBase}"));

        // 使用HTTP方式提交任务到Worker (LDAP服务所在主机)
        ExecResult execResult = WorkerTaskHelper.submitAndWait(openldapIp, ldapCommand, 180);
        if (execResult.getExecResult()) {
            logger.info("delete ldap user {} success", clusterUserEntity.getUsername());
        } else {
            logger.error("delete ldap user {} failed", clusterUserEntity.getUsername());
            logger.error(execResult.getExecOut());
            logger.error(execResult.getExecErrOut());
        }

        return this.removeById(id);
    }

    @Override
    public boolean deleteClusterUserOnKubernetes(Long id) {
        ClusterUserEntity clusterUserEntity = this.getById(id);
        // delete user and group
        userGroupService.deleteByUser(id);
        List<ClusterHostEntity> hostList = hostService.getHostListByClusterIdAndManaged(clusterUserEntity.getClusterId());
        // sync to all hosts
        for (ClusterHostEntity clusterHost : hostList) {
            try {
                if (!delUnixUser(clusterUserEntity.getUsername(), clusterHost.getHostname()).equals(Constants.FAILED)) {
                    logger.info("del unix user success at {}", clusterHost.getHostname());
                } else {
                    logger.info("del unix user failed at {}", clusterHost.getHostname());
                }
            } catch (Exception e) {
                logger.info("del unix user failed at {}", clusterHost.getHostname());
            }
        }

        // delete ldap user
        Map<String, String> globalVariables = GlobalVariables.get(clusterUserEntity.getClusterId());

        LdapCommand ldapCommand = new LdapCommand();
        ldapCommand.setOperation("delete");
        ldapCommand.setLdapUrl(globalVariables.get("${syncLdapUrl}"));
        ldapCommand.setRootDn(globalVariables.get("${syncLdapBindDn}"));
        ldapCommand.setLdapPwd(globalVariables.get("${syncLdapBindPassword}"));
        ldapCommand.setUsername(clusterUserEntity.getUsername());
        ldapCommand.setUserRootDn(globalVariables.get("${syncLdapUserSearchBase}"));

        try {
            if (openldapProcess(ldapCommand)) {
                logger.info("delete ldap user {} success", clusterUserEntity.getUsername());
            } else {
                logger.error("delete ldap user {} failed", clusterUserEntity.getUsername());
            }
        } catch (Exception e) {
            logger.error("delete ldap user {} failed", clusterUserEntity.getUsername());
            logger.error(e.getMessage());
        }

        return this.removeById(id);
    }

    @Override
    public List<ClusterUserDTO> listAllUser(Long clusterId) {
        List<ClusterUserEntity> entities = getMapper().selectByClusterId(clusterId);
        return clusterUserConverter.entityListToDtoList(entities);
    }

    @Override
    public void createUnixUserOnHost(ClusterUserDTO clusterUserDTO, String hostname) {
        String username = clusterUserDTO.username();
        ClusterGroupDTO mainGroup = userGroupService.queryMainGroup(clusterUserDTO.id());
        List<ClusterGroupDTO> otherGroupList = userGroupService.listOtherGroups(clusterUserDTO.id());
        String otherGroup = "";
        if (Objects.nonNull(otherGroupList) && !otherGroupList.isEmpty()) {
            otherGroup = otherGroupList.stream()
                    .map(ClusterGroupDTO::groupName)
                    .collect(java.util.stream.Collectors.joining(","));
        }

        // 使用HTTP方式提交命令到Worker
        CreateUnixUserCommand createUnixUserCommand = new CreateUnixUserCommand();
        createUnixUserCommand.setUsername(clusterUserDTO.username());
        createUnixUserCommand.setMainGroup(mainGroup.groupName());
        createUnixUserCommand.setOtherGroups(otherGroup);

        try {
            ExecResult execResult = com.datasophon.api.master.handler.service.WorkerTaskHelper.submitAndWait(
                    hostname, createUnixUserCommand, 180);
            
            if (execResult != null && execResult.getExecResult()) {
                logger.info("create unix user {} success at {}", username, hostname);
            } else {
                logger.info(execResult != null ? execResult.getExecOut() : "No response");
                throw new ServiceException(500, "create unix user " + username + " failed at " + hostname);
            }
        } catch (Exception e) {
            logger.error("Failed to create unix user {} at {}", username, hostname, e);
            throw new ServiceException(500, "create unix user " + username + " failed at " + hostname);
        }

    }

    public static String createUnixUser(String username, String mainGroup, String otherGroups, String hostname,
            Integer createUnixUserUid) {
        ArrayList<String> commands = new ArrayList<>();
        if (!isUserExists(username, hostname).equals(Constants.FAILED)) {
            commands.add("usermod");
        } else {
            commands.add("useradd");
        }
        commands.add(username);
        if (StringUtils.isNotBlank(mainGroup)) {
            commands.add("-g");
            commands.add(mainGroup);
        }
        if (StringUtils.isNotBlank(otherGroups)) {
            commands.add("-G");
            commands.add(otherGroups);
        }
        if (createUnixUserUid != null) {
            commands.add("-u");
            commands.add(String.valueOf(createUnixUserUid));
        }

        return KubernetesMinaUtils.execCmdWithResult(hostname, String.join(" ", commands));
    }

    public static String isUserExists(String username, String hostname) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("id");
        commands.add(username);
        return KubernetesMinaUtils.execCmdWithResult(hostname, String.join(" ", commands));
    }

    public static String delUnixUser(String username, String hostname) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("userdel");
        commands.add("-r");
        commands.add(username);
        return KubernetesMinaUtils.execCmdWithResult(hostname, String.join(" ", commands));
    }

    @Override
    public List<String> getUsernamesByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return getMapper().selectUsernamesByIds(userIds);
    }

}
