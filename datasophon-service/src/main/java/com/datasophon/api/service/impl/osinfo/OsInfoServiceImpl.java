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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * 操作系统信息服务实现类
 * 负责管理主机操作系统信息的获取和缓存
 */
@Service
public class OsInfoServiceImpl implements OsInfoService {

    private static final Logger logger = LoggerFactory.getLogger(OsInfoServiceImpl.class);

    @Autowired
    private OsInfoCollectorFactory osInfoCollectorFactory;

    // 使用Spring的ThreadPoolTaskExecutor替代原来的ExecutorService
    @Autowired
    private ThreadPoolTaskExecutor hostnameExecutor;

    @Autowired
    private ThreadPoolTaskExecutor osInfoExecutor;

    @Autowired
    private ThreadPoolTaskExecutor dnsExecutor;

    @Autowired
    private ThreadPoolTaskExecutor hostsFileExecutor;

    @Autowired
    private ThreadPoolTaskExecutor cpuInfoExecutor;

    @Autowired
    private ThreadPoolTaskExecutor memoryInfoExecutor;

    @Autowired
    private ThreadPoolTaskExecutor diskInfoExecutor;

    @Autowired
    private ThreadPoolTaskExecutor swapInfoExecutor;

    @Autowired
    private ThreadPoolTaskExecutor gpuInfoExecutor;

    @Autowired
    private ThreadPoolTaskExecutor hardwareInfoExecutor;

    // 添加主机信息收集队列管理器
    private final HostInfoCollectionQueueManager queueManager = new HostInfoCollectionQueueManager(this);

    @PostConstruct
    public void init() {
        logger.info("=====================================================");
        logger.info("初始化OS信息收集服务，线程池由Spring管理");
        logger.info("信息收集线程池配置如下（按优先级排序）：");
        logger.info("1. hostnameExecutor: 主机名收集（最高优先级）");
        logger.info("2. osInfoExecutor: 操作系统信息收集（次高优先级）");
        logger.info("3. dnsExecutor: DNS服务器信息收集（高优先级）");
        logger.info("4. hostsFileExecutor: hosts文件收集（中高优先级）");
        logger.info("5. cpuInfoExecutor: CPU信息收集（中优先级）");
        logger.info("6. memoryInfoExecutor: 内存信息收集（中优先级）");
        logger.info("7. diskInfoExecutor: 磁盘信息收集（中优先级）");
        logger.info("8. swapInfoExecutor: 交换空间信息收集（中优先级）");
        logger.info("9. gpuInfoExecutor: GPU信息收集（中优先级）");
        logger.info("");
        logger.info("主机信息收集流程：");
        logger.info("1. 按IP地址排序所有主机");
        logger.info("2. 收集每个主机的主机名");
        logger.info("3. 收集每个主机的操作系统信息");
        logger.info("4. 收集每个主机的DNS服务器信息");
        logger.info("5. 收集每个主机的hosts文件信息");
        logger.info("6. 收集每个主机的CPU信息");
        logger.info("7. 收集每个主机的内存信息");
        logger.info("8. 收集每个主机的磁盘信息");
        logger.info("9. 收集每个主机的交换空间信息");
        logger.info("10. 收集每个主机的GPU信息");
        logger.info("每收集一步都会立即更新缓存，让前端能够及时显示信息");
        logger.info("=====================================================");
    }

    @PreDestroy
    public void destroy() {
        logger.info("OsInfoServiceImpl正在关闭...");
    }

    @Override
    public void getHostOsInfoAsync(HostInfo hostInfo) {
        // 记录主机信息收集开始时间
        logger.info("主机信息收集流程：");
        logger.info("IP: {}, 开始收集信息", hostInfo.getIp());

        // 初始化状态为LOADING，使用枚举
        // 对所有状态设置为LOADING
        hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
        hostInfo.setHostnameStatus(OsInfoStatusEnum.LOADING);
        hostInfo.setOsStatus(OsInfoStatusEnum.LOADING);
        hostInfo.setDnsStatus(OsInfoStatusEnum.LOADING);
        hostInfo.setHostsFileStatus(OsInfoStatusEnum.LOADING);
        hostInfo.setCpuStatus(OsInfoStatusEnum.LOADING);
        hostInfo.setMemoryStatus(OsInfoStatusEnum.LOADING);
        hostInfo.setDiskStatus(OsInfoStatusEnum.LOADING);
        hostInfo.setSwapStatus(OsInfoStatusEnum.LOADING);
        hostInfo.setGpuStatus(OsInfoStatusEnum.LOADING);

        // 立即更新缓存，让前端看到加载状态
        updateHostInfoCache(hostInfo);

        // 将主机信息收集任务添加到队列，按IP排序串行执行
        queueManager.addHostToQueue(hostInfo, null);
    }

    /**
     * 处理主机信息收集流程
     * 该方法由队列管理器调用，按照指定的顺序获取主机信息
     * 
     * @deprecated 不再直接使用该方法，改为使用队列管理器的分阶段收集
     */
    @Deprecated
    private void processHostInfoCollection(HostInfo hostInfo) {
        // 此方法已被弃用，不再直接使用
        // 改为使用队列管理器的分阶段串行收集机制
        logger.info("不再使用此方法收集主机信息");

        // 将主机添加到队列管理器
        queueManager.addHostToQueue(hostInfo, null);
    }

    /**
     * 主机信息收集队列管理器
     * 管理多个阶段的收集队列，确保所有主机按阶段同步收集信息
     */
    private static class HostInfoCollectionQueueManager {
        // 持有外部类引用
        private final OsInfoServiceImpl service;

        // 用于收集基本系统信息的队列
        private final Queue<HostInfo> osInfoQueue = new ConcurrentLinkedQueue<>();

        // 用于按阶段收集硬件信息的队列
        private final Queue<HardwareInfoTask> cpuInfoQueue = new ConcurrentLinkedQueue<>();
        private final Queue<HardwareInfoTask> memoryInfoQueue = new ConcurrentLinkedQueue<>();
        private final Queue<HardwareInfoTask> diskInfoQueue = new ConcurrentLinkedQueue<>();
        private final Queue<HardwareInfoTask> gpuInfoQueue = new ConcurrentLinkedQueue<>();

        // 用于存储已排序的主机任务
        private final List<HostInfo> sortedHostList = new ArrayList<>();
        private final List<HardwareInfoTask> sortedHardwareTasks = new ArrayList<>();

        // 跟踪各阶段完成状态的计数器
        private int hostNameCollectionCompleted = 0;
        private int osTypeCollectionCompleted = 0;
        private int dnsCollectionCompleted = 0; // DNS收集计数器
        private int hostsFileCollectionCompleted = 0; // 主机文件收集计数器
        private int cpuInfoCollectionCompleted = 0;
        private int memoryInfoCollectionCompleted = 0;
        private int diskInfoCollectionCompleted = 0;
        private int swapInfoCollectionCompleted = 0; // 新增交换空间收集计数器
        private int gpuInfoCollectionCompleted = 0;

