package com.datasophon.common.dto;

import java.util.List;
import java.util.Map;

/**
 * 主机发现结果DTO - 使用Java 21 Record特性
 * 用于API返回给前端的数据结构
 */
public record HostDiscoveryResultDTO(
    /*
      发现的主机列表
     */
    List<HostInfoDTO> hosts,
    
    /*
      总数
     */
    Integer totalCount,
    
    /*
      是否成功
     */
    Boolean success,
    
    /*
      错误消息（如果有）
     */
    String errorMessage,
    
    /*
      元数据信息
     */
    Map<String, Object> metadata,
    
    /*
      发现耗时（毫秒）
     */
    Long discoveryTime
) {
    /**
     * 成功结果的构造器
     */
    public static HostDiscoveryResultDTO success(List<HostInfoDTO> hosts, 
                                                Integer totalCount,
                                                Map<String, Object> metadata, 
                                                Long discoveryTime) {
        return new HostDiscoveryResultDTO(hosts, totalCount, true, null, metadata, discoveryTime);
    }
    
    /**
     * 失败结果的构造器
     */
    public static HostDiscoveryResultDTO error(String errorMessage) {
        return new HostDiscoveryResultDTO(List.of(), 0, false, errorMessage, Map.of(), 0L);
    }
}