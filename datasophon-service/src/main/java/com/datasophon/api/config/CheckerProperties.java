package com.datasophon.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检查项配置类
 * 对应checker-config.yml中的配置项
 */
@Configuration
@ConfigurationProperties(prefix = "datasophon.checker")
@Data
public class CheckerProperties {

    /**
     * 元数据配置
     */
    private MetaConfig meta = new MetaConfig();

    /**
     * CPU检查配置
     */
    private CpuConfig cpu = new CpuConfig();

    /**
     * 内存检查配置
     */
    private MemoryConfig memory = new MemoryConfig();

    /**
     * Java环境检查配置
     */
    private JavaConfig java = new JavaConfig();

    /**
     * 磁盘空间检查配置
     */
    private DiskConfig disk = new DiskConfig();

    /**
     * 文件句柄数配置
     */
    private FileHandleConfig fileHandle = new FileHandleConfig();

    /**
     * 时间同步配置
     */
    private TimeSyncConfig timeSync = new TimeSyncConfig();

    /**
     * 用户和组检查配置
     */
    private UserGroupConfig userGroup = new UserGroupConfig();

    /**
     * SSH免密检查配置
     */
    private SshPasswordlessConfig sshPasswordless = new SshPasswordlessConfig();

    /**
     * 系统信息收集配置
     */
    private SystemInfoConfig systemInfo = new SystemInfoConfig();

    /**
     * 防火墙检查配置
     */
    private FirewallConfig firewall = new FirewallConfig();

    /**
     * SELinux检查配置
     */
    private SelinuxConfig selinux = new SelinuxConfig();

    /**
     * 系统服务检查配置
     */
    private ServicesConfig services = new ServicesConfig();

    /**
     * Hosts文件检查配置
     */
    private HostsFileConfig hostsFile = new HostsFileConfig();
    
    /**
     * 主机名检查配置
     */
    private HostnameConfig hostname = new HostnameConfig();

    /**
     * 主机校验流程控制配置
     */
    private ValidationFlowConfig validationFlow = new ValidationFlowConfig();

    /**
     * 修复功能配置
     */
    private RepairConfig repair = new RepairConfig();

    /**
     * 日志记录配置
     */
    private LoggingConfig logging = new LoggingConfig();

    /**
     * 调度器配置
     */
    private SchedulerConfig scheduler = new SchedulerConfig();

    /**
     * 实时通信配置
     */
    private RealtimeConfig realtime = new RealtimeConfig();

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 通知配置
     */
    private NotificationsConfig notifications = new NotificationsConfig();

    /**
     * 元数据配置类
     */
    @Data
    public static class MetaConfig {
        /**
         * 版本目录，如DDP-1.2.1
         */
        private String versions;

        /**
         * 元数据基础目录
         */
        private String baseDir;
    }

    /**
     * CPU检查配置类
     */
    @Data
    public static class CpuConfig {
        /**
         * 最小核心数要求
         */
        private int minCores = 4;

        /**
         * 建议核心数
         */
        private int recommendedCores = 8;
    }

    /**
     * 内存检查配置类
     */
    @Data
    public static class MemoryConfig {
        /**
         * 最小内存要求(MB)
         */
        private int minMemory = 8192;

        /**
         * 建议内存(MB)
         */
        private int recommendedMemory = 16384;

        /**
         * 交换区最小值(MB)
         */
        private int minSwap = 4096;
    }

    /**
     * Java环境检查配置类
     */
    @Data
    public static class JavaConfig {
        /**
         * 最小版本要求
         */
        private String minVersion = "1.8";

        /**
         * 默认JDK路径
         */
        private String defaultPath = "/usr/local/jdk1.8.0_333";

        /**
         * 是否检查默认路径
         */
        private boolean checkDefaultPath = true;
        
        /**
         * JDK包配置
         */
        private JdkPackagesConfig packages = new JdkPackagesConfig();
    }
    
    /**
     * JDK包配置类
     */
    @Data
    public static class JdkPackagesConfig {
        /**
         * 存储库中JDK子目录（固定为jdk/）
         */
        private String repositorySubDir = "jdk/";
        
