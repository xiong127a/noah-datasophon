package com.datasophon.k8s.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.DockerImageUtils;
import com.datasophon.k8s.util.K8sKerberosUtils;
import com.datasophon.k8s.util.K8sUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class K8sHbaseHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sHbaseHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws IOException {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        final String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        String hostname = command.getHostname();

        if (command.getEnableRangerPlugin()) {
            logger.info("start to enable  hbase plugin");
            ArrayList<String> commands = new ArrayList<>();
            commands.add("sh");
            commands.add("./enable-hbase-plugin.sh");
            if (!FileUtil.exist(Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName()
                    + "/ranger-hbase-plugin/success.id")) {
                ExecResult execResult = ShellUtils.execWithStatus(Constants.INSTALL_PATH + Constants.SLASH
                        + command.getDecompressPackageName() + "/ranger-hbase-plugin", commands, 30L, logger);
                if (execResult.getExecResult()) {
                    logger.info("enable ranger hbase plugin success");
                    FileUtil.writeUtf8String("success", Constants.INSTALL_PATH + Constants.SLASH
                            + command.getDecompressPackageName() + "/ranger-hbase-plugin/success.id");
                } else {
                    logger.info("enable ranger hbase plugin failed");
                    return execResult;
                }
            }
        }

        if (command.getEnableKerberos()) {
            logger.info("start to get hbase keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!FileUtil.exist("/etc/security/keytab/hbase.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "hbase/" + hostname, "hbase.keytab");
            }
        }

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            String hadoopHome = "/opt/datasophon/hadoop-3.3.3";
            String dirPath = "/hbase";
            String jobCmd = "su - hdfs -c \"" + hadoopHome + "/bin/hdfs dfs -test -e " + dirPath + "\" " +
                    "|| (su - hdfs -c \"" + hadoopHome + "/bin/hdfs dfs -mkdir -p " + dirPath + "\" " +
                    "&& su - hdfs -c \"" + hadoopHome + "/bin/hdfs dfs -chown hbase:hadoop " + dirPath + "\" " +
                    "&& su - hdfs -c \"" + hadoopHome + "/bin/hdfs dfs -chmod 777 " + dirPath + "\")\n";
            VolumeMountDTO[] volumeMountDTOS = volumeMountList(workPath, command.getConfigFileMap());
            VolumeMountDTO[] volumeMounts = hadoopVolumeMountList();
            VolumeMountDTO[] allVolume = Stream.concat(
                    Arrays.stream(volumeMountDTOS),
                    Arrays.stream(volumeMounts)
            ).toArray(VolumeMountDTO[]::new);

            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                K8sUtil.runJob(
                        Constants.DATASOPHON,
                        "init-hbase-dir",
                        kubeClient,
                        allVolume,
                        DockerImageUtils.getString(command.getServiceName()),
                        jobCmd,
                        logger,
                        command.getHostname()
                );
                logger.info("init hbase dir success");
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.info("init hbase dir failed");
                startResult.setExecResult(false);
                return startResult;
            }
        }

        startResult = serviceHandler.start(command);
        return startResult;

    }
}
