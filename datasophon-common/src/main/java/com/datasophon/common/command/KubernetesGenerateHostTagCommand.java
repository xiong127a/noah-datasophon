package com.datasophon.common.command;

import com.datasophon.common.enums.CommandType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class KubernetesGenerateHostTagCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = -4211566568993105684L;

    private String kubeConfig;

    private String serviceName;

    private String serviceRoleName;

    private String hostName;
    private Integer clusterId;
    private String namespace;

    private CommandType commandType;
}
