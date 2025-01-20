package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class Sqlite3ExecCommand implements Serializable {
    /**
     * grafana安装节点的Ip
     */
    private String grafanaIp;
    /**
     * url
     */
    private String url;
}
