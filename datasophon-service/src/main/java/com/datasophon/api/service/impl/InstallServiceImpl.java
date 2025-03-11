/*
 *
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
 *
 */

package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.enums.Status;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.DispatcherWorkerActor;
import com.datasophon.api.master.HostConnectActor;
import com.datasophon.api.master.WorkerStartActor;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.InstallService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.DispatcherHostAgentCommand;
import com.datasophon.common.command.HostCheckCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.WorkerServiceMessage;
import com.datasophon.common.utils.*;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.InstallStepEntity;
import com.datasophon.dao.mapper.InstallStepMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;



import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service("installService")
public class InstallServiceImpl implements InstallService {

    private static final Logger logger = LoggerFactory.getLogger(InstallServiceImpl.class);

    @Autowired
    InstallStepMapper stepMapper;

    @Autowired
    ClusterInfoService clusterInfoService;

    @Autowired
    ClusterHostService hostService;

    private static final String SSHUSER = "SSHUSER";
    private static final String CHECK_ITEMS_CACHE_PREFIX = "CHECK_ITEMS_";

    @Override
    public Result getInstallStep(Integer type) {
        List<InstallStepEntity> list =
                stepMapper.selectList(
                        new QueryWrapper<InstallStepEntity>().eq(Constants.INSTALL_TYPE, type));
        return Result.success(list);
    }

    /**
     * 解析主机列表并进行环境检测
     * <p>
     * 处理流程：
     * 1. 获取全局变量并设置SSH用户
     * 2. 检查缓存中是否存在主机列表
     * 3. 解析主机列表（支持IP、主机名、IP域格式）
     * 4. 对未受管主机进行环境检测
     * 5. 分页返回结果
     *
     * @param clusterId   集群ID
     * @param hosts       主机列表字符串，支持格式：单个IP/主机名，逗号分隔的列表，IP域如[1-5]
     * @param sshUser     SSH用户名
     * @param sshPort     SSH端口
     * @param sshPassword SSH密码
     * @param page        当前页码
     * @param pageSize    每页大小
     * @return 分页后的主机列表结果
     */
    @Override
    public Result analysisHostList(Integer clusterId, String hosts, String sshUser, Integer sshPort, String sshPassword, Integer page, Integer pageSize) {
        // 1. 获取全局变量并设置SSH用户
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, SSHUSER, sshUser);

        // 2. 初始化结果列表和缓存key
        List<HostInfo> resultList;
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        if (Objects.isNull(clusterInfo)) {
            return Result.error("集群信息不存在");
        }

        // 3. 生成主机列表的MD5，用于缓存验证
        hosts = StringUtils.deleteWhitespace(hosts);
        String hostsMd5 = SecureUtil.md5(hosts);

        // 4. 处理主机信息Map
        Map<String, HostInfo> hostInfoMap = processHostList(clusterId, hosts, hostsMd5, sshPort, sshUser, sshPassword);

        // 5. 对结果进行排序和分页
        resultList = hostInfoMap.values().stream()
                .sorted(Comparator.comparing(HostInfo::getHostname))
                .collect(Collectors.toList());

        // 6. 计算分页参数
        Integer offset = (page - 1) * pageSize;
        List<HostInfo> pagedResult = getPagedList(resultList, offset, pageSize);

        // 异步调用startCheckItem方法
        CompletableFuture.runAsync(() -> startCheckItem(clusterId));

