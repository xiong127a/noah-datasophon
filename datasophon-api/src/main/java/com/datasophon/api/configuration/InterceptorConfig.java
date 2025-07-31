package com.datasophon.api.configuration;

import com.datasophon.api.interceptor.HeaderInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private HeaderInterceptor headerInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册HeaderInterceptor，应用于所有请求
        registry.addInterceptor(headerInterceptor)
                .addPathPatterns("/**");

        // 如果有特定路径需要排除，可以使用:
        // .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**");
    }
}