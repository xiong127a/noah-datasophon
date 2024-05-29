package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@TableName("t_ddh_cluster_user_tenant")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClusterUserTenant implements Serializable {

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

    private Integer userId;

    private Integer tenantId;

    @TableField(exist = false)
    private String tenantName;

}
