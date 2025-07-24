package com.datasophon.common.model.hardware;

import com.datasophon.common.enums.OsInfoStatusEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 硬件信息基类
 * 所有硬件类的公共基类，提供状态和基本属性
 */
@Data
public abstract class HardwareInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 硬件信息收集状态
     */
    private OsInfoStatusEnum status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 硬件类型名称
     */
    private String typeName;

    /**
     * 上次更新时间
     */
    private Long lastUpdateTime;

    /**
     * 硬件品牌
     */
    private String brand;

    /**
     * 硬件型号
     */
    private String model;

    /**
     * 制造商
     */
    private String manufacturer;

    /**
     * 序列号
     */
    private String serialNumber;

    /**
     * 硬件描述
     */
    private String description;
}