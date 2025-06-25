package com.datasophon.k8s.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.K8sKerberosUtils;

import java.io.IOException;
import java.util.List;

public class K8sZkServerHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sZkServerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws IOException {

        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        if (command.getEnableKerberos()) {
            logger.info("start to get zkserver keytab file");
            String hostname = command.getHostname();
            K8sKerberosUtils.createKeytabDir(command.getHostname());
            if (!FileUtil.exist("/etc/security/keytab/zkserver.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "zookeeper/" + hostname, "zkserver.service.keytab");
            }
            if (!FileUtil.exist("/etc/security/keytab/zkclient.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "zkcli/" + hostname, "zkclient.service.keytab");
            }
        }
        startResult = serviceHandler.start(command);
        return startResult;
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        // 定义ZK服务名
        final String ZK_SERVICE_NAME = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);

        // 简单遍历找到server.开头的配置并修改
        for (ServiceConfig config : list) {
            String name = config.getName();
            if (name != null && name.startsWith("server.")) {
                try {
                    // 提取server ID
                    int serverId = Integer.parseInt(name.substring(7));
                    // 计算Pod索引 (serverId-1)
                    int podIndex = serverId - 1;

                    // 修改为 FQDN DNS 名称
                    String newValue = ZK_SERVICE_NAME + "-" + podIndex + "." + ZK_SERVICE_NAME + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":2888:3888";
                    config.setValue(newValue);

                    logger.info("更新ZK配置: {} = {}", name, newValue);
                } catch (Exception e) {
                    // 忽略错误
                }
            }
        }
    }
}
