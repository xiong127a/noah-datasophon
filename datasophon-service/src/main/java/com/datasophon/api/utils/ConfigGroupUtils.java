package com.datasophon.api.utils;

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

        logger.info("开始预处理Kubernetes配置，服务: {}, 框架: {}, 配置项数量: {}", serviceName, frameCode, list.size());

        // 1. 获取服务定义中的所有配置文件及其角色列表
        Map<String, Set<String>> configFileToRolesMap = getConfigFileRolesMap(frameCode, serviceName);

        if (configFileToRolesMap.isEmpty()) {
            logger.warn("无法获取服务 {} 的配置文件角色映射，将保持原始配置", serviceName);
            return list;
        }

        logger.info("获取到的配置文件角色映射: {}", configFileToRolesMap);

        // 2. 对配置进行分类
        List<ServiceConfig> processedConfigs = new ArrayList<>();
        List<ServiceConfig> nonK8sConfigs = new ArrayList<>();
        Map<String, List<ServiceConfig>> k8sConfigsByType = new HashMap<>();

        for (ServiceConfig config : list) {
            String configGroup = config.getConfigGroup();

            if (configGroup != null && configGroup.startsWith("kubernetes.config.")) {
                // 提取K8S配置类型（如persistentVolumeClaims或resources）
                String k8sConfigType = extractK8sConfigType(configGroup);
                logger.debug("发现K8S配置: {}, 类型: {}, 组: {}", config.getName(), k8sConfigType, configGroup);

                // 添加到相应类型的列表
                k8sConfigsByType.computeIfAbsent(k8sConfigType, k -> new ArrayList<>()).add(config);
            } else {
                // 非K8S配置，直接保留
                nonK8sConfigs.add(config);
            }
        }

        // 3. 处理K8S配置
        for (Map.Entry<String, List<ServiceConfig>> entry : k8sConfigsByType.entrySet()) {
            String k8sConfigType = entry.getKey();
            List<ServiceConfig> configs = entry.getValue();

            logger.info("处理K8S配置类型: {}, 配置数量: {}", k8sConfigType, configs.size());

            // 获取该类型配置的角色集合
            Set<String> targetRoles = getTargetRolesForConfigType(configFileToRolesMap, k8sConfigType);

            if (targetRoles.isEmpty()) {
                logger.warn("无法确定K8S配置类型 {} 的目标角色，将使用通用角色", k8sConfigType);
                targetRoles.add(GENERAL);
            } else {
                logger.info("K8S配置类型 {} 的目标角色: {}", k8sConfigType, targetRoles);
            }

            // 为每个角色创建每个配置的副本
            for (String roleName : targetRoles) {
                logger.debug("为角色 {} 创建配置副本", roleName);
                for (ServiceConfig config : configs) {
                    // 创建配置副本
                    ServiceConfig copy = cloneServiceConfig(config);

                    // 设置单一角色
                    copy.setConfigTargetRoles(roleName);

                    // 设置角色特定的configGroup
                    copy.setConfigGroup("kubernetes.config." + k8sConfigType + "." + roleName);

                    // 添加角色前缀到name
                    addRolePrefixToName(copy, roleName);

                    // 添加到处理后的配置列表
                    processedConfigs.add(copy);
                    logger.debug("创建的配置副本: name={}, group={}", copy.getName(), copy.getConfigGroup());
                }
            }
        }

        // 添加非K8S配置到结果列表
        processedConfigs.addAll(nonK8sConfigs);

        logger.info("完成Kubernetes配置预处理，处理后配置数量: {}", processedConfigs.size());
        return processedConfigs;
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
            JSONObject configWriter = serviceObj.getJSONObject("configWriter");
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
                if (filename != null && (filename.toLowerCase().endsWith(".k8s") ||
                        filename.toLowerCase().startsWith("kubernetes.config."))) {
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
                    logger.info("找到K8S配置文件: {}，角色: {}", filename, roles);
                }
            }

            // 第二步：遍历参数定义，将configGroup与filename进行匹配
            JSONArray parameters = serviceObj.getJSONArray("parameters");
            if (parameters != null && !parameters.isEmpty()) {
                for (int i = 0; i < parameters.size(); i++) {
                    JSONObject parameter = parameters.getJSONObject(i);
                    String configGroup = parameter.getString("configGroup");

                    // 如果configGroup以kubernetes.config.开头，查找对应的filename
                    if (configGroup != null && configGroup.startsWith("kubernetes.config.")) {
                        // 查找匹配的filename（就是configGroup完全一致的filename）
                        Set<String> roles = filenameToRolesMap.get(configGroup);

                        if (roles != null && !roles.isEmpty()) {
                            // 将configGroup映射到对应的角色
                            result.put(configGroup, roles);
                            logger.info("为配置组 {} 找到角色: {}", configGroup, roles);

                            // 同时将参数名映射到角色
                            String paramName = parameter.getString("name");
                            if (StrUtil.isNotBlank(paramName)) {
                                result.put(paramName, roles);
                            }
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
        String fullConfigGroup = "kubernetes.config." + configType;

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
        // kubernetes.config.persistentVolumeClaims 或 kubernetes.config.resources 等
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

        // 将roleName转为小写并将驼峰转为下划线格式
        String normRoleName = roleName.toLowerCase().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();

        // 检查是否已经有前缀
        if (!configName.startsWith(normRoleName + "_")) {
            config.setName(normRoleName + "_" + configName);
        }
    }

    /**
     * 将配置项按配置组分组
     * 
     * @param serviceName 服务名称
     * @param list        配置项列表
     * @return 按配置组分组后的映射
     */
    public static Map<String, List<ServiceConfig>> groupByConfigTargetRoleOrCommon(String serviceName,
            List<ServiceConfig> list) {
        // 最终返回结果
        Map<String, List<ServiceConfig>> resultMap = new LinkedHashMap<>();

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
        Map<String, List<ServiceConfig>> groupedConfigs = new HashMap<>();

        if (list != null) {
            for (ServiceConfig config : list) {
                String configCategory = config.getConfigCategory();
                String configGroup = config.getConfigGroup();
                String configLevel = config.getConfigLevel();
                String configTargetRoles = config.getConfigTargetRoles();
                String groupKey = GENERAL;
                String actualConfigGroup = configGroup;

                // 处理非空的configGroup
                if (StrUtil.isNotBlank(configGroup) && configGroup.contains(".")) {
                    actualConfigGroup = configGroup.substring(configGroup.lastIndexOf(".") + 1);
                }

                // 处理kubernetes.config类型的配置组
                if (StrUtil.isNotBlank(configGroup) && configGroup.startsWith("kubernetes.config.")) {
                    // 特殊处理kubernetes配置，使用完整的configGroup作为键
                    groupKey = configGroup;
                    groupedConfigs.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(config);
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
                        StrUtil.isNotBlank(actualConfigGroup)) {

                    String levelPrefix = configLevel.toLowerCase() + "_";
                    if (actualConfigGroup.startsWith(levelPrefix)) {
                        groupKey = actualConfigGroup;
                    } else {
                        groupKey = levelPrefix + actualConfigGroup;
                    }
                    // 将配置添加到对应分组
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
                        StrUtil.isNotBlank(actualConfigGroup) ||
                        StrUtil.isNotBlank(configLevel)) {
                    // 优先使用configGroup作为分组键
                    if (StrUtil.isNotBlank(actualConfigGroup)) {
                        groupKey = actualConfigGroup;
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
                // 只有当三个字段都为空时，才归入General分组
                else {
                    groupKey = GENERAL;
                    groupedConfigs.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(config);
                }
            }
        }

        // 排序分组键并构建结果
        List<String> sortedGroups = ConfigGroupSorter.sortGroups(serviceName, new ArrayList<>(groupedConfigs.keySet()));

        logger.info("按服务{}排序后的配置组顺序: {}", serviceName, sortedGroups);

        for (String group : sortedGroups) {
            resultMap.put(group, groupedConfigs.get(group));
        }

        return resultMap;
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
        Map<JSONObject, JSONArray> map = JSONObject.parseObject(config.getConfigFileJson(),
                new TypeReference<Map<JSONObject, JSONArray>>() {
                }, Feature.SupportAutoType);

        // 收集所有角色名
        List<Generators> generatorsList = new ArrayList<>();
        for (JSONObject fileJson : map.keySet()) {
            Generators generator = fileJson.toJavaObject(Generators.class);
            generatorsList.add(generator);
        }

        Set<String> roleNames = collectRoleNamesFromGenerators(generatorsList);

        logger.info("收集到的角色名: {}", roleNames);

        // 如果没有找到角色名，无法添加前缀，直接处理并返回
        if (roleNames.isEmpty()) {
            logger.warn("没有找到任何角色名，无法为Kubernetes配置添加前缀");

            for (JSONObject fileJson : map.keySet()) {
                Generators generator = fileJson.toJavaObject(Generators.class);
                List<ServiceConfig> serviceConfigs = map.get(fileJson).toJavaList(ServiceConfig.class);

                // replace variable
                replaceVariable(serviceConfigs, clusterId);
                configFileMap.put(generator, serviceConfigs);
            }

            return;
        }

        // 按K8S配置类型组织所有配置
        Map<String, List<ServiceConfig>> k8sConfigsByType = new HashMap<>();
        Map<Generators, List<ServiceConfig>> nonK8sConfigs = new HashMap<>();

        // 第一步：将配置按K8S类型分组
        for (JSONObject fileJson : map.keySet()) {
            Generators generator = fileJson.toJavaObject(Generators.class);
            List<ServiceConfig> originalConfigs = map.get(fileJson).toJavaList(ServiceConfig.class);

            // 分拣K8S配置和非K8S配置
            List<ServiceConfig> k8sConfigs = new ArrayList<>();
            List<ServiceConfig> otherConfigs = new ArrayList<>();

            for (ServiceConfig serviceConfig : originalConfigs) {
                if (serviceConfig.getConfigGroup() != null
                        && serviceConfig.getConfigGroup().startsWith("kubernetes.config.")) {
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

        // 第二步：为每个K8S配置类型和角色创建配置
        for (Map.Entry<String, List<ServiceConfig>> entry : k8sConfigsByType.entrySet()) {
            String k8sConfigType = entry.getKey();
            List<ServiceConfig> configs = entry.getValue();

            List<ServiceConfig> allK8sConfigs = new ArrayList<>();

            // 为每个角色创建配置
            for (String roleName : roleNames) {
                for (ServiceConfig k8sConfig : configs) {
                    // 创建配置副本
                    ServiceConfig newConfig = cloneServiceConfig(k8sConfig);

                    // 设置角色特定的configGroup
                    newConfig.setConfigGroup("kubernetes.config." + k8sConfigType + "." + roleName);

                    // 添加角色前缀到name
                    addRolePrefixToName(newConfig, roleName);

                    allK8sConfigs.add(newConfig);
                }
            }

            // 替换变量
            replaceVariable(allK8sConfigs, clusterId);

            // 创建新的生成器并添加到结果映射
            Generators k8sGenerator = new Generators();
            k8sGenerator.setFilename(k8sConfigType + ".k8s");
            k8sGenerator.setConfigFormat("properties");
            configFileMap.put(k8sGenerator, allK8sConfigs);
        }

        // 添加非K8S配置到结果映射
        configFileMap.putAll(nonK8sConfigs);
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
     * 创建ServiceConfig的深拷贝
     * 
     * @param source 源配置对象
     * @return 克隆的配置对象
     */
    public static ServiceConfig cloneServiceConfig(ServiceConfig source) {
        ServiceConfig target = new ServiceConfig();

        // 复制基本字段
        target.setName(source.getName());
        target.setValue(source.getValue());
        target.setLabel(source.getLabel());
        target.setDescription(source.getDescription());
        target.setRequired(source.isRequired());
        target.setType(source.getType());
        target.setConfigurableInWizard(source.isConfigurableInWizard());
        target.setDefaultValue(source.getDefaultValue());
        target.setMinValue(source.getMinValue());
        target.setMaxValue(source.getMaxValue());
        target.setUnit(source.getUnit());
        target.setHidden(source.isHidden());
        target.setSelectValue(source.getSelectValue());
        target.setConfigType(source.getConfigType());
        target.setConfigWithKerberos(source.isConfigWithKerberos());
        target.setConfigWithRack(source.isConfigWithRack());
        target.setConfigWithHA(source.isConfigWithHA());
        target.setSeparator(source.getSeparator());
        target.setOpen(source.getOpen());
        target.setClose(source.getClose());
        target.setConfigTargetRoles(source.getConfigTargetRoles());
        target.setConfigCategory(source.getConfigCategory());
        target.setConfigGroup(source.getConfigGroup());
        target.setConfigLevel(source.getConfigLevel());
        target.setTemplateName(source.getTemplateName());
        target.setTemplateContent(source.getTemplateContent());
        target.setDisplayName(source.getDisplayName());
        target.setHeightMultiple(source.getHeightMultiple());
        target.setServiceName(source.getServiceName());

        return target;
    }
}