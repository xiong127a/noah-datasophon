package com.datasophon.api.master;

import akka.actor.UntypedActor;
import com.datasophon.api.utils.ranger.client.RangerClient;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.model.Role;
import com.datasophon.api.utils.ranger.strategy.AbstractRangerStrategy;
import com.datasophon.api.utils.ranger.strategy.RangerStrategyFactory;
import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.model.TenantResource.TenantResource;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.datasophon.api.utils.ranger.client.RangerUtil.getRangerClient;

public class TenantRangerActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(TenantRangerActor.class);

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof TenantRangerCommand) {
            TenantRangerCommand rangerCommand = (TenantRangerCommand) message;
            if ("createService".equals(rangerCommand.getOperateType())) {
                ExecResult execResult = createRangerService(rangerCommand.getClusterId(), rangerCommand.getServiceName());
                getSender().tell(execResult, getSelf());
            } else if ("addUser".equals(rangerCommand.getOperateType())) {
                getSender().tell(addRoleUser(rangerCommand), getSelf());
            }
        } else if (message instanceof TenantResource) {
            // 创建租户对应组件策略
            TenantResource resource = (TenantResource) message;
            ExecResult execResult = operateRangerPolicy(resource);
            getSender().tell(execResult, getSelf());
        } else {
            unhandled(message);
        }
    }

    private ExecResult addRoleUser(TenantRangerCommand rangerCommand) throws Exception {
        ExecResult execResult = new ExecResult();
        RangerClient rangerClient = getRangerClient(rangerCommand.getClusterId());
        try {
            RangerUtil.setRoleUser(rangerClient, rangerCommand.getRoleName(), rangerCommand.getUserList());
            execResult.setExecResult(true);
            return execResult;
        } catch (Exception e) {
            logger.error("add ranger role user failed");
            logger.error(e.getMessage());
            return execResult;
        } finally {
            rangerClient.stop();
        }
    }

    private ExecResult createRangerService(Integer clusterId, String serviceName) throws Exception {
        RangerClient rangerClient = getRangerClient(clusterId);
        RangerUtil.createSuperRole(rangerClient);
        AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy(serviceName, clusterId);
        ExecResult execResult = rangerStrategy.createService();
        rangerClient.stop();
        return execResult;
    }

    private ExecResult operateRangerPolicy(TenantResource resource) throws Exception {
        RangerClient rangerClient = getRangerClient(resource.getClusterId());
        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);

        Role role = new Role();
        role.setName(resource.getTenantName());
        try {
            rangerClient.getRoles().createRole(role);
            logger.info("create ranger role {} success", resource.getTenantName());
        } catch (Exception e) {
            logger.error("create ranger role {} failed", resource.getTenantName());
            logger.error(e.getMessage());
        }

        // 操作组件策略
        List<String> serviceList = Arrays.asList("HDFS", "HIVE", "HBASE", "YARN");
        for (String serviceName : serviceList) {
            AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy(serviceName, resource.getClusterId());
            execResult = rangerStrategy.operatePolicy(resource);
            if (!execResult.getExecResult()) {
                logger.error("operateRangerPolicy for service {} failed", serviceName);
                logger.error(execResult.getExecErrOut());
            }
        }

        rangerClient.stop();
        return execResult;
    }

}
