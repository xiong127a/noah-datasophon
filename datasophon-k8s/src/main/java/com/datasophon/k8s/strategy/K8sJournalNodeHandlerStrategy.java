package com.datasophon.k8s.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sKerberosUtils;
import com.datasophon.k8s.util.K8sMinaUtils;

import java.io.IOException;
import java.sql.SQLException;

public class K8sJournalNodeHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sJournalNodeHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException, IOException {
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        if (command.getEnableKerberos()) {
            String hostname = command.getHostname();
            logger.info("start to get journalnode keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            String hadoopConfDir =
                    Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + "/etc/hadoop/";
            if (!K8sMinaUtils.checkPathExists(hostname, hadoopConfDir + "ssl-server.xml")) {
                K8sMinaUtils.execCmdWithResult(hostname, "cp " + hadoopConfDir + "ssl-server.xml.template " + hadoopConfDir + "ssl-server.xml");
            }
            if (!K8sMinaUtils.checkPathExists(hostname, hadoopConfDir + "ssl-client.xml")) {
                K8sMinaUtils.execCmdWithResult(hostname, "cp " + hadoopConfDir + "ssl-client.xml.template " + hadoopConfDir + "ssl-client.xml");
            }
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/jn.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "jn/" + command.getHostname(), "jn.service.keytab");
            }
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/keystore")) {
                K8sMinaUtils.execCmdWithResult(hostname, "cd " + Constants.WORKER_SCRIPT_PATH + " && sh keystore.sh " + hostname);
            }
        }
        return serviceHandler.start(command);
    }
}
