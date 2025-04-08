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
        /**
         * 最大允许时间偏差(秒)
         */
        private int maxTimeDiff = 120;

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
}