package com.datasophon.api.kubernetes.handler;

import cn.hutool.core.util.ObjectUtil;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateServiceConfigCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.kubernetes.actor.handler.KubernetesConfigureServiceHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class KubernetesServiceConfigureHandler extends ServiceHandler {

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) {
        // config

        ServiceRoleInfo cloneByStream = ObjectUtil.cloneByStream(serviceRoleInfo);
        GenerateServiceConfigCommand generateServiceConfigCommand = new GenerateServiceConfigCommand();
        generateServiceConfigCommand.setServiceName(serviceRoleInfo.getParentName());
        generateServiceConfigCommand.setClusterId(serviceRoleInfo.getClusterId()); // 设置集群ID
        generateServiceConfigCommand.setCofigFileMap(serviceRoleInfo.getConfigFileMap());
        generateServiceConfigCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        generateServiceConfigCommand.setRunAs(serviceRoleInfo.getRunAs());
        if ("zkserver".equalsIgnoreCase(serviceRoleInfo.getName())) {
            generateServiceConfigCommand.setMyid((Integer) CacheUtils.get("zkserver_" + serviceRoleInfo.getHostname()));
        }
        String namespace = ClusterInfoUtils.getKubernetesNamespace(serviceRoleInfo.getClusterId());
        generateServiceConfigCommand.setNamespace(namespace);
        generateServiceConfigCommand.setServiceRoleName(serviceRoleInfo.getName());
        generateServiceConfigCommand.setHostName(serviceRoleInfo.getHostname());
        
        // 直接调用KubernetesConfigureServiceHandler处理，无需通过Actor
        try {
            log.info("start configure {}", generateServiceConfigCommand.getServiceName());
            
            KubernetesConfigureServiceHandler serviceHandler = new KubernetesConfigureServiceHandler(
                    generateServiceConfigCommand.getServiceName(), 
                    generateServiceConfigCommand.getServiceRoleName());
            
            // 设置集群ID到handler，更新logger路径
            serviceHandler.setClusterId(generateServiceConfigCommand.getClusterId());
            
            ExecResult configResult = serviceHandler.configure(
                    generateServiceConfigCommand.getNamespace(),
                    generateServiceConfigCommand.getCofigFileMap(),
                    generateServiceConfigCommand.getDecompressPackageName(),
                    generateServiceConfigCommand.getMyid(),
                    generateServiceConfigCommand.getServiceRoleName(),
                    generateServiceConfigCommand.getRunAs(),
                    generateServiceConfigCommand.getHostName());
            
            log.info("{} configure result: {}", generateServiceConfigCommand.getServiceName(),
                    configResult.getExecResult() ? "success" : "failed");
            
            if (Objects.nonNull(configResult) && configResult.getExecResult()) {
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(cloneByStream);
                }
            }
            return configResult;
        } catch (Exception e) {
            log.error("配置服务失败", e);
            return new ExecResult();
        }
    }
}
