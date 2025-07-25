package com.datasophon.api.controller;

import com.datasophon.api.common.HeaderContextHolder;
import com.datasophon.api.service.HeaderDemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求头处理演示控制器
 */
@RestController
@RequestMapping("/api/header-demo")
public class HeaderDemoController {

    @Autowired
    private HeaderDemoService headerDemoService;

    /**
     * 演示如何在Controller中访问请求头
     */
    @GetMapping("/headers")
    public ResponseEntity<Map<String, Object>> getHeaders() {
        Map<String, Object> result = new HashMap<>();

        // 获取特定的请求头
        String userId = HeaderContextHolder.getHeader("X-User-Id");
        String tenantId = HeaderContextHolder.getHeader("X-Tenant-Id");
        String authorization = HeaderContextHolder.getHeader("Authorization");
        String grafanaHost = HeaderContextHolder.getHeader("grafanaHost");

        // 获取所有请求头
        Map<String, String> allHeaders = HeaderContextHolder.getAllHeaders();

        result.put("userId", userId);
        result.put("tenantId", tenantId);
        result.put("authorization", authorization);
        result.put("grafanaHost", grafanaHost);
        result.put("allHeaders", allHeaders);

        return ResponseEntity.ok(result);
    }

    /**
     * 演示在Service调用中如何获取请求头
     */
    @GetMapping("/service-demo")
    public ResponseEntity<String> serviceDemo() {
        // 调用服务层方法，服务层可以自行获取请求头
        String result = headerDemoService.processRequest();
        return ResponseEntity.ok(result);
    }

    /**
     * 演示调用其他服务时传递请求头
     */
    @GetMapping("/call-another-service")
    public ResponseEntity<String> callAnotherService() {
        headerDemoService.callAnotherService();
        return ResponseEntity.ok("调用完成，查看控制台日志");
    }

    /**
     * 演示在事务中使用请求头
     */
    @GetMapping("/save-data")
    public ResponseEntity<String> saveData() {
        headerDemoService.saveDataWithUserContext();
        return ResponseEntity.ok("数据保存完成，查看控制台日志");
    }
}