        /**
         * 默认JDK版本（用于非高级模式）
         */
        private String defaultVersion = "JDK21";
        
        /**
         * 是否启用高级选择（让用户选择版本和架构）
         */
        private boolean advancedSelectionEnabled = false;
        
        /**
         * 可用JDK版本列表
         */
        private List<JdkVersionConfig> availableVersions = new ArrayList<>();
    }
    
    /**
     * JDK版本配置类
     */
    @Data
    public static class JdkVersionConfig {
        /**
         * 版本标识，如JDK21
         */
        private String version;
        
        /**
         * 显示名称，如"JDK 21"
         */
        private String displayName;
        
        /**
         * 文件名，如jdk-21_linux-x64_bin.tar.gz
         */
        private String filename;
        
        /**
         * 描述信息，如"OpenJDK 21 LTS (推荐)"
         */
        private String description;
        
        /**
         * 根据架构获取文件名
         * ARM架构会在扩展名前添加-arm后缀
         * 
         * @param isArm 是否为ARM架构
         * @return 对应架构的文件名
         */
        public String getFilenameForArch(boolean isArm) {
            if (!isArm) {
                return filename;
            }
            // ARM架构：在扩展名前添加-arm
            int lastDot = filename.lastIndexOf('.');
            if (lastDot > 0) {
                String baseName = filename.substring(0, lastDot);
                String extension = filename.substring(lastDot);
                // 检查是否是 .tar.gz
                if (extension.equals(".gz") && baseName.endsWith(".tar")) {
                    int tarDot = baseName.lastIndexOf('.');
                    String realBase = baseName.substring(0, tarDot);
                    return realBase + "-arm.tar.gz";
                }
                return baseName + "-arm" + extension;
            }
            return filename + "-arm";
        }
    }

    /**
     * 磁盘空间检查配置类
     */
    @Data
    public static class DiskConfig {
        /**
         * 检查目录列表
         */
        private List<DiskDirectoryConfig> checkDirectories = new ArrayList<>();

        /**
         * 全局最小可用空间百分比
         */
        private int minAvailablePercent = 20;
    }

    /**
     * 磁盘目录检查配置
     */
    @Data
    public static class DiskDirectoryConfig {
        /**
         * 目录路径
         */
        private String path;

        /**
         * 最小可用空间(GB)
         */
        private int minAvailableGb;
    }

    /**
     * 文件句柄数配置类
     */
    @Data
    public static class FileHandleConfig {
        /**
         * 最小限制数
         */
        private int minLimit = 65535;
    }

    /**
     * 时间同步配置类
     */
    @Data
    public static class TimeSyncConfig {
        private boolean enabled = true;
        private int priority = 2;
        private int timeoutSeconds = 30;
        
        /**
         * 最大允许时间偏差(秒)
         */
        private int maxTimeDiff = 120;
        
        /**
         * 最大允许时间差（秒）- 用于环境检查
         */
        private int maxTimeDiffSeconds = 1;
        
        /**
         * 同步命令
         */
        private String syncCommand = "date -s";

        /**
         * 推荐的NTP服务器
         */
        private List<String> ntpServers = new ArrayList<>();
    }

    /**
     * 用户和组检查配置类
     */
    @Data
    public static class UserGroupConfig {
        /**
         * 是否自动创建不存在的用户和组
         */
        private boolean autoCreate = true;

        /**
         * 默认用户组映射，当用户不存在时根据此映射创建
         */
        private Map<String, String> defaultGroupMappings = new HashMap<>();
    }

    /**
     * SSH免密检查配置类
     */
    @Data
    public static class SshPasswordlessConfig {
        private boolean enabled = true;
        private int priority = 1;
        private int timeoutSeconds = 30;
        private List<String> publicKeyPaths = List.of("~/.ssh/id_rsa.pub", "~/.ssh/id_ecdsa.pub", "~/.ssh/id_ed25519.pub");
        private List<String> privateKeyPaths = List.of("~/.ssh/id_rsa", "~/.ssh/id_ecdsa", "~/.ssh/id_ed25519");
        
        /**
         * 主公钥文件路径
         */
        private String publicKeyPath = "~/.ssh/id_rsa.pub";
        
