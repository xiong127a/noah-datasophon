package com.datasophon.api.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.datasophon.api.converter.FrameServiceConverter;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.TemplatePathUtils;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.FrameServiceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.datasophon.common.Constants.GENERAL;

/**
 * 配置分组工具类，提供统一的配置分组处理逻辑
 * <p>
 * 本类已全面使用JDK8-21新特性进行现代化改造，包括：
 * <ul>
 * <li><strong>JDK10:</strong> var 局部变量类型推断，简化代码可读性</li>
 * <li><strong>JDK11:</strong> String新方法(isBlank, formatted)、Collection.toArray(IntFunction)</li>
 * <li><strong>JDK15:</strong> Text blocks多行字符串，改进日志格式化</li>
 * <li><strong>JDK16:</strong> instanceof模式匹配、Records数据结构、Stream.toList()</li>
 * <li><strong>JDK17:</strong> 增强的switch表达式和模式匹配</li>
 * <li><strong>JDK21:</strong> 模式匹配优化、虚拟线程支持、现代集合API</li>
 * </ul>
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-12
 * @since JDK21
 */
public class ConfigGroupUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConfigGroupUtils.class);

    private ConfigGroupUtils() {
        // 私有构造函数，防止实例化
    }

    /**
     * 统一的配置角色分组处理方法
     *
     * @param configTargetRoles 目标角色配置字符串
     * @return 分割后的角色名称集合
     */
    public static Set<String> parseRoleNames(String configTargetRoles) {
        if (configTargetRoles == null || configTargetRoles.isEmpty()) {
            return Set.of();
        }

        // 使用JDK21 Stream API优化处理逻辑
        return configTargetRoles.contains(",") 
            ? Arrays.stream(configTargetRoles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty() && !GENERAL.equals(role))
                .collect(Collectors.toSet())
            : GENERAL.equals(configTargetRoles.trim()) 
                ? Set.of() 
                : Set.of(configTargetRoles.trim());
    }

    /**
     * 从generators收集所有角色名
     *
     * @param generators generators列表
     * @return 收集到的角色名集合
     */
    public static Set<String> collectRoleNamesFromGenerators(List<Generators> generators) {
        return generators.stream()
            .map(Generators::getConfigTargetRoles)
            .map(ConfigGroupUtils::parseRoleNames)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    }

    /**
     * 从服务定义中获取原始Kubernetes配置
     *
     * @param frameCode   框架代码
     * @param serviceName 服务名称
     * @return 按Kubernetes配置类型分组的原始配置映射
     */
    private static Map<String, List<ServiceConfig>> getOriginalKubernetesConfigs(String frameCode, String serviceName) {
        Map<String, List<ServiceConfig>> kubernetesConfigsByType = new HashMap<>();

        try {
            // 从Spring上下文获取服务 - 使用var简化类型声明
            var frameServiceService = SpringUtil.getBean(FrameServiceService.class);

            // 获取服务定义 - 使用var简化类型声明
            var frameServiceConverter = SpringUtil.getBean(FrameServiceConverter.class);
            var frameServiceDTO = frameServiceService.getServiceByFrameCodeAndServiceName(frameCode, serviceName);
            var frameServiceEntity = frameServiceDTO != null ? 
                    frameServiceConverter.dtoToEntity(frameServiceDTO) : null;

            if (frameServiceEntity == null) {
                logger.error("无法获取服务定义: {}", serviceName);
                return kubernetesConfigsByType;
            }

            // 解析服务JSON - 使用var和JDK11的isBlank()优化
            var serviceJson = frameServiceEntity.getServiceJson();
            if (serviceJson == null || serviceJson.isBlank()) {
                logger.error("服务定义JSON为空: {}", serviceName);
                return kubernetesConfigsByType;
            }

            var serviceObj = JSONObject.parseObject(serviceJson);

            // 获取parameters数组并转为ServiceConfig列表
            var parameters = serviceObj.getJSONArray("parameters");
            if (parameters == null || parameters.isEmpty()) {
                logger.warn("服务定义中没有parameters数组，无法获取原始配置");
                return kubernetesConfigsByType;
            }

            // 遍历parameters，过滤出Kubernetes配置并按类型分组 - 使用JDK16 instanceof模式匹配
            parameters.stream()
                .filter(param -> param instanceof JSONObject)
                .map(param -> (JSONObject) param)
                .filter(paramJson -> {
                    var configGroup = paramJson.getString("configGroup");
                    return configGroup != null && configGroup.startsWith(Constants.KUBERNETES_CONFIG_PREFIX);
                })
                .forEach(paramJson -> {
                    // 将JSON转为ServiceConfig对象 - 使用var
                    var config = paramJson.toJavaObject(ServiceConfig.class);
                    
                    // 提取Kubernetes配置类型 - 使用var
                    var kubernetesConfigType = extractKubernetesConfigType(paramJson.getString("configGroup"));
                    
                    // 按类型分组
                    kubernetesConfigsByType.computeIfAbsent(kubernetesConfigType, k -> new ArrayList<>()).add(config);
                });

        } catch (Exception e) {
            logger.error("获取原始Kubernetes配置时出错: {}", e.getMessage(), e);
        }

        return kubernetesConfigsByType;
    }

    /**
     * 预处理配置列表，处理Kubernetes配置的角色分组
     *
     * @param list        配置列表
     * @param frameCode   框架代码
     * @param serviceName 服务名称
     * @return 处理后的配置列表
     */
    public static List<ServiceConfig> preprocessKubernetesConfigs(List<ServiceConfig> list, String frameCode,
            String serviceName) {
        if (list == null || list.isEmpty()) {
            return list;
        }

        // 1. 获取服务定义中的所有配置文件及其角色列表 - 使用var
        var configFileToRolesMap = getConfigFileRolesMap(frameCode, serviceName);

        if (configFileToRolesMap.isEmpty()) {
            logger.warn("无法获取服务 {} 的配置文件角色映射，将保持原始配置", serviceName);
            return list;
        }

        // 2. 从服务定义中获取原始Kubernetes配置 - 使用var
        var originalKubernetesConfigsByType = getOriginalKubernetesConfigs(frameCode, serviceName);

        if (originalKubernetesConfigsByType.isEmpty()) {
            logger.warn("无法从服务定义中获取原始Kubernetes配置，将使用传入的配置列表");
            // 回退到原有逻辑，使用传入的配置列表
        }

        // 3. 对配置进行分类 - 使用var
        var processedConfigs = new ArrayList<ServiceConfig>();
        var nonKubernetesConfigs = new ArrayList<ServiceConfig>();
        var portConfigs = new HashMap<String, ServiceConfig>(); // 存储端口配置，用于后续处理

        // 4. 获取非Kubernetes配置和端口配置
        for (ServiceConfig config : list) {
            // 只处理非Kubernetes配置，Kubernetes配置将使用从服务定义中获取的原始配置
            if (config.getConfigGroup() == null || !config.getConfigGroup().startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
                // 检查是否有端口绑定相关的配置
                if (config.getBindRole() != null && config.getPortNumber() != null) {
                    // 使用配置名称作为键，存储端口配置
                    portConfigs.put(config.getName(), config);
                }

                // 非Kubernetes配置，直接保留
                nonKubernetesConfigs.add(config);
            }
        }

        // 5. 处理Kubernetes配置（使用原始配置或回退到传入的配置）- 使用var
        var kubernetesConfigsByType = originalKubernetesConfigsByType;
        if (kubernetesConfigsByType.isEmpty()) {
            // 如果没有获取到原始配置，回退到使用传入的配置
            kubernetesConfigsByType = new HashMap<>();
            for (var config : list) {
                var configGroup = config.getConfigGroup();
                if (configGroup != null && configGroup.startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
                    var kubernetesConfigType = extractKubernetesConfigType(configGroup);
                    kubernetesConfigsByType.computeIfAbsent(kubernetesConfigType, k -> new ArrayList<>()).add(config);
                }
            }
        }

        // 6. 为每个配置类型创建角色特定的配置 - 使用var
        for (var entry : kubernetesConfigsByType.entrySet()) {
            var kubernetesConfigType = entry.getKey();
            var configs = entry.getValue();

            // 获取该类型配置的角色集合 - 使用var
            var targetRoles = getTargetRolesForConfigType(configFileToRolesMap, kubernetesConfigType);

            if (targetRoles.isEmpty()) {
                logger.warn("无法确定Kubernetes配置类型 {} 的目标角色，将使用通用角色", kubernetesConfigType);
                targetRoles = new HashSet<>(List.of(GENERAL));
            }

            // 为每个角色创建配置副本 - 使用var
            for (var roleName : targetRoles) {
                for (var config : configs) {
                    // 创建配置副本（使用原始配置） - 使用var
                    var copy = ObjectUtil.cloneByStream(config);

                    // 设置单一角色
                    copy.setConfigTargetRoles(roleName);

                    // 设置角色特定的configGroup
                    copy.setConfigGroup(Constants.KUBERNETES_CONFIG_PREFIX + kubernetesConfigType + "." + roleName);

                    // 添加角色前缀到name
                    addRolePrefixToName(copy, roleName);

                    // 添加到处理后的配置列表
                    processedConfigs.add(copy);
                }
            }
        }

        // 7. 处理端口配置
        if (!portConfigs.isEmpty()) {
            processPortConfigs(portConfigs, processedConfigs);
        }

        // 8. 添加非Kubernetes配置到结果列表
        processedConfigs.addAll(nonKubernetesConfigs);
        // 添加JMX端口处理逻辑
        processJmxPorts(processedConfigs);
        return processedConfigs;
    }

    /**
     * 处理端口配置，将bindRole、serviceType、portNumber和nodePort信息应用到相应的配置中
     *
     * @param portConfigs      包含端口信息的配置
     * @param processedConfigs 处理后的配置列表
     */
    private static void processPortConfigs(Map<String, ServiceConfig> portConfigs,
            List<ServiceConfig> processedConfigs) {
        // 遍历所有端口配置 - 使用var
        for (var portConfig : portConfigs.values()) {
            var bindRole = portConfig.getBindRole();
            var portNumber = portConfig.getPortNumber();
            var serviceType = portConfig.getServiceType();
            var nodePort = portConfig.getNodePort();

            // 跳过无效的配置
            if (bindRole == null || portNumber == null) {
                continue;
            }

            // 遍历bindRole中的所有角色 - 使用var
            for (var role : bindRole.split(",")) {
                var roleName = role.trim().toLowerCase();

                // 构建要查找的配置名称 - 使用var
                var nodePortMappingName = roleName + "_node_port_mappings";
                var clusterPortMappingName = roleName + "_cluster_port_mappings";
                var loadBalancerMappingName = roleName + "_load_balancer_port_mappings";

                // 直接查找和更新匹配的配置 - 使用var
                for (var config : processedConfigs) {
                    var configName = config.getName();
                    if (configName == null) {
                        continue;
                    }

                    // 处理NodePort类型的端口映射
                    if (configName.equals(nodePortMappingName) && "NodePort".equalsIgnoreCase(serviceType)
                            && nodePort != null) {
                        updatePortMapping(config, portNumber, nodePort);
                        logger.debug("Updated {} for role {}: port {} -> nodePort {}",
                                nodePortMappingName, roleName, portNumber, nodePort);
                    }

                    // 处理LoadBalancer类型的端口映射
                    if (configName.equals(loadBalancerMappingName) && "LoadBalancer".equalsIgnoreCase(serviceType)) {
                        updatePortMapping(config, portNumber, portNumber);
                        logger.debug("Updated {} for role {}: port {} for LoadBalancer",
                                loadBalancerMappingName, roleName, portNumber);
                    }

                    // 处理ClusterIP类型的端口映射
                    if (configName.equals(clusterPortMappingName)) {
                        updatePortMapping(config, portNumber, portNumber);
                        logger.debug("Updated {} for role {}: port {}",
                                clusterPortMappingName, roleName, portNumber);
                    }
                }
            }
        }
    }

    /**
     * 处理JMX端口，将角色定义中的JMX端口添加到集群IP端口映射中
     * 
     * @param processedConfigs 处理后的配置列表
     */
    private static void processJmxPorts(List<ServiceConfig> processedConfigs) {
        // 从Spring上下文获取服务
        FrameServiceService frameServiceService = SpringUtil
                .getBean(FrameServiceService.class);

        // 收集所有角色配置映射 - 使用Stream API优化
        Map<String, ServiceConfig> clusterPortMappingConfigs = processedConfigs.stream()
            .filter(config -> config.getName() != null && config.getName().endsWith("_cluster_port_mappings"))
            .collect(Collectors.toMap(
                config -> config.getName().substring(0, config.getName().length() - "_cluster_port_mappings".length()),
                config -> config
            ));

        // 如果没有找到任何集群端口映射配置，直接返回
        if (clusterPortMappingConfigs.isEmpty()) {
            logger.info("未找到任何集群端口映射配置，跳过JMX端口处理");
            return;
        }

        try {
            // 获取所有服务定义
            List<FrameServiceEntity> allServices = frameServiceService.list();

            // 使用Stream API优化服务处理逻辑
            allServices.stream()
                .filter(service -> StrUtil.isNotBlank(service.getServiceJson()))
                .forEach(service -> {
                    JSONObject serviceObj = JSONObject.parseObject(service.getServiceJson());
                    JSONArray roles = serviceObj.getJSONArray("roles");

                    if (roles != null && !roles.isEmpty()) {
                        // 遍历所有角色 - 使用Stream API
                        roles.stream()
                            .map(role -> (JSONObject) role)
                            .filter(roleObj -> roleObj.getString("name") != null && roleObj.get("jmxPort") != null)
                            .forEach(roleObj -> {
                                String roleName = roleObj.getString("name");
                                String jmxPort = String.valueOf(roleObj.get("jmxPort"));

                                // 检查是否是有效的端口号
                                if (StrUtil.isNotBlank(jmxPort) && !"null".equals(jmxPort)) {
                                    processJmxPortForRole(clusterPortMappingConfigs, roleName, jmxPort);
                                }
                            });
                    }
                });
        } catch (Exception e) {
            logger.error("处理JMX端口时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 为特定角色处理JMX端口
     *
     * @param clusterPortMappingConfigs 集群端口映射配置
     * @param roleName                  角色名
     * @param jmxPort                   JMX端口
     */
    private static void processJmxPortForRole(Map<String, ServiceConfig> clusterPortMappingConfigs, 
                                              String roleName, String jmxPort) {
        // 将角色名转为小写下划线格式 - 使用var
        var normRoleName = roleName.toLowerCase().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();

        // 特殊处理ZKFC角色，将其JMX端口添加到NameNode的端口映射中 - 使用var
        if ("ZKFC".equals(roleName)) {
            var namenodeRoleName = "namenode"; // NameNode的标准化角色名
            var namenodeConfig = clusterPortMappingConfigs.get(namenodeRoleName);

            if (namenodeConfig != null) {
                // 更新NameNode的端口映射，添加ZKFC的JMX端口
                updatePortMapping(namenodeConfig, jmxPort, jmxPort);
                logger.info("将ZKFC的JMX端口 {} 添加到NameNode的cluster_port_mappings中", jmxPort);
            } else {
                logger.warn("未找到NameNode的端口映射配置，无法添加ZKFC的JMX端口 {}", jmxPort);
            }
        } else {
            // 查找对应的集群端口映射配置 - 使用var
            var clusterPortConfig = clusterPortMappingConfigs.get(normRoleName);

            if (clusterPortConfig != null) {
                // 更新端口映射，添加JMX端口
                updatePortMapping(clusterPortConfig, jmxPort, jmxPort);
                logger.debug("添加JMX端口 {} 到cluster_port_mappings，角色: {}", jmxPort, roleName);
            }
        }
    }

    /**
     * 解析字符串类型的端口映射
     *
     * @param s 端口映射字符串
     * @return 解析后的端口映射列表
     */
    private static List<Map<String, String>> parseStringPortMappings(String s) {
        var portMappings = new ArrayList<Map<String, String>>();
        try {
            portMappings = JSONObject.parseObject(s, new TypeReference<>() {});
        } catch (Exception e) {
            logger.warn("解析端口映射字符串失败，尝试其他格式: {}", e.getMessage());
            // 尝试解析为JSONArray - 使用var
            try {
                var jsonArray = JSONArray.parseArray(s);
                portMappings = new ArrayList<>(jsonArray.stream()
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(jsonObj -> {
                        var mapping = new HashMap<String, String>();
                        jsonObj.keySet().forEach(key -> mapping.put(key, jsonObj.getString(key)));
                        return mapping;
                    })
                    .toList());
            } catch (Exception ex) {
                logger.warn("解析JSONArray格式也失败: {}", ex.getMessage());
            }
        }
        return portMappings;
    }

    /**
     * 解析List类型的端口映射
     *
     * @param rawList 原始List对象
     * @return 解析后的端口映射列表
     */
    private static List<Map<String, String>> parseListPortMappings(List<?> rawList) {
        return rawList.stream()
            .filter(Map.class::isInstance)
            .map(item -> (Map<?, ?>) item)
            .map(rawMap -> {
                var convertedMap = new HashMap<String, String>();
                rawMap.entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .forEach(entry -> {
                        var key = entry.getKey().toString();
                        var value = entry.getValue() != null ? entry.getValue().toString() : "";
                        convertedMap.put(key, value);
                    });
                return convertedMap;
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 更新端口映射配置
     *
     * @param config     配置对象
     * @param port       端口号
     * @param mappedPort 映射的端口号
     */
    private static void updatePortMapping(ServiceConfig config, String port, String mappedPort) {
        try {
            // 获取当前值 - 使用var
            var currentValue = config.getValue();
            
            // 解析当前值 - 使用JDK21增强的switch表达式和var
            var portMappings = switch (currentValue) {
                case null -> new ArrayList<Map<String, String>>(); // 空值时返回空列表
                case String s -> parseStringPortMappings(s);
                case List<?> rawList -> parseListPortMappings(rawList);
                default -> {
                                // 使用JDK15 Text blocks改进日志格式
            var errorMessage = """
                    无法处理的端口映射值:
                    - 类型: %s
                    - 内容: %s
                    """.formatted(currentValue.getClass().getName(), currentValue);
            logger.warn(errorMessage.trim());
                    yield new ArrayList<Map<String, String>>(); // 返回空列表而不是return
                }
            };

            // 检查是否已存在相同的端口映射 - 使用var
            var found = false;
            for (var mapping : portMappings) {
                if (mapping.containsKey(port)) {
                    // 获取现有的映射值 - 使用var
                    var existingValue = mapping.get(port);
                    // 如果新值不在现有值中，则追加
                    if (existingValue != null && !existingValue.contains(mappedPort)) {
                        // 使用逗号分隔追加新的端口值
                        mapping.put(port, existingValue + "," + mappedPort);
                        logger.info("追加新端口映射 {} 到现有端口 {}", mappedPort, port);
                    } else if (existingValue == null) {
                        // 如果现有值为null，直接设置
                        mapping.put(port, mappedPort);
                        logger.debug("更新端口 {} 的映射为 {}", port, mappedPort);
                    } else {
                        logger.debug("端口 {} 已存在映射 {}，跳过", port, mappedPort);
                    }
                    found = true;
                    break;
                }
            }

            // 如果不存在，添加新的映射 - 使用var
            if (!found) {
                var newMapping = new HashMap<String, String>();
                newMapping.put(port, mappedPort);
                portMappings.add(newMapping);
                logger.debug("添加新端口映射: {} -> {}", port, mappedPort);
            }

            // 更新配置值
            config.setValue(portMappings);
        } catch (Exception e) {
            logger.error("更新端口映射失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从服务定义中获取所有配置文件及其目标角色
     *
     * @param frameCode   框架代码
     * @param serviceName 服务名称
     * @return 配置文件到角色集合的映射
     */
    private static Map<String, Set<String>> getConfigFileRolesMap(String frameCode, String serviceName) {
        Map<String, Set<String>> result = new HashMap<>();

        try {
            // 从Spring上下文获取服务 - 使用var
            var frameServiceService = SpringUtil.getBean(FrameServiceService.class);
            var frameServiceConverter = SpringUtil.getBean(FrameServiceConverter.class);

            // 获取服务定义 - 使用var
            var frameServiceDTO = frameServiceService.getServiceByFrameCodeAndServiceName(frameCode, serviceName);
            var frameServiceEntity = frameServiceDTO != null ? 
                    frameServiceConverter.dtoToEntity(frameServiceDTO) : null;

            if (frameServiceEntity == null) {
                logger.error("无法获取服务定义: {}", serviceName);
                return result;
            }

            // 解析服务JSON - 使用var和JDK11的isBlank()
            var serviceJson = frameServiceEntity.getServiceJson();
            if (serviceJson == null || serviceJson.isBlank()) {
                logger.error("服务定义JSON为空: {}", serviceName);
                return result;
            }

            var serviceObj = JSONObject.parseObject(serviceJson);

            // 记录服务定义中的parameters - 使用var
            var parameters = serviceObj.getJSONArray("parameters");
            var configWriter = serviceObj.getJSONObject("configWriter");
            if (configWriter == null) {
                logger.warn("服务定义中没有configWriter对象");
                return result;
            }

            var generators = configWriter.getJSONArray("generators");
            if (generators == null || generators.isEmpty()) {
                logger.warn("服务定义中没有配置文件定义: {}", serviceName);
                return result;
            }

            // 解析所有配置文件定义，关键是将filename和configTargetRoles对应起来 - 使用var
            var filenameToRolesMap = new HashMap<String, Set<String>>();

            for (var i = 0; i < generators.size(); i++) {
                var generator = generators.getJSONObject(i);
                var filename = generator.getString("filename");
                var configTargetRoles = generator.getString("configTargetRoles");

                // 检查是否有Kubernetes相关的配置文件
                if (filename != null && filename.toLowerCase().startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
                    var roles = new HashSet<String>();
                    // 解析角色列表 - 使用JDK11的isBlank()和var
                    if (configTargetRoles != null && !configTargetRoles.isBlank()) {
                        for (var role : configTargetRoles.split(",")) {
                            var trimmedRole = role.trim();
                            if (!trimmedRole.isEmpty()) {
                                roles.add(trimmedRole);
                            }
                        }
                    }

                    // 重要：将filename映射到对应的角色列表
                    filenameToRolesMap.put(filename, roles);
                }
            }

            // 第二步：遍历参数定义，将configGroup与filename进行匹配 - 使用var
            if (parameters != null && !parameters.isEmpty()) {
                for (var i = 0; i < parameters.size(); i++) {
                    var parameter = parameters.getJSONObject(i);
                    var configGroup = parameter.getString("configGroup");

                    // 如果configGroup以kubernetes.config.开头，查找对应的filename
                    if (configGroup != null && configGroup.startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
                        // 查找匹配的filename（就是configGroup完全一致的filename） - 使用var
                        var roles = filenameToRolesMap.get(configGroup);

                        if (roles != null && !roles.isEmpty()) {
                            // 将configGroup映射到对应的角色
                            result.put(configGroup, roles);

                            // 同时将参数名映射到角色 - 使用var和JDK11的isBlank()
                            var paramName = parameter.getString("name");
                            if (paramName != null && !paramName.isBlank()) {
                                result.put(paramName, roles);
                            }
                        } else {
                            logger.warn("参数 {} 的configGroup {} 没有找到对应的角色",
                                    parameter.getString("name"), configGroup);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("获取配置文件角色映射时出错：{}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 根据配置类型获取目标角色
     *
     * @param configFileToRolesMap 配置文件到角色集合的映射
     * @param configType           配置类型
     * @return 目标角色集合
     */
    private static Set<String> getTargetRolesForConfigType(Map<String, Set<String>> configFileToRolesMap,
            String configType) {
        Set<String> result = new HashSet<>();

        // 完整的configGroup
        String fullConfigGroup = Constants.KUBERNETES_CONFIG_PREFIX + configType;

        // 1. 优先尝试完全匹配
        if (configFileToRolesMap.containsKey(fullConfigGroup)) {
            return configFileToRolesMap.get(fullConfigGroup);
        }

        // 2. 如果没有找到完全匹配，尝试模糊匹配 - 使用var
        for (var entry : configFileToRolesMap.entrySet()) {
            var key = entry.getKey();

            // 如果key包含configType或configType包含key
            if (key.contains(configType) || configType.contains(key)) {
                result.addAll(entry.getValue());
            }
        }

        return result;
    }

    /**
     * 提取Kubernetes配置类型
     *
     * @param configGroup 配置组
     * @return Kubernetes配置类型（如persistentVolumeClaims或resources）
     */
    private static String extractKubernetesConfigType(String configGroup) {
        // kubernetes.config.persistent-volume-claims 或 kubernetes.config.resources 等
        String[] parts = configGroup.split("\\.");
        if (parts.length >= 3) {
            return parts[2]; // 返回第三部分作为配置类型
        }
        return "unknown";
    }

    /**
     * 添加角色前缀到配置名称
     *
     * @param config   配置项
     * @param roleName 角色名
     */
    private static void addRolePrefixToName(ServiceConfig config, String roleName) {
        String configName = config.getName();
        if (configName == null) {
            return;
        }

        // 将roleName转为小写并将驼峰转为下划线格式 - 使用var
        var normRoleName = roleName.toLowerCase().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();

        // 直接添加前缀（不需要检查，因为我们从原始配置创建） - 使用JDK11的formatted方法
        config.setName("%s_%s".formatted(normRoleName, configName));
    }

    /**
     * 将配置项按角色和类型进行分组
     * 新的分组逻辑：
     * 1. 普通配置 → General分组
     * 2. kubernetes.config.{type}.{role} → {role}分组下的k8s子分组{type}
     *
     * @param list 配置项列表
     * @return 按角色分组的配置映射
     */
    public static Map<String, List<ServiceConfig>> groupByConfigTargetRoleOrCommon(List<ServiceConfig> list) {
        // 先处理所有配置项的模板内容
        if (list != null) {
            for (var config : list) {
                var templateName = config.getTemplateName();
                if (templateName != null && !templateName.isBlank()) {
                    var templateContent = TemplatePathUtils.getTemplateContent(templateName);
                    config.setTemplateContent(templateContent);
                }
            }
        }

        // 存储最终分组结果：角色 -> 配置列表
        var finalGroupedConfigs = new LinkedHashMap<String, List<ServiceConfig>>();

        if (list != null) {
            for (var config : list) {
                var configGroup = config.getConfigGroup();
                
                // 处理kubernetes.config类型的配置
                if (configGroup != null && configGroup.startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
                    // 解析kubernetes配置：kubernetes.config.{type}.{role}
                    var parts = configGroup.split("\\.");
                    if (parts.length >= 4) {
                        var type = parts[2]; // persistent-volume-claims, resources, services
                        var role = parts[3]; // NodeExporter, Prometheus等
                        
                        // 将配置添加到对应角色分组
                        finalGroupedConfigs.computeIfAbsent(role, k -> new ArrayList<>()).add(config);
                        
                        logger.debug("Kubernetes配置 {} 分组到角色: {}, 类型: {}", config.getName(), role, type);
                    } else {
                        // 格式不正确的kubernetes配置，放入General
                        finalGroupedConfigs.computeIfAbsent(GENERAL, k -> new ArrayList<>()).add(config);
                        logger.warn("Kubernetes配置格式不正确: {}, 已分组到General", configGroup);
                    }
                }
                // 处理普通配置，都放入General分组
                else {
                    finalGroupedConfigs.computeIfAbsent(GENERAL, k -> new ArrayList<>()).add(config);
                    logger.debug("普通配置 {} 分组到General", config.getName());
                }
            }
        }

        // 处理空键分组，将其合并到General分组中
        if (finalGroupedConfigs.containsKey("")) {
            var emptyGroupConfigs = finalGroupedConfigs.remove("");
            finalGroupedConfigs.computeIfAbsent(GENERAL, k -> new ArrayList<>()).addAll(emptyGroupConfigs);
            logger.warn("发现空键分组，已将 {} 个配置项合并到General分组中", emptyGroupConfigs.size());
        }

        // 确保每个配置组内的配置名称唯一
        for (var entry : finalGroupedConfigs.entrySet()) {
            var uniqueNameMap = new LinkedHashMap<String, ServiceConfig>();
            for (var config : entry.getValue()) {
                uniqueNameMap.put(config.getName(), config);
            }
            entry.setValue(new ArrayList<>(uniqueNameMap.values()));
        }

        logger.info("配置分组完成，共 {} 个角色分组: {}", finalGroupedConfigs.size(), finalGroupedConfigs.keySet());
        return finalGroupedConfigs;
    }



    /**
     * 为Kubernetes配置生成角色前缀的配置映射
     *
     * @param configFileMap 结果配置映射
     * @param config        角色组配置
     * @param clusterId     集群ID
     */
    public static void generateConfigFileMap(Map<Generators, List<ServiceConfig>> configFileMap,
            ClusterServiceRoleGroupConfig config, Integer clusterId) {

        // 1. 解析配置文件JSON
        Map<JSONObject, JSONArray> originalConfigMap = parseConfigJson(config.getConfigFileJson());

        // 2. 收集服务角色名
        Set<String> roleNames = collectRoleNames(originalConfigMap);

        // 3. 根据角色名处理配置
        if (roleNames.isEmpty()) {
            // 无角色信息，直接处理原始配置
            processOriginalConfig(configFileMap, originalConfigMap, clusterId);
        } else {
            // 有角色信息，分别处理Kubernetes和非Kubernetes配置
            processConfigWithRoles(configFileMap, originalConfigMap, roleNames, clusterId);
        }

        // 4. 从configJson获取配置值并应用到configFileMap
        List<ServiceConfig> configs = JSONArray.parseArray(config.getConfigJson(), ServiceConfig.class);

        if (CollUtil.isNotEmpty(configs)) {
            logger.info("从configJson解析出 {} 个配置项", configs.size());

            // 创建名称到配置的映射，便于快速查找
            Map<String, ServiceConfig> configNameMap = new HashMap<>();
            for (ServiceConfig config1 : configs) {
                if (config1.getName() != null) {
                    configNameMap.put(config1.getName(), config1);
                }
            }

            // 遍历configFileMap并更新配置值
            for (Map.Entry<Generators, List<ServiceConfig>> entry : configFileMap.entrySet()) {
                List<ServiceConfig> serviceConfigs = entry.getValue();
                if (serviceConfigs != null) {
                    for (ServiceConfig serviceConfig : serviceConfigs) {
                        String configName = serviceConfig.getName();
                        if (configName != null && configNameMap.containsKey(configName)) {
                            // 找到匹配的配置项，更新值
                            ServiceConfig matchedConfig = configNameMap.get(configName);
                            serviceConfig.setValue(matchedConfig.getValue());
                            logger.debug("更新配置项 {}: 值={}", configName, matchedConfig.getValue());
                        }
                    }
                }
            }

            logger.info("已完成configFileMap的配置值更新");
        } else {
            logger.warn("configJson中没有有效的配置项");
        }
    }

    /**
     * 解析配置JSON字符串为Map
     *
     * @param configFileJson 配置文件JSON字符串
     * @return 解析后的Map
     */
    private static Map<JSONObject, JSONArray> parseConfigJson(String configFileJson) {
        return JSONObject.parseObject(configFileJson,
                new TypeReference<>() {
                });
    }

    /**
     * 从配置信息中收集角色名
     *
     * @param configMap 配置映射
     * @return 角色名集合
     */
    private static Set<String> collectRoleNames(Map<JSONObject, JSONArray> configMap) {
        List<Generators> generatorsList = new ArrayList<>();
        for (JSONObject fileJson : configMap.keySet()) {
            Generators generator = fileJson.toJavaObject(Generators.class);
            generatorsList.add(generator);
        }

        return collectRoleNamesFromGenerators(generatorsList);
    }

    /**
     * 处理没有角色信息的原始配置
     *
     * @param resultMap   结果配置映射
     * @param originalMap 原始配置映射
     * @param clusterId   集群ID
     */
    private static void processOriginalConfig(
            Map<Generators, List<ServiceConfig>> resultMap,
            Map<JSONObject, JSONArray> originalMap,
            Integer clusterId) {

        logger.warn("没有找到任何角色名，无法为Kubernetes配置添加前缀");

        for (JSONObject fileJson : originalMap.keySet()) {
            Generators generator = fileJson.toJavaObject(Generators.class);
            List<ServiceConfig> serviceConfigs = originalMap.get(fileJson).toJavaList(ServiceConfig.class);

            // 替换变量
            replaceVariable(serviceConfigs, clusterId);
            resultMap.put(generator, serviceConfigs);
        }
    }

    /**
     * 处理带有角色信息的配置
     *
     * @param resultMap   结果配置映射
     * @param originalMap 原始配置映射
     * @param roleNames   角色名集合
     * @param clusterId   集群ID
     */
    private static void processConfigWithRoles(
            Map<Generators, List<ServiceConfig>> resultMap,
            Map<JSONObject, JSONArray> originalMap,
            Set<String> roleNames,
            Integer clusterId) {

        // 1. 分离Kubernetes和非Kubernetes配置
        Map<String, List<ServiceConfig>> kubernetesConfigsByType = new HashMap<>();
        Map<Generators, List<ServiceConfig>> nonKubernetesConfigs = new HashMap<>();

        separateKubernetesAndNonKubernetesConfigs(originalMap, kubernetesConfigsByType, nonKubernetesConfigs, clusterId);

        // 2. 处理Kubernetes配置
        processKubernetesConfigs(resultMap, originalMap, kubernetesConfigsByType, roleNames, clusterId);

        // 3. 添加非Kubernetes配置到结果
        resultMap.putAll(nonKubernetesConfigs);
    }

    /**
     * 分离Kubernetes和非Kubernetes配置
     *
     * @param originalMap      原始配置映射
     * @param kubernetesConfigsByType Kubernetes配置映射（按类型分组）
     * @param nonKubernetesConfigs    非Kubernetes配置映射
     * @param clusterId        集群ID
     */
    private static void separateKubernetesAndNonKubernetesConfigs(
            Map<JSONObject, JSONArray> originalMap,
            Map<String, List<ServiceConfig>> kubernetesConfigsByType,
            Map<Generators, List<ServiceConfig>> nonKubernetesConfigs,
            Integer clusterId) {

        for (JSONObject fileJson : originalMap.keySet()) {
            Generators generator = fileJson.toJavaObject(Generators.class);
            List<ServiceConfig> originalConfigs = originalMap.get(fileJson).toJavaList(ServiceConfig.class);

            // 分拣Kubernetes配置和非Kubernetes配置
            List<ServiceConfig> kubernetesConfigs = new ArrayList<>();
            List<ServiceConfig> otherConfigs = new ArrayList<>();

            for (ServiceConfig serviceConfig : originalConfigs) {
                if (serviceConfig.getConfigGroup() != null
                        && serviceConfig.getConfigGroup().startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
                    kubernetesConfigs.add(serviceConfig);
                } else {
                    otherConfigs.add(serviceConfig);
                }
            }

            // 处理非Kubernetes配置
            if (!otherConfigs.isEmpty()) {
                replaceVariable(otherConfigs, clusterId);
                nonKubernetesConfigs.put(generator, otherConfigs);
            }

            // 按类型分组Kubernetes配置
            for (ServiceConfig kubernetesConfig : kubernetesConfigs) {
                String kubernetesConfigType = extractKubernetesConfigType(kubernetesConfig.getConfigGroup());
                kubernetesConfigsByType.computeIfAbsent(kubernetesConfigType, k -> new ArrayList<>()).add(kubernetesConfig);
            }
        }
    }

    /**
     * 处理Kubernetes配置
     *
     * @param resultMap        结果配置映射
     * @param originalMap      原始配置映射
     * @param kubernetesConfigsByType Kubernetes配置映射（按类型分组）
     * @param roleNames        角色名集合
     * @param clusterId        集群ID
     */
    private static void processKubernetesConfigs(
            Map<Generators, List<ServiceConfig>> resultMap,
            Map<JSONObject, JSONArray> originalMap,
            Map<String, List<ServiceConfig>> kubernetesConfigsByType,
            Set<String> roleNames,
            Integer clusterId) {

        // 处理每种类型的Kubernetes配置
        for (Map.Entry<String, List<ServiceConfig>> entry : kubernetesConfigsByType.entrySet()) {
            String kubernetesConfigType = entry.getKey();
            List<ServiceConfig> configs = entry.getValue();

            // 1. 为每个角色创建配置副本
            List<ServiceConfig> allKubernetesConfigs = createConfigsForRoles(configs, kubernetesConfigType, roleNames);

            // 2. 替换变量
            replaceVariable(allKubernetesConfigs, clusterId);

            // 3. 查找并使用原始Generator对象
            findAndUseOriginalGenerator(resultMap, originalMap, kubernetesConfigType, allKubernetesConfigs);
        }
    }

    /**
     * 为每个角色创建配置副本
     *
     * @param configs       配置列表
     * @param kubernetesConfigType Kubernetes配置类型
     * @param roleNames     角色名集合
     * @return 创建的配置列表
     */
    private static List<ServiceConfig> createConfigsForRoles(
            List<ServiceConfig> configs,
            String kubernetesConfigType,
            Set<String> roleNames) {

        List<ServiceConfig> allConfigs = new ArrayList<>();

        // 为每个角色创建配置副本
        for (String roleName : roleNames) {

            for (ServiceConfig config : configs) {
                // 创建配置副本（使用原始配置）
                ServiceConfig newConfig = ObjectUtil.cloneByStream(config);

                // 设置角色特定的configGroup
                newConfig.setConfigGroup(Constants.KUBERNETES_CONFIG_PREFIX + kubernetesConfigType + "." + roleName);

                // 添加角色前缀到name
                addRolePrefixToName(newConfig, roleName);

                allConfigs.add(newConfig);
            }
        }

        return allConfigs;
    }

    /**
     * 查找并使用原始Generator对象
     *
     * @param resultMap     结果配置映射
     * @param originalMap   原始配置映射
     * @param kubernetesConfigType Kubernetes配置类型
     * @param configs       配置列表
     */
    private static void findAndUseOriginalGenerator(
            Map<Generators, List<ServiceConfig>> resultMap,
            Map<JSONObject, JSONArray> originalMap,
            String kubernetesConfigType,
            List<ServiceConfig> configs) {

        // 查找原始Generator对象
        Generators originalGenerator = null;
        for (JSONObject generatorJson : originalMap.keySet()) {
            Generators generator = generatorJson.toJavaObject(Generators.class);
            if (generator.getFilename() != null &&
                    generator.getFilename().equals(Constants.KUBERNETES_CONFIG_PREFIX + kubernetesConfigType)) {
                originalGenerator = generator;
                break;
            }
        }

        // 使用原始Generator对象
        if (originalGenerator != null) {
            resultMap.put(originalGenerator, configs);
        } else {
            logger.warn("无法为Kubernetes配置类型 {} 找到原始Generator对象", kubernetesConfigType);
        }
    }

    /**
     * 替换配置中的变量
     *
     * @param serviceConfigs 配置列表
     * @param clusterId      集群ID
     */
    private static void replaceVariable(List<ServiceConfig> serviceConfigs, Integer clusterId) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        for (ServiceConfig serviceConfig : serviceConfigs) {
            if (Constants.INPUT.equals(serviceConfig.getType())) {
                String name = PlaceholderUtils.replacePlaceholders(serviceConfig.getName(), globalVariables,
                        Constants.REGEX_VARIABLE);
                serviceConfig.setName(name);

                String value = PlaceholderUtils.replacePlaceholders((String) serviceConfig.getValue(), globalVariables,
                        Constants.REGEX_VARIABLE);
                serviceConfig.setValue(value);
            }
        }
    }

    /**
     * 根据角色名构建配置名称到角色的映射 - JDK21框架隔离版本
     *
     * @param configFileMap 配置文件映射
     * @param frameCode 框架代码，用于隔离不同框架的配置处理
     * @return 配置名称到角色的映射
     */
    public static Map<String, String> buildNameToRoleMap(Map<Generators, List<ServiceConfig>> configFileMap, 
                                                         String frameCode) {
        logger.debug("开始为框架 {} 构建配置名称到角色的映射", frameCode);
        var resultMap = configFileMap.entrySet().stream()
            .flatMap(entry -> {
                var generator = entry.getKey();
                var configs = entry.getValue();
                var configTargetRoles = generator.getConfigTargetRoles();
                
                // 确定角色名
                var roleName = determineRoleNameForGenerator(configTargetRoles);
                
                // 为每个配置创建名称到角色的映射
                return configs.stream().map(config -> 
                    Map.entry(config.getName(), roleName)
                );
            })
            .collect(Collectors.toMap(
                Map.Entry::getKey, 
                Map.Entry::getValue,
                // JDK21框架隔离：处理重复键时包含框架上下文
                (existingValue, newValue) -> {
                    if (!existingValue.equals(newValue)) {
                        logger.debug("框架 {} 中配置名称重复但角色不同: 现有角色={}, 新角色={}, 使用现有角色", 
                            frameCode, existingValue, newValue);
                    }
                    return existingValue;
                }
            ));
        
        logger.debug("框架 {} 完成配置名称映射，共处理 {} 个配置项", frameCode, resultMap.size());
        return resultMap;
    }

    /**
     * 为Generator确定角色名
     *
     * @param configTargetRoles 配置目标角色字符串
     * @return 确定的角色名
     */
    private static String determineRoleNameForGenerator(String configTargetRoles) {
        if (configTargetRoles == null || configTargetRoles.isEmpty()) {
            return GENERAL;
        }
        
        var roleNames = parseRoleNames(configTargetRoles);
        return roleNames.isEmpty() ? GENERAL : roleNames.iterator().next();
    }

    /**
     * 为Kubernetes配置项的名称添加角色前缀
     *
     * @param roleName    角色名称
     * @param configName  配置项名称
     * @param configGroup 配置分组
     * @return 添加了角色前缀的配置项名称
     */
    public static String addRolePrefixForKubernetesConfig(String roleName, String configName, String configGroup) {
        if (roleName == null || configName == null) {
            return configName;
        }

        // 如果配置组是Kubernetes相关的，添加角色前缀
        if (configGroup != null && configGroup.startsWith(Constants.KUBERNETES_CONFIG_PREFIX)) {
            // 将角色名转换为小写下划线格式，保持与ProcessUtils.generateConfigFileMap一致 - 使用var
            var normRoleName = roleName.toLowerCase().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
            return "%s_%s".formatted(normRoleName, configName);
        }

        return configName;
    }
}