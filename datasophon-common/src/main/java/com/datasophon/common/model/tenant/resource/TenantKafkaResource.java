package com.datasophon.common.model.tenant.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TenantKafkaResource extends TenantFrameResource {
    /**
     * kafka topic名称
     */
    private String kafkaTopicName;

    /**
     * kafka topic容量
     */
    private String kafkaTopicCapacity;

    /**
     * topic副本数
     */
    private String kafkaReplicas;

    /**
     * kafka zk地址
     */
    private String kafkaZkAddr;

}
