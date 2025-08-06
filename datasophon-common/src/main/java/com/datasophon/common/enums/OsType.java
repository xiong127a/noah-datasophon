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
     * Linux操作系统
     */
    LINUX("linux", "Linux"),
    
    /**
     * CentOS
     */
    CENTOS("centos", "CentOS"),
    
    /**
     * Ubuntu
     */
    UBUNTU("ubuntu", "Ubuntu"),
    
    /**
     * Red Hat Enterprise Linux
     */
    RHEL("rhel", "Red Hat Enterprise Linux"),
    
    /**
     * Rocky Linux
     */
    ROCKY("rocky", "Rocky Linux"),
    
    /**
     * Alma Linux
     */
    ALMA("alma", "Alma Linux"),
    
    /**
     * SUSE Linux
     */
    SUSE("suse", "SUSE Linux"),
    
    /**
     * Debian
     */
    DEBIAN("debian", "Debian"),
    
    /**
     * Windows
     */
    WINDOWS("windows", "Windows"),
    
    /**
     * macOS
     */
    MACOS("macos", "macOS"),
    
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
        return this == LINUX || this == CENTOS || this == UBUNTU || 
               this == RHEL || this == ROCKY || this == ALMA || 
               this == SUSE || this == DEBIAN;
    }
    
    /**
     * 判断是否为Red Hat系列
     */
    public boolean isRedHatFamily() {
        return this == CENTOS || this == RHEL || this == ROCKY || this == ALMA;
    }
    
    /**
     * 判断是否为Debian系列
     */
    public boolean isDebianFamily() {
        return this == UBUNTU || this == DEBIAN;
    }
}