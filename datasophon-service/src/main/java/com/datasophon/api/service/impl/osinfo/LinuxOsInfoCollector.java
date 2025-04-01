package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.enums.LinuxDistribution;
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
            logger.info("开始收集Linux系统信息: {}", hostInfo.getIp());

            // 设置状态为COLLECTING，并立即更新缓存
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.COLLECTING);

            // 获取主机名
            String hostname = MinaUtils.execCmdWithResult(session, "hostname");
            if (StringUtils.isNotBlank(hostname)) {
                hostname = hostname.trim();
                osInfo.setHostname(hostname);
                hostInfo.setHostname(hostname);
                logger.info("获取到主机名: {}", hostname);
            }

            // 获取完全限定域名(FQDN)
            String fqdn = MinaUtils.execCmdWithResult(session, "hostname -f");
            if (StringUtils.isNotBlank(fqdn)) {
                fqdn = fqdn.trim();
                osInfo.setFqdn(fqdn);
                hostInfo.setFqdn(fqdn);
                logger.info("获取到FQDN: {}", fqdn);
            }

            // 读取/etc/hosts文件内容
            String hostsFile = MinaUtils.execCmdWithResult(session, "cat /etc/hosts 2>/dev/null");
            if (StringUtils.isNotBlank(hostsFile)) {
                hostInfo.setHostsFile(hostsFile);
                logger.info("获取到hosts文件内容");
            }

            // 获取DNS服务器信息
            collectDnsInfo(osInfo, session);

            // 首先尝试使用特定文件获取发行版信息（优先级更高）
            tryAlternativeFiles(osInfo, session);

            // 如果没有设置distribution，则尝试使用/etc/os-release
            if (osInfo.getDistributionId() == null || "unknown".equals(osInfo.getDistributionId())) {
                // 读取/etc/os-release文件获取发行版信息
                String osRelease = MinaUtils.execCmdWithResult(session, "cat /etc/os-release 2>/dev/null");
                if (StringUtils.isNotBlank(osRelease)) {
                    parseDistributionInfo(osInfo, osRelease);
                }
            }

            // 如果仍未识别到发行版信息，使用uname命令
            if (osInfo.getDistributionId() == null || "unknown".equals(osInfo.getDistributionId())) {
                String uname = MinaUtils.execCmdWithResult(session, "uname -a");
                if (StringUtils.isNotBlank(uname)) {
                    logger.info("通过uname命令识别系统: {}", uname);

                    // 设置默认值
                    osInfo.setDistributionId("linux");
                    osInfo.setDistribution("Linux");
                    osInfo.setDistributionType(LinuxDistribution.OTHER);
                    osInfo.setDistributionName("Generic Linux");
                    osInfo.setDisplayName("Linux");

                    // 尝试从uname输出识别系统类型
                    uname = uname.toLowerCase();
                    if (uname.contains("ubuntu")) {
                        osInfo.setDistributionId("ubuntu");
                        osInfo.setDistribution("Ubuntu");
                        osInfo.setDistributionType(LinuxDistribution.UBUNTU);
                        osInfo.setDistributionName("Ubuntu");
                        osInfo.setDisplayName("Ubuntu Linux");
                    } else if (uname.contains("debian")) {
                        osInfo.setDistributionId("debian");
                        osInfo.setDistribution("Debian");
                        osInfo.setDistributionType(LinuxDistribution.DEBIAN);
                        osInfo.setDistributionName("Debian GNU/Linux");
                        osInfo.setDisplayName("Debian GNU/Linux");
                    }
                }
            }

            // 获取内核版本
            String kernelVersion = MinaUtils.execCmdWithResult(session, "uname -r");
            if (StringUtils.isNotBlank(kernelVersion)) {
                kernelVersion = kernelVersion.trim();
                osInfo.setKernelVersion(kernelVersion);
                logger.info("获取到内核版本: {}", kernelVersion);
            }

            // 获取系统架构
            String architecture = MinaUtils.execCmdWithResult(session, "uname -m");
            if (StringUtils.isNotBlank(architecture)) {
                architecture = architecture.trim();
                osInfo.setArchitecture(architecture);
                logger.info("获取到系统架构: {}", architecture);
            }

            // 获取系统负载
            String systemLoad = MinaUtils.execCmdWithResult(session, "uptime");
            if (StringUtils.isNotBlank(systemLoad)) {
                systemLoad = systemLoad.trim();
                // 提取load average部分
                Pattern pattern = Pattern.compile("load average: (.+)");
                Matcher matcher = pattern.matcher(systemLoad);
                if (matcher.find()) {
                    systemLoad = matcher.group(1);
                }
                // 将负载信息添加到CPU对象中
                if (osInfo.getCpuInfo() == null) {
                    osInfo.setCpuInfo(new CpuInfo());
                }

                // 尝试解析负载值
                try {
                    String[] loads = systemLoad.split(",\\s*");
                    if (loads.length >= 3) {
                        osInfo.getCpuInfo().setLoad1Min(Double.parseDouble(loads[0]));
                        osInfo.getCpuInfo().setLoad5Min(Double.parseDouble(loads[1]));
                        osInfo.getCpuInfo().setLoad15Min(Double.parseDouble(loads[2]));
                    }
                } catch (Exception e) {
                    logger.warn("解析系统负载值出错: {}", e.getMessage());
                }

                logger.info("获取到系统负载: {}", systemLoad);
            }

            // 完成收集
            osInfo.setOsType("linux");
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setOsStatus(OsInfoStatusEnum.SUCCESS);
            osInfo.setLastUpdatedItem("os_info_collected");
            cacheUpdater.updateCache(null);

            logger.info("完成Linux系统信息收集: {}", hostInfo.getIp());
            return osInfo;

        } catch (Exception e) {
            logger.error("收集Linux系统信息时出错: {}", e.getMessage(), e);
            osInfo.setLastUpdatedItem("error");
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
            hostInfo.setOsStatus(OsInfoStatusEnum.ERROR);
            hostInfo.setMessage("收集系统信息时出错: " + e.getMessage());
            // 出错时更新状态
            cacheUpdater.updateCache(null);
            return osInfo;
        }
    }

    @Override
    public void collectHardwareInfo(OsInfo osInfo, ClientSession session, CacheUpdater cacheUpdater) {
        logger.info("开始收集Linux硬件信息");
        osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.COLLECTING);
        // 更新收集状态
        cacheUpdater.updateCache(null); // 让回调函数处理缓存更新

        try {
            // 收集CPU信息
            osInfo.setLastUpdatedItem("collecting_cpu");
            logger.info("收集CPU信息...");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(null);
            collectCpuInfo(osInfo, session);

            // 收集内存信息
            osInfo.setLastUpdatedItem("collecting_memory");
            logger.info("收集内存信息...");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(null);
            collectMemoryInfo(osInfo, session);

            // 收集磁盘信息
            osInfo.setLastUpdatedItem("collecting_disk");
            logger.info("收集磁盘信息...");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(null);
            collectDiskInfo(osInfo, session);

            // 收集交换分区信息
            osInfo.setLastUpdatedItem("collecting_swap");
            logger.info("收集交换分区信息...");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(null);
            collectSwapInfo(osInfo, session);

            // 收集GPU信息
            osInfo.setLastUpdatedItem("collecting_gpu");
            logger.info("收集GPU信息...");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(null);
            collectGpuInfo(osInfo, session);

            // 标记为完成
            osInfo.setLastUpdatedItem("completed");
            osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.SUCCESS);
            // 完成时更新一次
            cacheUpdater.updateCache(null);

            logger.info("Linux硬件信息收集完成");
        } catch (Exception e) {
            logger.error("收集Linux硬件信息时出错: {}", e.getMessage(), e);
            osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
            osInfo.setLastUpdatedItem("error");
            // 出错时更新状态
            cacheUpdater.updateCache(null);
        }
    }

    /**
     * 解析Linux发行版信息
     */
    private void parseDistributionInfo(OsInfo osInfo, String osRelease) {
        logger.info("解析Linux发行版信息");

        try {
            // 从/etc/os-release中提取ID、NAME、VERSION_ID等信息
            Pattern idPattern = Pattern.compile("^ID=\"?(.*?)\"?$", Pattern.MULTILINE);
            Pattern namePattern = Pattern.compile("^NAME=\"?(.*?)\"?$", Pattern.MULTILINE);
            Pattern versionIdPattern = Pattern.compile("^VERSION_ID=\"?(.*?)\"?$", Pattern.MULTILINE);
            Pattern prettyNamePattern = Pattern.compile("^PRETTY_NAME=\"?(.*?)\"?$", Pattern.MULTILINE);

            // 提取ID (发行版ID)
            Matcher idMatcher = idPattern.matcher(osRelease);
            if (idMatcher.find()) {
                String distributionId = idMatcher.group(1).trim().toLowerCase();
                osInfo.setDistributionId(distributionId);
                logger.info("发行版ID: {}", distributionId);

                // 根据distributionId设置distributionType
                LinuxDistribution distType = LinuxDistribution.fromId(distributionId);
                osInfo.setDistributionType(distType);

                // 设置distribution
                switch (distType) {
                    case CENTOS:
                    case CENTOS7:
                    case CENTOS8:
                        osInfo.setDistribution("CentOS");
                        break;
                    case UBUNTU:
                    case UBUNTU22:
                    case UBUNTU24:
                        osInfo.setDistribution("Ubuntu");
                        break;
                    case DEBIAN:
                        osInfo.setDistribution("Debian");
                        break;
                    case REDHAT:
                        osInfo.setDistribution("RedHat");
                        break;
                    case KYLIN:
                    case KYLIN_V4:
                    case KYLIN_V10:
                        osInfo.setDistribution("Kylin");
                        break;
                    default:
                        // 如果不是已知发行版，直接使用ID作为distribution
                        osInfo.setDistribution(StringUtils.capitalize(distributionId));
                }
            } else {
                // 如果没有找到ID，设置为Other
                osInfo.setDistributionId("unknown");
                osInfo.setDistribution("Other");
                osInfo.setDistributionType(LinuxDistribution.OTHER);
            }

            // 提取NAME (发行版名称)
            Matcher nameMatcher = namePattern.matcher(osRelease);
            if (nameMatcher.find()) {
                String distributionName = nameMatcher.group(1).trim();
                osInfo.setDistributionName(distributionName);
                logger.info("发行版名称: {}", distributionName);
            } else {
                // 如果没有NAME，使用ID首字母大写作为distributionName
                if (StringUtils.isNotBlank(osInfo.getDistributionId())) {
                    osInfo.setDistributionName(StringUtils.capitalize(osInfo.getDistributionId()));
                } else {
                    osInfo.setDistributionName("Unknown Linux");
                }
            }

            // 提取VERSION_ID (版本号)
            Matcher versionIdMatcher = versionIdPattern.matcher(osRelease);
            if (versionIdMatcher.find()) {
                String versionId = versionIdMatcher.group(1).trim();
                osInfo.setVersionId(versionId);
                osInfo.setVersion(versionId);
                logger.info("版本号: {}", versionId);

                // 设置特定版本标志 (例如centOS7, ubuntu22等)
                setVersionFlags(osInfo);
            }

            // 提取PRETTY_NAME (完整名称)
            Matcher prettyNameMatcher = prettyNamePattern.matcher(osRelease);
            if (prettyNameMatcher.find()) {
                String prettyName = prettyNameMatcher.group(1).trim();
                osInfo.setFullName(prettyName);
                logger.info("完整名称: {}", prettyName);
            }

            // 设置displayName
            setDisplayName(osInfo);

            // 针对Ubuntu、Debian系统进行额外识别
            handleSpecialDistributions(osInfo);

        } catch (Exception e) {
            logger.error("解析Linux发行版信息出错", e);
            // 设置默认值
            osInfo.setDistributionId("unknown");
            osInfo.setDistribution("Other");
            osInfo.setDistributionType(LinuxDistribution.OTHER);
            osInfo.setDisplayName("未知Linux发行版");
        }
    }

    /**
     * 对特殊发行版进行额外处理
     */
    private void handleSpecialDistributions(OsInfo osInfo) {
        String distId = osInfo.getDistributionId();
        if (distId == null) {
            return;
        }

        // 处理Ubuntu系统
        if ("ubuntu".equalsIgnoreCase(distId)) {
            String version = osInfo.getVersionId();
            if (version != null) {
                if (version.startsWith("22")) {
                    osInfo.setUbuntu22(true);
                    // 为悬浮卡片设置详细版本信息
                    osInfo.setDistributionName("Ubuntu 22.04 LTS");
                    // 设置完整名称用于悬浮卡片
                    osInfo.setFullName("Ubuntu 22.04 LTS (Jammy Jellyfish)");
                    // 为列表显示设置简单名称
                    osInfo.setDisplayName("Ubuntu");
                } else if (version.startsWith("24")) {
                    osInfo.setUbuntu24(true);
                    // 判断具体的24版本
                    if (version.contains("24.04")) {
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu 24.04 LTS");
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu 24.04 LTS (Noble Numbat)");
                    } else if (version.contains("24.10")) {
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu 24.10");
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu 24.10 (Oracular Oriole)");
                    } else {
                        // 默认24版本处理
                        osInfo.setDistributionName("Ubuntu 24.04 LTS");
                        osInfo.setFullName("Ubuntu 24.04 LTS (Noble Numbat)");
                    }
                    // 为列表显示设置简单名称
                    osInfo.setDisplayName("Ubuntu");
                } else if (version.startsWith("20")) {
                    // 为悬浮卡片设置详细版本信息
                    osInfo.setDistributionName("Ubuntu 20.04 LTS");
                    // 设置完整名称用于悬浮卡片
                    osInfo.setFullName("Ubuntu 20.04 LTS (Focal Fossa)");
                    // 为列表显示设置简单名称
                    osInfo.setDisplayName("Ubuntu");
                } else if (version.startsWith("18")) {
                    // 为悬浮卡片设置详细版本信息
                    osInfo.setDistributionName("Ubuntu 18.04 LTS");
                    // 设置完整名称用于悬浮卡片
                    osInfo.setFullName("Ubuntu 18.04 LTS (Bionic Beaver)");
                    // 为列表显示设置简单名称
                    osInfo.setDisplayName("Ubuntu");
                } else if (version.startsWith("16")) {
                    // 为悬浮卡片设置详细版本信息
                    osInfo.setDistributionName("Ubuntu 16.04 LTS");
                    // 设置完整名称用于悬浮卡片
                    osInfo.setFullName("Ubuntu 16.04 LTS (Xenial Xerus)");
                    // 为列表显示设置简单名称
                    osInfo.setDisplayName("Ubuntu");
                } else {
                    // 为悬浮卡片设置详细版本信息
                    osInfo.setDistributionName("Ubuntu " + version);
                    // 设置完整名称用于悬浮卡片
                    osInfo.setFullName("Ubuntu " + version);
                    // 为列表显示设置简单名称
                    osInfo.setDisplayName("Ubuntu");
                }
            } else {
                osInfo.setDistributionName("Ubuntu");
                osInfo.setFullName("Ubuntu");
                osInfo.setDisplayName("Ubuntu");
            }
        }

        // 处理Debian系统
        else if ("debian".equalsIgnoreCase(distId)) {
            String version = osInfo.getVersionId();
            if (version != null) {
                if (version.equals("12") || version.startsWith("12.")) {
                    osInfo.setDistributionName("Debian GNU/Linux 12 (bookworm)");
                    osInfo.setDisplayName("Debian GNU/Linux 12 (bookworm)");
                } else if (version.equals("11") || version.startsWith("11.")) {
                    osInfo.setDistributionName("Debian GNU/Linux 11 (bullseye)");
                    osInfo.setDisplayName("Debian GNU/Linux 11 (bullseye)");
                } else if (version.equals("10") || version.startsWith("10.")) {
                    osInfo.setDistributionName("Debian GNU/Linux 10 (buster)");
                    osInfo.setDisplayName("Debian GNU/Linux 10 (buster)");
                } else if (version.equals("9") || version.startsWith("9.")) {
                    osInfo.setDistributionName("Debian GNU/Linux 9 (stretch)");
                    osInfo.setDisplayName("Debian GNU/Linux 9 (stretch)");
                } else {
                    osInfo.setDistributionName("Debian GNU/Linux " + version);
                    osInfo.setDisplayName("Debian GNU/Linux " + version);
                }
            } else {
                osInfo.setDistributionName("Debian GNU/Linux");
                osInfo.setDisplayName("Debian GNU/Linux");
            }
        }

        // 处理Kylin系统
        else if ("kylin".equalsIgnoreCase(distId)) {
            logger.info("识别到麒麟系统，ID: kylin");

            // 设置发行版名称
            osInfo.setDistribution("Kylin");
            osInfo.setDistributionType(LinuxDistribution.KYLIN);

            // 设置简洁的显示名称
            osInfo.setDisplayName("中标麒麟");

            String versionId = osInfo.getVersionId();
            if (versionId != null) {
                logger.info("麒麟系统版本: {}", versionId);

                // 处理V10版本
                if (versionId.equals("V10") || versionId.equals("10")) {
                    osInfo.setKylinV10(true);
                    osInfo.setDistributionName("中标麒麟 V10");

                    // 获取PRETTY_NAME设置完整名称
                    if (StringUtils.isNotBlank(osInfo.getFullName())) {
                        // 已设置fullName，保持不变
                    } else {
                        osInfo.setFullName("Kylin Linux Advanced Server V10 (Halberd)");
                    }
                }
                // 处理V4版本
                else if (versionId.equals("V4") || versionId.equals("4")) {
                    osInfo.setKylinV4(true);
                    osInfo.setDistributionName("中标麒麟 V4");

                    // 获取PRETTY_NAME设置完整名称
                    if (StringUtils.isNotBlank(osInfo.getFullName())) {
                        // 已设置fullName，保持不变
                    } else {
                        osInfo.setFullName("中标麒麟操作系统 V4");
                    }
                }
                // 其他版本
                else {
                    osInfo.setDistributionName("中标麒麟 " + versionId);

                    // 获取PRETTY_NAME设置完整名称
                    if (StringUtils.isNotBlank(osInfo.getFullName())) {
                        // 已设置fullName，保持不变
                    } else {
                        osInfo.setFullName("中标麒麟操作系统 " + versionId);
                    }
                }
            } else {
                // 没有版本信息
                osInfo.setDistributionName("中标麒麟");

                // 获取PRETTY_NAME设置完整名称
                if (StringUtils.isNotBlank(osInfo.getFullName())) {
                    // 已设置fullName，保持不变
                } else {
                    osInfo.setFullName("中标麒麟操作系统");
                }
            }
            logger.info("设置麒麟系统信息：distribution={}, displayName={}, distributionName={}, fullName={}",
                    osInfo.getDistribution(), osInfo.getDisplayName(), osInfo.getDistributionName(),
                    osInfo.getFullName());
        }
    }

    /**
     * 尝试使用其他方式获取Linux发行版信息
     */
    private void tryAlternativeFiles(OsInfo osInfo, ClientSession session) {
        logger.info("尝试其他方式获取Linux发行版信息");

        try {
            // 优先检查麒麟系统内核特征
            boolean kylinDetected = checkKylinKernel(osInfo, session);
            if (kylinDetected) {
                logger.info("通过内核版本成功识别为麒麟系统");
                return; // 麒麟系统信息已获取，直接返回
            }

            // 检查Ubuntu特定文件
            boolean ubuntuDetected = checkUbuntuFile(osInfo, session);
            if (ubuntuDetected) {
                logger.info("成功识别为Ubuntu系统");
                return; // Ubuntu信息已获取，直接返回
            }

            // 检查Debian特定文件
            boolean debianDetected = checkDebianFile(osInfo, session);
            if (debianDetected) {
                logger.info("成功识别为Debian系统");
                return; // Debian信息已获取，直接返回
            }

            // 检查RedHat/CentOS特定文件
            boolean centosDetected = checkRedHatFile(osInfo, session);
            if (centosDetected) {
                logger.info("成功识别为RedHat/CentOS系统");
                return; // RedHat/CentOS信息已获取，直接返回
            }

            logger.info("无法通过特定文件识别发行版类型");
        } catch (Exception e) {
            logger.error("获取Linux发行版信息时出错", e);
        }
    }

    /**
     * 通过内核版本识别麒麟系统
     */
    private boolean checkKylinKernel(OsInfo osInfo, ClientSession session) {
        try {
            // 获取内核版本
            String kernelVersion = MinaUtils.execCmdWithResult(session, "uname -r");
            if (StringUtils.isNotBlank(kernelVersion)) {
                kernelVersion = kernelVersion.trim();
                logger.info("获取到内核版本: {}", kernelVersion);

                // 检查内核版本是否包含麒麟系统特征
                boolean isKylin = kernelVersion.contains("ky10") ||
                        kernelVersion.contains("ky4") ||
                        kernelVersion.contains("kylin") ||
                        kernelVersion.contains(".ky.") ||
                        kernelVersion.matches(".*\\.ky\\d+\\..*");

                if (isKylin) {
                    logger.info("通过内核版本特征识别为麒麟系统: {}", kernelVersion);

                    // 设置基本信息
                    osInfo.setDistributionId("kylin");
                    osInfo.setDistribution("Kylin");
                    osInfo.setDistributionType(LinuxDistribution.KYLIN);
                    osInfo.setKernelVersion(kernelVersion);

                    // 设置简洁的显示名称
                    osInfo.setDisplayName("中标麒麟");

                    // 尝试判断麒麟版本
                    if (kernelVersion.contains("ky10")) {
                        logger.info("识别到麒麟V10特征");
                        osInfo.setVersionId("V10");
                        osInfo.setVersion("V10");
                        osInfo.setKylinV10(true);
                        osInfo.setDistributionName("中标麒麟 V10");
                        osInfo.setFullName("Kylin Linux Advanced Server V10 (Halberd)");
                    } else if (kernelVersion.contains("ky4")) {
                        logger.info("识别到麒麟V4特征");
                        osInfo.setVersionId("V4");
                        osInfo.setVersion("V4");
                        osInfo.setKylinV4(true);
                        osInfo.setDistributionName("中标麒麟 V4");
                        osInfo.setFullName("中标麒麟操作系统 V4");
                    } else {
                        // 尝试用正则表达式提取版本号
                        Pattern kyPattern = Pattern.compile("\\.ky(\\d+)\\.");
                        Matcher kyMatcher = kyPattern.matcher(kernelVersion);
                        if (kyMatcher.find()) {
                            String versionNumber = kyMatcher.group(1);
                            logger.info("通过正则表达式识别到麒麟V{}特征", versionNumber);
                            osInfo.setVersionId("V" + versionNumber);
                            osInfo.setVersion("V" + versionNumber);

                            if ("10".equals(versionNumber)) {
                                osInfo.setKylinV10(true);
                            } else if ("4".equals(versionNumber)) {
                                osInfo.setKylinV4(true);
                            }

                            osInfo.setDistributionName("中标麒麟 V" + versionNumber);
                            osInfo.setFullName("中标麒麟操作系统 V" + versionNumber);
                        } else {
                            // 版本未知，使用默认值
                            logger.info("无法确定麒麟具体版本，使用默认值");
                            osInfo.setDistributionName("中标麒麟");
                            osInfo.setFullName("中标麒麟操作系统");
                        }
                    }

                    // 尝试读取麒麟特定文件获取更多信息
                    tryReadKylinSpecificFiles(osInfo, session);

                    logger.info("麒麟系统识别完成: distribution={}, displayName={}, distributionName={}, fullName={}",
                            osInfo.getDistribution(), osInfo.getDisplayName(), osInfo.getDistributionName(),
                            osInfo.getFullName());
                    return true;
                } else {
                    logger.debug("内核版本不包含麒麟特征: {}", kernelVersion);
                }
            }
            return false;
        } catch (Exception e) {
            logger.warn("检查Ubuntu系统时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查Debian系统
     */
    private boolean checkDebianFile(OsInfo osInfo, ClientSession session) {
        try {
            // 检查debian_version文件
            String debianVersion = MinaUtils.execCmdWithResult(session, "cat /etc/debian_version 2>/dev/null");
            if (StringUtils.isNotBlank(debianVersion)) {
                logger.info("发现debian_version文件，确认为Debian发行版");
                debianVersion = debianVersion.trim();

                osInfo.setDistributionId("debian");
                osInfo.setDistribution("Debian");
                osInfo.setDistributionType(LinuxDistribution.DEBIAN);
                osInfo.setVersionId(debianVersion);
                osInfo.setVersion(debianVersion);

                // 设置简洁的显示名称
                osInfo.setDisplayName("Debian");

                // 设置详细名称
                if (debianVersion.startsWith("12")) {
                    osInfo.setDistributionName("Debian 12 (Bookworm)");
                    osInfo.setFullName("Debian GNU/Linux 12 (bookworm)");
                } else if (debianVersion.startsWith("11")) {
                    osInfo.setDistributionName("Debian 11 (Bullseye)");
                    osInfo.setFullName("Debian GNU/Linux 11 (bullseye)");
                } else if (debianVersion.startsWith("10")) {
                    osInfo.setDistributionName("Debian 10 (Buster)");
                    osInfo.setFullName("Debian GNU/Linux 10 (buster)");
                } else if (debianVersion.startsWith("9")) {
                    osInfo.setDistributionName("Debian 9 (Stretch)");
                    osInfo.setFullName("Debian GNU/Linux 9 (stretch)");
                } else {
                    osInfo.setDistributionName("Debian " + debianVersion);
                    osInfo.setFullName("Debian GNU/Linux " + debianVersion);
                }

                return true;
            }

            // 如果debian_version不存在，尝试检查/etc/os-release
            String osReleaseId = MinaUtils.execCmdWithResult(session, "grep -i debian /etc/os-release 2>/dev/null");
            if (StringUtils.isNotBlank(osReleaseId) && osReleaseId.toLowerCase().contains("debian")) {
                logger.info("通过/etc/os-release确认为Debian系统");
                osInfo.setDistributionId("debian");
                osInfo.setDistribution("Debian");
                osInfo.setDistributionType(LinuxDistribution.DEBIAN);

                // 设置简洁的显示名称
                osInfo.setDisplayName("Debian");

                // 尝试获取版本号
                String versionCmd = "grep VERSION_ID /etc/os-release | cut -d '=' -f2 | tr -d '\"'";
                String version = MinaUtils.execCmdWithResult(session, versionCmd);
                if (StringUtils.isNotBlank(version)) {
                    version = version.trim();
                    osInfo.setVersionId(version);
                    osInfo.setVersion(version);

                    // 根据版本设置详细名称
                    if (version.equals("12") || version.startsWith("12.")) {
                        osInfo.setDistributionName("Debian 12 (Bookworm)");
                        osInfo.setFullName("Debian GNU/Linux 12 (bookworm)");
                    } else if (version.equals("11") || version.startsWith("11.")) {
                        osInfo.setDistributionName("Debian 11 (Bullseye)");
                        osInfo.setFullName("Debian GNU/Linux 11 (bullseye)");
                    } else {
                        osInfo.setDistributionName("Debian " + version);
                        osInfo.setFullName("Debian GNU/Linux " + version);
                    }
                } else {
                    osInfo.setDistributionName("Debian GNU/Linux");
                    osInfo.setFullName("Debian GNU/Linux");
                }

                return true;
            }

            return false;
        } catch (Exception e) {
            logger.warn("检查Debian系统时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查RedHat/CentOS系统
     */
    private boolean checkRedHatFile(OsInfo osInfo, ClientSession session) {
        try {
            // 检查RedHat/CentOS特定文件
            String redhatRelease = MinaUtils.execCmdWithResult(session, "cat /etc/redhat-release 2>/dev/null");
            if (StringUtils.isNotBlank(redhatRelease)) {
                logger.info("发现redhat-release文件，可能是RedHat或CentOS");
                redhatRelease = redhatRelease.trim();

                // 提取版本号
                String versionId = null;
                Pattern versionPattern = Pattern.compile("release\\s+([\\d\\.]+)");
                Matcher versionMatcher = versionPattern.matcher(redhatRelease);
                if (versionMatcher.find()) {
                    versionId = versionMatcher.group(1);
                }

                if (redhatRelease.toLowerCase().contains("centos")) {
                    osInfo.setDistributionId("centos");
                    osInfo.setDistribution("CentOS");
                    osInfo.setDistributionType(LinuxDistribution.CENTOS);
                    osInfo.setFullName(redhatRelease);

                    // 设置简洁的显示名称
                    osInfo.setDisplayName("CentOS");

                    if (versionId != null) {
                        osInfo.setVersionId(versionId);
                        osInfo.setVersion(versionId);

                        // 设置特定版本标记和distributionName
                        if (versionId.startsWith("7")) {
                            osInfo.setCentOS7(true);
                            osInfo.setDistributionName("CentOS Linux 7");
                        } else if (versionId.startsWith("8")) {
                            osInfo.setCentOS8(true);
                            osInfo.setDistributionName("CentOS Linux 8");
                        } else {
                            osInfo.setDistributionName("CentOS Linux " + versionId);
                        }
                    } else {
                        osInfo.setDistributionName("CentOS Linux");
                    }
                } else if (redhatRelease.toLowerCase().contains("fedora")) {
                    // 处理Fedora系统
                    osInfo.setDistributionId("fedora");
                    osInfo.setDistribution("Fedora");
                    osInfo.setDistributionType(LinuxDistribution.REDHAT); // 目前还是归类为REDHAT族
                    osInfo.setFullName(redhatRelease);

                    // 设置简洁的显示名称
                    osInfo.setDisplayName("Fedora");

                    if (versionId != null) {
                        osInfo.setVersionId(versionId);
                        osInfo.setVersion(versionId);
                        osInfo.setDistributionName("Fedora " + versionId);
                    } else {
                        osInfo.setDistributionName("Fedora");
                    }
                } else if (redhatRelease.toLowerCase().contains("red hat")
                        || redhatRelease.toLowerCase().contains("redhat")) {
                    osInfo.setDistributionId("rhel");
                    osInfo.setDistribution("RedHat");
                    osInfo.setDistributionType(LinuxDistribution.REDHAT);
                    osInfo.setFullName(redhatRelease);

                    // 设置简洁的显示名称
                    osInfo.setDisplayName("Red Hat");

                    if (versionId != null) {
                        osInfo.setVersionId(versionId);
                        osInfo.setVersion(versionId);
                        osInfo.setDistributionName("Red Hat Enterprise Linux " + versionId);
                    } else {
                        osInfo.setDistributionName("Red Hat Enterprise Linux");
                    }
                } else {
                    // 其他基于RedHat的发行版
                    osInfo.setDistributionId("rhel-based");
                    osInfo.setDistribution("RedHat Based");
                    osInfo.setDistributionType(LinuxDistribution.REDHAT);
                    osInfo.setDistributionName(redhatRelease);
                    osInfo.setFullName(redhatRelease);
                    osInfo.setDisplayName("RedHat Based");
                }

                return true;
            }

            // 检查是否存在Fedora系统
            String fedoraRelease = MinaUtils.execCmdWithResult(session, "cat /etc/fedora-release 2>/dev/null");
            if (StringUtils.isNotBlank(fedoraRelease)) {
                logger.info("发现fedora-release文件，确认为Fedora系统");
                fedoraRelease = fedoraRelease.trim();

                // 提取版本号
                String versionId = null;
                Pattern versionPattern = Pattern.compile("release\\s+([\\d\\.]+)");
                Matcher versionMatcher = versionPattern.matcher(fedoraRelease);
                if (versionMatcher.find()) {
                    versionId = versionMatcher.group(1);
                }

                // 设置Fedora系统信息
                osInfo.setDistributionId("fedora");
                osInfo.setDistribution("Fedora");
                osInfo.setDistributionType(LinuxDistribution.REDHAT); // 目前还是归类为REDHAT族
                osInfo.setFullName(fedoraRelease);
                osInfo.setDisplayName("Fedora");

                if (versionId != null) {
                    osInfo.setVersionId(versionId);
                    osInfo.setVersion(versionId);
                    osInfo.setDistributionName("Fedora " + versionId);
                } else {
                    osInfo.setDistributionName("Fedora");
                }

                return true;
            }

            return false;
        } catch (Exception e) {
            logger.warn("检查RedHat/CentOS系统时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 设置版本特定标志
     */
    private void setVersionFlags(OsInfo osInfo) {
        // 重置所有版本标志
        osInfo.setCentOS7(false);
        osInfo.setCentOS8(false);
        osInfo.setUbuntu22(false);
        osInfo.setUbuntu24(false);
        osInfo.setKylinV10(false);
        osInfo.setKylinV4(false);

        String distId = osInfo.getDistributionId();
        String version = osInfo.getVersionId();

        if (distId == null || version == null) {
            return;
        }

        // 设置CentOS版本标志
        if (distId.equals("centos")) {
            if (version.startsWith("7")) {
                osInfo.setCentOS7(true);
            } else if (version.startsWith("8")) {
                osInfo.setCentOS8(true);
            }
        }
        // 设置Ubuntu版本标志
        else if (distId.equals("ubuntu")) {
            if (version.equals("22.04") || version.startsWith("22.")) {
                osInfo.setUbuntu22(true);
            } else if (version.equals("24.04") || version.equals("24.10") || version.startsWith("24.")) {
                osInfo.setUbuntu24(true);
            }
        }
        // 设置麒麟版本标志
        else if (distId.equals("kylin")) {
            if (version.equals("V4") || version.equals("4")) {
                osInfo.setKylinV4(true);
            } else if (version.equals("V10") || version.equals("10")) {
                osInfo.setKylinV10(true);
            }
        }
        // 设置Fedora版本标志 (虽然当前没有特定版本标志，但保留扩展性)
        else if (distId.equals("fedora")) {
            // 目前没有为Fedora设置特定的版本标志
            // 如果未来需要添加Fedora特定版本的标志，可以在这里扩展
            logger.info("检测到Fedora系统，版本: {}", version);
        }
    }

    /**
     * 设置显示名称
     */
    private void setDisplayName(OsInfo osInfo) {
        // 如果已经设置了显示名称，直接返回
        if (StringUtils.isNotBlank(osInfo.getDisplayName())) {
            return;
        }

        String distType = osInfo.getDistribution();
        if (distType == null) {
            osInfo.setDisplayName("未知Linux发行版");
            osInfo.setDistributionName("未知Linux发行版");
            osInfo.setFullName("未知Linux发行版");
            return;
        }

        switch (distType) {
            case "CentOS":
                // 设置简洁的显示名称，与Ubuntu保持一致风格
                osInfo.setDisplayName("CentOS");

                String centosVersion = osInfo.getVersionId();
                if (StringUtils.isNotBlank(centosVersion)) {
                    // 为悬浮卡片设置详细版本信息
                    if (centosVersion.startsWith("7")) {
                        osInfo.setDistributionName("CentOS Linux 7");
                        // 设置完整名称如果尚未设置
                        if (StringUtils.isBlank(osInfo.getFullName())) {
                            osInfo.setFullName("CentOS Linux release 7." + centosVersion.substring(2));
                        }
                    } else if (centosVersion.startsWith("8")) {
                        osInfo.setDistributionName("CentOS Linux 8");
                        // 设置完整名称如果尚未设置
                        if (StringUtils.isBlank(osInfo.getFullName())) {
                            osInfo.setFullName("CentOS Linux release 8." + centosVersion.substring(2));
                        }
                    } else {
                        osInfo.setDistributionName("CentOS Linux " + centosVersion);
                        // 设置完整名称如果尚未设置
                        if (StringUtils.isBlank(osInfo.getFullName())) {
                            osInfo.setFullName("CentOS Linux release " + centosVersion);
                        }
                    }
                } else {
                    osInfo.setDistributionName("CentOS Linux");
                    // 设置完整名称如果尚未设置
                    if (StringUtils.isBlank(osInfo.getFullName())) {
                        osInfo.setFullName("CentOS Linux");
                    }
                }
                break;
            case "Ubuntu":
                // 设置列表显示用的简单名称
                osInfo.setDisplayName("Ubuntu");

                // 保留设置distributionName，为悬浮卡片提供详细信息
                String version = osInfo.getVersionId();
                if (StringUtils.isNotBlank(version)) {
                    // 为Ubuntu特定版本设置分发名称（用于悬浮卡片详细信息）
                    if (version.equals("22.04") || version.startsWith("22.")) {
                        osInfo.setDistributionName("Ubuntu 22.04 LTS");
                    } else if (version.equals("24.04")) {
                        osInfo.setDistributionName("Ubuntu 24.04 LTS");
                    } else if (version.equals("24.10")) {
                        osInfo.setDistributionName("Ubuntu 24.10");
                    } else {
                        osInfo.setDistributionName("Ubuntu " + version);
                    }
                } else {
                    osInfo.setDistributionName("Ubuntu Linux");
                }
                break;
            case "Debian":
                // 设置简洁的显示名称
                osInfo.setDisplayName("Debian");

                String debVersion = osInfo.getVersionId();
                if (StringUtils.isNotBlank(debVersion)) {
                    if (debVersion.startsWith("12")) {
                        osInfo.setDistributionName("Debian 12 (Bookworm)");
                        // 设置完整名称如果尚未设置
                        if (StringUtils.isBlank(osInfo.getFullName())) {
                            osInfo.setFullName("Debian GNU/Linux 12 (bookworm)");
                        }
                    } else if (debVersion.startsWith("11")) {
                        osInfo.setDistributionName("Debian 11 (Bullseye)");
                        // 设置完整名称如果尚未设置
                        if (StringUtils.isBlank(osInfo.getFullName())) {
                            osInfo.setFullName("Debian GNU/Linux 11 (bullseye)");
                        }
                    } else if (debVersion.startsWith("10")) {
                        osInfo.setDistributionName("Debian 10 (Buster)");
                        // 设置完整名称如果尚未设置
                        if (StringUtils.isBlank(osInfo.getFullName())) {
                            osInfo.setFullName("Debian GNU/Linux 10 (buster)");
                        }
                    } else if (debVersion.startsWith("9")) {
                        osInfo.setDistributionName("Debian 9 (Stretch)");
                        // 设置完整名称如果尚未设置
                        if (StringUtils.isBlank(osInfo.getFullName())) {
                            osInfo.setFullName("Debian GNU/Linux 9 (stretch)");
                        }
                    } else {
                        osInfo.setDistributionName("Debian " + debVersion);
                        // 设置完整名称如果尚未设置
                        if (StringUtils.isBlank(osInfo.getFullName())) {
                            osInfo.setFullName("Debian GNU/Linux " + debVersion);
                        }
                    }
                } else {
                    osInfo.setDistributionName("Debian GNU/Linux");
                    // 设置完整名称如果尚未设置
                    if (StringUtils.isBlank(osInfo.getFullName())) {
                        osInfo.setFullName("Debian GNU/Linux");
                    }
                }
                break;
            case "RedHat":
                // 设置简洁的显示名称
                osInfo.setDisplayName("Red Hat");

                String rhVersion = osInfo.getVersionId();
                if (StringUtils.isNotBlank(rhVersion)) {
                    osInfo.setDistributionName("Red Hat Enterprise Linux " + rhVersion);
                    // 设置完整名称如果尚未设置
                    if (StringUtils.isBlank(osInfo.getFullName())) {
                        osInfo.setFullName("Red Hat Enterprise Linux release " + rhVersion);
                    }
                } else {
                    osInfo.setDistributionName("Red Hat Enterprise Linux");
                    // 设置完整名称如果尚未设置
                    if (StringUtils.isBlank(osInfo.getFullName())) {
                        osInfo.setFullName("Red Hat Enterprise Linux");
                    }
                }
                break;
            case "Fedora":
                // 设置简洁的显示名称
                osInfo.setDisplayName("Fedora");

                String fedoraVersion = osInfo.getVersionId();
                if (StringUtils.isNotBlank(fedoraVersion)) {
                    osInfo.setDistributionName("Fedora " + fedoraVersion);
                    // 设置完整名称如果尚未设置
                    if (StringUtils.isBlank(osInfo.getFullName())) {
                        osInfo.setFullName("Fedora release " + fedoraVersion);
                    }
                } else {
                    osInfo.setDistributionName("Fedora");
                    // 设置完整名称如果尚未设置
                    if (StringUtils.isBlank(osInfo.getFullName())) {
                        osInfo.setFullName("Fedora Linux");
                    }
                }
                break;
            case "Kylin":
                // 设置简洁的显示名称
                osInfo.setDisplayName("中标麒麟");

                String kylinVersion = osInfo.getVersionId();
                if ("V10".equals(kylinVersion) || "10".equals(kylinVersion)) {
                    osInfo.setDistributionName("中标麒麟 V10");
                    // 设置完整名称如果尚未设置
                    if (StringUtils.isBlank(osInfo.getFullName())) {
                        osInfo.setFullName("中标麒麟操作系统 V10");
                    }
                } else if ("V4".equals(kylinVersion) || "4".equals(kylinVersion)) {
                    osInfo.setDistributionName("中标麒麟 V4");
                    // 设置完整名称如果尚未设置
                    if (StringUtils.isBlank(osInfo.getFullName())) {
                        osInfo.setFullName("中标麒麟操作系统 V4");
                    }
                } else if (StringUtils.isNotBlank(kylinVersion)) {
                    osInfo.setDistributionName("中标麒麟 " + kylinVersion);
                    // 设置完整名称如果尚未设置
                    if (StringUtils.isBlank(osInfo.getFullName())) {
                        osInfo.setFullName("中标麒麟操作系统 " + kylinVersion);
                    }
                } else {
                    osInfo.setDistributionName("中标麒麟");
                    // 设置完整名称如果尚未设置
                    if (StringUtils.isBlank(osInfo.getFullName())) {
                        osInfo.setFullName("中标麒麟操作系统");
                    }
                }
                break;
            default:
                // 对于其他发行版
                if (StringUtils.isNotBlank(osInfo.getFullName())) {
                    // 如果已有fullName，则用简化版本作为displayName
                    osInfo.setDisplayName(distType);
                    // 确保distributionName也被设置
                    if (StringUtils.isBlank(osInfo.getDistributionName())) {
                        osInfo.setDistributionName(osInfo.getFullName());
                    }
                } else if (StringUtils.isNotBlank(osInfo.getDistributionName())) {
                    // 如果已有distributionName，则用简化版本作为displayName，复制给fullName
                    osInfo.setDisplayName(distType);
                    osInfo.setFullName(osInfo.getDistributionName());
                } else {
                    // 全都没有，则使用发行版名称
                    osInfo.setDisplayName(distType);
                    osInfo.setDistributionName(distType);
                    osInfo.setFullName(distType);
                }
        }
    }

    /**
     * 判断是否为纯数字版本号
     */
    private boolean isNumericVersion(String version) {
        if (StringUtils.isBlank(version)) {
            return false;
        }

        // 判断是否为数字或数字+点号的格式
        return version.matches("\\d+(\\.\\d+)*");
    }

    /**
     * 收集DNS服务器信息
     */
    private void collectDnsInfo(OsInfo osInfo, ClientSession session) {
        try {
            String dnsInfo = MinaUtils.execCmdWithResult(session,
                    "cat /etc/resolv.conf | grep nameserver | awk '{print $2}'");
            if (StringUtils.isNotBlank(dnsInfo)) {
                List<String> dnsServers = new ArrayList<>();
                for (String line : dnsInfo.split("\n")) {
                    if (StringUtils.isNotBlank(line)) {
                        dnsServers.add(line.trim());
                    }
                }

                if (!dnsServers.isEmpty()) {
                    osInfo.setDnsServers(dnsServers);
                    logger.info("获取到DNS服务器列表: {}", dnsServers);
                }
            }
        } catch (Exception e) {
            logger.warn("收集DNS服务器信息失败: {}", e.getMessage());
        }
    }

    /**
     * 收集CPU信息
     */
    private void collectCpuInfo(OsInfo osInfo, ClientSession session) {
        try {
            // 初始化CPU信息对象（如果不存在）
            if (osInfo.getCpuInfo() == null) {
                osInfo.setCpuInfo(new CpuInfo());
            }
            osInfo.setCpuStatus(OsInfoStatusEnum.COLLECTING);

            // 获取CPU型号
            String cpuModel = MinaUtils.execCmdWithResult(session, "cat /proc/cpuinfo | grep 'model name' | head -n 1");
            if (StringUtils.isNotBlank(cpuModel)) {
                String model = cpuModel.split(":")[1].trim();
                osInfo.getCpuInfo().setModel(model);
                logger.info("获取到CPU型号: {}", model);
            }

            // 获取CPU核心数和线程数
            String cpuCoresCmd = "grep -c ^processor /proc/cpuinfo";
            String physicalCoresCmd = "grep 'cpu cores' /proc/cpuinfo | head -1 | awk '{print $4}'";

            // 逻辑CPU数量（包括超线程）
            String logicalCores = MinaUtils.execCmdWithResult(session, cpuCoresCmd);
            if (StringUtils.isNotBlank(logicalCores)) {
                try {
                    int cores = Integer.parseInt(logicalCores.trim());
                    osInfo.getCpuInfo().setLogicalCores(cores);
                    logger.info("获取到逻辑CPU数量: {}", cores);
                } catch (NumberFormatException e) {
                    logger.warn("解析逻辑CPU数量失败: {}", e.getMessage());
                }
            }

            // 物理核心数
            String physicalCores = MinaUtils.execCmdWithResult(session, physicalCoresCmd);
            if (StringUtils.isNotBlank(physicalCores) && !physicalCores.trim().equals("")) {
                try {
                    int cores = Integer.parseInt(physicalCores.trim());
                    osInfo.getCpuInfo().setCores(cores);
                    osInfo.getCpuInfo().setPhysicalCount(cores);
                    logger.info("获取到物理CPU核心数: {}", cores);

                    // 计算每核心线程数（如果有逻辑核心数）
                    if (osInfo.getCpuInfo().getLogicalCores() != null &&
                            osInfo.getCpuInfo().getLogicalCores() > 0 &&
                            cores > 0) {
                        int threadsPerCore = osInfo.getCpuInfo().getLogicalCores() / cores;
                        osInfo.getCpuInfo().setThreadsPerCore(threadsPerCore);
                        logger.info("计算得到每核心线程数: {}", threadsPerCore);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("解析物理CPU核心数失败: {}", e.getMessage());
                }
            } else {
                // 如果无法获取物理核心数，则使用逻辑核心数作为物理核心数
                if (osInfo.getCpuInfo().getLogicalCores() != null) {
                    osInfo.getCpuInfo().setCores(1); // 默认至少有1个物理核心

                    // 尝试检测物理CPU数量
                    String physicalCountCmd = "grep 'physical id' /proc/cpuinfo | sort -u | wc -l";
                    String physicalCount = MinaUtils.execCmdWithResult(session, physicalCountCmd);
                    if (StringUtils.isNotBlank(physicalCount)) {
                        try {
                            int count = Integer.parseInt(physicalCount.trim());
                            osInfo.getCpuInfo().setPhysicalCount(count);
                            logger.info("获取到物理CPU数量: {}", count);
                        } catch (NumberFormatException e) {
                            osInfo.getCpuInfo().setPhysicalCount(1);
                            logger.warn("解析物理CPU数量失败，使用默认值: 1");
                        }
                    } else {
                        osInfo.getCpuInfo().setPhysicalCount(1);
                        logger.info("无法获取物理CPU数量，使用默认值: 1");
                    }

                    // 设置每核心线程数
                    int threadsPerCore = osInfo.getCpuInfo().getLogicalCores()
                            / Math.max(1, osInfo.getCpuInfo().getCores());
                    osInfo.getCpuInfo().setThreadsPerCore(threadsPerCore);
                }
            }

            // 获取CPU使用率
            String cpuUsageCmd = "top -bn1 | grep 'Cpu(s)' | sed 's/.*, *\\([0-9.]*\\)%* id.*/\\1/' | awk '{print 100 - $1}'";
            String cpuUsage = MinaUtils.execCmdWithResult(session, cpuUsageCmd);
            if (StringUtils.isNotBlank(cpuUsage)) {
                try {
                    double usage = Double.parseDouble(cpuUsage.trim());
                    osInfo.getCpuInfo().setUsagePercent(usage);
                    logger.info("获取到CPU使用率: {}%", usage);
                } catch (NumberFormatException e) {
                    logger.warn("解析CPU使用率失败: {}", e.getMessage());
                }
            }

            // 标记收集状态为成功
            osInfo.getCpuInfo().setStatus(OsInfoStatusEnum.SUCCESS);
            osInfo.setCpuStatus(OsInfoStatusEnum.SUCCESS);
            osInfo.getCpuInfo().setTypeName("CPU");
            logger.info("CPU信息收集完成");

        } catch (Exception e) {
            logger.error("收集CPU信息失败: {}", e.getMessage(), e);

            // 设置默认值和错误状态
            if (osInfo.getCpuInfo() == null) {
                osInfo.setCpuInfo(new CpuInfo());
            }

            osInfo.getCpuInfo().setModel("未知CPU");
            osInfo.getCpuInfo().setCores(1);
            osInfo.getCpuInfo().setLogicalCores(1);
            osInfo.getCpuInfo().setPhysicalCount(1);
            osInfo.getCpuInfo().setThreadsPerCore(1);
            osInfo.getCpuInfo().setErrorMessage(e.getMessage());
            osInfo.getCpuInfo().setStatus(OsInfoStatusEnum.ERROR);
            osInfo.setCpuStatus(OsInfoStatusEnum.ERROR);
            osInfo.getCpuInfo().setTypeName("CPU");
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
        logger.debug("开始收集内存信息");

        try {
            // 确保内存信息对象存在
            if (osInfo.getMemoryInfo() == null) {
                osInfo.setMemoryInfo(new MemoryInfo());
            }
            MemoryInfo memoryInfo = osInfo.getMemoryInfo();

            // 读取/proc/meminfo获取内存信息
            String meminfoOutput = MinaUtils.execCmdWithResult(session, "cat /proc/meminfo");
            if (StringUtils.isNotBlank(meminfoOutput)) {
                // 解析总内存
                Pattern totalMemPattern = Pattern.compile("MemTotal:\\s+(\\d+)\\s+kB");
                Matcher totalMemMatcher = totalMemPattern.matcher(meminfoOutput);
                if (totalMemMatcher.find()) {
                    try {
                        long totalMemKB = Long.parseLong(totalMemMatcher.group(1).trim());
                        // 转换为MB
                        Long totalMemMB = totalMemKB / 1024;
                        memoryInfo.setTotalMemory(totalMemMB);
                        logger.debug("获取到总内存: {} MB", totalMemMB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析总内存失败: {}", e.getMessage());
                    }
                }

                // 解析可用内存（MemAvailable或MemFree）
                Pattern availableMemPattern = Pattern.compile("MemAvailable:\\s+(\\d+)\\s+kB");
                Matcher availableMemMatcher = availableMemPattern.matcher(meminfoOutput);
                if (availableMemMatcher.find()) {
                    try {
                        long availableMemKB = Long.parseLong(availableMemMatcher.group(1).trim());
                        // 转换为MB
                        Long availableMemMB = availableMemKB / 1024;
                        memoryInfo.setAvailableMemory(availableMemMB);
                        logger.debug("获取到可用内存: {} MB", availableMemMB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析可用内存失败: {}", e.getMessage());
                    }
                } else {
                    // 如果没有MemAvailable，则使用MemFree
                    Pattern freeMemPattern = Pattern.compile("MemFree:\\s+(\\d+)\\s+kB");
                    Matcher freeMemMatcher = freeMemPattern.matcher(meminfoOutput);
                    if (freeMemMatcher.find()) {
                        try {
                            long freeMemKB = Long.parseLong(freeMemMatcher.group(1).trim());
                            // 转换为MB
                            Long freeMemMB = freeMemKB / 1024;
                            memoryInfo.setAvailableMemory(freeMemMB);
                            logger.debug("获取到空闲内存: {} MB", freeMemMB);
                        } catch (NumberFormatException e) {
                            logger.warn("解析空闲内存失败: {}", e.getMessage());
                        }
                    }
                }

                // 计算内存使用率
                if (memoryInfo.getTotalMemory() != null && memoryInfo.getAvailableMemory() != null) {
                    double usedMemory = memoryInfo.getTotalMemory() - memoryInfo.getAvailableMemory();
                    double usagePercent = (usedMemory / memoryInfo.getTotalMemory()) * 100;
                    memoryInfo.setUsagePercent(Math.round(usagePercent * 10) / 10.0);
                    logger.debug("计算得到内存使用率: {}%", memoryInfo.getUsagePercent());
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("memoryInfo");
            memoryInfo.setStatus(OsInfoStatusEnum.SUCCESS);

            logger.info("内存信息收集完成");
        } catch (Exception e) {
            logger.error("收集内存信息时出错: {}", e.getMessage(), e);
            if (osInfo.getMemoryInfo() != null) {
                osInfo.getMemoryInfo().setStatus(OsInfoStatusEnum.ERROR);
                osInfo.getMemoryInfo().setErrorMessage("收集内存信息失败: " + e.getMessage());
            }
            throw e; // 向上抛出异常，由调用者处理
        }
    }

    /**
     * 收集磁盘信息
     * 单独提取方法以便队列系统调用
     * 
     * @param osInfo  操作系统信息对象
     * @param session SSH会话
     */
    public void collectDiskInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集磁盘信息");

        try {
            // 确保磁盘信息对象存在
            if (osInfo.getDiskInfo() == null) {
                osInfo.setDiskInfo(new DiskInfo());
            }
            DiskInfo diskInfo = osInfo.getDiskInfo();

            // 使用df命令获取磁盘使用情况
            String dfOutput = MinaUtils.execCmdWithResult(session, "df -P");
            if (StringUtils.isNotBlank(dfOutput)) {
                // 解析磁盘信息
                String[] lines = dfOutput.split("\n");
                long totalBytes = 0;
                long usedBytes = 0;
                long availableBytes = 0;

                for (int i = 1; i < lines.length; i++) { // 跳过标题行
                    String line = lines[i].trim();
                    String[] parts = line.split("\\s+");

                    if (parts.length >= 6) {
                        try {
                            // 排除没有路径前缀的特殊文件系统
                            if (parts[0].startsWith("/") || parts[5].startsWith("/")) {
                                long size = Long.parseLong(parts[1]) * 1024; // 转换为字节
                                long used = Long.parseLong(parts[2]) * 1024; // 转换为字节
                                long avail = Long.parseLong(parts[3]) * 1024; // 转换为字节

                                totalBytes += size;
                                usedBytes += used;
                                availableBytes += avail;
                            }
                        } catch (NumberFormatException e) {
                            logger.warn("解析磁盘信息失败: {}", line);
                        }
                    }
                }

                // 转换为GB并保存
                double totalGB = (double) totalBytes / (1024 * 1024 * 1024);
                double usedGB = (double) usedBytes / (1024 * 1024 * 1024);
                double availableGB = (double) availableBytes / (1024 * 1024 * 1024);

                diskInfo.setTotalDiskSpace(totalGB);
                diskInfo.setUsedDiskSpace(usedGB);
                diskInfo.setAvailableDiskSpace(availableGB);

                // 计算使用率
                if (totalGB > 0) {
                    double usagePercent = (usedGB / totalGB) * 100;
                    diskInfo.setUsagePercent(Math.round(usagePercent * 10) / 10.0);
                }

                logger.debug("获取到磁盘总空间: {}GB, 已用空间: {}GB, 可用空间: {}GB, 使用率: {}%",
                        totalGB, usedGB, availableGB, diskInfo.getUsagePercent());
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("diskInfo");
            diskInfo.setStatus(OsInfoStatusEnum.SUCCESS);

            logger.info("磁盘信息收集完成");
        } catch (Exception e) {
            logger.error("收集磁盘信息时出错: {}", e.getMessage(), e);
            if (osInfo.getDiskInfo() != null) {
                osInfo.getDiskInfo().setStatus(OsInfoStatusEnum.ERROR);
                osInfo.getDiskInfo().setErrorMessage("收集磁盘信息失败: " + e.getMessage());
            }
            throw e; // 向上抛出异常，由调用者处理
        }
    }

    /**
     * 收集交换分区信息
     * 单独提取方法以便队列系统调用
     * 
     * @param osInfo  操作系统信息对象
     * @param session SSH会话
     */
    public void collectSwapInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集交换分区信息");

        try {
            // 确保交换空间信息对象存在
            if (osInfo.getSwapInfo() == null) {
                osInfo.setSwapInfo(new SwapInfo());
            }
            SwapInfo swapInfo = osInfo.getSwapInfo();

            // 读取/proc/meminfo中的交换分区信息
            String swapInfoOutput = MinaUtils.execCmdWithResult(session, "grep Swap /proc/meminfo");
            if (StringUtils.isNotBlank(swapInfoOutput)) {
                // 解析交换分区总容量
                Pattern totalSwapPattern = Pattern.compile("SwapTotal:\\s+(\\d+)\\s+kB");
                Matcher totalSwapMatcher = totalSwapPattern.matcher(swapInfoOutput);
                if (totalSwapMatcher.find()) {
                    try {
                        long totalSwapKB = Long.parseLong(totalSwapMatcher.group(1).trim());

                        // 检查交换空间是否开启（总容量为0表示未开启）
                        if (totalSwapKB <= 0) {
                            logger.warn("Linux主机交换空间未开启 (SwapTotal=0)");
                            swapInfo.setTotalSwap(0L);
                            swapInfo.setAvailableSwap(0L);
                            swapInfo.setEnabled(false);
                            swapInfo.setErrorMessage("交换空间未开启，建议配置交换分区以提高系统稳定性");

                            // 设置格式化值为0
                            swapInfo.setTotalSwapFormatted("0.0");
                            swapInfo.setTotalSwapUnit("GB");
                            swapInfo.setAvailableSwapFormatted("0.0");
                            swapInfo.setAvailableSwapUnit("GB");
                            swapInfo.setUsedSwapFormatted("0.0");
                            swapInfo.setUsedSwapUnit("GB");
                        } else {
                            // 交换空间已开启
                            // 转换为MB并保存
                            Long totalSwapMB = totalSwapKB / 1024;
                            swapInfo.setTotalSwap(totalSwapMB);
                            swapInfo.setEnabled(true);

                            // 解析交换分区可用容量
                            Pattern freeSwapPattern = Pattern.compile("SwapFree:\\s+(\\d+)\\s+kB");
                            Matcher freeSwapMatcher = freeSwapPattern.matcher(swapInfoOutput);
                            if (freeSwapMatcher.find()) {
                                try {
                                    long freeSwapKB = Long.parseLong(freeSwapMatcher.group(1).trim());
                                    // 转换为MB并保存
                                    Long freeSwapMB = freeSwapKB / 1024;
                                    swapInfo.setAvailableSwap(freeSwapMB);

                                    // 计算使用率
                                    long usedSwapMB = totalSwapMB - freeSwapMB;
                                    double usagePercent = ((double) usedSwapMB / totalSwapMB) * 100;
                                    swapInfo.setUsagePercent(Math.round(usagePercent * 10) / 10.0);

                                    // 设置格式化后的值和单位
                                    swapInfo.setTotalSwapFormatted(String.format("%.1f", totalSwapMB / 1024.0));
                                    swapInfo.setTotalSwapUnit("GB");
                                    swapInfo.setAvailableSwapFormatted(String.format("%.1f", freeSwapMB / 1024.0));
                                    swapInfo.setAvailableSwapUnit("GB");
                                    swapInfo.setUsedSwapFormatted(String.format("%.1f", usedSwapMB / 1024.0));
                                    swapInfo.setUsedSwapUnit("GB");

                                    logger.debug("获取到交换空间: 总容量={}MB, 可用容量={}MB, 已用={}MB, 使用率={}%",
                                            totalSwapMB, freeSwapMB, usedSwapMB, swapInfo.getUsagePercent());
                                } catch (NumberFormatException e) {
                                    logger.warn("解析交换分区可用容量失败: {}", e.getMessage());
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("解析交换分区总容量失败: {}", e.getMessage());
                    }
                } else {
                    // 未找到交换分区信息，可能是未启用
                    logger.warn("未找到交换分区信息，可能未启用交换空间");
                    swapInfo.setTotalSwap(0L);
                    swapInfo.setAvailableSwap(0L);
                    swapInfo.setEnabled(false);
                    swapInfo.setErrorMessage("未找到交换分区信息，建议配置交换分区以提高系统稳定性");

                    // 设置格式化值为0
                    swapInfo.setTotalSwapFormatted("0.0");
                    swapInfo.setTotalSwapUnit("GB");
                    swapInfo.setAvailableSwapFormatted("0.0");
                    swapInfo.setAvailableSwapUnit("GB");
                    swapInfo.setUsedSwapFormatted("0.0");
                    swapInfo.setUsedSwapUnit("GB");
                }
            } else {
                // 命令执行失败或无输出
                logger.warn("获取交换分区信息命令无返回结果");
                swapInfo.setTotalSwap(0L);
                swapInfo.setAvailableSwap(0L);
                swapInfo.setEnabled(false);
                swapInfo.setErrorMessage("无法获取交换分区信息");

                // 设置格式化值为0
                swapInfo.setTotalSwapFormatted("0.0");
                swapInfo.setTotalSwapUnit("GB");
                swapInfo.setAvailableSwapFormatted("0.0");
                swapInfo.setAvailableSwapUnit("GB");
                swapInfo.setUsedSwapFormatted("0.0");
                swapInfo.setUsedSwapUnit("GB");
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("swapInfo");
            swapInfo.setStatus(OsInfoStatusEnum.SUCCESS);

            logger.info("交换分区信息收集完成");
        } catch (Exception e) {
            logger.error("收集交换分区信息时出错: {}", e.getMessage(), e);
            if (osInfo.getSwapInfo() != null) {
                osInfo.getSwapInfo().setStatus(OsInfoStatusEnum.ERROR);
                osInfo.getSwapInfo().setErrorMessage("收集交换分区信息失败: " + e.getMessage());
            }
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
        logger.debug("开始收集GPU信息");

        try {
            // 确保GPU信息对象存在
            if (osInfo.getGpuInfo() == null) {
                osInfo.setGpuInfo(new GpuInfo());
            }
            GpuInfo gpuInfo = osInfo.getGpuInfo();

            // 尝试获取GPU信息
            String gpuInfoCmd = "lspci | grep -i 'vga\\|3d\\|2d' | cut -d ':' -f3";
            String gpuInfoStr = MinaUtils.execCmdWithResult(session, gpuInfoCmd);

            if (StringUtils.isNotBlank(gpuInfoStr)) {
                // 通过lspci发现GPU
                gpuInfo.setInfo(gpuInfoStr.trim());
                gpuInfo.setModel(gpuInfoStr.trim());

                // 获取显存信息 - 先尝试通过lspci详细输出获取
                String gpuMemoryCmd = "lspci -v | grep -i vga -A 10 | grep -i 'memory.*size' | head -n 1 | sed -r 's/.*size=([0-9]+)[Mm].*/\\1/g'";
                String gpuMemoryStr = MinaUtils.execCmdWithResult(session, gpuMemoryCmd);

                if (StringUtils.isNotBlank(gpuMemoryStr)) {
                    try {
                        // 转换MB到GB
                        double gpuMemoryMB = Double.parseDouble(gpuMemoryStr.trim());
                        double gpuMemoryGB = Math.round(gpuMemoryMB / 1024 * 10) / 10.0;
                        gpuInfo.setMemorySize(gpuMemoryGB);
                        logger.debug("获取到GPU显存: {} GB", gpuMemoryGB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析GPU显存失败: {}", gpuMemoryStr);
                    }
                }

                logger.debug("获取到GPU信息: {}", gpuInfoStr.trim());
            } else {
                // 尝试使用nvidia-smi获取NVIDIA GPU信息
                String nvidiaCmd = "which nvidia-smi && nvidia-smi --query-gpu=name,memory.total --format=csv,noheader";
                String nvidiaSmiOutput = MinaUtils.execCmdWithResult(session, nvidiaCmd);

                if (StringUtils.isNotBlank(nvidiaSmiOutput) && !nvidiaSmiOutput.contains("which: no nvidia-smi")) {
                    // 通过nvidia-smi发现GPU
                    gpuInfo.setInfo(nvidiaSmiOutput.trim());
                    gpuInfo.setModel(nvidiaSmiOutput.trim().split(",")[0].trim());

                    // 提取显存信息
                    Pattern memPattern = Pattern.compile("(\\d+) MiB");
                    Matcher memMatcher = memPattern.matcher(nvidiaSmiOutput);

                    if (memMatcher.find()) {
                        try {
                            double gpuMemoryMB = Double.parseDouble(memMatcher.group(1).trim());
                            double gpuMemoryGB = Math.round(gpuMemoryMB / 1024 * 10) / 10.0;
                            gpuInfo.setMemorySize(gpuMemoryGB);
                            logger.debug("获取到NVIDIA GPU显存: {} GB", gpuMemoryGB);
                        } catch (NumberFormatException e) {
                            logger.warn("解析NVIDIA GPU显存失败: {}", e.getMessage());
                        }
                    }

                    logger.debug("获取到NVIDIA GPU信息: {}", nvidiaSmiOutput.trim());
                } else {
                    // 未检测到GPU
                    gpuInfo.setInfo("无GPU或无法检测");
                    gpuInfo.setModel("无GPU设备");
                    gpuInfo.setMemorySize(0.0);
                    logger.debug("未检测到GPU信息");
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("gpuInfo");
            gpuInfo.setStatus(OsInfoStatusEnum.SUCCESS);

            logger.info("GPU信息收集完成");
        } catch (Exception e) {
            logger.error("收集GPU信息时出错: {}", e.getMessage(), e);
            if (osInfo.getGpuInfo() != null) {
                osInfo.getGpuInfo().setStatus(OsInfoStatusEnum.ERROR);
                osInfo.getGpuInfo().setErrorMessage("收集GPU信息失败: " + e.getMessage());
            }
            throw e; // 向上抛出异常，由调用者处理
        }
    }

    /**
     * 尝试读取麒麟系统特定文件获取更多信息
     */
    private void tryReadKylinSpecificFiles(OsInfo osInfo, ClientSession session) {
        try {
            // 尝试读取/etc/neokylin-release文件
            String neokylinRelease = MinaUtils.execCmdWithResult(session, "cat /etc/neokylin-release 2>/dev/null");
            if (StringUtils.isNotBlank(neokylinRelease)) {
                logger.info("读取到/etc/neokylin-release: {}", neokylinRelease.trim());
                osInfo.setFullName(neokylinRelease.trim());
                return;
            }

            // 尝试读取/etc/kylin-release文件
            String kylinRelease = MinaUtils.execCmdWithResult(session, "cat /etc/kylin-release 2>/dev/null");
            if (StringUtils.isNotBlank(kylinRelease)) {
                logger.info("读取到/etc/kylin-release: {}", kylinRelease.trim());
                osInfo.setFullName(kylinRelease.trim());
                return;
            }

            // 尝试读取/etc/uos-release文件 (统信UOS文件)
            String uosRelease = MinaUtils.execCmdWithResult(session, "cat /etc/uos-release 2>/dev/null");
            if (StringUtils.isNotBlank(uosRelease)) {
                logger.info("读取到/etc/uos-release: {}", uosRelease.trim());
                osInfo.setFullName(uosRelease.trim());
                return;
            }

            // 尝试从os-release中获取信息
            String osRelease = MinaUtils.execCmdWithResult(session, "cat /etc/os-release 2>/dev/null");
            if (StringUtils.isNotBlank(osRelease)) {
                logger.info("尝试从os-release中获取麒麟系统信息");

                // 提取PRETTY_NAME (完整名称)
                Pattern prettyNamePattern = Pattern.compile("^PRETTY_NAME=\"?(.*?)\"?$", Pattern.MULTILINE);
                Matcher prettyNameMatcher = prettyNamePattern.matcher(osRelease);
                if (prettyNameMatcher.find()) {
                    String prettyName = prettyNameMatcher.group(1).trim();
                    osInfo.setFullName(prettyName);
                    logger.info("从os-release获取到麒麟系统完整名称: {}", prettyName);
                }

                // 提取VERSION_ID (版本号)
                Pattern versionIdPattern = Pattern.compile("^VERSION_ID=\"?(.*?)\"?$", Pattern.MULTILINE);
                Matcher versionIdMatcher = versionIdPattern.matcher(osRelease);
                if (versionIdMatcher.find()) {
                    String versionId = versionIdMatcher.group(1).trim();
                    osInfo.setVersionId(versionId);
                    osInfo.setVersion(versionId);
                    logger.info("从os-release获取到麒麟系统版本ID: {}", versionId);

                    // 设置版本特定标记
                    if ("V10".equals(versionId) || "10".equals(versionId)) {
                        osInfo.setKylinV10(true);
                        osInfo.setDistributionName("中标麒麟 V10");
                    } else if ("V4".equals(versionId) || "4".equals(versionId)) {
                        osInfo.setKylinV4(true);
                        osInfo.setDistributionName("中标麒麟 V4");
                    } else {
                        osInfo.setDistributionName("中标麒麟 " + versionId);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("读取麒麟系统特定文件时出错: {}", e.getMessage());
        }
    }

    /**
     * 检查Ubuntu系统
     */
    private boolean checkUbuntuFile(OsInfo osInfo, ClientSession session) {
        try {
            // 检查lsb-release文件
            String lsbRelease = MinaUtils.execCmdWithResult(session, "cat /etc/lsb-release 2>/dev/null");
            if (StringUtils.isNotBlank(lsbRelease) && lsbRelease.toLowerCase().contains("ubuntu")) {
                logger.info("发现lsb-release文件，确认为Ubuntu系统");

                // 提取DISTRIB_ID
                Pattern idPattern = Pattern.compile("^DISTRIB_ID=(.*?)$", Pattern.MULTILINE);
                Matcher idMatcher = idPattern.matcher(lsbRelease);
                if (idMatcher.find()) {
                    String distribId = idMatcher.group(1).trim().replace("\"", "").toLowerCase();
                    osInfo.setDistributionId(distribId);
                } else {
                    // 如果没有找到DISTRIB_ID，手动设置
                    osInfo.setDistributionId("ubuntu");
                }

                // 设置发行版名称
                osInfo.setDistribution("Ubuntu");
                osInfo.setDistributionType(LinuxDistribution.UBUNTU);

                // 提取DISTRIB_RELEASE (版本号)
                Pattern releasePattern = Pattern.compile("^DISTRIB_RELEASE=(.*?)$", Pattern.MULTILINE);
                Matcher releaseMatcher = releasePattern.matcher(lsbRelease);
                if (releaseMatcher.find()) {
                    String version = releaseMatcher.group(1).trim().replace("\"", "");
                    osInfo.setVersionId(version);
                    osInfo.setVersion(version);

                    // 设置Ubuntu特定版本标志
                    if (version.startsWith("22")) {
                        osInfo.setUbuntu22(true);
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu 22.04 LTS");
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu 22.04 LTS (Jammy Jellyfish)");
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    } else if (version.startsWith("24")) {
                        osInfo.setUbuntu24(true);
                        // 判断具体的24版本
                        if (version.contains("24.04")) {
                            // 为悬浮卡片设置详细版本信息
                            osInfo.setDistributionName("Ubuntu 24.04 LTS");
                            // 设置完整名称用于悬浮卡片
                            osInfo.setFullName("Ubuntu 24.04 LTS (Noble Numbat)");
                        } else if (version.contains("24.10")) {
                            // 为悬浮卡片设置详细版本信息
                            osInfo.setDistributionName("Ubuntu 24.10");
                            // 设置完整名称用于悬浮卡片
                            osInfo.setFullName("Ubuntu 24.10 (Oracular Oriole)");
                        } else {
                            // 默认24版本处理
                            osInfo.setDistributionName("Ubuntu 24.04 LTS");
                            osInfo.setFullName("Ubuntu 24.04 LTS (Noble Numbat)");
                        }
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    } else if (version.startsWith("20")) {
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu 20.04 LTS");
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu 20.04 LTS (Focal Fossa)");
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    } else if (version.startsWith("18")) {
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu 18.04 LTS");
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu 18.04 LTS (Bionic Beaver)");
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    } else if (version.startsWith("16")) {
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu 16.04 LTS");
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu 16.04 LTS (Xenial Xerus)");
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    } else {
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu " + version);
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu " + version);
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    }
                } else {
                    // 如果没有版本信息，设置默认值
                    osInfo.setVersionId("unknown");
                    osInfo.setVersion("unknown");
                    osInfo.setDistributionName("Ubuntu");
                    osInfo.setFullName("Ubuntu");
                    osInfo.setDisplayName("Ubuntu");
                }

                // 提取DISTRIB_DESCRIPTION (完整名称)
                Pattern descPattern = Pattern.compile("^DISTRIB_DESCRIPTION=\"?(.*?)\"?$", Pattern.MULTILINE);
                Matcher descMatcher = descPattern.matcher(lsbRelease);
                if (descMatcher.find()) {
                    String description = descMatcher.group(1).trim();
                    // 只有在当前fullName不包含代号时才使用DISTRIB_DESCRIPTION
                    if (StringUtils.isBlank(osInfo.getFullName()) ||
                            !osInfo.getFullName().contains("(") && description.contains("(")) {
                        osInfo.setFullName(description);
                    }
                }

                return true;
            }

            // 如果lsb-release不存在或不包含Ubuntu，尝试其他方法
            String osReleaseId = MinaUtils.execCmdWithResult(session, "grep -i ubuntu /etc/os-release 2>/dev/null");
            if (StringUtils.isNotBlank(osReleaseId) && osReleaseId.toLowerCase().contains("ubuntu")) {
                logger.info("通过/etc/os-release确认为Ubuntu系统");
                osInfo.setDistributionId("ubuntu");
                osInfo.setDistribution("Ubuntu");
                osInfo.setDistributionType(LinuxDistribution.UBUNTU);

                // 尝试获取版本号
                String versionCmd = "grep VERSION_ID /etc/os-release | cut -d '=' -f2 | tr -d '\"'";
                String version = MinaUtils.execCmdWithResult(session, versionCmd);
                if (StringUtils.isNotBlank(version)) {
                    version = version.trim();
                    osInfo.setVersionId(version);
                    osInfo.setVersion(version);

                    // 设置版本标志和显示名称
                    if (version.startsWith("22")) {
                        osInfo.setUbuntu22(true);
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu 22.04 LTS");
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu 22.04 LTS (Jammy Jellyfish)");
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    } else if (version.startsWith("24")) {
                        osInfo.setUbuntu24(true);
                        // 判断具体的24版本
                        if (version.contains("24.04")) {
                            // 为悬浮卡片设置详细版本信息
                            osInfo.setDistributionName("Ubuntu 24.04 LTS");
                            // 设置完整名称用于悬浮卡片
                            osInfo.setFullName("Ubuntu 24.04 LTS (Noble Numbat)");
                        } else if (version.contains("24.10")) {
                            // 为悬浮卡片设置详细版本信息
                            osInfo.setDistributionName("Ubuntu 24.10");
                            // 设置完整名称用于悬浮卡片
                            osInfo.setFullName("Ubuntu 24.10 (Oracular Oriole)");
                        } else {
                            // 默认24版本处理
                            osInfo.setDistributionName("Ubuntu 24.04 LTS");
                            osInfo.setFullName("Ubuntu 24.04 LTS (Noble Numbat)");
                        }
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    } else if (version.startsWith("20")) {
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu 20.04 LTS");
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu 20.04 LTS (Focal Fossa)");
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    } else if (version.startsWith("18")) {
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu 18.04 LTS");
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu 18.04 LTS (Bionic Beaver)");
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    } else if (version.startsWith("16")) {
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu 16.04 LTS");
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu 16.04 LTS (Xenial Xerus)");
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    } else {
                        // 为悬浮卡片设置详细版本信息
                        osInfo.setDistributionName("Ubuntu " + version);
                        // 设置完整名称用于悬浮卡片
                        osInfo.setFullName("Ubuntu " + version);
                        // 为列表显示设置简单名称
                        osInfo.setDisplayName("Ubuntu");
                    }
                } else {
                    osInfo.setDistributionName("Ubuntu");
                    osInfo.setFullName("Ubuntu");
                    osInfo.setDisplayName("Ubuntu");
                }

                // 尝试从os-release获取PRETTY_NAME
                String prettyCmd = "grep PRETTY_NAME /etc/os-release | cut -d '=' -f2 | tr -d '\"'";
                String prettyName = MinaUtils.execCmdWithResult(session, prettyCmd);
                if (StringUtils.isNotBlank(prettyName)) {
                    prettyName = prettyName.trim();
                    // 只有在当前fullName不包含代号时才使用PRETTY_NAME
                    if (StringUtils.isBlank(osInfo.getFullName()) ||
                            !osInfo.getFullName().contains("(") && prettyName.contains("(")) {
                        osInfo.setFullName(prettyName);
                    }
                }

                return true;
            }

            return false;
        } catch (Exception e) {
            logger.warn("检查Ubuntu系统时出错: {}", e.getMessage());
            return false;
        }
    }
}