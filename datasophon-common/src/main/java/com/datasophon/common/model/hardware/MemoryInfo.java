package com.datasophon.common.model.hardware;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 内存详细信息类
 * 存储主机内存的详细信息和使用状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemoryInfo extends HardwareInfo {
    private static final long serialVersionUID = 1L;

    /**
     * 总内存大小(MB)
     */
    private Long totalMemory;

    /**
     * 已使用内存大小(MB)
     */
    private Long usedMemory;

    /**
     * 可用内存大小(MB)
     */
    private Long availableMemory;

    /**
     * 内存使用率(%)
     */
    private Double usagePercent;

    /**
     * 内存频率(MHz)
     */
    private Integer frequency;

    /**
     * 内存类型(DDR4/DDR5等)
     */
    private String memoryType;

    /**
     * 内存插槽总数
     */
    private Integer totalSlots;

    /**
     * 已使用内存插槽数
     */
    private Integer usedSlots;

    /**
     * 单条内存大小(MB)
     */
    private Long singleSize;

    /**
     * 内存条信息列表
     */
    private List<MemoryModule> modules;

    public MemoryInfo() {
        setTypeName("内存");
    }

    /**
     * 内存条信息类
     */
    @Data
    public static class MemoryModule implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 插槽编号
         */
        private String slotNumber;

        /**
         * 内存大小(MB)
         */
        private Long capacity;

        /**
         * 内存频率(MHz)
         */
        private Integer frequency;

        /**
         * 内存类型(DDR4/DDR5等)
         */
        private String memoryType;

        /**
         * 制造商
         */
        private String manufacturer;

        /**
         * 型号
         */
        private String model;

        /**
         * 序列号
         */
        private String serialNumber;

        /**
         * 是否支持ECC
         */
        private Boolean eccSupported;

        /**
         * 内存条描述信息
         */
        private String description;
    }
}