package com.datasophon.kubernetes.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubeUtil;
import com.datasophon.kubernetes.util.KubernetesKerberosUtils;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;
import com.datasophon.kubernetes.util.KubernetesUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.util.List;

public class KubernetesHbaseHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesHbaseHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());
        final String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        String hostname = command.getHostname();

        String jobCmd = "";
        if (command.getEnableKerberos()) {
            logger.info("start to get hbase keytab file");
            KubernetesKerberosUtils.createKeytabDir(hostname);
            if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/security/keytab/hbase.keytab")) {
                KubernetesKerberosUtils.downloadKeytabFromMaster(hostname, "hbase/" + hostname, "hbase.keytab");
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
                KubernetesUtil.runCmd(
                        command.getNamespace(),
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
                    StringBuilder newValue = new StringBuilder();
                    for (int i = 0; i < split.length; i++){
                        newValue.append("zookeeper-zkserver-").append(i).append(".zookeeper-zkserver.datasophon.svc.cluster.local:2181,");
                    }
                     config.setValue(newValue.substring(0, newValue.length() - 1)); // 去掉最后一个逗号
                } catch (Exception e) {
                    // 忽略错误
                }
            }
        }

    }
}
