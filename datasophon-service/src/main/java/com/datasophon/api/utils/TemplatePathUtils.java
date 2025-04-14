package com.datasophon.api.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板路径工具类
 * 用于处理模板文件路径相关操作
 */
@UtilityClass
public class TemplatePathUtils {
    private static final Logger logger = LoggerFactory.getLogger(TemplatePathUtils.class);

    /**
     * 模板目录路径缓存
     */
    private static String templateDirectoryPath = null;

    /**
     * 获取模板目录路径
     * 
     * @return 模板目录路径
     */
    public static String getTemplateDirectoryPath() {
        if (CharSequenceUtil.isNotBlank(templateDirectoryPath)) {
            return templateDirectoryPath;
        }

        String path = System.getProperty("user.dir");
        // 向上找两级父目录
        String parentPath = new File(path).getParent();
        if (CharSequenceUtil.isBlank(parentPath)) {
            logger.error("获取父目录失败");
            return null;
        }

        // 构建模板目录路径
        templateDirectoryPath = parentPath + File.separator + "datasophon-worker" + File.separator + "src"
                + File.separator + "main" + File.separator + "resources" + File.separator + "templates";

        if (!FileUtil.exist(templateDirectoryPath)) {
            logger.error("模板目录不存在: {}", templateDirectoryPath);
            return null;
        }

        logger.info("模板目录路径: {}", templateDirectoryPath);
        return templateDirectoryPath;
    }

    /**
     * 获取模板文件路径
     * 
     * @param templateName 模板名称
     * @return 模板文件完整路径
     */
    public static String getTemplateFilePath(String templateName) {
        String templateDir = getTemplateDirectoryPath();
        if (CharSequenceUtil.isBlank(templateDir)) {
            return null;
        }

        String templateFilePath = templateDir + File.separator + templateName;
        if (!FileUtil.exist(templateFilePath)) {
            logger.error("模板文件不存在: {}", templateFilePath);
            return null;
        }

        return templateFilePath;
    }

    /**
     * 清除模板路径缓存
     */
    public static void clearTemplateCache() {
        templateDirectoryPath = null;
        logger.info("模板路径缓存已清除");
    }

    /**
     * 获取所有可用的模板文件名列表
     *
     * @return 模板文件名列表
     */
    public static List<String> getTemplateList() {
        // 使用TemplatePathUtils工具类获取模板目录路径
        String templateDirPath = TemplatePathUtils.getTemplateDirectoryPath();
        if (templateDirPath == null) {
            logger.error("模板目录不存在");
            return new ArrayList<>();
        }

        File templateDir = new File(templateDirPath);
        File[] templates = templateDir.listFiles();

        List<String> templateList = new ArrayList<>();
        if (templates != null) {
            for (File template : templates) {
                if (template.isFile()) {
                    templateList.add(template.getName());
                }
            }
        }

        return templateList;
    }

    /**
     * 获取指定模板文件内容
     *
     * @param templateName 模板文件名
     * @return 模板文件内容
     */
    public String getTemplateContent(String templateName) {
        // 使用TemplatePathUtils工具类获取模板文件路径
        String templateFilePath = TemplatePathUtils.getTemplateFilePath(templateName);
        if (templateFilePath == null) {
            logger.error("模板文件未找到: {}", templateName);
            return null;
        }
        // 读取文件内容
        return FileUtil.readUtf8String(templateFilePath);
    }
}