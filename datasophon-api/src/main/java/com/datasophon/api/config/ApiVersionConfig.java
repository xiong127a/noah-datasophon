package com.datasophon.api.config;

import com.datasophon.api.resolver.ClusterIdArgumentResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
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
public class ApiVersionConfig implements WebMvcConfigurer {
    
    /**
     * 使用BeanFactoryPostProcessor在Bean定义阶段替换RequestMappingHandlerMapping
     * 这比@EnableWebMvc更安全，不会禁用Spring Boot的其他自动配置
     */
    @Bean
    public static BeanFactoryPostProcessor requestMappingHandlerMappingReplacer() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                // 查找所有RequestMappingHandlerMapping的Bean定义
                String[] handlerMappingNames = beanFactory.getBeanNamesForType(RequestMappingHandlerMapping.class, false, false);
                for (String beanName : handlerMappingNames) {
                    if ("requestMappingHandlerMapping".equals(beanName)) {
                        // 如果beanFactory是BeanDefinitionRegistry，替换Bean定义
                        if (beanFactory instanceof BeanDefinitionRegistry) {
                            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
                            
                            // 创建我们自定义的Bean定义
                            RootBeanDefinition customDefinition = new RootBeanDefinition();
                            customDefinition.setBeanClass(VersionedRequestMappingHandlerMapping.class);
                            customDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
                            customDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                            
                            // 设置最高优先级，确保在ResourceHttpRequestHandler之前被处理
                            customDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
                            
                            // 移除原有定义并注册新定义
                            registry.removeBeanDefinition(beanName);
                            registry.registerBeanDefinition(beanName, customDefinition);
                            
                            System.out.println("=== 已替换RequestMappingHandlerMapping为VersionedRequestMappingHandlerMapping ===");
                        }
                        break;
                    }
                }
            }
        };
    }
    

    /**
     * 添加自定义参数解析器
     * 注册集群ID参数解析器，支持@ClusterId注解
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(0, new ClusterIdArgumentResolver());
        System.out.println("=== ClusterIdArgumentResolver已注册 ===");
    }

    
    /**
     * 配置资源处理器
     * 明确排除/api/**路径被ResourceHttpRequestHandler处理
     * 确保API路径优先被Controller处理而不是静态资源处理器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 明确配置静态资源路径，避免与API路径冲突
        // 静态资源只处理明确指定的路径，不处理/api/**
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true);
        
        registry.addResourceHandler("/public/**")
                .addResourceLocations("classpath:/public/")
                .resourceChain(true);
                
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(true);
        
        // 明确配置根路径下的静态文件，但排除/api/**
        registry.addResourceHandler("/*.html", "/*.ico", "/*.css", "/*.js")
                .addResourceLocations("classpath:/static/", "classpath:/public/")
                .resourceChain(true);
        
        // 关键：设置资源处理器的优先级低于API HandlerMapping
        // 这样API请求会优先被Controller处理
        registry.setOrder(Integer.MAX_VALUE);
    }
}