package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class CacheCommand implements Serializable {

    private String key;

    private Boolean isDelete;

    public CacheCommand(String key,Boolean isDelete) {
        this.key = key;
        this.isDelete = isDelete;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Boolean isDelete() {
        return isDelete;
    }

    public void setIsDelete(Boolean delete) {
        isDelete = delete;
    }
}
