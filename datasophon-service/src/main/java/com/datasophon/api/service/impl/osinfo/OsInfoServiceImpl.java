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
import com.datasophon.common.model.hardware.MemoryInfo;
import com.datasophon.common.model.hardware.NetworkInfo;
import com.datasophon.common.model.hardware.SwapInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作系统信息服务实现类
 * 负责管理主机操作系统信息的获取和缓存
 * 采用分阶段收集策略：
 * - 第一阶段：使用osInfoExecutor收集主机名和操作系统信息，优先展示给用户
 * - 第二阶段：使用hardwareInfoExecutor收集详细硬件信息，后台处理
 */
@Service
public class OsInfoServiceImpl implements OsInfoService {

    private static final Logger logger = LoggerFactory.getLogger(OsInfoServiceImpl.class);

    // SSH会话缓存，以IP:PORT为键
    private final Map<String, ClientSession> sessionCache = new ConcurrentHashMap<>();

    @Autowired
    private OsInfoCollectorFactory osInfoCollectorFactory;

    // 两个专用线程池
    @Resource(name = "osInfoExecutor")
    private ExecutorService osInfoExecutor;

    @Resource(name = "hardwareInfoExecutor")
    private ExecutorService hardwareInfoExecutor;

    // 保留缓存管理对象
    private final HostInfoCollectionQueueManager queueManager = new HostInfoCollectionQueueManager(this);

