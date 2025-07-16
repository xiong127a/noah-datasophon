package com.datasophon.kubernetes.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;

import java.sql.SQLException;

public class KubernetesPostgresqlHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesPostgresqlHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException {
        ExecResult execResult;
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        String hostname = command.getHostname();
        RunAs runAs = command.getRunAs();
        if (command.getCommandType() == CommandType.INSTALL_SERVICE ) {
            KubernetesMinaUtils.execCmdWithResult(hostname,"chmod -R 700 "+workPath+"/data/");
            KubernetesMinaUtils.execCmdWithResult(hostname,String.format("chown -R %s:%s  %s/data/",runAs.getUser(),runAs.getGroup(),workPath));
        }
        execResult = serviceHandler.start(command);
        return execResult;
    }
}