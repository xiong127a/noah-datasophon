package com.datasophon.api.service.host.dto;

import com.datasophon.common.enums.ManagementStatus;
import lombok.Data;

import java.util.Date;

@Data
public class QueryHostListPageDTO {

    private Integer id;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 主机名
     */
    private String hostname;
    /**
     * IP
     */
    private String ip;
    /**
     * 机架
     */
    private String rack;
    /**
     * 核数
     */
    private Integer coreNum;
    /**
     * 总内存
     */
    private Integer totalMem;
    /**
     * 总磁盘
     */
    private Integer totalDisk;
    /**
     * 已用内存
     */
    private Integer usedMem;
    /**
     * 已用磁盘
     */
    private Integer usedDisk;
    /**
     * 平均负载
     */
    private String averageLoad;
    /**
     * 检测时间
     */
    private Date checkTime;
    /**
     * 集群id
     */
    private Integer clusterId;
    /**
     * 1:正常运行 2：断线 3、存在告警
     */
    private Integer hostState;
    /**
     * 主机管理状态：1-受管，2-未受管，3-配置中
     */
    private ManagementStatus managementStatus;

    private String cpuArchitecture;

    private String nodeLabel;

    private Long serviceRoleNum;

    /**
     * 主机的hosts文件内容
     */
    private String hostsFile;

    /**
     * 操作系统类型
     */
    private String osType;
}
