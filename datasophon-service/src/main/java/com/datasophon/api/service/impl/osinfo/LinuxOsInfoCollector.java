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

            // 读取/etc/os-release文件获取发行版信息
            String osRelease = MinaUtils.execCmdWithResult(session, "cat /etc/os-release 2>/dev/null");
            if (StringUtils.isNotBlank(osRelease)) {
                parseDistributionInfo(osInfo, osRelease);
            } else {
                // 如果没有/etc/os-release文件，尝试其他方法
                tryAlternativeFiles(osInfo, session);
            }

            // 额外检查：如果distributionName和distributionId相同且都是数字，可能是Ubuntu版本误识别
            if (isNumericVersion(osInfo.getDistributionId()) && isNumericVersion(osInfo.getDistributionName())) {
                // 检查是否是Ubuntu版本格式（年.月，如22.04, 24.10）
                if (osInfo.getDistributionId().contains(".")) {
                    try {
                        String[] parts = osInfo.getDistributionId().split("\\.");
                        int major = Integer.parseInt(parts[0]);

                        // Ubuntu版本号通常是年份格式（从10开始），且小于30
                        if (major >= 10 && major < 30) {
                            osInfo.setDistributionId("ubuntu");
                            osInfo.setDistribution("Ubuntu");
                            osInfo.setDistributionName("Ubuntu");
                            // 保持原有的version信息
                            logger.info("检测到Ubuntu版本格式，已更正分布为Ubuntu {}", osInfo.getVersionId());

                            // 设置显示名称和主版本号
                            osInfo.setDisplayName("Ubuntu " + osInfo.getVersionId());
                            osInfo.setMajorVersion(parts[0]);
                        }
                    } catch (Exception e) {
                        logger.warn("解析可能的Ubuntu版本时出错", e);
                    }
                }
                // 检查Debian版本格式（纯数字，如11, 12）
                else {
                    try {
                        int version = Integer.parseInt(osInfo.getDistributionId());
                        // Debian当前版本在7-15之间
                        if (version >= 7 && version <= 15) {
                            osInfo.setDistributionId("debian");
                            osInfo.setDistribution("Debian");
                            osInfo.setDistributionName("Debian");
                            logger.info("检测到Debian版本格式，已更正分布为Debian {}", osInfo.getVersionId());

                            // 设置显示名称和主版本号
                            osInfo.setDisplayName("Debian GNU/Linux " + osInfo.getVersionId());
                            osInfo.setMajorVersion(String.valueOf(version));
                        }
                    } catch (Exception e) {
                        logger.warn("解析可能的Debian版本时出错", e);
                    }
                }
            }

            // 获取内核版本
            String kernelVersion = MinaUtils.execCmdWithResult(session, "uname -r");
            if (StringUtils.isNotBlank(kernelVersion)) {
                osInfo.setKernelVersion(kernelVersion.trim());
                logger.info("获取到内核版本: {}", kernelVersion.trim());
            }

            // 获取CPU架构
            String architecture = MinaUtils.execCmdWithResult(session, "uname -m");
            if (StringUtils.isNotBlank(architecture)) {
                osInfo.setArchitecture(architecture.trim());
                logger.info("获取到CPU架构: {}", architecture.trim());
            }

            // 获取负载信息
            String loadCmd = "cat /proc/loadavg";
            String loadInfo = MinaUtils.execCmdWithResult(session, loadCmd);
            if (StringUtils.isNotBlank(loadInfo)) {
                String[] parts = loadInfo.trim().split("\\s+");
                if (parts.length >= 3) {
                    try {
                        // 确保存在CPU信息对象
                        if (osInfo.getCpuInfo() == null) {
                            osInfo.setCpuInfo(new CpuInfo());
                        }
                        osInfo.getCpuInfo().setLoad1Min(Double.parseDouble(parts[0]));
                        osInfo.getCpuInfo().setLoad5Min(Double.parseDouble(parts[1]));
                        osInfo.getCpuInfo().setLoad15Min(Double.parseDouble(parts[2]));
                        logger.info("获取到负载信息: 1分钟={}, 5分钟={}, 15分钟={}",
                                osInfo.getCpuInfo().getLoad1Min(), osInfo.getCpuInfo().getLoad5Min(),
                                osInfo.getCpuInfo().getLoad15Min());
                    } catch (NumberFormatException e) {
                        logger.warn("解析负载信息失败: {}", e.getMessage());
                    }
                }
            }

            // 检查是否需要补充分发版名称
            if (StringUtils.isBlank(osInfo.getDistributionName())
                    && StringUtils.isNotBlank(osInfo.getDistributionId())) {
                // 根据ID设置Name
                String distId = osInfo.getDistributionId().toLowerCase();
                if (distId.contains("ubuntu")) {
                    osInfo.setDistributionName("Ubuntu");
                } else if (distId.contains("centos")) {
                    osInfo.setDistributionName("CentOS");
                } else if (distId.contains("debian")) {
                    osInfo.setDistributionName("Debian");
                } else if (distId.contains("fedora")) {
                    osInfo.setDistributionName("Fedora");
                } else if (distId.contains("rhel") || distId.contains("redhat")) {
                    osInfo.setDistributionName("Red Hat Enterprise Linux");
                } else if (distId.contains("suse")) {
                    osInfo.setDistributionName("SUSE Linux");
                } else if (distId.contains("kylin")) {
                    osInfo.setDistributionName("Kylin Linux");
                } else {
                    // 如果无法匹配已知发行版，将ID首字母大写作为名称
                    osInfo.setDistributionName(osInfo.getDistributionId().substring(0, 1).toUpperCase()
                            + osInfo.getDistributionId().substring(1));
                }
                logger.info("根据distributionId设置distributionName: {}", osInfo.getDistributionName());
            }

            // 确保设置了显示名称
            if (StringUtils.isBlank(osInfo.getDisplayName())) {
                // 优先使用fullName
                if (StringUtils.isNotBlank(osInfo.getFullName())) {
                    osInfo.setDisplayName(osInfo.getFullName());
                }
                // 其次使用distributionName + version
                else if (StringUtils.isNotBlank(osInfo.getDistributionName())) {
                    if (StringUtils.isNotBlank(osInfo.getVersionId())) {
                        osInfo.setDisplayName(osInfo.getDistributionName() + " " + osInfo.getVersionId());
                    } else {
                        osInfo.setDisplayName(osInfo.getDistributionName());
                    }
                }
            }

            // 标记OS信息为有效
            osInfo.setValid(true);

            // 设置操作系统信息收集成功
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);

            return osInfo;
        } catch (Exception e) {
            logger.error("收集Linux系统信息时出错: {}", e.getMessage(), e);
            osInfo.setValid(false);

            // 设置错误状态
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);

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
     * 修改处理系统分发名称的方法
     */
    private void parseDistributionInfo(OsInfo osInfo, String content) {
        if (StringUtils.isBlank(content)) {
            return;
        }

        try {
            Map<String, String> osReleaseData = new HashMap<>();

            // 按行解析os-release文件
            String[] lines = content.split("\n");
            for (String line : lines) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim().replaceAll("^\"|\"$", ""); // 移除引号
                    osReleaseData.put(key, value);
                }
            }

            // 获取ID (例如: ID=centos)
            String id = osReleaseData.get("ID");
            if (StringUtils.isNotBlank(id)) {
                id = id.toLowerCase();
                osInfo.setDistribution(id);

                // 使用枚举设置发行版类型
                osInfo.setDistributionType(LinuxDistribution.fromId(id));

                // 获取版本ID (例如: VERSION_ID="7", VERSION_ID="20.04")
                String versionId = osReleaseData.get("VERSION_ID");
                if (StringUtils.isNotBlank(versionId)) {
                    osInfo.setVersionId(versionId);
                    osInfo.setVersion(versionId);

                    // 设置主版本号（提取版本号的第一部分）
                    String majorVersion = versionId;
                    if (versionId.contains(".")) {
                        majorVersion = versionId.split("\\.")[0];
                    }
                    osInfo.setMajorVersion(majorVersion);
                }

                // 获取显示名称（PRETTY_NAME字段）
                String prettyName = osReleaseData.get("PRETTY_NAME");
                if (StringUtils.isNotBlank(prettyName)) {
                    osInfo.setDisplayName(prettyName);
                    osInfo.setFullName(prettyName);
                } else {
                    // 没有PRETTY_NAME，使用NAME和VERSION
                    String name = osReleaseData.get("NAME");
                    String version = osReleaseData.get("VERSION");

                    if (StringUtils.isNotBlank(name)) {
                        if (StringUtils.isNotBlank(version)) {
                            osInfo.setDisplayName(name + " " + version);
                            osInfo.setFullName(name + " " + version);
                        } else {
                            osInfo.setDisplayName(name);
                            osInfo.setFullName(name);
                        }
                    }
                }

                // 设置分发ID
                osInfo.setDistributionId(id);

                logger.debug("从/etc/os-release解析到发行版: {}，版本: {}，显示名称: {}",
                        osInfo.getDistribution(), osInfo.getVersionId(), osInfo.getDisplayName());
            }
        } catch (Exception e) {
            logger.error("解析/etc/os-release文件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 收集DNS服务器信息
     */
    public void collectDnsInfo(OsInfo osInfo, ClientSession session) {
        try {
            // 读取/etc/resolv.conf获取DNS服务器
            String dnsInfoCmd = "cat /etc/resolv.conf | grep nameserver | awk '{print $2}'";
            String dnsInfo = MinaUtils.execCmdWithResult(session, dnsInfoCmd);

            if (StringUtils.isNotBlank(dnsInfo)) {
                // 转换为列表
                List<String> dnsServers = new ArrayList<>();
                String[] servers = dnsInfo.split("\\s+");
                for (String server : servers) {
                    if (StringUtils.isNotBlank(server)) {
                        dnsServers.add(server.trim());
                    }
                }

                osInfo.setDnsServers(dnsServers);
                logger.info("获取到DNS服务器信息: {}", dnsServers);
            }
        } catch (Exception e) {
            logger.error("收集DNS服务器信息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 尝试从其他文件中获取发行版信息
     */
    private void tryAlternativeFiles(OsInfo osInfo, ClientSession session) {
        try {
            // 1. 尝试从/etc/redhat-release获取信息（适用于RHEL/CentOS）
            String redhatRelease = MinaUtils.execCmdWithResult(session, "cat /etc/redhat-release 2>/dev/null");
            if (StringUtils.isNotBlank(redhatRelease)) {
                redhatRelease = redhatRelease.trim();
                osInfo.setDistribution(redhatRelease);

                LinuxDistribution distType = LinuxDistribution.OTHER;
                String distId = redhatRelease.toLowerCase();

                // 使用contains检查发行版类型
                if (distId.contains("centos")) {
                    osInfo.setDistributionId("centos");
                    osInfo.setDistributionName("CentOS");

                    // 检查CentOS版本
                    if (distId.contains("7.")) {
                        distType = LinuxDistribution.CENTOS7;
                    } else if (distId.contains("8.")) {
                        distType = LinuxDistribution.CENTOS8;
                    } else {
                        distType = LinuxDistribution.CENTOS;
                    }
                } else if (distId.contains("red hat")) {
                    osInfo.setDistributionId("rhel");
                    osInfo.setDistributionName("Red Hat Enterprise Linux");
                    distType = LinuxDistribution.REDHAT;
                }

                osInfo.setDistributionType(distType);

                // 提取版本号
                Pattern pattern = Pattern.compile("release\\s+([\\d\\.]+)");
                Matcher matcher = pattern.matcher(redhatRelease);
                if (matcher.find()) {
                    String version = matcher.group(1);
                    osInfo.setVersionId(version);
                    osInfo.setVersion(version);

                    // 设置主版本号（提取版本号的第一部分）
                    if (version.contains(".")) {
                        osInfo.setMajorVersion(version.split("\\.")[0]);
                    } else {
                        osInfo.setMajorVersion(version);
                    }
                }

                osInfo.setFullName(redhatRelease);
                return;
            }

            // 2. 尝试从/etc/debian_version获取信息（适用于Debian/Ubuntu）
            String debianVersion = MinaUtils.execCmdWithResult(session, "cat /etc/debian_version 2>/dev/null");
            if (StringUtils.isNotBlank(debianVersion)) {
                debianVersion = debianVersion.trim();

                // 检查是否为Ubuntu（Ubuntu同时也有/etc/debian_version文件）
                String lsbRelease = MinaUtils.execCmdWithResult(session, "lsb_release -a 2>/dev/null");
                if (StringUtils.isNotBlank(lsbRelease) && lsbRelease.toLowerCase().contains("ubuntu")) {
                    osInfo.setDistributionId("ubuntu");
                    osInfo.setDistribution("Ubuntu");
                    osInfo.setDistributionName("Ubuntu");

                    LinuxDistribution distType = LinuxDistribution.UBUNTU;

                    // 从lsb_release提取Ubuntu版本
                    Pattern pattern = Pattern.compile("Release:\\s+([\\d\\.]+)");
                    Matcher matcher = pattern.matcher(lsbRelease);
                    if (matcher.find()) {
                        String version = matcher.group(1);
                        osInfo.setVersionId(version);
                        osInfo.setVersion(version);

                        // 设置主版本号（Ubuntu使用年份作为主版本号，例如20.04的主版本是20）
                        if (version.contains(".")) {
                            String majorVersion = version.split("\\.")[0];
                            osInfo.setMajorVersion(majorVersion);

                            // 判断Ubuntu具体版本
                            if (version.startsWith("22.")) {
                                distType = LinuxDistribution.UBUNTU22;
                            } else if (version.startsWith("24.")) {
                                distType = LinuxDistribution.UBUNTU24;
                            }
                        } else {
                            osInfo.setMajorVersion(version);
                        }
                    }

                    osInfo.setDistributionType(distType);

                    // 提取完整名称
                    Pattern descPattern = Pattern.compile("Description:\\s+(.+)");
                    Matcher descMatcher = descPattern.matcher(lsbRelease);
                    if (descMatcher.find()) {
                        String fullName = descMatcher.group(1).trim();
                        osInfo.setFullName(fullName);
                        osInfo.setDisplayName(fullName);
                    } else {
                        String displayName = "Ubuntu " + osInfo.getVersionId();
                        osInfo.setFullName(displayName);
                        osInfo.setDisplayName(displayName);
                    }
                } else {
                    // 是纯Debian系统
                    osInfo.setDistributionId("debian");
                    osInfo.setDistribution("Debian");
                    osInfo.setDistributionName("Debian");
                    osInfo.setVersionId(debianVersion);
                    osInfo.setVersion(debianVersion);
                    osInfo.setDistributionType(LinuxDistribution.DEBIAN);

                    // 对于Debian，版本号通常是数字，直接设置主版本号
                    if (debianVersion.contains(".")) {
                        osInfo.setMajorVersion(debianVersion.split("\\.")[0]);
                    } else {
                        osInfo.setMajorVersion(debianVersion);
                    }

                    String displayName = "Debian GNU/Linux " + debianVersion;
                    osInfo.setFullName(displayName);
                    osInfo.setDisplayName(displayName);
                }
                return;
            }

            // 3. 尝试使用lsb_release命令（大多数现代Linux发行版支持）
            String lsbRelease = MinaUtils.execCmdWithResult(session, "lsb_release -a 2>/dev/null");
            if (StringUtils.isNotBlank(lsbRelease)) {
                // 提取发行版ID
                Pattern idPattern = Pattern.compile("Distributor ID:\\s+(.+)");
                Matcher idMatcher = idPattern.matcher(lsbRelease);
                if (idMatcher.find()) {
                    String id = idMatcher.group(1).trim().toLowerCase();
                    osInfo.setDistributionId(id);

                    // 使用枚举设置发行版类型
                    osInfo.setDistributionType(LinuxDistribution.fromId(id));

                    // 规范化分发版名称（首字母大写）
                    String distributionName = id.substring(0, 1).toUpperCase() + id.substring(1);
                    osInfo.setDistribution(distributionName);
                    osInfo.setDistributionName(distributionName);
                }

                // 提取版本
                Pattern versionPattern = Pattern.compile("Release:\\s+(.+)");
                Matcher versionMatcher = versionPattern.matcher(lsbRelease);
                if (versionMatcher.find()) {
                    String version = versionMatcher.group(1).trim();
                    osInfo.setVersionId(version);
                    osInfo.setVersion(version);

                    // 设置主版本号
                    if (version.contains(".")) {
                        osInfo.setMajorVersion(version.split("\\.")[0]);
                    } else {
                        osInfo.setMajorVersion(version);
                    }
                }

                // 提取完整名称
                Pattern descPattern = Pattern.compile("Description:\\s+(.+)");
                Matcher descMatcher = descPattern.matcher(lsbRelease);
                if (descMatcher.find()) {
                    String fullName = descMatcher.group(1).trim();
                    osInfo.setFullName(fullName);
                    osInfo.setDisplayName(fullName);
                }
                return;
            }

            // 4. 检查一些常见的发行版特定文件
            Map<String, LinuxDistribution> distroFileMap = new HashMap<>();
            distroFileMap.put("cat /etc/SuSE-release 2>/dev/null", LinuxDistribution.SUSE);
            distroFileMap.put("cat /etc/arch-release 2>/dev/null", LinuxDistribution.OTHER); // Arch没有对应的枚举
            distroFileMap.put("cat /etc/gentoo-release 2>/dev/null", LinuxDistribution.OTHER); // Gentoo没有对应的枚举
            distroFileMap.put("cat /etc/slackware-version 2>/dev/null", LinuxDistribution.OTHER); // Slackware没有对应的枚举
            distroFileMap.put("cat /etc/alpine-release 2>/dev/null", LinuxDistribution.OTHER); // Alpine没有对应的枚举
            distroFileMap.put("cat /etc/kylin-release 2>/dev/null", LinuxDistribution.KYLIN);

            for (Map.Entry<String, LinuxDistribution> entry : distroFileMap.entrySet()) {
                String cmd = entry.getKey();
                LinuxDistribution distType = entry.getValue();

                String result = MinaUtils.execCmdWithResult(session, cmd);
                if (StringUtils.isNotBlank(result)) {
                    result = result.trim();

                    // 使用switch替代if-else链
                    String distroKey = "";
                    if (cmd.contains("SuSE"))
                        distroKey = "suse";
                    else if (cmd.contains("arch"))
                        distroKey = "arch";
                    else if (cmd.contains("gentoo"))
                        distroKey = "gentoo";
                    else if (cmd.contains("slackware"))
                        distroKey = "slackware";
                    else if (cmd.contains("alpine"))
                        distroKey = "alpine";
                    else if (cmd.contains("kylin"))
                        distroKey = "kylin";

                    switch (distroKey) {
                        case "suse":
                            osInfo.setDistributionId("suse");
                            osInfo.setDistribution("SUSE");
                            osInfo.setDistributionName("SUSE");
                            osInfo.setDistributionType(LinuxDistribution.SUSE);
                            break;
                        case "arch":
                            osInfo.setDistributionId("arch");
                            osInfo.setDistribution("Arch");
                            osInfo.setDistributionName("Arch Linux");
                            osInfo.setDistributionType(LinuxDistribution.OTHER);
                            break;
                        case "gentoo":
                            osInfo.setDistributionId("gentoo");
                            osInfo.setDistribution("Gentoo");
                            osInfo.setDistributionName("Gentoo Linux");
                            osInfo.setDistributionType(LinuxDistribution.OTHER);
                            break;
                        case "slackware":
                            osInfo.setDistributionId("slackware");
                            osInfo.setDistribution("Slackware");
                            osInfo.setDistributionName("Slackware Linux");
                            osInfo.setDistributionType(LinuxDistribution.OTHER);
                            break;
                        case "alpine":
                            osInfo.setDistributionId("alpine");
                            osInfo.setDistribution("Alpine");
                            osInfo.setDistributionName("Alpine Linux");
                            osInfo.setDistributionType(LinuxDistribution.OTHER);
                            break;
                        case "kylin":
                            osInfo.setDistributionId("kylin");
                            osInfo.setDistribution("Kylin");
                            osInfo.setDistributionName("Kylin");
                            osInfo.setDistributionType(LinuxDistribution.KYLIN);

                            // 尝试提取Kylin版本号
                            Pattern pattern = Pattern.compile("V([\\d\\.]+)");
                            Matcher matcher = pattern.matcher(result);
                            if (matcher.find()) {
                                String version = matcher.group(1);
                                osInfo.setVersionId(version);
                                osInfo.setVersion(version);
                                osInfo.setMajorVersion(version);

                                // 判断具体版本
                                if (version.startsWith("4")) {
                                    osInfo.setDistributionType(LinuxDistribution.KYLIN_V4);
                                } else if (version.startsWith("10")) {
                                    osInfo.setDistributionType(LinuxDistribution.KYLIN_V10);
                                }

                                osInfo.setFullName(result);
                            } else {
                                osInfo.setFullName(result);
                            }
                            break;
                    }

                    osInfo.setDisplayName(result);

                    // 从版本中提取主版本号（如果尚未设置）
                    if (StringUtils.isBlank(osInfo.getMajorVersion())
                            && StringUtils.isNotBlank(osInfo.getVersionId())) {
                        String version = osInfo.getVersionId();
                        if (version.contains(".")) {
                            osInfo.setMajorVersion(version.split("\\.")[0]);
                        } else {
                            osInfo.setMajorVersion(version);
                        }
                    }

                    return;
                }
            }

            // 5. 最后尝试uname命令，提供基本信息
            String osType = MinaUtils.execCmdWithResult(session, "uname -s");
            if (StringUtils.isNotBlank(osType)) {
                osType = osType.trim();
                boolean isLinux = "Linux".equalsIgnoreCase(osType);

                osInfo.setDistributionId(isLinux ? "linux" : osType.toLowerCase());
                osInfo.setDistribution(isLinux ? "Linux" : osType);
                osInfo.setDistributionName(isLinux ? "Linux" : osType);
                osInfo.setFullName(isLinux ? "Linux" : osType);
                osInfo.setDisplayName(isLinux ? "Linux" : osType);
                osInfo.setDistributionType(LinuxDistribution.OTHER);
            }
        } catch (Exception e) {
            logger.error("尝试从替代文件获取系统信息时出错", e);
        }
    }

    /**
     * 检查字符串是否为数字版本格式
     */
    private boolean isNumericVersion(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        // 检查是否为数字或包含点的数字（如22.04）
        return str.matches("\\d+(\\.\\d+)?");
    }

    private String extractFromLsb(String content, String prefix) {
        if (content == null || prefix == null) {
            return "";
        }

        for (String line : content.split("\n")) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }

        return "";
    }

    private String extractValue(String content, String key) {
        if (content == null || key == null) {
            return "";
        }

        Pattern pattern = Pattern.compile(key + "=([\"']?)([^\"'\\s]*)\\1");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(2);
        }

        return "";
    }

    /**
     * 收集CPU信息
     * 单独提取方法以便队列系统调用
     * 
     * @param osInfo  操作系统信息对象
     * @param session SSH会话
     */
    public void collectCpuInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集CPU信息");

        try {
            // 确保CPU信息对象存在
            if (osInfo.getCpuInfo() == null) {
                osInfo.setCpuInfo(new CpuInfo());
            }
            CpuInfo cpuInfo = osInfo.getCpuInfo();

            // 使用lscpu命令获取详细CPU信息
            String lscpuOutput = MinaUtils.execCmdWithResult(session, "lscpu");
            if (StringUtils.isNotBlank(lscpuOutput)) {
                // 解析CPU型号
                Pattern modelNamePattern = Pattern.compile("Model name:\\s+(.+)");
                Matcher modelNameMatcher = modelNamePattern.matcher(lscpuOutput);
                if (modelNameMatcher.find()) {
                    String cpuModel = modelNameMatcher.group(1).trim();
                    cpuInfo.setModel(cpuModel);
                    logger.debug("获取到CPU型号: {}", cpuModel);
                }

                // 解析CPU频率
                Pattern cpuFreqPattern = Pattern.compile("CPU MHz:\\s+(\\d+\\.?\\d*)");
                Matcher cpuFreqMatcher = cpuFreqPattern.matcher(lscpuOutput);
                if (cpuFreqMatcher.find()) {
                    try {
                        double freqMHz = Double.parseDouble(cpuFreqMatcher.group(1).trim());
                        // 转换MHz为GHz
                        double freqGHz = Math.round(freqMHz / 1000 * 100) / 100.0;
                        cpuInfo.setFrequency(freqGHz);
                        logger.debug("获取到CPU频率: {} GHz", freqGHz);
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU频率失败: {}", e.getMessage());
                    }
                }

                // 解析CPU数量
                Pattern cpuCountPattern = Pattern.compile("Socket\\(s\\):\\s+(\\d+)");
                Matcher cpuCountMatcher = cpuCountPattern.matcher(lscpuOutput);
                if (cpuCountMatcher.find()) {
                    try {
                        int cpuCount = Integer.parseInt(cpuCountMatcher.group(1).trim());
                        cpuInfo.setPhysicalCount(cpuCount);
                        logger.debug("获取到CPU物理数量: {}", cpuCount);
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU数量失败: {}", e.getMessage());
                    }
                }

                // 解析每CPU的核心数
                Pattern coresPerCpuPattern = Pattern.compile("Core\\(s\\) per socket:\\s+(\\d+)");
                Matcher coresPerCpuMatcher = coresPerCpuPattern.matcher(lscpuOutput);
                if (coresPerCpuMatcher.find()) {
                    try {
                        int coresPerCpu = Integer.parseInt(coresPerCpuMatcher.group(1).trim());
                        // 计算物理核心数 = CPU数量 * 每CPU的核心数
                        if (cpuInfo.getPhysicalCount() != null) {
                            cpuInfo.setCores(coresPerCpu * cpuInfo.getPhysicalCount());
                            logger.debug("计算得到物理CPU核心数: {}", cpuInfo.getCores());
                        } else {
                            cpuInfo.setCores(coresPerCpu);
                            logger.debug("获取到每CPU核心数: {}", coresPerCpu);
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("解析每CPU核心数失败: {}", e.getMessage());
                    }
                }

                // 解析每核心的线程数
                Pattern threadsPerCorePattern = Pattern.compile("Thread\\(s\\) per core:\\s+(\\d+)");
                Matcher threadsPerCoreMatcher = threadsPerCorePattern.matcher(lscpuOutput);
                if (threadsPerCoreMatcher.find()) {
                    try {
                        int threadsPerCore = Integer.parseInt(threadsPerCoreMatcher.group(1).trim());
                        cpuInfo.setThreadsPerCore(threadsPerCore);
                        logger.debug("获取到每核心线程数: {}", threadsPerCore);
                    } catch (NumberFormatException e) {
                        logger.warn("解析每核心线程数失败: {}", e.getMessage());
                    }
                }

                // 解析CPU总核心数
                Pattern cpuCoresPattern = Pattern.compile("CPU\\(s\\):\\s+(\\d+)");
                Matcher cpuCoresMatcher = cpuCoresPattern.matcher(lscpuOutput);
                if (cpuCoresMatcher.find()) {
                    try {
                        int logicalCores = Integer.parseInt(cpuCoresMatcher.group(1).trim());
                        cpuInfo.setLogicalCores(logicalCores);
                        logger.debug("获取到CPU逻辑核心数: {}", logicalCores);

                        // 如果没有设置物理核心数，使用逻辑核心数
                        if (cpuInfo.getCores() == null) {
                            cpuInfo.setCores(logicalCores);
                            logger.debug("无法计算物理CPU核心数，使用逻辑核心数: {}", logicalCores);
                        }

                        // 计算每核心线程数
                        if (cpuInfo.getCores() > 0 && cpuInfo.getThreadsPerCore() == null) {
                            cpuInfo.setThreadsPerCore(logicalCores / cpuInfo.getCores());
                            logger.debug("计算得到每核心线程数: {}", cpuInfo.getThreadsPerCore());
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU核心数失败: {}", e.getMessage());
                    }
                }
            }

            // 读取/proc/cpuinfo获取完整CPU信息
            String cpuinfoOutput = MinaUtils.execCmdWithResult(session,
                    "cat /proc/cpuinfo | grep -E 'processor|model name|cpu MHz'");
            if (StringUtils.isNotBlank(cpuinfoOutput)) {
                cpuInfo.setRawInfo(cpuinfoOutput);
                logger.debug("获取到完整CPU信息");
            }

            // 获取负载信息
            String loadAvgOutput = MinaUtils.execCmdWithResult(session, "cat /proc/loadavg");
            if (StringUtils.isNotBlank(loadAvgOutput)) {
                String[] loadParts = loadAvgOutput.trim().split("\\s+");
                if (loadParts.length >= 3) {
                    try {
                        cpuInfo.setLoad1Min(Double.parseDouble(loadParts[0]));
                        cpuInfo.setLoad5Min(Double.parseDouble(loadParts[1]));
                        cpuInfo.setLoad15Min(Double.parseDouble(loadParts[2]));
                        logger.debug("获取到系统负载: 1分钟={}, 5分钟={}, 15分钟={}",
                                cpuInfo.getLoad1Min(), cpuInfo.getLoad5Min(), cpuInfo.getLoad15Min());
                    } catch (NumberFormatException e) {
                        logger.warn("解析系统负载失败: {}", e.getMessage());
                    }
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("cpuInfo");
            cpuInfo.setStatus(OsInfoStatusEnum.SUCCESS);

            logger.info("CPU信息收集完成");
        } catch (Exception e) {
            logger.error("收集CPU信息时出错: {}", e.getMessage(), e);
            if (osInfo.getCpuInfo() != null) {
                osInfo.getCpuInfo().setStatus(OsInfoStatusEnum.ERROR);
                osInfo.getCpuInfo().setErrorMessage("收集CPU信息失败: " + e.getMessage());
            }
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
                }
            } else {
                // 命令执行失败或无输出
                logger.warn("获取交换分区信息命令无返回结果");
                swapInfo.setTotalSwap(0L);
                swapInfo.setAvailableSwap(0L);
                swapInfo.setEnabled(false);
                swapInfo.setErrorMessage("无法获取交换分区信息");
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
}