package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class VariableCacheCommand implements Serializable{
    private String key;

    private String value;

    private Long clusterId;

    public VariableCacheCommand(String key, String value, Long clusterId) {
        this.key = key;
        this.value = value;
        this.clusterId = clusterId;
    }
}
