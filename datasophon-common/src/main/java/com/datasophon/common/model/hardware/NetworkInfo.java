package com.datasophon.common.model.hardware;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 网络接口详细信息类
 * 存储主机网络接口的详细信息和状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NetworkInfo extends HardwareInfo {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 网络接口列表
     */
    private List<InterfaceInfo> interfaces;

    /**
     * 活动连接数
     */
    private Integer activeConnections;

    /**
     * 默认网关IP地址
     */
    private String defaultGateway;

    /**
     * DNS服务器列表
     */
    private List<String> dnsServers;

    /**
     * 原始IP信息
     */
    private String rawIpInfo;

    /**
     * 原始路由信息
     */
    private String rawRouteInfo;

    /**
     * 原始DNS信息
     */
    private String rawDnsInfo;

    public NetworkInfo() {
        setTypeName("网络");
    }

    /**
     * 网络接口信息类
     */
    @Data
    public static class NetworkInterface implements java.io.Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 接口名称
         */
        private String name;

        /**
         * 接口MAC地址
         */
        private String macAddress;

        /**
         * IPv4地址
         */
        private String ipv4Address;

        /**
         * IPv4子网掩码(CIDR格式)
         */
        private String ipv4Subnet;

        /**
         * IPv6地址
         */
        private String ipv6Address;

        /**
         * IPv6子网掩码(CIDR格式)
         */
        private String ipv6Subnet;

        /**
         * 是否启用
         */
        private Boolean enabled;

        /**
         * 接口速度(Mbps)
         */
        private Long speed;

        /**
         * 接口类型
         */
        private String type;

        /**
         * 接口型号
         */
        private String model;

        /**
         * 已发送字节数
         */
        private Long bytesSent;

        /**
         * 已接收字节数
         */
        private Long bytesReceived;

        /**
         * 已发送数据包数
         */
        private Long packetsSent;

        /**
         * 已接收数据包数
         */
        private Long packetsReceived;

        /**
         * 是否为虚拟接口
         */
        private Boolean virtual;

        /**
         * 驱动程序名称
         */
        private String driver;
    }
}