    @PostConstruct
    public void init() {
        logger.debug("=====================================================");
        logger.debug("初始化OS信息收集服务，使用分阶段收集策略");
        logger.debug("信息收集流程：");
        logger.debug("1. 基本信息收集：使用osInfoExecutor线程池，同时处理最多5台主机");
        logger.debug("2. 详细信息收集：使用hardwareInfoExecutor线程池，同时处理最多3台主机");
        logger.debug("3. 收集顺序：先收集所有主机的基本信息（主机名和操作系统类型）");
        logger.debug("4. 然后收集详细硬件信息（CPU、内存、磁盘等）");
        logger.debug("5. 每收集完一项信息立即更新缓存，优先展示主机名和操作系统信息");
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
        private static final int MAX_CONCURRENT_HOSTS = 5;

        // 总共处理的主机数量
        private final AtomicInteger totalHostCount = new AtomicInteger(0);

        // 已完成处理的主机数量
        private final AtomicInteger completedHostCount = new AtomicInteger(0);

        // 已完成基本信息收集的主机数量
        private final AtomicInteger basicInfoCompletedCount = new AtomicInteger(0);

        // 等待收集详细信息的主机列表
        private final List<HostInfo> waitForDetailInfoList = new ArrayList<>();

        // 阶段二收集状态
        private final AtomicInteger phase2ProcessingCount = new AtomicInteger(0);

        // 最大同时收集详细信息的主机数量
        private static final int MAX_CONCURRENT_DETAIL_HOSTS = 3;

        public HostInfoCollectionQueueManager(OsInfoServiceImpl service) {
            this.service = service;
        }

        /**
         * 重置所有计数器
         */
        public synchronized void resetCounters() {
            // 重置所有计数器
            totalHostCount.set(0);
            completedHostCount.set(0);
            processingHostCount.set(0);
            basicInfoCompletedCount.set(0);
            phase2ProcessingCount.set(0);

            // 清空队列和列表
            hostQueue.clear();
            sortedHostList.clear();
            waitForDetailInfoList.clear();

            logger.info("所有计数器和队列已重置");
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
            try {
                // 检查是否有主机等待处理，且未超过最大并发限制
                while (!hostQueue.isEmpty() && processingHostCount.get() < MAX_CONCURRENT_HOSTS) {
                    HostInfo hostInfo = hostQueue.poll();
                    if (hostInfo != null) {
                        processingHostCount.incrementAndGet();
                        logger.info("开始处理主机: {}, 当前处理中: {}/{}, 队列中: {}",
                                hostInfo.getIp(), processingHostCount.get(), totalHostCount.get(), hostQueue.size());

                        // 使用osInfoExecutor异步处理
                        CompletableFuture.runAsync(() -> {
                            collectBasicInfoForHost(hostInfo);
                        }, service.osInfoExecutor); // 使用service中的osInfoExecutor
                    }
                }
            } catch (Exception e) {
                logger.error("启动主机处理时出错", e);
            }
        }

        /**
         * 为单台主机收集基本信息（主机名和操作系统类型）
         * 第一阶段信息收集，优先级最高，使用osInfoExecutor处理
         * 收集完成后立即更新缓存，并关闭会话，等待第二阶段收集
         */
        private void collectBasicInfoForHost(HostInfo hostInfo) {
            ClientSession session = null;
            try {
                logger.info("【第一阶段】开始收集主机 {} 的基本信息", hostInfo.getIp());
                long startTime = System.currentTimeMillis();

                // 设置状态为正在收集
                hostInfo.setMessage("正在收集基本信息...");
                service.updateHostInfoCache(hostInfo);

                // 建立SSH连接
                session = connectToHost(hostInfo);
                if (session == null) {
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("无法建立SSH连接");
                    service.updateHostInfoCache(hostInfo);
                    return;
                }

                // 创建一个新的OsInfo对象
                OsInfo osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);

                // 先收集主机名
                collectHostName(hostInfo, session);

                // 再收集操作系统基本信息
                collectOsBasicInfo(hostInfo, session);

                // 关闭会话，第二阶段会重新创建
                if (session != null && session.isOpen()) {
                    try {
                        session.close();
                        logger.info("【第一阶段完成】已关闭主机 {} 的SSH会话，第二阶段将重新建立连接", hostInfo.getIp());
                    } catch (Exception e) {
                        logger.warn("关闭SSH会话时出错: {}", e.getMessage());
                    }
                }

                // 更新缓存
                service.updateHostInfoCache(hostInfo);

                // 记录完成时间
                logger.info("【第一阶段完成】主机 {} 基本信息收集完成，耗时 {}ms",
                        hostInfo.getIp(), System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集主机 {} 基本信息时出错: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("基本信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);

                // 关闭会话
                if (session != null && session.isOpen()) {
                    try {
                        session.close();
                    } catch (Exception ex) {
                        logger.warn("关闭SSH会话时出错: {}", ex.getMessage());
                    }
                }
            } finally {
                // 减少处理计数并处理下一个
                synchronized (HostInfoCollectionQueueManager.this) {
                    processingHostCount.decrementAndGet();

                    // 增加基本信息完成计数
                    int completed = basicInfoCompletedCount.incrementAndGet();
                    int total = totalHostCount.get();

                    // 记录进度
                    logger.info("已完成 {}/{} 台主机的基本信息收集", completed, total);

                    // 将主机添加到等待详细信息收集的列表
                    if (hostInfo.getOsInfoStatus() != OsInfoStatusEnum.ERROR) {
                        synchronized (waitForDetailInfoList) {
                            waitForDetailInfoList.add(hostInfo);
                        }
                    }

                    // 继续处理下一台主机
                    startProcessingIfNeeded();

                    // 检查是否所有主机的基本信息都已收集完成
                    if (completed == total) {
                        logger.info("所有主机的基本信息收集完成，开始第二阶段收集详细信息");
                        startDetailInfoCollection();
                    }
                }
            }
        }

        /**
         * 为单台主机收集详细信息（硬件信息）
         * 第二阶段信息收集，优先级较低，使用hardwareInfoExecutor处理
         * 收集完成后立即更新缓存，并关闭会话
         */
        private void collectDetailInfoForHost(HostInfo hostInfo) {
            try {
                logger.info("【第二阶段】开始收集主机 {} 的详细硬件信息", hostInfo.getIp());
                // 使用hardwareInfoExecutor执行详细信息收集
                CompletableFuture.runAsync(() -> {
                    ClientSession session = null;
                    try {
                        // 获取已有的基础信息
                        OsInfo osInfo = hostInfo.getOsInfo();
                        if (osInfo == null) {
                            osInfo = new OsInfo();
                            hostInfo.setOsInfo(osInfo);
                        }

                        // 创建新的SSH会话
                        session = connectToHost(hostInfo);
                        if (session == null) {
                            logger.error("【第二阶段】无法连接到主机 {}", hostInfo.getIp());
                            hostInfo.setMessage("无法建立SSH连接");
                            service.updateHostInfoCache(hostInfo);
                            return;
                        }

                        // 获取收集器
                        IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector(osInfo.getOsType());
                        if (collector == null) {
                            logger.error("【第二阶段】无法为操作系统 {} 找到合适的信息收集器", osInfo.getOsType());
                            hostInfo.setMessage("不支持的操作系统类型");
                            service.updateHostInfoCache(hostInfo);
                            return;
                        }

                        // 获取详细硬件信息
                        if ("windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                            collectWindowsDetailInfo(hostInfo, session, osInfo);
                        } else {
                            collectLinuxDetailInfo(hostInfo, session, osInfo);
                        }

                        // 更新网络信息
                        collectNetworkInfoNew(hostInfo, osInfo, session, collector);

                        // 更新交换分区信息
                        collectSwapInfo(hostInfo, osInfo, session, collector);

                        hostInfo.setMessage("硬件信息收集完成");
                        service.updateHostInfoCache(hostInfo);

                        logger.info("【第二阶段】主机 {} 的详细硬件信息收集完成", hostInfo.getIp());
                    } catch (Exception e) {
                        logger.error("【第二阶段】收集主机 {} 的详细硬件信息时出错", hostInfo.getIp(), e);
                        hostInfo.setMessage("硬件信息收集出错: " + e.getMessage());
                        service.updateHostInfoCache(hostInfo);
                    } finally {
                        // 关闭会话
                        if (session != null && session.isOpen()) {
                            try {
                                session.close();
                                logger.debug("【第二阶段】已关闭主机 {} 的SSH会话", hostInfo.getIp());
                            } catch (Exception e) {
                                logger.warn("【第二阶段】关闭主机 {} 的SSH会话时出错", hostInfo.getIp(), e);
                            }
                        }

                        // 更新计数器并启动下一个任务
                        synchronized (HostInfoCollectionQueueManager.this) {
                            phase2ProcessingCount.decrementAndGet();
                            completedHostCount.incrementAndGet();
                            logger.info("【第二阶段】详细硬件信息收集完成，已完成: {}/{}, 正在处理: {}",
                                    completedHostCount.get(), totalHostCount.get(), phase2ProcessingCount.get());
                            startDetailInfoCollection();
                        }
                    }
                }, service.hardwareInfoExecutor); // 使用service中的hardwareInfoExecutor
            } catch (Exception e) {
                logger.error("【第二阶段】提交详细信息收集任务时出错", e);
                synchronized (this) {
                    phase2ProcessingCount.decrementAndGet();
                    completedHostCount.incrementAndGet();
                    startDetailInfoCollection();
                }
            }
        }

        /**
         * 收集主机名
         */
        private void collectHostName(HostInfo hostInfo, ClientSession session) {
            logger.info("开始收集主机名: {}", hostInfo.getIp());
            long startTime = System.currentTimeMillis();

            // 设置状态为正在收集
            hostInfo.setHostnameStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集主机名...");
            service.updateHostInfoCache(hostInfo);

            try {
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
         * 收集操作系统基本信息
         */
        private void collectOsBasicInfo(HostInfo hostInfo, ClientSession session) {
            try {
                logger.info("开始收集主机 [{}] 的操作系统基本信息", hostInfo.getIp());
                hostInfo.setOsStatus(OsInfoStatusEnum.COLLECTING);

                // 创建OsInfo对象
                OsInfo osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);

                // 检测操作系统类型
                String osType = service.detectOperatingSystemType(session);
                osInfo.setOsType(osType);

                // 获取操作系统基本信息
                if ("linux".equalsIgnoreCase(osType)) {
                    // 预先检查常见的Linux发行版文件
                    preCheckLinuxDistribution(session, osInfo);

                    // 收集基本版本信息
                    collectLinuxBasicInfo(session, osInfo);
                } else if ("windows".equalsIgnoreCase(osType)) {
                    // 收集Windows基本版本信息
                    collectWindowsBasicInfo(session, osInfo);
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
                hostInfo.setMessage("操作系统基本信息收集完成");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 [{}] 的操作系统基本信息收集完成: {}", hostInfo.getIp(),
                        osInfo.getDistribution() + " " + osInfo.getVersion());
            } catch (Exception e) {
                logger.error("收集主机 [{}] 的操作系统基本信息时出错: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setOsStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("收集操作系统基本信息失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 收集Linux基本信息（仅系统版本相关）
         */
        public void collectLinuxBasicInfo(ClientSession session, OsInfo osInfo) {
            try {
                // 获取内核版本
                String kernel = executeCommand(session, "uname -r").trim();
                osInfo.setKernelVersion(kernel);

                // 获取架构
                String arch = executeCommand(session, "uname -m").trim();
                osInfo.setArchitecture(arch);

                // 获取Linux版本信息
                if (osInfo.getDistributionId() == null) {
                    // 尝试从/etc/os-release获取
                    checkOsRelease(session, osInfo);
                }

                // 检查处理器信息（仅基本信息）
                String processor = executeCommand(session, "grep 'model name' /proc/cpuinfo | head -1").trim();
                if (processor.contains(":")) {
                    processor = processor.split(":", 2)[1].trim();
                    if (osInfo.getCpuInfo() == null) {
                        osInfo.setCpuInfo(new CpuInfo());
                    }
                    osInfo.getCpuInfo().setModel(processor);
                }

                // 确保设置了基本显示名称
                if (osInfo.getDisplayName() == null) {
                    if (StringUtils.isNotBlank(osInfo.getDistribution())) {
                        if (StringUtils.isNotBlank(osInfo.getVersion())) {
                            osInfo.setDisplayName(osInfo.getDistribution() + " " + osInfo.getVersion());
                        } else {
                            osInfo.setDisplayName(osInfo.getDistribution());
                        }
                    } else {
                        osInfo.setDisplayName("Linux");
                    }
                }

                // 标记系统信息有效
                osInfo.setValid(true);

            } catch (Exception e) {
                logger.error("收集Linux基本信息时出错: {}", e.getMessage(), e);
            }
        }

        /**
         * 收集Windows基本信息（仅系统版本相关）
         */
        private void collectWindowsBasicInfo(ClientSession session, OsInfo osInfo) {
            try {
                // 获取Windows版本
                String winVer = MinaUtils.execCmdWithResult(session, "cmd /c ver").trim();
                osInfo.setKernelVersion(winVer);

                // 设置分发版信息
                osInfo.setDistribution("Windows");
                osInfo.setDistributionId("windows");

                // 设置架构
                String arch = MinaUtils.execCmdWithResult(session, "cmd /c echo %PROCESSOR_ARCHITECTURE%").trim();
                osInfo.setArchitecture(arch.equalsIgnoreCase("AMD64") ? "x86_64" : arch);

                // 获取系统信息
                try {
                    String sysInfo = MinaUtils.execCmdWithResult(session,
                            "cmd /c systeminfo | findstr /B /C:\"OS\" /C:\"系统\" /C:\"注册\" /C:\"Registered\"");
                    if (StringUtils.isNotBlank(sysInfo)) {
                        // 使用distributionName存储详细系统信息
                        osInfo.setDistributionName("Windows " + sysInfo.replace("\r\n", " | "));

                        // 解析版本信息
                        if (sysInfo.contains("Windows 10")) {
                            osInfo.setVersion("10");
                            osInfo.setVersionId("10");
                        } else if (sysInfo.contains("Windows 11")) {
                            osInfo.setVersion("11");
                            osInfo.setVersionId("11");
                        } else if (sysInfo.contains("Windows Server 2019")) {
                            osInfo.setVersion("2019");
                            osInfo.setVersionId("2019");
                        } else if (sysInfo.contains("Windows Server 2022")) {
                            osInfo.setVersion("2022");
                            osInfo.setVersionId("2022");
                        }
                    }
                } catch (Exception e) {
                    logger.warn("获取Windows系统详细信息失败: {}", e.getMessage());
                }

                // 设置显示名称
                if (StringUtils.isNotBlank(osInfo.getVersion())) {
                    osInfo.setDisplayName("Windows " + osInfo.getVersion());
                } else {
                    osInfo.setDisplayName("Windows");
                }

                // 标记系统信息有效
                osInfo.setValid(true);

            } catch (Exception e) {
                logger.error("收集Windows基本信息时出错: {}", e.getMessage(), e);
            }
        }

        /**
         * 收集Linux详细信息（硬件信息）
         */
        private void collectLinuxDetailInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo) {
            try {
                logger.info("开始收集Linux详细硬件信息: {}", hostInfo.getIp());

                // 获取适用于Linux的收集器
                IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector("linux");
                if (collector != null) {
                    // 收集硬件信息
                    collector.collectHardwareInfo(osInfo, session,
                            (info) -> service.updateHostInfoCache(hostInfo));

                    // 更新状态
                    hostInfo.setOsInfo(osInfo);
                    hostInfo.setMessage("Linux硬件信息收集完成");
                    service.updateHostInfoCache(hostInfo);

                    logger.info("Linux硬件信息收集成功: {}", hostInfo.getIp());
                } else {
                    logger.error("找不到Linux系统信息收集器");
                    hostInfo.setMessage("找不到Linux系统信息收集器");
                    service.updateHostInfoCache(hostInfo);
                }
            } catch (Exception e) {
                logger.error("收集Linux硬件信息时出错: {}", e.getMessage(), e);
                hostInfo.setMessage("收集Linux硬件信息失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 收集Windows详细信息（硬件信息）
         */
        private void collectWindowsDetailInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo) {
            try {
                logger.info("开始收集Windows详细硬件信息: {}", hostInfo.getIp());

                // 获取适用于Windows的收集器
                IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector("windows");
                if (collector != null) {
                    // 收集硬件信息
                    collector.collectHardwareInfo(osInfo, session,
                            (info) -> service.updateHostInfoCache(hostInfo));

                    // 更新状态
                    hostInfo.setOsInfo(osInfo);
                    hostInfo.setMessage("Windows硬件信息收集完成");
                    service.updateHostInfoCache(hostInfo);

                    logger.info("Windows硬件信息收集成功: {}", hostInfo.getIp());
                } else {
                    logger.error("找不到Windows系统信息收集器");
                    hostInfo.setMessage("找不到Windows系统信息收集器");
                    service.updateHostInfoCache(hostInfo);
                }
            } catch (Exception e) {
                logger.error("收集Windows硬件信息时出错: {}", e.getMessage(), e);
                hostInfo.setMessage("收集Windows硬件信息失败: " + e.getMessage());
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

                // 创建网卡列表
                List<OsInfoLegacy.NetworkInterface> legacyNetworkInterfaces = new ArrayList<>();

                if (osInfo.getDistributionId() != null && "windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集网卡信息
                    collectWindowsNetworkInfo(session, legacyNetworkInterfaces);
                } else {
                    // Linux系统收集网卡信息
                    collectLinuxNetworkInfo(session, legacyNetworkInterfaces);
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
         * 收集Windows系统网卡信息
         */
        private void collectWindowsNetworkInfo(ClientSession session,
                List<OsInfoLegacy.NetworkInterface> networkInterfaces) {
            try {
                // 获取网卡基本信息（包括名称、状态、IP地址）
                String nicListCmd = "powershell -command \"Get-NetAdapter | Select-Object Name,InterfaceDescription,Status,MacAddress,LinkSpeed,MediaType | ConvertTo-Json\"";
                String nicList = MinaUtils.execCmdWithResult(session, nicListCmd);

                // 获取IP地址信息
                String ipConfigCmd = "powershell -command \"Get-NetIPAddress | Select-Object InterfaceAlias,IPAddress,PrefixLength,AddressFamily | ConvertTo-Json\"";
                String ipConfig = MinaUtils.execCmdWithResult(session, ipConfigCmd);

                if (StringUtils.isNotBlank(nicList) && StringUtils.isNotBlank(ipConfig)) {
                    parseWindowsNetworkInfo(networkInterfaces, nicList, ipConfig);
                }
            } catch (Exception e) {
                logger.error("收集Windows网卡信息失败", e);
                throw e;
            }
        }

        /**
         * 收集Linux系统网卡信息
         */
        private void collectLinuxNetworkInfo(ClientSession session,
                List<OsInfoLegacy.NetworkInterface> networkInterfaces) {
            try {
                // 获取网卡列表（排除虚拟网卡和回环接口）
                String ifconfigCmd = "ip -o addr show | grep -v 'lo\\|docker\\|veth\\|br-\\|tun\\|tap' | awk '{print $2}' | sort | uniq";
                String nicList = MinaUtils.execCmdWithResult(session, ifconfigCmd);

                if (StringUtils.isNotBlank(nicList)) {
                    String[] nics = nicList.split("\\n");
                    for (String nic : nics) {
                        nic = nic.trim();
                        if (StringUtils.isBlank(nic)) {
                            continue;
                        }

                        // 创建网卡对象
                        OsInfoLegacy.NetworkInterface netInterface = new OsInfoLegacy.NetworkInterface();
                        netInterface.setName(nic);

                        // 获取网卡状态
                        String statusCmd = "cat /sys/class/net/" + nic + "/operstate 2>/dev/null || echo 'unknown'";
                        String status = MinaUtils.execCmdWithResult(session, statusCmd).trim();
                        // 更准确的状态判断
                        boolean isUp = "up".equalsIgnoreCase(status);
                        netInterface.setUp(isUp);

                        // 获取网卡是否启用
                        String adminStatusCmd = "cat /sys/class/net/" + nic + "/flags 2>/dev/null || echo '0'";
                        String adminStatus = MinaUtils.execCmdWithResult(session, adminStatusCmd).trim();
                        // 检查网卡是否被禁用
                        boolean isDisabled = "0".equals(adminStatus) || "down".equalsIgnoreCase(status);

                        // 设置网卡状态描述
                        if (isDisabled) {
                            netInterface.setStatus("已禁用");
                        } else if (!isUp) {
                            netInterface.setStatus("未连接");
                        } else {
                            netInterface.setStatus("已连接");
                        }

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

                        // 获取网卡型号和驱动信息
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
                        networkInterfaces.add(netInterface);
                    }
                }
            } catch (Exception e) {
                logger.error("收集Linux网卡信息失败", e);
                throw e;
            }
        }

        /**
         * 解析Windows网卡信息
         */
        private void parseWindowsNetworkInfo(List<OsInfoLegacy.NetworkInterface> networkInterfaces,
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
                    String mediaType = nic.getString("MediaType");

                    // 跳过没有名称的网卡
                    if (StringUtils.isBlank(name)) {
                        continue;
                    }

                    // 创建网卡对象
                    OsInfoLegacy.NetworkInterface netInterface = new OsInfoLegacy.NetworkInterface();
                    netInterface.setName(name);
                    // Windows系统网卡状态判断
                    boolean isUp = "Up".equalsIgnoreCase(status);
                    netInterface.setUp(isUp);

                    // 设置网卡状态描述
                    if ("Disconnected".equalsIgnoreCase(status)) {
                        netInterface.setStatus("已断开");
                    } else if ("Down".equalsIgnoreCase(status)) {
                        netInterface.setStatus("已禁用");
                    } else if (isUp) {
                        netInterface.setStatus("已连接");
                    } else {
                        netInterface.setStatus("未连接");
                    }
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
                throw e;
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

                // 创建网卡列表
                List<OsInfoLegacy.NetworkInterface> legacyNetworkInterfaces = new ArrayList<>();

                if (osInfo.getDistributionId() != null && "windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集网卡信息
                    collectWindowsNetworkInfo(session, legacyNetworkInterfaces);
                } else {
                    // Linux系统收集网卡信息
                    collectLinuxNetworkInfo(session, legacyNetworkInterfaces);
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

        private void collectSwapInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            if (osInfo == null) {
                return;
            }

            try {
                logger.info("开始为主机 [{}] 收集交换空间信息", hostInfo.getIp());
                long startTime = System.currentTimeMillis();

                // 设置状态为加载中
                osInfo.setSwapStatus(OsInfoStatusEnum.LOADING);

                if (session == null || !session.isOpen()) {
                    logger.error("主机 [{}] 的SSH会话未建立或已关闭", hostInfo.getIp());
                    osInfo.setSwapStatus(OsInfoStatusEnum.ERROR);
                    return;
                }

                // 创建交换空间信息对象
                SwapInfo swapInfo = new SwapInfo();
                swapInfo.setEnabled(false); // 默认未启用

                if (osInfo.getDistributionId() != null && "windows".equalsIgnoreCase(osInfo.getDistributionId())) {
                    // Windows系统收集交换空间信息
                    collectWindowsSwapInfo(session, swapInfo);
                } else {
                    // Linux系统收集交换空间信息
                    collectLinuxSwapInfo(session, swapInfo);
                }

                // 设置交换空间信息
                osInfo.setSwapInfo(swapInfo);

                // 更新状态
                osInfo.setSwapStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("交换空间信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("交换空间信息收集完成: {}, 用时: {}ms", hostInfo.getIp(), (System.currentTimeMillis() - startTime));
            } catch (Exception e) {
                logger.error("收集交换空间信息失败: {}", hostInfo.getIp(), e);
                osInfo.setSwapStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("交换空间信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 收集Windows系统交换空间信息
         */
        private void collectWindowsSwapInfo(ClientSession session, SwapInfo swapInfo) {
            try {
                // 获取页面文件信息
                String pageFileCmd = "powershell -command \"Get-WmiObject Win32_PageFileSetting | Select-Object InitialSize,MaximumSize | ConvertTo-Json\"";
                String pageFileInfo = MinaUtils.execCmdWithResult(session, pageFileCmd);

                if (StringUtils.isNotBlank(pageFileInfo)) {
                    JSONArray pageFiles = JSON.parseArray(pageFileInfo);
                    if (pageFiles != null && !pageFiles.isEmpty()) {
                        // 计算总大小（MB）
                        long totalSize = 0;
                        for (int i = 0; i < pageFiles.size(); i++) {
                            JSONObject pageFile = pageFiles.getJSONObject(i);
                            totalSize += pageFile.getLongValue("MaximumSize");
                        }

                        // 设置交换空间信息
                        swapInfo.setEnabled(true);
                        swapInfo.setTotalSwap(totalSize * 1024 * 1024); // 转换为字节
                        swapInfo.setAvailableSwap(totalSize * 1024 * 1024); // Windows下无法获取可用空间

                        // 设置格式化后的值和单位
                        swapInfo.setTotalSwapFormatted(String.format("%.1f", totalSize / 1024.0));
                        swapInfo.setTotalSwapUnit("GB");
                        swapInfo.setAvailableSwapFormatted(String.format("%.1f", totalSize / 1024.0));
                        swapInfo.setAvailableSwapUnit("GB");
                        swapInfo.setUsedSwapFormatted("0.0");
                        swapInfo.setUsedSwapUnit("GB");
                    }
                }
            } catch (Exception e) {
                logger.error("收集Windows交换空间信息失败", e);
                throw e;
            }
        }

        /**
         * 收集Linux系统交换空间信息
         */
        private void collectLinuxSwapInfo(ClientSession session, SwapInfo swapInfo) {
            try {
                // 获取交换空间信息
                String swapInfoCmd = "free -b | grep Swap";
                String swapInfoStr = MinaUtils.execCmdWithResult(session, swapInfoCmd);

                if (StringUtils.isNotBlank(swapInfoStr)) {
                    // 解析交换空间信息
                    String[] parts = swapInfoStr.split("\\s+");
                    if (parts.length >= 4) {
                        long total = Long.parseLong(parts[1]);
                        long used = Long.parseLong(parts[2]);
                        long free = Long.parseLong(parts[3]);

                        // 设置交换空间信息
                        swapInfo.setEnabled(true);
                        swapInfo.setTotalSwap(total);
                        swapInfo.setAvailableSwap(free);

                        // 计算使用率
                        double usagePercent = total > 0 ? (100.0 * used / total) : 0;
                        swapInfo.setUsagePercent(usagePercent);

                        // 设置格式化后的值和单位
                        swapInfo.setTotalSwapFormatted(String.format("%.1f", total / (1024.0 * 1024 * 1024)));
                        swapInfo.setTotalSwapUnit("GB");
                        swapInfo.setAvailableSwapFormatted(String.format("%.1f", free / (1024.0 * 1024 * 1024)));
                        swapInfo.setAvailableSwapUnit("GB");
                        swapInfo.setUsedSwapFormatted(String.format("%.1f", used / (1024.0 * 1024 * 1024)));
                        swapInfo.setUsedSwapUnit("GB");
                    }
                }
            } catch (Exception e) {
                logger.error("收集Linux交换空间信息失败", e);
                throw e;
            }
        }

        /**
         * 开始第二阶段详细信息收集
         */
        private synchronized void startDetailInfoCollection() {
            logger.info("开始第二阶段详细信息收集，等待队列中有 {} 台主机", waitForDetailInfoList.size());

            // 按照IP排序处理
            waitForDetailInfoList.sort((h1, h2) -> {
                if (h1.getIp() == null || h2.getIp() == null) {
                    return 0;
                }
                return h1.getIp().compareTo(h2.getIp());
            });

            // 批量启动详细信息收集
            while (phase2ProcessingCount.get() < MAX_CONCURRENT_DETAIL_HOSTS && !waitForDetailInfoList.isEmpty()) {
                HostInfo hostInfo;
                synchronized (waitForDetailInfoList) {
                    if (waitForDetailInfoList.isEmpty()) {
                        break;
                    }
                    hostInfo = waitForDetailInfoList.remove(0);
                }

                if (hostInfo != null) {
                    phase2ProcessingCount.incrementAndGet();
                    collectDetailInfoForHost(hostInfo);
                }
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
                // 只有在没有主机名的情况下才设置SSH连接状态为ERROR
                if (StringUtils.isBlank(hostInfo.getHostname()) ||
                        hostInfo.getHostname().equals(hostInfo.getIp())) {
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                }
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
            // 设置操作系统信息错误状态
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);

            // 只有在没有主机名的情况下才设置SSH连接状态为ERROR
            if (StringUtils.isBlank(hostInfo.getHostname()) ||
                    hostInfo.getHostname().equals(hostInfo.getIp())) {
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
            }

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

    // 在OsInfoServiceImpl类中添加以下方法实现

    /**
     * 收集主机名信息
     * 
     * @param hostInfo 主机信息
     */
    @Override
    public void collectHostnameInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectHostnameInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                updateHostInfoCache(hostInfo);
                return;
            }

            // 设置状态为收集中
            hostInfo.setHostnameStatus(OsInfoStatusEnum.LOADING);
            updateHostInfoCache(hostInfo);

            // 收集主机名 - 先尝试hostname -f
            String hostname = MinaUtils.execCmdWithResult(session, "hostname -f");

            // 检查执行结果是否包含退出码前缀
            if (hostname != null && hostname.startsWith("EXIT_CODE_")) {
                logger.warn("获取主机全名失败: {}，尝试获取短主机名", hostname);

                // 尝试仅获取短主机名
                hostname = MinaUtils.execCmdWithResult(session, "hostname");

                // 如果短主机名也失败，记录错误并设置状态
                if (hostname != null && hostname.startsWith("EXIT_CODE_")) {
                    logger.error("获取主机名失败: {}", hostname);
                    hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                    // 对于Windows系统，可能需要特殊处理
                    if (hostname.contains("not recognized") || hostname.contains("不是内部或外部命令")) {
                        // 尝试Windows特定命令
                        hostname = MinaUtils.execCmdWithResult(session, "cmd /c hostname");
                        if (hostname != null && !hostname.startsWith("EXIT_CODE_")) {
                            hostname = hostname.trim();
                            hostInfo.setHostname(hostname);
                            hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
                            logger.info("使用Windows命令成功获取主机名: {}", hostname);
                        } else {
                            // 使用IP地址作为主机名
                            hostInfo.setHostname(hostInfo.getIp());
                            logger.warn("无法获取主机名，使用IP地址[{}]作为主机名", hostInfo.getIp());
                        }
                    } else {
                        // 使用IP地址作为主机名
                        hostInfo.setHostname(hostInfo.getIp());
                        logger.warn("无法获取主机名，使用IP地址[{}]作为主机名", hostInfo.getIp());
                    }
                }
            }

            // 如果获取到了有效的主机名
            if (hostname != null && !hostname.startsWith("EXIT_CODE_")) {
                hostname = hostname.trim();
                hostInfo.setHostname(hostname);
                hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
                logger.info("主机[{}]主机名收集成功: {}", hostInfo.getIp(), hostname);
            }

            // 更新缓存
            updateHostInfoCache(hostInfo);
        } catch (Exception e) {
            logger.error("收集主机[{}]主机名时发生异常: {}", hostInfo.getIp(), e.getMessage());
            hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
            // 使用IP地址作为主机名
            hostInfo.setHostname(hostInfo.getIp());
            logger.warn("发生异常，使用IP地址[{}]作为主机名", hostInfo.getIp());
            updateHostInfoCache(hostInfo);
        } finally {
            // 不关闭会话，留给其他方法使用
        }
    }

    /**
     * 收集操作系统基本信息
     * 
     * @param hostInfo 主机信息
     */
    @Override
    public void collectOsBasicInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectOsBasicInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                return;
            }

            // 设置状态为收集中
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
            updateHostInfoCache(hostInfo);

            // 初始化或获取OsInfo
            OsInfo osInfo = hostInfo.getOsInfo();
            if (osInfo == null) {
                osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);
            }

            // 检测操作系统类型
            String osType = detectOperatingSystemType(session);

            // 设置操作系统类型
            osInfo.setOsType(osType);
            logger.info("设置操作系统类型: {}", osType);

            // 根据操作系统类型收集基本信息
            if ("Linux".equalsIgnoreCase(osType)) {
                queueManager.collectLinuxBasicInfo(session, osInfo);
            } else if ("Windows".equalsIgnoreCase(osType)) {
                queueManager.collectWindowsBasicInfo(session, osInfo);
            } else {
                logger.warn("不支持的操作系统类型: {}", osType);
            }

            // 更新状态
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);

            // 更新缓存
            updateHostInfoCache(hostInfo);

            logger.info("主机[{}]操作系统基本信息收集成功", hostInfo.getIp());
        } catch (Exception e) {
            logger.error("收集主机[{}]操作系统基本信息时发生异常: {}", hostInfo.getIp(), e.getMessage());
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
            updateHostInfoCache(hostInfo);
        } finally {
            // 不关闭会话，留给其他方法使用
        }
    }

    /**
     * 收集DNS配置信息
     * 
     * @param hostInfo 主机信息
     */
    @Override
    public void collectDnsInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectDnsInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setDnsStatus(OsInfoStatusEnum.ERROR);
                    updateHostInfoCache(hostInfo);
                }
                return;
            }

            // 获取OsInfo
            OsInfo osInfo = hostInfo.getOsInfo();
            if (osInfo == null) {
                osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);
            }

            // 设置状态为收集中
            osInfo.setDnsStatus(OsInfoStatusEnum.LOADING);

            // 检测操作系统类型
            String osType = osInfo.getOsType();
            if (osType == null) {
                osType = detectOperatingSystemType(session);
                osInfo.setOsType(osType);
            }

            // 根据不同操作系统类型使用不同的收集方法
            if ("windows".equalsIgnoreCase(osType)) {
                collectWindowsDnsInfo(session, osInfo, hostInfo.getIp());
            } else {
                collectLinuxDnsInfo(session, osInfo, hostInfo.getIp());
            }

            // 更新缓存
            updateHostInfoCache(hostInfo);
        } catch (Exception e) {
            logger.error("收集主机[{}]DNS配置时发生异常: {}", hostInfo.getIp(), e.getMessage());
            // 设置错误状态
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setDnsStatus(OsInfoStatusEnum.ERROR);
                updateHostInfoCache(hostInfo);
            }
        } finally {
            // 不关闭会话，留给其他方法使用
        }
    }

    /**
     * 收集Windows系统的DNS信息
     */
    private void collectWindowsDnsInfo(ClientSession session, OsInfo osInfo, String ip) {
        try {
            // 执行Windows命令获取DNS信息
            MinaUtils.CommandResult dnsInfoResult = MinaUtils.execCmdWithResultObject(session,
                    "powershell -command \"Get-DnsClientServerAddress | Select-Object -ExpandProperty ServerAddresses | Where-Object {$_ -notmatch '^::' -and $_ -notmatch '^fe80'} | Select-Object -Unique\"");

            if (dnsInfoResult.isSuccess()) {
                String dnsInfo = dnsInfoResult.getOutput();
                // 解析DNS信息
                List<String> dnsServerList = new ArrayList<>();
                String[] lines = dnsInfo.split("\r?\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("::") && !line.startsWith("fe80")) {
                        dnsServerList.add(line);
                    }
                }

                // 设置DNS服务器列表
                if (!dnsServerList.isEmpty()) {
                    osInfo.setDnsServers(dnsServerList);
                    logger.info("主机[{}]Windows DNS配置收集成功，发现{}个DNS服务器", ip, dnsServerList.size());
                    osInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);
                } else {
                    // 尝试备用方法
                    MinaUtils.CommandResult ipconfigResult = MinaUtils.execCmdWithResultObject(session,
                            "powershell -command \"ipconfig /all | Select-String 'DNS Servers'\"");

                    if (ipconfigResult.isSuccess()) {
                        dnsServerList = parseDnsFromIpconfig(ipconfigResult.getOutput());
                        if (!dnsServerList.isEmpty()) {
                            osInfo.setDnsServers(dnsServerList);
                            logger.info("主机[{}]Windows DNS配置(备用方法)收集成功，发现{}个DNS服务器", ip, dnsServerList.size());
                            osInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);
                        } else {
                            logger.warn("主机[{}]Windows未找到DNS服务器配置", ip);
                            osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
                        }
                    } else {
                        logger.warn("主机[{}]Windows DNS配置收集失败: {}", ip, ipconfigResult.getError());
                        osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
                    }
                }
            } else {
                logger.warn("主机[{}]Windows DNS配置收集失败: {}", ip, dnsInfoResult.getError());
                osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
            }
        } catch (Exception e) {
            logger.error("收集Windows DNS配置时发生异常: {}", e.getMessage());
            osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
        }
    }

    /**
     * 收集Linux系统的DNS信息
     */
    private void collectLinuxDnsInfo(ClientSession session, OsInfo osInfo, String ip) {
        // 获取DNS配置
        MinaUtils.CommandResult dnsConfigResult = MinaUtils.execCmdWithResultObject(session, "cat /etc/resolv.conf");

        if (dnsConfigResult.isSuccess()) {
            String dnsConfig = dnsConfigResult.getOutput();
            // 解析DNS配置
            List<String> dnsServerList = new ArrayList<>();
            String[] lines = dnsConfig.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("nameserver")) {
                    String dnsServer = line.substring("nameserver".length()).trim();
                    if (StringUtils.isNotBlank(dnsServer)) {
                        dnsServerList.add(dnsServer);
                    }
                }
            }

            // 设置DNS服务器列表
            if (!dnsServerList.isEmpty()) {
                osInfo.setDnsServers(dnsServerList);
                logger.info("主机[{}]DNS配置收集成功，发现{}个DNS服务器", ip, dnsServerList.size());
                osInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);
            } else {
                // 尝试备用方法
                MinaUtils.CommandResult nmcliResult = MinaUtils.execCmdWithResultObject(session,
                        "nmcli dev show | grep DNS");
                if (nmcliResult.isSuccess()) {
                    dnsServerList = parseDnsFromNmcli(nmcliResult.getOutput());
                    if (!dnsServerList.isEmpty()) {
                        osInfo.setDnsServers(dnsServerList);
                        logger.info("主机[{}]DNS配置(备用方法)收集成功，发现{}个DNS服务器", ip, dnsServerList.size());
                        osInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);
                    } else {
                        logger.warn("主机[{}]未找到DNS服务器配置", ip);
                        // 设置一个默认的DNS服务器避免前端显示问题
                        dnsServerList.add("8.8.8.8");
                        osInfo.setDnsServers(dnsServerList);
                        osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
                    }
                } else {
                    logger.warn("主机[{}]未找到DNS服务器配置，使用默认值", ip);
                    // 设置一个默认的DNS服务器避免前端显示问题
                    dnsServerList.add("8.8.8.8");
                    osInfo.setDnsServers(dnsServerList);
                    osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
                }
            }
        } else {
            logger.warn("主机[{}]DNS配置文件读取失败: {}", ip, dnsConfigResult.getError());
            // 尝试备用方法
            MinaUtils.CommandResult nmcliResult = MinaUtils.execCmdWithResultObject(session,
                    "nmcli dev show | grep DNS");
            if (nmcliResult.isSuccess()) {
                List<String> dnsServerList = parseDnsFromNmcli(nmcliResult.getOutput());
                if (!dnsServerList.isEmpty()) {
                    osInfo.setDnsServers(dnsServerList);
                    logger.info("主机[{}]DNS配置(备用方法)收集成功，发现{}个DNS服务器", ip, dnsServerList.size());
                    osInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);
                } else {
                    logger.warn("主机[{}]未找到DNS服务器配置，使用默认值", ip);
                    List<String> defaultDns = new ArrayList<>();
                    defaultDns.add("8.8.8.8");
                    osInfo.setDnsServers(defaultDns);
                    osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
                }
            } else {
                logger.warn("主机[{}]DNS配置收集失败，使用默认值", ip);
                List<String> defaultDns = new ArrayList<>();
                defaultDns.add("8.8.8.8");
                osInfo.setDnsServers(defaultDns);
                osInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
            }
        }
    }

    /**
     * 从ipconfig输出解析DNS服务器
     */
    private List<String> parseDnsFromIpconfig(String ipconfigOutput) {
        List<String> dnsServers = new ArrayList<>();
        if (ipconfigOutput == null || ipconfigOutput.isEmpty()) {
            return dnsServers;
        }

        // 解析ipconfig输出中的DNS服务器信息
        String[] lines = ipconfigOutput.split("\r?\n");
        for (String line : lines) {
            if (line.contains("DNS Servers") || line.contains("DNS 服务器")) {
                // 提取冒号后面的部分
                int colonPos = line.indexOf(':');
                if (colonPos > 0 && colonPos < line.length() - 1) {
                    String dnsServer = line.substring(colonPos + 1).trim();
                    if (!dnsServer.isEmpty() && !dnsServer.equals("::") && !dnsServer.startsWith("fe80")) {
                        dnsServers.add(dnsServer);
                    }
                }
            }
        }

        return dnsServers;
    }

    /**
     * 从nmcli输出解析DNS服务器
     */
    private List<String> parseDnsFromNmcli(String nmcliOutput) {
        List<String> dnsServers = new ArrayList<>();
        if (nmcliOutput == null || nmcliOutput.isEmpty()) {
            return dnsServers;
        }

        // 解析nmcli输出中的DNS服务器信息
        String[] lines = nmcliOutput.split("\n");
        for (String line : lines) {
            if (line.contains("DNS")) {
                // 提取冒号后面的部分
                int colonPos = line.indexOf(':');
                if (colonPos > 0 && colonPos < line.length() - 1) {
                    String dnsServer = line.substring(colonPos + 1).trim();
                    if (!dnsServer.isEmpty() && !dnsServer.equals("::") && !dnsServer.startsWith("fe80")) {
                        dnsServers.add(dnsServer);
                    }
                }
            }
        }

        return dnsServers;
    }

    /**
     * 收集hosts文件信息
     * 
     * @param hostInfo 主机信息
     */
    @Override
    public void collectHostsFileInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectHostsFileInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                return;
            }

            // 获取hosts文件
            String hostsFile = MinaUtils.execCmdWithResult(session, "cat /etc/hosts");
            if (StringUtils.isNotBlank(hostsFile)) {
                hostInfo.setHostsFile(hostsFile);
                logger.info("主机[{}]hosts文件收集成功", hostInfo.getIp());
            } else {
                logger.warn("主机[{}]hosts文件收集失败", hostInfo.getIp());
            }

            // 更新缓存
            updateHostInfoCache(hostInfo);
        } catch (Exception e) {
            logger.error("收集主机[{}]hosts文件时发生异常: {}", hostInfo.getIp(), e.getMessage());
        } finally {
            // 不关闭会话，留给其他方法使用
        }
    }

    /**
     * 收集CPU信息
     * 
     * @param hostInfo 主机信息
     */
    @Override
    public void collectCpuInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectCpuInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                return;
            }

            // 获取OsInfo
            OsInfo osInfo = hostInfo.getOsInfo();
            if (osInfo == null) {
                osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);
            }

            // 获取CPU信息
            // 获取系统类型
            String osType = osInfo.getDistributionId() != null ? osInfo.getDistributionId() : "Linux";
            if ("Linux".equalsIgnoreCase(osType)) {
                // 使用Linux的CPU信息收集方法
                String cpuInfo = MinaUtils.execCmdWithResult(session, "lscpu");
                if (StringUtils.isNotBlank(cpuInfo)) {
                    // 解析CPU信息并存储
                    processCpuInfo(osInfo, cpuInfo);
                    logger.info("主机[{}]CPU信息收集成功", hostInfo.getIp());
                }
            } else {
                logger.info("暂不支持收集非Linux系统的CPU信息");
            }

            // 更新缓存
            updateHostInfoCache(hostInfo);
        } catch (Exception e) {
            logger.error("收集主机[{}]CPU信息时发生异常: {}", hostInfo.getIp(), e.getMessage());
        } finally {
            // 不关闭会话，留给其他方法使用
        }
    }

    /**
     * 处理CPU信息
     */
    private void processCpuInfo(OsInfo osInfo, String cpuInfoStr) {
        try {
            // 创建CPU信息对象
            CpuInfo cpuInfo = new CpuInfo();

            // 解析CPU信息
            String[] lines = cpuInfoStr.split("\n");
            for (String line : lines) {
                if (line.contains("Model name:")) {
                    cpuInfo.setModel(line.split(":")[1].trim());
                } else if (line.contains("CPU(s):")) {
                    try {
                        cpuInfo.setCores(Integer.parseInt(line.split(":")[1].trim()));
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU核心数失败: {}", e.getMessage());
                    }
                } else if (line.contains("CPU MHz:")) {
                    try {
                        cpuInfo.setFrequency(Double.parseDouble(line.split(":")[1].trim()));
                    } catch (NumberFormatException e) {
                        logger.warn("解析CPU频率失败: {}", e.getMessage());
                    }
                }
            }

            // 设置CPU信息
            osInfo.setCpuInfo(cpuInfo);
        } catch (Exception e) {
            logger.error("处理CPU信息时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 收集内存信息
     * 
     * @param hostInfo 主机信息
     */
    @Override
    public void collectMemoryInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectMemoryInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                return;
            }

            // 获取OsInfo
            OsInfo osInfo = hostInfo.getOsInfo();
            if (osInfo == null) {
                osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);
            }

            // 获取内存信息
            // 获取系统类型
            String osType = osInfo.getDistributionId() != null ? osInfo.getDistributionId() : "Linux";
            if ("Linux".equalsIgnoreCase(osType)) {
                // 使用Linux命令收集内存信息
                String memInfo = MinaUtils.execCmdWithResult(session, "free -m");
                if (StringUtils.isNotBlank(memInfo)) {
                    // 解析内存信息
                    processMemoryInfo(osInfo, memInfo);
                    logger.info("主机[{}]内存信息收集成功", hostInfo.getIp());
                }
            } else {
                logger.info("暂不支持收集非Linux系统的内存信息");
            }

            // 更新缓存
            updateHostInfoCache(hostInfo);
        } catch (Exception e) {
            logger.error("收集主机[{}]内存信息时发生异常: {}", hostInfo.getIp(), e.getMessage());
        } finally {
            // 不关闭会话，留给其他方法使用
        }
    }

    /**
     * 处理内存信息
     */
    private void processMemoryInfo(OsInfo osInfo, String memInfoStr) {
        try {
            // 解析内存信息
            String[] lines = memInfoStr.split("\n");
            if (lines.length >= 2) {
                String[] parts = lines[1].trim().split("\\s+");
                if (parts.length >= 3) {
                    try {
                        long totalMemInMB = Long.parseLong(parts[1]); // free -m 输出单位是MB
                        long totalMemInBytes = totalMemInMB * 1024 * 1024; // 转换为字节

                        // 确保memoryInfo对象已初始化
                        if (osInfo.getMemoryInfo() == null) {
                            osInfo.setMemoryInfo(new MemoryInfo());
                        }

                        // 设置内存大小
                        osInfo.getMemoryInfo().setTotalMemory(totalMemInBytes);
                        osInfo.setMemoryStatus(OsInfoStatusEnum.SUCCESS);
                        logger.info("解析内存信息成功: {}MB", totalMemInMB);
                    } catch (NumberFormatException e) {
                        logger.warn("解析内存信息失败: {}", e.getMessage());
                        osInfo.setMemoryStatus(OsInfoStatusEnum.ERROR);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("处理内存信息时发生异常: {}", e.getMessage());
            osInfo.setMemoryStatus(OsInfoStatusEnum.ERROR);
        }
    }

    /**
     * 收集GPU信息
     * 
     * @param hostInfo 主机信息
     */
    @Override
    public void collectGpuInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectGpuInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                return;
            }

            // 获取OsInfo
            OsInfo osInfo = hostInfo.getOsInfo();
            if (osInfo == null) {
                osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);
            }

            // 获取GPU信息
            // 获取系统类型
            String osType = osInfo.getDistributionId() != null ? osInfo.getDistributionId() : "Linux";
            if ("Linux".equalsIgnoreCase(osType)) {
                // 检查是否安装了nvidia-smi
                String gpuCheck = MinaUtils.execCmdWithResult(session, "which nvidia-smi");
                if (StringUtils.isNotBlank(gpuCheck) && !gpuCheck.contains("no nvidia-smi")) {
                    // 获取GPU信息
                    String gpuInfo = MinaUtils.execCmdWithResult(session,
                            "nvidia-smi --query-gpu=name,memory.total,utilization.gpu --format=csv,noheader");
                    if (StringUtils.isNotBlank(gpuInfo)) {
                        // 解析GPU信息
                        processGpuInfo(osInfo, gpuInfo);
                        logger.info("主机[{}]GPU信息收集成功", hostInfo.getIp());
                    }
                } else {
                    logger.info("主机未安装NVIDIA驱动或无GPU");
                }
            } else {
                logger.info("暂不支持收集非Linux系统的GPU信息");
            }

            // 更新缓存
            updateHostInfoCache(hostInfo);
        } catch (Exception e) {
            logger.error("收集主机[{}]GPU信息时发生异常: {}", hostInfo.getIp(), e.getMessage());
        } finally {
            // 不关闭会话，留给其他方法使用
        }
    }

    /**
     * 处理GPU信息
     */
    private void processGpuInfo(OsInfo osInfo, String gpuInfoStr) {
        try {
            // 解析GPU信息
            com.datasophon.common.model.hardware.GpuInfo gpuInfo = new com.datasophon.common.model.hardware.GpuInfo();
            List<com.datasophon.common.model.hardware.GpuInfo.GpuDevice> devices = new ArrayList<>();

            String[] lines = gpuInfoStr.split("\n");
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    com.datasophon.common.model.hardware.GpuInfo.GpuDevice device = new com.datasophon.common.model.hardware.GpuInfo.GpuDevice();
                    device.setName(parts[0].trim());

                    // 解析内存
                    try {
                        String memStr = parts[1].trim();
                        if (memStr.contains("MiB")) {
                            double memoryMB = Double.parseDouble(memStr.replace("MiB", "").trim());
                            device.setTotalMemory(memoryMB);
                            // 同时更新全局显存大小
                            gpuInfo.setTotalMemory(
                                    gpuInfo.getTotalMemory() != null ? gpuInfo.getTotalMemory() + memoryMB : memoryMB);
                        }
                    } catch (Exception e) {
                        logger.warn("解析GPU内存失败: {}", e.getMessage());
                    }

                    // 解析使用率
                    try {
                        String utilStr = parts[2].trim();
                        if (utilStr.contains("%")) {
                            double utilization = Double.parseDouble(utilStr.replace("%", "").trim());
                            device.setUsagePercent(utilization);
                            // 同时更新全局使用率
                            gpuInfo.setUtilization(utilization); // 如果有多个GPU，取最后一个值
                        }
                    } catch (Exception e) {
                        logger.warn("解析GPU使用率失败: {}", e.getMessage());
                    }

                    devices.add(device);
                }
            }

            if (!devices.isEmpty()) {
                gpuInfo.setDeviceCount(devices.size());
                gpuInfo.setDevices(devices);
                // 设置GPU型号为第一个设备名称
                gpuInfo.setModel(devices.get(0).getName());
                gpuInfo.setInfo("NVIDIA " + devices.get(0).getName());
                gpuInfo.setVendor("NVIDIA");
            }

            // 设置GPU信息
            osInfo.setGpuInfo(gpuInfo);
        } catch (Exception e) {
            logger.error("处理GPU信息时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 第一阶段信息收集（主机名和操作系统基本信息）
     * 供前端主列表显示使用
     * 
     * @param hostInfo 主机信息
     */
    @Override
    public void collectPhaseOneInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectPhaseOneInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                return;
            }

            logger.info("【第一阶段】开始收集主机 {} 的基本信息", hostInfo.getIp());

            // 1. 收集主机名
            collectHostnameInfo(hostInfo);

            // 2. 收集操作系统基本信息
            collectOsBasicInfo(hostInfo);

            // 更新缓存
            updateHostInfoCache(hostInfo);

            logger.info("【第一阶段完成】主机 {} 的基本信息收集完成", hostInfo.getIp());
        } catch (Exception e) {
            logger.error("【第一阶段异常】收集主机[{}]基本信息时发生异常: {}", hostInfo.getIp(), e.getMessage());
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
            updateHostInfoCache(hostInfo);
        } finally {
            // 关闭第一阶段的会话，第二阶段将重新创建
            if (session != null && session.isOpen()) {
                try {
                    session.close();
                    logger.info("【第一阶段结束】已关闭主机 {} 的SSH会话，第二阶段将重新建立连接", hostInfo.getIp());
                } catch (Exception e) {
                    logger.warn("关闭SSH会话时出错: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 第二阶段信息收集（详细硬件和系统配置）
     * 供前端悬浮卡片显示使用
     * 
     * @param hostInfo 主机信息
     */
    @Override
    public void collectPhaseTwoInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectPhaseTwoInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新的SSH会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                return;
            }

            logger.info("【第二阶段】开始收集主机 {} 的详细信息", hostInfo.getIp());

            // 获取OsInfo
            OsInfo osInfo = hostInfo.getOsInfo();
            if (osInfo == null) {
                osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);
            }

            // 3. 收集DNS配置信息
            collectDnsInfo(hostInfo);

            // 4. 收集hosts文件信息
            collectHostsFileInfo(hostInfo);

            // 5. 收集CPU信息
            collectCpuInfo(hostInfo);

            // 6. 收集内存信息
            collectMemoryInfo(hostInfo);

            // 7. 收集磁盘信息
            collectDiskInfo(hostInfo);

            // 8. 收集网络信息
            collectNetworkInfo(hostInfo);

            // 9. 收集GPU信息
            collectGpuInfo(hostInfo);

            // 更新缓存
            updateHostInfoCache(hostInfo);

            logger.info("【第二阶段完成】主机 {} 的详细信息收集完成", hostInfo.getIp());
        } catch (Exception e) {
            logger.error("【第二阶段异常】收集主机[{}]详细信息时发生异常: {}", hostInfo.getIp(), e.getMessage());
        } finally {
            // 关闭会话
            if (session != null && session.isOpen()) {
                try {
                    session.close();
                    logger.info("【第二阶段结束】已关闭主机 {} 的SSH会话", hostInfo.getIp());
                } catch (Exception e) {
                    logger.warn("关闭SSH会话时出错: {}", e.getMessage());
                }
            }
        }
    }

    // 添加网络收集接口实现
    @Override
    public void collectNetworkInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectNetworkInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                return;
            }

            // 获取OsInfo
            OsInfo osInfo = hostInfo.getOsInfo();
            if (osInfo == null) {
                osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);
            }

            // 设置网络状态为收集中
            osInfo.setNetworkStatus(OsInfoStatusEnum.LOADING);

            // 获取系统类型
            String osType = osInfo.getDistributionId() != null ? osInfo.getDistributionId() : "Linux";
            if ("Linux".equalsIgnoreCase(osType)) {
                // 使用现有代码中的网络收集方法
                IOsInfoCollector collector = osInfoCollectorFactory.getCollector(osType);
                if (collector != null) {
                    queueManager.collectNetworkInfoNew(hostInfo, osInfo, session, collector);
                    logger.info("主机[{}]网络信息收集成功", hostInfo.getIp());
                }
            } else {
                logger.info("暂不支持收集非Linux系统的网络信息");
            }

            // 更新缓存
            updateHostInfoCache(hostInfo);
        } catch (Exception e) {
            logger.error("收集主机[{}]网络信息时发生异常: {}", hostInfo.getIp(), e.getMessage());
            // 设置错误状态
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setNetworkStatus(OsInfoStatusEnum.ERROR);
            }
        } finally {
            // 不关闭会话，留给其他方法使用
        }
    }

    // 添加磁盘收集接口实现
    @Override
    public void collectDiskInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectDiskInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                return;
            }

            // 获取OsInfo
            OsInfo osInfo = hostInfo.getOsInfo();
            if (osInfo == null) {
                osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);
            }

            // 设置磁盘状态为收集中
            osInfo.setDiskStatus(OsInfoStatusEnum.LOADING);

            // 获取系统类型
            String osType = osInfo.getDistributionId() != null ? osInfo.getDistributionId() : "Linux";
            if ("Linux".equalsIgnoreCase(osType)) {
                // 使用Linux命令收集磁盘信息
                String diskInfo = MinaUtils.execCmdWithResult(session, "df -h");
                if (StringUtils.isNotBlank(diskInfo)) {
                    // 解析磁盘信息
                    processDiskInfo(osInfo, diskInfo);
                    logger.info("主机[{}]磁盘信息收集成功", hostInfo.getIp());
                    osInfo.setDiskStatus(OsInfoStatusEnum.SUCCESS);
                } else {
                    logger.warn("主机[{}]磁盘信息收集失败", hostInfo.getIp());
                    osInfo.setDiskStatus(OsInfoStatusEnum.ERROR);
                }
            } else {
                logger.info("暂不支持收集非Linux系统的磁盘信息");
            }

            // 更新缓存
            updateHostInfoCache(hostInfo);
        } catch (Exception e) {
            logger.error("收集主机[{}]磁盘信息时发生异常: {}", hostInfo.getIp(), e.getMessage());
            // a设置错误状态
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setDiskStatus(OsInfoStatusEnum.ERROR);
            }
        } finally {
            // 不关闭会话，留给其他方法使用
        }
    }

    // 添加处理磁盘信息的方法
    /**
     * 处理磁盘信息
     */
    private void processDiskInfo(OsInfo osInfo, String diskInfoStr) {
        try {
            // 从df -h命令输出解析磁盘信息
            com.datasophon.common.model.hardware.DiskInfo diskInfo = new com.datasophon.common.model.hardware.DiskInfo();
            List<com.datasophon.common.model.hardware.DiskInfo.DiskPartition> partitions = new ArrayList<>();

            String[] lines = diskInfoStr.split("\n");
            for (int i = 1; i < lines.length; i++) { // 跳过标题行
                String line = lines[i].trim();
                String[] parts = line.split("\\s+");
                if (parts.length >= 6) {
                    com.datasophon.common.model.hardware.DiskInfo.DiskPartition partition = new com.datasophon.common.model.hardware.DiskInfo.DiskPartition();
                    partition.setName(parts[0]); // 文件系统
                    partition.setMountPoint(parts[5]); // 挂载点

                    // 解析容量
                    try {
                        // 总空间
                        double totalSpace = parseHumanReadableSize(parts[1]);
                        partition.setTotalSpace(totalSpace);

                        // 已用空间
                        double usedSpace = parseHumanReadableSize(parts[2]);
                        partition.setUsedSpace(usedSpace);

                        // 可用空间
                        double availableSpace = parseHumanReadableSize(parts[3]);
                        partition.setAvailableSpace(availableSpace);

                        // 使用率
                        String usageStr = parts[4].replace("%", "");
                        partition.setUsagePercent(Double.parseDouble(usageStr));
                    } catch (Exception e) {
                        logger.warn("解析磁盘分区信息失败: {}", e.getMessage());
                    }

                    partitions.add(partition);
                }
            }

            // 计算总体磁盘信息
            double totalSpace = 0;
            double usedSpace = 0;
            double availableSpace = 0;

            for (com.datasophon.common.model.hardware.DiskInfo.DiskPartition partition : partitions) {
                if (partition.getTotalSpace() != null) {
                    totalSpace += partition.getTotalSpace();
                }
                if (partition.getUsedSpace() != null) {
                    usedSpace += partition.getUsedSpace();
                }
                if (partition.getAvailableSpace() != null) {
                    availableSpace += partition.getAvailableSpace();
                }
            }

            diskInfo.setTotalDiskSpace(totalSpace);
            diskInfo.setUsedDiskSpace(usedSpace);
            diskInfo.setAvailableDiskSpace(availableSpace);
            diskInfo.setUsagePercent(totalSpace > 0 ? (usedSpace / totalSpace) * 100 : 0);
            diskInfo.setPartitions(partitions);

            // 设置磁盘信息
            osInfo.setDiskInfo(diskInfo);
        } catch (Exception e) {
            logger.error("处理磁盘信息时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 解析人类可读的容量字符串（如2G, 500M等），转换为GB
     */
    private double parseHumanReadableSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return 0;
        }

        double size;

        try {
            if (sizeStr.endsWith("K") || sizeStr.endsWith("k")) {
                size = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) / 1024.0 / 1024.0; // KB to GB
            } else if (sizeStr.endsWith("M") || sizeStr.endsWith("m")) {
                size = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) / 1024.0; // MB to GB
            } else if (sizeStr.endsWith("G") || sizeStr.endsWith("g")) {
                size = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)); // GB
            } else if (sizeStr.endsWith("T") || sizeStr.endsWith("t")) {
                size = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) * 1024.0; // TB to GB
            } else {
                size = Double.parseDouble(sizeStr) / 1024.0 / 1024.0 / 1024.0; // Bytes to GB
            }

            return size;
        } catch (NumberFormatException e) {
            logger.warn("解析大小字符串失败: {}", sizeStr);
            return 0;
        }
    }
}