package com.datasophon.common.model.TenantResource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TenantYarnResource extends TenantFrameResource {

    /**
     * 队列名称
     */
    private String yarnQueueName;

    /**
     * yarn内存
     */
    private String yarnMemory;

    /**
     * yarn cpu
     */
    private String yarnCpu;

}
