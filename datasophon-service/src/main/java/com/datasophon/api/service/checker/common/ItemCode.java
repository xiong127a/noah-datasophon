package com.datasophon.api.service.checker.common;

import lombok.Getter;

@Getter
public enum ItemCode {
    PASSWORD_FREE("PASSWORD_FREE", "主机免密检查", 1, false, null),
    DISK("DISK", "磁盘空间检查", 2, true, "磁盘空间不足无法自动修复，需要您手动增加磁盘空间。确定要继续尝试修复吗？"),
    MEMORY("MEMORY", "内存检查", 3, true, "内存不足无法自动修复，需要您手动增加内存。确定要继续尝试修复吗？"),
    CPU("CPU", "CPU检查", 4, true, "CPU核心数不足无法自动修复，需要您手动增加CPU资源。确定要继续尝试修复吗？"),
    JAVA_ENV("JAVA_ENV", "专用Java环境检查", 5, false, null),
    FILE_HANDLE("FILE_HANDLE", "最大文件句柄数检查", 6, false, null),
    FIREWALL("FIREWALL", "防火墙检查", 7, false, null),
    SELINUX("SELINUX", "SELinux检查", 8, false, null),
    TIME_SYNC("TIME_SYNC", "时间同步检查", 9, false, null),
    USER_GROUP_CHECK("USER_GROUP_CHECK", "用户和组检查", 10, false, null),
    BASH_SHELL_CHECK("BASH_SHELL_CHECK", "Bash Shell检查", 11, false, null),
    SUDO_COMMAND_CHECK("SUDO_COMMAND_CHECK", "Sudo命令检查", 12, false, "sudo命令未安装，需要手动安装。确定要继续尝试修复吗？"),
    /**
     * 连字符函数名检查
     */
    HYPHEN_FUNCTION_CHECK("HYPHEN_FUNCTION_CHECK", "连字符函数名检查", 13, false,
            "系统不支持带连字符的函数名，需要手动修改脚本中的函数名，将连字符替换为下划线。确定要继续尝试修复吗？");

    private final String code;
    private final String name;
    private final int sequence;
    /**
     * -- GETTER --
     * 是否需要二次确认
     *
     * @return 是否需要二次确认
     */
    private final boolean needConfirm; // 是否需要二次确认
    /**
     * -- GETTER --
     * 获取确认消息
     *
     * @return 确认消息
     */
    private final String confirmMessage; // 确认消息

    ItemCode(String code, String name, int sequence, boolean needConfirm, String confirmMessage) {
        this.code = code;
        this.name = name;
        this.sequence = sequence;
        this.needConfirm = needConfirm;
        this.confirmMessage = confirmMessage;
    }

    ItemCode(String code, String description) {
        this.code = code;
        this.name = description;
        this.sequence = 0;
        this.needConfirm = false;
        this.confirmMessage = null;
    }

    /**
     * 通过代码查找检查项
     *
     * @param code 检查项代码
     * @return 检查项
     */
    public static ItemCode getByCode(String code) {
        for (ItemCode itemCode : values()) {
            if (itemCode.getCode().equals(code)) {
                return itemCode;
            }
        }
        return null;
    }

    /**
     * 通过序列号查找检查项
     *
     * @param sequence 检查项的序列号
     * @return 对应序列号的检查项，如果未找到则返回 null
     */
    public static ItemCode getBySequence(int sequence) {
        for (ItemCode itemCode : values()) {
            if (itemCode.getSequence() == sequence) {
                return itemCode;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return name;
    }
}