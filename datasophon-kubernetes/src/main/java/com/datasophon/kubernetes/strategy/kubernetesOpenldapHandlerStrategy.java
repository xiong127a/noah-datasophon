package com.datasophon.kubernetes.strategy;

import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;

import java.io.IOException;
import java.sql.SQLException;

public class kubernetesOpenldapHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public kubernetesOpenldapHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException, IOException {
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String hostname = command.getHostname();
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            KubernetesMinaUtils.createUserAndGroup(hostname, "ldap", "ldap");
        }
        return serviceHandler.start(command);
    }



}
