package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import com.datasophon.common.model.hardware.CpuInfo;
import com.datasophon.common.model.hardware.DiskInfo;
import com.datasophon.common.model.hardware.GpuInfo;
import com.datasophon.common.model.hardware.HardwareInfo;
import com.datasophon.common.model.hardware.InterfaceInfo;
import com.datasophon.common.model.hardware.MemoryInfo;
import com.datasophon.common.model.hardware.NetworkInfo;
import com.datasophon.common.model.hardware.SwapInfo;
import com.datasophon.common.model.hardware.DnsInfo;
import com.datasophon.common.utils.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;
import java.util.function.Function;
import java.text.DecimalFormat;

/**
 * Linux系统信息收集器
 */
@Component
public class LinuxOsInfoCollector implements IOsInfoCollector {

    private static final Logger logger = LoggerFactory.getLogger(LinuxOsInfoCollector.class);

    @Override
    public String getSupportedOsType() {
        return "linux";
    }

    @Override
    public OsInfo collectOsInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater) {
        try {
            if (hostInfo != null) {
                // 设置初始状态为加载中
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
                if (cacheUpdater != null) {
                    cacheUpdater.updateCache(hostInfo);
                }
            }

            // 首先尝试读取特定的系统release文件
            final boolean[] foundReleaseFile = { false }; // 使用数组作为可变引用

            // 检查麒麟系统文件
            executeCommandAndUpdateCache(
                    session,
                    "cat /etc/kylin-release 2>/dev/null || echo 'Not Found'",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        if (!output.contains("Not Found")) {
                            // 直接使用文件内容作为操作系统全称
                            osInfo.setFullName(output.trim());
                            osInfo.setDistribution("Kylin");

                            // 提取版本号（如果有）
                            Pattern versionPattern = Pattern.compile("release\\s+([0-9.]+)");
                            Matcher versionMatcher = versionPattern.matcher(output);
                            if (versionMatcher.find()) {
                                String version = versionMatcher.group(1);
                                osInfo.setVersion(version);
                                osInfo.setVersionId(version);
                            }
                            foundReleaseFile[0] = true;
                        }
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 如果没有麒麟系统文件，检查redhat-release
            if (!foundReleaseFile[0]) {
                executeCommandAndUpdateCache(
                        session,
                        "cat /etc/redhat-release 2>/dev/null || echo 'Not Found'",
                        hostInfo,
                        osInfo,
                        cacheUpdater,
                        (output) -> {
                            if (!output.contains("Not Found")) {
                                // 直接使用文件内容作为操作系统全称
                                osInfo.setFullName(output.trim());

                                // 确定是CentOS还是RedHat
                                if (output.toLowerCase().contains("centos")) {
                                    osInfo.setDistribution("CentOS");
                                } else {
                                    osInfo.setDistribution("RedHat");
                                }

                                // 提取版本号（如果有）
                                Pattern versionPattern = Pattern.compile("release\\s+([0-9.]+)");
                                Matcher versionMatcher = versionPattern.matcher(output);
                                if (versionMatcher.find()) {
                                    String version = versionMatcher.group(1);
                                    osInfo.setVersion(version);
                                    osInfo.setVersionId(version);
                                }
                                foundReleaseFile[0] = true;
                            }
                            return null;
                        },
                        () -> {
                            if (hostInfo != null) {
                                hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                            }
                        });
            }

            // 如果没有获取到全名，再尝试读取/etc/os-release文件
            if (StringUtils.isBlank(osInfo.getFullName())) {
                executeCommandAndUpdateCache(
                        session,
                        "cat /etc/os-release 2>/dev/null",
                        hostInfo,
                        osInfo,
                        cacheUpdater,
                        (output) -> {
                            if (StringUtils.isNotBlank(output)) {
                                // 解析OS信息
                                parseOsRelease(osInfo, output);
                            }
                            return null;
                        },
                        () -> {
                            if (hostInfo != null) {
                                hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                            }
                        });
            }

            // 获取内核版本
            executeCommandAndUpdateCache(
                    session,
                    "uname -r",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        osInfo.setKernelVersion(output.trim());
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 获取架构
            executeCommandAndUpdateCache(
                    session,
                    "uname -m",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        osInfo.setArchitecture(output.trim());

                        // 更新发行版信息
                        updateDistributionInfo(osInfo);

                        // 标记系统信息有效
                        osInfo.setValid(true);

                        // 更新状态为成功
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 最终更新
            if (hostInfo != null && cacheUpdater != null) {
                hostInfo.setOsInfo(osInfo);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                cacheUpdater.updateCache(hostInfo);
            }

            return osInfo;
        } catch (Exception e) {
            logger.error("收集Linux操作系统信息失败: {}", e.getMessage(), e);
            if (hostInfo != null && cacheUpdater != null) {
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("收集Linux操作系统信息失败: " + e.getMessage());
                cacheUpdater.updateCache(hostInfo);
            }
            return osInfo;
        }
    }

    /**
     * 解析OS-Release文件内容
     */
    private void parseOsRelease(OsInfo osInfo, String osRelease) {
        try {
            // 从/etc/os-release中提取ID、NAME和PRETTY_NAME信息
            Pattern idPattern = Pattern.compile("^ID=\"?(.*?)\"?$", Pattern.MULTILINE);
            Pattern namePattern = Pattern.compile("^NAME=\"?(.*?)\"?$", Pattern.MULTILINE);
            Pattern versionPattern = Pattern.compile("^VERSION=\"?(.*?)\"?$", Pattern.MULTILINE);
            Pattern versionIdPattern = Pattern.compile("^VERSION_ID=\"?(.*?)\"?$", Pattern.MULTILINE);
            Pattern prettyNamePattern = Pattern.compile("^PRETTY_NAME=\"?(.*?)\"?$", Pattern.MULTILINE);

            // 提取ID (简要操作系统名称)
            Matcher idMatcher = idPattern.matcher(osRelease);
            if (idMatcher.find()) {
                String distroId = idMatcher.group(1).trim().toLowerCase();
                String capitalizedDistroId = distroId.substring(0, 1).toUpperCase() + distroId.substring(1);
                osInfo.setDistribution(capitalizedDistroId);
            }

            // 提取NAME(如果ID没有获取到)
            String name = "";
            Matcher nameMatcher = namePattern.matcher(osRelease);
            if (nameMatcher.find()) {
                name = nameMatcher.group(1).trim();
                if (StringUtils.isBlank(osInfo.getDistribution())) {
                    osInfo.setDistribution(name);
                }
            }

            // 提取VERSION
            String version = "";
            Matcher versionMatcher = versionPattern.matcher(osRelease);
            if (versionMatcher.find()) {
                version = versionMatcher.group(1).trim();
            }

            // 提取版本ID
            Matcher versionIdMatcher = versionIdPattern.matcher(osRelease);
            if (versionIdMatcher.find()) {
                String versionId = versionIdMatcher.group(1).trim();
                osInfo.setVersionId(versionId);
                osInfo.setVersion(versionId);
                // 如果VERSION字段为空，使用VERSION_ID
                if (StringUtils.isBlank(version)) {
                    version = versionId;
                }
            }

            // 设置全名(fullName)，优先使用NAME+VERSION组合
            if (StringUtils.isNotBlank(name)) {
                if (StringUtils.isNotBlank(version)) {
                    // NAME + VERSION组合作为全名
                    String fullOsName = name + " " + version;
                    osInfo.setFullName(fullOsName);
                } else {
                    // 如果没有版本号，只使用NAME
                    osInfo.setFullName(name);
                }
            } else {
                // 如果NAME为空，尝试使用PRETTY_NAME
                Matcher prettyNameMatcher = prettyNamePattern.matcher(osRelease);
                if (prettyNameMatcher.find()) {
                    String prettyName = prettyNameMatcher.group(1).trim();
                    osInfo.setFullName(prettyName);
                } else if (StringUtils.isNotBlank(osInfo.getDistribution())) {
                    // 如果没有PRETTY_NAME但有distribution
                    if (StringUtils.isNotBlank(osInfo.getVersion())) {
                        // 如果有version，拼接distribution和version
                        String fullOsName = osInfo.getDistribution() + " " + osInfo.getVersion();
                        osInfo.setFullName(fullOsName);
                    } else {
                        // 如果没有version，只使用distribution
                        osInfo.setFullName(osInfo.getDistribution());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("解析/etc/os-release时出错: {}", e.getMessage());
        }
    }

    /**
     * 检查其他Linux文件
     */
    private void checkAlternativeLinuxFiles(ClientSession session, OsInfo osInfo) {
        try {
            // 尝试读取版本信息文件
            String[] versionFiles = {
                    "/etc/kylin-release", // 银河麒麟
                    "/etc/redhat-release", // RHEL/CentOS
                    "/etc/lsb-release", // Ubuntu
                    "/etc/debian_version", // Debian
                    "/etc/SuSE-release", // SuSE
                    "/etc/system-release" // Amazon Linux
            };

            for (String versionFile : versionFiles) {
                String releaseInfo = MinaUtils.execCmdWithResult(session, "cat " + versionFile + " 2>/dev/null").trim();
                if (StringUtils.isNotBlank(releaseInfo)) {
                    // 直接使用文件内容作为操作系统全称
                    osInfo.setFullName(releaseInfo);

                    // 尝试从文件名获取简要操作系统名称
                    String filename = versionFile.substring(versionFile.lastIndexOf('/') + 1);
                    switch (filename) {
                        case "kylin-release":
                            osInfo.setDistribution("Kylin");
                            break;
                        case "redhat-release":
                            if (releaseInfo.toLowerCase().contains("centos")) {
                                osInfo.setDistribution("CentOS");
                            } else {
                                osInfo.setDistribution("RedHat");
                            }
                            break;
                        case "lsb-release":
                            osInfo.setDistribution("Ubuntu");
                            break;
                        case "debian_version":
                            osInfo.setDistribution("Debian");
                            break;
                        case "SuSE-release":
                            osInfo.setDistribution("SuSE");
                            break;
                        case "system-release":
                            if (releaseInfo.toLowerCase().contains("amazon")) {
                                osInfo.setDistribution("Amazon Linux");
                            }
                            break;
                        default:
                            // 使用文件名作为发行版名
                            osInfo.setDistribution(filename.replace("-release", "").replace("_version", ""));
                    }

                    // 提取版本号（如果有）
                    Pattern versionPattern = Pattern.compile("release\\s+([0-9.]+)");
                    Matcher versionMatcher = versionPattern.matcher(releaseInfo);
                    if (versionMatcher.find()) {
                        String version = versionMatcher.group(1);
                        osInfo.setVersion(version);
                        osInfo.setVersionId(version);
                    }

                    // 找到信息后就退出循环
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("检查其他Linux文件时出错: {}", e.getMessage());
        }
    }

    /**
     * 更新发行版信息
     */
    private void updateDistributionInfo(OsInfo osInfo) {
        // 确保fullName设置正确
        if (StringUtils.isBlank(osInfo.getFullName())) {
            if (StringUtils.isNotBlank(osInfo.getDistribution())) {
                // 拼接发行版和版本
                if (StringUtils.isNotBlank(osInfo.getVersion())) {
                    osInfo.setFullName(osInfo.getDistribution() + " " + osInfo.getVersion());
                } else {
                    osInfo.setFullName(osInfo.getDistribution());
                }
            } else {
                // 如果没有其他信息，使用通用名称
                osInfo.setFullName("Linux 操作系统");
                osInfo.setDistribution("Linux");
            }
        }

        // 如果没有设置简要名称，从全称中提取
        if (StringUtils.isBlank(osInfo.getDistribution()) && StringUtils.isNotBlank(osInfo.getFullName())) {
            String lowerFullName = osInfo.getFullName().toLowerCase();
            if (lowerFullName.contains("centos")) {
                osInfo.setDistribution("CentOS");
            } else if (lowerFullName.contains("ubuntu")) {
                osInfo.setDistribution("Ubuntu");
            } else if (lowerFullName.contains("debian")) {
                osInfo.setDistribution("Debian");
            } else if (lowerFullName.contains("red hat") || lowerFullName.contains("redhat")) {
                osInfo.setDistribution("RedHat");
            } else if (lowerFullName.contains("kylin")) {
                osInfo.setDistribution("Kylin");
            } else {
                // 使用通用名称
                osInfo.setDistribution("Linux");
            }
        }

        // 更新osDistribution枚举值
        osInfo.updateOsDistribution();
    }

    @Override
    public void collectHardwareInfo(OsInfo osInfo, ClientSession session, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集Linux硬件信息");

            // 将主机信息缓存更新器包装成一个临时的，以便传递给单独的收集方法
            final HostInfo hostInfoTemp;
            CacheUpdater tempUpdater = null;

            if (osInfo != null) {
                hostInfoTemp = new HostInfo();
                hostInfoTemp.setOsInfo(osInfo);
                hostInfoTemp.setIp("unknown");

                if (cacheUpdater != null) {
                    tempUpdater = (h) -> cacheUpdater.updateCache(hostInfoTemp);
                }
            } else {
                hostInfoTemp = null;
            }

            // 收集CPU信息
            collectCpuInfo(hostInfoTemp, session, osInfo, tempUpdater);

            // 收集内存信息
            collectMemoryInfo(hostInfoTemp, session, osInfo, tempUpdater);

            // 收集磁盘信息
            collectDiskInfo(hostInfoTemp, session, osInfo, tempUpdater);

            // 收集交换分区信息
            collectSwapInfo(hostInfoTemp, session, osInfo, tempUpdater);

            // 收集GPU信息
            collectGpuInfo(hostInfoTemp, session, osInfo, tempUpdater);

            // 收集网络信息
            collectNetworkInfo(hostInfoTemp, session, osInfo, tempUpdater);

            // 设置硬件收集状态为成功
            osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            if (cacheUpdater != null && hostInfoTemp != null) {
                cacheUpdater.updateCache(hostInfoTemp);
            }
        } catch (Exception e) {
            logger.error("收集Linux硬件信息失败: {}", e.getMessage(), e);
            // 设置硬件收集状态为错误
            osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
        }
    }

    private <T> void executeCommandAndUpdateCache(ClientSession session, String command, HostInfo hostInfo,
            OsInfo osInfo, CacheUpdater cacheUpdater, Function<String, T> resultProcessor, Runnable fieldStatus) {
        if (session != null) {
            try {
                CommandResult commandResult = MinaUtils.execCmdWithResultObject(session, command);
                if (commandResult.isSuccess()) {
                    String commandOutput = commandResult.getOutput();
                    if (resultProcessor != null && commandOutput != null) {
                        T processedResult = resultProcessor.apply(commandOutput);
                        if (fieldStatus != null) {
                            fieldStatus.run();
                        }
                    }
                } else {
                    logger.error("命令执行失败: {}, 错误: {}", command, commandResult.getError());
                }
            } catch (Exception e) {
                logger.error("执行命令时出错: " + command, e);
            } finally {
                if (cacheUpdater != null && hostInfo != null) {
                    cacheUpdater.updateCache(hostInfo);
                }
            }
        }
    }

    @Override
    public void collectSwapInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater) {
        logger.info("开始收集交换分区信息：{}", hostInfo != null ? hostInfo.getIp() : "未知");

        try {
            // 创建交换分区信息对象（无论是否存在交换空间，都创建这个对象）
            SwapInfo swapInfo = new SwapInfo();
            osInfo.setSwapInfo(swapInfo);

            // 设置状态为收集中
            swapInfo.setStatus(OsInfoStatusEnum.LOADING);
            osInfo.setSwapStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            boolean successWithAnyMethod = false;

            // 方法1: 先尝试使用free命令获取交换分区信息
            CommandResult swapResult = MinaUtils.execCmdWithResultObject(session, "free -b 2>/dev/null | grep -i Swap");
            if (swapResult.isSuccess() && swapResult.getOutput() != null && !swapResult.getOutput().isEmpty()) {
                successWithAnyMethod = processSwapFromFree(swapResult.getOutput(), swapInfo);
            }

            // 方法2: 如果free命令失败，尝试读取/proc/swaps
            if (!successWithAnyMethod) {
                logger.info("使用free命令获取交换分区信息失败，尝试从/proc/swaps读取");
                CommandResult procSwapsResult = MinaUtils.execCmdWithResultObject(session,
                        "cat /proc/swaps 2>/dev/null | grep -v Filename");
                if (procSwapsResult.isSuccess() && procSwapsResult.getOutput() != null
                        && !procSwapsResult.getOutput().isEmpty()) {
                    successWithAnyMethod = processSwapFromProcSwaps(procSwapsResult.getOutput(), swapInfo);
                }
            }

            // 方法3: 使用swapon -s命令
            if (!successWithAnyMethod) {
                logger.info("尝试使用swapon -s命令获取交换分区信息");
                CommandResult swaponResult = MinaUtils.execCmdWithResultObject(session,
                        "swapon -s 2>/dev/null | grep -v Filename");
                if (swaponResult.isSuccess() && swaponResult.getOutput() != null
                        && !swaponResult.getOutput().isEmpty()) {
                    successWithAnyMethod = processSwapFromSwapon(swaponResult.getOutput(), swapInfo);
                }
            }

            // 方法4: 使用vmstat获取交换分区信息
            if (!successWithAnyMethod) {
                logger.info("尝试使用vmstat命令获取交换分区信息");
                CommandResult vmstatResult = MinaUtils.execCmdWithResultObject(session,
                        "vmstat -s 2>/dev/null | grep -i swap");
                if (vmstatResult.isSuccess() && vmstatResult.getOutput() != null
                        && !vmstatResult.getOutput().isEmpty()) {
                    successWithAnyMethod = processSwapFromVmstat(vmstatResult.getOutput(), swapInfo);
                }
            }

            // 如果所有尝试都失败，设置为禁用状态
            if (!successWithAnyMethod) {
                logger.info("所有获取交换分区信息的尝试都失败，设置为禁用状态");
                swapInfo.setEnabled(false);
                swapInfo.setTotalSwap(0L);
                swapInfo.setUsedSwap(0L);
                swapInfo.setAvailableSwap(0L);
                swapInfo.setUsagePercent(0.0);

                // 设置UI显示字段
                swapInfo.setTotalSwapFormatted("0");
                swapInfo.setUsedSwapFormatted("0");
                swapInfo.setAvailableSwapFormatted("0");
                swapInfo.setTotalSwapUnit("GB");
                swapInfo.setUsedSwapUnit("GB");
                swapInfo.setAvailableSwapUnit("GB");
            }

            // 无论如何都设置为SUCCESS状态，避免永久loading
            swapInfo.setStatus(OsInfoStatusEnum.SUCCESS);
            osInfo.setSwapStatus(OsInfoStatusEnum.SUCCESS);

        } catch (Exception e) {
            logger.error("收集交换分区信息失败: {}", e.getMessage(), e);
            // 异常情况下也需要设置状态，避免永久loading
            SwapInfo swapInfo = new SwapInfo();
            swapInfo.setEnabled(false);
            swapInfo.setTotalSwap(0L);
            swapInfo.setUsedSwap(0L);
            swapInfo.setAvailableSwap(0L);
            swapInfo.setUsagePercent(0.0);

            // 设置UI显示字段
            swapInfo.setTotalSwapFormatted("0");
            swapInfo.setUsedSwapFormatted("0");
            swapInfo.setAvailableSwapFormatted("0");
            swapInfo.setTotalSwapUnit("GB");
            swapInfo.setUsedSwapUnit("GB");
            swapInfo.setAvailableSwapUnit("GB");

            swapInfo.setStatus(OsInfoStatusEnum.SUCCESS); // 即使失败也设为SUCCESS，避免loading
            osInfo.setSwapInfo(swapInfo);
            osInfo.setSwapStatus(OsInfoStatusEnum.SUCCESS);
        } finally {
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
    }

    /**
     * 处理free命令输出的交换分区信息
     */
    private boolean processSwapFromFree(String swapInfoStr, SwapInfo swapInfo) {
        try {
            String[] parts = swapInfoStr.split("\\s+");

            // 检查parts的长度是否合适（不同版本的free命令输出格式可能不同）
            if (parts.length >= 3) {
                // Swap: 总量 已用 空闲
                int startIndex = 1; // 默认从索引1开始

                // 检查第一个数值是否为数字，如果不是可能是标题
                if (!parts[1].matches("\\d+")) {
                    startIndex = 2; // 跳过标题
                }

                // 确保有足够的数组元素
                if (parts.length >= startIndex + 2) {
                    long totalBytes = Long.parseLong(parts[startIndex]);
                    long usedBytes = Long.parseLong(parts[startIndex + 1]);
                    long freeBytes = totalBytes - usedBytes; // 如果没有显式给出空闲值，则计算

                    if (parts.length > startIndex + 2) {
                        try {
                            freeBytes = Long.parseLong(parts[startIndex + 2]);
                        } catch (NumberFormatException e) {
                            logger.warn("解析空闲交换空间失败，使用计算值: {}", e.getMessage());
                        }
                    }

                    setSwapInfoValues(swapInfo, totalBytes, usedBytes, freeBytes);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("从free命令解析交换分区信息失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 处理/proc/swaps输出的交换分区信息
     */
    private boolean processSwapFromProcSwaps(String swapInfoStr, SwapInfo swapInfo) {
        try {
            String[] swapDevices = swapInfoStr.trim().split("\n");
            long totalBytes = 0;

            for (String swapDevice : swapDevices) {
                String[] deviceInfo = swapDevice.trim().split("\\s+");
                if (deviceInfo.length >= 3) {
                    try {
                        // 设备 类型 大小(KB) 已用(KB) 优先级
                        long sizeKB = Long.parseLong(deviceInfo[2]);
                        long usedKB = 0;

                        if (deviceInfo.length >= 4) {
                            try {
                                usedKB = Long.parseLong(deviceInfo[3]);
                            } catch (NumberFormatException e) {
                                logger.warn("解析已用交换空间失败: {}", e.getMessage());
                            }
                        }

                        totalBytes += sizeKB * 1024; // 转换为字节
                    } catch (NumberFormatException e) {
                        logger.warn("解析交换设备大小失败: {}", e.getMessage());
                    }
                }
            }

            if (totalBytes > 0) {
                // 使用默认值
                long usedBytes = 0;
                long freeBytes = totalBytes;

                setSwapInfoValues(swapInfo, totalBytes, usedBytes, freeBytes);
                return true;
            }
        } catch (Exception e) {
            logger.warn("从/proc/swaps解析交换分区信息失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 处理swapon -s命令输出的交换分区信息
     */
    private boolean processSwapFromSwapon(String swapInfoStr, SwapInfo swapInfo) {
        try {
            String[] swapDevices = swapInfoStr.trim().split("\n");
            long totalBytes = 0;

            for (String swapDevice : swapDevices) {
                String[] deviceInfo = swapDevice.trim().split("\\s+");
                if (deviceInfo.length >= 3) {
                    try {
                        // 设备 类型 大小 已用 优先级
                        // swapon输出通常是以KB为单位
                        long sizeKB = Long.parseLong(deviceInfo[2]);
                        totalBytes += sizeKB * 1024; // 转换为字节
                    } catch (NumberFormatException e) {
                        logger.warn("解析swapon输出的交换设备大小失败: {}", e.getMessage());
                    }
                }
            }

            if (totalBytes > 0) {
                // 使用默认值
                long usedBytes = 0;
                long freeBytes = totalBytes;

                setSwapInfoValues(swapInfo, totalBytes, usedBytes, freeBytes);
                return true;
            }
        } catch (Exception e) {
            logger.warn("从swapon命令解析交换分区信息失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 处理vmstat -s命令输出的交换分区信息
     */
    private boolean processSwapFromVmstat(String swapInfoStr, SwapInfo swapInfo) {
        try {
            long totalBytes = 0;
            long usedBytes = 0;

            // 解析总交换空间和已用交换空间
            Pattern totalPattern = Pattern.compile("(\\d+)\\s+[kK]?\\s*total\\s+swap");
            Pattern usedPattern = Pattern.compile("(\\d+)\\s+[kK]?\\s*used\\s+swap");

            Matcher totalMatcher = totalPattern.matcher(swapInfoStr);
            Matcher usedMatcher = usedPattern.matcher(swapInfoStr);

            if (totalMatcher.find()) {
                // vmstat通常以KB为单位
                totalBytes = Long.parseLong(totalMatcher.group(1)) * 1024;
            }

            if (usedMatcher.find()) {
                usedBytes = Long.parseLong(usedMatcher.group(1)) * 1024;
            }

            if (totalBytes > 0) {
                long freeBytes = totalBytes - usedBytes;

                setSwapInfoValues(swapInfo, totalBytes, usedBytes, freeBytes);
                return true;
            }
        } catch (Exception e) {
            logger.warn("从vmstat命令解析交换分区信息失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 设置交换分区信息的各个值
     */
    private void setSwapInfoValues(SwapInfo swapInfo, long totalBytes, long usedBytes, long freeBytes) {
        // 计算转换为GB和MB（保留两位小数）
        double totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0);
        double usedGB = usedBytes / (1024.0 * 1024.0 * 1024.0);
        double freeGB = freeBytes / (1024.0 * 1024.0 * 1024.0);

        double totalMB = totalBytes / (1024.0 * 1024.0);
        double usedMB = usedBytes / (1024.0 * 1024.0);
        double freeMB = freeBytes / (1024.0 * 1024.0);

        // 设置交换分区信息（字节和GB/MB）
        swapInfo.setTotalSwap(totalBytes);
        swapInfo.setUsedSwap(usedBytes);
        swapInfo.setAvailableSwap(freeBytes);
        swapInfo.setEnabled(totalBytes > 0); // 如果总容量大于0，说明启用了交换分区

        // 添加格式化的GB单位信息
        DecimalFormat df = new DecimalFormat("0.00");
        swapInfo.setTotalSwapGB(df.format(totalGB) + " GB");
        swapInfo.setUsedSwapGB(df.format(usedGB) + " GB");
        swapInfo.setFreeSwapGB(df.format(freeGB) + " GB");

        // 添加MB单位信息
        swapInfo.setTotalSwapMB(Math.round(totalMB) + " MB");
        swapInfo.setUsedSwapMB(Math.round(usedMB) + " MB");
        swapInfo.setFreeSwapMB(Math.round(freeMB) + " MB");

        // 设置交换空间UI显示格式
        swapInfo.setTotalSwapFormatted(df.format(totalGB));
        swapInfo.setUsedSwapFormatted(df.format(usedGB));
        swapInfo.setAvailableSwapFormatted(df.format(freeGB));
        swapInfo.setTotalSwapUnit("GB");
        swapInfo.setUsedSwapUnit("GB");
        swapInfo.setAvailableSwapUnit("GB");

        // 设置使用率百分比
        if (totalBytes > 0) {
            swapInfo.setUsagePercent((double) usedBytes / totalBytes * 100);
        } else {
            swapInfo.setUsagePercent(0.0);
        }

        logger.info("已收集交换分区信息: 总={} GB, 已用={} GB, 空闲={} GB",
                df.format(totalGB), df.format(usedGB), df.format(freeGB));
    }

    @Override
    public void collectCpuInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集CPU信息");

            // 设置正在收集状态
            osInfo.setCpuStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            // 创建CPU信息对象
            CpuInfo cpuInfo = new CpuInfo();
            osInfo.setCpuInfo(cpuInfo);

            // 获取CPU型号
            executeCommandAndUpdateCache(
                    session,
                    "cat /proc/cpuinfo | grep 'model name' | head -n 1 | cut -d':' -f2",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        cpuInfo.setModel(output.trim());
                        // 设置中间状态，表示部分信息已收集
                        cpuInfo.setStatus(OsInfoStatusEnum.LOADING);
                        osInfo.setCpuStatus(OsInfoStatusEnum.LOADING);
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 获取物理CPU个数
            executeCommandAndUpdateCache(
                    session,
                    "cat /proc/cpuinfo | grep 'physical id' | sort -u | wc -l",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        try {
                            cpuInfo.setPhysicalCount(Integer.parseInt(output.trim()));
                        } catch (NumberFormatException e) {
                            logger.warn("解析物理CPU个数失败: {}", output);
                            cpuInfo.setPhysicalCount(1); // 设置默认值
                        }
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 获取CPU核心数
            executeCommandAndUpdateCache(
                    session,
                    "cat /proc/cpuinfo | grep 'cpu cores' | head -n 1 | cut -d':' -f2",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        String cpuCores = output.trim();
                        if (cpuCores.isEmpty()) {
                            // 如果获取不到，使用nproc命令
                            try {
                                CommandResult nprocResult = MinaUtils.execCmdWithResultObject(session, "nproc");
                                if (nprocResult.isSuccess()) {
                                    cpuCores = nprocResult.getOutput().trim();
                                } else {
                                    logger.warn("获取CPU核心数失败: {}", nprocResult.getError());
                                }
                            } catch (Exception e) {
                                logger.warn("获取CPU核心数失败", e);
                            }
                        }

                        try {
                            cpuInfo.setCores(Integer.parseInt(cpuCores.isEmpty() ? "1" : cpuCores));
                        } catch (NumberFormatException e) {
                            logger.warn("解析CPU核心数失败: {}", cpuCores);
                            cpuInfo.setCores(1); // 设置默认值
                        }
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 获取CPU频率
            executeCommandAndUpdateCache(
                    session,
                    "cat /proc/cpuinfo | grep 'cpu MHz' | head -n 1 | cut -d':' -f2",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        String cpuFreq = output.trim();
                        if (!cpuFreq.isEmpty()) {
                            try {
                                double freqMHz = Double.parseDouble(cpuFreq);
                                // 转换为GHz并保留一位小数
                                cpuInfo.setFrequency(freqMHz / 1000.0);
                            } catch (NumberFormatException e) {
                                logger.warn("解析CPU频率失败: {}", cpuFreq);
                            }
                        }
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 存储CPU原始信息
            executeCommandAndUpdateCache(
                    session,
                    "cat /proc/cpuinfo | head -20",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        cpuInfo.setRawInfo(output.trim());

                        // 设置状态为成功
                        cpuInfo.setStatus(OsInfoStatusEnum.SUCCESS);
                        osInfo.setCpuStatus(OsInfoStatusEnum.SUCCESS);
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            logger.info("CPU信息收集完成");
        } catch (Exception e) {
            logger.error("收集CPU信息时出错: {}", e.getMessage(), e);
            osInfo.setCpuStatus(OsInfoStatusEnum.ERROR);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
    }

    @Override
    public void collectMemoryInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集内存信息");

            // 设置正在收集状态
            osInfo.setMemoryStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            // 创建内存信息对象
            MemoryInfo memoryInfo = new MemoryInfo();
            osInfo.setMemoryInfo(memoryInfo);

            // 获取内存信息 - 使用字节模式获取更准确的值
            executeCommandAndUpdateCache(
                    session,
                    "free -b | grep Mem:",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        try {
                            String[] parts = output.trim().split("\\s+");
                            if (parts.length >= 2) {
                                // 解析内存总量（字节）
                                long totalBytes = Long.parseLong(parts[1]);
                                memoryInfo.setTotalMemory(totalBytes / (1024 * 1024)); // 转换为MB

                                // 计算总内存GB数（保留一位小数）
                                double totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0);
                                DecimalFormat df = new DecimalFormat("0.00");
                                memoryInfo.setTotalMemoryGB(df.format(totalGB));
                            }

                            if (parts.length >= 3) {
                                // 解析已用内存（字节）
                                long usedBytes = Long.parseLong(parts[2]);
                                memoryInfo.setUsedMemory(usedBytes / (1024 * 1024)); // 转换为MB

                                // 计算已用内存GB数（保留一位小数）
                                double usedGB = usedBytes / (1024.0 * 1024.0 * 1024.0);
                                DecimalFormat df = new DecimalFormat("0.00");
                                memoryInfo.setUsedMemoryGB(df.format(usedGB));

                                // 如果有总内存，计算使用率
                                if (memoryInfo.getTotalMemory() != null && memoryInfo.getTotalMemory() > 0) {
                                    double usagePercent = (double) usedBytes / (double) Long.parseLong(parts[1])
                                            * 100.0;
                                    memoryInfo.setUsagePercent(usagePercent);
                                }
                            }

                            if (parts.length >= 7) {
                                // 解析可用内存（字节）
                                long availableBytes = Long.parseLong(parts[6]);
                                memoryInfo.setAvailableMemory(availableBytes / (1024 * 1024)); // 转换为MB

                                // 计算可用内存GB数（保留一位小数）
                                double availableGB = availableBytes / (1024.0 * 1024.0 * 1024.0);
                                DecimalFormat df = new DecimalFormat("0.00");
                                memoryInfo.setFreeMemoryGB(df.format(availableGB));
                            }
                        } catch (NumberFormatException e) {
                            logger.error("解析内存信息失败", e);
                        }
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 尝试获取内存类型和频率信息
            executeCommandAndUpdateCache(
                    session,
                    "which dmidecode >/dev/null 2>&1 && sudo dmidecode -t memory 2>/dev/null || echo 'dmidecode not found'",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        if (!output.contains("dmidecode not found")) {
                            try {
                                // 尝试识别内存类型
                                Pattern typePattern = Pattern.compile("Type:\\s+(DDR\\d+)");
                                Matcher typeMatcher = typePattern.matcher(output);
                                if (typeMatcher.find()) {
                                    memoryInfo.setMemoryType(typeMatcher.group(1));
                                    logger.info("检测到内存类型: {}", memoryInfo.getMemoryType());
                                }

                                // 尝试识别内存频率
                                Pattern speedPattern = Pattern.compile("Speed:\\s+(\\d+)\\s+MHz");
                                Matcher speedMatcher = speedPattern.matcher(output);
                                if (speedMatcher.find()) {
                                    String speedStr = speedMatcher.group(1);
                                    try {
                                        int frequency = Integer.parseInt(speedStr);
                                        memoryInfo.setFrequency(frequency);
                                        logger.info("检测到内存频率: {} MHz", frequency);
                                    } catch (NumberFormatException e) {
                                        logger.warn("解析内存频率失败: {}", speedStr);
                                    }
                                }

                                // 尝试识别内存插槽数量
                                int totalSlots = 0;
                                int usedSlots = 0;
                                Pattern slotPattern = Pattern.compile("Memory Device");
                                Matcher slotMatcher = slotPattern.matcher(output);
                                while (slotMatcher.find()) {
                                    totalSlots++;
                                }

                                Pattern sizePattern = Pattern.compile("Size:\\s+(\\d+)\\s+([MG]B)");
                                Matcher sizeMatcher = sizePattern.matcher(output);
                                while (sizeMatcher.find()) {
                                    usedSlots++;
                                }

                                if (totalSlots > 0) {
                                    memoryInfo.setTotalSlots(totalSlots);
                                    memoryInfo.setUsedSlots(usedSlots);
                                    logger.info("检测到内存插槽: 总数={}, 已使用={}", totalSlots, usedSlots);
                                }
                            } catch (Exception e) {
                                logger.warn("解析内存详细信息失败", e);
                            }
                        } else {
                            logger.info("未找到dmidecode工具，无法获取内存类型和频率");

                            // 尝试使用替代方法获取内存类型信息
                            try {
                                CommandResult lshwResult = MinaUtils.execCmdWithResultObject(session,
                                        "which lshw >/dev/null 2>&1 && sudo lshw -C memory 2>/dev/null || echo 'lshw not found'");

                                if (lshwResult.isSuccess() && !lshwResult.getOutput().contains("lshw not found")) {
                                    String lshwOutput = lshwResult.getOutput();

                                    // 尝试从lshw输出中提取内存类型
                                    Pattern lshwTypePattern = Pattern.compile("(DDR\\d+)");
                                    Matcher lshwTypeMatcher = lshwTypePattern.matcher(lshwOutput);
                                    if (lshwTypeMatcher.find()) {
                                        memoryInfo.setMemoryType(lshwTypeMatcher.group(1));
                                        logger.info("通过lshw检测到内存类型: {}", memoryInfo.getMemoryType());
                                    }
                                }
                            } catch (Exception e) {
                                logger.warn("使用lshw获取内存类型失败", e);
                            }
                        }
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 设置状态为成功
            memoryInfo.setStatus(OsInfoStatusEnum.SUCCESS);
            osInfo.setMemoryStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            logger.info("内存信息收集完成，总内存: {}MB, 已用: {}MB",
                    memoryInfo.getTotalMemory(),
                    memoryInfo.getUsedMemory());

            if (memoryInfo.getMemoryType() != null) {
                logger.info("内存类型: {}, 频率: {} MHz",
                        memoryInfo.getMemoryType(),
                        memoryInfo.getFrequency() != null ? memoryInfo.getFrequency() : "未知");
            }
        } catch (Exception e) {
            logger.error("收集内存信息时出错: {}", e.getMessage(), e);
            osInfo.setMemoryStatus(OsInfoStatusEnum.ERROR);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
    }

    @Override
    public void collectDiskInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集磁盘信息");

            // 设置正在收集状态
            osInfo.setDiskStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            // 创建磁盘信息对象
            DiskInfo diskInfo = new DiskInfo();
            osInfo.setDiskInfo(diskInfo);

            // 构建描述信息累积器
            StringBuilder diskDescription = new StringBuilder();

            // 获取df命令输出
            executeCommandAndUpdateCache(
                    session,
                    "df -h",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        String dfOutput = output.trim();
                        diskDescription.append(dfOutput);

                        // 部分更新状态
                        diskInfo.setDescription(diskDescription.toString());
                        diskInfo.setStatus(OsInfoStatusEnum.LOADING);
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 获取lsblk命令输出
            executeCommandAndUpdateCache(
                    session,
                    "lsblk -d -o NAME,SIZE,TYPE,MODEL",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        String lsblkOutput = output.trim();
                        diskDescription.append("\n\n").append(lsblkOutput);
                        diskInfo.setDescription(diskDescription.toString());
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 使用df -BG命令获取总容量
            executeCommandAndUpdateCache(
                    session,
                    "df -BG / | tail -1 | awk '{print $2}'",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        String diskTotal = output.trim().replace("G", "");
                        if (!diskTotal.isEmpty()) {
                            try {
                                double totalGB = Double.parseDouble(diskTotal);
                                diskInfo.setTotalDiskSpace(totalGB);
                            } catch (NumberFormatException e) {
                                logger.warn("解析磁盘总容量失败: {}", diskTotal);
                            }
                        }
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            // 获取已用容量
            executeCommandAndUpdateCache(
                    session,
                    "df -BG / | tail -1 | awk '{print $3}'",
                    hostInfo,
                    osInfo,
                    cacheUpdater,
                    (output) -> {
                        String diskUsed = output.trim().replace("G", "");
                        if (!diskUsed.isEmpty()) {
                            try {
                                double usedGB = Double.parseDouble(diskUsed);
                                diskInfo.setUsedDiskSpace(usedGB);

                                // 计算剩余容量
                                if (diskInfo.getTotalDiskSpace() != null) {
                                    diskInfo.setAvailableDiskSpace(diskInfo.getTotalDiskSpace() - usedGB);

                                    // 计算使用率
                                    if (diskInfo.getTotalDiskSpace() > 0) {
                                        diskInfo.setUsagePercent(usedGB / diskInfo.getTotalDiskSpace() * 100);
                                    }
                                }
                            } catch (NumberFormatException e) {
                                logger.warn("解析磁盘已用容量失败: {}", diskUsed);
                            }
                        }

                        // 设置状态为成功
                        diskInfo.setStatus(OsInfoStatusEnum.SUCCESS);
                        osInfo.setDiskStatus(OsInfoStatusEnum.SUCCESS);
                        return null;
                    },
                    () -> {
                        if (hostInfo != null) {
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                        }
                    });

            logger.info("磁盘信息收集完成");
        } catch (Exception e) {
            logger.error("收集磁盘信息时出错: {}", e.getMessage(), e);
            osInfo.setDiskStatus(OsInfoStatusEnum.ERROR);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
    }

    @Override
    public void collectGpuInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集GPU信息");

            // 设置正在收集状态
            osInfo.setGpuStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            // 创建GPU信息对象
            GpuInfo gpuInfo = new GpuInfo();
            osInfo.setGpuInfo(gpuInfo);

            // 立即更新，确保前端能看到加载效果
            gpuInfo.setStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            // 标记是否检测到GPU
            boolean gpuDetected = false;

            // 尝试检测NVIDIA GPU
            logger.info("尝试检测NVIDIA GPU");
            CommandResult nvidiaSmiResult = MinaUtils.execCmdWithResultObject(session,
                    "which nvidia-smi >/dev/null 2>&1 && nvidia-smi -L || echo 'No NVIDIA GPU found'");

            if (nvidiaSmiResult.isSuccess() && !nvidiaSmiResult.getOutput().contains("No NVIDIA GPU found")
                    && !nvidiaSmiResult.getOutput().isEmpty()) {
                // 检测到NVIDIA GPU
                gpuDetected = true;
                String nvidiaSmiOutput = nvidiaSmiResult.getOutput().trim();
                gpuInfo.setInfo(nvidiaSmiOutput);
                gpuInfo.setVendor("NVIDIA");
                gpuInfo.setType("独立显卡");

                // 提取型号信息
                String[] lines = nvidiaSmiOutput.split("\n");
                int deviceCount = lines.length;
                gpuInfo.setDeviceCount(deviceCount);

                if (deviceCount > 0) {
                    String modelLine = lines[0];
                    Pattern modelPattern = Pattern.compile("GPU \\d+: (.*?) \\(");
                    Matcher modelMatcher = modelPattern.matcher(modelLine);
                    if (modelMatcher.find()) {
                        gpuInfo.setModel(modelMatcher.group(1));
                    } else {
                        gpuInfo.setModel(modelLine);
                    }
                }

                // 获取详细信息
                CommandResult nvidiaSmiDetailedResult = MinaUtils.execCmdWithResultObject(session,
                        "which nvidia-smi >/dev/null 2>&1 && nvidia-smi --query-gpu=name,memory.total,memory.used,temperature.gpu,utilization.gpu --format=csv,noheader,nounits || echo ''");

                if (nvidiaSmiDetailedResult.isSuccess() && !nvidiaSmiDetailedResult.getOutput().isEmpty()) {
                    String detailedOutput = nvidiaSmiDetailedResult.getOutput().trim();
                    String[] parts = detailedOutput.split(",");

                    // 解析GPU型号
                    if (parts.length >= 1) {
                        gpuInfo.setModel(parts[0].trim());
                    }

                    // 解析显存信息
                    if (parts.length >= 3) {
                        try {
                            String totalMemStr = parts[1].trim();
                            String usedMemStr = parts[2].trim();

                            // 解析显存大小
                            double totalMem = Double.parseDouble(totalMemStr);
                            gpuInfo.setTotalMemory(totalMem);

                            // 将显存从MiB转换为GB并格式化
                            double totalMemGB = totalMem / 1024.0;
                            DecimalFormat df = new DecimalFormat("0.00");
                            gpuInfo.setFormattedMemory(df.format(totalMemGB) + " GB");

                            double usedMem = Double.parseDouble(usedMemStr);
                            gpuInfo.setUsedMemory(usedMem);

                            // 计算可用内存和使用率
                            if (gpuInfo.getTotalMemory() != null) {
                                gpuInfo.setFreeMemory(gpuInfo.getTotalMemory() - usedMem);
                                gpuInfo.setMemoryUsagePercent(usedMem / gpuInfo.getTotalMemory() * 100);
                            }
                        } catch (Exception e) {
                            logger.warn("解析NVIDIA GPU显存信息失败: {}", e.getMessage());
                        }
                    }

                    // 解析温度
                    if (parts.length >= 4) {
                        try {
                            String tempStr = parts[3].trim();
                            double temp = Double.parseDouble(tempStr);
                            gpuInfo.setTemperature(temp);
                            gpuInfo.setFormattedTemperature(temp + " °C");
                        } catch (Exception e) {
                            logger.warn("解析NVIDIA GPU温度信息失败: {}", e.getMessage());
                        }
                    }

                    // 解析使用率
                    if (parts.length >= 5) {
                        try {
                            String utilStr = parts[4].trim();
                            double util = Double.parseDouble(utilStr);
                            gpuInfo.setUtilization(util);
                            gpuInfo.setFormattedUtilization(util + "%");
                        } catch (Exception e) {
                            logger.warn("解析NVIDIA GPU使用率信息失败: {}", e.getMessage());
                        }
                    }
                }

                // 立即更新NVIDIA信息
                if (cacheUpdater != null && hostInfo != null) {
                    cacheUpdater.updateCache(hostInfo);
                }
            }

            // 如果未检测到NVIDIA GPU，尝试检测AMD GPU
            if (!gpuDetected) {
                logger.info("尝试检测AMD GPU");
                CommandResult rocmSmiResult = MinaUtils.execCmdWithResultObject(session,
                        "which rocm-smi >/dev/null 2>&1 && rocm-smi -l || echo 'No AMD GPU found'");

                if (rocmSmiResult.isSuccess() && !rocmSmiResult.getOutput().contains("No AMD GPU found")
                        && !rocmSmiResult.getOutput().isEmpty()) {
                    // 检测到AMD GPU
                    gpuDetected = true;
                    gpuInfo.setVendor("AMD");
                    gpuInfo.setType("独立显卡");

                    // 获取详细信息
                    CommandResult gpuOutputResult = MinaUtils.execCmdWithResultObject(session,
                            "which rocm-smi >/dev/null 2>&1 && rocm-smi --showmeminfo vram || echo ''");

                    if (gpuOutputResult.isSuccess() && !gpuOutputResult.getOutput().isEmpty()) {
                        String gpuOutput = gpuOutputResult.getOutput().trim();
                        gpuInfo.setInfo(gpuOutput);

                        // 尝试从rocm-smi输出中解析型号和显存信息
                        Pattern memUsagePattern = Pattern
                                .compile("GPU\\s+\\d+:\\s+\\w+\\s+(\\d+)\\s+vram\\s+(\\d+)\\s+");
                        Matcher memMatcher = memUsagePattern.matcher(gpuOutput);

                        if (memMatcher.find()) {
                            try {
                                double totalMemMB = Double.parseDouble(memMatcher.group(1));
                                double usedMemMB = Double.parseDouble(memMatcher.group(2));

                                gpuInfo.setTotalMemory(totalMemMB);
                                gpuInfo.setUsedMemory(usedMemMB);
                                gpuInfo.setFreeMemory(totalMemMB - usedMemMB);

                                // 计算使用率
                                if (totalMemMB > 0) {
                                    gpuInfo.setMemoryUsagePercent((usedMemMB / totalMemMB) * 100);
                                }

                                // 将显存从MB转换为GB并格式化
                                double totalMemGB = totalMemMB / 1024.0;
                                DecimalFormat df = new DecimalFormat("0.00");
                                gpuInfo.setFormattedMemory(df.format(totalMemGB) + " GB");
                            } catch (NumberFormatException e) {
                                logger.warn("解析AMD GPU显存信息失败: {}", e.getMessage());
                            }
                        }

                        // 获取型号信息
                        CommandResult amdModelResult = MinaUtils.execCmdWithResultObject(session,
                                "which rocm-smi >/dev/null 2>&1 && rocm-smi -i || echo ''");
                        if (amdModelResult.isSuccess() && !amdModelResult.getOutput().isEmpty()) {
                            String modelOutput = amdModelResult.getOutput().trim();
                            Pattern modelPattern = Pattern.compile("GPU\\[\\d+\\]\\s*:\\s*(.+)");
                            Matcher modelMatcher = modelPattern.matcher(modelOutput);
                            if (modelMatcher.find()) {
                                gpuInfo.setModel(modelMatcher.group(1).trim());
                            } else {
                                // 如果无法解析出型号，则直接设置
                                gpuInfo.setModel("AMD Radeon");
                            }
                        } else {
                            gpuInfo.setModel("AMD Radeon");
                        }

                        // 获取GPU数量
                        CommandResult gpuCountResult = MinaUtils.execCmdWithResultObject(session,
                                "which rocm-smi >/dev/null 2>&1 && rocm-smi -i | grep -c 'GPU\\[' || echo '1'");
                        if (gpuCountResult.isSuccess() && !gpuCountResult.getOutput().isEmpty()) {
                            try {
                                int count = Integer.parseInt(gpuCountResult.getOutput().trim());
                                gpuInfo.setDeviceCount(count);
                            } catch (NumberFormatException e) {
                                logger.warn("解析AMD GPU数量失败: {}", e.getMessage());
                                gpuInfo.setDeviceCount(1); // 默认值
                            }
                        } else {
                            logger.error("获取AMD GPU数量失败: {}", gpuCountResult.getError());
                            gpuInfo.setDeviceCount(1); // 默认值
                        }

                        // 立即更新AMD信息
                        if (cacheUpdater != null && hostInfo != null) {
                            cacheUpdater.updateCache(hostInfo);
                        }
                    } else {
                        logger.error("获取AMD GPU信息失败: {}",
                                gpuOutputResult.isSuccess() ? "无输出" : gpuOutputResult.getError());
                    }
                }
            }

            // 如果既没有NVIDIA也没有AMD GPU，尝试通过lspci检测
            if (!gpuDetected) {
                // 尝试Alpine和其他特殊Linux系统的兼容命令
                CommandResult alpineResult = MinaUtils.execCmdWithResultObject(session,
                        "if command -v lspci >/dev/null 2>&1; then lspci | grep -i 'vga\\|3d\\|display'; else echo 'lspci not found'; fi");

                if (alpineResult.isSuccess() && !alpineResult.getOutput().trim().isEmpty()
                        && !alpineResult.getOutput().contains("lspci not found")) {
                    String lspciOutput = alpineResult.getOutput().trim();
                    gpuInfo.setInfo(lspciOutput);
                    gpuDetected = true;

                    // 提取第一个设备的型号信息
                    String[] lines = lspciOutput.split("\n");
                    if (lines.length > 0) {
                        String firstLine = lines[0];
                        if (firstLine.contains(":")) {
                            String modelInfo = firstLine.substring(firstLine.indexOf(":") + 1).trim();
                            gpuInfo.setModel(modelInfo);
                        } else {
                            gpuInfo.setModel(firstLine.trim());
                        }
                    }

                    // 判断GPU厂商
                    if (lspciOutput.toLowerCase().contains("nvidia")) {
                        gpuInfo.setVendor("NVIDIA");
                        gpuInfo.setType("独立显卡");
                    } else if (lspciOutput.toLowerCase().contains("amd") ||
                            lspciOutput.toLowerCase().contains("ati") ||
                            lspciOutput.toLowerCase().contains("qxl") ||
                            lspciOutput.toLowerCase().contains("red hat")) {
                        gpuInfo.setVendor("AMD");
                        gpuInfo.setType("独立显卡");
                    } else if (lspciOutput.toLowerCase().contains("intel")) {
                        gpuInfo.setVendor("Intel");
                        gpuInfo.setType("集成显卡");
                    } else {
                        gpuInfo.setVendor("未知厂商");
                        gpuInfo.setType("未知图形设备");
                    }

                    // 计算GPU卡数量
                    gpuInfo.setDeviceCount(lines.length);
                }
            }

            // 修改为无论是否检测到GPU，都设置为成功状态
            if (!gpuDetected) {
                // 将状态改为SUCCESS而不是LOADING
                gpuInfo.setStatus(OsInfoStatusEnum.SUCCESS);
                osInfo.setGpuStatus(OsInfoStatusEnum.SUCCESS);

                // 更改提示文本为明确的无GPU信息
                gpuInfo.setVendor("未检测到");
                gpuInfo.setType("未检测到图形处理器");
                gpuInfo.setModel("未检测到图形处理器设备");
                gpuInfo.setStatusMessage("GPU信息加载完成");
                gpuInfo.setDeviceCount(0);
                gpuInfo.setDetected(false);
            } else {
                // 检测到GPU才设置为成功状态
                gpuInfo.setStatus(OsInfoStatusEnum.SUCCESS);
                osInfo.setGpuStatus(OsInfoStatusEnum.SUCCESS);
                gpuInfo.setDetected(true);
                gpuInfo.setStatusMessage("GPU信息加载完成");

                // 确保显存格式化字段有值
                if (gpuInfo.getTotalMemory() != null && gpuInfo.getFormattedMemory() == null) {
                    double totalMemGB = gpuInfo.getTotalMemory() / 1024.0;
                    DecimalFormat df = new DecimalFormat("0.00");
                    gpuInfo.setFormattedMemory(df.format(totalMemGB) + " GB");
                }
            }

            // 设置到OS信息对象并更新缓存
            osInfo.setGpuInfo(gpuInfo);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            logger.info("GPU信息收集完成，是否检测到GPU: {}", gpuDetected);
        } catch (Exception e) {
            logger.error("收集GPU信息时出错: {}", e.getMessage(), e);

            // 确保异常情况下也保持加载状态
            GpuInfo gpuInfo = new GpuInfo();
            gpuInfo.setStatus(OsInfoStatusEnum.ERROR);
            gpuInfo.setVendor("加载失败");
            gpuInfo.setType("GPU信息获取出错");
            gpuInfo.setModel("无法获取图形设备信息");
            gpuInfo.setStatusMessage("GPU信息加载失败: " + e.getMessage());
            gpuInfo.setDeviceCount(0);

            osInfo.setGpuInfo(gpuInfo);
            osInfo.setGpuStatus(OsInfoStatusEnum.ERROR);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
    }

    @Override
    public void collectNetworkInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集网络信息");

            // 设置正在收集状态
            osInfo.setNetworkStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            // 创建网络信息对象
            NetworkInfo networkInfo = new NetworkInfo();
            osInfo.setNetworkInfo(networkInfo);

            // 立即更新，确保前端能看到加载效果
            networkInfo.setStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            // 获取IP地址信息
            CommandResult ipInfoResult = MinaUtils.execCmdWithResultObject(session, "ip addr show");
            if (!ipInfoResult.isSuccess()) {
                logger.error("获取IP地址信息失败: {}", ipInfoResult.getError());
                osInfo.setNetworkStatus(OsInfoStatusEnum.ERROR);
                networkInfo.setStatus(OsInfoStatusEnum.ERROR);
                if (cacheUpdater != null && hostInfo != null) {
                    cacheUpdater.updateCache(hostInfo);
                }
                return;
            }
            String ipInfo = ipInfoResult.getOutput().trim();

            // 获取网络接口状态
            CommandResult ifstatResult = MinaUtils.execCmdWithResultObject(session, "ip -s link");
            String ifstatInfo = ifstatResult.isSuccess() ? ifstatResult.getOutput().trim() : "";

            // 获取路由信息
            CommandResult routeInfoResult = MinaUtils.execCmdWithResultObject(session, "ip route");
            if (!routeInfoResult.isSuccess()) {
                logger.error("获取路由信息失败: {}", routeInfoResult.getError());
                // 不返回，继续收集其他信息
            }
            String routeInfo = routeInfoResult.isSuccess() ? routeInfoResult.getOutput().trim() : "";

            // 获取DNS信息
            CommandResult dnsInfoResult = MinaUtils.execCmdWithResultObject(session,
                    "cat /etc/resolv.conf | grep nameserver");
            String dnsInfo = dnsInfoResult.isSuccess() ? dnsInfoResult.getOutput().trim() : "";

            // 获取活动连接信息
            CommandResult connectionsResult = MinaUtils.execCmdWithResultObject(session,
                    "netstat -an | grep ESTABLISHED | wc -l");
            int connections = 0;
            if (connectionsResult.isSuccess()) {
                try {
                    connections = Integer.parseInt(connectionsResult.getOutput().trim());
                } catch (NumberFormatException e) {
                    logger.warn("解析活动连接数失败: {}", connectionsResult.getOutput());
                }
            } else {
                logger.error("获取活动连接数失败: {}", connectionsResult.getError());
            }

            // 解析IP地址信息，提取网络接口名称和IP地址
            Pattern ifacePattern = Pattern.compile("\\d+:\\s+(\\w+):.*");
            Pattern ipv4Pattern = Pattern.compile("\\s+inet\\s+([0-9.]+)/\\d+\\s+");
            Pattern macPattern = Pattern.compile("\\s+link/ether\\s+([0-9a-f:]+)\\s+");

            Map<String, InterfaceInfo> interfaces = new HashMap<>();
            String currentIface = null;

            for (String line : ipInfo.split("\n")) {
                Matcher ifaceMatcher = ifacePattern.matcher(line);
                if (ifaceMatcher.find()) {
                    currentIface = ifaceMatcher.group(1);
                    // 跳过回环接口
                    if (!currentIface.equals("lo")) {
                        interfaces.put(currentIface, new InterfaceInfo());
                        interfaces.get(currentIface).setName(currentIface);
                    }
                } else if (currentIface != null && !currentIface.equals("lo")) {
                    Matcher ipv4Matcher = ipv4Pattern.matcher(line);
                    if (ipv4Matcher.find()) {
                        String ip = ipv4Matcher.group(1);
                        interfaces.get(currentIface).setIpAddress(ip);
                    }

                    Matcher macMatcher = macPattern.matcher(line);
                    if (macMatcher.find()) {
                        String mac = macMatcher.group(1);
                        interfaces.get(currentIface).setMacAddress(mac);
                    }
                }
            }

            // 解析网络接口状态和流量信息
            currentIface = null;
            Pattern ifstatNamePattern = Pattern.compile("\\d+:\\s+(\\w+):");
            Pattern rxPacketsPattern = Pattern.compile("\\s+RX:\\s+bytes\\s+(\\d+)\\s+");
            Pattern txPacketsPattern = Pattern.compile("\\s+TX:\\s+bytes\\s+(\\d+)\\s+");

            for (String line : ifstatInfo.split("\n")) {
                Matcher ifstatNameMatcher = ifstatNamePattern.matcher(line);
                if (ifstatNameMatcher.find()) {
                    currentIface = ifstatNameMatcher.group(1);
                } else if (currentIface != null && interfaces.containsKey(currentIface)) {
                    Matcher rxMatcher = rxPacketsPattern.matcher(line);
                    if (rxMatcher.find()) {
                        try {
                            long rxBytes = Long.parseLong(rxMatcher.group(1));
                            interfaces.get(currentIface).setRxBytes(rxBytes);
                            // 转换为KB/MB/GB
                            double rxKB = rxBytes / 1024.0;
                            double rxMB = rxKB / 1024.0;
                            double rxGB = rxMB / 1024.0;

                            DecimalFormat df = new DecimalFormat("0.00");
                            String rxTraffic = "";
                            if (rxGB >= 1.0) {
                                rxTraffic = df.format(rxGB) + " GB";
                            } else if (rxMB >= 1.0) {
                                rxTraffic = df.format(rxMB) + " MB";
                            } else {
                                rxTraffic = df.format(rxKB) + " KB";
                            }
                            interfaces.get(currentIface).setRxTraffic(rxTraffic);
                            logger.debug("接口 {} 接收流量: {}", currentIface, rxTraffic);
                        } catch (NumberFormatException e) {
                            logger.warn("解析接收字节数失败: {}", rxMatcher.group(1));
                        }
                    }

                    Matcher txMatcher = txPacketsPattern.matcher(line);
                    if (txMatcher.find()) {
                        try {
                            long txBytes = Long.parseLong(txMatcher.group(1));
                            interfaces.get(currentIface).setTxBytes(txBytes);
                            // 转换为KB/MB/GB
                            double txKB = txBytes / 1024.0;
                            double txMB = txKB / 1024.0;
                            double txGB = txMB / 1024.0;

                            DecimalFormat df = new DecimalFormat("0.00");
                            String txTraffic = "";
                            if (txGB >= 1.0) {
                                txTraffic = df.format(txGB) + " GB";
                            } else if (txMB >= 1.0) {
                                txTraffic = df.format(txMB) + " MB";
                            } else {
                                txTraffic = df.format(txKB) + " KB";
                            }
                            interfaces.get(currentIface).setTxTraffic(txTraffic);
                            logger.debug("接口 {} 发送流量: {}", currentIface, txTraffic);
                        } catch (NumberFormatException e) {
                            logger.warn("解析发送字节数失败: {}", txMatcher.group(1));
                        }
                    }
                }
            }

            // 设置收集到的网络信息
            networkInfo.setInterfaces(new ArrayList<>(interfaces.values()));
            networkInfo.setActiveConnections(connections);
            networkInfo.setRawIpInfo(ipInfo);
            networkInfo.setRawRouteInfo(routeInfo);
            networkInfo.setRawDnsInfo(dnsInfo);

            // 解析默认网关
            Pattern defaultRoutePattern = Pattern.compile("default\\s+via\\s+([0-9.]+)\\s+");
            Matcher defaultRouteMatcher = defaultRoutePattern.matcher(routeInfo);
            if (defaultRouteMatcher.find()) {
                networkInfo.setDefaultGateway(defaultRouteMatcher.group(1));
            }

            // 解析DNS服务器
            Pattern dnsPattern = Pattern.compile("nameserver\\s+([0-9.]+)");
            Matcher dnsMatcher = dnsPattern.matcher(dnsInfo);
            List<String> dnsServers = new ArrayList<>();
            while (dnsMatcher.find()) {
                dnsServers.add(dnsMatcher.group(1));
            }
            networkInfo.setDnsServers(dnsServers);

            // 设置状态为成功
            networkInfo.setStatus(OsInfoStatusEnum.SUCCESS);
            osInfo.setNetworkStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            logger.info("网络信息收集完成，接口数量: {}, 活动连接: {}", interfaces.size(), connections);
        } catch (Exception e) {
            logger.error("收集网络信息时出错: {}", e.getMessage(), e);
            osInfo.setNetworkStatus(OsInfoStatusEnum.ERROR);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
    }

    @Override
    public void collectDnsInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集DNS信息");

            // 设置正在收集状态
            osInfo.setDnsStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            // 创建或获取DnsInfo对象
            DnsInfo dnsInfo = osInfo.getDnsInfo();
            if (dnsInfo == null) {
                dnsInfo = new DnsInfo();
                osInfo.setDnsInfo(dnsInfo);
            }

            // 获取resolv.conf文件内容
            CommandResult resolvConfResult = MinaUtils.execCmdWithResultObject(session, "cat /etc/resolv.conf");
            String resolvConf = "";

            if (resolvConfResult.isSuccess()) {
                resolvConf = resolvConfResult.getOutput().trim();
                logger.info("成功获取resolv.conf文件内容");
                dnsInfo.setResolvConfContent(resolvConf);
            } else {
                logger.error("获取DNS配置失败: {}", resolvConfResult.getError());
            }

            // 提取DNS服务器地址
            List<String> dnsServers = new ArrayList<>();
            Pattern nameserverPattern = Pattern.compile("nameserver\\s+([0-9.]+)");
            Matcher matcher = nameserverPattern.matcher(resolvConf);
            while (matcher.find()) {
                dnsServers.add(matcher.group(1));
            }

            // 设置服务器列表到DnsInfo对象
            dnsInfo.setServers(dnsServers);

            // 获取/etc/hosts文件内容
            CommandResult hostsResult = MinaUtils.execCmdWithResultObject(session, "cat /etc/hosts");
            if (hostsResult.isSuccess()) {
                String hostsContent = hostsResult.getOutput().trim();
                logger.info("成功获取hosts文件内容");
                dnsInfo.setHostsFileContent(hostsContent);
            } else {
                logger.error("获取hosts文件内容失败: {}", hostsResult.getError());
            }

            // 检查DNS是否工作正常
            boolean dnsWorking = checkDnsWorking(session);
            dnsInfo.setWorking(dnsWorking);

            // 设置状态为成功
            dnsInfo.setStatus(OsInfoStatusEnum.SUCCESS);
            osInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);

            logger.info("DNS信息收集完成，发现{}个DNS服务器", dnsServers.size());

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        } catch (Exception e) {
            logger.error("收集DNS信息时出错: {}", e.getMessage(), e);
            osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
    }

    /**
     * 检查DNS是否工作正常
     * 在无外网环境下主要检查DNS配置而非实际连通性
     */
    private boolean checkDnsWorking(ClientSession session) {
        // 方法1: 检查/etc/resolv.conf文件是否包含nameserver条目
        try {
            CommandResult resolvConfResult = MinaUtils.execCmdWithResultObject(session,
                    "grep nameserver /etc/resolv.conf");
            if (resolvConfResult.isSuccess() && !resolvConfResult.getOutput().trim().isEmpty()) {
                logger.info("DNS检测(/etc/resolv.conf): 包含nameserver条目");
                return true;
            }
        } catch (Exception e) {
            logger.debug("DNS检测(/etc/resolv.conf): 检查失败: {}", e.getMessage());
        }

        // 方法2: 尝试查看是否安装了nslookup或ping命令
        try {
            CommandResult whichResult = MinaUtils.execCmdWithResultObject(session, "which nslookup ping 2>/dev/null");
            if (whichResult.isSuccess() && !whichResult.getOutput().trim().isEmpty()) {
                logger.info("DNS检测: 系统安装了DNS查询工具");
                return true;
            }
        } catch (Exception e) {
            logger.debug("DNS检测(which): 命令不可用: {}", e.getMessage());
        }

        // 方法3: 检查network-scripts目录下的配置文件中是否有DNS设置
        try {
            CommandResult netConfResult = MinaUtils.execCmdWithResultObject(session,
                    "grep -i dns /etc/sysconfig/network-scripts/ifcfg-* 2>/dev/null || grep -i dns /etc/network/interfaces* 2>/dev/null");
            if (netConfResult.isSuccess() && !netConfResult.getOutput().trim().isEmpty()) {
                logger.info("DNS检测(network-config): 网络配置中包含DNS设置");
                return true;
            }
        } catch (Exception e) {
            logger.debug("DNS检测(network-config): 检查失败: {}", e.getMessage());
        }

        // 方法4: 检查NetworkManager配置
        try {
            CommandResult nmcliResult = MinaUtils.execCmdWithResultObject(session,
                    "nmcli con show 2>/dev/null | grep -i dns || cat /etc/NetworkManager/system-connections/* 2>/dev/null | grep -i dns");
            if (nmcliResult.isSuccess() && !nmcliResult.getOutput().trim().isEmpty()) {
                logger.info("DNS检测(NetworkManager): 发现DNS配置");
                return true;
            }
        } catch (Exception e) {
            logger.debug("DNS检测(NetworkManager): 检查失败: {}", e.getMessage());
        }

        // 方法5: 尝试通过内网域名测试，而不是外网域名
        try {
            // 使用主机自身作为目标
            CommandResult localPingResult = MinaUtils.execCmdWithResultObject(session,
                    "ping -c 1 -W 1 localhost 2>/dev/null");
            if (localPingResult.isSuccess() && !localPingResult.getOutput().contains("unknown host")) {
                logger.info("DNS检测(localhost): 本地解析正常");
                return true;
            }
        } catch (Exception e) {
            logger.debug("DNS检测(localhost): 测试失败: {}", e.getMessage());
        }

        logger.warn("DNS检测: 所有检测方法都失败，可能没有配置DNS或网络环境受限");
        return false;
    }

    @Override
    public void collectHostsFileInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo,
            CacheUpdater cacheUpdater) {
        if (osInfo == null) {
            logger.error("collectHostsFileInfo: 操作系统信息对象为空");
            return;
        }

        // 如果DnsInfo对象不存在，创建一个新的
        if (osInfo.getDnsInfo() == null) {
            osInfo.setDnsInfo(new DnsInfo());
        }

        // 设置hosts文件状态为LOADING
        osInfo.setDnsStatus(OsInfoStatusEnum.COLLECTING);
        osInfo.getDnsInfo().setStatus(OsInfoStatusEnum.COLLECTING);

        // 更新缓存
        if (cacheUpdater != null) {
            cacheUpdater.updateCache(hostInfo);
        }

        try {
            // 读取hosts文件内容
            CommandResult hostsResult = MinaUtils.execCmdWithResultObject(session, "cat /etc/hosts");

            if (hostsResult.isSuccess()) {
                String hostsContent = hostsResult.getOutput().trim();
                // 保存到DnsInfo中而不是hostInfo
                osInfo.getDnsInfo().setHostsFileContent(hostsContent);
                osInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);
                osInfo.getDnsInfo().setStatus(OsInfoStatusEnum.SUCCESS);
                logger.info("成功获取hosts文件信息: {}", hostInfo.getIp());
            } else {
                logger.error("读取hosts文件失败: {}", hostsResult.getError());
                osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
                osInfo.getDnsInfo().setStatus(OsInfoStatusEnum.ERROR);
            }
        } catch (Exception e) {
            logger.error("获取hosts文件时出错: {}", e.getMessage(), e);
            osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
            osInfo.getDnsInfo().setStatus(OsInfoStatusEnum.ERROR);
        } finally {
            // 确保状态更新被保存
            if (cacheUpdater != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
    }
}