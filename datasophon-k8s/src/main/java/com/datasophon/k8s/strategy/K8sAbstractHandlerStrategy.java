package com.datasophon.k8s.strategy;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.datasophon.common.Constants.SERVICE_ROLE_HOST_MAPPING;
import static com.datasophon.common.Constants.UNDERLINE;

@Data
public class K8sAbstractHandlerStrategy {
    public String serviceName;

    public String serviceRoleName;

    public String serviceRoleFullName;

    public final String NAMESPACE = "datasophon";
    public final String CLUSTER_DOMAIN = "svc.cluster.local";

    public Logger logger;

    public K8sAbstractHandlerStrategy(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public int getCurrentRoleLoopIndex() {
        String cacheKey = String.format("ROLE_LOOP_INDEX_%s_%s", serviceRoleName, serviceName);
        Integer currentIndex = (Integer) CacheUtils.get(cacheKey);
        if (currentIndex == null) {
            currentIndex = 1; // 初始化为1
            CacheUtils.put(cacheKey, currentIndex);
        }
        return currentIndex;
    }

    /**
     * 获取当前角色需要的总循环次数
     *
     * @return 总循环次数
     */
    public int getTotalRoleLoopCount() {
        // 使用与 getCurrentRoleLoopIndex 相同的缓存键格式，但添加 "_TOTAL" 后缀
        String cacheKey = String.format("ROLE_LOOP_INDEX_%s_%s_TOTAL", serviceRoleName, serviceName);
        Integer totalCount = (Integer) CacheUtils.get(cacheKey);
        if (totalCount == null) {
            logger.warn("未找到角色 [{}] 的总循环次数缓存", serviceRoleFullName);
            // 如果缓存中没有，则尝试从角色主机映射中获取
            totalCount = 1;
        }

        logger.debug("角色 [{}] 的总循环次数: {}", serviceRoleFullName, totalCount);
        return totalCount;
    }

    /**
     * 获取角色的安装数量
     * 
     * @param clusterId 集群ID
     * @return 角色安装数量
     */
    public Integer getRoleInstallCount(Integer clusterId) {
       return getRoleInstallCount(clusterId,serviceRoleName);
    }

    public Integer getRoleInstallCount(Integer clusterId, String serviceRoleName) {
        final String serviceRoleHostMappingKey = clusterId + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        Object mappingObj = CacheUtils.get(serviceRoleHostMappingKey);
        if (Objects.nonNull(mappingObj)) {
            JSONObject mapping = JSONUtil.parseObj(mappingObj);
            if (mapping.containsKey(serviceRoleName)) {
                int roleCount = mapping.getJSONArray(serviceRoleName).size();
                logger.debug("从 {} 中获取到 {} 节点数量为: {}", Constants.SERVICE_ROLE_HOST_MAPPING, serviceRoleName, roleCount);
                return roleCount;
            } else {
                logger.warn("在 {} 中未找到 {} 角色", Constants.SERVICE_ROLE_HOST_MAPPING, serviceRoleName);
            }
        } else {
            logger.warn("缓存中未找到 {}", serviceRoleHostMappingKey);
        }
        return 0;
    }

    public VolumeMountDTO[] volumeMountList(String workerPath, Map<Generators, List<ServiceConfig>> configFileMap,
            boolean enableKerberos) {
        List<VolumeMountDTO> volumeList = new ArrayList<>();
        int fileCount = 1;
        int pathCount = 1;
        for (Map.Entry<Generators, List<ServiceConfig>> entry : configFileMap.entrySet()) {
            Generators generators = entry.getKey();
            String configFilePath;
            if (StrUtil.isNotBlank(generators.getOutputDirectory())) {
                // 如果输出目录以斜杠开头，则直接使用输出目录作为输出文件的路径
                if (generators.getOutputDirectory().startsWith(Constants.SLASH)) {
                    configFilePath = String.join(Constants.SLASH, generators.getOutputDirectory(),
                            generators.getFilename());
                } else {
                    String output = generators.getOutputDirectory().replaceAll("^/+", "").replaceAll("/+$", "");
                    configFilePath = String.join(Constants.SLASH, workerPath, output, generators.getFilename());
                }
            } else {
                configFilePath = String.join(Constants.SLASH, workerPath, generators.getFilename());
            }

            // 配置文件挂载
            volumeList.add(new VolumeMountDTO("config" + fileCount++, configFilePath, configFilePath));

            // path配置目录挂载
            for (ServiceConfig serviceConfig : entry.getValue()) {
                if (Constants.PATH.equals(serviceConfig.getConfigType())) {
                    volumeList.add(
                            new VolumeMountDTO(
                                    "path" + pathCount++,
                                    (String) serviceConfig.getValue(),
                                    (String) serviceConfig.getValue()));
                }
            }
        }
        if (enableKerberos) {
            String keytabDir = "/etc/security/keytab/";
            volumeList.add(new VolumeMountDTO("keytab", keytabDir, keytabDir));
            String krb5Conf = "/etc/krb5.conf";
            volumeList.add(new VolumeMountDTO("krd5conf", krb5Conf, krb5Conf));
        }

        return volumeList.toArray(new VolumeMountDTO[0]);
    }

    public VolumeMountDTO[] hadoopVolumeMountList() {
        String coreSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/core-site.xml";
        String hdfsSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hdfs-site.xml";
        String hadoopEnv = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hadoop-env.sh";
        String mapredSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/mapred-site.xml";
        String yarnSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/yarn-site.xml";
        return new VolumeMountDTO[] {
                new VolumeMountDTO("core-site", coreSite, coreSite),
                new VolumeMountDTO("hdfs-site", hdfsSite, hdfsSite),
                new VolumeMountDTO("hadoop-env", hadoopEnv, hadoopEnv),
                new VolumeMountDTO("mapred-site", mapredSite, mapredSite),
                new VolumeMountDTO("yarn-site", yarnSite, yarnSite),
        };
    }

    /**
     * 根据基础端口和节点数量生成端口映射字符串
     *
     * @param basePort  基础端口号
     * @param nodeCount 节点数量
     * @return 逗号分隔的端口映射字符串，例如：30092,30093,30094
     */
    public String generatePortMappings(int basePort, int nodeCount) {
        StringBuilder portMappings = new StringBuilder();
        for (int i = 0; i < nodeCount; i++) {
            if (i > 0) {
                portMappings.append(",");
            }
            portMappings.append(basePort + i);
        }
        return portMappings.toString();
    }

    /**
     * 处理NodePort特殊绑定
     * 根据节点数量和配置生成端口映射并缓存，是一个通用方法
     */
    public void processNodePortMappings(Integer clusterId, List<ServiceConfig> serviceConfigList) {
        // 获取节点数量

        Object obj = CacheUtils.get(
                clusterId
                        + UNDERLINE
                        + SERVICE_ROLE_HOST_MAPPING);
        JSONObject parseObj = JSONUtil.parseObj(obj);
        JSONArray jsonArray = parseObj.getJSONArray(serviceRoleName);
        int nodeCount = jsonArray.size();

        // 获取基础端口值
        int baseNodePort = 30092; // 默认基础端口号

        // 从serviceConfigList中查找配置了nodePort的任意配置项
        for (ServiceConfig config : serviceConfigList) {
            if (StrUtil.equalsIgnoreCase(config.getName(), serviceRoleName + "_node_port_mappings")) {
                try {
                    // 将配置值解析为JSON数组
                    String jsonStr = JSONUtil.toJsonStr(config.getValue());
                    logger.info("端口映射配置原始值: {}", jsonStr);
                    JSONArray portMappingsArray = JSONUtil.parseArray(jsonStr);

                    // 创建一个新的JSON数组来存储处理后的结果
                    JSONArray resultArray = new JSONArray();
                    boolean hasValidMapping = false;

                    // 遍历JSON数组，处理每个端口映射
                    for (int i = 0; i < portMappingsArray.size(); i++) {
                        JSONObject portMapping = portMappingsArray.getJSONObject(i);

                        // 每个对象只有一个键值对，获取键(内部端口)和值(NodePort)
                        String internalPort = null;
                        String nodePortStr = null;

                        for (String key : portMapping.keySet()) {
                            internalPort = key;
                            nodePortStr = portMapping.getStr(key);
                            break;
                        }

                        if (internalPort != null && nodePortStr != null) {
                            try {
                                int nodePort = Integer.parseInt(nodePortStr);
                                // 为当前NodePort生成端口映射序列
                                String mappings = generatePortMappings(nodePort, nodeCount);

                                // 创建新的JSON对象，保持原始的内部端口作为key，生成的NodePort序列作为value
                                JSONObject resultMapping = new JSONObject();
                                resultMapping.set(internalPort, mappings);
                                resultArray.add(resultMapping);

                                logger.info("处理端口映射: 内部端口 {} -> NodePort {} -> 生成序列 {}",
                                        internalPort, nodePort, mappings);

                                hasValidMapping = true;
                            } catch (NumberFormatException e) {
                                logger.warn("解析NodePort值[{}]失败，跳过此端口映射", nodePortStr, e);
                            }
                        }
                    }

                    // 如果成功生成了端口映射，则更新配置值
                    if (hasValidMapping) {
                        config.setValue(resultArray);
                        logger.info("生成节点端口映射: {} -> {}", config.getName(), resultArray.toString());
                    } else {
                        // 如果没有成功解析任何端口映射，保留原始配置
                        logger.info("未能解析任何有效的端口映射，保留原始配置: {}", config.getName());
                    }
                    break;
                } catch (Exception e) {
                    logger.warn("解析配置[{}]的端口映射失败，保留原始配置", config.getName(), e);
                }
            }
        }
    }

}
