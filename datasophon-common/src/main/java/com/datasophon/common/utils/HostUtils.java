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

import static com.datasophon.common.Constants.DATASOPHON;

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

    //判断服务在线
    public static boolean isServiceOnline(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;  // 如果能够连接上，说明服务在线
        } catch (IOException e) {
            e.printStackTrace();
            return false;  // 如果连接失败，认为服务不在线
        }
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

    public static List<String> generateHosts(List<String> host, String serviceRoleFullName) {
        return IntStream.range(0, host.size()).mapToObj(i -> serviceRoleFullName + "-" + i).collect(Collectors.toList());
    }
}
