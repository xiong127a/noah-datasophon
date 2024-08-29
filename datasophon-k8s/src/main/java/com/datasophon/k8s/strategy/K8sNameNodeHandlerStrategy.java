package com.datasophon.k8s.strategy;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class K8sNameNodeHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sNameNodeHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws IOException {
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String hostname = command.getHostname();
        ExecResult execResult = new ExecResult();

        if (command.getEnableKerberos()) {
            logger.info("Start to get namenode keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/nn.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "nn/" + hostname, "nn.service.keytab");
            }
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/spnego.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "HTTP/" + hostname, "spnego.service.keytab");
            }
        }

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            Map<Generators, List<ServiceConfig>> configFileMap = command.getConfigFileMap();
            String namenodeDir = configFileMap.values()
                    .stream()
                    .flatMap(List::stream)
                    .filter(t -> "dfs.namenode.name.dir".equals(t.getName()))
                    .map(t -> Convert.toStr(t.getValue()))
                    .findFirst()
                    .orElse("");
            String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
            KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig());
            VolumeMountDTO[] volumeMounts = volumeMountList(workPath, configFileMap);

            if (command.isSlave()) {
                // 执行hdfs namenode -bootstrapStandby
                logger.info("Start to execute hdfs namenode -bootstrapStandby");
                String jobCmd = "echo Y | /opt/datasophon/hadoop-3.3.3/bin/hdfs namenode -bootstrapStandby";
                try {
                    K8sUtil.runJob(
                            Constants.DATASOPHON,
                            "hdfs-namenode-format-standby",
                            kubeClient,
                            volumeMounts,
                            DockerImageUtils.getString(command.getServiceName()),
                            jobCmd,
                            logger,
                            hostname);
                    execResult.setExecResult(true);
                    logger.info("Namenode standby success");
                } catch (Exception e) {
                    logger.error("Namenode standby failed");
                    return execResult;
                }
            } else {
                logger.info("Start to execute format namenode");
                String jobCmd = "echo Y | /opt/datasophon/hadoop-3.3.3/bin/hdfs namenode -format smhadoop";
                K8sMinaUtils.execCmdWithResult(hostname, "rm -rf " + namenodeDir);
                try {
                    K8sUtil.runJob(
                            Constants.DATASOPHON,
                            "hdfs-namenode-format",
                            kubeClient,
                            volumeMounts,
                            DockerImageUtils.getString(command.getServiceName()),
                            jobCmd,
                            logger,
                            hostname);
                    execResult.setExecResult(true);
                    logger.info("Namenode format success");
                } catch (Exception e) {
                    logger.error("Namenode format failed");
                    return execResult;
                }
            }
        }

        if (command.getEnableRangerPlugin()) {
//            logger.info("Start to enable ranger hdfs plugin");
//            ArrayList<String> commands = new ArrayList<>();
//            commands.add("sh");
//            commands.add(workPath + "/ranger-hdfs-plugin/enable-hdfs-plugin.sh");
//            if (!FileUtil.exist(workPath + "/ranger-hdfs-plugin/success.id")) {
//                ExecResult execResult = ShellUtils.execWithStatus(workPath + "/ranger-hdfs-plugin", commands, 30L, logger);
//                if (execResult.getExecResult()) {
//                    logger.info("Enable ranger hdfs plugin success");
//                    // 写入ranger plugin集成成功标识
//                    FileUtil.writeUtf8String("success", workPath + "/ranger-hdfs-plugin/success.id");
//                } else {
//                    logger.info("Enable ranger hdfs plugin failed");
//                    return execResult;
//                }
//            }
        }

        return serviceHandler.start(command);
    }

}
