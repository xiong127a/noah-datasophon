package com.datasophon.api.controller;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * API文档控制器，用于查看所有注册的API端点
 */
@RestController
@RequestMapping("api/doc")
public class ApiDocController {

    private static final Logger logger = LoggerFactory.getLogger(ApiDocController.class);

    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    /**
     * 获取所有API端点信息
     * 
     * @return API端点列表
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getAllEndpoints() {
        try {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

            // 按控制器分组
            Map<String, List<Map<String, Object>>> groupedEndpoints = new TreeMap<>();
            int totalEndpoints = 0;

            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
                RequestMappingInfo info = entry.getKey();
                HandlerMethod method = entry.getValue();

                try {
                    String controllerName = method.getBeanType().getSimpleName();

                    // 跳过自身控制器，避免无限递归
                    if (controllerName.equals(this.getClass().getSimpleName())) {
                        continue;
                    }

                    // 获取URL模式
                    Set<String> patterns = extractPatterns(info);

                    // 获取HTTP方法
                    Set<String> httpMethods = extractHttpMethods(info);

                    // 获取处理方法名
                    final String methodName = method.getMethod().getName();

                    // 收集信息
                    for (String pattern : patterns) {
                        Map<String, Object> endpointInfo = new HashMap<>();
                        endpointInfo.put("path", pattern);
                        endpointInfo.put("method", String.join(",", httpMethods));
                        endpointInfo.put("controller", controllerName);
                        endpointInfo.put("handler", methodName);

                        groupedEndpoints.computeIfAbsent(controllerName, k -> new ArrayList<>())
                                .add(endpointInfo);
                        totalEndpoints++;
                    }
                } catch (Exception e) {
                    // 忽略单个端点处理中的异常，继续处理其他端点
                    logger.warn("处理端点信息时出错: {}", e.getMessage());
                }
            }

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("endpoints", groupedEndpoints);
            response.put("totalControllers", groupedEndpoints.size());
            response.put("totalEndpoints", totalEndpoints);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取API端点时出错", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取API端点失败");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取所有API路径的纯文本列表
     * 
     * @return API路径列表
     */
    @GetMapping("/paths")
    public ResponseEntity<?> getAllPaths() {
        try {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

            List<String> paths = new ArrayList<>();
            for (RequestMappingInfo info : handlerMethods.keySet()) {
                try {
                    Set<String> patterns = extractPatterns(info);
                    // 过滤掉无URL模式的占位符
                    patterns.stream()
                            .filter(p -> !"[无URL模式]".equals(p))
                            .forEach(paths::add);
                } catch (Exception e) {
                    logger.warn("处理路径信息时出错: {}", e.getMessage());
                }
            }

            return ResponseEntity.ok(paths.stream()
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("获取API路径时出错", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取API路径失败");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 从RequestMappingInfo中提取路径模式
     * 
     * @param info RequestMappingInfo对象
     * @return 路径模式集合
     */
    private Set<String> extractPatterns(RequestMappingInfo info) {
        Set<String> patterns = new HashSet<>();

        try {
            if (info == null) {
                patterns.add("[无URL模式]");
                return patterns;
            }

            // 尝试使用getPatternsCondition获取路径
            if (info.getPatternsCondition() != null) {
                Set<String> directPatterns = info.getPatternsCondition().getPatterns();
                if (!directPatterns.isEmpty()) {
                    // 处理路径，添加上下文前缀
                    patterns.addAll(directPatterns.stream()
                            .map(this::normalizeAndPrefixPath)
                            .collect(Collectors.toSet()));
                    return patterns;
                }
            }

            // 尝试通过PathPatternsCondition获取路径（Spring Boot 2.6+）
            try {
                Object pathPatternsCondition = info.getClass().getMethod("getPathPatternsCondition").invoke(info);
                if (pathPatternsCondition != null) {
                    @SuppressWarnings("unchecked")
                    Set<String> pathPatterns = (Set<String>) pathPatternsCondition.getClass()
                            .getMethod("getPatternValues").invoke(pathPatternsCondition);

                    if (pathPatterns != null && !pathPatterns.isEmpty()) {
                        // 处理路径，添加上下文前缀
                        patterns.addAll(pathPatterns.stream()
                                .map(this::normalizeAndPrefixPath)
                                .collect(Collectors.toSet()));
                        return patterns;
                    }
                }
            } catch (Exception e) {
                logger.debug("通过PathPatternsCondition获取路径失败: {}", e.getMessage());
                // 忽略异常，继续尝试其他方法
            }

            // 如果都失败，尝试通过toString方法分析
            String infoString = info.toString();
            if (infoString.contains("{") && infoString.contains("}")) {
                String pathPart = infoString.substring(
                        infoString.indexOf("{") + 1,
                        infoString.indexOf("}"));

                if (!pathPart.trim().isEmpty()) {
                    Arrays.stream(pathPart.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(this::normalizeAndPrefixPath)
                            .forEach(patterns::add);
                    return patterns;
                }
            }

            // 如果仍然无法获取路径，添加占位符
            if (patterns.isEmpty()) {
                patterns.add("[无URL模式]");
            }
        } catch (Exception e) {
            logger.warn("提取路径模式时出错: {}", e.getMessage());
            patterns.add("[无URL模式]");
        }

        return patterns;
    }

    /**
     * 从RequestMappingInfo中提取HTTP方法
     * 
     * @param info RequestMappingInfo对象
     * @return HTTP方法集合
     */
    private Set<String> extractHttpMethods(RequestMappingInfo info) {
        Set<String> httpMethods = new HashSet<>();

        try {
            if (info != null) {
                if (info.getMethodsCondition().getMethods().isEmpty()) {
                    httpMethods.add("ALL");
                } else {
                    info.getMethodsCondition().getMethods()
                            .forEach(httpMethod -> httpMethods.add(httpMethod.name()));
                }
            } else {
                httpMethods.add("ALL");
            }
        } catch (Exception e) {
            logger.warn("提取HTTP方法时出错: {}", e.getMessage());
            httpMethods.add("ALL");
        }

        return httpMethods;
    }

    /**
     * 规范化路径并添加上下文前缀
     * 
     * @param path 原始路径
     * @return 规范化后的路径
     */
    private String normalizeAndPrefixPath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // 确保路径以/开头
        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        // 如果上下文路径不是"/"，则添加上下文路径前缀
        if (!"/".equals(contextPath)) {
            String ctx = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
            // 避免重复添加上下文路径
            if (!normalizedPath.startsWith(ctx)) {
                normalizedPath = ctx + normalizedPath;
            }
        }

        return normalizedPath;
    }
}