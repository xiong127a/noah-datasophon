package com.datasophon.plugins.api.model;

/**
 * 命令执行结果
 *
 * @author DataSophon Team
 */
public record CommandResult(String command, int exitCode, String output, String error) {

    public boolean isSuccess() {
        return exitCode == 0;
    }
}