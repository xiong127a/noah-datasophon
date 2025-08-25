package com.datasophon.dao.entity;

import com.datasophon.dao.entity.base.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 自动伸缩任务实体类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("auto_scale_task")
public class AutoScaleTaskEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;
    

    
    private String taskName;
    
    private Long clusterId;
    
    private Long serviceId;
    
    private String serviceName;
    
    private String scaleType;
    
    private String scalePolicy;
    
    private Integer minReplicas;
    
    private Integer maxReplicas;
    
    private String cronExpression;
    
    private Boolean enabled;
    
    private String description;
    

}