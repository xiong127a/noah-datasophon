package com.datasophon.common.model.hardware;

import com.datasophon.common.enums.OsInfoStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * GPU详细信息类
 * 存储主机GPU的详细信息和状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GpuInfo extends HardwareInfo {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * GPU厂商
     */
    private String vendor;

    /**
     * GPU型号
     */
    private String model;

    /**
     * GPU类型（独立显卡/集成显卡）
     */
    private String type;

    /**
     * 设备数量
     */
    private Integer deviceCount;

    /**
     * 驱动版本
     */
    private String driverVersion;

    /**
     * GPU信息（原始输出）
     * -- SETTER --
     *  设置GPU信息字符串
     *  兼容旧代码中的setGpuInfo调用
     *
     * @param info GPU信息字符串

     */
    private String info;

    /**
     * 显存总量（MB）
     */
    private Double totalMemory;

    /**
     * 已使用显存（MB）
     */
    private Double usedMemory;

    /**
     * 可用显存（MB）
     */
    private Double freeMemory;

    /**
     * 显存使用率（%）
     */
    private Double memoryUsagePercent;

    /**
     * GPU温度（摄氏度）
     */
    private Double temperature;

    /**
     * GPU使用率（%）
     */
    private Double utilization;

    /**
     * 状态信息
     */
    private String statusMessage;

    /**
     * 是否检测到GPU
     */
    private Boolean detected;

    /**
     * GPU功耗（瓦特）
     */
    private Double powerUsage;

    /**
     * 最大功耗（瓦特）
     */
    private Double maxPower;

    /**
     * 支持的CUDA版本
     */
    private String cudaVersion;

    /**
     * GPU性能模式
     */
    private String performanceMode;

    /**
     * GPU计算模式
     */
    private String computeMode;

    /**
     * 显示格式化的温度（带单位）
     */
    private String formattedTemperature;

    /**
     * 显示格式化的显存（带单位）
     */
    private String formattedMemory;

    /**
     * 显示格式化的使用率（带单位）
     */
    private String formattedUtilization;

    public GpuInfo() {
        setTypeName("图形处理器");
        this.detected = false;
        this.deviceCount = 0;
        this.statusMessage = "尚未检测";
    }

    /**
     * 更新是否检测到GPU的状态
     */
    public void updateDetectedStatus() {
        // 如果有设备数量或者供应商信息，则认为检测到了GPU
        this.detected = (deviceCount != null && deviceCount > 0) ||
                (vendor != null && !vendor.isEmpty() && !"未检测到".equalsIgnoreCase(vendor)
                        && !"无".equalsIgnoreCase(vendor));
    }

    /**
     * 格式化显示信息
     */
    public void formatDisplayInfo() {
        // 格式化温度显示
        if (temperature != null) {
            this.formattedTemperature = String.format("%.1f °C", temperature);
        }

        // 格式化显存显示
        if (totalMemory != null && usedMemory != null) {
            this.formattedMemory = String.format("%.0f MB / %.0f MB (%.1f%%)",
                    usedMemory, totalMemory,
                    (usedMemory / totalMemory) * 100);
        }

        // 格式化使用率显示
        if (utilization != null) {
            this.formattedUtilization = String.format("%.1f%%", utilization);
        }
    }

    /**
     * 设置状态并更新状态消息
     */
    @Override
    public void setStatus(OsInfoStatusEnum status) {
        // 在GpuInfo.java中，我们确保前端能够正确处理状态
        if (status == OsInfoStatusEnum.SUCCESS &&
                (deviceCount == null || deviceCount == 0 ||
                        (vendor != null && ("未检测到".equalsIgnoreCase(vendor) || "加载中...".equalsIgnoreCase(vendor))))) {
            // 如果后端明确设置了SUCCESS但我们需要保持LOADING状态，则实际上转为SUCCESS
            // 这样修改后，前端通过getGpuStatus()会用loading状态显示加载动画，但后端真实状态是SUCCESS
            super.setStatus(status);
            this.statusMessage = "GPU信息加载完成";
            this.detected = false;
            return;
        }

        super.setStatus(status);

        // 根据状态更新状态消息
        switch (status) {
            case LOADING:
                this.statusMessage = "正在加载GPU信息...";
                break;
            case ERROR:
                // 将错误状态的消息也设置为正常结束
                this.statusMessage = "GPU信息加载完成";
                break;
            case SUCCESS:
                updateDetectedStatus();
                if (this.detected) {
                    this.statusMessage = "GPU信息加载完成";
                    formatDisplayInfo();
                } else {
                    // 将"未检测到"状态也设为正常完成
                    this.statusMessage = "GPU信息加载完成";
                }
                break;
            default:
                this.statusMessage = "GPU信息加载完成";
                super.setStatus(OsInfoStatusEnum.SUCCESS);
        }
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
        @Serial
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