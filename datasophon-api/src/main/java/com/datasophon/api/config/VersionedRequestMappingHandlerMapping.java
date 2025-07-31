package com.datasophon.api.config;

import com.datasophon.api.annotation.ApiVersion;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * 支持API版本的RequestMappingHandlerMapping
 * 自动为带有@ApiVersion注解的Controller添加版本前缀
 * 
 * @author DataSophon Team
 */
public class VersionedRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
    
    private static final String API_PREFIX = "api";
    
    @Override
    public void afterPropertiesSet() {
        // 设置最高优先级，确保在ResourceHttpRequestHandler之前被处理
        setOrder(Ordered.HIGHEST_PRECEDENCE);
        super.afterPropertiesSet();
    }
    
    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
        
        if (info != null) {
            ApiVersion apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
            if (apiVersion != null) {
                // 创建版本化的路径前缀
                String versionPrefix = buildVersionPrefix(apiVersion);
                info = createVersionedRequestMappingInfo(info, versionPrefix);
            }
        }
        
        return info;
    }
    
    /**
     * 构建版本前缀
     */
    private String buildVersionPrefix(ApiVersion apiVersion) {
        StringBuilder prefix = new StringBuilder();
        prefix.append("/").append(API_PREFIX);
        
        if (apiVersion.versionEnabled() && !apiVersion.version().isEmpty()) {
            prefix.append("/").append(apiVersion.version());
        }
        
        // 注意：不在这里添加apiVersion.path()，因为@ApiVersion继承了@RequestMapping
        // path信息已经通过@RequestMapping机制包含在原始的RequestMappingInfo中了
        
        return prefix.toString();
    }
    
    /**
     * 创建版本化的RequestMappingInfo
     */
    private RequestMappingInfo createVersionedRequestMappingInfo(RequestMappingInfo info, String prefix) {
        // 为JDK21和现代Spring Boot版本适配
        RequestMappingInfo prefixInfo = RequestMappingInfo.paths(prefix).build();
        return prefixInfo.combine(info);
    }
}