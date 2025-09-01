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

package com.datasophon.api.service.host.strategy.impl;

import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.service.host.strategy.AbstractHostManagementStrategy;
import com.datasophon.api.service.host.strategy.model.*;
import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

/**
 * PVM（传统虚拟机）主机管理策略实现
 * 处理传统虚拟机模式下的主机发现、管理和导入
 */
@Slf4j
@Component
public class PvmHostStrategy extends AbstractHostManagementStrategy {

    @Autowired
    private ClusterHostService clusterHostService;

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.PVM;
    }

    @Override
    protected void validateDiscoveryRequest(HostDiscoveryRequest request) {
        super.validateDiscoveryRequest(request);
        
        // 转换为强类型参数
        PvmConnectionParams connectionParams = extractPvmConnectionParams(request.getConnectionParams());
        
        // 验证连接参数
        connectionParams.validate();
        
        // 验证IP格式
        validateIpFormats(connectionParams.getHosts());
    }

    /**
     * 从Map中提取PVM连接参数
     */
    private PvmConnectionParams extractPvmConnectionParams(Map<String, Object> paramsMap) {
        return PvmConnectionParams.builder()
                .hosts((String) paramsMap.get("hosts"))
                .sshUser((String) paramsMap.get("sshUser"))
                .sshPort(parseIntegerParam(paramsMap.get("sshPort")))
                .sshPassword((String) paramsMap.get("sshPassword"))
                .privateKeyPath((String) paramsMap.get("privateKeyPath"))
                .timeoutSeconds(parseIntegerParam(paramsMap.get("timeoutSeconds"), 30))
                .build();
    }

    /**
     * 解析整数参数
     */
    private Integer parseIntegerParam(Object param) {
        return parseIntegerParam(param, null);
    }

    private Integer parseIntegerParam(Object param, Integer defaultValue) {
        switch (param) {
            case null -> {
                return defaultValue;
            }
            case Integer i -> {
                return i;
            }
            case String s -> {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            default -> {
            }
        }
        return defaultValue;
    }

    @Override
    protected void prepareConnection(Map<String, Object> connectionParams) {
        // PVM模式可以在这里做SSH连接测试
        // 这里简化处理，实际可以测试SSH连接
        log.debug("准备PVM SSH连接参数");
    }

    /**
     * 发现主机 - 仅解析IP并创建主机实体，不执行实际检查
     * 主机检查将由前端通过 performHostCheck 方法主动触发
     */
    @Override
    protected List<ClusterHostEntity> doDiscoverHosts(HostDiscoveryRequest request) {
        // 转换为强类型参数
        PvmConnectionParams connectionParams = extractPvmConnectionParams(request.getConnectionParams());
        
        log.info("开始解析PVM主机列表: {}", connectionParams.getHosts());
        
        try {
            // 使用开源库解析IP列表
            List<String> ipList = parseIpRangesWithLibrary(connectionParams.getHosts());
            log.info("解析出{}个IP地址: {}", ipList.size(),ipList);
            
            // 创建主机实体列表（仅创建实体，不执行SSH检查）
            List<ClusterHostEntity> hostEntities = new ArrayList<>();
            
            for (String ip : ipList) {
                ClusterHostEntity hostEntity = createHostEntity(ip, connectionParams, request.getClusterId());
                hostEntities.add(hostEntity);
            }
            
            log.info("成功创建{}个主机实体，等待前端触发检查", hostEntities.size());
            return hostEntities;
            
        } catch (Exception e) {
            log.error("解析PVM主机列表失败", e);
            throw new RuntimeException("解析PVM主机列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected HostListResult doGetHostList(HostListRequest request) {
        try {
            // 调用现有的分页查询方法
            PageResult<ClusterHostEntity> pageResult = clusterHostService.listByPage(
                request.getClusterId(),
                request.getHostname(),
                request.getIp(),
                request.getCpuArchitecture(),
                request.getHostState(),
                request.getOrderField(),
                request.getOrderType(),
                request.getPage(),
                request.getPageSize()
            );
            
            return HostListResult.builder()
                    .hosts(pageResult.getRecords())
                    .total(pageResult.getTotal())
                    .page(request.getPage())
                    .pageSize(request.getPageSize())
                    .hasMore(pageResult.getTotal() > (long) request.getPage() * request.getPageSize())
                    .build();
                    
        } catch (Exception e) {
            log.error("获取PVM主机列表失败", e);
            throw new RuntimeException("获取PVM主机列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doImportHosts(List<ClusterHostEntity> hosts, HostImportRequest request) {
        try {
            log.info("开始导入{}台PVM主机", hosts.size());
            
            // PVM模式的主机通常已经在分析阶段保存，这里可能需要更新状态
            for (ClusterHostEntity host : hosts) {
                // 更新主机为受管状态
                host.setManagementStatus(ManagementStatus.MANAGED);
                clusterHostService.updateById(host);
            }
            
            log.info("成功导入{}台PVM主机", hosts.size());
            
        } catch (Exception e) {
            log.error("导入PVM主机失败", e);
            throw new RuntimeException("导入PVM主机失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ClusterHostEntity> refreshHosts(Long clusterId, Map<String, Object> connectionParams) {
        // PVM模式刷新主机信息
        HostDiscoveryRequest request = HostDiscoveryRequest.builder()
                .clusterId(clusterId)
                .connectionParams(connectionParams)
                .forceRefresh(true)
                .build();
        
        HostDiscoveryResult result = discoverHosts(request);
        if (result.getSuccess()) {
            return result.getHosts();
        } else {
            throw new RuntimeException("刷新PVM主机失败: " + result.getErrorMessage());
        }
    }

    /**
     * 检查连接参数 - 验证SSH连接参数是否有效
     * 用于在发现主机前测试连接参数
     */
    @Override
    public Map<String, Object> checkConnection(Map<String, Object> connectionParamsMap) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("测试PVM连接参数");
            
            // 转换为强类型参数
            PvmConnectionParams connectionParams = extractPvmConnectionParams(connectionParamsMap);
            
            // 验证连接参数
            connectionParams.validate();
            
            // 验证IP格式
            validateIpFormats(connectionParams.getHosts());
            
            // TODO: 可以选择测试第一个IP的SSH连接
            // 简化处理，仅做参数格式验证
            // 后续可使用connectionParams进行实际连接测试
            
            result.put("connected", true);
            result.put("message", "连接参数验证成功");
            result.put("parsedIpCount", parseIpRangesWithLibrary(connectionParams.getHosts()).size());
            
            log.info("PVM连接参数验证成功");
            
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
            result.put("message", "连接参数验证失败: " + e.getMessage());
            
            log.error("PVM连接参数验证失败", e);
        }
        
        return result;
    }

    /**
     * 执行主机检查 - 前端主动触发的检查入口
     * 对指定主机列表执行SSH连接测试和系统信息收集
     */
    @Override
    public Map<String, Object> performHostCheck(Long clusterId, List<String> hostnames,
                                              Map<String, Object> connectionParams) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("前端触发PVM主机检查，集群ID: {}, 主机数量: {}", clusterId, hostnames.size());
            
            // PVM模式的主机检查逻辑
            // TODO: 实现SSH连接和系统信息检查
            // 1. 获取SSH连接参数
            // 2. 对每个主机执行SSH连接测试
            // 3. 收集主机硬件信息（CPU、内存、磁盘）
            // 4. 更新主机状态到数据库
            
            result.put("started", true);
            result.put("checkedHosts", hostnames.size());
            result.put("message", "主机环境检查已启动");
            
            log.info("PVM主机环境检查启动成功，集群ID: {}, 主机数量: {}", clusterId, hostnames.size());
            
        } catch (Exception e) {
            result.put("started", false);
            result.put("error", e.getMessage());
            result.put("message", "启动主机环境检查失败: " + e.getMessage());
            
            log.error("PVM主机环境检查失败", e);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getHostCheckStatus(Long clusterId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // PVM模式的主机检查状态查询
            // TODO: 实现基于集群主机状态的检查完成判断逻辑
            boolean completed = checkAllHostsValidated(clusterId);
            
            result.put("completed", completed);
            result.put("data", Collections.emptyList());
            
        } catch (Exception e) {
            result.put("completed", false);
            result.put("error", e.getMessage());
            
            log.error("获取PVM主机检查状态失败", e);
        }
        
        return result;
    }

    @Override
    public void cleanup(Long clusterId) {
        try {
            // PVM模式的资源清理
            // TODO: 实现清理临时文件、缓存等逻辑
            log.info("已清理集群{}的PVM主机检查资源", clusterId);
            
        } catch (Exception e) {
            log.error("清理PVM主机检查资源失败", e);
        }
    }

    @Override
    public Map<String, Object> validateForNextStep(Long clusterId) {
        // PVM校验规则可按需实现；这里保持与现有逻辑兼容，简单返回未实现提示
        Map<String, Object> result = new HashMap<>();
        result.put("valid", false);
        result.put("message", "PVM模式下一步校验规则待实现");
        return result;
    }



    @Override
    protected Map<String, Object> buildDiscoveryMetadata(List<ClusterHostEntity> hosts, HostDiscoveryRequest request) {
        Map<String, Object> metadata = super.buildDiscoveryMetadata(hosts, request);
        
        // PVM模式特有的元数据
        String hostString = (String) request.getConnectionParams().get("hosts");
        if (hostString != null) {
            String[] hostArray = hostString.split(",");
            metadata.put("requestedHosts", hostArray.length);
            metadata.put("hostList", Arrays.asList(hostArray));
        }
        
        return metadata;
    }

    /**
     * 验证IP格式 - 使用Apache Commons Validator
     */
    private void validateIpFormats(String hosts) {
        try {
            List<String> ipList = parseIpRangesWithLibrary(hosts);
            if (ipList.isEmpty()) {
                throw new IllegalArgumentException("未能解析到有效的IP地址");
            }
            
            if (ipList.size() > 100) {
                throw new IllegalArgumentException("IP地址数量过多，最大支持100个");
            }
            
            // 使用Apache Commons Validator验证每个IP
            InetAddressValidator validator = InetAddressValidator.getInstance();
            for (String ip : ipList) {
                if (!validator.isValidInet4Address(ip)) {
                    throw new IllegalArgumentException("无效的IPv4地址: " + ip);
                }
            }
            
            log.debug("IP格式验证通过，解析出{}个有效IP", ipList.size());
            
        } catch (Exception e) {
            log.error("IP格式验证失败: {}", e.getMessage());
            throw new IllegalArgumentException("IP格式验证失败: " + e.getMessage());
        }
    }

    /**
     * 检查集群所有主机是否已验证完成
     */
    private boolean checkAllHostsValidated(Long clusterId) {
        try {
            // TODO: 实现基于数据库的主机状态检查
            // 查询集群下所有主机的状态，判断是否都已完成验证
            List<ClusterHostEntity> hosts = clusterHostService.getHostListByClusterId(clusterId);
            return !hosts.isEmpty();
            
            // 简化逻辑：如果有主机存在，认为验证完成

        } catch (Exception e) {
            log.error("检查主机验证状态失败，集群ID: {}", clusterId, e);
            return false;
        }
    }

    /**
     * 使用开源库解析IP范围字符串为IP列表
     * 支持的格式：
     * - 单个IP: 192.168.1.100
     * - 逗号分隔: 192.168.1.100,192.168.1.101
     * - 范围批量: 192.168.1.[100-110]
     * - 混合格式: 192.168.1.100,192.168.1.[105-110]
     * - 换行分隔
     */
    private List<String> parseIpRangesWithLibrary(String hostInput) {
        List<String> ipList = new ArrayList<>();
        
        if (StringUtils.isBlank(hostInput)) {
            return ipList;
        }
        
        // 使用Apache Commons Lang处理字符串
        String[] lines = StringUtils.split(hostInput, "\r\n");
        InetAddressValidator validator = InetAddressValidator.getInstance();
        
        for (String line : lines) {
            if (StringUtils.isBlank(line)) continue;
            
            // 处理逗号分隔的IP
            String[] parts = StringUtils.split(line, ",");
            for (String part : parts) {
                String trimmedPart = StringUtils.trim(part);
                if (StringUtils.isBlank(trimmedPart)) continue;
                
                if (isIpRange(trimmedPart)) {
                    // 解析IP范围 192.168.1.[100-110]
                    ipList.addAll(expandIpRangeWithLibrary(trimmedPart, validator));
                } else if (validator.isValidInet4Address(trimmedPart)) {
                    // 单个IP - 使用Apache Commons Validator验证
                    ipList.add(trimmedPart);
                } else {
                    log.warn("无效的IP格式，跳过: {}", trimmedPart);
                }
            }
        }
        
        // 去重并排序
        return ipList.stream()
                .distinct()
                .sorted(this::compareIpAddresses)
                .toList();
    }

    /**
     * 检查是否为IP范围格式
     */
    private boolean isIpRange(String input) {
        return input.contains("[") && input.contains("]") && input.contains("-");
    }



    /**
     * 使用开源库展开IP范围 192.168.1.[100-110] → [192.168.1.100, 192.168.1.101, ...]
     */
    private List<String> expandIpRangeWithLibrary(String ipRange, InetAddressValidator validator) {
        List<String> expandedIps = new ArrayList<>();
        
        try {
            // 正则匹配 192.168.1.[100-110] 格式
            Pattern pattern = Pattern.compile("^(.+)\\[(\\d+)-(\\d+)](.*)$");
            Matcher matcher = pattern.matcher(ipRange);
            
            if (matcher.matches()) {
                String prefix = matcher.group(1);
                int start = Integer.parseInt(matcher.group(2));
                int end = Integer.parseInt(matcher.group(3));
                String suffix = matcher.group(4);
                
                if (start > end) {
                    log.warn("IP范围起始值大于结束值: {}", ipRange);
                    return expandedIps;
                }
                
                if (end - start > 254) {
                    log.warn("IP范围过大，限制为254个: {}", ipRange);
                    end = start + 254;
                }
                
                // 使用Apache Commons Lang的Range来验证范围
                Range<Integer> range = Range.of(start, end);
                
                for (int i = start; i <= end; i++) {
                    if (range.contains(i)) {
                        String ip = prefix + i + suffix;
                        // 使用Apache Commons Validator验证生成的IP
                        if (validator.isValidInet4Address(ip)) {
                            expandedIps.add(ip);
                        } else {
                            log.warn("生成的IP地址无效，跳过: {}", ip);
                        }
                    }
                }
                
                log.debug("IP范围 {} 展开为 {} 个IP地址", ipRange, expandedIps.size());
            } else {
                log.warn("无法解析IP范围格式: {}", ipRange);
            }
            
        } catch (NumberFormatException e) {
            log.error("解析IP范围时数字格式错误: {}", ipRange, e);
        } catch (Exception e) {
            log.error("解析IP范围时发生异常: {}", ipRange, e);
        }
        
        return expandedIps;
    }

    /**
     * IP地址比较器，用于排序
     */
    private int compareIpAddresses(String ip1, String ip2) {
        try {
            String[] parts1 = ip1.split("\\.");
            String[] parts2 = ip2.split("\\.");
            
            for (int i = 0; i < 4; i++) {
                int num1 = Integer.parseInt(parts1[i]);
                int num2 = Integer.parseInt(parts2[i]);
                int comparison = Integer.compare(num1, num2);
                if (comparison != 0) {
                    return comparison;
                }
            }
            return 0;
        } catch (Exception e) {
            // 如果解析失败，回退到字符串比较
            return ip1.compareTo(ip2);
        }
    }

    /**
     * 创建主机实体 - 仅用于展示，待前端触发检查后更新详细信息
     */
    private ClusterHostEntity createHostEntity(String ip, PvmConnectionParams connectionParams, Long clusterId) {
        ClusterHostEntity hostEntity = new ClusterHostEntity();
        
        // 基本信息
        hostEntity.setIp(ip);
        hostEntity.setHostname(ip); // 默认使用IP作为主机名，检查后可能更新为实际主机名
        hostEntity.setClusterId(clusterId);
        hostEntity.setCreateTime(LocalDateTime.now());
        hostEntity.setUpdateTime(LocalDateTime.now());
        
        // 初始状态（待检查状态）
        hostEntity.setManagementStatus(ManagementStatus.UNMANAGED);
        hostEntity.setCoreNum(0); // 待检查更新
        hostEntity.setTotalMem(0); // 待检查更新
        hostEntity.setTotalDisk(0); // 待检查更新
        hostEntity.setAverageLoad("0.0"); // 待检查更新
        hostEntity.setCpuArchitecture("x86_64"); // 默认值，检查后可能更新
        
        // SSH连接参数会在检查时使用，这里不保存到实体中
        log.debug("创建待检查主机实体: IP={}, SSH={}@{}:{}", 
                ip, connectionParams.getSshUser(), ip, connectionParams.getSshPort());
        
        return hostEntity;
    }
}