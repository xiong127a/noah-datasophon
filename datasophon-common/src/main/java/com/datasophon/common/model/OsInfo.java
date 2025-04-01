package com.datasophon.common.model;

import com.datasophon.common.enums.LinuxDistribution;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.hardware.CpuInfo;
import com.datasophon.common.model.hardware.DiskInfo;
import com.datasophon.common.model.hardware.GpuInfo;
import com.datasophon.common.model.hardware.MemoryInfo;
import com.datasophon.common.model.hardware.NetworkInfo;
import com.datasophon.common.model.hardware.SwapInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * 操作系统信息类
 * 存储主机操作系统和硬件信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "networkInterfaces", "diskInfos" })
public class OsInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;

    /**
     * 操作系统类型(Windows/Linux等)
     */
    private String osType;

    /**
     * 操作系统发行版本(CentOS/Ubuntu等)
     */
    private String distribution;

    /**
     * 操作系统版本
     */
    private String version;

    /**
     * 内核版本
     */
    private String kernelVersion;

    /**
     * 主机名
     */
    private String hostname;

    /**
     * 主机完全限定域名(FQDN)
     */
    private String fqdn;

    /**
     * DNS服务器列表
     */
    private List<String> dnsServers;

    /**
     * hosts文件内容
     */
    private String hostsFile;

    /**
     * CPU信息
     */
    private CpuInfo cpuInfo;

    /**
     * 内存信息
     */
    private MemoryInfo memoryInfo;

    /**
     * 磁盘信息
     */
    private DiskInfo diskInfo;

    /**
     * 交换空间信息
     */
    private SwapInfo swapInfo;

    /**
     * GPU信息
     */
    private GpuInfo gpuInfo;

    /**
     * 网络接口信息
     */
    private NetworkInfo networkInfo;

    /**
     * 操作系统位数(32/64)
     */
    private String architecture;

    /**
     * 操作系统信息收集状态
     */
    private OsInfoStatusEnum osInfoStatus;

    /**
     * 主机名收集状态
     */
    private OsInfoStatusEnum hostnameStatus;

    /**
     * 操作系统信息收集状态
     */
    private OsInfoStatusEnum osStatus;

    /**
     * DNS服务器信息收集状态
     */
    private OsInfoStatusEnum dnsStatus;

    /**
     * hosts文件收集状态
     */
    private OsInfoStatusEnum hostsFileStatus;

    /**
     * CPU信息收集状态
     */
    private OsInfoStatusEnum cpuStatus;

    /**
     * 内存信息收集状态
     */
    private OsInfoStatusEnum memoryStatus;

    /**
     * 磁盘信息收集状态
     */
    private OsInfoStatusEnum diskStatus;

    /**
     * 交换空间信息收集状态
     */
    private OsInfoStatusEnum swapStatus;

    /**
     * GPU信息收集状态
     */
    private OsInfoStatusEnum gpuStatus;

    /**
     * 网络信息收集状态
     */
    private OsInfoStatusEnum networkStatus;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 版本ID
     */
    private String versionId;

    /**
     * 操作系统全名
     */
    private String fullName;

    /**
     * 显示名称（用于UI显示）
     */
    private String displayName;

    /**
     * 主版本号
     */
    private String majorVersion;

    /**
     * Linux发行版类型
     */
    private LinuxDistribution distributionType = LinuxDistribution.OTHER;

    /**
     * 信息是否有效
     */
    private boolean valid = false;

    /**
     * 最后更新项
     */
    private String lastUpdatedItem;

    /**
     * Linux发行版ID（用于兼容旧版API）
     */
    private String distributionId;

    /**
     * Linux发行版名称（用于兼容旧版API）
     */
    private String distributionName;

    /**
     * 设置硬件收集状态
     */
    public void setHardwareCollectionStatus(OsInfoStatusEnum status) {
        // 更新所有硬件组件的状态
        if (cpuInfo != null)
            cpuInfo.setStatus(status);
        if (memoryInfo != null)
            memoryInfo.setStatus(status);
        if (diskInfo != null)
            diskInfo.setStatus(status);
        if (swapInfo != null)
            swapInfo.setStatus(status);
        if (gpuInfo != null)
            gpuInfo.setStatus(status);
        if (networkInfo != null)
            networkInfo.setStatus(status);
    }

    /**
     * 设置GPU状态
     */
    public void setGpuStatus(OsInfoStatusEnum status) {
        this.gpuStatus = status;
        if (gpuInfo == null) {
            gpuInfo = new GpuInfo();
        }
        gpuInfo.setStatus(status);
    }

    /**
     * 设置网络状态
     */
    public void setNetworkStatus(OsInfoStatusEnum status) {
        this.networkStatus = status;
        if (networkInfo == null) {
            networkInfo = new NetworkInfo();
        }
        networkInfo.setStatus(status);
    }

    /**
     * 获取分发类型
     */
    public LinuxDistribution getDistributionType() {
        if (distributionType == null) {
            distributionType = LinuxDistribution.OTHER;
        }
        return distributionType;
    }

    /**
     * 设置分发类型
     * 同时更新相关发行版信息
     */
    public void setDistributionType(LinuxDistribution distributionType) {
        this.distributionType = distributionType;
        // 根据分发类型设置显示名称
        if (distributionType != null) {
            this.distribution = distributionType.getName();
            this.displayName = distributionType.getDisplayName();
        }
    }

    /**
     * 更新分发类型
     * 根据已设置的分发ID自动判断更新分发类型
     */
    public void updateDistributionType() {
        // 检查分发ID是否设置
        if (this.distribution == null || this.distribution.isEmpty()) {
            this.distributionType = LinuxDistribution.OTHER;
            return;
        }

        // 根据分发ID匹配相应分发类型
        for (LinuxDistribution distro : LinuxDistribution.values()) {
            if (distro.matches(this.distribution)) {
                this.distributionType = distro;
                this.displayName = distro.getDisplayName();
                return;
            }
        }

        // 未匹配到已知分发类型
        this.distributionType = LinuxDistribution.OTHER;
    }

    /**
     * 检查是否为特定分发类型
     */
    public boolean isDistribution(LinuxDistribution distro) {
        return this.distributionType == distro;
    }

    /**
     * 强制更新分发类型
     * 根据已设置的分发ID自动判断更新分发类型
     */
    public void forceUpdateDistribution() {
        if (this.distribution == null || this.distribution.isEmpty()) {
            this.distributionType = LinuxDistribution.OTHER;
            return;
        }

        String distId = this.distribution.toLowerCase();

        if (distId.contains("centos")) {
            if (this.version != null && this.version.startsWith("7")) {
                this.distributionType = LinuxDistribution.CENTOS7;
            } else if (this.version != null && this.version.startsWith("8")) {
                this.distributionType = LinuxDistribution.CENTOS8;
            } else {
                this.distributionType = LinuxDistribution.CENTOS;
            }
        } else if (distId.contains("redhat") || distId.contains("rhel")) {
            this.distributionType = LinuxDistribution.REDHAT;
        } else if (distId.contains("ubuntu")) {
            if (this.version != null && this.version.startsWith("22")) {
                this.distributionType = LinuxDistribution.UBUNTU22;
            } else if (this.version != null && this.version.startsWith("24")) {
                this.distributionType = LinuxDistribution.UBUNTU24;
            } else {
                this.distributionType = LinuxDistribution.UBUNTU;
            }
        } else if (distId.contains("debian")) {
            this.distributionType = LinuxDistribution.DEBIAN;
        } else if (distId.contains("kylin")) {
            if (this.version != null && this.version.startsWith("4")) {
                this.distributionType = LinuxDistribution.KYLIN_V4;
            } else if (this.version != null && this.version.startsWith("10")) {
                this.distributionType = LinuxDistribution.KYLIN_V10;
            } else {
                this.distributionType = LinuxDistribution.KYLIN;
            }
        } else if (distId.contains("uos") || distId.contains("deepin")) {
            this.distributionType = LinuxDistribution.UOS;
        } else {
            this.distributionType = LinuxDistribution.OTHER;
        }

        this.displayName = this.distributionType.getDisplayName();
    }

    /**
     * 检查版本是否匹配指定字符串
     */
    public boolean isVersion(String versionToCheck) {
        if (this.version == null || versionToCheck == null) {
            return false;
        }
        return this.version.startsWith(versionToCheck);
    }

    /**
     * 判断信息是否有效
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 设置信息是否有效
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    // 下面是常用的简便版本检查方法

    /**
     * 检查是否为CentOS 7
     */
    public boolean isCentOS7() {
        return isDistribution(LinuxDistribution.CENTOS7) ||
                (isDistribution(LinuxDistribution.CENTOS) && isVersion("7"));
    }

    /**
     * 检查是否为CentOS 8
     */
    public boolean isCentOS8() {
        return isDistribution(LinuxDistribution.CENTOS8) ||
                (isDistribution(LinuxDistribution.CENTOS) && isVersion("8"));
    }

    /**
     * 检查是否为Ubuntu 22.04
     */
    public boolean isUbuntu22() {
        return isDistribution(LinuxDistribution.UBUNTU22) ||
                (isDistribution(LinuxDistribution.UBUNTU) && isVersion("22"));
    }

    /**
     * 检查是否为Ubuntu 24.04
     */
    public boolean isUbuntu24() {
        return isDistribution(LinuxDistribution.UBUNTU24) ||
                (isDistribution(LinuxDistribution.UBUNTU) && isVersion("24"));
    }

    /**
     * 检查是否为麒麟V4
     */
    public boolean isKylinV4() {
        return isDistribution(LinuxDistribution.KYLIN_V4) ||
                (isDistribution(LinuxDistribution.KYLIN) && isVersion("4"));
    }

    /**
     * 检查是否为麒麟V10
     */
    public boolean isKylinV10() {
        return isDistribution(LinuxDistribution.KYLIN_V10) ||
                (isDistribution(LinuxDistribution.KYLIN) && isVersion("10"));
    }

    /**
     * 获取发行版ID
     */
    public String getDistributionId() {
        return StringUtils.isNotBlank(distributionId) ? distributionId : distribution;
    }

    /**
     * 设置发行版ID
     */
    public void setDistributionId(String distributionId) {
        this.distributionId = distributionId;
    }

    /**
     * 获取发行版名称
     */
    public String getDistributionName() {
        return StringUtils.isNotBlank(distributionName) ? distributionName : distribution;
    }

    /**
     * 设置发行版名称
     */
    public void setDistributionName(String distributionName) {
        this.distributionName = distributionName;
    }
}