package com.datasophon.k8s.util;

import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.FileUtils;

public class CommonUtil {

    public static String generateServiceRoleFullName(String serviceName, String serviceRoleName) {
        return String.format("%s-%s", serviceName.toLowerCase(), serviceRoleName.toLowerCase());
    }

    public static String k8sYamlFilePath(String serviceRoleFullName) {
        return FileUtils.concatPath(StrUtil.blankToDefault(Constants.YAML_PATH,Constants.INSTALL_PATH), "k8sDep" , serviceRoleFullName + ".yaml");
    }

}