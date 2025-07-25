package com.datasophon.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求头配置类
 * 允许通过application.yml配置需要处理的请求头
 */
@Configuration
@ConfigurationProperties(prefix = "app.headers")
public class HeaderConfig {

    /**
     * 需要全局处理的请求头名称列表
     */
    private List<String> names = new ArrayList<>(List.of(
            "Authorization",
            "X-User-Id",
            "X-Tenant-Id",
            "X-Client-Version"));

    /**
     * 获取需要处理的请求头列表
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * 设置需要处理的请求头列表
     */
    public void setNames(List<String> names) {
        this.names = names;
    }
}