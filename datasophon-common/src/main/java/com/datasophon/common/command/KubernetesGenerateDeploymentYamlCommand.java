package com.datasophon.common.command;

import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleRunner;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class KubernetesGenerateDeploymentYamlCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = -4211566568993105684L;

    private Integer clusterId;

    private String namespace;

    private String serviceName;

    private String decompressPackageName;

    Map<Generators, List<ServiceConfig>> cofigFileMap;

    private String serviceRoleName;

    private RunAs runAs;

    private ServiceRoleRunner startRunner;

    private ServiceRoleRunner stopRunner;

    private ServiceRoleRunner statusRunner;

    private ServiceRoleRunner restartRunner;

    private String hostName;

    private Integer roleNodeCnt;

    private String logFile;

    private String masterHost;

    private Boolean enableKerberos;

    private Boolean enableRangerPlugin;

    private CommandType commandType;

    public KubernetesGenerateDeploymentYamlCommand() {
        this.enableKerberos = false;
        this.enableRangerPlugin = false;
    }
}
