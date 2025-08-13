package com.datasophon.common.model.tenant.resource;

import lombok.Data;

import java.io.Serializable;

@Data
public class TenantFrameResource implements Serializable {
    private Long clusterId;
    private String serviceName;
    private String type;
    private Boolean enableKerberos;
}
