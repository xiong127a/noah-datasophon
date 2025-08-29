package com.datasophon.plugins.api.model;

import com.datasophon.common.enums.OsType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统信息模型集合
 * 定义各种系统信息的数据结构
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfo {

    // Getters and Setters
    // 基础信息
    private String hostname;
    private OsType osType;
    private String osVersion;
    private String kernelVersion;
    private String cpuArchitecture;
    private int cpuCoreCount;
    private String cpuModelName;
    private long totalMemoryMB;
    private long freeMemoryMB;
    private long totalSwapMB;
    private long freeSwapMB;
    
    // 详细信息
    private CpuInfo cpuInfo;
    private MemoryInfo memoryInfo;
    private List<DiskInfo> diskInfos;
    private OsInfo osInfo;
    private JavaInfo javaInfo;
    private NetworkInfo networkInfo;
    private FileHandleLimitInfo fileHandleLimitInfo;
    private TimeSyncInfo timeSyncInfo;
    private UserGroupInfo userGroupInfo;
    private FirewallInfo firewallInfo;
    private SelinuxInfo selinuxInfo;

    /**
     * CPU信息
     */
    public record CpuInfo(
        int coreCount,          // CPU核心数
        String modelName,       // CPU型号
        double usagePercent     // CPU使用率(%)
    ) {}
    
    /**
     * 内存信息
     */
    public record MemoryInfo(
        long totalMB,           // 总内存(MB)
        long availableMB,       // 可用内存(MB)
        long swapTotalMB,       // 总交换空间(MB)
        long swapFreeMB         // 剩余交换空间(MB)
    ) {}
    
    /**
     * 磁盘信息
     */
    public record DiskInfo(
        long totalGB,           // 总大小(GB)
        long availableGB,       // 可用大小(GB)
        double usagePercent     // 使用率(%)
    ) {}
    
    /**
     * 操作系统信息
     */
    public record OsInfo(
        String osName,           // 操作系统名称
        String osVersion,        // 操作系统版本
        String kernelVersion,    // 内核版本
        String architecture,     // 系统架构
        OsType osType,          // 操作系统类型
        String hostname,         // 主机名
        String hostsFileContent  // hosts文件内容
    ) {}
    
    /**
     * Java环境信息
     */
    public record JavaInfo(
        boolean installed,        // 是否安装Java
        String version,           // Java版本
        String javaHome           // JAVA_HOME路径
    ) {
        public boolean isInstalled() {
            return installed;
        }
        
        public String getVersion() {
            return version;
        }
    }
    
    /**
     * 网络信息
     */
    public record NetworkInfo() {}
    
    /**
     * 文件句柄限制信息
     */
    public record FileHandleLimitInfo(
        int softLimit,           // 软限制
        int hardLimit,           // 硬限制
        int currentUsage,        // 当前使用量
        boolean isAdequate       // 是否足够
    ) {}
    
    /**
     * 时间同步信息
     */
    public record TimeSyncInfo(
        boolean ntpEnabled,           // NTP是否启用
        boolean timeSynced,           // 时间是否同步
        LocalDateTime systemTime,     // 系统时间
        List<String> ntpServers,      // NTP服务器列表
        long timeOffset,              // 时间偏差(毫秒)
        String ntpStatus              // NTP状态描述
    ) {}
    
    /**
     * 用户组信息
     */
    public record UserGroupInfo(
        boolean userExists,          // 用户是否存在
        boolean groupExists,         // 组是否存在
        boolean userInGroup,         // 用户是否在组中
        String userId,               // 用户ID
        String groupId,              // 组ID
        String homeDirectory,        // 用户主目录
        List<String> userGroups      // 用户所属组列表
    ) {}
    
    /**
     * 防火墙信息
     */
    public record FirewallInfo(
        boolean enabled,             // 防火墙是否启用
        String type                  // 防火墙类型(iptables/firewalld/ufw)
    ) {
        public boolean isEnabled() {
            return enabled;
        }
        
        public String getType() {
            return type;
        }
    }
    
    /**
     * SELinux信息
     */
    public record SelinuxInfo(
        boolean enabled,             // SELinux是否启用
        String mode                  // 当前模式(enforcing/permissive/disabled)
    ) {
        public boolean isEnabled() {
            return enabled;
        }
        
        public String getMode() {
            return mode;
        }
    }
}
