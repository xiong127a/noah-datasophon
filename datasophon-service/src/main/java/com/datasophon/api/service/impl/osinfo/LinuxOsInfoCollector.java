package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import com.datasophon.common.model.hardware.CpuInfo;
import com.datasophon.common.model.hardware.DiskInfo;
import com.datasophon.common.model.hardware.GpuInfo;
import com.datasophon.common.model.hardware.MemoryInfo;
import com.datasophon.common.model.hardware.SwapInfo;
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
            // 首先尝试读取特定的系统release文件
            checkAlternativeLinuxFiles(session, osInfo);

            // 如果没有获取到全名，再尝试读取/etc/os-release文件
            if (StringUtils.isBlank(osInfo.getFullName())) {
                String osRelease = MinaUtils.execCmdWithResult(session, "cat /etc/os-release 2>/dev/null");
                if (StringUtils.isNotBlank(osRelease)) {
                    // 解析OS信息
                    parseOsRelease(osInfo, osRelease);
                }
            }

            // 获取内核版本
            String kernel = MinaUtils.execCmdWithResult(session, "uname -r").trim();
            osInfo.setKernelVersion(kernel);

            // 获取架构
            String arch = MinaUtils.execCmdWithResult(session, "uname -m").trim();
            osInfo.setArchitecture(arch);

            // 更新发行版信息
            updateDistributionInfo(osInfo);

            // 标记系统信息有效
            osInfo.setValid(true);

            // 如果提供了主机信息和缓存更新器，更新缓存
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
            } else {
                // 使用通用名称
                osInfo.setDistribution("Linux");
            }
        }
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

    /**
     * 执行命令并返回结果
     */
    private String executeCommand(ClientSession session, String command) {
        try {
            return MinaUtils.execCmdWithResult(session, command);
        } catch (Exception e) {
            logger.error("执行命令失败: {}, 错误: {}", command, e.getMessage(), e);
            return "";
        }
    }

    public void collectSwapInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater) {
        logger.info("开始收集交换分区信息：{}", hostInfo.getIp());

        try {
            // 设置状态为收集中
            osInfo.setSwapStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            // 使用free命令获取交换分区信息
            String swapInfoStr = executeCommand(session, "free -b | grep Swap");
            if (swapInfoStr != null && !swapInfoStr.isEmpty()) {
                String[] parts = swapInfoStr.trim().split("\\s+");
                if (parts.length >= 3) {
                    // 创建交换分区信息对象
                    SwapInfo swapInfo = new SwapInfo();

                    // Swap: 总量 已用 空闲
                    long totalBytes = Long.parseLong(parts[1]);
                    long usedBytes = Long.parseLong(parts[2]);
                    long freeBytes = Long.parseLong(parts[3]);

                    // 设置交换分区信息（字节）
                    swapInfo.setTotalSwap(totalBytes);
                    swapInfo.setEnabled(totalBytes > 0); // 如果总容量大于0，说明启用了交换分区
                    swapInfo.setAvailableSwap(freeBytes);

                    // 设置使用率百分比
                    if (totalBytes > 0) {
                        swapInfo.setUsagePercent((double) usedBytes / totalBytes * 100);
                    }

                    // 设置状态为成功
                    swapInfo.setStatus(OsInfoStatusEnum.SUCCESS);

                    // 设置到OS信息对象
                    osInfo.setSwapInfo(swapInfo);
                    osInfo.setSwapStatus(OsInfoStatusEnum.SUCCESS);

                    logger.info("已收集交换分区信息: 总={} 字节, 已用={} 字节, 空闲={} 字节",
                            totalBytes, usedBytes, freeBytes);
                }
            }
        } catch (Exception e) {
            logger.error("收集交换分区信息失败: {}", e.getMessage(), e);
            osInfo.setSwapStatus(OsInfoStatusEnum.ERROR);
        } finally {
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
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

            // 获取CPU型号
            String cpuModel = MinaUtils.execCmdWithResult(session,
                    "cat /proc/cpuinfo | grep 'model name' | head -n 1 | cut -d':' -f2").trim();
            cpuInfo.setModel(cpuModel);

            // 获取物理CPU个数
            String physicalCpuCount = MinaUtils.execCmdWithResult(session,
                    "cat /proc/cpuinfo | grep 'physical id' | sort -u | wc -l").trim();
            cpuInfo.setPhysicalCount(Integer.parseInt(physicalCpuCount));

            // 获取CPU核心数
            String cpuCores = MinaUtils.execCmdWithResult(session,
                    "cat /proc/cpuinfo | grep 'cpu cores' | head -n 1 | cut -d':' -f2").trim();
            if (cpuCores.isEmpty()) {
                // 如果获取不到核心数，则尝试获取处理器个数
                cpuCores = MinaUtils.execCmdWithResult(session, "nproc").trim();
            }
            cpuInfo.setCores(Integer.parseInt(cpuCores));

            // 获取CPU频率
            String cpuFreq = MinaUtils.execCmdWithResult(session,
                    "cat /proc/cpuinfo | grep 'cpu MHz' | head -n 1 | cut -d':' -f2").trim();
            if (!cpuFreq.isEmpty()) {
                cpuInfo.setFrequency(Double.parseDouble(cpuFreq));
            }

            // 存储CPU原始信息
            String rawInfo = MinaUtils.execCmdWithResult(session, "cat /proc/cpuinfo | head -20").trim();
            cpuInfo.setRawInfo(rawInfo);

            // 设置状态为成功
            cpuInfo.setStatus(OsInfoStatusEnum.SUCCESS);

            // 设置到OS信息对象
            osInfo.setCpuInfo(cpuInfo);
            osInfo.setCpuStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

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

            // 获取总内存大小和已使用内存（MB）
            String memoryInfoStr = MinaUtils.execCmdWithResult(session, "free -m | grep Mem").trim();
            if (!memoryInfoStr.isEmpty()) {
                String[] parts = memoryInfoStr.split("\\s+");
                if (parts.length >= 3) {
                    // Mem: 总内存 已用内存 空闲内存
                    long totalMB = Long.parseLong(parts[1]);
                    long usedMB = Long.parseLong(parts[2]);
                    long freeMB = Long.parseLong(parts[3]);

                    // 转换为字节
                    memoryInfo.setTotalMemory(totalMB);
                    memoryInfo.setUsedMemory(usedMB);
                    memoryInfo.setAvailableMemory(freeMB);

                    // 计算使用率
                    if (totalMB > 0) {
                        memoryInfo.setUsagePercent((double) usedMB / totalMB * 100);
                    }
                }
            }

            // 设置状态为成功
            memoryInfo.setStatus(OsInfoStatusEnum.SUCCESS);

            // 设置到OS信息对象
            osInfo.setMemoryInfo(memoryInfo);
            osInfo.setMemoryStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            logger.info("内存信息收集完成");
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

            // 获取df命令输出
            String dfOutput = MinaUtils.execCmdWithResult(session, "df -h").trim();
            diskInfo.setDescription(dfOutput);

            // 获取lsblk命令输出
            String lsblkOutput = MinaUtils.execCmdWithResult(session, "lsblk -d -o NAME,SIZE,TYPE,MODEL").trim();
            diskInfo.setDescription(diskInfo.getDescription() + "\n\n" + lsblkOutput);

            // 使用df -BG命令获取总容量
            String diskTotal = MinaUtils.execCmdWithResult(session,
                    "df -BG / | tail -1 | awk '{print $2}'").trim().replace("G", "");
            if (!diskTotal.isEmpty()) {
                try {
                    double totalGB = Double.parseDouble(diskTotal);
                    diskInfo.setTotalDiskSpace(totalGB);
                } catch (NumberFormatException e) {
                    logger.warn("解析磁盘总容量失败: {}", diskTotal);
                }
            }

            // 获取已用容量
            String diskUsed = MinaUtils.execCmdWithResult(session,
                    "df -BG / | tail -1 | awk '{print $3}'").trim().replace("G", "");
            if (!diskUsed.isEmpty()) {
                try {
                    double usedGB = Double.parseDouble(diskUsed);
                    diskInfo.setUsedDiskSpace(usedGB);
                } catch (NumberFormatException e) {
                    logger.warn("解析磁盘已用容量失败: {}", diskUsed);
                }
            }

            // 计算剩余容量
            if (diskInfo.getTotalDiskSpace() != null && diskInfo.getUsedDiskSpace() != null) {
                diskInfo.setAvailableDiskSpace(diskInfo.getTotalDiskSpace() - diskInfo.getUsedDiskSpace());

                // 计算使用率
                if (diskInfo.getTotalDiskSpace() > 0) {
                    diskInfo.setUsagePercent(diskInfo.getUsedDiskSpace() / diskInfo.getTotalDiskSpace() * 100);
                }
            }

            // 设置状态为成功
            diskInfo.setStatus(OsInfoStatusEnum.SUCCESS);

            // 设置到OS信息对象
            osInfo.setDiskInfo(diskInfo);
            osInfo.setDiskStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

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

            // 检查是否有NVIDIA GPU
            String hasNvidia = MinaUtils.execCmdWithResult(session,
                    "command -v nvidia-smi >/dev/null 2>&1 && echo 'yes' || echo 'no'").trim();
            if ("yes".equals(hasNvidia)) {
                logger.info("检测到NVIDIA GPU");

                // 获取NVIDIA GPU信息
                String gpuOutput = MinaUtils.execCmdWithResult(session,
                        "nvidia-smi --query-gpu=name,memory.total,memory.used,temperature.gpu --format=csv,noheader")
                        .trim();
                if (!gpuOutput.isEmpty()) {
                    gpuInfo.setInfo(gpuOutput);
                    gpuInfo.setVendor("NVIDIA");
                    gpuInfo.setType("独立显卡");

                    // 计算GPU卡数量
                    String gpuCount = MinaUtils.execCmdWithResult(session,
                            "nvidia-smi --query-gpu=count --format=csv,noheader").trim();
                    try {
                        gpuInfo.setDeviceCount(Integer.parseInt(gpuCount));
                    } catch (NumberFormatException e) {
                        gpuInfo.setDeviceCount(1); // 默认值
                    }
                }
            } else {
                // 检查是否有AMD GPU
                String hasAmd = MinaUtils.execCmdWithResult(session,
                        "command -v rocm-smi >/dev/null 2>&1 && echo 'yes' || echo 'no'").trim();
                if ("yes".equals(hasAmd)) {
                    logger.info("检测到AMD GPU");

                    // 获取AMD GPU信息
                    String gpuOutput = MinaUtils.execCmdWithResult(session, "rocm-smi --showproductname").trim();
                    if (!gpuOutput.isEmpty()) {
                        gpuInfo.setInfo(gpuOutput);
                        gpuInfo.setVendor("AMD");
                        gpuInfo.setType("独立显卡");

                        // 计算GPU卡数量
                        String gpuCount = MinaUtils.execCmdWithResult(session, "rocm-smi -i | wc -l").trim();
                        try {
                            gpuInfo.setDeviceCount(Integer.parseInt(gpuCount));
                        } catch (NumberFormatException e) {
                            gpuInfo.setDeviceCount(1); // 默认值
                        }
                    }
                } else {
                    // 尝试通过lspci检测GPU
                    String lspciOutput = MinaUtils.execCmdWithResult(session,
                            "lspci | grep -i 'vga\\|3d\\|display'").trim();
                    if (!lspciOutput.isEmpty()) {
                        gpuInfo.setInfo(lspciOutput);

                        if (lspciOutput.toLowerCase().contains("nvidia")) {
                            gpuInfo.setVendor("NVIDIA");
                            gpuInfo.setType("独立显卡");
                        } else if (lspciOutput.toLowerCase().contains("amd") ||
                                lspciOutput.toLowerCase().contains("ati")) {
                            gpuInfo.setVendor("AMD");
                            gpuInfo.setType("独立显卡");
                        } else if (lspciOutput.toLowerCase().contains("intel")) {
                            gpuInfo.setVendor("Intel");
                            gpuInfo.setType("集成显卡");
                        } else {
                            gpuInfo.setVendor("未知厂商");
                            gpuInfo.setType("未知类型");
                        }

                        // 计算GPU卡数量
                        String[] lines = lspciOutput.split("\n");
                        gpuInfo.setDeviceCount(lines.length);
                    } else {
                        // 没有检测到GPU
                        gpuInfo.setVendor("无");
                        gpuInfo.setType("无");
                        gpuInfo.setDeviceCount(0);
                    }
                }
            }

            // 设置状态为成功
            gpuInfo.setStatus(OsInfoStatusEnum.SUCCESS);

            // 设置到OS信息对象
            osInfo.setGpuInfo(gpuInfo);
            osInfo.setGpuStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            logger.info("GPU信息收集完成");
        } catch (Exception e) {
            logger.error("收集GPU信息时出错: {}", e.getMessage(), e);
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

            // 获取IP地址信息
            String ipInfo = MinaUtils.execCmdWithResult(session, "ip addr show").trim();

            // 获取路由信息
            String routeInfo = MinaUtils.execCmdWithResult(session, "ip route").trim();

            // 获取活动连接信息
            String connectionsStr = MinaUtils.execCmdWithResult(session, "netstat -an | grep ESTABLISHED | wc -l")
                    .trim();
            int connections = 0;
            try {
                connections = Integer.parseInt(connectionsStr);
            } catch (NumberFormatException e) {
                logger.warn("解析活动连接数失败: {}", connectionsStr);
            }

            // 解析IP地址信息，提取网络接口名称和IP地址
            Pattern ifacePattern = Pattern.compile("\\d+:\\s+(\\w+):.*");
            Pattern ipv4Pattern = Pattern.compile("\\s+inet\\s+([0-9.]+)/\\d+\\s+");

            Map<String, String> interfaceIps = new HashMap<>();
            String currentIface = null;

            for (String line : ipInfo.split("\n")) {
                Matcher ifaceMatcher = ifacePattern.matcher(line);
                if (ifaceMatcher.find()) {
                    currentIface = ifaceMatcher.group(1);
                } else if (currentIface != null) {
                    Matcher ipv4Matcher = ipv4Pattern.matcher(line);
                    if (ipv4Matcher.find()) {
                        String ip = ipv4Matcher.group(1);
                        if (!ip.equals("127.0.0.1")) {
                            interfaceIps.put(currentIface, ip);
                        }
                    }
                }
            }

            // 创建网络信息对象并设置基本信息
            osInfo.setNetworkStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            logger.info("网络信息收集完成");
        } catch (Exception e) {
            logger.error("收集网络信息时出错: {}", e.getMessage(), e);
            osInfo.setNetworkStatus(OsInfoStatusEnum.ERROR);

            // 更新缓存
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

            // 获取resolv.conf文件内容
            String resolvConf = MinaUtils.execCmdWithResult(session, "cat /etc/resolv.conf").trim();

            // 提取DNS服务器地址
            List<String> dnsServers = new ArrayList<>();
            Pattern nameserverPattern = Pattern.compile("nameserver\\s+([0-9.]+)");
            Matcher matcher = nameserverPattern.matcher(resolvConf);
            while (matcher.find()) {
                dnsServers.add(matcher.group(1));
            }

            // 设置DNS服务器列表
            osInfo.setDnsServers(dnsServers);
            osInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            logger.info("DNS信息收集完成");
        } catch (Exception e) {
            logger.error("收集DNS信息时出错: {}", e.getMessage(), e);
            osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
    }

    @Override
    public void collectHostsFileInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo,
            CacheUpdater cacheUpdater) {
        try {
            logger.info("收集hosts文件信息");

            // 设置正在收集状态
            osInfo.setHostsFileStatus(OsInfoStatusEnum.LOADING);
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            // 获取/etc/hosts文件内容
            String hostsFile = MinaUtils.execCmdWithResult(session, "cat /etc/hosts").trim();

            // 设置hosts文件内容
            osInfo.setHostsFile(hostsFile);
            osInfo.setHostsFileStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }

            logger.info("hosts文件信息收集完成");
        } catch (Exception e) {
            logger.error("收集hosts文件信息时出错: {}", e.getMessage(), e);
            osInfo.setHostsFileStatus(OsInfoStatusEnum.ERROR);

            // 更新缓存
            if (cacheUpdater != null && hostInfo != null) {
                cacheUpdater.updateCache(hostInfo);
            }
        }
    }
}