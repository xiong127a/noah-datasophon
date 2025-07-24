package com.datasophon.api.service.checker.helpers;

/**
 * HTML样式工具类
 * 提供统一的HTML样式生成方法，供所有检查器使用
 */
public class HtmlStyleHelper {

    /**
     * 生成HTML样式的标题
     * 
     * @param title     标题文本
     * @param isSuccess 是否成功（成功显示绿色，失败显示红色）
     * @return HTML格式的标题
     */
    public static String generateTitle(String title, boolean isSuccess) {
        String color = isSuccess ? "#52c41a" : "#f5222d";
        return String.format("<h3 style='color:%s;margin-bottom:10px'>%s</h3>", color, title);
    }

    /**
     * 生成HTML样式的属性行
     * 
     * @param label 属性标签
     * @param value 属性值
     * @param color 属性值颜色
     * @return HTML格式的属性行
     */
    public static String generatePropertyRow(String label, String value, String color) {
        return String.format("<p><strong>%s:</strong> <span style='color:%s;font-weight:bold'>%s</span></p>",
                label, color, value);
    }

    /**
     * 生成HTML样式的属性行（带单位和阈值）
     * 
     * @param label         属性标签
     * @param value         属性值
     * @param color         属性值颜色
     * @param threshold     阈值
     * @param thresholdUnit 阈值单位
     * @return HTML格式的属性行
     */
    public static String generatePropertyRowWithThreshold(String label, String value, String color, int threshold,
            String thresholdUnit) {
        return String.format(
                "<p><strong>%s:</strong> <span style='color:%s;font-weight:bold'>%s</span> (阈值: <span style='color:#722ed1;font-weight:bold'>%d%s</span>)</p>",
                label, color, value, threshold, thresholdUnit);
    }

    /**
     * 生成HTML样式的进度条
     * 
     * @param percentage 百分比（0-100）
     * @param color      进度条颜色
     * @param label      进度条标签
     * @return HTML格式的进度条
     */
    public static String generateProgressBar(int percentage, String color, String label) {
        return String.format(
                "<div style='background:#f0f0f0;border-radius:8px;height:20px;width:100%%;position:relative;overflow:hidden;margin-top:5px;margin-bottom:10px'>"
                        +
                        "<div style='background:%s;height:100%%;width:%d%%;border-radius:8px;'></div>" +
                        "<div style='position:absolute;top:0;left:0;right:0;bottom:0;text-align:center;color:%s;line-height:20px;font-weight:bold;'>%s</div>"
                        +
                        "</div>",
                color, percentage, percentage > 70 ? "white" : "#333", label);
    }

    /**
     * 生成HTML样式的成功提示框
     * 
     * @param title   提示标题
     * @param message 提示内容
     * @return HTML格式的成功提示框
     */
    public static String generateSuccessAlert(String title, String message) {
        return String.format(
                "<div style='background:#f6ffed;border-left:4px solid #52c41a;padding:10px;border-radius:0 4px 4px 0;margin-top:10px'>"
                        +
                        "<p style='margin:0;color:#52c41a;font-weight:bold'>%s</p>" +
                        "<p style='margin-top:5px;margin-bottom:0;'>%s</p>" +
                        "</div>",
                title, message);
    }

    /**
     * 生成HTML样式的警告提示框
     * 
     * @param title   提示标题
     * @param message 提示内容
     * @return HTML格式的警告提示框
     */
    public static String generateWarningAlert(String title, String message) {
        return String.format(
                "<div style='background:#fff2f0;border-left:4px solid #f5222d;padding:10px;border-radius:0 4px 4px 0;margin-top:10px'>"
                        +
                        "<p style='margin:0;color:#f5222d;font-weight:bold'>%s</p>" +
                        "<p style='margin-top:5px;margin-bottom:0;'>%s</p>" +
                        "</div>",
                title, message);
    }

