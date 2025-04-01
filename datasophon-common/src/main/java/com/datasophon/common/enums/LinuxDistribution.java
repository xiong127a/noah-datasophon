package com.datasophon.common.enums;

/**
 * Linux发行版枚举
 * 包含常见的Linux发行版及其标识
 */
public enum LinuxDistribution {

    /**
     * CentOS发行版
     */
    CENTOS("CentOS", "CentOS Linux", "centos"),

    /**
     * CentOS 7发行版
     */
    CENTOS7("CentOS 7", "CentOS Linux 7", "centos 7"),

    /**
     * CentOS 8发行版
     */
    CENTOS8("CentOS 8", "CentOS Linux 8", "centos 8"),

    /**
     * RedHat Enterprise Linux发行版
     */
    REDHAT("RHEL", "Red Hat Enterprise Linux", "rhel", "redhat"),

    /**
     * Ubuntu发行版
     */
    UBUNTU("Ubuntu", "Ubuntu Linux", "ubuntu"),

    /**
     * Ubuntu 22.04发行版
     */
    UBUNTU22("Ubuntu 22.04", "Ubuntu 22.04 LTS", "ubuntu 22"),

    /**
     * Ubuntu 24.04发行版
     */
    UBUNTU24("Ubuntu 24.04", "Ubuntu 24.04 LTS", "ubuntu 24"),

    /**
     * Kylin发行版（国产麒麟）
     */
    KYLIN("Kylin", "中标麒麟", "kylin"),

    /**
     * Kylin V4发行版
     */
    KYLIN_V4("Kylin V4", "中标麒麟 V4", "kylin 4"),

    /**
     * Kylin V10发行版
     */
    KYLIN_V10("Kylin V10", "中标麒麟 V10", "kylin 10"),

    /**
     * Debian发行版
     */
    DEBIAN("Debian", "Debian GNU/Linux", "debian"),

    /**
     * OpenSUSE发行版
     */
    OPENSUSE("OpenSUSE", "OpenSUSE Linux", "opensuse"),

    /**
     * SUSE Enterprise Linux发行版
     */
    SUSE("SUSE", "SUSE Linux Enterprise", "suse"),

    /**
     * UOS统信操作系统
     */
    UOS("UOS", "统信操作系统", "uos", "deepin"),

    /**
     * Fedora发行版
     */
    FEDORA("Fedora", "Fedora Linux", "fedora"),

    /**
     * OpenEuler发行版
     */
    OPENEULER("OpenEuler", "OpenEuler Linux", "openeuler"),

    /**
     * 其他发行版
     */
    OTHER("Other", "未知Linux发行版", "unknown");

    /**
     * 发行版名称
     */
    private final String name;

    /**
     * 发行版显示名称
     */
    private final String displayName;

    /**
     * 发行版标识符列表，一个发行版可能有多个标识符
     */
    private final String[] identifiers;

    /**
     * 构造函数
     *
     * @param name        发行版名称
     * @param displayName 发行版显示名称
     * @param identifiers 发行版标识符列表
     */
    LinuxDistribution(String name, String displayName, String... identifiers) {
        this.name = name;
        this.displayName = displayName;
        this.identifiers = identifiers;
    }

    /**
     * 获取发行版名称
     *
     * @return 发行版名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取发行版显示名称
     *
     * @return 发行版显示名称
     */
    public String getDisplayName() {
        return displayName;
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
     * 检查传入的标识符是否匹配当前发行版（match方法的别名）
     *
     * @param id 要检查的标识符
     * @return 如果匹配返回true，否则返回false
     */
    public boolean matches(String id) {
        return match(id);
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
        // 先检查特定版本
        for (LinuxDistribution distro : values()) {
            if (distro != OTHER && distro != CENTOS && distro != UBUNTU && distro != KYLIN &&
                    distro.match(id)) {
                return distro;
            }
        }

        // 然后检查通用版本
        for (LinuxDistribution distro : values()) {
            if (distro != OTHER && distro.match(id)) {
                return distro;
            }
        }

        return OTHER;
    }

    @Override
    public String toString() {
        return this.name;
    }
}