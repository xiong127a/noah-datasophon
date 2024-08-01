package com.datasophon.k8s.util;

public class CommonUtil {

    public static String generateServiceRoleFullName(String serviceName, String serviceRoleName) {
        return String.format("%s-%s", serviceName.toLowerCase(), serviceRoleName.toLowerCase());
    }

    public static String generateRoleTagName(String serviceRoleName) {
        return String.format("%s=true", serviceRoleName);
    }

}