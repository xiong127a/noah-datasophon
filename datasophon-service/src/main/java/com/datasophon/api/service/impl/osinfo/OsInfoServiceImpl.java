package com.datasophon.api.service.impl.osinfo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.api.service.OsInfoService;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import com.datasophon.common.model.OsInfoLegacy;
import com.datasophon.common.enums.LinuxDistribution;
import com.datasophon.common.model.hardware.NetworkInfo;
import com.datasophon.common.model.hardware.CpuInfo;
import com.datasophon.common.model.hardware.MemoryInfo;
import com.datasophon.common.model.hardware.DiskInfo;
import com.datasophon.common.model.hardware.SwapInfo;
import com.datasophon.common.model.hardware.GpuInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作系统信息服务实现类
 * 负责管理主机操作系统信息的获取和缓存
 */
@Service
public class OsInfoServiceImpl implements OsInfoService {

    private static final Logger logger = LoggerFactory.getLogger(OsInfoServiceImpl.class);

    // SSH会话缓存，以IP:PORT为键
    private final Map<String, ClientSession> sessionCache = new ConcurrentHashMap<>();

    @Autowired
    private OsInfoCollectorFactory osInfoCollectorFactory;

    // 使用Spring的ThreadPoolTaskExecutor替代原来的ExecutorService
    @Resource(name = "hardwareInfoExecutor")
    private ThreadPoolTaskExecutor hostInfoExecutor;

    // 保留缓存管理对象
    private final HostInfoCollectionQueueManager queueManager = new HostInfoCollectionQueueManager(this);

    @PostConstruct
    public void init() {
        logger.debug("=====================================================");
        logger.debug("初始化OS信息收集服务，使用硬件信息线程池");
        logger.debug("信息收集流程：");
        logger.debug("1. 同时处理最多3台主机");
        logger.debug("2. 每台主机按顺序收集全部信息（主机名、OS类型、DNS、hosts文件、CPU、内存、磁盘等）");
        logger.debug("3. 每收集完一项信息立即更新缓存");
        logger.debug("4. 一台主机收集完毕后自动开始下一台主机");
        logger.debug("=====================================================");
    }

    @PreDestroy
    public void destroy() {
        logger.info("OsInfoServiceImpl正在关闭...");
        // 清理所有缓存的SSH会话
        closeAllSessions();
    }

    /**
     * 关闭所有缓存的SSH会话
     */
    private void closeAllSessions() {
        logger.info("清理SSH连接缓存，关闭所有会话，当前缓存连接数: {}", sessionCache.size());
        for (Map.Entry<String, ClientSession> entry : sessionCache.entrySet()) {
            try {
                ClientSession session = entry.getValue();
                if (session != null && session.isOpen()) {
                    logger.info("关闭主机{}的SSH会话", entry.getKey());
                    session.close();
                }
            } catch (Exception e) {
                logger.warn("关闭SSH会话时出错: {}", e.getMessage());
            }
        }
        sessionCache.clear();
        logger.info("SSH连接缓存清理完成");
    }

    /**
     * 异步收集主机OS信息
     */
    @Override
    public void getHostOsInfoAsync(HostInfo hostInfo) {
        if (hostInfo == null) {
            return;
        }

        logger.info("主机信息收集流程：");
        logger.info("IP: {}, 开始收集信息", hostInfo.getIp());

        // 判断是否需要重置计数器（单台主机收集不重置，多台主机时重置）
        if (queueManager.totalHostCount.get() == 0) {
            // 队列为空，说明是一个新的批次，重置计数器
            queueManager.resetCounters();
            logger.info("开始新的批次收集，已重置计数器");
        }

        // 将主机添加到收集队列
        queueManager.addHostToQueue(hostInfo, null);
    }

    /**
     * 管理主机信息收集队列的类
     * 负责对多台主机的信息收集进行调度和管理
     */
    private static class HostInfoCollectionQueueManager {

        private final OsInfoServiceImpl service;

        // 待处理的主机队列
        private final Queue<HostInfo> hostQueue = new ConcurrentLinkedQueue<>();

        // 排序后的主机列表
        private final List<HostInfo> sortedHostList = new ArrayList<>();

        // 当前正在处理的主机数量
        private final AtomicInteger processingHostCount = new AtomicInteger(0);

        // 最大同时处理的主机数量
        private static final int MAX_CONCURRENT_HOSTS = 3;

        // 总共处理的主机数量
        private final AtomicInteger totalHostCount = new AtomicInteger(0);

        // 已完成处理的主机数量
        private final AtomicInteger completedHostCount = new AtomicInteger(0);

        public HostInfoCollectionQueueManager(OsInfoServiceImpl service) {
            this.service = service;
        }

        /**
         * 将主机添加到收集队列
         */
        public synchronized void addHostToQueue(HostInfo hostInfo, Consumer<HostInfo> processor) {
            if (hostInfo == null) {
                return;
            }

            // 初始化主机状态
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("等待收集主机信息...");
            service.updateHostInfoCache(hostInfo);

            // 添加到队列
            hostQueue.offer(hostInfo);

            // 添加到排序列表并增加计数
            sortedHostList.add(hostInfo);
            int total = totalHostCount.incrementAndGet();
            logger.info("已添加主机{}到收集队列，当前队列总数: {}", hostInfo.getIp(), total);

            // 开始处理队列
            startProcessingIfNeeded();
        }

        /**
         * 根据需要开始处理队列
         */
        private synchronized void startProcessingIfNeeded() {
            // 如果已经达到最大并发数，不再增加
            if (processingHostCount.get() >= MAX_CONCURRENT_HOSTS) {
                logger.debug("当前已有{}台主机正在处理中，达到最大并发数", processingHostCount.get());
                return;
            }

            // 检查队列中是否还有主机待处理
            HostInfo hostInfo = hostQueue.poll();
            if (hostInfo == null) {
                logger.debug("队列中无待处理主机");
                return;
            }

            // 增加处理计数
            processingHostCount.incrementAndGet();

            // 异步处理该主机的全部信息收集
            CompletableFuture.runAsync(() -> {
                try {
                    logger.info("开始收集主机 {} 的全部信息", hostInfo.getIp());
                    collectAllInfoForHost(hostInfo);
                } catch (Exception e) {
                    logger.error("收集主机 {} 信息时发生错误: {}", hostInfo.getIp(), e.getMessage(), e);
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("信息收集失败: " + e.getMessage());
                    service.updateHostInfoCache(hostInfo);
                } finally {
                    // 完成一台主机的处理，减少计数并增加完成计数
                    processingHostCount.decrementAndGet();
                    int completed = completedHostCount.incrementAndGet();
                    int total = totalHostCount.get();

                    // 记录进度
                    logger.info("已完成 {}/{} 台主机的信息收集",
                            completed, total);

                    // 继续处理下一台主机
                    startProcessingIfNeeded();
                }
            }, service.hostInfoExecutor);
        }

        /**
         * 为单台主机收集所有信息
         * 按顺序收集：主机名 -> OS类型 -> DNS -> Hosts文件 -> CPU -> 内存 -> 磁盘 -> 交换空间 -> GPU
         */
        private void collectAllInfoForHost(HostInfo hostInfo) {
            // 1. 收集主机名
            collectHostName(hostInfo);

            // 如果主机名收集失败，跳过后续步骤
            if (hostInfo.getHostnameStatus() != OsInfoStatusEnum.SUCCESS) {
                logger.warn("主机 {} 主机名收集失败，跳过后续信息收集", hostInfo.getIp());
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("主机名收集失败，无法继续");
                service.updateHostInfoCache(hostInfo);
                return;
            }

            // 2. 收集操作系统类型
            collectOsType(hostInfo);

            // 即使操作系统类型收集失败，也继续后续步骤
            // 注意：此时hostInfo.setOsInfoStatus可能是ERROR或SUCCESS
            // 但collectOsType方法已经保证了基本信息的设置
            logger.info("主机 {} 操作系统类型收集状态: {}, 继续收集其他信息",
                    hostInfo.getIp(), hostInfo.getOsInfoStatus());

            // 获取会话和操作系统对象
            ClientSession session = service.getOrCreateSession(hostInfo);
            OsInfo osInfo = hostInfo.getOsInfo();

            // 如果会话为空，无法继续
            if (session == null) {
                logger.error("主机 {} 无法创建SSH会话，无法继续收集", hostInfo.getIp());
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("无法创建SSH会话，无法继续");
                service.updateHostInfoCache(hostInfo);
                return;
            }

            // 如果osInfo为空，创建默认的osInfo对象
            if (osInfo == null) {
                osInfo = new OsInfo();
                osInfo.setHostname(hostInfo.getHostname());
                osInfo.setFqdn(hostInfo.getFqdn());
                osInfo.setDistributionId("linux"); // 默认假设为Linux
                osInfo.setDistribution("Unknown Linux");
                osInfo.setDistributionName("Unknown Linux");
                osInfo.setDistributionType(LinuxDistribution.OTHER);
                hostInfo.setOsInfo(osInfo);
            }

            // 使用缓存的osType字段
            String osType = osInfo.getDistributionId() != null ? osInfo.getDistributionId() : "linux"; // 默认为linux
            IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector(osType);

            // 如果无法获取合适的收集器，使用默认的Linux收集器
            if (collector == null) {
                logger.warn("主机 {} 无法找到对应的信息收集器，使用默认Linux收集器", hostInfo.getIp());
                collector = service.osInfoCollectorFactory.getCollector("linux");

                // 如果仍然无法获取收集器，这是严重错误，无法继续
                if (collector == null) {
                    logger.error("主机 {} 无法创建默认收集器，无法继续收集", hostInfo.getIp());
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("系统错误：无法创建信息收集器");
                    service.updateHostInfoCache(hostInfo);
                    return;
                }
            }

            // 3. 收集DNS信息
            collectDnsInfo(hostInfo, osInfo, session, collector);

            // 4. 收集Hosts文件
            collectHostsFile(hostInfo, osInfo, session, collector);

            // 5. 收集CPU信息
            collectCpuInfo(hostInfo, osInfo, session, collector);

            // 6. 收集内存信息
            collectMemoryInfo(hostInfo, osInfo, session, collector);

            // 7. 收集磁盘信息
            collectDiskInfo(hostInfo, osInfo, session, collector);

            // 8. 收集交换空间信息
            collectSwapInfo(hostInfo, osInfo, session, collector);

            // 9. 收集GPU信息
            collectGpuInfo(hostInfo, osInfo, session, collector);

            // 10. 收集网络接口信息
            collectNetworkInfo(hostInfo, osInfo, session, collector);

            // 全部完成，设置最终状态
            osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.SUCCESS);
            osInfo.setLastUpdatedItem("completed");
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("所有信息收集完成");
            service.updateHostInfoCache(hostInfo);

            logger.info("主机 {} 全部信息收集完成", hostInfo.getIp());
        }

