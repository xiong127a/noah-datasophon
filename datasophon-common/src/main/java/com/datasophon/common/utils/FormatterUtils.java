/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.common.utils;

import lombok.experimental.UtilityClass;
import org.mapstruct.Named;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 通用格式化工具类
 * 使用 Lombok @UtilityClass 注解，提供静态格式化方法
 * 可以在 MapStruct 转换器中通过 @Named 引用
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@UtilityClass
public class FormatterUtils {

    // ============ 日期时间格式化 ============

    /**
     * 通用日期时间格式化
     * 
     * @param date 日期对象
     * @return 格式化后的日期字符串 "yyyy-MM-dd HH:mm:ss"
     */
    @Named("formatDateTime")
    public static String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }

    /**
     * 通用日期格式化（仅日期部分）
     * 
     * @param date 日期对象
     * @return 格式化后的日期字符串 "yyyy-MM-dd"
     */
    @Named("formatDate")
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    /**
     * 通用时间格式化（仅时间部分）
     * 
     * @param date 日期对象
     * @return 格式化后的时间字符串 "HH:mm:ss"
     */
    @Named("formatTime")
    public static String formatTime(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        return formatter.format(date);
    }

    // ============ 状态格式化 ============

    /**
     * 通用布尔状态格式化
     * 
     * @param status 布尔状态
     * @return 格式化后的状态字符串
     */
    @Named("formatBooleanStatus")
    public static String formatBooleanStatus(Boolean status) {
        if (status == null) {
            return "未知";
        }
        return status ? "是" : "否";
    }

    /**
     * 通用启用/禁用状态格式化
     * 
     * @param enabled 是否启用
     * @return 格式化后的状态字符串
     */
    @Named("formatEnabledStatus")
    public static String formatEnabledStatus(Boolean enabled) {
        if (enabled == null) {
            return "未知";
        }
        return enabled ? "启用" : "禁用";
    }

    /**
     * 通用有效/无效状态格式化
     * 
     * @param valid 是否有效
     * @return 格式化后的状态字符串
     */
    @Named("formatValidStatus")
    public static String formatValidStatus(Boolean valid) {
        if (valid == null) {
            return "未知";
        }
        return valid ? "有效" : "无效";
    }

    // ============ 时间计算 ============

    /**
     * 计算距离当前时间的秒数
     * 
     * @param targetTime 目标时间
     * @return 距离当前时间的秒数，如果已过期则返回负数
     */
    @Named("calculateRemainingSeconds")
    public static Long calculateRemainingSeconds(Date targetTime) {
        if (targetTime == null) {
            return null;
        }
        return (targetTime.getTime() - System.currentTimeMillis()) / 1000;
    }

    /**
     * 格式化剩余时间
     * 
     * @param targetTime 目标时间
     * @return 格式化后的剩余时间字符串
     */
    @Named("formatRemainingTime")
    public static String formatRemainingTime(Date targetTime) {
        if (targetTime == null) {
            return "永不过期";
        }

        long remaining = targetTime.getTime() - System.currentTimeMillis();
        if (remaining <= 0) {
            return "已过期";
        }

        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        return switch ((int) days) {
            case 0 -> {
                if (hours > 0) {
                    yield hours + "小时" + (minutes % 60) + "分钟";
                } else if (minutes > 0) {
                    yield minutes + "分钟";
                } else {
                    yield seconds + "秒";
                }
            }
            case 1 -> "1天" + (hours % 24) + "小时";
            default -> days + "天";
        };
    }

    // ============ 数值格式化 ============

    /**
     * 通用数量格式化
     * 
     * @param count 数量
     * @param unit  单位
     * @return 格式化后的数量字符串
     */
    @Named("formatCount")
    public static String formatCount(Integer count, String unit) {
        if (count == null) {
            return "0个" + unit;
        }
        return switch (count) {
            case 0 -> "0个" + unit;
            case 1 -> "1个" + unit;
            default -> count + "个" + unit;
        };
    }

    /**
     * 通用文件大小格式化
     * 
     * @param bytes 字节数
     * @return 格式化后的文件大小字符串
     */
    @Named("formatFileSize")
    public static String formatFileSize(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "0 B";
        }

        String[] units = { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);

        double size = bytes / Math.pow(1024, digitGroups);
        return String.format("%.1f %s", size, units[digitGroups]);
    }

    /**
     * 通用百分比格式化
     * 
     * @param value 数值 (0.0 - 1.0)
     * @return 格式化后的百分比字符串
     */
    @Named("formatPercentage")
    public static String formatPercentage(Double value) {
        if (value == null) {
            return "0%";
        }
        return String.format("%.1f%%", value * 100);
    }
}