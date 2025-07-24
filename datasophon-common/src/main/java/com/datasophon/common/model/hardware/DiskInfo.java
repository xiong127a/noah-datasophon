package com.datasophon.common.model.hardware;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 磁盘详细信息类
 * 存储主机磁盘的总体状态和分区信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DiskInfo extends HardwareInfo {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总磁盘空间(GB)
     */
    private Double totalDiskSpace;

    /**
     * 已使用磁盘空间(GB)
     */
    private Double usedDiskSpace;

    /**
     * 可用磁盘空间(GB)
     */
    private Double availableDiskSpace;

    /**
     * 磁盘使用率(%)
     */
    private Double usagePercent;

    /**
     * 磁盘总数
     */
    private Integer diskCount;

    /**
     * 磁盘类型(SSD/HDD等)
     */
    private String diskType;

    /**
     * 磁盘接口类型(SATA/NVMe等)
     */
    private String interfaceType;

    /**
     * 磁盘读取速度(MB/s)
     */
    private Double readSpeed;

    /**
     * 磁盘写入速度(MB/s)
     */
    private Double writeSpeed;

    /**
     * 总磁盘空间格式化显示（如：128.50 GB）
     */
    private String totalDiskSpaceFormatted;

    /**
     * 已使用磁盘空间格式化显示（如：64.25 GB）
     */
    private String usedDiskSpaceFormatted;

    /**
     * 可用磁盘空间格式化显示（如：64.25 GB）
     */
    private String availableDiskSpaceFormatted;

    /**
     * 物理磁盘信息列表
     */
    private List<PhysicalDisk> physicalDisks;

    /**
     * 磁盘分区信息列表
     */
    private List<DiskPartition> partitions;

    public DiskInfo() {
        setTypeName("磁盘");
    }

    /**
     * 物理磁盘信息类
     */
    @Data
    public static class PhysicalDisk implements java.io.Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 磁盘名称
         */
        private String name;

        /**
         * 磁盘型号
         */
        private String model;

        /**
         * 磁盘序列号
         */
        private String serialNumber;

        /**
         * 磁盘容量(GB)
         */
        private Double capacity;

        /**
         * 磁盘类型(SSD/HDD等)
         */
        private String type;

        /**
         * 磁盘接口类型(SATA/NVMe等)
         */
        private String interfaceType;

        /**
         * 转速(HDD专用, RPM)
         */
        private Integer rpm;

        /**
         * 读取速度(MB/s)
         */
        private Double readSpeed;

        /**
         * 写入速度(MB/s)
         */
        private Double writeSpeed;

        /**
         * 温度(℃)
         */
        private Double temperature;

        /**
         * 健康状态(%)
         */
        private Integer health;

        /**
         * 已读取数据总量(GB)
         */
        private Double totalRead;

        /**
         * 已写入数据总量(GB)
         */
        private Double totalWrite;

        /**
         * 通电时间(小时)
         */
        private Long powerOnHours;
    }

    /**
     * 磁盘分区信息类
     */
    @Data
    public static class DiskPartition implements java.io.Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 分区名称
         */
        private String name;

        /**
         * 分区挂载点
         */
        private String mountPoint;

        /**
         * 分区文件系统类型
         */
        private String fsType;

        /**
         * 分区总容量(GB)
         */
        private Double totalSpace;

        /**
         * 已用空间(GB)
         */
        private Double usedSpace;

        /**
         * 可用空间(GB)
         */
        private Double availableSpace;

        /**
         * 使用率(%)
         */
        private Double usagePercent;

        /**
         * 所属物理磁盘
         */
        private String physicalDiskName;

        /**
         * 分区UUID
         */
        private String uuid;

        /**
         * 分区标签
         */
        private String label;

        /**
         * 是否为只读
         */
        private Boolean readOnly;
    }
}