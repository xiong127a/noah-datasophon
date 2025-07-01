package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.K8sServiceScaleCommand;
import com.datasophon.common.command.K8sGenerateHostTagCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sScaleServiceHandler;
import com.datasophon.k8s.actor.handler.K8sTagHostHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sScaleServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sScaleServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof K8sServiceScaleCommand) {

            K8sServiceScaleCommand command = (K8sServiceScaleCommand) msg;
            logger.info("start scale service role {}", command.getServiceRoleName());
            K8sScaleServiceHandler serviceHandler = new K8sScaleServiceHandler(command.getServiceName(),
                    command.getServiceRoleName());

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

            ExecResult startResult = serviceHandler.scaleService(
                    command.getKubeConfig(),
                    command.getScaleType());
            getSender().tell(startResult, getSelf());

            logger.info("{} scale {}",
                    command.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }
    }
}
