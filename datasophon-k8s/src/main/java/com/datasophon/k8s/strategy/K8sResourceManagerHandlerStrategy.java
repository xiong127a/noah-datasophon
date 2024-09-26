package com.datasophon.k8s.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class K8sResourceManagerHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sResourceManagerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException, IOException {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        String hostname = command.getHostname();
        String jobCmd = "";
        if (command.getEnableKerberos()) {
            logger.info("start to get resourcemanager keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/rm.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "rm/" + hostname, "rm.service.keytab");
            }
        }
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            // 存在 tez 则创建软连接
            final String tezHomePath = Constants.INSTALL_PATH + Constants.SLASH + "tez";
            if (K8sMinaUtils.checkPathExists(hostname, tezHomePath)) {
                K8sMinaUtils.execCmdWithResult(hostname, "ln -s " + tezHomePath + "/conf/tez-site.xml " + workPath + "/etc/hadoop/tez-site.xml");
            }
        }
        return serviceHandler.start(command);
    }
}
