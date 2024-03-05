package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.datasophon.dao.entity.tenantResource.*;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName(value = "t_ddh_cluster_tenant", autoResultMap = true)
public class ClusterTenant implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
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
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<TenantFrameResource> hdfsResourceList;

    /**
     * yarn资源列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<TenantFrameResource> yarnResourceList;

    /**
     * hive资源列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<TenantFrameResource> hiveResourceList;

    /**
     * hbase资源列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<TenantFrameResource> hbaseResourceList;

    /**
     * kafka资源列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<TenantFrameResource> kafkaResourceList;
}

