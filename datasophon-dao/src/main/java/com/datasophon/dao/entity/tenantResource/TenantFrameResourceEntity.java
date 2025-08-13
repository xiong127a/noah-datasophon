package com.datasophon.dao.entity.tenantResource;

import lombok.Data;

import java.io.Serializable;

@Data
public class TenantFrameResourceEntity implements Serializable {
    private Long clusterId;
    private String serviceName;
    private String type;
}
