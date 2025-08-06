package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.service.OsInfoService;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import com.datasophon.common.enums.OsInfoStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作系统信息服务实现类（插件化版本）
 * 完全基于插件系统收集主机信息，使用SSH连接池而非旧的MinaUtils
 * 
 * @author DataSophon Team
 */
@Service
@Slf4j
public class OsInfoServiceImpl implements OsInfoService {

    // TODO: 等插件系统完全集成后启用
    // @Autowired
    // private PluginManager pluginManager;
    
    // TODO: 等插件系统完全集成后启用
    // @Autowired
    // private SshConnectionService sshConnectionService;
    
    @Override
    public void getHostOsInfoAsync(HostInfo hostInfo) {
        log.info("开始异步收集主机操作系统信息: {}", hostInfo.getIp());
        
        // TODO: 实现插件化逻辑
        // 临时实现：调用两个阶段的收集
        collectPhaseOneInfo(hostInfo);
        collectPhaseTwoInfo(hostInfo);
    }
    
    @Override
    public void collectHostnameInfo(HostInfo hostInfo) {
        log.info("收集主机名信息: {}", hostInfo.getIp());
        
        // TODO: 完整插件集成后的实现：
        // try (HostCheckContext context = createHostCheckContext(hostInfo)) {
        //     List<HostCheckerPlugin> hostnameCheckers = pluginManager.getPluginsForCheckType("hostname");
        //     for (HostCheckerPlugin checker : hostnameCheckers) {
        //         if (checker.canExecute(context)) {
        //             CheckResult result = checker.execute(context);
        //             if (result.getStatus() == CheckStatus.SUCCESS) {
        //                 updateHostnameFromResult(hostInfo, result);
        //                 break;
        //             }
        //         }
        //     }
        // } catch (Exception e) {
        //     log.error("收集主机名信息失败: {}", hostInfo.getIp(), e);
        // }
        
        // 临时简单实现
        collectHostnameInfoWithSshPool(hostInfo);
    }
    
    @Override
    public void collectOsBasicInfo(HostInfo hostInfo) {
        log.info("收集操作系统基础信息: {}", hostInfo.getIp());
        
        // TODO: 完整插件集成后的实现：
        // try (HostCheckContext context = createHostCheckContext(hostInfo)) {
        //     List<HostCheckerPlugin> osCheckers = pluginManager.getPluginsForCheckType("os");
        //     for (HostCheckerPlugin checker : osCheckers) {
        //         if (checker.canExecute(context)) {
        //             CheckResult result = checker.execute(context);
        //             if (result.getStatus() == CheckStatus.SUCCESS) {
        //                 updateOsBasicInfoFromResult(hostInfo, result);
        //                 break;
        //             }
        //         }
        //     }
        // } catch (Exception e) {
        //     log.error("收集操作系统基础信息失败: {}", hostInfo.getIp(), e);
        // }
        
        // 临时简单实现
        collectOsBasicInfoWithSshPool(hostInfo);
    }
    
    @Override
    public void collectDnsInfo(HostInfo hostInfo) {
        log.info("收集DNS信息: {}", hostInfo.getIp());
        
        // TODO: 使用DNS检查插件
        log.warn("插件系统暂未完全集成，跳过DNS信息收集");
    }
    
    @Override
    public void collectHostsFileInfo(HostInfo hostInfo) {
        log.info("收集hosts文件信息: {}", hostInfo.getIp());
        
        // TODO: 使用hosts文件检查插件
        log.warn("插件系统暂未完全集成，跳过hosts文件信息收集");
    }
    
    @Override
    public void collectCpuInfo(HostInfo hostInfo) {
        log.info("收集CPU信息: {}", hostInfo.getIp());
        
        // TODO: 使用CPU检查插件
        log.warn("插件系统暂未完全集成，跳过CPU信息收集");
    }
    
    @Override
    public void collectMemoryInfo(HostInfo hostInfo) {
        log.info("收集内存信息: {}", hostInfo.getIp());
        
        // TODO: 使用内存检查插件
        log.warn("插件系统暂未完全集成，跳过内存信息收集");
    }
    
    @Override
    public void collectDiskInfo(HostInfo hostInfo) {
        log.info("收集磁盘信息: {}", hostInfo.getIp());
        
        // TODO: 使用磁盘检查插件
        log.warn("插件系统暂未完全集成，跳过磁盘信息收集");
    }
    
    @Override
    public void collectGpuInfo(HostInfo hostInfo) {
        log.info("收集GPU信息: {}", hostInfo.getIp());
        
        // TODO: 使用GPU检查插件
        log.debug("插件系统暂未完全集成，跳过GPU信息收集");
    }
    
    @Override
    public void collectNetworkInfo(HostInfo hostInfo) {
        log.info("收集网络信息: {}", hostInfo.getIp());
        
        // TODO: 使用网络检查插件
        log.warn("插件系统暂未完全集成，跳过网络信息收集");
    }
    
    @Override
    public void collectSwapInfo(HostInfo hostInfo) {
        log.info("收集交换空间信息: {}", hostInfo.getIp());
        
        // TODO: 使用交换空间检查插件
        log.warn("插件系统暂未完全集成，跳过交换空间信息收集");
            }

            @Override
    public void collectPhaseOneInfo(HostInfo hostInfo) {
        log.info("开始第一阶段信息收集（基础信息）: {}", hostInfo.getIp());
        
        // 收集主机名和基础操作系统信息
        collectHostnameInfo(hostInfo);
        collectOsBasicInfo(hostInfo);
        
        // 更新缓存
        updateHostInfoCache(hostInfo);
        
        log.info("第一阶段信息收集完成: {}", hostInfo.getIp());
    }
    
