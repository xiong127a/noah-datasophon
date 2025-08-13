package com.datasophon.dao.entity.tenantResource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TenantHbaseResourceEntity extends TenantFrameResourceEntity {
    /**
     * hbase 命名空间
     */
    private String hbaseNamespace;

    /**
     * hbase 容量
     */
    private String hbaseCapacity;

    /**
     * hbase regionserver数量
     */
    private String hbaseRegionServerNum;

}
