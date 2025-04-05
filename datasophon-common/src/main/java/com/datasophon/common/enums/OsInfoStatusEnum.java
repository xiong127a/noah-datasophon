package com.datasophon.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 操作系统信息收集状态枚举
 */
@Getter
public enum OsInfoStatusEnum {
    /**
     * 尚未开始收集
     */
    NONE("none", "未开始"),

    /**
     * 正在收集中
     */
    COLLECTING("collecting", "收集中"),

    /**
     * 正在加载
     */
    LOADING("loading", "加载中"),

    /**
     * 收集成功
     */
    SUCCESS("success", "成功"),

    /**
     * 收集出错
     */
    ERROR("error", "错误"),

    /**
     * 检测中
     */
    DETECTING("detecting", "检测中"),

    /**
     * 未检测到
     */
    NOT_DETECTED("not_detected", "未检测到"),

    /**
     * 部分成功
     */
    PARTIAL_SUCCESS("partial_success", "部分成功");

    /**
     * 状态码（与原字符串兼容）
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String desc;

    OsInfoStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 通过状态码获取枚举实例
     * 用于兼容处理字符串状态
     */
    public static OsInfoStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }

        for (OsInfoStatusEnum status : OsInfoStatusEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }

        return null;
    }

    /**
     * 确保枚举序列化为状态码
     */
    @JsonValue
    public String getCode() {
        return this.code;
    }

    @Override
    public String toString() {
        return this.code;
    }
}