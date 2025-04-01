package com.datasophon.common.model;

import com.datasophon.common.model.hardware.NetworkInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 旧版OsInfo兼容类
 * 提供旧版内部类定义和转换方法
 */
public class OsInfoLegacy {

    /**
     * 网卡信息内部类
     * 用于兼容旧版API
     */
    @Data
    public static class NetworkInterface implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name; // 网卡名称
        private boolean up; // 是否启用
        private String status; // 网卡状态描述
        private String mac; // MAC地址
        private String ipv4; // IPv4地址
        private String ipv6; // IPv6地址
        private String netmask; // 子网掩码
        private String model; // 网卡型号
        private Long speed; // 网卡速率(bps)
        private NetworkStats stats; // 网卡统计信息

        @Data
        public static class NetworkStats implements Serializable {
            private static final long serialVersionUID = 1L;

            private Long txBytes; // 发送字节数
            private Long rxBytes; // 接收字节数
        }
    }

    /**
     * 将新版网卡信息转换为旧版格式
     * 
     * @param newInterfaces 新格式网卡列表
     * @return 旧格式网卡列表
     */
    public static List<NetworkInterface> convertNetworkInterfaces(List<NetworkInfo.NetworkInterface> newInterfaces) {
        if (newInterfaces == null) {
            return null;
        }

        List<NetworkInterface> oldInterfaces = new ArrayList<>();
        for (NetworkInfo.NetworkInterface newInterface : newInterfaces) {
            NetworkInterface oldInterface = new NetworkInterface();
            oldInterface.setName(newInterface.getName());
            oldInterface.setUp(newInterface.getEnabled() != null ? newInterface.getEnabled() : false);
            oldInterface.setModel(newInterface.getModel());
            oldInterface.setSpeed(newInterface.getSpeed());
            oldInterface.setIpv4(newInterface.getIpv4Address());
            oldInterface.setIpv6(newInterface.getIpv6Address());
            oldInterface.setNetmask(newInterface.getIpv4Subnet());
            oldInterface.setMac(newInterface.getMacAddress());

            NetworkInterface.NetworkStats stats = new NetworkInterface.NetworkStats();
            stats.setTxBytes(newInterface.getBytesSent());
            stats.setRxBytes(newInterface.getBytesReceived());
            oldInterface.setStats(stats);

            oldInterfaces.add(oldInterface);
        }
        return oldInterfaces;
    }

    /**
     * 将旧版网卡信息转换为新版格式
     * 
     * @param oldInterfaces 旧格式网卡列表
     * @return 新格式网卡列表
     */
    public static List<NetworkInfo.NetworkInterface> convertToNewNetworkInterfaces(
            List<NetworkInterface> oldInterfaces) {
        if (oldInterfaces == null) {
            return null;
        }

        List<NetworkInfo.NetworkInterface> newInterfaces = new ArrayList<>();
        for (NetworkInterface oldInterface : oldInterfaces) {
            NetworkInfo.NetworkInterface newInterface = new NetworkInfo.NetworkInterface();
            newInterface.setName(oldInterface.getName());
            newInterface.setEnabled(oldInterface.isUp());
            newInterface.setModel(oldInterface.getModel());
            newInterface.setSpeed(oldInterface.getSpeed());
            newInterface.setIpv4Address(oldInterface.getIpv4());
            newInterface.setIpv6Address(oldInterface.getIpv6());
            newInterface.setIpv4Subnet(oldInterface.getNetmask());
            newInterface.setMacAddress(oldInterface.getMac());

            if (oldInterface.getStats() != null) {
                newInterface.setBytesSent(oldInterface.getStats().getTxBytes());
                newInterface.setBytesReceived(oldInterface.getStats().getRxBytes());
            }

            newInterfaces.add(newInterface);
        }
        return newInterfaces;
    }
}