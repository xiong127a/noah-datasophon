package com.datasophon.common.model;

public enum ItemCode {
    PASSWORD_FREE("PASSWORD_FREE", "主机免密检查"),
    JAVA_ENV("JAVA_ENV", "Java环境检查"),
    FILE_HANDLE("FILE_HANDLE", "最大文件句柄数检查"),
    FIREWALL("FIREWALL", "防火墙检查"),
    SELINUX("SELINUX", "SELinux检查"),
    TIME_SYNC("TIME_SYNC", "时间同步检查");

    private final String code;
    private final String desc;

    ItemCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
} 