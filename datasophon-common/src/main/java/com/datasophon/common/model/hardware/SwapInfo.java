package com.datasophon.common.model.hardware;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 交换空间信息类
 * 存储主机交换空间的详细信息和状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SwapInfo extends HardwareInfo {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否启用交换空间
     */
    private Boolean enabled;

    /**
     * 总交换空间（字节）
     */
    private Long totalSwap;

    /**
     * 已使用交换空间（字节）
     */
    private Long usedSwap;

    /**
     * 可用交换空间（字节）
     */
    private Long availableSwap;

    /**
     * 使用率百分比
     */
    private Double usagePercent;

    /**
     * 总交换空间（GB）- 带单位
     */
    private String totalSwapGB;

    /**
     * 已使用交换空间（GB）- 带单位
     */
    private String usedSwapGB;

    /**
     * 可用交换空间（GB）- 带单位
     */
    private String freeSwapGB;

    /**
     * 总交换空间（MB）- 带单位
     */
    private String totalSwapMB;

    /**
     * 已使用交换空间（MB）- 带单位
     */
    private String usedSwapMB;

    /**
     * 可用交换空间（MB）- 带单位
     */
    private String freeSwapMB;

    /**
     * 格式化后的总交换空间值
     */
    private String totalSwapFormatted;

    /**
     * 总交换空间单位
     */
    private String totalSwapUnit;

    /**
     * 格式化后的可用交换空间值
     */
    private String availableSwapFormatted;

    /**
     * 可用交换空间单位
     */
    private String availableSwapUnit;

    /**
     * 格式化后的已用交换空间值
     */
    private String usedSwapFormatted;

    /**
     * 已用交换空间单位
     */
    private String usedSwapUnit;

    public SwapInfo() {
        setTypeName("交换空间");
    }
}