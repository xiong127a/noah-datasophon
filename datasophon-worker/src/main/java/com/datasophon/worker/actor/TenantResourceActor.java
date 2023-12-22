package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.model.KafkaTopicsConfig;
import com.datasophon.common.model.TenantResource;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TenantResourceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(TenantResourceActor.class);

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof TenantResource) {
            TenantResource msg = (TenantResource) message;
            ExecResult execResult = new ExecResult();

            if (StrUtil.isNotBlank(msg.getHdfsPath())) {
                execResult = operateTenantHdfsResource(msg);
            }

            if (StrUtil.isNotBlank(msg.getKafkaTopicsConfig())) {
                execResult = operateTenantKafkaResource(msg);
            }

            if (StrUtil.isNotBlank(msg.getHbaseNamespace())) {
                execResult = operateTenantHbaseResource(msg);
            }

            if (StrUtil.isNotBlank(msg.getHiveDatabase())) {
                execResult = operateTenantHiveResource(msg);
            }

            getSender().tell(execResult, getSelf());

        } else {
            unhandled(message);
        }
    }

    /**
     * 创建租户hdfs目录及设置限额
     */
    private ExecResult operateTenantHdfsResource(TenantResource tenantResource) throws Exception {
        ExecResult execResult = null;
        if (Objects.isNull(tenantResource.getId())) {
            List<String> commands = new ArrayList<>();
            commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
            commands.add("dfs");
            commands.add("-mkdir");
            commands.add("-p");
            commands.add(tenantResource.getHdfsPath());

            execResult = ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 180L, logger);
            if (execResult.getExecResult()) {
                logger.info("hdfs create dir {} success", tenantResource.getHdfsPath());
                execResult.setExecResult(true);
            } else {
                logger.error("hdfs create dir {} failed", tenantResource.getHdfsPath());
                logger.error(execResult.getExecErrOut());
                return execResult;
            }
        }

        execResult = setHdfsQuota(convertGBToByte(tenantResource.getHdfsSpaceQuota()), tenantResource.getHdfsPath());
        if (execResult.getExecResult()) {
            logger.info("hdfs set dir {} quota success", tenantResource.getHdfsPath());
        } else {
            logger.error("hdfs set dir {} quota failed", tenantResource.getHdfsPath());
            logger.error(execResult.getExecErrOut());
            execResult.setExecResult(false);
            return execResult;
        }

        return execResult;
    }

    /**
     * 设置hdfs文件夹容量限额
     */
    private ExecResult setHdfsQuota(String size, String hdfsPath) {
        // /opt/datasophon/hadoop-3.3.3/bin/hdfs dfsadmin -setSpaceQuota 1024 /tenant/t1
        List<String> commands = new ArrayList<>();
        commands.add("sudo");
        commands.add("-u");
        commands.add("hdfs");
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfsadmin");
        commands.add("-setSpaceQuota");
        commands.add(size);
        commands.add(hdfsPath);
        return ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 180L, logger);
    }

    /**
     * 创建hive数据库及设置限额
     */
    private ExecResult operateTenantHiveResource(TenantResource tenantResource) throws Exception {
        ExecResult execResult = null;
        String dbPathDir = tenantResource.getHiveMetastoreDir() + "/" + tenantResource.getHiveDatabase();

        if (Objects.isNull(tenantResource.getId())) {
            // /opt/datasophon/hive/bin/hive -e "CREATE DATABASE IF NOT EXISTS t1 LOCATION '/user/hive/warehouse/t1'"
            execResult = ShellUtils.exceShell(Constants.INSTALL_PATH + "/hive/bin/hive -e \"CREATE DATABASE IF NOT EXISTS "
                    + tenantResource.getHiveDatabase() + " LOCATION '" + dbPathDir + "'\"");
            if (execResult.getExecResult()) {
                logger.info("create hive database {} success", tenantResource.getHiveDatabase());
                execResult.setExecResult(true);
            } else {
                logger.error("create hive database {} failed", tenantResource.getHiveDatabase());
                logger.error(execResult.getExecErrOut());
                return execResult;
            }
        }

        execResult = setHdfsQuota(convertGBToByte(tenantResource.getHiveDatabaseCapacity()), dbPathDir);
        if (execResult.getExecResult()) {
            logger.info("hdfs set dir {} quota success", dbPathDir);
        } else {
            logger.error("hdfs set dir {} quota failed", dbPathDir);
            execResult.setExecResult(false);
        }

        return execResult;
    }

    /**
     * 创建kafka topic及设置限额
     */
    private ExecResult operateTenantKafkaResource(TenantResource tenantResource) throws Exception {
        List<String> commands = new ArrayList<>();
        ExecResult execResult = null;
        List<KafkaTopicsConfig> configs = JSONUtil.toList(JSONUtil.parseArray(tenantResource.getKafkaTopicsConfig()), KafkaTopicsConfig.class);

        if (Objects.isNull(tenantResource.getId())) {
            for (KafkaTopicsConfig topicConfig : configs) {
                //  kafka-topics.sh --create --topic mytopi1c --bootstrap-server localhost:9092 --partitions 3 --replication-factor 2 --config max.message.bytes=64000
                commands.add(Constants.INSTALL_PATH + "/kafka/bin/kafka-topics.sh");
                commands.add("--create");
                commands.add("--topic");
                commands.add(topicConfig.getTopic());
                commands.add("--bootstrap-server");
                commands.add("localhost:9092");
                commands.add("--replication-factor");
                commands.add(topicConfig.getReplicas());
                commands.add("--config");
                commands.add("retention.bytes=" + convertGBToByte(topicConfig.getCapacity()));

                execResult = ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 180L, logger);
                if (execResult.getExecResult()) {
                    logger.info("create kafka topic {} success", topicConfig.getTopic());
                } else {
                    logger.error("create kafka topic {} failed", topicConfig.getTopic());
                }
            }
        } else {
            for (KafkaTopicsConfig topicConfig : configs) {
                // /opt/datasophon/kafka/bin/kafka-configs.sh --zookeeper hadoop2:2181,hadoop3:2181,hadoop1:2181/kafka --entity-type topics --entity-name t1 --alter --add-config retention.bytes=128000
                String shell =
                        Constants.INSTALL_PATH + "/kafka/bin/kafka-configs.sh " +
                                "--zookeeper " +
                                tenantResource.getKafkaZkAddr() +
                                " --entity-type topics --entity-name " +
                                topicConfig.getTopic() +
                                "  --alter --add-config retention.bytes=" +
                                convertGBToByte(topicConfig.getCapacity());
                execResult = ShellUtils.exceShell(shell);
                if (execResult.getExecResult()) {
                    logger.info("alter kafka topic {} success", topicConfig.getTopic());
                } else {
                    logger.error("alter kafka topic {} failed", topicConfig.getTopic());
                }
            }
        }

        return execResult;
    }

    /**
     * 创建hbase命名空间及设置限额
     */
    private ExecResult operateTenantHbaseResource(TenantResource tenantResource) throws Exception {
        ExecResult execResult;

        if (Objects.isNull(tenantResource)) {
            // echo "create_namespace 'test4'; set_quota TYPE => SPACE, NAMESPACE => 'test4', LIMIT => '1G', POLICY => NO_INSERTS; " | hbase shell
            execResult = ShellUtils.exceShell(
                    "echo \"create_namespace '" + tenantResource.getHbaseNamespace() + "'; "
                            + "set_quota TYPE => SPACE, NAMESPACE => '" + tenantResource.getHbaseNamespace() + "', "
                            + "LIMIT => '" + tenantResource.getHbaseCapacity() + "G', POLICY => NO_INSERTS;\" | " + Constants.INSTALL_PATH + "/hbase-2.4.16/bin/hbase shell");

            if (execResult.getExecResult()) {
                logger.info("create hbase namespace {} success", tenantResource.getHbaseNamespace());
            } else {
                logger.error("create hbase namespace {} failed", tenantResource.getHbaseNamespace());
                logger.error(execResult.getExecErrOut());
            }
        } else {
            // echo "alter_namespace 'test4'; set_quota TYPE => SPACE, NAMESPACE => 'test4', LIMIT => '1G', POLICY => NO_INSERTS; " | hbase shell
            execResult = ShellUtils.exceShell(
                    "echo \"alter_namespace '" + tenantResource.getHbaseNamespace() + "'; "
                            + "set_quota TYPE => SPACE, NAMESPACE => '" + tenantResource.getHbaseNamespace() + "', "
                            + "LIMIT => '" + tenantResource.getHbaseCapacity() + "G', POLICY => NO_INSERTS;\" | " + Constants.INSTALL_PATH + "/hbase-2.4.16/bin/hbase shell");

            if (execResult.getExecResult()) {
                logger.info("alter hbase namespace {} quota success", tenantResource.getHbaseNamespace());
            } else {
                logger.error("alter hbase namespace {} quota failed", tenantResource.getHbaseNamespace());
                logger.error(execResult.getExecErrOut());
            }
        }

        return execResult;
    }

    private String convertGBToByte(String size) {
        return Convert.toStr(Long.parseLong(size) * 1024L * 1024L * 1024L);
    }

}
