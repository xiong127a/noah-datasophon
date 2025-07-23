package com.datasophon.dao.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Table("t_ddh_cluster_user_tenant")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClusterUserTenant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    private Integer id;

    /**
     * 集群id
     */
    private Integer clusterId;

    private Integer userId;

    private Integer tenantId;

    @Column(ignore = true)
    private String tenantName;

}
