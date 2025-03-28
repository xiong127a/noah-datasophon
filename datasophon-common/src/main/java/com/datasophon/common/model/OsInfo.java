package com.datasophon.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * 操作系统信息类
 * 存储主机操作系统相关信息
 */
@Data
@NoArgsConstructor
@ToString
public class OsInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主机名
     */
    private String hostname = "";

    /**
     * 完全限定域名(FQDN)
     */
    private String fqdn = "";

    /**
     * 发行版ID，如 centos、ubuntu
     */
    private String distributionId = "";

    /**
     * 发行版名称，如CentOS、Ubuntu
     */
    private String distribution = "";

    /**
     * 发行版名称的别名，与distribution相同
     */
    private String distributionName = "";

    /**
     * 版本号，如 7、8、20.04
     */
    private String versionId = "";

    /**
     * 版本号的别名，与versionId相同
     */
    private String distributionVersion = "";

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
     * DNS服务器信息
     */
    private String dnsServers = "";

    /**
     * CPU信息（如型号、核心数）
     */
    private String cpuInfo = "";

    /**
     * CPU型号
     */
    private String cpuModel = "";

    /**
     * CPU频率（GHz）
     */
    private Double cpuFrequency = 0.0;

    /**
     * CPU核心数
     */
    private Integer cpuCores = 0;

    /**
     * CPU核心数的别名，与cpuCores相同
     */
    private Integer cpuCoreNum = 0;

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
     * 内存总量（字节）
     */
    private Long totalMem = 0L;

    /**
     * 可用内存（GB）
     */
    private Double availableMemory = 0.0;

    /**
     * 可用内存（字节）
     */
    private Long availableMem = 0L;

    /**
     * 交换空间总量（GB）
     */
    private Double totalSwap = 0.0;

    /**
     * 交换空间总量（字节）
     */
    private Long totalSwapBytes = 0L;

    /**
     * 可用交换空间（GB）
     */
    private Double availableSwap = 0.0;

    /**
     * 可用交换空间（字节）
     */
    private Long availableSwapBytes = 0L;

    /**
     * 磁盘总容量（GB）
     */
    private Double totalDisk = 0.0;

    /**
     * 磁盘总容量（字节）
     */
    private Long totalDiskBytes = 0L;

    /**
     * 可用磁盘容量（GB）
     */
    private Double availableDisk = 0.0;

    /**
     * 可用磁盘容量（字节）
     */
    private Long availableDiskBytes = 0L;

    /**
     * 显卡信息
     */
    private String gpuInfo = "";

    /**
     * 显卡显存大小（GB）
     */
    private Double gpuMemory = 0.0;

    /**
     * 信息是否有效
     */
    private boolean valid = false;

    /**
     * 错误信息
     */
    private String errorMessage = "";

    /**
     * 1分钟负载
     */
    private Double load1Min;

    /**
     * 5分钟负载
     */
    private Double load5Min;

    /**
     * 15分钟负载
     */
    private Double load15Min;

    /**
     * 硬件信息收集状态
     * 可能的值:
     * - null/未设置：未开始收集
     * - "loading"：收集中
     * - "success"：收集完成
     * - "error"：收集出错
     */
    private String hardwareCollectionStatus;

    /**
     * 最后更新的硬件项
     * 记录最近更新的硬件信息项，便于前端知道哪些信息已经收集完成
     */
    private String lastUpdatedItem;

    /**
     * 操作系统主版本号（不带小数点），如"7"、"20"
     */
    private String majorVersion;

    /**
     * 显示名称（用于界面展示）
     */
    private String displayName;

    /**
     * 获取操作系统主版本号
     * 如7.9返回7
     */
    public String getMajorVersion() {
        if (majorVersion != null && !majorVersion.isEmpty()) {
            return majorVersion;
        }

        String version = versionId;
        if (distributionVersion != null && !distributionVersion.isEmpty()) {
            version = distributionVersion;
        }

        if (version == null || version.isEmpty()) {
            return "";
        }

        if (version.contains(".")) {
            return version.split("\\.")[0];
        }

        return version;
    }

    /**
     * 设置操作系统主版本号
     */
    public void setMajorVersion(String majorVersion) {
        this.majorVersion = majorVersion;
    }

    /**
     * 获取展示用的系统名称
     */
    public String getDisplayName() {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }

        if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        }

        // 尝试使用distributionName
        if (distributionName != null && !distributionName.isEmpty()) {
            if (distributionVersion != null && !distributionVersion.isEmpty()) {
                return distributionName + " " + distributionVersion;
            }
            return distributionName;
        }

        // 兼容原有字段
        if (distribution != null && !distribution.isEmpty() && versionId != null && !versionId.isEmpty()) {
            return distribution + " " + versionId;
        }

        if (distributionId != null && !distributionId.isEmpty()) {
            if (versionId != null && !versionId.isEmpty()) {
                return distributionId + " " + versionId;
            }
            if (distributionVersion != null && !distributionVersion.isEmpty()) {
                return distributionId + " " + distributionVersion;
            }
            return distributionId;
        }

        return "未知操作系统";
    }

    /**
     * 设置展示名称
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 将新的字节值字段同步到原有GB字段
     * 设置字节值后自动同步到GB
     */
    public void setTotalMem(Long totalMem) {
        this.totalMem = totalMem;
        if (totalMem != null) {
            this.totalMemory = Math.round(totalMem / (1024.0 * 1024.0 * 1024.0) * 10) / 10.0;
        }
    }

    public void setAvailableMem(Long availableMem) {
        this.availableMem = availableMem;
        if (availableMem != null) {
            this.availableMemory = Math.round(availableMem / (1024.0 * 1024.0 * 1024.0) * 10) / 10.0;
        }
    }

    public void setTotalSwap(Long totalSwap) {
        this.totalSwapBytes = totalSwap;
        if (totalSwap != null) {
            this.totalSwap = Math.round(totalSwap / (1024.0 * 1024.0 * 1024.0) * 10) / 10.0;
        }
    }

    public void setAvailableSwap(Long availableSwap) {
        this.availableSwapBytes = availableSwap;
        if (availableSwap != null) {
            this.availableSwap = Math.round(availableSwap / (1024.0 * 1024.0 * 1024.0) * 10) / 10.0;
        }
    }

    public void setTotalDisk(Long totalDisk) {
        this.totalDiskBytes = totalDisk;
        if (totalDisk != null) {
            this.totalDisk = Math.round(totalDisk / (1024.0 * 1024.0 * 1024.0) * 10) / 10.0;
        }
    }

    public void setAvailableDisk(Long availableDisk) {
        this.availableDiskBytes = availableDisk;
        if (availableDisk != null) {
            this.availableDisk = Math.round(availableDisk / (1024.0 * 1024.0 * 1024.0) * 10) / 10.0;
        }
    }

    /**
     * 兼容性方法，保持与旧代码兼容
     */
    public void setDistributionName(String distributionName) {
        this.distributionName = distributionName;
        this.distribution = distributionName;
    }

    public void setDistributionVersion(String distributionVersion) {
        this.distributionVersion = distributionVersion;
        this.versionId = distributionVersion;
    }

    public void setCpuModel(String cpuModel) {
        this.cpuModel = cpuModel;
        this.cpuInfo = cpuModel;
    }

    public void setCpuCoreNum(Integer cpuCoreNum) {
        this.cpuCoreNum = cpuCoreNum;
        this.cpuCores = cpuCoreNum;
    }
}