        // 标记当前收集阶段
        private enum CollectionStage {
            HOST_NAME, // 收集主机名
            OS_TYPE, // 收集操作系统类型
            DNS, // 收集DNS服务器信息
            HOSTS, // 收集hosts文件
            CPU_INFO, // 收集CPU信息
            MEMORY_INFO, // 收集内存信息
            DISK_INFO, // 收集磁盘信息
            SWAP_INFO, // 收集交换空间信息
            GPU_INFO, // 收集GPU信息
            COMPLETED // 所有收集完成
        }

        private volatile CollectionStage currentStage = CollectionStage.HOST_NAME;

        // 标记各队列处理状态
        private volatile boolean processingOsInfo = false;
        private volatile boolean processingDns = false; // DNS处理状态
        private volatile boolean processingHosts = false; // hosts文件处理状态
        private volatile boolean processingCpuInfo = false;
        private volatile boolean processingMemoryInfo = false;
        private volatile boolean processingDiskInfo = false;
        private volatile boolean processingSwapInfo = false; // 交换空间处理状态
        private volatile boolean processingGpuInfo = false;

        // 当前正在处理的主机索引
        private int currentHostIndex = 0;

        /**
         * 构造函数
         * 
         * @param service 外部服务实例
         */
        public HostInfoCollectionQueueManager(OsInfoServiceImpl service) {
            this.service = service;
        }

        /**
         * 如果处理器未运行，则启动相应的处理器
         */
        private synchronized void startProcessingIfNeeded() {
            logger.info("当前收集阶段: {}, 待处理主机数: {}", currentStage, sortedHostList.size());

            // 按照阶段顺序执行处理
            switch (currentStage) {
                case HOST_NAME:
                    if (!processingOsInfo && !sortedHostList.isEmpty()) {
                        processHostNameCollection();
                    }
                    break;
                case OS_TYPE:
                    if (!processingOsInfo && hostNameCollectionCompleted == sortedHostList.size()) {
                        // 重置当前主机索引，准备下一阶段收集
                        currentHostIndex = 0;
                        processOsTypeCollection();
                    }
                    break;
                case DNS: // DNS服务器收集阶段
                    if (!processingDns && osTypeCollectionCompleted == sortedHostList.size()) {
                        // 重置当前主机索引，准备下一阶段收集
                        currentHostIndex = 0;
                        processDnsCollection();
                    }
                    break;
                case HOSTS: // hosts文件收集阶段
                    if (!processingHosts && dnsCollectionCompleted == sortedHostList.size()) {
                        // 重置当前主机索引，准备下一阶段收集
                        currentHostIndex = 0;
                        processHostsFileCollection();
                    }
                    break;
                case CPU_INFO:
                    if (!processingCpuInfo && hostsFileCollectionCompleted == sortedHostList.size()) {
                        // 重置当前主机索引，准备下一阶段收集
                        currentHostIndex = 0;
                        processCpuInfoQueue();
                    }
                    break;
                case MEMORY_INFO:
                    if (!processingMemoryInfo && cpuInfoCollectionCompleted == sortedHardwareTasks.size()) {
                        // 重置当前主机索引，准备下一阶段收集
                        currentHostIndex = 0;
                        processMemoryInfoQueue();
                    }
                    break;
                case DISK_INFO:
                    if (!processingDiskInfo && memoryInfoCollectionCompleted == sortedHardwareTasks.size()) {
                        // 重置当前主机索引，准备下一阶段收集
                        currentHostIndex = 0;
                        processDiskInfoQueue();
                    }
                    break;
                case SWAP_INFO: // 交换空间收集阶段
                    if (!processingSwapInfo && diskInfoCollectionCompleted == sortedHardwareTasks.size()) {
                        // 重置当前主机索引，准备下一阶段收集
                        currentHostIndex = 0;
                        processSwapInfoQueue();
                    }
                    break;
                case GPU_INFO:
                    if (!processingGpuInfo && swapInfoCollectionCompleted == sortedHardwareTasks.size()) {
                        // 重置当前主机索引，准备下一阶段收集
                        currentHostIndex = 0;
                        processGpuInfoQueue();
                    }
                    break;
                default:
                    break;
            }
        }

        /**
         * 添加主机到系统信息收集队列，按IP地址排序
         */
        public synchronized void addHostToQueue(HostInfo hostInfo, Consumer<HostInfo> processor) {
            // 添加到排序列表
            sortedHostList.add(hostInfo);

            // 排序（按IP地址排序）
            sortedHostList.sort(Comparator.comparing(HostInfo::getIp));

            // 如果当前没有处理任务，且处于主机名收集阶段，则开始处理
            if (!processingOsInfo && currentStage == CollectionStage.HOST_NAME) {
                processHostNameCollection();
            }
        }

        /**
         * 处理主机名收集
         * 修改为串行处理，一次只处理一台主机
         */
        private void processHostNameCollection() {
            processingOsInfo = true;

            // 使用CompletableFuture处理当前主机
            CompletableFuture.runAsync(() -> {
                try {
                    // 确保索引在有效范围内
                    if (currentHostIndex < sortedHostList.size()) {
                        HostInfo hostInfo = sortedHostList.get(currentHostIndex);
                        logger.info("开始收集主机名 [{}/{}]: {}",
                                currentHostIndex + 1, sortedHostList.size(), hostInfo.getIp());

                        // 设置主机名收集状态为LOADING
                        hostInfo.setMessage("正在收集主机名...");
                        hostInfo.setHostnameStatus(OsInfoStatusEnum.LOADING);
                        service.updateHostInfoCache(hostInfo);
                        logger.info("主机 {} 状态更新：hostnameStatus={}, 开始收集主机名",
                                hostInfo.getIp(), hostInfo.getHostnameStatus());

                        try {
                            // 执行主机名收集
                            ClientSession session = service.getOrCreateSession(hostInfo);
                            if (session == null) {
                                // 如果无法创建SSH会话，设置错误状态
                                logger.warn("创建SSH会话失败，无法收集主机名: {}", hostInfo.getIp());
                                hostInfo.setMessage("无法收集主机名: SSH连接失败");
                                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                                hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                                hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                                service.updateHostInfoCache(hostInfo);
                                logger.info("主机 {} 状态更新：hostnameStatus={}, SSH连接失败",
                                        hostInfo.getIp(), hostInfo.getHostnameStatus());
                                // 继续处理下一台主机
                                currentHostIndex++;
                                hostNameCollectionCompleted++;
                                processingOsInfo = false;
                                startProcessingIfNeeded();
                                return;
                            }

                            // 执行主机名收集命令
                            String hostname = executeCommand(session, "hostname");
                            if (StringUtils.isNotBlank(hostname)) {
                                hostInfo.setHostname(hostname.trim());
                                // 尝试获取FQDN（完全限定域名）
                                String fqdn = executeCommand(session, "hostname -f");
                                if (StringUtils.isNotBlank(fqdn)) {
                                    hostInfo.setFqdn(fqdn.trim());
                                }

                                // 设置主机名收集成功状态
                                hostInfo.setMessage("主机名收集完成");
                                hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
                                hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
                                logger.info("主机 {} 主机名收集成功：hostname={}, fqdn={}, hostnameStatus={}",
                                        hostInfo.getIp(), hostInfo.getHostname(), hostInfo.getFqdn(),
                                        hostInfo.getHostnameStatus());
                            } else {
                                // 未能获取主机名
                                logger.warn("未能获取主机{}的主机名", hostInfo.getIp());
                                hostInfo.setMessage("未能获取主机名");
                                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                                hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                                logger.info("主机 {} 状态更新：hostnameStatus={}, 未能获取主机名",
                                        hostInfo.getIp(), hostInfo.getHostnameStatus());
                            }

                            // 无论成功或失败，立即更新缓存
                            service.updateHostInfoCache(hostInfo);

                            // 尝试关闭会话
                            try {
                                session.close();
                            } catch (Exception e) {
                                logger.warn("关闭SSH会话时出错: {}", e.getMessage());
                            }
                        } catch (Exception e) {
                            // 处理异常
                            logger.error("收集主机名时出错: {}", e.getMessage(), e);
                            hostInfo.setMessage("收集主机名失败: " + e.getMessage());
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                            hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                            hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                            service.updateHostInfoCache(hostInfo);
                            logger.info("主机 {} 状态更新：hostnameStatus={}, 收集主机名异常: {}",
                                    hostInfo.getIp(), hostInfo.getHostnameStatus(), e.getMessage());
                        }

                        // 更新计数器和当前索引
                        hostNameCollectionCompleted++;
                        currentHostIndex++;

                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // 检查是否完成所有主机
                        if (hostNameCollectionCompleted == sortedHostList.size()) {
                            // 所有主机名收集完成，进入下一阶段
                            logger.info("所有主机名收集完成，进入操作系统类型收集阶段");
                            currentStage = CollectionStage.OS_TYPE;
                        }
                    }
                } finally {
                    processingOsInfo = false;
                    // 如果还有主机待处理，继续处理
                    startProcessingIfNeeded();
                }
            }, service.hostnameExecutor);
        }

