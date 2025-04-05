package com.datasophon.common.model.hardware;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * DNS信息类
 * 存储主机DNS配置的详细信息和状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DnsInfo extends HardwareInfo {
    private static final long serialVersionUID = 1L;

    /**
     * DNS服务器列表
     */
    private List<String> servers;

    /**
     * /etc/resolv.conf文件内容
     */
    private String resolvConfContent;

    /**
     * /etc/hosts文件内容
     */
    private String hostsFileContent;

    /**
     * DNS是否工作正常
     */
    private boolean working;

    /**
     * 构造函数，设置类型名称
     */
    public DnsInfo() {
        setTypeName("DNS");
    }
}