    @Override
    public void collectPhaseTwoInfo(HostInfo hostInfo) {
        log.info("开始第二阶段信息收集（详细硬件信息）: {}", hostInfo.getIp());
        
        // 收集详细硬件信息
        collectCpuInfo(hostInfo);
        collectMemoryInfo(hostInfo);
        collectDiskInfo(hostInfo);
        collectNetworkInfo(hostInfo);
        collectSwapInfo(hostInfo);
        collectGpuInfo(hostInfo);
        
        // 收集系统配置信息
        collectDnsInfo(hostInfo);
        collectHostsFileInfo(hostInfo);

            // 更新缓存
        updateHostInfoCache(hostInfo);
        
        log.info("第二阶段信息收集完成: {}", hostInfo.getIp());
    }
    
    @Override
    public void updateHostInfoCache(HostInfo hostInfo) {
            if (hostInfo == null) {
                return;
            }
        
        try {
            CacheUtils.putHostInfo(hostInfo.getClusterId(), hostInfo.getIp(), hostInfo);
            log.debug("已更新主机缓存: {}", hostInfo.getIp());
                    } catch (Exception e) {
            log.error("更新主机缓存失败: {}, 原因: {}", hostInfo.getIp(), e.getMessage(), e);
        }
    }
    
    @Override
    public void resetCollectionQueue() {
        log.info("插件化架构无需队列管理，忽略重置队列操作");
    }
    
    @Override
    public void addHostToCollectionQueue(HostInfo hostInfo) {
        log.info("插件化架构无需队列管理，直接开始信息收集: {}", hostInfo.getIp());
        getHostOsInfoAsync(hostInfo);
    }
    
    /**
     * 使用SSH连接池收集主机名信息（临时实现）
     */
    private void collectHostnameInfoWithSshPool(HostInfo hostInfo) {
        // TODO: 这里应该使用插件化的SSH连接池服务
        // 当前先记录日志，等插件系统完全就绪后实现
        log.info("TODO: 使用SSH连接池收集主机名 - {}", hostInfo.getIp());
        
        try {
            // 初始化OsInfo如果不存在
            if (hostInfo.getOsInfo() == null) {
                OsInfo osInfo = OsInfo.builder()
                        .osInfoStatus(OsInfoStatusEnum.COLLECTING)
                        .build();
                // TODO: 等Lombok生成setter方法后设置主机名
                // osInfo.setHostname(hostInfo.getHostname());
                    hostInfo.setOsInfo(osInfo);
                }

            // 设置基本信息
            hostInfo.getOsInfo().setHostnameStatus(OsInfoStatusEnum.SUCCESS);
            
            } catch (Exception e) {
            log.error("使用SSH连接池收集主机名失败: {}", hostInfo.getIp(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setHostnameStatus(OsInfoStatusEnum.ERROR);
            }
            }
        }

        /**
     * 使用SSH连接池收集操作系统基础信息（临时实现）
     */
    private void collectOsBasicInfoWithSshPool(HostInfo hostInfo) {
        // TODO: 这里应该使用插件化的SSH连接池服务
        log.info("TODO: 使用SSH连接池收集操作系统基础信息 - {}", hostInfo.getIp());
        
        try {
            // 初始化OsInfo如果不存在
            if (hostInfo.getOsInfo() == null) {
                OsInfo osInfo = OsInfo.builder()
                        .distribution("Unknown")
                        .version("Unknown") 
                        .architecture("Unknown")
                        .osInfoStatus(OsInfoStatusEnum.COLLECTING)
                        .build();
                    hostInfo.setOsInfo(osInfo);
                }

            // 设置基本信息收集状态
            hostInfo.getOsInfo().setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
            
            } catch (Exception e) {
            log.error("使用SSH连接池收集操作系统基础信息失败: {}", hostInfo.getIp(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setOsInfoStatus(OsInfoStatusEnum.ERROR);
            }
        }
    }
    
    // TODO: 等插件系统完全集成后，添加以下方法：
    
    /*
     * 创建主机检查上下文（使用SSH连接池）
     * 
    private HostCheckContext createHostCheckContext(HostInfo hostInfo) throws Exception {
        ClientSession session = null;
        try {
            // 使用插件化的SSH连接服务
            session = sshConnectionService.borrowConnection(
                HostCheckContext.builder()
                    .hostInfo(hostInfo)
                    .build()
            );
            
            return HostCheckContext.builder()
                    .hostInfo(hostInfo)
                    .sshSession(session)
                    .sharedData(new java.util.concurrent.ConcurrentHashMap<>())
                    .build();
                    
                    } catch (Exception e) {
                            if (session != null) {
                try {
                    sshConnectionService.returnConnection(
                        HostCheckContext.builder().hostInfo(hostInfo).build(), 
                        session
                    );
                } catch (Exception closeEx) {
                    log.warn("归还SSH连接失败", closeEx);
                }
            }
            throw new RuntimeException("创建主机检查上下文失败: " + e.getMessage(), e);
            }
        }

        /**
     * 从检查结果更新主机名信息
     * 
    private void updateHostnameFromResult(HostInfo hostInfo, CheckResult result) {
        if (result.getData() != null && result.getData().containsKey("hostname")) {
            String hostname = (String) result.getData().get("hostname");
                hostInfo.setHostname(hostname);
                if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setHostname(hostname);
                hostInfo.getOsInfo().setHostnameStatus(OsInfoStatusEnum.SUCCESS);
            }
            log.debug("更新主机名: {} -> {}", hostInfo.getIp(), hostname);
        }
    }
    */
}