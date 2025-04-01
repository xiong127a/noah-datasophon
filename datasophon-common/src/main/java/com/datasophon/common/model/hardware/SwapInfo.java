package com.datasophon.common.model.hardware;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 交换空间详细信息类
 * 存储主机交换空间的详细信息和使用状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SwapInfo extends HardwareInfo {
    private static final long serialVersionUID = 1L;

    /**
     * 总交换空间大小(MB)
     */
    private Long totalSwap;

    /**
     * 已使用交换空间大小(MB)
     */
    private Long usedSwap;

    /**
     * 可用交换空间大小(MB)
     */
    private Long availableSwap;

    /**
     * 交换空间使用率(%)
     */
    private Double usagePercent;

    /**
     * 交换空间是否启用
     */
    private Boolean enabled;

    public SwapInfo() {
        setTypeName("交换空间");
    }
}