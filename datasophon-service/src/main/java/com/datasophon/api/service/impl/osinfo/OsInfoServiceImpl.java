package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.service.OsInfoService;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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

    // 为OS信息收集创建专用的高优先级线程池
    private ExecutorService osInfoExecutor;
    private ExecutorService hardwareInfoExecutor;

    // 添加四个硬件收集队列，分别用于收集CPU、内存、存储和GPU信息
    private ExecutorService cpuInfoExecutor;
    private ExecutorService memoryInfoExecutor;
    private ExecutorService diskInfoExecutor;
    private ExecutorService gpuInfoExecutor;

    // 添加主机信息收集队列管理器
    private final HostInfoCollectionQueueManager queueManager = new HostInfoCollectionQueueManager(this);

    @PostConstruct
    public void init() {
        // 创建一个自定义线程工厂，设置线程为守护线程并设置最高优先级
        ThreadFactory osInfoThreadFactory = r -> {
            Thread t = new Thread(r, "os-info-collector");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY); // 设置最高优先级
            return t;
        };

        ThreadFactory hardwareInfoThreadFactory = r -> {
            Thread t = new Thread(r, "hardware-info-collector");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY - 1); // 设置次高优先级
            return t;
        };

        // 创建四个硬件信息收集线程工厂
        ThreadFactory cpuInfoThreadFactory = r -> {
            Thread t = new Thread(r, "cpu-info-collector");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 3);
            return t;
        };

        ThreadFactory memoryInfoThreadFactory = r -> {
            Thread t = new Thread(r, "memory-info-collector");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 2);
            return t;
        };

        ThreadFactory diskInfoThreadFactory = r -> {
            Thread t = new Thread(r, "disk-info-collector");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 1);
            return t;
        };

        ThreadFactory gpuInfoThreadFactory = r -> {
            Thread t = new Thread(r, "gpu-info-collector");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        };

        // 创建固定大小的线程池，专用于OS信息收集
        osInfoExecutor = Executors.newFixedThreadPool(4, osInfoThreadFactory);
        hardwareInfoExecutor = Executors.newFixedThreadPool(4, hardwareInfoThreadFactory);

        // 创建四个固定大小的线程池，分别用于收集不同类型的硬件信息
        cpuInfoExecutor = Executors.newFixedThreadPool(4, cpuInfoThreadFactory);
        memoryInfoExecutor = Executors.newFixedThreadPool(4, memoryInfoThreadFactory);
        diskInfoExecutor = Executors.newFixedThreadPool(4, diskInfoThreadFactory);
        gpuInfoExecutor = Executors.newFixedThreadPool(4, gpuInfoThreadFactory);

        logger.info("已初始化OS信息收集高优先级线程池");
    }

    @PreDestroy
    public void destroy() {
        // 程序关闭时，关闭线程池
        if (osInfoExecutor != null) {
            osInfoExecutor.shutdown();
        }
        if (hardwareInfoExecutor != null) {
            hardwareInfoExecutor.shutdown();
        }

        // 关闭四个硬件信息收集线程池
        if (cpuInfoExecutor != null) {
            cpuInfoExecutor.shutdown();
        }
        if (memoryInfoExecutor != null) {
            memoryInfoExecutor.shutdown();
        }
        if (diskInfoExecutor != null) {
            diskInfoExecutor.shutdown();
        }
        if (gpuInfoExecutor != null) {
            gpuInfoExecutor.shutdown();
        }

        logger.info("已关闭OS信息收集线程池");
    }

    @Override
    public void getHostOsInfoAsync(HostInfo hostInfo) {
        // 保存原始主机名，避免在异步处理中丢失
        final String originalHostname = hostInfo.getHostname();
        logger.debug("开始异步获取主机信息, IP:{}, 原始主机名:{}", hostInfo.getIp(), originalHostname);

        hostInfo.setOsInfoStatus("loading");
        hostInfo.setSshConnectStatus("connecting"); // 添加SSH连接状态
        // 立即更新缓存，让前端看到加载状态
        updateHostInfoCache(hostInfo);

        // 将主机信息收集任务添加到队列
        queueManager.addHostToQueue(hostInfo, null);
    }

    /**
     * 处理主机信息收集流程
     * 该方法由队列管理器调用，按照指定的顺序获取主机信息
     */
    private void processHostInfoCollection(HostInfo hostInfo) {
        // 保存原始主机名，避免在异步处理中丢失
        final String originalHostname = hostInfo.getHostname();

        // 使用高优先级线程池执行OS信息收集任务
        CompletableFuture.runAsync(() -> {
            try {
                // 获取操作系统信息
                OsInfo osInfo = getHostOsInfoInternal(hostInfo);

                // 确保主机名不会丢失
                if (StringUtils.isBlank(hostInfo.getHostname()) && StringUtils.isNotBlank(originalHostname)) {
                    logger.info("检测到主机名为空，恢复原始主机名: {}", originalHostname);
                    hostInfo.setHostname(originalHostname);
                }

                // 确保OsInfo中的主机名与HostInfo一致
                if (osInfo != null && StringUtils.isNotBlank(hostInfo.getHostname())) {
                    if (StringUtils.isBlank(osInfo.getHostname())) {
                        osInfo.setHostname(hostInfo.getHostname());
                        logger.debug("设置OSInfo主机名为: {}", hostInfo.getHostname());
                    }
                }

                // 如果SSH连接失败，直接返回错误状态
                if ("error".equals(hostInfo.getSshConnectStatus())) {
                    logger.warn("主机 {} 的SSH连接失败，不再继续收集硬件信息", hostInfo.getIp());
                    updateHostInfoCache(hostInfo);
                    return;
                }

                // 设置数据
                hostInfo.setOsInfo(osInfo);
                hostInfo.setOsInfoStatus("success");
                hostInfo.setSshConnectStatus("success"); // 设置SSH连接成功状态
                
                // 设置硬件收集状态为collecting
                if (osInfo != null) {
                    osInfo.setHardwareCollectionStatus("collecting");
                }
                
                updateHostInfoCache(hostInfo);

                // 添加硬件信息收集任务到队列
                queueManager.addHostToCpuInfoQueue(hostInfo, osInfo, this);

            } catch (Exception e) {
                logger.error("获取操作系统信息时出错: {}", e.getMessage(), e);
                hostInfo.setOsInfoStatus("error");
                hostInfo.setSshConnectStatus("error"); // 设置SSH连接错误状态

                // 确保即使出错时也保留主机名
                if (StringUtils.isBlank(hostInfo.getHostname()) && StringUtils.isNotBlank(originalHostname)) {
                    hostInfo.setHostname(originalHostname);
                }

                // 如果osInfo不为空，设置错误信息
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setValid(false);
                    hostInfo.getOsInfo().setErrorMessage("SSH连接异常: " + e.getMessage());
                }

                updateHostInfoCache(hostInfo);
            }
        }, osInfoExecutor);
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
        private int cpuInfoCollectionCompleted = 0;
        private int memoryInfoCollectionCompleted = 0;
        private int diskInfoCollectionCompleted = 0;
        private int gpuInfoCollectionCompleted = 0;
        
        // 标记当前收集阶段
        private enum CollectionStage {
            HOST_NAME,    // 收集主机名
            OS_TYPE,      // 收集操作系统类型
            CPU_INFO,     // 收集CPU信息
            MEMORY_INFO,  // 收集内存信息
            DISK_INFO,    // 收集磁盘信息
            GPU_INFO,     // 收集GPU信息
            COMPLETED     // 所有收集完成
        }
        
        private volatile CollectionStage currentStage = CollectionStage.HOST_NAME;

        // 标记各队列处理状态
        private volatile boolean processingOsInfo = false;
        private volatile boolean processingCpuInfo = false;
        private volatile boolean processingMemoryInfo = false;
        private volatile boolean processingDiskInfo = false;
        private volatile boolean processingGpuInfo = false;

        /**
         * 构造函数
         * @param service 外部服务实例
         */
        public HostInfoCollectionQueueManager(OsInfoServiceImpl service) {
            this.service = service;
        }

        /**
         * 如果处理器未运行，则启动相应的处理器
         */
        private synchronized void startProcessingIfNeeded() {
            // 按照阶段顺序执行处理
            switch (currentStage) {
                case HOST_NAME:
                    if (!processingOsInfo && !sortedHostList.isEmpty()) {
                        processHostNameCollection();
                    }
                    break;
                case OS_TYPE:
                    if (!processingOsInfo && hostNameCollectionCompleted == sortedHostList.size()) {
                        processOsTypeCollection();
                    }
                    break;
                case CPU_INFO:
                    if (!processingCpuInfo && osTypeCollectionCompleted == sortedHostList.size()) {
                        processCpuInfoQueue();
                    }
                    break;
                case MEMORY_INFO:
                    if (!processingMemoryInfo && cpuInfoCollectionCompleted == sortedHardwareTasks.size()) {
                        processMemoryInfoQueue();
                    }
                    break;
                case DISK_INFO:
                    if (!processingDiskInfo && memoryInfoCollectionCompleted == sortedHardwareTasks.size()) {
                        processDiskInfoQueue();
                    }
                    break;
                case GPU_INFO:
                    if (!processingGpuInfo && diskInfoCollectionCompleted == sortedHardwareTasks.size()) {
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
         * 按顺序处理主机名收集
         */
        private void processHostNameCollection() {
            processingOsInfo = true;
            
            // 使用CompletableFuture处理排序后的主机列表
            CompletableFuture.runAsync(() -> {
                try {
                    for (HostInfo hostInfo : sortedHostList) {
                        logger.info("开始收集主机名: {}", hostInfo.getIp());
                        
                        // 执行主机名收集
                        processHostName(hostInfo);
                        
                        // 更新计数器
                        hostNameCollectionCompleted++;
                        
                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    
                    // 所有主机名收集完成，进入下一阶段
                    logger.info("所有主机名收集完成，进入操作系统类型收集阶段");
                    currentStage = CollectionStage.OS_TYPE;
                    
                    // 开始操作系统类型收集
                    processOsTypeCollection();
                } finally {
                    processingOsInfo = false;
                }
            }, service.osInfoExecutor);
        }
        
        /**
         * 收集单个主机的主机名
         */
        private void processHostName(HostInfo hostInfo) {
            try {
                ClientSession session = service.getOrCreateSession(hostInfo);
                if (session == null) {
                    logger.warn("创建SSH会话失败，无法收集主机名: {}", hostInfo.getIp());
                    hostInfo.setOsInfoStatus("error");
                    hostInfo.setSshConnectStatus("error");
                    service.updateHostInfoCache(hostInfo);
                    return;
                }
                
                // 根据操作系统类型选择适当的命令获取主机名
                String hostname;
                
                // 先尝试通用命令
                hostname = MinaUtils.execCmdWithResult(session, "hostname");
                
                if (StringUtils.isNotBlank(hostname)) {
                    hostname = hostname.trim();
                    hostInfo.setHostname(hostname);
                    logger.info("获取到主机名: {}", hostname);
                    
                    // 更新缓存
                    service.updateHostInfoCache(hostInfo);
                }
                
                // 关闭会话
                if (session != null) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        logger.warn("关闭SSH会话时出错: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.error("收集主机名时出错: {}", e.getMessage(), e);
                hostInfo.setOsInfoStatus("error");
                service.updateHostInfoCache(hostInfo);
            }
        }
        
        /**
         * 按顺序处理操作系统类型收集
         */
        private void processOsTypeCollection() {
            processingOsInfo = true;
            
            // 使用CompletableFuture处理排序后的主机列表
            CompletableFuture.runAsync(() -> {
                try {
                    for (HostInfo hostInfo : sortedHostList) {
                        logger.info("开始收集操作系统类型: {}", hostInfo.getIp());
                        
                        // 执行操作系统类型收集
                        processOsType(hostInfo);
                        
                        // 更新计数器
                        osTypeCollectionCompleted++;
                        
                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    
                    // 所有操作系统类型收集完成，进入下一阶段
                    logger.info("所有操作系统类型收集完成，进入系统详细信息收集阶段");
                    currentStage = CollectionStage.CPU_INFO;
                    
                    // 准备CPU收集队列
                    prepareCpuInfoQueue();
                } finally {
                    processingOsInfo = false;
                }
            }, service.osInfoExecutor);
        }
        
        /**
         * 收集单个主机的操作系统类型
         */
        private void processOsType(HostInfo hostInfo) {
            try {
                // 获取操作系统信息
                OsInfo osInfo = service.getHostOsInfoInternal(hostInfo);
                
                // 设置数据
                hostInfo.setOsInfo(osInfo);
                hostInfo.setOsInfoStatus("success");
                hostInfo.setSshConnectStatus("success");
                
                // 设置硬件收集状态为collecting
                if (osInfo != null) {
                    osInfo.setHardwareCollectionStatus("collecting");
                }
                
                // 更新缓存
                service.updateHostInfoCache(hostInfo);
            } catch (Exception e) {
                logger.error("收集操作系统类型时出错: {}", e.getMessage(), e);
                hostInfo.setOsInfoStatus("error");
                hostInfo.setSshConnectStatus("error");
                service.updateHostInfoCache(hostInfo);
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
                    if ("success".equals(hostInfo.getOsInfoStatus())) {
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
                            service.updateHostInfoCache(task.hostInfo);
                            
                            // 从收集器获取CPU信息收集方法并执行
                            collectCpuInfo(task);
                            
                            // 更新计数器
                            cpuInfoCollectionCompleted++;
                        } catch (Exception e) {
                            logger.error("收集CPU信息时出错: {}", e.getMessage(), e);
                            task.osInfo.setHardwareCollectionStatus("error");
                            task.osInfo.setLastUpdatedItem("CPU收集失败");
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
                            service.updateHostInfoCache(task.hostInfo);
                            
                            // 从收集器获取内存信息收集方法并执行
                            collectMemoryInfo(task);
                            
                            // 更新计数器
                            memoryInfoCollectionCompleted++;
                        } catch (Exception e) {
                            logger.error("收集内存信息时出错: {}", e.getMessage(), e);
                            task.osInfo.setHardwareCollectionStatus("error");
                            task.osInfo.setLastUpdatedItem("内存收集失败");
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
                            service.updateHostInfoCache(task.hostInfo);
                            
                            // 处理Linux和Windows系统的磁盘和交换分区信息收集逻辑
                            if (task.osInfo.getDistributionId() != null && 
                                task.osInfo.getDistributionId().toLowerCase().contains("windows")) {
                                // Windows系统只有磁盘信息
                                collectDiskInfoWindows(task);
                            } else {
                                // Linux系统有磁盘和交换分区信息
                                collectDiskInfoLinux(task);
                                
                                // 收集交换分区信息
                                task.osInfo.setLastUpdatedItem("collecting_swap");
                                service.updateHostInfoCache(task.hostInfo);
                                collectSwapInfoLinux(task);
                            }
                            
                            // 更新计数器
                            diskInfoCollectionCompleted++;
                        } catch (Exception e) {
                            logger.error("收集磁盘信息时出错: {}", e.getMessage(), e);
                            task.osInfo.setHardwareCollectionStatus("error");
                            task.osInfo.setLastUpdatedItem("磁盘收集失败");
                            service.updateHostInfoCache(task.hostInfo);
                        }
                        
                        // 短暂休眠，避免CPU占用过高
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    
                    // 所有主机的磁盘信息收集完成，进入下一阶段
                    logger.info("所有主机的磁盘信息收集完成，进入GPU信息收集阶段");
                    currentStage = CollectionStage.GPU_INFO;
                    
                    // 开始处理GPU信息收集
                    processGpuInfoQueue();
                } finally {
                    processingDiskInfo = false;
                }
            }, service.diskInfoExecutor);
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
                            service.updateHostInfoCache(task.hostInfo);
                            
                            // 从收集器获取GPU信息收集方法并执行
                            collectGpuInfo(task);
                            
                            // 标记为完成
                            task.osInfo.setLastUpdatedItem("completed");
                            task.osInfo.setHardwareCollectionStatus("success");
                            service.updateHostInfoCache(task.hostInfo);
                            
                            // 更新计数器
                            gpuInfoCollectionCompleted++;
                            
                            logger.info("硬件信息收集完成: {}, 主机名: {}",
                                    task.hostInfo.getIp(),
                                    task.hostInfo.getHostname());
                        } catch (Exception e) {
                            logger.error("收集GPU信息时出错: {}", e.getMessage(), e);
                            task.osInfo.setHardwareCollectionStatus("error");
                            task.osInfo.setLastUpdatedItem("GPU收集失败");
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
         * 收集Windows系统CPU信息
         */
        private void collectCpuInfoWindows(HardwareInfoTask task) {
            String cpuInfoCmd = "wmic cpu get Name, NumberOfCores, NumberOfLogicalProcessors /Value";
            String cpuInfo = MinaUtils.execCmdWithResult(task.session, cpuInfoCmd);

            // TODO：解析CPU信息并更新osInfo对象
            // 这里可以调用WindowsOsInfoCollector中的解析方法

            task.service.updateHostInfoCache(task.hostInfo);
        }

        /**
         * 收集Linux系统CPU信息
         */
        private void collectCpuInfoLinux(HardwareInfoTask task) {
            String cpuInfoCmd = "cat /proc/cpuinfo";
            String cpuInfo = MinaUtils.execCmdWithResult(task.session, cpuInfoCmd);

            // TODO：解析CPU信息并更新osInfo对象
            // 这里可以调用LinuxOsInfoCollector中的解析方法

            task.service.updateHostInfoCache(task.hostInfo);
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
         * 收集Windows系统内存信息
         */
        private void collectMemoryInfoWindows(HardwareInfoTask task) {
            String memInfoCmd = "wmic OS get TotalVisibleMemorySize, FreePhysicalMemory /Value";
            String memInfo = MinaUtils.execCmdWithResult(task.session, memInfoCmd);

            // TODO：解析内存信息并更新osInfo对象
            // 这里可以调用WindowsOsInfoCollector中的解析方法

            task.service.updateHostInfoCache(task.hostInfo);
        }

        /**
         * 收集Linux系统内存信息
         */
        private void collectMemoryInfoLinux(HardwareInfoTask task) {
            String memInfoCmd = "cat /proc/meminfo";
            String memInfo = MinaUtils.execCmdWithResult(task.session, memInfoCmd);

            // TODO：解析内存信息并更新osInfo对象
            // 这里可以调用LinuxOsInfoCollector中的解析方法

            task.service.updateHostInfoCache(task.hostInfo);
        }

        /**
         * 收集Windows磁盘信息
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
        try {
            // 如果传入的是null，尝试从当前处理中的任务获取hostInfo
            if (hostInfo == null) {
                // 目前我们简单地记录日志并返回，后续可以改进为从线程上下文获取当前处理的任务
                logger.debug("更新缓存时传入的hostInfo为null，跳过此次更新");
                return;
            }
            
            // 获取缓存中的主机信息
            Integer clusterId = hostInfo.getClusterId();
            if (clusterId == null) {
                logger.warn("主机 {} 未关联集群ID，无法更新缓存", hostInfo.getIp());
                return;
            }

            String cacheKey = clusterId + Constants.HOST_MAP;
            if (!CacheUtils.constainsKey(cacheKey)) {
                logger.warn("找不到集群 {} 的主机缓存，无法更新", clusterId);
                return;
            }

            Map<String, HostInfo> hostMap = (Map<String, HostInfo>) CacheUtils.get(cacheKey);
            if (hostMap != null) {
                // 记录更新前的状态
                String ip = hostInfo.getIp();

                // 获取缓存中已有的主机信息
                HostInfo existingHostInfo = hostMap.get(ip);
                String existingHostname = existingHostInfo != null ? existingHostInfo.getHostname() : null;

                String newHostname = hostInfo.getHostname();
                String osInfoStatus = hostInfo.getOsInfoStatus();
                OsInfo osInfo = hostInfo.getOsInfo();
                String hardwareStatus = osInfo != null ? osInfo.getHardwareCollectionStatus() : "unknown";
                String lastUpdatedItem = osInfo != null ? osInfo.getLastUpdatedItem() : "unknown";

                logger.debug("准备更新主机缓存: IP:{}, 现有主机名:{}, 新主机名:{}, 状态:{}, 硬件状态:{}",
                        ip, existingHostname, newHostname, osInfoStatus, hardwareStatus);

                // 创建主机信息的深拷贝，避免引用问题
                HostInfo hostInfoCopy = deepCopyHostInfo(hostInfo);

                // 如果新主机信息中hostname为空但缓存中有值，则保留缓存中的值
                if (StringUtils.isBlank(hostInfoCopy.getHostname()) &&
                        StringUtils.isNotBlank(existingHostname)) {

                    hostInfoCopy.setHostname(existingHostname);
                    logger.info("从缓存保留主机名: IP={}, 主机名={}", ip, existingHostname);

                    // 同时更新OsInfo中的主机名
                    if (hostInfoCopy.getOsInfo() != null &&
                            StringUtils.isBlank(hostInfoCopy.getOsInfo().getHostname())) {
                        hostInfoCopy.getOsInfo().setHostname(existingHostname);
                    }
                }

                // 更新缓存中的主机信息
                hostMap.put(ip, hostInfoCopy);

                // 立即更新缓存
                boolean updateSuccess = false;
                try {
                    CacheUtils.put(cacheKey, hostMap);
                    updateSuccess = true;
                } catch (Exception e) {
                    logger.error("更新缓存失败: {}", e.getMessage(), e);
                    // 尝试重新更新
                    try {
                        Thread.sleep(50); // 等待一段时间
                        CacheUtils.put(cacheKey, hostMap);
                        updateSuccess = true;
                        logger.info("重试更新缓存成功");
                    } catch (Exception e2) {
                        logger.error("重试更新缓存失败: {}", e2.getMessage(), e2);
                    }
                }

                if (updateSuccess) {
                    logger.info("已成功更新集群 {} 中主机 {} 的缓存信息: 主机名={}, 状态={}, 硬件状态={}",
                            clusterId, ip, hostInfoCopy.getHostname(), osInfoStatus, hardwareStatus);
                }
            } else {
                logger.warn("获取到的主机缓存映射为null, 集群ID: {}", clusterId);
            }
        } catch (Exception e) {
            logger.error("更新主机 {} 的缓存信息时出错: {}", hostInfo.getIp(), e.getMessage(), e);
        }
    }

    /**
     * 创建主机信息的深拷贝，避免引用问题
     */
    private HostInfo deepCopyHostInfo(HostInfo source) {
        if (source == null) {
            return null;
        }

        HostInfo copy = new HostInfo();

        // 复制基本属性
        copy.setIp(source.getIp());
        copy.setHostname(source.getHostname()); // 确保主机名被正确复制
        copy.setFqdn(source.getFqdn()); // 复制FQDN
        copy.setHostsFile(source.getHostsFile()); // 复制hosts文件内容
        copy.setSshUser(source.getSshUser());
        copy.setSshPort(source.getSshPort());
        copy.setSshPassword(source.getSshPassword());
        copy.setManaged(source.isManaged());
        copy.setProgress(source.getProgress());
        copy.setInstallState(source.getInstallState());
        copy.setInstallStateCode(source.getInstallStateCode());
        copy.setMessage(source.getMessage());
        copy.setErrMsg(source.getErrMsg());
        copy.setClusterId(source.getClusterId());
        copy.setCreateTime(source.getCreateTime());
        copy.setCheckResult(source.getCheckResult());
        copy.setCheckItems(source.getCheckItems());
        copy.setOsInfoStatus(source.getOsInfoStatus());

        // 复制OS信息 - 创建OsInfo的深拷贝以避免引用问题
        if (source.getOsInfo() != null) {
            OsInfo sourceOsInfo = source.getOsInfo();
            OsInfo osInfoCopy = new OsInfo();

            // 复制基本字符串属性
            osInfoCopy.setHostname(sourceOsInfo.getHostname());
            osInfoCopy.setFqdn(sourceOsInfo.getFqdn());
            osInfoCopy.setDistributionId(sourceOsInfo.getDistributionId());
            osInfoCopy.setDistribution(sourceOsInfo.getDistribution());
            osInfoCopy.setDistributionName(sourceOsInfo.getDistributionName());
            osInfoCopy.setVersionId(sourceOsInfo.getVersionId());
            osInfoCopy.setDistributionVersion(sourceOsInfo.getDistributionVersion());
            osInfoCopy.setFullName(sourceOsInfo.getFullName());
            osInfoCopy.setKernelVersion(sourceOsInfo.getKernelVersion());
            osInfoCopy.setArchitecture(sourceOsInfo.getArchitecture());
            osInfoCopy.setDnsServers(sourceOsInfo.getDnsServers());
            osInfoCopy.setCpuInfo(sourceOsInfo.getCpuInfo());
            osInfoCopy.setCpuModel(sourceOsInfo.getCpuModel());

            // 复制数值属性
            osInfoCopy.setCpuFrequency(sourceOsInfo.getCpuFrequency());
            osInfoCopy.setCpuCores(sourceOsInfo.getCpuCores());
            osInfoCopy.setCpuCoreNum(sourceOsInfo.getCpuCoreNum());
            osInfoCopy.setCpuCount(sourceOsInfo.getCpuCount());
            osInfoCopy.setCpuCoresPerProcessor(sourceOsInfo.getCpuCoresPerProcessor());
            osInfoCopy.setCpuThreadsPerCore(sourceOsInfo.getCpuThreadsPerCore());
            osInfoCopy.setCpuLogicalCores(sourceOsInfo.getCpuLogicalCores());

            // 复制内存相关属性
            osInfoCopy.setTotalMemory(sourceOsInfo.getTotalMemory());
            osInfoCopy.setTotalMem(sourceOsInfo.getTotalMem());
            osInfoCopy.setAvailableMemory(sourceOsInfo.getAvailableMemory());
            osInfoCopy.setAvailableMem(sourceOsInfo.getAvailableMem());

            // 复制交换空间相关属性 - 使用正确的类型
            osInfoCopy.setTotalSwapBytes(sourceOsInfo.getTotalSwapBytes());
            osInfoCopy.setAvailableSwapBytes(sourceOsInfo.getAvailableSwapBytes());

            // 复制磁盘相关属性 - 使用正确的类型
            osInfoCopy.setTotalDiskBytes(sourceOsInfo.getTotalDiskBytes());
            osInfoCopy.setAvailableDiskBytes(sourceOsInfo.getAvailableDiskBytes());

            // 复制GPU相关属性
            osInfoCopy.setGpuInfo(sourceOsInfo.getGpuInfo());
            osInfoCopy.setGpuMemory(sourceOsInfo.getGpuMemory());

            // 复制其他属性
            osInfoCopy.setValid(sourceOsInfo.isValid());
            osInfoCopy.setLoad1Min(sourceOsInfo.getLoad1Min());
            osInfoCopy.setLoad5Min(sourceOsInfo.getLoad5Min());
            osInfoCopy.setLoad15Min(sourceOsInfo.getLoad15Min());
            osInfoCopy.setHardwareCollectionStatus(sourceOsInfo.getHardwareCollectionStatus());
            osInfoCopy.setLastUpdatedItem(sourceOsInfo.getLastUpdatedItem());
            osInfoCopy.setMajorVersion(sourceOsInfo.getMajorVersion());
            osInfoCopy.setDisplayName(sourceOsInfo.getDisplayName());

            // 设置OsInfo对象
            copy.setOsInfo(osInfoCopy);
        }

        return copy;
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
                hostInfo.setOsInfoStatus("error");
                hostInfo.setSshConnectStatus("error");
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
            hostInfo.setOsInfoStatus("error");
            hostInfo.setSshConnectStatus("error");
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