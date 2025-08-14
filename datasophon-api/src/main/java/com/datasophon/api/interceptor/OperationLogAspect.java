package com.datasophon.api.interceptor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.common.enums.Status;
import com.datasophon.api.exceptions.BusinessException;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.service.OperationLogService;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.common.model.OperationLogProp;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.OperationLogEntity;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
public class OperationLogAspect {

    Map<String, String> operationLogUrlMap;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private OperationLogService operationLogService;




    @PostConstruct
    public void initialize() {
        //读取操作日志元数据配置
        try {
            File file = ResourceUtils.getFile("classpath:templates/operation-log.json");
            String operationLogString = FileUtil.readString(file, StandardCharsets.UTF_8);
            List<OperationLogProp> operationLogProps = JSONArray.parseArray(operationLogString, OperationLogProp.class);

            operationLogUrlMap = new HashMap<>();
            //全部拼成url和 type 的map
            for (OperationLogProp operationLogProp : operationLogProps) {
                String url = contextPath + operationLogProp.getUrl();
                String operationModule = operationLogProp.getOperationModule();
                operationLogUrlMap.put(url, operationModule);
                //添加通用url
                addCommonUrl(url, operationLogUrlMap);
                //拼接子目录
                Map<String, String> operationType = operationLogProp.getOperationType();
                if (MapUtil.isNotEmpty(operationType)) {
                    for (String key : operationType.keySet()) {
                        operationLogUrlMap.put(url + "/" + key, operationType.get(key));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            log.warn("log config read error");
        }

    }

    /**
     * 添加基本url
     *
     */
    private void addCommonUrl(String url, Map<String, String> operationLogUrlMap) {
        operationLogUrlMap.put(url + "/save", "添加");
        operationLogUrlMap.put(url + "/create", "添加");
        operationLogUrlMap.put(url + "/update", "修改");
        operationLogUrlMap.put(url + "/delete", "删除");
    }


    public final boolean apiLogAutoEnable = true;

    @Pointcut("execution(* *..*Controller.*(..))")
    public void normalPointcutWeb() {
    }

    @Pointcut("execution(* com.datasophon.api.controller.ClusterServiceRoleInstanceController.list(..)) " +
            "|| execution(* com.datasophon.api.controller.ClusterServiceInstanceController.list(..)) " +
            "|| execution(* com.datasophon.api.controller.ClusterServiceRoleInstanceWebuisController.getWebUis(..))")
    public void excludePointcutWeb() {
    }

    @Pointcut("normalPointcutWeb() && !excludePointcutWeb()")
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
            // 只有在operation-log.json 中配置的模块才会记录
            if (Objects.nonNull(operationLogUrlMap.get(requestURI))) {
                return around(joinPoint, request, requestURI);
            } else {
                return joinPoint.proceed();
            }
        }
        return joinPoint.proceed();
    }

    private Object around(ProceedingJoinPoint joinPoint, HttpServletRequest request, String requestURI) throws Throwable {
        Object object;
        OperationLogEntity op = null;

        try {
            //构建日志对象
            op = OperationLogEntity.builder()
                    .url(requestURI)
                    .ip(request.getRemoteAddr())
                    .startTime(LocalDateTime.now()) // 设置开始时间
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
                op.setEndTime(LocalDateTime.now());
            }
            //将该对象insert到数据库中，这里使用log打印该对象数据
            log.debug("api-log :{}", JSONObject.toJSONString(op));
            operationLogService.save(op);
        }
        return object;
    }

    private void setProp(ProceedingJoinPoint joinPoint, HttpServletRequest request, String requestURI, OperationLogEntity op) {
        try {

            //方法参数
            setParams(joinPoint, request, op);

            //设置操作类型
            setOperationType(op, requestURI);

        } catch (Exception e) {
            //日志报错 不能影响业务流程，这个只做 提示。
            log.warn("log prop  set fail :{}", e.getMessage());
        }
    }


