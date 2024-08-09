package com.datasophon.common;

import com.datasophon.common.enums.K8sScaleType;
import lombok.Data;

import java.io.Serializable;

@Data
public class K8sServiceScaleCommand implements Serializable {

    private static final long serialVersionUID = -4211566568993105684L;

    private String kubeConfig;

    private String serviceName;

    private String serviceRoleName;

    private K8sScaleType scaleType;
}
