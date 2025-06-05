package com.datasophon.api.utils;

import cn.hutool.core.io.FileUtil;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

/**
 * 模板路径工具类
 * 用于处理模板文件路径相关操作
 */
@UtilityClass
public class TemplatePathUtils {
    private static final Logger logger = LoggerFactory.getLogger(TemplatePathUtils.class);


    private static final String PATH = "worker" + File.separator + "templates" + File.separator;


    /**
     * 获取模板文件路径
     *
     * @param templateName 模板名称
     * @return 模板文件完整路径
     */
    public static String getTemplateFilePath(String path, String templateName) {
        File file = getTemplateFile(path, templateName);
        return Objects.requireNonNull(file).getAbsolutePath();
    }

    public static File getTemplateFile(String path, String templateName) {
        return FileUtil.file(path+"/"+templateName);
    }

    /**
     * 获取指定模板文件内容
     *
     * @param templateName 模板文件名
     * @return 模板文件内容
     */
    public static String getTemplateContent(String templateName) {
        try {
            String templateFilePath = getTemplateFilePath(PATH, templateName);
            return FileUtil.readUtf8String(templateFilePath);
        } catch (Exception e) {
            logger.error("获取模板内容时发生异常: {}", templateName, e);
            return null;
        }
    }
}