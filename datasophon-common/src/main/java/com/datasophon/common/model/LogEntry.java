package com.datasophon.common.model;

import lombok.Getter;

import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 结构化日志记录
 */
@Getter
public class LogEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 日志级别枚举
     */
    @Getter
    public enum Level {
        DEBUG(0, "#8c8c8c", "#fafafa"),  // 灰色,不显眼
        INFO(1, "#389e0d", "#f8fff0"),   // 深绿色，更柔和的背景
        WARN(2, "#faad14", "#fff7e6"),   // 黄色/橙色
        ERROR(3, "#f5222d", "#fff1f0");  // 红色
        
        private int value;
        private String color;           // 主要颜色（用于强调文本）
        private String backgroundColor; // 背景色（用于整行）
        
        Level(int value) {
            this.value = value;
            // 默认颜色
            this.color = "#000000";
            this.backgroundColor = "#ffffff";
        }
        
        Level(int value, String color, String backgroundColor) {
            this.value = value;
            this.color = color;
            this.backgroundColor = backgroundColor;
        }

        public String getColor() {
            return color;
        }

        public String getBackgroundColor() {
            return backgroundColor;
        }

        /**
         * 判断当前级别是否包含目标级别
         * 例如: INFO.includes(DEBUG) 返回 false
         *      INFO.includes(INFO) 返回 true
         *      INFO.includes(WARN) 返回 true
         */
        public boolean includes(Level targetLevel) {
            return this.value <= targetLevel.value;
        }
        
        /**
         * 判断当前级别是否高于或等于目标级别
         */
        public boolean isHigherOrEqual(Level targetLevel) {
            return this.value >= targetLevel.value;
        }
    }
    
    /**
     * 日志类型枚举
     */
    @Getter
    public enum Type {
        CHECK("检查日志", "#1890ff"),
        FIX("修复日志", "#722ed1");
        
        private String displayName;
        private String color;
        
        Type(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public static Type fromString(String typeStr) {
            if (typeStr == null || typeStr.isEmpty()) {
                return CHECK; // 默认返回CHECK类型
            }
            
            try {
                return valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return CHECK; // 默认返回CHECK类型
            }
        }
    }
    
    // 日志时间
    private Date timestamp;
    // 日志级别
    private Level level;
    // 线程名称
    private String threadName;
    // 日志来源类名
    private String className;
    // 日志内容
    private String message;
    // 源代码行号
    private int lineNumber = -1;
    // 日志类型 
    private Type type = Type.CHECK; // 默认为CHECK类型
    // 堆栈信息的日志级别
    private Level stackTraceLevel = Level.ERROR; // 默认为ERROR级别
    
    public LogEntry() {
    }
    
    public LogEntry(Date timestamp, Level level, String threadName, String className, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.threadName = threadName;
        this.className = className;
        this.message = message;
    }
    
    public LogEntry(Date timestamp, Level level, String threadName, String className, String message, int lineNumber) {
        this.timestamp = timestamp;
        this.level = level;
        this.threadName = threadName;
        this.className = className;
        this.message = message;
        this.lineNumber = lineNumber;
    }
    
    public LogEntry(Date timestamp, Level level, String threadName, String className, String message, Type type) {
        this.timestamp = timestamp;
        this.level = level;
        this.threadName = threadName;
        this.className = className;
        this.message = message;
        this.type = type;
    }
    
    public LogEntry(Date timestamp, Level level, String threadName, String className, String message, int lineNumber, Type type) {
        this.timestamp = timestamp;
        this.level = level;
        this.threadName = threadName;
        this.className = className;
        this.message = message;
        this.lineNumber = lineNumber;
        this.type = type;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public void setStackTraceLevel(Level stackTraceLevel) {
        this.stackTraceLevel = stackTraceLevel;
    }
    
    public Level getStackTraceLevel() {
        return stackTraceLevel;
    }
    
    public Type getType() {
        return type;
    }
    
    /**
     * 获取日志创建时间的时间戳
     */
    public Long getCreatedTime() {
        return timestamp != null ? timestamp.getTime() : null;
    }
    
    /**
     * 将日志转换为带有颜色的HTML格式
     */
    public String toColoredHtml() {
        StringBuilder sb = new StringBuilder();
        
        // 根据日志级别设置样式
        String levelColor = level != null ? level.getColor() : "#000000";
        String bgColor = level != null ? level.getBackgroundColor() : "#ffffff";
        String borderColor;
        
        switch (level) {
            case ERROR:
                borderColor = "#ffccc7";
                break;
            case WARN:
                borderColor = "#ffe58f";
                break;
            case INFO:
                borderColor = "#b7eb8f";
                break;
            case DEBUG:
                borderColor = "#d9d9d9";
                break;
            default:
                borderColor = "#d9d9d9";
        }
        
        // 获取时间戳格式化字符串
        String timeFormatted = formatCreatedTime(getCreatedTime());
        
        // 构建日志条目容器
        sb.append(String.format("<div class=\"log-entry\" style=\"margin-bottom: 8px; padding: 8px 12px; background-color: %s; border-left: 3px solid %s;\">", bgColor, borderColor));
        
        // 日志头部信息（时间、级别、类型、线程）
        sb.append("<div class=\"log-header\" style=\"display: flex; justify-content: space-between; margin-bottom: 4px;\">");
        
        // 左侧：时间、日志级别和线程名
        sb.append("<div class=\"log-info\" style=\"display: flex; align-items: center; gap: 8px;\">");
        // 时间戳
        sb.append(String.format("<span class=\"log-time\" style=\"color: #595959;\">%s</span>", timeFormatted));
        // 日志级别
        sb.append(String.format("<span class=\"log-level\" style=\"color: %s; font-weight: 500; padding: 1px 6px; border-radius: 2px; background-color: %s; font-size: 12px;\">%s</span>", 
                              levelColor, level.getBackgroundColor(), level.name()));
        // 线程名
        if (threadName != null) {
            sb.append(String.format("<span class=\"log-thread\" style=\"color: #8c8c8c; font-size: 12px;\">[%s]</span>", threadName));
        }
        sb.append("</div>");
        
        // 右侧：类名和行号
        if (className != null) {
            String classInfo = className + (lineNumber > 0 ? ":" + lineNumber : "");
            sb.append(String.format("<div class=\"log-source\" style=\"color: #8c8c8c; font-size: 12px; cursor: pointer;\" onclick=\"copyToClipboard('%s')\" title=\"点击复制\">%s</div>", 
                classInfo, classInfo));
        }
        sb.append("</div>");
        
        // 日志内容
        String formattedContent = formatLogContent(message);
        sb.append(String.format("<div class=\"log-content\" style=\"color: %s;\">%s</div>", levelColor, formattedContent));
        
        sb.append("</div>");
        return sb.toString();
    }
    
    /**
     * 格式化日志内容，处理堆栈跟踪和高亮关键词
     * @param content 原始日志内容
     * @return 格式化后的日志内容
     */
    private String formatLogContent(String content) {
        if (content == null) {
            return "";
        }
        
        // 首先移除所有可能存在的HTML标签
        content = content.replaceAll("<[^>]*>", "");
        
        // 处理堆栈跟踪
        if (content.contains("Exception") || content.contains("Error:") || content.contains("Caused by:")) {
            return formatStackTrace(content);
        }
        
        // 转义HTML特殊字符
        content = escapeHtml(content);
        
        // 高亮关键字
        return highlightKeywords(content);
    }
    
    private String escapeHtml(String content) {
        if (content == null) {
            return "";
        }
        
        // 先移除所有可能存在的HTML标签
        content = content.replaceAll("<[^>]*>", "");
        
        // 转义HTML特殊字符
        return content.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\"", "&quot;")
                     .replace("'", "&#39;");
    }
    
    private String highlightKeywords(String content) {
        // 高亮时间戳
        content = content.replaceAll("(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?)", 
            "<span style=\"color: #8c8c8c;\">$1</span>");
        
        // 高亮成功/失败状态
        content = content.replaceAll("(?i)(成功|通过|SUCCESS)", 
            String.format("<span style=\"color: %s; font-weight: bold;\">$1</span>", Level.INFO.getColor()));
        content = content.replaceAll("(?i)(失败|错误|FAILED|FAILURE|ERROR)", 
            String.format("<span style=\"color: %s; font-weight: bold;\">$1</span>", Level.ERROR.getColor()));
        
        // 高亮IP地址和端口
        content = content.replaceAll("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?::(\\d+))?", 
            "<span style=\"color: #1890ff;\">$1</span>$2");
        
        // 高亮端口号（单独出现的情况）
        content = content.replaceAll("端口:\\s*(\\d+)", 
            "端口: <span style=\"color: #1890ff;\">$1</span>");
        
        // 高亮用户名
        content = content.replaceAll("用户:\\s*(\\w+)", 
            "用户: <span style=\"color: #722ed1;\">$1</span>");
        
        // 高亮路径 - 使用非贪婪匹配，避免匹配过多
        content = content.replaceAll("(?<=[\\s:]|^)(/[^\\s<>\"']+?)(?=[\\s<>\"']|$)", 
            "<span style=\"color: #722ed1;\">$1</span>");
        
        return content;
    }
    
    private String formatStackTrace(String content) {
        // 首先移除所有可能存在的HTML标签
        content = content.replaceAll("<[^>]*>", "");
        
        String[] lines = content.split("\n");
        StringBuilder sb = new StringBuilder();
        
        sb.append("<div class=\"stack-trace\" style=\"margin-top: 4px;\">");
        
        for (String line : lines) {
            // 转义HTML特殊字符
            String escapedLine = escapeHtml(line);
            
            if (line.contains("Exception") || line.contains("Error:")) {
                // 异常标题
                sb.append(String.format("<div style=\"color: %s; font-weight: bold;\">%s</div>", 
                    stackTraceLevel.getColor(), escapedLine));
            } else if (line.contains("Caused by:")) {
                // 内部异常
                sb.append(String.format("<div style=\"color: %s; font-weight: bold; margin-top: 8px;\">%s</div>", 
                    stackTraceLevel.getColor(), escapedLine));
            } else if (line.contains("at ")) {
                // 堆栈行
                if (line.contains("com.datasophon")) {
                    // 项目相关代码
                    sb.append(String.format("<div style=\"padding-left: 20px; color: %s;\">%s</div>", 
                        stackTraceLevel.getColor(), escapedLine));
                } else {
                    // 外部代码
                    sb.append(String.format("<div style=\"padding-left: 20px; color: #8c8c8c;\">%s</div>", 
                        escapedLine));
                }
            } else if (!line.trim().isEmpty()) {
                // 其他非空行
                sb.append(String.format("<div>%s</div>", escapedLine));
            }
        }
        
        sb.append("</div>");
        return sb.toString();
    }
    
    /**
     * 格式化时间戳为可读格式
     * @param timestamp 时间戳
     * @return 格式化后的时间字符串
     */
    private String formatCreatedTime(Long timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        );
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return dateTime.format(formatter);
    }
    
    /**
     * 将日志格式化为字符串
     * 包含完整信息：时间戳、级别、线程名、类名和消息
     */
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String classInfo = className;
        if (lineNumber > 0) {
            classInfo = String.format("%s:[%d]", className, lineNumber);
        }
        return String.format("%s [%-5s] [%-8s] [%-20s] %-30s - %s", 
                sdf.format(timestamp), level, type.getDisplayName(), threadName, classInfo, message);
    }
} 