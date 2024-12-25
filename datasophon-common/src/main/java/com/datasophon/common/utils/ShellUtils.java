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

import com.datasophon.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShellUtils {

    private static ProcessBuilder processBuilder = new ProcessBuilder();



    public static Process exec(List<String> command) {
        Process process = null;
        try {
            String[] commands = new String[command.size()];
            command.toArray(commands);
            processBuilder.command(commands);
            process = processBuilder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return process;
    }

    /**
     * @param pathOrCommand 脚本路径或者命令
     * @return
     */
    public static ExecResult exceShell(String pathOrCommand) {
        ExecResult result = new ExecResult();
        StringBuffer outputBuffer = new StringBuffer();
        StringBuffer errorBuffer = new StringBuffer();

        try {
            // 执行脚本
            Process ps = Runtime.getRuntime().exec(new String[]{"sh", "-c", pathOrCommand});

            // 读取标准输出流
            try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(ps.getErrorStream()))) {

                String line;
                while ((line = outputReader.readLine()) != null) {
                    outputBuffer.append(line);
                    outputBuffer.append(System.lineSeparator());
                }

                // 读取错误输出流
                while ((line = errorReader.readLine()) != null) {
                    errorBuffer.append(line);
                    errorBuffer.append(System.lineSeparator());
                }
            }

            // 等待进程结束
            int exitValue = ps.waitFor();
            String execOut = outputBuffer.toString();

            if (exitValue == 0) {
                log.info("{} command exec out is : {} {}", pathOrCommand, System.lineSeparator(), execOut);
                result.setExecResult(true);
                result.setExecOut(execOut);
            } else {
                result.setExecOut("call shell failed. error code is :" + exitValue + ", error: " + errorBuffer.toString());
                log.error("{} command exec out is : {} {}", pathOrCommand, System.lineSeparator(), execOut);
                log.error("Error output: {}", errorBuffer.toString());
            }

        } catch (Exception e) {
            result.setExecOut(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    // 获取cpu架构 arm或x86
    public static String getCpuArchitecture() {
        try {
            Process ps = Runtime.getRuntime().exec("arch");
            StringBuffer stringBuffer = new StringBuffer();
            int exitValue = ps.waitFor();
            if (0 == exitValue) {
                // 只能接收脚本echo打印的数据，并且是echo打印的最后一次数据
                BufferedInputStream in = new BufferedInputStream(ps.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = br.readLine()) != null) {
                    log.info("脚本返回的数据如下： " + line);
                    stringBuffer.append(line);
                }
                in.close();
                br.close();
                return stringBuffer.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static ExecResult execWithStatus(String workPath, List<String> command, long timeout) {
        Process process = null;
        ExecResult result = new ExecResult();
        try {
            processBuilder.directory(new File(workPath));
            processBuilder.command(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            getOutput(process);
            boolean execResult = process.waitFor(timeout, TimeUnit.SECONDS);
            if (execResult && process.exitValue() == 0) {
                log.info("script execute success --> " + String.join(" ", command));
                result.setExecResult(true);
                result.setExecOut("script execute success");
            } else {
                result.setExecOut("script execute failed --> " + String.join(" ", command));
                log.error(getError(process));
            }
            return result;
        } catch (Exception e) {
            result.setExecErrOut(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public static ExecResult execWithStatus(String workPath, List<String> command, long timeout, Logger logger) {
        Process process = null;
        ExecResult result = new ExecResult();
        try {
            processBuilder.directory(new File(workPath));
            processBuilder.command(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            getOutput(process, logger);
            boolean execResult = process.waitFor(timeout, TimeUnit.SECONDS);
            if (execResult && process.exitValue() == 0) {
                logger.info("script execute success --> " + String.join(" ", command));
                result.setExecResult(true);
                result.setExecOut("script execute success --> " + String.join(" ", command));
            } else {
                result.setExecOut("script execute failed --> " + String.join(" ", command));
            }
            return result;
        } catch (Exception e) {
            result.setExecErrOut(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public static void getOutput(Process process, Logger logger) {

        ExecutorService getOutputLogService = Executors.newSingleThreadExecutor();

        getOutputLogService.submit(() -> {
            BufferedReader inReader = null;
            try {
                inReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                StringBuffer stringBuffer = new StringBuffer();
                while ((line = inReader.readLine()) != null) {
                    stringBuffer.append(line);
                    stringBuffer.append(System.lineSeparator());
                }
                if (stringBuffer.length() != 0) {
                    logger.info(stringBuffer.toString());
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                closeQuietly(inReader);
            }
            BufferedReader errorReader = null;
            try {
                errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                StringBuffer stringBuffer = new StringBuffer();
                while ((line = errorReader.readLine()) != null) {
                    stringBuffer.append(line);
                    stringBuffer.append(System.lineSeparator());
                }
                if (stringBuffer.length() != 0) {
                    logger.error(stringBuffer.toString());
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                closeQuietly(errorReader);
            }
        });
        getOutputLogService.shutdown();
    }

    public static void getOutput(Process process) {

        ExecutorService getOutputLogService = Executors.newSingleThreadExecutor();

        getOutputLogService.submit(() -> {
            BufferedReader inReader = null;
            try {
                inReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                StringBuffer stringBuffer = new StringBuffer();
                while ((line = inReader.readLine()) != null) {
                    stringBuffer.append(line);
                    stringBuffer.append(System.lineSeparator());
                }
                if (stringBuffer.length() != 0) {
                    log.trace(stringBuffer.toString());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                closeQuietly(inReader);
            }
        });
        getOutputLogService.shutdown();
    }

    public static String getError(Process process) {
        String errput = null;
        BufferedReader reader = null;
        try {
            if (process != null) {
                StringBuffer stringBuffer = new StringBuffer();
                reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while (reader.read() != -1) {
                    stringBuffer.append("\n" + reader.readLine());
                }
                errput = stringBuffer.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeQuietly(reader);
        return errput;
    }

    public static void closeQuietly(Reader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void destroy(Process process) {
        if (process != null) {
            process.destroyForcibly();
        }
    }

    public static void addChmod(String path, String chmod) {
        ArrayList<String> command = new ArrayList<>();
        command.add("chmod");
        command.add("-R");
        command.add(chmod);
        command.add(path);
        execWithStatus(Constants.INSTALL_PATH, command, 60, log);
    }

    public static void addChown(String path, String user, String group) {
        ArrayList<String> command = new ArrayList<>();
        command.add("chown");
        command.add("-R");
        command.add(user + ":" + group);
        command.add(path);
        execWithStatus(Constants.INSTALL_PATH, command, 60, log);
    }
}
