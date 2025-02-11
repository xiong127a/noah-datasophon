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

import cn.hutool.core.collection.CollUtil;
import com.datasophon.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShellUtils {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private ShellUtils() {
        throw new UnsupportedOperationException("This class should not be instantiated");
    }

    public static ExecResult execShell(String pathOrCommand) {
        ExecResult result = new ExecResult();
        StringBuilder outputBuffer = new StringBuilder();
        StringBuilder errorBuffer = new StringBuilder();

        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sh", "-c", pathOrCommand);
            process = processBuilder.start();

            try (InputStream inputStream = process.getInputStream();
                 InputStream errorStream = process.getErrorStream()) {

                // 使用并行读取避免阻塞
                Thread outputThread = readStream(inputStream, outputBuffer, log);
                Thread errorThread = readStream(errorStream, errorBuffer, log);

                outputThread.join();
                errorThread.join();
            }

            int exitValue = process.waitFor();
            String execOut = outputBuffer.toString();

            if (exitValue == 0) {
                log.info("Command [{}] executed successfully\nOutput: {}", pathOrCommand, execOut);
                result.setExecResult(true);
                result.setExecOut(execOut);
            } else {
                String errorMsg = String.format("Command failed with code %d\nError: %s",
                        exitValue, errorBuffer.toString());
                log.error("Command [{}] failed\nOutput: {}\nError: {}",
                        pathOrCommand, execOut, errorBuffer.toString());
                result.setExecOut(errorMsg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleException(result, "Command execution interrupted", e);
        } catch (Exception e) {
            handleException(result, "Command execution error", e);
        } finally {
            safeDestroyProcess(process);
        }
        return result;
    }

    public static ExecResult execWithStatus(String workPath, List<String> command, long timeout) {
        return execWithStatus(workPath, command, timeout, log);
    }

    public static ExecResult execWithStatus(String workPath, List<String> command,
                                            long timeout, Logger logger) {
        ExecResult result = new ExecResult();
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            if (workPath != null) {
                processBuilder.directory(new File(workPath));
            }
            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();
            ExecutorService executor = Executors.newFixedThreadPool(2);

            StringBuilder outputBuffer = new StringBuilder();
            Process finalProcess = process;
            executor.submit(() -> readOutput(finalProcess.getInputStream(), outputBuffer, logger));

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Process output reading timed out");
            }

            if (finished && process.exitValue() == 0) {
                logger.info("Command executed successfully: {}", String.join(" ", command));
                result.setExecResult(true);
                result.setExecOut(outputBuffer.toString());
            } else {
                String errorMsg = finished ?
                        "Exited with code: " + process.exitValue() : "Process timed out";
                logger.error("Command failed: {} - {}", String.join(" ", command), errorMsg);
                result.setExecOut(errorMsg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleException(result, "Command execution interrupted", e);
        } catch (Exception e) {
            handleException(result, "Command execution error", e);
        } finally {
            safeDestroyProcess(process);
        }
        return result;
    }

    private static Thread readStream(InputStream inputStream, StringBuilder buffer, Logger logger) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append(System.lineSeparator());
                    logger.debug(line);
                }
            } catch (IOException e) {
                logger.error("Error reading stream", e);
            }
        });
        thread.start();
        return thread;
    }

    private static void readOutput(InputStream inputStream, StringBuilder buffer, Logger logger) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append(System.lineSeparator());
                logger.info(line);
            }
        } catch (IOException e) {
            logger.error("Error reading output stream", e);
        }
    }

    public static String getCpuArchitecture() {
        try {
            Process process = new ProcessBuilder("arch").start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            if (process.waitFor() == 0) {
                return output.toString().trim();
            }
        } catch (Exception e) {
            log.error("Failed to get CPU architecture", e);
        }
        return "unknown";
    }

    public static void addChmod(String path, String chmod) {
        executePrivilegeCommand("chmod", "-R", chmod, path);
    }

    public static void addChown(String path, String user, String group) {
        executePrivilegeCommand("chown", "-R", user + ":" + group, path);
    }

    private static void executePrivilegeCommand(String... commandParts) {
        List<String> command = CollUtil.newArrayList(commandParts);
        ExecResult result = execWithStatus(Constants.INSTALL_PATH, command, DEFAULT_TIMEOUT_SECONDS);
        if (!result.getExecResult()) {
            log.error("Failed to execute command: {}", String.join(" ", command));
        }
    }

    private static void handleException(ExecResult result, String message, Exception e) {
        log.error(message, e);
        result.setExecResult(false);
        result.setExecErrOut(message + ": " + e.getMessage());
    }

    private static void safeDestroyProcess(Process process) {
        if (process != null) {
            try {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                log.warn("Error destroying process", e);
            }
        }
    }
}
