package com.datasophon.api.service.checker.common;

/**
 * 命令执行结果
 */
public class CommandResult {
    private final String output; // 命令输出
    private final String error; // 错误输出
    private final int exitCode; // 退出状态码
    private final boolean success; // 是否成功执行
    private String command; // 执行的命令

    public CommandResult(String output, String error, int exitCode) {
        this.output = output;
        this.error = error;
        this.exitCode = exitCode;
        this.success = exitCode == 0;
    }

    public CommandResult(String command, int exitCode, String output, String error) {
        this.command = command;
        this.output = output;
        this.error = error;
        this.exitCode = exitCode;
        this.success = exitCode == 0;
    }

    // 从MinaUtils.CommandResult创建新实例的静态工厂方法
    public static CommandResult fromMinaCommandResult(CommandResult minaResult) {
        return new CommandResult(minaResult.getCommand(), minaResult.getExitCode(),
                minaResult.getOutput(), minaResult.getError());
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCommand() {
        return command;
    }

    /**
     * 获取错误信息，如果没有错误则返回输出
     */
    public String getErrorOrOutput() {
        return error != null && !error.isEmpty() ? error : output;
    }

    @Override
    public String toString() {
        if (success) {
            return output;
        } else {
            return String.format("ERROR: 命令执行失败 (退出状态码: %d), 错误信息: %s", exitCode, getErrorOrOutput());
        }
    }
}