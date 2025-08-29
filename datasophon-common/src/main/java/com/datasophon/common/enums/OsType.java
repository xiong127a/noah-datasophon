package com.datasophon.common.enums;

import lombok.Getter;

/**
 * 操作系统类型枚举
 * 
 * @author DataSophon Team
 */
@Getter
public enum OsType {
    
    /**
     * CentOS
     */
    CENTOS("centos", "CentOS"),
    
    /**
     * Red Hat Enterprise Linux
     */
    RHEL("rhel", "Red Hat Enterprise Linux"),
    
    /**
     * Ubuntu
     */
    UBUNTU("ubuntu", "Ubuntu"),
    
    /**
     * Debian
     */
    DEBIAN("debian", "Debian"),
    
    /**
     * 麒麟操作系统 V10
     */
    KYLIN_V10("kylin-v10", "银河麒麟 V10"),
    
    /**
     * 麒麟操作系统 V4
     */
    KYLIN_V4("kylin-v4", "中标麒麟 V4"),
    
    /**
     * 麒麟操作系统（通用）
     */
    KYLIN("kylin", "麒麟操作系统"),
    
    /**
     * 其他/未知操作系统
     */
    OTHER("other", "Other");
    
    private final String code;
    private final String displayName;
    
    OsType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 根据代码获取操作系统类型
     */
    public static OsType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return OTHER;
        }
        
        for (OsType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        
        return OTHER;
    }
    
    /**
     * 根据显示名称获取操作系统类型
     */
    public static OsType fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return OTHER;
        }
        
        for (OsType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName.trim())) {
                return type;
            }
        }
        
        return OTHER;
    }
    
    /**
     * 判断是否为Linux系列
     */
    public boolean isLinuxFamily() {
        return this != OTHER; // 除了OTHER，其他都是Linux系列
    }
    
    /**
     * 判断是否为Red Hat系列
     */
    public boolean isRedHatFamily() {
        return this == CENTOS || this == RHEL;
    }
    
    /**
     * 判断是否为Debian系列
     */
    public boolean isDebianFamily() {
        return this == UBUNTU || this == DEBIAN;
    }
    
    /**
     * 判断是否为麒麟系列
     */
    public boolean isKylinFamily() {
        return this == KYLIN_V10 || this == KYLIN_V4 || this == KYLIN;
    }
    
    /**
     * 获取包管理器类型
     */
    public String getPackageManager() {
        if (isRedHatFamily() || isKylinFamily()) {
            return "yum/rpm";
        } else if (isDebianFamily()) {
            return "apt/dpkg";
        }
        return "unknown";
    }
}