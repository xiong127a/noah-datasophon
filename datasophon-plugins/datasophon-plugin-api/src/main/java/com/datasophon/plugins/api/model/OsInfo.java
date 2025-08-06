package com.datasophon.plugins.api.model;

import com.datasophon.common.enums.OsType;
import lombok.Builder;
import lombok.Data;

/**
 * 操作系统信息模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class OsInfo {
    
    /**
     * 操作系统类型
     */
    private OsType osType;
    
    /**
     * 操作系统名称
     */
    private String osName;
    
    /**
     * 系统版本
     */
    private String version;
    
    /**
     * 内核版本
     */
    private String kernelVersion;
    
    /**
     * 系统架构
     */
    private String architecture;
    
    /**
     * 发行版本
     */
    private String distribution;
    
    /**
     * 发行版版本
     */
    private String distributionVersion;
    
    /**
     * 系统位数
     */
    private String bits;
    
    /**
     * 主机名
     */
    private String hostname;
    
    /**
     * 时区
     */
    private String timezone;
    
    /**
     * 语言环境
     */
    private String locale;
    
    /**
     * 检查是否是指定的操作系统类型
     */
    public boolean isOsType(OsType osType) {
        return this.osType == osType;
    }
    
    /**
     * 检查是否是Linux系统
     */
    public boolean isLinux() {
        return osType == OsType.LINUX;
    }
    
    /**
     * 检查是否是指定的发行版
     */
    public boolean isDistribution(String distribution) {
        return this.distribution != null && 
               this.distribution.toLowerCase().contains(distribution.toLowerCase());
    }
    
    /**
     * 获取完整的系统描述
     */
    public String getFullDescription() {
        return String.format("%s %s (%s) - %s", 
                            osName, version, architecture, kernelVersion);
    }
}