        /**
         * 主私钥文件路径
         */
        private String privateKeyPath = "~/.ssh/id_rsa";
        
        /**
         * 自动生成密钥对
         */
        private boolean autoGenerateKey = true;
        
        /**
         * 密钥类型
         */
        private String keyType = "rsa";
        
        /**
         * 密钥位数
         */
        private int keyBits = 2048;
    }

    /**
     * 系统信息收集配置类
     */
    @Data
    public static class SystemInfoConfig {
        private boolean enabled = true;
        private int priority = 2;
        private int timeoutSeconds = 60;
        private CollectItemsConfig collectItems = new CollectItemsConfig();

        @Data
        public static class CollectItemsConfig {
            private boolean osInfo = true;
            private boolean hardwareInfo = true;
            private boolean networkInfo = true;
            private boolean mountInfo = true;
            private boolean processInfo = false;
            private boolean serviceInfo = true;
        }
    }

    /**
     * 防火墙检查配置类
     */
    @Data
    public static class FirewallConfig {
        private boolean enabled = true;
        private int priority = 8;
        private int timeoutSeconds = 30;
        private boolean autoDisable = false;
        private List<String> checkServices = List.of("firewalld", "iptables", "ufw");
    }

    /**
     * SELinux检查配置类
     */
    @Data
    public static class SelinuxConfig {
        private boolean enabled = true;
        private int priority = 9;
        private int timeoutSeconds = 30;
        private boolean autoDisable = false;
        private List<String> allowedModes = List.of("disabled", "permissive");
    }

    /**
     * 系统服务检查配置类
     */
    @Data
    public static class ServicesConfig {
        private boolean enabled = true;
        private int priority = 10;
        private int timeoutSeconds = 45;
        private List<String> requiredServices = List.of("sshd", "chronyd");
        private List<String> blockedServices = List.of("postfix", "sendmail");
    }

    /**
     * Hosts文件检查配置类
     */
    @Data
    public static class HostsFileConfig {
        private boolean enabled = true;
        private int priority = 101;
        private int timeoutSeconds = 60;
        private boolean autoSync = false;
        private boolean backupBeforeModify = true;
        
        /**
         * 管理段开始标记
         */
        private String managedMarkerStart = "# === DataSophon Managed Hosts Start ===";
        
        /**
         * 管理段结束标记
         */
        private String managedMarkerEnd = "# === DataSophon Managed Hosts End ===";
        
        /**
         * 备份文件后缀
         */
        private String backupSuffix = ".datasophon.bak";
        
        /**
         * 需要保留的系统默认条目
         */
        private List<String> preserveEntries = List.of("127.0.0.1", "::1", "localhost");
    }
    
    /**
     * 主机名检查配置类（全局检查）
     */
    @Data
    public static class HostnameConfig {
        private boolean enabled = true;
        private int priority = 100;
        private int timeoutSeconds = 60;
        
        /**
         * 推荐的主机名前缀
         */
        private List<String> recommendedPrefixes = List.of("bigdata", "bdp", "hadoop");
        
        /**
         * 默认前缀
         */
        private String defaultPrefix = "bigdata";
        
        /**
         * 后缀递增格式
         */
        private List<SuffixFormat> suffixFormats = new ArrayList<>();
        
        /**
         * 默认格式索引
         */
        private int defaultFormatIndex = 1;
        
        /**
         * 后缀格式类
         */
        @Data
        public static class SuffixFormat {
            /**
             * 格式名称
             */
            private String name;
            
            /**
             * 格式模式
             */
            private String pattern;
            
            /**
             * 示例
             */
            private String example;
        }
    }

    /**
     * 主机校验流程控制配置类
     */
    @Data
    public static class ValidationFlowConfig {
        private int maxConcurrentHosts = 20;
        private int maxConcurrentItems = 5;
        private int globalTimeoutMinutes = 30;
        private int itemTimeoutSeconds = 300;
        private int maxRetryAttempts = 3;
        private int retryDelaySeconds = 5;
        private boolean respectDependencies = true;
        private boolean failFast = false;
        private boolean allowSkip = true;
        private boolean autoSkipOnFailure = false;
    }

