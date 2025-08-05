package com.datasophon.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Kubernetes资源通用DTO
 * 用于封装各种Kubernetes资源对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KubernetesResourceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 资源名称
     */
    private String name;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 资源类型
     */
    private String kind;

    /**
     * 创建时间
     */
    private String creationTimestamp;

    /**
     * 状态
     */
    private String status;

    /**
     * 标签
     */
    private Map<String, String> labels;

    /**
     * 注解
     */
    private Map<String, String> annotations;

    /**
     * 扩展属性（用于存储特定资源的额外信息）
     */
    private Map<String, Object> additionalProperties;
}