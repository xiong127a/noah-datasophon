package com.datasophon.kubernetes.strategy;

import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;

import java.util.List;

public interface KubernetesServiceRoleStrategy {

    ExecResult handler(KubernetesServiceRoleOperateCommand command);

    /**
     * 获取服务配置时修改配置，根据ServiceName调用
     * handler之后handlerConfig之前调用
     * 提取角色本身配置和handler中自定义的变量
     *
     * @param clusterId 集群ID
     * @param namespace Kubernetes命名空间
     * @param list      服务配置列表
     */
    default void getConfig(Integer clusterId, String namespace, List<ServiceConfig> list) {
    }
}
