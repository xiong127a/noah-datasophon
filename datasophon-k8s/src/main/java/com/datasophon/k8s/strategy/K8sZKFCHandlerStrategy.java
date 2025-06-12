package com.datasophon.k8s.strategy;

import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sZKFCHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {
        private static final Logger logger = LoggerFactory.getLogger(K8sZKFCHandlerStrategy.class);

        public K8sZKFCHandlerStrategy(String serviceName, String serviceRoleName) {
                super(serviceName, serviceRoleName);
        }

        @Override
        public ExecResult handler(K8sServiceRoleOperateCommand command) {
                K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(),
                                command.getServiceRoleName());
                return serviceHandler.start(command);
        }
}
