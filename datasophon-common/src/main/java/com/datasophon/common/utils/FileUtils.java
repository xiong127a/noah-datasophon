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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * 基本文件的特殊操作，文件MD5，从 targz 压缩包不解压读取一个文本文件，读取一个文件的第一行 等
 *
 * <pre>
 *
 * Created by zhenqin.
 * User: zhenqin
 * Date: 2023/4/21
 * Time: 下午9:58
 *
 * </pre>
 *
 * @author zhenqin
 */
public class FileUtils {

    // 用于replaceHost方法的编译正则表达式（线程安全）
    private static final Pattern HOST_PATTERN = Pattern.compile("(\\w+://)([\\w.-]+)(:\\d+)?");

    /**
     * 获取一个文件的md5值(可处理大文件)
     * 
     * @return md5 value
     */
    public static String md5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fileInputStream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 从 tar.gz 的压缩包内读取一个 文本文件
     *
     */
    public static String readTargzTextFile(File targz, String name, Charset charset) throws IOException {
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
                new GzipCompressorInputStream(new FileInputStream(targz)))) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entry.isDirectory()) {
                    // 如果是文件夹，跳过
                    continue;
                }
                if (entryName.endsWith(name)) {
                    // 找到目标文件，读取内容并返回
                    return IOUtils.toString(tarIn, charset);
                }
            }
        }
        return null;
    }

    /**
     * 读取文件第一行，第一行的非空行
     *
     */
    public static String readFirstLine(File file) throws Exception {
        try {
            return org.apache.commons.io.FileUtils.readLines(file, Charset.defaultCharset())
                    .stream()
                    .filter(StringUtils::isNotBlank)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new Exception("Failed to read first line", e);
        }
    }

    /**
     * 连接路径，确保使用Linux风格的分隔符(/)
     * 
     * <p>
     * 该方法可以连接多个路径片段，并自动处理以下情况：
     * </p>
     * <ul>
     * <li>自动跳过空或null的路径片段</li>
     * <li>自动将Windows风格反斜杠(\)转换为Linux风格正斜杠(/)</li>
     * <li>智能处理路径片段之间的分隔符，不会出现重复的/或缺失的/</li>
     * <li>保留第一个路径片段开头的/（如果存在），表示绝对路径</li>
     * <li>移除结果路径末尾的/（根路径"/"除外）</li>
     * </ul>
     * 
     * <p>
     * <strong>示例：</strong>
     * </p>
     * 
     * <pre>
     * concatPath("/usr", "local", "bin")         返回 "/usr/local/bin"
     * concatPath("usr", "local", "bin")          返回 "usr/local/bin"
     * concatPath("/usr/", "/local/", "/bin/")    返回 "/usr/local/bin"
     * concatPath("/usr", "", "bin")              返回 "/usr/bin"
     * concatPath("C:\\Program Files", "App")     返回 "C:/Program Files/App"
     * concatPath("/")                            返回 "/"
     * concatPath("/usr", "..", "bin")            返回 "/usr/../bin"（注意：不会解析..）
     * concatPath()                               返回 ""
     * </pre>
     * 
     * <p>
     * <strong>注意：</strong>
     * </p>
     * <ul>
     * <li>此方法不会解析路径中的 "." 或 ".."，如需规范化路径请使用其他方法</li>
     * <li>此方法总是使用Linux风格的正斜杠(/)，即使在Windows环境中</li>
     * <li>返回的路径在Kubernetes容器环境中可以直接使用</li>
     * </ul>
     *
     * @param paths 要连接的路径片段
     * @return 连接后的路径（使用/作为分隔符）
     */
    public static String concatPath(String... paths) {
        if (paths == null || paths.length == 0) {
            return "";
        }

        // 过滤掉空路径
        List<String> validPaths = new ArrayList<>();
        boolean isAbsolutePath = false;

        for (String path : paths) {
            if (StringUtils.isBlank(path)) {
                continue;
            }

            // 检查第一个有效路径是否为绝对路径
            if (validPaths.isEmpty() && path.startsWith("/")) {
                isAbsolutePath = true;
            }

            validPaths.add(path);
        }

        if (validPaths.isEmpty()) {
            return "";
        }

        // 使用Java NIO构建路径
        Path result;
        try {
            String first = validPaths.getFirst();
            String[] more = validPaths.subList(1, validPaths.size()).toArray(new String[0]);
            result = Paths.get(first, more);
        } catch (Exception e) {
            // 回退到原始方法

            return getPathStr(validPaths);
        }

        // 确保分隔符为Linux风格
        String pathStr = result.toString().replace('\\', '/');

        // 保持原始行为，确保绝对路径保持/开头
        if (isAbsolutePath && !pathStr.startsWith("/")) {
            pathStr = "/" + pathStr;
        }

        // 保持原始行为，移除末尾的斜杠（除非是根路径"/"）
        if (pathStr.length() > 1 && pathStr.endsWith("/")) {
            pathStr = pathStr.substring(0, pathStr.length() - 1);
        }

        return pathStr;
    }

    private static String getPathStr(List<String> validPaths) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        for (String path : validPaths) {
            String normalized = path.replace('\\', '/');

            if (isFirst) {
                sb.append(normalized);
                isFirst = false;
            } else {
                if (!sb.toString().endsWith("/") && !normalized.startsWith("/")) {
                    sb.append("/");
                } else if (sb.toString().endsWith("/") && normalized.startsWith("/")) {
                    normalized = normalized.substring(1);
                }
                sb.append(normalized);
            }
        }

        // 保持原始行为，移除末尾的斜杠（除非是根路径"/"）
        String pathStr = sb.toString();
        if (pathStr.length() > 1 && pathStr.endsWith("/")) {
            pathStr = pathStr.substring(0, pathStr.length() - 1);
        }
        return pathStr;
    }

    /**
     * 读取文件最后几行 <br>
     * 相当于Linux系统中的tail命令
     *
     * @param filename 文件名
     * @param charset  文件编码格式,传null默认使用defaultCharset
     * @param rows     读取行数
     * @throws IOException IOException
     */
    public static String readLastRows(String filename, Charset charset, int rows) throws IOException {
        charset = charset == null ? Charset.defaultCharset() : charset;
        List<String> lastLines = new ArrayList<>();

        try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
                .setFile(new File(filename))
                .setCharset(charset)
                .get()) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < rows) {
                lastLines.add(line);
                count++;
            }
        }

        // 反转为正确顺序
        Collections.reverse(lastLines);
        return String.join(System.lineSeparator(), lastLines);
    }

    /**
     * 替换字符串中的主机名部分
     * 
     * @param original 原始字符串（包含URL）
     * @param newHost  新的主机名（IP或域名）
     * @return 替换主机名后的字符串
     */
    public static String replaceHost(String original, String newHost) {
        if (StringUtils.isBlank(original) || StringUtils.isBlank(newHost)) {
            return original;
        }

        // 使用预编译的正则表达式
        Matcher matcher = HOST_PATTERN.matcher(original);
        return matcher.replaceAll("$1" + Matcher.quoteReplacement(newHost) + "$3");
    }
}
