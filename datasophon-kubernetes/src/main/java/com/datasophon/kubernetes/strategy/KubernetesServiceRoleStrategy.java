package com.datasophon.kubernetes.strategy;

import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface KubernetesServiceRoleStrategy {

    ExecResult handler(KubernetesServiceRoleOperateCommand command);

    /**
     * 获取服务配置时修改配置，根据ServiceName调用
     * handler之后handlerConfig之前调用
     * 提取角色本身配置和handler中自定义的变量
     */
    default void getConfig(Integer clusterId, List<ServiceConfig> list) {
    }
}
