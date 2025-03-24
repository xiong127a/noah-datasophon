package com.datasophon.api.service.checker.common;

import com.datasophon.api.service.checker.core.AbstractItemChecker.LinuxDistribution;

/**
 * 操作系统信息类
 * 存储主机操作系统相关信息
 */
public class OsInfo {
    // 发行版ID，如 centos、ubuntu
    private String distributionId = "";

    // 版本号，如 7、8、20.04
    private String versionId = "";

    // 内核版本
    private String kernelVersion = "";

    // 操作系统全名
    private String fullName = "";

    // Linux发行版类型
    private LinuxDistribution distribution = LinuxDistribution.UNKNOWN;

    // 信息是否有效
    private boolean valid = false;

    /**
     * 判断操作系统是否匹配指定的发行版
     * 
     * @param distro 要检查的发行版类型
     * @return 如果匹配返回true，否则返回false
     */
    public boolean isDistribution(LinuxDistribution distro) {
        return distro == this.distribution;
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
        if (versionId.startsWith(versionToCheck + ".") || versionToCheck.startsWith(versionId + ".")) {
            return true;
        }

        return false;
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
     * 判断信息是否有效
     * 
     * @return 如果信息有效返回true，否则返回false
     */
    public boolean isValid() {
        return valid;
    }

    // Getter 和 Setter 方法

    public String getDistributionId() {
        return distributionId;
    }

    public void setDistributionId(String distributionId) {
        this.distributionId = distributionId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LinuxDistribution getDistribution() {
        return distribution;
    }

    public void setDistribution(LinuxDistribution distribution) {
        this.distribution = distribution;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public String toString() {
        return "OsInfo{" +
                "distribution=" + distribution +
                ", distributionId='" + distributionId + '\'' +
                ", versionId='" + versionId + '\'' +
                ", kernelVersion='" + kernelVersion + '\'' +
                ", fullName='" + fullName + '\'' +
                '}';
    }
}