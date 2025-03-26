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
     * 系统架构（如 x86_64, arm64, aarch64 等）
     */
    private String architecture = "";

    /**
     * CPU信息（如型号、核心数）
     */
    private String cpuInfo = "";

    /**
     * CPU核心数
     */
    private Integer cpuCores = 0;

    /**
     * CPU物理数量
     */
    private Integer cpuCount = 1;

    /**
     * 每颗CPU的核心数
     */
    private Integer cpuCoresPerProcessor = 0;

    /**
     * 每核心的线程数
     */
    private Integer cpuThreadsPerCore = 2;

    /**
     * CPU逻辑处理器总数
     */
    private Integer cpuLogicalCores = 0;

    /**
     * 内存总量（GB）
     */
    private Double totalMemory = 0.0;

    /**
     * 可用内存（GB）
     */
    private Double availableMemory = 0.0;

    /**
     * 交换空间总量（GB）
     */
    private Double totalSwap = 0.0;

    /**
     * 可用交换空间（GB）
     */
    private Double availableSwap = 0.0;

    /**
     * 磁盘总容量（GB）
     */
    private Double totalDisk = 0.0;

    /**
     * 可用磁盘容量（GB）
     */
    private Double availableDisk = 0.0;

    /**
     * 显卡信息
     */
    private String gpuInfo = "";

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