        /**
         * 执行SSH命令并返回结果
         */
        private String executeCommand(ClientSession session, String command) {
            try {
                String result = MinaUtils.execCmdWithResult(session, command);
                return result;
            } catch (Exception e) {
                logger.warn("执行命令 {} 失败: {}", command, e.getMessage());
                return null;
            }
        }

        /**
         * 按顺序处理操作系统类型收集
         * 修改为串行处理，一次只处理一台主机
         */
        private void processOsTypeCollection() {
            processingOsInfo = true;

            // 使用CompletableFuture处理当前主机
            CompletableFuture.runAsync(() -> {
                try {
                    // 确保索引在有效范围内
                    if (currentHostIndex < sortedHostList.size()) {
                        HostInfo hostInfo = sortedHostList.get(currentHostIndex);
                        logger.info("开始收集操作系统类型 [{}/{}]: {}",
                                currentHostIndex + 1, sortedHostList.size(), hostInfo.getIp());

                        // 执行操作系统类型收集
                        processOsType(hostInfo);

                        // 更新计数器和当前索引
                        osTypeCollectionCompleted++;
                        currentHostIndex++;

                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // 检查是否完成所有主机
                        if (osTypeCollectionCompleted == sortedHostList.size()) {
                            // 所有操作系统类型收集完成，进入下一阶段
                            logger.info("所有操作系统类型收集完成，进入DNS服务器收集阶段");
                            currentStage = CollectionStage.DNS;
                        }
                    }
                } finally {
                    processingOsInfo = false;
                    // 如果还有主机待处理，继续处理
                    startProcessingIfNeeded();
                }
            }, service.osInfoExecutor);
        }

