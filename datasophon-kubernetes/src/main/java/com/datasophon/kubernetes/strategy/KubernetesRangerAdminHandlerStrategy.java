package com.datasophon.kubernetes.strategy;

import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubernetesKerberosUtils;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;

import java.io.IOException;

public class KubernetesRangerAdminHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesRangerAdminHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) throws IOException {
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String hostname = command.getHostname();

        if (command.getEnableKerberos()) {
            logger.info("start to get ranger keytab file");
            KubernetesKerberosUtils.createKeytabDir(hostname);
            if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/security/keytab/spnego.service.keytab")) {
                KubernetesKerberosUtils.downloadKeytabFromMaster(hostname, "HTTP/" + hostname, "spnego.service.keytab");
            }
            if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/security/keytab/rangeradmin.keytab")) {
                KubernetesKerberosUtils.downloadKeytabFromMaster(hostname, "rangeradmin/" + hostname, "rangeradmin.keytab");
            }
        }

        return serviceHandler.start(command);
    }

}
