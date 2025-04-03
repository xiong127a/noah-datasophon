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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

import com.datasophon.common.Constants;
import com.google.common.net.InetAddresses;

/**
 * 读取hosts文件
 *
 * @author gaodayu
 */
public enum HostUtils {
    ;

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
        String ip = getIp(hostname);
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

                return 0; // 相等的情况
            } catch (Exception e) {
                // 处理可能的异常情况（无效IP格式等）
                return ip1.compareTo(ip2); // 使用字符串比较作为后备方案
            }
        });

        return sortedIps;
    }
}
