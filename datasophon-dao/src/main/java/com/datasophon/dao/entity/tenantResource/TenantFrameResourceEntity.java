package com.datasophon.dao.entity.tenantResource;

import com.datasophon.dao.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serial;

/**
 * 租户框架资源实体类
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
public class TenantFrameResourceEntity extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    private Long clusterId;
    private String serviceName;
    private String type;
}
