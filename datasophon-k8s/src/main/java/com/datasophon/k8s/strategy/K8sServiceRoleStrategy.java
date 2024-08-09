package com.datasophon.k8s.strategy;

import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;

import java.sql.SQLException;

public interface K8sServiceRoleStrategy {

    public ExecResult handler(K8sServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException;
}
