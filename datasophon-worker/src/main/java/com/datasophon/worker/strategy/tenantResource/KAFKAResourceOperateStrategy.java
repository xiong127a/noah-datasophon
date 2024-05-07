package com.datasophon.worker.strategy.tenantResource;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.TenantResource.TenantFrameResource;
import com.datasophon.common.model.TenantResource.TenantHbaseResource;
import com.datasophon.common.model.TenantResource.TenantKafkaResource;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class KAFKAResourceOperateStrategy extends AbstractOperateStrategy implements ResourceOperateStrategy {

    private final TenantKafkaResource kafkaResource;

    public KAFKAResourceOperateStrategy(TenantFrameResource tenantResource) {
        super(tenantResource);
        this.kafkaResource = (TenantKafkaResource) tenantResource;
    }

    @Override
    public ExecResult addSource() {
        execResult = createKafkaTopic(kafkaResource.getKafkaTopicName(), kafkaResource.getKafkaReplicas(), kafkaResource.getKafkaTopicCapacity());
        if (execResult.getExecResult()) {
            logger.info("create kafka topic {} success", kafkaResource.getKafkaTopicName());
        } else {
            logger.error("create kafka topic {} failed", kafkaResource.getKafkaTopicName());
            logger.error(execResult.getExecOut());
        }
        return execResult;
    }

    @Override
    public ExecResult updateSource() {
        execResult = alertKafkaTopic(kafkaResource.getKafkaZkAddr(), kafkaResource.getKafkaTopicName(), kafkaResource.getKafkaTopicCapacity());
        if (execResult.getExecResult()) {
            logger.info("alter kafka topic {} success", kafkaResource.getKafkaTopicName());
        } else {
            logger.error("alter kafka topic {} failed", kafkaResource.getKafkaTopicName());
            logger.error(execResult.getExecOut());
        }
        return execResult;
    }

    @Override
    public ExecResult deleteSource() {
        execResult = deleteKafkaTopic(kafkaResource.getKafkaTopicName());
        if (execResult.getExecResult()) {
            logger.info("delete kafka topic {} success", kafkaResource.getKafkaTopicName());
        } else {
            logger.error("delete kafka topic {} failed", kafkaResource.getKafkaTopicName());
            logger.error(execResult.getExecOut());
        }
        return execResult;
    }

    private ExecResult alertKafkaTopic(String kafkaZkAddr, String topicName, String topicCapacity) {
        // /opt/datasophon/kafka/bin/kafka-configs.sh --zookeeper hadoop2:2181,hadoop3:2181,hadoop1:2181/kafka
        // --entity-type topics --entity-name t1 --alter --add-config retention.bytes=128000
        String shell =
                kinitKafkaStr(kafkaResource) +
                        ";" +
                        Constants.INSTALL_PATH + "/kafka/bin/kafka-configs.sh " +
                        "--zookeeper " +
                        kafkaZkAddr +
                        " --entity-type topics --entity-name " +
                        topicName +
                        "  --alter --add-config retention.bytes=" +
                        convertGBToByte(topicCapacity);
        return ShellUtils.exceShell(shell);
    }

    private ExecResult createKafkaTopic(String topicName, String topicReplicas, String topicCapacity) {
        //  kafka-topics.sh --create --topic mytopi1c --bootstrap-server localhost:9092
        //  --partitions 3 --replication-factor 2 --config max.message.bytes=64000
        StringJoiner commands = new StringJoiner(" ");
        if (kafkaResource.getEnableKerberos()) {
            commands.add(kinitKbStr("kafka"));
            commands.add(";");
        }
        commands.add(Constants.INSTALL_PATH + "/kafka/bin/kafka-topics.sh");
        commands.add("--create");
        commands.add("--topic");
        commands.add(topicName);
        commands.add("--bootstrap-server");
        commands.add(CacheUtils.get(Constants.HOSTNAME) + ":9092");
        commands.add("--replication-factor");
        commands.add(topicReplicas);
        commands.add("--config");
        commands.add("retention.bytes=" + convertGBToByte(topicCapacity));

        return ShellUtils.exceShell(commands.toString());
    }

    private ExecResult deleteKafkaTopic(String topicName) {
        // kafka-topics.sh --delete --topic mytopic --bootstrap-server localhost:9092
        StringJoiner commands = new StringJoiner(" ");
        if (kafkaResource.getEnableKerberos()) {
            commands.add(kinitKbStr("kafka"));
            commands.add(";");
        }
        commands.add(Constants.INSTALL_PATH + "/kafka/bin/kafka-topics.sh");
        commands.add("--delete");
        commands.add("--topic");
        commands.add(topicName);
        commands.add("--bootstrap-server");
        commands.add(CacheUtils.get(Constants.HOSTNAME) + ":9092");

        return ShellUtils.exceShell(commands.toString());
    }

    private String kinitKafkaStr(TenantKafkaResource kafkaResource) {
        String kbString = "";
        if (kafkaResource.getEnableKerberos()) kbString = kinitKbStr("kafka");
        return kbString;
    }

}
