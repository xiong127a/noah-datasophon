package com.datasophon.kubernetes.strategy;

import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubernetesKerberosUtils;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;

import java.io.IOException;
import java.sql.SQLException;

public class KubernetesJournalNodeHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesJournalNodeHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException, IOException {
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());
        if (command.getEnableKerberos()) {
            String hostname = command.getHostname();
            logger.info("start to get journalnode keytab file");
            KubernetesKerberosUtils.createKeytabDir(hostname);
            if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/security/keytab/jn.service.keytab")) {
                KubernetesKerberosUtils.downloadKeytabFromMaster(hostname, "jn/" + command.getHostname(), "jn.service.keytab");
            }
        }
        return serviceHandler.start(command);
    }
}
