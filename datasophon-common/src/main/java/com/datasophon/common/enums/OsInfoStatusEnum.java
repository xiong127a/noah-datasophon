package com.datasophon.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 操作系统信息状态枚举
 * 用于OsInfo中的状态字段
 */
@Getter
public enum OsInfoStatusEnum {

    /**
     * 收集中状态
     */
    COLLECTING("collecting", "收集中"),

    /**
     * 加载中状态
     */
    LOADING("loading", "加载中"),

    /**
     * 成功状态
     */
    SUCCESS("success", "成功"),

    /**
     * 错误状态
     */
    ERROR("error", "错误");

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