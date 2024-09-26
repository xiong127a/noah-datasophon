package com.datasophon.k8s.strategy;

import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sKerberosUtils;
import com.datasophon.k8s.util.K8sMinaUtils;

import java.io.IOException;

public class K8sRangerAdminHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sRangerAdminHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws IOException {
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String hostname = command.getHostname();

        if (command.getEnableKerberos()) {
            logger.info("start to get ranger keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/spnego.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "HTTP/" + hostname, "spnego.service.keytab");
            }
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/rangeradmin.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "rangeradmin/" + hostname, "rangeradmin.keytab");
            }
        }

        return serviceHandler.start(command);
    }

}
