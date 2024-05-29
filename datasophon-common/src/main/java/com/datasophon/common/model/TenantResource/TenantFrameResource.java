package com.datasophon.common.model.TenantResource;

import lombok.Data;

import java.io.Serializable;

@Data
public class TenantFrameResource implements Serializable {
    private Integer clusterId;
    private String serviceName;
    private String type;
    private Boolean enableKerberos;
}
