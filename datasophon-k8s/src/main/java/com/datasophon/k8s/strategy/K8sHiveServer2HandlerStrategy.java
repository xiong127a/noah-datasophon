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
        VolumeMountDTO[] volumeMountDTOS = volumeMountList(workPath, command.getConfigFileMap(), command.getEnableKerberos());
        VolumeMountDTO[] volumeMounts = hadoopVolumeMountList();
        VolumeMountDTO[] allVolume = Stream.concat(
                Arrays.stream(volumeMountDTOS),
                Arrays.stream(volumeMounts)
        ).toArray(VolumeMountDTO[]::new);


        logger.info("command is slave : {}", command.isSlave());
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE) && !command.isSlave()) {
            String jobCmd = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + Constants.SLASH +
                    "bin/schematool -dbType mysql -initSchema";
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                K8sUtil.runJob(
                        Constants.DATASOPHON,
                        "init-hive-db",
                        kubeClient,
                        allVolume,
                        DockerImageUtils.getString(command.getServiceName()),
                        jobCmd,
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
        String jobCmd = "";
        if (command.getEnableKerberos()) {
            logger.info("start to get hive keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/hive.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "hive/" + hostname, "hive.service.keytab");
            }
            jobCmd = "su - hdfs -c \"kinit -kt /etc/security/keytab/spnego.service.keytab HTTP/" + hostname + "@HADOOP.COM\"  && ";
            jobCmd += "su - hdfs -c \"kinit -kt /etc/security/keytab/hdfs.user.keytab hdfs/user@HADOOP.COM\" && ";
        }

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            String baseCmd = "/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs";
            jobCmd += "su - hdfs -c \"" + baseCmd + " -test -e /user/hive/warehouse\" " +
                    "|| (su - hdfs -c \"" + baseCmd + " -mkdir -p /user/hive/warehouse\" " +
                    "&& su - hdfs -c \"" + baseCmd + " -chown hive:hadoop /user/hive/warehouse\") && " +
                    "su - hdfs -c \"" + baseCmd + " -test -e /tmp/hive\" " +
                    "|| (su - hdfs -c \"" + baseCmd + " -mkdir -p /tmp/hive\" " +
                    "&& su - hdfs -c \"" + baseCmd + " -chown hive:hadoop /tmp/hive\" " +
                    "&& su - hdfs -c \"" + baseCmd + " -chmod 777 /tmp/hive\")";
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                K8sUtil.runCmd(
                        Constants.DATASOPHON,
                        kubeClient,
                        "hdfs-namenode",
                        command.getNnHost(),
                        jobCmd);
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
