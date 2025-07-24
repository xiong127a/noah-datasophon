package com.datasophon.common.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * Linux发行版枚举
 * 表示支持的操作系统发行版及版本
 */
@Getter
public enum OsDistribution {
    CENTOS("CentOS", "centos"),
    CENTOS7("CentOS 7", "centos"),
    CENTOS8("CentOS 8", "centos"),
    UBUNTU("Ubuntu", "ubuntu"),
    UBUNTU22("Ubuntu 22.04", "ubuntu"),
    UBUNTU24("Ubuntu 24.04", "ubuntu"),
    DEBIAN("Debian", "debian"),
    REDHAT("RedHat", "rhel"),
    FEDORA("Fedora", "fedora"),
    KYLIN("Kylin", "kylin"),
    KYLIN_V10("Kylin V10", "kylin"),
    KYLIN_V4("Kylin V4", "kylin"),
    UOS("UOS", "uos"),
    ALPINE("Alpine", "alpine"),
    ALPINE3("Alpine 3", "alpine"),
    OTHER("Linux", "linux");

    // 显示名称
    private final String displayName;
    // 系统标识符
    private final String identifier;

    OsDistribution(String displayName, String identifier) {
        this.displayName = displayName;
        this.identifier = identifier;
    }

    /**
     * 根据操作系统名称和版本确定具体发行版
     */
    public static OsDistribution determine(String distribution, String version) {
        if (StringUtils.isBlank(distribution)) {
            return OTHER;
        }

        String distLower = distribution.toLowerCase();

        if (distLower.contains("centos")) {
            if (version != null) {
                if (version.startsWith("7")) {
                    return CENTOS7;
                } else if (version.startsWith("8")) {
                    return CENTOS8;
                }
            }
            return CENTOS;
        } else if (distLower.contains("ubuntu")) {
            if (version != null) {
                if (version.startsWith("22")) {
                    return UBUNTU22;
                } else if (version.startsWith("24")) {
                    return UBUNTU24;
                }
            }
            return UBUNTU;
        } else if (distLower.contains("kylin")) {
            if (version != null) {
                if (version.startsWith("10")) {
                    return KYLIN_V10;
                } else if (version.startsWith("4")) {
                    return KYLIN_V4;
                }
            }
            return KYLIN;
        } else if (distLower.contains("alpine")) {
            if (version != null) {
                if (version.startsWith("3")) {
                    return ALPINE3;
                }
            }
            return ALPINE;
        } else if (distLower.contains("fedora")) {
            return FEDORA;
        } else if (distLower.contains("redhat") || distLower.contains("rhel")) {
            return REDHAT;
        } else if (distLower.contains("debian")) {
            return DEBIAN;
        } else if (distLower.contains("uos") || distLower.contains("deepin")) {
            return UOS;
        }

        return OTHER;
    }
}