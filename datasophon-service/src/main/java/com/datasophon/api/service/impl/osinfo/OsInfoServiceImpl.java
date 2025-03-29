package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.service.OsInfoService;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
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

            // 如果操作系统类型收集失败，跳过后续步骤
            if (hostInfo.getOsInfoStatus() != OsInfoStatusEnum.SUCCESS) {
                logger.warn("主机 {} 操作系统类型收集失败，跳过后续信息收集", hostInfo.getIp());
                hostInfo.setMessage("操作系统类型收集失败，无法继续");
                service.updateHostInfoCache(hostInfo);
                return;
            }

            // 获取会话和操作系统对象
            ClientSession session = service.getOrCreateSession(hostInfo);
            OsInfo osInfo = hostInfo.getOsInfo();
            // 使用缓存的osType字段
            String osType = osInfo != null ? osInfo.getDistributionId() : "linux"; // 默认为linux
            IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector(osType);

            if (session == null || osInfo == null || collector == null) {
                logger.error("主机 {} 缺少必要信息，无法继续收集", hostInfo.getIp());
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("缺少必要信息，无法继续");
                service.updateHostInfoCache(hostInfo);
                return;
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

                // 执行获取主机名命令
                String hostname = executeCommand(session, "hostname").trim();
                if (hostname.isEmpty()) {
                    throw new Exception("获取主机名失败");
                }

                logger.info("主机名获取成功: {}, 开始获取FQDN", hostname);

                // 获取FQDN
                String fqdn = executeCommand(session, "hostname -f").trim();
                if (fqdn.isEmpty()) {
                    fqdn = hostname; // 如果获取FQDN失败，使用主机名作为FQDN
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
                String osType = service.detectOperatingSystemType(session);
                logger.info("主机 {} 的操作系统类型为: {}", hostInfo.getIp(), osType);

                // 创建操作系统信息对象
                OsInfo osInfo = new OsInfo();
                osInfo.setHostname(hostInfo.getHostname());
                osInfo.setFqdn(hostInfo.getFqdn());

                // 使用对应的收集器收集操作系统信息
                IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector(osType);
                if (collector == null) {
                    throw new Exception("不支持的操作系统类型: " + osType);
                }

                // 收集操作系统信息 - 使用正确的方法签名
                OsInfo collectedOsInfo = collector.collectOsInfo(hostInfo, session, osInfo,
                        service::updateHostInfoCache);
                if (collectedOsInfo != null) {
                    osInfo = collectedOsInfo; // 使用收集到的信息
                }

                // 设置操作系统类型 - 直接设置到OsInfo上
                osInfo.setDistributionId(osType);
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

                logger.info("主机 {} 操作系统信息收集成功：{} {}，已设置状态：osInfoStatus=success",
                        hostInfo.getIp(), osInfo.getDistribution(), osInfo.getVersionId());
                logger.info("主机 {} 操作系统类型收集总用时: {}ms",
                        hostInfo.getIp(), System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集操作系统类型时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("操作系统类型收集失败: " + e.getMessage());
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
                if (osInfo.getDistributionId() != null &&
                        osInfo.getDistributionId().toLowerCase().contains("windows")) {
                    // Windows系统收集DNS
                    dnsServers = MinaUtils.execCmdWithResult(session,
                            "powershell -command \"Get-DnsClientServerAddress | Select-Object -ExpandProperty ServerAddresses | ForEach-Object { $_ }\"");
                } else {
                    // Linux系统收集DNS
                    dnsServers = MinaUtils.execCmdWithResult(session,
                            "cat /etc/resolv.conf | grep nameserver | awk '{print $2}'");
                }

                // 更新操作系统信息
                osInfo.setDnsServers(dnsServers);
                osInfo.setLastUpdatedItem("dns_collected");
                hostInfo.setMessage("DNS信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 {} DNS信息收集成功：{}，耗时 {}ms",
                        hostInfo.getIp(), dnsServers, System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集DNS信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setMessage("DNS信息收集失败: " + e.getMessage() + "，继续收集其他信息");
                service.updateHostInfoCache(hostInfo);
            }
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
                if (osInfo.getDistributionId() != null &&
                        osInfo.getDistributionId().toLowerCase().contains("windows")) {
                    // Windows系统收集hosts
                    hostsFile = MinaUtils.execCmdWithResult(session,
                            "powershell -command \"Get-Content C:\\Windows\\System32\\drivers\\etc\\hosts\"");
                } else {
                    // Linux系统收集hosts
                    hostsFile = MinaUtils.execCmdWithResult(session, "cat /etc/hosts");
                }

                // 更新操作系统信息
                // 注意：Hosts文件内容较大，可能需要存储在单独的字段或数据库中
                hostInfo.setMessage("Hosts文件收集成功");
                osInfo.setLastUpdatedItem("hosts_collected");
                hostInfo.setHostsFile(hostsFile); // 假设HostInfo有hostFile字段
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
                        osInfo.getDistributionId().toLowerCase().contains("windows")) {
                    // Windows系统收集CPU信息
                    String cpuInfoCmd = "wmic cpu get Name, NumberOfCores, NumberOfLogicalProcessors /Value";
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
                        hostInfo.getIp(), osInfo.getCpuModel(), osInfo.getCpuCores(),
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
            osInfo.setCpuModel(model);
            osInfo.setCpuCores(cores);
            osInfo.setCpuCount(cpuCount);
            osInfo.setCpuCoresPerProcessor(cores);
            osInfo.setCpuLogicalCores(cores * cpuCount);
        }

        /**
         * 解析Windows CPU信息
         */
        private void parseWindowsCpuInfo(OsInfo osInfo, String cpuInfo) {
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
            osInfo.setCpuModel(model);
            osInfo.setCpuCores(cores);
            osInfo.setCpuLogicalCores(logicalProcessors);
            osInfo.setCpuCount(cores > 0 ? logicalProcessors / cores : 1);
            osInfo.setCpuCoresPerProcessor(cores);
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
                        osInfo.getDistributionId().toLowerCase().contains("windows")) {
                    // Windows系统收集内存信息
                    String memInfoCmd = "wmic OS get TotalVisibleMemorySize, FreePhysicalMemory /Value";
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
                        hostInfo.getIp(), osInfo.getTotalMemory(), osInfo.getAvailableMemory(),
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
            long totalMem = 0;
            long availableMem = 0;

            String[] lines = memInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("MemTotal:")) {
                    // MemTotal以KB为单位
                    totalMem = Long.parseLong(line.replaceAll("[^0-9]", "")) * 1024;
                } else if (line.startsWith("MemAvailable:") || line.startsWith("MemFree:")) {
                    // 首选MemAvailable，其次使用MemFree，也是KB为单位
                    availableMem = Long.parseLong(line.replaceAll("[^0-9]", "")) * 1024;
                }
            }

            // 设置内存信息
            osInfo.setTotalMem(totalMem);
            osInfo.setAvailableMem(availableMem);
        }

        /**
         * 解析Windows内存信息
         */
        private void parseWindowsMemoryInfo(OsInfo osInfo, String memInfo) {
            long totalMem = 0;
            long availableMem = 0;

            String[] lines = memInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("TotalVisibleMemorySize=")) {
                    // Windows中为KB单位
                    totalMem = Long.parseLong(line.substring(23).trim()) * 1024;
                } else if (line.startsWith("FreePhysicalMemory=")) {
                    availableMem = Long.parseLong(line.substring(19).trim()) * 1024;
                }
            }

            // 设置内存信息
            osInfo.setTotalMem(totalMem);
            osInfo.setAvailableMem(availableMem);
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
                        osInfo.getDistributionId().toLowerCase().contains("windows")) {
                    // Windows系统收集磁盘信息
                    String diskInfoCmd = "wmic logicaldisk where DeviceID='C:' get Size,FreeSpace /Value";
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
                        hostInfo.getIp(), osInfo.getTotalDisk(), osInfo.getAvailableDisk(),
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
            String[] lines = diskInfo.split("\n");
            if (lines.length >= 2) {
                String[] parts = lines[1].trim().split("\\s+");
                if (parts.length >= 4) {
                    // df -k输出的单位是KB，需要乘以1024转换为字节
                    long totalBlocks = Long.parseLong(parts[1].trim()) * 1024;
                    long availBlocks = Long.parseLong(parts[3].trim()) * 1024;

                    // 设置磁盘信息
                    osInfo.setTotalDisk(totalBlocks);
                    osInfo.setAvailableDisk(availBlocks);
                }
            }
        }

        /**
         * 解析Windows磁盘信息
         */
        private void parseWindowsDiskInfo(OsInfo osInfo, String diskInfo) {
            long totalSize = 0;
            long freeSpace = 0;

            String[] lines = diskInfo.split("[\r\n]+");
            for (String line : lines) {
                if (line.startsWith("Size=")) {
                    totalSize = Long.parseLong(line.substring(5).trim());
                } else if (line.startsWith("FreeSpace=")) {
                    freeSpace = Long.parseLong(line.substring(10).trim());
                }
            }

            // 设置磁盘信息
            osInfo.setTotalDisk(totalSize);
            osInfo.setAvailableDisk(freeSpace);
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
                        !osInfo.getDistributionId().toLowerCase().contains("windows")) {
                    // 收集交换空间信息
                    String swapInfoCmd = "grep Swap /proc/meminfo";
                    String swapInfo = MinaUtils.execCmdWithResult(session, swapInfoCmd);

                    if (StringUtils.isNotBlank(swapInfo)) {
                        // 解析Linux交换空间信息
                        parseLinuxSwapInfo(osInfo, swapInfo);
                    }
                } else {
                    logger.info("Windows系统跳过交换空间收集: {}", hostInfo.getIp());
                }

                // 更新状态
                hostInfo.setSwapStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("交换空间信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 {} 交换空间信息收集成功：总交换空间 {}GB，可用交换空间 {}GB，耗时 {}ms",
                        hostInfo.getIp(), osInfo.getTotalSwap(), osInfo.getAvailableSwap(),
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
            long swapTotal = 0;
            long swapFree = 0;

            String[] lines = swapInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("SwapTotal:")) {
                    // SwapTotal以KB为单位
                    swapTotal = Long.parseLong(line.replaceAll("[^0-9]", "")) * 1024;
                } else if (line.startsWith("SwapFree:")) {
                    // SwapFree以KB为单位
                    swapFree = Long.parseLong(line.replaceAll("[^0-9]", "")) * 1024;
                }
            }

            // 设置交换空间信息
            osInfo.setTotalSwap(swapTotal);
            osInfo.setAvailableSwap(swapFree);
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
                // 收集GPU信息
                if (osInfo.getDistributionId() != null &&
                        osInfo.getDistributionId().toLowerCase().contains("windows")) {
                    // Windows系统收集GPU信息
                    String gpuInfoCmd = "wmic path win32_VideoController get Name, AdapterRAM /Value";
                    String gpuInfo = MinaUtils.execCmdWithResult(session, gpuInfoCmd);

                    if (StringUtils.isNotBlank(gpuInfo)) {
                        // 解析Windows GPU信息
                        parseWindowsGpuInfo(osInfo, gpuInfo);
                    }
                } else {
                    // Linux系统收集GPU信息
                    String gpuInfoCmd = "lspci | grep -i 'vga\\|3d\\|2d'";
                    String gpuInfo = MinaUtils.execCmdWithResult(session, gpuInfoCmd);

                    if (StringUtils.isNotBlank(gpuInfo)) {
                        // 解析Linux GPU信息
                        parseLinuxGpuInfo(osInfo, gpuInfo);
                    }
                }

                // 更新状态
                hostInfo.setGpuStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("GPU信息收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 {} GPU信息收集成功，耗时 {}ms",
                        hostInfo.getIp(), System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("收集GPU信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                osInfo.setLastUpdatedItem("GPU收集失败");
                hostInfo.setGpuStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("GPU信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 解析Linux GPU信息
         */
        private void parseLinuxGpuInfo(OsInfo osInfo, String gpuInfo) {
            // 简单设置GPU信息
            osInfo.setGpuInfo(gpuInfo.trim());
        }

        /**
         * 解析Windows GPU信息
         */
        private void parseWindowsGpuInfo(OsInfo osInfo, String gpuInfo) {
            String gpuName = "";
            long gpuMemory = 0;

            String[] lines = gpuInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("Name=")) {
                    gpuName = line.substring(5).trim();
                } else if (line.startsWith("AdapterRAM=")) {
                    try {
                        gpuMemory = Long.parseLong(line.substring(11).trim());
                    } catch (NumberFormatException e) {
                        // 忽略解析错误
                    }
                }
            }

            // 设置GPU信息
            osInfo.setGpuInfo(gpuName);
            // 转换为GB
            osInfo.setGpuMemory(gpuMemory / (1024.0 * 1024.0 * 1024.0));
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