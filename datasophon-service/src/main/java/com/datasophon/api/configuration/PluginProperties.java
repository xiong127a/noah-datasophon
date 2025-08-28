package com.datasophon.api.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件配置属性类
 * 映射application.yml中的datasophon.plugins配置
 * 
 * @author DataSophon Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "datasophon.plugins")
public class PluginProperties {
    
    /**
     * 插件目录
     */
    private String directory = "plugins";
    
    /**
     * 插件扫描路径列表
     */
    private List<String> scanPaths = new ArrayList<>();
    
    /**
     * 开发模式配置
     */
    private Development development = new Development();
    
    /**
     * 插件加载配置
     */
    private Loading loading = new Loading();
    
    /**
     * SSH连接池配置
     */
    private SshConnectionPool sshConnectionPool = new SshConnectionPool();
    
    /**
     * 开发模式配置
     */
    @Data
    public static class Development {
        
        /**
         * 是否启用开发模式
         */
        private boolean enabled = true;
        
        /**
         * 开发模式下的插件路径
         */
        private List<String> pluginPaths = new ArrayList<>();
    }
    
    /**
     * 插件加载配置
     */
    @Data
    public static class Loading {
        
        /**
         * 是否启用插件功能
         */
        private boolean enabled = true;
        
        /**
         * 是否延迟加载插件
         * true: 启动时不自动加载插件，按需手动加载
         * false: 启动时自动加载所有插件
         */
        private boolean lazy = false;
        
        /**
         * 是否支持热重载
         */
        private boolean hotReload = true;
    }
    
    /**
     * SSH连接池配置
     */
    @Data
    public static class SshConnectionPool {
        
        /**
         * 默认连接池配置
         */
        private PoolConfig defaultConfig = new PoolConfig();
        
        /**
         * 连接池配置
         */
        @Data
        public static class PoolConfig {
            private int maxTotal = 10;
            private int maxIdle = 5;
            private int minIdle = 2;
            private long maxWaitMillis = 30000;
            private boolean testOnBorrow = true;
            private boolean testOnReturn = true;
            private boolean testWhileIdle = true;
            private long timeBetweenEvictionRunsMillis = 30000;
            private long minEvictableIdleTimeMillis = 300000;
        }
    }
    
    /**
     * 判断是否启用延迟加载
     */
    public boolean isLazyLoadingEnabled() {
        return loading.enabled && loading.lazy;
    }
    
    /**
     * 判断插件功能是否启用
     */
    public boolean isPluginEnabled() {
        return loading.enabled;
    }
}