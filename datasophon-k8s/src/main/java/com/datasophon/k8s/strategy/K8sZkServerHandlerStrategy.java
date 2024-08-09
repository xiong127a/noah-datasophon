package com.datasophon.k8s.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sKerberosUtils;

public class K8sZkServerHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sZkServerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName,serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        if (command.getEnableKerberos()) {
            logger.info("start to get zkserver keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            K8sKerberosUtils.createKeytabDir();
            if (!FileUtil.exist("/etc/security/keytab/zkserver.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster("zookeeper/" + hostname, "zkserver.service.keytab");
            }
            if (!FileUtil.exist("/etc/security/keytab/zkclient.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster("zkcli/" + hostname, "zkclient.service.keytab");
            }
            startResult = serviceHandler.start(command.getKubeConfig());
        } else {
            startResult = serviceHandler.start(command.getKubeConfig());
        }
        return startResult;
    }

}
