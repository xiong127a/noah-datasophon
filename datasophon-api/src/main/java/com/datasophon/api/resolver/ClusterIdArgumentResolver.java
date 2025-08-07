package com.datasophon.api.resolver;

import com.datasophon.api.annotation.ClusterId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 集群ID参数解析器
 * 从请求头中解析集群ID并注入到带有 @ClusterId 注解的参数
 * 
 * 该解析器通过注解的value属性获取请求头名称，不依赖Java编译器的-parameters参数
 * 
 * @author DataSophon Team
 */
public class ClusterIdArgumentResolver implements HandlerMethodArgumentResolver {
    
    private static final Logger log = LoggerFactory.getLogger(ClusterIdArgumentResolver.class);
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ClusterId.class);
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter, 
                                ModelAndViewContainer mavContainer, 
                                NativeWebRequest webRequest, 
                                WebDataBinderFactory binderFactory) throws Exception {
        
        ClusterId annotation = parameter.getParameterAnnotation(ClusterId.class);
        if (annotation == null) {
            throw new IllegalStateException("@ClusterId annotation is missing");
        }
        
        // 从注解中获取请求头名称
        String headerName = annotation.value();
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        
        if (request == null) {
            return getDefaultValue(annotation, parameter);
        }
        
        // 从请求头获取值
        String headerValue = request.getHeader(headerName);
        
        log.debug("从请求头 {} 获取到值: {}", headerName, headerValue);
        
        // 如果请求头中没有值
        if (headerValue == null || headerValue.trim().isEmpty()) {
            if (annotation.required()) {
                throw new IllegalArgumentException("Required cluster ID is missing from request header: " + headerName);
            }
            return getDefaultValue(annotation, parameter);
        }
        
        // 根据参数类型进行转换
        return convertValue(headerValue, parameter.getParameterType(), annotation);
    }
    
    /**
     * 转换值到目标类型
     */
    private Object convertValue(String value, Class<?> targetType, ClusterId annotation) {
        try {
            if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(value);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(value);
            } else if (targetType == String.class) {
                return value;
            } else {
                throw new IllegalArgumentException("Unsupported parameter type for @ClusterId: " + targetType.getName());
            }
        } catch (NumberFormatException e) {
            if (annotation.required()) {
                throw new IllegalArgumentException("Invalid cluster ID format: " + value, e);
            }
            return getDefaultValue(annotation, null);
        }
    }
    
    /**
     * 获取默认值
     */
    private Object getDefaultValue(ClusterId annotation, MethodParameter parameter) {
        String defaultValue = annotation.defaultValue();
        
        // 如果没有默认值且是必需的，已经在上面抛出异常了
        // 这里处理有默认值或非必需的情况
        if (defaultValue.isEmpty()) {
            // 没有默认值，返回类型相关的默认值
            if (parameter != null) {
                Class<?> parameterType = parameter.getParameterType();
                if (parameterType == Integer.class || parameterType == int.class) {
                    return -1;
                } else if (parameterType == Long.class || parameterType == long.class) {
                    return -1L;
                } else if (parameterType == String.class) {
                    return "";
                }
            }
            return null;
        }
        
        // 使用注解中的默认值
        if (parameter != null) {
            return convertValue(defaultValue, parameter.getParameterType(), annotation);
        }
        return defaultValue;
    }
}