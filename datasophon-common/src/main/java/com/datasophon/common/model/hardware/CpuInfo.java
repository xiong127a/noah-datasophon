package com.datasophon.common.model.hardware;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * CPU信息类
 * @author 63588
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CpuInfo extends HardwareInfo implements Serializable  {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * CPU型号
     */
    private String model;

    /**
     * CPU核心数（物理核心数）
     */
    private Integer cores;

    /**
     * CPU逻辑核心数
     */
    private Integer logicalCores;

    /**
     * 物理处理器数量
     */
    private Integer physicalCount;

    /**
     * CPU使用率
     */
    private Double usagePercent;

    /**
     * 1分钟平均负载
     */
    private Double load1Min;

    /**
     * 5分钟平均负载
     */
    private Double load5Min;

    /**
     * 15分钟平均负载
     */
    private Double load15Min;

    /**
     * CPU频率(GHz)
     */
    private Double frequency;

    /**
     * 每核心线程数
     */
    private Integer threadsPerCore;

    /**
     * CPU原始信息
     */
    private String rawInfo;
    /**
     * 错误信息
     */
    private String errorMessage;
}