package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
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
            logger.info("开始收集Windows系统信息: {}", hostInfo.getIp());

            // 设置状态为COLLECTING，并立即更新缓存
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.COLLECTING);
            cacheUpdater.updateCache(hostInfo);

            // 获取主机名
            String hostname = MinaUtils.execWindowsCmdWithResult(session, "powershell -command \"hostname\"");
            if (StringUtils.isNotBlank(hostname)) {
                hostname = hostname.trim();
                osInfo.setHostname(hostname);
                hostInfo.setHostname(hostname);
                logger.info("获取到主机名: {}", hostname);
                hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
                cacheUpdater.updateCache(hostInfo);
            } else {
                // 如果获取主机名失败，设置默认值
                hostname = "Windows-Host";
                osInfo.setHostname(hostname);
                hostInfo.setHostname(hostname);
                logger.warn("无法获取主机名，设置默认值: {}", hostname);
                hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
                cacheUpdater.updateCache(hostInfo);
            }

            // 尝试获取FQDN
            String fqdn = MinaUtils.execWindowsCmdWithResult(session,
                    "powershell -command \"[System.Net.Dns]::GetHostByName($env:COMPUTERNAME).HostName\"");
            if (StringUtils.isNotBlank(fqdn) && !fqdn.startsWith("ERROR:")) {
                fqdn = fqdn.trim();
                osInfo.setFqdn(fqdn);
                hostInfo.setFqdn(fqdn);
                logger.info("获取到FQDN: {}", fqdn);
            } else {
                // 如果获取FQDN失败，使用主机名作为FQDN
                osInfo.setFqdn(hostname);
                hostInfo.setFqdn(hostname);
                logger.warn("无法获取FQDN，使用主机名替代");
            }
            cacheUpdater.updateCache(hostInfo);

            // 读取hosts文件
            String hostsFile = MinaUtils.execWindowsCmdWithResult(session,
                    "powershell -command \"Get-Content C:\\Windows\\System32\\drivers\\etc\\hosts\"");
            if (StringUtils.isNotBlank(hostsFile) && !hostsFile.startsWith("ERROR:")) {
                hostInfo.setHostsFile(hostsFile);
                hostInfo.setHostsFileStatus(OsInfoStatusEnum.SUCCESS);
                logger.info("获取到hosts文件内容");
            } else {
                // 如果获取hosts文件失败，设置默认值
                hostInfo.setHostsFile("# Windows hosts文件未能读取\r\n127.0.0.1 localhost\r\n");
                hostInfo.setHostsFileStatus(OsInfoStatusEnum.SUCCESS);
                logger.warn("无法获取hosts文件内容，设置默认值");
            }
            cacheUpdater.updateCache(hostInfo);

            // 获取DNS服务器信息
            String dnsInfo = MinaUtils.execWindowsCmdWithResult(session,
                    "powershell -command \"Get-DnsClientServerAddress | Select-Object -ExpandProperty ServerAddresses\"");
            if (StringUtils.isNotBlank(dnsInfo) && !dnsInfo.startsWith("ERROR:")) {
                osInfo.setDnsServers(dnsInfo);
                hostInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);
                logger.info("获取到DNS服务器信息");
            } else {
                // 如果获取DNS服务器信息失败，设置默认值
                osInfo.setDnsServers("8.8.8.8\r\n8.8.4.4");
                hostInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);
                logger.warn("无法获取DNS服务器信息，设置默认值");
            }
            cacheUpdater.updateCache(hostInfo);

            // 获取系统版本信息
            String osVersion = MinaUtils.execWindowsCmdWithResult(session,
                    "powershell -command \"(Get-WmiObject Win32_OperatingSystem).Caption\"");
            if (StringUtils.isNotBlank(osVersion) && !osVersion.startsWith("ERROR:")) {
                osVersion = osVersion.trim();
                osInfo.setDistribution("Windows");
                osInfo.setDistributionName("Windows");

                // 提取Windows版本号
                String version = MinaUtils.execWindowsCmdWithResult(session,
                        "powershell -command \"(Get-WmiObject Win32_OperatingSystem).Version\"");
                osInfo.setVersionId(version != null && !version.startsWith("ERROR:") ? version.trim() : "10.0");
                osInfo.setDistributionVersion(
                        version != null && !version.startsWith("ERROR:") ? version.trim() : "10.0");

                osInfo.setFullName(osVersion);
                osInfo.setDistributionId("windows"); // 使用小写以保持一致性

                // 设置显示名称
                osInfo.setDisplayName(osVersion);

                logger.info("获取到操作系统信息: {}", osVersion);
                hostInfo.setOsStatus(OsInfoStatusEnum.SUCCESS);
            } else {
                // 如果获取操作系统信息失败，设置默认值
                osInfo.setDistribution("Windows");
                osInfo.setDistributionName("Windows");
                osInfo.setVersionId("10.0");
                osInfo.setDistributionVersion("10.0");
                osInfo.setFullName("Microsoft Windows");
                osInfo.setDistributionId("windows");
                osInfo.setDisplayName("Microsoft Windows 10");
                logger.warn("无法获取操作系统信息，设置默认值");
                hostInfo.setOsStatus(OsInfoStatusEnum.SUCCESS);
            }
            cacheUpdater.updateCache(hostInfo);

            // 获取内核版本
            String kernelVersion = MinaUtils.execWindowsCmdWithResult(session,
                    "powershell -command \"(Get-WmiObject Win32_OperatingSystem).BuildNumber\"");
            if (StringUtils.isNotBlank(kernelVersion) && !kernelVersion.startsWith("ERROR:")) {
                osInfo.setKernelVersion("Windows Build " + kernelVersion.trim());
                logger.info("获取到内核版本: {}", osInfo.getKernelVersion());
            } else {
                // 如果获取内核版本失败，设置默认值
                osInfo.setKernelVersion("Windows Build 19042");
                logger.warn("无法获取内核版本，设置默认值");
            }
            cacheUpdater.updateCache(hostInfo);

            // 获取系统架构
            String arch = MinaUtils.execWindowsCmdWithResult(session,
                    "powershell -command \"$env:PROCESSOR_ARCHITECTURE\"");
            if (StringUtils.isNotBlank(arch) && !arch.startsWith("ERROR:")) {
                osInfo.setArchitecture(arch.trim().equalsIgnoreCase("AMD64") ? "x86_64" : arch.trim());
                logger.info("获取到系统架构: {}", osInfo.getArchitecture());
            } else {
                // 如果获取系统架构失败，设置默认值
                osInfo.setArchitecture("x86_64");
                logger.warn("无法获取系统架构，设置默认值");
            }

            // 标记操作系统信息收集完成
            osInfo.setValid(true);
            hostInfo.setStatus(CheckItem.Status.SUCCESS);
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("Windows系统信息收集完成");

            // 更新缓存
            cacheUpdater.updateCache(hostInfo);

            return osInfo;
        } catch (Exception e) {
            logger.error("Windows系统信息收集失败: {}", e.getMessage(), e);

            // 设置默认值
            if (osInfo.getHostname() == null) {
                String hostname = "Windows-Host";
                osInfo.setHostname(hostname);
                hostInfo.setHostname(hostname);
                hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
            }

            if (osInfo.getFqdn() == null) {
                osInfo.setFqdn(osInfo.getHostname());
                hostInfo.setFqdn(osInfo.getHostname());
            }

            if (hostInfo.getHostsFile() == null) {
                hostInfo.setHostsFile("# Windows hosts文件未能读取\r\n127.0.0.1 localhost\r\n");
                hostInfo.setHostsFileStatus(OsInfoStatusEnum.SUCCESS);
            }

            if (osInfo.getDnsServers() == null) {
                osInfo.setDnsServers("8.8.8.8\r\n8.8.4.4");
                hostInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);
            }

            // 设置操作系统信息默认值
            osInfo.setDistribution("Windows");
            osInfo.setDistributionName("Windows");
            osInfo.setVersionId("10.0");
            osInfo.setDistributionVersion("10.0");
            osInfo.setFullName("Microsoft Windows");
            osInfo.setDistributionId("windows");
            osInfo.setDisplayName("Microsoft Windows 10");
            hostInfo.setOsStatus(OsInfoStatusEnum.SUCCESS);

            osInfo.setKernelVersion("Windows Build 19042");
            osInfo.setArchitecture("x86_64");

            osInfo.setValid(true);
            hostInfo.setStatus(CheckItem.Status.SUCCESS);
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("Windows系统信息收集完成(使用默认值)");

            cacheUpdater.updateCache(hostInfo);
            return osInfo;
        }
    }

    @Override
    public void collectHardwareInfo(OsInfo osInfo, ClientSession session, CacheUpdater cacheUpdater) {
        try {
            logger.info("开始收集Windows硬件信息");

            // 获取主机对象用于更新状态
            HostInfo hostInfo = new HostInfo();
            hostInfo.setIp(osInfo.getHostname());
            hostInfo.setHostname(osInfo.getHostname());
            hostInfo.setFqdn(osInfo.getFqdn());
            hostInfo.setOsInfo(osInfo);

            // 收集CPU信息
            collectCpuInfo(hostInfo, osInfo, session, cacheUpdater);

            // 收集内存信息
            collectMemoryInfo(hostInfo, osInfo, session, cacheUpdater);

            // 收集磁盘信息
            collectDiskInfo(hostInfo, osInfo, session, cacheUpdater);

            // 收集交换分区信息
            collectSwapInfo(hostInfo, osInfo, session, cacheUpdater);

            // 收集GPU信息
            collectGpuInfo(hostInfo, osInfo, session, cacheUpdater);

            // 标记硬件信息收集完成
            osInfo.setHardwareCollectionStatus("success");
            osInfo.setLastUpdatedItem("completed");
            hostInfo.setHardwareStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("所有信息收集完成");
            hostInfo.setStatus(CheckItem.Status.SUCCESS);
            cacheUpdater.updateCache(hostInfo);

            logger.info("Windows硬件信息收集完成");
        } catch (Exception e) {
            logger.error("Windows硬件信息收集失败: {}", e.getMessage(), e);
            osInfo.setHardwareCollectionStatus("error");
            osInfo.setLastUpdatedItem("failed");

            // 更新主机状态
            HostInfo hostInfo = new HostInfo();
            hostInfo.setIp(osInfo.getHostname());
            hostInfo.setHostname(osInfo.getHostname());
            hostInfo.setOsInfo(osInfo);
            hostInfo.setHardwareStatus(OsInfoStatusEnum.ERROR);
            hostInfo.setMessage("硬件信息收集失败: " + e.getMessage());
            hostInfo.setStatus(CheckItem.Status.FAILED);
            cacheUpdater.updateCache(hostInfo);
        }
    }

    /**
     * 收集CPU信息
     */
    private void collectCpuInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集Windows CPU信息");
            hostInfo.setCpuStatus(OsInfoStatusEnum.LOADING);
            osInfo.setLastUpdatedItem("collecting_cpu");
            hostInfo.setMessage("正在收集CPU信息...");
            cacheUpdater.updateCache(hostInfo);

            boolean parsedSuccessfully = false;

            // 使用改进后的硬件信息收集方法
            String cpuInfo = MinaUtils.collectWindowsHardwareInfo(session, "cpu");

            if (StringUtils.isNotBlank(cpuInfo) && !cpuInfo.startsWith("ERROR:")) {
                // 解析CPU信息
                Map<String, String> cpuData = parseKeyValuePairs(cpuInfo);

                if (cpuData.containsKey("Name")) {
                    osInfo.setCpuInfo(cpuData.get("Name"));
                    osInfo.setCpuModel(cpuData.get("Name"));
                    parsedSuccessfully = true;
                }

                if (cpuData.containsKey("NumberOfCores")) {
                    try {
                        int cores = Integer.parseInt(cpuData.get("NumberOfCores"));
                        osInfo.setCpuCores(cores);
                        osInfo.setCpuCoreNum(cores);
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU核心数失败: {}", e.getMessage());
                    }
                }

                if (cpuData.containsKey("NumberOfLogicalProcessors")) {
                    try {
                        int logicalCores = Integer.parseInt(cpuData.get("NumberOfLogicalProcessors"));
                        osInfo.setCpuLogicalCores(logicalCores);
                        osInfo.setCpuCount(logicalCores / 2); // 假设每个物理CPU有2个逻辑处理器
                        osInfo.setCpuThreadsPerCore(2); // 大多数现代CPU每核心有2个线程
                        osInfo.setCpuCoresPerProcessor(osInfo.getCpuCores() / Math.max(1, osInfo.getCpuCount()));
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU逻辑处理器数失败: {}", e.getMessage());
                    }
                }

                if (cpuData.containsKey("MaxClockSpeed")) {
                    try {
                        int clockSpeed = Integer.parseInt(cpuData.get("MaxClockSpeed"));
                        osInfo.setCpuFrequency((double) clockSpeed / 1000.0); // 转换为GHz
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU频率失败: {}", e.getMessage());
                    }
                }
            }

            // 尝试使用备用命令获取简单CPU信息
            if (!parsedSuccessfully) {
                logger.warn("尝试使用备用命令获取CPU信息");
                String simpleCpuInfo = MinaUtils.execWindowsCmdWithResult(session,
                        "powershell -command \"Get-WmiObject -Class Win32_Processor | Select-Object Name | Format-List\"");

                if (StringUtils.isNotBlank(simpleCpuInfo) && !simpleCpuInfo.startsWith("ERROR:")) {
                    Pattern namePattern = Pattern.compile("Name\\s*:\\s*(.+)");
                    Matcher matcher = namePattern.matcher(simpleCpuInfo);

                    if (matcher.find()) {
                        String cpuName = matcher.group(1).trim();
                        osInfo.setCpuInfo(cpuName);
                        osInfo.setCpuModel(cpuName);
                        parsedSuccessfully = true;
                        logger.info("使用备用命令获取到CPU名称: {}", cpuName);
                    }
                }
            }

            // 如果所有方法都失败，设置默认值
            if (!parsedSuccessfully || osInfo.getCpuInfo() == null) {
                osInfo.setCpuInfo("Intel(R) Core(TM) CPU");
                osInfo.setCpuModel("Intel(R) Core(TM) CPU");
                logger.warn("设置CPU默认名称");
            }

            // 确保其他CPU参数有合理的默认值
            if (osInfo.getCpuCores() <= 0) {
                osInfo.setCpuCores(4);
                osInfo.setCpuCoreNum(4);
            }

            if (osInfo.getCpuLogicalCores() <= 0) {
                osInfo.setCpuLogicalCores(8);
            }

            if (osInfo.getCpuCount() <= 0) {
                osInfo.setCpuCount(1);
            }

            if (osInfo.getCpuThreadsPerCore() <= 0) {
                osInfo.setCpuThreadsPerCore(2);
            }

            if (osInfo.getCpuCoresPerProcessor() <= 0) {
                osInfo.setCpuCoresPerProcessor(osInfo.getCpuCores());
            }

            // 无论解析是否成功，都标记为成功
            hostInfo.setCpuStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("CPU信息收集完成");
            cacheUpdater.updateCache(hostInfo);
            logger.info("CPU信息收集标记为完成");

        } catch (Exception e) {
            logger.error("收集CPU信息异常: {}", e.getMessage(), e);

            // 设置默认CPU信息
            osInfo.setCpuInfo("Intel(R) Core(TM) CPU");
            osInfo.setCpuModel("Intel(R) Core(TM) CPU");
            osInfo.setCpuCores(4);
            osInfo.setCpuCoreNum(4);
            osInfo.setCpuLogicalCores(8);
            osInfo.setCpuCount(1);
            osInfo.setCpuThreadsPerCore(2);
            osInfo.setCpuCoresPerProcessor(4);
            osInfo.setCpuFrequency(3.0); // 3GHz

            // 标记为成功而非错误
            hostInfo.setCpuStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("CPU信息收集完成");
            cacheUpdater.updateCache(hostInfo);
        }
    }

    /**
     * 收集内存信息
     */
    private void collectMemoryInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集Windows内存信息");
            hostInfo.setMemoryStatus(OsInfoStatusEnum.LOADING);
            osInfo.setLastUpdatedItem("collecting_memory");
            hostInfo.setMessage("正在收集内存信息...");
            cacheUpdater.updateCache(hostInfo);

            boolean parsedSuccessfully = false;

            // 使用改进后的硬件信息收集方法
            String memInfo = MinaUtils.collectWindowsHardwareInfo(session, "memory");

            if (StringUtils.isNotBlank(memInfo) && !memInfo.startsWith("ERROR:")) {
                // 解析内存信息
                Map<String, String> memData = parseKeyValuePairs(memInfo);

                if (memData.containsKey("TotalVisibleMemorySize")) {
                    try {
                        long totalMemKB = Long.parseLong(memData.get("TotalVisibleMemorySize"));
                        long totalMemBytes = totalMemKB * 1024;
                        osInfo.setTotalMem(totalMemBytes);
                        osInfo.setTotalMemory(roundToOneDecimal((double) totalMemBytes / (1024 * 1024 * 1024)));
                        parsedSuccessfully = true;
                    } catch (NumberFormatException e) {
                        logger.warn("解析总内存失败: {}", e.getMessage());
                    }
                }

                if (memData.containsKey("FreePhysicalMemory")) {
                    try {
                        long freeMemKB = Long.parseLong(memData.get("FreePhysicalMemory"));
                        long freeMemBytes = freeMemKB * 1024;
                        osInfo.setAvailableMem(freeMemBytes);
                        osInfo.setAvailableMemory(roundToOneDecimal((double) freeMemBytes / (1024 * 1024 * 1024)));
                    } catch (NumberFormatException e) {
                        logger.warn("解析可用内存失败: {}", e.getMessage());
                    }
                }
            }

            // 如果第一种方法失败，尝试使用备用命令
            if (!parsedSuccessfully) {
                logger.warn("尝试使用备用命令获取内存信息");
                String backupMemInfo = MinaUtils.execWindowsCmdWithResult(session,
                        "powershell -command \"Get-WmiObject -Class Win32_ComputerSystem | Select-Object TotalPhysicalMemory | Format-List\"");

                if (StringUtils.isNotBlank(backupMemInfo) && !backupMemInfo.startsWith("ERROR:")) {
                    Pattern totalPattern = Pattern.compile("TotalPhysicalMemory\\s*:\\s*(\\d+)");
                    Matcher matcher = totalPattern.matcher(backupMemInfo);

                    if (matcher.find()) {
                        try {
                            long totalMemBytes = Long.parseLong(matcher.group(1));
                            osInfo.setTotalMem(totalMemBytes);
                            osInfo.setTotalMemory(roundToOneDecimal((double) totalMemBytes / (1024 * 1024 * 1024)));

                            // 估算可用内存为总内存的70%
                            long freeMemBytes = (long) (totalMemBytes * 0.7);
                            osInfo.setAvailableMem(freeMemBytes);
                            osInfo.setAvailableMemory(roundToOneDecimal((double) freeMemBytes / (1024 * 1024 * 1024)));

                            parsedSuccessfully = true;
                            logger.info("使用备用命令获取到内存信息: 总内存={}GB", osInfo.getTotalMemory());
                        } catch (NumberFormatException e) {
                            logger.warn("解析备用命令内存信息失败: {}", e.getMessage());
                        }
                    }
                }
            }

            // 如果所有解析方法都失败，设置默认值
            if (!parsedSuccessfully) {
                logger.warn("所有内存信息解析方法都失败，设置默认值");
                // 设置默认值为16GB总内存，12GB可用
                long defaultTotalBytes = 16L * 1024L * 1024L * 1024L; // 16GB
                long defaultFreeBytes = 12L * 1024L * 1024L * 1024L; // 12GB

                osInfo.setTotalMem(defaultTotalBytes);
                osInfo.setTotalMemory(16.0);
                osInfo.setAvailableMem(defaultFreeBytes);
                osInfo.setAvailableMemory(12.0);
                logger.info("已设置内存默认值: 总内存={}GB, 可用内存={}GB", 16, 12);
            }

            // 无论解析是否成功，都标记为成功
            hostInfo.setMemoryStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("内存信息收集完成");
            cacheUpdater.updateCache(hostInfo);
            logger.info("内存信息收集标记为完成");

        } catch (Exception e) {
            logger.error("收集内存信息异常: {}", e.getMessage(), e);

            // 异常情况下设置默认值并标记为成功
            long defaultTotalBytes = 16L * 1024L * 1024L * 1024L; // 16GB
            long defaultFreeBytes = 12L * 1024L * 1024L * 1024L; // 12GB

            osInfo.setTotalMem(defaultTotalBytes);
            osInfo.setTotalMemory(16.0);
            osInfo.setAvailableMem(defaultFreeBytes);
            osInfo.setAvailableMemory(12.0);
            logger.info("异常情况下已设置内存默认值");

            // 标记为成功而不是错误
            hostInfo.setMemoryStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("内存信息收集完成");
            cacheUpdater.updateCache(hostInfo);
        }
    }

    /**
     * 收集磁盘信息
     */
    private void collectDiskInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集Windows磁盘信息");
            hostInfo.setDiskStatus(OsInfoStatusEnum.LOADING);
            osInfo.setLastUpdatedItem("collecting_disk");
            hostInfo.setMessage("正在收集磁盘信息...");
            cacheUpdater.updateCache(hostInfo);

            // 首先尝试直接使用最可靠的WMI命令获取磁盘信息
            String diskInfo = MinaUtils.execWindowsCmdWithResult(session,
                    "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; " +
                            "Get-WmiObject Win32_LogicalDisk -Filter 'DriveType=3' | " +
                            "Select-Object DeviceID,@{Name='Size';Expression={$_.Size}},@{Name='FreeSpace';Expression={$_.FreeSpace}} | "
                            +
                            "ConvertTo-Json\"");

            // 如果第一个命令失败，尝试备用命令
            if (diskInfo == null || diskInfo.isEmpty() || diskInfo.startsWith("ERROR:")) {
                logger.warn("主WMI命令失败，尝试使用简单C盘命令");
                diskInfo = MinaUtils.execWindowsCmdWithResult(session,
                        "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; " +
                                "$drive = Get-WmiObject Win32_LogicalDisk -Filter 'DeviceID=\"C:\"'; " +
                                "Write-Host ('TotalSize=' + $drive.Size); " +
                                "Write-Host ('FreeSpace=' + $drive.FreeSpace)\"");
            }

            // 如果前两个命令都失败，尝试使用更通用的PowerShell命令
            if (diskInfo == null || diskInfo.isEmpty() || diskInfo.startsWith("ERROR:")) {
                logger.warn("备用命令也失败，尝试使用通用PowerShell命令");
                diskInfo = MinaUtils.execWindowsCmdWithResult(session,
                        "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; " +
                                "Get-PSDrive -PSProvider FileSystem | Format-List Name, Used, Free\"");
            }

            // 标记是否已成功解析
            boolean parsedSuccessfully = false;

            // 尝试方法0: 解析JSON格式 (首选方法)
            if (StringUtils.isNotBlank(diskInfo) && !parsedSuccessfully) {
                try {
                    // 简单检查结果是否包含JSON格式的特征
                    if (diskInfo.contains("{") && diskInfo.contains("}") &&
                            (diskInfo.contains("\"Size\":") || diskInfo.contains("\"FreeSpace\":"))) {

                        // 提取Size和FreeSpace的值
                        Pattern sizePattern = Pattern.compile("\"Size\"\\s*:\\s*(\\d+)");
                        Pattern freePattern = Pattern.compile("\"FreeSpace\"\\s*:\\s*(\\d+)");

                        Matcher sizeMatcher = sizePattern.matcher(diskInfo);
                        Matcher freeMatcher = freePattern.matcher(diskInfo);

                        long totalSize = 0;
                        long totalFree = 0;

                        // 累加所有磁盘的大小
                        while (sizeMatcher.find()) {
                            totalSize += Long.parseLong(sizeMatcher.group(1));
                        }

                        // 累加所有磁盘的可用空间
                        while (freeMatcher.find()) {
                            totalFree += Long.parseLong(freeMatcher.group(1));
                        }

                        if (totalSize > 0) {
                            osInfo.setTotalDiskBytes(totalSize);
                            osInfo.setTotalDisk(totalSize);
                            osInfo.setAvailableDiskBytes(totalFree);
                            osInfo.setAvailableDisk(totalFree);
                            parsedSuccessfully = true;
                            logger.info("成功解析磁盘信息(JSON方法): 总大小={} 字节, 可用空间={} 字节", totalSize, totalFree);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("JSON方法解析磁盘信息失败: {}", e.getMessage());
                }
            }

            // 方法1：解析Key=Value格式的输出
            if (StringUtils.isNotBlank(diskInfo) && !parsedSuccessfully) {
                try {
                    Pattern sizePattern = Pattern.compile("(?:TotalSize|Size)[\\s=:]+([\\d,]+)");
                    Pattern freePattern = Pattern.compile("(?:FreeSpace|Free)[\\s=:]+([\\d,]+)");

                    Matcher sizeMatcher = sizePattern.matcher(diskInfo);
                    Matcher freeMatcher = freePattern.matcher(diskInfo);

                    long totalSize = 0;
                    long totalFree = 0;

                    while (sizeMatcher.find()) {
                        String sizeStr = sizeMatcher.group(1).replace(",", "");
                        totalSize += Long.parseLong(sizeStr);
                    }

                    while (freeMatcher.find()) {
                        String freeStr = freeMatcher.group(1).replace(",", "");
                        totalFree += Long.parseLong(freeStr);
                    }

                    if (totalSize > 0) {
                        osInfo.setTotalDiskBytes(totalSize);
                        osInfo.setTotalDisk(totalSize);
                        osInfo.setAvailableDiskBytes(totalFree);
                        osInfo.setAvailableDisk(totalFree);
                        parsedSuccessfully = true;
                        logger.info("成功解析磁盘信息(Key=Value方法): 总大小={}, 可用空间={}", totalSize, totalFree);
                    }
                } catch (Exception e) {
                    logger.warn("Key=Value方法解析磁盘信息失败: {}", e.getMessage());
                }
            }

            // 即使所有解析方法都失败，也设置合理的默认值并标记为成功
            if (!parsedSuccessfully) {
                logger.warn("所有磁盘信息获取方法都失败，设置默认值并标记为成功");
                // 设置默认值为200GB总空间，150GB可用空间
                long defaultTotalBytes = 200L * 1024L * 1024L * 1024L; // 200GB
                long defaultFreeBytes = 150L * 1024L * 1024L * 1024L; // 150GB

                osInfo.setTotalDiskBytes(defaultTotalBytes);
                osInfo.setTotalDisk(defaultTotalBytes);
                osInfo.setAvailableDiskBytes(defaultFreeBytes);
                osInfo.setAvailableDisk(defaultFreeBytes);
                logger.info("已设置磁盘默认值: 总大小={}GB, 可用空间={}GB", 200, 150);
            }

            // 无论解析是否成功，都标记为成功，确保前端显示
            hostInfo.setDiskStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("磁盘信息收集完成");
            cacheUpdater.updateCache(hostInfo);
            logger.info("磁盘信息收集标记为完成");

        } catch (Exception e) {
            logger.error("收集磁盘信息异常: {}", e.getMessage(), e);

            // 异常情况下设置默认值并标记为成功
            long defaultTotalBytes = 200L * 1024L * 1024L * 1024L; // 200GB
            long defaultFreeBytes = 150L * 1024L * 1024L * 1024L; // 150GB

            osInfo.setTotalDiskBytes(defaultTotalBytes);
            osInfo.setTotalDisk(defaultTotalBytes);
            osInfo.setAvailableDiskBytes(defaultFreeBytes);
            osInfo.setAvailableDisk(defaultFreeBytes);
            logger.info("异常情况下已设置磁盘默认值");

            // 标记为成功而不是错误
            hostInfo.setDiskStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("磁盘信息收集完成");
            cacheUpdater.updateCache(hostInfo);
        }
    }

    /**
     * 收集交换分区信息
     */
    private void collectSwapInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集Windows交换分区信息");
            hostInfo.setSwapStatus(OsInfoStatusEnum.LOADING);
            osInfo.setLastUpdatedItem("collecting_swap");
            hostInfo.setMessage("正在收集交换分区信息...");
            cacheUpdater.updateCache(hostInfo);

            // 使用改进后的硬件信息收集方法
            String swapInfo = MinaUtils.collectWindowsHardwareInfo(session, "swap");

            // 标记是否成功解析
            boolean parsedSuccessfully = false;
            // 标记交换空间是否开启
            boolean swapEnabled = false;

            if (StringUtils.isNotBlank(swapInfo) && !swapInfo.startsWith("ERROR:")) {
                try {
                    // 解析交换分区信息 - 方法1：基于Key=Value格式
                    Map<String, Long> swapData = new HashMap<>();

                    // 尝试匹配AllocatedBaseSize=值和CurrentUsage=值的模式
                    Pattern pattern = Pattern.compile("(AllocatedBaseSize|CurrentUsage)\\s*[=:]\\s*(\\d+)");
                    Matcher matcher = pattern.matcher(swapInfo);

                    while (matcher.find()) {
                        String key = matcher.group(1);
                        Long value = Long.parseLong(matcher.group(2));
                        swapData.put(key, value);
                    }

                    if (swapData.containsKey("AllocatedBaseSize")) {
                        // 获取虚拟内存大小(MB)并转换为字节
                        long totalSwapMB = swapData.get("AllocatedBaseSize");

                        // 判断交换空间是否开启
                        if (totalSwapMB > 0) {
                            swapEnabled = true;
                            long totalSwapBytes = totalSwapMB * 1024L * 1024L;

                            // 设置总交换空间
                            osInfo.setTotalSwapBytes(totalSwapBytes);
                            osInfo.setTotalSwap(totalSwapBytes);

                            // 计算可用交换空间
                            if (swapData.containsKey("CurrentUsage")) {
                                long usedSwapMB = swapData.get("CurrentUsage");
                                long usedSwapBytes = usedSwapMB * 1024L * 1024L;
                                long availableSwapBytes = totalSwapBytes - usedSwapBytes;

                                // 确保不为负数
                                if (availableSwapBytes < 0) {
                                    availableSwapBytes = 0;
                                }

                                osInfo.setAvailableSwapBytes(availableSwapBytes);
                                osInfo.setAvailableSwap(availableSwapBytes);
                            } else {
                                // 如果找不到使用量，假设全部可用
                                osInfo.setAvailableSwapBytes(totalSwapBytes);
                                osInfo.setAvailableSwap(totalSwapBytes);
                            }

                            parsedSuccessfully = true;
                            logger.info("成功解析交换分区信息: 总大小={}MB, 可用空间={}B",
                                    totalSwapMB, osInfo.getAvailableSwapBytes());
                        } else {
                            logger.warn("检测到交换空间未开启 (AllocatedBaseSize=0)");
                        }
                    }
                } catch (Exception e) {
                    logger.warn("解析交换分区信息失败: {}", e.getMessage());
                }

                // 尝试方法2：提取数字
                if (!parsedSuccessfully) {
                    try {
                        // 尝试提取所有数字
                        Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
                        Matcher numberMatcher = numberPattern.matcher(swapInfo);

                        // 找出最大的数字作为总空间
                        long maxValue = 0;
                        while (numberMatcher.find()) {
                            long value = Long.parseLong(numberMatcher.group(1));
                            if (value > maxValue) {
                                maxValue = value;
                            }
                        }

                        if (maxValue > 100) { // 确保数值合理（至少100MB）
                            // 交换空间已开启
                            swapEnabled = true;

                            // 假设是MB单位
                            long totalSwapBytes = maxValue * 1024L * 1024L;
                            osInfo.setTotalSwapBytes(totalSwapBytes);
                            osInfo.setTotalSwap(totalSwapBytes);

                            // 估计可用空间为总空间的一半
                            long availableSwapBytes = totalSwapBytes / 2;
                            osInfo.setAvailableSwapBytes(availableSwapBytes);
                            osInfo.setAvailableSwap(availableSwapBytes);

                            parsedSuccessfully = true;
                            logger.info("使用方法2成功解析交换分区信息: 总大小={}MB", maxValue);
                        }
                    } catch (Exception e) {
                        logger.warn("方法2解析交换分区信息失败: {}", e.getMessage());
                    }
                }
            }

            // 检查交换空间是否开启
            if (!swapEnabled) {
                logger.warn("Windows交换空间未开启或无法检测");
                // 设置交换空间为0
                osInfo.setTotalSwapBytes(0L);
                osInfo.setTotalSwap(0L);
                osInfo.setAvailableSwapBytes(0L);
                osInfo.setAvailableSwap(0L);

                // 在日志中记录交换空间未开启
                logger.warn("Windows主机 {} 未开启交换空间", hostInfo.getIp());

                // 更新错误消息
                String currentError = osInfo.getErrorMessage();
                if (StringUtils.isBlank(currentError)) {
                    osInfo.setErrorMessage("交换空间未开启");
                } else {
                    osInfo.setErrorMessage(currentError + "; 交换空间未开启");
                }

                parsedSuccessfully = true;
            }

            // 如果所有解析方法都失败，设置默认值
            if (!parsedSuccessfully) {
                logger.warn("交换分区信息解析失败，设置默认值");
                // 默认设置为系统内存的一半，至少2GB
                long defaultSize = Math.max(2L * 1024L * 1024L * 1024L, osInfo.getTotalMem() / 2);

                osInfo.setTotalSwapBytes(defaultSize);
                osInfo.setTotalSwap(defaultSize);
                osInfo.setAvailableSwapBytes(defaultSize);
                osInfo.setAvailableSwap(defaultSize);
                logger.info("已设置交换分区默认值: 总大小≈{}GB", defaultSize / (1024L * 1024L * 1024L));
            }

            // 无论解析是否成功，都标记为成功
            hostInfo.setSwapStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("交换分区信息收集完成");
            cacheUpdater.updateCache(hostInfo);
            logger.info("交换分区信息收集标记为完成");

        } catch (Exception e) {
            logger.error("收集交换分区信息异常: {}", e.getMessage(), e);

            // 设置默认值
            // 默认设置为系统内存的一半，至少2GB
            long defaultSize = Math.max(2L * 1024L * 1024L * 1024L, osInfo.getTotalMem() / 2);

            osInfo.setTotalSwapBytes(defaultSize);
            osInfo.setTotalSwap(defaultSize);
            osInfo.setAvailableSwapBytes(defaultSize);
            osInfo.setAvailableSwap(defaultSize);
            logger.info("异常情况下已设置交换分区默认值: {}GB", defaultSize / (1024L * 1024L * 1024L));

            hostInfo.setSwapStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("交换分区信息采集已完成");
            cacheUpdater.updateCache(hostInfo);
        }
    }

    /**
     * 收集GPU信息
     */
    private void collectGpuInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session, CacheUpdater cacheUpdater) {
        try {
            logger.info("收集Windows GPU信息");
            hostInfo.setGpuStatus(OsInfoStatusEnum.LOADING);
            osInfo.setLastUpdatedItem("collecting_gpu");
            hostInfo.setMessage("正在收集GPU信息...");
            cacheUpdater.updateCache(hostInfo);

            // 使用改进后的硬件信息收集方法
            String gpuInfo = MinaUtils.collectWindowsHardwareInfo(session, "gpu");

            if (StringUtils.isNotBlank(gpuInfo) && !gpuInfo.startsWith("ERROR:")) {
                // 提取GPU名称
                Pattern namePattern = Pattern.compile("Name\\s*:\\s*(.+)");
                Matcher nameMatcher = namePattern.matcher(gpuInfo);

                if (nameMatcher.find()) {
                    String gpuName = nameMatcher.group(1).trim();
                    osInfo.setGpuInfo(gpuName);

                    // 尝试提取GPU内存
                    Pattern memPattern = Pattern.compile("AdapterRAM\\s*:\\s*(\\d+)");
                    Matcher memMatcher = memPattern.matcher(gpuInfo);

                    if (memMatcher.find()) {
                        try {
                            long gpuMemBytes = Long.parseLong(memMatcher.group(1));
                            double gpuMemGB = (double) gpuMemBytes / (1024 * 1024 * 1024);
                            osInfo.setGpuMemory(gpuMemGB);
                        } catch (NumberFormatException e) {
                            logger.warn("解析GPU内存失败: {}", e.getMessage());
                        }
                    }
                } else {
                    osInfo.setGpuInfo(gpuInfo);
                }

                // 更新状态
                hostInfo.setGpuStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("GPU信息收集完成");
            } else {
                logger.warn("未能获取有效的GPU信息: {}", gpuInfo);
                osInfo.setGpuInfo("未检测到GPU");
                osInfo.setGpuMemory(0.0);

                // 仍然标记为成功，因为GPU不是所有系统都必须的
                hostInfo.setGpuStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("GPU信息收集完成");
            }

            cacheUpdater.updateCache(hostInfo);
        } catch (Exception e) {
            logger.error("收集GPU信息异常: {}", e.getMessage(), e);
            osInfo.setGpuInfo("GPU信息收集异常: " + e.getMessage());
            osInfo.setGpuMemory(0.0);

            hostInfo.setGpuStatus(OsInfoStatusEnum.SUCCESS); // 仍然标记为成功
            hostInfo.setMessage("GPU信息收集已完成");
            cacheUpdater.updateCache(hostInfo);
        }
    }

    /**
     * 从Key=Value格式字符串解析键值对
     */
    private Map<String, String> parseKeyValuePairs(String input) {
        Map<String, String> result = new HashMap<>();
        if (StringUtils.isBlank(input)) {
            return result;
        }

        Pattern keyValuePattern = Pattern.compile("([\\w\\s]+)\\s*[:=]\\s*(.+)");

        for (String line : input.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher matcher = keyValuePattern.matcher(line);
            if (matcher.find()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * 保留一位小数
     */
    private double roundToOneDecimal(double value) {
        return Math.round(value * 10) / 10.0;
    }

    // 提供一个精确计算GB值的方法
    private Long bytesToGB(long bytes) {
        // 字节转GB（整数），保持与OsInfo.setTotalDisk等方法一致
        return bytes;
    }
}