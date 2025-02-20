package com.datasophon.k8s.util;

import java.io.IOException;
import java.io.Writer;

public class UnixNewlineWriter extends Writer {
    public final Writer target;
    private boolean lastCharWasCR = false;

    public UnixNewlineWriter(Writer target) {
        this.target = target;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i = off; i < off + len; i++) {
            char c = cbuf[i];

            if (lastCharWasCR) {
                // 处理CR后跟LF的情况（CRLF）
                if (c == '\n') {
                    target.write('\n');
                } else {
                    target.write('\n');
                    writeSingleChar(c);
                }
                lastCharWasCR = false;
            } else if (c == '\r') {
                // 记录CR状态，等待下一个字符
                lastCharWasCR = true;
            } else if (c == '\n') {
                // 直接处理LF
                target.write('\n');
            } else {
                // 普通字符直接写入
                target.write(c);
            }
        }
    }

    private void writeSingleChar(char c) throws IOException {
        if (c == '\r') {
            lastCharWasCR = true;
        } else {
            target.write(c);
        }
    }

    @Override
    public void flush() throws IOException {
        // 处理缓冲区末尾单独的CR
        if (lastCharWasCR) {
            target.write('\n');
            lastCharWasCR = false;
        }
        target.flush();
    }

    @Override
    public void close() throws IOException {
        flush(); // 确保最后的CR被处理
        target.close();
    }

    // 快捷使用方法
    public static UnixNewlineWriter wrap(Writer writer) {
        return new UnixNewlineWriter(writer);
    }

    // 示例用法
    public static void main(String[] args) throws IOException {
        try (Writer rawWriter = new java.io.FileWriter("output.txt");
             UnixNewlineWriter writer = new UnixNewlineWriter(rawWriter)) {

            writer.write("Line 1\r\n");    // 转换为\n
            writer.write("Line 2\n");       // 保持\n
            writer.write("Line 3\r");      // 转换为\n
            writer.write("Line 4");         // 无换行
            writer.write("\r");            // 单独的CR转换为\n
            writer.write("\nLine 5");       // 保持\n
        }
    }
}