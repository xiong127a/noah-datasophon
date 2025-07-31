package com.datasophon.api.config;

import com.datasophon.api.annotation.ApiVersion;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
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
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
        
        if (info != null) {
            ApiVersion apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
            if (apiVersion != null && apiVersion.enabled()) {
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
        
        if (apiVersion.versionEnabled() && !apiVersion.value().isEmpty()) {
            prefix.append("/").append(apiVersion.value());
        }
        
        if (!apiVersion.path().isEmpty()) {
            prefix.append("/").append(apiVersion.path());
        }
        
        return prefix.toString();
    }
    
    /**
     * 创建版本化的RequestMappingInfo
     */
    private RequestMappingInfo createVersionedRequestMappingInfo(RequestMappingInfo info, String prefix) {
        return RequestMappingInfo.paths(prefix)
                .methods(info.getMethodsCondition().getMethods().toArray(new org.springframework.web.bind.annotation.RequestMethod[0]))
                .params(info.getParamsCondition())
                .headers(info.getHeadersCondition())
                .consumes(info.getConsumesCondition())
                .produces(info.getProducesCondition())
                .mappingName(info.getName())
                .customCondition(info.getCustomCondition())
                .build()
                .combine(info);
    }
}