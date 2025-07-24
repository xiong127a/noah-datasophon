package com.datasophon.common.model.tenant.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TenantKmsResource extends TenantFrameResource {

    /**
     * keyname
     */
    private String keyname;

    /**
     * hdfs空间配额
     */
    private String hdfsSpaceQuota;

}