    /**
     * 修复功能配置类
     */
    @Data
    public static class RepairConfig {
        private boolean enabled = true;
        private boolean autoRepair = false;
        private boolean confirmationRequired = true;
        private boolean backupBeforeRepair = true;
        private int maxRepairAttempts = 3;
        private int repairTimeoutSeconds = 180;
        private Map<String, Boolean> supportedItems = Map.of(
            "ssh-passwordless", true,
            "java-env", true,
            "firewall", true,
            "selinux", true,
            "file-handle-limit", true,
            "hosts-file", true,
            "services", true
        );
    }

    /**
     * 日志记录配置类
     */
    @Data
    public static class LoggingConfig {
        private boolean enabled = true;
        private boolean verbose = false;
        private int maxEntriesPerHost = 1000;
        private int retentionHours = 24;
        private Map<String, String> levels = Map.of(
            "ssh-connection", "INFO",
            "ssh-passwordless", "INFO",
            "system-info", "DEBUG",
            "checks", "INFO",
            "repairs", "WARN",
            "errors", "ERROR"
        );
        private OutputConfig output = new OutputConfig();

        @Data
        public static class OutputConfig {
            private boolean console = true;
            private boolean file = false;
            private boolean sse = true;
        }
    }

    /**
     * 调度器配置类
     */
    @Data
    public static class SchedulerConfig {
        private boolean enabled = true;
        private int threads = 10;
        private int pollingIntervalSeconds = 10;
        private int heartbeatIntervalMinutes = 5;
        private TasksConfig tasks = new TasksConfig();

        @Data
        public static class TasksConfig {
            private ValidationTaskConfig validation = new ValidationTaskConfig();
            private RepairTaskConfig repair = new RepairTaskConfig();
            private CleanupTaskConfig cleanup = new CleanupTaskConfig();

            @Data
            public static class ValidationTaskConfig {
                private int maxExecutionTimeMinutes = 60;
                private boolean retryOnFailure = true;
            }

            @Data
            public static class RepairTaskConfig {
                private int maxExecutionTimeMinutes = 30;
                private boolean retryOnFailure = false;
            }

            @Data
            public static class CleanupTaskConfig {
                private int executionTimeMinutes = 30;
            }
        }
    }

    /**
     * 实时通信配置类
     */
    @Data
    public static class RealtimeConfig {
        private SseConfig sse = new SseConfig();
        private PushConfig push = new PushConfig();

        @Data
        public static class SseConfig {
            private boolean enabled = true;
            private int timeoutSeconds = 300;
            private int heartbeatIntervalSeconds = 30;
            private int maxConnectionsPerCluster = 50;
            private int bufferSize = 1000;
        }

        @Data
        public static class PushConfig {
            private boolean statusUpdates = true;
            private boolean logMessages = true;
            private boolean progressUpdates = true;
            private int batchSize = 10;
            private int flushIntervalMs = 100;
        }
    }

    /**
     * 缓存配置类
     */
    @Data
    public static class CacheConfig {
        private boolean enabled = true;
        private ValidationSessionsConfig validationSessions = new ValidationSessionsConfig();
        private SystemInfoCacheConfig systemInfo = new SystemInfoCacheConfig();

        @Data
        public static class ValidationSessionsConfig {
            private int maxSize = 100;
            private int expireAfterWriteMinutes = 60;
            private int expireAfterAccessMinutes = 30;
        }

        @Data
        public static class SystemInfoCacheConfig {
            private int maxSize = 200;
            private int expireAfterWriteMinutes = 180;
        }
    }

    /**
     * 通知配置类
     */
    @Data
    public static class NotificationsConfig {
        private boolean enabled = false;
        private ChannelsConfig channels = new ChannelsConfig();
        private EventsConfig events = new EventsConfig();

        @Data
        public static class ChannelsConfig {
            private boolean email = false;
            private boolean webhook = false;
            private boolean sms = false;
        }

        @Data
        public static class EventsConfig {
            private boolean validationCompleted = true;
            private boolean validationFailed = true;
            private boolean repairCompleted = false;
        }
    }
}