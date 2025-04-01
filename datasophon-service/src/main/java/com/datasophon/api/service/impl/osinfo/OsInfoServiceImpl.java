package com.datasophon.api.service.impl.osinfo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.api.service.OsInfoService;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.LinuxDistribution;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import com.datasophon.common.model.OsInfoLegacy;
import com.datasophon.common.model.hardware.CpuInfo;
import com.datasophon.common.model.hardware.NetworkInfo;
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
         * 按顺序收集：主机名 -> OS类型 -> 硬件信息
         */
        private void collectAllInfoForHost(HostInfo hostInfo) {
            try {
                logger.info("开始收集主机信息: {}", hostInfo.getIp());
                long startTime = System.currentTimeMillis();

                // 设置状态为正在收集
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.COLLECTING);
                service.updateHostInfoCache(hostInfo);

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

                // 2. 收集操作系统类型（Linux或Windows）并收集相关信息
                collectOsType(hostInfo);

                // 操作系统信息收集由collectOsType方法完成，包括：
                // - 识别操作系统类型（Linux或Windows）
                // - 调用collectLinuxInfo或collectWindowsInfo收集详细信息
                // - 这些方法内部会使用IOsInfoCollector收集所有必要的硬件和系统信息

                // 完成收集
                hostInfo.setMessage("主机信息收集完成");
                service.updateHostInfoCache(hostInfo);

                // 记录完成时间
                logger.info("主机 {} 信息收集完成，总耗时 {}ms",
                        hostInfo.getIp(), System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集主机 {} 信息时出错: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setOsStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("主机信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            } finally {
                // 减少正在处理的主机计数
                processingHostCount.decrementAndGet();
                // 完成一台主机，增加计数
                completedHostCount.incrementAndGet();
                // 如果有其他主机等待处理，继续处理
                startProcessingIfNeeded();
            }
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
            try {
                logger.info("开始收集主机 [{}] 的操作系统类型", hostInfo.getIp());
                hostInfo.setOsStatus(OsInfoStatusEnum.COLLECTING);

                ClientSession session = connectToHost(hostInfo);
                if (session == null) {
                    logger.error("主机 [{}] 的SSH会话未建立，无法收集操作系统类型", hostInfo.getIp());
                    hostInfo.setOsStatus(OsInfoStatusEnum.ERROR);
                    return;
                }

                // 创建OsInfo对象
                OsInfo osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);

                // 检测操作系统类型
                String osType = service.detectOperatingSystemType(session);
                osInfo.setOsType(osType);

                // 根据操作系统类型进行不同的信息收集
                if ("linux".equalsIgnoreCase(osType)) {
                    collectLinuxInfo(hostInfo, session, osInfo);
                } else if ("windows".equalsIgnoreCase(osType)) {
                    collectWindowsInfo(hostInfo, session, osInfo);
                } else {
                    logger.warn("未知的操作系统类型: {}", osType);
                    osInfo.setDistribution("Unknown");
                    osInfo.setDistributionId("unknown");
                    osInfo.setDisplayName("未知操作系统");
                }

                // 额外检查：如果内核版本包含kylin特征，但未被识别为麒麟系统，则强制识别
                String kernelVersion = osInfo.getKernelVersion();
                if (StringUtils.isNotBlank(kernelVersion) &&
                        (kernelVersion.contains("ky10") || kernelVersion.contains("kylin")) &&
                        !"kylin".equals(osInfo.getDistributionId())) {

                    logger.info("发现麒麟系统内核特征但未被识别，强制设置为麒麟系统: {}", kernelVersion);

                    // 设置基本信息
                    osInfo.setDistributionId("kylin");
                    osInfo.setDistribution("Kylin");
                    osInfo.setDistributionType(LinuxDistribution.KYLIN);

                    // 设置简洁的显示名称
                    osInfo.setDisplayName("中标麒麟");

                    // 尝试判断麒麟版本
                    if (kernelVersion.contains("ky10")) {
                        osInfo.setVersionId("V10");
                        osInfo.setVersion("V10");
                        osInfo.setKylinV10(true);
                        osInfo.setDistributionName("中标麒麟 V10");
                        osInfo.setFullName("Kylin Linux Advanced Server V10");
                    } else if (kernelVersion.contains("ky4")) {
                        osInfo.setVersionId("V4");
                        osInfo.setVersion("V4");
                        osInfo.setKylinV4(true);
                        osInfo.setDistributionName("中标麒麟 V4");
                        osInfo.setFullName("中标麒麟操作系统 V4");
                    } else {
                        // 版本未知，使用默认值
                        osInfo.setDistributionName("中标麒麟");
                        osInfo.setFullName("中标麒麟操作系统");
                    }
                }

                // 收集成功，更新状态
                hostInfo.setOsStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("操作系统信息收集完成");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 [{}] 的操作系统信息收集完成: {}", hostInfo.getIp(),
                        osInfo.getDistribution() + " " + osInfo.getVersion());
            } catch (Exception e) {
                logger.error("收集主机 [{}] 的操作系统类型时出错: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setOsStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("收集操作系统信息失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 连接到主机
         */
        private ClientSession connectToHost(HostInfo hostInfo) {
            try {
                logger.info("连接到主机: {}", hostInfo.getIp());
                // 委托给服务类方法创建连接
                return service.getOrCreateSession(hostInfo);
            } catch (Exception e) {
                logger.error("连接到主机 {} 失败: {}", hostInfo.getIp(), e.getMessage(), e);
                return null;
            }
        }

        /**
         * 收集Linux操作系统信息
         * 使用Linux系统信息收集器获取系统和硬件信息
         */
        private void collectLinuxInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo) {
            try {
                logger.info("开始收集Linux系统详细信息: {}", hostInfo.getIp());

                // 预先检查常见的Linux发行版文件
                preCheckLinuxDistribution(session, osInfo);

                // 获取适用于Linux的收集器
                IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector("linux");
                if (collector != null) {
                    // 收集操作系统基本信息
                    osInfo = collector.collectOsInfo(hostInfo, session, osInfo,
                            (info) -> service.updateHostInfoCache(hostInfo));
                    hostInfo.setOsInfo(osInfo);
                    service.updateHostInfoCache(hostInfo);

                    // 收集硬件信息
                    collector.collectHardwareInfo(osInfo, session,
                            (info) -> service.updateHostInfoCache(hostInfo));

                    // 更新状态
                    hostInfo.setOsInfo(osInfo);
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                    hostInfo.setMessage("Linux系统信息收集完成");
                    service.updateHostInfoCache(hostInfo);

                    logger.info("Linux系统信息收集成功: {}", hostInfo.getIp());
                } else {
                    logger.error("找不到Linux系统信息收集器");
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("找不到Linux系统信息收集器");
                    service.updateHostInfoCache(hostInfo);
                }
            } catch (Exception e) {
                logger.error("收集Linux系统信息时出错: {}", e.getMessage(), e);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("收集Linux系统信息失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 收集Windows操作系统信息
         * 使用Windows系统信息收集器获取系统和硬件信息
         */
        private void collectWindowsInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo) {
            try {
                logger.info("开始收集Windows系统详细信息: {}", hostInfo.getIp());

                // 获取适用于Windows的收集器
                IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector("windows");
                if (collector != null) {
                    // 收集操作系统基本信息
                    osInfo = collector.collectOsInfo(hostInfo, session, osInfo,
                            (info) -> service.updateHostInfoCache(hostInfo));
                    hostInfo.setOsInfo(osInfo);
                    service.updateHostInfoCache(hostInfo);

                    // 收集硬件信息
                    collector.collectHardwareInfo(osInfo, session,
                            (info) -> service.updateHostInfoCache(hostInfo));

                    // 更新状态
                    hostInfo.setOsInfo(osInfo);
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                    hostInfo.setMessage("Windows系统信息收集完成");
                    service.updateHostInfoCache(hostInfo);

                    logger.info("Windows系统信息收集成功: {}", hostInfo.getIp());
                } else {
                    logger.error("找不到Windows系统信息收集器");
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("找不到Windows系统信息收集器");
                    service.updateHostInfoCache(hostInfo);
                }
            } catch (Exception e) {
                logger.error("收集Windows系统信息时出错: {}", e.getMessage(), e);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("收集Windows系统信息失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 预检查Linux发行版类型
         */
        private void preCheckLinuxDistribution(ClientSession session, OsInfo osInfo) {
            try {
                // 检查Ubuntu系统
                checkUbuntu(session, osInfo);

                // 如果不是Ubuntu，检查Debian系统
                if (osInfo.getDistributionId() == null || !osInfo.getDistributionId().equals("ubuntu")) {
                    checkDebian(session, osInfo);
                }

                // 如果既不是Ubuntu也不是Debian，检查CentOS/RHEL系统
                if (osInfo.getDistributionId() == null ||
                        (!osInfo.getDistributionId().equals("ubuntu")
                                && !osInfo.getDistributionId().equals("debian"))) {
                    checkCentOS(session, osInfo);
                }

                // 如果仍未识别，使用/etc/os-release
                if (osInfo.getDistributionId() == null) {
                    checkOsRelease(session, osInfo);
                }
            } catch (Exception e) {
                logger.warn("预检查Linux发行版时出错: {}", e.getMessage());
            }
        }

        /**
         * 检查Ubuntu系统
         */
        private void checkUbuntu(ClientSession session, OsInfo osInfo) {
            try {
                // 检查lsb-release文件 (Ubuntu)
                String lsbRelease = MinaUtils.execCmdWithResult(session, "cat /etc/lsb-release 2>/dev/null");
                if (StringUtils.isNotBlank(lsbRelease) && lsbRelease.toLowerCase().contains("ubuntu")) {
                    logger.info("检测到Ubuntu系统配置文件");

                    // 提取版本
                    Pattern versionPattern = Pattern.compile("DISTRIB_RELEASE=([0-9.]+)");
                    Matcher versionMatcher = versionPattern.matcher(lsbRelease);
                    if (versionMatcher.find()) {
                        String version = versionMatcher.group(1);

                        // 设置详细信息
                        osInfo.setDistribution("Ubuntu");
                        osInfo.setDistributionId("ubuntu");
                        osInfo.setDistributionType(LinuxDistribution.UBUNTU);
                        osInfo.setVersion(version);
                        osInfo.setVersionId(version);

                        // 设置特定版本标志和完整名称
                        if (version.startsWith("22.")) {
                            osInfo.setUbuntu22(true);
                            // 为悬浮卡片设置详细版本信息
                            osInfo.setDistributionName("Ubuntu 22.04 LTS");
                            // 设置完整名称用于悬浮卡片
                            osInfo.setFullName("Ubuntu 22.04 LTS (Jammy Jellyfish)");
                            // 为列表显示设置简单名称
                            osInfo.setDisplayName("Ubuntu");
                        } else if (version.startsWith("24.")) {
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
                        } else if (version.startsWith("20.")) {
                            // 为悬浮卡片设置详细版本信息
                            osInfo.setDistributionName("Ubuntu 20.04 LTS");
                            // 设置完整名称用于悬浮卡片
                            osInfo.setFullName("Ubuntu 20.04 LTS (Focal Fossa)");
                            // 为列表显示设置简单名称
                            osInfo.setDisplayName("Ubuntu");
                        } else if (version.startsWith("18.")) {
                            // 为悬浮卡片设置详细版本信息
                            osInfo.setDistributionName("Ubuntu 18.04 LTS");
                            // 设置完整名称用于悬浮卡片
                            osInfo.setFullName("Ubuntu 18.04 LTS (Bionic Beaver)");
                            // 为列表显示设置简单名称
                            osInfo.setDisplayName("Ubuntu");
                        } else if (version.startsWith("16.")) {
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

                        // 提取DISTRIB_DESCRIPTION作为可能的完整名称
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

                        logger.info("通过lsb-release识别为Ubuntu系统，版本: {}", version);
                    }
                }
            } catch (Exception e) {
                logger.warn("检查Ubuntu系统时出错: {}", e.getMessage());
            }
        }

        /**
         * 检查Debian系统
         */
        private void checkDebian(ClientSession session, OsInfo osInfo) {
            try {
                // 检查debian_version文件
                String debianVersion = MinaUtils.execCmdWithResult(session, "cat /etc/debian_version 2>/dev/null");
                if (StringUtils.isNotBlank(debianVersion)) {
                    debianVersion = debianVersion.trim();

                    // 设置详细信息
                    osInfo.setDistribution("Debian");
                    osInfo.setDistributionId("debian");
                    osInfo.setVersion(debianVersion);
                    osInfo.setVersionId(debianVersion);
                    osInfo.setDistributionType(LinuxDistribution.DEBIAN);

                    // 设置简洁的显示名称
                    osInfo.setDisplayName("Debian");

                    // 匹配主要版本
                    String majorVersion = debianVersion.split("\\.")[0];

                    // 基于版本号设置详细名称
                    if (majorVersion.equals("12")) {
                        osInfo.setDistributionName("Debian 12 (Bookworm)");
                        osInfo.setFullName("Debian GNU/Linux 12 (bookworm)");
                    } else if (majorVersion.equals("11")) {
                        osInfo.setDistributionName("Debian 11 (Bullseye)");
                        osInfo.setFullName("Debian GNU/Linux 11 (bullseye)");
                    } else if (majorVersion.equals("10")) {
                        osInfo.setDistributionName("Debian 10 (Buster)");
                        osInfo.setFullName("Debian GNU/Linux 10 (buster)");
                    } else {
                        osInfo.setDistributionName("Debian " + debianVersion);
                        osInfo.setFullName("Debian GNU/Linux " + debianVersion);
                    }

                    logger.info("通过debian_version识别为Debian系统，版本: {}", debianVersion);
                }
            } catch (Exception e) {
                logger.warn("检查Debian系统时出错: {}", e.getMessage());
            }
        }

        /**
         * 检查CentOS系统
         */
        private void checkCentOS(ClientSession session, OsInfo osInfo) {
            try {
                // 检查CentOS/RHEL系统
                String redhatRelease = MinaUtils.execCmdWithResult(session, "cat /etc/redhat-release 2>/dev/null");
                if (StringUtils.isNotBlank(redhatRelease)) {
                    String release = redhatRelease.toLowerCase();
                    String versionId = null;

                    // 提取版本号
                    Pattern versionPattern = Pattern.compile("release\\s+([\\d\\.]+)");
                    Matcher versionMatcher = versionPattern.matcher(redhatRelease);
                    if (versionMatcher.find()) {
                        versionId = versionMatcher.group(1);
                    }

                    if (release.contains("centos")) {
                        osInfo.setDistribution("CentOS");
                        osInfo.setDistributionId("centos");
                        osInfo.setDistributionType(LinuxDistribution.CENTOS);
                        osInfo.setFullName(redhatRelease);

                        // 设置简洁的显示名称
                        osInfo.setDisplayName("CentOS");

                        if (versionId != null) {
                            osInfo.setVersionId(versionId);
                            osInfo.setVersion(versionId);

                            // 设置特定版本标记和详细分发名称
                            if (versionId.startsWith("7")) {
                                osInfo.setCentOS7(true);
                                osInfo.setDistributionName("CentOS Linux 7");
                            } else if (versionId.startsWith("8")) {
                                osInfo.setCentOS8(true);
                                osInfo.setDistributionName("CentOS Linux 8");
                            } else {
                                osInfo.setDistributionName("CentOS Linux " + versionId);
                            }

                            logger.info("通过redhat-release识别为CentOS系统，版本: {}", versionId);
                        } else {
                            osInfo.setDistributionName("CentOS Linux");
                        }
                    } else if (release.contains("fedora")) {
                        // 处理Fedora系统
                        osInfo.setDistribution("Fedora");
                        osInfo.setDistributionId("fedora");
                        osInfo.setDistributionType(LinuxDistribution.REDHAT); // 目前仍归类为REDHAT族
                        osInfo.setFullName(redhatRelease);

                        // 设置简洁的显示名称
                        osInfo.setDisplayName("Fedora");

                        if (versionId != null) {
                            osInfo.setVersionId(versionId);
                            osInfo.setVersion(versionId);
                            osInfo.setDistributionName("Fedora " + versionId);

                            logger.info("通过redhat-release识别为Fedora系统，版本: {}", versionId);
                        } else {
                            osInfo.setDistributionName("Fedora");
                        }
                    } else if (release.contains("red hat") || release.contains("redhat")) {
                        osInfo.setDistribution("RedHat");
                        osInfo.setDistributionId("rhel");
                        osInfo.setDistributionType(LinuxDistribution.REDHAT);
                        osInfo.setFullName(redhatRelease);

                        // 设置简洁的显示名称
                        osInfo.setDisplayName("Red Hat");

                        if (versionId != null) {
                            osInfo.setVersionId(versionId);
                            osInfo.setVersion(versionId);
                            osInfo.setDistributionName("Red Hat Enterprise Linux " + versionId);

                            logger.info("通过redhat-release识别为RHEL系统，版本: {}", versionId);
                        } else {
                            osInfo.setDistributionName("Red Hat Enterprise Linux");
                        }
                    }
                }

                // 检查是否存在fedora-release文件
                String fedoraRelease = MinaUtils.execCmdWithResult(session, "cat /etc/fedora-release 2>/dev/null");
                if (StringUtils.isNotBlank(fedoraRelease)) {
                    String release = fedoraRelease.trim();
                    String versionId = null;

                    // 提取版本号
                    Pattern versionPattern = Pattern.compile("release\\s+([\\d\\.]+)");
                    Matcher versionMatcher = versionPattern.matcher(fedoraRelease);
                    if (versionMatcher.find()) {
                        versionId = versionMatcher.group(1);
                    }

                    // 设置Fedora系统信息
                    osInfo.setDistribution("Fedora");
                    osInfo.setDistributionId("fedora");
                    osInfo.setDistributionType(LinuxDistribution.REDHAT); // 目前仍归类为REDHAT族
                    osInfo.setFullName(fedoraRelease);

                    // 设置简洁的显示名称
                    osInfo.setDisplayName("Fedora");

                    if (versionId != null) {
                        osInfo.setVersionId(versionId);
                        osInfo.setVersion(versionId);
                        osInfo.setDistributionName("Fedora " + versionId);

                        logger.info("通过fedora-release识别为Fedora系统，版本: {}", versionId);
                    } else {
                        osInfo.setDistributionName("Fedora");
                    }
                }
            } catch (Exception e) {
                logger.warn("检查CentOS/RHEL/Fedora系统时出错: {}", e.getMessage());
            }
        }

        /**
         * 检查/etc/os-release文件
         */
        private void checkOsRelease(ClientSession session, OsInfo osInfo) {
            try {
                String osRelease = MinaUtils.execCmdWithResult(session, "cat /etc/os-release 2>/dev/null");
                if (StringUtils.isNotBlank(osRelease)) {
                    logger.debug("获取到/etc/os-release文件内容：\n{}", osRelease);

                    // 提取ID (发行版ID)
                    Pattern idPattern = Pattern.compile("^ID=\"?(.*?)\"?$", Pattern.MULTILINE);
                    Matcher idMatcher = idPattern.matcher(osRelease);
                    if (idMatcher.find()) {
                        String distributionId = idMatcher.group(1).trim().toLowerCase();
                        osInfo.setDistributionId(distributionId);
                        logger.info("从os-release中提取到ID: {}", distributionId);

                        // 对Kylin系统进行特殊处理
                        if ("kylin".equalsIgnoreCase(distributionId)) {
                            logger.info("检测到麒麟系统，进行专门处理");
                            checkKylinSystem(osInfo, osRelease);
                            return; // Kylin系统由专门方法处理
                        }

                        // 根据ID设置distribution和distributionType
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
                                osInfo.setDistribution(StringUtils.capitalize(distributionId));
                        }
                    } else {
                        osInfo.setDistributionId("unknown");
                        osInfo.setDistribution("Other");
                        osInfo.setDistributionType(LinuxDistribution.OTHER);
                    }

                    // 提取NAME (发行版名称)
                    Pattern namePattern = Pattern.compile("^NAME=\"?(.*?)\"?$", Pattern.MULTILINE);
                    Matcher nameMatcher = namePattern.matcher(osRelease);
                    if (nameMatcher.find()) {
                        String distributionName = nameMatcher.group(1).trim();
                        osInfo.setDistributionName(distributionName);
                    } else if (StringUtils.isNotBlank(osInfo.getDistributionId())) {
                        osInfo.setDistributionName(StringUtils.capitalize(osInfo.getDistributionId()));
                    } else {
                        osInfo.setDistributionName("Unknown Linux");
                    }

                    // 提取VERSION_ID (版本号)
                    Pattern versionIdPattern = Pattern.compile("^VERSION_ID=\"?(.*?)\"?$", Pattern.MULTILINE);
                    Matcher versionIdMatcher = versionIdPattern.matcher(osRelease);
                    if (versionIdMatcher.find()) {
                        String versionId = versionIdMatcher.group(1).trim();
                        osInfo.setVersionId(versionId);
                        osInfo.setVersion(versionId);
                    }

                    // 提取PRETTY_NAME (完整名称)
                    Pattern prettyNamePattern = Pattern.compile("^PRETTY_NAME=\"?(.*?)\"?$", Pattern.MULTILINE);
                    Matcher prettyNameMatcher = prettyNamePattern.matcher(osRelease);
                    if (prettyNameMatcher.find()) {
                        String prettyName = prettyNameMatcher.group(1).trim();
                        osInfo.setFullName(prettyName);
                        // 如果还没有设置显示名称，使用prettyName
                        if (StringUtils.isBlank(osInfo.getDisplayName())) {
                            osInfo.setDisplayName(prettyName);
                        }
                    }

                    logger.info("通过os-release识别为{}系统，版本: {}",
                            osInfo.getDistribution(), osInfo.getVersionId());
                }
            } catch (Exception e) {
                logger.warn("检查/etc/os-release时出错: {}", e.getMessage());
            }
        }

        /**
         * 专门处理Kylin系统
         */
        private void checkKylinSystem(OsInfo osInfo, String osRelease) {
            try {
                logger.info("开始处理麒麟系统信息");

                // 设置基本信息
                osInfo.setDistribution("Kylin");
                osInfo.setDistributionType(LinuxDistribution.KYLIN);
                osInfo.setDisplayName("中标麒麟");

                // 提取VERSION_ID (版本号)
                Pattern versionIdPattern = Pattern.compile("^VERSION_ID=\"?(.*?)\"?$", Pattern.MULTILINE);
                Matcher versionIdMatcher = versionIdPattern.matcher(osRelease);
                if (versionIdMatcher.find()) {
                    String versionId = versionIdMatcher.group(1).trim();
                    osInfo.setVersionId(versionId);
                    osInfo.setVersion(versionId);
                    logger.info("麒麟系统版本号: {}", versionId);

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
                } else {
                    // 如果没有找到版本ID，设置默认值
                    osInfo.setDistributionName("中标麒麟");
                }

                // 提取PRETTY_NAME (完整名称)
                Pattern prettyNamePattern = Pattern.compile("^PRETTY_NAME=\"?(.*?)\"?$", Pattern.MULTILINE);
                Matcher prettyNameMatcher = prettyNamePattern.matcher(osRelease);
                if (prettyNameMatcher.find()) {
                    String prettyName = prettyNameMatcher.group(1).trim();
                    osInfo.setFullName(prettyName);
                    logger.info("麒麟系统完整名称: {}", prettyName);
                } else {
                    // 如果没有PRETTY_NAME，根据版本设置一个默认值
                    String versionId = osInfo.getVersionId();
                    if ("V10".equals(versionId) || "10".equals(versionId)) {
                        osInfo.setFullName("Kylin Linux Advanced Server V10 (Halberd)");
                    } else if ("V4".equals(versionId) || "4".equals(versionId)) {
                        osInfo.setFullName("中标麒麟操作系统 V4");
                    } else if (StringUtils.isNotBlank(versionId)) {
                        osInfo.setFullName("中标麒麟操作系统 " + versionId);
                    } else {
                        osInfo.setFullName("中标麒麟操作系统");
                    }
                }

                logger.info("麒麟系统信息处理完成：distribution={}, displayName={}, distributionName={}, fullName={}",
                        osInfo.getDistribution(), osInfo.getDisplayName(), osInfo.getDistributionName(),
                        osInfo.getFullName());
            } catch (Exception e) {
                logger.warn("处理麒麟系统信息时出错: {}", e.getMessage());

                // 出错时设置基本信息，确保不会识别为其他系统
                osInfo.setDistribution("Kylin");
                osInfo.setDistributionType(LinuxDistribution.KYLIN);
                osInfo.setDisplayName("中标麒麟");
                osInfo.setDistributionName("中标麒麟");
                osInfo.setFullName("中标麒麟操作系统");
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
                    String arch = MinaUtils.execCmdWithResult(session,
                            "cmd /c echo %PROCESSOR_ARCHITECTURE%").trim();
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