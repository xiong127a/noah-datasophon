package com.datasophon.k8s.util;

/**
 * 控制台彩色日志工具类
 */
public class ColorLogUtils {

    // ANSI 颜色转义序列
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    // 背景色
    public static final String ANSI_BLACK_BG = "\u001B[40m";
    public static final String ANSI_RED_BG = "\u001B[41m";
    public static final String ANSI_GREEN_BG = "\u001B[42m";
    public static final String ANSI_YELLOW_BG = "\u001B[43m";
    public static final String ANSI_BLUE_BG = "\u001B[44m";
    public static final String ANSI_PURPLE_BG = "\u001B[45m";
    public static final String ANSI_CYAN_BG = "\u001B[46m";
    public static final String ANSI_WHITE_BG = "\u001B[47m";

    // 文本样式
    public static final String ANSI_BOLD = "\u001B[1m";

    /**
     * 打印绿色背景的创建资源日志
     * 
     * @param resourceType 资源类型
     * @param resourceName 资源名称
     * @param namespace    命名空间
     */
    public static void printResourceCreated(String resourceType, String resourceName, String namespace) {
        System.out.println(ANSI_GREEN_BG + ANSI_BLACK + " ✓ CREATED " + ANSI_RESET +
                ANSI_GREEN + " " + resourceType + " " + ANSI_RESET +
                ANSI_BOLD + "\"" + resourceName + "\"" + ANSI_RESET +
                ANSI_CYAN + " in namespace " + ANSI_RESET +
                ANSI_BOLD + "\"" + namespace + "\"" + ANSI_RESET);
    }

    /**
     * 打印蓝色背景的更新资源日志
     * 
     * @param resourceType 资源类型
     * @param resourceName 资源名称
     * @param namespace    命名空间
     */
    public static void printResourceUpdated(String resourceType, String resourceName, String namespace) {
        System.out.println(ANSI_BLUE_BG + ANSI_BLACK + " ↻ UPDATED " + ANSI_RESET +
                ANSI_BLUE + " " + resourceType + " " + ANSI_RESET +
                ANSI_BOLD + "\"" + resourceName + "\"" + ANSI_RESET +
                ANSI_CYAN + " in namespace " + ANSI_RESET +
                ANSI_BOLD + "\"" + namespace + "\"" + ANSI_RESET);
    }

    /**
     * 打印黄色背景的警告日志
     * 
     * @param message 警告消息
     */
    public static void printWarning(String message) {
        System.out.println(ANSI_YELLOW_BG + ANSI_BLACK + " ! WARNING " + ANSI_RESET +
                ANSI_YELLOW + " " + message + ANSI_RESET);
    }

    /**
     * 打印红色背景的错误日志
     * 
     * @param message 错误消息
     */
    public static void printError(String message) {
        System.out.println(ANSI_RED_BG + ANSI_WHITE + " ✗ ERROR " + ANSI_RESET +
                ANSI_RED + " " + message + ANSI_RESET);
    }
}