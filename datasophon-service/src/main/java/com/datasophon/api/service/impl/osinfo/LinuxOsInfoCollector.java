package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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

            // 获取主机名
            String hostname = MinaUtils.execCmdWithResult(session, "hostname");
            if (StringUtils.isNotBlank(hostname)) {
                hostname = hostname.trim();
                osInfo.setHostname(hostname);
                hostInfo.setHostname(hostname);
                logger.info("获取到主机名: {}", hostname);
                // 立即更新缓存，使前端能看到主机名
                cacheUpdater.updateCache(hostInfo);
            }

            // 获取完全限定域名(FQDN)
            String fqdn = MinaUtils.execCmdWithResult(session, "hostname -f");
            if (StringUtils.isNotBlank(fqdn)) {
                fqdn = fqdn.trim();
                osInfo.setFqdn(fqdn);
                logger.info("获取到FQDN: {}", fqdn);
                // 立即更新缓存，使前端能看到FQDN
                cacheUpdater.updateCache(hostInfo);
            }

            // 读取/etc/os-release文件获取发行版信息
            String osRelease = MinaUtils.execCmdWithResult(session, "cat /etc/os-release 2>/dev/null");
            if (StringUtils.isNotBlank(osRelease)) {
                parseOsRelease(osInfo, osRelease);
                // 更新发行版信息
                cacheUpdater.updateCache(hostInfo);
            } else {
                // 如果没有/etc/os-release文件，尝试其他方法
                tryAlternativeFiles(osInfo, session);
                // 更新发行版信息
                cacheUpdater.updateCache(hostInfo);
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
                            // 更新Ubuntu信息
                            cacheUpdater.updateCache(hostInfo);
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
                            // 更新Debian信息
                            cacheUpdater.updateCache(hostInfo);
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
                // 更新内核版本信息
                cacheUpdater.updateCache(hostInfo);
            }

            // 获取CPU架构
            String architecture = MinaUtils.execCmdWithResult(session, "uname -m");
            if (StringUtils.isNotBlank(architecture)) {
                osInfo.setArchitecture(architecture.trim());
                logger.info("获取到CPU架构: {}", architecture.trim());
                // 更新CPU架构信息
                cacheUpdater.updateCache(hostInfo);
            }

            // 获取负载信息
            String loadCmd = "cat /proc/loadavg";
            String loadInfo = MinaUtils.execCmdWithResult(session, loadCmd);
            if (StringUtils.isNotBlank(loadInfo)) {
                String[] parts = loadInfo.trim().split("\\s+");
                if (parts.length >= 3) {
                    try {
                        osInfo.setLoad1Min(Double.parseDouble(parts[0]));
                        osInfo.setLoad5Min(Double.parseDouble(parts[1]));
                        osInfo.setLoad15Min(Double.parseDouble(parts[2]));
                        logger.info("获取到负载信息: 1分钟={}, 5分钟={}, 15分钟={}",
                                osInfo.getLoad1Min(), osInfo.getLoad5Min(), osInfo.getLoad15Min());
                        // 更新负载信息
                        cacheUpdater.updateCache(hostInfo);
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
                // 更新发行版名称
                cacheUpdater.updateCache(hostInfo);
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
                // 更新显示名称
                cacheUpdater.updateCache(hostInfo);
            }

            // 标记OS信息为有效
            osInfo.setValid(true);
            // 完成时更新一次
            cacheUpdater.updateCache(hostInfo);

            return osInfo;
        } catch (Exception e) {
            logger.error("收集Linux系统信息时出错: {}", e.getMessage(), e);
            osInfo.setValid(false);
            // 出错时也更新缓存，标记错误状态
            cacheUpdater.updateCache(hostInfo);
            return osInfo;
        }
    }

    @Override
    public void collectHardwareInfo(OsInfo osInfo, ClientSession session, CacheUpdater cacheUpdater) {
        logger.info("开始收集Linux硬件信息");
        osInfo.setHardwareCollectionStatus("collecting");
        // 更新收集状态
        HostInfo hostInfo = osInfo.getHostInfo();
        cacheUpdater.updateCache(hostInfo);

        try {
            // 收集CPU信息
            osInfo.setLastUpdatedItem("collecting_cpu");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(hostInfo);
            collectCpuInfo(osInfo, session);

            // 收集内存信息
            osInfo.setLastUpdatedItem("collecting_memory");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(hostInfo);
            collectMemoryInfo(osInfo, session);

            // 收集磁盘信息
            osInfo.setLastUpdatedItem("collecting_disk");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(hostInfo);
            collectDiskInfo(osInfo, session);

            // 收集交换分区信息
            osInfo.setLastUpdatedItem("collecting_swap");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(hostInfo);
            collectSwapInfo(osInfo, session);

            // 收集GPU信息
            osInfo.setLastUpdatedItem("collecting_gpu");
            // 更新当前正在处理的项
            cacheUpdater.updateCache(hostInfo);
            collectGpuInfo(osInfo, session);

            // 标记为完成
            osInfo.setLastUpdatedItem("completed");
            osInfo.setHardwareCollectionStatus("success");
            // 完成时更新一次
            cacheUpdater.updateCache(hostInfo);

            logger.info("Linux硬件信息收集完成");
        } catch (Exception e) {
            logger.error("收集Linux硬件信息时出错: {}", e.getMessage(), e);
            osInfo.setHardwareCollectionStatus("error");
            osInfo.setLastUpdatedItem("error");
            // 出错时也更新
            cacheUpdater.updateCache(hostInfo);
        }
    }

    /**
     * 解析/etc/os-release文件内容
     */
    private void parseOsRelease(OsInfo osInfo, String content) {
        try {
            logger.debug("解析/etc/os-release文件内容");
            Map<String, String> osReleaseData = new HashMap<>();

            // 按行处理，提取key=value对
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int equalPos = line.indexOf('=');
                if (equalPos > 0) {
                    String key = line.substring(0, equalPos).trim();
                    String value = line.substring(equalPos + 1).trim();

                    // 移除引号（如果存在）
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    osReleaseData.put(key, value);
                }
            }

            // 设置ID和版本信息
            String id = osReleaseData.get("ID");
            if (StringUtils.isNotBlank(id)) {
                id = id.toLowerCase();
                osInfo.setDistributionId(id);

                // 根据ID设置合适的发行版名称
                String prettyName = osReleaseData.get("PRETTY_NAME");
                if (StringUtils.isNotBlank(prettyName)) {
                    osInfo.setFullName(prettyName);
                }

                // 设置分发版名称（首字母大写）
                String name = osReleaseData.get("NAME");
                if (StringUtils.isNotBlank(name)) {
                    osInfo.setDistributionName(name);
                    osInfo.setDistribution(name);
                } else {
                    // 如果没有NAME字段，使用ID首字母大写
                    String capitalizedId = id.substring(0, 1).toUpperCase() + id.substring(1);
                    osInfo.setDistributionName(capitalizedId);
                    osInfo.setDistribution(capitalizedId);
                }

                // 修正特定发行版的名称
                if ("ubuntu".equals(id)) {
                    osInfo.setDistribution("Ubuntu");
                    osInfo.setDistributionName("Ubuntu");

                    // 设置displayName，优先使用PRETTY_NAME，否则使用标准格式
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    } else {
                        String versionId = osReleaseData.get("VERSION_ID");
                        if (StringUtils.isNotBlank(versionId)) {
                            osInfo.setDisplayName("Ubuntu " + versionId);
                        }
                    }
                } else if ("debian".equals(id)) {
                    osInfo.setDistribution("Debian");
                    osInfo.setDistributionName("Debian");

                    // 设置displayName，优先使用PRETTY_NAME，否则使用标准格式
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    } else {
                        String versionId = osReleaseData.get("VERSION_ID");
                        if (StringUtils.isNotBlank(versionId)) {
                            osInfo.setDisplayName("Debian GNU/Linux " + versionId);
                        }
                    }
                } else if ("centos".equals(id)) {
                    osInfo.setDistribution("CentOS");
                    osInfo.setDistributionName("CentOS");

                    // 设置displayName
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    }
                } else if ("rhel".equals(id)) {
                    osInfo.setDistribution("Red Hat Enterprise Linux");
                    osInfo.setDistributionName("Red Hat Enterprise Linux");

                    // 设置displayName
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    }
                } else if ("fedora".equals(id)) {
                    osInfo.setDistribution("Fedora");
                    osInfo.setDistributionName("Fedora");

                    // 设置displayName
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    }
                } else if ("opensuse".equals(id) || "suse".equals(id)) {
                    osInfo.setDistribution("SUSE");
                    osInfo.setDistributionName("SUSE Linux");

                    // 设置displayName
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    }
                } else if ("arch".equals(id)) {
                    osInfo.setDistribution("Arch");
                    osInfo.setDistributionName("Arch Linux");

                    // 设置displayName
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    }
                } else if ("manjaro".equals(id)) {
                    osInfo.setDistribution("Manjaro");
                    osInfo.setDistributionName("Manjaro Linux");

                    // 设置displayName
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    }
                } else if ("alpine".equals(id)) {
                    osInfo.setDistribution("Alpine");
                    osInfo.setDistributionName("Alpine Linux");

                    // 设置displayName
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    }
                } else if ("gentoo".equals(id)) {
                    osInfo.setDistribution("Gentoo");
                    osInfo.setDistributionName("Gentoo Linux");

                    // 设置displayName
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    }
                } else if ("kylin".equals(id)) {
                    osInfo.setDistribution("Kylin");
                    osInfo.setDistributionName("Kylin");

                    // 设置displayName
                    if (StringUtils.isNotBlank(prettyName)) {
                        osInfo.setDisplayName(prettyName);
                    }
                }

                // 如果没有设置displayName，使用distributionName
                if (StringUtils.isBlank(osInfo.getDisplayName())) {
                    osInfo.setDisplayName(osInfo.getDistributionName());
                }
            }

            // 设置版本ID并更新主版本
            String versionId = osReleaseData.get("VERSION_ID");
            if (StringUtils.isNotBlank(versionId)) {
                // 移除引号（如果存在）
                if ((versionId.startsWith("\"") && versionId.endsWith("\"")) ||
                        (versionId.startsWith("'") && versionId.endsWith("'"))) {
                    versionId = versionId.substring(1, versionId.length() - 1);
                }

                osInfo.setVersionId(versionId);
                osInfo.setDistributionVersion(versionId);

                // 设置主版本号（例如从20.04中提取20）
                if (versionId.contains(".")) {
                    String majorVersion = versionId.split("\\.")[0];
                    osInfo.setMajorVersion(majorVersion);
                } else {
                    osInfo.setMajorVersion(versionId);
                }
            }

            // 如果有VERSION字段，设置更完整的版本描述
            String version = osReleaseData.get("VERSION");
            if (StringUtils.isNotBlank(version)) {
                // 如果fullName未设置，使用distribution + version
                if (StringUtils.isBlank(osInfo.getFullName())) {
                    osInfo.setFullName(osInfo.getDistribution() + " " + version);
                }

                // 如果displayName未设置，也使用此格式
                if (StringUtils.isBlank(osInfo.getDisplayName())) {
                    osInfo.setDisplayName(osInfo.getDistribution() + " " + version);
                }
            } else if (StringUtils.isBlank(osInfo.getFullName()) && StringUtils.isNotBlank(versionId)) {
                // 如果没有VERSION字段但有VERSION_ID，使用distribution + versionId
                osInfo.setFullName(osInfo.getDistribution() + " " + versionId);

                // 如果displayName未设置，也使用此格式
                if (StringUtils.isBlank(osInfo.getDisplayName())) {
                    osInfo.setDisplayName(osInfo.getDistribution() + " " + versionId);
                }
            }

            logger.debug("从/etc/os-release解析到发行版: {}，版本: {}，显示名称: {}",
                    osInfo.getDistributionName(), osInfo.getVersionId(), osInfo.getDisplayName());
        } catch (Exception e) {
            logger.error("解析/etc/os-release内容时出错", e);
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

                // 识别CentOS和RHEL
                if (redhatRelease.toLowerCase().contains("centos")) {
                    osInfo.setDistributionId("centos");
                    osInfo.setDistributionName("CentOS");
                } else if (redhatRelease.toLowerCase().contains("red hat")) {
                    osInfo.setDistributionId("rhel");
                    osInfo.setDistributionName("Red Hat Enterprise Linux");
                }

                // 提取版本号
                Pattern pattern = Pattern.compile("release\\s+([\\d\\.]+)");
                Matcher matcher = pattern.matcher(redhatRelease);
                if (matcher.find()) {
                    String version = matcher.group(1);
                    osInfo.setVersionId(version);
                    osInfo.setDistributionVersion(version);

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

                    // 从lsb_release提取Ubuntu版本
                    Pattern pattern = Pattern.compile("Release:\\s+([\\d\\.]+)");
                    Matcher matcher = pattern.matcher(lsbRelease);
                    if (matcher.find()) {
                        String version = matcher.group(1);
                        osInfo.setVersionId(version);
                        osInfo.setDistributionVersion(version);

                        // 设置主版本号（Ubuntu使用年份作为主版本号，例如20.04的主版本是20）
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
                    osInfo.setDistributionVersion(debianVersion);

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
                    osInfo.setDistributionVersion(version);

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
            String[] distroFiles = {
                    "cat /etc/SuSE-release 2>/dev/null", // SUSE
                    "cat /etc/arch-release 2>/dev/null", // Arch Linux
                    "cat /etc/gentoo-release 2>/dev/null", // Gentoo
                    "cat /etc/slackware-version 2>/dev/null", // Slackware
                    "cat /etc/alpine-release 2>/dev/null", // Alpine
                    "cat /etc/kylin-release 2>/dev/null" // Kylin
            };

            for (String cmd : distroFiles) {
                String result = MinaUtils.execCmdWithResult(session, cmd);
                if (StringUtils.isNotBlank(result)) {
                    result = result.trim();

                    // 基于命令确定发行版类型
                    if (cmd.contains("SuSE")) {
                        osInfo.setDistributionId("suse");
                        osInfo.setDistribution("SUSE");
                        osInfo.setDistributionName("SUSE");
                        osInfo.setFullName("SUSE Linux " + result);
                        osInfo.setDisplayName(osInfo.getFullName());
                    } else if (cmd.contains("arch")) {
                        osInfo.setDistributionId("arch");
                        osInfo.setDistribution("Arch");
                        osInfo.setDistributionName("Arch Linux");
                        osInfo.setFullName("Arch Linux");
                        osInfo.setDisplayName(osInfo.getFullName());
                    } else if (cmd.contains("gentoo")) {
                        osInfo.setDistributionId("gentoo");
                        osInfo.setDistribution("Gentoo");
                        osInfo.setDistributionName("Gentoo Linux");
                        osInfo.setFullName("Gentoo Linux " + result);
                        osInfo.setDisplayName(osInfo.getFullName());
                    } else if (cmd.contains("slackware")) {
                        osInfo.setDistributionId("slackware");
                        osInfo.setDistribution("Slackware");
                        osInfo.setDistributionName("Slackware Linux");
                        osInfo.setFullName("Slackware Linux " + result);
                        osInfo.setDisplayName(osInfo.getFullName());
                    } else if (cmd.contains("alpine")) {
                        osInfo.setDistributionId("alpine");
                        osInfo.setDistribution("Alpine");
                        osInfo.setDistributionName("Alpine Linux");
                        osInfo.setVersionId(result);
                        osInfo.setDistributionVersion(result);
                        osInfo.setFullName("Alpine Linux " + result);
                        osInfo.setDisplayName(osInfo.getFullName());
                    } else if (cmd.contains("kylin")) {
                        osInfo.setDistributionId("kylin");
                        osInfo.setDistribution("Kylin");
                        osInfo.setDistributionName("Kylin");

                        // 尝试提取Kylin版本号
                        Pattern pattern = Pattern.compile("V([\\d\\.]+)");
                        Matcher matcher = pattern.matcher(result);
                        if (matcher.find()) {
                            String version = matcher.group(1);
                            osInfo.setVersionId(version);
                            osInfo.setDistributionVersion(version);
                            osInfo.setMajorVersion(version);
                            osInfo.setFullName("Kylin Linux V" + version);
                        } else {
                            osInfo.setFullName("Kylin Linux " + result);
                        }
                        osInfo.setDisplayName(osInfo.getFullName());
                    }

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
                if ("Linux".equalsIgnoreCase(osType)) {
                    osInfo.setDistributionId("linux");
                    osInfo.setDistribution("Linux");
                    osInfo.setDistributionName("Linux");
                    osInfo.setFullName("Linux");
                    osInfo.setDisplayName("Linux");
                } else {
                    osInfo.setDistributionId(osType.toLowerCase());
                    osInfo.setDistribution(osType);
                    osInfo.setDistributionName(osType);
                    osInfo.setFullName(osType);
                    osInfo.setDisplayName(osType);
                }
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

    private void collectCpuInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集CPU信息");

        // 获取CPU型号
        String cpuInfoCmd = "cat /proc/cpuinfo | grep 'model name' | head -n 1 | cut -d ':' -f 2";
        String cpuInfo = MinaUtils.execCmdWithResult(session, cpuInfoCmd);
        if (StringUtils.isNotBlank(cpuInfo)) {
            osInfo.setCpuInfo(cpuInfo.trim());
            osInfo.setLastUpdatedItem("cpuInfo");
            logger.debug("获取到CPU型号: {}", cpuInfo.trim());
        }

        // 获取物理CPU数量
        String cpuCountCmd = "cat /proc/cpuinfo | grep 'physical id' | sort -u | wc -l";
        String cpuCountStr = MinaUtils.execCmdWithResult(session, cpuCountCmd);
        if (StringUtils.isNotBlank(cpuCountStr)) {
            try {
                int cpuCount = Integer.parseInt(cpuCountStr.trim());
                osInfo.setCpuCount(cpuCount > 0 ? cpuCount : 1);
                logger.debug("获取到物理CPU数量: {}", osInfo.getCpuCount());
            } catch (NumberFormatException e) {
                logger.warn("解析物理CPU数量失败: {}", cpuCountStr);
            }
        }

        // 获取每颗CPU的核心数
        String coresPerCpuCmd = "cat /proc/cpuinfo | grep 'cpu cores' | head -n 1 | cut -d ':' -f 2";
        String coresPerCpuStr = MinaUtils.execCmdWithResult(session, coresPerCpuCmd);
        if (StringUtils.isNotBlank(coresPerCpuStr)) {
            try {
                int coresPerCpu = Integer.parseInt(coresPerCpuStr.trim());
                osInfo.setCpuCoresPerProcessor(coresPerCpu);
                osInfo.setCpuCores(coresPerCpu * osInfo.getCpuCount());
                osInfo.setLastUpdatedItem("cpuCores");
                logger.debug("获取到每颗CPU的核心数: {}, 总核心数: {}",
                        osInfo.getCpuCoresPerProcessor(), osInfo.getCpuCores());
            } catch (NumberFormatException e) {
                logger.warn("解析每颗CPU核心数失败: {}", coresPerCpuStr);
            }
        }

        // 获取CPU线程数量（逻辑处理器数）
        String logicalCoresCmd = "nproc";
        String logicalCoresStr = MinaUtils.execCmdWithResult(session, logicalCoresCmd);
        if (StringUtils.isNotBlank(logicalCoresStr)) {
            try {
                int logicalCores = Integer.parseInt(logicalCoresStr.trim());
                osInfo.setCpuLogicalCores(logicalCores);

                // 计算每核心的线程数
                if (osInfo.getCpuCores() > 0) {
                    osInfo.setCpuThreadsPerCore(logicalCores / osInfo.getCpuCores());
                }

                logger.debug("获取到CPU逻辑处理器数量: {}, 每核心线程数: {}",
                        logicalCores, osInfo.getCpuThreadsPerCore());
            } catch (NumberFormatException e) {
                logger.warn("解析CPU逻辑处理器数量失败: {}", logicalCoresStr);
            }
        }
    }

    private void collectMemoryInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集内存信息");

        // 获取内存信息
        String memInfoCmd = "cat /proc/meminfo | grep -E 'MemTotal|MemAvailable'";
        String memInfo = MinaUtils.execCmdWithResult(session, memInfoCmd);

        if (StringUtils.isNotBlank(memInfo)) {
            Pattern totalPattern = Pattern.compile("MemTotal:\\s+(\\d+)\\s+kB");
            Pattern availablePattern = Pattern.compile("MemAvailable:\\s+(\\d+)\\s+kB");

            Matcher totalMatcher = totalPattern.matcher(memInfo);
            Matcher availableMatcher = availablePattern.matcher(memInfo);

            if (totalMatcher.find()) {
                try {
                    long totalKb = Long.parseLong(totalMatcher.group(1));
                    double totalGb = totalKb / (1024.0 * 1024.0);
                    osInfo.setTotalMemory(Math.round(totalGb * 10) / 10.0);
                    logger.debug("获取到内存总量: {} GB", osInfo.getTotalMemory());
                } catch (NumberFormatException e) {
                    logger.warn("解析内存总量失败: {}", totalMatcher.group(1));
                }
            }

            if (availableMatcher.find()) {
                try {
                    long availableKb = Long.parseLong(availableMatcher.group(1));
                    double availableGb = availableKb / (1024.0 * 1024.0);
                    osInfo.setAvailableMemory(Math.round(availableGb * 10) / 10.0);
                    logger.debug("获取到可用内存: {} GB", osInfo.getAvailableMemory());
                } catch (NumberFormatException e) {
                    logger.warn("解析可用内存失败: {}", availableMatcher.group(1));
                }
            }

            osInfo.setLastUpdatedItem("memoryInfo");
        }
    }

    private void collectDiskInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集磁盘信息");

        // 获取磁盘信息，排除tmpfs, devtmpfs等临时文件系统
        String diskInfoCmd = "df -BG | grep -v -E 'tmpfs|devtmpfs|overlay|udev|/dev/loop|docker'";
        String diskInfo = MinaUtils.execCmdWithResult(session, diskInfoCmd);

        if (StringUtils.isNotBlank(diskInfo)) {
            double totalDisk = 0;
            double availableDisk = 0;

            String[] lines = diskInfo.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("Filesystem") || line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 4) {
                    try {
                        double size = Double.parseDouble(parts[1].replace("G", ""));
                        double available = Double.parseDouble(parts[3].replace("G", ""));

                        totalDisk += size;
                        availableDisk += available;
                    } catch (NumberFormatException e) {
                        logger.warn("解析磁盘信息失败: {}", line);
                    }
                }
            }

            osInfo.setTotalDisk((long) (totalDisk * 1024 * 1024 * 1024));
            osInfo.setAvailableDisk((long) (availableDisk * 1024 * 1024 * 1024));
            osInfo.setLastUpdatedItem("diskInfo");

            logger.debug("获取到磁盘总量: {} GB, 可用磁盘: {} GB",
                    osInfo.getTotalDisk(), osInfo.getAvailableDisk());
        }
    }

    private void collectSwapInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集交换空间信息");

        // 获取swap信息
        String swapInfoCmd = "cat /proc/meminfo | grep -E 'SwapTotal|SwapFree'";
        String swapInfo = MinaUtils.execCmdWithResult(session, swapInfoCmd);

        if (StringUtils.isNotBlank(swapInfo)) {
            Pattern totalPattern = Pattern.compile("SwapTotal:\\s+(\\d+)\\s+kB");
            Pattern freePattern = Pattern.compile("SwapFree:\\s+(\\d+)\\s+kB");

            Matcher totalMatcher = totalPattern.matcher(swapInfo);
            Matcher freeMatcher = freePattern.matcher(swapInfo);

            if (totalMatcher.find()) {
                try {
                    long totalKb = Long.parseLong(totalMatcher.group(1));
                    double totalGb = totalKb / (1024.0 * 1024.0);
                    osInfo.setTotalSwap((long) (totalGb * 1024 * 1024 * 1024));
                    logger.debug("获取到交换空间总量: {} GB", osInfo.getTotalSwap());
                } catch (NumberFormatException e) {
                    logger.warn("解析交换空间总量失败: {}", totalMatcher.group(1));
                }
            }

            if (freeMatcher.find()) {
                try {
                    long freeKb = Long.parseLong(freeMatcher.group(1));
                    double freeGb = freeKb / (1024.0 * 1024.0);
                    osInfo.setAvailableSwap((long) (freeGb * 1024 * 1024 * 1024));
                    logger.debug("获取到可用交换空间: {} GB", osInfo.getAvailableSwap());
                } catch (NumberFormatException e) {
                    logger.warn("解析可用交换空间失败: {}", freeMatcher.group(1));
                }
            }

            osInfo.setLastUpdatedItem("swapInfo");
        }
    }

    private void collectGpuInfo(OsInfo osInfo, ClientSession session) {
        logger.debug("开始收集GPU信息");

        // 尝试获取GPU信息
        String gpuInfoCmd = "lspci | grep -i 'vga\\|3d\\|2d' | cut -d ':' -f3";
        String gpuInfo = MinaUtils.execCmdWithResult(session, gpuInfoCmd);
        if (gpuInfo != null && !gpuInfo.isEmpty()) {
            osInfo.setGpuInfo(gpuInfo.trim());
            // 获取显存信息 - 先尝试通过lspci详细输出获取
            String gpuMemoryCmd = "lspci -v | grep -i vga -A 10 | grep -i 'memory.*size' | head -n 1 | sed -r 's/.*size=([0-9]+)[Mm].*/\\1/g'";
            String gpuMemoryStr = MinaUtils.execCmdWithResult(session, gpuMemoryCmd);
            if (gpuMemoryStr != null && !gpuMemoryStr.isEmpty()) {
                try {
                    // 转换MB到GB
                    double gpuMemoryMB = Double.parseDouble(gpuMemoryStr.trim());
                    osInfo.setGpuMemory(Math.round(gpuMemoryMB / 1024 * 10) / 10.0);
                    logger.debug("获取到GPU显存: {} GB", osInfo.getGpuMemory());
                } catch (NumberFormatException e) {
                    logger.warn("解析GPU显存失败: {}", gpuMemoryStr);
                }
            }
            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("gpuInfo");
        } else {
            // 尝试使用nvidia-smi查询NVIDIA GPU
            gpuInfo = MinaUtils.execCmdWithResult(session,
                    "which nvidia-smi && nvidia-smi --query-gpu=name --format=csv,noheader 2>/dev/null || echo ''");
            if (gpuInfo != null && !gpuInfo.isEmpty() && !gpuInfo.contains("which")) {
                osInfo.setGpuInfo(gpuInfo.trim());

                // 获取NVIDIA显存
                String gpuMemoryCmd = "nvidia-smi --query-gpu=memory.total --format=csv,noheader,nounits";
                String gpuMemoryStr = MinaUtils.execCmdWithResult(session, gpuMemoryCmd);
                if (gpuMemoryStr != null && !gpuMemoryStr.isEmpty()) {
                    try {
                        // 转换MB到GB并保留1位小数
                        double gpuMemoryMB = Double.parseDouble(gpuMemoryStr.trim());
                        osInfo.setGpuMemory(Math.round(gpuMemoryMB / 1024 * 10) / 10.0);
                        logger.debug("获取到NVIDIA GPU显存: {} GB", osInfo.getGpuMemory());
                    } catch (NumberFormatException e) {
                        logger.warn("解析NVIDIA GPU显存失败: {}", gpuMemoryStr);
                    }
                }

                // 更新硬件收集状态
                osInfo.setLastUpdatedItem("gpuInfo");
            }
        }
    }
}