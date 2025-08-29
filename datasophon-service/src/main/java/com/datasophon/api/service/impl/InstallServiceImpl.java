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

import java.time.temporal.ChronoUnit;
import cn.hutool.core.exceptions.ExceptionUtil;
import com.datasophon.common.enums.Status;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.DispatcherWorkerActor;
import com.datasophon.api.master.WorkerStartActor;
import com.datasophon.api.converter.InstallStepConverter;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.InstallService;
import com.datasophon.api.hostvalidation.service.HostValidationService;
import com.datasophon.common.dto.HostValidationRequestDTO;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.dto.InstallStepDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.mapper.InstallStepMapper;
import com.datasophon.plugins.api.model.CommandResult;

import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.MessageResolverUtils;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.DispatcherHostAgentCommand;
import com.datasophon.common.dto.HostCheckStatusDto;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.exception.ServiceException;
import com.datasophon.common.exception.BusinessException;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.WorkerServiceMessage;
import com.datasophon.common.utils.HostUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.InstallStepEntity;
import com.datasophon.kubernetes.util.KubeUtil;
import com.datasophon.kubernetes.model.K8sNodeInfo;
import com.datasophon.api.converter.K8sToClusterHostConverter;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import com.datasophon.api.service.SshPluginAdapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 安装服务实现类
 * 负责处理集群安装流程、主机检查、Agent分发等核心业务逻辑
 * 支持传统和Kubernetes两种部署模式
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Slf4j
@Service("installService")
@RequiredArgsConstructor
public class InstallServiceImpl extends ServiceImpl<InstallStepMapper, InstallStepEntity> implements InstallService {

    // 依赖注入 - 使用构造器注入
    @Autowired
    private  ClusterInfoService clusterInfoService;
    @Autowired
    private  ClusterHostService hostService;
    @Autowired
    private  HostValidationService hostValidationService;
    @Autowired
    private  SshPluginAdapterService sshPluginAdapter;

    @Autowired
    private  K8sToClusterHostConverter k8sToClusterHostConverter;
    @Autowired
    private  InstallStepConverter installStepConverter;

    // 线程池需要特殊处理，因为有@Qualifier注解，使用字段注入
    // 临时注释掉executor依赖，避免Spring启动失败
    // @Qualifier("osInfoExecutor")
    // private final ExecutorService osInfoExecutor;

    // @Qualifier("hardwareInfoExecutor")
    // private final ExecutorService hardwareInfoExecutor;

