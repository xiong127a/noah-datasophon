package com.datasophon.common.model.TenantResource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantResource implements Serializable {

    private static final long serialVersionUID = 8665156195475027337L;

    /**
     * 主键
     */
    private Integer id;

    /**
     * 集群id
     */
    private Integer clusterId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * hdfs资源列表
     */
    private List<TenantFrameResource> hdfsResourceList;

    /**
     * yarn资源列表
     */
    private List<TenantFrameResource> yarnResourceList;

    /**
     * hive资源列表
     */
    private List<TenantFrameResource> hiveResourceList;

    /**
     * hbase资源列表
     */
    private List<TenantFrameResource> hbaseResourceList;

    /**
     * kafka资源列表
     */
    private List<TenantFrameResource> kafkaResourceList;

}