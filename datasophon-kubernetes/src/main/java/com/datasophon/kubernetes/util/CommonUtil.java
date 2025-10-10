package com.datasophon.kubernetes.util;

import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.FileUtils;

public class CommonUtil {

    public static String generateServiceRoleFullName(String serviceName, String serviceRoleName) {
        return String.format("%s-%s", serviceName.toLowerCase(), serviceRoleName.toLowerCase());
    }

    public static String KubernetesYamlFilePath(String serviceRoleFullName) {
        return FileUtils.concatPath(StrUtil.blankToDefault(Constants.YAML_PATH,Constants.INSTALL_PATH), "kubernetesYaml" , serviceRoleFullName + ".yaml");
    }

}