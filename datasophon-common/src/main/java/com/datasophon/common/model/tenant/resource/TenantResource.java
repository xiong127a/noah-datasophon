package com.datasophon.common.model.tenant.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantResource implements Serializable {

    @Serial
    private static final long serialVersionUID = 8665156195475027337L;

    /**
     * 主键
     */
    private Integer id;

    /**
     * 集群id
     */
    private Long clusterId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 租户对应操作，ADD UPDATE DELETE
     */
    private String operateType;

    /**
     * hdfs资源列表
     */
    private List<TenantHdfsResource> hdfsResourceList;

    /**
     * yarn资源列表
     */
    private List<TenantYarnResource> yarnResourceList;

    /**
     * hive资源列表
     */
    private List<TenantHiveResource> hiveResourceList;

    /**
     * hbase资源列表
     */
    private List<TenantHbaseResource> hbaseResourceList;

    /**
     * kafka资源列表
     */
    private List<TenantKafkaResource> kafkaResourceList;
    /**
     * kms资源列表
     */
    private List<TenantKmsResource> kmsResourceList;
}