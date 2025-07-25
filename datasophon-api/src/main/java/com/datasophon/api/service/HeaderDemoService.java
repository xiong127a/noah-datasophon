package com.datasophon.api.service;

import com.datasophon.api.common.HeaderContextHolder;
import org.springframework.stereotype.Service;

/**
 * 演示服务类
 * 展示如何在服务层访问请求头信息
 */
@Service
public class HeaderDemoService {

    /**
     * 处理用户请求的业务逻辑
     * 
     * @return 处理结果
     */
    public String processRequest() {
        // 可以直接从HeaderContextHolder获取请求头，无需通过参数传递
        String userId = HeaderContextHolder.getHeader("X-User-Id");
        String tenantId = HeaderContextHolder.getHeader("X-Tenant-Id");

        // 根据请求头中的信息进行业务处理
        return String.format("处理来自用户 %s (租户: %s) 的请求",
                userId != null ? userId : "未知",
                tenantId != null ? tenantId : "未知");
    }

    /**
     * 调用另一个微服务的示例
     * 可以将请求头传递给下游服务
     */
    public void callAnotherService() {
        // 获取当前请求的认证信息
        String authorization = HeaderContextHolder.getHeader("Authorization");

        // 模拟调用其他服务时传递认证信息
        if (authorization != null) {
            // 例如：restTemplate.exchange(url, HttpMethod.GET,
            // createHeadersWithAuth(authorization), responseType);
            System.out.println("将认证信息传递给下游服务: " + authorization);
        }
    }

    /**
     * 演示在事务中使用请求头
     */
    public void saveDataWithUserContext() {
        String userId = HeaderContextHolder.getHeader("X-User-Id");

        // 模拟使用用户ID进行数据存储，例如记录创建者ID等
        if (userId != null) {
            // 例如: entityRepository.save(entity.setCreatedBy(userId));
            System.out.println("使用用户ID保存数据: " + userId);
        }
    }
}