package com.datasophon.common.dto;

import com.datasophon.common.enums.ClusterType;
import lombok.Data;

/**
 * Step1配置数据传输对象
 * 用于接收前端Step1页面的配置信息
 * 
 * @author DataSophon Team
 */
@Data
public class Step1ConfigurationDto {
    
    /**
     * 集群ID（由Controller层从请求头设置）
     */
    private Long clusterId;
    
    /**
     * 集群类型枚举
     */
    private ClusterType clusterType;
    
    // =================== PVM集群配置参数 ===================
    
    /**
     * 主机IP列表（支持逗号分隔、换行分隔、IP范围等格式）
     * 例如：192.168.1.100,192.168.1.101 或 10.3.144.[19-23]
     */
    private String hosts;
    
    /**
     * SSH用户名
     */
    private String sshUser;
    
    /**
     * SSH端口
     */
    private String sshPort;
    
    /**
     * SSH密码
     */
    private String sshPassword;
    
    // =================== Kubernetes集群配置参数 ===================
    
    /**
     * Kubernetes配置文件内容
     */
    private String kubeConfigContent;
    
    /**
     * Kubernetes命名空间
     */
    private String namespace;
    
    /**
     * 是否创建新的命名空间
     */
    private Boolean isCreatingNewNamespace;
    
    /**
     * 自定义命名空间名称（当isCreatingNewNamespace为true时使用）
     */
    private String customNamespace;
    
    /**
     * 集群版本信息（K8S）
     */
    private String clusterVersion;
    
    /**
     * 可用的命名空间列表（前端解析后传递，用于验证）
     */
    private String[] namespaces;
    
    /**
     * 是否强制刷新
     */
    private Boolean forceRefresh = false;
}