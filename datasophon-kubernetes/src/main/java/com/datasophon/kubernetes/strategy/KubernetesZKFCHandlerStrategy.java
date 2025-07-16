package com.datasophon.kubernetes.strategy;

import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesZKFCHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {
        private static final Logger logger = LoggerFactory.getLogger(KubernetesZKFCHandlerStrategy.class);

        public KubernetesZKFCHandlerStrategy(String serviceName, String serviceRoleName) {
                super(serviceName, serviceRoleName);
        }

        @Override
        public ExecResult handler(KubernetesServiceRoleOperateCommand command) {
                KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                                command.getServiceRoleName());
                return serviceHandler.start(command);
        }
}
