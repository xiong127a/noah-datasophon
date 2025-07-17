/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.common.utils;

import cn.hutool.core.net.NetUtil;
import com.datasophon.common.Constants;
import com.google.common.net.InetAddresses;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;



/**
 * 读取hosts文件
 *
 * @author gaodayu
 */
@Slf4j
@UtilityClass
public class HostUtils {

    public static final Pattern HOST_NAME_STR = Pattern.compile("[0-9a-zA-Z-.]{1,64}");

    public static boolean checkIP(String ipStr) {
        return InetAddresses.isInetAddress(ipStr);
    }

    private static void checkIPThrow(String ipStr, Map<String, String> ipHost) {
        if (!checkIP(ipStr)) {
            throw new RuntimeException("Invalid IP in file /etc/hosts, IP：" + ipStr);
        }
        if (ipHost.containsKey(ipStr)) {
            throw new RuntimeException("Duplicate ip in file /etc/hosts, IP：" + ipStr);
        }
    }

    public static boolean checkHostname(String hostname) {
        if (!HOST_NAME_STR.matcher(hostname).matches()) {
            return false;
        }
        return !hostname.startsWith("-") && !hostname.endsWith("-");
    }

    private static void validHostname(String hostname) {
        if (!checkHostname(hostname)) {
            throw new RuntimeException("Invalid hostname in file /etc/hosts, hostname：" + hostname);
        }
    }

    public static String findIp(String hostname) {
        validHostname(hostname);
        String ip = getIpByHost(hostname);
        return ip;
    }

    public static String getHostName(String hostOrIp) {
        try {
            InetAddress byName = InetAddress.getByName(hostOrIp);
            return byName.getCanonicalHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getIp(String hostName) {
        try {
            InetAddress byName = InetAddress.getByName(hostName);
            return byName.getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 根据主机名获取IP地址
     * 此方法通过查询主机名来获取对应的IP地址为了确保获取的是最新的IP地址信息，
     * 方法在查询前设置JVM的系统属性，以禁用网络地址缓存
     *
     * @param hostName 主机名，用于查询对应的IP地址
     * @return 返回查询到的IP地址如果查询失败或无法解析主机名，则返回空字符串
     */
    public static String getIpByHost(String hostName) {
        // 设置系统属性以禁用负缓存，以便立即尝试重新解析最近查询失败的主机
        System.setProperty("networkaddress.cache.negative.ttl", "0");
        // 设置系统属性以禁用正缓存，确保每次查询都直接对网络进行请求，获取最新的IP地址信息
        System.setProperty("networkaddress.cache.ttl", "0");
        Security.setProperty("networkaddress.cache.negative.ttl", "0");
        return NetUtil.getIpByHost(hostName);
    }

    public static List<String> GetMasterHost() {
        String[] array = PropertyUtils.getArray(Constants.MASTER_HOST, ",");
        return Arrays.asList(array);
    }

    /**
     * 对IP地址进行统一排序
     * 按照IP地址的四个段，依次比较数值大小
     *
     * 注意：此方法是所有IP排序的标准方法，在多个地方共享使用，包括：
     * 1. startHostCheck - 开始检查任务时的IP排序
     * 2. analysisHostList - 解析主机列表时的IP排序
     * 确保所有地方使用相同的排序逻辑保持一致性
     *
     * @param ips 需要排序的IP地址列表
     * @return 排序后的IP地址列表
     */
    public static List<String> sortIpAddresses(List<String> ips) {
        if (ips == null || ips.isEmpty()) {
            return ips;
        }

        // 创建副本，避免修改原始集合
        List<String> sortedIps = new ArrayList<>(ips);

        // 按照IP地址进行排序
        sortedIps.sort((ip1, ip2) -> {
            try {
                // 将IP地址解析为整数数组进行比较
                String[] parts1 = ip1.split("\\.");
                String[] parts2 = ip2.split("\\.");

                // 比较每一段IP地址
                for (int i = 0; i < 4; i++) {
                    int num1 = Integer.parseInt(parts1[i]);
                    int num2 = Integer.parseInt(parts2[i]);
                    if (num1 != num2) {
                        return num1 - num2;
                    }
                }
    //判断服务在线
    public static boolean isServiceOnline(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;  // 如果能够连接上，说明服务在线
        } catch (IOException e) {
            e.printStackTrace();
            return false;  // 如果连接失败，认为服务不在线
        }
    }


                return 0; // 相等的情况
            } catch (Exception e) {
                // 处理可能的异常情况（无效IP格式等）
                return ip1.compareTo(ip2); // 使用字符串比较作为后备方案
            }
        });

        return sortedIps;
    }
    public static boolean checkServiceOnlineWithRetry(String host, int port, int retries, long waitTime) {
        int attempts = 0;
        while (attempts < retries) {
            // 如果不是第一次尝试，等待一段时间
            if (attempts > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    // 恢复中断状态
                    Thread.currentThread().interrupt();
                    System.err.println("线程被中断，退出重试。");
                    return false;
                }
            }

            // 检查服务是否在线
            if (isServiceOnline(host, port)) {
                System.out.println("端口 " + port + " 已启动!");
                return true;
            }

            attempts++;
        }

        // 达到最大重试次数仍未成功
        System.out.println("端口 " + port + " 未启动，已达到最大重试次数 " + retries);
        return false;
    }

    public static List<String> generateDnsName(List<String> host, String serviceRoleFullName,String namespace) {
        return IntStream.range(0, host.size()).mapToObj(i -> serviceRoleFullName + "-" + i + "." + serviceRoleFullName + "." + namespace +".svc.cluster.local").collect(Collectors.toList());
    }
}
