package com.datasophon.kubernetes.strategy;

import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.FileUtils;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;

import java.io.IOException;
import java.util.List;

public class KubernetesTrinoHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesTrinoHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) throws IOException {
        ExecResult startResult = new ExecResult();
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());
        startResult = serviceHandler.start(command);
        return startResult;

    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        for (ServiceConfig config : list) {
            String name = config.getName();
            if (name != null && name.equals("discovery.uri")) {
                try {
                    String value = (String) config.getValue();// 获取当前配置值
                    config.setValue(FileUtils.replaceHost(value, "trino-trinocoordinator.datasophon.svc.cluster.local")); // 去掉最后一个逗号
                } catch (Exception e) {
                    // 忽略错误
                }
            }
        }

    }
}
