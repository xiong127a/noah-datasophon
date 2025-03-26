package com.datasophon.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 操作系统信息类
 * 存储主机操作系统相关信息
 */
@Data
@NoArgsConstructor
@ToString
public class OsInfo {
    /**
     * 发行版ID，如 centos、ubuntu
     */
    private String distributionId = "";

    /**
     * 发行版名称，如CentOS、Ubuntu
     */
    private String distribution = "";

    /**
     * 版本号，如 7、8、20.04
     */
    private String versionId = "";

    /**
     * 操作系统全名
     */
    private String fullName = "";

    /**
     * 内核版本
     */
    private String kernelVersion = "";

    /**
     * 信息是否有效
     */
    private boolean valid = false;

    /**
     * 获取操作系统主版本号
     * 如7.9返回7
     */
    public String getMajorVersion() {
        if (versionId == null || versionId.isEmpty()) {
            return "";
        }
        
        if (versionId.contains(".")) {
            return versionId.split("\\.")[0];
        }
        
        return versionId;
    }

    /**
     * 获取展示用的系统名称
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        }
        
        if (distribution != null && !distribution.isEmpty() && versionId != null && !versionId.isEmpty()) {
            return distribution + " " + versionId;
        }
        
        if (distributionId != null && !distributionId.isEmpty() && versionId != null && !versionId.isEmpty()) {
            return distributionId + " " + versionId;
        }
        
        return "未知操作系统";
    }
} 