package com.datasophon.common.model.tenant.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TenantHiveResource extends TenantFrameResource {
    /**
     * hive数据库名称
     */
    private String hiveDatabase;

    /**
     * hive数据库容量
     */
    private String hiveDatabaseCapacity;

    private String hiveMetastoreDir;

}
