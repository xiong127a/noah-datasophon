package com.datasophon.k8s.strategy;

import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sKerberosUtils;
import com.datasophon.k8s.util.K8sMinaUtils;

import java.io.IOException;

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
