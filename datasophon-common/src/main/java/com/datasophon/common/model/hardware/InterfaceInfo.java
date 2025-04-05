package com.datasophon.common.model.hardware;

import lombok.Data;

/**
 * 网络接口信息类
 * 存储主机每个网络接口的详细信息
 */
@Data
public class InterfaceInfo {

    /**
     * 接口名称
     */
    private String name;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * MAC地址
     */
    private String macAddress;

    /**
     * 接口状态（up/down）
     */
    private String status;

    /**
     * 接收流量（字节）
     */
    private Long rxBytes;

    /**
     * 发送流量（字节）
     */
    private Long txBytes;

    /**
     * 接收流量（可读格式）
     */
    private String rxTraffic;

    /**
     * 发送流量（可读格式）
     */
    private String txTraffic;

    /**
     * 接口速度（如1000Mb/s）
     */
    private String speed;

    /**
     * 接口类型（如以太网）
     */
    private String type;

    /**
     * 是否为默认网关接口
     */
    private Boolean isDefault;

    public InterfaceInfo() {
        this.isDefault = false;
    }
}