        /**
         * 收集单个主机的操作系统类型
         */
        private void processOsType(HostInfo hostInfo) {
            try {
                // 设置操作系统收集状态
                hostInfo.setOsStatus(OsInfoStatusEnum.LOADING);
                hostInfo.setMessage("正在收集操作系统信息...");
                service.updateHostInfoCache(hostInfo);

                // 获取操作系统信息
                OsInfo osInfo = service.getHostOsInfoInternal(hostInfo);

                // 设置数据
                hostInfo.setOsInfo(osInfo);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setOsStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("操作系统信息收集完成");

                // 设置硬件收集状态为collecting
                if (osInfo != null) {
                    osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.COLLECTING);
                }

                // 立即更新缓存，让前端看到结果
                service.updateHostInfoCache(hostInfo);
            } catch (Exception e) {
                logger.error("收集操作系统类型时出错: {}", e.getMessage(), e);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setOsStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("收集操作系统信息失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 按顺序处理DNS服务器收集
         * 修改为串行处理，一次只处理一台主机
         */
        private void processDnsCollection() {
            processingDns = true;

            // 使用CompletableFuture处理当前主机
            CompletableFuture.runAsync(() -> {
                try {
                    // 确保索引在有效范围内
                    if (currentHostIndex < sortedHostList.size()) {
                        HostInfo hostInfo = sortedHostList.get(currentHostIndex);
                        logger.info("开始收集DNS服务器信息 [{}/{}]: {}",
                                currentHostIndex + 1, sortedHostList.size(), hostInfo.getIp());

                        // 执行DNS服务器收集
                        processDnsServers(hostInfo);

                        // 更新计数器和当前索引
                        dnsCollectionCompleted++;
                        currentHostIndex++;

                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // 检查是否完成所有主机
                        if (dnsCollectionCompleted == sortedHostList.size()) {
                            // 所有DNS服务器收集完成，进入下一阶段
                            logger.info("所有DNS服务器收集完成，进入Hosts文件收集阶段");
                            currentStage = CollectionStage.HOSTS;
                        }
                    }
                } finally {
                    processingDns = false;
                    // 如果还有主机待处理，继续处理
                    startProcessingIfNeeded();
                }
            }, service.dnsExecutor); // 使用专用的DNS服务器收集线程池
        }

        /**
         * 收集DNS服务器信息
         */
        private void processDnsServers(HostInfo hostInfo) {
            try {
                // 设置DNS收集状态
                hostInfo.setDnsStatus(OsInfoStatusEnum.LOADING);
                hostInfo.setMessage("正在收集DNS服务器信息...");
                service.updateHostInfoCache(hostInfo);

                ClientSession session = service.getOrCreateSession(hostInfo);
                if (session == null) {
                    logger.warn("创建SSH会话失败，无法收集DNS服务器信息: {}", hostInfo.getIp());
                    // 使用临时字段标记状态
                    hostInfo.setMessage("无法收集DNS服务器信息: SSH连接失败");
                    hostInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
                    service.updateHostInfoCache(hostInfo);
                    return;
                }

                // 收集DNS服务器信息
                collectDnsServers(hostInfo, session);

                // 更新状态
                hostInfo.setMessage("DNS服务器信息收集完成");
                hostInfo.setDnsStatus(OsInfoStatusEnum.SUCCESS);
                service.updateHostInfoCache(hostInfo);

                // 关闭会话
                try {
                    session.close();
                } catch (Exception e) {
                    logger.warn("关闭SSH会话时出错: {}", e.getMessage());
                }
            } catch (Exception e) {
                logger.error("收集DNS服务器信息时出错: {}", e.getMessage(), e);
                hostInfo.setMessage("收集DNS服务器信息失败: " + e.getMessage());
                hostInfo.setDnsStatus(OsInfoStatusEnum.ERROR);
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 收集DNS服务器信息
         */
        private void collectDnsServers(HostInfo hostInfo, ClientSession session) {
            try {
                OsInfo osInfo = hostInfo.getOsInfo();
                if (osInfo == null) {
                    logger.warn("主机 {} 的osInfo为空，无法收集DNS服务器信息", hostInfo.getIp());
                    return;
                }

                String dnsCommand;
                if (osInfo.getDistributionId() != null &&
                        osInfo.getDistributionId().toLowerCase().contains("windows")) {
                    // Windows系统获取DNS命令
                    dnsCommand = "powershell -command \"Get-DnsClientServerAddress | Select-Object -ExpandProperty ServerAddresses | ForEach-Object { $_ }\"";
                } else {
                    // Linux系统获取DNS命令
                    dnsCommand = "cat /etc/resolv.conf | grep nameserver | awk '{print $2}'";
                }

                String dnsServers = MinaUtils.execCmdWithResult(session, dnsCommand);

                if (StringUtils.isNotBlank(dnsServers)) {
                    osInfo.setDnsServers(dnsServers.trim());
                    // 立即更新缓存
                    service.updateHostInfoCache(hostInfo);
                    logger.info("成功收集DNS服务器信息: {}", hostInfo.getIp());
                } else {
                    logger.warn("主机 {} 未返回DNS服务器信息", hostInfo.getIp());
                }
            } catch (Exception e) {
                logger.error("收集DNS服务器信息时出错: {}", e.getMessage(), e);
            }
        }

        /**
         * 按顺序处理Hosts文件收集
         * 修改为串行处理，一次只处理一台主机
         */
        private void processHostsFileCollection() {
            processingHosts = true;

            // 使用CompletableFuture处理当前主机
            CompletableFuture.runAsync(() -> {
                try {
                    // 确保索引在有效范围内
                    if (currentHostIndex < sortedHostList.size()) {
                        HostInfo hostInfo = sortedHostList.get(currentHostIndex);
                        logger.info("开始收集Hosts文件 [{}/{}]: {}",
                                currentHostIndex + 1, sortedHostList.size(), hostInfo.getIp());

                        // 执行Hosts文件收集
                        processHostsFile(hostInfo);

                        // 更新计数器和当前索引
                        hostsFileCollectionCompleted++;
                        currentHostIndex++;

                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // 检查是否完成所有主机
                        if (hostsFileCollectionCompleted == sortedHostList.size()) {
                            // 所有Hosts文件收集完成，进入下一阶段
                            logger.info("所有Hosts文件收集完成，进入CPU信息收集阶段");
                            currentStage = CollectionStage.CPU_INFO;
                            // 准备CPU收集队列
                            prepareCpuInfoQueue();
                        }
                    }
                } finally {
                    processingHosts = false;
                    // 如果还有主机待处理，继续处理
                    startProcessingIfNeeded();
                }
            }, service.hostsFileExecutor); // 使用专用的Hosts文件收集线程池
        }

        /**
         * 收集单个主机的Hosts文件
         */
        private void processHostsFile(HostInfo hostInfo) {
            try {
                // 设置hosts文件收集状态
                hostInfo.setHostsFileStatus(OsInfoStatusEnum.LOADING);
                hostInfo.setMessage("正在收集Hosts文件...");
                service.updateHostInfoCache(hostInfo);

                ClientSession session = service.getOrCreateSession(hostInfo);
                if (session == null) {
                    logger.warn("创建SSH会话失败，无法收集Hosts文件: {}", hostInfo.getIp());
                    // 使用临时字段标记状态
                    hostInfo.setMessage("无法收集Hosts文件: SSH连接失败");
                    hostInfo.setHostsFileStatus(OsInfoStatusEnum.ERROR);
                    service.updateHostInfoCache(hostInfo);
                    return;
                }

                // 收集Hosts文件
                collectHostsFile(hostInfo, session);

                // 更新状态
                hostInfo.setMessage("Hosts文件收集完成");
                hostInfo.setHostsFileStatus(OsInfoStatusEnum.SUCCESS);
                service.updateHostInfoCache(hostInfo);

                // 关闭会话
                try {
                    session.close();
                } catch (Exception e) {
                    logger.warn("关闭SSH会话时出错: {}", e.getMessage());
                }
            } catch (Exception e) {
                logger.error("收集Hosts文件时出错: {}", e.getMessage(), e);
                hostInfo.setMessage("收集Hosts文件失败: " + e.getMessage());
                hostInfo.setHostsFileStatus(OsInfoStatusEnum.ERROR);
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 收集hosts文件
         */
        private void collectHostsFile(HostInfo hostInfo, ClientSession session) {
            try {
                OsInfo osInfo = hostInfo.getOsInfo();
                if (osInfo == null) {
                    logger.warn("主机 {} 的osInfo为空，无法收集hosts文件", hostInfo.getIp());
                    return;
                }

                String hostsCommand;
                if (osInfo.getDistributionId() != null &&
                        osInfo.getDistributionId().toLowerCase().contains("windows")) {
                    // Windows系统获取hosts文件命令
                    hostsCommand = "powershell -command \"Get-Content C:\\Windows\\System32\\drivers\\etc\\hosts\"";
                } else {
                    // Linux系统获取hosts文件命令
                    hostsCommand = "cat /etc/hosts";
                }

                String hostsFile = MinaUtils.execCmdWithResult(session, hostsCommand);

                if (StringUtils.isNotBlank(hostsFile)) {
                    hostInfo.setHostsFile(hostsFile.trim());
                    // 立即更新缓存
                    service.updateHostInfoCache(hostInfo);
                    logger.info("成功收集Hosts文件: {}", hostInfo.getIp());
                } else {
                    logger.warn("主机 {} 未返回Hosts文件内容", hostInfo.getIp());
                }
            } catch (Exception e) {
                logger.error("收集hosts文件时出错: {}", e.getMessage(), e);
            }
        }

        /**
         * 准备CPU信息收集队列
         */
        private void prepareCpuInfoQueue() {
            // 清空并重新准备硬件任务列表
            sortedHardwareTasks.clear();

            for (HostInfo hostInfo : sortedHostList) {
                try {
                    // 只处理状态正常的主机
                    if (OsInfoStatusEnum.SUCCESS.equals(hostInfo.getOsInfoStatus())) {
                        ClientSession session = service.getOrCreateSession(hostInfo);
                        if (session == null) {
                            logger.warn("无法创建SSH会话，跳过硬件信息收集: {}", hostInfo.getIp());
                            continue;
                        }

                        OsInfo osInfo = hostInfo.getOsInfo();
                        if (osInfo == null) {
                            logger.warn("主机 {} 的操作系统信息为空，跳过硬件信息收集", hostInfo.getIp());
                            continue;
                        }

                        // 确定操作系统类型
                        String osType = service.detectOperatingSystemType(session);
                        IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector(osType);
                        if (collector == null) {
                            logger.warn("未找到适用于{}操作系统的信息收集器", osType);
                            continue;
                        }

                        // 确保OsInfo有初始的硬件收集状态
                        if (osInfo.getHardwareCollectionStatus() == null) {
                            osInfo.setHardwareCollectionStatus("pending");
                        }

                        // 创建任务并添加到排序列表
                        HardwareInfoTask task = new HardwareInfoTask(hostInfo, osInfo, session, collector, service);
                        sortedHardwareTasks.add(task);
                    }
                } catch (Exception e) {
                    logger.error("准备硬件信息收集任务时出错: {}, 主机: {}", e.getMessage(), hostInfo.getIp(), e);
                }
            }

            // 对硬件任务进行IP排序
            sortedHardwareTasks.sort(Comparator.comparing(task -> task.hostInfo.getIp()));

            // 开始处理CPU信息收集
            if (!sortedHardwareTasks.isEmpty()) {
                processCpuInfoQueue();
            } else {
                logger.warn("没有可用的硬件信息收集任务");
                currentStage = CollectionStage.COMPLETED;
            }
        }

        /**
         * 处理CPU信息收集队列
         */
        private void processCpuInfoQueue() {
            processingCpuInfo = true;

            // 使用cpuInfoExecutor处理队列中的所有任务
            CompletableFuture.runAsync(() -> {
                try {
                    for (HardwareInfoTask task : sortedHardwareTasks) {
                        if (task == null || task.hostInfo == null) {
                            continue;
                        }

                        logger.info("开始收集CPU信息: {}", task.hostInfo.getIp());

                        // 收集CPU信息
                        try {
                            // 设置正在收集CPU信息
                            task.osInfo.setLastUpdatedItem("collecting_cpu");
                            task.hostInfo.setCpuStatus(OsInfoStatusEnum.LOADING);
                            task.hostInfo.setMessage("正在收集CPU信息...");
                            service.updateHostInfoCache(task.hostInfo);

                            // 从收集器获取CPU信息收集方法并执行
                            collectCpuInfo(task);

                            // 设置成功状态
                            task.hostInfo.setCpuStatus(OsInfoStatusEnum.SUCCESS);
                            task.hostInfo.setMessage("CPU信息收集完成");
                            service.updateHostInfoCache(task.hostInfo);

                            // 设置硬件信息收集成功
                            task.osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.SUCCESS);

                            // 更新计数器
                            cpuInfoCollectionCompleted++;
                        } catch (Exception e) {
                            logger.error("收集CPU信息时出错: {}", e.getMessage(), e);
                            task.osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
                            task.osInfo.setLastUpdatedItem("CPU收集失败");
                            task.hostInfo.setCpuStatus(OsInfoStatusEnum.ERROR);
                            task.hostInfo.setMessage("CPU信息收集失败: " + e.getMessage());
                            service.updateHostInfoCache(task.hostInfo);
                        }

                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    // 所有主机的CPU信息收集完成，进入下一阶段
                    logger.info("所有主机的CPU信息收集完成，进入内存信息收集阶段");
                    currentStage = CollectionStage.MEMORY_INFO;

                    // 开始处理内存信息收集
                    processMemoryInfoQueue();
                } finally {
                    processingCpuInfo = false;
                }
            }, service.cpuInfoExecutor);
        }

        /**
         * 处理内存信息收集队列
         */
        private void processMemoryInfoQueue() {
            processingMemoryInfo = true;

            // 使用memoryInfoExecutor处理队列中的所有任务
            CompletableFuture.runAsync(() -> {
                try {
                    for (HardwareInfoTask task : sortedHardwareTasks) {
                        if (task == null || task.hostInfo == null) {
                            continue;
                        }

                        logger.info("开始收集内存信息: {}", task.hostInfo.getIp());

                        // 收集内存信息
                        try {
                            // 设置正在收集内存信息
                            task.osInfo.setLastUpdatedItem("collecting_memory");
                            task.hostInfo.setMemoryStatus(OsInfoStatusEnum.LOADING);
                            task.hostInfo.setMessage("正在收集内存信息...");
                            service.updateHostInfoCache(task.hostInfo);

                            // 从收集器获取内存信息收集方法并执行
                            collectMemoryInfo(task);

                            // 设置成功状态
                            task.hostInfo.setMemoryStatus(OsInfoStatusEnum.SUCCESS);
                            task.hostInfo.setMessage("内存信息收集完成");
                            service.updateHostInfoCache(task.hostInfo);

                            // 设置硬件信息收集成功
                            task.osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.SUCCESS);

                            // 更新计数器
                            memoryInfoCollectionCompleted++;
                        } catch (Exception e) {
                            logger.error("收集内存信息时出错: {}", e.getMessage(), e);
                            task.osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
                            task.osInfo.setLastUpdatedItem("内存收集失败");
                            task.hostInfo.setMemoryStatus(OsInfoStatusEnum.ERROR);
                            task.hostInfo.setMessage("内存信息收集失败: " + e.getMessage());
                            service.updateHostInfoCache(task.hostInfo);
                        }

                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    // 所有主机的内存信息收集完成，进入下一阶段
                    logger.info("所有主机的内存信息收集完成，进入磁盘信息收集阶段");
                    currentStage = CollectionStage.DISK_INFO;

                    // 开始处理磁盘信息收集
                    processDiskInfoQueue();
                } finally {
                    processingMemoryInfo = false;
                }
            }, service.memoryInfoExecutor);
        }

        /**
         * 处理磁盘信息收集队列
         */
        private void processDiskInfoQueue() {
            processingDiskInfo = true;

            // 使用diskInfoExecutor处理队列中的所有任务
            CompletableFuture.runAsync(() -> {
                try {
                    for (HardwareInfoTask task : sortedHardwareTasks) {
                        if (task == null || task.hostInfo == null) {
                            continue;
                        }

                        logger.info("开始收集磁盘信息: {}", task.hostInfo.getIp());

                        // 收集磁盘信息
                        try {
                            // 设置正在收集磁盘信息
                            task.osInfo.setLastUpdatedItem("collecting_disk");
                            task.hostInfo.setDiskStatus(OsInfoStatusEnum.LOADING);
                            task.hostInfo.setMessage("正在收集磁盘信息...");
                            service.updateHostInfoCache(task.hostInfo);

                            // 根据操作系统类型收集磁盘信息
                            if (task.osInfo.getDistributionId() != null &&
                                    task.osInfo.getDistributionId().toLowerCase().contains("windows")) {
                                // Windows系统磁盘信息
                                collectDiskInfoWindows(task);
                            } else {
                                // Linux系统磁盘信息
                                collectDiskInfoLinux(task);
                            }

                            // 设置成功状态
                            task.hostInfo.setDiskStatus(OsInfoStatusEnum.SUCCESS);
                            task.hostInfo.setMessage("磁盘信息收集完成");
                            service.updateHostInfoCache(task.hostInfo);

                            // 设置硬件信息收集成功
                            task.osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.SUCCESS);

                            // 更新计数器
                            diskInfoCollectionCompleted++;
                        } catch (Exception e) {
                            logger.error("收集磁盘信息时出错: {}", e.getMessage(), e);
                            task.osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
                            task.osInfo.setLastUpdatedItem("磁盘收集失败");
                            task.hostInfo.setDiskStatus(OsInfoStatusEnum.ERROR);
                            task.hostInfo.setMessage("磁盘信息收集失败: " + e.getMessage());
                            service.updateHostInfoCache(task.hostInfo);
                        }

                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    // 所有主机的磁盘信息收集完成，进入交换空间收集阶段
                    logger.info("所有主机的磁盘信息收集完成，进入交换空间收集阶段");
                    currentStage = CollectionStage.SWAP_INFO;

                    // 开始处理交换空间信息收集
                    processSwapInfoQueue();
                } finally {
                    processingDiskInfo = false;
                }
            }, service.diskInfoExecutor);
        }

        /**
         * 处理交换空间信息收集队列
         */
        private void processSwapInfoQueue() {
            processingSwapInfo = true;

            // 使用swapInfoExecutor处理队列中的所有任务
            CompletableFuture.runAsync(() -> {
                try {
                    for (HardwareInfoTask task : sortedHardwareTasks) {
                        if (task == null || task.hostInfo == null) {
                            continue;
                        }

                        logger.info("开始收集交换空间信息: {}", task.hostInfo.getIp());

                        // 收集交换空间信息
                        try {
                            // 设置正在收集交换空间信息
                            task.osInfo.setLastUpdatedItem("collecting_swap");
                            task.hostInfo.setSwapStatus(OsInfoStatusEnum.LOADING);
                            task.hostInfo.setMessage("正在收集交换空间信息...");
                            service.updateHostInfoCache(task.hostInfo);

                            // Windows系统通常没有交换分区的概念
                            if (task.osInfo.getDistributionId() != null &&
                                    !task.osInfo.getDistributionId().toLowerCase().contains("windows")) {
                                // 只为Linux系统收集交换空间信息
                                collectSwapInfoLinux(task);
                            } else {
                                logger.info("Windows系统跳过交换空间收集: {}", task.hostInfo.getIp());
                            }

                            // 设置成功状态
                            task.hostInfo.setSwapStatus(OsInfoStatusEnum.SUCCESS);
                            task.hostInfo.setMessage("交换空间信息收集完成");
                            service.updateHostInfoCache(task.hostInfo);

                            // 设置硬件信息收集成功
                            task.osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.SUCCESS);

                            // 更新计数器
                            swapInfoCollectionCompleted++;
                        } catch (Exception e) {
                            logger.error("收集交换空间信息时出错: {}", e.getMessage(), e);
                            task.osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
                            task.osInfo.setLastUpdatedItem("交换空间收集失败");
                            task.hostInfo.setSwapStatus(OsInfoStatusEnum.ERROR);
                            task.hostInfo.setMessage("交换空间信息收集失败: " + e.getMessage());
                            service.updateHostInfoCache(task.hostInfo);
                        }

                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    // 所有主机的交换空间信息收集完成，进入GPU信息收集阶段
                    logger.info("所有主机的交换空间信息收集完成，进入GPU信息收集阶段");
                    currentStage = CollectionStage.GPU_INFO;

                    // 开始处理GPU信息收集
                    processGpuInfoQueue();
                } finally {
                    processingSwapInfo = false;
                }
            }, service.swapInfoExecutor);
        }

        /**
         * 处理GPU信息收集队列
         */
        private void processGpuInfoQueue() {
            processingGpuInfo = true;

            // 使用gpuInfoExecutor处理队列中的所有任务
            CompletableFuture.runAsync(() -> {
                try {
                    for (HardwareInfoTask task : sortedHardwareTasks) {
                        if (task == null || task.hostInfo == null) {
                            continue;
                        }

                        logger.info("开始收集GPU信息: {}", task.hostInfo.getIp());

                        // 收集GPU信息
                        try {
                            // 设置正在收集GPU信息
                            task.osInfo.setLastUpdatedItem("collecting_gpu");
                            task.hostInfo.setGpuStatus(OsInfoStatusEnum.LOADING);
                            task.hostInfo.setMessage("正在收集GPU信息...");
                            service.updateHostInfoCache(task.hostInfo);

                            // 从收集器获取GPU信息收集方法并执行
                            collectGpuInfo(task);

                            // 设置成功状态
                            task.hostInfo.setGpuStatus(OsInfoStatusEnum.SUCCESS);
                            task.hostInfo.setMessage("GPU信息收集完成");
                            task.osInfo.setLastUpdatedItem("completed");
                            task.osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.SUCCESS);
                            service.updateHostInfoCache(task.hostInfo);

                            // 更新计数器
                            gpuInfoCollectionCompleted++;

                            logger.info("硬件信息收集完成: {}, 主机名: {}",
                                    task.hostInfo.getIp(),
                                    task.hostInfo.getHostname());
                        } catch (Exception e) {
                            logger.error("收集GPU信息时出错: {}", e.getMessage(), e);
                            task.osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
                            task.osInfo.setLastUpdatedItem("GPU收集失败");
                            task.hostInfo.setGpuStatus(OsInfoStatusEnum.ERROR);
                            task.hostInfo.setMessage("GPU信息收集失败: " + e.getMessage());
                            service.updateHostInfoCache(task.hostInfo);
                        }

                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    // 所有主机的GPU信息收集完成，整个收集过程完成
                    logger.info("所有主机的信息收集完成");
                    currentStage = CollectionStage.COMPLETED;

                    // 遍历所有收集任务，设置完成状态
                    for (HardwareInfoTask task : sortedHardwareTasks) {
                        if (task != null && task.hostInfo != null) {
                            // 设置整体收集完成状态
                            task.hostInfo.setMessage("所有信息收集完成");
                            service.updateHostInfoCache(task.hostInfo);

                            logger.info("主机 {} 的所有信息收集已完成", task.hostInfo.getIp());
                        }
                    }

                    // 更新服务缓存中主机的信息收集状态
                    logger.info("更新所有主机的整体收集完成状态");

                    // 关闭所有会话
                    for (HardwareInfoTask task : sortedHardwareTasks) {
                        if (task != null && task.session != null) {
                            try {
                                task.session.close();
                            } catch (Exception e) {
                                logger.warn("关闭会话时出错: {}", e.getMessage());
                            }
                        }
                    }
                } finally {
                    processingGpuInfo = false;
                }
            }, service.gpuInfoExecutor);
        }

        /**
         * 收集CPU信息
         */
        private void collectCpuInfo(HardwareInfoTask task) {
            if (task.osInfo.getDistributionId().toLowerCase().contains("windows")) {
                collectCpuInfoWindows(task);
            } else {
                collectCpuInfoLinux(task);
            }
        }

        /**
         * 收集Linux系统CPU信息
         */
        private void collectCpuInfoLinux(HardwareInfoTask task) {
            try {
                String cpuInfoCmd = "cat /proc/cpuinfo";
                String cpuInfo = MinaUtils.execCmdWithResult(task.session, cpuInfoCmd);

                if (StringUtils.isNotBlank(cpuInfo)) {
                    // 首先设置CPU状态为加载中
                    task.osInfo.setCpuStatus(OsInfoStatusEnum.COLLECTING);
                    task.service.updateHostInfoCache(task.hostInfo);

                    // 调用LinuxOsInfoCollector解析CPU信息
                    LinuxOsInfoCollector linuxCollector = new LinuxOsInfoCollector();
                    linuxCollector.parseCpuInfo(task.osInfo, cpuInfo);

                    // 再执行top命令获取CPU使用率
                    String topCpuInfo = MinaUtils.execCmdWithResult(task.session, "top -bn1 | grep '%Cpu'");
                    if (StringUtils.isNotBlank(topCpuInfo)) {
                        linuxCollector.parseCpuUsage(task.osInfo, topCpuInfo);
                    }

                    // 设置CPU状态为成功
                    task.osInfo.setCpuStatus(OsInfoStatusEnum.SUCCESS);
                    logger.info("Linux CPU信息收集完成：{}, 型号: {}, 核心数: {}",
                            task.hostInfo.getIp(), task.osInfo.getCpuModel(), task.osInfo.getCpuCores());
                } else {
                    // 如果命令执行结果为空，设置失败状态
                    task.osInfo.setCpuStatus(OsInfoStatusEnum.ERROR);
                    logger.warn("Linux CPU信息获取失败：{}, 命令执行结果为空", task.hostInfo.getIp());
                }
            } catch (Exception e) {
                // 发生异常，设置失败状态
                task.osInfo.setCpuStatus(OsInfoStatusEnum.ERROR);
                logger.error("Linux CPU信息收集异常：{}, 错误: {}", task.hostInfo.getIp(), e.getMessage(), e);
            } finally {
                // 无论成功或失败，都更新缓存
                task.service.updateHostInfoCache(task.hostInfo);
            }
        }

        /**
         * 收集Windows系统CPU信息
         */
        private void collectCpuInfoWindows(HardwareInfoTask task) {
            try {
                String cpuInfoCmd = "wmic cpu get Name, NumberOfCores, NumberOfLogicalProcessors /Value";
                String cpuInfo = MinaUtils.execCmdWithResult(task.session, cpuInfoCmd);

                if (StringUtils.isNotBlank(cpuInfo)) {
                    // 首先设置CPU状态为加载中
                    task.osInfo.setCpuStatus(OsInfoStatusEnum.COLLECTING);
                    task.service.updateHostInfoCache(task.hostInfo);

                    // 调用WindowsOsInfoCollector解析CPU信息
                    WindowsOsInfoCollector windowsCollector = new WindowsOsInfoCollector();
                    windowsCollector.parseCpuInfo(task.osInfo, cpuInfo);

                    // 设置CPU状态为成功
                    task.osInfo.setCpuStatus(OsInfoStatusEnum.SUCCESS);
                    logger.info("Windows CPU信息收集完成：{}, 型号: {}, 核心数: {}",
                            task.hostInfo.getIp(), task.osInfo.getCpuModel(), task.osInfo.getCpuCores());
                } else {
                    // 如果命令执行结果为空，设置失败状态
                    task.osInfo.setCpuStatus(OsInfoStatusEnum.ERROR);
                    logger.warn("Windows CPU信息获取失败：{}, 命令执行结果为空", task.hostInfo.getIp());
                }
            } catch (Exception e) {
                // 发生异常，设置失败状态
                task.osInfo.setCpuStatus(OsInfoStatusEnum.ERROR);
                logger.error("Windows CPU信息收集异常：{}, 错误: {}", task.hostInfo.getIp(), e.getMessage(), e);
            } finally {
                // 无论成功或失败，都更新缓存
                task.service.updateHostInfoCache(task.hostInfo);
            }
        }

        /**
         * 收集内存信息
         */
        private void collectMemoryInfo(HardwareInfoTask task) {
            if (task.osInfo.getDistributionId().toLowerCase().contains("windows")) {
                collectMemoryInfoWindows(task);
            } else {
                collectMemoryInfoLinux(task);
            }
        }

        /**
         * 收集Linux系统内存信息
         */
        private void collectMemoryInfoLinux(HardwareInfoTask task) {
            try {
                String memInfoCmd = "cat /proc/meminfo";
                String memInfo = MinaUtils.execCmdWithResult(task.session, memInfoCmd);

                if (StringUtils.isNotBlank(memInfo)) {
                    // 首先设置内存状态为加载中
                    task.osInfo.setMemoryStatus(OsInfoStatusEnum.COLLECTING);
                    task.service.updateHostInfoCache(task.hostInfo);

                    // 调用LinuxOsInfoCollector解析内存信息
                    LinuxOsInfoCollector linuxCollector = new LinuxOsInfoCollector();
                    linuxCollector.parseMemoryInfo(task.osInfo, memInfo);

                    // 设置内存状态为成功
                    task.osInfo.setMemoryStatus(OsInfoStatusEnum.SUCCESS);
                    logger.info("Linux 内存信息收集完成：{}, 总内存: {}MB, 可用内存: {}MB",
                            task.hostInfo.getIp(), task.osInfo.getTotalMemory(), task.osInfo.getAvailableMemory());
                } else {
                    // 如果命令执行结果为空，设置失败状态
                    task.osInfo.setMemoryStatus(OsInfoStatusEnum.ERROR);
                    logger.warn("Linux 内存信息获取失败：{}, 命令执行结果为空", task.hostInfo.getIp());
                }
            } catch (Exception e) {
                // 发生异常，设置失败状态
                task.osInfo.setMemoryStatus(OsInfoStatusEnum.ERROR);
                logger.error("Linux 内存信息收集异常：{}, 错误: {}", task.hostInfo.getIp(), e.getMessage(), e);
            } finally {
                // 无论成功或失败，都更新缓存
                task.service.updateHostInfoCache(task.hostInfo);
            }
        }

        /**
         * 收集Windows系统内存信息
         */
        private void collectMemoryInfoWindows(HardwareInfoTask task) {
            try {
                String memInfoCmd = "wmic OS get TotalVisibleMemorySize, FreePhysicalMemory /Value";
                String memInfo = MinaUtils.execCmdWithResult(task.session, memInfoCmd);

                if (StringUtils.isNotBlank(memInfo)) {
                    // 首先设置内存状态为加载中
                    task.osInfo.setMemoryStatus(OsInfoStatusEnum.COLLECTING);
                    task.service.updateHostInfoCache(task.hostInfo);

                    // 调用WindowsOsInfoCollector解析内存信息
                    WindowsOsInfoCollector windowsCollector = new WindowsOsInfoCollector();
                    windowsCollector.parseMemoryInfo(task.osInfo, memInfo);

                    // 设置内存状态为成功
                    task.osInfo.setMemoryStatus(OsInfoStatusEnum.SUCCESS);
                    logger.info("Windows 内存信息收集完成：{}, 总内存: {}MB, 可用内存: {}MB",
                            task.hostInfo.getIp(), task.osInfo.getTotalMemory(), task.osInfo.getAvailableMemory());
                } else {
                    // 如果命令执行结果为空，设置失败状态
                    task.osInfo.setMemoryStatus(OsInfoStatusEnum.ERROR);
                    logger.warn("Windows 内存信息获取失败：{}, 命令执行结果为空", task.hostInfo.getIp());
                }
            } catch (Exception e) {
                // 发生异常，设置失败状态
                task.osInfo.setMemoryStatus(OsInfoStatusEnum.ERROR);
                logger.error("Windows 内存信息收集异常：{}, 错误: {}", task.hostInfo.getIp(), e.getMessage(), e);
            } finally {
                // 无论成功或失败，都更新缓存
                task.service.updateHostInfoCache(task.hostInfo);
            }
        }

        /**
         * 收集磁盘信息
         */
        private void collectDiskInfoWindows(HardwareInfoTask task) {
            String diskInfoCmd = "wmic logicaldisk get DeviceID, Size, FreeSpace /Value";
            String diskInfo = MinaUtils.execCmdWithResult(task.session, diskInfoCmd);

            // TODO：解析磁盘信息并更新osInfo对象
            // 这里可以调用WindowsOsInfoCollector中的解析方法

            task.service.updateHostInfoCache(task.hostInfo);
        }

        /**
         * 收集Linux磁盘信息
         */
        private void collectDiskInfoLinux(HardwareInfoTask task) {
            String diskInfoCmd = "df -P";
            String diskInfo = MinaUtils.execCmdWithResult(task.session, diskInfoCmd);

            // TODO：解析磁盘信息并更新osInfo对象
            // 这里可以调用LinuxOsInfoCollector中的解析方法

            task.service.updateHostInfoCache(task.hostInfo);
        }

        /**
         * 收集Linux交换分区信息
         */
        private void collectSwapInfoLinux(HardwareInfoTask task) {
            String swapInfoCmd = "grep Swap /proc/meminfo";
            String swapInfo = MinaUtils.execCmdWithResult(task.session, swapInfoCmd);

            // TODO：解析交换分区信息并更新osInfo对象
            // 这里可以调用LinuxOsInfoCollector中的解析方法

            task.service.updateHostInfoCache(task.hostInfo);
        }

        /**
         * 收集GPU信息
         */
        private void collectGpuInfo(HardwareInfoTask task) {
            if (task.osInfo.getDistributionId().toLowerCase().contains("windows")) {
                collectGpuInfoWindows(task);
            } else {
                collectGpuInfoLinux(task);
            }
        }

        /**
         * 收集Windows GPU信息
         */
        private void collectGpuInfoWindows(HardwareInfoTask task) {
            String gpuInfoCmd = "wmic path win32_VideoController get Name, AdapterRAM /Value";
            String gpuInfo = MinaUtils.execCmdWithResult(task.session, gpuInfoCmd);

            // TODO：解析GPU信息并更新osInfo对象
            // 这里可以调用WindowsOsInfoCollector中的解析方法

            task.service.updateHostInfoCache(task.hostInfo);
        }

        /**
         * 收集Linux GPU信息
         */
        private void collectGpuInfoLinux(HardwareInfoTask task) {
            String gpuInfoCmd = "lspci | grep -i 'vga\\|3d\\|2d' | cut -d ':' -f3";
            String gpuInfo = MinaUtils.execCmdWithResult(task.session, gpuInfoCmd);

            // TODO：解析GPU信息并更新osInfo对象
            // 这里可以调用LinuxOsInfoCollector中的解析方法

            task.service.updateHostInfoCache(task.hostInfo);
        }

        /**
         * 添加主机到CPU信息收集队列
         * 此方法保留用于兼容现有代码，实际上新的收集流程不会直接调用此方法
         */
        public synchronized void addHostToCpuInfoQueue(HostInfo hostInfo, OsInfo osInfo, OsInfoServiceImpl service) {
            // 由于我们现在使用排序列表进行收集，这个方法只是为了兼容性而保留
            // 实际上，所有的主机收集任务都应该通过addHostToQueue方法添加
            logger.debug("使用旧方法添加主机到CPU信息收集队列: {}", hostInfo.getIp());

            // 如果主机不在排序列表中，将其添加到主机列表中
            if (!sortedHostList.contains(hostInfo)) {
                addHostToQueue(hostInfo, null);
            }
        }
    }

    /**
     * 硬件信息收集任务类
     * 封装硬件信息收集所需的数据
     */
    private static class HardwareInfoTask {
        final HostInfo hostInfo;
        final OsInfo osInfo;
        final ClientSession session;
        final IOsInfoCollector collector;
        final OsInfoServiceImpl service;

        public HardwareInfoTask(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector, OsInfoServiceImpl service) {
            this.hostInfo = hostInfo;
            this.osInfo = osInfo;
            this.session = session;
            this.collector = collector;
            this.service = service;
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
                return osInfo;
            }

            // 首先确定操作系统类型（Windows或Linux）
            String osType = detectOperatingSystemType(session);
            logger.info("主机 {} 的操作系统类型为: {}", hostInfo.getIp(), osType);

            // 使用工厂获取相应的操作系统信息收集器
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector(osType);
            if (collector != null) {
                // 使用收集器并传入缓存更新函数
                return collector.collectOsInfo(hostInfo, session, osInfo, this::updateHostInfoCache);
            } else {
                logger.warn("未找到适用于{}操作系统的信息收集器", osType);
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
            return osInfo;
        }
    }

    /**
     * 检测操作系统类型（Windows或Linux）
     */
    public String detectOperatingSystemType(ClientSession session) {
        try {
            // 尝试执行Windows命令
            String windowsCheck = MinaUtils.execCmdWithResult(session, "powershell -command \"$env:OS\"");
            if (windowsCheck != null && windowsCheck.toLowerCase().contains("windows")) {
                return "windows";
            }

            // 尝试执行Linux命令
            String linuxCheck = MinaUtils.execCmdWithResult(session, "uname -a");
            if (linuxCheck != null && linuxCheck.toLowerCase().contains("linux")) {
                return "linux";
            }

            // 尝试检查/etc/os-release文件
            String osReleaseCheck = MinaUtils.execCmdWithResult(session, "test -f /etc/os-release && echo 'exists'");
            if (osReleaseCheck != null && osReleaseCheck.trim().equals("exists")) {
                return "linux";
            }

            // 尝试执行lsb_release命令
            String lsbReleaseCheck = MinaUtils.execCmdWithResult(session, "which lsb_release && echo 'exists'");
            if (lsbReleaseCheck != null && lsbReleaseCheck.trim().equals("exists")) {
                return "linux";
            }

            // 默认假设为Linux
            logger.warn("无法确定操作系统类型，默认为Linux");
            return "linux";
        } catch (Exception e) {
            logger.error("检测操作系统类型时出错: {}", e.getMessage(), e);
            return "linux";
        }
    }

    /**
     * 获取或创建SSH会话
     */
    public ClientSession getOrCreateSession(HostInfo hostInfo) {
        ClientSession session = null;

        try {
            String ip = hostInfo.getIp();
            Integer sshPort = hostInfo.getSshPort();
            String sshUser = hostInfo.getSshUser();
            String sshPassword = hostInfo.getSshPassword();

            if (StringUtils.isBlank(ip) || sshPort == null || StringUtils.isBlank(sshUser)
                    || StringUtils.isBlank(sshPassword)) {
                logger.warn("创建SSH会话失败: 缺少必要的连接参数");
                return null;
            }

            logger.info("创建到主机 {} 的SSH会话，用户: {}, 端口: {}", ip, sshUser, sshPort);

            // 使用密码方式连接
            try {
                session = MinaUtils.openConnectionWithPassword(hostInfo);
                if (session != null) {
                    logger.info("成功使用密码连接到主机: {}", ip);
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