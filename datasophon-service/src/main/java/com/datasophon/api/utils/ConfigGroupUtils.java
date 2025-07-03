package com.datasophon.api.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.datasophon.common.Constants.GENERAL;

/**
 * 配置分组工具类，提供统一的配置分组处理逻辑
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
        Set<String> roleNames = new HashSet<>();

        if (configTargetRoles == null || configTargetRoles.isEmpty()) {
            return roleNames;
        }

        // 处理逗号分割的情况
        if (configTargetRoles.contains(",")) {
            String[] roles = configTargetRoles.split(",");
            for (String role : roles) {
                String trimmedRole = role.trim();
                if (!trimmedRole.isEmpty() && !GENERAL.equals(trimmedRole)) {
                    roleNames.add(trimmedRole);
                }
            }
        } else {
            // 单一角色，直接添加
            if (!GENERAL.equals(configTargetRoles.trim())) {
                roleNames.add(configTargetRoles.trim());
            }
        }

        return roleNames;
    }

    /**
     * 从generators收集所有角色名
     *
     * @param generators generators列表
     * @return 收集到的角色名集合
     */
    public static Set<String> collectRoleNamesFromGenerators(List<Generators> generators) {
        Set<String> roleNames = new HashSet<>();

        for (Generators generator : generators) {
            String configTargetRoles = generator.getConfigTargetRoles();
            roleNames.addAll(parseRoleNames(configTargetRoles));
        }

        return roleNames;
    }

    /**
     * 从服务定义中获取原始K8S配置
     *
     * @param frameCode   框架代码
     * @param serviceName 服务名称
     * @return 按K8S配置类型分组的原始配置映射
     */
    private static Map<String, List<ServiceConfig>> getOriginalKubernetesConfigs(String frameCode, String serviceName) {
        Map<String, List<ServiceConfig>> k8sConfigsByType = new HashMap<>();

        try {
            // 从Spring上下文获取服务
            FrameServiceService frameServiceService = SpringTool.getApplicationContext()
                    .getBean(FrameServiceService.class);

            // 获取服务定义
            FrameServiceEntity frameServiceEntity = frameServiceService.getServiceByFrameCodeAndServiceName(
                    frameCode, serviceName);

            if (frameServiceEntity == null) {
                logger.error("无法获取服务定义: {}", serviceName);
                return k8sConfigsByType;
            }

            // 解析服务JSON
            String serviceJson = frameServiceEntity.getServiceJson();
            if (StrUtil.isBlank(serviceJson)) {
                logger.error("服务定义JSON为空: {}", serviceName);
                return k8sConfigsByType;
            }

            JSONObject serviceObj = JSONObject.parseObject(serviceJson);

            // 获取parameters数组并转为ServiceConfig列表
            JSONArray parameters = serviceObj.getJSONArray("parameters");
            if (parameters == null || parameters.isEmpty()) {
                logger.warn("服务定义中没有parameters数组，无法获取原始配置");
                return k8sConfigsByType;
            }

            // 遍历parameters，过滤出K8S配置并按类型分组
            for (int i = 0; i < parameters.size(); i++) {
                JSONObject paramJson = parameters.getJSONObject(i);

                // 只处理K8S配置
                String configGroup = paramJson.getString("configGroup");
                if (configGroup != null && configGroup.startsWith(Constants.K8S_CONFIG_PREFIX)) {
                    // 将JSON转为ServiceConfig对象
                    ServiceConfig config = paramJson.toJavaObject(ServiceConfig.class);

                    // 提取K8S配置类型
                    String k8sConfigType = extractK8sConfigType(configGroup);

                    // 按类型分组
                    k8sConfigsByType.computeIfAbsent(k8sConfigType, k -> new ArrayList<>()).add(config);

                }
            }

        } catch (Exception e) {
            logger.error("获取原始K8S配置时出错: {}", e.getMessage(), e);
        }

        return k8sConfigsByType;
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

        // 1. 获取服务定义中的所有配置文件及其角色列表
        Map<String, Set<String>> configFileToRolesMap = getConfigFileRolesMap(frameCode, serviceName);

        if (configFileToRolesMap.isEmpty()) {
            logger.warn("无法获取服务 {} 的配置文件角色映射，将保持原始配置", serviceName);
            return list;
        }

        // 2. 从服务定义中获取原始K8S配置
        Map<String, List<ServiceConfig>> originalK8sConfigsByType = getOriginalKubernetesConfigs(frameCode,
                serviceName);

        if (originalK8sConfigsByType.isEmpty()) {
            logger.warn("无法从服务定义中获取原始K8S配置，将使用传入的配置列表");
            // 回退到原有逻辑，使用传入的配置列表
        }

        // 3. 对配置进行分类
        List<ServiceConfig> processedConfigs = new ArrayList<>();
        List<ServiceConfig> nonK8sConfigs = new ArrayList<>();
        Map<String, ServiceConfig> portConfigs = new HashMap<>(); // 存储端口配置，用于后续处理

        // 4. 获取非K8S配置和端口配置
        for (ServiceConfig config : list) {
            // 只处理非K8S配置，K8S配置将使用从服务定义中获取的原始配置
            if (config.getConfigGroup() == null || !config.getConfigGroup().startsWith(Constants.K8S_CONFIG_PREFIX)) {
                // 检查是否有端口绑定相关的配置
                if (config.getBindRole() != null && config.getPortNumber() != null) {
                    // 使用配置名称作为键，存储端口配置
                    portConfigs.put(config.getName(), config);
                }

                // 非K8S配置，直接保留
                nonK8sConfigs.add(config);
            }
        }

        // 5. 处理K8S配置（使用原始配置或回退到传入的配置）
        Map<String, List<ServiceConfig>> k8sConfigsByType = originalK8sConfigsByType;
        if (k8sConfigsByType.isEmpty()) {
            // 如果没有获取到原始配置，回退到使用传入的配置
            k8sConfigsByType = new HashMap<>();
            for (ServiceConfig config : list) {
                String configGroup = config.getConfigGroup();
                if (configGroup != null && configGroup.startsWith(Constants.K8S_CONFIG_PREFIX)) {
                    String k8sConfigType = extractK8sConfigType(configGroup);
                    k8sConfigsByType.computeIfAbsent(k8sConfigType, k -> new ArrayList<>()).add(config);
                }
            }
        }

        // 6. 为每个配置类型创建角色特定的配置
        for (Map.Entry<String, List<ServiceConfig>> entry : k8sConfigsByType.entrySet()) {
            String k8sConfigType = entry.getKey();
            List<ServiceConfig> configs = entry.getValue();

            // 获取该类型配置的角色集合
            Set<String> targetRoles = getTargetRolesForConfigType(configFileToRolesMap, k8sConfigType);

            if (targetRoles.isEmpty()) {
                logger.warn("无法确定K8S配置类型 {} 的目标角色，将使用通用角色", k8sConfigType);
                targetRoles.add(GENERAL);
            }

            // 为每个角色创建配置副本
            for (String roleName : targetRoles) {
                for (ServiceConfig config : configs) {
                    // 创建配置副本（使用原始配置）
                    ServiceConfig copy = ObjectUtil.cloneByStream(config);

                    // 设置单一角色
                    copy.setConfigTargetRoles(roleName);

                    // 设置角色特定的configGroup
                    copy.setConfigGroup(Constants.K8S_CONFIG_PREFIX + k8sConfigType + "." + roleName);

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

        // 8. 添加非K8S配置到结果列表
        processedConfigs.addAll(nonK8sConfigs);
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
        // 遍历所有端口配置
        for (ServiceConfig portConfig : portConfigs.values()) {
            String bindRole = portConfig.getBindRole();
            String portNumber = portConfig.getPortNumber();
            String serviceType = portConfig.getServiceType();
            String nodePort = portConfig.getNodePort();

            // 跳过无效的配置
            if (bindRole == null || portNumber == null) {
                continue;
            }

            // 遍历bindRole中的所有角色
            for (String role : bindRole.split(",")) {
                String roleName = role.trim().toLowerCase();

                // 构建要查找的配置名称
                String nodePortMappingName = roleName + "_node_port_mappings";
                String clusterPortMappingName = roleName + "_cluster_port_mappings";
                String loadBalancerMappingName = roleName + "_load_balancer_port_mappings";

                // 直接查找和更新匹配的配置
                for (ServiceConfig config : processedConfigs) {
                    String configName = config.getName();
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
        FrameServiceService frameServiceService = SpringTool.getApplicationContext()
                .getBean(FrameServiceService.class);

        // 收集所有角色配置映射
        Map<String, ServiceConfig> clusterPortMappingConfigs = new HashMap<>();
        for (ServiceConfig config : processedConfigs) {
            String configName = config.getName();
            if (configName != null && configName.endsWith("_cluster_port_mappings")) {
                String roleName = configName.substring(0, configName.length() - "_cluster_port_mappings".length());
                clusterPortMappingConfigs.put(roleName, config);
            }
        }

        // 如果没有找到任何集群端口映射配置，直接返回
        if (clusterPortMappingConfigs.isEmpty()) {
            logger.info("未找到任何集群端口映射配置，跳过JMX端口处理");
            return;
        }

        try {
            // 获取所有服务定义
            List<FrameServiceEntity> allServices = frameServiceService.list();

            for (FrameServiceEntity service : allServices) {
                String serviceJson = service.getServiceJson();
                if (StrUtil.isBlank(serviceJson)) {
                    continue;
                }

                JSONObject serviceObj = JSONObject.parseObject(serviceJson);
                JSONArray roles = serviceObj.getJSONArray("roles");

                if (roles == null || roles.isEmpty()) {
                    continue;
                }

                // 遍历所有角色
                for (int i = 0; i < roles.size(); i++) {
                    JSONObject roleObj = roles.getJSONObject(i);
                    String roleName = roleObj.getString("name");
                    Object jmxPortObj = roleObj.get("jmxPort");

                    // 如果角色定义了JMX端口
                    if (roleName != null && jmxPortObj != null) {
                        String jmxPort = String.valueOf(jmxPortObj);

                        // 检查是否是有效的端口号
                        if (StrUtil.isNotBlank(jmxPort) && !jmxPort.equals("null")) {
                            // 将角色名转为小写下划线格式
                            String normRoleName = roleName.toLowerCase().replaceAll("([a-z])([A-Z])", "$1_$2")
                                    .toLowerCase();

                            // 特殊处理ZKFC角色，将其JMX端口添加到NameNode的端口映射中
                            if ("ZKFC".equals(roleName)) {
                                String namenodeRoleName = "namenode"; // NameNode的标准化角色名
                                ServiceConfig namenodeConfig = clusterPortMappingConfigs.get(namenodeRoleName);

                                if (namenodeConfig != null) {
                                    // 更新NameNode的端口映射，添加ZKFC的JMX端口
                                    updatePortMapping(namenodeConfig, jmxPort, jmxPort);
                                    logger.info("将ZKFC的JMX端口 {} 添加到NameNode的cluster_port_mappings中", jmxPort);
                                } else {
                                    logger.warn("未找到NameNode的端口映射配置，无法添加ZKFC的JMX端口 {}", jmxPort);
                                }
                            } else {
                                // 查找对应的集群端口映射配置
                                ServiceConfig clusterPortConfig = clusterPortMappingConfigs.get(normRoleName);

                                if (clusterPortConfig != null) {
                                    // 更新端口映射，添加JMX端口
                                    updatePortMapping(clusterPortConfig, jmxPort, jmxPort);
                                    logger.debug("添加JMX端口 {} 到cluster_port_mappings，角色: {}", jmxPort, roleName);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("处理JMX端口时出错: {}", e.getMessage(), e);
        }
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
            // 获取当前值
            Object currentValue = config.getValue();
            List<Map<String, String>> portMappings = new ArrayList<>();

            // 解析当前值
            if (currentValue == null) {
                // 保持空列表
            } else if (currentValue instanceof String) {
                try {
                    portMappings = JSONObject.parseObject((String) currentValue,
                            new TypeReference<List<Map<String, String>>>() {
                            });
                } catch (Exception e) {
                    logger.warn("解析端口映射字符串失败，尝试其他格式: {}", e.getMessage());
                    // 尝试解析为JSONArray
                    JSONArray jsonArray = JSONObject.parseArray((String) currentValue);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        Object item = jsonArray.get(i);
                        if (item instanceof JSONObject) {
                            Map<String, String> mapping = new HashMap<>();
                            JSONObject jsonObj = (JSONObject) item;
                            for (String key : jsonObj.keySet()) {
                                mapping.put(key, jsonObj.getString(key));
                            }
                            portMappings.add(mapping);
                        }
                    }
                }
            } else if (currentValue instanceof List) {
                // 尝试转换List中的每个元素
                List<?> rawList = (List<?>) currentValue;
                for (Object item : rawList) {
                    if (item instanceof Map) {
                        Map<?, ?> rawMap = (Map<?, ?>) item;
                        Map<String, String> convertedMap = new HashMap<>();
                        for (Object key : rawMap.keySet()) {
                            if (key != null) {
                                Object val = rawMap.get(key);
                                convertedMap.put(key.toString(), val != null ? val.toString() : "");
                            }
                        }
                        portMappings.add(convertedMap);
                    } else if (item instanceof JSONObject) {
                        JSONObject jsonObj = (JSONObject) item;
                        Map<String, String> mapping = new HashMap<>();
                        for (String key : jsonObj.keySet()) {
                            mapping.put(key, jsonObj.getString(key));
                        }
                        portMappings.add(mapping);
                    }
                }
            } else if (currentValue instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) currentValue;
                for (int i = 0; i < jsonArray.size(); i++) {
                    Object item = jsonArray.get(i);
                    if (item instanceof JSONObject) {
                        Map<String, String> mapping = new HashMap<>();
                        JSONObject jsonObj = (JSONObject) item;
                        for (String key : jsonObj.keySet()) {
                            mapping.put(key, jsonObj.getString(key));
                        }
                        portMappings.add(mapping);
                    }
                }
            } else {
                logger.warn("无法处理的端口映射值类型: {}", currentValue.getClass().getName());
                logger.debug("端口映射值内容: {}", currentValue);
                return;
            }

            // 检查是否已存在相同的端口映射
            boolean found = false;
            for (Map<String, String> mapping : portMappings) {
                if (mapping.containsKey(port)) {
                    // 获取现有的映射值
                    String existingValue = mapping.get(port);
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

            // 如果不存在，添加新的映射
            if (!found) {
                Map<String, String> newMapping = new HashMap<>();
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
            // 从Spring上下文获取服务
            FrameServiceService frameServiceService = SpringTool.getApplicationContext()
                    .getBean(FrameServiceService.class);

            // 获取服务定义
            FrameServiceEntity frameServiceEntity = frameServiceService.getServiceByFrameCodeAndServiceName(
                    frameCode, serviceName);

            if (frameServiceEntity == null) {
                logger.error("无法获取服务定义: {}", serviceName);
                return result;
            }

            // 解析服务JSON
            String serviceJson = frameServiceEntity.getServiceJson();
            if (StrUtil.isBlank(serviceJson)) {
                logger.error("服务定义JSON为空: {}", serviceName);
                return result;
            }

            JSONObject serviceObj = JSONObject.parseObject(serviceJson);

            // 记录服务定义中的parameters
            JSONArray parameters = serviceObj.getJSONArray("parameters");
            JSONObject configWriter = serviceObj.getJSONObject("configWriter");
            if (configWriter == null) {
                logger.warn("服务定义中没有configWriter对象");
                return result;
            }

            JSONArray generators = configWriter.getJSONArray("generators");
            if (generators == null || generators.isEmpty()) {
                logger.warn("服务定义中没有配置文件定义: {}", serviceName);
                return result;
            }

            // 解析所有配置文件定义，关键是将filename和configTargetRoles对应起来
            Map<String, Set<String>> filenameToRolesMap = new HashMap<>();

            for (int i = 0; i < generators.size(); i++) {
                JSONObject generator = generators.getJSONObject(i);
                String filename = generator.getString("filename");
                String configTargetRoles = generator.getString("configTargetRoles");

                // 检查是否有K8S相关的配置文件
                if (filename != null && filename.toLowerCase().startsWith(Constants.K8S_CONFIG_PREFIX)) {
                    Set<String> roles = new HashSet<>();
                    // 解析角色列表
                    if (StrUtil.isNotBlank(configTargetRoles)) {
                        for (String role : configTargetRoles.split(",")) {
                            String trimmedRole = role.trim();
                            if (!trimmedRole.isEmpty()) {
                                roles.add(trimmedRole);
                            }
                        }
                    }

                    // 重要：将filename映射到对应的角色列表
                    filenameToRolesMap.put(filename, roles);
                }
            }

            // 第二步：遍历参数定义，将configGroup与filename进行匹配
            if (parameters != null && !parameters.isEmpty()) {
                for (int i = 0; i < parameters.size(); i++) {
                    JSONObject parameter = parameters.getJSONObject(i);
                    String configGroup = parameter.getString("configGroup");

                    // 如果configGroup以kubernetes.config.开头，查找对应的filename
                    if (configGroup != null && configGroup.startsWith(Constants.K8S_CONFIG_PREFIX)) {
                        // 查找匹配的filename（就是configGroup完全一致的filename）
                        Set<String> roles = filenameToRolesMap.get(configGroup);

                        if (roles != null && !roles.isEmpty()) {
                            // 将configGroup映射到对应的角色
                            result.put(configGroup, roles);

                            // 同时将参数名映射到角色
                            String paramName = parameter.getString("name");
                            if (StrUtil.isNotBlank(paramName)) {
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
        String fullConfigGroup = Constants.K8S_CONFIG_PREFIX + configType;

        // 1. 优先尝试完全匹配
        if (configFileToRolesMap.containsKey(fullConfigGroup)) {
            return configFileToRolesMap.get(fullConfigGroup);
        }

        // 2. 如果没有找到完全匹配，尝试模糊匹配
        for (Map.Entry<String, Set<String>> entry : configFileToRolesMap.entrySet()) {
            String key = entry.getKey();

            // 如果key包含configType或configType包含key
            if (key.contains(configType) || configType.contains(key)) {
                result.addAll(entry.getValue());
            }
        }

        return result;
    }

    /**
     * 提取K8S配置类型
     *
     * @param configGroup 配置组
     * @return K8S配置类型（如persistentVolumeClaims或resources）
     */
    private static String extractK8sConfigType(String configGroup) {
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

        // 将roleName转为小写并将驼峰转为下划线格式
        String normRoleName = roleName.toLowerCase().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();

        // 直接添加前缀（不需要检查，因为我们从原始配置创建）
        config.setName(normRoleName + "_" + configName);
    }

    /**
     * 将配置项按配置组分组
     *
     * @param list 配置项列表
     * @return 按配置组分组后的映射
     */
    public static Map<String, List<ServiceConfig>> groupByConfigTargetRoleOrCommon(List<ServiceConfig> list) {
        // 先处理所有配置项的模板内容
        if (list != null) {
            for (ServiceConfig config : list) {
                String templateName = config.getTemplateName();
                if (StrUtil.isNotBlank(templateName)) {
                    String templateContent = TemplatePathUtils.getTemplateContent(templateName);
                    config.setTemplateContent(templateContent);
                }
            }
        }

        // 存储分组结果
        Map<String, List<ServiceConfig>> groupedConfigs = new LinkedHashMap<>();
        // 收集从Kubernetes配置中提取的角色名
        Set<String> k8sRoles = new HashSet<>();

        if (list != null) {
            for (ServiceConfig config : list) {
                String configCategory = config.getConfigCategory();
                String configGroup = config.getConfigGroup();
                String configLevel = config.getConfigLevel();
                String configTargetRoles = config.getConfigTargetRoles();
                String groupKey = GENERAL;

                // 处理kubernetes.config类型的配置组
                if (StrUtil.isNotBlank(configGroup) && configGroup.startsWith(Constants.K8S_CONFIG_PREFIX)) {
                    // 特殊处理kubernetes配置，使用完整的configGroup作为键
                    groupKey = configGroup;
                    groupedConfigs.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(config);

                    // 从Kubernetes配置组名中提取角色名
                    String roleName = extractRoleFromK8sConfigGroup(configGroup);
                    if (StrUtil.isNotBlank(roleName)) {
                        k8sRoles.add(roleName);
                    }
                }
                // 处理角色配置
                else if ("role".equals(configCategory) && configGroup != null) {
                    // 使用configGroup作为分组键，这样角色配置会被正确地分到各自的角色组
                    groupKey = configGroup;
                    groupedConfigs.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(config);
                }
                // 处理配置级别为"custom"或"advanced"的情况
                else if (StrUtil.isNotBlank(configLevel) &&
                        ("custom".equalsIgnoreCase(configLevel) || "advanced".equalsIgnoreCase(configLevel)) &&
                        StrUtil.isNotBlank(configGroup)) {

                    String levelPrefix = configLevel.toLowerCase() + "_";

                    // 直接将configLevel和configGroup用下划线连接
                    if (configGroup.startsWith(levelPrefix)) {
                        groupKey = configGroup;
                    } else {
                        groupKey = levelPrefix + configGroup;
                    }

                    // 将配置添加到对应分组
                    groupedConfigs.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(config);
                } else if (StrUtil.isAllBlank(configCategory, configGroup, configLevel)) {
                    groupKey = GENERAL;
                    // 为空字段设置默认值
                    if (StrUtil.isBlank(configCategory)) {
                        config.setConfigCategory("role");
                    }
                    if (StrUtil.isBlank(configGroup)) {
                        config.setConfigGroup("General");
                    }
                    if (StrUtil.isBlank(configLevel)) {
                        config.setConfigLevel("advanced");
                    }
                    groupedConfigs.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(config);
                }
                // 处理configTargetRoles情况
                else if (configTargetRoles != null) {
                    Set<String> roleNames = parseRoleNames(configTargetRoles);
                    for (String roleName : roleNames) {
                        groupedConfigs.computeIfAbsent(roleName, k -> new ArrayList<>()).add(config);
                    }
                }
                // 处理至少有一个字段不为空的情况
                else if (StrUtil.isNotBlank(configCategory) ||
                        StrUtil.isNotBlank(configGroup) ||
                        StrUtil.isNotBlank(configLevel)) {
                    // 优先使用configGroup作为分组键
                    if (StrUtil.isNotBlank(configGroup)) {
                        groupKey = configGroup;
                    }
                    // 如果configGroup为空但configCategory不为空，使用configCategory
                    else if (StrUtil.isNotBlank(configCategory)) {
                        groupKey = configCategory;
                    }
                    // 如果前两者都为空但configLevel不为空，使用configLevel
                    else {
                        groupKey = configLevel;
                    }
                    groupedConfigs.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(config);
                }
            }
        }

        // 为从K8S配置组中提取的角色创建空角色分组（如果尚不存在）
        for (String roleName : k8sRoles) {
            groupedConfigs.computeIfAbsent(roleName, k -> new ArrayList<>());
        }

        // 确保每个配置组内的配置名称唯一
        for (Map.Entry<String, List<ServiceConfig>> entry : groupedConfigs.entrySet()) {
            Map<String, ServiceConfig> uniqueNameMap = new LinkedHashMap<>();
            for (ServiceConfig config : entry.getValue()) {
                uniqueNameMap.put(config.getName(), config);
            }
            entry.setValue(new ArrayList<>(uniqueNameMap.values()));
        }

        return groupedConfigs;
    }

    /**
     * 从Kubernetes配置组名中提取角色名
     * 例如从 "kubernetes.config.persistent-volume-claims.DataNode" 提取 "DataNode"
     *
     * @param configGroup Kubernetes配置组名
     * @return 提取的角色名，如果无法提取则返回null
     */
    private static String extractRoleFromK8sConfigGroup(String configGroup) {
        if (configGroup == null || !configGroup.startsWith(Constants.K8S_CONFIG_PREFIX)) {
            return null;
        }

        String[] parts = configGroup.split("\\.");
        if (parts.length >= 4) {
            return parts[3]; // 返回第四部分作为角色名
        }

        return null;
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
            // 有角色信息，分别处理K8S和非K8S配置
            processConfigWithRoles(configFileMap, originalConfigMap, roleNames, clusterId);
        }

        // 4. 从configJson获取配置值并应用到configFileMap
        List<ServiceConfig> configs = JSONObject.parseArray(config.getConfigJson(), ServiceConfig.class);

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
                new TypeReference<Map<JSONObject, JSONArray>>() {
                }, Feature.SupportAutoType);
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

        // 1. 分离K8S和非K8S配置
        Map<String, List<ServiceConfig>> k8sConfigsByType = new HashMap<>();
        Map<Generators, List<ServiceConfig>> nonK8sConfigs = new HashMap<>();

        separateK8sAndNonK8sConfigs(originalMap, k8sConfigsByType, nonK8sConfigs, clusterId);

        // 2. 处理K8S配置
        processK8sConfigs(resultMap, originalMap, k8sConfigsByType, roleNames, clusterId);

        // 3. 添加非K8S配置到结果
        resultMap.putAll(nonK8sConfigs);
    }

    /**
     * 分离Kubernetes和非Kubernetes配置
     *
     * @param originalMap      原始配置映射
     * @param k8sConfigsByType K8S配置映射（按类型分组）
     * @param nonK8sConfigs    非K8S配置映射
     * @param clusterId        集群ID
     */
    private static void separateK8sAndNonK8sConfigs(
            Map<JSONObject, JSONArray> originalMap,
            Map<String, List<ServiceConfig>> k8sConfigsByType,
            Map<Generators, List<ServiceConfig>> nonK8sConfigs,
            Integer clusterId) {

        for (JSONObject fileJson : originalMap.keySet()) {
            Generators generator = fileJson.toJavaObject(Generators.class);
            List<ServiceConfig> originalConfigs = originalMap.get(fileJson).toJavaList(ServiceConfig.class);

            // 分拣K8S配置和非K8S配置
            List<ServiceConfig> k8sConfigs = new ArrayList<>();
            List<ServiceConfig> otherConfigs = new ArrayList<>();

            for (ServiceConfig serviceConfig : originalConfigs) {
                if (serviceConfig.getConfigGroup() != null
                        && serviceConfig.getConfigGroup().startsWith(Constants.K8S_CONFIG_PREFIX)) {
                    k8sConfigs.add(serviceConfig);
                } else {
                    otherConfigs.add(serviceConfig);
                }
            }

            // 处理非K8S配置
            if (!otherConfigs.isEmpty()) {
                replaceVariable(otherConfigs, clusterId);
                nonK8sConfigs.put(generator, otherConfigs);
            }

            // 按类型分组K8S配置
            for (ServiceConfig k8sConfig : k8sConfigs) {
                String k8sConfigType = extractK8sConfigType(k8sConfig.getConfigGroup());
                k8sConfigsByType.computeIfAbsent(k8sConfigType, k -> new ArrayList<>()).add(k8sConfig);
            }
        }
    }

    /**
     * 处理Kubernetes配置
     *
     * @param resultMap        结果配置映射
     * @param originalMap      原始配置映射
     * @param k8sConfigsByType K8S配置映射（按类型分组）
     * @param roleNames        角色名集合
     * @param clusterId        集群ID
     */
    private static void processK8sConfigs(
            Map<Generators, List<ServiceConfig>> resultMap,
            Map<JSONObject, JSONArray> originalMap,
            Map<String, List<ServiceConfig>> k8sConfigsByType,
            Set<String> roleNames,
            Integer clusterId) {

        // 处理每种类型的K8S配置
        for (Map.Entry<String, List<ServiceConfig>> entry : k8sConfigsByType.entrySet()) {
            String k8sConfigType = entry.getKey();
            List<ServiceConfig> configs = entry.getValue();

            // 1. 为每个角色创建配置副本
            List<ServiceConfig> allK8sConfigs = createConfigsForRoles(configs, k8sConfigType, roleNames);

            // 2. 替换变量
            replaceVariable(allK8sConfigs, clusterId);

            // 3. 查找并使用原始Generator对象
            findAndUseOriginalGenerator(resultMap, originalMap, k8sConfigType, allK8sConfigs);
        }
    }

    /**
     * 为每个角色创建配置副本
     *
     * @param configs       配置列表
     * @param k8sConfigType K8S配置类型
     * @param roleNames     角色名集合
     * @return 创建的配置列表
     */
    private static List<ServiceConfig> createConfigsForRoles(
            List<ServiceConfig> configs,
            String k8sConfigType,
            Set<String> roleNames) {

        List<ServiceConfig> allConfigs = new ArrayList<>();

        // 为每个角色创建配置副本
        for (String roleName : roleNames) {

            for (ServiceConfig config : configs) {
                // 创建配置副本（使用原始配置）
                ServiceConfig newConfig = ObjectUtil.cloneByStream(config);

                // 设置角色特定的configGroup
                newConfig.setConfigGroup(Constants.K8S_CONFIG_PREFIX + k8sConfigType + "." + roleName);

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
     * @param k8sConfigType K8S配置类型
     * @param configs       配置列表
     */
    private static void findAndUseOriginalGenerator(
            Map<Generators, List<ServiceConfig>> resultMap,
            Map<JSONObject, JSONArray> originalMap,
            String k8sConfigType,
            List<ServiceConfig> configs) {

        // 查找原始Generator对象
        Generators originalGenerator = null;
        for (JSONObject generatorJson : originalMap.keySet()) {
            Generators generator = generatorJson.toJavaObject(Generators.class);
            if (generator.getFilename() != null &&
                    generator.getFilename().equals(Constants.K8S_CONFIG_PREFIX + k8sConfigType)) {
                originalGenerator = generator;
                break;
            }
        }

        // 使用原始Generator对象
        if (originalGenerator != null) {
            resultMap.put(originalGenerator, configs);
        } else {
            logger.warn("无法为K8S配置类型 {} 找到原始Generator对象", k8sConfigType);
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
     * 根据角色名构建配置名称到角色的映射
     *
     * @param configFileMap 配置文件映射
     * @return 配置名称到角色的映射
     */
    public static Map<String, String> buildNameToRoleMap(Map<Generators, List<ServiceConfig>> configFileMap) {
        Map<String, String> resultMap = new HashMap<>();

        for (Map.Entry<Generators, List<ServiceConfig>> entry : configFileMap.entrySet()) {
            Generators generator = entry.getKey();
            List<ServiceConfig> configs = entry.getValue();

            String configTargetRoles = generator.getConfigTargetRoles();
            if (configTargetRoles == null || configTargetRoles.isEmpty()) {
                // 处理configTargetRoles为空的情况
                for (ServiceConfig config : configs) {
                    resultMap.put(config.getName(), GENERAL);
                }
            } else {
                // 使用ConfigGroupUtils处理configTargetRoles
                Set<String> roleNames = parseRoleNames(configTargetRoles);
                if (roleNames.isEmpty()) {
                    // 如果解析后没有角色名，使用通用分组
                    for (ServiceConfig config : configs) {
                        resultMap.put(config.getName(), GENERAL);
                    }
                } else {
                    // 使用第一个角色名
                    String firstRole = roleNames.iterator().next();
                    for (ServiceConfig config : configs) {
                        resultMap.put(config.getName(), firstRole);
                    }
                }
            }
        }

        return resultMap;
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
        if (configGroup != null && configGroup.startsWith(Constants.K8S_CONFIG_PREFIX)) {
            // 将角色名转换为小写下划线格式，保持与ProcessUtils.generateConfigFileMap一致
            String normRoleName = roleName.toLowerCase().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
            return normRoleName + "_" + configName;
        }

        return configName;
    }
}