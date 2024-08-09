package com.datasophon.k8s.util;

import com.datasophon.common.Constants;

public class CommonUtil {

    public static String generateServiceRoleFullName(String serviceName, String serviceRoleName) {
        return String.format("%s-%s", serviceName.toLowerCase(), serviceRoleName.toLowerCase());
    }

    public static String k8sYamlFilePath(String serviceRoleFullName) {
        return Constants.INSTALL_PATH + Constants.SLASH + "k8sDep" + Constants.SLASH + serviceRoleFullName + ".yaml";
    }

}