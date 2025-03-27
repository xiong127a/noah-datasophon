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
    public OsInfo collectOsInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo) {
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
                }
            }

            // 获取操作系统版本信息（已使用注册表方式）
            collectWindowsVersionInfo(osInfo, session);

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
                }
            }

            osInfo.setValid(true);
            return osInfo;
        } catch (Exception e) {
            logger.error("收集Windows操作系统信息时出错: {}", e.getMessage(), e);
            osInfo.setValid(false);
            return osInfo;
        }
    }

    @Override
    public void collectHardwareInfo(OsInfo osInfo, ClientSession session) {
        try {
            osInfo.setHardwareCollectionStatus("loading");

            logger.info("开始收集Windows硬件信息");

            // 获取CPU信息
            collectCpuInfo(osInfo, session);

            // 获取内存信息
            collectMemoryInfo(osInfo, session);

            // 获取存储信息
            collectStorageInfo(osInfo, session);

            // 获取GPU信息
            collectGpuInfo(osInfo, session);

            osInfo.setHardwareCollectionStatus("success");
            logger.info("Windows硬件信息收集完成");
        } catch (Exception e) {
            logger.error("收集Windows硬件信息时出错: {}", e.getMessage(), e);
            osInfo.setHardwareCollectionStatus("error");
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
     */
    private void collectCpuInfo(OsInfo osInfo, ClientSession session) {
        try {
            logger.info("开始收集CPU信息");
            // 设置当前正在更新的项
            osInfo.setLastUpdatedItem("collecting_cpu");

            // 通过注册表获取CPU信息
            String regPath = "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor";

            // 获取CPU型号
            String cpuModelReg = MinaUtils.execCmdWithResult(session,
                    "reg query " + regPath + "\\0 /v ProcessorNameString");
            if (StringUtils.isNotBlank(cpuModelReg)) {
                Pattern pattern = Pattern.compile("ProcessorNameString\\s+REG_SZ\\s+(.+)");
                Matcher matcher = pattern.matcher(cpuModelReg);
                if (matcher.find()) {
                    osInfo.setCpuModel(matcher.group(1).trim());
                    logger.info("通过注册表获取到CPU型号: {}", osInfo.getCpuModel());
                }
            }

            // 获取CPU核心数（物理核心）
            String cpuCountReg = MinaUtils.execCmdWithResult(session,
                    "reg query " + regPath + " /s /v FeatureSet");
            if (StringUtils.isNotBlank(cpuCountReg)) {
                int coreCount = StringUtils.countMatches(cpuCountReg, "HKEY_LOCAL_MACHINE");
                osInfo.setCpuCoreNum(coreCount);
                logger.info("通过注册表获取到CPU物理核心数: {}", coreCount);
            }

            // 获取CPU频率
            String cpuFreqReg = MinaUtils.execCmdWithResult(session,
                    "reg query " + regPath + "\\0 /v ~MHz");
            if (StringUtils.isNotBlank(cpuFreqReg)) {
                Pattern pattern = Pattern.compile("~MHz\\s+REG_DWORD\\s+0x([0-9a-fA-F]+)");
                Matcher matcher = pattern.matcher(cpuFreqReg);
                if (matcher.find()) {
                    double freq = Long.parseLong(matcher.group(1), 16) / 1000.0;
                    osInfo.setCpuFrequency(freq);
                    logger.info("通过注册表获取到CPU频率: {}GHz", freq);
                }
            }

            // 获取CPU逻辑核心数
            String logicalCoresCmd = MinaUtils.execCmdWithResult(session,
                    "powershell -command \"(Get-WmiObject -class Win32_processor).NumberOfLogicalProcessors\"");
            if (StringUtils.isNotBlank(logicalCoresCmd)) {
                try {
                    int logicalCores = Integer.parseInt(logicalCoresCmd.trim());
                    osInfo.setCpuLogicalCores(logicalCores);

                    // 计算每核心的线程数
                    if (osInfo.getCpuCores() > 0) {
                        osInfo.setCpuThreadsPerCore(logicalCores / osInfo.getCpuCores());
                    }

                    logger.info("获取到CPU逻辑处理器数量: {}, 每核心线程数: {}",
                            logicalCores, osInfo.getCpuThreadsPerCore());
                } catch (NumberFormatException e) {
                    logger.warn("解析CPU逻辑处理器数量失败: {}", logicalCoresCmd);
                }
            }

            // 更新最后收集的项目为CPU信息
            osInfo.setLastUpdatedItem("cpuInfo");
        } catch (Exception e) {
            logger.error("通过注册表收集CPU信息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 收集内存信息
     */
    private void collectMemoryInfo(OsInfo osInfo, ClientSession session) {
        try {
            logger.info("开始收集内存信息");
            // 设置当前正在更新的项
            osInfo.setLastUpdatedItem("collecting_memory");

            // 通过注册表获取物理内存总量
            String regQuery = MinaUtils.execCmdWithResult(session,
                    "reg query \"HKLM\\HARDWARE\\RESOURCEMAP\\System Resources\\Physical Memory\" /v .Translated");

            if (StringUtils.isNotBlank(regQuery)) {
                Pattern pattern = Pattern.compile("Memory Range.*?Length\\s+0x([0-9a-fA-F]+)");
                Matcher matcher = pattern.matcher(regQuery);
                long totalMem = 0;
                while (matcher.find()) {
                    totalMem += Long.parseLong(matcher.group(1), 16);
                }
                osInfo.setTotalMem(totalMem);
                logger.info("通过注册表获取到物理内存总量: {}GB", formatGigabytes(totalMem));
            }

            // 获取可用内存（需要配合性能计数器）
            String perfQuery = MinaUtils.execCmdWithResult(session,
                    "reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009\" /v Counter");
            if (StringUtils.isNotBlank(perfQuery)) {
                String[] lines = perfQuery.split("\r\n");
                for (String line : lines) {
                    if (line.contains("Available Bytes")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            String counterIndex = parts[0];
                            String availableMem = MinaUtils.execCmdWithResult(session,
                                    "typeperf \"\\Memory\\Available Bytes\" -sc 1");
                            if (StringUtils.isNotBlank(availableMem)) {
                                Pattern memPattern = Pattern.compile("\"([0-9.]+)\"");
                                Matcher memMatcher = memPattern.matcher(availableMem);
                                if (memMatcher.find()) {
                                    long availableBytes = (long) (Double.parseDouble(memMatcher.group(1)));
                                    osInfo.setAvailableMem(availableBytes);
                                    logger.info("通过性能计数器获取到可用内存: {}GB", formatGigabytes(availableBytes));
                                }
                            }
                        }
                        break;
                    }
                }
            }

            // 如果无法通过性能计数器获取可用内存，尝试使用PowerShell
            if (osInfo.getAvailableMem() == 0) {
                String psAvailableMem = MinaUtils.execCmdWithResult(session,
                        "powershell -command \"(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory * 1024\"");
                if (StringUtils.isNotBlank(psAvailableMem)) {
                    try {
                        long availableBytes = Long.parseLong(psAvailableMem.trim());
                        osInfo.setAvailableMem(availableBytes);
                        logger.info("通过PowerShell获取到可用内存: {}GB", formatGigabytes(availableBytes));
                    } catch (NumberFormatException e) {
                        logger.warn("解析PowerShell可用内存失败: {}", psAvailableMem);
                    }
                }
            }

            // 更新最后收集的项目为内存信息
            osInfo.setLastUpdatedItem("memoryInfo");
        } catch (Exception e) {
            logger.error("通过注册表收集内存信息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 收集存储信息
     */
    private void collectStorageInfo(OsInfo osInfo, ClientSession session) {
        try {
            logger.info("开始收集存储信息");
            // 设置当前正在更新的项
            osInfo.setLastUpdatedItem("collecting_disk");

            // 获取C盘总大小和可用空间
            String totalDisk = MinaUtils.execCmdWithResult(session,
                    "powershell -command \"(Get-PSDrive C).Used + (Get-PSDrive C).Free\"");
            if (StringUtils.isNotBlank(totalDisk)) {
                try {
                    osInfo.setTotalDisk(Long.parseLong(totalDisk.trim()));
                    logger.info("获取到C盘总大小: {}GB", formatGigabytes(osInfo.getTotalDisk()));
                } catch (NumberFormatException e) {
                    logger.warn("解析C盘总大小失败: {}", totalDisk);
                }
            }

            String freeDisk = MinaUtils.execCmdWithResult(session,
                    "powershell -command \"(Get-PSDrive C).Free\"");
            if (StringUtils.isNotBlank(freeDisk)) {
                try {
                    osInfo.setAvailableDisk(Long.parseLong(freeDisk.trim()));
                    logger.info("获取到C盘可用空间: {}GB", formatGigabytes(osInfo.getAvailableDisk()));
                } catch (NumberFormatException e) {
                    logger.warn("解析C盘可用空间失败: {}", freeDisk);
                }
            }

            // 更新最后收集的项目为磁盘信息
            osInfo.setLastUpdatedItem("diskInfo");
        } catch (Exception e) {
            logger.error("收集存储信息时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 收集GPU信息
     */
    private void collectGpuInfo(OsInfo osInfo, ClientSession session) {
        try {
            logger.info("开始收集GPU信息");
            // 设置当前正在更新的项
            osInfo.setLastUpdatedItem("collecting_gpu");

            StringBuilder gpuInfo = new StringBuilder();
            StringBuilder gpuMemory = new StringBuilder();

            // 通过注册表枚举所有显示适配器
            String regPath = "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}";
            String regQuery = MinaUtils.execCmdWithResult(session,
                    "reg query " + regPath + " /s /v DriverDesc");

            // 解析显卡型号
            if (StringUtils.isNotBlank(regQuery)) {
                Pattern pattern = Pattern.compile("DriverDesc\\s+REG_SZ\\s+(.+)");
                Matcher matcher = pattern.matcher(regQuery);
                while (matcher.find()) {
                    String model = matcher.group(1);
                    gpuInfo.append(model).append("; ");
                }
            }

            // 获取显存信息（单位MB）
            String memoryQuery = MinaUtils.execCmdWithResult(session,
                    "reg query " + regPath + "\\0000 /v HardwareInformation.qwMemorySize");
            if (StringUtils.isNotBlank(memoryQuery)) {
                Pattern pattern = Pattern.compile("qwMemorySize\\s+REG_BINARY\\s+([0-9a-fA-F]+)");
                Matcher matcher = pattern.matcher(memoryQuery);
                if (matcher.find()) {
                    try {
                        // 将十六进制转换为十进制
                        long bytes = Long.parseLong(matcher.group(1), 16);
                        double memoryGB = bytes / (1024.0 * 1024.0 * 1024.0);
                        gpuMemory.append(String.format("%.1fGB", memoryGB));
                    } catch (NumberFormatException e) {
                        logger.error("解析显存大小失败: {}", memoryQuery);
                    }
                }
            }

            // 如果通过注册表没有获取到信息，尝试使用PowerShell
            if (gpuInfo.length() == 0) {
                String psGpuInfo = MinaUtils.execCmdWithResult(session,
                        "powershell -command \"(Get-WmiObject Win32_VideoController).Name\"");
                if (StringUtils.isNotBlank(psGpuInfo)) {
                    gpuInfo.append(psGpuInfo.trim());
                }
            }

            // 设置最终结果
            if (gpuInfo.length() > 0) {
                osInfo.setGpuInfo(gpuInfo.toString().replaceAll("; $", ""));
            } else {
                osInfo.setGpuInfo("未知");
            }

            if (gpuMemory.length() > 0) {
                osInfo.setGpuMemory(Double.parseDouble(gpuMemory.toString().replace("GB", "")));
            } else {
                osInfo.setGpuMemory(0.0);
            }

            // 更新最后收集的项目为GPU信息
            osInfo.setLastUpdatedItem("gpuInfo");
        } catch (Exception e) {
            logger.error("通过注册表获取GPU信息失败: {}", e.getMessage());
            osInfo.setGpuInfo("未知");
            osInfo.setGpuMemory(0.0);
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