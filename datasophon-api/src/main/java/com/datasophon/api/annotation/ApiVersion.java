package com.datasophon.api.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 一体化API版本注解
 * 集成@RestController和@RequestMapping，一个注解搞定所有配置
 * 
 * 使用示例：
 * @ApiVersion(value = "v1", path = "host/install")
 * public class HostInstallController {
 *     // 自动映射到 /ddh/api/v1/host/install/...
 *     // 自动添加@RestController功能
 * }
 * 
 * @ApiVersion(value = "v1", path = "user")
 * public class UserController {
 *     // 自动映射到 /ddh/api/v1/user/...
 * }
 * 
 * @author DataSophon Team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
@RequestMapping
public @interface ApiVersion {
    
    /**
     * API版本号
     * @return 版本号，如 "v1", "v2"
     */
    String value() default "v1";
    
    /**
     * API完整路径（推荐使用）
     * 例如："host/install", "cluster/service", "user/info"
     * 最终路径为：/ddh/api/{version}/{path}
     * @return API路径
     */
    String path() default "";
    
    /**
     * HTTP请求方法（继承自@RequestMapping）
     * @return 支持的HTTP方法
     */
    @AliasFor(annotation = RequestMapping.class, attribute = "method")
    RequestMethod[] method() default {};
    
    /**
     * 请求参数（继承自@RequestMapping）
     * @return 请求参数限制
     */
    @AliasFor(annotation = RequestMapping.class, attribute = "params")
    String[] params() default {};
    
    /**
     * 请求头（继承自@RequestMapping）
     * @return 请求头限制
     */
    @AliasFor(annotation = RequestMapping.class, attribute = "headers")
    String[] headers() default {};
    
    /**
     * 消费的媒体类型（继承自@RequestMapping）
     * @return 消费的Content-Type
     */
    @AliasFor(annotation = RequestMapping.class, attribute = "consumes")
    String[] consumes() default {};
    
    /**
     * 产生的媒体类型（继承自@RequestMapping）
     * @return 产生的Content-Type
     */
    @AliasFor(annotation = RequestMapping.class, attribute = "produces")
    String[] produces() default {};
    
    /**
     * 是否启用版本控制
     * @return true表示启用版本控制，false表示不使用版本号
     */
    boolean versionEnabled() default true;
}