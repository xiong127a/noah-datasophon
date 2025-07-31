package com.datasophon.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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
}