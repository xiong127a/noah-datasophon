package com.datasophon.common.dto;

import java.util.List;

/**
 * 筛选选项DTO
 * 用于前端下拉框的筛选选项
 */
public record FilterOptionsDTO(
    List<String> statuses,
    List<String> roles
) {}