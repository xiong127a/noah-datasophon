package com.datasophon.api.master;

import akka.actor.UntypedActor;
import com.datasophon.api.utils.ranger.client.RangerClient;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.model.Role;
import com.datasophon.api.utils.ranger.strategy.AbstractRangerStrategy;
import com.datasophon.api.utils.ranger.strategy.RangerStrategyFactory;
import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterTenant;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.datasophon.api.utils.ranger.client.RangerUtil.getRangerClient;

@Slf4j
public class TenantRangerActor extends UntypedActor {

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
        } else if (message instanceof ClusterTenant) {
            // 创建租户对应组件策略
            ClusterTenant clusterTenant = (ClusterTenant) message;
            ExecResult execResult = createRangerPolicy(clusterTenant);
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
            log.error("add ranger role user failed");
            log.error(e.getMessage());
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

    private ExecResult createRangerPolicy(ClusterTenant clusterTenant) throws Exception {
        RangerClient rangerClient = getRangerClient(clusterTenant.getClusterId());
        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);

        Role role = new Role();
        role.setName(clusterTenant.getTenantName());
        rangerClient.getRoles().createRole(role);
        log.info("create ranger role {}", clusterTenant.getTenantName());

        // 操作组件策略
        List<String> serviceList = Arrays.asList("HDFS", "HIVE", "HBASE", "YARN");
        for (String serviceName : serviceList) {
            AbstractRangerStrategy rangerStrategy = RangerStrategyFactory.createRangerStrategy(serviceName, clusterTenant.getClusterId());
            execResult = rangerStrategy.operatePolicy(clusterTenant);
            if (!execResult.getExecResult()) {
                return execResult;
            }
        }

        rangerClient.stop();
        return execResult;
    }

}
