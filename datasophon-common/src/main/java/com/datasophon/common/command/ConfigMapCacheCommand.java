package com.datasophon.common.command;

import com.datasophon.common.model.ServiceConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class ConfigMapCacheCommand implements Serializable {
    private String key;

    private List<ServiceConfig> configs;

    public ConfigMapCacheCommand(String key, List<ServiceConfig> configs) {
        this.key = key;
        this.configs = configs;
    }
}
