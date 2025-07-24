package com.datasophon.api.master;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
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

public class TenantRangerActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(TenantRangerActor.class);

    private static final List<String> SUPPORT_SERVICE = Arrays.asList("HDFS", "HIVE", "HBASE", "YARN");

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(TenantRangerCommand.class, this::handleTenantRangerCommand)
                .match(TenantResource.class, this::handleTenantResource)
                .matchAny(this::unhandled)
                .build();
    }

    private void handleTenantRangerCommand(TenantRangerCommand rangerCommand) {
        try {
            ExecResult execResult;
            switch (rangerCommand.getOperateType()) {
                case CREATE_SERVICE:
                    execResult = createRangerService(rangerCommand.getClusterId(), rangerCommand.getServiceName());
                    getSender().tell(execResult, getSelf());
                    break;
                case OP_USER_TO_ROLE:
                    getSender().tell(addRoleUser(rangerCommand), getSelf());
                    break;
                case DELETE_TENANT:
                    execResult = deleteRangerPolicy(rangerCommand.getTenantName(), rangerCommand.getClusterId());
                    deleteRangerRole(rangerCommand.getTenantName(), rangerCommand.getClusterId());
                    getSender().tell(execResult, getSelf());
                    break;
                default:
                    unhandled(rangerCommand);
            }
        } catch (Exception e) {
            logger.error("Error handling TenantRangerCommand", e);
            ExecResult errorResult = new ExecResult();
            errorResult.setExecResult(false);
            errorResult.setExecErrOut(e.getMessage());
            getSender().tell(errorResult, getSelf());
        }
    }

    private void handleTenantResource(TenantResource resource) {
        try {
            ExecResult execResult = operateRangerPolicy(resource);
            getSender().tell(execResult, getSelf());
        } catch (Exception e) {
            logger.error("Error handling TenantResource", e);
            ExecResult errorResult = new ExecResult();
            errorResult.setExecResult(false);
            errorResult.setExecErrOut(e.getMessage());
            getSender().tell(errorResult, getSelf());
        }
    }

    private ExecResult addRoleUser(TenantRangerCommand rangerCommand) {
        ExecResult execResult = new ExecResult();
        RangerClient rangerClient;
        try {
            rangerClient = getRangerClient(rangerCommand.getClusterId());
            RangerUtil.setRoleUser(rangerClient, rangerCommand.getRoleName(), rangerCommand.getUserList());
            execResult.setExecResult(true);
            return execResult;
        } catch (Exception e) {
            logger.error("add ranger role user failed");
            logger.error(e.getMessage());
            return execResult;
        }
    }

    private ExecResult createRangerService(Integer clusterId, String serviceName) throws Exception {
        RangerClient rangerClient = getRangerClient(clusterId);
        RangerUtil.createSuperRole(rangerClient);
        AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy(serviceName, clusterId);
        return rangerStrategy.createService();
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
        for (String serviceName : SUPPORT_SERVICE) {
            AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy(serviceName,
                    resource.getClusterId());
            rangerStrategy.deletePolicy(resource.getTenantName());
            execResult = rangerStrategy.operatePolicy(resource);
            if (!execResult.getExecResult()) {
                logger.error("operateRangerPolicy for service {} failed", serviceName);
                logger.error(execResult.getExecErrOut());
            }
        }

        return execResult;
    }

    private ExecResult deleteRangerPolicy(String tenantName, Integer clusterId) throws Exception {
        RangerClient rangerClient = getRangerClient(clusterId);
        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);

        try {
            rangerClient.getRoles().deleteRoleByName(tenantName);
            logger.info("delete role {} success", tenantName);
        } catch (Exception e) {
            logger.error("delete role {} failed", tenantName);
        }

        for (String serviceName : SUPPORT_SERVICE) {
            AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy(serviceName, clusterId);
            execResult = rangerStrategy.deletePolicy(tenantName);
            if (!execResult.getExecResult()) {
                logger.error("delete ranger policy {} for service {} failed", tenantName, serviceName);
                logger.error(execResult.getExecErrOut());
            }
        }

        return execResult;
    }

    private void deleteRangerRole(String tenantName, Integer clusterId) {
        try {
            RangerClient rangerClient = getRangerClient(clusterId);
            rangerClient.getRoles().deleteRoleByName(tenantName);
            logger.info("remove ranger role user success");
        } catch (Exception e) {
            logger.error("remove ranger role user failed");
            logger.error(e.getMessage());
        }
    }
}
