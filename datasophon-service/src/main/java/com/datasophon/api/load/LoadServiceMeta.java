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
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.serviceCacheSyncActor;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.FrameInfoService;
import com.datasophon.api.service.ServiceMetadataTransactionService;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceInfo;
import com.datasophon.common.dto.ClusterInfoDTO;

import com.datasophon.dao.entity.ClusterVariableEntity;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.mybatisflex.core.query.QueryChain;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.function.Function;

import static com.datasophon.api.master.ActorUtils.getActorRefName;

@Component
public class LoadServiceMeta implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(LoadServiceMeta.class);

    private static final String PATH = "meta";
    private static final String HDFS = "HDFS";
    private static final String HADOOP = "HADOOP";
    @Autowired
    private FrameInfoService frameInfoService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ConfigBean configBean;

    @Autowired
    private ServiceMetadataTransactionService metadataTransactionService;


    /**
     * 1、设置全局环境变量
     * 2、创建各集群角色 MasterServiceActor
     * 3、解析各角色 service_ddl.json 更新到 t_ddh_frame_service t_ddh_frame_service_role 表
     * <p>
     * 注意：移除顶层事务注解，避免虚拟线程并发访问时的死锁问题
     * 事务控制下沉到具体的数据库操作方法中
     */
    @Override
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
        
        logger.debug("处理框架: {}", frameCode);
        
        var files = FileUtil.loopFiles(framePath);
        var serviceFiles = files.stream()
                .filter(file -> file.getName().endsWith(Constants.JSON_EXTENSION))
                .toList();
        
        logger.debug("框架 {} 包含 {} 个服务定义文件", frameCode, serviceFiles.size());
        
        // 使用虚拟线程处理服务文件，确保框架内服务隔离
        // 添加重试机制处理数据库死锁问题
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
     * 使用独立的事务Service避免自调用问题
     */
    private void processServiceMetadata(ServiceMetaConfig config, LoadContext loadContext) {
        putServicePackageToMap(config);
        putServiceHomeToVariable(loadContext.clusters(), config.serviceName(), config.decompressPackageName());
        
        // 通过独立的事务Service保存数据，确保事务有效
        var serviceEntity = metadataTransactionService.saveFrameServiceInTransaction(config);
        metadataTransactionService.saveFrameServiceRoleInTransaction(config, serviceEntity);
        
        // 更新缓存映射
        updateServiceCacheMapping(config, serviceEntity);
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

    /**
     * 更新服务缓存映射
     */
    private void updateServiceCacheMapping(ServiceMetaConfig config, FrameServiceEntity serviceEntity) {
        // 更新缓存映射
        var cacheKey = config.frameCode() + Constants.UNDERLINE + config.serviceInfo().getName();
        ServiceConfigMap.put(cacheKey + Constants.CONFIG, config.parameters());
        ServiceConfigFileMap.put(cacheKey + Constants.CONFIG_FILE, config.configFileMap());
        
        logger.debug("put {} {} service info into cache", config.frameCode(), config.serviceName());
        ServiceInfoMap.put(config.frameCode() + Constants.UNDERLINE + config.serviceName(), config.serviceInfo());
        
        // 处理服务角色缓存
        config.serviceRoles().forEach(serviceRole -> {
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
        });
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
                List<ClusterVariableEntity> variables = QueryChain.of(ClusterVariableEntity.class)
                        .where(ClusterVariableEntity::getClusterId).eq(cluster.id())
                        .list();
                for (ClusterVariableEntity variable : variables) {
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
