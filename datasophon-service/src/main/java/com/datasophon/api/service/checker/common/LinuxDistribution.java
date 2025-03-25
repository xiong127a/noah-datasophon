package com.datasophon.api.service.checker.common;

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
    OTHER("unknown"), FEDORA("fedora"), OPENEULER("openeuler");

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
}