    /**
     * 生成HTML样式的注意提示框
     * 
     * @param title   提示标题
     * @param message 提示内容
     * @return HTML格式的注意提示框
     */
    public static String generateNoteAlert(String title, String message) {
        return String.format(
                "<div style='background:#fffbe6;border-left:4px solid #faad14;padding:10px;border-radius:0 4px 4px 0;margin-top:10px'>"
                        +
                        "<p style='margin:0;color:#d48806;font-weight:bold'>%s</p>" +
                        "<p style='margin-top:5px;margin-bottom:0;color:#666'>%s</p>" +
                        "</div>",
                title, message);
    }

    /**
     * 生成HTML样式的代码块
     * 
     * @param code 代码内容
     * @return HTML格式的代码块
     */
    public static String generateCodeBlock(String code) {
        return String.format(
                "<pre style='background:#f0f2f5;padding:10px;border-radius:5px;overflow:auto;font-family:monospace;margin-top:5px;margin-bottom:10px'>%s</pre>",
                code.replace("\n", "<br>"));
    }

    /**
     * 生成HTML样式的内联代码
     * 
     * @param code 代码内容
     * @return HTML格式的内联代码
     */
    public static String generateInlineCode(String code) {
        return String.format("<code style='background:#f5f5f5;padding:2px 4px;border-radius:3px'>%s</code>", code);
    }

    /**
     * 生成HTML样式的彩色数值
     * 
     * @param value 数值
     * @param color 颜色
     * @return HTML格式的彩色数值
     */
    public static String generateColoredValue(Object value, String color) {
        return String.format("<span style='color:%s;font-weight:bold'>%s</span>", color, value.toString());
    }

    /**
     * 生成HTML样式的容器开始标签
     * 
     * @return HTML容器开始标签
     */
    public static String beginContainer() {
        return "<div style='line-height:1.6'>";
    }

    /**
     * 生成HTML样式的容器结束标签
     * 
     * @return HTML容器结束标签
     */
    public static String endContainer() {
        return "</div>";
    }

    /**
     * 生成HTML样式的分组开始标签
     * 
     * @return HTML分组开始标签
     */
    public static String beginGroup() {
        return "<div style='margin-bottom:15px'>";
    }

    /**
     * 生成HTML样式的分组结束标签
     * 
     * @return HTML分组结束标签
     */
    public static String endGroup() {
        return "</div>";
    }

    /**
     * 根据数值确定颜色（红黄绿）
     * 
     * @param value          当前值
     * @param threshold      阈值
     * @param higherIsBetter 值越高越好
     * @return 颜色代码
     */
    public static String determineColor(double value, double threshold, boolean higherIsBetter) {
        // 绿色
        if (higherIsBetter) {
            if (value < threshold * 0.7)
                return "#f5222d"; // 红色
            if (value < threshold * 0.9)
                return "#faad14"; // 黄色
        } else {
            if (value > threshold * 1.3)
                return "#f5222d"; // 红色
            if (value > threshold * 1.1)
                return "#faad14"; // 黄色
        }
        return "#52c41a"; // 绿色
    }

    /**
     * 获取常用颜色代码
     */
    public static class Colors {
        public static final String SUCCESS = "#52c41a"; // 成功绿色
        public static final String ERROR = "#f5222d"; // 错误红色
        public static final String WARNING = "#faad14"; // 警告黄色
        public static final String INFO = "#1890ff"; // 信息蓝色
        public static final String PURPLE = "#722ed1"; // 紫色
        public static final String CYAN = "#13c2c2"; // 青色
        public static final String ORANGE = "#fa8c16"; // 橙色
        public static final String PINK = "#eb2f96"; // 粉色
        public static final String BLUE = "#2f54eb"; // 蓝色
        public static final String GRAY = "#8c8c8c"; // 灰色
    }

    /**
     * 创建一个进度条
     * 
     * @param percentage 百分比数值
     * @param isError    是否显示为错误状态（红色）
     * @return 进度条HTML代码
     */
    public static String createProgressBar(int percentage, boolean isError) {
        String colorClass = isError ? "background-color:#f5222d" : "background-color:#52c41a";
        return "<div style='width:100%;height:20px;background-color:#f0f0f0;border-radius:10px;margin:10px 0'>" +
                "<div style='width:" + percentage + "%;height:100%;" + colorClass
                + ";border-radius:10px;text-align:center;line-height:20px;color:white;font-size:12px'>" +
                percentage + "%</div></div>";
    }
}