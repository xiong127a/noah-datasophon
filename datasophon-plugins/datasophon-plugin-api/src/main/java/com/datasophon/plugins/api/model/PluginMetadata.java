package com.datasophon.plugins.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 插件元数据模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class PluginMetadata {
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 插件名称
     */
    private String name;
    
    /**
     * 插件版本
     */
    private String version;
    
    /**
     * 插件描述
     */
    private String description;
    
    /**
     * 插件作者
     */
    private String author;
    
    /**
     * 插件主页
     */
    private String homepage;
    
    /**
     * 许可证
     */
    private String license;
    
    /**
     * 支持的操作系统
     */
    private Set<String> supportedOs;
    
    /**
     * 依赖的插件
     */
    private List<String> dependencies;
    
    /**
     * 插件标签
     */
    private Set<String> tags;
    
    /**
     * 插件类别
     */
    private String category;
    
    /**
     * 最小Java版本
     */
    private String minJavaVersion;
    
    /**
     * 是否是核心插件
     */
    @Builder.Default
    private boolean corePlugin = false;
    
    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * 配置文件路径
     */
    private String configFile;
    
    /**
     * 插件文档URL
     */
    private String documentationUrl;
    
    /**
     * 检查是否支持指定操作系统
     */
    public boolean supportsOs(String osName) {
        return supportedOs == null || 
               supportedOs.isEmpty() || 
               supportedOs.contains(osName.toLowerCase());
    }
    
    /**
     * 检查是否有标签
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }
}