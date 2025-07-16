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

import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

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

    /**
     * 获取一个文件的md5值(可处理大文件)
     * 
     * @return md5 value
     */
    public static String md5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return new String(Hex.encodeHex(MD5.digest()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 从 tar.gz 的压缩包内读取一个 文本文件
     * 
     * @param targz
     * @param name
     * @return
     * @throws IOException
     */
    public static String readTargzTextFile(File targz, String name, Charset charset) throws IOException {
        String content = null;
        TarEntry tarEntry = null;
        try (TarInputStream tarInputStream = new TarInputStream(new GZIPInputStream(new FileInputStream(targz)));
                BufferedReader reader = new BufferedReader(new InputStreamReader(tarInputStream, charset));) {
            boolean hasNext = reader.readLine() != null;
            if (hasNext) {
                return null;
            }
            while ((tarEntry = tarInputStream.getNextEntry()) != null) {
                String entryName = tarEntry.getName();
                if (tarEntry.isDirectory()) {
                    // 如果是文件夹,创建文件夹并加速循环
                    continue;
                }
                if (entryName.endsWith(name)) {
                    // 找到第一个文件就结束
                    content = CharStreams.toString(reader);
                    break;
                }
            }
        }
        return content;
    }

    /**
     * 读取文件第一行，第一行的非空行
     * 
     * @param file
     * @return
     * @throws Exception
     */
    public static String readFirstLine(File file) throws Exception {
        final String firstLine = CharStreams.readLines(new FileReader(file), new LineProcessor<String>() {

            String firstLine = null;

            @Override
            public boolean processLine(String line) throws IOException {
                this.firstLine = line;
                // 第一行非空则返回
                return StringUtils.trimToNull(line) == null;
            }

            @Override
            public String getResult() {
                return firstLine;
            }
        });
        return firstLine;
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

        // 计算大致需要的容量以减少扩容操作
        int capacity = 0;
        for (String path : paths) {
            if (StringUtils.isNotBlank(path)) {
                capacity += path.length() + 1; // +1 for potential separator
            }
        }

        StringBuilder sb = new StringBuilder(capacity);
        boolean isFirst = true;

        for (String path : paths) {
            if (StringUtils.isBlank(path)) {
                continue;
            }

            // 标准化当前路径段（替换Windows分隔符为Linux分隔符）
            String normalized = path.replace('\\', '/');

            if (isFirst) {
                // 第一个路径片段，保留开头的/（如果有）
                sb.append(normalized);
                isFirst = false;
            } else {
                // 非第一个路径片段
                if (sb.charAt(sb.length() - 1) != '/' && normalized.charAt(0) != '/') {
                    // 如果前一段不以/结尾且当前段不以/开头，添加分隔符
                    sb.append('/');
                } else if (sb.charAt(sb.length() - 1) == '/' && normalized.charAt(0) == '/') {
                    // 如果前一段以/结尾且当前段以/开头，去掉当前段开头的/
                    normalized = normalized.substring(1);
                }
                sb.append(normalized);
            }
        }

        // 移除结尾的/（如果有且不是根路径"/"）
        int sbLen = sb.length();
        if (sbLen > 1 && sb.charAt(sbLen - 1) == '/') {
            sb.setLength(sbLen - 1);
        }

        return sb.toString();
    }

    /**
     * 读取文件最后几行 <br>
     * 相当于Linux系统中的tail命令 读取大小限制是2GB
     *
     * @param filename 文件名
     * @param charset  文件编码格式,传null默认使用defaultCharset
     * @param rows     读取行数
     * @throws IOException IOException
     */
    public static String readLastRows(String filename, Charset charset, int rows) throws IOException {
        charset = charset == null ? Charset.defaultCharset() : charset;
        byte[] lineSeparator = System.getProperty("line.separator").getBytes();
        try (RandomAccessFile rf = new RandomAccessFile(filename, "r")) {
            // 每次读取的字节数要和系统换行符大小一致
            byte[] c = new byte[lineSeparator.length];
            // 在获取到指定行数和读完文档之前,从文档末尾向前移动指针,遍历文档每一个字节
            for (long pointer = rf.length(), lineSeparatorNum = 0; pointer >= 0 && lineSeparatorNum < rows;) {
                // 移动指针
                rf.seek(pointer--);
                // 读取数据
                int readLength = rf.read(c);
                if (readLength != -1 && Arrays.equals(lineSeparator, c)) {
                    lineSeparatorNum++;
                }
                // 扫描完依然没有找到足够的行数,将指针归0
                if (pointer == -1 && lineSeparatorNum < rows) {
                    rf.seek(0);
                }
            }
            byte[] tempbytes = new byte[(int) (rf.length() - rf.getFilePointer())];
            rf.readFully(tempbytes);
            return new String(tempbytes, charset);
        }
    }

    /**
     * 替换字符串中的主机名部分
     * 
     * @param original 原始字符串（包含URL）
     * @param newHost 新的主机名（IP或域名）
     * @return 替换主机名后的字符串
     */
    public static String replaceHost(String original, String newHost) {
        if (StringUtils.isBlank(original) || StringUtils.isBlank(newHost)) {
            return original;
        }
        // 匹配协议://主机名(:端口)的模式
        return original.replaceAll("(\\w+://)([\\w.-]+)(:\\d+)?", "$1" + newHost + "$3");
    }
}
