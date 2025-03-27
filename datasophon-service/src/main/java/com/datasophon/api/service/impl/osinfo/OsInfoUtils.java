package com.datasophon.api.service.impl.osinfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 操作系统信息工具类
 * 提供公共方法用于处理操作系统信息
 */
public class OsInfoUtils {

    private static final Logger logger = LoggerFactory.getLogger(OsInfoUtils.class);

    /**
     * 从配置文件内容中提取指定键的值
     * 主要用于解析/etc/os-release文件内容
     *
     * @param content 文件内容
     * @param key     要查找的键（如ID=、NAME=等）
     * @return 找到的值，如果未找到则返回空字符串
     */
    public static String extractValue(String content, String key) {
        if (content == null || key == null) {
            return "";
        }

        // 确保键以等号结尾
        if (!key.endsWith("=")) {
            key = key + "=";
        }

        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(key)) {
                String value = line.substring(key.length()).trim();
                // 移除引号（如果有）
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }

        return "";
    }

    /**
     * 从Windows注册表值中提取信息
     * 
     * @param regValue 注册表查询结果
     * @param key      要查找的键
     * @return 找到的值，如果未找到则返回空字符串
     */
    public static String extractWindowsRegValue(String regValue, String key) {
        if (regValue == null || key == null) {
            return "";
        }

        // 移除可能存在的注册表查询输出头部信息
        String[] lines = regValue.split("\r\n|\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains(key)) {
                // 提取键后面的值
                int keyIndex = line.indexOf(key);
                if (keyIndex >= 0) {
                    String value = line.substring(keyIndex + key.length()).trim();
                    // 移除REG_SZ、REG_DWORD等类型标识
                    int typeIndex = value.indexOf("REG_");
                    if (typeIndex >= 0) {
                        int valueStart = value.indexOf(" ", typeIndex + 4);
                        if (valueStart >= 0) {
                            value = value.substring(valueStart).trim();
                        }
                    }
                    return value;
                }
            }
        }

        return "";
    }

    /**
     * 解析内存大小字符串（如4GB，2048MB等）为字节数
     * 
     * @param memoryStr 内存大小字符串
     * @return 内存字节数，解析失败时返回0
     */
    public static long parseMemorySize(String memoryStr) {
        if (memoryStr == null || memoryStr.trim().isEmpty()) {
            return 0;
        }

        try {
            memoryStr = memoryStr.trim().toUpperCase();

            // 移除可能存在的逗号和空格
            memoryStr = memoryStr.replace(",", "").replace(" ", "");

            if (memoryStr.endsWith("GB") || memoryStr.endsWith("G")) {
                String numPart = memoryStr.replace("GB", "").replace("G", "");
                return (long) (Double.parseDouble(numPart) * 1024 * 1024 * 1024);
            } else if (memoryStr.endsWith("MB") || memoryStr.endsWith("M")) {
                String numPart = memoryStr.replace("MB", "").replace("M", "");
                return (long) (Double.parseDouble(numPart) * 1024 * 1024);
            } else if (memoryStr.endsWith("KB") || memoryStr.endsWith("K")) {
                String numPart = memoryStr.replace("KB", "").replace("K", "");
                return (long) (Double.parseDouble(numPart) * 1024);
            } else if (memoryStr.matches("\\d+")) {
                // 纯数字，假设为字节数
                return Long.parseLong(memoryStr);
            }
        } catch (Exception e) {
            logger.warn("解析内存大小失败: {}", memoryStr, e);
        }

        return 0;
    }
}