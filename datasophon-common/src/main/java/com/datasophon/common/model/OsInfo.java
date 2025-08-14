package com.datasophon.common.model;

import com.datasophon.common.enums.OsDistribution;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.enums.OsType;
import com.datasophon.common.model.hardware.CpuInfo;
import com.datasophon.common.model.hardware.DiskInfo;
import com.datasophon.common.model.hardware.DnsInfo;
import com.datasophon.common.model.hardware.GpuInfo;
import com.datasophon.common.model.hardware.MemoryInfo;
import com.datasophon.common.model.hardware.NetworkInfo;
import com.datasophon.common.model.hardware.SwapInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.io.Serial;
import java.io.Serializable;

/**
 * 操作系统信息类
 * 存储主机操作系统和硬件信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString()
public class OsInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

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
     * 主机名
     */
    private String hostname;

    /**
     * 系统位数 (32/64)
     */
    private String bits;

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
     * 操作系统全名
     */
    private String fullName;

    /**
     * 操作系统发行版
     */
    private OsDistribution osDistribution = OsDistribution.OTHER;

    /**
     * 信息是否有效
     */
    private boolean valid = false;

    /**
     * 最后更新项
     */
    private String lastUpdatedItem;

    /**
     * DNS信息
     */
    private DnsInfo dnsInfo;

    /**
     * 获取操作系统类型
     * 基于发行版信息返回对应的OsType
     */
    public OsType getOsType() {
        if (osDistribution == null) {
            return OsType.LINUX; // 默认返回Linux
        }
        
        return switch (osDistribution) {
            case CENTOS -> OsType.CENTOS;
            case UBUNTU -> OsType.UBUNTU;
            case DEBIAN -> OsType.DEBIAN;
            case REDHAT -> OsType.RHEL;  // RedHat映射到RHEL
            case FEDORA, KYLIN, UOS -> OsType.OTHER;  // 这些发行版映射到OTHER
            default -> OsType.LINUX;
        };
    }
    
    /**
     * 获取操作系统完整描述
     */
    public String getFullDescription() {
        StringBuilder desc = new StringBuilder();
        
        if (distribution != null && !distribution.trim().isEmpty()) {
            desc.append(distribution);
        }
        
        if (version != null && !version.trim().isEmpty()) {
            if (!desc.isEmpty()) {
                desc.append(" ");
            }
            desc.append(version);
        }
        
        if (kernelVersion != null && !kernelVersion.trim().isEmpty()) {
            if (!desc.isEmpty()) {
                desc.append(" (kernel ");
                desc.append(kernelVersion);
                desc.append(")");
            }
        }
        
        return desc.isEmpty() ? "Unknown OS" : desc.toString();
    }
    
    /**
     * 获取版本ID（与version保持一致）
     */
    public String getVersionId() {
        return this.version;
    }

    /**
     * 设置版本ID（同时更新version）
     */
    public void setVersionId(String versionId) {
        this.version = versionId;
    }

    /**
     * 设置硬件收集状态
     */
    public void setHardwareCollectionStatus(OsInfoStatusEnum status) {
        // 更新所有硬件组件的状态
        if (cpuInfo != null) {
            cpuInfo.setStatus(status);
        }
        if (memoryInfo != null) {
            memoryInfo.setStatus(status);
        }
        if (diskInfo != null) {
            diskInfo.setStatus(status);
        }
        if (swapInfo != null) {
            swapInfo.setStatus(status);
        }
        if (gpuInfo != null) {
            gpuInfo.setStatus(status);
        }
        if (networkInfo != null) {
            networkInfo.setStatus(status);
        }
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
     * 设置DNS服务器状态
     */
    public void setDnsStatus(OsInfoStatusEnum status) {
        this.dnsStatus = status;
        if (dnsInfo == null) {
            dnsInfo = new DnsInfo();
        }
        dnsInfo.setStatus(status);
    }

    /**
     * 更新操作系统发行版
     * 根据当前设置的distribution和version确定具体发行版
     */
    public void updateOsDistribution() {
        this.osDistribution = OsDistribution.determine(this.distribution, this.version);
    }

    /**
     * 根据distributionId强制更新发行版类型
     */
    public void forceUpdateDistribution() {
        // 从distribution字段直接确定发行版
        if (StringUtils.isNotBlank(this.distribution)) {
            String distLower = this.distribution.toLowerCase();
            if (distLower.contains("centos")) {
                this.osDistribution = OsDistribution.CENTOS;
            } else if (distLower.contains("ubuntu")) {
                this.osDistribution = OsDistribution.UBUNTU;
            } else if (distLower.contains("debian")) {
                this.osDistribution = OsDistribution.DEBIAN;
            } else if (distLower.contains("fedora")) {
                this.osDistribution = OsDistribution.FEDORA;
            } else if (distLower.contains("rhel") ||
                    distLower.contains("redhat")) {
                this.osDistribution = OsDistribution.REDHAT;
            } else if (distLower.contains("kylin")) {
                this.osDistribution = OsDistribution.KYLIN;
            } else if (distLower.contains("uos")) {
                this.osDistribution = OsDistribution.UOS;
            } else {
                this.osDistribution = OsDistribution.OTHER;
            }
        }
    }

    /**
     * 获取发行版ID
     */
    public String getDistributionId() {
        // 优先返回distribution字段作为简要系统名称
        if (StringUtils.isNotBlank(distribution)) {
            return distribution;
        }
        // 如果distribution为空，则使用枚举值的identifier
        return osDistribution != null ? osDistribution.getIdentifier() : "linux";
    }

    /**
     * 检查版本是否匹配指定字符串
     * 
     * @param versionToCheck 要检查的版本号
     * @return 是否匹配
     */
    public boolean isVersion(String versionToCheck) {
        if (this.version == null || versionToCheck == null) {
            return false;
        }
        return this.version.startsWith(versionToCheck);
    }

    /**
     * 判断是否为特定操作系统版本
     */
    public boolean is(OsDistribution distro) {
        return this.osDistribution == distro;
    }

    /**
     * 获取硬件收集状态
     */
    public OsInfoStatusEnum getHardwareCollectionStatus() {
        // 如果任何子组件状态为ERROR，则返回ERROR
        if (cpuStatus == OsInfoStatusEnum.ERROR ||
                memoryStatus == OsInfoStatusEnum.ERROR ||
                diskStatus == OsInfoStatusEnum.ERROR ||
                networkStatus == OsInfoStatusEnum.ERROR) {
            return OsInfoStatusEnum.ERROR;
        }

        // 如果任何子组件状态为LOADING，则返回LOADING
        if (cpuStatus == OsInfoStatusEnum.LOADING ||
                memoryStatus == OsInfoStatusEnum.LOADING ||
                diskStatus == OsInfoStatusEnum.LOADING ||
                networkStatus == OsInfoStatusEnum.LOADING) {
            return OsInfoStatusEnum.LOADING;
        }

        // 如果所有必要组件都是SUCCESS，则返回SUCCESS
        if (cpuStatus == OsInfoStatusEnum.SUCCESS &&
                memoryStatus == OsInfoStatusEnum.SUCCESS &&
                diskStatus == OsInfoStatusEnum.SUCCESS &&
                networkStatus == OsInfoStatusEnum.SUCCESS) {
            return OsInfoStatusEnum.SUCCESS;
        }

        // 默认为COLLECTING
        return OsInfoStatusEnum.COLLECTING;
    }

    /**
     * 检查硬件信息是否已全部收集完成
     * 
     * @return 是否完成
     */
    public boolean isHardwareInfoComplete() {
        return getHardwareCollectionStatus() == OsInfoStatusEnum.SUCCESS;
    }

    /**
     * 检查硬件信息是否正在收集中
     * 
     * @return 是否正在收集
     */
    public boolean isHardwareInfoCollecting() {
        return getHardwareCollectionStatus() == OsInfoStatusEnum.LOADING;
    }
}