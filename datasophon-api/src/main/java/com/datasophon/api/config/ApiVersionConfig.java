package com.datasophon.api.config;

import com.datasophon.api.resolver.ClusterIdArgumentResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
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
    
    // 构造函数 - 确保配置类被正确加载
    public ApiVersionConfig() {
        System.out.println("=== ApiVersionConfig: 配置类构造函数被调用 ===");
    }
    
    // 移除循环依赖 - 不再注入RequestMappingHandlerMapping
    
    /**
     * 注册支持API版本的RequestMappingHandlerMapping
     * 使用@Primary注解覆盖默认的RequestMappingHandlerMapping
     * 已在application.yml中启用allow-bean-definition-overriding
     */
    @Bean(name = "requestMappingHandlerMapping")
    @Primary
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        System.out.println("=== ApiVersionConfig: 开始创建VersionedRequestMappingHandlerMapping Bean ===");
        VersionedRequestMappingHandlerMapping mapping = new VersionedRequestMappingHandlerMapping();
        
        // 立即测试这个Bean
        System.out.println("=== Bean类型验证: " + mapping.getClass().getName() + " ===");
        
        System.out.println("=== ApiVersionConfig: VersionedRequestMappingHandlerMapping 创建完成 ===");
        return mapping;
    }
    
    /**
     * 简单的测试Bean - 验证配置类是否被加载
     */
    @Bean
    public String testConfigBean() {
        System.out.println("=== ApiVersionConfig: testConfigBean 被创建 - 配置类正常工作 ===");
        return "test-config-working";
    }
    
    /**
     * BeanPostProcessor - 监视所有RequestMappingHandlerMapping Bean的创建
     */
    @Bean
    public BeanPostProcessor requestMappingHandlerMappingMonitor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof RequestMappingHandlerMapping) {
                    System.out.println("=== BeanPostProcessor: 发现 RequestMappingHandlerMapping Bean: " + 
                                     beanName + " -> " + bean.getClass().getName() + " ===");
                }
                return bean;
            }
        };
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