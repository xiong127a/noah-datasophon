package com.datasophon.common.command;

import com.datasophon.common.enums.K8sHostTagOperation;
import lombok.Data;

import java.io.Serializable;

@Data
public class K8sGenerateHostTagCommand implements Serializable {

    private static final long serialVersionUID = -4211566568993105684L;

    private String kubeConfig;

    private String serviceName;

    private String serviceRoleName;

    private String hostName;

    private K8sHostTagOperation  tagOperation;
}
