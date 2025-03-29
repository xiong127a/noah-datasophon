package com.datasophon.common.model;

import com.datasophon.common.enums.OsInfoStatusEnum;
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
     * - COLLECTING：收集中
     * - SUCCESS：收集完成
     * - ERROR：收集出错
     */
    private OsInfoStatusEnum hardwareCollectionStatus;

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
     * CPU信息状态：success, error, collecting
     */
    private OsInfoStatusEnum cpuStatus;

    /**
     * 内存信息状态：success, error, collecting
     */
    private OsInfoStatusEnum memoryStatus;

    /**
     * 磁盘信息状态：success, error, collecting
     */
    private OsInfoStatusEnum diskStatus;

    /**
     * Linux发行版枚举
     * 包含常见的Linux发行版及其标识
     */
    public enum LinuxDistribution {

        /**
         * CentOS发行版
         */
        CENTOS("centos"),

        /**
         * RedHat Enterprise Linux发行版
         */
        REDHAT("rhel", "redhat"),

        /**
         * Ubuntu发行版
         */
        UBUNTU("ubuntu"),

        /**
         * Kylin发行版（国产麒麟）
         */
        KYLIN("kylin"),

        /**
         * Debian发行版
         */
        DEBIAN("debian"),

        /**
         * OpenSUSE发行版
         */
        OPENSUSE("opensuse"),

        /**
         * SUSE Enterprise Linux发行版
         */
        SUSE("suse"),

        /**
         * 其他发行版
         */
        OTHER("unknown"),

        /**
         * Fedora发行版
         */
        FEDORA("fedora"),

        /**
         * OpenEuler发行版
         */
        OPENEULER("openeuler");

        /**
         * 发行版标识符列表，一个发行版可能有多个标识符
         */
        private final String[] identifiers;

        /**
         * 构造函数
         *
         * @param identifiers 发行版标识符列表
         */
        LinuxDistribution(String... identifiers) {
            this.identifiers = identifiers;
        }

        /**
         * 获取发行版标识符列表
         *
         * @return 标识符列表
         */
        public String[] getIdentifiers() {
            return identifiers;
        }

        /**
         * 检查传入的标识符是否匹配当前发行版
         *
         * @param id 要检查的标识符
         * @return 如果匹配返回true，否则返回false
         */
        public boolean match(String id) {
            if (id == null || id.isEmpty()) {
                return false;
            }

            id = id.toLowerCase();
            for (String identifier : identifiers) {
                if (id.contains(identifier)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * 根据标识符获取对应的发行版枚举
         *
         * @param id 发行版标识符
         * @return 匹配的发行版枚举，如果没有匹配项则返回OTHER
         */
        public static LinuxDistribution fromId(String id) {
            if (id == null || id.isEmpty()) {
                return OTHER;
            }

            id = id.toLowerCase();
            for (LinuxDistribution distro : values()) {
                if (distro.match(id)) {
                    return distro;
                }
            }

            return OTHER;
        }

        @Override
        public String toString() {
            return this.identifiers[0];
        }
    }

    /**
     * Linux发行版类型
     */
    private LinuxDistribution distributionType = LinuxDistribution.OTHER;

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
     * 设置发行版名称
     */
    public void setDistributionName(String distributionName) {
        this.distributionName = distributionName;
        this.distribution = distributionName;
        if (distributionName != null) {
            this.distributionType = LinuxDistribution.fromId(distributionName);
        }
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

    /**
     * 设置硬件收集状态（向后兼容）
     */
    public void setHardwareCollectionStatus(String hardwareCollectionStatus) {
        this.hardwareCollectionStatus = OsInfoStatusEnum.getByCode(hardwareCollectionStatus);
    }

    /**
     * 设置硬件收集状态（使用枚举）
     */
    public void setHardwareCollectionStatus(OsInfoStatusEnum hardwareCollectionStatus) {
        this.hardwareCollectionStatus = hardwareCollectionStatus;
    }

    /**
     * 设置CPU信息状态
     */
    public void setCpuStatus(OsInfoStatusEnum status) {
        this.cpuStatus = status;
    }

    /**
     * 设置内存信息状态
     */
    public void setMemoryStatus(OsInfoStatusEnum status) {
        this.memoryStatus = status;
    }

    /**
     * 设置磁盘信息状态
     */
    public void setDiskStatus(OsInfoStatusEnum status) {
        this.diskStatus = status;
    }

    /**
     * 判断操作系统是否匹配指定的发行版
     * 
     * @param distro 要检查的发行版类型
     * @return 如果匹配返回true，否则返回false
     */
    public boolean isDistribution(LinuxDistribution distro) {
        return distro == this.distributionType;
    }

    /**
     * 检查版本号是否匹配指定的版本
     * 
     * @param versionToCheck 要检查的版本号
     * @return 如果匹配返回true，否则返回false
     */
    public boolean isVersion(String versionToCheck) {
        if (versionId == null || versionToCheck == null) {
            return false;
        }

        // 精确匹配
        if (versionId.equals(versionToCheck)) {
            return true;
        }

        // 前缀匹配，如 "7" 匹配 "7.9"
        return versionId.startsWith(versionToCheck + ".") || versionToCheck.startsWith(versionId + ".");
    }

    /**
     * 检查是否是CentOS 7.x版本
     * 
     * @return 如果是CentOS 7.x版本返回true，否则返回false
     */
    public boolean isCentOS7() {
        return isDistribution(LinuxDistribution.CENTOS) && isVersion("7");
    }

    /**
     * 检查是否是CentOS 8.x版本
     * 
     * @return 如果是CentOS 8.x版本返回true，否则返回false
     */
    public boolean isCentOS8() {
        return isDistribution(LinuxDistribution.CENTOS) && isVersion("8");
    }

    /**
     * 检查是否是Ubuntu 22.04版本
     * 
     * @return 如果是Ubuntu 22.04版本返回true，否则返回false
     */
    public boolean isUbuntu22() {
        return isDistribution(LinuxDistribution.UBUNTU) && isVersion("22.04");
    }

    /**
     * 检查是否是Ubuntu 24.04版本
     * 
     * @return 如果是Ubuntu 24.04版本返回true，否则返回false
     */
    public boolean isUbuntu24() {
        return isDistribution(LinuxDistribution.UBUNTU) && isVersion("24.04");
    }

    /**
     * 检查是否是Kylin V4版本
     * 
     * @return 如果是Kylin V4版本返回true，否则返回false
     */
    public boolean isKylinV4() {
        return isDistribution(LinuxDistribution.KYLIN) && isVersion("4");
    }

    /**
     * 检查是否是Kylin V10版本
     * 
     * @return 如果是Kylin V10版本返回true，否则返回false
     */
    public boolean isKylinV10() {
        return isDistribution(LinuxDistribution.KYLIN) && isVersion("10");
    }

    /**
     * 判断当前系统是否使用systemd作为初始化系统
     * 
     * @return 如果使用systemd返回true，否则返回false
     */
    public boolean usesSystemd() {
        // 大部分现代发行版都使用systemd，除了旧版本
        if (isDistribution(LinuxDistribution.CENTOS) || isDistribution(LinuxDistribution.REDHAT)) {
            // CentOS/RHEL 7及以上版本使用systemd
            try {
                int majorVersion = Integer.parseInt(versionId.split("\\.")[0]);
                return majorVersion >= 7;
            } catch (Exception e) {
                // 解析出错，默认可能使用systemd
                return true;
            }
        } else if (isDistribution(LinuxDistribution.UBUNTU)) {
            // Ubuntu 16.04及以上版本使用systemd
            try {
                float version = Float.parseFloat(versionId);
                return version >= 16.04;
            } catch (Exception e) {
                // 解析出错，默认可能使用systemd
                return true;
            }
        } else if (isDistribution(LinuxDistribution.DEBIAN)) {
            // Debian 8及以上版本使用systemd
            try {
                int version = Integer.parseInt(versionId);
                return version >= 8;
            } catch (Exception e) {
                // 解析出错，默认可能使用systemd
                return true;
            }
        } else if (isDistribution(LinuxDistribution.KYLIN)) {
            // 麒麟V4及以上版本使用systemd
            try {
                int version = Integer.parseInt(versionId);
                return version >= 4;
            } catch (Exception e) {
                // 解析出错，默认可能使用systemd
                return true;
            }
        }

        // 默认假设使用systemd
        return true;
    }

    /**
     * 根据distributionId强制更新distributionType枚举值
     * 解决某些情况下distributionId和distributionType不一致的问题
     */
    public void forceUpdateDistribution() {
        if (this.distributionId == null || this.distributionId.isEmpty()) {
            return;
        }

        // 将ID转为小写以便更好地进行比较
        String lowerDistroId = this.distributionId.toLowerCase().trim();

        // 根据分发ID判断Linux发行版类型
        if (lowerDistroId.contains("centos")) {
            this.distributionType = LinuxDistribution.CENTOS;
        } else if (lowerDistroId.contains("rhel") || lowerDistroId.contains("redhat")
                || lowerDistroId.contains("red hat")) {
            this.distributionType = LinuxDistribution.REDHAT;
        } else if (lowerDistroId.contains("ubuntu")) {
            this.distributionType = LinuxDistribution.UBUNTU;
        } else if (lowerDistroId.contains("debian")) {
            this.distributionType = LinuxDistribution.DEBIAN;
        } else if (lowerDistroId.contains("fedora")) {
            this.distributionType = LinuxDistribution.FEDORA;
        } else if (lowerDistroId.contains("suse") || lowerDistroId.contains("sles")) {
            this.distributionType = LinuxDistribution.SUSE;
        } else if (lowerDistroId.contains("kylin")) {
            this.distributionType = LinuxDistribution.KYLIN;
        } else if (lowerDistroId.contains("openeuler")) {
            this.distributionType = LinuxDistribution.OPENEULER;
        } else {
            this.distributionType = LinuxDistribution.OTHER;
        }

        // 更新distribution字符串以保持一致性
        if (this.distributionType != LinuxDistribution.OTHER) {
            this.distribution = this.distributionType.toString();
        }
    }

    /**
     * 获取Linux发行版类型枚举
     */
    public LinuxDistribution getDistributionType() {
        return distributionType;
    }

    /**
     * 设置Linux发行版类型枚举
     */
    public void setDistributionType(LinuxDistribution distributionType) {
        this.distributionType = distributionType;
        // 更新distribution字符串以保持一致性
        if (distributionType != null && distributionType != LinuxDistribution.OTHER) {
            this.distribution = distributionType.toString();
        }
    }
}