package com.datasophon.k8s.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.util.List;

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

        String jobCmd = "";
        if (command.getEnableKerberos()) {
            logger.info("start to get hbase keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/hbase.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "hbase/" + hostname, "hbase.keytab");
            }
            jobCmd = "su - hdfs -c \"kinit -kt /etc/security/keytab/spnego.service.keytab HTTP/" + hostname + "@HADOOP.COM\"  && ";
            jobCmd += "su - hdfs -c \"kinit -kt /etc/security/keytab/hdfs.user.keytab hdfs/user@HADOOP.COM\" && ";
        }

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)&&command.getServiceRoleType().equals(ServiceRoleType.MASTER)) {
            String hadoopHome = "/opt/datasophon/hadoop-3.3.3";
            String dirPath = "/hbase";
            jobCmd += "su - hdfs -c \"" + hadoopHome + "/bin/hdfs dfs -test -e " + dirPath + "\" " +
                    "|| (su - hdfs -c \"" + hadoopHome + "/bin/hdfs dfs -mkdir -p " + dirPath + "\" " +
                    "&& su - hdfs -c \"" + hadoopHome + "/bin/hdfs dfs -chown hbase:hadoop " + dirPath + "\" " +
                    "&& su - hdfs -c \"" + hadoopHome + "/bin/hdfs dfs -chmod 777 " + dirPath + "\")\n";
            logger.info(jobCmd);
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                K8sUtil.runCmd(
                        Constants.DATASOPHON,
                        kubeClient,
                        "hdfs-namenode",
                        command.getNnHost(),
                        jobCmd);
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

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        for (ServiceConfig config : list) {
            String name = config.getName();
            if (name != null && name.equals("hbase.zookeeper.quorum")) {
                try {
                    String value = (String) config.getValue();// 获取当前配置值
                    String[] split = value.split(",");
                    String newValue = "";
                    for (int i = 0; i < split.length; i++){
                        newValue+= "zookeeper-zkserver-" + i + ".zookeeper-zkserver.datasophon.svc.cluster.local:2181,";
                    }
                     config.setValue(newValue.substring(0, newValue.length() - 1)); // 去掉最后一个逗号
                } catch (Exception e) {
                    // 忽略错误
                }
            }
        }

    }
}
