package com.datasophon.k8s.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sKerberosUtils;

import java.io.IOException;

public class K8sZkServerHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sZkServerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws IOException {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        if (command.getEnableKerberos()) {
            logger.info("start to get zkserver keytab file");
            String hostname = command.getHostname();
            K8sKerberosUtils.createKeytabDir(command.getHostname());
            if (!FileUtil.exist("/etc/security/keytab/zkserver.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "zookeeper/" + hostname, "zkserver.service.keytab");
            }
            if (!FileUtil.exist("/etc/security/keytab/zkclient.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "zkcli/" + hostname, "zkclient.service.keytab");
            }
        }
        startResult = serviceHandler.start(command);
        return startResult;
    }

}
