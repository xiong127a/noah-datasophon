package com.datasophon.common.model.tenant.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TenantYarnResource extends TenantFrameResource {

    /**
     * 父队列名称
     */
    private String parentQueueName;

    /**
     * 队列名称
     */
    private String queueName;

    /**
     * 队列容量占比
     */
    private String capacityPercent;

    /**
     * 标签
     */
    private String nodeLabel;

}
