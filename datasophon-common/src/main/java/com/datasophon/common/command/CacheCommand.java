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

    public Boolean isDelete() {
        return isDelete;
    }

}
