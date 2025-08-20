/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.common.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.PropertyPlaceholderHelper;

public class PlaceholderUtils {
    
    // Spring自带的占位符处理器，支持${...}格式
    // 参数：前缀="${"，后缀="}"，默认值分隔符=":"，忽略不可解析的占位符=true
    private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER = 
        new PropertyPlaceholderHelper("${", "}");

    public static void main(String[] args) {
        // 测试新的简化API
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("${apiHost}", "ddp1016");
        paramMap.put("${apiPort}", "8081");
        
        // 现在可以直接使用，无需regex参数
        String replacePlaceholders = PlaceholderUtils.replacePlaceholders("""
                [
                    {
                      "name": "apiHost",
                      "label": "DDH管理端地址",
                      "description": "DDH管理端地址",
                      "required": true,
                      "type": "input",
                      "value": "",
                      "configurableInWizard": true,
                      "hidden": false,
                      "defaultValue": "${apiHost}:${apiPort}"
                    },
                    {
                      "name": "apiPort",
                      "label": "DDH管理端端口",
                      "description": "DDH管理端端口",
                      "required": true,
                      "type": "input",
                      "value": "",
                      "configurableInWizard": true,
                      "hidden": false,
                      "defaultValue": "${apiPort}"
                    }
                  ]""", paramMap);

        System.out.println(replacePlaceholders);
        List<String> newEquipmentNoList = PlaceholderUtils.getNewEquipmentNoList("001", "002");
        for (String s : newEquipmentNoList) {
            System.out.println(s);
        }
    }

    /**
     * 替换字符串中${...}占位符
     * 使用Spring框架的PropertyPlaceholderHelper实现，代码更简洁高效
     */
    public static String replacePlaceholders(String value, Map<String, String> paramsMap, String regex) {
        if (value == null || paramsMap == null || paramsMap.isEmpty()) {
            return value;
        }
        
        // 将Map<String, String>转换为Properties，并处理${key}格式的key
        Properties properties = new Properties();
        paramsMap.forEach((key, val) -> {
            // 如果key包含${}，去掉这些符号；否则直接使用
            String cleanKey = key.startsWith("${") && key.endsWith("}") 
                ? key.substring(2, key.length() - 1) 
                : key;
            properties.setProperty(cleanKey, val);
        });
        
        // 使用Spring的PropertyPlaceholderHelper进行替换，比正则表达式更高效
        return PLACEHOLDER_HELPER.replacePlaceholders(value, properties);
    }
    
    /**
     * 替换字符串中${...}占位符 - 简化版本，不需要regex参数
     */
    public static String replacePlaceholders(String value, Map<String, String> paramsMap) {
        return replacePlaceholders(value, paramsMap, null);
    }

    public static List<String> getMatchValue(String value) {
        String regex = "\\[.*?\\]";
        ArrayList<String> list = new ArrayList<>();

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        // 自旋进行最小匹配，直到无法匹配
        while (matcher.find()) {
            String group = matcher.group();
            // 替换匹配内容
            list.add(group);
        }
        return list;
    }

    public static List<String> getNewEquipmentNoList(String pre, String last) {
        int length = pre.length();
        ArrayList<String> list = new ArrayList<>();
        int start = Integer.parseInt(pre);
        int end = Integer.parseInt(last);
        int next = start;
        list.add(pre);
        while (next < end) {
            next = next + 1;
            String nextStr = String.format("%0" + length + "d", next);
            list.add(nextStr);
        }
        return list;
    }

}
