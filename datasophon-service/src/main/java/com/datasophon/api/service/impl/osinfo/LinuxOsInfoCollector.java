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
                hostInfo.setFqdn(fqdn);
                logger.info("获取到FQDN: {}", fqdn);
                // 立即更新缓存，使前端能看到FQDN
                cacheUpdater.updateCache(hostInfo);
            }

            // 读取/etc/hosts文件内容
            String hostsFile = MinaUtils.execCmdWithResult(session, "cat /etc/hosts 2>/dev/null");
            if (StringUtils.isNotBlank(hostsFile)) {
                hostInfo.setHostsFile(hostsFile);
                logger.info("获取到hosts文件内容");
                // 立即更新缓存，使前端能看到hosts文件内容
                cacheUpdater.updateCache(hostInfo);
            }

            // 获取DNS服务器信息
            String dnsInfo = MinaUtils.execCmdWithResult(session,
                    "cat /etc/resolv.conf | grep nameserver | awk '{print $2}' | tr '\\n' ' '");
            if (StringUtils.isNotBlank(dnsInfo)) {
                dnsInfo = dnsInfo.trim();
                osInfo.setDnsServers(dnsInfo);
                logger.info("获取到DNS服务器信息: {}", dnsInfo);
                // 立即更新缓存，使前端能看到DNS服务器信息
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
            osInfo.setHardwareCollectionStatus("success");
            // 完成时更新一次
            cacheUpdater.updateCache(null);

            logger.info("Linux硬件信息收集完成");
        } catch (Exception e) {
            logger.error("收集Linux硬件信息时出错: {}", e.getMessage(), e);
            osInfo.setHardwareCollectionStatus("error");
            osInfo.setLastUpdatedItem("error");
            // 出错时更新状态
            cacheUpdater.updateCache(null);
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

            // 如果有VERSION_ID，更新分发版名称和版本显示
            String versionId = osReleaseData.get("VERSION_ID");
            if (StringUtils.isNotBlank(versionId)) {
                // 移除引号（如果存在）
                if ((versionId.startsWith("\"") && versionId.endsWith("\"")) ||
                        (versionId.startsWith("'") && versionId.endsWith("'"))) {
                    versionId = versionId.substring(1, versionId.length() - 1);
                }

                osInfo.setVersionId(versionId);
                osInfo.setDistributionVersion(versionId);

                // 设置主版本号
                String majorVersion = versionId;
                if (versionId.contains(".")) {
                    majorVersion = versionId.split("\\.")[0];
                }
                osInfo.setMajorVersion(majorVersion);

                // 格式化显示名称
                String distribution = osInfo.getDistributionType().toString();
                osInfo.setFullName(distribution + " " + versionId);
                osInfo.setDisplayName(distribution + " " + versionId);

                logger.debug("从/etc/os-release解析到发行版: {}，版本: {}，显示名称: {}",
                        osInfo.getDistributionName(), osInfo.getVersionId(), osInfo.getDisplayName());
            }

            // 设置显示名称
            if (osInfo.getFullName() != null) {
                osInfo.setDisplayName(osInfo.getDistributionType().toString() + " " + versionId);
            } else {
                osInfo.setDisplayName(osInfo.getDistributionType().toString() + " " + versionId);
            }
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
            // 使用lscpu命令获取详细CPU信息
            String lscpuOutput = MinaUtils.execCmdWithResult(session, "lscpu");
            if (StringUtils.isNotBlank(lscpuOutput)) {
                // 解析CPU型号
                Pattern modelNamePattern = Pattern.compile("Model name:\\s+(.+)");
                Matcher modelNameMatcher = modelNamePattern.matcher(lscpuOutput);
                if (modelNameMatcher.find()) {
                    String cpuModel = modelNameMatcher.group(1).trim();
                    osInfo.setCpuModel(cpuModel);
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
                        osInfo.setCpuFrequency(freqGHz);
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
                        osInfo.setCpuCount(cpuCount);
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
                        osInfo.setCpuCoresPerProcessor(coresPerCpu);
                        logger.debug("获取到每CPU核心数: {}", coresPerCpu);
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
                        osInfo.setCpuThreadsPerCore(threadsPerCore);
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
                        int cpuCores = Integer.parseInt(cpuCoresMatcher.group(1).trim());
                        osInfo.setCpuLogicalCores(cpuCores);

                        // 计算物理核心数 = CPU数量 * 每CPU的核心数
                        if (osInfo.getCpuCount() != null && osInfo.getCpuCoresPerProcessor() != null) {
                            int physicalCores = osInfo.getCpuCount() * osInfo.getCpuCoresPerProcessor();
                            osInfo.setCpuCores(physicalCores);
                            osInfo.setCpuCoreNum(physicalCores); // 设置别名
                            logger.debug("计算得到物理CPU核心数: {}", physicalCores);
                        } else {
                            // 如果无法计算物理核心，使用逻辑核心数代替
                            osInfo.setCpuCores(cpuCores);
                            osInfo.setCpuCoreNum(cpuCores); // 设置别名
                            logger.debug("无法计算物理CPU核心数，使用逻辑核心数: {}", cpuCores);
                        }

                        logger.debug("获取到CPU逻辑核心数: {}", cpuCores);
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU核心数失败: {}", e.getMessage());
                    }
                }
            }

            // 读取/proc/cpuinfo获取完整CPU信息
            String cpuinfoOutput = MinaUtils.execCmdWithResult(session,
                    "cat /proc/cpuinfo | grep -E 'processor|model name|cpu MHz'");
            if (StringUtils.isNotBlank(cpuinfoOutput)) {
                osInfo.setCpuInfo(cpuinfoOutput);
                logger.debug("获取到完整CPU信息");
            }

            // 获取负载信息
            String loadAvgOutput = MinaUtils.execCmdWithResult(session, "cat /proc/loadavg");
            if (StringUtils.isNotBlank(loadAvgOutput)) {
                String[] loadParts = loadAvgOutput.trim().split("\\s+");
                if (loadParts.length >= 3) {
                    try {
                        osInfo.setLoad1Min(Double.parseDouble(loadParts[0]));
                        osInfo.setLoad5Min(Double.parseDouble(loadParts[1]));
                        osInfo.setLoad15Min(Double.parseDouble(loadParts[2]));
                        logger.debug("获取到系统负载: 1分钟={}, 5分钟={}, 15分钟={}",
                                osInfo.getLoad1Min(), osInfo.getLoad5Min(), osInfo.getLoad15Min());
                    } catch (NumberFormatException e) {
                        logger.warn("解析系统负载失败: {}", e.getMessage());
                    }
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("cpuInfo");

            logger.info("CPU信息收集完成");
        } catch (Exception e) {
            logger.error("收集CPU信息时出错: {}", e.getMessage(), e);
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
            // 读取/proc/meminfo获取内存信息
            String meminfoOutput = MinaUtils.execCmdWithResult(session, "cat /proc/meminfo");
            if (StringUtils.isNotBlank(meminfoOutput)) {
                // 解析总内存
                Pattern totalMemPattern = Pattern.compile("MemTotal:\\s+(\\d+)\\s+kB");
                Matcher totalMemMatcher = totalMemPattern.matcher(meminfoOutput);
                if (totalMemMatcher.find()) {
                    try {
                        long totalMemKB = Long.parseLong(totalMemMatcher.group(1).trim());
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

                // 解析可用内存（MemAvailable或MemFree）
                Pattern availableMemPattern = Pattern.compile("MemAvailable:\\s+(\\d+)\\s+kB");
                Matcher availableMemMatcher = availableMemPattern.matcher(meminfoOutput);
                if (availableMemMatcher.find()) {
                    try {
                        long availableMemKB = Long.parseLong(availableMemMatcher.group(1).trim());
                        // 保存原始字节数
                        osInfo.setAvailableMem(availableMemKB * 1024);
                        // 转换为GB并保留一位小数
                        double availableMemGB = Math.round(availableMemKB / 1024.0 / 1024.0 * 10) / 10.0;
                        osInfo.setAvailableMemory(availableMemGB);
                        logger.debug("获取到可用内存: {} GB", availableMemGB);
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
                            // 保存原始字节数
                            osInfo.setAvailableMem(freeMemKB * 1024);
                            // 转换为GB并保留一位小数
                            double freeMemGB = Math.round(freeMemKB / 1024.0 / 1024.0 * 10) / 10.0;
                            osInfo.setAvailableMemory(freeMemGB);
                            logger.debug("获取到空闲内存: {} GB", freeMemGB);
                        } catch (NumberFormatException e) {
                            logger.warn("解析空闲内存失败: {}", e.getMessage());
                        }
                    }
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("memoryInfo");

            logger.info("内存信息收集完成");
        } catch (Exception e) {
            logger.error("收集内存信息时出错: {}", e.getMessage(), e);
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
            // 使用df命令获取磁盘使用情况
            String dfOutput = MinaUtils.execCmdWithResult(session, "df -P");
            if (StringUtils.isNotBlank(dfOutput)) {
                // 解析磁盘信息
                String[] lines = dfOutput.split("\n");
                long totalBytes = 0;
                long availableBytes = 0;

                for (int i = 1; i < lines.length; i++) { // 跳过标题行
                    String line = lines[i].trim();
                    String[] parts = line.split("\\s+");

                    if (parts.length >= 6) {
                        try {
                            // 排除没有路径前缀的特殊文件系统
                            if (parts[0].startsWith("/") || parts[5].startsWith("/")) {
                                long size = Long.parseLong(parts[1]) * 1024; // 转换为字节
                                long avail = Long.parseLong(parts[3]) * 1024; // 转换为字节

                                totalBytes += size;
                                availableBytes += avail;
                            }
                        } catch (NumberFormatException e) {
                            logger.warn("解析磁盘信息失败: {}", line);
                        }
                    }
                }

                // 保存原始字节数
                osInfo.setTotalDiskBytes(totalBytes);
                osInfo.setAvailableDiskBytes(availableBytes);

                // 使用接受Long类型的setter方法，传入字节数
                osInfo.setTotalDisk(totalBytes);
                osInfo.setAvailableDisk(availableBytes);

                // 计算GB值用于日志记录
                double totalDiskGB = Math.round(totalBytes / 1024.0 / 1024.0 / 1024.0 * 10) / 10.0;
                double availableDiskGB = Math.round(availableBytes / 1024.0 / 1024.0 / 1024.0 * 10) / 10.0;
                logger.debug("获取到磁盘总容量: {} GB, 可用容量: {} GB", totalDiskGB, availableDiskGB);
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("diskInfo");

            logger.info("磁盘信息收集完成");
        } catch (Exception e) {
            logger.error("收集磁盘信息时出错: {}", e.getMessage(), e);
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
            // 读取/proc/meminfo中的交换分区信息
            String swapInfoOutput = MinaUtils.execCmdWithResult(session, "grep Swap /proc/meminfo");
            if (StringUtils.isNotBlank(swapInfoOutput)) {
                // 解析交换分区总容量
                Pattern totalSwapPattern = Pattern.compile("SwapTotal:\\s+(\\d+)\\s+kB");
                Matcher totalSwapMatcher = totalSwapPattern.matcher(swapInfoOutput);
                if (totalSwapMatcher.find()) {
                    try {
                        long totalSwapKB = Long.parseLong(totalSwapMatcher.group(1).trim());
                        // 计算字节数
                        long totalSwapBytes = totalSwapKB * 1024;
                        // 保存原始字节数
                        osInfo.setTotalSwapBytes(totalSwapBytes);
                        // 使用接受Long类型的setter方法
                        osInfo.setTotalSwap(totalSwapBytes);

                        // 计算GB值用于日志记录
                        double totalSwapGB = Math.round(totalSwapKB / 1024.0 / 1024.0 * 10) / 10.0;
                        logger.debug("获取到交换分区总容量: {} GB", totalSwapGB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析交换分区总容量失败: {}", e.getMessage());
                    }
                }

                // 解析交换分区可用容量
                Pattern freeSwapPattern = Pattern.compile("SwapFree:\\s+(\\d+)\\s+kB");
                Matcher freeSwapMatcher = freeSwapPattern.matcher(swapInfoOutput);
                if (freeSwapMatcher.find()) {
                    try {
                        long freeSwapKB = Long.parseLong(freeSwapMatcher.group(1).trim());
                        // 计算字节数
                        long freeSwapBytes = freeSwapKB * 1024;
                        // 保存原始字节数
                        osInfo.setAvailableSwapBytes(freeSwapBytes);
                        // 使用接受Long类型的setter方法
                        osInfo.setAvailableSwap(freeSwapBytes);

                        // 计算GB值用于日志记录
                        double freeSwapGB = Math.round(freeSwapKB / 1024.0 / 1024.0 * 10) / 10.0;
                        logger.debug("获取到交换分区可用容量: {} GB", freeSwapGB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析交换分区可用容量失败: {}", e.getMessage());
                    }
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("swapInfo");

            logger.info("交换分区信息收集完成");
        } catch (Exception e) {
            logger.error("收集交换分区信息时出错: {}", e.getMessage(), e);
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

                logger.debug("获取到GPU信息: {}", gpuInfo.trim());
            } else {
                // 尝试使用nvidia-smi获取NVIDIA GPU信息
                String nvidiaCmd = "which nvidia-smi && nvidia-smi --query-gpu=name,memory.total --format=csv,noheader";
                String nvidiaSmiOutput = MinaUtils.execCmdWithResult(session, nvidiaCmd);
                if (nvidiaSmiOutput != null && !nvidiaSmiOutput.isEmpty()
                        && !nvidiaSmiOutput.contains("which: no nvidia-smi")) {
                    osInfo.setGpuInfo(nvidiaSmiOutput.trim());

                    // 提取显存信息
                    Pattern memPattern = Pattern.compile("(\\d+) MiB");
                    Matcher memMatcher = memPattern.matcher(nvidiaSmiOutput);
                    if (memMatcher.find()) {
                        try {
                            double gpuMemoryMB = Double.parseDouble(memMatcher.group(1).trim());
                            osInfo.setGpuMemory(Math.round(gpuMemoryMB / 1024 * 10) / 10.0);
                            logger.debug("获取到NVIDIA GPU显存: {} GB", osInfo.getGpuMemory());
                        } catch (NumberFormatException e) {
                            logger.warn("解析NVIDIA GPU显存失败: {}", e.getMessage());
                        }
                    }

                    logger.debug("获取到NVIDIA GPU信息: {}", nvidiaSmiOutput.trim());
                } else {
                    osInfo.setGpuInfo("无GPU或无法检测");
                    logger.debug("未检测到GPU信息");
                }
            }

            // 更新硬件收集状态
            osInfo.setLastUpdatedItem("gpuInfo");

            logger.info("GPU信息收集完成");
        } catch (Exception e) {
            logger.error("收集GPU信息时出错: {}", e.getMessage(), e);
            throw e; // 向上抛出异常，由调用者处理
        }
    }

    /**
     * 解析CPU信息
     *
     * @param osInfo        CPU信息对象
     * @param cpuInfoOutput CPU信息命令输出
     */
    public void parseCpuInfo(OsInfo osInfo, String cpuInfoOutput) {
        if (StringUtils.isBlank(cpuInfoOutput)) {
            return;
        }

        // 解析CPU型号
        Pattern modelPattern = Pattern.compile("model name\\s*:\\s*(.+)");
        Matcher modelMatcher = modelPattern.matcher(cpuInfoOutput);
        if (modelMatcher.find()) {
            String cpuModel = modelMatcher.group(1).trim();
            osInfo.setCpuModel(cpuModel);
        }

        // 解析物理CPU数量
        int physicalId = -1;
        int cpuCount = 0;
        Pattern physicalIdPattern = Pattern.compile("physical id\\s*:\\s*(\\d+)");
        Matcher physicalIdMatcher = physicalIdPattern.matcher(cpuInfoOutput);
        while (physicalIdMatcher.find()) {
            int id = Integer.parseInt(physicalIdMatcher.group(1));
            if (id > physicalId) {
                physicalId = id;
                cpuCount = id + 1;
            }
        }
        if (cpuCount > 0) {
            osInfo.setCpuCount(cpuCount);
        } else {
            osInfo.setCpuCount(1); // 默认至少有1个物理CPU
        }

        // 解析每个CPU的核心数
        Pattern coresPattern = Pattern.compile("cpu cores\\s*:\\s*(\\d+)");
        Matcher coresMatcher = coresPattern.matcher(cpuInfoOutput);
        if (coresMatcher.find()) {
            int coresPerCpu = Integer.parseInt(coresMatcher.group(1));
            osInfo.setCpuCoresPerProcessor(coresPerCpu);
            osInfo.setCpuCoreNum(coresPerCpu * osInfo.getCpuCount());
        }

        // 解析处理器总数（逻辑核心数）
        int processorCount = 0;
        Pattern processorPattern = Pattern.compile("processor\\s*:\\s*(\\d+)");
        Matcher processorMatcher = processorPattern.matcher(cpuInfoOutput);
        while (processorMatcher.find()) {
            int id = Integer.parseInt(processorMatcher.group(1));
            if (id > processorCount) {
                processorCount = id;
            }
        }
        osInfo.setCpuLogicalCores(processorCount + 1);

        // 计算每核心线程数
        if (osInfo.getCpuCores() > 0) {
            osInfo.setCpuThreadsPerCore(osInfo.getCpuLogicalCores() / osInfo.getCpuCores());
        }

        // 解析CPU频率
        Pattern mhzPattern = Pattern.compile("cpu MHz\\s*:\\s*([\\d\\.]+)");
        Matcher mhzMatcher = mhzPattern.matcher(cpuInfoOutput);
        if (mhzMatcher.find()) {
            float mhz = Float.parseFloat(mhzMatcher.group(1));
            osInfo.setCpuFrequency(mhz / 1000.0); // 转换为GHz
        }
    }

    /**
     * 解析CPU使用率
     *
     * @param osInfo    CPU信息对象
     * @param topOutput top命令输出
     */
    public void parseCpuUsage(OsInfo osInfo, String topOutput) {
        if (StringUtils.isBlank(topOutput)) {
            return;
        }

        // top命令的%Cpu行输出格式类似: "%Cpu(s): 2.0 us, 0.7 sy, 0.0 ni, 97.3 id, 0.0 wa, 0.0 hi,
        // 0.0 si, 0.0 st"
        Pattern cpuUsagePattern = Pattern.compile("([\\d\\.]+)\\s+id");
        Matcher matcher = cpuUsagePattern.matcher(topOutput);
        if (matcher.find()) {
            String idleStr = matcher.group(1);
            double idle = Double.parseDouble(idleStr);
            double usage = 100.0 - idle;
            // 这里可以设置到osInfo对象中，如果有CPU使用率字段的话
        }
    }

    /**
     * 解析内存信息
     *
     * @param osInfo        内存信息对象
     * @param memInfoOutput 内存信息命令输出
     */
    public void parseMemoryInfo(OsInfo osInfo, String memInfoOutput) {
        if (StringUtils.isBlank(memInfoOutput)) {
            return;
        }

        // 解析总内存
        Pattern totalPattern = Pattern.compile("MemTotal:\\s+(\\d+)\\s+kB");
        Matcher totalMatcher = totalPattern.matcher(memInfoOutput);
        if (totalMatcher.find()) {
            long totalKb = Long.parseLong(totalMatcher.group(1));
            osInfo.setTotalMem(totalKb * 1024L); // 转换为字节
        }

        // 解析可用内存（MemAvailable或者计算Free+Buffers+Cached）
        Pattern availablePattern = Pattern.compile("MemAvailable:\\s+(\\d+)\\s+kB");
        Matcher availableMatcher = availablePattern.matcher(memInfoOutput);
        if (availableMatcher.find()) {
            long availableKb = Long.parseLong(availableMatcher.group(1));
            osInfo.setAvailableMem(availableKb * 1024L); // 转换为字节
        } else {
            // 如果没有MemAvailable（较老的内核版本）
            long freeKb = 0;
            long buffersKb = 0;
            long cachedKb = 0;

            Pattern freePattern = Pattern.compile("MemFree:\\s+(\\d+)\\s+kB");
            Matcher freeMatcher = freePattern.matcher(memInfoOutput);
            if (freeMatcher.find()) {
                freeKb = Long.parseLong(freeMatcher.group(1));
            }

            Pattern buffersPattern = Pattern.compile("Buffers:\\s+(\\d+)\\s+kB");
            Matcher buffersMatcher = buffersPattern.matcher(memInfoOutput);
            if (buffersMatcher.find()) {
                buffersKb = Long.parseLong(buffersMatcher.group(1));
            }

            Pattern cachedPattern = Pattern.compile("Cached:\\s+(\\d+)\\s+kB");
            Matcher cachedMatcher = cachedPattern.matcher(memInfoOutput);
            if (cachedMatcher.find()) {
                cachedKb = Long.parseLong(cachedMatcher.group(1));
            }

            osInfo.setAvailableMem((freeKb + buffersKb + cachedKb) * 1024L); // 转换为字节
        }

        // 解析交换空间
        Pattern swapTotalPattern = Pattern.compile("SwapTotal:\\s+(\\d+)\\s+kB");
        Matcher swapTotalMatcher = swapTotalPattern.matcher(memInfoOutput);
        if (swapTotalMatcher.find()) {
            long swapTotalKb = Long.parseLong(swapTotalMatcher.group(1));
            osInfo.setTotalSwap(swapTotalKb * 1024L);
        }

        Pattern swapFreePattern = Pattern.compile("SwapFree:\\s+(\\d+)\\s+kB");
        Matcher swapFreeMatcher = swapFreePattern.matcher(memInfoOutput);
        if (swapFreeMatcher.find()) {
            long swapFreeKb = Long.parseLong(swapFreeMatcher.group(1));
            osInfo.setAvailableSwap(swapFreeKb * 1024L);
        }
    }
}