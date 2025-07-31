/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页结果数据传输对象
 * 
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总数
     */
    private long total;

    /**
     * 当前页码
     */
    private int pageNum;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 创建分页结果
     * 
     * @param list     数据列表
     * @param total    总数
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        return PageResult.<T>builder()
                .list(list)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }

    /**
     * 创建空的分页结果
     * 
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return PageResult.<T>builder()
                .list(List.of())
                .total(0)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }
}