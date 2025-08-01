package com.datasophon.kubernetes.strategy;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.util.CommonUtil;
import com.datasophon.kubernetes.util.KubernetesUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class KubernetesAbstractHandlerStrategy {
    public String serviceName;

    public String serviceRoleName;

    public String serviceRoleFullName;

    public final String CLUSTER_DOMAIN = "svc.cluster.local";

    public Logger logger;

    public KubernetesAbstractHandlerStrategy(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constants.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public int getCurrentRoleLoopIndex() {
        Integer count = (Integer) CacheUtils.get(serviceRoleFullName + "_" + Constants.CURRENT_NODE_CNT);
        if (Objects.isNull(count)) {
            count = getTotalRoleLoopCount();
        }
        return count;
    }

    /**
     * 获取当前角色需要的总循环次数
     *
     * @return 总循环次数
     */
    public int getTotalRoleLoopCount() {
        // 使用与 getCurrentRoleLoopIndex 相同的缓存键格式，但添加 "_TOTAL" 后缀
        return (Integer) CacheUtils.get(serviceRoleFullName + "_" + Constants.ROLE_NODE_CNT);
    }

    /**
     * 获取角色的安装数量
     *
     * @param clusterId 集群ID
     * @return 角色安装数量
     */
    public Integer getRoleInstallCount(Integer clusterId) {
        return getRoleInstallCount(clusterId, serviceRoleName);
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

    /**
     * 直接在指定Pod中批量执行MySQL SQL命令
     *
     * @param namespace     Kubernetes命名空间
     * @param kubeClient    Kubernetes客户端
     * @param podName       Pod名称(如: starrocks-srfe-0)
     * @param sqlStatements SQL语句列表
     * @return 执行结果
     */
    public ExecResult executeMySqlInPod(String namespace, KubernetesClient kubeClient,
            String podName, List<String> sqlStatements) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(sqlStatements)) {
            logger.warn("没有提供SQL语句，无法执行");
            ExecResult result = new ExecResult();
            result.setExecResult(false);
            result.setExecErrOut("没有提供SQL语句");
            return result;
        }

        // 使用Hutool的StrUtil.join来拼接命令，更优雅
        List<String> mysqlCommands = sqlStatements.stream()
                .map(sql -> String.format("mysql -h127.0.0.1 -P9030 -uroot --connect-timeout=10 -e \"%s\"",
                        sql.replace("\"", "\\\"")))
                .collect(Collectors.toList());

        String finalCmd = cn.hutool.core.util.StrUtil.join(" && ", mysqlCommands);

        logger.info("在Pod [{}] 中执行MySQL命令: {}", podName, finalCmd);
        return KubernetesUtil.runCmdInPod(namespace, kubeClient, podName, finalCmd);
    }
}
