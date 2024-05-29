package com.datasophon.dao.entity.tenantResource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TenantHdfsResource extends TenantFrameResource {

    /**
     * hdfs路径
     */
    private String hdfsPath;

    /**
     * hdfs空间配额
     */
    private String hdfsSpaceQuota;

}
