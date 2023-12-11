package com.datasophon.dao.model;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class MPage<T> extends Page<T> {

    {
//        super.optimizeCountSql = false;
    }

    private T param;

    private String keyword = "";

    private Integer page;

    public void setKeyword(String keyword) {
        if (null == keyword || "".equals(keyword)) {
            this.keyword = null;
        } else {
            this.keyword = keyword;
        }
    }

    public MPage() {

    }
}