        return Result.success(pagedResult).put(Constants.TOTAL, resultList.size());
    }

    /**
     * 处理主机列表，返回主机信息Map
     *
     * @param clusterId   集群id
     * @param hosts       主机列表
     * @param hostsMd5    主机列表MD5
     * @param sshPort     SSH端口
     * @param sshUser     SSH用户名
     * @param sshPassword SSH密码
     * @return 主机信息Map
     */
    private Map<String, HostInfo> processHostList(Integer clusterId, String hosts, String hostsMd5,
                                                  Integer sshPort, String sshUser, String sshPassword) {
        HashMap<String, HostInfo> hostInfoMap = new HashMap<>();

        // 1. 检查缓存中是否存在有效的主机列表
        if (isCacheValid(clusterId, hostsMd5)) {
            logger.info("从缓存获取主机列表");
            return (HashMap<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        }

        logger.info("解析主机列表");
        String[] hostArray = hosts.split(Constants.COMMA);

        // 获取集群信息
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        if (Objects.isNull(clusterInfo)) {
            logger.error("集群信息不存在, clusterId: {}", clusterId);
            return hostInfoMap;
        }
        String clusterCode = clusterInfo.getClusterCode();

        // 2. 遍历处理每个主机
        for (String host : hostArray) {
            processHost(host, sshPort, sshUser, sshPassword, clusterCode, hostInfoMap);
        }

        // 3. 将结果存入缓存
        CacheUtils.put(clusterId + Constants.HOST_MAP, hostInfoMap);
        CacheUtils.put(clusterId + Constants.HOST_MD5, hostsMd5);
        logger.info("主机列表已存入缓存");

        return hostInfoMap;
    }

    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid(Integer clusterId, String hostsMd5) {
        return CacheUtils.constainsKey(clusterId + Constants.HOST_MAP)
                && CacheUtils.constainsKey(clusterId + Constants.HOST_MD5)
                && hostsMd5.equals(CacheUtils.getString(clusterId + Constants.HOST_MD5));
    }

    /**
     * 处理单个主机信息
     */
    private void processHost(String host, Integer sshPort, String sshUser, String sshPassword,
                             String clusterCode, Map<String, HostInfo> hostInfoMap) {
        // 添加参数日志
        logger.info("处理主机连接参数: host={}, sshPort={}, sshUser={}, sshPassword={}, clusterCode={}",
                host, sshPort, sshUser,
                StringUtils.isNotBlank(sshPassword) ? "******" : "null",
                clusterCode);

        // 1. 处理IP域格式 [x-y]
        if (host.contains("[") && host.contains("-")) {
            processIpRange(host, sshPort, sshUser, sshPassword, clusterCode, hostInfoMap);
            return;
        }

        // 2. 处理单个主机
        HostInfo hostInfo = createHostInfo(host, sshPort, sshUser, sshPassword, clusterCode);
        if (Objects.nonNull(hostInfo)) {
            hostInfoMap.put(hostInfo.getHostname(), hostInfo);
            if (!hostInfo.isManaged()) {
//                tellHostCheck(clusterCode, hostInfo);
            }
        }
    }

    /**
     * 处理IP范围
     */
    private void processIpRange(String host, Integer sshPort, String sshUser, String sshPassword,
                                String clusterCode, Map<String, HostInfo> hostInfoMap) {
        int start = host.indexOf("[");
        String prefix = host.substring(0, start);
        String range = host.substring(start + 1, host.length() - 1);
        String[] split = range.split("-");

        // 1. 处理字母范围，如[a-e]
        if (host.matches(Constants.HAS_EN)) {
            processLetterRange(prefix, split[0], split[1], sshPort, sshUser, sshPassword, clusterCode, hostInfoMap);
            return;
        }

        // 2. 处理数字范围，如[1-5]
        processNumberRange(prefix, split, sshPort, sshUser, sshPassword, clusterCode, hostInfoMap);
    }

    /**
     * 处理字母范围的主机名
     */
    private void processLetterRange(String prefix, String start, String end, Integer sshPort,
                                    String sshUser, String sshPassword, String clusterCode, Map<String, HostInfo> hostInfoMap) {
        List<String> hostList = PlaceholderUtils.getNewEquipmentNoList(start, end);
        for (String suffix : hostList) {
            HostInfo hostInfo = createHostInfo(prefix + suffix, sshPort, sshUser, sshPassword, clusterCode);
            if (Objects.nonNull(hostInfo)) {
                hostInfoMap.put(hostInfo.getHostname(), hostInfo);
                if (!hostInfo.isManaged()) {
//                    tellHostCheck(clusterCode, hostInfo);
                }
            }
        }
    }

    /**
     * 处理数字范围的主机名
     */
    private void processNumberRange(String prefix, String[] range, Integer sshPort,
                                    String sshUser, String sshPassword, String clusterCode, Map<String, HostInfo> hostInfoMap) {
        int start = Integer.parseInt(range[0]);
        int end = Integer.parseInt(range[1]);
        for (int i = start; i <= end; i++) {
            HostInfo hostInfo = createHostInfo(prefix + i, sshPort, sshUser, sshPassword, clusterCode);
            if (Objects.nonNull(hostInfo)) {
                hostInfoMap.put(hostInfo.getHostname(), hostInfo);
                if (!hostInfo.isManaged()) {
//                    tellHostCheck(clusterCode, hostInfo);
                }
            }
        }
    }

    /**
     * 获取分页后的列表
     */
    private List<HostInfo> getPagedList(List<HostInfo> list, Integer offset, Integer pageSize) {
        List<HostInfo> result = new ArrayList<>();
        int limit = Math.min(offset + pageSize, list.size());
        for (int i = offset; i < limit; i++) {
            result.add(list.get(i));
        }
        return result;
    }

    /**
     * 创建主机信息对象
     *
     * @param host        主机地址
     * @param sshPort     SSH端口
     * @param sshUser     SSH用户名
     * @param sshPassword SSH密码
     * @param clusterCode 集群编码
     * @return 主机信息对象
     */
    private HostInfo createHostInfo(String host, Integer sshPort, String sshUser, String sshPassword, String clusterCode) {
        HostInfo hostInfo = new HostInfo();

        // 1. 设置基本信息
        hostInfo.setHostname(HostUtils.getHostName(host));
        hostInfo.setIp(HostUtils.getIp(host));
        hostInfo.setSshPort(sshPort);
        hostInfo.setSshUser(sshUser);
        hostInfo.setSshPassword(sshPassword);
        hostInfo.setClusterCode(clusterCode);
        hostInfo.setCreateTime(new Date());

        // 2. 检查主机是否已受管
        ClusterHostDO hostEntity = hostService.getClusterHostByHostname(hostInfo.getHostname());
        if (Objects.nonNull(hostEntity)) {
            setManagedHostInfo(hostInfo);
        } else {
            setUnmanagedHostInfo(hostInfo);
        }

        return hostInfo;
    }

    /**
     * 设置已受管主机的信息
     */
    private void setManagedHostInfo(HostInfo hostInfo) {
        hostInfo.setManaged(true);
        hostInfo.setInstallState(InstallState.SUCCESS);
        hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
        hostInfo.setProgress(Constants.ONE_HUNDRRD);
        hostInfo.setCheckResult(new CheckResult(
                Status.CHECK_HOST_SUCCESS.getCode(),
                Status.CHECK_HOST_SUCCESS.getMsg()));
    }

    /**
     * 设置未受管主机的信息
     */
    private void setUnmanagedHostInfo(HostInfo hostInfo) {
        hostInfo.setManaged(false);
        hostInfo.setInstallState(InstallState.RUNNING);
        hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
        hostInfo.setProgress(0);
        hostInfo.setCheckResult(new CheckResult(
                Status.START_CHECK_HOST.getCode(),
                Status.START_CHECK_HOST.getMsg()));
    }

    private void tellHostCheck(String clusterCode, HostInfo hostInfo) {
        ActorRef actor =
                ActorUtils.getLocalActor(HostConnectActor.class, "hostActor-" + hostInfo.getHostname());
        actor.tell(new HostCheckCommand(hostInfo, clusterCode), ActorRef.noSender());
    }

    @Override
    public Result getHostCheckStatus(Integer clusterId, String sshUser, Integer sshPort) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map =
                (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
        List<HostInfo> list =
                map.entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey()))
                        .map(e -> e.getValue())
                        .collect(Collectors.toList());
        return Result.success(list);
    }

    @Override
    public Result rehostCheck(
            Integer clusterId, String hostnames, String sshUser, Integer sshPort) {
        // 开启主机校验
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map =
                (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
        for (String hostname : hostnames.split(",")) {
            if (map.containsKey(hostname)) {
                ActorRef hostActor =
                        ActorUtils.getLocalActor(HostConnectActor.class, "hostActor-" + hostname);
                HostInfo hostInfo = map.get(hostname);
                hostInfo.setCheckResult(
                        new CheckResult(
                                Status.START_CHECK_HOST.getCode(),
                                Status.START_CHECK_HOST.getMsg()));
                hostActor.tell(new HostCheckCommand(hostInfo, clusterCode), ActorRef.noSender());
            }
        }
        return Result.success();
    }

    @Override
    public Result dispatcherHostAgentList(
            Integer clusterId, Integer installStateCode, Integer page, Integer pageSize) {

        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        String distributeAgentKey =
                clusterCode + Constants.UNDERLINE + Constants.START_DISTRIBUTE_AGENT;
        Map<String, HostInfo> map =
                (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
        List<HostInfo> list =
                map.entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey()))
                        .map(e -> e.getValue())
                        .filter(e -> e.getCheckResult().getCode() == 10001)
                        .collect(Collectors.toList());

        for (HostInfo hostInfo : list) {
            if (hostInfo.isManaged()) {
                hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                hostInfo.setProgress(Constants.ONE_HUNDRRD);
                hostInfo.setMessage(MessageResolverUtils.getMessage("distribution.success"));
                hostInfo.setInstallState(InstallState.SUCCESS);
            } else if (!CacheUtils.constainsKey(
                    distributeAgentKey + Constants.UNDERLINE + hostInfo.getHostname())) {
                logger.info("start to dispatcher host agent to {}", hostInfo.getHostname());
                ActorRef hostActor =
                        ActorUtils.getLocalActor(
                                DispatcherWorkerActor.class,
                                "dispatcherWorkerActor-" + hostInfo.getHostname());
                hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
                hostInfo.setCreateTime(new Date());
                hostActor.tell(
                        new DispatcherHostAgentCommand(
                                hostInfo, clusterId, clusterInfo.getClusterFrame()),
                        ActorRef.noSender());
                // 保存主机agent分发历史
                CacheUtils.put(
                        distributeAgentKey + Constants.UNDERLINE + hostInfo.getHostname(), true);

            } else {
                long timeout =
                        DateUtil.between(hostInfo.getCreateTime(), new Date(), DateUnit.MINUTE);
                long timeOutPeriodOne = PropertyUtils.getLong("timeOutPeriodOne");
                long timeOutPeriodTwo = PropertyUtils.getLong("timeOutPeriodTwo");
                Integer progress = hostInfo.getProgress();
                if ("75".equals(String.valueOf(progress)) && timeout > timeOutPeriodOne) {
                    hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setMessage(
                            MessageResolverUtils.getMessage("distribution.fail.tips.one"));
                    hostInfo.setInstallState(InstallState.FAILED);
                }
                if (timeout > timeOutPeriodTwo) {
                    hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setInstallState(InstallState.FAILED);
                }
            }
        }
        // list分页
        Integer offset = (page - 1) * pageSize;
        List<HostInfo> result = getListPage(list, offset, pageSize);
        return Result.success(result).put(Constants.TOTAL, list.size());
    }

    @Override
    public Result reStartDispatcherHostAgent(Integer clusterId, String hostnames) {

        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map =
                (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);

        for (String hostname : hostnames.split(",")) {
            ClusterHostDO clusterHost = hostService.getClusterHostByHostname(hostname);
            HostInfo hostInfo = new HostInfo();
            if (Objects.nonNull(map) && map.containsKey(hostname)) {
                hostInfo = map.get(hostname);
            } else if (Objects.nonNull(clusterHost)) {
                hostInfo.setHostname(hostname);
                hostInfo.setSshUser("root");
                hostInfo.setSshPort(22);
            }
            ActorRef hostActor =
                    ActorUtils.getLocalActor(
                            DispatcherWorkerActor.class, "dispatcherWorkerActor-" + hostname);

            hostInfo.setInstallState(InstallState.RUNNING);
            hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
            hostInfo.setErrMsg("");
            hostInfo.setProgress(0);

            hostActor.tell(
                    new DispatcherHostAgentCommand(
                            hostInfo, clusterId, clusterInfo.getClusterFrame()),
                    ActorRef.noSender());
        }
        return Result.success();
    }

    @Override
    public Result hostCheckCompleted(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map =
                (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
        for (Map.Entry<String, HostInfo> hostInfoEntry : map.entrySet()) {
            HostInfo value = hostInfoEntry.getValue();
            if (Objects.isNull(value.getCheckResult())
                    || (Objects.nonNull(value.getCheckResult())
                    && value.getCheckResult().getCode() != 10001)) {
                return Result.success().put("hostCheckCompleted", false);
            }
        }
        return Result.success().put("hostCheckCompleted", true);
    }

    @Override
    public Result cancelDispatcherHostAgent(
            Integer clusterId, String hostname, Integer installStateCode) {

        return null;
    }

    @Override
    public Result dispatcherHostAgentCompleted(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map =
                (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
        for (Map.Entry<String, HostInfo> hostInfoEntry : map.entrySet()) {
            HostInfo hostInfo = hostInfoEntry.getValue();
            if (hostInfo.getProgress() == 75
                    && DateUtil.between(hostInfo.getCreateTime(), new Date(), DateUnit.MINUTE) > 1) {
                logger.info("dispatcher host agent timeout");
                hostInfo.setInstallState(InstallState.FAILED);
                hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                hostInfo.setErrMsg("dispatcher host agent timeout");
            }
            if (hostInfo.getInstallState() != InstallState.SUCCESS) {
                return Result.success().put("dispatcherHostAgentCompleted", false);
            }
        }
        return Result.success().put("dispatcherHostAgentCompleted", true);
    }

    @Override
    public Result generateHostAgentCommand(String clusterHostIds, String commandType) throws Exception {
        if (StringUtils.isBlank(clusterHostIds)) {
            return Result.error(Status.SELECT_LEAST_ONE_HOST.getMsg());
        }
        String[] clusterHostIdArray = clusterHostIds.split(Constants.COMMA);
        List<String> clusterHostIdList = Arrays.asList(clusterHostIdArray);
        List<ClusterHostDO> clusterHostList = hostService.getHostListByIds(clusterHostIdList);
        for (ClusterHostDO clusterHostDO : clusterHostList) {
            ClientSession session =
                    MinaUtils.openConnection(clusterHostDO.getHostname(), 22, Constants.ROOT);
            MinaUtils.execCmdWithResult(session, "service datasophon-worker " + commandType);
            logger.info("hostAgent command:{}", "service datasophon-worker " + commandType);
            if (ObjectUtil.isNotEmpty(session)) {
                session.close();
            }
        }
        return Result.success();
    }

    /**
     * 一键 启动 主机上安装的服务
     *
     * @param clusterHostIds
     * @param commandType
     * @return
     * @throws Exception
     */
    @Override
    public Result generateHostServiceCommand(String clusterHostIds, String commandType) throws Exception {
        if (StringUtils.isBlank(clusterHostIds)) {
            return Result.error(Status.SELECT_LEAST_ONE_HOST.getMsg());
        }
        String[] clusterHostIdArray = clusterHostIds.split(Constants.COMMA);
        List<ClusterHostDO> clusterHostList = hostService.getHostListByIds(Arrays.asList(clusterHostIdArray));
        Result result = null;

        CommandType serviceCommandType = "start".equalsIgnoreCase(commandType) ? CommandType.START_SERVICE : CommandType.STOP_SERVICE;
        for (ClusterHostDO clusterHostDO : clusterHostList) {
            WorkerServiceMessage serviceMessage = new WorkerServiceMessage(
                    clusterHostDO.getHostname(), clusterHostDO.getClusterId(), serviceCommandType);
            try {
                ActorRef actor =
                        ActorUtils.getLocalActor(WorkerStartActor.class, "workerStartActor");
                actor.tell(serviceMessage, ActorRef.noSender());
            } catch (Exception e) {
                logger.error("launcher worker service error!", e);
                result = Result.error("启动服务异常，Cause: " + e.getMessage());
            }
        }
        return result == null ? Result.success() : result;
    }

    private List<HostInfo> getListPage(List<HostInfo> list, Integer offset, Integer pageSize) {
        List<HostInfo> result = new ArrayList<>();
        Integer limit = offset + pageSize;
        if (list.size() < offset + pageSize) {
            limit = list.size();
        }
        for (int i = offset; i < limit; i++) {
            result.add(list.get(i));
        }
        return result;
    }

    @Override
    public Result fixCheckItem(Integer clusterId, String hostname, Integer itemId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);

        HostInfo hostInfo = map.get(hostname);
        if (Objects.isNull(hostInfo)) {
            return Result.error("主机不存在");
        }

        CheckItem targetItem = hostInfo.getCheckItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);

        if (Objects.isNull(targetItem)) {
            return Result.error("检查项不存在");
        }

        try {
            return executeFix(hostInfo, targetItem, clusterCode);
        } catch (Exception e) {
            logger.error("修复检查项失败", e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }

    @Override
    public Result fixSelectedCheckItems(Integer clusterId, String hostname, String itemIds) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);

        HostInfo hostInfo = map.get(hostname);
        if (Objects.isNull(hostInfo)) {
            return Result.error("主机不存在");
        }

        List<Integer> itemIdList = Arrays.stream(itemIds.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        List<CheckItem> itemsToFix = hostInfo.getCheckItems().stream()
                .filter(item -> itemIdList.contains(item.getId()) &&
                        item.getStatus() == CheckItem.Status.FAILED)
                .collect(Collectors.toList());

        if (itemsToFix.isEmpty()) {
            return Result.error("没有可修复的检查项");
        }

        try {
            ClientSession session = MinaUtils.openConnection(
                    hostInfo.getHostname(),
                    hostInfo.getSshPort(),
                    hostInfo.getSshUser());

            if (Objects.isNull(session)) {
                return Result.error("无法连接到主机");
            }

            // 执行所有选中的检查项修复
            for (CheckItem item : itemsToFix) {
                executeFix(hostInfo, item, clusterCode);
            }

            MinaUtils.closeConnection(session);

            return Result.success();
        } catch (Exception e) {
            logger.error("修复选中检查项失败", e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }


    public void startCheckItem(Integer clusterId) {
        // 检查是否已经在进行检查
        String checkingCacheKey = "HOST_CHECKING_" + clusterId;
        if (CacheUtils.constainsKey(checkingCacheKey)) {
            return;
        }

        try {
            // 设置检查中的缓存
            CacheUtils.put(checkingCacheKey, true);

            // 获取集群信息
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            if (Objects.isNull(clusterInfo)) {
                logger.error("集群不存在");
                return;
            }

            // 从缓存中获取主机信息
            Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
            if (Objects.isNull(map) || map.isEmpty()) {
                logger.error("主机列表缓存不存在");
                return;
            }

            // 遍历所有主机,开始检查
            for (HostInfo hostInfo : map.values()) {


            }


        } catch (Exception e) {
            logger.error("开始检查失败: ", e);
        } finally {
            // 检查完成后删除缓存
            CacheUtils.removeKey(checkingCacheKey);
        }
    }

    /**
     * 执行修复
     */
    private Result executeFix(HostInfo hostInfo, CheckItem checkItem, String clusterCode) {
        try {
            ClientSession session = MinaUtils.openConnection(
                    hostInfo.getHostname(),
                    hostInfo.getSshPort(),
                    hostInfo.getSshUser());

            if (Objects.isNull(session)) {
                return Result.error("无法连接到主机");
            }

            switch (checkItem.getId()) {
                case 1: // 主机免密检查
                    MinaUtils.execCmdWithResult(session, "mkdir -p ~/.ssh");
                    MinaUtils.execCmdWithResult(session, "chmod 700 ~/.ssh");
                    MinaUtils.execCmdWithResult(session, "[ -f ~/.ssh/id_rsa ] || ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa");
                    MinaUtils.execCmdWithResult(session, "cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys");
                    MinaUtils.execCmdWithResult(session, "chmod 600 ~/.ssh/authorized_keys");
                    break;

                case 2: // Java环境检查
                    MinaUtils.execCmdWithResult(session, "yum install -y java-1.8.0-openjdk");
                    break;

                case 3: // 最大文件句柄数检查
                    MinaUtils.execCmdWithResult(session, "echo '* soft nofile 65535\n* hard nofile 65535' >> /etc/security/limits.conf");
                    break;

                case 4: // 防火墙检查
                    MinaUtils.execCmdWithResult(session, "systemctl stop firewalld");
                    MinaUtils.execCmdWithResult(session, "systemctl disable firewalld");
                    break;

                case 5: // SELinux检查
                    MinaUtils.execCmdWithResult(session, "setenforce 0");
                    MinaUtils.execCmdWithResult(session, "sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/selinux/config");
                    break;

                case 6: // 时间同步检查
                    MinaUtils.execCmdWithResult(session, "yum install -y chrony");
                    MinaUtils.execCmdWithResult(session, "systemctl start chronyd");
                    MinaUtils.execCmdWithResult(session, "systemctl enable chronyd");
                    break;

                default:
                    return Result.error("未知的修复项");
            }

            MinaUtils.closeConnection(session);

            // 重新触发主机检查
            retriggerHostCheck(hostInfo, clusterCode);

            return Result.success();
        } catch (Exception e) {
            logger.error("执行修复失败", e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }

    private String getCheckItemsCacheKey(Integer clusterId, String hostname) {
        return CHECK_ITEMS_CACHE_PREFIX + clusterId + "_" + hostname;
    }

    private void retriggerHostCheck(HostInfo hostInfo, String clusterCode) {
        ActorRef hostActor = ActorUtils.getLocalActor(HostConnectActor.class, "hostActor-" + hostInfo.getHostname());
        hostActor.tell(new HostCheckCommand(hostInfo, clusterCode), ActorRef.noSender());
    }

    @Override
    public Result fixAllCheckItems(Integer clusterId, String hostname) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            String clusterCode = clusterInfo.getClusterCode();
            Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);

            HostInfo hostInfo = map.get(hostname);
            if (Objects.isNull(hostInfo)) {
                return Result.error("主机不存在");
            }

            // 执行所有失败的检查项修复
            for (CheckItem item : hostInfo.getCheckItems()) {
                if (item.getStatus() == CheckItem.Status.FAILED) {
                    executeFix(hostInfo, item, clusterCode);
                }
            }

            return Result.success();
        } catch (Exception e) {
            logger.error("修复所有检查项失败", e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }


    public Result getHostCheckItems(String hostname, Integer clusterId) {
        List<CheckItem> checkItems = new ArrayList<>();

        // 1. 主机免密检查
        CheckItem sshCheck = new CheckItem();
        sshCheck.setId(1);
        sshCheck.setItemName("主机免密检查");
        sshCheck.setStatus(CheckItem.Status.WAITING);
        sshCheck.setMessage("等待检查");
        checkItems.add(sshCheck);

        // 2. Java环境检查
        CheckItem javaCheck = new CheckItem();
        javaCheck.setId(2);
        javaCheck.setItemName("Java环境检查");
        javaCheck.setStatus(CheckItem.Status.WAITING);
        javaCheck.setMessage("等待检查");
        checkItems.add(javaCheck);

        // 3. 最大文件句柄数检查
        CheckItem fileHandleCheck = new CheckItem();
        fileHandleCheck.setId(3);
        fileHandleCheck.setItemName("最大文件句柄数检查");
        fileHandleCheck.setStatus(CheckItem.Status.WAITING);
        fileHandleCheck.setMessage("等待检查");
        checkItems.add(fileHandleCheck);

        // 4. 防火墙检查
        CheckItem firewallCheck = new CheckItem();
        firewallCheck.setId(4);
        firewallCheck.setItemName("防火墙检查");
        firewallCheck.setStatus(CheckItem.Status.WAITING);
        firewallCheck.setMessage("等待检查");
        checkItems.add(firewallCheck);

        // 5. SELinux检查
        CheckItem selinuxCheck = new CheckItem();
        selinuxCheck.setId(5);
        selinuxCheck.setItemName("SELinux检查");
        selinuxCheck.setStatus(CheckItem.Status.WAITING);
        selinuxCheck.setMessage("等待检查");
        checkItems.add(selinuxCheck);

        // 6. 时间同步检查
        CheckItem timeCheck = new CheckItem();
        timeCheck.setId(6);
        timeCheck.setItemName("时间同步检查");
        timeCheck.setStatus(CheckItem.Status.WAITING);
        timeCheck.setMessage("等待检查");
        checkItems.add(timeCheck);


        return Result.success(checkItems);
    }

    /**
     * 执行检查
     */
    private Result executeCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            ClientSession session = MinaUtils.openConnection(
                    hostInfo.getHostname(),
                    hostInfo.getSshPort(),
                    hostInfo.getSshUser());

            if (Objects.isNull(session)) {
                return Result.error("无法连接到主机");
            }

            boolean success = false;
            String message = "";
            String result;

            switch (checkItem.getId()) {
                case 1: // 主机免密检查
                    result = MinaUtils.execCmdWithResult(session, "ssh -o BatchMode=yes localhost echo OK");
                    success = "OK".equals(result.trim());
                    message = success ? "免密登录配置正确" : "未配置免密登录";
                    break;

                case 2: // Java环境检查
                    result = MinaUtils.execCmdWithResult(session, "which java");
                    success = result != null && !result.isEmpty();
                    message = success ? "Java环境已安装" : "未安装Java环境";
                    break;

                case 3: // 最大文件句柄数检查
                    result = MinaUtils.execCmdWithResult(session, "ulimit -n");
                    try {
                        int limit = Integer.parseInt(result.trim());
                        success = limit >= 65535;
                        message = success ? "文件句柄数配置正确" : "文件句柄数配置过低";
                    } catch (NumberFormatException e) {
                        success = false;
                        message = "无法获取文件句柄数配置";
                    }
                    break;

                case 4: // 防火墙检查
                    result = MinaUtils.execCmdWithResult(session, "systemctl status firewalld | grep Active");
                    success = result.contains("inactive") || result.contains("dead");
                    message = success ? "防火墙已关闭" : "防火墙未关闭";
                    break;

                case 5: // SELinux检查
                    result = MinaUtils.execCmdWithResult(session, "getenforce");
                    success = "Disabled".equalsIgnoreCase(result.trim());
                    message = success ? "SELinux已禁用" : "SELinux未禁用";
                    break;

                case 6: // 时间同步检查
                    result = MinaUtils.execCmdWithResult(session, "systemctl status chronyd | grep Active");
                    success = result.contains("active");
                    message = success ? "时间同步服务运行正常" : "时间同步服务未运行";
                    break;

                default:
                    return Result.error("未知的检查项");
            }

            MinaUtils.closeConnection(session);

            checkItem.setStatus(success ? CheckItem.Status.SUCCESS : CheckItem.Status.FAILED);
            checkItem.setMessage(message);

            return Result.success();
        } catch (Exception e) {
            logger.error("执行检查失败", e);
            return Result.error("检查失败: " + e.getMessage());
        }
    }
}


