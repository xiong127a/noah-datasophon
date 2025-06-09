package com.datasophon.k8s.strategy;

import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface K8sServiceRoleStrategy {

    ExecResult handler(K8sServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException, IOException;

    /**
     * 获取服务配置时修改配置，根据ServiceName调用
     * handler之后handlerConfig之前调用
     * 提取角色本身配置和handler中自定义的变量
     */
    default void getConfig(Integer clusterId, List<ServiceConfig> list) {
    }
}
