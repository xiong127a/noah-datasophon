package com.datasophon.common.dto.host;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 批量修改主机名请求
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Data
public class BatchHostnameChangeRequest {
    
    /**
     * 集群ID
     */
    private Long clusterId;
    
    /**
     * 主机名前缀
     */
    private String prefix;
    
    /**
     * 后缀格式索引（对应配置中的suffixFormats列表）
     */
    private Integer suffixFormatIndex;
    
    /**
     * 起始编号
     */
    private Integer startIndex;
    
    /**
     * 需要修改的主机IP列表
     */
    private List<String> hostIps;
    
    /**
     * SSH连接参数
     */
    private Map<String, Object> connectionParams;
}

