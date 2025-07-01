package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.strategy.K8sServiceRoleStrategy;
import com.datasophon.k8s.strategy.K8sServiceRoleStrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class K8sStartServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sStartServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof K8sServiceRoleOperateCommand) {
            K8sServiceRoleOperateCommand command = (K8sServiceRoleOperateCommand) msg;
            logger.info("start to start service role {} on k8s", command.getServiceRoleName());
            ExecResult startResult = new ExecResult();
            K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(),
                    command.getServiceRoleName());

            K8sServiceRoleStrategy serviceRoleHandler = K8sServiceRoleStrategyContext
                    .getServiceRoleHandler(command.getServiceRoleName());
            if (Objects.nonNull(serviceRoleHandler)) {
                // 设置当前角色的循环次数缓存
                String roleName = command.getServiceRoleName();
                String parentName = command.getServiceName();
                String cacheKey = String.format("ROLE_LOOP_INDEX_%s_%s", roleName, parentName);

                // 从缓存获取当前循环索引，不存在则初始化为0
                Integer currentLoopIndex = (Integer) com.datasophon.common.cache.CacheUtils.get(cacheKey);
                if (currentLoopIndex == null) {
                    currentLoopIndex = 0;
                }

                // 增加循环索引并更新缓存
                currentLoopIndex++;
                com.datasophon.common.cache.CacheUtils.put(cacheKey, currentLoopIndex);
                logger.info("设置角色 [{}_{}}] 的当前循环索引缓存: {}", parentName, roleName, currentLoopIndex);

                startResult = serviceRoleHandler.handler(command);
            } else {
                startResult = serviceHandler.start(command);
            }

            getSender().tell(startResult, getSelf());
            logger.info("service role {} start on k8s result {}", command.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }

    }
}
