package com.datasophon.k8s.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sMinaUtils;

import java.sql.SQLException;
import java.util.ArrayList;

public class K8sPostgresqlHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sPostgresqlHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException {
        ExecResult execResult;
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        String hostname = command.getHostname();
        RunAs runAs = command.getRunAs();
        if (command.getCommandType() == CommandType.INSTALL_SERVICE ) {
            K8sMinaUtils.execCmdWithResult(hostname,"chmod -R 700 "+workPath+"/data/");
            K8sMinaUtils.execCmdWithResult(hostname,String.format("chown -R %s:%s  %s/data/",runAs.getUser(),runAs.getGroup(),workPath));
        }
        execResult = serviceHandler.start(command);
        return execResult;
    }
}