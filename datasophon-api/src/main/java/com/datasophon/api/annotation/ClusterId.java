package com.datasophon.api.annotation;

import java.lang.annotation.*;

/**
 * 集群ID注解
 * 用于自动注入请求头中的集群ID到Controller方法参数
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @GetMapping("/cluster/info")
 * public Result getClusterInfo(@ClusterId Integer clusterId) {
 *     // clusterId 会自动从请求头 x-cluster-id 中获取
 *     return Result.success(clusterInfo);
 * }
 * }
 * </pre>
 * 
 * @author DataSophon Team
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ClusterId {
    
    /**
     * 当请求头中没有集群ID时的默认值
     * @return 默认集群ID，-1表示无效集群
     */
    int defaultValue() default -1;
    
    /**
     * 是否必需，如果为true且请求头中没有集群ID，会抛出异常
     * @return 是否必需
     */
    boolean required() default true;
}