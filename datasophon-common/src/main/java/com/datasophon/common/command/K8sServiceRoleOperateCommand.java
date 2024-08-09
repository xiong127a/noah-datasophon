package com.datasophon.common.command;

import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.RunAs;
import lombok.Data;

import java.io.Serializable;

@Data
public class K8sServiceRoleOperateCommand extends BaseCommand implements Serializable {

    private static final long serialVersionUID = 6454341380133032878L;
    private Integer serviceRoleInstanceId;

    private CommandType commandType;

    private String decompressPackageName;

    private boolean isSlave;

    private String masterHost;

    private String managerHost;

    private Boolean enableRangerPlugin;

    private RunAs runAs;

    private Boolean enableKerberos;

    private String kubeConfig;

    private String hostname;

    public K8sServiceRoleOperateCommand() {
        this.enableRangerPlugin = false;
        this.enableKerberos = false;
    }
}
