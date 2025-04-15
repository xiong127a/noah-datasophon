package com.datasophon.api.utils;

import cn.hutool.core.io.FileUtil;
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


    private static final String PATH = "worker" + File.separator + "templates" + File.separator;


    /**
     * 获取模板文件路径
     *
     * @param templateName 模板名称
     * @return 模板文件完整路径
     */
    public static String getTemplateFilePath(String templateName) {
        File file = FileUtil.file(PATH + templateName);
        if (!FileUtil.exist(file)) {
            logger.error("模板文件不存在: {}", file.getAbsolutePath());
            return null;
        }

        return file.getAbsolutePath();
    }

    /**
     * 获取所有可用的模板文件名列表
     *
     * @return 模板文件名列表
     */
    public static List<String> getTemplateList() {


        File[] templates = FileUtil.ls(PATH);

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
    public static String getTemplateContent(String templateName) {
        try {
            String templateFilePath = getTemplateFilePath(templateName);
            return FileUtil.readUtf8String(templateFilePath);
        } catch (Exception e) {
            logger.error("获取模板内容时发生异常: {}", templateName, e);
            return null;
        }
    }
}