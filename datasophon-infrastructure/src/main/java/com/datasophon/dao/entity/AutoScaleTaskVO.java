package com.datasophon.dao.entity;

import lombok.Data;

import java.util.List;
@Data
public class AutoScaleTaskVO {
    private Integer taskId;
    private Integer serviceId;
    private Integer clusterId;
    private List<String> serviceRoles;
    private String scaleType;
}