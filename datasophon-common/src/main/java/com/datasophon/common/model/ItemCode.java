package com.datasophon.common.model;

public enum ItemCode {
    PASSWORD_FREE("PASSWORD_FREE", "主机免密检查", 1),
    DISK("DISK", "磁盘空间检查", 2),
    MEMORY("MEMORY", "内存检查", 3),
    CPU("CPU", "CPU检查", 4),
    JAVA_ENV("JAVA_ENV", "Java环境检查", 5),
    FILE_HANDLE("FILE_HANDLE", "最大文件句柄数检查", 6),
    FIREWALL("FIREWALL", "防火墙检查", 7),
    SELINUX("SELINUX", "SELinux检查", 8),
    TIME_SYNC("TIME_SYNC", "时间同步检查", 9);

    private final String code;
    private final String desc;
    private final int sequence;

    ItemCode(String code, String desc, int sequence) {
        this.code = code;
        this.desc = desc;
        this.sequence = sequence;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
    
    public int getSequence() {
        return sequence;
    }
} 