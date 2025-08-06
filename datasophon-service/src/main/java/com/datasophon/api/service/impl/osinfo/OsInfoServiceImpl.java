package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.service.OsInfoService;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.HostInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作系统信息服务实现类（插件化版本）
 * 完全基于插件系统收集主机信息，不再使用线程池和旧代码
 * 
 * @author DataSophon Team
 */
@Service
@Slf4j
public class OsInfoServiceImpl implements OsInfoService {

    // TODO: 暂时注释掉插件管理器，等插件系统编译完成后再启用
    // @Autowired
    // private PluginManager pluginManager;
    
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
        
        // TODO: 使用hostname检查插件
        log.warn("插件系统暂未完全集成，跳过主机名收集");
    }
    
    @Override
    public void collectOsBasicInfo(HostInfo hostInfo) {
        log.info("收集操作系统基础信息: {}", hostInfo.getIp());
        
        // TODO: 使用操作系统检查插件
        log.warn("插件系统暂未完全集成，跳过操作系统基础信息收集");
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
}