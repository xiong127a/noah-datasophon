package com.datasophon.dao.model;


import com.mybatisflex.core.paginate.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MPage<T> extends Page<T> {

    private T param;

    private String keyword = "";

    private Integer page;


    public MPage() {

    }
}

