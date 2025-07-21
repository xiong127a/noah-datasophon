package com.datasophon.api.utils;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.dao.entity.FrameServiceEntity;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 元数据工具类
 * 用于处理服务配置元数据相关操作
 */
@UtilityClass
public class MetaPathUtils {
    private static final Logger logger = LoggerFactory.getLogger(MetaPathUtils.class);

    /**
     * 获取服务配置文件生成器列表（从数据库）
     * 
     * @param frameCode   框架代码
     * @param serviceName 服务名称
     * @return 配置文件生成器列表
     */
    public static JSONArray getServiceConfigGenerators(String frameCode, String serviceName) {
        try {
            // 获取服务元数据
            JSONObject metaJson = getServiceMetaJson(frameCode, serviceName);
            if (metaJson == null) {
                return new JSONArray();
            }

            // 解析配置生成器
            JSONObject configWriter = metaJson.getJSONObject("configWriter");
            if (configWriter == null) {
                logger.warn("服务配置中未包含configWriter: frameCode={}, serviceName={}", frameCode, serviceName);
                return new JSONArray();
            }

            JSONArray generators = configWriter.getJSONArray("generators");
            if (generators == null || generators.isEmpty()) {
                logger.warn("服务配置中未包含generators: frameCode={}, serviceName={}", frameCode, serviceName);
                return new JSONArray();
            }

            return generators;
        } catch (Exception e) {
            logger.error("获取服务配置文件生成器列表失败", e);
            return new JSONArray();
        }
    }

    /**
     * 获取服务元数据JSON
     * 
     * @param frameCode   框架代码
     * @param serviceName 服务名称
     * @return 服务元数据JSON对象
     */
    public static JSONObject getServiceMetaJson(String frameCode, String serviceName) {
        try {
            // 从数据库获取服务配置
            FrameServiceService frameServiceService = SpringUtil.getBean(FrameServiceService.class);
            if (frameServiceService == null) {
                logger.error("无法获取FrameServiceService Bean");
                return null;
            }

            FrameServiceEntity serviceEntity = frameServiceService.getServiceByFrameCodeAndServiceName(frameCode,
                    serviceName);
            if (serviceEntity == null) {
                logger.error("未找到服务配置: frameCode={}, serviceName={}", frameCode, serviceName);
                return null;
            }

            String serviceJson = serviceEntity.getServiceJson();
            if (CharSequenceUtil.isBlank(serviceJson)) {
                logger.error("服务配置JSON为空: frameCode={}, serviceName={}", frameCode, serviceName);
                return null;
            }

            // 解析JSON
            return JSON.parseObject(serviceJson);
        } catch (Exception e) {
            logger.error("获取服务元数据JSON失败", e);
            return null;
        }
    }
}