package com.datasophon.api.master;

import akka.actor.UntypedActor;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ranger.client.RangerClient;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.config.RangerAuthConfig;
import com.datasophon.api.utils.ranger.client.config.RangerClientConfig;
import com.datasophon.api.utils.ranger.client.model.Role;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterTenant;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

@Slf4j
public class TenantRangerActor extends UntypedActor {

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof TenantRangerCommand) {
            TenantRangerCommand rangerCommand = (TenantRangerCommand) message;
            if ("createService".equals(rangerCommand.getOperateType())) {
                createRangerService(rangerCommand.getClusterId(), rangerCommand.getServiceName());
            } else if ("addUser".equals(rangerCommand.getOperateType())) {
                getSender().tell(addRoleUser(rangerCommand), getSelf());
            }
        } else if (message instanceof ClusterTenant) {
            ClusterTenant clusterTenant = (ClusterTenant) message;
            createRangerPolicy(clusterTenant);
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

    private void createRangerService(Integer clusterId, String serviceName) throws Exception {
        RangerClient rangerClient = getRangerClient(clusterId);

        RangerUtil.createSuperRole(rangerClient);

        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        if ("HDFS".equals(serviceName)) {
            String nn1Add = "hdfs://" + globalVariables.get("${dfs.namenode.rpc-address.nameservice1.nn1}");
            String nn2Add = "hdfs://" + globalVariables.get("${dfs.namenode.rpc-address.nameservice1.nn2}");
            try {
                rangerClient.getServices()
                        .createService(RangerUtil.simpleHdfsService("hadoopdev", String.join(",", nn1Add, nn2Add)));
                RangerUtil.updateDefaultPolicy(rangerClient, "hadoopdev");
                log.info("config hdfs ranger plugin success");
            } catch (RangerClientException e) {
                log.error("config hdfs ranger plugin failed");
                log.error(e.getMessage());
            }
        }

        if ("YARN".equals(serviceName)) {
            String rm1Addr = "http://" + globalVariables.get("${yarn.resourcemanager.webapp.address.rm1}");
            String rm2Addr = "http://" + globalVariables.get("${yarn.resourcemanager.webapp.address.rm2}");

            try {
                rangerClient.getServices()
                        .createService(RangerUtil.simpleYarnService("yarndev", String.join(",", rm1Addr, rm2Addr)));
                RangerUtil.updateDefaultPolicy(rangerClient, "yarndev");
                log.info("config yarn ranger plugin success");
            } catch (RangerClientException e) {
                log.error("config yarn ranger plugin failed");
                log.error(e.getMessage());
            }
        }

        if ("HIVE".equals(serviceName)) {
            String hiveServer2Host = globalVariables.get("${hive.server2.thrift.bind.host}");
            String hiveServer2Port = globalVariables.get("${hive.server2.thrift.port}");
            String hiveUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;

            try {
                rangerClient.getServices()
                        .createService(RangerUtil.simpleHiveService("hivedev", hiveUrl));
                RangerUtil.updateDefaultPolicy(rangerClient, "hivedev");
                log.info("config hive ranger plugin success");
            } catch (RangerClientException e) {
                log.error("config hive ranger plugin failed");
                log.error(e.getMessage());
            }
        }

        if ("HBASE".equals(serviceName)) {
            String zkUrl = globalVariables.get("${zkUrls}");
            String zkPort = globalVariables.get("${clientPort}");
            String hbaseRootDir = globalVariables.get("${hbase.rootdir}");

            try {
                rangerClient.getServices()
                        .createService(RangerUtil.simpleHbaseService("hbasedev", zkUrl, zkPort, hbaseRootDir));
                RangerUtil.updateDefaultPolicy(rangerClient, "hbasedev");
                log.info("config hbase ranger plugin success");
            } catch (RangerClientException e) {
                log.error("config hbase ranger plugin failed");
                log.error(e.getMessage());
            }
        }

        rangerClient.stop();
    }

    private void createRangerPolicy(ClusterTenant clusterTenant) throws Exception {
        RangerClient rangerClient = getRangerClient(clusterTenant.getClusterId());

        Role role = new Role();
        role.setName(clusterTenant.getTenantName());
        rangerClient.getRoles().createRole(role);
        log.info("create ranger role {}", clusterTenant.getTenantName());

        if (StrUtil.isNotBlank(clusterTenant.getHdfsPath())) {
            rangerClient.getPolicies().createPolicy(
                    RangerUtil.simpleHdfsPolicy(
                            "hadoopdev",
                            clusterTenant.getTenantName(),
                            Collections.singletonList(clusterTenant.getHdfsPath()),
                            Collections.singletonList(clusterTenant.getTenantName()))
            );
            log.info("create hdfs policy success");
        }

        if (StrUtil.isNotBlank(clusterTenant.getHbaseNamespace())) {
            rangerClient.getPolicies().createPolicy(
                    RangerUtil.simpleHbasePolicy(
                            "hbasedev",
                            clusterTenant.getTenantName(),
                            Collections.singletonList(clusterTenant.getHbaseNamespace() + ":*"),
                            Collections.singletonList(clusterTenant.getTenantName())
                    )
            );
            log.info("create hbase policy success");
        }

        if (StrUtil.isNotBlank(clusterTenant.getHiveDatabase())) {
            rangerClient.getPolicies().createPolicy(
                    RangerUtil.simpleHivePolicyForDatabase(
                            "hivedev",
                            clusterTenant.getTenantName(),
                            Collections.singletonList(clusterTenant.getHiveDatabase()),
                            Collections.singletonList(clusterTenant.getTenantName())
                    )
            );
            log.info("create hive policy success");
        }

        if (StrUtil.isNotBlank(clusterTenant.getYarnMemory())) {
            rangerClient.getPolicies().createPolicy(
                    RangerUtil.simpleYarnPolicy(
                            "yarndev",
                            clusterTenant.getTenantName(),
                            Collections.singletonList(clusterTenant.getTenantName()),
                            Collections.singletonList(clusterTenant.getTenantName())
                    )
            );
            log.info("create yarn policy success");
        }

        rangerClient.stop();
    }

    private static RangerClient getRangerClient(Integer clusterTenant) throws Exception {
        Map<String, String> globalVariables = GlobalVariables.get(clusterTenant);
        String rangerAdminUrl = globalVariables.get("${rangerAdminUrl}");
        RangerClientConfig clientConfig = RangerClientConfig.builder()
                .connectTimeoutMillis(1000)
                .readTimeoutMillis(1000)
                .logLevel(feign.Logger.Level.BASIC)
                .authConfig(RangerAuthConfig.builder()
                        .username("admin")
                        .password("admin123")
                        .build())
                .url(rangerAdminUrl)
                .build();
        RangerClient rangerClient = new RangerClient(clientConfig);
        rangerClient.start();
        return rangerClient;
    }

}
