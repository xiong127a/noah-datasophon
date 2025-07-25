package com.datasophon.api.interceptor;

import com.datasophon.api.common.HeaderContextHolder;
import com.datasophon.api.configuration.HeaderConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求头拦截器
 * 从请求中提取指定的请求头并保存到ThreadLocal中
 */
@Component
public class HeaderInterceptor implements HandlerInterceptor {

    @Autowired
    private HeaderConfig headerConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 提取所有关注的请求头
        headerConfig.getNames().forEach(headerName -> {
            String value = request.getHeader(headerName);
            if (value != null && !value.isEmpty()) {
                HeaderContextHolder.setHeader(headerName, value);
            }
        });

        return true; // 继续处理请求
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        // 清理ThreadLocal，避免内存泄漏
        HeaderContextHolder.clear();
    }
}