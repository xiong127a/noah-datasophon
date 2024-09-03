package com.datasophon.k8s.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class K8sHiveServer2HandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sHiveServer2HandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws IOException {
        ExecResult startResult = new ExecResult();
        final String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String hostname = command.getHostname();
        String coreSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/core-site.xml";
        String hdfsSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hdfs-site.xml";
        String hadoopEnv = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hadoop-env.sh";
        String mapredSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/mapred-site.xml";
        String yarnSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/yarn-site.xml";
        VolumeMountDTO[] volumeMountDTOS = volumeMountList(workPath, command.getConfigFileMap());
        VolumeMountDTO[] volumeMounts = {
                new VolumeMountDTO("core-site", coreSite, coreSite),
                new VolumeMountDTO("hdfs-site", hdfsSite, hdfsSite),
                new VolumeMountDTO("hadoop-env", hadoopEnv, hadoopEnv),
                new VolumeMountDTO("mapred-site", mapredSite, mapredSite),
                new VolumeMountDTO("yarn-site", yarnSite, yarnSite),
        };
        VolumeMountDTO[] allVolume = Stream.concat(
                Arrays.stream(volumeMountDTOS),
                Arrays.stream(volumeMounts)
        ).toArray(VolumeMountDTO[]::new);

        if (command.getEnableRangerPlugin()) {
            logger.info("Start to enable Hive HDFS plugin");
            String successFilePath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + "/ranger-hive-plugin/success.id";
            String pluginPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + "/ranger-hive-plugin";
            if (!K8sMinaUtils.checkPathExists(hostname, successFilePath)) {
                String result = K8sMinaUtils.execCmdWithResult(hostname, "cd " + pluginPath + " && sh ./enable-hive-plugin.sh");
                if (!"failed".equals(result)) {
                    logger.info("Enable Ranger Hive plugin success");
                    K8sMinaUtils.writeUtf8String(hostname, "success", successFilePath);
                    startResult.setExecResult(true);
                } else {
                    logger.info("Enable Ranger Hive plugin failed");
                    startResult.setExecResult(false);
                    return startResult;
                }
            }
        }


        logger.info("command is slave : {}", command.isSlave());
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE) && !command.isSlave()) {
            KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig());
            String jobCmd = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + Constants.SLASH +
                    "bin/schematool -dbType mysql -initSchema";
            try {
                K8sUtil.runJob(
                        Constants.DATASOPHON,
                        "init-hive-db",
                        kubeClient,
                        allVolume,
                        DockerImageUtils.getString(command.getServiceName()),
                        jobCmd,
                        logger,
                        command.getHostname()
                );
                logger.info("init hive schema success");
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.info("init hive schema failed");
                startResult.setExecResult(false);
                return startResult;
            }
        }

        if (command.getEnableKerberos()) {
            logger.info("start to get hive keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/hive.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "hive/" + hostname, "hive.service.keytab");
            }
        }

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig());
            String baseCmd = "/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs";
            String jobCmd = "su - hdfs -c \"" + baseCmd + " -test -e /user/hive/warehouse\" " +
                    "|| (su - hdfs -c \"" + baseCmd + " -mkdir -p /user/hive/warehouse\" " +
                    "&& su - hdfs -c \"" + baseCmd + " -chown hive:hadoop /user/hive/warehouse\") && " +
                    "su - hdfs -c \"" + baseCmd + " -test -e /tmp/hive\" " +
                    "|| (su - hdfs -c \"" + baseCmd + " -mkdir -p /tmp/hive\" " +
                    "&& su - hdfs -c \"" + baseCmd + " -chown hive:hadoop /tmp/hive\" " +
                    "&& su - hdfs -c \"" + baseCmd + " -chmod 777 /tmp/hive\")";
            try {
                K8sUtil.runJob(
                        Constants.DATASOPHON,
                        "init-hive-dir",
                        kubeClient,
                        allVolume,
                        DockerImageUtils.getString(command.getServiceName()),
                        jobCmd,
                        logger,
                        command.getHostname()
                );
                logger.info("init hive dir success");
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.info("init hive dir failed");
                startResult.setExecResult(false);
                return startResult;
            }

            // 存在 tez 则创建软连接
            final String tezHomePath = Constants.INSTALL_PATH + Constants.SLASH + "tez";
            if (K8sMinaUtils.checkPathExists(hostname, tezHomePath)) {
                K8sMinaUtils.execCmdWithResult(hostname, "ln -s " + tezHomePath + "/conf/tez-site.xml " + workPath + "/conf/tez-site.xml");
            }
        }

        startResult = serviceHandler.start(command);
        return startResult;
    }
}