    @Override
    public List<InstallStepDTO> getInstallStepsByType(Integer installType) {
        try {
            if (installType == null) {
                throw new RuntimeException("安装类型不能为空");
            }

            List<InstallStepEntity> entities = getMapper().selectByInstallType(installType);
            return installStepConverter.entityListToDtoList(entities);
        } catch (Exception e) {
            log.error("根据类型获取安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("根据类型获取安装步骤失败: " + e.getMessage());
        }
    }

    @Override
    public InstallStepDTO getInstallStepById(Long id) {
        try {
            if (id == null) {
                throw new RuntimeException("安装步骤ID不能为空");
            }

            InstallStepEntity entity = getById(id);
            if (entity == null) {
                throw new RuntimeException("未找到ID为 " + id + " 的安装步骤");
            }

            return installStepConverter.entityToDto(entity);
        } catch (Exception e) {
            log.error("根据ID获取安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("根据ID获取安装步骤失败: " + e.getMessage());
        }
    }

    @Override
    public InstallStepDTO getInstallStep(Integer type) {
        try {
            if (type == null) {
                throw new RuntimeException("安装类型不能为空");
            }

            List<InstallStepDTO> steps = getInstallStepsByType(type);
            if (steps == null || steps.isEmpty()) {
                throw new RuntimeException("未找到类型为 " + type + " 的安装步骤");
            }

            // 返回第一个匹配的安装步骤
            return steps.getFirst();
        } catch (Exception e) {
            log.error("根据类型获取安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("根据类型获取安装步骤失败: " + e.getMessage());
        }
    }

    @Override
    public InstallStepDTO saveInstallStep(InstallStepDTO installStepDTO) {
        try {
            if (installStepDTO == null) {
                throw new RuntimeException("安装步骤信息不能为空");
            }

            InstallStepEntity entity = installStepConverter.dtoToEntity(installStepDTO);
            boolean result = save(entity);

            if (!result) {
                throw new RuntimeException("保存安装步骤失败");
            }

            return installStepConverter.entityToDto(entity);
        } catch (Exception e) {
            log.error("保存安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存安装步骤失败: " + e.getMessage());
        }
    }

    @Override
    public InstallStepDTO updateInstallStep(InstallStepDTO installStepDTO) {
        try {
            if (installStepDTO == null || installStepDTO.id() == null) {
                throw new RuntimeException("安装步骤信息或ID不能为空");
            }

            // 检查记录是否存在
            InstallStepEntity existingEntity = getById(installStepDTO.id());
            if (existingEntity == null) {
                throw new RuntimeException("未找到ID为 " + installStepDTO.id() + " 的安装步骤");
            }

            InstallStepEntity entity = installStepConverter.dtoToEntity(installStepDTO);
            boolean result = updateById(entity);

            if (!result) {
                throw new RuntimeException("更新安装步骤失败");
            }

            return installStepConverter.entityToDto(entity);
        } catch (Exception e) {
            log.error("更新安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新安装步骤失败: " + e.getMessage());
        }
    }

    @Override
    public boolean removeInstallStepByIds(List<Integer> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                throw new RuntimeException("删除的ID列表不能为空");
            }

            return removeByIds(ids);
        } catch (Exception e) {
            log.error("批量删除安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量删除安装步骤失败: " + e.getMessage());
        }
    }

    /**
     * 解析主机列表并进行环境检测
     * <p>
     * 处理流程：
     * 1. 获取集群信息，判断集群类型（K8S或传统）
     * 2. K8S模式：从K8S API获取节点列表和架构信息
     * 3. 传统模式：解析用户输入的主机列表并进行SSH检测
     * 4. 分页返回结果
     *
     * @param clusterId   集群ID
     * @param ips         主机列表字符串（K8S模式下可为空）
     * @param sshUser     SSH用户名（K8S模式下可为空）
     * @param sshPort     SSH端口（K8S模式下可为空）
     * @param sshPassword SSH密码（K8S模式下可为空）
     * @param page        当前页码
     * @param pageSize    每页大小
     * @return 分页后的主机列表结果
     */
    @Override
    public PageResult<HostInfo> analysisHostList(Long clusterId, String ips, String sshUser, Integer sshPort,
                                                 String sshPassword, String kubeConfigContent, Integer page, Integer pageSize) {
        try {
            // 获取集群信息以判断集群类型
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            if (clusterInfo == null) {
                throw new ServiceException("集群不存在");
            }
            ClusterType depType = clusterInfo.getDepType();
            log.info("集群ID: {}, 部署类型: {}", clusterId, depType);

            // 根据部署类型路由到不同的处理方法
            if (depType != null && depType.isKubernetes()) {
                log.info("检测到Kubernetes集群，使用K8S API获取节点列表");
                return analysisHostListForKubernetes(clusterId, kubeConfigContent, page, pageSize);
            } else {
                log.info("检测到传统集群，使用SSH方式获取主机列表");
                return analysisHostListForTraditional(clusterId, ips, sshUser, sshPort, sshPassword, page, pageSize);
            }

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析主机列表失败: {}", ExceptionUtil.getSimpleMessage(e));
            throw new ServiceException("解析主机列表失败: " + e.getMessage());
        }
    }

    /**
     * 传统集群模式的主机列表解析 (重构为插件化架构)
     */
    private PageResult<HostInfo> analysisHostListForTraditional(Long clusterId, String ips, String sshUser,
            Integer sshPort,
            String sshPassword,
            Integer page, Integer pageSize) {
        try {
            if (StringUtils.isBlank(ips)) {
                throw new ServiceException("主机列表不能为空");
            }

            log.info("使用插件化架构进行主机验证: 集群ID={}, 主机数量={}, 用户={}, 端口={}", 
                    clusterId, ips.split("[,\\n]").length, sshUser, sshPort);

            // 解析主机列表
            List<String> hostIpList = parseHostList(ips);
            
            // 使用新的插件化验证架构
            Map<String, HostInfo> hostMap = startPluginBasedValidation(
                    clusterId.toString(), hostIpList, sshUser, sshPort, sshPassword);

            // 如果没有获取到主机信息,返回错误提示
            List<HostInfo> hostList = new ArrayList<>(hostMap.values());

            // 使用HostUtils的统一排序方法对IP进行排序
            List<String> sortedIps = HostUtils.sortIpAddresses(hostList.stream()
                    .map(HostInfo::getIp)
                    .collect(Collectors.toList()));

            // 按照排序后的IP顺序重新组织主机列表
            List<HostInfo> sortedHostList = new ArrayList<>();
            for (String ip : sortedIps) {
                hostList.stream()
                        .filter(host -> ip.equals(host.getIp()))
                        .findFirst()
                        .ifPresent(sortedHostList::add);
            }

            // 使用排序后的主机列表
            hostList = sortedHostList;

            // 计算每个主机的状态
            hostList.forEach(hostInfo -> {
                processHostInfo(hostInfo);
                hostInfo.calculateStatus();
            });

            // 分页处理
            int offset = (page - 1) * pageSize;
            int end = Math.min(offset + pageSize, hostList.size());
            List<HostInfo> pagedHosts = hostList.subList(offset, end);

            return PageResult.of(pagedHosts, hostList.size(), page, pageSize);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("传统集群主机列表解析失败: {}", ExceptionUtil.getSimpleMessage(e));
            throw new ServiceException("传统集群主机列表解析失败: " + e.getMessage());
        }
    }

    /**
     * 处理主机信息，确保各项信息可用于前端展示
     *
     * @param hostInfo 主机信息对象
     */
    private void processHostInfo(HostInfo hostInfo) {

    }

    /**
     * 格式化SSH错误消息，使其更用户友好
     */
    private String formatSshErrorMessage(Exception e) {
        String errorMsg = e.getMessage();
        if (errorMsg == null) {
            errorMsg = e.getClass().getSimpleName();
        }

        // 根据异常类型和错误消息设置友好的错误提示
        String friendlyMessage;
        String errorCode;
        String solution;

        if (errorMsg.contains("Auth fail") || errorMsg.contains("authentication failed")) {
            friendlyMessage = "用户名或密码错误";
            errorCode = "SSH_AUTH_ERROR";
            solution = "请检查SSH用户名和密码是否正确";
        } else if (errorMsg.contains("Connection refused")) {
            friendlyMessage = "SSH服务未启动或端口未开放";
            errorCode = "SSH_CONNECTION_REFUSED";
            solution = "请确认SSH服务已启动，端口(默认22)已开放，并检查防火墙设置";
        } else if (errorMsg.contains("connect timed out")) {
            friendlyMessage = "连接超时，网络不通或防火墙阻止";
            errorCode = "SSH_TIMEOUT";
            solution = "请检查网络连接和防火墙设置，确保主机可访问";
        } else if (errorMsg.contains("UnknownHostException")) {
            friendlyMessage = "无法解析主机名";
            errorCode = "SSH_UNKNOWN_HOST";
            solution = "请检查DNS配置或hosts文件，确保主机名可解析";
        } else if (errorMsg.contains("No route to host")) {
            friendlyMessage = "无法访问主机";
            errorCode = "SSH_NO_ROUTE";
            solution = "请检查网络连接，确保主机已启动且网络可达";
        } else if (errorMsg.contains("Too many authentication failures")) {
            friendlyMessage = "认证失败次数过多";
            errorCode = "SSH_TOO_MANY_AUTH_FAILURES";
            solution = "请等待一段时间后重试，或尝试使用密钥认证";
        } else if (errorMsg.contains("Permission denied")) {
            friendlyMessage = "权限被拒绝";
            errorCode = "SSH_PERMISSION_DENIED";
            solution = "请检查用户权限或尝试使用sudo权限的用户";
        } else if (errorMsg.contains("Host key verification failed")) {
            friendlyMessage = "主机密钥验证失败";
            errorCode = "SSH_HOST_KEY_VERIFICATION_FAILED";
            solution = "主机密钥已更改，请更新known_hosts文件";
        } else {
            friendlyMessage = errorMsg;
            errorCode = "SSH_UNKNOWN_ERROR";
            solution = "请检查SSH服务配置和网络连接";
        }

        // 返回完整原始错误信息，便于调试
        String originalError = errorMsg;

        // 构建结构化的错误信息
        return String.format("%s [%s] - %s (原始错误: %s)",
                friendlyMessage,
                errorCode,
                solution,
                originalError);
    }

    /**
     * 验证SSH连接
     *
     * @param hostInfo 主机信息
     * @return 连接是否成功
     */
    private boolean validateSshConnection(HostInfo hostInfo) {
        try {
            log.debug("【安装服务】开始验证SSH连接: {}@{}:{}", 
                    hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort());
            
            // 通过SSH插件适配器测试连接
            CommandResult connectionTestResult = sshPluginAdapter.testConnection(hostInfo);
            
            if (connectionTestResult.isSuccess()) {
                // 连接成功，执行测试命令验证功能
                String testOutput = sshPluginAdapter.executeCommand(hostInfo, "echo connection_test");
                
                boolean commandSuccess = testOutput != null && testOutput.contains("connection_test");
                
                if (commandSuccess) {
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
                    log.info("【安装服务】SSH连接验证成功: {}", hostInfo.getIp());
                    return true;
                } else {
                    // 连接成功但命令执行失败
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setSshErrorMsg("SSH连接成功但无法执行命令，请检查用户权限");
                    hostInfo.setErrorMessage("SSH连接成功但无法执行命令，请检查用户权限");
                    log.warn("【安装服务】SSH连接成功但命令执行失败: {}", hostInfo.getIp());
                    return false;
                }
            } else {
                // 连接失败
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                
                String errorMsg = connectionTestResult.error() != null ? 
                        connectionTestResult.error() : "无法创建SSH连接，请检查网络连接和SSH配置";
                
                hostInfo.setSshErrorMsg(errorMsg);
                hostInfo.setErrorMessage("SSH连接失败：" + errorMsg);
                
                log.error("【安装服务】SSH连接验证失败: {} -> {}", hostInfo.getIp(), errorMsg);
                return false;
            }
            
        } catch (Exception e) {
            // 处理异常，设置错误信息
            hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
            
            String formattedErrorMsg = formatSshErrorMessage(e);
            hostInfo.setSshErrorMsg(formattedErrorMsg);
            hostInfo.setErrorMessage("SSH连接失败：" + formattedErrorMsg);
            
            log.error("【安装服务】SSH连接验证异常: {} -> {}", hostInfo.getIp(), formattedErrorMsg, e);
            return false;
        }
    }

    /**
     * 处理主机列表
     *
     * @param clusterId   集群ID
     * @param hosts       主机列表字符串
     * @param sshPort     SSH端口
     * @param sshUser     SSH用户名
     * @param sshPassword SSH密码
     * @return 主机信息映射，IP为键
     */
    private Map<String, HostInfo> processHostList(Long clusterId, String hosts, Integer sshPort,
            String sshUser, String sshPassword) {
        HashMap<String, HostInfo> hostInfoMap = new HashMap<>();

        log.info("解析主机列表");
        String[] hostArray = hosts.split(Constants.COMMA);

        // 2. 遍历处理每个主机，将结果存入临时Map
        Map<String, HostInfo> tempMap = new HashMap<>();
        for (String host : hostArray) {
            // 只创建主机信息，不执行异步获取操作系统信息
            processHostWithoutOsInfo(host, sshPort, sshUser, sshPassword, clusterId, tempMap);
        }

        // List<CheckItem> checkItems = hostCheckService.getHostCheckItems();
        List<CheckItem> checkItems = null;
        // 3. 将所有主机信息添加到返回结果中，以IP为键
        for (HostInfo hostInfo : tempMap.values()) {
            // 获取主机检查项列表
            hostInfo.setCheckItems(checkItems);

            // 初始化SSH连接状态为loading，而不是同步验证
            hostInfo.setSshConnectStatus(OsInfoStatusEnum.LOADING);

            // 使用IP作为键
            hostInfoMap.put(hostInfo.getIp(), hostInfo);
        }

        return hostInfoMap;
    }

    /**
     * 处理单个主机信息（不执行异步获取操作系统信息）
     */
    private void processHostWithoutOsInfo(String host, Integer sshPort, String sshUser, String sshPassword,
            Long clusterId,
            Map<String, HostInfo> hostInfoMap) {
        // 1. 处理IP范围，如192.168.1.[1-5]
        if (host.contains("[") && host.contains("]")) {
            processIpRangeWithoutOsInfo(host, sshPort, sshUser, sshPassword, clusterId, hostInfoMap);
            return;
        }

        // 2. 普通主机，直接创建主机信息
        HostInfo hostInfo = createHostInfo(host, sshPort, sshUser, sshPassword, clusterId);
        hostInfoMap.put(hostInfo.getIp(), hostInfo);
    }

    /**
     * 处理IP范围（不执行异步获取操作系统信息）
     */
    private void processIpRangeWithoutOsInfo(String host, Integer sshPort, String sshUser, String sshPassword,
            Long clusterId,
            Map<String, HostInfo> hostInfoMap) {
        int start = host.indexOf("[");
        String prefix = host.substring(0, start);
        String range = host.substring(start + 1, host.length() - 1);
        String[] split = range.split("-");

        // 1. 处理字母范围，如[a-e]
        if (host.matches(Constants.HAS_EN)) {
            processLetterRangeWithoutOsInfo(prefix, split[0], split[1], sshPort, sshUser, sshPassword, clusterId,
                    hostInfoMap);
            return;
        }

        // 2. 处理数字范围，如[1-5]
        processNumberRangeWithoutOsInfo(prefix, split, sshPort, sshUser, sshPassword, clusterId, hostInfoMap);
    }

    /**
     * 处理字母范围的主机名（不执行异步获取操作系统信息）
     */
    private void processLetterRangeWithoutOsInfo(String prefix, String start, String end, Integer sshPort,
            String sshUser,
            String sshPassword, Long clusterId, Map<String, HostInfo> hostInfoMap) {
        List<String> hostList = PlaceholderUtils.getNewEquipmentNoList(start, end);
        for (String suffix : hostList) {
            HostInfo hostInfo = createHostInfo(prefix + suffix, sshPort, sshUser, sshPassword, clusterId);
            hostInfoMap.put(hostInfo.getIp(), hostInfo);
        }
    }

    private void processNumberRangeWithoutOsInfo(String prefix, String[] range, Integer sshPort, String sshUser,
            String sshPassword,
            Long clusterId, Map<String, HostInfo> hostInfoMap) {
        int start = Integer.parseInt(range[0]);
        int end = Integer.parseInt(range[1]);
        for (int i = start; i <= end; i++) {
            HostInfo hostInfo = createHostInfo(prefix + i, sshPort, sshUser, sshPassword, clusterId);
            hostInfoMap.put(hostInfo.getIp(), hostInfo);
        }
    }

    /**
     * 创建主机信息对象
     *
     * @param host        主机地址
     * @param sshPort     SSH端口
     * @param sshUser     SSH用户名
     * @param sshPassword SSH密码
     * @return 主机信息对象
     */
    private HostInfo createHostInfo(String host, Integer sshPort, String sshUser, String sshPassword,
            Long clusterId) {
        HostInfo hostInfo = new HostInfo();

        hostInfo.setHostname(HostUtils.getHostName(host));
        hostInfo.setIp(HostUtils.getIpByHost(host));
        // 1. 设置基本信息
        hostInfo.setIp(HostUtils.getIp(host));
        hostInfo.setSshPort(sshPort);
        hostInfo.setSshUser(sshUser);
        hostInfo.setSshPassword(sshPassword);
        hostInfo.setClusterId(clusterId);
        hostInfo.setCreateTime(LocalDateTime.now());

        // 初始化错误信息字段
        hostInfo.setSshErrorMsg("");
        hostInfo.setErrorMessage("");

        // 2. 检查主机是否已受管
        ClusterHostEntity hostEntity = hostService.getClusterHostByHostname(hostInfo.getHostname());
        if (Objects.nonNull(hostEntity)) {
            setManagedHostInfo(hostInfo);
        } else {
            setUnmanagedHostInfo(hostInfo);
        }

        return hostInfo;
    }

    // getOrCreateSession方法已删除 - SSH连接现在通过插件适配器管理

    /**
     * 设置已受管主机的信息
     */
    private void setManagedHostInfo(HostInfo hostInfo) {
        hostInfo.setManaged(true);
        hostInfo.setInstallState(InstallState.SUCCESS);
        hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
        hostInfo.setProgress(Constants.ONE_HUNDRRD);
        hostInfo.setCheckResult(
                new CheckResult(Status.CHECK_HOST_SUCCESS.getCode(), Status.CHECK_HOST_SUCCESS.getMsg()));
    }

    /**
     * 设置未受管主机的信息
     */
    private void setUnmanagedHostInfo(HostInfo hostInfo) {
        hostInfo.setManaged(false);
        hostInfo.setInstallState(InstallState.RUNNING);
        hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
        hostInfo.setProgress(0);
        hostInfo.setCheckResult(new CheckResult(Status.START_CHECK_HOST.getCode(), Status.START_CHECK_HOST.getMsg()));
    }

    @Override
    public HostCheckStatusDto getHostCheckStatus(Long clusterId, String sshUser, Integer sshPort) {
        try {
            // 获取检查结果列表
            Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
            List<HostInfo> list = map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .toList();

            // 统计检查状态
            int totalHosts = list.size();
            int completedHosts = 0;
            int failedHosts = 0;
            int successHosts = 0;
            boolean allCompleted = true;

            for (HostInfo hostInfo : list) {
                CheckItem.Status status = hostInfo.getStatus();
                if (status == CheckItem.Status.SUCCESS) {
                    completedHosts++;
                    successHosts++;
                } else if (status == CheckItem.Status.FAILED) {
                    completedHosts++;
                    failedHosts++;
                } else if (status == CheckItem.Status.CHECKING || status == CheckItem.Status.WAITING) {
                    allCompleted = false;
                }
            }

            return HostCheckStatusDto.builder()
                    .hosts(list)
                    .completed(allCompleted)
                    .totalHosts(totalHosts)
                    .completedHosts(completedHosts)
                    .failedHosts(failedHosts)
                    .successHosts(successHosts)
                    .build();
        } catch (Exception e) {
            log.error("获取主机检查状态失败", e);
            throw new ServiceException("获取主机检查状态失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<HostInfo> dispatcherHostAgentList(
            Long clusterId, Integer installStateCode, Integer page, Integer pageSize) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            String clusterCode = clusterInfo.getClusterCode();
            String distributeAgentKey = clusterCode + Constants.UNDERLINE + Constants.START_DISTRIBUTE_AGENT;
            @SuppressWarnings("unchecked")
            Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
            List<HostInfo> list = map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .filter(e -> e.getCheckResult().getCode() == 10001)
                    .toList();

            for (HostInfo hostInfo : list) {
                if (hostInfo.isManaged()) {
                    hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setMessage(MessageResolverUtils.getMessage("distribution.success"));
                    hostInfo.setInstallState(InstallState.SUCCESS);
                } else if (!CacheUtils.constainsKey(distributeAgentKey + Constants.UNDERLINE + hostInfo.getIp())) {
                    log.info("start to dispatcher host agent to {}", hostInfo.getIp());
                    ActorRef hostActor = ActorUtils.getLocalActor(DispatcherWorkerActor.class,
                            "dispatcherWorkerActor-" + hostInfo.getIp());
                    hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
                    hostInfo.setCreateTime(LocalDateTime.now());
                    hostActor.tell(new DispatcherHostAgentCommand(hostInfo, clusterId, clusterInfo.getClusterFrame()),
                            ActorRef.noSender());
                    // 保存主机agent分发历史
                    CacheUtils.put(distributeAgentKey + Constants.UNDERLINE + hostInfo.getIp(), true);

                } else {
                    long timeout = ChronoUnit.MINUTES.between(hostInfo.getCreateTime(), LocalDateTime.now());
                    long timeOutPeriodOne = PropertyUtils.getLong("timeOutPeriodOne");
                    long timeOutPeriodTwo = PropertyUtils.getLong("timeOutPeriodTwo");
                    Integer progress = hostInfo.getProgress();
                    if ("75".equals(String.valueOf(progress)) && timeout > timeOutPeriodOne) {
                        hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                        hostInfo.setProgress(Constants.ONE_HUNDRRD);
                        hostInfo.setMessage(MessageResolverUtils.getMessage("distribution.fail.tips.one"));
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
            return PageResult.of(result, list.size(), page, pageSize);
        } catch (Exception e) {
            log.error("获取主机代理分发列表失败", e);
            throw new ServiceException("获取主机代理分发列表失败: " + e.getMessage());
        }
    }

    @Override
    public boolean reStartDispatcherHostAgent(Long clusterId, String ips) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

            for (String ip : ips.split(",")) {
                // 不使用不存在的getClusterHostByIp方法
                HostInfo hostInfo = new HostInfo();
                boolean foundInMap = false;

                // 在缓存map中查找匹配IP的主机信息
                for (Map.Entry<String, HostInfo> entry : map.entrySet()) {
                    HostInfo hi = entry.getValue();
                    if (hi != null && ip.equals(hi.getIp())) {
                        hostInfo = hi;
                        foundInMap = true;
                        break;
                    }
                }

                // 如果在缓存中没找到，则使用提供的IP构建基本信息
                if (!foundInMap) {
                    hostInfo.setIp(ip);
                    hostInfo.setSshUser("root");
                    hostInfo.setSshPort(22);
                    hostInfo.setHostname(ip); // 使用IP作为hostname
                }

                ActorRef hostActor = ActorUtils.getLocalActor(DispatcherWorkerActor.class,
                        "dispatcherWorkerActor-" + hostInfo.getHostname());

                hostInfo.setInstallState(InstallState.RUNNING);
                hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
                hostInfo.setErrMsg("");
                hostInfo.setProgress(0);

                hostActor.tell(new DispatcherHostAgentCommand(hostInfo, clusterId, clusterInfo.getClusterFrame()),
                        ActorRef.noSender());
            }
            return true;
        } catch (Exception e) {
            log.error("重启主机代理分发失败", e);
            throw new BusinessException(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), "重启主机代理分发失败: " + e.getMessage());
        }
    }

    @Override
    public boolean hostCheckCompleted(Long clusterId) {
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

        // 收集未通过检查的主机信息
        List<Map<String, Object>> failedHosts = new ArrayList<>();
        Map<String, List<String>> hostToFailedItems = new HashMap<>();

        // 检查是否存在未完成的主机
        for (Map.Entry<String, HostInfo> hostInfoEntry : map.entrySet()) {
            HostInfo hostInfo = hostInfoEntry.getValue();

            // 检查主机整体状态
            CheckResult checkResult = hostInfo.getCheckResult();
            if (checkResult == null) {
                Map<String, Object> failInfo = new HashMap<>();
                failInfo.put("hostname", hostInfo.getIp());
                failInfo.put("reason", "主机整体检查状态未完成");
                failInfo.put("code",
                        Objects.nonNull(hostInfo.getCheckResult()) ? hostInfo.getCheckResult().getCode() : "未知");
                failedHosts.add(failInfo);

                // 添加到错误映射
                List<String> failedItemsList = new ArrayList<>();
                failedItemsList.add("检查未完成");
                hostToFailedItems.put(hostInfo.getIp(), failedItemsList);

                log.info("主机 {} 的整体检查状态未完成，主机检查不通过", hostInfo.getIp());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean cleanupHostCheckResources(Long clusterId) {
        try {
            if (clusterId == null) {
                log.error("集群ID为空，无法清理主机检查资源");
                throw new BusinessException(Status.REQUEST_PARAMS_NOT_VALID_ERROR.getCode(), "集群ID不能为空");
            }

            log.info("开始清理集群[{}]的主机检查资源", clusterId);

            // 1. 删除缓存中当前集群ID的所有日志
            // 主要的日志前缀模式
            String[] logPrefixes = {
                    "CHECK_ITEM_LOG_" + clusterId + "_", // 检查项日志
                    "CHECK_LOG_" + clusterId + "_", // 检查操作日志
                    "FIX_LOG_" + clusterId + "_", // 修复操作日志
                    "CHECK_TASK_STATUS_" + clusterId + "_" // 任务状态
            };

            // 获取所有的缓存键
            // 由于没有直接获取所有键的方法，我们需要基于前缀前缀模式清理
            for (String prefix : logPrefixes) {
                log.info("清理前缀为[{}]的日志缓存", prefix);
                // 注意：这里无法遍历所有键，所以我们只能在后续操作中处理相关键
            }

            // 通用日志缓存键
            String logCacheKey = clusterId + "_HOST_CHECK_LOG";
            if (CacheUtils.constainsKey(logCacheKey)) {
                CacheUtils.removeKey(logCacheKey);
                log.info("已清理集群[{}]的主机检查日志缓存", clusterId);
            }

            // 2. 清理其他与检查相关的缓存
            String hostMapKey = clusterId + Constants.HOST_MAP;
            if (CacheUtils.constainsKey(hostMapKey)) {
                // 在清理前，获取所有主机信息，用于清理特定主机的日志
                Map<String, HostInfo> hostMap = CacheUtils.getHostMap(hostMapKey);
                for (Map.Entry<String, HostInfo> entry : hostMap.entrySet()) {
                    String hostname = entry.getKey();
                    HostInfo hostInfo = entry.getValue();

                    // 清理该主机的所有检查项日志
                    if (hostInfo.getCheckItems() != null) {
                        for (CheckItem item : hostInfo.getCheckItems()) {
                            // 删除该主机该检查项的所有日志
                            for (String prefix : logPrefixes) {
                                String itemLogKey = prefix + hostname + "_" + item.getId();
                                if (CacheUtils.constainsKey(itemLogKey)) {
                                    CacheUtils.removeKey(itemLogKey);
                                    log.debug("已清理日志: {}", itemLogKey);
                                }
                            }
                        }
                    }
                }
            }
        } catch (BusinessException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public boolean cancelDispatcherHostAgent(Long clusterId, String ip, Integer installStateCode) {
        // 此方法虽然定义但实际未使用
        log.warn("cancelDispatcherHostAgent方法暂未实现具体逻辑");
        return false;
    }

    @Override
    public boolean dispatcherHostAgentCompleted(Long clusterId) {
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        for (Map.Entry<String, HostInfo> hostInfoEntry : map.entrySet()) {
            HostInfo hostInfo = hostInfoEntry.getValue();
            if (hostInfo.getProgress() == 75
                    && ChronoUnit.MINUTES.between(hostInfo.getCreateTime(), LocalDateTime.now()) > 1) {
                log.info("dispatcher host agent timeout");
                hostInfo.setInstallState(InstallState.FAILED);
                hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                hostInfo.setErrMsg("dispatcher host agent timeout");
            }
            if (hostInfo.getInstallState() != InstallState.SUCCESS) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Map<String, Object>> generateHostAgentCommand(String clusterHostIds, String commandType) {
        if (StringUtils.isBlank(clusterHostIds)) {
            throw new BusinessException(Status.SELECT_LEAST_ONE_HOST.getCode(), Status.SELECT_LEAST_ONE_HOST.getMsg());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        String[] clusterHostIdArray = clusterHostIds.split(Constants.COMMA);
        List<String> clusterHostIdList = Arrays.asList(clusterHostIdArray);
        List<ClusterHostEntity> clusterHostList = hostService.getHostListByIds(clusterHostIdList);

        for (ClusterHostEntity clusterHostEntity : clusterHostList) {
            Map<String, Object> result = new HashMap<>();
            result.put("hostname", clusterHostEntity.getHostname());
            result.put("ip", clusterHostEntity.getIp());
            result.put("command", "service datasophon-worker " + commandType);

            try {
                // 通过SSH插件适配器执行命令
                HostInfo hostInfo = new HostInfo(clusterHostEntity.getIp(), 22, Constants.ROOT);
                String command = "service datasophon-worker " + commandType;
                
                String commandResult = sshPluginAdapter.executeCommand(hostInfo, command);
                result.put("success", true);
                result.put("output", commandResult);
                log.info("【安装服务】主机代理命令执行成功: {} -> {}", clusterHostEntity.getIp(), commandResult);
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", e.getMessage());
                log.error("【安装服务】主机代理命令执行失败: {} -> {}", clusterHostEntity.getIp(), e.getMessage());
            }
            results.add(result);
        }
        return results;
    }

    /**
     * 一键 启动 主机上安装的服务
     *
     */
    @Override
    public List<Map<String, Object>> generateHostServiceCommand(String clusterHostIds, String commandType) {
        if (StringUtils.isBlank(clusterHostIds)) {
            throw new BusinessException(Status.SELECT_LEAST_ONE_HOST.getCode(), Status.SELECT_LEAST_ONE_HOST.getMsg());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        String[] clusterHostIdArray = clusterHostIds.split(Constants.COMMA);
        List<ClusterHostEntity> clusterHostList = hostService.getHostListByIds(Arrays.asList(clusterHostIdArray));

        CommandType serviceCommandType = "start".equalsIgnoreCase(commandType) ? CommandType.START_SERVICE
                : CommandType.STOP_SERVICE;

        for (ClusterHostEntity clusterHostEntity : clusterHostList) {
            Map<String, Object> result = new HashMap<>();
            result.put("hostname", clusterHostEntity.getHostname());
            result.put("ip", clusterHostEntity.getIp());
            result.put("commandType", serviceCommandType.toString());

            WorkerServiceMessage serviceMessage = new WorkerServiceMessage(clusterHostEntity.getHostname(),
                    clusterHostEntity.getClusterId(), serviceCommandType);
            try {
                ActorRef actor = ActorUtils.getLocalActor(WorkerStartActor.class, "workerStartActor");
                actor.tell(serviceMessage, ActorRef.noSender());
                result.put("success", true);
                result.put("message", "服务命令已发送");
                log.info("Service command sent successfully to {}: {}", clusterHostEntity.getHostname(),
                        serviceCommandType);
            } catch (Exception e) {
                log.error("launcher worker service error!", e);
                result.put("success", false);
                result.put("error", "启动服务异常，Cause: " + e.getMessage());
            }
            results.add(result);
        }
        return results;
    }

    /**
     * Kubernetes集群模式的主机列表解析
     * 从K8S API获取节点信息，包括CPU架构
     */
    private PageResult<HostInfo> analysisHostListForKubernetes(Long clusterId, String kubeConfig, Integer page,
            Integer pageSize) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

            if (kubeConfig == null || kubeConfig.trim().isEmpty()) {
                throw new ServiceException("Kubernetes配置不能为空，请先完成集群配置");
            }

            log.info("开始从Kubernetes API获取节点列表，集群ID: {}", clusterId);

            // 从Kubernetes集群获取节点列表
            List<K8sNodeInfo> k8sNodeInfoList = KubeUtil.getHostListByConfig(kubeConfig);
            List<ClusterHostEntity> kubernetesHosts = k8sToClusterHostConverter.convertToClusterHostList(k8sNodeInfoList,
                    clusterId);

            if (kubernetesHosts.isEmpty()) {
                log.warn("未从Kubernetes集群获取到任何节点");
                throw new ServiceException("未找到任何Kubernetes节点，请检查集群配置");
            }

            log.info("从Kubernetes API获取到 {} 个节点", kubernetesHosts.size());

            // 转换为HostInfo格式
            List<HostInfo> hostInfoList = new ArrayList<>();
            String clusterCode = clusterInfo.getClusterCode();

            // 获取检查项列表（K8S模式下也需要检查项用于环境验证）
            // List<CheckItem> checkItems = hostCheckService.getHostCheckItems();
            List<CheckItem> checkItems = null;
            // 保存从K8S API获取的完整节点信息，用于后续保存
            List<ClusterHostEntity> kubernetesHostsForSave = new ArrayList<>();

            for (ClusterHostEntity kubernetesHost : kubernetesHosts) {
                HostInfo hostInfo = new HostInfo();
                hostInfo.setHostname(kubernetesHost.getHostname());
                hostInfo.setIp(kubernetesHost.getIp());
                hostInfo.setSshPort(22); // Kubernetes模式默认SSH端口
                hostInfo.setSshUser("root"); // Kubernetes模式默认用户
                hostInfo.setClusterCode(clusterCode);
                hostInfo.setClusterId(clusterId);
                hostInfo.setCreateTime(LocalDateTime.now());

                // 重要：从Kubernetes API获取的CPU架构信息
                String cpuArchitecture = kubernetesHost.getCpuArchitecture();
                hostInfo.setCpuArchitecture(cpuArchitecture);

                log.info("节点 {} (IP: {}) 的完整信息: 核心数={}, 总内存={}GB, 总磁盘={}GB, 架构={}",
                        kubernetesHost.getHostname(), kubernetesHost.getIp(),
                        kubernetesHost.getCoreNum(), kubernetesHost.getTotalMem(),
                        kubernetesHost.getTotalDisk(), cpuArchitecture);

                // 设置检查项列表
                hostInfo.setCheckItems(checkItems);

                // Kubernetes模式下检查主机受管状态
                ClusterHostEntity existingHost = hostService.getClusterHostByHostname(kubernetesHost.getHostname());

                if (existingHost != null && existingHost.getClusterId().equals(clusterId)) {
                    // 主机已在当前集群中受管 - 重复添加
                    hostInfo.setManaged(true);
                    hostInfo.setInstallState(InstallState.FAILED);
                    hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setCheckResult(
                            new CheckResult(Status.CONNECTION_FAILED.getCode(), "主机已在当前集群中受管，请勿重复添加"));
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("主机已受管");
                    log.info("Host {} is already managed in current Kubernetes cluster {}",
                            kubernetesHost.getHostname(), clusterId);
                } else if (existingHost != null) {
                    // 主机已在其他集群中受管
                    hostInfo.setManaged(true);
                    hostInfo.setInstallState(InstallState.FAILED);
                    hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setCheckResult(
                            new CheckResult(Status.CONNECTION_FAILED.getCode(), "主机已在其他集群中受管"));
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("主机已在其他集群受管");
                    log.info("Host {} is already managed in another cluster {}",
                            kubernetesHost.getHostname(), existingHost.getClusterId());
                } else {
                    // 主机未受管，Kubernetes模式下校验成功，可以添加
                    hostInfo.setManaged(false);
                    hostInfo.setInstallState(InstallState.SUCCESS);
                    hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setCheckResult(
                            new CheckResult(Status.CHECK_HOST_SUCCESS.getCode(), Status.CHECK_HOST_SUCCESS.getMsg()));
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
                    hostInfo.setMessage("K8S节点验证成功，可以添加");
                    log.info("Host {} is not managed in Kubernetes mode, can be added",
                            kubernetesHost.getHostname());

                    // 将可以添加的主机信息保存到列表中，用于后续保存
                    kubernetesHostsForSave.add(kubernetesHost);
                }

                // 初始化检查项状态（K8S模式下某些检查项可能不适用）
                if (hostInfo.getCheckItems() != null) {
                    for (CheckItem checkItem : hostInfo.getCheckItems()) {
                        // K8S模式下，某些检查项设置为成功（如SSH连接等）
                        if ("SSH连接检查".equals(checkItem.getItemName()) ||
                                "主机名检查".equals(checkItem.getItemName()) ||
                                "操作系统检查".equals(checkItem.getItemName())) {
                            checkItem.setStatus(CheckItem.Status.SUCCESS);
                            checkItem.setMessage("K8S模式自动通过");
                        }
                    }
                }

                hostInfoList.add(hostInfo);
            }



            // 使用HostUtils的统一排序方法对IP进行排序
            List<String> sortedIps = HostUtils.sortIpAddresses(hostInfoList.stream()
                    .map(HostInfo::getIp)
                    .collect(Collectors.toList()));

            // 按照排序后的IP顺序重新组织主机列表
            List<HostInfo> sortedHostList = new ArrayList<>();
            for (String ip : sortedIps) {
                hostInfoList.stream()
                        .filter(host -> ip.equals(host.getIp()))
                        .findFirst()
                        .ifPresent(sortedHostList::add);
            }

            // 缓存主机列表
            HashMap<String, HostInfo> hostMap = new HashMap<>();
            for (HostInfo hostInfo : sortedHostList) {
                hostMap.put(hostInfo.getIp(), hostInfo);
            }
            CacheUtils.put(clusterId + Constants.HOST_MAP, hostMap);

            log.info("已缓存K8S集群主机列表，共{}台主机", hostMap.size());
            log.info("已缓存K8S完整硬件信息，共{}台主机", kubernetesHostsForSave.size());

            // 分页处理
            int offset = (page - 1) * pageSize;
            int end = Math.min(offset + pageSize, sortedHostList.size());
            List<HostInfo> pagedResult = sortedHostList.subList(offset, end);

            return PageResult.of(pagedResult, sortedHostList.size(), page, pageSize);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取Kubernetes节点列表失败", e);
            throw new ServiceException("获取Kubernetes节点列表失败: " + e.getMessage());
        }
    }

    private List<HostInfo> getListPage(List<HostInfo> list, Integer offset, Integer pageSize) {
        List<HostInfo> result = new ArrayList<>();
        int limit = offset + pageSize;
        if (list.size() < offset + pageSize) {
            limit = list.size();
        }
        for (int i = offset; i < limit; i++) {
            result.add(list.get(i));
        }
        return result;
    }

    @Override
    public boolean clearHostEnvCheckCache() {
        try {
            log.debug("删除主机检查项缓存");
            CacheUtils.clear();
            log.info("主机环境校验缓存清理完成");
            return true;
        } catch (Exception e) {
            log.error("清理主机环境校验缓存失败", e);
            throw new ServiceException("清理主机环境校验缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取主机最近日志
     *
     * @param ip        主机IP
     * @param clusterId 集群ID
     * @return 主机最近日志内容
     */
    @Override
    public String getWorkerLog(String ip, Long clusterId) {
        try {
            // 1. 从缓存中获取主机信息
            Map<String, HostInfo> hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

            // 2. 查找指定IP的主机信息
            HostInfo hostInfo = hostMap.get(ip);
            if (hostInfo == null) {
                throw new BusinessException(Status.IP_IS_EMPTY.getCode(), "未找到主机信息");
            }

            // 3. 通过SSH插件适配器执行日志获取命令
            try {
                String command = "tail -n 100 /opt/datasophon/datasophon-worker/logs/datasophon-worker.log";
                
                // 4. 使用SSH插件适配器执行命令并返回日志内容
                String logContent = sshPluginAdapter.executeCommand(hostInfo, command);
                
                log.debug("【安装服务】获取主机工作日志成功: {} -> {} 字符", hostInfo.getIp(), 
                        logContent != null ? logContent.length() : 0);
                
                return logContent;
            } catch (Exception sshException) {
                throw new ServiceException("SSH连接失败: " + sshException.getMessage());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取主机日志失败", e);
            throw new ServiceException("获取主机日志失败: " + e.getMessage());
        }
    }
    
    // ==================== 插件化主机验证新方法 ====================
    
    /**
     * 解析主机列表字符串为IP列表
     */
    private List<String> parseHostList(String hostInput) {
        List<String> hostIpList = new ArrayList<>();
        
        if (StringUtils.isBlank(hostInput)) {
            return hostIpList;
        }
        
        // 支持换行符和逗号分隔
        String[] lines = hostInput.split("[\\r\\n]+");
        for (String line : lines) {
            if (StringUtils.isNotBlank(line)) {
                // 支持逗号分隔的多个IP
                String[] ips = line.trim().split("[,\\s]+");
                for (String ip : ips) {
                    if (StringUtils.isNotBlank(ip)) {
                        hostIpList.add(ip.trim());
                    }
                }
            }
        }
        
        return hostIpList;
    }
    
    /**
     * 启动基于插件的主机验证
     */
    private Map<String, HostInfo> startPluginBasedValidation(String clusterId, List<String> hostIpList, 
                                                           String sshUser, Integer sshPort, String sshPassword) {
        Map<String, HostInfo> hostMap = new HashMap<>();
        
        try {
            log.info("启动插件化主机验证: 集群={}, 主机数量={}", clusterId, hostIpList.size());
            
            // 创建主机校验请求
            HostValidationRequestDTO request = new HostValidationRequestDTO(
                    Long.parseLong(clusterId),
                    hostIpList,
                    sshUser,
                    sshPassword,
                    sshPort,
                    null // privateKeyPath 暂时为空
            );
            
            // 启动新架构的主机校验
            hostValidationService.startValidation(request);
            
            // 为每个主机创建初始的HostInfo
            for (String hostIp : hostIpList) {
                try {
                    // 创建初始的HostInfo
                    HostInfo hostInfo = createInitialHostInfo(clusterId, hostIp, sshUser, sshPort, sshPassword);
                    hostMap.put(hostIp, hostInfo);
                    
                    log.debug("已为主机创建HostInfo: 主机={}", hostIp);
                    
                } catch (Exception e) {
                    log.error("创建主机信息失败: 主机={}, 错误={}", hostIp, e.getMessage(), e);
                    
                    // 创建失败的HostInfo
                    HostInfo failedHostInfo = createFailedHostInfo(clusterId, hostIp, sshUser, sshPort, sshPassword, e.getMessage());
                    hostMap.put(hostIp, failedHostInfo);
                }
            }
            
            log.info("插件化主机验证启动完成: 集群={}, 成功启动={}, 失败={}", 
                    clusterId, hostMap.size(), hostIpList.size() - hostMap.size());
            
        } catch (Exception e) {
            log.error("插件化主机验证启动失败: 集群={}, 错误={}", clusterId, e.getMessage(), e);
            throw new ServiceException("主机验证启动失败: " + e.getMessage());
        }
        
        return hostMap;
    }
    
    /**
     * 创建初始的HostInfo对象
     */
    private HostInfo createInitialHostInfo(String clusterId, String hostIp, String sshUser, Integer sshPort, String sshPassword) {
        HostInfo hostInfo = new HostInfo();
        
        // 基本信息
        hostInfo.setClusterId(Long.parseLong(clusterId));
        hostInfo.setIp(hostIp);
        hostInfo.setHostname(hostIp); // 初始使用IP作为主机名
        hostInfo.setSshUser(sshUser);
        hostInfo.setSshPort(sshPort);
        hostInfo.setSshPassword(sshPassword);
        hostInfo.setCreateTime(LocalDateTime.now());
        
        // 初始状态设置
        hostInfo.setInstallState(InstallState.RUNNING);
        hostInfo.setInstallStateCode(1);
        hostInfo.setManaged(false);
        hostInfo.setProgress(0);
        hostInfo.setStatus(CheckItem.Status.WAITING);
        
        // 检查结果初始化
        CheckResult checkResult = new CheckResult();
        checkResult.setCode(9999);
        checkResult.setMsg("等待主机校验");
        hostInfo.setCheckResult(checkResult);
        
        // 各状态初始化为loading
        hostInfo.setSshConnectStatus(OsInfoStatusEnum.LOADING);
        
        // 错误信息初始化
        hostInfo.setSshErrorMsg("");

        hostInfo.setErrorMessage("");
        
        return hostInfo;
    }
    
    /**
     * 创建失败的HostInfo对象
     */
    private HostInfo createFailedHostInfo(String clusterId, String hostIp, String sshUser, Integer sshPort, String sshPassword, String errorMessage) {
        HostInfo hostInfo = createInitialHostInfo(clusterId, hostIp, sshUser, sshPort, sshPassword);
        
        // 设置失败状态
        hostInfo.setInstallState(InstallState.FAILED);
        hostInfo.setInstallStateCode(-1);
        hostInfo.setStatus(CheckItem.Status.FAILED);
        hostInfo.setErrorMessage(errorMessage);
        hostInfo.setSshErrorMsg(errorMessage);
        hostInfo.setErrMsg(errorMessage);
        
        // 检查结果设置为失败
        CheckResult checkResult = new CheckResult();
        checkResult.setCode(500);
        checkResult.setMsg("主机验证启动失败: " + errorMessage);
        hostInfo.setCheckResult(checkResult);
        
        // 所有状态设置为failed
        hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
        
        return hostInfo;
    }
}
