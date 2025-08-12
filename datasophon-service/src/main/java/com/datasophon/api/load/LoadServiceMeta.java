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

package com.datasophon.api.load;

// JDK 21新特性导入
import com.datasophon.api.load.model.LoadContext;
import com.datasophon.api.load.model.ParseResult;
import com.datasophon.api.load.model.ServiceMetaConfig;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.net.NetUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.serviceCacheSyncActor;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.FrameInfoService;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.api.utils.CommonUtils;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceInfo;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;

import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterVariable;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.api.converter.FrameServiceConverter;
import com.datasophon.api.converter.FrameServiceRoleConverter;
import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.mybatisflex.core.query.QueryChain;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.pekko.actor.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.function.Function;

import static com.datasophon.api.master.ActorUtils.getActorRefName;
import static com.datasophon.common.Constants.GENERAL;

@Component
public class LoadServiceMeta implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(LoadServiceMeta.class);

    private static final String PATH = "meta";
    private static final String HDFS = "HDFS";
    private static final String HADOOP = "HADOOP";
    @Autowired
    private FrameServiceService frameServiceService;

    @Autowired
    private FrameInfoService frameInfoService;

    @Autowired
    private FrameServiceRoleService roleService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ConfigBean configBean;

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private ClusterServiceInstanceRoleGroupService roleGroupService;

    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;


    /**
     * 1、设置全局环境变量
     * 2、创建各集群角色 MasterServiceActor
     * 3、解析各角色 service_ddl.json 更新到 t_ddh_frame_service t_ddh_frame_service_role 表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws Exception {
        // 使用JDK 21的text blocks和现代化方法
        logger.info("""
                ============================================================
                  开始加载服务元数据 (DataSophon Service Meta Loader)
                  使用JDK21虚拟线程实现框架隔离处理
                ============================================================
                """);

        var ddps = Optional.ofNullable(FileUtil.ls(PATH))
                .orElse(new File[0]);
        
        // 加载全局变量和集群信息
        var clusters = clusterInfoService.getClusterList();
        var loadContext = createLoadContext(clusters);
        
        logger.info("发现 {} 个框架目录，{} 个集群", ddps.length, loadContext.clusterCount());
        
        // 使用JDK21虚拟线程实现框架隔离处理
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var frameTasks = Arrays.stream(ddps)
                .map(framePath -> 
                    executor.submit(() -> processFrameDirectory(framePath, loadContext))
                )
                .toList();
            
            // 等待所有框架处理完成，确保完全隔离
            frameTasks.forEach(task -> {
                try {
                    task.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("框架处理失败: {}", e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // 启动服务缓存同步Actor
        ActorUtils.actorSystem.actorOf(Props.create(serviceCacheSyncActor.class),
                getActorRefName(serviceCacheSyncActor.class));
        
        logger.info("服务元数据加载完成");
    }

    /**
     * 使用JDK 21现代化方法创建加载上下文
     */
    private LoadContext createLoadContext(List<ClusterInfoDTO> clusters) throws UnknownHostException {
        loadGlobalVariables(clusters);
        
        var hostName = InetAddress.getLocalHost().getHostName();
        var priorityNetworks = getPriorityNetworks(
                NetUtil.getIpByHost(hostName));
        
        return LoadContext.of(clusters, hostName, configBean.getServerPort(), priorityNetworks);
    }

    /**
     * 处理单个框架目录 - 使用JDK 21新特性
     */
    private void processFrameDirectory(File framePath, LoadContext loadContext) {
        var frameCode = framePath.getName();
        var frameInfo = saveClusterFrame(frameCode);
        
        logger.info("处理框架: {}", frameCode);
        
        var files = FileUtil.loopFiles(framePath);
        var serviceFiles = files.stream()
                .filter(file -> file.getName().endsWith(Constants.JSON_EXTENSION))
                .toList();
        
        logger.info("框架 {} 包含 {} 个服务定义文件", frameCode, serviceFiles.size());
        
        // 使用虚拟线程处理服务文件，确保框架内服务隔离
        var parseResults = new ArrayList<ParseResult>();
        try (var serviceExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var serviceTasks = serviceFiles.stream()
                .map(file -> 
                    serviceExecutor.submit(() -> parseServiceFile(frameCode, frameInfo, file, loadContext))
                )
                .toList();
            
            // 收集所有服务处理结果
            for (var task : serviceTasks) {
                try {
                    parseResults.add(task.get());
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("服务文件处理失败: {}", e.getMessage(), e);
                    parseResults.add(ParseResult.Failure.of("未知服务", "任务执行异常", e));
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // 统计处理结果
        var successCount = parseResults.stream()
                .mapToLong(result -> result.isSuccess() ? 1 : 0)
                .sum();
        
        var failureCount = parseResults.size() - successCount;
        
        // 记录失败的服务
        parseResults.stream()
                .filter(ParseResult::isFailure)
                .map(result -> (ParseResult.Failure) result)
                .forEach(failure -> logger.error("""
                        服务 {} 解析失败:
                        错误信息: {}
                        """, failure.serviceName(), failure.fullErrorMessage()));
        
        logger.info("框架 {} 处理完成: 成功 {}, 失败 {}", frameCode, successCount, failureCount);
    }

    /**
     * 解析单个服务文件 - 使用密封类返回结果
     */
    private ParseResult parseServiceFile(String frameCode, FrameInfoEntity frameInfo, 
                                       File file, LoadContext loadContext) {
        var serviceName = file.getParentFile().getName();
        
        try {
            var serviceDdl = FileReader.create(file).readString();
            var serviceInfo = JSONObject.parseObject(serviceDdl, ServiceInfo.class);
            var serviceInfoMd5 = SecureUtil.md5(serviceDdl);
            
            // 构建配置文件映射
            var configFileMap = buildConfigFileMap(serviceInfo);
            
            // 创建配置对象
            var config = ServiceMetaConfig.of(frameCode, frameInfo, serviceName, 
                    serviceDdl, serviceInfo, serviceInfoMd5, configFileMap);
            
            // 处理服务元数据
            processServiceMetadata(config, loadContext);
            
            return new ParseResult.Success(config);
            
        } catch (Exception e) {
            return ParseResult.Failure.of(serviceName, "服务DDL解析失败", e);
        }
    }

    /**
     * 处理服务元数据 - 重构使用JDK 21新特性
     */
    private void processServiceMetadata(ServiceMetaConfig config, LoadContext loadContext) {
        putServicePackageToMap(config);
        putServiceHomeToVariable(loadContext.clusters(), config.serviceName(), config.decompressPackageName());
        
        // 保存服务和服务配置
        var serviceEntity = saveFrameService(config);
        // 保存框架服务角色
        saveFrameServiceRole(config, serviceEntity);
    }

    /**
     * 将服务包信息放入映射表
     */
    private void putServicePackageToMap(ServiceMetaConfig config) {
        PackageUtils.putServicePackageName(
                config.frameCode(), 
                config.serviceName(), 
                config.decompressPackageName());
    }



    private void putServiceHomeToVariable(
            List<ClusterInfoDTO> clusters, String serviceName,
            String decompressPackageName) {
        for (ClusterInfoDTO cluster : clusters) {
            Map<String, String> globalVariables = GlobalVariables.get(cluster.id());
            if (HDFS.equals(serviceName)) {
                serviceName = HADOOP;
            }
            globalVariables.put(
                    "${" + serviceName + "_HOME}",
                    Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
        }
    }

    /**
     * 保存框架服务角色 - 重构使用JDK 21新特性
     */
    private void saveFrameServiceRole(ServiceMetaConfig config, FrameServiceEntity serviceEntity) {
        var serviceRoles = config.serviceRoles();
        
        // 使用虚拟线程处理服务角色，确保角色级别的隔离
        try (var roleExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var roleTasks = serviceRoles.stream()
                .map(serviceRole -> 
                    roleExecutor.submit(() -> processServiceRole(config, serviceEntity, serviceRole))
                )
                .toList();
            
            // 等待所有角色处理完成
            roleTasks.forEach(task -> {
                try {
                    task.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("服务角色处理失败: {}", e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        logger.debug("put {} {} service info into cache", config.frameCode(), config.serviceName());
        ServiceInfoMap.put(config.frameCode() + Constants.UNDERLINE + config.serviceName(), config.serviceInfo());
    }

    /**
     * 处理单个服务角色
     */
    private void processServiceRole(ServiceMetaConfig config, FrameServiceEntity serviceEntity, 
                                  ServiceRoleInfo serviceRole) {
        var roleKey = config.frameCode() + Constants.UNDERLINE + 
                      config.serviceInfo().getName() + Constants.UNDERLINE + 
                      serviceRole.getName();
        
        logger.debug("put {} {} {} service role info into cache",
                config.frameCode(), config.serviceName(), serviceRole.getName());
        
        // 处理JMX端口
        Optional.ofNullable(serviceRole.getJmxPort())
                .filter(StringUtils::isNotBlank)
                .ifPresent(jmxPort -> {
                    logger.debug("{} jmx port is :{} and the jmx key is: {}",
                            serviceRole.getName(), jmxPort, roleKey);
                    ServiceRoleJmxMap.put(roleKey, jmxPort);
                });
        
        ServiceRoleMap.put(roleKey, serviceRole);
        
        var serviceRoleJson = JSONObject.toJSONString(serviceRole);
        var serviceRoleJsonMd5 = SecureUtil.md5(serviceRoleJson);
        
        saveOrUpdateServiceRole(config, serviceEntity, serviceRole, serviceRoleJson, serviceRoleJsonMd5);
    }

    /**
     * 保存或更新服务角色实体
     */
    private void saveOrUpdateServiceRole(ServiceMetaConfig config, FrameServiceEntity serviceEntity,
                                       ServiceRoleInfo serviceRole, String serviceRoleJson, 
                                       String serviceRoleJsonMd5) {
        // 使用新的非异常方法查找服务角色
        var roleDto = roleService.findServiceRoleByServiceIdAndServiceRoleName(
                serviceEntity.getId(), serviceRole.getName()).orElse(null);
        var roleConverter = SpringUtil.getBean(FrameServiceRoleConverter.class);
        var role = roleDto != null ? roleConverter.dtoToEntity(roleDto) : null;
        
        // JDK 21现代化处理 - 使用简洁的条件处理
        if (role == null) {
            var newRole = new FrameServiceRoleEntity();
            buildFrameServiceRole(config, serviceEntity, serviceRole, 
                    serviceRoleJson, serviceRoleJsonMd5, newRole);
            roleService.save(newRole);
        } else if (!role.getServiceRoleJsonMd5().equals(serviceRoleJsonMd5)) {
            buildFrameServiceRole(config, serviceEntity, serviceRole, 
                    serviceRoleJson, serviceRoleJsonMd5, role);
            roleService.updateById(role);
        }
    }



    /**
     * 保存框架服务 - 重构使用JDK 21新特性和新的查找方法
     */
    private FrameServiceEntity saveFrameService(ServiceMetaConfig config) {
        // 使用新的非异常方法查找服务
        var serviceDto = frameServiceService.findServiceByFrameIdAndServiceName(
                config.frameInfo().getId(), config.serviceName()).orElse(null);
        
        var serviceConverter = SpringUtil.getBean(FrameServiceConverter.class);
        var serviceEntity = serviceDto != null ? serviceConverter.dtoToEntity(serviceDto) : null;
        var parameters = config.parameters();
        // 使用JDK21框架隔离版本，传入框架代码确保配置隔离
        var nameToRoleMap = ConfigGroupUtils.buildNameToRoleMap(config.configFileMap(), config.frameCode());

        // 使用现代化流式API处理配置目标角色
        parameters.stream()
                .filter(serviceConfig -> ObjectUtils.isEmpty(serviceConfig.getConfigTargetRoles()))
                .forEach(serviceConfig -> {
                    var configTargetRoles = nameToRoleMap.getOrDefault(serviceConfig.getName(), GENERAL);
                    serviceConfig.setConfigTargetRoles(configTargetRoles);
                });

        // JDK 21现代化处理服务实体状态
        if (serviceEntity == null) {
            serviceEntity = new FrameServiceEntity();
            buildServiceEntity(config, serviceEntity);
            frameServiceService.save(serviceEntity);
        } else if (!serviceEntity.getServiceJsonMd5().equals(config.serviceInfoMd5())) {
            var configMapStr = JSONObject.toJSONString(config.configFileMap());
            var configFileMapStrMd5 = SecureUtil.md5(configMapStr);
            
            if (!configFileMapStrMd5.equals(serviceEntity.getConfigFileJsonMd5())) {
                updateServiceInstanceConfig(config.frameCode(), 
                        config.serviceInfo().getName(), 
                        config.serviceInfo().getParameters());
            }
            buildServiceEntity(config, serviceEntity);
            frameServiceService.updateById(serviceEntity);
        }

        // 更新缓存映射
        var cacheKey = config.frameCode() + Constants.UNDERLINE + config.serviceInfo().getName();
        ServiceConfigMap.put(cacheKey + Constants.CONFIG, parameters);
        ServiceConfigFileMap.put(cacheKey + Constants.CONFIG_FILE, config.configFileMap());

        return serviceEntity;
    }

    /**
     * 构建配置文件映射 - 重构使用JDK 21新特性
     */
    private Map<Generators, List<ServiceConfig>> buildConfigFileMap(ServiceInfo serviceInfo) {
        var allParameters = serviceInfo.getParameters();
        var parameterMap = allParameters.stream()
                .collect(Collectors.toMap(
                        ServiceConfig::getName,
                        Function.identity(),
                        (v1, v2) -> v1));
        
        return buildConfigFileMap(serviceInfo, parameterMap);
    }

    /**
     * 构建配置文件映射的核心实现
     */
    private Map<Generators, List<ServiceConfig>> buildConfigFileMap(
            ServiceInfo serviceInfo,
            Map<String, ServiceConfig> parameterMap) {
        var configFileMap = new HashMap<Generators, List<ServiceConfig>>();
        var configWriter = serviceInfo.getConfigWriter();
        var generators = configWriter.getGenerators();
        
        // 使用现代化流式API处理配置生成器
        generators.forEach(generator -> {
            var configList = generator.getIncludeParams().stream()
                    .filter(parameterMap::containsKey)
                    .map(paramName -> {
                        var sourceConfig = parameterMap.get(paramName);
                        var newConfig = new ServiceConfig();
                        BeanUtils.copyProperties(sourceConfig, newConfig);
                        return newConfig;
                    })
                    .collect(Collectors.toList());
            
            configFileMap.merge(generator, configList, (existing, newList) -> {
                existing.addAll(newList);
                return existing;
            });
        });
        
        return configFileMap;
    }

    private FrameInfoEntity saveClusterFrame(String frameCode) {
        FrameInfoEntity frameInfo = QueryChain.of(FrameInfoEntity.class)
                .where(FrameInfoEntity::getFrameCode).eq(frameCode)
                .one();
        if (Objects.isNull(frameInfo)) {
            frameInfo = new FrameInfoEntity();
            frameInfo.setFrameCode(frameCode);
            frameInfoService.save(frameInfo);
        }
        return frameInfo;
    }

    public void loadGlobalVariables(List<ClusterInfoDTO> clusters) throws UnknownHostException {
        if (CollUtil.isNotEmpty(clusters)) {
            for (ClusterInfoDTO cluster : clusters) {
                HashMap<String, String> globalVariables = new HashMap<>();
                // TODO: 需要创建ClusterVariableService来替代直接QueryChain调用
                List<ClusterVariable> variables = QueryChain.of(ClusterVariable.class)
                        .where(ClusterVariable::getClusterId).eq(cluster.id())
                        .list();
                for (ClusterVariable variable : variables) {
                    globalVariables.put(variable.getVariableName(), variable.getVariableValue());
                }
                globalVariables.put("${apiHost}", InetAddress.getLocalHost().getHostName());
                globalVariables.put("${apiPort}", configBean.getServerPort());
                globalVariables.put("${INSTALL_PATH}", Constants.INSTALL_PATH);

                String priorityNetworks = getPriorityNetworks(
                        NetUtil.getIpByHost(InetAddress.getLocalHost().getHostName()));
                globalVariables.put("${priority_networks}", priorityNetworks);

                GlobalVariables.put(cluster.id(), globalVariables);

                // TODO: ProcessUtils.createServiceActor需要ClusterInfoEntity，暂时跳过
                // ProcessUtils.createServiceActor(cluster);
            }
        }
    }

    private void updateServiceInstanceConfig(
            String frameCode, String serviceName, List<ServiceConfig> parameters) {
        // 查询frameCode相同的集群
        List<ClusterInfoDTO> clusters = clusterInfoService.getClusterByFrameCode(frameCode);
        // 查询集群的服务实例
        for (ClusterInfoDTO cluster : clusters) {
            ClusterServiceInstanceDTO serviceInstanceDto = serviceInstanceService
                    .getServiceInstanceByClusterIdAndServiceName(
                            cluster.id(), serviceName);
            if (Objects.nonNull(serviceInstanceDto)) {
                ClusterServiceRoleGroupConfigDTO configDto = roleGroupService
                        .getRoleGroupConfigByServiceId(serviceInstanceDto.id());
                ClusterServiceRoleGroupConfigConverter configConverter = SpringUtil.getBean(ClusterServiceRoleGroupConfigConverter.class);
                ClusterServiceRoleGroupConfig config = configConverter.dtoToEntity(configDto);
                String configJson = config.getConfigJson();
                List<ServiceConfig> serviceConfigs = JSONArray.parseArray(configJson, ServiceConfig.class);
                ProcessUtils.addAll(serviceConfigs, parameters);
                // 更新服务实例的配置
                config.setConfigJson(JSONObject.toJSONString(serviceConfigs));
                roleGroupConfigService.updateById(config);
            }
        }
    }

    /**
     * 构建框架服务角色实体 - 重构使用JDK 21新特性
     */
    private void buildFrameServiceRole(
            ServiceMetaConfig config,
            FrameServiceEntity serviceEntity,
            ServiceRoleInfo serviceRole,
            String serviceRoleJson,
            String serviceRoleJsonMd5,
            FrameServiceRoleEntity role) {
        role.setServiceId(serviceEntity.getId());
        role.setServiceRoleName(serviceRole.getName());
        role.setCardinality(serviceRole.getCardinality());
        role.setFrameCode(config.frameCode());
        role.setServiceRoleJson(serviceRoleJson);
        role.setServiceRoleType(CommonUtils.convertRoleType(serviceRole.getRoleType().getName()));
        role.setJmxPort(serviceRole.getJmxPort());
        role.setServiceRoleJsonMd5(serviceRoleJsonMd5);
        role.setLogFile(serviceRole.getLogFile());
    }

    /**
     * 构建服务实体 - 重构使用JDK 21新特性
     */
    private void buildServiceEntity(ServiceMetaConfig config, FrameServiceEntity serviceEntity) {
        var serviceInfo = config.serviceInfo();
        serviceEntity.setServiceName(config.serviceName());
        serviceEntity.setLabel(serviceInfo.getLabel());
        serviceEntity.setFrameId(config.frameInfo().getId());
        serviceEntity.setServiceDesc(serviceInfo.getDescription());
        serviceEntity.setServiceVersion(serviceInfo.getVersion());
        serviceEntity.setPackageName(serviceInfo.getPackageName());
        serviceEntity.setDependencies(StringUtils.join(serviceInfo.getDependencies(), ","));
        serviceEntity.setFrameCode(config.frameCode());
        serviceEntity.setServiceConfig(JSON.toJSONString(serviceInfo.getParameters()));
        serviceEntity.setServiceJson(config.serviceDdl());
        serviceEntity.setServiceJsonMd5(config.serviceInfoMd5());
        serviceEntity.setDecompressPackageName(config.decompressPackageName());
        serviceEntity.setConfigFileJson(JSONObject.toJSONString(config.configFileMap()));
        serviceEntity.setConfigFileJsonMd5(SecureUtil.md5(serviceEntity.getConfigFileJson()));
        serviceEntity.setSortNum(serviceInfo.getSortNum());
    }

    // 根据 IP 地址推断子网掩码
    public static String getSubnetFromIp(String ip) {
        if (ip == null) {
            return null;
        }

        // 拆分 IP 地址
        String[] ipParts = ip.split("\\."); // 将 IP 地址分割为四个部分
        if (ipParts.length != 4) {
            return null; // 无效的 IP 地址
        }

        int firstOctet = Integer.parseInt(ipParts[0]);

        // 根据 IP 地址的第一部分推断出适当的子网掩码
        String subnetMask;
        if (firstOctet >= 1 && firstOctet <= 126) {
            // A 类地址，使用 /8
            subnetMask = "/8";
        } else if (firstOctet >= 128 && firstOctet <= 191) {
            // B 类地址，使用 /16
            subnetMask = "/16";
        } else if (firstOctet >= 192 && firstOctet <= 223) {
            // C 类地址，使用 /24
            subnetMask = "/24";
        } else {
            // 其他情况，暂不处理
            subnetMask = "/24"; // 默认返回 /24
        }

        // 构造网络前缀
        String networkPrefix = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".0";
        return networkPrefix + subnetMask;
    }

    // 设置 priority_networks 参数
    public static String getPriorityNetworks(String ipAddress) {
        if (ipAddress != null) {
            // 根据 IP 地址获取子网
            return getSubnetFromIp(ipAddress);
        }
        return null;
    }

}
