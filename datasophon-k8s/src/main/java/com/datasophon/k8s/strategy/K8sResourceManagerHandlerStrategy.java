package com.datasophon.k8s.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.sql.SQLException;

public class K8sResourceManagerHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sResourceManagerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException, IOException {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        String hostname = command.getHostname();
        if (command.getEnableKerberos()) {
            logger.info("start to get resourcemanager keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/rm.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "rm/" + hostname, "rm.service.keytab");
            }
        }
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig());
            String coreSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/core-site.xml";
            String hdfsSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hdfs-site.xml";
            String hadoopEnv = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hadoop-env.sh";
            VolumeMountDTO[] volumeMounts = {
                    new VolumeMountDTO("core-site", coreSite, coreSite),
                    new VolumeMountDTO("hdfs-site", hdfsSite, hdfsSite),
                    new VolumeMountDTO("hadoop-env", hadoopEnv, hadoopEnv),
            };
            String jobCmd =
                    "su - hdfs -c \"/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -test -e /user/yarn\" " +
                            "|| (su - hdfs -c \"/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -mkdir -p /user/yarn\" " +
                            "&& su - hdfs -c \"/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -chown yarn:hadoop /user/yarn\")\n";
            try {
                K8sUtil.runJob(
                        Constants.DATASOPHON,
                        "create-yarn-dir",
                        kubeClient,
                        volumeMounts,
                        DockerImageUtils.getString(command.getServiceName()),
                        jobCmd,
                        logger,
                        command.getHostname()
                );
                logger.info("create yarn dir success");
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.info("create yarn dir failed");
                startResult.setExecResult(false);
                return startResult;
            }

            // 存在 tez 则创建软连接
            final String tezHomePath = Constants.INSTALL_PATH + Constants.SLASH + "tez";
            if (K8sMinaUtils.checkPathExists(hostname, tezHomePath)) {
                K8sMinaUtils.execCmdWithResult(hostname, "ln -s " + tezHomePath + "/conf/tez-site.xml " + workPath + "/etc/hadoop/tez-site.xml");
            }
        }
        if (command.getEnableRangerPlugin()) {
            logger.info("Start to enable ranger yarn plugin");
            if (!K8sMinaUtils.checkPathExists(hostname, workPath + "/ranger-yarn-plugin/success.id")) {
                String commands =
                        "cd " + workPath + "/ranger-yarn-plugin && " +
                                " sh " + workPath + "/ranger-yarn-plugin/enable-yarn-plugin.sh";
                K8sMinaUtils.execCmdWithResult(hostname, commands);
                boolean success = K8sMinaUtils.writeUtf8String(hostname, "success", workPath + "/ranger-yarn-plugin/success.id");
                if (success) {
                    logger.info("Enable ranger yarn plugin failed");
                    startResult.setExecResult(true);
                } else {
                    logger.info("Enable ranger yarn plugin failed");
                    return startResult;
                }
            }
        }
        return serviceHandler.start(command);
    }
}
