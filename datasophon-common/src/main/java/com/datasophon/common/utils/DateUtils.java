package com.datasophon.common.utils;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 日期工具类
 */
public class DateUtils {
    
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 格式化日期为字符串
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
    
    /**
     * 格式化日期为字符串
     * @param date 日期
     * @param pattern 格式化模式
     * @return 格式化后的字符串
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }
    
    /**
     * 计算时间差，并格式化为可读形式
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 格式化后的时间差字符串 (如: 2小时5分钟30秒)
     */
    public static String formatDuration(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return "";
        }
        
        long diffInMillis = endTime.getTime() - startTime.getTime();
        return formatDuration(diffInMillis);
    }
    
    /**
     * 格式化持续时间为可读形式
     * @param millis 毫秒数
     * @return 格式化后的持续时间字符串 (如: 2小时5分钟30秒)
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "0秒";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("天");
        }
        
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        
        if (minutes > 0) {
            sb.append(minutes).append("分钟");
        }
        
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("秒");
        }
        
        return sb.toString();
    }
} 