    /**
     * 解析和设置请求参数
     *
     */
    private void setParams(ProceedingJoinPoint point, HttpServletRequest request, OperationLogEntity op) {
        //操作用户
        String username = Objects.isNull(SecurityUtils.getAuthUser()) ? request.getParameter("username") : SecurityUtils.getAuthUser().getUsername();
        op.setOperateUser(username);

        //从header中获取集群 id
        Long clusterId = (long) request.getIntHeader("X-Cluster-Id");
        if (ObjUtil.isNotNull(clusterId)) {
            op.setClusterId(clusterId);
        }

        //从request中查找
        JSONObject requestParam = new JSONObject();
        request.getParameterMap().forEach((key, value) -> requestParam.put(key, String.join(" ", value)));
        clusterParam(op, requestParam);

        Object[] args = point.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            int i=0;
            for (Map.Entry<String, Object> stringObjectEntry : requestParam.entrySet()) {
                Object arg = args[i];
                i++;
                // 过滤不能转换成JSON的参数
                if ((arg instanceof ServletRequest) || (arg instanceof ServletResponse)) {
                    continue;
                } else if ((arg instanceof MultipartFile)) {
                    arg = arg.toString();
                }
                params.put(stringObjectEntry.getKey(), arg);

                //从方法参数中查找
                clusterParam(op, arg);
            }


            //设置请求参数
            op.setParam(JSONObject.toJSONString(params));
            op.setParamMap(params);//暂存数据

            //删除查找数据查询
            getDeleteData(point, op);
        } catch (Exception e) {
            log.error("接口出入参日志打印切面处理请求参数异常", e);
        }
    }

    /**
     * 集群参数
     */
    private static void clusterParam(OperationLogEntity op, Object arg) {
        Object parse = JSON.parse(JSONObject.toJSONString(arg));
        if (!(parse instanceof JSONObject param)) {
            return;
        }

        if (Objects.isNull(op.getClusterId())) {
            Object cId = param.get("clusterId");
            if (Objects.nonNull(cId) && cId instanceof Integer) {
                op.setClusterId(Long.valueOf(cId.toString()));
            }
        }

        if (Objects.isNull(op.getHostIds())) {
            Object hId = param.get("hostIds");
            if (Objects.nonNull(hId) && hId instanceof String) {
                op.setHostIds(hId.toString());
            }
        }

        if (Objects.isNull(op.getServiceName())) {
            Object serviceName = param.get("serviceName");
            if (Objects.nonNull(serviceName) && serviceName instanceof String) {
                op.setServiceName(serviceName.toString());
            }
        }

        if (Objects.isNull(op.getServiceRoleInstancesIds())) {
            Object serviceRoleInstancesIds = param.get("serviceRoleInstancesIds");
            if (Objects.nonNull(serviceRoleInstancesIds) && serviceRoleInstancesIds instanceof String) {
                op.setServiceRoleInstancesIds(serviceRoleInstancesIds.toString());
            }
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
    private void setOperationType(OperationLogEntity op, String requestURI) {

        //设置操作类型
        String operationType = operationLogUrlMap.get(requestURI);
        if (StrUtil.isNotEmpty(operationType)) {
            parserAndSetOperationType(op, operationType);
        }

        //模块URI ，匹配模块访问路径匹配
        String moduleUri = getModuleUri(requestURI);
        //设置操作模块
        String module = Optional.ofNullable(operationLogUrlMap.get(moduleUri)).orElse(op.getOperationType());
        op.setOperationModule(module);

    }

    /**
     * 解析并设置操作类型取值
     *
     */
    private void parserAndSetOperationType(OperationLogEntity op, String operationType) {
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

    private static void getDeleteData(ProceedingJoinPoint joinPoint, OperationLogEntity op) {
        if (!op.getUrl().endsWith("delete")) {
            return;
        }
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
                .filter(v -> {
                    String serviceClassName = joinPoint.getTarget().getClass().getDeclaredFields()[0].getType().getSimpleName();
                    return serviceClassName.substring(0, serviceClassName.lastIndexOf("Service")).equals(controller);
                })
                .findFirst().orElse(null);

        //执行service方法获取删除的数据
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
                Object bean = SpringUtil.getBean(service);
                List<Object> oldObjs = (List<Object>) findMethod.invoke(bean, ids);
                Object oldObj = oldObjs.stream().findFirst().orElse(null);
                if (Objects.nonNull(oldObj)) {
//                log.info(JSONObject.toJSONString(oldObj));
                    op.setParam(JSONObject.toJSONString(oldObj));
                    //从数据库查找
                    clusterParam(op, oldObj);
                }

            } catch (Exception e) {
                log.warn("delete log invoke listByIds method to get delete object error");
            }
        }
    }

}
