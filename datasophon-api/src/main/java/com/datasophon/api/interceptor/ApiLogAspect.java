package com.datasophon.api.interceptor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.common.model.OperationLogProp;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 操作日志切面 注解
 */
@Aspect
@Component
@Slf4j
public class ApiLogAspect {


    Map<String, OperationLogProp> operationLogPropMap;


    @PostConstruct
    public void initialize() {

        //读取操作日志元数据配置，
        String tempFileName = "templates/operation-log.json";
        String operationLogString = FileUtil.readString(tempFileName, StandardCharsets.UTF_8);
        List<OperationLogProp> operationLogProps = JSONArray.parseArray(operationLogString, OperationLogProp.class);
        operationLogPropMap = operationLogProps.stream().collect(Collectors.toMap(OperationLogProp::getUrl, v2 -> v2));
    }


//    @Value("${common-log.api-log.enable}")
    public boolean apiLogAutoEnable = false;

    /**
     * 以 controller 包下定义的所有请求为切入点
     * execution(public * com.norintech.api..*.*(..)) && @annotation(com.norintech.log.annotation.OperateLog)
     */

    /**
     * controller  定义的所有请求为切入点
     */
    @Pointcut("execution(* *..*Controller.*(..))")
    public void apiLogAuto() {

    }


    /**
     * 环绕
     * controller
     *
     * @return
     */
    @Around("apiLogAuto()")
    public Object doAroundApiLogAuto(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!apiLogAutoEnable) {
            return joinPoint.proceed();
        }
        return around(joinPoint);
    }

    private Object around(ProceedingJoinPoint joinPoint) {
        Object object = null;
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            assert attributes != null;
            HttpServletRequest request = attributes.getRequest();

            //操作用户
            String username = SecurityUtils.getAuthUser().getUsername();

            //构建日志对象
            OperationLog op = OperationLog.builder()
                    .url(request.getRequestURL().toString())
                    .ip(request.getRemoteAddr())
                    .startTime(DateUtil.now()) // 设置开始时间
                    .paramAndValue(null == joinPoint.getArgs() ? null : JSONObject.toJSONString(joinPoint.getArgs()))
                    .operateUser(username)
                    .build();

            setOperationType(op, request);
            //记录时间
            long startTime = System.currentTimeMillis();
            object = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            op.setCostTime(endTime - startTime);


            //记录返回状态
            if (object != null && Result.class.getName().equals(object.getClass().getName())) {
                Result rel = (Result) object;
                op.setReturnCode(rel.getCode());
            }

            //将该对象insert到数据库中，这里使用log打印该对象数据
            log.info("api-log :{}", JSONObject.toJSONString(op));

            //设置结束时间
            op.setEndTime(DateUtil.now());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            log.warn(" auto log error :{}", throwable.getMessage());
        }
        return object;
    }

    /**
     * 设置业务日志
     *
     * @param op
     * @param request
     */
    private void setOperationType(OperationLog op, HttpServletRequest request) {
        String requestURI = request.getRequestURI();

        //模块URI ，匹配模块访问路径匹配
        String moduleUri = requestURI.substring(0, requestURI.lastIndexOf("/"));

        //设置操作模块
        OperationLogProp operationLogProp = Optional.ofNullable(operationLogPropMap.get(moduleUri)).orElse(OperationLogProp.builder().build());
        op.setOperationModule(operationLogProp.getOperationModule());

        //设置操作类型
        if (Objects.nonNull(operationLogProp.getOperationType())) {
            String substring = requestURI.substring(requestURI.lastIndexOf("/"));
            String operationType = operationLogProp.getOperationType().get(substring);
            op.setOperationType(operationType);
        }

        //设置默认操作类型
        if (StringUtils.isEmpty(op.getOperationType())){
            setCommonOperationType(op, requestURI);
        }
    }

    /**
     * 通用操作类型
     *
     * @param op
     * @param requestURI
     */
    private static void setCommonOperationType(OperationLog op, String requestURI) {
        //设置操作类型
        if (requestURI.endsWith("save")) {
            op.setOperationType("添加");
        } else if (requestURI.equals("update")) {
            op.setOperationType("修改");
        } else if (requestURI.equals("delete")) {
            op.setOperationType("删除");
        } else {
            op.setOperationType("其他");
        }
    }
}
