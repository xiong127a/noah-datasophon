package com.datasophon.api.config;

import com.datasophon.api.resolver.ClusterIdArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

/**
 * Web MVC配置
 * 注册自定义的RequestMappingHandlerMapping以支持API版本化
 * 
 * @author DataSophon Team
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {
    
    /**
     * 注册支持API版本的RequestMappingHandlerMapping
     * 使用@Primary注解覆盖默认的RequestMappingHandlerMapping
     * 已在application.yml中启用allow-bean-definition-overriding
     */
    @Bean
    @Primary
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        return new VersionedRequestMappingHandlerMapping();
    }
    
    /**
     * 添加自定义参数解析器
     * 注册集群ID参数解析器，支持@ClusterId注解
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new ClusterIdArgumentResolver());
    }
}