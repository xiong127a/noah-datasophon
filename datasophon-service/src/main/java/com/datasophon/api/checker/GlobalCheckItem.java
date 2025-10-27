package com.datasophon.api.checker;

import com.datasophon.common.vo.environment.GlobalCheckResult;

import java.util.List;
import java.util.Map;

/**
 * 全局检查项接口
 * 用于跨主机的全局检查（如主机名唯一性、hosts文件一致性）
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
public interface GlobalCheckItem {
    
    /**
     * 获取检查项键名
     * 例如: "hostname", "hosts-file"
     * 
     * @return 检查项键名
     */
    String getCheckKey();
    
    /**
     * 获取检查项显示名称
     * 例如: "主机名唯一性检查", "Hosts文件一致性检查"
     * 
     * @return 显示名称
     */
    String getDisplayName();
    
    /**
     * 获取优先级
     * 数字越小优先级越高
     * 
     * @return 优先级
     */
    int getPriority();
    
    /**
     * 执行全局检查（针对整个集群）
     * 
     * @param hosts 所有主机的信息（IP、主机名等）
     * @param clusterId 集群ID
     * @param connectionParams SSH连接参数
     * @return 全局检查结果
     */
    GlobalCheckResult execute(List<HostInfo> hosts, Long clusterId, Map<String, Object> connectionParams);
    
    /**
     * 是否启用此检查项
     * 可以从配置中读取
     * 
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * 主机信息类（简化版，用于全局检查）
     */
    public static class HostInfo {
        private String ip;
        private String hostname;
        private Map<String, Object> additionalInfo;
        
        public HostInfo(String ip, String hostname) {
            this.ip = ip;
            this.hostname = hostname;
        }
        
        public HostInfo(String ip, String hostname, Map<String, Object> additionalInfo) {
            this.ip = ip;
            this.hostname = hostname;
            this.additionalInfo = additionalInfo;
        }
        
        public String getIp() {
            return ip;
        }
        
        public void setIp(String ip) {
            this.ip = ip;
        }
        
        public String getHostname() {
            return hostname;
        }
        
        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
        
        public Map<String, Object> getAdditionalInfo() {
            return additionalInfo;
        }
        
        public void setAdditionalInfo(Map<String, Object> additionalInfo) {
            this.additionalInfo = additionalInfo;
        }
    }
}

