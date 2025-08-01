package com.datasophon.dao.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.util.Date;

/**
 * 自动伸缩任务实体
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Data
@Table("auto_scale_task")
public class AutoScaleTaskEntity {
    
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    private String taskName;
    
    private Integer clusterId;
    
    private Integer serviceId;
    
    private String serviceName;
    
    private String scaleType;
    
    private String scalePolicy;
    
    private Integer minReplicas;
    
    private Integer maxReplicas;
    
    private String cronExpression;
    
    private Boolean enabled;
    
    private String description;
    
    private Date createdAt;
    
    private Date updatedAt;
}