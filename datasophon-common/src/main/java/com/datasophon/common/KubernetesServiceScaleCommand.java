package com.datasophon.common;

import com.datasophon.common.enums.CommandType;
import lombok.Data;

import java.io.Serializable;

@Data
public class KubernetesServiceScaleCommand implements Serializable {

    private static final long serialVersionUID = -4211566568993105684L;

    private String kubeConfig;

    private String serviceName;

    private String serviceRoleName;

    private CommandType commandType;

}
