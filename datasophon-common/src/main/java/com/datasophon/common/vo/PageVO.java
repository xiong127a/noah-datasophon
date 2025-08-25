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

package com.datasophon.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页响应VO
 * Controller层返回给前端的分页数据结构
 * 
 * @param <T> 数据类型
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> data;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long pageNum;

    /**
     * 每页大小
     */
    private Long pageSize;

    /**
     * 总页数
     */
    private Long totalPages;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 从PageResult转换为PageVO
     * 
     * @param pageResult PageResult对象
     * @param data 转换后的数据列表
     * @param <S> 源数据类型
     * @param <T> 目标数据类型
     * @return PageVO对象
     */
    public static <S, T> PageVO<T> from(com.datasophon.common.model.PageResult<S> pageResult, List<T> data) {
        if (pageResult == null) {
            return PageVO.<T>builder()
                    .data(data != null ? data : List.of())
                    .total(0L)
                    .pageNum(1L)
                    .pageSize(10L)
                    .totalPages(0L)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }

        long totalPages = (pageResult.getTotal() + pageResult.getSize() - 1) / pageResult.getSize();
        
        return PageVO.<T>builder()
                .data(data != null ? data : List.of())
                .total(pageResult.getTotal())
                .pageNum(pageResult.getCurrent())
                .pageSize(pageResult.getSize())
                .totalPages(totalPages)
                .hasNext(pageResult.getCurrent() < totalPages)
                .hasPrevious(pageResult.getCurrent() > 1)
                .build();
    }

    /**
     * 创建空的分页结果
     * 
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param <T> 数据类型
     * @return 空的PageVO
     */
    public static <T> PageVO<T> empty(Long pageNum, Long pageSize) {
        return PageVO.<T>builder()
                .data(List.of())
                .total(0L)
                .pageNum(pageNum != null ? pageNum : 1L)
                .pageSize(pageSize != null ? pageSize : 10L)
                .totalPages(0L)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