        /**
         * 收集主机名
         */
        private void collectHostName(HostInfo hostInfo) {
            logger.info("开始收集主机名: {}", hostInfo.getIp());
            long startTime = System.currentTimeMillis();

            // 设置状态为正在收集
            hostInfo.setHostnameStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集主机名...");
            service.updateHostInfoCache(hostInfo);

            try {
                // 创建SSH会话
                logger.info("开始创建SSH会话：{}", hostInfo.getIp());
                ClientSession session = service.getOrCreateSession(hostInfo);

                if (session == null) {
                    throw new Exception("无法创建SSH会话");
                }

                // 检测是否为Windows系统
                boolean isWindows = false;
                try {
                    String winVerResult = MinaUtils.execCmdWithResult(session, "cmd /c ver");
                    if (StringUtils.isNotBlank(winVerResult) &&
                            (winVerResult.contains("Microsoft Windows") || winVerResult.contains("Windows"))) {
                        isWindows = true;
                    }
                } catch (Exception e) {
                    // 忽略异常，可能不是Windows系统
                }

                String hostname;
                String fqdn;

                if (isWindows) {
                    // Windows系统获取主机名
                    hostname = MinaUtils.execCmdWithResult(session, "cmd /c hostname").trim();
                    // Windows通常不使用FQDN，所以我们使用主机名作为FQDN
                    fqdn = hostname;
                    logger.info("Windows主机 {} 主机名获取成功: {}", hostInfo.getIp(), hostname);
                } else {
                    // Linux系统获取主机名
                    hostname = executeCommand(session, "hostname").trim();
                    if (hostname.isEmpty()) {
                        throw new Exception("获取主机名失败");
                    }

                    logger.info("主机名获取成功: {}, 开始获取FQDN", hostname);

                    // 获取FQDN
                    fqdn = executeCommand(session, "hostname -f").trim();
                    if (fqdn.isEmpty()) {
                        fqdn = hostname; // 如果获取FQDN失败，使用主机名作为FQDN
                    }
                }

                logger.info("FQDN获取成功: {}", fqdn);

                // 更新主机信息
                hostInfo.setHostname(hostname);
                hostInfo.setFqdn(fqdn);
                hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("主机名收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 {} 主机名收集成功：hostname={}, fqdn={}, hostnameStatus=success",
                        hostInfo.getIp(), hostname, fqdn);
                logger.info("主机 {} 主机名收集总用时: {}ms",
                        hostInfo.getIp(), System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集主机名时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("主机名收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 收集操作系统类型
         */
        private void collectOsType(HostInfo hostInfo) {
            logger.info("开始收集操作系统类型: {}", hostInfo.getIp());
            long startTime = System.currentTimeMillis();

            // 设置状态为正在收集
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集操作系统类型...");
            service.updateHostInfoCache(hostInfo);

            try {
                // 获取SSH会话
                ClientSession session = service.getOrCreateSession(hostInfo);

                if (session == null) {
                    throw new Exception("无法创建SSH会话");
                }

                // 检测操作系统类型
                boolean isWindows = false;
                try {
                    // 首先尝试Windows特有命令，避免在Linux上执行可能导致的错误
                    String winVerResult = MinaUtils.execCmdWithResult(session, "cmd /c ver");
                    if (StringUtils.isNotBlank(winVerResult) &&
                            (winVerResult.contains("Microsoft Windows") || winVerResult.contains("Windows"))) {
                        isWindows = true;
                        logger.info("检测到Windows系统: {}", winVerResult.trim());
                    }
                } catch (Exception e) {
                    logger.debug("执行Windows版本命令失败: {}", e.getMessage());
                }

                String osType = isWindows ? "windows" : "linux";
                logger.info("主机 {} 的操作系统类型为: {}", hostInfo.getIp(), osType);

                // 创建操作系统信息对象
                OsInfo osInfo = new OsInfo();
                osInfo.setHostname(hostInfo.getHostname());
                osInfo.setFqdn(hostInfo.getFqdn());

                // 为Windows系统设置特定信息
                if (isWindows) {
                    try {
                        // 获取Windows版本信息
                        String winInfo = MinaUtils.execCmdWithResult(session, "cmd /c ver");
                        if (StringUtils.isNotBlank(winInfo)) {
                            osInfo.setDistribution("Windows");
                            osInfo.setDistributionName("Windows");

                            // 解析Windows版本
                            if (winInfo.contains("Windows 10")) {
                                osInfo.setVersionId("10");
                                osInfo.setVersion("10");
                                osInfo.setFullName("Windows 10");
                            } else if (winInfo.contains("Windows 11")) {
                                osInfo.setVersionId("11");
                                osInfo.setVersion("11");
                                osInfo.setFullName("Windows 11");
                            } else if (winInfo.contains("Windows Server")) {
                                if (winInfo.contains("2016")) {
                                    osInfo.setVersionId("2016");
                                    osInfo.setVersion("2016");
                                    osInfo.setFullName("Windows Server 2016");
                                } else if (winInfo.contains("2019")) {
                                    osInfo.setVersionId("2019");
                                    osInfo.setVersion("2019");
                                    osInfo.setFullName("Windows Server 2019");
                                } else if (winInfo.contains("2022")) {
                                    osInfo.setVersionId("2022");
                                    osInfo.setVersion("2022");
                                    osInfo.setFullName("Windows Server 2022");
                                } else {
                                    osInfo.setVersionId("Server");
                                    osInfo.setVersion("Server");
                                    osInfo.setFullName("Windows Server");
                                }
                            } else {
                                // 尝试从版本字符串中提取版本号
                                if (winInfo.contains("[Version")) {
                                    String version = winInfo.substring(winInfo.indexOf("[Version") + 9);
                                    version = version.substring(0, version.indexOf("]")).trim();
                                    if (version.startsWith("10.")) {
                                        osInfo.setVersionId("10");
                                        osInfo.setVersion("10");
                                        osInfo.setFullName("Windows 10 (" + version + ")");
                                    } else if (version.startsWith("6.3")) {
                                        osInfo.setVersionId("8.1");
                                        osInfo.setVersion("8.1");
                                        osInfo.setFullName("Windows 8.1");
                                    } else if (version.startsWith("6.2")) {
                                        osInfo.setVersionId("8");
                                        osInfo.setVersion("8");
                                        osInfo.setFullName("Windows 8");
                                    } else if (version.startsWith("6.1")) {
                                        osInfo.setVersionId("7");
                                        osInfo.setVersion("7");
                                        osInfo.setFullName("Windows 7");
                                    } else {
                                        osInfo.setVersionId(version);
                                        osInfo.setVersion(version);
                                        osInfo.setFullName("Windows " + version);
                                    }
                                } else {
                                    osInfo.setVersionId("Unknown");
                                    osInfo.setVersion("Unknown");
                                    osInfo.setFullName("Windows");
                                }
                            }
                        }

                        // 获取系统架构信息
                        String archInfo = MinaUtils.execCmdWithResult(session, "cmd /c set processor_architecture");
                        if (StringUtils.isNotBlank(archInfo) && archInfo.contains("=")) {
                            String arch = archInfo.split("=")[1].trim();
                            if ("AMD64".equalsIgnoreCase(arch)) {
                                osInfo.setArchitecture("x86_64");
                            } else if ("x86".equalsIgnoreCase(arch)) {
                                osInfo.setArchitecture("x86");
                            } else if ("ARM64".equalsIgnoreCase(arch)) {
                                osInfo.setArchitecture("aarch64");
                            } else {
                                osInfo.setArchitecture(arch);
                            }
                        } else {
                            osInfo.setArchitecture("unknown");
                        }

                        // 获取内核版本（对Windows来说就是系统版本）
                        String buildInfo = MinaUtils.execCmdWithResult(session, "cmd /c ver");
                        if (StringUtils.isNotBlank(buildInfo)) {
                            // 尝试提取版本号，如 10.0.19044.2251
                            if (buildInfo.contains("[Version ")) {
                                String[] parts = buildInfo.split("\\[Version ");
                                if (parts.length > 1 && parts[1].contains("]")) {
                                    osInfo.setKernelVersion(parts[1].split("\\]")[0].trim());
                                } else {
                                    osInfo.setKernelVersion(buildInfo.trim());
                                }
                            } else {
                                osInfo.setKernelVersion(buildInfo.trim());
                            }
                        }

                        // 获取系统类型和版本的更多详细信息
                        try {
                            String sysInfo = MinaUtils.execCmdWithResult(session,
                                    "cmd /c systeminfo | findstr /B /C:\"OS\" /C:\"系统\" /C:\"注册\" /C:\"Registered\"");
                            if (StringUtils.isNotBlank(sysInfo)) {
                                // 使用distributionName存储详细系统信息，而不是使用不存在的setOsType方法
                                osInfo.setDistributionName("Windows " + sysInfo.replace("\r\n", " | "));
                            }
                        } catch (Exception e) {
                            logger.warn("获取Windows系统详细信息失败: {}", e.getMessage());
                        }
                    } catch (Exception e) {
                        logger.error("收集Windows系统信息时出错: {}", e.getMessage());
                    }

                    // 设置分发ID为windows
                    osInfo.setDistributionId("windows");
                    // 使用枚举值而不是字符串
                    osInfo.setDistributionType(LinuxDistribution.OTHER);

                    // 避免使用Linux特有的收集器
                    hostInfo.setOsInfo(osInfo);
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                    hostInfo.setMessage("操作系统类型收集成功");

                    // 初始化所有硬件收集状态
                    hostInfo.setCpuStatus(OsInfoStatusEnum.LOADING);
                    hostInfo.setMemoryStatus(OsInfoStatusEnum.LOADING);
                    hostInfo.setDiskStatus(OsInfoStatusEnum.LOADING);
                    hostInfo.setSwapStatus(OsInfoStatusEnum.LOADING);
                    hostInfo.setGpuStatus(OsInfoStatusEnum.LOADING);

                    // 设置操作系统信息收集状态
                    osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.LOADING);
                    osInfo.setLastUpdatedItem("os_info_collected");

                    // 更新缓存
                    service.updateHostInfoCache(hostInfo);
                } else {
                    // Linux系统设置默认信息
                    osInfo.setDistributionId("linux");
                    osInfo.setDistribution("Linux");
                    osInfo.setDistributionName("Linux");
                    osInfo.setDistributionType(LinuxDistribution.OTHER);

                    // 尝试获取Linux详细信息
                    try {
                        // 获取Linux发行版信息
                        String osReleaseCmd = "cat /etc/os-release 2>/dev/null || cat /etc/system-release 2>/dev/null";
                        String osReleaseInfo = MinaUtils.execCmdWithResult(session, osReleaseCmd);

                        if (StringUtils.isNotBlank(osReleaseInfo)) {
                            Pattern namePattern = Pattern.compile("NAME=[\"']?([^\"'\\n]+)[\"']?");
                            Pattern versionPattern = Pattern.compile("VERSION_ID=[\"']?([^\"'\\n]+)[\"']?");
                            Pattern idPattern = Pattern.compile("ID=[\"']?([^\"'\\n]+)[\"']?");

                            Matcher nameMatcher = namePattern.matcher(osReleaseInfo);
                            Matcher versionMatcher = versionPattern.matcher(osReleaseInfo);
                            Matcher idMatcher = idPattern.matcher(osReleaseInfo);

                            if (nameMatcher.find()) {
                                osInfo.setDistribution(nameMatcher.group(1).trim());
                                osInfo.setDistributionName(nameMatcher.group(1).trim());
                            }

                            if (versionMatcher.find()) {
                                osInfo.setVersionId(versionMatcher.group(1).trim());
                                osInfo.setVersion(versionMatcher.group(1).trim());
                            }

                            if (idMatcher.find()) {
                                String distroId = idMatcher.group(1).trim().toLowerCase();
                                osInfo.setDistributionId(distroId);

                                // 设置发行版类型
                                if (distroId.contains("centos")) {
                                    osInfo.setDistributionType(LinuxDistribution.CENTOS);
                                } else if (distroId.contains("redhat") || distroId.contains("rhel")) {
                                    osInfo.setDistributionType(LinuxDistribution.REDHAT);
                                } else if (distroId.contains("ubuntu")) {
                                    osInfo.setDistributionType(LinuxDistribution.UBUNTU);
                                } else if (distroId.contains("debian")) {
                                    osInfo.setDistributionType(LinuxDistribution.DEBIAN);
                                } else if (distroId.contains("kylin") || distroId.contains("neokylin")) {
                                    osInfo.setDistributionType(LinuxDistribution.KYLIN);
                                } else {
                                    osInfo.setDistributionType(LinuxDistribution.OTHER);
                                }
                            }
                        } else {
                            // 尝试其他方法，例如/etc/redhat-release
                            String redhatReleaseCmd = "cat /etc/redhat-release 2>/dev/null";
                            String redhatReleaseInfo = MinaUtils.execCmdWithResult(session, redhatReleaseCmd);

                            if (StringUtils.isNotBlank(redhatReleaseInfo)) {
                                if (redhatReleaseInfo.toLowerCase().contains("centos")) {
                                    osInfo.setDistribution("CentOS");
                                    osInfo.setDistributionName("CentOS");
                                    osInfo.setDistributionId("centos");
                                    osInfo.setDistributionType(LinuxDistribution.CENTOS);
                                } else if (redhatReleaseInfo.toLowerCase().contains("red hat")) {
                                    osInfo.setDistribution("Red Hat Enterprise Linux");
                                    osInfo.setDistributionName("Red Hat Enterprise Linux");
                                    osInfo.setDistributionId("rhel");
                                    osInfo.setDistributionType(LinuxDistribution.REDHAT);
                                }

                                // 尝试提取版本
                                Pattern versionPattern = Pattern.compile("(\\d+(\\.\\d+)?)");
                                Matcher matcher = versionPattern.matcher(redhatReleaseInfo);
                                if (matcher.find()) {
                                    osInfo.setVersionId(matcher.group(1));
                                    osInfo.setVersion(matcher.group(1));
                                }
                            }
                        }

                        // 获取内核版本
                        String kernelCmd = "uname -r";
                        String kernelInfo = MinaUtils.execCmdWithResult(session, kernelCmd);
                        if (StringUtils.isNotBlank(kernelInfo)) {
                            osInfo.setKernelVersion(kernelInfo.trim());
                        }

                        // 获取系统架构
                        String archCmd = "uname -m";
                        String archInfo = MinaUtils.execCmdWithResult(session, archCmd);
                        if (StringUtils.isNotBlank(archInfo)) {
                            osInfo.setArchitecture(archInfo.trim());
                        }
                    } catch (Exception e) {
                        logger.warn("获取Linux详细信息失败，使用默认值: {}", e.getMessage());
                    }

                    // 即使Linux发行版详细信息收集失败，仍设置基本信息并继续
                    hostInfo.setOsInfo(osInfo);
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                    hostInfo.setMessage("操作系统类型收集成功");

                    // 初始化所有硬件收集状态
                    hostInfo.setCpuStatus(OsInfoStatusEnum.LOADING);
                    hostInfo.setMemoryStatus(OsInfoStatusEnum.LOADING);
                    hostInfo.setDiskStatus(OsInfoStatusEnum.LOADING);
                    hostInfo.setSwapStatus(OsInfoStatusEnum.LOADING);
                    hostInfo.setGpuStatus(OsInfoStatusEnum.LOADING);

                    // 设置操作系统信息收集状态
                    osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.LOADING);
                    osInfo.setLastUpdatedItem("os_info_collected");

                    // 更新缓存
                    service.updateHostInfoCache(hostInfo);
                }

                logger.info("主机 {} 操作系统信息收集成功：{}，已设置状态：osInfoStatus=success",
                        hostInfo.getIp(), hostInfo.getOsInfoStatus());
                logger.info("主机 {} 操作系统类型收集总用时: {}ms",
                        hostInfo.getIp(), System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集操作系统类型时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);

                // 创建默认的操作系统信息对象
                OsInfo osInfo = new OsInfo();
                osInfo.setHostname(hostInfo.getHostname());
                osInfo.setFqdn(hostInfo.getFqdn());
                osInfo.setDistributionId("linux"); // 默认假设为Linux
                osInfo.setDistribution("Unknown Linux");
                osInfo.setDistributionName("Unknown Linux");
                osInfo.setDistributionType(LinuxDistribution.OTHER);
                osInfo.setVersionId("unknown");
                osInfo.setVersion("unknown");

                // 设置主机信息
                hostInfo.setOsInfo(osInfo);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS); // 更改为SUCCESS让流程继续
                hostInfo.setMessage("操作系统类型收集基本成功，但详细信息可能不完整");

                // 初始化所有硬件收集状态
                hostInfo.setCpuStatus(OsInfoStatusEnum.LOADING);
                hostInfo.setMemoryStatus(OsInfoStatusEnum.LOADING);
                hostInfo.setDiskStatus(OsInfoStatusEnum.LOADING);
                hostInfo.setSwapStatus(OsInfoStatusEnum.LOADING);
                hostInfo.setGpuStatus(OsInfoStatusEnum.LOADING);

                // 设置操作系统信息收集状态
                osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.LOADING);
                osInfo.setLastUpdatedItem("os_info_collected_partially");

                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 收集DNS信息
         */
        private void collectDnsInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            logger.info("开始收集DNS信息: {}", hostInfo.getIp());
            long startTime = System.currentTimeMillis();

            // 设置状态为正在收集
            hostInfo.setMessage("正在收集DNS信息...");
            service.updateHostInfoCache(hostInfo);

            try {
                // 收集DNS服务器信息 - 使用命令直接收集
                String dnsServers;
                if ("windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集DNS
                    try {
                        dnsServers = MinaUtils.execCmdWithResult(session,
                                "cmd /c powershell -command \"Get-DnsClientServerAddress | Select-Object -ExpandProperty ServerAddresses | ForEach-Object { $_ }\"");

                        // 如果PowerShell命令失败，尝试使用ipconfig
                        if (StringUtils.isBlank(dnsServers) || dnsServers.toLowerCase().contains("error")) {
                            logger.info("使用PowerShell获取DNS失败，尝试使用ipconfig");
                            String ipconfig = MinaUtils.execCmdWithResult(session, "cmd /c ipconfig /all");

                            // 简单解析ipconfig输出以提取DNS服务器
                            dnsServers = extractDnsFromIpconfig(ipconfig);
                        }
                    } catch (Exception e) {
                        logger.warn("Windows获取DNS错误: {}", e.getMessage());
                        dnsServers = "无法获取DNS信息";
                    }
                } else {
                    // Linux系统收集DNS
                    try {
                        dnsServers = MinaUtils.execCmdWithResult(session,
                                "cat /etc/resolv.conf | grep nameserver | awk '{print $2}'");
                    } catch (Exception e) {
                        logger.warn("Linux获取DNS错误: {}", e.getMessage());
                        dnsServers = "无法获取DNS信息";
                    }
                }

                // 修复setDnsServers方法的参数类型问题
                if (StringUtils.isNotBlank(dnsServers)) {
                    // 更新操作系统信息
                    List<String> dnsServerList = new ArrayList<>();
                    for (String server : dnsServers.split("\\n")) {
                        if (StringUtils.isNotBlank(server)) {
                            dnsServerList.add(server.trim());
                        }
                    }
                    osInfo.setDnsServers(dnsServerList);
                    osInfo.setLastUpdatedItem("dns_collected");
                    hostInfo.setMessage("DNS信息收集成功");
                    service.updateHostInfoCache(hostInfo);
                }

                logger.info("主机 {} DNS信息收集成功，耗时 {}ms",
                        hostInfo.getIp(), System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集DNS信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setMessage("DNS信息收集失败: " + e.getMessage() + "，继续收集其他信息");
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 从ipconfig输出中提取DNS服务器信息
         */
        private String extractDnsFromIpconfig(String ipconfig) {
            if (StringUtils.isBlank(ipconfig)) {
                return "";
            }

            StringBuilder dnsServers = new StringBuilder();
            String[] lines = ipconfig.split("[\r\n]+");
            boolean dnsSection = false;

            for (String line : lines) {
                // 判断是否进入了DNS配置部分
                if (line.contains("DNS Servers") || line.contains("DNS 服务器")) {
                    dnsSection = true;

                    // 处理同一行中的IP地址
                    String[] parts = line.split(":");
                    if (parts.length > 1 && parts[1].trim().matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        dnsServers.append(parts[1].trim()).append("\n");
                    }
                    continue;
                }

                // 如果在DNS部分且当前行是缩进的IP地址
                if (dnsSection && line.trim().matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    dnsServers.append(line.trim()).append("\n");
                } else if (dnsSection && !line.trim().isEmpty() && !line.contains(":")) {
                    // 如果遇到了不是IP地址的行，且不是空行和冒号行，结束DNS部分
                    dnsSection = false;
                }
            }

            return dnsServers.toString();
        }

        /**
         * 收集Hosts文件
         */
        private void collectHostsFile(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            logger.info("开始收集Hosts文件: {}", hostInfo.getIp());
            long startTime = System.currentTimeMillis();

            // 设置状态为正在收集
            hostInfo.setMessage("正在收集Hosts文件...");
            service.updateHostInfoCache(hostInfo);

            try {
                // 收集Hosts文件内容 - 使用命令直接收集
                String hostsFile;
                if ("windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集hosts
                    try {
                        hostsFile = MinaUtils.execCmdWithResult(session,
                                "cmd /c type C:\\Windows\\System32\\drivers\\etc\\hosts");

                        // 如果type命令失败，尝试使用PowerShell
                        if (StringUtils.isBlank(hostsFile) || hostsFile.toLowerCase().contains("error")) {
                            logger.info("使用type命令获取hosts文件失败，尝试使用PowerShell");
                            hostsFile = MinaUtils.execCmdWithResult(session,
                                    "cmd /c powershell -command \"Get-Content C:\\Windows\\System32\\drivers\\etc\\hosts\"");
                        }
                    } catch (Exception e) {
                        logger.warn("Windows获取hosts文件错误: {}", e.getMessage());
                        hostsFile = "无法获取hosts文件内容";
                    }
                } else {
                    // Linux系统收集hosts
                    try {
                        hostsFile = MinaUtils.execCmdWithResult(session, "cat /etc/hosts");
                    } catch (Exception e) {
                        logger.warn("Linux获取hosts文件错误: {}", e.getMessage());
                        hostsFile = "无法获取hosts文件内容";
                    }
                }

                // 更新操作系统信息
                hostInfo.setMessage("Hosts文件收集成功");
                osInfo.setLastUpdatedItem("hosts_collected");
                hostInfo.setHostsFile(hostsFile);
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 {} Hosts文件收集成功，耗时 {}ms",
                        hostInfo.getIp(), System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集Hosts文件时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setMessage("Hosts文件收集失败: " + e.getMessage() + "，继续收集其他信息");
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 收集CPU信息
         */
        private void collectCpuInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            logger.info("开始收集CPU信息: {}", hostInfo.getIp());
            long startTime = System.currentTimeMillis();

            // 设置状态为正在收集
            osInfo.setLastUpdatedItem("collecting_cpu");
            hostInfo.setCpuStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集CPU信息...");
            service.updateHostInfoCache(hostInfo);

            try {
                // 直接使用系统收集器的方法进行CPU信息收集
                if (osInfo.getDistributionId() != null &&
                        "windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集CPU信息
                    String cpuInfoCmd = "cmd /c wmic cpu get Name, NumberOfCores, NumberOfLogicalProcessors /Value";
                    String cpuInfo = MinaUtils.execCmdWithResult(session, cpuInfoCmd);

                    if (StringUtils.isNotBlank(cpuInfo)) {
                        // 解析Windows CPU信息
                        parseWindowsCpuInfo(osInfo, cpuInfo);
                    }
                } else {
                    // Linux系统收集CPU信息
                    String cpuInfoCmd = "cat /proc/cpuinfo";
                    String cpuInfo = MinaUtils.execCmdWithResult(session, cpuInfoCmd);

                    if (StringUtils.isNotBlank(cpuInfo)) {
                        // 解析Linux CPU信息
                        parseLinuxCpuInfo(osInfo, cpuInfo);
                    }
                }

                // 更新状态
                hostInfo.setCpuStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("CPU信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 {} CPU信息收集成功：{}，{}核，耗时 {}ms",
                        hostInfo.getIp(),
                        osInfo.getCpuInfo() != null ? osInfo.getCpuInfo().getModel() : "Unknown",
                        osInfo.getCpuInfo() != null ? osInfo.getCpuInfo().getCores() : 0,
                        System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集CPU信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                osInfo.setLastUpdatedItem("CPU收集失败");
                hostInfo.setCpuStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("CPU信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 解析Linux CPU信息
         */
        private void parseLinuxCpuInfo(OsInfo osInfo, String cpuInfo) {
            // 确保CpuInfo对象存在
            if (osInfo.getCpuInfo() == null) {
                osInfo.setCpuInfo(new CpuInfo());
            }
            CpuInfo cpuInfoObj = osInfo.getCpuInfo();

            // 解析物理CPU数量
            int physicalId = -1;
            int cpuCount = 0;

            // 解析CPU型号
            String model = "";

            // 解析核心数
            int cores = 0;

            String[] lines = cpuInfo.split("\n");
            for (String line : lines) {
                if (line.contains("physical id")) {
                    int id = Integer.parseInt(line.split(":")[1].trim());
                    if (id > physicalId) {
                        physicalId = id;
                        cpuCount = physicalId + 1;
                    }
                } else if (line.contains("model name") && model.isEmpty()) {
                    model = line.split(":")[1].trim();
                } else if (line.contains("cpu cores") && cores == 0) {
                    cores = Integer.parseInt(line.split(":")[1].trim());
                }
            }

            if (cpuCount == 0)
                cpuCount = 1; // 至少有一个CPU
            if (cores == 0) {
                // 尝试计算逻辑处理器数量
                int processors = 0;
                for (String line : lines) {
                    if (line.contains("processor")) {
                        processors++;
                    }
                }
                cores = processors > 0 ? processors / cpuCount : 1;
            }

            // 设置CPU信息
            cpuInfoObj.setModel(model);
            cpuInfoObj.setCores(cores);
            cpuInfoObj.setPhysicalCount(cpuCount);
            cpuInfoObj.setLogicalCores(cores * cpuCount);
        }

        /**
         * 解析Windows CPU信息
         */
        private void parseWindowsCpuInfo(OsInfo osInfo, String cpuInfo) {
            // 确保CpuInfo对象存在
            if (osInfo.getCpuInfo() == null) {
                osInfo.setCpuInfo(new CpuInfo());
            }
            CpuInfo cpuInfoObj = osInfo.getCpuInfo();

            String model = "";
            int cores = 0;
            int logicalProcessors = 0;

            String[] lines = cpuInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("Name=")) {
                    model = line.substring(5).trim();
                } else if (line.startsWith("NumberOfCores=")) {
                    cores = Integer.parseInt(line.substring(14).trim());
                } else if (line.startsWith("NumberOfLogicalProcessors=")) {
                    logicalProcessors = Integer.parseInt(line.substring(26).trim());
                }
            }

            // 设置CPU信息
            cpuInfoObj.setModel(model);
            cpuInfoObj.setCores(cores);
            cpuInfoObj.setLogicalCores(logicalProcessors);
            cpuInfoObj.setPhysicalCount(cores > 0 ? logicalProcessors / cores : 1);
        }

        /**
         * 收集内存信息
         */
        private void collectMemoryInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            logger.info("开始收集内存信息: {}", hostInfo.getIp());
            long startTime = System.currentTimeMillis();

            // 设置状态为正在收集
            osInfo.setLastUpdatedItem("collecting_memory");
            hostInfo.setMemoryStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集内存信息...");
            service.updateHostInfoCache(hostInfo);

            try {
                // 直接使用命令收集内存信息
                if (osInfo.getDistributionId() != null &&
                        "windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集内存信息
                    String memInfoCmd = "cmd /c wmic OS get TotalVisibleMemorySize, FreePhysicalMemory /Value";
                    String memInfo = MinaUtils.execCmdWithResult(session, memInfoCmd);

                    if (StringUtils.isNotBlank(memInfo)) {
                        // 解析Windows内存信息
                        parseWindowsMemoryInfo(osInfo, memInfo);
                    }
                } else {
                    // Linux系统收集内存信息
                    String memInfoCmd = "cat /proc/meminfo";
                    String memInfo = MinaUtils.execCmdWithResult(session, memInfoCmd);

                    if (StringUtils.isNotBlank(memInfo)) {
                        // 解析Linux内存信息
                        parseLinuxMemoryInfo(osInfo, memInfo);
                    }
                }

                // 更新状态
                hostInfo.setMemoryStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("内存信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 {} 内存信息收集成功：总内存 {}GB，可用内存 {}GB，耗时 {}ms",
                        hostInfo.getIp(),
                        osInfo.getMemoryInfo() != null
                                ? String.format("%.2f", osInfo.getMemoryInfo().getTotalMemory() / 1024.0)
                                : "0",
                        osInfo.getMemoryInfo() != null
                                ? String.format("%.2f", osInfo.getMemoryInfo().getAvailableMemory() / 1024.0)
                                : "0",
                        System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集内存信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                osInfo.setLastUpdatedItem("内存收集失败");
                hostInfo.setMemoryStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("内存信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 解析Linux内存信息
         */
        private void parseLinuxMemoryInfo(OsInfo osInfo, String memInfo) {
            // 确保MemoryInfo对象存在
            if (osInfo.getMemoryInfo() == null) {
                osInfo.setMemoryInfo(new MemoryInfo());
            }
            MemoryInfo memoryInfo = osInfo.getMemoryInfo();

            long totalMem = 0;
            long availableMem = 0;

            String[] lines = memInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("MemTotal:")) {
                    // MemTotal以KB为单位
                    totalMem = Long.parseLong(line.replaceAll("[^0-9]", "")) / 1024; // 转换为MB
                } else if (line.startsWith("MemAvailable:") || line.startsWith("MemFree:") && availableMem == 0) {
                    // 首选MemAvailable，其次使用MemFree，转换为MB
                    availableMem = Long.parseLong(line.replaceAll("[^0-9]", "")) / 1024;
                }
            }

            // 设置内存信息
            memoryInfo.setTotalMemory(totalMem);
            memoryInfo.setAvailableMemory(availableMem);

            // 计算使用率
            if (totalMem > 0) {
                double usedMemory = totalMem - availableMem;
                double usagePercent = (usedMemory / totalMem) * 100;
                memoryInfo.setUsagePercent(Math.round(usagePercent * 10) / 10.0);
            }
        }

        /**
         * 解析Windows内存信息
         */
        private void parseWindowsMemoryInfo(OsInfo osInfo, String memInfo) {
            // 确保MemoryInfo对象存在
            if (osInfo.getMemoryInfo() == null) {
                osInfo.setMemoryInfo(new MemoryInfo());
            }
            MemoryInfo memoryInfo = osInfo.getMemoryInfo();

            long totalMem = 0;
            long availableMem = 0;

            String[] lines = memInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("TotalVisibleMemorySize=")) {
                    // Windows中为KB单位，转换为MB
                    totalMem = Long.parseLong(line.substring(23).trim()) / 1024;
                } else if (line.startsWith("FreePhysicalMemory=")) {
                    availableMem = Long.parseLong(line.substring(19).trim()) / 1024;
                }
            }

            // 设置内存信息
            memoryInfo.setTotalMemory(totalMem);
            memoryInfo.setAvailableMemory(availableMem);

            // 计算使用率
            if (totalMem > 0) {
                double usedMemory = totalMem - availableMem;
                double usagePercent = (usedMemory / totalMem) * 100;
                memoryInfo.setUsagePercent(Math.round(usagePercent * 10) / 10.0);
            }
        }

        /**
         * 收集磁盘信息
         */
        private void collectDiskInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            logger.info("开始收集磁盘信息: {}", hostInfo.getIp());
            long startTime = System.currentTimeMillis();

            // 设置状态为正在收集
            osInfo.setLastUpdatedItem("collecting_disk");
            hostInfo.setDiskStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集磁盘信息...");
            service.updateHostInfoCache(hostInfo);

            try {
                // 直接使用命令收集磁盘信息
                if (osInfo.getDistributionId() != null &&
                        "windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集磁盘信息
                    String diskInfoCmd = "cmd /c wmic logicaldisk where DeviceID='C:' get Size,FreeSpace /Value";
                    String diskInfo = MinaUtils.execCmdWithResult(session, diskInfoCmd);

                    if (StringUtils.isNotBlank(diskInfo)) {
                        // 解析Windows磁盘信息
                        parseWindowsDiskInfo(osInfo, diskInfo);
                    }
                } else {
                    // Linux系统收集磁盘信息
                    String diskInfoCmd = "df -P -k /";
                    String diskInfo = MinaUtils.execCmdWithResult(session, diskInfoCmd);

                    if (StringUtils.isNotBlank(diskInfo)) {
                        // 解析Linux磁盘信息
                        parseLinuxDiskInfo(osInfo, diskInfo);
                    }
                }

                // 更新状态
                hostInfo.setDiskStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("磁盘信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 {} 磁盘信息收集成功：总磁盘 {}GB，可用磁盘 {}GB，耗时 {}ms",
                        hostInfo.getIp(),
                        osInfo.getDiskInfo() != null ? String.format("%.2f", osInfo.getDiskInfo().getTotalDiskSpace())
                                : "0",
                        osInfo.getDiskInfo() != null
                                ? String.format("%.2f", osInfo.getDiskInfo().getAvailableDiskSpace())
                                : "0",
                        System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集磁盘信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                osInfo.setLastUpdatedItem("磁盘收集失败");
                hostInfo.setDiskStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("磁盘信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 解析Linux磁盘信息
         */
        private void parseLinuxDiskInfo(OsInfo osInfo, String diskInfo) {
            // 确保DiskInfo对象存在
            if (osInfo.getDiskInfo() == null) {
                osInfo.setDiskInfo(new DiskInfo());
            }
            DiskInfo diskInfoObj = osInfo.getDiskInfo();

            String[] lines = diskInfo.split("\n");
            if (lines.length >= 2) {
                String[] parts = lines[1].trim().split("\\s+");
                if (parts.length >= 4) {
                    // df -k输出的单位是KB
                    double totalGB = Double.parseDouble(parts[1].trim()) / (1024 * 1024); // 转换为GB
                    double usedGB = Double.parseDouble(parts[2].trim()) / (1024 * 1024); // 转换为GB
                    double availGB = Double.parseDouble(parts[3].trim()) / (1024 * 1024); // 转换为GB

                    // 设置磁盘信息
                    diskInfoObj.setTotalDiskSpace(totalGB);
                    diskInfoObj.setUsedDiskSpace(usedGB);
                    diskInfoObj.setAvailableDiskSpace(availGB);

                    // 计算使用率
                    if (totalGB > 0) {
                        double usagePercent = (usedGB / totalGB) * 100;
                        diskInfoObj.setUsagePercent(Math.round(usagePercent * 10) / 10.0);
                    }
                }
            }
        }

        /**
         * 解析Windows磁盘信息
         */
        private void parseWindowsDiskInfo(OsInfo osInfo, String diskInfo) {
            // 确保DiskInfo对象存在
            if (osInfo.getDiskInfo() == null) {
                osInfo.setDiskInfo(new DiskInfo());
            }
            DiskInfo diskInfoObj = osInfo.getDiskInfo();

            double totalSize = 0;
            double freeSpace = 0;

            String[] lines = diskInfo.split("[\r\n]+");
            for (String line : lines) {
                if (line.startsWith("Size=")) {
                    totalSize = Double.parseDouble(line.substring(5).trim()) / (1024 * 1024 * 1024); // 转换为GB
                } else if (line.startsWith("FreeSpace=")) {
                    freeSpace = Double.parseDouble(line.substring(10).trim()) / (1024 * 1024 * 1024); // 转换为GB
                }
            }

            // 设置磁盘信息
            diskInfoObj.setTotalDiskSpace(totalSize);
            diskInfoObj.setAvailableDiskSpace(freeSpace);
            diskInfoObj.setUsedDiskSpace(totalSize - freeSpace);

            // 计算使用率
            if (totalSize > 0) {
                double usagePercent = ((totalSize - freeSpace) / totalSize) * 100;
                diskInfoObj.setUsagePercent(Math.round(usagePercent * 10) / 10.0);
            }
        }

        /**
         * 收集交换空间信息
         */
        private void collectSwapInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            logger.info("开始收集交换空间信息: {}", hostInfo.getIp());
            long startTime = System.currentTimeMillis();

            // 设置状态为正在收集
            osInfo.setLastUpdatedItem("collecting_swap");
            hostInfo.setSwapStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集交换空间信息...");
            service.updateHostInfoCache(hostInfo);

            try {
                // 检查是否Linux系统，Windows不收集交换空间
                if (osInfo.getDistributionId() != null &&
                        !"windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // 收集交换空间信息
                    String swapInfoCmd = "grep Swap /proc/meminfo";
                    String swapInfo = MinaUtils.execCmdWithResult(session, swapInfoCmd);

                    if (StringUtils.isNotBlank(swapInfo)) {
                        // 解析Linux交换空间信息
                        parseLinuxSwapInfo(osInfo, swapInfo);
                    }
                } else {
                    // Windows系统使用页面文件作为交换空间
                    String pagingFileCmd = "cmd /c wmic pagefile get CurrentUsage, AllocatedBaseSize /Value";
                    String pagingInfo = MinaUtils.execCmdWithResult(session, pagingFileCmd);

                    if (StringUtils.isNotBlank(pagingInfo)) {
                        // 解析Windows页面文件信息
                        parseWindowsPagefileInfo(osInfo, pagingInfo);
                    } else {
                        logger.info("Windows系统无法获取页面文件信息: {}", hostInfo.getIp());
                    }
                }

                // 更新状态
                hostInfo.setSwapStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("交换空间信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 {} 交换空间信息收集成功：总交换空间 {}GB，可用交换空间 {}GB，耗时 {}ms",
                        hostInfo.getIp(),
                        osInfo.getSwapInfo() != null
                                ? String.format("%.2f", osInfo.getSwapInfo().getTotalSwap() / 1024.0)
                                : "0",
                        osInfo.getSwapInfo() != null
                                ? String.format("%.2f", osInfo.getSwapInfo().getAvailableSwap() / 1024.0)
                                : "0",
                        System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集交换空间信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                osInfo.setLastUpdatedItem("交换空间收集失败");
                hostInfo.setSwapStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("交换空间信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 解析Linux交换空间信息
         */
        private void parseLinuxSwapInfo(OsInfo osInfo, String swapInfo) {
            // 确保SwapInfo对象存在
            if (osInfo.getSwapInfo() == null) {
                osInfo.setSwapInfo(new SwapInfo());
            }
            SwapInfo swapInfoObj = osInfo.getSwapInfo();

            long swapTotal = 0;
            long swapFree = 0;

            String[] lines = swapInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("SwapTotal:")) {
                    // SwapTotal以KB为单位，转换为MB
                    swapTotal = Long.parseLong(line.replaceAll("[^0-9]", "")) / 1024;
                } else if (line.startsWith("SwapFree:")) {
                    // SwapFree以KB为单位，转换为MB
                    swapFree = Long.parseLong(line.replaceAll("[^0-9]", "")) / 1024;
                }
            }

            // 设置交换空间信息
            swapInfoObj.setTotalSwap(swapTotal);
            swapInfoObj.setAvailableSwap(swapFree);

            // 检查交换空间是否启用
            swapInfoObj.setEnabled(swapTotal > 0);

            // 计算使用率
            if (swapTotal > 0) {
                long usedSwap = swapTotal - swapFree;
                double usagePercent = ((double) usedSwap / swapTotal) * 100;
                swapInfoObj.setUsagePercent(Math.round(usagePercent * 10) / 10.0);
            }
        }

        /**
         * 解析Windows页面文件信息
         */
        private void parseWindowsPagefileInfo(OsInfo osInfo, String pagingInfo) {
            // 确保SwapInfo对象存在
            if (osInfo.getSwapInfo() == null) {
                osInfo.setSwapInfo(new SwapInfo());
            }
            SwapInfo swapInfoObj = osInfo.getSwapInfo();

            long totalPageFile = 0;
            long usedPageFile = 0;

            String[] lines = pagingInfo.split("[\r\n]+");
            for (String line : lines) {
                if (line.startsWith("AllocatedBaseSize=")) {
                    try {
                        // 页面文件大小以MB为单位
                        totalPageFile = Long.parseLong(line.substring(18).trim());
                    } catch (NumberFormatException e) {
                        logger.warn("解析Windows页面文件总大小失败: {}", e.getMessage());
                    }
                } else if (line.startsWith("CurrentUsage=")) {
                    try {
                        // 当前使用的页面文件以MB为单位
                        usedPageFile = Long.parseLong(line.substring(13).trim());
                    } catch (NumberFormatException e) {
                        logger.warn("解析Windows页面文件使用量失败: {}", e.getMessage());
                    }
                }
            }

            // 设置交换空间信息
            swapInfoObj.setTotalSwap(totalPageFile);
            swapInfoObj.setAvailableSwap(totalPageFile > usedPageFile ? totalPageFile - usedPageFile : 0);
            swapInfoObj.setEnabled(totalPageFile > 0);

            // 计算使用率
            if (totalPageFile > 0) {
                double usagePercent = ((double) usedPageFile / totalPageFile) * 100;
                swapInfoObj.setUsagePercent(Math.round(usagePercent * 10) / 10.0);
            }
        }

        /**
         * 收集GPU信息
         */
        private void collectGpuInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            logger.info("开始收集GPU信息: {}", hostInfo.getIp());
            long startTime = System.currentTimeMillis();

            // 设置状态为正在收集
            osInfo.setLastUpdatedItem("collecting_gpu");
            hostInfo.setGpuStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集GPU信息...");
            service.updateHostInfoCache(hostInfo);

            try {
                if (osInfo.getDistributionId() != null && "windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集GPU信息
                    String gpuInfoCmd = "powershell -command \"Get-WmiObject Win32_VideoController | Select-Object Name, AdapterRAM, DriverVersion | ConvertTo-Json\"";
                    String gpuInfo = MinaUtils.execCmdWithResult(session, gpuInfoCmd);

                    if (StringUtils.isNotBlank(gpuInfo)) {
                        // 解析Windows GPU信息
                        parseWindowsGpuInfo(osInfo, gpuInfo);

                        // 如果是NVIDIA GPU，尝试获取显存使用情况
                        if (osInfo.getGpuInfo() != null && osInfo.getGpuInfo().getModel() != null &&
                                osInfo.getGpuInfo().getModel().toLowerCase().contains("nvidia")) {
                            try {
                                // 尝试使用nvidia-smi获取显存使用情况
                                String nvidiaSmiCmd = "nvidia-smi --query-gpu=memory.total,memory.used --format=csv,noheader,nounits";
                                String nvidiaSmiResult = MinaUtils.execCmdWithResult(session, nvidiaSmiCmd);

                                if (StringUtils.isNotBlank(nvidiaSmiResult)) {
                                    parseNvidiaGpuMemory(osInfo, nvidiaSmiResult);
                                }
                            } catch (Exception e) {
                                logger.warn("获取NVIDIA显存使用情况失败: {}", e.getMessage());
                            }
                        }
                    }
                } else {
                    // Linux系统收集GPU信息
                    // 先尝试nvidia-smi命令获取NVIDIA GPU信息
                    try {
                        String nvidiaSmiCmd = "nvidia-smi --query-gpu=name,memory.total,memory.used --format=csv,noheader";
                        String nvidiaSmiResult = MinaUtils.execCmdWithResult(session, nvidiaSmiCmd);

                        if (StringUtils.isNotBlank(nvidiaSmiResult) && !nvidiaSmiResult.contains("not found")) {
                            parseLinuxNvidiaGpuInfo(osInfo, nvidiaSmiResult);
                        } else {
                            // 尝试使用lspci命令获取GPU信息
                            String lspciCmd = "lspci | grep -i vga";
                            String lspciResult = MinaUtils.execCmdWithResult(session, lspciCmd);

                            if (StringUtils.isNotBlank(lspciResult)) {
                                parseLinuxGpuInfo(osInfo, lspciResult);

                                // 如果是NVIDIA GPU，尝试设置显存（虽然lspci显示了NVIDIA但nvidia-smi失败的情况）
                                if (osInfo.getGpuInfo() != null
                                        && osInfo.getGpuInfo().getInfo() != null
                                        && osInfo.getGpuInfo().getInfo().toLowerCase().contains("nvidia")) {
                                    // 创建和设置GPU对象
                                    GpuInfo gpuInfo = new GpuInfo();
                                    gpuInfo.setModel("未检测到GPU设备");
                                    gpuInfo.setInfo("未检测到GPU设备");
                                    gpuInfo.setMemorySize(0.0);
                                    osInfo.setGpuInfo(gpuInfo);
                                }
                            } else {
                                // 创建和设置GPU对象
                                GpuInfo gpuInfo = new GpuInfo();
                                gpuInfo.setModel("未检测到GPU设备");
                                gpuInfo.setInfo("未检测到GPU设备");
                                gpuInfo.setMemorySize(0.0);
                                osInfo.setGpuInfo(gpuInfo);
                            }
                        }
                    } catch (Exception e) {
                        // 如果nvidia-smi命令失败，尝试使用lspci命令
                        try {
                            String lspciCmd = "lspci | grep -i vga";
                            String lspciResult = MinaUtils.execCmdWithResult(session, lspciCmd);

                            if (StringUtils.isNotBlank(lspciResult)) {
                                parseLinuxGpuInfo(osInfo, lspciResult);
                            } else {
                                // 创建和设置GPU对象
                                GpuInfo gpuInfo = new GpuInfo();
                                gpuInfo.setModel("未检测到GPU设备");
                                gpuInfo.setInfo("未检测到GPU设备");
                                gpuInfo.setMemorySize(0.0);
                                osInfo.setGpuInfo(gpuInfo);
                            }
                        } catch (Exception ex) {
                            logger.error("获取Linux GPU信息失败", ex);
                            // 创建和设置GPU对象
                            GpuInfo gpuInfo = new GpuInfo();
                            gpuInfo.setModel("获取GPU信息失败: " + ex.getMessage());
                            gpuInfo.setInfo("获取GPU信息失败: " + ex.getMessage());
                            gpuInfo.setMemorySize(0.0);
                            osInfo.setGpuInfo(gpuInfo);
                        }
                    }
                }

                // 更新状态
                hostInfo.setGpuStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("GPU信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("GPU信息收集完成: {}, 用时: {}ms, GPU型号: {}",
                        hostInfo.getIp(), (System.currentTimeMillis() - startTime),
                        osInfo.getGpuInfo() != null && osInfo.getGpuInfo().getModel() != null
                                ? osInfo.getGpuInfo().getModel()
                                : "未知");
            } catch (Exception e) {
                logger.error("收集GPU信息失败: {}", hostInfo.getIp(), e);
                hostInfo.setGpuStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("GPU信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);

                // 创建和设置GPU对象
                GpuInfo gpuInfo = new GpuInfo();
                gpuInfo.setModel("收集GPU信息失败: " + e.getMessage());
                gpuInfo.setInfo("收集GPU信息失败: " + e.getMessage());
                gpuInfo.setMemorySize(0.0);
                osInfo.setGpuInfo(gpuInfo);
            }
        }

        /**
         * 解析NVIDIA GPU显存信息
         */
        private void parseNvidiaGpuMemory(OsInfo osInfo, String nvidiaSmiResult) {
            try {
                // 确保GpuInfo对象存在
                if (osInfo.getGpuInfo() == null) {
                    osInfo.setGpuInfo(new GpuInfo());
                }
                GpuInfo gpuInfo = osInfo.getGpuInfo();

                String[] parts = nvidiaSmiResult.trim().split(",\\s*");
                if (parts.length >= 2) {
                    // 转换为GB
                    Double totalMemory = Double.parseDouble(parts[0].trim()) / 1024.0;
                    Double usedMemory = Double.parseDouble(parts[1].trim()) / 1024.0;

                    gpuInfo.setMemorySize(Math.round(totalMemory * 10.0) / 10.0);

                    logger.info("解析NVIDIA GPU显存信息成功: 总显存={}GB",
                            gpuInfo.getMemorySize());
                }
            } catch (Exception e) {
                logger.error("解析NVIDIA GPU显存信息失败", e);
            }
        }

        /**
         * 解析Linux NVIDIA GPU信息
         */
        private void parseLinuxNvidiaGpuInfo(OsInfo osInfo, String nvidiaSmiResult) {
            try {
                // 确保GpuInfo对象存在
                if (osInfo.getGpuInfo() == null) {
                    osInfo.setGpuInfo(new GpuInfo());
                }
                GpuInfo gpuInfo = osInfo.getGpuInfo();

                String[] lines = nvidiaSmiResult.trim().split("\\n");
                if (lines.length > 0) {
                    String[] parts = lines[0].split(",\\s*");
                    if (parts.length >= 3) {
                        // 设置GPU型号
                        gpuInfo.setModel(parts[0].trim());
                        gpuInfo.setInfo(parts[0].trim());

                        // 设置显存信息（从MiB转换为GB）
                        try {
                            double totalMemory = Double.parseDouble(parts[1].trim().replace("MiB", "").trim()) / 1024.0;
                            gpuInfo.setMemorySize(Math.round(totalMemory * 10.0) / 10.0);
                        } catch (NumberFormatException e) {
                            logger.warn("解析NVIDIA显存大小失败: {}", e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("解析Linux NVIDIA GPU信息失败", e);
            }
        }

        private void parseLinuxGpuInfo(OsInfo osInfo, String gpuInfo) {
            // 确保GpuInfo对象存在
            if (osInfo.getGpuInfo() == null) {
                osInfo.setGpuInfo(new GpuInfo());
            }
            GpuInfo gpuInfoObj = osInfo.getGpuInfo();

            // 通过正则表达式提取GPU型号
            Pattern pattern = Pattern.compile("VGA.*\\[([^\\]]+)\\]");
            Matcher matcher = pattern.matcher(gpuInfo);
            if (matcher.find()) {
                String gpuModel = matcher.group(1).trim();
                gpuInfoObj.setModel(gpuModel);
                gpuInfoObj.setInfo(gpuModel);

                // 根据型号判断是否是集成显卡，设置默认显存大小
                if (gpuModel.toLowerCase().contains("intel")) {
                    gpuInfoObj.setMemorySize(1.0); // 假设Intel集成显卡有1GB显存
                } else if (gpuModel.toLowerCase().contains("nvidia")) {
                    gpuInfoObj.setMemorySize(4.0); // 假设NVIDIA显卡有4GB显存
                } else if (gpuModel.toLowerCase().contains("amd") || gpuModel.toLowerCase().contains("radeon")) {
                    gpuInfoObj.setMemorySize(2.0); // 假设AMD显卡有2GB显存
                } else {
                    gpuInfoObj.setMemorySize(0.0);
                }
            } else {
                gpuInfoObj.setModel("未识别的显卡");
                gpuInfoObj.setInfo("未识别的显卡");
                gpuInfoObj.setMemorySize(0.0);
            }
        }

        private void parseWindowsGpuInfo(OsInfo osInfo, String gpuInfo) {
            try {
                // 确保GpuInfo对象存在
                if (osInfo.getGpuInfo() == null) {
                    osInfo.setGpuInfo(new GpuInfo());
                }
                GpuInfo gpuInfoObj = osInfo.getGpuInfo();

                JSONArray gpus = JSON.parseArray(gpuInfo);
                if (gpus != null && !gpus.isEmpty()) {
                    JSONObject gpu = gpus.getJSONObject(0);
                    String name = gpu.getString("Name");
                    Long adapterRam = gpu.getLong("AdapterRAM");

                    if (StringUtils.isNotBlank(name)) {
                        gpuInfoObj.setModel(name);
                        gpuInfoObj.setInfo(name);

                        // 计算显存大小（字节转GB）
                        if (adapterRam != null && adapterRam > 0) {
                            double gpuMemoryGB = adapterRam / (1024.0 * 1024.0 * 1024.0);
                            gpuInfoObj.setMemorySize(Math.round(gpuMemoryGB * 10.0) / 10.0);
                        } else {
                            gpuInfoObj.setMemorySize(0.0);
                        }
                    } else {
                        gpuInfoObj.setModel("未检测到GPU设备");
                        gpuInfoObj.setInfo("未检测到GPU设备");
                        gpuInfoObj.setMemorySize(0.0);
                    }
                } else {
                    gpuInfoObj.setModel("未检测到GPU设备");
                    gpuInfoObj.setInfo("未检测到GPU设备");
                    gpuInfoObj.setMemorySize(0.0);
                }
            } catch (Exception e) {
                logger.error("解析Windows GPU信息失败", e);
                GpuInfo gpuInfoObj = new GpuInfo();
                gpuInfoObj.setModel("获取GPU信息失败: " + e.getMessage());
                gpuInfoObj.setInfo("获取GPU信息失败: " + e.getMessage());
                gpuInfoObj.setMemorySize(0.0);
                osInfo.setGpuInfo(gpuInfoObj);
            }
        }

        /**
         * 收集网络接口信息
         * 获取网卡型号、速率和其他网络信息
         */
        private void collectNetworkInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            if (osInfo == null) {
                return;
            }

            try {
                logger.info("开始为主机 [{}] 收集网络接口信息", hostInfo.getIp());
                long startTime = System.currentTimeMillis();

                // 设置状态为加载中
                osInfo.setNetworkStatus(OsInfoStatusEnum.LOADING);

                if (session == null || !session.isOpen()) {
                    logger.error("主机 [{}] 的SSH会话未建立或已关闭", hostInfo.getIp());
                    osInfo.setNetworkStatus(OsInfoStatusEnum.ERROR);
                    return;
                }

                // 创建网卡列表 - 使用OsInfoLegacy.NetworkInterface
                List<OsInfoLegacy.NetworkInterface> legacyNetworkInterfaces = new ArrayList<>();

                if (osInfo.getDistributionId() != null && "windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集网卡信息
                    // 获取网卡基本信息（包括名称、状态、IP地址）
                    String nicListCmd = "powershell -command \"Get-NetAdapter | Select-Object Name,InterfaceDescription,Status,MacAddress,LinkSpeed | ConvertTo-Json\"";
                    String nicList = MinaUtils.execCmdWithResult(session, nicListCmd);

                    // 获取IP地址信息
                    String ipConfigCmd = "powershell -command \"Get-NetIPAddress | Select-Object InterfaceAlias,IPAddress,PrefixLength,AddressFamily | ConvertTo-Json\"";
                    String ipConfig = MinaUtils.execCmdWithResult(session, ipConfigCmd);

                    if (StringUtils.isNotBlank(nicList) && StringUtils.isNotBlank(ipConfig)) {
                        // 解析Windows网卡信息
                        parseWindowsNetworkInfoNew(legacyNetworkInterfaces, nicList, ipConfig);
                    }
                } else {
                    // Linux系统收集网卡信息
                    // 获取网卡列表
                    String ifconfigCmd = "ip -o addr show | grep -v 'lo\\|docker\\|veth\\|br-' | awk '{print $2}' | sort | uniq";
                    String nicList = MinaUtils.execCmdWithResult(session, ifconfigCmd);

                    if (StringUtils.isNotBlank(nicList)) {
                        String[] nics = nicList.split("\\n");
                        for (String nic : nics) {
                            nic = nic.trim();
                            if (StringUtils.isBlank(nic)) {
                                continue;
                            }

                            // 创建网卡对象 - 使用OsInfoLegacy.NetworkInterface
                            OsInfoLegacy.NetworkInterface netInterface = new OsInfoLegacy.NetworkInterface();
                            netInterface.setName(nic);

                            // 获取网卡状态
                            String statusCmd = "cat /sys/class/net/" + nic + "/operstate 2>/dev/null || echo 'unknown'";
                            String status = MinaUtils.execCmdWithResult(session, statusCmd).trim();
                            netInterface.setUp("up".equalsIgnoreCase(status));

                            // 获取MAC地址
                            String macCmd = "cat /sys/class/net/" + nic + "/address 2>/dev/null || echo ''";
                            String mac = MinaUtils.execCmdWithResult(session, macCmd).trim();
                            netInterface.setMac(mac);

                            // 获取IP地址信息
                            String ipCmd = "ip addr show " + nic + " | grep 'inet ' | awk '{print $2}'";
                            String ipInfo = MinaUtils.execCmdWithResult(session, ipCmd).trim();
                            if (StringUtils.isNotBlank(ipInfo)) {
                                String[] ipParts = ipInfo.split("/");
                                if (ipParts.length >= 2) {
                                    netInterface.setIpv4(ipParts[0]);
                                    // 计算子网掩码
                                    int cidr = Integer.parseInt(ipParts[1]);
                                    netInterface.setNetmask(cidrToNetmask(cidr));
                                }
                            }

                            // 获取IPv6地址
                            String ipv6Cmd = "ip addr show " + nic
                                    + " | grep 'inet6 ' | grep -v 'fe80' | awk '{print $2}' | head -1";
                            String ipv6Info = MinaUtils.execCmdWithResult(session, ipv6Cmd).trim();
                            if (StringUtils.isNotBlank(ipv6Info)) {
                                String[] ipv6Parts = ipv6Info.split("/");
                                if (ipv6Parts.length > 0) {
                                    netInterface.setIpv6(ipv6Parts[0]);
                                }
                            }

                            // 获取网卡型号
                            String modelCmd = "ethtool -i " + nic
                                    + " 2>/dev/null | grep 'driver\\|version\\|bus-info' || echo ''";
                            String modelInfo = MinaUtils.execCmdWithResult(session, modelCmd);
                            String model = parseLinuxNicModel(modelInfo, nic);
                            netInterface.setModel(model);

                            // 获取网卡速率
                            String speedCmd = "ethtool " + nic + " 2>/dev/null | grep 'Speed:' || echo ''";
                            String speedInfo = MinaUtils.execCmdWithResult(session, speedCmd).trim();
                            Long speed = parseLinuxNicSpeed(speedInfo);
                            netInterface.setSpeed(speed);

                            // 获取网卡流量统计
                            String txCmd = "cat /sys/class/net/" + nic + "/statistics/tx_bytes 2>/dev/null || echo '0'";
                            String rxCmd = "cat /sys/class/net/" + nic + "/statistics/rx_bytes 2>/dev/null || echo '0'";
                            String txBytes = MinaUtils.execCmdWithResult(session, txCmd).trim();
                            String rxBytes = MinaUtils.execCmdWithResult(session, rxCmd).trim();

                            OsInfoLegacy.NetworkInterface.NetworkStats stats = new OsInfoLegacy.NetworkInterface.NetworkStats();
                            stats.setTxBytes(StringUtils.isNumeric(txBytes) ? Long.parseLong(txBytes) : 0);
                            stats.setRxBytes(StringUtils.isNumeric(rxBytes) ? Long.parseLong(rxBytes) : 0);
                            netInterface.setStats(stats);

                            // 添加到网卡列表
                            legacyNetworkInterfaces.add(netInterface);
                        }
                    }
                }

                // 将旧版格式转换为新版格式并存储
                List<NetworkInfo.NetworkInterface> newNetworkInterfaces = OsInfoLegacy
                        .convertToNewNetworkInterfaces(legacyNetworkInterfaces);

                // 确保NetworkInfo已初始化
                if (osInfo.getNetworkInfo() == null) {
                    osInfo.setNetworkInfo(new NetworkInfo());
                }

                // 设置新版接口列表
                osInfo.getNetworkInfo().setInterfaces(newNetworkInterfaces);

                // 旧版API不再支持直接设置接口列表
                // 移除: osInfo.setNetworkInterfaces(newNetworkInterfaces);

                // 更新状态
                osInfo.setNetworkStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("网络接口信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("网络接口信息收集完成: {}, 用时: {}ms, 发现{}个网卡",
                        hostInfo.getIp(), (System.currentTimeMillis() - startTime), legacyNetworkInterfaces.size());
            } catch (Exception e) {
                logger.error("收集网络接口信息失败: {}", hostInfo.getIp(), e);
                osInfo.setNetworkStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("网络接口信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 解析Windows网卡信息（新版方法）
         */
        private void parseWindowsNetworkInfoNew(List<OsInfoLegacy.NetworkInterface> networkInterfaces,
                String nicList, String ipConfig) {
            try {
                // 解析网卡基本信息
                JSONArray nics = JSON.parseArray(nicList);
                JSONArray ips = JSON.parseArray(ipConfig);

                for (int i = 0; i < nics.size(); i++) {
                    JSONObject nic = nics.getJSONObject(i);
                    String name = nic.getString("Name");
                    String description = nic.getString("InterfaceDescription");
                    String status = nic.getString("Status");
                    String mac = nic.getString("MacAddress");
                    String linkSpeed = nic.getString("LinkSpeed");

                    // 跳过没有名称的网卡
                    if (StringUtils.isBlank(name)) {
                        continue;
                    }

                    // 创建网卡对象
                    OsInfoLegacy.NetworkInterface netInterface = new OsInfoLegacy.NetworkInterface();
                    netInterface.setName(name);
                    netInterface.setUp("Up".equalsIgnoreCase(status));
                    netInterface.setModel(description);
                    netInterface.setMac(mac);
                    netInterface.setSpeed(parseWindowsNicSpeed(linkSpeed));

                    // 查找IP地址信息
                    for (int j = 0; j < ips.size(); j++) {
                        JSONObject ip = ips.getJSONObject(j);
                        String ifAlias = ip.getString("InterfaceAlias");

                        if (name.equals(ifAlias)) {
                            int addressFamily = ip.getIntValue("AddressFamily");
                            String ipAddress = ip.getString("IPAddress");
                            Integer prefixLength = ip.getInteger("PrefixLength");

                            // IPv4
                            if (addressFamily == 2 && StringUtils.isNotBlank(ipAddress)) {
                                netInterface.setIpv4(ipAddress);
                                if (prefixLength != null) {
                                    netInterface.setNetmask(cidrToNetmask(prefixLength));
                                }
                            }

                            // IPv6
                            if (addressFamily == 23 && StringUtils.isNotBlank(ipAddress)) {
                                // 排除本地链路地址
                                if (!ipAddress.toLowerCase().startsWith("fe80")) {
                                    netInterface.setIpv6(ipAddress);
                                }
                            }
                        }
                    }

                    // 获取网卡流量统计
                    // 由于Windows中获取网卡流量较复杂，这里设置为0
                    OsInfoLegacy.NetworkInterface.NetworkStats stats = new OsInfoLegacy.NetworkInterface.NetworkStats();
                    stats.setTxBytes(0L);
                    stats.setRxBytes(0L);
                    netInterface.setStats(stats);

                    // 添加到网卡列表
                    networkInterfaces.add(netInterface);
                }
            } catch (Exception e) {
                logger.error("解析Windows网卡信息失败", e);
            }
        }

        /**
         * 解析Linux网卡型号
         */
        private String parseLinuxNicModel(String modelInfo, String nicName) {
            if (StringUtils.isBlank(modelInfo)) {
                return nicName + " Network Card";
            }

            // 先查找型号名称
            Pattern modelPattern = Pattern.compile("driver:\\s*([^\\n]+)");
            Matcher modelMatcher = modelPattern.matcher(modelInfo);
            if (modelMatcher.find()) {
                String driver = modelMatcher.group(1).trim();

                // 根据驱动判断网卡类型
                if (driver.contains("e1000") || driver.contains("igb") || driver.contains("ixgbe")) {
                    return "Intel " + driver + " (" + getNicSpeedType(driver) + ")";
                } else if (driver.contains("r8169") || driver.contains("r8168")) {
                    return "Realtek " + driver + " (" + getNicSpeedType(driver) + ")";
                } else if (driver.contains("bnx2x") || driver.contains("tg3")) {
                    return "Broadcom " + driver + " (" + getNicSpeedType(driver) + ")";
                } else if (driver.contains("mlx")) {
                    return "Mellanox " + driver + " (" + getNicSpeedType(driver) + ")";
                } else {
                    return driver + " Network Card";
                }
            }

            return nicName + " Network Card";
        }

        /**
         * 根据驱动名称判断网卡速率类型
         */
        private String getNicSpeedType(String driver) {
            if (driver.contains("10g") || driver.contains("ixgbe")) {
                return "10 Gigabit";
            } else if (driver.contains("40g") || driver.contains("mlx")) {
                return "40 Gigabit";
            } else if (driver.contains("100g")) {
                return "100 Gigabit";
            } else if (driver.contains("1g") || driver.contains("igb")) {
                return "Gigabit";
            } else {
                return "Fast Ethernet";
            }
        }

        /**
         * 解析Linux网卡速率
         */
        private Long parseLinuxNicSpeed(String speedInfo) {
            if (StringUtils.isBlank(speedInfo)) {
                return 0L;
            }

            // 匹配速率信息
            Pattern speedPattern = Pattern.compile("Speed:\\s*(\\d+)(\\w+)/s");
            Matcher speedMatcher = speedPattern.matcher(speedInfo);
            if (speedMatcher.find()) {
                String speedValue = speedMatcher.group(1);
                String speedUnit = speedMatcher.group(2).toUpperCase();

                long speed = Long.parseLong(speedValue);

                // 转换单位
                if ("GB".equals(speedUnit)) {
                    return speed * 1000000000L;
                } else if ("MB".equals(speedUnit)) {
                    return speed * 1000000L;
                } else if ("KB".equals(speedUnit)) {
                    return speed * 1000L;
                } else {
                    return speed;
                }
            }

            return 0L;
        }

        /**
         * 解析Windows网卡速率
         */
        private Long parseWindowsNicSpeed(String linkSpeed) {
            if (StringUtils.isBlank(linkSpeed)) {
                return 0L;
            }

            try {
                Pattern speedPattern = Pattern.compile("(\\d+)\\s+(\\w+)");
                Matcher speedMatcher = speedPattern.matcher(linkSpeed);
                if (speedMatcher.find()) {
                    String speedValue = speedMatcher.group(1);
                    String speedUnit = speedMatcher.group(2).toUpperCase();

                    long speed = Long.parseLong(speedValue);

                    // 转换单位
                    if (speedUnit.contains("GBPS") || speedUnit.contains("GB")) {
                        return speed * 1000000000L;
                    } else if (speedUnit.contains("MBPS") || speedUnit.contains("MB")) {
                        return speed * 1000000L;
                    } else if (speedUnit.contains("KBPS") || speedUnit.contains("KB")) {
                        return speed * 1000L;
                    } else {
                        return speed;
                    }
                }
            } catch (Exception e) {
                logger.error("解析Windows网卡速率失败: {}", linkSpeed, e);
            }

            return 0L;
        }

        /**
         * CIDR转子网掩码
         */
        private String cidrToNetmask(int cidr) {
            int mask = 0xffffffff << (32 - cidr);
            int[] octets = new int[4];
            octets[0] = (mask >> 24) & 0xff;
            octets[1] = (mask >> 16) & 0xff;
            octets[2] = (mask >> 8) & 0xff;
            octets[3] = mask & 0xff;

            return String.format("%d.%d.%d.%d", octets[0], octets[1], octets[2], octets[3]);
        }

        /**
         * 执行命令并返回结果
         */
        private String executeCommand(ClientSession session, String command) {
            try {
                // 使用MinaUtils直接执行命令
                String result = MinaUtils.execCmdWithResult(session, command);
                if (result != null) {
                    return result;
                } else {
                    throw new Exception("命令执行失败: " + command);
                }
            } catch (Exception e) {
                logger.error("执行命令失败: {}, 错误: {}", command, e.getMessage(), e);
                throw new RuntimeException("执行命令失败: " + command, e);
            }
        }

        /**
         * 重置所有计数器
         */
        public synchronized void resetCounters() {
            // 清空结果列表但保留当前处理中的任务
            sortedHostList.clear();

            // 重置完成计数
            completedHostCount.set(0);

            // 不重置processingHostCount，因为可能有正在处理的主机

            // 如果当前没有处理中的主机，那么也可以重置总数
            if (processingHostCount.get() == 0) {
                totalHostCount.set(0);
            }

            logger.info("主机信息收集计数器已重置");
        }

        /**
         * 新版收集网络接口信息
         * 获取网卡型号、速率和其他网络信息，使用新的数据模型
         */
        private void collectNetworkInfoNew(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            if (osInfo == null) {
                return;
            }

            try {
                logger.info("开始为主机 [{}] 收集网络接口信息", hostInfo.getIp());
                long startTime = System.currentTimeMillis();

                // 设置状态为加载中
                osInfo.setNetworkStatus(OsInfoStatusEnum.LOADING);

                if (session == null || !session.isOpen()) {
                    logger.error("主机 [{}] 的SSH会话未建立或已关闭", hostInfo.getIp());
                    osInfo.setNetworkStatus(OsInfoStatusEnum.ERROR);
                    return;
                }

                // 创建网卡列表 - 使用OsInfoLegacy.NetworkInterface
                List<OsInfoLegacy.NetworkInterface> legacyNetworkInterfaces = new ArrayList<>();

                if (osInfo.getDistributionId() != null && "windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集网卡信息
                    // 获取网卡基本信息（包括名称、状态、IP地址）
                    String nicListCmd = "powershell -command \"Get-NetAdapter | Select-Object Name,InterfaceDescription,Status,MacAddress,LinkSpeed | ConvertTo-Json\"";
                    String nicList = MinaUtils.execCmdWithResult(session, nicListCmd);

                    // 获取IP地址信息
                    String ipConfigCmd = "powershell -command \"Get-NetIPAddress | Select-Object InterfaceAlias,IPAddress,PrefixLength,AddressFamily | ConvertTo-Json\"";
                    String ipConfig = MinaUtils.execCmdWithResult(session, ipConfigCmd);

                    if (StringUtils.isNotBlank(nicList) && StringUtils.isNotBlank(ipConfig)) {
                        // 解析Windows网卡信息
                        parseWindowsNetworkInfoNew(legacyNetworkInterfaces, nicList, ipConfig);
                    }
                } else {
                    // Linux系统收集网卡信息
                    // 获取网卡列表
                    String ifconfigCmd = "ip -o addr show | grep -v 'lo\\|docker\\|veth\\|br-' | awk '{print $2}' | sort | uniq";
                    String nicList = MinaUtils.execCmdWithResult(session, ifconfigCmd);

                    if (StringUtils.isNotBlank(nicList)) {
                        String[] nics = nicList.split("\\n");
                        for (String nic : nics) {
                            nic = nic.trim();
                            if (StringUtils.isBlank(nic)) {
                                continue;
                            }

                            // 创建网卡对象 - 使用OsInfoLegacy.NetworkInterface
                            OsInfoLegacy.NetworkInterface netInterface = new OsInfoLegacy.NetworkInterface();
                            netInterface.setName(nic);

                            // 获取网卡状态
                            String statusCmd = "cat /sys/class/net/" + nic + "/operstate 2>/dev/null || echo 'unknown'";
                            String status = MinaUtils.execCmdWithResult(session, statusCmd).trim();
                            netInterface.setUp("up".equalsIgnoreCase(status));

                            // 获取MAC地址
                            String macCmd = "cat /sys/class/net/" + nic + "/address 2>/dev/null || echo ''";
                            String mac = MinaUtils.execCmdWithResult(session, macCmd).trim();
                            netInterface.setMac(mac);

                            // 获取IP地址信息
                            String ipCmd = "ip addr show " + nic + " | grep 'inet ' | awk '{print $2}'";
                            String ipInfo = MinaUtils.execCmdWithResult(session, ipCmd).trim();
                            if (StringUtils.isNotBlank(ipInfo)) {
                                String[] ipParts = ipInfo.split("/");
                                if (ipParts.length >= 2) {
                                    netInterface.setIpv4(ipParts[0]);
                                    // 计算子网掩码
                                    int cidr = Integer.parseInt(ipParts[1]);
                                    netInterface.setNetmask(cidrToNetmask(cidr));
                                }
                            }

                            // 获取IPv6地址
                            String ipv6Cmd = "ip addr show " + nic
                                    + " | grep 'inet6 ' | grep -v 'fe80' | awk '{print $2}' | head -1";
                            String ipv6Info = MinaUtils.execCmdWithResult(session, ipv6Cmd).trim();
                            if (StringUtils.isNotBlank(ipv6Info)) {
                                String[] ipv6Parts = ipv6Info.split("/");
                                if (ipv6Parts.length > 0) {
                                    netInterface.setIpv6(ipv6Parts[0]);
                                }
                            }

                            // 获取网卡型号
                            String modelCmd = "ethtool -i " + nic
                                    + " 2>/dev/null | grep 'driver\\|version\\|bus-info' || echo ''";
                            String modelInfo = MinaUtils.execCmdWithResult(session, modelCmd);
                            String model = parseLinuxNicModel(modelInfo, nic);
                            netInterface.setModel(model);

                            // 获取网卡速率
                            String speedCmd = "ethtool " + nic + " 2>/dev/null | grep 'Speed:' || echo ''";
                            String speedInfo = MinaUtils.execCmdWithResult(session, speedCmd).trim();
                            Long speed = parseLinuxNicSpeed(speedInfo);
                            netInterface.setSpeed(speed);

                            // 获取网卡流量统计
                            String txCmd = "cat /sys/class/net/" + nic + "/statistics/tx_bytes 2>/dev/null || echo '0'";
                            String rxCmd = "cat /sys/class/net/" + nic + "/statistics/rx_bytes 2>/dev/null || echo '0'";
                            String txBytes = MinaUtils.execCmdWithResult(session, txCmd).trim();
                            String rxBytes = MinaUtils.execCmdWithResult(session, rxCmd).trim();

                            OsInfoLegacy.NetworkInterface.NetworkStats stats = new OsInfoLegacy.NetworkInterface.NetworkStats();
                            stats.setTxBytes(StringUtils.isNumeric(txBytes) ? Long.parseLong(txBytes) : 0);
                            stats.setRxBytes(StringUtils.isNumeric(rxBytes) ? Long.parseLong(rxBytes) : 0);
                            netInterface.setStats(stats);

                            // 添加到网卡列表
                            legacyNetworkInterfaces.add(netInterface);
                        }
                    }
                }

                // 将旧版格式转换为新版格式并存储
                List<NetworkInfo.NetworkInterface> newNetworkInterfaces = OsInfoLegacy
                        .convertToNewNetworkInterfaces(legacyNetworkInterfaces);

                // 确保NetworkInfo已初始化
                if (osInfo.getNetworkInfo() == null) {
                    osInfo.setNetworkInfo(new NetworkInfo());
                }

                // 设置新版接口列表
                osInfo.getNetworkInfo().setInterfaces(newNetworkInterfaces);

                // 旧版API不再使用
                // osInfo.setNetworkInterfaces(newNetworkInterfaces);

                // 更新状态
                osInfo.setNetworkStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("网络接口信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("网络接口信息收集完成: {}, 用时: {}ms, 发现{}个网卡",
                        hostInfo.getIp(), (System.currentTimeMillis() - startTime), legacyNetworkInterfaces.size());
            } catch (Exception e) {
                logger.error("收集网络接口信息失败: {}", hostInfo.getIp(), e);
                osInfo.setNetworkStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("网络接口信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }
    }

    @Override
    public synchronized void updateHostInfoCache(HostInfo hostInfo) {
        if (hostInfo == null)
            return;

        try {
            // 标记状态已更新，以便前端能察觉到变化
            hostInfo.setStatusCacheDirty(true);

            // 更新缓存
            CacheUtils.putHostInfo(hostInfo.getClusterId(), hostInfo.getIp(), hostInfo);
            logger.debug("已更新主机缓存: {}", hostInfo.getIp());
        } catch (Exception e) {
            logger.error("更新主机缓存失败: {}, 原因: {}", hostInfo.getIp(), e.getMessage(), e);
        }
    }

    /**
     * 内部方法：获取主机操作系统信息
     */
    public OsInfo getHostOsInfoInternal(HostInfo hostInfo) {
        OsInfo osInfo = new OsInfo();
        hostInfo.setOsInfo(osInfo);

        ClientSession session = null;
        try {
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.warn("无法创建SSH会话，主机IP: {}", hostInfo.getIp());
                // 设置SSH连接失败状态
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                // 设置更详细的错误信息
                osInfo.setValid(false);
                osInfo.setErrorMessage("无法创建SSH连接，请检查SSH配置");
                // 立即更新缓存
                updateHostInfoCache(hostInfo);
                logger.error("主机 {} 无法创建SSH会话，已设置状态：osInfoStatus={}, sshConnectStatus={}",
                        hostInfo.getIp(), hostInfo.getOsInfoStatus(), hostInfo.getSshConnectStatus());
                return osInfo;
            }

            // 首先确定操作系统类型（Windows或Linux）
            String osType = detectOperatingSystemType(session);
            logger.info("主机 {} 的操作系统类型为: {}", hostInfo.getIp(), osType);

            // 判断是否为Windows系统
            if ("windows".equalsIgnoreCase(osType)) {
                // Windows系统需要特殊处理
                try {
                    // 获取主机名
                    String hostname = MinaUtils.execCmdWithResult(session, "cmd /c hostname").trim();
                    hostInfo.setHostname(hostname);
                    hostInfo.setFqdn(hostname); // Windows通常不使用FQDN
                    hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);

                    // 获取系统版本
                    String winVer = MinaUtils.execCmdWithResult(session, "cmd /c ver").trim();

                    // 创建Windows OsInfo对象
                    osInfo.setHostname(hostname);
                    osInfo.setFqdn(hostname);
                    osInfo.setDistributionId("windows");
                    osInfo.setDistribution("Windows");
                    osInfo.setDistributionName("Windows");
                    osInfo.setDistributionType(LinuxDistribution.OTHER);

                    // 解析版本
                    if (winVer.contains("Windows 10")) {
                        osInfo.setVersionId("10");
                        osInfo.setVersion("10");
                        osInfo.setFullName("Windows 10");
                    } else if (winVer.contains("Windows 11")) {
                        osInfo.setVersionId("11");
                        osInfo.setVersion("11");
                        osInfo.setFullName("Windows 11");
                    } else if (winVer.contains("Windows Server")) {
                        if (winVer.contains("2016")) {
                            osInfo.setVersionId("2016");
                            osInfo.setVersion("2016");
                            osInfo.setFullName("Windows Server 2016");
                        } else if (winVer.contains("2019")) {
                            osInfo.setVersionId("2019");
                            osInfo.setVersion("2019");
                            osInfo.setFullName("Windows Server 2019");
                        } else if (winVer.contains("2022")) {
                            osInfo.setVersionId("2022");
                            osInfo.setVersion("2022");
                            osInfo.setFullName("Windows Server 2022");
                        } else {
                            osInfo.setVersionId("Server");
                            osInfo.setVersion("Server");
                            osInfo.setFullName("Windows Server");
                        }
                    } else {
                        // 尝试从版本字符串中提取版本号
                        if (winVer.contains("[Version")) {
                            String version = winVer.substring(winVer.indexOf("[Version") + 9);
                            version = version.substring(0, version.indexOf("]")).trim();
                            if (version.startsWith("10.")) {
                                osInfo.setVersionId("10");
                                osInfo.setVersion("10");
                                osInfo.setFullName("Windows 10 (" + version + ")");
                            } else if (version.startsWith("6.3")) {
                                osInfo.setVersionId("8.1");
                                osInfo.setVersion("8.1");
                                osInfo.setFullName("Windows 8.1");
                            } else if (version.startsWith("6.2")) {
                                osInfo.setVersionId("8");
                                osInfo.setVersion("8");
                                osInfo.setFullName("Windows 8");
                            } else if (version.startsWith("6.1")) {
                                osInfo.setVersionId("7");
                                osInfo.setVersion("7");
                                osInfo.setFullName("Windows 7");
                            } else {
                                osInfo.setVersionId(version);
                                osInfo.setVersion(version);
                                osInfo.setFullName("Windows " + version);
                            }
                        } else {
                            osInfo.setVersionId("Unknown");
                            osInfo.setVersion("Unknown");
                            osInfo.setFullName("Windows");
                        }
                    }

                    // 获取系统架构
                    String arch = MinaUtils.execCmdWithResult(session, "cmd /c echo %PROCESSOR_ARCHITECTURE%").trim();
                    osInfo.setArchitecture(arch.equalsIgnoreCase("AMD64") ? "x86_64" : arch);

                    // 获取更多详细信息用于显示
                    try {
                        String sysInfo = MinaUtils.execCmdWithResult(session,
                                "cmd /c systeminfo | findstr /B /C:\"OS\" /C:\"系统\" /C:\"注册\" /C:\"Registered\"");
                        if (StringUtils.isNotBlank(sysInfo)) {
                            // 使用distributionName存储详细系统信息
                            osInfo.setDistributionName("Windows " + sysInfo.replace("\r\n", " | "));
                        }
                    } catch (Exception e) {
                        logger.warn("获取Windows系统详细信息失败: {}", e.getMessage());
                    }

                    // 获取CPU信息
                    String cpuInfoStr = MinaUtils.execCmdWithResult(session,
                            "cmd /c wmic cpu get Name, NumberOfCores, NumberOfLogicalProcessors /Value");
                    if (StringUtils.isNotBlank(cpuInfoStr)) {
                        CpuInfo cpuInfo = new CpuInfo();
                        osInfo.setCpuInfo(cpuInfo);

                        String[] lines = cpuInfoStr.split("\n");
                        for (String line : lines) {
                            if (line.startsWith("Name=")) {
                                cpuInfo.setModel(line.substring(5).trim());
                            } else if (line.startsWith("NumberOfCores=")) {
                                cpuInfo.setCores(Integer.parseInt(line.substring(14).trim()));
                            } else if (line.startsWith("NumberOfLogicalProcessors=")) {
                                cpuInfo.setLogicalCores(Integer.parseInt(line.substring(26).trim()));
                            }
                        }

                        // 计算物理CPU数量
                        if (cpuInfo.getCores() != null && cpuInfo.getLogicalCores() != null && cpuInfo.getCores() > 0) {
                            cpuInfo.setPhysicalCount(cpuInfo.getLogicalCores() / cpuInfo.getCores());
                        }
                    }

                    // 设置为有效
                    osInfo.setValid(true);
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);

                    // 更新缓存
                    hostInfo.setOsInfo(osInfo);
                    updateHostInfoCache(hostInfo);

                    return osInfo;
                } catch (Exception e) {
                    logger.error("Windows系统信息收集失败: {}", e.getMessage());
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("Windows系统信息收集失败: " + e.getMessage());
                    updateHostInfoCache(hostInfo);
                    return osInfo;
                }
            }

            // 使用工厂获取相应的操作系统信息收集器
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector(osType);
            if (collector != null) {
                // 使用收集器并传入缓存更新函数
                OsInfo result = collector.collectOsInfo(hostInfo, session, osInfo, this::updateHostInfoCache);
                logger.info("主机 {} 操作系统信息收集成功：{}，已设置状态：osInfoStatus={}",
                        hostInfo.getIp(), result.getFullName(), hostInfo.getOsInfoStatus());
                return result;
            } else {
                logger.warn("未找到适用于{}操作系统的信息收集器", osType);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("未找到适用的操作系统信息收集器");
                updateHostInfoCache(hostInfo);
                return osInfo;
            }
        } catch (Exception e) {
            logger.error("获取主机操作系统信息时出错: {}", e.getMessage(), e);
            // 设置SSH连接错误状态
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
            hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
            // 设置更详细的错误信息
            osInfo.setValid(false);
            osInfo.setErrorMessage("SSH连接异常: " + e.getMessage());
            updateHostInfoCache(hostInfo);
            return osInfo;
        }
    }

    /**
     * 检测操作系统类型
     */
    public String detectOperatingSystemType(ClientSession session) {
        // 最后尝试Windows特有命令，避免在Linux上执行可能导致的错误
        try {
            String winVerResult = MinaUtils.execCmdWithResult(session, "cmd /c ver");
            if (StringUtils.isNotBlank(winVerResult) &&
                    (winVerResult.contains("Microsoft Windows") || winVerResult.contains("Windows"))) {
                logger.info("检测到Windows系统: {}", winVerResult.trim());
                return "windows";
            }
        } catch (Exception e) {
            logger.debug("执行Windows版本命令失败: {}", e.getMessage());
        }

        // 首先尝试执行uname命令，适用于Linux/Unix系统
        try {
            String unameResult = MinaUtils.execCmdWithResult(session, "uname -a");
            if (StringUtils.isNotBlank(unameResult)) {
                logger.info("检测到Linux系统: {}", unameResult.trim());
                return "linux";
            }
        } catch (Exception e) {
            logger.debug("执行uname命令失败，尝试其他方法: {}", e.getMessage());
        }

        // 尝试检查/etc/os-release文件，适用于大多数现代Linux发行版
        try {
            String osReleaseResult = MinaUtils.execCmdWithResult(session, "cat /etc/os-release 2>/dev/null");
            if (StringUtils.isNotBlank(osReleaseResult)) {
                logger.info("检测到Linux系统，发现/etc/os-release文件");
                return "linux";
            }
        } catch (Exception e) {
            logger.debug("检查/etc/os-release失败: {}", e.getMessage());
        }

        // 默认返回linux，因为大多数情况是Linux系统
        logger.warn("无法明确检测操作系统类型，默认假设为Linux");
        return "linux";
    }

    /**
     * 获取或创建SSH会话，优先从缓存获取
     */
    public ClientSession getOrCreateSession(HostInfo hostInfo) {
        String ip = hostInfo.getIp();
        Integer sshPort = hostInfo.getSshPort();

        if (StringUtils.isBlank(ip) || sshPort == null ||
                StringUtils.isBlank(hostInfo.getSshUser()) || StringUtils.isBlank(hostInfo.getSshPassword())) {
            logger.warn("创建SSH会话失败: 缺少必要的连接参数");
            return null;
        }

        // 生成缓存键
        String cacheKey = ip + ":" + sshPort;

        // 尝试从缓存获取会话
        ClientSession session = sessionCache.get(cacheKey);

        // 验证会话是否有效
        if (session != null && session.isOpen()) {
            logger.info("从缓存复用主机{}的SSH会话", ip);
            return session;
        }

        // 如果缓存中没有有效会话，则创建新会话
        try {
            logger.info("创建到主机 {} 的新SSH会话，用户: {}, 端口: {}", ip, hostInfo.getSshUser(), sshPort);

            // 使用密码方式连接
            try {
                session = MinaUtils.openConnectionWithPassword(hostInfo);
                if (session != null) {
                    logger.info("成功使用密码连接到主机: {}", ip);
                    // 将新会话存入缓存
                    sessionCache.put(cacheKey, session);
                    logger.info("将主机{}的SSH会话加入缓存", ip);
                    return session;
                }
            } catch (Exception e) {
                logger.warn("使用密码连接到主机 {} 失败: {}", ip, e.getMessage());
            }

            // 尝试使用免密方式连接
            try {
                logger.info("尝试使用免密方式连接到主机: {}", ip);
                session = MinaUtils.openConnection(hostInfo);
                if (session != null) {
                    logger.info("成功使用免密方式连接到主机: {}", ip);
                    // 将新会话存入缓存
                    sessionCache.put(cacheKey, session);
                    logger.info("将主机{}的SSH会话加入缓存", ip);
                    return session;
                }
            } catch (Exception e) {
                logger.warn("使用免密方式连接到主机 {} 失败: {}", ip, e.getMessage());
            }

            logger.error("无法创建到主机 {} 的SSH会话", ip);
            return null;
        } catch (Exception e) {
            logger.error("创建SSH会话时出错: {}", e.getMessage(), e);
            return null;
        }
    }
}