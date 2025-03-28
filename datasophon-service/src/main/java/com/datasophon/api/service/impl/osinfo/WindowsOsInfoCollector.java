package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Windows操作系统信息收集器
 * 专门收集Windows系统的操作系统和硬件信息
 */
@Component
public class WindowsOsInfoCollector implements IOsInfoCollector {

    private static final Logger logger = LoggerFactory.getLogger(WindowsOsInfoCollector.class);

    @Override
    public String getSupportedOsType() {
        return "windows";
    }

    @Override
    public OsInfo collectOsInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater) {
        try {
            logger.info("开始收集Windows操作系统信息: {}", hostInfo.getIp());

            // 通过注册表获取主机名
            String hostnameReg = MinaUtils.execCmdWithResult(session,
                    "reg query \"HKLM\\SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters\" /v Hostname");
            if (StringUtils.isNotBlank(hostnameReg)) {
                Pattern hostPattern = Pattern.compile("Hostname\\s+REG_SZ\\s+(.+)");
                Matcher hostMatcher = hostPattern.matcher(hostnameReg);
                if (hostMatcher.find()) {
                    String hostname = hostMatcher.group(1).trim();
                    osInfo.setHostname(hostname);
                    hostInfo.setHostname(hostname);
                    logger.info("通过注册表获取到计算机名: {}", hostname);
                    // 立即更新缓存，使前端能看到主机名
                    cacheUpdater.updateCache(hostInfo);
                }
            }

            // 通过注册表获取FQDN（主机名+域名）
            String domainReg = MinaUtils.execCmdWithResult(session,
                    "reg query \"HKLM\\SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters\" /v Domain");
            if (StringUtils.isNotBlank(domainReg)) {
                Pattern domainPattern = Pattern.compile("Domain\\s+REG_SZ\\s+(.+)");
                Matcher domainMatcher = domainPattern.matcher(domainReg);
                if (domainMatcher.find()) {
                    String domain = domainMatcher.group(1).trim();
                    String fqdn = osInfo.getHostname() + "." + domain;
                    osInfo.setFqdn(fqdn);
                    logger.info("通过注册表获取到FQDN: {}", fqdn);
                    // 立即更新缓存，使前端能看到FQDN
                    cacheUpdater.updateCache(hostInfo);
                }
            }

            // 获取操作系统版本信息（已使用注册表方式）
            collectWindowsVersionInfo(osInfo, session);
            // 更新缓存，使前端能看到Windows版本信息
            cacheUpdater.updateCache(hostInfo);

            // 通过注册表获取CPU架构
            String archReg = MinaUtils.execCmdWithResult(session,
                    "reg query \"HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment\" /v PROCESSOR_ARCHITECTURE");
            if (StringUtils.isNotBlank(archReg)) {
                Pattern archPattern = Pattern.compile("PROCESSOR_ARCHITECTURE\\s+REG_SZ\\s+(.+)");
                Matcher archMatcher = archPattern.matcher(archReg);
                if (archMatcher.find()) {
                    String architecture = archMatcher.group(1).trim();
                    osInfo.setArchitecture(architecture);
                    logger.info("通过注册表获取到CPU架构: {}", architecture);
                    // 更新CPU架构信息
                    cacheUpdater.updateCache(hostInfo);
                }
            }

            osInfo.setValid(true);
            // 完成时更新一次
            cacheUpdater.updateCache(hostInfo);
            return osInfo;
        } catch (Exception e) {
            logger.error("收集Windows操作系统信息时出错: {}", e.getMessage(), e);
            osInfo.setValid(false);
            // 出错时也更新缓存，标记错误状态
            cacheUpdater.updateCache(hostInfo);
            return osInfo;
        }
    }

    @Override
    public void collectHardwareInfo(OsInfo osInfo, ClientSession session, CacheUpdater cacheUpdater) {
        try {
            osInfo.setHardwareCollectionStatus("loading");
            // 更新收集状态，不再依赖hostInfo
            cacheUpdater.updateCache(null);

            logger.info("开始收集Windows硬件信息");

            // 获取CPU信息
            osInfo.setLastUpdatedItem("collecting_cpu");
            logger.info("收集CPU信息...");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(null);
            collectCpuInfo(osInfo, session);

            // 获取内存信息
            osInfo.setLastUpdatedItem("collecting_memory");
            logger.info("收集内存信息...");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(null);
            collectMemoryInfo(osInfo, session);

            // 获取存储信息
            osInfo.setLastUpdatedItem("collecting_disk");
            logger.info("收集磁盘信息...");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(null);
            collectStorageInfo(osInfo, session);

            // 获取GPU信息
            osInfo.setLastUpdatedItem("collecting_gpu");
            logger.info("收集GPU信息...");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(null);
            collectGpuInfo(osInfo, session);

            // 标记为完成
            osInfo.setLastUpdatedItem("completed");
            osInfo.setHardwareCollectionStatus("success");
            // 完成时更新一次
            cacheUpdater.updateCache(null);

            logger.info("Windows硬件信息收集完成");
        } catch (Exception e) {
            logger.error("收集Windows硬件信息时出错: {}", e.getMessage(), e);
            osInfo.setHardwareCollectionStatus("error");
            osInfo.setLastUpdatedItem("error");
            // 出错时也更新，不再依赖hostInfo
            cacheUpdater.updateCache(null);
        }
    }

    /**
     * 收集Windows版本信息
     */
    private void collectWindowsVersionInfo(OsInfo osInfo, ClientSession session) {
        try {
            // 使用注册表查询获取操作系统信息
            String regQuery = MinaUtils.execCmdWithResult(session,
                    "reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\" /v ProductName");

            if (StringUtils.isNotBlank(regQuery)) {
                String productName = OsInfoUtils.extractWindowsRegValue(regQuery, "ProductName");
                if (StringUtils.isNotBlank(productName)) {
                    osInfo.setFullName(productName);
                    osInfo.setDistributionName("Windows");
                    osInfo.setDisplayName(productName);

                    // 从名称判断版本（如Windows 10, Windows Server 2019等）
                    if (productName.contains("Server")) {
                        osInfo.setDistributionId("windows-server");
                    } else {
                        osInfo.setDistributionId("windows");
                    }

                    logger.info("获取到Windows产品名称: {}", productName);
                }
            }

            // 获取版本号
            String versionCmd = MinaUtils.execCmdWithResult(session,
                    "reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\" /v CurrentBuildNumber");

            if (StringUtils.isNotBlank(versionCmd)) {
                String buildNumber = OsInfoUtils.extractWindowsRegValue(versionCmd, "CurrentBuildNumber");
                if (StringUtils.isNotBlank(buildNumber)) {
                    osInfo.setDistributionVersion(buildNumber);
                    osInfo.setMajorVersion(buildNumber);
                    logger.info("获取到Windows内部版本号: {}", buildNumber);
                }
            }

            // 获取主要版本号（例如Windows 10, 11等）
            String majorVersionCmd = MinaUtils.execCmdWithResult(session,
                    "reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\" /v CurrentMajorVersionNumber");

            if (StringUtils.isNotBlank(majorVersionCmd)) {
                String majorVersionHex = OsInfoUtils.extractWindowsRegValue(majorVersionCmd,
                        "CurrentMajorVersionNumber");
                if (StringUtils.isNotBlank(majorVersionHex) && majorVersionHex.startsWith("0x")) {
                    try {
                        int majorVersion = Integer.parseInt(majorVersionHex.substring(2), 16);
                        osInfo.setMajorVersion(String.valueOf(majorVersion));
                        logger.info("获取到Windows主版本号: {}", majorVersion);
                    } catch (NumberFormatException e) {
                        logger.warn("解析Windows主版本号失败: {}", majorVersionHex);
                    }
                }
            }

            // 如果通过注册表没有获取到信息，使用ver命令
            if (StringUtils.isBlank(osInfo.getFullName())) {
                String verOutput = MinaUtils.execCmdWithResult(session, "ver");
                if (StringUtils.isNotBlank(verOutput)) {
                    osInfo.setFullName(verOutput.trim());
                    osInfo.setDistributionName("Windows");
                    osInfo.setDistributionId("windows");
                    osInfo.setDisplayName(verOutput.trim());

                    // 简单解析版本号
                    if (verOutput.contains("Version")) {
                        int start = verOutput.indexOf("Version") + 8;
                        int end = verOutput.indexOf("]", start);
                        if (start > 0 && end > start) {
                            String version = verOutput.substring(start, end).trim();
                            osInfo.setDistributionVersion(version);

                            // 设置主版本号（取第一个点前的数字）
                            if (version.contains(".")) {
                                osInfo.setMajorVersion(version.split("\\.")[0]);
                            } else {
                                osInfo.setMajorVersion(version);
                            }
                        }
                    }

                    logger.info("通过ver命令获取到Windows版本: {}", verOutput.trim());
                }
            }

            // 获取内核版本
            String kernelVersion = MinaUtils.execCmdWithResult(session,
                    "powershell -command \"[Environment]::OSVersion.Version.ToString()\"");
            if (StringUtils.isNotBlank(kernelVersion)) {
                osInfo.setKernelVersion(kernelVersion.trim());
                logger.info("获取到Windows内核版本: {}", kernelVersion.trim());
            }

        } catch (Exception e) {
            logger.error("收集Windows版本信息时出错: {}", e.getMessage(), e);
            osInfo.setDistributionId("windows");
            osInfo.setDistributionName("Windows");
            osInfo.setFullName("Unknown Windows Version");
            osInfo.setDisplayName("Unknown Windows Version");
        }
    }

    /**
     * 收集CPU信息
     * 单独提取方法以便队列系统调用
     * 
     * @param osInfo  操作系统信息对象
     * @param session SSH会话
     */
    public void collectCpuInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集Windows CPU信息");

        try {
            // 使用wmic命令获取CPU信息
            String cpuInfoCmd = "wmic cpu get Name, NumberOfCores, NumberOfLogicalProcessors, MaxClockSpeed /Value";
            String cpuInfo = MinaUtils.execCmdWithResult(session, cpuInfoCmd);

            if (StringUtils.isNotBlank(cpuInfo)) {
                // 解析CPU型号
                Pattern namePattern = Pattern.compile("Name=(.+)");
                Matcher nameMatcher = namePattern.matcher(cpuInfo);
                if (nameMatcher.find()) {
                    String cpuModel = nameMatcher.group(1).trim();
                    osInfo.setCpuModel(cpuModel);
                    osInfo.setCpuInfo(cpuModel); // 同时设置完整信息
                    logger.debug("获取到CPU型号: {}", cpuModel);
                }

                // 解析CPU频率
                Pattern freqPattern = Pattern.compile("MaxClockSpeed=(\\d+)");
                Matcher freqMatcher = freqPattern.matcher(cpuInfo);
                if (freqMatcher.find()) {
                    try {
                        int freqMHz = Integer.parseInt(freqMatcher.group(1).trim());
                        // 转换MHz为GHz
                        double freqGHz = Math.round(freqMHz / 1000.0 * 100) / 100.0;
                        osInfo.setCpuFrequency(freqGHz);
                        logger.debug("获取到CPU频率: {} GHz", freqGHz);
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU频率失败: {}", e.getMessage());
                    }
                }

                // 解析物理核心数
                Pattern coresPattern = Pattern.compile("NumberOfCores=(\\d+)");
                Matcher coresMatcher = coresPattern.matcher(cpuInfo);
                if (coresMatcher.find()) {
                    try {
                        int cores = Integer.parseInt(coresMatcher.group(1).trim());
                        osInfo.setCpuCores(cores);
                        osInfo.setCpuCoreNum(cores); // 设置别名
                        logger.debug("获取到CPU物理核心数: {}", cores);
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU核心数失败: {}", e.getMessage());
                    }
                }

                // 解析逻辑处理器数量
                Pattern logicalPattern = Pattern.compile("NumberOfLogicalProcessors=(\\d+)");
                Matcher logicalMatcher = logicalPattern.matcher(cpuInfo);
                if (logicalMatcher.find()) {
                    try {
                        int logical = Integer.parseInt(logicalMatcher.group(1).trim());
                        osInfo.setCpuLogicalCores(logical);

                        // 计算每核心的线程数
                        if (osInfo.getCpuCores() != null && osInfo.getCpuCores() > 0) {
                            int threadsPerCore = logical / osInfo.getCpuCores();
                            osInfo.setCpuThreadsPerCore(threadsPerCore);
                            logger.debug("计算得到每核心线程数: {}", threadsPerCore);
                        }

                        logger.debug("获取到CPU逻辑处理器数量: {}", logical);
                    } catch (NumberFormatException e) {
                        logger.warn("解析逻辑处理器数量失败: {}", e.getMessage());
                    }
                }

                // 设置CPU物理数量，Windows默认为1
                osInfo.setCpuCount(1);

                // 计算每颗CPU的核心数
                if (osInfo.getCpuCores() != null) {
                    osInfo.setCpuCoresPerProcessor(osInfo.getCpuCores());
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("cpuInfo");

            logger.info("Windows CPU信息收集完成");
        } catch (Exception e) {
            logger.error("收集Windows CPU信息时出错: {}", e.getMessage(), e);
            throw e; // 向上抛出异常，由调用者处理
        }
    }

    /**
     * 收集内存信息
     * 单独提取方法以便队列系统调用
     * 
     * @param osInfo  操作系统信息对象
     * @param session SSH会话
     */
    public void collectMemoryInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集Windows内存信息");

        try {
            // 使用wmic命令获取内存信息
            String memInfoCmd = "wmic OS get TotalVisibleMemorySize, FreePhysicalMemory /Value";
            String memInfo = MinaUtils.execCmdWithResult(session, memInfoCmd);

            if (StringUtils.isNotBlank(memInfo)) {
                // 解析总内存
                Pattern totalPattern = Pattern.compile("TotalVisibleMemorySize=(\\d+)");
                Matcher totalMatcher = totalPattern.matcher(memInfo);
                if (totalMatcher.find()) {
                    try {
                        long totalMemKB = Long.parseLong(totalMatcher.group(1).trim());
                        // 保存原始字节数
                        osInfo.setTotalMem(totalMemKB * 1024);
                        // 转换为GB并保留一位小数
                        double totalMemGB = Math.round(totalMemKB / 1024.0 / 1024.0 * 10) / 10.0;
                        osInfo.setTotalMemory(totalMemGB);
                        logger.debug("获取到总内存: {} GB", totalMemGB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析总内存失败: {}", e.getMessage());
                    }
                }

                // 解析可用内存
                Pattern freePattern = Pattern.compile("FreePhysicalMemory=(\\d+)");
                Matcher freeMatcher = freePattern.matcher(memInfo);
                if (freeMatcher.find()) {
                    try {
                        long freeMemKB = Long.parseLong(freeMatcher.group(1).trim());
                        // 保存原始字节数
                        osInfo.setAvailableMem(freeMemKB * 1024);
                        // 转换为GB并保留一位小数
                        double freeMemGB = Math.round(freeMemKB / 1024.0 / 1024.0 * 10) / 10.0;
                        osInfo.setAvailableMemory(freeMemGB);
                        logger.debug("获取到可用内存: {} GB", freeMemGB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析可用内存失败: {}", e.getMessage());
                    }
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("memoryInfo");

            logger.info("Windows内存信息收集完成");
        } catch (Exception e) {
            logger.error("收集Windows内存信息时出错: {}", e.getMessage(), e);
            throw e; // 向上抛出异常，由调用者处理
        }
    }

    /**
     * 收集存储信息
     * 单独提取方法以便队列系统调用
     * 
     * @param osInfo  操作系统信息对象
     * @param session SSH会话
     */
    public void collectStorageInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集Windows存储信息");

        try {
            // 使用wmic命令获取磁盘信息
            String diskInfoCmd = "wmic logicaldisk get DeviceID, Size, FreeSpace /Value";
            String diskInfo = MinaUtils.execCmdWithResult(session, diskInfoCmd);

            if (StringUtils.isNotBlank(diskInfo)) {
                // 分割每个磁盘的信息
                String[] diskEntries = diskInfo.split("\r\n\r\n");

                long totalBytes = 0;
                long availableBytes = 0;

                for (String entry : diskEntries) {
                    if (StringUtils.isBlank(entry))
                        continue;

                    // 提取设备ID、大小和可用空间
                    String deviceId = null;
                    Long size = null;
                    Long freeSpace = null;

                    Pattern devicePattern = Pattern.compile("DeviceID=([A-Z]:)");
                    Matcher deviceMatcher = devicePattern.matcher(entry);
                    if (deviceMatcher.find()) {
                        deviceId = deviceMatcher.group(1);
                    }

                    Pattern sizePattern = Pattern.compile("Size=(\\d+)");
                    Matcher sizeMatcher = sizePattern.matcher(entry);
                    if (sizeMatcher.find()) {
                        try {
                            size = Long.parseLong(sizeMatcher.group(1));
                        } catch (NumberFormatException e) {
                            logger.warn("解析磁盘大小失败: {}", entry);
                        }
                    }

                    Pattern freePattern = Pattern.compile("FreeSpace=(\\d+)");
                    Matcher freeMatcher = freePattern.matcher(entry);
                    if (freeMatcher.find()) {
                        try {
                            freeSpace = Long.parseLong(freeMatcher.group(1));
                        } catch (NumberFormatException e) {
                            logger.warn("解析磁盘可用空间失败: {}", entry);
                        }
                    }

                    // 如果获取到了所有信息，则累加到总量
                    if (deviceId != null && size != null && freeSpace != null) {
                        // 排除特殊驱动器（如光驱或网络驱动器，通常这些都有大小）
                        if (size > 0) {
                            totalBytes += size;
                            availableBytes += freeSpace;
                            logger.debug("磁盘 {} 大小: {} GB, 可用: {} GB",
                                    deviceId,
                                    Math.round(size / 1024.0 / 1024.0 / 1024.0 * 10) / 10.0,
                                    Math.round(freeSpace / 1024.0 / 1024.0 / 1024.0 * 10) / 10.0);
                        }
                    }
                }

                // 保存原始字节数
                osInfo.setTotalDiskBytes(totalBytes);
                osInfo.setAvailableDiskBytes(availableBytes);

                // 转换为GB并保留一位小数
                double totalDiskGB = Math.round(totalBytes / 1024.0 / 1024.0 / 1024.0 * 10) / 10.0;
                double availableDiskGB = Math.round(availableBytes / 1024.0 / 1024.0 / 1024.0 * 10) / 10.0;

                // 使用接受Long类型的setter方法
                osInfo.setTotalDisk(totalBytes);
                osInfo.setAvailableDisk(availableBytes);

                logger.debug("获取到磁盘总容量: {} GB, 可用容量: {} GB", totalDiskGB, availableDiskGB);
            }

            // Windows虚拟内存信息（交换空间）
            String pagingInfoCmd = "wmic pagefile get CurrentUsage, AllocatedBaseSize /Value";
            String pagingInfo = MinaUtils.execCmdWithResult(session, pagingInfoCmd);

            if (StringUtils.isNotBlank(pagingInfo)) {
                // 解析交换空间（分页文件）总容量
                Pattern totalSwapPattern = Pattern.compile("AllocatedBaseSize=(\\d+)");
                Matcher totalSwapMatcher = totalSwapPattern.matcher(pagingInfo);
                if (totalSwapMatcher.find()) {
                    try {
                        long totalSwapMB = Long.parseLong(totalSwapMatcher.group(1).trim());
                        // 计算字节数
                        long totalSwapBytes = totalSwapMB * 1024 * 1024;
                        // 保存原始字节数
                        osInfo.setTotalSwapBytes(totalSwapBytes);
                        // 使用接受Long类型的setter方法
                        osInfo.setTotalSwap(totalSwapBytes);

                        // 计算GB值用于日志
                        double totalSwapGB = Math.round(totalSwapMB / 1024.0 * 10) / 10.0;
                        logger.debug("获取到交换空间总容量: {} GB", totalSwapGB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析交换空间总容量失败: {}", e.getMessage());
                    }
                }

                // 解析交换空间使用量
                Pattern usageSwapPattern = Pattern.compile("CurrentUsage=(\\d+)");
                Matcher usageSwapMatcher = usageSwapPattern.matcher(pagingInfo);
                if (usageSwapMatcher.find() && osInfo.getTotalSwapBytes() != null) {
                    try {
                        long usageSwapMB = Long.parseLong(usageSwapMatcher.group(1).trim());
                        // 计算可用空间
                        long totalSwapMB = osInfo.getTotalSwapBytes() / (1024 * 1024);
                        long availableSwapMB = totalSwapMB - usageSwapMB;
                        if (availableSwapMB < 0)
                            availableSwapMB = 0;

                        // 计算字节数
                        long availableSwapBytes = availableSwapMB * 1024 * 1024;
                        // 保存原始字节数
                        osInfo.setAvailableSwapBytes(availableSwapBytes);
                        // 使用接受Long类型的setter方法
                        osInfo.setAvailableSwap(availableSwapBytes);

                        // 计算GB值用于日志
                        double availableSwapGB = Math.round(availableSwapMB / 1024.0 * 10) / 10.0;
                        logger.debug("获取到交换空间可用容量: {} GB", availableSwapGB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析交换空间使用量失败: {}", e.getMessage());
                    }
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("diskInfo");

            logger.info("Windows存储信息收集完成");
        } catch (Exception e) {
            logger.error("收集Windows存储信息时出错: {}", e.getMessage(), e);
            throw e; // 向上抛出异常，由调用者处理
        }
    }

    /**
     * 收集GPU信息
     * 单独提取方法以便队列系统调用
     * 
     * @param osInfo  操作系统信息对象
     * @param session SSH会话
     */
    public void collectGpuInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集Windows GPU信息");

        try {
            // 使用wmic命令获取显卡信息
            String gpuInfoCmd = "wmic path win32_VideoController get Name, AdapterRAM /Value";
            String gpuInfo = MinaUtils.execCmdWithResult(session, gpuInfoCmd);

            if (StringUtils.isNotBlank(gpuInfo)) {
                // 解析GPU名称
                Pattern namePattern = Pattern.compile("Name=(.+)");
                Matcher nameMatcher = namePattern.matcher(gpuInfo);
                if (nameMatcher.find()) {
                    String gpuName = nameMatcher.group(1).trim();
                    osInfo.setGpuInfo(gpuName);
                    logger.debug("获取到GPU名称: {}", gpuName);
                }

                // 解析GPU显存
                Pattern ramPattern = Pattern.compile("AdapterRAM=(\\d+)");
                Matcher ramMatcher = ramPattern.matcher(gpuInfo);
                if (ramMatcher.find()) {
                    try {
                        long ramBytes = Long.parseLong(ramMatcher.group(1).trim());
                        // 转换为GB并保留一位小数
                        double ramGB = Math.round(ramBytes / 1024.0 / 1024.0 / 1024.0 * 10) / 10.0;
                        osInfo.setGpuMemory(ramGB);
                        logger.debug("获取到GPU显存: {} GB", ramGB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析GPU显存失败: {}", e.getMessage());
                    }
                }
            } else {
                // 如果wmic命令没有返回有效结果，尝试使用PowerShell
                String psGpuCmd = "powershell -Command \"Get-WmiObject -Class Win32_VideoController | Select-Object -Property Name, AdapterRAM | ConvertTo-Csv -NoTypeInformation\"";
                String psGpuInfo = MinaUtils.execCmdWithResult(session, psGpuCmd);

                if (StringUtils.isNotBlank(psGpuInfo) && psGpuInfo.contains("Name")) {
                    String[] lines = psGpuInfo.split("\r\n");
                    if (lines.length > 1) {
                        // 解析CSV格式输出
                        String dataLine = lines[1];
                        String[] values = dataLine.split(",");

                        if (values.length >= 1) {
                            String gpuName = values[0].replace("\"", "").trim();
                            osInfo.setGpuInfo(gpuName);
                            logger.debug("通过PowerShell获取到GPU名称: {}", gpuName);

                            if (values.length >= 2) {
                                try {
                                    String ramValue = values[1].replace("\"", "").trim();
                                    if (!ramValue.isEmpty() && !ramValue.equalsIgnoreCase("null")) {
                                        long ramBytes = Long.parseLong(ramValue);
                                        // 转换为GB并保留一位小数
                                        double ramGB = Math.round(ramBytes / 1024.0 / 1024.0 / 1024.0 * 10) / 10.0;
                                        osInfo.setGpuMemory(ramGB);
                                        logger.debug("通过PowerShell获取到GPU显存: {} GB", ramGB);
                                    }
                                } catch (NumberFormatException e) {
                                    logger.warn("通过PowerShell解析GPU显存失败: {}", e.getMessage());
                                }
                            }
                        }
                    }
                } else {
                    osInfo.setGpuInfo("无GPU或无法检测");
                    logger.debug("未检测到GPU信息");
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("gpuInfo");

            logger.info("Windows GPU信息收集完成");
        } catch (Exception e) {
            logger.error("收集Windows GPU信息时出错: {}", e.getMessage(), e);
            throw e; // 向上抛出异常，由调用者处理
        }
    }

    /**
     * 格式化字节数为GB，保留两位小数
     */
    private String formatGigabytes(Long bytes) {
        return String.format("%.2f", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 格式化字节数为GB，保留两位小数（Double版本）
     */
    private String formatGigabytes(Double bytes) {
        return String.format("%.2f", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}