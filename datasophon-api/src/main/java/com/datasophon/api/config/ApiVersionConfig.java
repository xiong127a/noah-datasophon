package com.datasophon.api.config;

import com.datasophon.api.resolver.ClusterIdArgumentResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
@EnableWebMvc
public class ApiVersionConfig implements WebMvcConfigurer {
    
    // 构造函数 - 确保配置类被正确加载
    public ApiVersionConfig() {
        System.out.println("=== ApiVersionConfig: 配置类构造函数被调用 ===");
    }
    
    // 移除循环依赖 - 不再注入RequestMappingHandlerMapping
    
    /**
     * 注册支持API版本的RequestMappingHandlerMapping
     * 使用@EnableWebMvc禁用Spring Boot自动配置，确保我们的自定义映射器被使用
     */
    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        System.out.println("=== ApiVersionConfig: 开始创建VersionedRequestMappingHandlerMapping Bean ===");
        VersionedRequestMappingHandlerMapping mapping = new VersionedRequestMappingHandlerMapping();
        
        // 设置映射优先级 - 确保在其他HandlerMapping之前被使用
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        
        // 立即测试这个Bean
        System.out.println("=== Bean类型验证: " + mapping.getClass().getName() + " ===");
        System.out.println("=== Bean优先级设置: " + mapping.getOrder() + " ===");
        
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
     * 现在主要用于调试目的
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
    
    /**
     * 配置资源处理器
     * 确保API路径不被静态资源处理器拦截
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("=== ApiVersionConfig: 配置资源处理器，确保API路径优先 ===");
        
        // 确保/api/**路径不被当作静态资源处理
        // 这样可以避免API请求被ResourceHttpRequestHandler拦截
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        registry.addResourceHandler("/public/**")
                .addResourceLocations("classpath:/public/");
        
        System.out.println("=== ApiVersionConfig: 静态资源处理配置完成，API路径将优先匹配Controller ===");
    }
}