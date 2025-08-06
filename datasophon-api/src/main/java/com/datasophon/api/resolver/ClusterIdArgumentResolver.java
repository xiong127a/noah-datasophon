package com.datasophon.api.resolver;

import com.datasophon.api.annotation.ClusterId;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 集群ID参数解析器
 * 从请求头 x-cluster-id 中解析集群ID并注入到带有 @ClusterId 注解的参数
 * 
 * @author DataSophon Team
 */
public class ClusterIdArgumentResolver implements HandlerMethodArgumentResolver {
    
    private static final String CLUSTER_ID_HEADER = "x-cluster-id";
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ClusterId.class);
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter, 
                                ModelAndViewContainer mavContainer, 
                                NativeWebRequest webRequest, 
                                WebDataBinderFactory binderFactory) throws Exception {
        
        ClusterId clusterIdAnnotation = parameter.getParameterAnnotation(ClusterId.class);
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        
        if (request == null) {
            return getDefaultValue(clusterIdAnnotation, parameter);
        }
        
        String clusterIdStr = request.getHeader(CLUSTER_ID_HEADER);
        
        // 如果请求头中没有集群ID
        if (clusterIdStr == null || clusterIdStr.trim().isEmpty()) {
            if (clusterIdAnnotation.required()) {
                throw new IllegalArgumentException("Required cluster ID is missing from request header: " + CLUSTER_ID_HEADER);
            }
            return getDefaultValue(clusterIdAnnotation, parameter);
        }
        
        // 解析集群ID
        try {
            Class<?> parameterType = parameter.getParameterType();
            
            if (parameterType == Integer.class || parameterType == int.class) {
                return Integer.parseInt(clusterIdStr);
            } else if (parameterType == Long.class || parameterType == long.class) {
                return Long.parseLong(clusterIdStr);
            } else if (parameterType == String.class) {
                return clusterIdStr;
            } else {
                throw new IllegalArgumentException("Unsupported parameter type for @ClusterId: " + parameterType.getName());
            }
        } catch (NumberFormatException e) {
            if (clusterIdAnnotation.required()) {
                throw new IllegalArgumentException("Invalid cluster ID format: " + clusterIdStr, e);
            }
            return getDefaultValue(clusterIdAnnotation, parameter);
        }
    }
    
    /**
     * 获取默认值
     */
    private Object getDefaultValue(ClusterId annotation, MethodParameter parameter) {
        Class<?> parameterType = parameter.getParameterType();
        
        if (parameterType == Integer.class || parameterType == int.class) {
            return annotation.defaultValue();
        } else if (parameterType == Long.class || parameterType == long.class) {
            return (long) annotation.defaultValue();
        } else if (parameterType == String.class) {
            return String.valueOf(annotation.defaultValue());
        }
        
        return null;
    }
}