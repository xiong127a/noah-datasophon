package com.datasophon.common.model.hardware;

import com.datasophon.common.enums.OsInfoStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * GPU详细信息类
 * 存储主机GPU的详细规格和状态信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GpuInfo extends HardwareInfo {
    private static final long serialVersionUID = 1L;

    /**
     * GPU型号
     */
    private String model;

    /**
     * GPU信息
     */
    private String info;

    /**
     * GPU类型（独立显卡、集成显卡等）
     */
    private String type;

    /**
     * 制造商
     */
    private String vendor;

    /**
     * 驱动版本
     */
    private String driverVersion;

    /**
     * 设备数量
     */
    private Integer deviceCount;

    /**
     * GPU单个设备列表
     */
    private List<GpuDevice> devices;

    /**
     * 总显存大小(MB)
     */
    private Double totalMemory;

    /**
     * 已使用显存大小(MB)
     */
    private Double usedMemory;

    /**
     * 空闲显存大小(MB)
     */
    private Double freeMemory;

    /**
     * 显存使用率(%)
     */
    private Double memoryUsagePercent;

    /**
     * GPU温度(℃)
     */
    private Double temperature;

    /**
     * GPU使用率(%)
     */
    private Double utilization;

    public GpuInfo() {
        setTypeName("显卡");
    }

    /**
     * 获取GPU信息字符串
     * 兼容旧代码中的getGpuInfo调用
     * 
     * @return GPU信息字符串
     */
    public String getInfo() {
        return this.info != null ? this.info : (this.model != null ? this.model : "未知GPU");
    }

    /**
     * 设置GPU信息字符串
     * 兼容旧代码中的setGpuInfo调用
     * 
     * @param info GPU信息字符串
     */
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * 获取字符串表示形式
     */
    @Override
    public String toString() {
        if (info != null) {
            return info;
        } else if (model != null) {
            return model;
        } else if (type != null) {
            return type;
        }
        return "GPU";
    }

    /**
     * GPU设备信息类
     */
    @Data
    public static class GpuDevice implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 设备ID
         */
        private String id;

        /**
         * 设备名称
         */
        private String name;

        /**
         * 设备型号
         */
        private String model;

        /**
         * 设备总显存(MB)
         */
        private Double totalMemory;

        /**
         * 已使用显存(MB)
         */
        private Double usedMemory;

        /**
         * 显存使用率(%)
         */
        private Double memoryUsagePercent;

        /**
         * GPU使用率(%)
         */
        private Double usagePercent;

        /**
         * 温度(℃)
         */
        private Double temperature;

        /**
         * 功耗(W)
         */
        private Double powerUsage;
    }

    /**
     * 设置显存大小（GB）
     * 兼容旧API中的memorySize
     */
    public void setMemorySize(Double size) {
        this.totalMemory = size;
    }

    /**
     * 获取显存大小（GB）
     * 兼容旧API中的memorySize
     */
    public Double getMemorySize() {
        return this.totalMemory;
    }
}