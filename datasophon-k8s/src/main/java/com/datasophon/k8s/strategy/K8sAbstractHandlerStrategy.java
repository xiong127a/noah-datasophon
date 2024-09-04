package com.datasophon.k8s.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
public class K8sAbstractHandlerStrategy {
    public String serviceName;

    public String serviceRoleName;

    private String serviceRoleFullName;

    public Logger logger;

    public K8sAbstractHandlerStrategy(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public VolumeMountDTO[] volumeMountList(String workerPath, Map<Generators, List<ServiceConfig>> configFileMap) {
        List<VolumeMountDTO> volumeList = new ArrayList<>();
        int fileCount = 1;
        int pathCount = 1;
        for (Map.Entry<Generators, List<ServiceConfig>> entry : configFileMap.entrySet()) {
            Generators generators = entry.getKey();
            String configFilePath;
            if (StrUtil.isNotBlank(generators.getOutputDirectory())) {
                String output = generators.getOutputDirectory().replaceAll("^/+", "").replaceAll("/+$", "");
                configFilePath = String.join(Constants.SLASH, workerPath, output, generators.getFilename());
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
        return volumeList.toArray(new VolumeMountDTO[0]);
    }

    public VolumeMountDTO[] hadoopVolumeMountList() {
        String coreSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/core-site.xml";
        String hdfsSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hdfs-site.xml";
        String hadoopEnv = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hadoop-env.sh";
        String mapredSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/mapred-site.xml";
        String yarnSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/yarn-site.xml";
        return new VolumeMountDTO[]{
                new VolumeMountDTO("core-site", coreSite, coreSite),
                new VolumeMountDTO("hdfs-site", hdfsSite, hdfsSite),
                new VolumeMountDTO("hadoop-env", hadoopEnv, hadoopEnv),
                new VolumeMountDTO("mapred-site", mapredSite, mapredSite),
                new VolumeMountDTO("yarn-site", yarnSite, yarnSite),
        };
    }

}
