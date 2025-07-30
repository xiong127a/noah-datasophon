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
import com.datasophon.api.enums.Status;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterGroupService;
import com.datasophon.api.service.ClusterUserGroupService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.api.utils.string.validator.WordValidator;
import com.datasophon.common.Constants;
import com.datasophon.common.command.LdapCommand;
import com.datasophon.common.command.remote.CreateUnixUserCommand;
import com.datasophon.common.command.remote.DelUnixUserCommand;
import com.datasophon.common.enums.UserEnum;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterGroup;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterUser;
import com.datasophon.dao.entity.ClusterUserGroup;
import com.datasophon.dao.mapper.ClusterUserMapper;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.datasophon.common.utils.OpenldapUtils.openldapProcess;

@Service("clusterUserService")
@Transactional
public class ClusterUserServiceImpl extends ServiceImpl<ClusterUserMapper, ClusterUser> implements ClusterUserService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterUserServiceImpl.class);
    @Autowired
    private ClusterGroupService groupService;

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterUserGroupService userGroupService;

    @Override
    public Result create(Integer clusterId, String username, Integer mainGroupId, String groupIds) {

        // 用户名校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        WordValidator wordValidator = new WordValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(wordValidator);
        wordValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(username);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }

        if (hasRepeatUserName(clusterId, username)) {
            return Result.error(Status.DUPLICATE_USER_NAME.getMsg());
        }
        List<ClusterHostDO> hostList = hostService.getHostListByClusterId(clusterId);

        ClusterUser clusterUser = new ClusterUser();
        clusterUser.setUsername(username);
        clusterUser.setClusterId(clusterId);
        this.save(clusterUser);
        buildClusterUserGroup(clusterId, clusterUser.getId(), mainGroupId, 1);

        String otherGroup = null;
        if (StringUtils.isNotBlank(groupIds)) {
            List<Integer> otherGroupIds = Arrays.stream(groupIds.split(",")).map(Integer::parseInt)
                    .collect(Collectors.toList());
            for (Integer id : otherGroupIds) {
                buildClusterUserGroup(clusterId, clusterUser.getId(), id, 2);
            }
            Collection<ClusterGroup> clusterGroups = groupService.listByIds(otherGroupIds);
            otherGroup = clusterGroups.stream().map(ClusterGroup::getGroupName).collect(Collectors.joining(","));
        }

        ClusterGroup mainGroup = groupService.getById(mainGroupId);
        // sync to all hosts
        for (ClusterHostDO clusterHost : hostList) {

            ActorSelection unixUserActor = ActorUtils.actorSystem.actorSelection(
                    "akka.tcp://datasophon@" + clusterHost.getHostname() + ":2552/user/worker/unixUserActor");

            CreateUnixUserCommand createUnixUserCommand = new CreateUnixUserCommand();
            createUnixUserCommand.setUsername(username);
            createUnixUserCommand.setMainGroup(mainGroup.getGroupName());
            createUnixUserCommand.setOtherGroups(otherGroup);

            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            Future<Object> execFuture = Patterns.ask(unixUserActor, createUnixUserCommand, timeout);
            ExecResult execResult;
            try {
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("create unix user {} success at {}", username, clusterHost.getHostname());
                } else {
                    logger.info(execResult.getExecOut());
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
        // akka.tcp://datasophon@hadoop1:2552/user/worker/openldapActor
        ActorRef ldapActor = ActorUtils.getRemoteActor(globalVariables.get("${openldapIp}"), "openldapActor");

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
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${syncLdapUidNumber}", "2000");
            ldapCommand.setUidNumber("2000");
        } else {
            String nextUid = NumberUtil.toStr(NumberUtil.add("2000", "1").longValue());
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${syncLdapUidNumber}", nextUid);
            ldapCommand.setUidNumber(nextUid);
        }
        ldapCommand.setGidNumber("55");

        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> execFuture = Patterns.ask(ldapActor, ldapCommand, timeout);
        ExecResult execResult;
        try {
            execResult = (ExecResult) Await.result(execFuture, timeout.duration());
            if (execResult.getExecResult()) {
                logger.info("create ldap user {} success", username);
            } else {
                logger.error("create ldap user {} failed", username);
                logger.error(execResult.getExecOut());
                logger.error(execResult.getExecErrOut());
            }
        } catch (Exception e) {
            logger.error("create ldap user {} failed", username);
            logger.error(e.getMessage());
        }

        return Result.success();
    }

    @Override
    public Result createOnKubernetes(Integer clusterId, String username, Integer mainGroupId, String groupIds) {

        // 用户名校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        WordValidator wordValidator = new WordValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(wordValidator);
        wordValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(username);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }

        if (hasRepeatUserName(clusterId, username)) {
            return Result.error(Status.DUPLICATE_USER_NAME.getMsg());
        }
        List<ClusterHostDO> hostList = hostService.getHostListByClusterId(clusterId);

        ClusterUser clusterUser = new ClusterUser();
        clusterUser.setUsername(username);
        clusterUser.setClusterId(clusterId);
        this.save(clusterUser);
        buildClusterUserGroup(clusterId, clusterUser.getId(), mainGroupId, 1);

        String otherGroup = null;
        if (StringUtils.isNotBlank(groupIds)) {
            List<Integer> otherGroupIds = Arrays.stream(groupIds.split(",")).map(Integer::parseInt)
                    .collect(Collectors.toList());
            for (Integer id : otherGroupIds) {
                buildClusterUserGroup(clusterId, clusterUser.getId(), id, 2);
            }
            Collection<ClusterGroup> clusterGroups = groupService.listByIds(otherGroupIds);
            otherGroup = clusterGroups.stream().map(ClusterGroup::getGroupName).collect(Collectors.joining(","));
        }
        ClusterGroup mainGroup = groupService.getById(mainGroupId);
        Map<String, UserEnum> userNameMap = UserEnum.getUserNameMap();
        Integer systemInitMaxUid = userNameMap.values().stream()
                .map(UserEnum::getUserId)
                .max(Integer::compareTo)
                .orElse(null);
        Integer globalMaxUid = null; // 声明一个全局变量来存储最大 UID

        for (ClusterHostDO clusterHost : hostList) {
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
            if (globalMaxUid == null || currentMaxUid > globalMaxUid) {
                globalMaxUid = currentMaxUid;
            }
        }
        Integer createUnixUserUid = Math.max(systemInitMaxUid, globalMaxUid) + 1;

        if (createUnixUserUid > 65535) {
            throw new ServiceException(500,
                    "create unix user " + username + " failed at Uid{" + createUnixUserUid + "} > 65535");
        }

        for (ClusterHostDO clusterHost : hostList) {
            ExecResult execResult = null;
            try {
                if (!createUnixUser(username, mainGroup.getGroupName(), otherGroup, clusterHost.getHostname(),
                        createUnixUserUid).equals(Constants.FAILED)) {
                    logger.info("create unix user {} success at {}", username, clusterHost.getHostname());
                } else {
                    logger.info(execResult.getExecOut());
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

        return Result.success();
    }

    private void buildClusterUserGroup(Integer clusterId, Integer userId, Integer groupId, Integer userGroupType) {
        ClusterUserGroup clusterUserGroup = new ClusterUserGroup();
        clusterUserGroup.setUserId(userId);
        clusterUserGroup.setGroupId(groupId);
        clusterUserGroup.setClusterId(clusterId);
        clusterUserGroup.setUserGroupType(userGroupType);
        userGroupService.save(clusterUserGroup);
    }

    private boolean hasRepeatUserName(Integer clusterId, String username) {
        List<ClusterUser> list = QueryChain.of(ClusterUser.class)
                .where(ClusterUser::getClusterId).eq(clusterId)
                .and(ClusterUser::getUsername).eq(username)
                .list();
        return CollUtil.isNotEmpty(list);
    }

    @Override
    public Result listPage(Integer clusterId, String username, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;

        QueryChain<ClusterUser> query = QueryChain.of(ClusterUser.class)
                .where(ClusterUser::getClusterId).eq(clusterId);

        if (StringUtils.isNotBlank(username)) {
            query.and(ClusterUser::getUsername).like("%" + username + "%");
        }

        List<ClusterUser> list = query.limit(offset, pageSize).list();

        for (ClusterUser clusterUser : list) {
            ClusterGroup mainGroup = userGroupService.queryMainGroup(clusterUser.getId());
            List<ClusterGroup> otherGroupList = userGroupService.listOtherGroups(clusterUser.getId());
            if (Objects.nonNull(otherGroupList) && !otherGroupList.isEmpty()) {
                String otherGroups = otherGroupList.stream().map(ClusterGroup::getGroupName)
                        .collect(Collectors.joining(","));
                clusterUser.setOtherGroups(otherGroups);
            }
            clusterUser.setMainGroup(mainGroup.getGroupName());
        }

        long total = query.count();
        return Result.success(list,total);
    }

    @Override
    public Result deleteClusterUser(Integer id) {
        ClusterUser clusterUser = this.getById(id);
        // delete user and group
        userGroupService.deleteByUser(id);
        List<ClusterHostDO> hostList = hostService.getHostListByClusterId(clusterUser.getClusterId());
        // sync to all hosts
        for (ClusterHostDO clusterHost : hostList) {
            ActorSelection unixUserActor = ActorUtils.actorSystem.actorSelection(
                    "akka.tcp://datasophon@" + clusterHost.getHostname() + ":2552/user/worker/unixUserActor");
            DelUnixUserCommand createUnixUserCommand = new DelUnixUserCommand();
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            createUnixUserCommand.setUsername(clusterUser.getUsername());
            Future<Object> execFuture = Patterns.ask(unixUserActor, createUnixUserCommand, timeout);
            ExecResult execResult;
            try {
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("del unix user success at {}", clusterHost.getHostname());
                } else {
                    logger.info("del unix user failed at {}", clusterHost.getHostname());
                }
            } catch (Exception e) {
                logger.info("del unix user failed at {}", clusterHost.getHostname());
            }
        }

        // delete ldap user
        Map<String, String> globalVariables = GlobalVariables.get(clusterUser.getClusterId());
        // akka.tcp://datasophon@hadoop1:2552/user/worker/openldapActor
        ActorRef ldapActor = ActorUtils.getRemoteActor(globalVariables.get("${openldapIp}"), "openldapActor");

        LdapCommand ldapCommand = new LdapCommand();
        ldapCommand.setOperation("delete");
        ldapCommand.setLdapUrl(globalVariables.get("${syncLdapUrl}"));
        ldapCommand.setRootDn(globalVariables.get("${syncLdapBindDn}"));
        ldapCommand.setLdapPwd(globalVariables.get("${syncLdapBindPassword}"));
        ldapCommand.setUsername(clusterUser.getUsername());
        ldapCommand.setUserRootDn(globalVariables.get("${syncLdapUserSearchBase}"));

        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> execFuture = Patterns.ask(ldapActor, ldapCommand, timeout);
        ExecResult execResult;
        try {
            execResult = (ExecResult) Await.result(execFuture, timeout.duration());
            if (execResult.getExecResult()) {
                logger.info("delete ldap user {} success", clusterUser.getUsername());
            } else {
                logger.error("delete ldap user {} failed", clusterUser.getUsername());
                logger.error(execResult.getExecOut());
                logger.error(execResult.getExecErrOut());
            }
        } catch (Exception e) {
            logger.error("delete ldap user {} failed", clusterUser.getUsername());
            logger.error(e.getMessage());
        }

        this.removeById(id);
        return Result.success();
    }

    @Override
    public Result deleteClusterUserOnkubernetes(Integer id) {
        ClusterUser clusterUser = this.getById(id);
        // delete user and group
        userGroupService.deleteByUser(id);
        List<ClusterHostDO> hostList = hostService.getHostListByClusterId(clusterUser.getClusterId());
        // sync to all hosts
        for (ClusterHostDO clusterHost : hostList) {
            try {
                if (!delUnixUser(clusterUser.getUsername(), clusterHost.getHostname()).equals(Constants.FAILED)) {
                    logger.info("del unix user success at {}", clusterHost.getHostname());
                } else {
                    logger.info("del unix user failed at {}", clusterHost.getHostname());
                }
            } catch (Exception e) {
                logger.info("del unix user failed at {}", clusterHost.getHostname());
            }
        }

        // delete ldap user
        Map<String, String> globalVariables = GlobalVariables.get(clusterUser.getClusterId());

        LdapCommand ldapCommand = new LdapCommand();
        ldapCommand.setOperation("delete");
        ldapCommand.setLdapUrl(globalVariables.get("${syncLdapUrl}"));
        ldapCommand.setRootDn(globalVariables.get("${syncLdapBindDn}"));
        ldapCommand.setLdapPwd(globalVariables.get("${syncLdapBindPassword}"));
        ldapCommand.setUsername(clusterUser.getUsername());
        ldapCommand.setUserRootDn(globalVariables.get("${syncLdapUserSearchBase}"));

        try {
            if (openldapProcess(ldapCommand)) {
                logger.info("delete ldap user {} success", clusterUser.getUsername());
            } else {
                logger.error("delete ldap user {} failed", clusterUser.getUsername());
            }
        } catch (Exception e) {
            logger.error("delete ldap user {} failed", clusterUser.getUsername());
            logger.error(e.getMessage());
        }

        this.removeById(id);
        return Result.success();
    }

    @Override
    public List<ClusterUser> listAllUser(Integer clusterId) {
        return QueryChain.of(ClusterUser.class)
                .where(ClusterUser::getClusterId).eq(clusterId)
                .list();
    }

    @Override
    public void createUnixUserOnHost(ClusterUser clusterUser, String hostname) {
        String username = clusterUser.getUsername();
        ClusterGroup mainGroup = userGroupService.queryMainGroup(clusterUser.getId());
        List<ClusterGroup> otherGroupList = userGroupService.listOtherGroups(clusterUser.getId());
        String otherGroup = "";
        if (Objects.nonNull(otherGroupList) && !otherGroupList.isEmpty()) {
            otherGroup = otherGroupList.stream().map(ClusterGroup::getGroupName).collect(Collectors.joining(","));
        }
        ActorSelection unixUserActor = ActorUtils.actorSystem
                .actorSelection("akka.tcp://datasophon@" + hostname + ":2552/user/worker/unixUserActor");

        CreateUnixUserCommand createUnixUserCommand = new CreateUnixUserCommand();
        createUnixUserCommand.setUsername(clusterUser.getUsername());
        createUnixUserCommand.setMainGroup(mainGroup.getGroupName());
        createUnixUserCommand.setOtherGroups(otherGroup);

        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> execFuture = Patterns.ask(unixUserActor, createUnixUserCommand, timeout);
        ExecResult execResult;
        try {
            execResult = (ExecResult) Await.result(execFuture, timeout.duration());
            if (execResult.getExecResult()) {
                logger.info("create unix user {} success at {}", username, hostname);
            } else {
                logger.info(execResult.getExecOut());
                throw new ServiceException(500, "create unix user " + username + " failed at " + hostname);
            }
        } catch (Exception e) {
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

}
