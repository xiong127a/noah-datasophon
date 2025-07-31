package com.datasophon.api.config;

import com.datasophon.api.annotation.ApiVersion;

import jakarta.annotation.PostConstruct;
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
    
    // 构造函数 - 确保类被正确实例化
    public VersionedRequestMappingHandlerMapping() {
        System.out.println("=== VersionedRequestMappingHandlerMapping: 构造函数被调用 ===");
    }
    
    @PostConstruct
    public void init() {
        System.out.println("=== VersionedRequestMappingHandlerMapping: Bean初始化完成，API前缀: " + API_PREFIX + " ===");
    }
    
    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
        
        System.out.println("=== VersionedRequestMappingHandlerMapping: 处理方法 " + 
                         handlerType.getSimpleName() + "." + method.getName() + " ===");
        
        if (info != null) {
            System.out.println("=== 原始路径信息: " + info.getPatternsCondition() + " ===");
            
            ApiVersion apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
            if (apiVersion != null) {
                // 创建版本化的路径前缀
                String versionPrefix = buildVersionPrefix(apiVersion);
                System.out.println("=== VersionedRequestMappingHandlerMapping: 为类 " + handlerType.getSimpleName() + 
                                 " 的方法 " + method.getName() + " 生成前缀: " + versionPrefix + " ===");
                info = createVersionedRequestMappingInfo(info, versionPrefix);
                System.out.println("=== VersionedRequestMappingHandlerMapping: 最终映射信息: " + info + " ===");
                System.out.println("=== 最终路径模式: " + info.getPatternsCondition() + " ===");
            } else {
                System.out.println("=== 类 " + handlerType.getSimpleName() + " 没有@ApiVersion注解 ===");
            }
        } else {
            System.out.println("=== 方法 " + method.getName() + " 没有RequestMapping信息 ===");
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
        
        if (!apiVersion.path().isEmpty()) {
            prefix.append("/").append(apiVersion.path());
        }
        
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