package com.datasophon.api.interceptor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.api.enums.Status;
import com.datasophon.api.exceptions.BusinessException;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.service.OperationLogService;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.model.OperationLogProp;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.util.Lists;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 操作日志切面 注解
 */
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    Map<String, OperationLogProp> operationLogPropMap;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    OperationLogService operationLogService;

    @PostConstruct
    public void initialize() {
        //读取操作日志元数据配置，
        String tempFileName = "templates/operation-log.json";
        String operationLogString = FileUtil.readString(tempFileName, StandardCharsets.UTF_8);
        List<OperationLogProp> operationLogProps = JSONArray.parseArray(operationLogString, OperationLogProp.class);
        operationLogPropMap = operationLogProps.stream().collect(Collectors.toMap(v1 -> contextPath + v1.getUrl(), v2 -> v2));
    }


    public boolean apiLogAutoEnable = true;

    /**
     * controller  定义的所有请求为切入点
     */
    @Pointcut("execution(* *..*Controller.*(..))")
    public void apiLogAuto() {

    }

    /**
     * 环绕
     * controller
     */
    @Around("apiLogAuto()")
    public Object doAroundApiLogAuto(ProceedingJoinPoint joinPoint) throws Throwable {
        if (apiLogAutoEnable) {
            //判断模块
            HttpServletRequest request = getRequest();
            String requestURI = request.getRequestURI();
            String moduleUri = getModuleUri(requestURI);
            // 只有在operation-log.json 中配置的模块才会记录
            if (Objects.nonNull(operationLogPropMap.get(moduleUri)) && !requestURI.endsWith("/list") && !requestURI.endsWith("List")) {
                return around(joinPoint, request, requestURI);
            } else {
                return joinPoint.proceed();
            }
        }
        return joinPoint.proceed();
    }

    private Object around(ProceedingJoinPoint joinPoint, HttpServletRequest request, String requestURI) throws Throwable {
        Object object;
        OperationLog op = null;

        try {
            //构建日志对象
            op = OperationLog.builder()
                    .url(requestURI)
                    .ip(request.getRemoteAddr())
                    .startTime(new Date()) // 设置开始时间
                    .build();


            //设置属性
            setProp(joinPoint, request, requestURI, op);

            //继续执行
            object = joinPoint.proceed();

            //记录返回状态
            if (object != null && Result.class.getName().equals(object.getClass().getName())) {
                Result rel = (Result) object;
                op.setReturnCode(rel.getCode());
            }

        } catch (Throwable throwable) {
            if (Objects.nonNull(op)) {
                if (throwable.getClass().equals(BusinessException.class)) {
                    op.setReturnMsg(throwable.getMessage());
                    op.setReturnCode(-1);
                }
                if (throwable.getClass().equals(ServiceException.class)) {
                    ServiceException s = ((ServiceException) throwable);
                    op.setReturnMsg(s.getMessage());
                    op.setReturnCode(s.getCode());
                } else {
                    op.setReturnMsg(throwable.getMessage());
                    op.setReturnCode(Status.INTERNAL_SERVER_ERROR_ARGS.getCode());
                }
            }
            log.warn(" auto log error :{}", throwable.getMessage());
            throw throwable;
        } finally {
            if (Objects.nonNull(op)) {
                //设置结束时间
                op.setEndTime(new Date());
            }
            //将该对象insert到数据库中，这里使用log打印该对象数据
            log.info("api-log :{}", JSONObject.toJSONString(op));
            operationLogService.save(op);
        }
        return object;
    }

    private void setProp(ProceedingJoinPoint joinPoint, HttpServletRequest request, String requestURI, OperationLog op) {
        try {
            //设置通用参数
            setCommonProp(request, op);

            //方法参数
            setParams(joinPoint, op);

            //设置操作类型
            setOperationType(joinPoint, op, requestURI);

        } catch (Exception e) {
            //日志报错 不能影响业务流程，这个只做 提示。
            log.warn("log prop  set fail :{}", e.getMessage());
        }
    }

    //设置通用参数
    private static void setCommonProp(HttpServletRequest request, OperationLog op) {
        //操作用户
        String username = Objects.isNull(SecurityUtils.getAuthUser()) ? request.getParameter("username") : SecurityUtils.getAuthUser().getUsername();
        op.setOperateUser(username);

        //设置集群id
        String clusterId = request.getParameter("clusterId");
        if (StrUtil.isNotEmpty(clusterId)) {
            op.setClusterId(Integer.parseInt(clusterId));
        }

        //设置hostIds
        String hostIds = request.getParameter("hostIds");
        op.setHostIds(hostIds);
    }


    /**
     * 解析和设置请求参数
     *
     * @param point
     * @param op
     */
    private void setParams(ProceedingJoinPoint point, OperationLog op) {
        Object[] args = point.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            String[] parameterNames = ((MethodSignature) point.getSignature()).getParameterNames();
            for (int i = 0; i < parameterNames.length; i++) {
                Object arg = args[i];
                // 过滤不能转换成JSON的参数
                if ((arg instanceof ServletRequest) || (arg instanceof ServletResponse)) {
                    continue;
                } else if ((arg instanceof MultipartFile)) {
                    arg = arg.toString();
                }
                params.put(parameterNames[i], arg);
            }
            //设置请求参数
            op.setParam(JSONObject.toJSONString(params));
            op.setParamMap(params);//暂存数据
        } catch (Exception e) {
            log.error("接口出入参日志打印切面处理请求参数异常", e);
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert attributes != null;
        return attributes.getRequest();
    }

    private String getModuleUri(String requestURI) {
        //去掉上下文
        String moduleUri = null;

        //去掉结尾
        if (requestURI.contains("/")) {
            moduleUri = requestURI.substring(0, requestURI.lastIndexOf("/"));
        }
        //不全开头
        assert moduleUri != null;
        if (!moduleUri.startsWith("/")) {
            moduleUri = "/" + moduleUri;
        }
        return moduleUri;
    }

    /**
     * 设置业务日志
     */
    private void setOperationType(ProceedingJoinPoint joinPoint, OperationLog op, String requestURI) {

        //模块URI ，匹配模块访问路径匹配
        String moduleUri = getModuleUri(requestURI);

        //设置操作模块
        OperationLogProp operationLogProp = Optional.ofNullable(operationLogPropMap.get(moduleUri)).orElse(OperationLogProp.builder().build());
        op.setOperationModule(operationLogProp.getOperationModule());

        String substring = requestURI.substring(requestURI.lastIndexOf("/") + 1);

        //设置操作类型
        if (Objects.nonNull(operationLogProp.getOperationType())) {
            String operationType = operationLogProp.getOperationType().get(substring);

            parserAndSetOperationType(op, operationType);
        }

        //设置默认操作类型
        if (StringUtils.isEmpty(op.getOperationType())) {
            setCommonOperationType(joinPoint, op, substring);
        }
    }

    /**
     * 解析并设置操作类型取值
     *
     * @param op
     * @param operationType
     * @return
     */
    private void parserAndSetOperationType(OperationLog op, String operationType) {
        if (StrUtil.isNotEmpty(operationType) && operationType.contains("${") && MapUtil.isNotEmpty(op.getParamMap())) {
            Map<String, String> collect = null;
            String key = null;
            try {
                //如果有表达式，那么从参数中获取
                String expression = operationType.replace("${", "").replace("}", "");
                String[] split = expression.split(":");
                String paramKey = split[0];
                collect = Arrays.stream(split[1].split(";")).collect(Collectors.toMap(v1 -> v1.split("=")[0], v2 -> v2.split("=")[1]));
                key = op.getParamMap().get(paramKey).toString();
            } catch (Exception e) {
                e.printStackTrace();
                log.warn("log parser error  ");
            }
            assert collect != null;
            op.setOperationType(collect.get(key));
        } else {
            op.setOperationType(operationType);
        }
    }

    /**
     * 通用操作类型
     */
    private static void setCommonOperationType(ProceedingJoinPoint joinPoint, OperationLog op, String subRequestURI) {
        //设置操作类型
        if (subRequestURI.endsWith("save") || subRequestURI.endsWith("create")) {
            op.setOperationType("添加");
        } else if (subRequestURI.equals("update")) {
            op.setOperationType("修改");
        } else if (subRequestURI.equals("delete")) {
            op.setOperationType("删除");


            getDeleteData(joinPoint, op);

        } else if (subRequestURI.equals("list")) {
            op.setOperationType("查询");
        } else {
            op.setOperationType(subRequestURI);
        }
    }

    private static void getDeleteData(ProceedingJoinPoint joinPoint, OperationLog op) {
        //如果是删除操作，查询出原来的数据，并记录
        String key = op.getParamMap().keySet().stream().filter(v ->
                        Objects.nonNull(op.getParamMap().get(v))
                                && (v.endsWith("id") || v.endsWith("Id") || v.endsWith("Ids") || v.endsWith("ids")))
                .findFirst().orElse(null);

        if (StrUtil.isEmpty(key)) {
            return;
        }

        //获取当前controller
        String controller = joinPoint.getTarget().getClass().getSimpleName().replace("Controller", "");

        //获取调用的service
        Class<?> service = Arrays.stream(joinPoint.getTarget().getClass().getDeclaredFields())
                .map(Field::getType)
                .filter(v -> v.getSimpleName().replace("Service", "").equals(controller))
                .findFirst().orElse(null);

        //执行service方法
        if (null != service) {

            List<Integer> ids;
            if (op.getParamMap().get(key).getClass().isArray()) {
                ids = Arrays.asList((Integer[]) op.getParamMap().get(key));
            } else {
                String idsString = op.getParamMap().get(key).toString();
                ids = Arrays.stream(idsString.split(",")).map(Integer::parseInt).collect(Collectors.toList());
            }

            try {
                Method findMethod = service.getMethod("listByIds", Collection.class);
                Object bean = SpringTool.getApplicationContext().getBean(service);
                Object oldObj = findMethod.invoke(bean, ids);

                log.info(JSONObject.toJSONString(oldObj));
                op.setParam(JSONObject.toJSONString(oldObj));
            } catch (Exception e) {
                log.warn("delete log invoke listByIds method to get delete object error");
            }
        }
    }

    private static Object getObjectById(Class<?> target, List<Integer> ids) {
        Object oldObj = null;
        Method findMethod = null;
        try {
            findMethod = target.getMethod("listByIds", Collection.class);
            Object bean = SpringTool.getApplicationContext().getBean(target);
            oldObj = findMethod.invoke(bean, ids);

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return oldObj;
    }

}
