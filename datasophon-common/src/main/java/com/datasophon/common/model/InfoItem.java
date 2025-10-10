package com.datasophon.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信息项类，用于存储连接信息的基本单元
 * 包含英文键名、中文显示名和实际值
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfoItem {
    /**
     * 键名，使用英文，用于在模板中引用
     */
    private String key;

    /**
     * 显示名称，通常使用中文，用于前端展示
     */
    private String displayName;

    /**
     * 值，任何类型
     */
    private String value;
}