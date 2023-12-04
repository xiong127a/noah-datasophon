package com.datasophon.api.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 访问日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * SpEL 模板表达式
     * 当前HandlerMethod的所有形参都会被放入到SpEL的Context，名称就是方法参数名称。
     */
    String expression();
}
