package com.datasophon.api.config;

import com.datasophon.api.resolver.ClusterIdArgumentResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    
    // 构造函数 - 确保配置类被正确加载
    public ApiVersionConfig() {
        System.out.println("=== ApiVersionConfig: 配置类构造函数被调用 ===");
    }
    
    // 移除循环依赖 - 不再注入RequestMappingHandlerMapping
    
    /**
     * 使用BeanFactoryPostProcessor在Bean定义阶段替换RequestMappingHandlerMapping
     * 这比@EnableWebMvc更安全，不会禁用Spring Boot的其他自动配置
     */
    @Bean
    public static BeanFactoryPostProcessor requestMappingHandlerMappingReplacer() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                System.out.println("=== BeanFactoryPostProcessor: 开始替换RequestMappingHandlerMapping ===");
                
                // 查找所有RequestMappingHandlerMapping的Bean定义
                String[] handlerMappingNames = beanFactory.getBeanNamesForType(RequestMappingHandlerMapping.class, false, false);
                for (String beanName : handlerMappingNames) {
                    if ("requestMappingHandlerMapping".equals(beanName)) {
                        System.out.println("=== 找到目标Bean: " + beanName + " ===");
                        
                        // 如果beanFactory是BeanDefinitionRegistry，替换Bean定义
                        if (beanFactory instanceof BeanDefinitionRegistry) {
                            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
                            
                            // 创建我们自定义的Bean定义
                            RootBeanDefinition customDefinition = new RootBeanDefinition();
                            customDefinition.setBeanClass(VersionedRequestMappingHandlerMapping.class);
                            customDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
                            customDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                            
                            // 移除原有定义并注册新定义
                            registry.removeBeanDefinition(beanName);
                            registry.registerBeanDefinition(beanName, customDefinition);
                            
                            System.out.println("=== 成功替换RequestMappingHandlerMapping为VersionedRequestMappingHandlerMapping ===");
                        }
                        break;
                    }
                }
            }
        };
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