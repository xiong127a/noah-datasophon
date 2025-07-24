package com.datasophon.common.command;

import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class KubernetesServiceRoleOperateCommand extends BaseCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 6454341380133032878L;
    private Integer serviceRoleInstanceId;

    private CommandType commandType;

    private String decompressPackageName;

    private boolean isSlave;

    private String masterHost;

    private String managerHost;

    private String graphHost;

    private Boolean enableRangerPlugin;

    private RunAs runAs;

    private Boolean enableKerberos;

    private String kubeConfig;

    private String hostname;

    private String nnHost;
    private String namespace;

    private Map<Generators, List<ServiceConfig>> configFileMap;

    public KubernetesServiceRoleOperateCommand() {
        this.enableRangerPlugin = false;
        this.enableKerberos = false;
    }
}
