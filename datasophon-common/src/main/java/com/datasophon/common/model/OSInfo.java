package com.datasophon.common.model;

import java.io.Serializable;

/**
 * 操作系统信息类
 * 用于存储主机的操作系统信息
 */
public class OSInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主机名
     */
    private String hostname;
    
    /**
     * 操作系统家族 (如 RedHat, Debian, SUSE等)
     */
    private String family;
    
    /**
     * 具体发行版 (如 CentOS, Ubuntu, Kylin等)
     */
    private String distro;
    
    /**
     * 主版本号 (如 CentOS 7.9中的7)
     */
    private Integer majorVersion;
    
    /**
     * 完整版本号 (如 "7.9.2009")
     */
    private String fullVersion;
    
    /**
     * 默认构造函数
     */
    public OSInfo() {
    }
    
    /**
     * 初始化操作系统信息
     * 
     * @param hostname 主机名
     * @param family 操作系统家族
     * @param distro 具体发行版
     * @param majorVersion 主版本号
     * @param fullVersion 完整版本号
     */
    public OSInfo(String hostname, String family, String distro, Integer majorVersion, String fullVersion) {
        this.hostname = hostname;
        this.family = family;
        this.distro = distro;
        this.majorVersion = majorVersion;
        this.fullVersion = fullVersion;
    }
    
    /**
     * 判断是否为CentOS系统
     */
    public boolean isCentOS() {
        return "CentOS".equalsIgnoreCase(distro);
    }
    
    /**
     * 判断是否为RHEL系统
     */
    public boolean isRHEL() {
        return "RHEL".equalsIgnoreCase(distro);
    }
    
    /**
     * 判断是否为RedHat系列系统
     */
    public boolean isRedHatFamily() {
        return "RedHat".equalsIgnoreCase(family);
    }
    
    /**
     * 判断是否为Ubuntu系统
     */
    public boolean isUbuntu() {
        return "Ubuntu".equalsIgnoreCase(distro);
    }
    
    /**
     * 判断是否为Debian系统
     */
    public boolean isDebian() {
        return "Debian".equalsIgnoreCase(distro);
    }
    
    /**
     * 判断是否为Debian系列系统
     */
    public boolean isDebianFamily() {
        return "Debian".equalsIgnoreCase(family);
    }
    
    /**
     * 判断是否为Kylin系统
     */
    public boolean isKylin() {
        return "Kylin".equalsIgnoreCase(distro);
    }
    
    /**
     * 判断是否为特定版本的系统
     */
    public boolean isVersion(int version) {
        return majorVersion != null && majorVersion == version;
    }
    
    /**
     * 判断是否为CentOS 7系统
     */
    public boolean isCentOS7() {
        return isCentOS() && isVersion(7);
    }
    
    /**
     * 判断是否为Ubuntu 22系统
     */
    public boolean isUbuntu22() {
        return isUbuntu() && isVersion(22);
    }
    
    /**
     * 判断是否为Ubuntu 24系统
     */
    public boolean isUbuntu24() {
        return isUbuntu() && isVersion(24);
    }
    
    /**
     * 判断是否为Kylin V4系统
     */
    public boolean isKylinV4() {
        return isKylin() && isVersion(4);
    }
    
    /**
     * 判断是否为Kylin V10系统
     */
    public boolean isKylinV10() {
        return isKylin() && isVersion(10);
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    public String getFamily() {
        return family;
    }
    
    public void setFamily(String family) {
        this.family = family;
    }
    
    public String getDistro() {
        return distro;
    }
    
    public void setDistro(String distro) {
        this.distro = distro;
    }
    
    public Integer getMajorVersion() {
        return majorVersion;
    }
    
    public void setMajorVersion(Integer majorVersion) {
        this.majorVersion = majorVersion;
    }
    
    public String getFullVersion() {
        return fullVersion;
    }
    
    public void setFullVersion(String fullVersion) {
        this.fullVersion = fullVersion;
    }
    
    @Override
    public String toString() {
        return distro + " " + fullVersion + " (" + family + ")";
    }
} 